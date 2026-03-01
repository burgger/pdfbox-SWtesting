package org.apache.pdfbox.pdfwriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.PDEncryption;
import org.apache.pdfbox.pdmodel.encryption.ProtectionPolicy;
import org.apache.pdfbox.pdmodel.encryption.SecurityHandler;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class COSWriterMockingTest
{
    @Test
    void mocking() throws IOException
    {
        // Arrange: create a real document, but "inject" mocked encryption via spy
        PDDocument realDoc = new PDDocument();
        PDDocument doc = spy(realDoc);

        @SuppressWarnings("unchecked")
        SecurityHandler<? extends ProtectionPolicy> handler =
                (SecurityHandler<? extends ProtectionPolicy>) mock(SecurityHandler.class);

        // COSWriter throws if there is an encryption dict but no protection policy,
        // so we force this to be "valid".
        doReturn(true).when(handler).hasProtectionPolicy();

        PDEncryption encryption = mock(PDEncryption.class);
        doReturn(handler).when(encryption).getSecurityHandler();

        // Dependency injection (test seam): replace doc.getEncryption() with our mock
        doReturn(encryption).when(doc).getEncryption();

        // Use in-memory output
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        COSWriter writer = new COSWriter(out);

        // Act
        writer.write(doc, null);

        // Assert: interaction verification (mocking)
        verify(handler, times(1)).prepareDocumentForEncryption(doc);

        doc.close();
    }
}
