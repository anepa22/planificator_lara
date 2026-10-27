package ar.com.anepanet.planificator.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocationCatalogSchemaTest {

    @Test
    void indexIsCreatedOnlyWhenNamesDoNotClashAndASecondStartupIsSafe() {
        assertTrue(LocationCatalogSchema.shouldCreateNameIndex(0));
        assertFalse(LocationCatalogSchema.shouldCreateNameIndex(1));
        assertTrue(LocationCatalogSchema.UNIQUE_NAME_INDEX.contains("IF NOT EXISTS"));
        assertFalse(LocationCatalogSchema.SHIFT_WEEK_VIEW.contains("l.is_active"));
        assertTrue(LocationCatalogSchema.SHIFT_WEEK_VIEW.contains("l.name"));
    }
}
