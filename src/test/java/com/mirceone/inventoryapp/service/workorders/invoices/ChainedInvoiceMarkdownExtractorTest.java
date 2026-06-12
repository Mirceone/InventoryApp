package com.mirceone.inventoryapp.service.workorders.invoices;

import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChainedInvoiceMarkdownExtractorTest {

    private static final Path VENV_PYTHON =
            Path.of(System.getProperty("user.dir"), ".venv-markitdown", "bin", "python3");
    private static final Path OCR_SCRIPT =
            Path.of(System.getProperty("user.dir"), "scripts", "invoice_pdf_ocr.py");
    private static final Path SAMPLE_PDF = Path.of(
            System.getProperty("user.home"),
            ".inventoryapp/uploads/64e31f5d-1c13-4ea1-a805-d0bf3c9d2f56/d3579815-9f41-40b1-996d-25ec323f2f6f/invoices/fd42f462-77b5-4750-be63-5fc6893df239.pdf");

    @Test
    void fallsBackToOcrWhenMarkItDownReturnsEmptyForPdf() throws Exception {
        MarkItDownCliExtractor markItDown = mock(MarkItDownCliExtractor.class);
        when(markItDown.extract(any(), eq("application/pdf")))
                .thenThrow(new java.io.IOException("MarkItDown produced empty output"));

        ScannedInvoiceOcrCliExtractor ocr = mock(ScannedInvoiceOcrCliExtractor.class);
        when(ocr.extract(any(), eq("application/pdf"))).thenReturn("# OCR invoice text");

        AppIntegrationProperties props = new AppIntegrationProperties();
        props.getInvoices().setOcrFallbackEnabled(true);

        ChainedInvoiceMarkdownExtractor chain =
                new ChainedInvoiceMarkdownExtractor(markItDown, ocr, props);

        Path tempPdf = Files.createTempFile("invoice-chain-", ".pdf");
        String markdown = chain.extract(tempPdf, "application/pdf");
        assertTrue(markdown.contains("OCR invoice text"));
        Files.deleteIfExists(tempPdf);
    }

    @Test
    @EnabledIf("ocrDepsAndSamplePdfAvailable")
    void ocrExtractsScannedInvoicePdf() throws Exception {
        AppIntegrationProperties props = new AppIntegrationProperties();
        props.getInvoices().setOcrPythonCommand(VENV_PYTHON.toString());
        props.getInvoices().setOcrLanguages("ro,en");
        props.getInvoices().setOcrTimeout(java.time.Duration.ofMinutes(5));

        InvoiceOcrCommandResolver resolver = new InvoiceOcrCommandResolver(props);
        resolver.resolveAtStartup();
        ScannedInvoiceOcrCliExtractor ocr = new ScannedInvoiceOcrCliExtractor(props, resolver);

        String markdown = ocr.extract(SAMPLE_PDF, "application/pdf");
        assertFalse(markdown.isBlank());
        assertTrue(markdown.toLowerCase().contains("factura") || markdown.toLowerCase().contains("darel"));
    }

    @SuppressWarnings("unused")
    static boolean ocrDepsAndSamplePdfAvailable() {
        return Files.isExecutable(VENV_PYTHON)
                && Files.isRegularFile(OCR_SCRIPT)
                && Files.isRegularFile(SAMPLE_PDF)
                && InvoiceOcrCommandResolver.probeOcrDeps(
                        InvoiceOcrCommandResolver.resolvePython(VENV_PYTHON.toString()), OCR_SCRIPT);
    }
}
