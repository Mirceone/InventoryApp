package com.mirceone.inventoryapp.service.workorders.classification;

import com.mirceone.inventoryapp.model.WorkOrderFolderEntity;
import com.mirceone.inventoryapp.repository.WorkOrderFolderRepository;
import com.mirceone.inventoryapp.service.workorders.FolderPaths;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class WorkOrderFolderResolver {

    private final WorkOrderFolderRepository folderRepository;
    private final FileNameHeuristicClassifier heuristicClassifier;

    public WorkOrderFolderResolver(
            WorkOrderFolderRepository folderRepository,
            FileNameHeuristicClassifier heuristicClassifier
    ) {
        this.folderRepository = folderRepository;
        this.heuristicClassifier = heuristicClassifier;
    }

    /**
     * Immutable snapshot of a work order's folder tree (entities + path map), loaded once so a
     * single classification does not re-query the database for every resolver call.
     */
    public record FolderTree(List<WorkOrderFolderEntity> folders, Map<UUID, String> paths) {
    }

    /** Loads the folder tree once for reuse across the resolver calls within one classification. */
    public FolderTree loadTree(UUID workOrderId) {
        List<WorkOrderFolderEntity> folders = loadFolders(workOrderId);
        return new FolderTree(folders, FolderPaths.buildPathMap(folders));
    }

    public UUID catchAllFolderId(UUID workOrderId) {
        return folderRepository.findByWorkOrderIdAndCatchAllTrue(workOrderId)
                .orElseThrow(() -> noCatchAll(workOrderId))
                .getId();
    }

    public UUID catchAllFolderId(FolderTree tree) {
        return tree.folders().stream()
                .filter(WorkOrderFolderEntity::isCatchAll)
                .map(WorkOrderFolderEntity::getId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Work order has no catch-all folder"));
    }

    public String catchAllPath(FolderTree tree) {
        return tree.paths().get(catchAllFolderId(tree));
    }

    public List<String> allowedPathsForPrompt(FolderTree tree) {
        List<String> allowed = new ArrayList<>(tree.paths().values());
        allowed.sort(Comparator.naturalOrder());
        return allowed;
    }

    public Optional<UUID> resolveFolderId(UUID workOrderId, String candidate) {
        return resolveFolderId(loadTree(workOrderId), candidate);
    }

    public Optional<UUID> resolveFolderId(FolderTree tree, String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return Optional.empty();
        }
        Map<UUID, String> paths = tree.paths();
        List<WorkOrderFolderEntity> folders = tree.folders();
        String normalizedCandidate = candidate.strip();

        for (Map.Entry<UUID, String> entry : paths.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(normalizedCandidate)) {
                return Optional.of(entry.getKey());
            }
        }

        Optional<String> canonical = heuristicClassifier.canonicalFolderName(normalizedCandidate);
        if (canonical.isPresent()) {
            for (WorkOrderFolderEntity folder : folders) {
                if (folder.getName().equalsIgnoreCase(canonical.get())) {
                    return Optional.of(folder.getId());
                }
            }
            for (Map.Entry<UUID, String> entry : paths.entrySet()) {
                if (entry.getValue().equalsIgnoreCase(canonical.get())) {
                    return Optional.of(entry.getKey());
                }
            }
        }

        String leaf = normalizedCandidate.contains("/")
                ? normalizedCandidate.substring(normalizedCandidate.lastIndexOf('/') + 1)
                : normalizedCandidate;
        for (WorkOrderFolderEntity folder : folders) {
            if (folder.getName().equalsIgnoreCase(leaf)) {
                return Optional.of(folder.getId());
            }
        }
        return Optional.empty();
    }

    public UUID resolveFinal(FolderTree tree, String aiSuggestion, Optional<String> ruleHint) {
        return resolveFolderId(tree, aiSuggestion)
                .or(() -> ruleHint.flatMap(hint -> resolveFolderId(tree, hint)))
                .orElseGet(() -> catchAllFolderId(tree));
    }

    private static IllegalStateException noCatchAll(UUID workOrderId) {
        return new IllegalStateException("Work order " + workOrderId + " has no catch-all folder");
    }

    private List<WorkOrderFolderEntity> loadFolders(UUID workOrderId) {
        return folderRepository.findAllByWorkOrderIdOrderBySortOrderAscNameAsc(workOrderId);
    }
}
