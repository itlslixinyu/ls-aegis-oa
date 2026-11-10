package com.ls.aegis.boot.tools;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;
import liquibase.*;
import liquibase.change.CheckSum;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

/** Standalone, fixed R29 release migration. Does not start Spring, load master/seed, or establish a baseline. */
public final class R29MigrationCli {
    private static final String RELEASE = "db/changelog/releases/r29-mysql.yaml";
    private static final int CHANGESET_COUNT = 52;
    // Canonical UTF-8/LF digests pin the reviewed, immutable release inputs, including the include list.
    private static final Map<String,String> DIGESTS = Map.ofEntries(
        Map.entry("db/changelog/mysql/patch_stock_mutation_lock.sql","d4eb61f2a280cff63096523bce16eb759cf9e77bafb43d626a0b2ced99c1d565"),
        Map.entry("db/changelog/mysql/patch_psi_finance_compensation.sql","ab54610b8e3c340960983402fadedf4f268fcb661dc455e7b04fd123c4a2c8ed"),
        Map.entry("db/changelog/mysql/patch_eoms_finance_compensation.sql","dc7671f510cf34f52dbd4ef230a345cdbdf0f0309d7060421f6a363c094fad43"),
        Map.entry("db/changelog/mysql/patch_stock_adjust_restore.sql","afdc25ecfc6a43d33113ddf5df865df501d4dbb5e99efa69a5358c181df223ed"),
        Map.entry("db/changelog/releases/r29-mysql.yaml","d14163278db880543326ec284029565eb0ff09eb3538590b346db996f8abedc4")
    );
    private static final Set<String> PATCHES = Set.of(
        "db/changelog/mysql/patch_stock_mutation_lock.sql",
        "db/changelog/mysql/patch_psi_finance_compensation.sql",
        "db/changelog/mysql/patch_eoms_finance_compensation.sql",
        "db/changelog/mysql/patch_stock_adjust_restore.sql"
    );
    private static final List<String> REQUIRED_TABLES = List.of("sys_option", "sys_user", "psi_receipt", "psi_payment",
        "psi_write_off_item", "psi_bank_statement", "psi_stock_out", "psi_stock_adjust_item", "eoms_payment",
        "eoms_income", "eoms_cost", "eoms_receivable", "eoms_repair_settlement");

    private R29MigrationCli() { }

    public static void main(String[] args) {
        try {
            if (args.length == 1 && args[0].equals("--help")) { usage(); return; }
            Options options = Options.parse(args);
            Target target = Target.fromEnvironment(System.getenv());
            if (!target.identity().equals(options.target)) throw new Rejected("TARGET_MISMATCH");
            verifyReleaseResources();
            Properties properties = new Properties();
            properties.setProperty("user", target.user);
            properties.setProperty("password", target.password);
            try (Connection connection = DriverManager.getConnection(target.jdbcUrl(), properties);
                 ClassLoaderResourceAccessor resources = new ClassLoaderResourceAccessor()) {
                connection.setReadOnly(!options.action.equals("apply"));
                verifyDatabase(connection, target.database);
                Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
                try (Liquibase migration = new Liquibase(RELEASE, resources, database)) {
                    Scope.child(Map.of("database", database, "resourceAccessor", resources), () -> execute(options, target, connection, database, migration));
                }
            }
        } catch (Rejected error) {
            System.err.println("R29_MIGRATION_REJECTED reason=" + error.getMessage());
            usage(); System.exit(2);
        } catch (Throwable error) {
            // JDBC/driver exception messages may contain credentials. Do not print raw messages or stack traces.
            System.err.println("R29_MIGRATION_FAILED type=" + error.getClass().getSimpleName()
                + "; inspect authorized database/history and migration diagnostics; no automatic cleanup or rollback");
            System.exit(1);
        }
    }

    private static void execute(Options options, Target target, Connection connection, Database database, Liquibase migration) throws Exception {
        List<ChangeSet> changes = migration.getDatabaseChangeLog().getChangeSets();
        Set<String> identities = new HashSet<>();
        if (changes.size() != CHANGESET_COUNT) throw new Rejected("UNEXPECTED_SCOPE_SIZE");
        for (ChangeSet change : changes) {
            if (!PATCHES.contains(change.getFilePath()) || !change.getAuthor().equals("ls") || !identities.add(identity(change)))
                throw new Rejected("UNEXPECTED_CHANGESET_SCOPE");
            for (var sql : change.getChanges()) if (sql.validate(database).hasErrors()) throw new Rejected("INVALID_RELEASE_CHANGE");
        }
        Map<String,String> before = readHistory(connection);
        for (ChangeSet change : changes) {
            String key = identity(change);
            if (before.containsKey(key)) {
                String checksum = before.get(key);
                if (checksum == null || !change.isCheckSumValid(CheckSum.parse(checksum))) throw new Rejected("SCOPED_CHECKSUM_MISMATCH");
            }
        }
        long pending = changes.stream().filter(change -> !before.containsKey(identity(change))).count();
        System.out.println("R29_" + options.action.toUpperCase(Locale.ROOT) + " target=" + target.identity()
            + " scope=R29-MYSQL total=" + CHANGESET_COUNT + " recorded=" + (CHANGESET_COUNT-pending) + " pending=" + pending);
        if (!options.action.equals("apply")) {
            // Do not call Liquibase history initialization for validate/status: even a missing history table stays untouched.
            System.out.println("R29_READ_ONLY_OK schema/history unchanged; pending changes may satisfy MARK_RAN preconditions");
            return;
        }
        migration.validate();
        migration.update(new Contexts(), new LabelExpression());
        Map<String,String> after = readHistory(connection);
        if (changes.stream().anyMatch(change -> !after.containsKey(identity(change)))) throw new Rejected("SCOPED_MIGRATION_INCOMPLETE");
        System.out.println("R29_APPLY_OK target=" + target.identity() + " pendingBefore=" + pending + " pendingAfter=0");
    }

    private static Map<String,String> readHistory(Connection connection) throws SQLException {
        boolean exists;
        try (PreparedStatement query = connection.prepareStatement("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND LOWER(table_name)='databasechangelog'");
             ResultSet rows = query.executeQuery()) { rows.next(); exists = rows.getInt(1) != 0; }
        Map<String,String> history = new HashMap<>();
        if (!exists) return history;
        try (PreparedStatement query = connection.prepareStatement("SELECT ID,AUTHOR,FILENAME,MD5SUM FROM DATABASECHANGELOG WHERE FILENAME IN (?,?,?,?)")) {
            int i=1; for (String file : PATCHES) query.setString(i++,file);
            try (ResultSet rows = query.executeQuery()) {
                while (rows.next()) {
                    String key = rows.getString("FILENAME") + "::" + rows.getString("AUTHOR") + "::" + rows.getString("ID");
                    if (history.containsKey(key)) throw new SQLException("Duplicate scoped history identity");
                    history.put(key,rows.getString("MD5SUM"));
                }
            }
        }
        return history;
    }

    private static void verifyDatabase(Connection connection, String expectedName) throws Exception {
        if (!connection.getMetaData().getDatabaseProductName().equalsIgnoreCase("MySQL") || connection.getMetaData().getDatabaseMajorVersion()<8)
            throw new Rejected("MYSQL8_REQUIRED");
        try (Statement query = connection.createStatement(); ResultSet rows = query.executeQuery("SELECT DATABASE()")) {
            if (!rows.next() || !expectedName.equals(rows.getString(1))) throw new Rejected("CONNECTED_SCHEMA_MISMATCH");
        }
        try (PreparedStatement query = connection.prepareStatement("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name=?")) {
            for (String table : REQUIRED_TABLES) {
                query.setString(1,table); try (ResultSet rows = query.executeQuery()) {
                    if (!rows.next() || rows.getInt(1)!=1) throw new Rejected("PLATFORM_BASE_TABLE_MISSING:" + table);
                }
            }
        }
    }

    private static void verifyReleaseResources() throws Exception {
        for (var entry : DIGESTS.entrySet()) {
            try (InputStream stream = R29MigrationCli.class.getClassLoader().getResourceAsStream(entry.getKey())) {
                if (stream == null) throw new Rejected("RELEASE_RESOURCE_MISSING");
                String sql = new String(stream.readAllBytes(),StandardCharsets.UTF_8).replace("\r\n","\n").replace("\r","\n");
                String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(sql.getBytes(StandardCharsets.UTF_8)));
                if (!actual.equals(entry.getValue())) throw new Rejected("RELEASE_RESOURCE_CHANGED");
            }
        }
    }
    private static String identity(ChangeSet change) { return change.getFilePath()+"::"+change.getAuthor()+"::"+change.getId(); }
    private static String required(Map<String,String> env,String key) throws Rejected {
        String value=env.get(key); if(value==null || value.isBlank()) throw new Rejected("MISSING_"+key); return value;
    }
    private static void usage() {
        System.err.println("Usage: R29MigrationCli validate|status|apply --target DB_HOST:DB_PORT/DB_NAME [--acknowledge-write]");
        System.err.println("apply requires --acknowledge-write; DB_HOST,DB_PORT,DB_NAME,DB_USER,DB_PWD must be explicitly configured. Fixed four-patch MySQL release only.");
    }
    static final class Rejected extends Exception { Rejected(String reason) { super(reason); } }
    record Options(String action,String target) {
        static Options parse(String[] args) throws Rejected {
            if(args.length<3 || !Set.of("validate","status","apply").contains(args[0])) throw new Rejected("INVALID_ACTION");
            String target=null; boolean acknowledge=false;
            for(int i=1;i<args.length;i++) {
                if(args[i].equals("--target") && target==null && i+1<args.length) target=args[++i];
                else if(args[i].equals("--acknowledge-write") && !acknowledge) acknowledge=true;
                else throw new Rejected("UNSUPPORTED_ARGUMENT");
            }
            if(target==null || target.isBlank()) throw new Rejected("EXPLICIT_TARGET_REQUIRED");
            if(args[0].equals("apply")!=acknowledge) throw new Rejected("WRITE_ACKNOWLEDGEMENT_REQUIRED_ONLY_FOR_APPLY");
            return new Options(args[0],target);
        }
    }
    record Target(String host,int port,String database,String user,String password,boolean ssl) {
        static Target fromEnvironment(Map<String,String> env) throws Rejected {
            String host=required(env,"DB_HOST"),rawPort=required(env,"DB_PORT"),name=required(env,"DB_NAME");
            if(!host.matches("[A-Za-z0-9][A-Za-z0-9._-]*") || !name.matches("[A-Za-z0-9_]+")) throw new Rejected("INVALID_DATABASE_ADDRESS");
            int port; try{port=Integer.parseInt(rawPort);}catch(NumberFormatException e){throw new Rejected("INVALID_DATABASE_PORT");}
            if(port<1 || port>65535)throw new Rejected("INVALID_DATABASE_PORT");
            String ssl=env.getOrDefault("MYSQL_USE_SSL","false");
            if(!Set.of("true","false").contains(ssl))throw new Rejected("INVALID_SSL_FLAG");
            return new Target(host,port,name,required(env,"DB_USER"),required(env,"DB_PWD"),Boolean.parseBoolean(ssl));
        }
        @Override public String toString(){return "Target["+identity()+"]";}
        String identity(){return host+":"+port+"/"+database;}
        String jdbcUrl(){return "jdbc:mysql://"+host+":"+port+"/"+database+"?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf8&useSSL="+ssl+"&allowPublicKeyRetrieval=true";}
    }
}
