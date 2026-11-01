package com.example.yakyeon2.data;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MedicineNameCatalogTest {

    @Test
    public void examplesAreOnlyTheTenSampleNames() {
        MedicineNameCatalog catalog = new MedicineNameCatalog();

        assertEquals(
                Arrays.asList("약1", "약2", "약3", "약4", "약5", "약6", "약7", "약8", "약9", "약10"),
                catalog.examples()
        );
        assertTrue(catalog.customNames().isEmpty());
        assertEquals(MedicineNameCatalog.Source.EXAMPLE, catalog.sourceOf("약1"));
        assertNull(catalog.sourceOf("없는 약"));
    }

    @Test
    public void searchFindsExamplesAndSavedNamesWithoutDuplicating() {
        MedicineNameCatalog catalog = new MedicineNameCatalog();

        assertEquals(2, catalog.search("약1").size());
        assertTrue(catalog.search("없는약").isEmpty());

        MedicineNameCatalog.AddResult added = catalog.addCustom("  직접약  ");
        assertEquals(MedicineNameCatalog.AddStatus.ADDED, added.status);
        assertEquals("직접약", added.name);
        assertEquals(MedicineNameCatalog.AddStatus.DUPLICATE, catalog.addCustom("직접약").status);
        assertEquals(MedicineNameCatalog.AddStatus.DUPLICATE, catalog.addCustom("약3").status);
        assertEquals(MedicineNameCatalog.AddStatus.EMPTY, catalog.addCustom("  ").status);
        assertEquals(1, catalog.customNames().size());
        assertEquals(MedicineNameCatalog.Source.CUSTOM, catalog.sourceOf("직접약"));

        MedicineNameCatalog restored = new MedicineNameCatalog();
        restored.restore(MedicineNameCodec.decode(MedicineNameCodec.encode(catalog.customNames())));
        assertEquals(Arrays.asList("직접약"), restored.customNames());
        restored.restore(MedicineNameCodec.decode("약1\n직접약\n\n직접약"));
        assertEquals(Arrays.asList("직접약"), restored.customNames());
    }
}
