package com.mirceone.inventoryapp.service.workorders;

import com.mirceone.inventoryapp.model.WorkOrderFileEntity;
import com.mirceone.inventoryapp.model.WorkOrderFolderEntity;
import com.mirceone.inventoryapp.model.WorkOrderFolderRuleEntity;
import com.mirceone.inventoryapp.repository.WorkOrderFileRepository;
import com.mirceone.inventoryapp.repository.WorkOrderFolderRepository;
import com.mirceone.inventoryapp.repository.WorkOrderFolderRuleRepository;
import com.mirceone.inventoryapp.service.firms.access.FirmAccessService;
import com.mirceone.inventoryapp.service.firms.access.FirmPermission;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

/**
 * Manages the virtual folder tree of a work order. All operations are pure DB updates;
 * blobs are never touched because storage keys do not encode structure.
 */
@Service
public class WorkOrderFolderService {

    public static final int MAX_DEPTH = 3;

    private final FirmAccessService firmAccessService;
    private final WorkOrderService workOrderService;
    private final WorkOrderFolderRepository folderRepository;
    private final WorkOrderFolderRuleRepository ruleRepository;
    private final WorkOrderFileRepository fileRepository;
    private final DisplayNameDeduplicator displayNameDeduplicator;

    public WorkOrderFolderService(
            FirmAccessService firmAccessService,
            WorkOrderService workOrderService,
            WorkOrderFolderRepository folderRepository,
            WorkOrderFolderRuleRepository ruleRepository,
            WorkOrderFileRepository fileRepository,
            DisplayNameDeduplicator displayNameDeduplicator
    ) {
        this.firmAccessService = firmAccessService;
        this.workOrderService = workOrderService;
        this.folderRepository = folderRepository;
        this.ruleRepository = ruleRepository;
        this.fileRepository = fileRepository;
        this.displayNameDeduplicator = displayNameDeduplicator;
    }

    @Transactional(readOnly = true)
    public List<FolderTreeNode> getFolderTree(UUID userId, UUID firmId, UUID workOrderId) {
        requireAccess(userId, firmId, workOrderId);
        return buildTree(workOrderId);
    }

    @Transactional
    public FolderTreeNode createFolder(UUID userId, UUID firmId, UUID workOrderId, WorkOrderContracts.CreateFolderSpec spec) {
        requireAccess(userId, firmId, workOrderId);

        String name = sanitizeName(spec.name());
        List<WorkOrderFolderEntity> all = folderRepository.findAllByWorkOrderIdOrderBySortOrderAscNameAsc(workOrderId);

        WorkOrderFolderEntity parent = null;
        if (spec.parentId() != null) {
            parent = requireFolder(workOrderId, spec.parentId());
            if (depthOf(parent, all) + 1 > MAX_DEPTH) {
                throw new ResponseStatusException(BAD_REQUEST, "Folder tree too deep (max " + MAX_DEPTH + " levels)");
            }
        }
        assertSiblingNameAvailable(workOrderId, spec.parentId(), name, null);

        List<String> extensions = normalizeExtensions(spec.extensions());
        assertExtensionsAvailable(workOrderId, extensions, null);

        WorkOrderFolderEntity folder = new WorkOrderFolderEntity(
                workOrderId,
                parent != null ? parent.getId() : null,
                name,
                false,
                nextSortOrder(all, spec.parentId())
        );
        try {
            folderRepository.save(folder);
            ruleRepository.saveAll(extensions.stream()
                    .map(ext -> new WorkOrderFolderRuleEntity(workOrderId, folder.getId(), ext))
                    .toList());
            ruleRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(CONFLICT, "Folder name or extension already in use", ex);
        }
        return toNode(folder, workOrderId);
    }

    @Transactional
    public FolderTreeNode updateFolder(
            UUID userId,
            UUID firmId,
            UUID workOrderId,
            UUID folderId,
            WorkOrderContracts.UpdateFolderSpec spec
    ) {
        requireAccess(userId, firmId, workOrderId);
        WorkOrderFolderEntity folder = requireFolder(workOrderId, folderId);

        if (spec.name() == null && !spec.parentPresent()) {
            throw new ResponseStatusException(BAD_REQUEST, "No fields to update");
        }

        List<WorkOrderFolderEntity> all = folderRepository.findAllByWorkOrderIdOrderBySortOrderAscNameAsc(workOrderId);
        UUID targetParentId = spec.parentPresent() ? spec.parentId() : folder.getParentId();

        if (spec.parentPresent() && !Objects.equals(spec.parentId(), folder.getParentId())) {
            if (folder.isCatchAll()) {
                throw new ResponseStatusException(BAD_REQUEST, "The catch-all folder cannot be moved");
            }
            if (spec.parentId() != null) {
                WorkOrderFolderEntity newParent = requireFolder(workOrderId, spec.parentId());
                Set<UUID> subtree = subtreeIds(folder.getId(), all);
                if (subtree.contains(newParent.getId())) {
                    throw new ResponseStatusException(BAD_REQUEST, "Cannot move a folder into itself or its descendants");
                }
                int subtreeHeight = subtreeHeight(folder.getId(), all);
                if (depthOf(newParent, all) + subtreeHeight > MAX_DEPTH) {
                    throw new ResponseStatusException(BAD_REQUEST, "Folder tree too deep (max " + MAX_DEPTH + " levels)");
                }
            }
            folder.setParentId(spec.parentId());
        }

        String targetName = spec.name() != null ? sanitizeName(spec.name()) : folder.getName();
        if (spec.name() != null || spec.parentPresent()) {
            assertSiblingNameAvailable(workOrderId, targetParentId, targetName, folder.getId());
            folder.setName(targetName);
        }

        try {
            folderRepository.saveAndFlush(folder);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(CONFLICT, "A folder with this name already exists here", ex);
        }
        return toNode(folder, workOrderId);
    }

    @Transactional
    public void deleteFolder(
            UUID userId,
            UUID firmId,
            UUID workOrderId,
            UUID folderId,
            boolean moveFilesToCatchAll
    ) {
        requireAccess(userId, firmId, workOrderId);
        WorkOrderFolderEntity folder = requireFolder(workOrderId, folderId);
        if (folder.isCatchAll()) {
            throw new ResponseStatusException(CONFLICT, "The catch-all folder cannot be deleted");
        }

        List<WorkOrderFolderEntity> all = folderRepository.findAllByWorkOrderIdOrderBySortOrderAscNameAsc(workOrderId);
        List<UUID> subtree = new ArrayList<>(subtreeIds(folder.getId(), all));

        if (fileRepository.existsByFolderIdIn(subtree)) {
            if (!moveFilesToCatchAll) {
                throw new ResponseStatusException(CONFLICT,
                        "Folder is not empty; pass moveFilesTo=catchAll to reassign its files");
            }
            WorkOrderFolderEntity catchAll = requireCatchAll(workOrderId);
            for (WorkOrderFileEntity file : fileRepository.findAllByFolderIdIn(subtree)) {
                file.setDisplayName(displayNameDeduplicator.uniqueName(catchAll.getId(), file.getDisplayName()));
                file.setFolderId(catchAll.getId());
                fileRepository.saveAndFlush(file);
            }
        }

        // Children and rules cascade at the DB level (parent_id / folder_id FKs).
        try {
            folderRepository.deleteById(folder.getId());
            folderRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            // Concurrent upload won the race: a file still references the subtree.
            throw new ResponseStatusException(CONFLICT, "Folder is not empty", ex);
        }
    }

    @Transactional
    public FolderTreeNode replaceRules(
            UUID userId,
            UUID firmId,
            UUID workOrderId,
            UUID folderId,
            List<String> extensions
    ) {
        requireAccess(userId, firmId, workOrderId);
        WorkOrderFolderEntity folder = requireFolder(workOrderId, folderId);
        if (folder.isCatchAll()) {
            throw new ResponseStatusException(BAD_REQUEST, "The catch-all folder cannot have extension rules");
        }

        List<String> normalized = normalizeExtensions(extensions);
        assertExtensionsAvailable(workOrderId, normalized, folder.getId());

        ruleRepository.deleteByFolderId(folder.getId());
        ruleRepository.flush();
        try {
            ruleRepository.saveAll(normalized.stream()
                    .map(ext -> new WorkOrderFolderRuleEntity(workOrderId, folder.getId(), ext))
                    .toList());
            ruleRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(CONFLICT, "An extension is already mapped to another folder", ex);
        }
        return toNode(folder, workOrderId);
    }

    public WorkOrderFolderEntity requireFolder(UUID workOrderId, UUID folderId) {
        return folderRepository.findByIdAndWorkOrderId(folderId, workOrderId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Folder not found"));
    }

    private WorkOrderFolderEntity requireCatchAll(UUID workOrderId) {
        return folderRepository.findByWorkOrderIdAndCatchAllTrue(workOrderId)
                .orElseThrow(() -> new IllegalStateException("Work order " + workOrderId + " has no catch-all folder"));
    }

    private void requireAccess(UUID userId, UUID firmId, UUID workOrderId) {
        workOrderService.assertWorkOrderEnabled();
        firmAccessService.requireOperationalPermission(firmId, userId, FirmPermission.DOCUMENT_WRITE);
        workOrderService.requireWorkOrder(firmId, workOrderId);
    }

    private List<FolderTreeNode> buildTree(UUID workOrderId) {
        List<WorkOrderFolderEntity> folders = folderRepository.findAllByWorkOrderIdOrderBySortOrderAscNameAsc(workOrderId);
        Map<UUID, String> paths = FolderPaths.buildPathMap(folders);
        Map<UUID, List<String>> extensionsByFolder = extensionsByFolder(workOrderId);
        Map<UUID, Long> fileCounts = new HashMap<>();
        for (Object[] row : fileRepository.countByFolderForWorkOrder(workOrderId)) {
            fileCounts.put((UUID) row[0], (Long) row[1]);
        }

        Map<UUID, List<WorkOrderFolderEntity>> childrenByParent = new HashMap<>();
        for (WorkOrderFolderEntity folder : folders) {
            childrenByParent.computeIfAbsent(folder.getParentId(), k -> new ArrayList<>()).add(folder);
        }
        return assembleNodes(childrenByParent.get(null), childrenByParent, paths, extensionsByFolder, fileCounts);
    }

    private List<FolderTreeNode> assembleNodes(
            List<WorkOrderFolderEntity> level,
            Map<UUID, List<WorkOrderFolderEntity>> childrenByParent,
            Map<UUID, String> paths,
            Map<UUID, List<String>> extensionsByFolder,
            Map<UUID, Long> fileCounts
    ) {
        if (level == null) {
            return List.of();
        }
        return level.stream()
                .map(folder -> new FolderTreeNode(
                        folder.getId(),
                        folder.getName(),
                        paths.get(folder.getId()),
                        folder.isCatchAll(),
                        fileCounts.getOrDefault(folder.getId(), 0L),
                        extensionsByFolder.getOrDefault(folder.getId(), List.of()),
                        assembleNodes(childrenByParent.get(folder.getId()), childrenByParent, paths, extensionsByFolder, fileCounts)
                ))
                .toList();
    }

    private FolderTreeNode toNode(WorkOrderFolderEntity folder, UUID workOrderId) {
        List<WorkOrderFolderEntity> all = folderRepository.findAllByWorkOrderIdOrderBySortOrderAscNameAsc(workOrderId);
        Map<UUID, String> paths = FolderPaths.buildPathMap(all);
        List<String> extensions = ruleRepository.findAllByFolderId(folder.getId()).stream()
                .map(WorkOrderFolderRuleEntity::getExtension)
                .sorted()
                .toList();
        long fileCount = fileRepository.countByFolderId(folder.getId());
        return new FolderTreeNode(
                folder.getId(),
                folder.getName(),
                paths.get(folder.getId()),
                folder.isCatchAll(),
                fileCount,
                extensions,
                List.of()
        );
    }

    private Map<UUID, List<String>> extensionsByFolder(UUID workOrderId) {
        Map<UUID, List<String>> result = new HashMap<>();
        for (WorkOrderFolderRuleEntity rule : ruleRepository.findAllByWorkOrderId(workOrderId)) {
            result.computeIfAbsent(rule.getFolderId(), k -> new ArrayList<>()).add(rule.getExtension());
        }
        result.values().forEach(list -> list.sort(Comparator.naturalOrder()));
        return result;
    }

    private String sanitizeName(String raw) {
        try {
            return FolderNameValidator.sanitize(raw);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(BAD_REQUEST, ex.getMessage());
        }
    }

    private List<String> normalizeExtensions(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String ext : raw) {
            try {
                normalized.add(ExtensionNormalizer.normalizeRuleExtension(ext));
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(BAD_REQUEST, ex.getMessage());
            }
        }
        return List.copyOf(normalized);
    }

    private void assertExtensionsAvailable(UUID workOrderId, List<String> extensions, UUID exceptFolderId) {
        for (String extension : extensions) {
            ruleRepository.findByWorkOrderIdAndExtension(workOrderId, extension).ifPresent(rule -> {
                if (!rule.getFolderId().equals(exceptFolderId)) {
                    throw new ResponseStatusException(CONFLICT,
                            "Extension '" + extension + "' is already mapped to another folder");
                }
            });
        }
    }

    private void assertSiblingNameAvailable(UUID workOrderId, UUID parentId, String name, UUID exceptFolderId) {
        boolean taken = parentId == null
                ? folderRepository.existsByWorkOrderIdAndParentIdIsNullAndNameIgnoreCase(workOrderId, name)
                : folderRepository.existsByWorkOrderIdAndParentIdAndNameIgnoreCase(workOrderId, parentId, name);
        if (taken && conflictIsNotSelf(workOrderId, parentId, name, exceptFolderId)) {
            throw new ResponseStatusException(CONFLICT, "A folder with this name already exists here");
        }
    }

    private boolean conflictIsNotSelf(UUID workOrderId, UUID parentId, String name, UUID exceptFolderId) {
        if (exceptFolderId == null) {
            return true;
        }
        return folderRepository.findAllByWorkOrderIdOrderBySortOrderAscNameAsc(workOrderId).stream()
                .anyMatch(f -> Objects.equals(f.getParentId(), parentId)
                        && f.getName().equalsIgnoreCase(name)
                        && !f.getId().equals(exceptFolderId));
    }

    private int nextSortOrder(List<WorkOrderFolderEntity> all, UUID parentId) {
        return all.stream()
                .filter(f -> Objects.equals(f.getParentId(), parentId))
                .mapToInt(WorkOrderFolderEntity::getSortOrder)
                .max()
                .orElse(-1) + 1;
    }

    private int depthOf(WorkOrderFolderEntity folder, List<WorkOrderFolderEntity> all) {
        Map<UUID, WorkOrderFolderEntity> byId = new HashMap<>();
        for (WorkOrderFolderEntity f : all) {
            byId.put(f.getId(), f);
        }
        int depth = 1;
        WorkOrderFolderEntity current = folder;
        while (current.getParentId() != null) {
            current = byId.get(current.getParentId());
            if (current == null) {
                break;
            }
            depth++;
        }
        return depth;
    }

    private Set<UUID> subtreeIds(UUID rootId, List<WorkOrderFolderEntity> all) {
        Map<UUID, List<UUID>> childrenByParent = new HashMap<>();
        for (WorkOrderFolderEntity folder : all) {
            if (folder.getParentId() != null) {
                childrenByParent.computeIfAbsent(folder.getParentId(), k -> new ArrayList<>()).add(folder.getId());
            }
        }
        Set<UUID> result = new HashSet<>();
        Deque<UUID> queue = new ArrayDeque<>();
        queue.add(rootId);
        while (!queue.isEmpty()) {
            UUID current = queue.poll();
            if (result.add(current)) {
                queue.addAll(childrenByParent.getOrDefault(current, List.of()));
            }
        }
        return result;
    }

    private int subtreeHeight(UUID rootId, List<WorkOrderFolderEntity> all) {
        Map<UUID, List<UUID>> childrenByParent = new HashMap<>();
        for (WorkOrderFolderEntity folder : all) {
            if (folder.getParentId() != null) {
                childrenByParent.computeIfAbsent(folder.getParentId(), k -> new ArrayList<>()).add(folder.getId());
            }
        }
        return heightOf(rootId, childrenByParent);
    }

    private int heightOf(UUID nodeId, Map<UUID, List<UUID>> childrenByParent) {
        int max = 0;
        for (UUID child : childrenByParent.getOrDefault(nodeId, List.of())) {
            max = Math.max(max, heightOf(child, childrenByParent));
        }
        return max + 1;
    }
}
