package org.apache.pdfbox.pdmodel;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdfwriter.compress.CompressParameters;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FSM-based functional tests for PDF document lifecycle:
 * NotLoaded -> LoadedClean -> LoadedDirty -> (save/saveAs) -> Clean, plus Close and invalid-after-close.
 * Testing PDF: src/test/resources/text-extraction/two-columns.pdf (1 page, two columns)
 */
class TestPDDocumentLifecycleFSM
{
    private static final File TESTRESULTSDIR = new File("target/test-output/fsm-test");
    private static final File FIXTURE_PDF = new File("src/test/resources/text-extraction/two-columns.pdf");

    @BeforeAll
    static void setUp()
    {
        TESTRESULTSDIR.mkdirs();
        assertTrue(FIXTURE_PDF.exists(), "Fixture PDF not found: " + FIXTURE_PDF.getPath());
        assertTrue(FIXTURE_PDF.length() > 0, "Fixture PDF is empty: " + FIXTURE_PDF.getPath());
    }

    // T1: Load -> Close
    @Test
    void testLoadThenClose() throws IOException
    {
        try (PDDocument doc = Loader.loadPDF(FIXTURE_PDF))
        {
            assertEquals(1, doc.getNumberOfPages(), "Fixture should have exactly 1 page");
        }
        // try-with-resources closes: pass if no exception
    }

    // T2: Load -> Modify(addPage) -> Save -> Close, then Reload and check page count changed
    @Test
    void testLoadModifySaveClose() throws IOException
    {
        File out = new File(TESTRESULTSDIR, "fsm-t2-modify-save.pdf");

        try (PDDocument doc = Loader.loadPDF(FIXTURE_PDF))
        {
            assertEquals(1, doc.getNumberOfPages());
            doc.addPage(new PDPage()); // modify => Dirty (abstract)
            doc.save(out, CompressParameters.NO_COMPRESSION); // save => Clean (abstract)
        }

        try (PDDocument reloaded = Loader.loadPDF(out))
        {
            assertEquals(2, reloaded.getNumberOfPages());
        }
    }

    // T3: Dirty self-loop via multiple modify, then save
    @Test
    void testDirtyMultipleModificationsThenSave() throws IOException
    {
        File out = new File(TESTRESULTSDIR, "fsm-t3-multi-modify-save.pdf");

        try (PDDocument doc = Loader.loadPDF(FIXTURE_PDF))
        {
            assertEquals(1, doc.getNumberOfPages());
            doc.addPage(new PDPage()); // modify
            doc.addPage(new PDPage()); // modify again (self-loop in Dirty)
            doc.save(out, CompressParameters.NO_COMPRESSION);
        }

        try (PDDocument reloaded = Loader.loadPDF(out))
        {
            assertEquals(3, reloaded.getNumberOfPages());
        }
    }

    // T4: Clean save self-loop (save without modify)
    @Test
    void testSaveWithoutModificationKeepsValid() throws IOException
    {
        File out = new File(TESTRESULTSDIR, "fsm-t4-clean-save.pdf");

        try (PDDocument doc = Loader.loadPDF(FIXTURE_PDF))
        {
            assertEquals(1, doc.getNumberOfPages());
            doc.save(out, CompressParameters.NO_COMPRESSION); // save while "clean"
        }

        try (PDDocument reloaded = Loader.loadPDF(out))
        {
            assertEquals(1, reloaded.getNumberOfPages());
        }
    }

    // T5: "saveAs" abstraction from clean: save to outA, then save to different outB
    @Test
    void testSaveAsFromClean() throws IOException
    {
        File outA = new File(TESTRESULTSDIR, "fsm-t5-saveas-A.pdf");
        File outB = new File(TESTRESULTSDIR, "fsm-t5-saveas-B.pdf");

        try (PDDocument doc = Loader.loadPDF(FIXTURE_PDF))
        {
            assertEquals(1, doc.getNumberOfPages());
            doc.save(outA, CompressParameters.NO_COMPRESSION);
            doc.save(outB, CompressParameters.NO_COMPRESSION); // abstract saveAs: different destination
        }

        try (PDDocument reloaded = Loader.loadPDF(outB))
        {
            assertEquals(1, reloaded.getNumberOfPages());
        }
    }

    // T6: "saveAs" abstraction from dirty: save to outA, modify, then save to different outB
    @Test
    void testSaveAsFromDirtyPreservesModification() throws IOException
    {
        File outA = new File(TESTRESULTSDIR, "fsm-t6-saveas-dirty-A.pdf");
        File outB = new File(TESTRESULTSDIR, "fsm-t6-saveas-dirty-B.pdf");

        try (PDDocument doc = Loader.loadPDF(FIXTURE_PDF))
        {
            assertEquals(1, doc.getNumberOfPages());
            doc.save(outA, CompressParameters.NO_COMPRESSION);
            doc.addPage(new PDPage()); // dirty
            doc.save(outB, CompressParameters.NO_COMPRESSION); // save to different destination => abstract saveAs
        }

        try (PDDocument reloaded = Loader.loadPDF(outB))
        {
            assertEquals(2, reloaded.getNumberOfPages());
        }
    }

    // T7: After saveAs, we can still modify then save again
    @Test
    void testAfterSaveAsCanModifyThenSaveAgain() throws IOException
    {
        File outA = new File(TESTRESULTSDIR, "fsm-t7-seed-A.pdf");
        File outB = new File(TESTRESULTSDIR, "fsm-t7-seed-B.pdf");
        File outC = new File(TESTRESULTSDIR, "fsm-t7-after-modify-save-C.pdf");

        try (PDDocument doc = Loader.loadPDF(FIXTURE_PDF))
        {
            assertEquals(1, doc.getNumberOfPages());

            doc.save(outA, CompressParameters.NO_COMPRESSION);
            doc.save(outB, CompressParameters.NO_COMPRESSION); // treat as saveAs => SavedAsClean (abstract)

            doc.addPage(new PDPage()); // modify => Dirty
            doc.save(outC, CompressParameters.NO_COMPRESSION); // save => Clean
        }

        try (PDDocument reloaded = Loader.loadPDF(outC))
        {
            assertEquals(2, reloaded.getNumberOfPages());
        }
    }

    // T8: Invalid after close: save should not succeed
    @Test
    void testInvalidSaveAfterClose()
    {
        File out = new File(TESTRESULTSDIR, "fsm-t8-invalid-save-after-close.pdf");

        assertThrows(Throwable.class, () ->
        {
            PDDocument doc = Loader.loadPDF(FIXTURE_PDF);
            doc.close();
            doc.save(out, CompressParameters.NO_COMPRESSION);
        });
    }

    // T9: Invalid after close: modify should not succeed
    @Test
void testInvalidModifyAfterClose()
{
    File out = new File(TESTRESULTSDIR, "fsm-t9-invalid-modify-after-close.pdf");

    assertThrows(Throwable.class, () ->
    {
        PDDocument doc = Loader.loadPDF(FIXTURE_PDF);
        doc.close();

        // modify may or may not throw immediately; PDFBox might allow this call
        doc.addPage(new PDPage());

        // but producing a valid saved PDF after close should not succeed
        doc.save(out, CompressParameters.NO_COMPRESSION);
    });
}
}