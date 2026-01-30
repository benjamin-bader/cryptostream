package com.bendb.cryptostream;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.junit.Assert.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
    public void crytpoRoundTrip() throws Exception {
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
