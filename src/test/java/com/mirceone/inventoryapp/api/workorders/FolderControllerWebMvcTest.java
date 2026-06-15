package com.mirceone.inventoryapp.api.workorders;

import com.mirceone.inventoryapp.security.AuthRateLimiter;
import com.mirceone.inventoryapp.service.workorders.FolderTreeNode;
import com.mirceone.inventoryapp.service.workorders.WorkOrderContracts;
import com.mirceone.inventoryapp.service.workorders.WorkOrderFolderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FolderController.class)
class FolderControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkOrderFolderService folderService;

    @MockitoBean
    @Qualifier("authRateLimiter")
    private AuthRateLimiter authRateLimiter;

    @MockitoBean
    @Qualifier("documentUploadRateLimiter")
    private AuthRateLimiter documentUploadRateLimiter;

    private final UUID userId = UUID.randomUUID();
    private final UUID firmId = UUID.randomUUID();
    private final UUID workOrderId = UUID.randomUUID();

    @Test
    void getFolderTreeReturnsNestedNodes() throws Exception {
        UUID docsId = UUID.randomUUID();
        UUID plansId = UUID.randomUUID();
        UUID miscId = UUID.randomUUID();
        when(folderService.getFolderTree(eq(userId), eq(firmId), eq(workOrderId))).thenReturn(List.of(
                new FolderTreeNode(docsId, "Documents", "Documents", false, 2, List.of("pdf"), List.of(
                        new FolderTreeNode(plansId, "Plans", "Documents/Plans", false, 1, List.of("dwg"), List.of())
                )),
                new FolderTreeNode(miscId, "Misc", "Misc", true, 0, List.of(), List.of())
        ));

        mockMvc.perform(get("/firms/{firmId}/work-orders/{workOrderId}/folders", firmId, workOrderId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Documents"))
                .andExpect(jsonPath("$[0].extensions[0]").value("pdf"))
                .andExpect(jsonPath("$[0].children[0].path").value("Documents/Plans"))
                .andExpect(jsonPath("$[1].catchAll").value(true));
    }

    @Test
    void createFolderReturns201() throws Exception {
        UUID folderId = UUID.randomUUID();
        when(folderService.createFolder(eq(userId), eq(firmId), eq(workOrderId), any(WorkOrderContracts.CreateFolderSpec.class)))
                .thenReturn(new FolderTreeNode(folderId, "Plans", "Plans", false, 0, List.of("dwg"), List.of()));

        mockMvc.perform(post("/firms/{firmId}/work-orders/{workOrderId}/folders", firmId, workOrderId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Plans",
                                  "extensions": ["dwg"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(folderId.toString()))
                .andExpect(jsonPath("$.extensions[0]").value("dwg"));
    }

    @Test
    void renameFolderReturns200() throws Exception {
        UUID folderId = UUID.randomUUID();
        when(folderService.updateFolder(eq(userId), eq(firmId), eq(workOrderId), eq(folderId), any(WorkOrderContracts.UpdateFolderSpec.class)))
                .thenReturn(new FolderTreeNode(folderId, "Renamed", "Renamed", false, 0, List.of(), List.of()));

        mockMvc.perform(patch("/firms/{firmId}/work-orders/{workOrderId}/folders/{folderId}", firmId, workOrderId, folderId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Renamed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"));
    }

    @Test
    void replaceRulesConflictReturns409() throws Exception {
        UUID folderId = UUID.randomUUID();
        when(folderService.replaceRules(eq(userId), eq(firmId), eq(workOrderId), eq(folderId), any()))
                .thenThrow(new ResponseStatusException(CONFLICT, "Extension 'pdf' is already mapped to another folder"));

        mockMvc.perform(put("/firms/{firmId}/work-orders/{workOrderId}/folders/{folderId}/rules", firmId, workOrderId, folderId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"extensions\":[\"pdf\"]}"))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteNonEmptyFolderReturns409() throws Exception {
        UUID folderId = UUID.randomUUID();
        doThrow(new ResponseStatusException(CONFLICT, "Folder is not empty"))
                .when(folderService).deleteFolder(eq(userId), eq(firmId), eq(workOrderId), eq(folderId), eq(false));

        mockMvc.perform(delete("/firms/{firmId}/work-orders/{workOrderId}/folders/{folderId}", firmId, workOrderId, folderId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(userId.toString()))))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteFolderWithMoveFilesReturns204() throws Exception {
        UUID folderId = UUID.randomUUID();

        mockMvc.perform(delete("/firms/{firmId}/work-orders/{workOrderId}/folders/{folderId}", firmId, workOrderId, folderId)
                        .param("moveFilesTo", "catchAll")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(userId.toString()))))
                .andExpect(status().isNoContent());

        org.mockito.Mockito.verify(folderService)
                .deleteFolder(eq(userId), eq(firmId), eq(workOrderId), eq(folderId), eq(true));
    }
}
