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

/** Standalone one-index R31 release. R29 options/target parsing is reused without changing that release. */
public final class R31MigrationCli {
    static final String RELEASE = "db/changelog/releases/r31-mysql.yaml";
    static final String PATCH = "db/changelog/mysql/patch_stock_alert_balance_index.sql";
    static final String CHANGE_ID = "r31_stock_alert_balance_index_mysql";
    static final String INDEX = "idx_stock_alert_balance";
    static final List<String> COLUMNS = List.of("tenant_id", "deleted", "goods_id", "warehouse_id", "remain_quantity");
    // Canonical UTF-8/LF inputs are immutable after this release is deployed.
    private static final Map<String,String> DIGESTS = Map.of(
        RELEASE, "8475dc7628d2cf6a54881332b50123097cc27d25e1a58822fdbeea7e5d024e3e",
        PATCH, "94ad3312deaff37e08ad691d9590e6a12e437ac44924fdfce64d2d9689fd954b"
    );

    private R31MigrationCli() { }

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
            System.err.println("R31_MIGRATION_REJECTED reason=" + error.getMessage()); usage(); System.exit(2);
        } catch (Throwable error) {
            // Driver messages/stack traces may contain connection credentials; deliberately omit them.
            System.err.println("R31_MIGRATION_FAILED type=" + error.getClass().getSimpleName()
                + "; inspect authorized index/history; no automatic cleanup, rollback, or baseline sync");
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
        boolean exists = verifyIndexDefinition(connection);
        if (previousChecksum != null && !exists) throw new Rejected("RECORDED_INDEX_MISSING");
        int pending = previousChecksum == null ? 1 : 0;
        System.out.println("R31_"+options.action().toUpperCase(Locale.ROOT)+" target="+target.identity()
            +" scope=R31-MYSQL total=1 recorded="+(1-pending)+" pending="+pending+" index="+(exists?"EXACT_EXISTS":"ABSENT"));
        if (!options.action().equals("apply")) {
            // No Liquibase validate/update/history initialization: a database without history stays without it.
            System.out.println("R31_READ_ONLY_OK schema/history unchanged; an exact pre-existing index may be MARK_RAN on apply");
            return;
        }
        migration.validate();
        migration.update(new Contexts(),new LabelExpression());
        String checksum = readHistory(connection);
        if (checksum == null || !change.isCheckSumValid(CheckSum.parse(checksum)) || !verifyIndexDefinition(connection))
            throw new Rejected("SCOPED_MIGRATION_INCOMPLETE");
        System.out.println("R31_APPLY_OK target="+target.identity()+" pendingBefore="+pending+" pendingAfter=0 index=EXACT_EXISTS");
    }

    /** Absent is permitted for a pending change; every property of an existing index must match. */
    static boolean verifyIndexDefinition(Connection connection) throws SQLException, Rejected {
        try (PreparedStatement query = connection.prepareStatement("SELECT COLUMN_NAME,SEQ_IN_INDEX,NON_UNIQUE,INDEX_TYPE,SUB_PART,COLLATION,IS_VISIBLE FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='psi_stock_batch' AND index_name=? ORDER BY SEQ_IN_INDEX")) {
            query.setString(1,INDEX);
            int count=0;
            try (ResultSet rows=query.executeQuery()) {
                while(rows.next()) {
                    if (count>=COLUMNS.size() || rows.getInt("SEQ_IN_INDEX")!=count+1
                            || !COLUMNS.get(count).equalsIgnoreCase(rows.getString("COLUMN_NAME"))
                            || rows.getInt("NON_UNIQUE")!=1 || !"BTREE".equalsIgnoreCase(rows.getString("INDEX_TYPE"))
                            || rows.getObject("SUB_PART")!=null || !"A".equalsIgnoreCase(rows.getString("COLLATION"))
                            || !"YES".equalsIgnoreCase(rows.getString("IS_VISIBLE"))) throw new Rejected("INDEX_DEFINITION_MISMATCH");
                    count++;
                }
            }
            if(count!=0 && count!=COLUMNS.size()) throw new Rejected("INDEX_DEFINITION_MISMATCH");
            return count!=0;
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
        try(Statement query=connection.createStatement();ResultSet rows=query.executeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='psi_stock_batch' AND engine='InnoDB'")) {
            rows.next(); if(rows.getInt(1)!=1) throw new Rejected("INNODB_STOCK_BATCH_REQUIRED");
        }
        try(PreparedStatement query=connection.prepareStatement("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='psi_stock_batch' AND column_name IN (?,?,?,?,?)")) {
            for(int i=0;i<COLUMNS.size();i++) query.setString(i+1,COLUMNS.get(i));
            try(ResultSet rows=query.executeQuery()){rows.next();if(rows.getInt(1)!=COLUMNS.size())throw new Rejected("STOCK_ALERT_COLUMNS_MISSING");}
        }
    }

    static void verifyReleaseResources() throws Exception {
        for(var entry:DIGESTS.entrySet())try(InputStream input=R31MigrationCli.class.getClassLoader().getResourceAsStream(entry.getKey())) {
            if(input==null) throw new Rejected("RELEASE_RESOURCE_MISSING");
            String text=new String(input.readAllBytes(),StandardCharsets.UTF_8).replace("\r\n","\n").replace("\r","\n");
            String actual=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
            if(!actual.equals(entry.getValue())) throw new Rejected("RELEASE_RESOURCE_CHANGED");
        }
    }
    private static void usage() {
        System.err.println("Usage: R31MigrationCli validate|status|apply --target DB_HOST:DB_PORT/DB_NAME [--acknowledge-write]");
        System.err.println("Explicit DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PWD required; fixed one-index MySQL8 release, no master/seed/sync. Only apply accepts/requires the write acknowledgement.");
    }
    static final class Rejected extends Exception { Rejected(String reason){super(reason);} }
}
