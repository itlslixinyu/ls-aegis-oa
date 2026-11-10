"""Exercise the fixed R43 nullable-column release on an owned disposable MySQL, never the in-use DB."""
import argparse,hashlib,json,secrets,subprocess,time,re
from pathlib import Path
root=Path(__file__).resolve().parents[2];local=root/'.local/r43-migration-probe';local.mkdir(parents=True,exist_ok=True)
statefile=local/'state.private.json'
def docker(*args,timeout=120,check=True):
 r=subprocess.run(['docker',*map(str,args)],capture_output=True,text=True,encoding='utf-8',errors='replace',timeout=timeout)
 if check and r.returncode:raise RuntimeError('Owned migration probe failed; credentials suppressed')
 return r
def owned(s):
 assert s['name'].startswith('itls-r43-probe-')
 assert docker('inspect','--format','{{index .Config.Labels "itls.quality.run"}}',s['name']).stdout.strip()=='R43-MIGRATION'
def sql(s,q):
 owned(s);return docker('exec',s['name'],'sh','-c','MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --default-character-set=utf8mb4 -uroot -N -B -e "$1"','sh',q).stdout.strip()
def loadsql(s,name,text):
 p=local/name;p.write_text(text,encoding='utf-8',newline='\n');docker('cp',p,s['name']+':/tmp/'+name)
 docker('exec',s['name'],'sh','-c','MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --default-character-set=utf8mb4 -uroot < "$1"','sh','/tmp/'+name)
def cli(s,db,action,jar,target=None):
 env=local/'cli.private.env';env.write_text('\n'.join(['DB_HOST='+s['name'],'DB_PORT=3306','DB_NAME='+db,'DB_USER=root','DB_PWD='+s['password']])+'\n',encoding='utf-8')
 args=['exec','--env-file',env,a.backend,'java','-Dloader.main=com.ls.aegis.boot.tools.R43MigrationCli','-cp',jar,'org.springframework.boot.loader.launch.PropertiesLauncher',action,'--target',target or s['name']+':3306/'+db]
 if action=='apply':args.append('--acknowledge-write')
 return docker(*args,check=False)
p=argparse.ArgumentParser();p.add_argument('phase',choices=['prepare','verify','cleanup']);p.add_argument('--jar',default='/app/app.jar');p.add_argument('--backend');p.add_argument('--network');a=p.parse_args()
if a.phase=='prepare' and not a.network:p.error('prepare requires an explicit disposable --network')
if a.phase=='verify' and not a.backend:p.error('verify requires an explicit disposable --backend runner')
if a.phase=='prepare':
 assert not statefile.exists(),'Do not overwrite an owned probe state'
 s={'name':'itls-r43-probe-'+secrets.token_hex(4),'password':secrets.token_urlsafe(24)};statefile.write_text(json.dumps(s),encoding='utf-8')
 env=local/'mysql.private.env';env.write_text('MYSQL_ROOT_PASSWORD='+s['password']+'\nMYSQL_DATABASE=itls_r43_probe\n',encoding='utf-8')
 docker('run','-d','--pull','never','--name',s['name'],'--network',a.network,'--label','itls.quality.run=R43-MIGRATION','--env-file',env,'mysql:8.0.42','--character-set-server=utf8mb4','--collation-server=utf8mb4_general_ci')
 for _ in range(60):
  r=docker('exec',s['name'],'sh','-c','MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --protocol=TCP -h127.0.0.1 -uroot -N -B -e "SELECT 1"',check=False)
  if r.returncode==0:break
  time.sleep(1)
 else:raise RuntimeError('Owned probe readiness not confirmed')
 source=(root/'ls-aegis-server/sql/init.sql').read_text(encoding='utf-8');table=re.search(r'CREATE TABLE IF NOT EXISTS `eoms_contract` \(.*?\) ENGINE=.*?;',source,re.S).group(0)
 legacy=re.sub(r'^\s*sign_date\s+datetime[^\n]*\n','',table,flags=re.M)
 loadsql(s,'probe.sql','USE itls_r43_probe;\n'+legacy+"\nINSERT INTO eoms_contract(contract_no,customer_id,contract_name,amount) VALUES('SYNTHETIC-LEGACY',1,'ITLS TEST ONLY',50);\nCREATE DATABASE itls_r43_existing;USE itls_r43_existing;\n"+table+'\nCREATE DATABASE itls_r43_conflict;USE itls_r43_conflict;CREATE TABLE eoms_contract(id BIGINT PRIMARY KEY,sign_date DATE) ENGINE=InnoDB;\n')
 print('R43 owned migration probe ready; no host ports published')
elif a.phase=='verify':
 s=json.loads(statefile.read_text());owned(s);proof={'cases':[],'noHostPortPublished':True}
 assert sql(s,"SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='itls_r43_probe' AND LOWER(table_name)='databasechangelog'")=='0'
 for action in ['validate','status']:
  r=cli(s,'itls_r43_probe',action,a.jar);assert r.returncode==0 and 'pending=1' in r.stdout
 assert sql(s,"SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='itls_r43_probe' AND LOWER(table_name)='databasechangelog'")=='0';proof['cases'].append('validate/status do not initialize history or schema')
 r=cli(s,'itls_r43_probe','apply',a.jar);assert r.returncode==0 and 'pendingAfter=0' in r.stdout;proof['cases'].append('apply absent nullable datetime column')
 assert sql(s,'SELECT COUNT(*) FROM itls_r43_probe.eoms_contract WHERE sign_date IS NULL AND amount=50 AND contract_no=\'SYNTHETIC-LEGACY\'')=='1';proof['cases'].append('legacy row unchanged, signing date remains NULL')
 r=cli(s,'itls_r43_probe','apply',a.jar);assert r.returncode==0;assert sql(s,'SELECT COUNT(*) FROM itls_r43_probe.DATABASECHANGELOG')=='1';proof['cases'].append('repeat apply has one history entry')
 r=cli(s,'itls_r43_probe','status',a.jar,target=s['name']+':3306/wrong_db');assert r.returncode!=0 and 'TARGET_MISMATCH' in r.stderr;proof['cases'].append('explicit target mismatch rejected')
 r=cli(s,'itls_r43_conflict','apply',a.jar);assert r.returncode!=0;assert sql(s,"SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='itls_r43_conflict' AND LOWER(table_name)='databasechangelog'")=='0';proof['cases'].append('conflicting existing date type rejected before history initialization')
 r=cli(s,'itls_r43_existing','apply',a.jar);assert r.returncode==0;assert sql(s,'SELECT EXECTYPE FROM itls_r43_existing.DATABASECHANGELOG')=='MARK_RAN';proof['cases'].append('exact pre-existing install column marked ran')
 proof['status']='PASS';(local/'evidence.json').write_text(json.dumps(proof,indent=2),encoding='utf-8');print(json.dumps(proof))
else:
 s=json.loads(statefile.read_text());owned(s);docker('rm','-f','-v',s['name'])
 for f in [statefile,local/'mysql.private.env',local/'cli.private.env']:
  if f.exists():f.unlink()
 (local/'cleanup.json').write_text(json.dumps({'containerRemoved':True,'ownAnonymousVolumesRemoved':True,'privateFilesRemoved':True}),encoding='utf-8');print('Owned R43 migration probe removed; no in-use container or volume touched')
