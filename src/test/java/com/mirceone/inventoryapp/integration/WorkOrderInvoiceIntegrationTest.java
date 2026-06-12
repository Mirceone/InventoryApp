package com.mirceone.inventoryapp.integration;

import com.mirceone.inventoryapp.model.InvoiceExtractionStatus;
import com.mirceone.inventoryapp.model.InvoiceProcessingStatus;
import com.mirceone.inventoryapp.model.UserEntity;
import com.mirceone.inventoryapp.service.workorders.ExtractionDetail;
import com.mirceone.inventoryapp.service.workorders.invoices.extraction.InvoiceStructuringService;
import com.mirceone.inventoryapp.repository.UserRepository;
import com.mirceone.inventoryapp.service.auth.AuthContracts;
import com.mirceone.inventoryapp.service.auth.AuthService;
import com.mirceone.inventoryapp.service.firms.FirmContracts;
import com.mirceone.inventoryapp.service.firms.FirmService;
import com.mirceone.inventoryapp.service.workorders.InvoiceSummary;
import com.mirceone.inventoryapp.service.workorders.WorkOrderContracts;
import com.mirceone.inventoryapp.service.workorders.WorkOrderInvoiceService;
import com.mirceone.inventoryapp.service.workorders.WorkOrderService;
import com.mirceone.inventoryapp.service.workorders.WorkOrderSummary;
import com.mirceone.inventoryapp.service.workorders.invoices.InvoiceProcessingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.charset.StandardCharsets;
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
    private InvoiceProcessingService processingService;
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
    void uploadProcessListAndDeleteInvoiceFlow() throws Exception {
        authService.signup(new AuthContracts.SignupSpec("invoice-it@example.com", "password123", "Invoice IT"));
        UserEntity user = userRepository.findByEmailIgnoreCase("invoice-it@example.com").orElse(null);
        assertNotNull(user);
        UUID userId = user.getId();

        FirmContracts.FirmSummary firm =
                firmService.createFirm(userId, new FirmContracts.CreateFirmSpec("Firm Invoices"));

        WorkOrderSummary workOrder = workOrderService.createWorkOrder(
                userId,
                firm.id(),
                new WorkOrderContracts.CreateWorkOrderSpec(
                        "Renovare",
                        "Client",
                        "Bucharest",
                        null,
                        LocalDate.now(ZoneOffset.UTC).plusDays(14)
                )
        );

        MockMultipartFile pdf = new MockMultipartFile(
                "file", "factura.pdf", "application/pdf", "%PDF-1.4".getBytes(StandardCharsets.UTF_8));

        InvoiceSummary uploaded = invoiceService.upload(userId, firm.id(), workOrder.id(), pdf);
        assertEquals(InvoiceProcessingStatus.PENDING, uploaded.processingStatus());
        assertNull(uploaded.markdownText());

        waitUntilReady(uploaded.id(), firm.id(), workOrder.id(), userId);

        InvoiceSummary ready = invoiceService.getInvoice(userId, firm.id(), workOrder.id(), uploaded.id());
        assertEquals(InvoiceProcessingStatus.READY, ready.processingStatus());
        assertNotNull(ready.markdownText());
        assertTrue(ready.markdownText().contains("Invoice stub"));

        Page<InvoiceSummary> page = invoiceService.listInvoices(
                userId, firm.id(), workOrder.id(), PageRequest.of(0, 10));
        assertEquals(1, page.getTotalElements());
        assertNull(page.getContent().getFirst().markdownText());

        Integer opaqueKeys = jdbcTemplate.queryForObject(
                "select count(*) from work_order_invoices where storage_key like ?",
                Integer.class,
                firm.id() + "/" + workOrder.id() + "/invoices/%"
        );
        assertEquals(1, opaqueKeys);

        invoiceService.deleteInvoice(userId, firm.id(), workOrder.id(), uploaded.id());
        assertEquals(0, jdbcTemplate.queryForObject(
                "select count(*) from work_order_invoices where work_order_id = ?",
                Integer.class,
                workOrder.id()
        ));
    }

    @Test
    void structuresLineItemsFromReadyInvoice() throws Exception {
        authService.signup(new AuthContracts.SignupSpec("invoice-extract@example.com", "password123", "Extract IT"));
        UUID userId = userRepository.findByEmailIgnoreCase("invoice-extract@example.com").orElseThrow().getId();

        FirmContracts.FirmSummary firm =
                firmService.createFirm(userId, new FirmContracts.CreateFirmSpec("Firm Extract"));
        WorkOrderSummary workOrder = workOrderService.createWorkOrder(
                userId, firm.id(),
                new WorkOrderContracts.CreateWorkOrderSpec(
                        "Renovare", "Client", "Bucharest", null,
                        LocalDate.now(ZoneOffset.UTC).plusDays(14)));

        MockMultipartFile pdf = new MockMultipartFile(
                "file", "factura.pdf", "application/pdf", "%PDF-1.4".getBytes(StandardCharsets.UTF_8));
        InvoiceSummary uploaded = invoiceService.upload(userId, firm.id(), workOrder.id(), pdf);

        waitUntilReady(uploaded.id(), firm.id(), workOrder.id(), userId);

        // The READY invoice enqueues a PENDING extraction; drive the structuring worker to completion.
        ExtractionDetail extraction = waitUntilExtracted(userId, firm.id(), workOrder.id(), uploaded.id());

        assertEquals(InvoiceExtractionStatus.READY, extraction.status());
        assertEquals("Stub Supplier SRL", extraction.supplierName());
        assertEquals(1, extraction.lineItems().size());
        ExtractionDetail.Line line = extraction.lineItems().getFirst();
        assertEquals("Stub Product", line.rawDescription());
        assertEquals("STUB-SKU", line.sku());
        assertEquals(0, line.quantity().compareTo(new java.math.BigDecimal("2")));
        assertEquals(0, line.unitPrice().compareTo(new java.math.BigDecimal("50.00")));
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

    private void waitUntilReady(UUID invoiceId, UUID firmId, UUID workOrderId, UUID userId) throws Exception {
        for (int i = 0; i < 30; i++) {
            processingService.processPendingBatch(5);
            InvoiceSummary current = invoiceService.getInvoice(userId, firmId, workOrderId, invoiceId);
            if (current.processingStatus() == InvoiceProcessingStatus.READY) {
                return;
            }
            if (current.processingStatus() == InvoiceProcessingStatus.FAILED) {
                fail("Invoice processing failed: " + current.processingError());
            }
            TimeUnit.MILLISECONDS.sleep(100);
        }
        fail("Invoice did not reach READY status in time");
    }
}
