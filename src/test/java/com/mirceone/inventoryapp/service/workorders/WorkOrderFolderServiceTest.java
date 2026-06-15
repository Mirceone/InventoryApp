package com.mirceone.inventoryapp.service.workorders;

import com.mirceone.inventoryapp.model.WorkOrderFileEntity;
import com.mirceone.inventoryapp.model.WorkOrderFolderEntity;
import com.mirceone.inventoryapp.model.WorkOrderFolderRuleEntity;
import com.mirceone.inventoryapp.repository.WorkOrderFileRepository;
import com.mirceone.inventoryapp.repository.WorkOrderFolderRepository;
import com.mirceone.inventoryapp.repository.WorkOrderFolderRuleRepository;
import com.mirceone.inventoryapp.service.firms.access.FirmAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkOrderFolderServiceTest {

    @Mock
    private FirmAccessService firmAccessService;
    @Mock
    private WorkOrderService workOrderService;
    @Mock
    private WorkOrderFolderRepository folderRepository;
    @Mock
    private WorkOrderFolderRuleRepository ruleRepository;
    @Mock
    private WorkOrderFileRepository fileRepository;

    private WorkOrderFolderService folderService;

    private UUID userId;
    private UUID firmId;
    private UUID workOrderId;
    private WorkOrderFolderEntity catchAll;
    private WorkOrderFolderEntity documents;

    @BeforeEach
    void setUp() {
        folderService = new WorkOrderFolderService(
                firmAccessService,
                workOrderService,
                folderRepository,
                ruleRepository,
                fileRepository,
                new DisplayNameDeduplicator(fileRepository)
        );
        userId = UUID.randomUUID();
        firmId = UUID.randomUUID();
        workOrderId = UUID.randomUUID();
        documents = new WorkOrderFolderEntity(workOrderId, null, "Documents", false, 0);
        catchAll = new WorkOrderFolderEntity(workOrderId, null, "Misc", true, 1);
        stubFolders(documents, catchAll);
    }

    private void stubFolders(WorkOrderFolderEntity... folders) {
        when(folderRepository.findAllByWorkOrderIdOrderBySortOrderAscNameAsc(workOrderId))
                .thenReturn(List.of(folders));
        for (WorkOrderFolderEntity folder : folders) {
            when(folderRepository.findByIdAndWorkOrderId(folder.getId(), workOrderId))
                    .thenReturn(Optional.of(folder));
        }
        when(folderRepository.findByWorkOrderIdAndCatchAllTrue(workOrderId)).thenReturn(Optional.of(catchAll));
    }

    @Test
    void getFolderTreeReturnsRootsWithCatchAllFlag() {
        List<FolderTreeNode> tree = folderService.getFolderTree(userId, firmId, workOrderId);

        assertEquals(2, tree.size());
        assertEquals(1, tree.stream().filter(FolderTreeNode::catchAll).count());
        assertEquals("Documents", tree.getFirst().name());
    }

    @Test
    void createFolderRejectsInvalidName() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> folderService.createFolder(userId, firmId, workOrderId,
                        new WorkOrderContracts.CreateFolderSpec(null, "bad/name", List.of()))
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void createFolderRejectsDuplicateSiblingName() {
        when(folderRepository.existsByWorkOrderIdAndParentIdIsNullAndNameIgnoreCase(workOrderId, "Documents"))
                .thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> folderService.createFolder(userId, firmId, workOrderId,
                        new WorkOrderContracts.CreateFolderSpec(null, "Documents", List.of()))
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void createFolderRejectsExtensionClaimedByAnotherFolder() {
        when(ruleRepository.findByWorkOrderIdAndExtension(workOrderId, "pdf"))
                .thenReturn(Optional.of(new WorkOrderFolderRuleEntity(workOrderId, documents.getId(), "pdf")));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> folderService.createFolder(userId, firmId, workOrderId,
                        new WorkOrderContracts.CreateFolderSpec(null, "Plans", List.of("pdf")))
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void createFolderRejectsTooDeepNesting() {
        WorkOrderFolderEntity level2 = new WorkOrderFolderEntity(workOrderId, documents.getId(), "Level2", false, 0);
        WorkOrderFolderEntity level3 = new WorkOrderFolderEntity(workOrderId, level2.getId(), "Level3", false, 0);
        stubFolders(documents, catchAll, level2, level3);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> folderService.createFolder(userId, firmId, workOrderId,
                        new WorkOrderContracts.CreateFolderSpec(level3.getId(), "Level4", List.of()))
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void createFolderPersistsFolderAndRules() {
        when(folderRepository.save(any(WorkOrderFolderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        FolderTreeNode created = folderService.createFolder(userId, firmId, workOrderId,
                new WorkOrderContracts.CreateFolderSpec(null, "Plans", List.of(".DWG", "pdf")));

        assertEquals("Plans", created.name());
        assertFalse(created.catchAll());
        verify(folderRepository).save(any(WorkOrderFolderEntity.class));
        verify(ruleRepository).saveAll(org.mockito.ArgumentMatchers.<List<WorkOrderFolderRuleEntity>>argThat(rules ->
                rules.size() == 2 && rules.stream().anyMatch(r -> r.getExtension().equals("dwg"))));
    }

    @Test
    void updateFolderRejectsMoveIntoOwnDescendant() {
        WorkOrderFolderEntity child = new WorkOrderFolderEntity(workOrderId, documents.getId(), "Child", false, 0);
        stubFolders(documents, catchAll, child);

        var spec = new WorkOrderContracts.UpdateFolderSpec(null, child.getId(), true);
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> folderService.updateFolder(userId, firmId, workOrderId, documents.getId(), spec)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void updateFolderRejectsMovingCatchAll() {
        var spec = new WorkOrderContracts.UpdateFolderSpec(null, documents.getId(), true);
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> folderService.updateFolder(userId, firmId, workOrderId, catchAll.getId(), spec)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void deleteCatchAllFolderRejected() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> folderService.deleteFolder(userId, firmId, workOrderId, catchAll.getId(), false)
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void deleteNonEmptyFolderWithoutMoveRejected() {
        when(fileRepository.existsByFolderIdIn(List.of(documents.getId()))).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> folderService.deleteFolder(userId, firmId, workOrderId, documents.getId(), false)
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void deleteFolderWithMoveReassignsFilesToCatchAll() {
        WorkOrderFileEntity file = new WorkOrderFileEntity(
                UUID.randomUUID(), firmId, workOrderId, documents.getId(), userId,
                "plan.pdf", "pdf", "application/pdf", 10, null, "key-1.pdf",
                com.mirceone.inventoryapp.model.FileClassificationStatus.CLASSIFIED,
                com.mirceone.inventoryapp.model.FileClassificationSource.RULE);
        when(fileRepository.existsByFolderIdIn(List.of(documents.getId()))).thenReturn(true);
        when(fileRepository.findAllByFolderIdIn(List.of(documents.getId()))).thenReturn(List.of(file));
        when(fileRepository.existsByFolderIdAndDisplayNameIgnoreCase(catchAll.getId(), "plan.pdf")).thenReturn(false);
        when(fileRepository.saveAndFlush(any(WorkOrderFileEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        folderService.deleteFolder(userId, firmId, workOrderId, documents.getId(), true);

        assertEquals(catchAll.getId(), file.getFolderId());
        verify(folderRepository).deleteById(documents.getId());
    }

    @Test
    void replaceRulesOnCatchAllRejected() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> folderService.replaceRules(userId, firmId, workOrderId, catchAll.getId(), List.of("pdf"))
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void replaceRulesSwapsRuleSet() {
        when(ruleRepository.findByWorkOrderIdAndExtension(workOrderId, "png")).thenReturn(Optional.empty());

        FolderTreeNode node = folderService.replaceRules(userId, firmId, workOrderId, documents.getId(), List.of("PNG"));

        verify(ruleRepository).deleteByFolderId(documents.getId());
        verify(ruleRepository).saveAll(org.mockito.ArgumentMatchers.<List<WorkOrderFolderRuleEntity>>argThat(rules ->
                rules.size() == 1 && rules.getFirst().getExtension().equals("png")));
        assertEquals(documents.getId(), node.id());
    }
}
