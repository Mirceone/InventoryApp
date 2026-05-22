package com.mirceone.inventoryapp.api.documents;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Former firm-level document routes; uploads must target a dossier.
 */
@RestController
@RequestMapping("/firms/{firmId}/documents")
public class LegacyDocumentController {

    @RequestMapping("/**")
    public void deprecated() {
        throw new ResponseStatusException(
                HttpStatus.GONE,
                "Use /firms/{firmId}/dossiers/{dossierId}/documents — create a dossier first, then upload into it"
        );
    }
}
