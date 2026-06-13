package com.mirceone.inventoryapp.service.workorders.invoices;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChainedInvoiceMarkdownExtractorTest {

    private MarkItDownCliExtractor markItDown;
    private VlmInvoiceExtractor vlm;
    private PdfTextLayerDetector textLayerDetector;
    private ChainedInvoiceMarkdownExtractor chain;

    private final Path file = Path.of("/tmp/invoice.pdf");

    @BeforeEach
    void setUp() {
        markItDown = mock(MarkItDownCliExtractor.class);
        vlm = mock(VlmInvoiceExtractor.class);
        textLayerDetector = mock(PdfTextLayerDetector.class);
        chain = new ChainedInvoiceMarkdownExtractor(markItDown, vlm, textLayerDetector);
    }

    @Test
    void pdfWithTextLayerUsesMarkItDown() throws Exception {
        when(textLayerDetector.hasTextLayer(file)).thenReturn(true);
        when(markItDown.extract(file, "application/pdf")).thenReturn("# Digital invoice");

        assertEquals("# Digital invoice", chain.extract(file, "application/pdf"));
        verify(vlm, never()).extract(any(), any());
    }

    @Test
    void scannedPdfWithoutTextLayerUsesVlm() throws Exception {
        when(textLayerDetector.hasTextLayer(file)).thenReturn(false);
        when(vlm.extract(file, "application/pdf")).thenReturn("# Scanned invoice");

        assertEquals("# Scanned invoice", chain.extract(file, "application/pdf"));
        verify(markItDown, never()).extract(any(), any());
    }

    @Test
    void imageUploadUsesVlm() throws Exception {
        Path image = Path.of("/tmp/invoice.png");
        when(vlm.extract(image, "image/png")).thenReturn("# Image invoice");

        assertEquals("# Image invoice", chain.extract(image, "image/png"));
        verify(markItDown, never()).extract(any(), any());
    }

    @Test
    void fallsBackToVlmWhenMarkItDownReturnsEmptyDespiteTextLayer() throws Exception {
        when(textLayerDetector.hasTextLayer(file)).thenReturn(true);
        when(markItDown.extract(file, "application/pdf"))
                .thenThrow(new java.io.IOException("MarkItDown produced empty output"));
        when(vlm.extract(file, "application/pdf")).thenReturn("# Recovered via VLM");

        assertEquals("# Recovered via VLM", chain.extract(file, "application/pdf"));
    }
}
