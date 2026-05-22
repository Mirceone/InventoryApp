package com.mirceone.inventoryapp.service.documents;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DocumentFolderTaxonomyTest {

    @Test
    void hasExactlyFivePaths() {
        assertEquals(5, DocumentFolderTaxonomy.allPaths().size());
        assertEquals(
                List.of("Documente", "Renders", "Poze", "Facturi", "Misc"),
                DocumentFolderTaxonomy.allPaths()
        );
    }

    @Test
    void resolvesRenderSynonymsToRenders() {
        assertEquals("Renders", DocumentFolderTaxonomy.resolve("Renderi", Optional.empty()));
        assertEquals("Renders", DocumentFolderTaxonomy.resolve("rendering", Optional.empty()));
    }

    @Test
    void mapsToolSynonymsToDocumente() {
        assertEquals("Documente", DocumentFolderTaxonomy.toCanonicalFolderPath("SketchUp"));
        assertEquals("Documente", DocumentFolderTaxonomy.toCanonicalFolderPath("CAD"));
        assertEquals("Documente", DocumentFolderTaxonomy.toCanonicalFolderPath("office"));
    }

    @Test
    void unknownPathFallsBackToMisc() {
        assertEquals("Misc", DocumentFolderTaxonomy.resolve("Random/New/Folder", Optional.empty()));
        assertEquals("Misc", DocumentFolderTaxonomy.toCanonicalFolderPath("Unknown/Weird"));
        assertEquals("Misc", DocumentFolderTaxonomy.toCanonicalFolderPath("Poze/Planuri"));
    }

    @Test
    void ruleDetectsInvoiceFilename() {
        assertEquals(
                Optional.of("Facturi"),
                DocumentFolderTaxonomy.ruleFolderHint("factura_2024.pdf", "application/pdf")
        );
    }

    @Test
    void ruleDetectsRenderInImageFilename() {
        assertEquals(
                Optional.of("Renders"),
                DocumentFolderTaxonomy.ruleFolderHint("kitchen_render_v3.png", "image/png")
        );
    }

    @Test
    void ruleMapsSkpToDocumente() {
        assertEquals(
                Optional.of("Documente"),
                DocumentFolderTaxonomy.ruleFolderHint("model.skp", "application/octet-stream")
        );
    }
}
