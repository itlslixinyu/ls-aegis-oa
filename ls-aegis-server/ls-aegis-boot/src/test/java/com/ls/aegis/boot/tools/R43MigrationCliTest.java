package com.ls.aegis.boot.tools;

import liquibase.Liquibase;
import liquibase.Scope;
import liquibase.change.core.SQLFileChange;
import liquibase.database.OfflineConnection;
import liquibase.database.core.MySQLDatabase;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.sql.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class R43MigrationCliTest {
    Connection column(String mismatch,boolean present) throws Exception {
        var c=mock(Connection.class);var s=mock(Statement.class);var r=mock(ResultSet.class);
        when(c.createStatement()).thenReturn(s);when(s.executeQuery(anyString())).thenReturn(r);when(r.next()).thenReturn(present,false);
        if(present){when(r.getString("DATA_TYPE")).thenReturn(mismatch.equals("type")?"date":"datetime");when(r.getInt("DATETIME_PRECISION")).thenReturn(mismatch.equals("precision")?6:0);when(r.getString("IS_NULLABLE")).thenReturn(mismatch.equals("nullable")?"NO":"YES");when(r.getObject("COLUMN_DEFAULT")).thenReturn(mismatch.equals("default")?"CURRENT_TIMESTAMP":null);when(r.getString("EXTRA")).thenReturn(mismatch.equals("extra")?"DEFAULT_GENERATED":"");when(r.getString("GENERATION_EXPRESSION")).thenReturn("");}
        return c;
    }
    @Test void packagedResourcesHaveFixedDigests() throws Exception {R43MigrationCli.verifyReleaseResources();}
    @Test void absentColumnIsPendingAndExactExistingDefinitionIsAccepted() throws Exception {assertFalse(R43MigrationCli.verifyColumnDefinition(column("",false)));assertTrue(R43MigrationCli.verifyColumnDefinition(column("",true)));}
    @ParameterizedTest @ValueSource(strings={"type","precision","nullable","default","extra"})
    void conflictingExistingColumnsAreRejected(String mismatch) throws Exception {assertThrows(R43MigrationCli.Rejected.class,()->R43MigrationCli.verifyColumnDefinition(column(mismatch,true)));}
    @Test void releaseContainsOnlyTheSingleReviewedSqlFile() throws Exception {
        try(var resources=new ClassLoaderResourceAccessor();var db=new MySQLDatabase()){
            db.setConnection(new OfflineConnection("offline:mysql?version=8.0.42",resources));
            try(var migration=new Liquibase(R43MigrationCli.RELEASE,resources,db)){
                Scope.child(Map.of("database",db,"resourceAccessor",resources),()->{var changes=migration.getDatabaseChangeLog().getChangeSets();assertEquals(1,changes.size());assertEquals("r43_eoms_contract_sign_date_mysql",changes.get(0).getId());assertEquals(1,changes.get(0).getChanges().size());assertEquals(R43MigrationCli.PATCH,((SQLFileChange)changes.get(0).getChanges().get(0)).getPath());});
            }
        }
    }
}
