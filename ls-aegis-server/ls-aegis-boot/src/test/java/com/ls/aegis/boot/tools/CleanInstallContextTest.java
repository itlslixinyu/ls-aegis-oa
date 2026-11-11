package com.ls.aegis.boot.tools;

import liquibase.Contexts;
import liquibase.changelog.ChangeLogParameters;
import liquibase.parser.core.formattedsql.FormattedSqlChangeLogParser;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CleanInstallContextTest {
    @Test void cleanupRequiresExplicitFreshInstallContextAndHasDataPreconditions() throws Exception {
        try (var resources = new ClassLoaderResourceAccessor()) {
            var log = new FormattedSqlChangeLogParser().parse(
                "db/changelog/mysql/patch_clean_install_defaults.sql", new ChangeLogParameters(), resources);
            assertEquals(2, log.getChangeSets().size());
            var brand = log.getChangeSets().get(0);
            var cleanup = log.getChangeSets().get(1);
            assertTrue(brand.getContexts().matches(new Contexts()));
            assertFalse(cleanup.getContexts().matches(new Contexts()));
            assertFalse(cleanup.getContexts().matches(new Contexts("prod")));
            assertTrue(cleanup.getContexts().matches(new Contexts("clean-install")));
            assertFalse(cleanup.getPreconditions().getNestedPreconditions().isEmpty());
        }
    }
}
