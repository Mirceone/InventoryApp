package com.mirceone.inventoryapp.api.documents;

import com.mirceone.inventoryapp.service.documents.DossierService;
import com.mirceone.inventoryapp.service.documents.DossierSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/firms/{firmId}/dossiers")
@Tag(name = "Electronic folder dossiers", description = "User-named dossiers; upload documents inside a dossier")
@SecurityRequirement(name = "bearerAuth")
public class DossierController {

    private final DossierService dossierService;

    public DossierController(DossierService dossierService) {
        this.dossierService = dossierService;
    }

    @PostMapping
    @Operation(summary = "Create a named dossier")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "409", description = "Duplicate name")
    })
    public ResponseEntity<DossierResponse> createDossier(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID firmId,
            @Valid @RequestBody CreateDossierRequest request
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        DossierSummary created = dossierService.createDossier(userId, firmId, request.name());
        var location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/firms/{firmId}/dossiers/{dossierId}")
                .buildAndExpand(firmId, created.id())
                .toUri();
        return ResponseEntity.created(location).body(DossierWebMapper.toResponse(created));
    }

    @GetMapping
    @Operation(summary = "List dossiers for firm")
    public List<DossierResponse> listDossiers(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID firmId
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return dossierService.listDossiers(userId, firmId).stream()
                .map(DossierWebMapper::toResponse)
                .toList();
    }

    @GetMapping("/{dossierId}")
    @Operation(summary = "Get dossier by id")
    public DossierResponse getDossier(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID firmId,
            @PathVariable UUID dossierId
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return DossierWebMapper.toResponse(dossierService.getDossier(userId, firmId, dossierId));
    }

    @PatchMapping("/{dossierId}")
    @Operation(summary = "Rename dossier")
    public DossierResponse renameDossier(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID firmId,
            @PathVariable UUID dossierId,
            @Valid @RequestBody UpdateDossierRequest request
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return DossierWebMapper.toResponse(dossierService.renameDossier(userId, firmId, dossierId, request.name()));
    }

    @DeleteMapping("/{dossierId}")
    @Operation(summary = "Delete dossier and all its documents")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDossier(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID firmId,
            @PathVariable UUID dossierId
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        dossierService.deleteDossier(userId, firmId, dossierId);
    }
}
