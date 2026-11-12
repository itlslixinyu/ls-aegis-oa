"""Additive local Compose/isolated-fixture upgrade; dry-run unless --apply. Never initializes a database."""
import argparse,json,re,subprocess,tempfile,uuid
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--container',default='ls-aegis-mysql')
    parser.add_argument('--database',default='ls_aegis')
    parser.add_argument('--apply',action='store_true')
    parser.add_argument('--report',type=Path,required=True)
    args=parser.parse_args()
    if not re.fullmatch(r'ls_aegis(?:_upgrade_[a-f0-9]{10})?',args.database):raise RuntimeError('Unsupported database name')
    def call(command,timeout=90):
        result=subprocess.run(command,capture_output=True,timeout=timeout)
        if result.returncode:raise RuntimeError(result.stderr.decode('utf-8',errors='replace')[-800:])
        return result.stdout.decode('utf-8',errors='replace').strip()
    info=json.loads(call(['docker','inspect',args.container]))[0]
    labels=info['Config'].get('Labels') or {}
    fixture=labels.get('itls.quality.run','')
    if not ((labels.get('com.docker.compose.project')=='ls-aegis' and labels.get('com.docker.compose.service')=='ls-aegis-mysql') or re.fullmatch('[a-f0-9]{10}',fixture)):
        raise RuntimeError('Container is not the project database or owned fixture')
    if args.database!='ls_aegis' and not fixture:raise RuntimeError('Auxiliary database only allowed in fixture')
    args.report=args.report.resolve();private=ROOT/'.local'
    if not args.report.is_relative_to(private):raise RuntimeError('Report must stay under project .local')
    args.report.parent.mkdir(parents=True,exist_ok=True)
    remote='/tmp/oa-service-upgrade-'+uuid.uuid4().hex+'.sql'
    def sql(query):
        # UTF-8 file transfer avoids PowerShell codepage corruption and never emits credentials.
        with tempfile.TemporaryDirectory(prefix='oa-service-upgrade-') as directory:
            source=Path(directory)/'query.sql';source.write_text(query,encoding='utf-8')
            call(['docker','cp',str(source),args.container+':'+remote])
            try:return call(['docker','exec',args.container,'sh','-c','MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --default-character-set=utf8mb4 -uroot -N -B '+args.database+' < '+remote])
            finally:call(['docker','exec',args.container,'rm',remote])
    assert sql("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='eoms_repair_apply'")=='1','Existing application table required'
    before=int(sql('SELECT COUNT(*) FROM eoms_repair_apply'))
    column=sql("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='eoms_repair_apply' AND column_name='report_address'")=='1'
    index=sql("SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='eoms_repair_apply' AND index_name='idx_apply_pool'")!='0'
    migration=ROOT/'ls-aegis-server/ls-aegis-boot/src/main/resources/db/changelog/mysql/patch_oa_service_pool.sql'
    statements=[statement.strip()+';' for statement in re.sub(r'^--.*$','',migration.read_text(encoding='utf-8'),flags=re.M).split(';') if statement.strip()]
    assert len(statements)==2 and statements[0].startswith('ALTER TABLE eoms_repair_apply ADD COLUMN report_address ') and statements[1].startswith('CREATE INDEX idx_apply_pool ON eoms_repair_apply(')
    actions=[]
    if not column:actions.append(statements[0])
    if not index:actions.append(statements[1])
    if args.apply:
        for statement in actions:sql(statement)
        assert sql("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='eoms_repair_apply' AND column_name='report_address'")=='1'
        assert sql("SELECT GROUP_CONCAT(column_name ORDER BY seq_in_index) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='eoms_repair_apply' AND index_name='idx_apply_pool'")=="tenant_id,status,apply_time,del_flag"
        assert sql("SELECT CONCAT(column_type,':',is_nullable) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='eoms_repair_apply' AND column_name='report_address'")=="varchar(500):YES"
    after=int(sql('SELECT COUNT(*) FROM eoms_repair_apply'))
    result={'status':'APPLIED' if args.apply else 'DRY_RUN','database':args.database,'existingRowsBefore':before,'existingRowsAfter':after,'neededStatements':len(actions),'businessDataWritten':False,'columnsDeleted':False,'existingAddressesBackfilled':False,'liquibaseHistoryModified':False}
    args.report.write_text(json.dumps(result,indent=2),encoding='utf-8');print(json.dumps(result))

if __name__=='__main__':main()
