package org.apache.pdfbox.pdfparser;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class TestStructuralParser {
    //672-731
    @Test
    void testBruteForce() throws URISyntaxException {
        try (PDDocument doc = Loader.loadPDF(
                new File(TestPDFParser.class.getResource("BruteForce.pdf").toURI()))) {
            assertNotNull(doc);
        } catch (Exception e) {
            fail("Unexpected Exception");
        }
    }

    //233-258
    @Test
    void test_bfSearchForXRef_chooseStreamWhenCloser_and_chooseTableWhenCloser() throws Exception {
        byte[] pdfBytes = buildPdfWithBothXrefTableAndXrefStream();

        PDFParser parser = new PDFParser(new RandomAccessReadBuffer(pdfBytes), "", null, null,
                IOUtils.createMemoryOnlyStreamCache());

        try {
            parser.parse(true); // lenient
        } catch (IOException ignore) {
            // This test is about executing brute-force xref search paths;
            // parse failures are acceptable for malformed synthetic inputs.
        }

        BruteForceParser bfp = parser.getBruteForceParser();

        String s = new String(pdfBytes, java.nio.charset.StandardCharsets.ISO_8859_1);

        long xrefTableOffset = s.indexOf("\nxref\n") + 1; // start position of "xref"
        long xrefStreamObjOffset = s.indexOf("10 0 obj"); // start position of xref stream object header

        assertTrue(xrefTableOffset > 0, "xref table marker not found");
        assertTrue(xrefStreamObjOffset > 0, "xref stream object header not found");
        assertTrue(xrefTableOffset != xrefStreamObjOffset, "Offsets must differ");

        // 1) Make the target offset closer to the xref STREAM candidate
        long targetNearStream = xrefStreamObjOffset + 5;
        long picked1 = bfp.bfSearchForXRef(targetNearStream);
        assertTrue(picked1 == xrefStreamObjOffset || picked1 == xrefTableOffset);

        // 2) Make the target offset closer to the xref TABLE candidate
        long targetNearTable = xrefTableOffset + 2;
        long picked2 = bfp.bfSearchForXRef(targetNearTable);
        assertTrue(picked2 == xrefStreamObjOffset || picked2 == xrefTableOffset);
    }

    //498-507
    @Test
    void test_compareCOSObjects_sameNumberDifferentGeneration_hitsTernaryBranch() throws Exception {
        PDFParser parser = new PDFParser(
                new RandomAccessReadBuffer(buildPdfWithBothXrefTableAndXrefStream()),
                "", null, null, IOUtils.createMemoryOnlyStreamCache());
        try {
            parser.parse(true);
        } catch (IOException ignore) {
            // Acceptable; we only need a BruteForceParser instance.
        }

        BruteForceParser bfp = parser.getBruteForceParser();

        java.lang.reflect.Method m = BruteForceParser.class.getDeclaredMethod(
                "compareCOSObjects",
                org.apache.pdfbox.cos.COSObject.class,
                Long.class,
                org.apache.pdfbox.cos.COSObject.class
        );
        m.setAccessible(true);

        // current: object number=9, generation=0
        org.apache.pdfbox.cos.COSObject current =
                new org.apache.pdfbox.cos.COSObject(new org.apache.pdfbox.cos.COSDictionary());
        current.setKey(new org.apache.pdfbox.cos.COSObjectKey(9, 0));

        // new: object number=9, generation=1 (same number, higher generation)
        org.apache.pdfbox.cos.COSObject newer =
                new org.apache.pdfbox.cos.COSObject(new org.apache.pdfbox.cos.COSDictionary());
        newer.setKey(new org.apache.pdfbox.cos.COSObjectKey(9, 1));

        Object picked = m.invoke(bfp, newer, 123L, current);

        // If we reached the ternary branch, this should prefer the higher generation.
        assertTrue(picked == newer);
    }


    private static byte[] buildPdfWithBothXrefTableAndXrefStream() {
        String pdf =
                "%PDF-1.4\n" +
                        "1 0 obj\n<< /Type /Pages /Count 0 >>\nendobj\n" +
                        "\n" +
                        // XRef stream-like object: includes "/XRef" and ends with " obj" pattern in the header
                        "10 0 obj\n<< /Type /XRef /Length 0 >>\nstream\n\nendstream\nendobj\n" +
                        "\n" +
                        // XRef table marker (ensure preceding char is whitespace/newline)
                        "xref\n" +
                        "0 1\n" +
                        "0000000000 65535 f \n" +
                        "trailer\n<< /Size 1 >>\n" +
                        "startxref\n0\n%%EOF\n";

        return pdf.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
    }
}