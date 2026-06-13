package com.mirceone.inventoryapp.service.workorders.invoices;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Rasterizes the first {@code maxPages} pages of a PDF to PNG byte arrays for the VLM.
 */
@Component
public class PdfPageRenderer {

    public List<byte[]> renderToPng(Path pdf, int maxPages, int dpi) throws IOException {
        List<byte[]> images = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            PDFRenderer renderer = new PDFRenderer(document);
            int pages = Math.min(document.getNumberOfPages(), Math.max(1, maxPages));
            for (int page = 0; page < pages; page++) {
                BufferedImage image = renderer.renderImageWithDPI(page, dpi, ImageType.RGB);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                if (!ImageIO.write(image, "png", out)) {
                    throw new IOException("No PNG writer available to rasterize PDF page " + (page + 1));
                }
                images.add(out.toByteArray());
            }
        }
        return images;
    }
}
