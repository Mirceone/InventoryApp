package com.mirceone.inventoryapp.service.ai;

/**
 * A single image passed to a vision model: raw bytes plus its MIME type (e.g. {@code image/png}).
 */
public record AiImage(String mimeType, byte[] data) {
}
