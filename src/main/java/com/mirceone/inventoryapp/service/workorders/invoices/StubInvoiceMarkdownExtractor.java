package com.mirceone.inventoryapp.service.workorders.invoices;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@ConditionalOnProperty(name = "app.invoices.extractor", havingValue = "stub")
public class StubInvoiceMarkdownExtractor implements InvoiceMarkdownExtractor {

    @Override
    public String extract(Path sourceFile, String mimeType) throws IOException {
        String name = sourceFile.getFileName().toString();
        long size = Files.size(sourceFile);
        return "# Invoice stub\n\n- file: " + name + "\n- mime: " + mimeType + "\n- bytes: " + size + "\n";
    }
}
