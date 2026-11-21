package com.ls.aegis.boot.tools;

import liquibase.Contexts;
import liquibase.changelog.ChangeLogParameters;
import liquibase.parser.core.formattedsql.FormattedSqlChangeLogParser;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OaSiteMigrationTest {
    @Test void identityAndStorageMigrationsAreParsedAsNormalNonDestructiveUpgrades() throws Exception {
        try (var resources = new ClassLoaderResourceAccessor()) {
            for (var name : new String[]{"patch_oa_site_identity.sql", "patch_oa_file_same_origin.sql", "patch_oa_copyright.sql"}) {
                var log = new FormattedSqlChangeLogParser().parse("db/changelog/mysql/" + name, new ChangeLogParameters(), resources);
                assertEquals(1, log.getChangeSets().size());
                assertTrue(log.getChangeSets().get(0).getContexts().matches(new Contexts("prod")));
            }
        }
    }
}
