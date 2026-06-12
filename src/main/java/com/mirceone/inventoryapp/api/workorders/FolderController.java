package com.mirceone.inventoryapp.api.workorders;

import com.mirceone.inventoryapp.api.support.CurrentUserId;
import com.mirceone.inventoryapp.service.workorders.WorkOrderFolderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/firms/{firmId}/work-orders/{workOrderId}/folders")
@Tag(name = "Work order folders", description = "User-customizable folder tree with per-folder extension rules")
@SecurityRequirement(name = "bearerAuth")
public class FolderController {

    private final WorkOrderFolderService folderService;

    public FolderController(WorkOrderFolderService folderService) {
        this.folderService = folderService;
    }

    @GetMapping
    @Operation(summary = "Get the folder tree with file counts and extension rules")
    public List<FolderNodeResponse> getFolderTree(
            @CurrentUserId UUID userId,
            @PathVariable UUID firmId,
            @PathVariable UUID workOrderId
    ) {
        return FolderWebMapper.toResponseList(folderService.getFolderTree(userId, firmId, workOrderId));
    }

    @PostMapping
    @Operation(summary = "Create a folder (optionally nested, with extension rules)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Invalid name, extension or depth"),
            @ApiResponse(responseCode = "409", description = "Name or extension already in use")
    })
    public ResponseEntity<FolderNodeResponse> createFolder(
            @CurrentUserId UUID userId,
            @PathVariable UUID firmId,
            @PathVariable UUID workOrderId,
            @Valid @RequestBody CreateFolderRequest request
    ) {
        var created = folderService.createFolder(
                userId, firmId, workOrderId, FolderWebMapper.toCreateFolderSpec(request));
        var location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/firms/{firmId}/work-orders/{workOrderId}/folders/{folderId}")
                .buildAndExpand(firmId, workOrderId, created.id())
                .toUri();
        return ResponseEntity.created(location).body(FolderWebMapper.toResponse(created));
    }

    @PatchMapping("/{folderId}")
    @Operation(summary = "Rename and/or move a folder")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "400", description = "Invalid name, cycle or depth"),
            @ApiResponse(responseCode = "409", description = "Sibling name conflict")
    })
    public FolderNodeResponse updateFolder(
            @CurrentUserId UUID userId,
            @PathVariable UUID firmId,
            @PathVariable UUID workOrderId,
            @PathVariable UUID folderId,
            @Valid @RequestBody UpdateFolderRequest request
    ) {
        return FolderWebMapper.toResponse(folderService.updateFolder(
                userId, firmId, workOrderId, folderId, FolderWebMapper.toUpdateFolderSpec(request)));
    }

    @PutMapping("/{folderId}/rules")
    @Operation(summary = "Replace the extension rules of a folder")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rules replaced"),
            @ApiResponse(responseCode = "400", description = "Invalid extension or catch-all folder"),
            @ApiResponse(responseCode = "409", description = "Extension mapped to another folder")
    })
    public FolderNodeResponse replaceRules(
            @CurrentUserId UUID userId,
            @PathVariable UUID firmId,
            @PathVariable UUID workOrderId,
            @PathVariable UUID folderId,
            @Valid @RequestBody ReplaceFolderRulesRequest request
    ) {
        return FolderWebMapper.toResponse(folderService.replaceRules(
                userId, firmId, workOrderId, folderId, request.extensions()));
    }

    @DeleteMapping("/{folderId}")
    @Operation(summary = "Delete a folder (empty, or with moveFilesTo=catchAll to reassign files)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "409", description = "Folder not empty or catch-all")
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFolder(
            @CurrentUserId UUID userId,
            @PathVariable UUID firmId,
            @PathVariable UUID workOrderId,
            @PathVariable UUID folderId,
            @RequestParam(name = "moveFilesTo", required = false) String moveFilesTo
    ) {
        boolean moveToCatchAll = "catchAll".equalsIgnoreCase(moveFilesTo);
        folderService.deleteFolder(userId, firmId, workOrderId, folderId, moveToCatchAll);
    }
}
