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
class R32MigrationCliTest {
    static List<Map<String,Object>> correctColumns(){
        var rows=new ArrayList<Map<String,Object>>();
        for(String column:List.of("tenant_id","create_user","dept_id")){
            var row=new HashMap<String,Object>();row.put("COLUMN_NAME",column);row.put("DATA_TYPE","bigint");row.put("COLUMN_TYPE","bigint");row.put("IS_NULLABLE","YES");row.put("COLUMN_DEFAULT",null);row.put("EXTRA","");row.put("GENERATION_EXPRESSION","");rows.add(row);
        }
        return rows;
    }
    static Stream<List<Map<String,Object>>> invalidColumns(){
        var cases=new ArrayList<List<Map<String,Object>>>();
        for(var change:Map.of("COLUMN_NAME","unrecognized","DATA_TYPE","int","COLUMN_TYPE","bigint unsigned","IS_NULLABLE","NO","COLUMN_DEFAULT","0","EXTRA","auto_increment","GENERATION_EXPRESSION","1+1").entrySet()){
            var rows=correctColumns();rows.get(0).put(change.getKey(),change.getValue());cases.add(rows);
        }
        for(String type:List.of("int","bigint unsigned zerofill")){var rows=correctColumns();rows.get(0).put("COLUMN_TYPE",type);cases.add(rows);}
        var partial=correctColumns();partial.remove(2);cases.add(partial);var one=correctColumns().subList(0,1);cases.add(one);
        var duplicate=correctColumns();duplicate.add(new HashMap<>(duplicate.get(0)));cases.add(duplicate);return cases.stream();
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
            ResultSet data=sql.contains("information_schema.columns") ? rows(index) : rows(checksum==null?List.of():List.of(Map.of("ID","r32_quote_data_scope_mysql","AUTHOR","ls","MD5SUM",checksum)));
            when(query.executeQuery()).thenReturn(data);return query;});return connection;
    }
    @Test void fixedReleaseResourcesArePinnedAndParseAsExactlyOneMySqlChange()throws Exception{
        R32MigrationCli.verifyReleaseResources();
        try(var resources=new ClassLoaderResourceAccessor();var database=new MySQLDatabase()){
            database.setConnection(new OfflineConnection("offline:mysql?version=8.0.42",resources));
            try(var migration=new Liquibase(R32MigrationCli.RELEASE,resources,database)){
                var log=migration.getDatabaseChangeLog();assertEquals(1,log.getChangeSets().size());var change=log.getChangeSets().get(0);
                assertEquals("r32_quote_data_scope_mysql",change.getId());assertEquals("ls",change.getAuthor());assertEquals(R32MigrationCli.RELEASE,change.getFilePath());assertNotNull(log.getPreconditions());assertNotNull(change.getPreconditions());
            }
        }
    }
    List<liquibase.precondition.Precondition> preconditions(liquibase.precondition.Precondition condition){
        var all=new ArrayList<liquibase.precondition.Precondition>();if(condition==null)return all;all.add(condition);
        if(condition instanceof liquibase.precondition.PreconditionLogic logic)for(var nested:logic.getNestedPreconditions())all.addAll(preconditions(nested));return all;
    }
    @Test void absentAndExactlyMatchingIndexAreDistinguishedWithoutWriting()throws Exception{assertFalse(R32MigrationCli.verifyColumnDefinition(connection(List.of(),null)));assertTrue(R32MigrationCli.verifyColumnDefinition(connection(correctColumns(),null)));}
    @ParameterizedTest @MethodSource("invalidColumns") void partialUnsignedNonNullableDefaultedGeneratedOrWrongColumnsFailClosed(List<Map<String,Object>> rows)throws Exception{var rejection=assertThrows(R32MigrationCli.Rejected.class,()->R32MigrationCli.verifyColumnDefinition(connection(rows,null)));assertTrue(rejection.getMessage().endsWith("MISMATCH")||rejection.getMessage().startsWith("PARTIAL"));}
    @ParameterizedTest @ValueSource(strings={"validate","status"}) void readOnlyStatusWithNoHistoryDoesNotInitializeLiquibaseHistoryOrRunDdl(String action)throws Exception{
        var connection=connection(List.of(),null);
        try(var resources=new ClassLoaderResourceAccessor();var database=new MySQLDatabase()){
            database.setConnection(new OfflineConnection("offline:mysql?version=8.0.42",resources));
            try(var migration=new Liquibase(R32MigrationCli.RELEASE,resources,database)){
                Scope.child(Map.of("database",database,"resourceAccessor",resources),()->R32MigrationCli.execute(new R29MigrationCli.Options(action,"db:3306/test"),new R29MigrationCli.Target("db",3306,"test","synthetic","not-used",false),connection,database,migration));
            }
        }
        verify(connection,never()).commit();verify(connection,never()).prepareStatement(argThat(sql->!sql.startsWith("SELECT")));
    }
    @Test void recordedHistoryWithMissingColumnsIsDriftNotAutomaticRebuild()throws Exception{
        try(var resources=new ClassLoaderResourceAccessor();var database=new MySQLDatabase()){
            database.setConnection(new OfflineConnection("offline:mysql?version=8.0.42",resources));
            try(var migration=new Liquibase(R32MigrationCli.RELEASE,resources,database)){
                Scope.child(Map.of("database",database,"resourceAccessor",resources),()->{
                    String checksum=migration.getDatabaseChangeLog().getChangeSets().get(0).generateCheckSum(liquibase.ChecksumVersion.latest()).toString();
                    var connection=connection(List.of(),checksum);
                    var rejected=assertThrows(R32MigrationCli.Rejected.class,()->R32MigrationCli.execute(new R29MigrationCli.Options("apply","db:3306/test"),new R29MigrationCli.Target("db",3306,"test","synthetic","not-used",false),connection,database,migration));
                    assertEquals("RECORDED_COLUMNS_MISSING",rejected.getMessage());
                });
            }
        }
    }
    @Test void recordedWrongChecksumIsRejectedBeforeAnApplyCanTouchTheIndex()throws Exception{
        var connection=connection(correctColumns(),"9:00000000000000000000000000000000");
        try(var resources=new ClassLoaderResourceAccessor();var database=new MySQLDatabase()){
            database.setConnection(new OfflineConnection("offline:mysql?version=8.0.42",resources));
            try(var migration=new Liquibase(R32MigrationCli.RELEASE,resources,database)){
                var rejected=assertThrows(Exception.class,()->Scope.child(Map.of("database",database,"resourceAccessor",resources),()->R32MigrationCli.execute(new R29MigrationCli.Options("apply","db:3306/test"),new R29MigrationCli.Target("db",3306,"test","synthetic","not-used",false),connection,database,migration)));
                assertTrue(String.valueOf(rejected.getMessage()).contains("SCOPED_CHECKSUM_MISMATCH"));
            }
        }
        verify(connection,never()).prepareStatement(contains("information_schema.columns"));
    }
    @Test void sharedParserRequiresExplicitTargetAndWriteAcknowledgementOnlyForApply()throws Exception{
        assertThrows(R29MigrationCli.Rejected.class,()->R29MigrationCli.Options.parse(new String[]{"apply","--target","db:3306/test"}));
        assertThrows(R29MigrationCli.Rejected.class,()->R29MigrationCli.Options.parse(new String[]{"status","--target","db:3306/test","--acknowledge-write"}));
        var options=R29MigrationCli.Options.parse(new String[]{"apply","--target","db:3306/test","--acknowledge-write"});assertEquals("apply",options.action());assertEquals("db:3306/test",options.target());
    }
    @Test void signedBigintDisplayWidthIsCompatibleAndColumnOrderDoesNotAssignOwnership() throws Exception {
        var rows=correctColumns();rows.get(0).put("COLUMN_TYPE","bigint(20)");Collections.reverse(rows);
        assertTrue(R32MigrationCli.verifyColumnDefinition(connection(rows,null)));
    }
    @Test void existingHistoryCannotHaveMissingChecksum() throws Exception {
        var connection=mock(Connection.class);var statement=mock(Statement.class);var count=mock(ResultSet.class);when(count.next()).thenReturn(true);when(count.getInt(1)).thenReturn(1);when(statement.executeQuery(anyString())).thenReturn(count);when(connection.createStatement()).thenReturn(statement);
        var query=mock(PreparedStatement.class);when(connection.prepareStatement(anyString())).thenReturn(query);var data=new HashMap<String,Object>();data.put("ID","r32_quote_data_scope_mysql");data.put("AUTHOR","ls");data.put("MD5SUM",null);var historyRows=rows(List.of(data));when(query.executeQuery()).thenReturn(historyRows);
        assertThrows(R32MigrationCli.Rejected.class,()->R32MigrationCli.readHistory(connection));
    }

    @ParameterizedTest @ValueSource(strings={"id","author","duplicate"})
    void wrongOrDuplicatedScopedHistoryIdentityIsRejected(String variant)throws Exception{
        var c=connection(correctColumns(),"9:11111111111111111111111111111111");
        var row=new HashMap<String,Object>();row.put("ID",variant.equals("id")?"unexpected":"r32_quote_data_scope_mysql");row.put("AUTHOR",variant.equals("author")?"unexpected":"ls");row.put("MD5SUM","9:11111111111111111111111111111111");
        var data=rows(variant.equals("duplicate")?List.of(row,new HashMap<>(row)):List.of(row));var query=mock(PreparedStatement.class);when(query.executeQuery()).thenReturn(data);when(c.prepareStatement(contains("FROM DATABASECHANGELOG"))).thenReturn(query);
        assertThrows(R32MigrationCli.Rejected.class,()->R32MigrationCli.readHistory(c));
    }

}
