package com.sismics.docs.core.util;

import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;
import com.sismics.BaseTest;
import com.sismics.docs.core.model.context.AppContext;
import org.junit.Assert;
import org.junit.Test;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Test of the encryption utilities.
 *
 * @author bgamard
 */
public class TestEncryptUtil extends BaseTest {
    @Test
    public void generatePrivateKeyTest() {
        String key = EncryptionUtil.generatePrivateKey();
        System.out.println(key);
        Assert.assertFalse(Strings.isNullOrEmpty(key));
    }

    @Test
    public void encryptStreamTest() throws Exception {
        try {
            EncryptionUtil.getEncryptionCipher("");
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // NOP
        }
        Cipher cipher = EncryptionUtil.getEncryptionCipher("OnceUponATime");
        InputStream inputStream = new CipherInputStream(getSystemResourceAsStream(FILE_PDF), cipher);
        byte[] encryptedData = ByteStreams.toByteArray(inputStream);
        byte[] assertData = ByteStreams.toByteArray(getSystemResourceAsStream(FILE_PDF_ENCRYPTED));

        Assert.assertEquals(encryptedData.length, assertData.length);
    }

    @Test
    public void decryptStreamTest() throws Exception {
        InputStream inputStream = EncryptionUtil.decryptInputStream(
                getSystemResourceAsStream(FILE_PDF_ENCRYPTED), "OnceUponATime");
        byte[] encryptedData = ByteStreams.toByteArray(inputStream);
        byte[] assertData = ByteStreams.toByteArray(getSystemResourceAsStream(FILE_PDF));

        Assert.assertEquals(encryptedData.length, assertData.length);
    }

    @Test
    public void decryptFileNullKeyTest() throws Exception {
        // When privateKey is null, decryptFile should return the same file
        Path tempFile = Files.createTempFile("test_decrypt_null", ".tmp");
        try {
            Files.write(tempFile, "test data".getBytes());
            Path result = EncryptionUtil.decryptFile(tempFile, null);
            Assert.assertEquals(tempFile, result);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    public void decryptFileTest() throws Exception {
        // Encrypt a temporary file, then decrypt it via decryptFile
        Path originalFile = Files.createTempFile("test_decrypt_orig", ".tmp");
        Path encryptedFile = Files.createTempFile("test_decrypt_enc", ".tmp");
        try {
            // Write original data
            byte[] originalData = "Hello Teedy Encryption Test!".getBytes();
            Files.write(originalFile, originalData);

            // Encrypt the original data into encryptedFile
            Cipher encryptCipher = EncryptionUtil.getEncryptionCipher("MySecretKey");
            try (InputStream is = Files.newInputStream(originalFile);
                 OutputStream os = Files.newOutputStream(encryptedFile)) {
                CipherInputStream cis = new CipherInputStream(is, encryptCipher);
                byte[] buf = new byte[4096];
                int n;
                while ((n = cis.read(buf)) != -1) {
                    os.write(buf, 0, n);
                }
            }

            // Decrypt via decryptFile
            Path decryptedFile = EncryptionUtil.decryptFile(encryptedFile, "MySecretKey");
            Assert.assertNotNull(decryptedFile);

            // Verify decrypted content matches original
            byte[] decryptedData = Files.readAllBytes(decryptedFile);
            Assert.assertArrayEquals(originalData, decryptedData);
        } finally {
            Files.deleteIfExists(originalFile);
            Files.deleteIfExists(encryptedFile);
        }
    }
}
