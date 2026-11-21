package com.ls.aegis.boot.tools;

import liquibase.Liquibase;
import liquibase.Scope;
import liquibase.database.OfflineConnection;
import liquibase.database.core.MySQLDatabase;
import liquibase.database.core.PostgresDatabase;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** No database access: actual Liquibase resource parsing plus defensive JDBC metadata/history boundaries. */
class R31MigrationCliTest {
    static List<Map<String,Object>> correctIndex(){
        var rows=new ArrayList<Map<String,Object>>();var columns=List.of("tenant_id","deleted","goods_id","warehouse_id","remain_quantity");
        for(int i=0;i<columns.size();i++){var row=new HashMap<String,Object>();row.put("COLUMN_NAME",columns.get(i));row.put("SEQ_IN_INDEX",i+1);row.put("NON_UNIQUE",1);row.put("INDEX_TYPE","BTREE");row.put("SUB_PART",null);row.put("COLLATION","A");row.put("IS_VISIBLE","YES");rows.add(row);}return rows;
    }
    static Stream<List<Map<String,Object>>> invalidIndex(){
        var cases=new ArrayList<List<Map<String,Object>>>();
        for(var change:Map.of("COLUMN_NAME","goods_id","SEQ_IN_INDEX",3,"NON_UNIQUE",0,"INDEX_TYPE","HASH","SUB_PART",4,"COLLATION","D","IS_VISIBLE","NO").entrySet()){
            var rows=correctIndex();rows.get(0).put(change.getKey(),change.getValue());cases.add(rows);
        }
        var shortIndex=correctIndex();shortIndex.remove(4);cases.add(shortIndex);var longer=correctIndex();longer.add(new HashMap<>(longer.get(4)));cases.add(longer);return cases.stream();
    }
    ResultSet rows(List<Map<String,Object>> data)throws Exception{
        var result=mock(ResultSet.class);var at=new AtomicInteger(-1);when(result.next()).thenAnswer(inv->at.incrementAndGet()<data.size());
        when(result.getString(anyString())).thenAnswer(inv->{Object value=data.get(at.get()).get(inv.getArgument(0));return value==null?null:value.toString();});
        when(result.getInt(anyString())).thenAnswer(inv->((Number)data.get(at.get()).get(inv.getArgument(0))).intValue());
        when(result.getObject(anyString())).thenAnswer(inv->data.get(at.get()).get(inv.getArgument(0)));return result;
    }
    Connection connection(List<Map<String,Object>> index,String checksum)throws Exception{
        var connection=mock(Connection.class);var tables=mock(Statement.class);var count=mock(ResultSet.class);when(count.next()).thenReturn(true);when(count.getInt(1)).thenReturn(checksum==null?0:1);when(tables.executeQuery(anyString())).thenReturn(count);when(connection.createStatement()).thenReturn(tables);
        when(connection.prepareStatement(anyString())).thenAnswer(inv->{var query=mock(PreparedStatement.class);String sql=inv.getArgument(0);
            ResultSet data=sql.contains("information_schema.statistics") ? rows(index) : rows(checksum==null?List.of():List.of(Map.of("ID","r31_stock_alert_balance_index_mysql","AUTHOR","ls","MD5SUM",checksum)));
            when(query.executeQuery()).thenReturn(data);return query;});return connection;
    }
    @Test void fixedReleaseResourcesArePinnedAndParseAsExactlyOneMySqlChange()throws Exception{
        R31MigrationCli.verifyReleaseResources();
        try(var resources=new ClassLoaderResourceAccessor();var database=new MySQLDatabase()){
            database.setConnection(new OfflineConnection("offline:mysql?version=8.0.42",resources));
            try(var migration=new Liquibase(R31MigrationCli.RELEASE,resources,database)){
                var log=migration.getDatabaseChangeLog();assertEquals(1,log.getChangeSets().size());var change=log.getChangeSets().get(0);
                assertEquals("r31_stock_alert_balance_index_mysql",change.getId());assertEquals("ls",change.getAuthor());assertEquals(R31MigrationCli.RELEASE,change.getFilePath());assertNotNull(log.getPreconditions());assertNotNull(change.getPreconditions());
            }
        }
    }
    @Test void postgresReleaseIsOneConcurrentIndexChangeOutsideATransaction()throws Exception{
        try(var resources=new ClassLoaderResourceAccessor();var database=new PostgresDatabase()){
            database.setConnection(new OfflineConnection("offline:postgresql?version=15",resources));
            try(var migration=new Liquibase("db/changelog/releases/r31-postgresql.yaml",resources,database)){
                var changes=migration.getDatabaseChangeLog().getChangeSets();assertEquals(1,changes.size());assertFalse(changes.get(0).isRunInTransaction());assertEquals("r31_stock_alert_balance_index_postgresql",changes.get(0).getId());
            }
        }
    }
    @Test void fullMasterIncludesTheOneIndexAndRetainsItsHaltingDefinitionPrecondition()throws Exception{
        try(var resources=new ClassLoaderResourceAccessor();var database=new MySQLDatabase()){
            database.setConnection(new OfflineConnection("offline:mysql?version=8.0.42",resources));
            try(var migration=new Liquibase("db/changelog/db.changelog-master.yaml",resources,database)){
                var log=migration.getDatabaseChangeLog();assertEquals(1,log.getChangeSets().stream().filter(c->c.getId().equals("r31_stock_alert_balance_index_mysql")).count());
                assertTrue(preconditions(log.getPreconditions()).stream().filter(liquibase.precondition.core.SqlPrecondition.class::isInstance)
                    .map(liquibase.precondition.core.SqlPrecondition.class::cast).anyMatch(p->p.getSql().contains("idx_stock_alert_balance")&&p.getExpectedResult().equals("1")));
            }
        }
    }
    List<liquibase.precondition.Precondition> preconditions(liquibase.precondition.Precondition condition){
        var all=new ArrayList<liquibase.precondition.Precondition>();if(condition==null)return all;all.add(condition);
        if(condition instanceof liquibase.precondition.PreconditionLogic logic)for(var nested:logic.getNestedPreconditions())all.addAll(preconditions(nested));return all;
    }
    @Test void absentAndExactlyMatchingIndexAreDistinguishedWithoutWriting()throws Exception{assertFalse(R31MigrationCli.verifyIndexDefinition(connection(List.of(),null)));assertTrue(R31MigrationCli.verifyIndexDefinition(connection(correctIndex(),null)));}
    @ParameterizedTest @MethodSource("invalidIndex") void sameNameWithAnyDifferentDefinitionIsRejectedRatherThanTreatedAsExisting(List<Map<String,Object>> rows)throws Exception{var rejection=assertThrows(R31MigrationCli.Rejected.class,()->R31MigrationCli.verifyIndexDefinition(connection(rows,null)));assertEquals("INDEX_DEFINITION_MISMATCH",rejection.getMessage());}
    @ParameterizedTest @ValueSource(strings={"validate","status"}) void readOnlyStatusWithNoHistoryDoesNotInitializeLiquibaseHistoryOrRunDdl(String action)throws Exception{
        var connection=connection(List.of(),null);
        try(var resources=new ClassLoaderResourceAccessor();var database=new MySQLDatabase()){
            database.setConnection(new OfflineConnection("offline:mysql?version=8.0.42",resources));
            try(var migration=new Liquibase(R31MigrationCli.RELEASE,resources,database)){
                Scope.child(Map.of("database",database,"resourceAccessor",resources),()->R31MigrationCli.execute(new R29MigrationCli.Options(action,"db:3306/test"),new R29MigrationCli.Target("db",3306,"test","synthetic","not-used",false),connection,database,migration));
            }
        }
        verify(connection,never()).commit();verify(connection,never()).prepareStatement(argThat(sql->!sql.startsWith("SELECT")));
    }
    @Test void recordedHistoryWithAMissingIndexIsSchemaDriftNotAnAutomaticRebuild()throws Exception{
        try(var resources=new ClassLoaderResourceAccessor();var database=new MySQLDatabase()){
            database.setConnection(new OfflineConnection("offline:mysql?version=8.0.42",resources));
            try(var migration=new Liquibase(R31MigrationCli.RELEASE,resources,database)){
                Scope.child(Map.of("database",database,"resourceAccessor",resources),()->{
                    String checksum=migration.getDatabaseChangeLog().getChangeSets().get(0).generateCheckSum(liquibase.ChecksumVersion.latest()).toString();
                    var connection=connection(List.of(),checksum);
                    var rejected=assertThrows(R31MigrationCli.Rejected.class,()->R31MigrationCli.execute(new R29MigrationCli.Options("apply","db:3306/test"),new R29MigrationCli.Target("db",3306,"test","synthetic","not-used",false),connection,database,migration));
                    assertEquals("RECORDED_INDEX_MISSING",rejected.getMessage());
                });
            }
        }
    }
    @Test void recordedWrongChecksumIsRejectedBeforeAnApplyCanTouchTheIndex()throws Exception{
        var connection=connection(correctIndex(),"9:00000000000000000000000000000000");
        try(var resources=new ClassLoaderResourceAccessor();var database=new MySQLDatabase()){
            database.setConnection(new OfflineConnection("offline:mysql?version=8.0.42",resources));
            try(var migration=new Liquibase(R31MigrationCli.RELEASE,resources,database)){
                var rejected=assertThrows(Exception.class,()->Scope.child(Map.of("database",database,"resourceAccessor",resources),()->R31MigrationCli.execute(new R29MigrationCli.Options("apply","db:3306/test"),new R29MigrationCli.Target("db",3306,"test","synthetic","not-used",false),connection,database,migration)));
                assertTrue(String.valueOf(rejected.getMessage()).contains("SCOPED_CHECKSUM_MISMATCH"));
            }
        }
        verify(connection,never()).prepareStatement(contains("information_schema.statistics"));
    }
    @Test void sharedParserRequiresExplicitTargetAndWriteAcknowledgementOnlyForApply()throws Exception{
        assertThrows(R29MigrationCli.Rejected.class,()->R29MigrationCli.Options.parse(new String[]{"apply","--target","db:3306/test"}));
        assertThrows(R29MigrationCli.Rejected.class,()->R29MigrationCli.Options.parse(new String[]{"status","--target","db:3306/test","--acknowledge-write"}));
        var options=R29MigrationCli.Options.parse(new String[]{"apply","--target","db:3306/test","--acknowledge-write"});assertEquals("apply",options.action());assertEquals("db:3306/test",options.target());
    }
}
