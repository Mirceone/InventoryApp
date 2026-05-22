package com.mirceone.inventoryapp.service.documents;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class FolderTaxonomyResolver {

    public String resolveFinal(String candidate, Optional<String> ruleHint) {
        return DocumentFolderTaxonomy.resolve(candidate, ruleHint);
    }
}
