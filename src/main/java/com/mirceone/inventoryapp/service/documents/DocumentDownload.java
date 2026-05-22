package com.mirceone.inventoryapp.service.documents;

import org.springframework.core.io.Resource;

/**
 * Prepared download: stream with HTTP headers metadata.
 */
public record DocumentDownload(String originalFilename, String mimeType, Resource resource) {
}
