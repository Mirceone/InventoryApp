package com.mirceone.inventoryapp.integration;

import com.mirceone.inventoryapp.model.InvoiceExtractionStatus;
import com.mirceone.inventoryapp.model.InvoiceProcessingStatus;
import com.mirceone.inventoryapp.model.UserEntity;
import com.mirceone.inventoryapp.repository.UserRepository;
import com.mirceone.inventoryapp.service.auth.AuthContracts;
import com.mirceone.inventoryapp.service.auth.AuthService;
import com.mirceone.inventoryapp.service.firms.FirmContracts;
import com.mirceone.inventoryapp.service.firms.FirmService;
import com.mirceone.inventoryapp.service.workorders.ExtractionDetail;
import com.mirceone.inventoryapp.service.workorders.InvoiceSummary;
import com.mirceone.inventoryapp.service.workorders.WorkOrderContracts;
import com.mirceone.inventoryapp.service.workorders.WorkOrderInvoiceService;
import com.mirceone.inventoryapp.service.workorders.WorkOrderService;
import com.mirceone.inventoryapp.service.workorders.WorkOrderSummary;
import com.mirceone.inventoryapp.service.workorders.invoices.extraction.InvoiceStructuringService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WorkOrderInvoiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private AuthService authService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FirmService firmService;
    @Autowired
    private WorkOrderService workOrderService;
    @Autowired
    private WorkOrderInvoiceService invoiceService;
    @Autowired
    private InvoiceStructuringService structuringService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @org.junit.jupiter.api.io.TempDir
    static Path storageRoot;

    @DynamicPropertySource
    static void storageProps(DynamicPropertyRegistry registry) {
        registry.add("app.storage.root", () -> storageRoot.toAbsolutePath().toString());
    }

    @Test
    void uploadExtractProductsListAndDeleteFlow() throws Exception {
        authService.signup(new AuthContracts.SignupSpec("invoice-it@example.com", "password123", "Invoice IT"));
        UserEntity user = userRepository.findByEmailIgnoreCase("invoice-it@example.com").orElseThrow();
        UUID userId = user.getId();

        FirmContracts.FirmSummary firm =
                firmService.createFirm(userId, new FirmContracts.CreateFirmSpec("Firm Invoices"));
        WorkOrderSummary workOrder = workOrderService.createWorkOrder(
                userId, firm.id(),
                new WorkOrderContracts.CreateWorkOrderSpec(
                        "Renovare", "Client", "Bucharest", null,
                        LocalDate.now(ZoneOffset.UTC).plusDays(14)));

        // Text PDF → PDFBox text → stub text-LLM returns deterministic product JSON.
        MockMultipartFile invoice = new MockMultipartFile(
                "file", "factura.pdf", "application/pdf", textPdf("Factura test - Widget A x2"));

        InvoiceSummary uploaded = invoiceService.upload(userId, firm.id(), workOrder.id(), invoice);
        assertEquals(InvoiceProcessingStatus.PENDING, uploaded.processingStatus());

        ExtractionDetail extraction = waitUntilExtracted(userId, firm.id(), workOrder.id(), uploaded.id());

        assertEquals(InvoiceExtractionStatus.READY, extraction.status());
        assertNotNull(extraction.rawJson());
        assertEquals(1, extraction.products().size());
        ExtractionDetail.Product product = extraction.products().getFirst();
        assertEquals("Stub Product", product.name());
        assertEquals("STUB-SKU", product.sku());
        assertEquals(0, product.quantity().compareTo(new BigDecimal("2")));

        // Invoice status mirrors the extraction outcome.
        InvoiceSummary ready = invoiceService.getInvoice(userId, firm.id(), workOrder.id(), uploaded.id());
        assertEquals(InvoiceProcessingStatus.READY, ready.processingStatus());

        Page<InvoiceSummary> page = invoiceService.listInvoices(
                userId, firm.id(), workOrder.id(), PageRequest.of(0, 10));
        assertEquals(1, page.getTotalElements());

        invoiceService.deleteInvoice(userId, firm.id(), workOrder.id(), uploaded.id());
        assertEquals(0, jdbcTemplate.queryForObject(
                "select count(*) from work_order_invoices where work_order_id = ?",
                Integer.class, workOrder.id()));
        // Extraction + line items cascade away with the invoice.
        assertEquals(0, jdbcTemplate.queryForObject(
                "select count(*) from invoice_extractions where invoice_id = ?",
                Integer.class, uploaded.id()));
    }

    private static byte[] textPdf(String text) throws Exception {
        try (org.apache.pdfbox.pdmodel.PDDocument doc = new org.apache.pdfbox.pdmodel.PDDocument()) {
            org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage();
            doc.addPage(page);
            try (org.apache.pdfbox.pdmodel.PDPageContentStream cs =
                         new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new org.apache.pdfbox.pdmodel.font.PDType1Font(
                        org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText(text);
                cs.endText();
            }
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    private ExtractionDetail waitUntilExtracted(UUID userId, UUID firmId, UUID workOrderId, UUID invoiceId)
            throws Exception {
        for (int i = 0; i < 30; i++) {
            structuringService.processPendingBatch(5);
            try {
                ExtractionDetail extraction = invoiceService.getExtraction(userId, firmId, workOrderId, invoiceId);
                if (extraction.status() == InvoiceExtractionStatus.READY) {
                    return extraction;
                }
                if (extraction.status() == InvoiceExtractionStatus.FAILED) {
                    fail("Invoice extraction failed: " + extraction.error());
                }
            } catch (org.springframework.web.server.ResponseStatusException notYet) {
                // extraction row not created yet
            }
            TimeUnit.MILLISECONDS.sleep(100);
        }
        return fail("Invoice extraction did not reach READY status in time");
    }
}
