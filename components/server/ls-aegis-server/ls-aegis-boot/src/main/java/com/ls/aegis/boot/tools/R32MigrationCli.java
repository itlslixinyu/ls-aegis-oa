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

/** Standalone quote-ownership R32 release. R29 options/target parsing is reused without changing that release. */
public final class R32MigrationCli {
    static final String RELEASE = "db/changelog/releases/r32-mysql.yaml";
    static final String PATCH = "db/changelog/mysql/patch_quote_data_scope.sql";
    static final String CHANGE_ID = "r32_quote_data_scope_mysql";
    static final List<String> COLUMNS = List.of("tenant_id", "create_user", "dept_id");
    // Canonical UTF-8/LF inputs are immutable after this release is deployed.
    private static final Map<String,String> DIGESTS = Map.of(
        RELEASE, "3feac1f1445729c855e64eff6671a50ad3d2865d17d85c1b33b96f57697b24b6",
        PATCH, "9c2fd5f0fa93bbf3d151439dd5f510349dc5ad25b6058104f40a55eb8a6e042d"
    );

    private R32MigrationCli() { }

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
            System.err.println("R32_MIGRATION_REJECTED reason=" + error.getMessage()); usage(); System.exit(2);
        } catch (Throwable error) {
            // Driver messages/stack traces may contain connection credentials; deliberately omit them.
            System.err.println("R32_MIGRATION_FAILED type=" + error.getClass().getSimpleName()
                + "; inspect authorized quote columns/history; no automatic cleanup, rollback, or baseline sync");
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
        boolean exists = verifyColumnDefinition(connection);
        if (previousChecksum != null && !exists) throw new Rejected("RECORDED_COLUMNS_MISSING");
        int pending = previousChecksum == null ? 1 : 0;
        System.out.println("R32_"+options.action().toUpperCase(Locale.ROOT)+" target="+target.identity()
            +" scope=R32-MYSQL total=1 recorded="+(1-pending)+" pending="+pending+" columns="+(exists?"EXACT_EXISTS":"ABSENT"));
        if (!options.action().equals("apply")) {
            // No Liquibase validate/update/history initialization: a database without history stays without it.
            System.out.println("R32_READ_ONLY_OK schema/history unchanged; all three exact pre-existing nullable ownership columns may be MARK_RAN on apply");
            return;
        }
        migration.validate();
        migration.update(new Contexts(),new LabelExpression());
        String checksum = readHistory(connection);
        if (checksum == null || !change.isCheckSumValid(CheckSum.parse(checksum)) || !verifyColumnDefinition(connection))
            throw new Rejected("SCOPED_MIGRATION_INCOMPLETE");
        System.out.println("R32_APPLY_OK target="+target.identity()+" pendingBefore="+pending+" pendingAfter=0 columns=EXACT_EXISTS");
    }

    /** None is permitted before apply; partial/wrong/defaulted/generated ownership must fail closed. */
    static boolean verifyColumnDefinition(Connection connection) throws SQLException, Rejected {
        try (PreparedStatement query = connection.prepareStatement("SELECT COLUMN_NAME,DATA_TYPE,COLUMN_TYPE,IS_NULLABLE,COLUMN_DEFAULT,EXTRA,GENERATION_EXPRESSION FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='eoms_quote' AND column_name IN ('tenant_id','create_user','dept_id')")) {
            Set<String> seen = new HashSet<>();
            try (ResultSet rows=query.executeQuery()) {
                while(rows.next()) {
                    String name=rows.getString("COLUMN_NAME");
                    String columnType=rows.getString("COLUMN_TYPE");
                    String extra=rows.getString("EXTRA");
                    String expression=rows.getString("GENERATION_EXPRESSION");
                    if(name==null || !COLUMNS.contains(name.toLowerCase(Locale.ROOT)) || !seen.add(name.toLowerCase(Locale.ROOT))
                        || !"bigint".equalsIgnoreCase(rows.getString("DATA_TYPE")) || columnType==null
                        || !columnType.toLowerCase(Locale.ROOT).matches("bigint(?:\\(\\d+\\))?")
                        || !"YES".equalsIgnoreCase(rows.getString("IS_NULLABLE")) || rows.getObject("COLUMN_DEFAULT")!=null
                        || (extra!=null && !extra.isBlank()) || (expression!=null && !expression.isBlank()))
                        throw new Rejected("OWNERSHIP_COLUMN_DEFINITION_MISMATCH");
                }
            }
            if(!seen.isEmpty() && seen.size()!=COLUMNS.size()) throw new Rejected("PARTIAL_OWNERSHIP_COLUMNS");
            return !seen.isEmpty();
        }
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
        if(!"MySQL".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName()) || connection.getMetaData().getDatabaseMajorVersion()<8)
            throw new Rejected("MYSQL8_REQUIRED");
        try(Statement query=connection.createStatement();ResultSet rows=query.executeQuery("SELECT DATABASE()")) {
            if(!rows.next() || !schema.equals(rows.getString(1))) throw new Rejected("CONNECTED_SCHEMA_MISMATCH");
        }
        try(Statement query=connection.createStatement();ResultSet rows=query.executeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='eoms_quote' AND engine='InnoDB'")) {
            rows.next(); if(rows.getInt(1)!=1) throw new Rejected("INNODB_QUOTE_TABLE_REQUIRED");
        }
        try(Statement query=connection.createStatement();ResultSet rows=query.executeQuery("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='eoms_quote' AND column_name IN ('id','create_by','del_flag')")) {
            rows.next(); if(rows.getInt(1)!=3) throw new Rejected("LEGACY_QUOTE_BASE_COLUMNS_REQUIRED");
        }
    }

    static void verifyReleaseResources() throws Exception {
        for(var entry:DIGESTS.entrySet())try(InputStream input=R32MigrationCli.class.getClassLoader().getResourceAsStream(entry.getKey())) {
            if(input==null) throw new Rejected("RELEASE_RESOURCE_MISSING");
            String text=new String(input.readAllBytes(),StandardCharsets.UTF_8).replace("\r\n","\n").replace("\r","\n");
            String actual=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
            if(!actual.equals(entry.getValue())) throw new Rejected("RELEASE_RESOURCE_CHANGED");
        }
    }
    private static void usage() {
        System.err.println("Usage: R32MigrationCli validate|status|apply --target DB_HOST:DB_PORT/DB_NAME [--acknowledge-write]");
        System.err.println("Explicit DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PWD required; fixed nullable quote-ownership MySQL8 release, no master/seed/sync. Only apply accepts/requires the write acknowledgement.");
    }
    static final class Rejected extends Exception { Rejected(String reason){super(reason);} }
}
