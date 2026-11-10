"""Exercise the fixed R44 retired-feature release on an owned disposable MySQL, never the in-use DB."""
import argparse,hashlib,json,secrets,subprocess,time,re
from pathlib import Path
root=Path(__file__).resolve().parents[2];local=root/'.local/r44-migration-probe';local.mkdir(parents=True,exist_ok=True)
statefile=local/'state.private.json'
def docker(*args,timeout=120,check=True):
 r=subprocess.run(['docker',*map(str,args)],capture_output=True,text=True,encoding='utf-8',errors='replace',timeout=timeout)
 if check and r.returncode:raise RuntimeError('Owned migration probe failed; credentials suppressed')
 return r
def owned(s):
 assert s['name'].startswith('itls-r44-probe-')
 assert docker('inspect','--format','{{index .Config.Labels "itls.quality.run"}}',s['name']).stdout.strip()=='R44-MIGRATION'
def sql(s,q):
 owned(s);return docker('exec',s['name'],'sh','-c','MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --default-character-set=utf8mb4 -uroot -N -B -e "$1"','sh',q).stdout.strip()
def loadsql(s,name,text):
 p=local/name;p.write_text(text,encoding='utf-8',newline='\n');docker('cp',p,s['name']+':/tmp/'+name)
 docker('exec',s['name'],'sh','-c','MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --default-character-set=utf8mb4 -uroot < "$1"','sh','/tmp/'+name)
def cli(s,db,action,jar,target=None):
 env=local/'cli.private.env';env.write_text('\n'.join(['DB_HOST='+s['name'],'DB_PORT=3306','DB_NAME='+db,'DB_USER=root','DB_PWD='+s['password']])+'\n',encoding='utf-8')
 args=['exec','--env-file',env,a.backend,'java','-Dloader.main=com.ls.aegis.boot.tools.R44MigrationCli','-cp',jar,'org.springframework.boot.loader.launch.PropertiesLauncher',action,'--target',target or s['name']+':3306/'+db]
 if action=='apply':args.append('--acknowledge-write')
 return docker(*args,check=False)
p=argparse.ArgumentParser();p.add_argument('phase',choices=['prepare','verify','cleanup']);p.add_argument('--jar',default='/app/app.jar');p.add_argument('--backend');p.add_argument('--network');a=p.parse_args()
if a.phase=='prepare' and not a.network:p.error('prepare requires an explicit disposable --network')
if a.phase=='verify' and not a.backend:p.error('verify requires an explicit disposable --backend runner')
if a.phase=='prepare':
 assert not statefile.exists(),'Do not overwrite probe state'
 s={'name':'itls-r44-probe-'+secrets.token_hex(4),'password':secrets.token_urlsafe(24)};statefile.write_text(json.dumps(s))
 env=local/'mysql.private.env';env.write_text('MYSQL_ROOT_PASSWORD='+s['password']+'\nMYSQL_DATABASE=itls_r44_probe\n')
 docker('run','-d','--pull','never','--name',s['name'],'--network',a.network,'--label','itls.quality.run=R44-MIGRATION','--env-file',env,'mysql:8.0.42','--character-set-server=utf8mb4','--collation-server=utf8mb4_general_ci')
 for _ in range(60):
  r=docker('exec',s['name'],'sh','-c','MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --protocol=TCP -h127.0.0.1 -uroot -N -B -e "SELECT 1"',check=False)
  if r.returncode==0:break
  time.sleep(1)
 else:raise RuntimeError('Probe readiness timeout')
 base=(root/'ls-aegis-server/sql/init.sql').read_text(encoding='utf-8')
 old='\n'.join((root/'ls-aegis-server/ls-aegis-boot/src/main/resources/db/changelog/mysql/plugin'/f).read_text(encoding='utf-8') for f in ['plugin_open.sql','plugin_generator.sql'])
 text='CREATE DATABASE itls_r44_nonempty;CREATE DATABASE itls_r44_fresh;\n'
 for db in ['itls_r44_probe','itls_r44_nonempty','itls_r44_fresh']:
  text+='USE '+db+';\n'+base+'\n'
  if db!='itls_r44_fresh':text+=old+'\n'
 text+="USE itls_r44_nonempty;INSERT INTO gen_config(table_name,module_name,package_name,business_name,author,create_time) VALUES('synthetic','test','com.test','ITLS-TEST-R44','TEST',NOW());\n"
 text+="USE itls_r44_probe;INSERT INTO sys_role_menu(role_id,menu_id) VALUES(1,9010);INSERT INTO tenant_package_menu(package_id,menu_id) VALUES(1,7010);\n"
 loadsql(s,'seed.sql',text);print('Owned R44 probe ready, no published ports')
elif a.phase=='verify':
 s=json.loads(statefile.read_text());owned(s);proof={'cases':[],'noHostPortPublished':True};db='itls_r44_probe'
 def history(db):return sql(s,"SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='"+db+"' AND LOWER(table_name)='databasechangelog'")
 before=sql(s,'SELECT id,client_id,client_type,auth_type,timeout,active_timeout,status FROM '+db+'.sys_client ORDER BY id')
 for action in ['validate','status']:
  r=cli(s,db,action,a.jar);assert r.returncode==0 and 'pending=1' in r.stdout
 assert history(db)=='0';proof['cases'].append('validate/status have no history or schema side effects')
 r=cli(s,'itls_r44_nonempty','apply',a.jar);assert r.returncode!=0 and 'NONEMPTY_RETIRED_TABLE' in r.stderr
 assert history('itls_r44_nonempty')=='0' and sql(s,'SELECT COUNT(*) FROM itls_r44_nonempty.gen_config')=='1';proof['cases'].append('nonempty configuration preserved; migration refused before history')
 sql(s,"INSERT INTO "+db+".sys_menu(id,title,parent_id,type,path,name,component,sort,status,create_user,create_time) VALUES(99000,'ITLS-TEST-R44',7000,2,'/custom','R44Custom','custom',1,1,1,NOW())")
 r=cli(s,db,'apply',a.jar);assert r.returncode!=0 and 'CUSTOM_MENU_DESCENDANTS' in r.stderr and history(db)=='0';sql(s,'DELETE FROM '+db+'.sys_menu WHERE id=99000');proof['cases'].append('custom descendant preserved; migration refused')
 sql(s,"UPDATE "+db+".sys_menu SET path='/custom-root' WHERE id=7000")
 r=cli(s,db,'apply',a.jar);assert r.returncode!=0 and 'RETIRED_MENU_IDENTITY_CONFLICT' in r.stderr and history(db)=='0';sql(s,"UPDATE "+db+".sys_menu SET path='/open' WHERE id=7000");proof['cases'].append('repurposed menu identity refused')
 r=cli(s,db,'apply',a.jar);(local/'apply.log').write_text(r.stdout+'\n'+r.stderr);assert r.returncode==0 and 'pendingAfter=0' in r.stdout
 assert sql(s,"SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='"+db+"' AND table_name IN ('sys_app','gen_config','gen_field_config')")=='0';proof['cases'].append('three empty private tables dropped')
 for table in ['sys_menu','sys_role_menu','tenant_package_menu']:
  col='id' if table=='sys_menu' else 'menu_id';assert sql(s,'SELECT COUNT(*) FROM '+db+'.'+table+' WHERE '+col+' IN (7000,7010,7011,7012,7013,7014,7015,7016,7017,7018,9000,9010,9011,9012,9013,9014)')=='0'
 assert before==sql(s,'SELECT id,client_id,client_type,auth_type,timeout,active_timeout,status FROM '+db+'.sys_client ORDER BY id');proof['cases'].append('retired menu grants removed; login clients unchanged')
 r=cli(s,db,'apply',a.jar);assert r.returncode==0 and sql(s,'SELECT COUNT(*) FROM '+db+'.DATABASECHANGELOG')=='1';proof['cases'].append('repeat applies exactly once')
 r=cli(s,db,'status',a.jar,target=s['name']+':3306/wrong');assert r.returncode!=0 and 'TARGET_MISMATCH' in r.stderr;proof['cases'].append('wrong target refused')
 r=cli(s,'itls_r44_fresh','apply',a.jar);assert r.returncode==0;proof['cases'].append('fresh install with absent retired objects accepts release')
 proof['status']='PASS';(local/'evidence.json').write_text(json.dumps(proof,indent=2));print(json.dumps(proof))
else:
 s=json.loads(statefile.read_text());owned(s);docker('rm','-f','-v',s['name'])
 for f in [statefile,local/'mysql.private.env',local/'cli.private.env']:
  if f.exists():f.unlink()
 (local/'cleanup.json').write_text(json.dumps({'containerRemoved':True,'ownAnonymousVolumesRemoved':True,'privateFilesRemoved':True}));print('Owned R44 probe cleaned')
