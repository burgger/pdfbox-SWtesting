package org.apache.pdfbox.text;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class TestTextExtractionPartitions {

    // ---------- helpers ----------

    private static PDDocument loadFromResource(String resourceName) throws IOException {
        String path = "text-extraction/" + resourceName; // under pdfbox/src/test/resources/
        InputStream in = TestTextExtractionPartitions.class.getClassLoader().getResourceAsStream(path);
        assertNotNull(in, "Missing test resource: " + path);
        return Loader.loadPDF(in.readAllBytes());
    }

    private static String extractText(PDDocument doc, boolean sortByPosition) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(sortByPosition);
        return stripper.getText(doc);
    }

    private static String normalizeWhitespace(String s) {
        // Keep it simple: collapse all whitespace to single spaces
        return s.replaceAll("\\s+", " ").trim();
    }

    // ---------- P4 rotated page ----------

    @Test
    public void testExtractText_rotatedPage() throws Exception {
        try (PDDocument doc = loadFromResource("rotated-page-90.pdf")) {
            String text = extractText(doc, true);
            String norm = normalizeWhitespace(text);
            assertTrue(norm.contains("ROTATED_90"), "Expected keyword not found in extracted text: " + norm);
        }
    }

    // ---------- P7 image-only ----------

    @Test
    public void testExtractText_imageOnly_returnsEmpty() throws Exception {
        try (PDDocument doc = loadFromResource("image-only.pdf")) {
            String text = extractText(doc, true);
            String trimmed = text.trim();
            // You can tighten this after one run if needed:
            assertTrue(trimmed.isEmpty() || trimmed.length() <= 5,
                    "Expected empty (or near-empty) output, but got: [" + trimmed + "]");
        }
    }

    // ---------- P8/P12 encrypted cases ----------

    @Test
    public void testExtractText_encrypted_noPassword_throws() throws Exception {
        // Without a password, loading or extraction should fail.
        // Depending on PDFBox behavior, failure can happen on load or on getText.
        assertThrows(Exception.class, () -> {
            try (PDDocument doc = loadFromResource("encrypted-userpass-secret.pdf")) {
                extractText(doc, true);
            }
        });
    }

    @Test
    public void testExtractText_encrypted_wrongPassword_throws() throws Exception {
        // If you want to pass a wrong password explicitly, you need to load with password.
        // PDFBox 4 Loader supports password overloads; if your build doesn't, we can adjust.
        assertThrows(Exception.class, () -> {
            String path = "text-extraction/encrypted-userpass-secret.pdf";
            InputStream in = TestTextExtractionPartitions.class.getClassLoader().getResourceAsStream(path);
            assertNotNull(in, "Missing test resource: " + path);

            // wrong password load
            try (PDDocument doc = Loader.loadPDF(in.readAllBytes(), "wrong")) {
                extractText(doc, true);
            }
        });
    }

    // ---------- P9 two columns ----------

    @Test
    public void testExtractText_twoColumns_sortingChangesOrder() throws Exception {
        try (PDDocument doc = loadFromResource("two-columns.pdf")) {
            String unsorted = normalizeWhitespace(extractText(doc, false));
            String sorted = normalizeWhitespace(extractText(doc, true));

            assertNotEquals(unsorted, sorted, "Expected different outputs between sortByPosition=false and true");

            // Minimal stable check for expected reading order (adjust tokens to match your PDF content)
            int lPos = sorted.indexOf("This is column1");
            int rPos = sorted.indexOf("This is column2");
            assertTrue(lPos >= 0 && rPos >= 0 && lPos < rPos,
                    "Expected left column content to appear before right column in sorted output: " + sorted);
        }
    }

    // ---------- P11 invalid ranges (start>end / out-of-bounds) ----------

    @Test
    public void testExtractText_invalidRange_startGreaterThanEnd() throws Exception {
        try (PDDocument doc = loadFromResource("multi-page-3.pdf")) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            // Invalid: start > end
            stripper.setStartPage(3);
            stripper.setEndPage(2);

            // First run: observe behavior, then lock it down.
            // Option A: expect exception
            // Option B: expect empty output
            String text = stripper.getText(doc);
            assertTrue(text.trim().isEmpty(), "Expected empty output for startPage > endPage, got: " + text);
        }
    }

    @Test
    public void testExtractText_invalidRange_outOfBounds() throws Exception {
        try (PDDocument doc = loadFromResource("multi-page-3.pdf")) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            // Out-of-bounds: request pages beyond document
            stripper.setStartPage(10);
            stripper.setEndPage(12);

            String text = stripper.getText(doc);
            assertTrue(text.trim().isEmpty(), "Expected empty output for out-of-bounds range, got: " + text);
        }
    }
}
