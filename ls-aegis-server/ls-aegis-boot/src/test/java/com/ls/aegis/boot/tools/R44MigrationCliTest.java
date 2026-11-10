package com.ls.aegis.boot.tools;

import liquibase.Liquibase;
import liquibase.Scope;
import liquibase.change.core.SQLFileChange;
import liquibase.database.OfflineConnection;
import liquibase.database.core.MySQLDatabase;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import java.sql.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class R44MigrationCliTest {
    Connection state(boolean tableExists, boolean hasRows, boolean customMenu, boolean wrongIdentity) throws Exception {
        var c = mock(Connection.class); var p = mock(PreparedStatement.class); var existence = mock(ResultSet.class);
        when(c.prepareStatement(anyString())).thenReturn(p); when(p.executeQuery()).thenReturn(existence);
        when(existence.next()).thenReturn(true); when(existence.getInt(1)).thenReturn(tableExists ? 1 : 0);
        var statement = mock(Statement.class); when(c.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenAnswer(i -> {
            String sql = i.getArgument(0); var row = mock(ResultSet.class); when(row.next()).thenReturn(true);
            when(row.getLong(1)).thenReturn(hasRows ? 1L : 0L);
            when(row.getInt(1)).thenReturn((sql.contains("AND id NOT IN") && customMenu) || (sql.contains("AND NOT ((id=") && wrongIdentity) ? 1 : 0);
            return row;
        }); return c;
    }
    @Test void resourcesAreFixedAndReleaseCannotIncludeTheMaster() throws Exception {
        R44MigrationCli.verifyReleaseResources();
        try (var resources = new ClassLoaderResourceAccessor(); var db = new MySQLDatabase()) {
            db.setConnection(new OfflineConnection("offline:mysql?version=8.0.42", resources));
            try (var migration = new Liquibase(R44MigrationCli.RELEASE, resources, db)) {
                Scope.child(Map.of("database", db, "resourceAccessor", resources), () -> {
                    var changes = migration.getDatabaseChangeLog().getChangeSets(); assertEquals(1, changes.size());
                    assertEquals("r44_retire_generator_open_mysql", changes.get(0).getId());
                    assertEquals(1, changes.get(0).getChanges().size());
                    assertEquals(R44MigrationCli.PATCH, ((SQLFileChange) changes.get(0).getChanges().get(0)).getPath());
                });
            }
        }
    }
    @Test void cleanRetiredSchemaIsAccepted() throws Exception { assertTrue(R44MigrationCli.verifyRemovalReady(state(false, false, false, false))); }
    @Test void existingEmptyTablesStillNeedRetirement() throws Exception { assertFalse(R44MigrationCli.verifyRemovalReady(state(true, false, false, false))); }
    @Test void nonemptyStorageMustNotBeDropped() throws Exception { assertThrows(R44MigrationCli.Rejected.class, () -> R44MigrationCli.verifyRemovalReady(state(true, true, false, false))); }
    @Test void customizedDescendantsMustNotBeOrphaned() throws Exception { assertThrows(R44MigrationCli.Rejected.class, () -> R44MigrationCli.verifyRemovalReady(state(false, false, true, false))); }
    @Test void repurposedMenuIdsMustNotBeRemoved() throws Exception { assertThrows(R44MigrationCli.Rejected.class, () -> R44MigrationCli.verifyRemovalReady(state(false, false, false, true))); }
}
