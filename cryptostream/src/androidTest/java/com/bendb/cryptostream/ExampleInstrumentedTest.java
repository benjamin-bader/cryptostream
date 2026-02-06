package com.bendb.cryptostream;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void cryptoRoundTrip() throws Exception {
        byte[] key = Native.generateKey();
        byte[] bytes = new byte[1024];
        new SecureRandom().nextBytes(bytes);

        byte[] decrypted;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (CryptoOutputStream out = new CryptoOutputStream(baos, key)) {
                out.write(bytes);
            }

            try (CryptoInputStream input = new CryptoInputStream(new ByteArrayInputStream(baos.toByteArray()), key)) {
                byte[] buffer = new byte[1024];
                baos = new ByteArrayOutputStream();
                while (true) {
                    int n = input.read(buffer);
                    if (n == -1) {
                        break;
                    }
                    baos.write(buffer, 0, n);
                }
                decrypted = baos.toByteArray();
            }
        } catch (Throwable t) {
            throw t;
        }

        Assert.assertArrayEquals(bytes, decrypted);
    }

    @Test
    public void readMultipleBlocks() throws Exception {
        int bs = Native.blockSize();
        byte[] key = Native.generateKey();
        byte[] bytes = new byte[bs * 4];
        Arrays.fill(bytes, bs * 0, bs * 1, (byte) 1);
        Arrays.fill(bytes, bs * 1, bs * 2, (byte) 2);
        Arrays.fill(bytes, bs * 2, bs * 3, (byte) 3);
        Arrays.fill(bytes, bs * 3, bs * 4, (byte) 4);

        byte[] encrypted;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (CryptoOutputStream out = new CryptoOutputStream(baos, key)) {
                out.write(bytes);
            }
            encrypted = baos.toByteArray();
        }

        byte[] decrypted;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(encrypted);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (CryptoInputStream in = new CryptoInputStream(bais, key)) {
                byte[] buffer = new byte[2000];
                while (true) {
                    int n = in.read(buffer);
                    if (n == -1) {
                        break;
                    }
                    baos.write(buffer, 0, n);
                }
            }
            decrypted = baos.toByteArray();
        }

        Assert.assertArrayEquals(bytes, decrypted);
    }

    @Test
    public void skipMultipleBlocks() throws Exception {
        int bs = Native.blockSize();
        byte[] key = Native.generateKey();
        byte[] bytes = new byte[bs * 4];
        Arrays.fill(bytes, bs * 0, bs * 1, (byte) 1);
        Arrays.fill(bytes, bs * 1, bs * 2, (byte) 2);
        Arrays.fill(bytes, bs * 2, bs * 3, (byte) 3);
        Arrays.fill(bytes, bs * 3, bs * 4, (byte) 4);

        byte[] encrypted;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (CryptoOutputStream out = new CryptoOutputStream(baos, key)) {
                out.write(bytes);
            }
            encrypted = baos.toByteArray();
        }

        byte[] lastBlock = new byte[bs];
        byte[] expected = new byte[bs];
        Arrays.fill(expected, 0, expected.length, (byte) 4);

        try (ByteArrayInputStream bais = new ByteArrayInputStream(encrypted)) {
            try (CryptoInputStream in = new CryptoInputStream(bais, key)) {
                long skipped = in.skip(bs * 3);
                Assert.assertEquals(bs * 3, skipped);

                int read = 0;
                while (read < bs) {
                    int n = in.read(lastBlock, read, lastBlock.length - read);
                    if (n == -1) {
                        break;
                    }
                    read += n;
                }

                Assert.assertEquals(bs, read);
                Assert.assertArrayEquals(expected, lastBlock);
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidKeyLengthOutputStream() throws Exception {
        byte[] badKey = new byte[16]; // Wrong size
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new CryptoOutputStream(baos, badKey); // Should throw
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidKeyLengthInputStream() throws Exception {
        byte[] badKey = new byte[16]; // Wrong size
        byte[] dummyData = new byte[Native.nonceSize() + 100];
        ByteArrayInputStream bais = new ByteArrayInputStream(dummyData);
        new CryptoInputStream(bais, badKey); // Should throw
    }

    @Test
    public void singleByteReadWrite() throws Exception {
        byte[] key = Native.generateKey();
        byte[] testData = "Hello, World!".getBytes();

        // Write byte by byte
        byte[] encrypted;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (CryptoOutputStream out = new CryptoOutputStream(baos, key)) {
                for (byte b : testData) {
                    out.write(b);
                }
            }
            encrypted = baos.toByteArray();
        }

        // Read byte by byte
        byte[] decrypted = new byte[testData.length];
        try (ByteArrayInputStream bais = new ByteArrayInputStream(encrypted)) {
            try (CryptoInputStream in = new CryptoInputStream(bais, key)) {
                for (int i = 0; i < testData.length; i++) {
                    int b = in.read();
                    Assert.assertNotEquals(-1, b);
                    decrypted[i] = (byte) b;
                }
                // Verify EOF
                Assert.assertEquals(-1, in.read());
            }
        }

        Assert.assertArrayEquals(testData, decrypted);
    }

    @Test
    public void counterSynchronizationAcrossBlocks() throws Exception {
        // Test that counter synchronization works correctly across multiple blocks
        // This verifies that bytesRead / 64 is the correct counter increment
        int bs = Native.blockSize();
        byte[] key = Native.generateKey();

        // Create data that's 2.5 blocks to test partial block handling
        byte[] originalData = new byte[bs * 2 + bs / 2];
        new SecureRandom().nextBytes(originalData);

        // Encrypt
        byte[] encrypted;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (CryptoOutputStream out = new CryptoOutputStream(baos, key)) {
                out.write(originalData);
            }
            encrypted = baos.toByteArray();
        }

        // Decrypt with different read patterns to ensure counter stays in sync
        byte[] decrypted1;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(encrypted);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (CryptoInputStream in = new CryptoInputStream(bais, key)) {
                // Read one byte at a time for first 100 bytes
                for (int i = 0; i < 100; i++) {
                    int b = in.read();
                    if (b == -1) break;
                    baos.write(b);
                }
                // Then read in chunks
                byte[] buffer = new byte[1000];
                while (true) {
                    int n = in.read(buffer);
                    if (n == -1) break;
                    baos.write(buffer, 0, n);
                }
            }
            decrypted1 = baos.toByteArray();
        }

        Assert.assertArrayEquals(originalData, decrypted1);
    }

    @Test
    public void emptyStream() throws Exception {
        byte[] key = Native.generateKey();
        byte[] emptyData = new byte[0];

        byte[] encrypted;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (CryptoOutputStream out = new CryptoOutputStream(baos, key)) {
                out.write(emptyData);
            }
            encrypted = baos.toByteArray();
        }

        byte[] decrypted;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(encrypted)) {
            try (CryptoInputStream in = new CryptoInputStream(bais, key)) {
                Assert.assertEquals(-1, in.read());
            }
        }
    }

    @Test
    public void partialBlockReadWrite() throws Exception {
        int bs = Native.blockSize();
        byte[] key = Native.generateKey();

        // Test data that's not a multiple of block size
        byte[] testData = new byte[bs / 2 + 7];
        new SecureRandom().nextBytes(testData);

        byte[] encrypted;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (CryptoOutputStream out = new CryptoOutputStream(baos, key)) {
                out.write(testData);
            }
            encrypted = baos.toByteArray();
        }

        byte[] decrypted;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(encrypted);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (CryptoInputStream in = new CryptoInputStream(bais, key)) {
                byte[] buffer = new byte[100];
                while (true) {
                    int n = in.read(buffer);
                    if (n == -1) break;
                    baos.write(buffer, 0, n);
                }
            }
            decrypted = baos.toByteArray();
        }

        Assert.assertArrayEquals(testData, decrypted);
    }

    @Test(expected = IllegalStateException.class)
    public void readAfterClose() throws Exception {
        byte[] key = Native.generateKey();
        byte[] testData = "test".getBytes();

        byte[] encrypted;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (CryptoOutputStream out = new CryptoOutputStream(baos, key)) {
                out.write(testData);
            }
            encrypted = baos.toByteArray();
        }

        CryptoInputStream in = new CryptoInputStream(new ByteArrayInputStream(encrypted), key);
        in.close();
        in.read(); // Should throw
    }

    @Test(expected = IllegalStateException.class)
    public void writeAfterClose() throws Exception {
        byte[] key = Native.generateKey();

        CryptoOutputStream out = new CryptoOutputStream(new ByteArrayOutputStream(), key);
        out.close();
        out.write(42); // Should throw
    }

    @Test
    public void multipleReadsAtEOF() throws Exception {
        byte[] key = Native.generateKey();
        byte[] testData = "test".getBytes();

        byte[] encrypted;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (CryptoOutputStream out = new CryptoOutputStream(baos, key)) {
                out.write(testData);
            }
            encrypted = baos.toByteArray();
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(encrypted);
             CryptoInputStream in = new CryptoInputStream(bais, key)) {
            byte[] buffer = new byte[100];
            int bytesRead = in.read(buffer);
            Assert.assertEquals(testData.length, bytesRead);

            // Multiple reads at EOF should all return -1
            Assert.assertEquals(-1, in.read());
            Assert.assertEquals(-1, in.read());
            Assert.assertEquals(-1, in.read(buffer));
        }
    }

    @Test(expected = EOFException.class)
    public void nonceReadFailure() throws Exception {
        byte[] key = Native.generateKey();
        byte[] tooShort = new byte[Native.nonceSize() - 1]; // Not enough for nonce

        new CryptoInputStream(new ByteArrayInputStream(tooShort), key);
    }

    @Test
    public void skipZero() throws Exception {
        byte[] key = Native.generateKey();
        byte[] testData = "test".getBytes();

        byte[] encrypted;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (CryptoOutputStream out = new CryptoOutputStream(baos, key)) {
                out.write(testData);
            }
            encrypted = baos.toByteArray();
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(encrypted);
             CryptoInputStream in = new CryptoInputStream(bais, key)) {
            long skipped = in.skip(0);
            Assert.assertEquals(0, skipped);

            // Should still be able to read all data
            byte[] buffer = new byte[100];
            int bytesRead = in.read(buffer);
            Assert.assertEquals(testData.length, bytesRead);
        }
    }

    @Test
    public void skipNegative() throws Exception {
        byte[] key = Native.generateKey();
        byte[] testData = "test".getBytes();

        byte[] encrypted;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (CryptoOutputStream out = new CryptoOutputStream(baos, key)) {
                out.write(testData);
            }
            encrypted = baos.toByteArray();
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(encrypted);
             CryptoInputStream in = new CryptoInputStream(bais, key)) {
            long skipped = in.skip(-5);
            Assert.assertEquals(0, skipped);
        }
    }

    @Test
    public void skipAtEOF() throws Exception {
        byte[] key = Native.generateKey();
        byte[] testData = "test".getBytes();

        byte[] encrypted;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (CryptoOutputStream out = new CryptoOutputStream(baos, key)) {
                out.write(testData);
            }
            encrypted = baos.toByteArray();
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(encrypted);
             CryptoInputStream in = new CryptoInputStream(bais, key)) {
            // Read all data
            byte[] buffer = new byte[100];
            in.read(buffer);

            // Skip at EOF should return 0
            long skipped = in.skip(100);
            Assert.assertEquals(0, skipped);
        }
    }

    @Test
    public void exactlyOneBlock() throws Exception {
        int bs = Native.blockSize();
        byte[] key = Native.generateKey();
        byte[] testData = new byte[bs];
        new SecureRandom().nextBytes(testData);

        byte[] encrypted;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (CryptoOutputStream out = new CryptoOutputStream(baos, key)) {
                out.write(testData);
            }
            encrypted = baos.toByteArray();
        }

        byte[] decrypted;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(encrypted);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (CryptoInputStream in = new CryptoInputStream(bais, key)) {
                byte[] buffer = new byte[bs * 2];
                while (true) {
                    int n = in.read(buffer);
                    if (n == -1) break;
                    baos.write(buffer, 0, n);
                }
            }
            decrypted = baos.toByteArray();
        }

        Assert.assertArrayEquals(testData, decrypted);
    }

    @Test
    public void oneBlockPlusOneByte() throws Exception {
        int bs = Native.blockSize();
        byte[] key = Native.generateKey();
        byte[] testData = new byte[bs + 1];
        new SecureRandom().nextBytes(testData);

        byte[] encrypted;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (CryptoOutputStream out = new CryptoOutputStream(baos, key)) {
                out.write(testData);
            }
            encrypted = baos.toByteArray();
        }

        byte[] decrypted;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(encrypted);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (CryptoInputStream in = new CryptoInputStream(bais, key)) {
                byte[] buffer = new byte[bs * 2];
                while (true) {
                    int n = in.read(buffer);
                    if (n == -1) break;
                    baos.write(buffer, 0, n);
                }
            }
            decrypted = baos.toByteArray();
        }

        Assert.assertArrayEquals(testData, decrypted);
    }

    @Test
    public void oneBlockMinusOneByte() throws Exception {
        int bs = Native.blockSize();
        byte[] key = Native.generateKey();
        byte[] testData = new byte[bs - 1];
        new SecureRandom().nextBytes(testData);

        byte[] encrypted;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (CryptoOutputStream out = new CryptoOutputStream(baos, key)) {
                out.write(testData);
            }
            encrypted = baos.toByteArray();
        }

        byte[] decrypted;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(encrypted);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (CryptoInputStream in = new CryptoInputStream(bais, key)) {
                byte[] buffer = new byte[bs * 2];
                while (true) {
                    int n = in.read(buffer);
                    if (n == -1) break;
                    baos.write(buffer, 0, n);
                }
            }
            decrypted = baos.toByteArray();
        }

        Assert.assertArrayEquals(testData, decrypted);
    }

    @Test
    public void skipBlocksAfterReadingSomeData() throws Exception {
        int bs = Native.blockSize();
        byte[] key = Native.generateKey();
        byte[] bytes = new byte[bs * 4];
        Arrays.fill(bytes, bs * 0, bs * 1, (byte) 1);
        Arrays.fill(bytes, bs * 1, bs * 2, (byte) 2);
        Arrays.fill(bytes, bs * 2, bs * 3, (byte) 3);
        Arrays.fill(bytes, bs * 3, bs * 4, (byte) 4);

        byte[] encrypted;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (CryptoOutputStream out = new CryptoOutputStream(baos, key)) {
                out.write(bytes);
            }
            encrypted = baos.toByteArray();
        }

        byte[] lastBlock = new byte[bs];
        byte[] expected = new byte[bs];
        Arrays.fill(expected, 0, expected.length, (byte) 4);

        try (ByteArrayInputStream bais = new ByteArrayInputStream(encrypted)) {
            try (CryptoInputStream in = new CryptoInputStream(bais, key)) {
                Assert.assertEquals(1, in.read());

                long skipped = in.skip(bs * 3 - 1);
                Assert.assertEquals(bs * 3 - 1, skipped);

                int read = 0;
                while (read < bs) {
                    int n = in.read(lastBlock, read, lastBlock.length - read);
                    if (n == -1) {
                        break;
                    }
                    read += n;
                }

                Assert.assertEquals(bs, read);
                Assert.assertArrayEquals(expected, lastBlock);
            }
        }
    }
}
