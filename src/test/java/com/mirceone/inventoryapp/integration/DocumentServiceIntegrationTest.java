package com.mirceone.inventoryapp.integration;

import com.mirceone.inventoryapp.model.DocumentProcessingStatus;
import com.mirceone.inventoryapp.model.UserEntity;
import com.mirceone.inventoryapp.repository.UserRepository;
import com.mirceone.inventoryapp.service.auth.AuthContracts;
import com.mirceone.inventoryapp.service.auth.AuthService;
import com.mirceone.inventoryapp.service.documents.*;
import com.mirceone.inventoryapp.service.firms.FirmContracts;
import com.mirceone.inventoryapp.service.firms.FirmService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DocumentServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private AuthService authService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FirmService firmService;
    @Autowired
    private DossierService dossierService;
    @Autowired
    private DocumentService documentService;
    @Autowired
    private DocumentOrganizationService documentOrganizationService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @org.junit.jupiter.api.io.TempDir
    static Path storageRoot;

    @DynamicPropertySource
    static void storageProps(DynamicPropertyRegistry registry) {
        registry.add("app.storage.root", () -> storageRoot.toAbsolutePath().toString());
    }

    @Test
    void createDossierUploadOrganizeAndList() throws Exception {
        authService.signup(new AuthContracts.SignupSpec("dossier-it@example.com", "password123", "Dossier IT"));
        UserEntity user = userRepository.findByEmailIgnoreCase("dossier-it@example.com").orElse(null);
        assertNotNull(user);
        UUID userId = user.getId();

        FirmContracts.FirmSummary firm =
                firmService.createFirm(userId, new FirmContracts.CreateFirmSpec("Firm Dossier"));

        DossierSummary dossier = dossierService.createDossier(userId, firm.id(), "Proiect Renovare");

        MockMultipartFile png = new MockMultipartFile(
                "files", "photo.png", "image/png", "png-bytes".getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile pdf = new MockMultipartFile(
                "files", "spec.pdf", "application/pdf", "%PDF".getBytes(StandardCharsets.UTF_8)
        );

        BatchUploadResult batch = documentService.uploadBatch(userId, firm.id(), dossier.id(), new MockMultipartFile[]{png, pdf});
        assertEquals(2, batch.accepted().size());

        for (var item : batch.accepted()) {
            documentOrganizationService.organizeDocument(item.id());
        }

        Page<DocumentSummary> poze = documentService.listDocuments(
                userId, firm.id(), dossier.id(), "Poze", PageRequest.of(0, 10)
        );
        assertEquals(1, poze.getTotalElements());
        assertEquals(dossier.id(), poze.getContent().getFirst().dossierId());

        List<String> folders = documentService.listFolders(userId, firm.id(), dossier.id());
        assertTrue(folders.contains("Poze"));
        assertTrue(folders.contains("Documente"));

        var structure = documentService.listFolderStructure(userId, firm.id(), dossier.id());
        assertEquals(5, structure.size());
        assertTrue(structure.stream().anyMatch(e -> "Poze".equals(e.path()) && e.documentCount() == 1));
        assertTrue(structure.stream().anyMatch(e -> "Documente".equals(e.path()) && e.documentCount() == 1));
        assertTrue(structure.stream().anyMatch(e -> "Facturi".equals(e.path()) && e.documentCount() == 0));

        DocumentDownload dl = documentService.openForDownload(
                userId, firm.id(), dossier.id(), poze.getContent().getFirst().id()
        );
        String text = StreamUtils.copyToString(dl.resource().getInputStream(), StandardCharsets.UTF_8);
        assertEquals("png-bytes", text);

        Integer linked = jdbcTemplate.queryForObject(
                "select count(*) from firm_documents where dossier_id = ? and processing_status = 'CLASSIFIED'",
                Integer.class,
                dossier.id()
        );
        assertEquals(2, linked);
    }
}
