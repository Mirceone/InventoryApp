package com.mirceone.inventoryapp.service.workorders.classification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public record AiFolderSuggestion(String folder) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static AiFolderSuggestion parse(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            throw new IllegalArgumentException("Empty AI response");
        }
        try {
            JsonNode root = MAPPER.readTree(rawJson);
            JsonNode folderNode = root.get("folder");
            if (folderNode == null || folderNode.isNull() || folderNode.asText().isBlank()) {
                throw new IllegalArgumentException("Missing folder field in AI response");
            }
            return new AiFolderSuggestion(folderNode.asText().strip());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid AI JSON response: " + e.getMessage(), e);
        }
    }
}
