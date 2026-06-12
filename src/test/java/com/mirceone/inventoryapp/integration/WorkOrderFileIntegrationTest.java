package com.mirceone.inventoryapp.integration;

import com.mirceone.inventoryapp.model.UserEntity;
import com.mirceone.inventoryapp.repository.UserRepository;
import com.mirceone.inventoryapp.service.auth.AuthContracts;
import com.mirceone.inventoryapp.service.auth.AuthService;
import com.mirceone.inventoryapp.service.firms.FirmContracts;
import com.mirceone.inventoryapp.service.firms.FirmService;
import com.mirceone.inventoryapp.service.workorders.BatchUploadResult;
import com.mirceone.inventoryapp.service.workorders.FileDownload;
import com.mirceone.inventoryapp.service.workorders.FileSummary;
import com.mirceone.inventoryapp.service.workorders.FolderTreeNode;
import com.mirceone.inventoryapp.service.workorders.WorkOrderContracts;
import com.mirceone.inventoryapp.service.workorders.WorkOrderFileService;
import com.mirceone.inventoryapp.service.workorders.WorkOrderFolderService;
import com.mirceone.inventoryapp.service.workorders.WorkOrderService;
import com.mirceone.inventoryapp.service.workorders.WorkOrderSummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.StreamUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WorkOrderFileIntegrationTest extends IntegrationTestBase {

    @Autowired
    private AuthService authService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FirmService firmService;
    @Autowired
    private WorkOrderService workOrderService;
    @Autowired
    private WorkOrderFolderService folderService;
    @Autowired
    private WorkOrderFileService fileService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @org.junit.jupiter.api.io.TempDir
    static Path storageRoot;

    @DynamicPropertySource
    static void storageProps(DynamicPropertyRegistry registry) {
        registry.add("app.storage.root", () -> storageRoot.toAbsolutePath().toString());
    }

    @Test
    void uploadClassifyMoveAndDeleteFolderFlow() throws Exception {
        authService.signup(new AuthContracts.SignupSpec("work-order-it@example.com", "password123", "Work Order IT"));
        UserEntity user = userRepository.findByEmailIgnoreCase("work-order-it@example.com").orElse(null);
        assertNotNull(user);
        UUID userId = user.getId();

        FirmContracts.FirmSummary firm =
                firmService.createFirm(userId, new FirmContracts.CreateFirmSpec("Firm Work Orders"));

        WorkOrderSummary workOrder = workOrderService.createWorkOrder(
                userId,
                firm.id(),
                new WorkOrderContracts.CreateWorkOrderSpec(
                        "Proiect Renovare",
                        "Client Test",
                        "Bucharest",
                        null,
                        LocalDate.now(ZoneOffset.UTC).plusDays(30)
                )
        );

        // Default template seeded: one catch-all + one regular folder
        List<FolderTreeNode> tree = folderService.getFolderTree(userId, firm.id(), workOrder.id());
        assertEquals(2, tree.size());
        FolderTreeNode catchAll = tree.stream().filter(FolderTreeNode::catchAll).findFirst().orElseThrow();

        // User-defined folder with a png rule
        FolderTreeNode poze = folderService.createFolder(userId, firm.id(), workOrder.id(),
                new WorkOrderContracts.CreateFolderSpec(null, "Poze", List.of("png", "jpg")));

        MockMultipartFile png = new MockMultipartFile(
                "files", "photo.png", "image/png", "png-bytes".getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile pngDuplicate = new MockMultipartFile(
                "files", "photo.png", "image/png", "png-bytes-2".getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile pdf = new MockMultipartFile(
                "files", "spec.pdf", "application/pdf", "%PDF".getBytes(StandardCharsets.UTF_8)
        );

        BatchUploadResult batch = fileService.uploadBatch(
                userId, firm.id(), workOrder.id(), new MockMultipartFile[]{png, pngDuplicate, pdf});
        assertEquals(3, batch.accepted().size());
        assertEquals(0, batch.errors().size());

        // png files classified by rule; pdf has no rule -> catch-all
        assertEquals(2, batch.accepted().stream().filter(i -> i.folderId().equals(poze.id())).count());
        assertEquals(1, batch.accepted().stream().filter(i -> i.folderId().equals(catchAll.id())).count());

        // Duplicate display name got a suffix
        Page<FileSummary> pozeFiles = fileService.listFiles(
                userId, firm.id(), workOrder.id(), poze.id(), PageRequest.of(0, 10));
        assertEquals(2, pozeFiles.getTotalElements());
        assertTrue(pozeFiles.getContent().stream().anyMatch(f -> f.displayName().equals("photo.png")));
        assertTrue(pozeFiles.getContent().stream().anyMatch(f -> f.displayName().equals("photo (1).png")));
        assertTrue(pozeFiles.getContent().stream().allMatch(f -> "Poze".equals(f.folderPath())));

        // Download round-trip
        FileSummary original = pozeFiles.getContent().stream()
                .filter(f -> f.displayName().equals("photo.png"))
                .findFirst().orElseThrow();
        FileDownload dl = fileService.openForDownload(userId, firm.id(), workOrder.id(), original.id());
        assertEquals("png-bytes", StreamUtils.copyToString(dl.resource().getInputStream(), StandardCharsets.UTF_8));

        // Storage keys are opaque (firmId/workOrderId/fileId.ext), no folder names on disk
        Integer opaqueKeys = jdbcTemplate.queryForObject(
                "select count(*) from work_order_files where storage_key like ?",
                Integer.class,
                firm.id() + "/" + workOrder.id() + "/%"
        );
        assertEquals(3, opaqueKeys);
        Integer keysWithFolderNames = jdbcTemplate.queryForObject(
                "select count(*) from work_order_files where storage_key like '%Poze%'",
                Integer.class
        );
        assertEquals(0, keysWithFolderNames);

        // Manual move is a pure DB update
        FileSummary moved = fileService.updateFile(userId, firm.id(), workOrder.id(), original.id(),
                new WorkOrderContracts.UpdateFileSpec(null, catchAll.id()));
        assertEquals(catchAll.id(), moved.folderId());

        // Non-empty folder cannot be deleted without reassignment...
        ResponseStatusException conflict = assertThrows(ResponseStatusException.class,
                () -> folderService.deleteFolder(userId, firm.id(), workOrder.id(), poze.id(), false));
        assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode());

        // ...but works with moveFilesTo=catchAll
        folderService.deleteFolder(userId, firm.id(), workOrder.id(), poze.id(), true);
        Page<FileSummary> catchAllFiles = fileService.listFiles(
                userId, firm.id(), workOrder.id(), catchAll.id(), PageRequest.of(0, 10));
        assertEquals(3, catchAllFiles.getTotalElements());

        // Deleting the work order removes everything
        workOrderService.deleteWorkOrder(userId, firm.id(), workOrder.id());
        Integer remainingFiles = jdbcTemplate.queryForObject(
                "select count(*) from work_order_files where work_order_id = ?",
                Integer.class,
                workOrder.id()
        );
        Integer remainingFolders = jdbcTemplate.queryForObject(
                "select count(*) from work_order_folders where work_order_id = ?",
                Integer.class,
                workOrder.id()
        );
        assertEquals(0, remainingFiles);
        assertEquals(0, remainingFolders);
    }
}
