package com.mirceone.inventoryapp.api.workorders;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Former firm-level document routes; uploads must target a work order.
 */
@RestController
@RequestMapping("/firms/{firmId}/documents")
public class LegacyDocumentController {

    @RequestMapping("/**")
    public void deprecated() {
        throw new ResponseStatusException(
                HttpStatus.GONE,
                "Use /firms/{firmId}/work-orders/{workOrderId}/files — create a work order first, then upload into it"
        );
    }
}
