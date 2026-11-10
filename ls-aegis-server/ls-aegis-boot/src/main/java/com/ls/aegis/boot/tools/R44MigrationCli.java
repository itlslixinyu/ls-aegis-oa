package com.ls.aegis.boot.tools;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;
import liquibase.*;
import liquibase.change.CheckSum;
import liquibase.change.core.SQLFileChange;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

/** Standalone retirement of empty generator/open feature storage. Reuses R29 explicit action/target parsing. */
public final class R44MigrationCli {
    static final String RELEASE = "db/changelog/releases/r44-mysql.yaml";
    static final String PATCH = "db/changelog/mysql/patch_retire_generator_open.sql";
    static final String CHANGE_ID = "r44_retire_generator_open_mysql";
    // Canonical UTF-8/LF inputs are immutable after this release is deployed.
    private static final Map<String,String> DIGESTS = Map.of(
        RELEASE, "40368810f1b2ea528e6aca50b36214e7c0d3b832ae07dedc96ef37a5cd9e3379",
        PATCH, "c66493a4b18a34d483a03d11d567659e6dbeae3381855c45ebb3efaac4178205"
    );

    private R44MigrationCli() { }

    public static void main(String[] args) {
        try {
            if (args.length == 1 && args[0].equals("--help")) { usage(); return; }
            R29MigrationCli.Options options = R29MigrationCli.Options.parse(args);
            R29MigrationCli.Target target = R29MigrationCli.Target.fromEnvironment(System.getenv());
            if (!target.identity().equals(options.target())) throw new Rejected("TARGET_MISMATCH");
            verifyReleaseResources();
            Properties credentials = new Properties();
            credentials.setProperty("user", target.user()); credentials.setProperty("password", target.password());
            try (Connection connection = DriverManager.getConnection(target.jdbcUrl(), credentials);
                 ClassLoaderResourceAccessor resources = new ClassLoaderResourceAccessor()) {
                connection.setReadOnly(!options.action().equals("apply"));
                verifyDatabase(connection, target.database());
                Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
                try (Liquibase migration = new Liquibase(RELEASE, resources, database)) {
                    Scope.child(Map.of("database",database,"resourceAccessor",resources), () -> execute(options,target,connection,database,migration));
                }
            }
        } catch (Rejected | R29MigrationCli.Rejected error) {
            System.err.println("R44_MIGRATION_REJECTED reason=" + error.getMessage()); usage(); System.exit(2);
        } catch (Throwable error) {
            // Driver messages/stack traces may contain connection credentials; deliberately omit them.
            System.err.println("R44_MIGRATION_FAILED type=" + error.getClass().getSimpleName()
                + "; inspect scoped retirement/history; no automatic cleanup, rollback, or baseline sync");
            System.exit(1);
        }
    }

    static void execute(R29MigrationCli.Options options, R29MigrationCli.Target target, Connection connection,
                        Database database, Liquibase migration) throws Exception {
        List<ChangeSet> changes = migration.getDatabaseChangeLog().getChangeSets();
        if (changes.size()!=1) throw new Rejected("UNEXPECTED_SCOPE_SIZE");
        ChangeSet change = changes.get(0);
        if (!CHANGE_ID.equals(change.getId()) || !"ls".equals(change.getAuthor()) || !RELEASE.equals(change.getFilePath())
                || change.getChanges().size()!=1 || !(change.getChanges().get(0) instanceof SQLFileChange sql)
                || !PATCH.equals(sql.getPath()) || sql.validate(database).hasErrors()) throw new Rejected("UNEXPECTED_CHANGESET_SCOPE");
        String previousChecksum = readHistory(connection);
        if (previousChecksum != null && !change.isCheckSumValid(CheckSum.parse(previousChecksum)))
            throw new Rejected("SCOPED_CHECKSUM_MISMATCH");
        boolean retired = verifyRemovalReady(connection);
        if (previousChecksum != null && !retired) throw new Rejected("RECORDED_RETIREMENT_DRIFT");
        int pending = previousChecksum == null ? 1 : 0;
        System.out.println("R44_"+options.action().toUpperCase(Locale.ROOT)+" target="+target.identity()
            +" scope=R44-MYSQL total=1 recorded="+(1-pending)+" pending="+pending+" retired="+retired);
        if (!options.action().equals("apply")) {
            System.out.println("R44_READ_ONLY_OK schema/history unchanged"); return;
        }
        migration.validate(); migration.update(new Contexts(),new LabelExpression());
        String checksum = readHistory(connection);
        if (checksum == null || !change.isCheckSumValid(CheckSum.parse(checksum)) || !verifyRemovalReady(connection))
            throw new Rejected("SCOPED_MIGRATION_INCOMPLETE");
        System.out.println("R44_APPLY_OK target="+target.identity()+" pendingBefore="+pending+" pendingAfter=0 retired=true");
    }

    /** Refuse nonempty feature storage or custom menu descendants; never print app credentials. */
    static boolean verifyRemovalReady(Connection connection) throws SQLException, Rejected {
        boolean retired = true;
        for (String table : List.of("sys_app", "gen_config", "gen_field_config")) {
            try (PreparedStatement query = connection.prepareStatement("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name=?")) {
                query.setString(1, table);
                try (ResultSet rows = query.executeQuery()) {
                    rows.next(); if (rows.getInt(1) == 0) continue;
                }
            }
            retired = false;
            try (Statement query = connection.createStatement(); ResultSet rows = query.executeQuery("SELECT COUNT(*) FROM " + table)) {
                rows.next(); if (rows.getLong(1) != 0) throw new Rejected("NONEMPTY_RETIRED_TABLE_REQUIRES_ARCHIVE");
            }
        }
        String ids = "7000,7010,7011,7012,7013,7014,7015,7016,7017,7018,9000,9010,9011,9012,9013,9014";
        try (Statement query = connection.createStatement(); ResultSet rows = query.executeQuery("SELECT COUNT(*) FROM sys_menu WHERE parent_id IN ("+ids+") AND id NOT IN ("+ids+")")) {
            rows.next(); if (rows.getInt(1) != 0) throw new Rejected("CUSTOM_MENU_DESCENDANTS_REQUIRE_REVIEW");
        }
        try (Statement query = connection.createStatement(); ResultSet rows = query.executeQuery("SELECT COUNT(*) FROM sys_menu WHERE id IN (7000,7010,7011,7012,7013,7014,7015,7016,7017,7018,9000,9010,9011,9012,9013,9014) AND NOT ((id=7000 AND COALESCE(path,'')='/open') OR (id=7010 AND parent_id=7000 AND COALESCE(path,'')='/open/app') OR (id BETWEEN 7011 AND 7018 AND parent_id=7010 AND COALESCE(permission,'') LIKE 'open:app:%') OR (id=9000 AND COALESCE(path,'')='/code') OR (id=9010 AND parent_id=9000 AND COALESCE(path,'')='/code/generator') OR (id BETWEEN 9011 AND 9014 AND parent_id=9010 AND COALESCE(permission,'') LIKE 'code:generator:%'))")) { rows.next(); if (rows.getInt(1) != 0) throw new Rejected("RETIRED_MENU_IDENTITY_CONFLICT"); }
        for (String q : List.of("SELECT COUNT(*) FROM sys_menu WHERE id IN ("+ids+")",
                "SELECT COUNT(*) FROM sys_role_menu WHERE menu_id IN ("+ids+")",
                "SELECT COUNT(*) FROM tenant_package_menu WHERE menu_id IN ("+ids+")",
                "SELECT COUNT(*) FROM sys_option WHERE code='MODULE_OPEN_ENABLED' AND category='MODULE'")) {
            try (Statement query = connection.createStatement(); ResultSet rows = query.executeQuery(q)) { rows.next(); if (rows.getInt(1) != 0) retired = false; }
        }
        return retired;
    }

    /** Read only this release identity. Never creates, syncs, clears or modifies existing history. */
    static String readHistory(Connection connection) throws SQLException, Rejected {
        try (Statement query=connection.createStatement(); ResultSet rows=query.executeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND LOWER(table_name)='databasechangelog'")) {
            rows.next(); if(rows.getInt(1)==0) return null;
        }
        try (PreparedStatement query=connection.prepareStatement("SELECT ID,AUTHOR,MD5SUM FROM DATABASECHANGELOG WHERE FILENAME=?")) {
            query.setString(1,RELEASE);
            try (ResultSet rows=query.executeQuery()) {
                if(!rows.next()) return null;
                String checksum=rows.getString("MD5SUM");
                if(!CHANGE_ID.equals(rows.getString("ID")) || !"ls".equals(rows.getString("AUTHOR"))
                        || checksum==null || checksum.isBlank() || rows.next()) throw new Rejected("UNEXPECTED_SCOPED_HISTORY");
                return checksum;
            }
        }
    }

    static void verifyDatabase(Connection connection,String schema) throws SQLException, Rejected {
        if(!"MySQL".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName()) || connection.getMetaData().getDatabaseMajorVersion()<8) throw new Rejected("MYSQL8_REQUIRED");
        try(Statement query=connection.createStatement();ResultSet rows=query.executeQuery("SELECT DATABASE()")) {
            if(!rows.next() || !schema.equals(rows.getString(1))) throw new Rejected("CONNECTED_SCHEMA_MISMATCH");
        }
        try(Statement query=connection.createStatement();ResultSet rows=query.executeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name IN ('sys_menu','sys_role_menu','tenant_package_menu','sys_option') AND engine='InnoDB'")) {
            rows.next();if(rows.getInt(1)!=4)throw new Rejected("INNODB_PLATFORM_TABLES_REQUIRED");
        }
    }

    static void verifyReleaseResources() throws Exception {
        for(var entry:DIGESTS.entrySet())try(InputStream input=R44MigrationCli.class.getClassLoader().getResourceAsStream(entry.getKey())) {
            if(input==null) throw new Rejected("RELEASE_RESOURCE_MISSING");
            String text=new String(input.readAllBytes(),StandardCharsets.UTF_8).replace("\r\n","\n").replace("\r","\n");
            String actual=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
            if(!actual.equals(entry.getValue())) throw new Rejected("RELEASE_RESOURCE_CHANGED");
        }
    }
    private static void usage() {
        System.err.println("Usage: R44MigrationCli validate|status|apply --target DB_HOST:DB_PORT/DB_NAME [--acknowledge-write]");
        System.err.println("Explicit DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PWD required; fixed empty-feature-retirement MySQL8 release, no master/seed/sync. Only apply accepts/requires the write acknowledgement.");
    }
    static final class Rejected extends Exception { Rejected(String reason){super(reason);} }
}
