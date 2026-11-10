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

/** Standalone nullable signing-date R43 release. Reuses R29 explicit action/target parsing. */
public final class R43MigrationCli {
    static final String RELEASE = "db/changelog/releases/r43-mysql.yaml";
    static final String PATCH = "db/changelog/mysql/patch_eoms_contract_sign_date.sql";
    static final String CHANGE_ID = "r43_eoms_contract_sign_date_mysql";
    // Canonical UTF-8/LF inputs are immutable after this release is deployed.
    private static final Map<String,String> DIGESTS = Map.of(
        RELEASE, "f8c063fdcec4f5a4654f7d7cf1ad15a77398634e839bb9385801a526c1cbb6ff",
        PATCH, "2f0d3f61222e2e8c134c45aa45c8d0b9f76602132b97124b4911122053618edd"
    );

    private R43MigrationCli() { }

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
            System.err.println("R43_MIGRATION_REJECTED reason=" + error.getMessage()); usage(); System.exit(2);
        } catch (Throwable error) {
            // Driver messages/stack traces may contain connection credentials; deliberately omit them.
            System.err.println("R43_MIGRATION_FAILED type=" + error.getClass().getSimpleName()
                + "; inspect authorized column/history; no automatic cleanup, rollback, or baseline sync");
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
        if (previousChecksum != null && !exists) throw new Rejected("RECORDED_COLUMN_MISSING");
        int pending = previousChecksum == null ? 1 : 0;
        System.out.println("R43_"+options.action().toUpperCase(Locale.ROOT)+" target="+target.identity()
            +" scope=R43-MYSQL total=1 recorded="+(1-pending)+" pending="+pending+" column="+(exists?"EXACT_EXISTS":"ABSENT"));
        if (!options.action().equals("apply")) {
            // No Liquibase validate/update/history initialization: a database without history stays without it.
            System.out.println("R43_READ_ONLY_OK schema/history unchanged; an exact pre-existing column may be MARK_RAN on apply");
            return;
        }
        migration.validate();
        migration.update(new Contexts(),new LabelExpression());
        String checksum = readHistory(connection);
        if (checksum == null || !change.isCheckSumValid(CheckSum.parse(checksum)) || !verifyColumnDefinition(connection))
            throw new Rejected("SCOPED_MIGRATION_INCOMPLETE");
        System.out.println("R43_APPLY_OK target="+target.identity()+" pendingBefore="+pending+" pendingAfter=0 column=EXACT_EXISTS");
    }

    /** Absent is permitted for a pending change; existing nullable datetime definition must match. */
    static boolean verifyColumnDefinition(Connection connection) throws SQLException, Rejected {
        try (Statement query=connection.createStatement(); ResultSet row=query.executeQuery("SELECT DATA_TYPE,DATETIME_PRECISION,IS_NULLABLE,COLUMN_DEFAULT,EXTRA,GENERATION_EXPRESSION FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='eoms_contract' AND column_name='sign_date'")) {
            if(!row.next()) return false;
            if(!"datetime".equalsIgnoreCase(row.getString("DATA_TYPE")) || row.getInt("DATETIME_PRECISION")!=0
                    || !"YES".equals(row.getString("IS_NULLABLE")) || row.getObject("COLUMN_DEFAULT")!=null
                    || !"".equals(row.getString("EXTRA")) || !"".equals(row.getString("GENERATION_EXPRESSION")) || row.next())
                throw new Rejected("SIGN_DATE_DEFINITION_CONFLICT");
            return true;
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
        if(!"MySQL".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName()) || connection.getMetaData().getDatabaseMajorVersion()<8) throw new Rejected("MYSQL8_REQUIRED");
        try(Statement query=connection.createStatement();ResultSet rows=query.executeQuery("SELECT DATABASE()")) {
            if(!rows.next() || !schema.equals(rows.getString(1))) throw new Rejected("CONNECTED_SCHEMA_MISMATCH");
        }
        try(Statement query=connection.createStatement();ResultSet rows=query.executeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='eoms_contract' AND engine='InnoDB'")) {
            rows.next();if(rows.getInt(1)!=1)throw new Rejected("INNODB_CONTRACT_TABLE_REQUIRED");
        }
    }

    static void verifyReleaseResources() throws Exception {
        for(var entry:DIGESTS.entrySet())try(InputStream input=R43MigrationCli.class.getClassLoader().getResourceAsStream(entry.getKey())) {
            if(input==null) throw new Rejected("RELEASE_RESOURCE_MISSING");
            String text=new String(input.readAllBytes(),StandardCharsets.UTF_8).replace("\r\n","\n").replace("\r","\n");
            String actual=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
            if(!actual.equals(entry.getValue())) throw new Rejected("RELEASE_RESOURCE_CHANGED");
        }
    }
    private static void usage() {
        System.err.println("Usage: R43MigrationCli validate|status|apply --target DB_HOST:DB_PORT/DB_NAME [--acknowledge-write]");
        System.err.println("Explicit DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PWD required; fixed one-column MySQL8 release, no master/seed/sync. Only apply accepts/requires the write acknowledgement.");
    }
    static final class Rejected extends Exception { Rejected(String reason){super(reason);} }
}
