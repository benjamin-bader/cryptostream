package com.bendb.cryptostream;

import android.support.annotation.NonNull;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * An OutputStream decorator that encrypts bytes using a stream cipher
 * and writes the resulting ciphertext to the underlying stream.
 */
public class CryptoOutputStream extends FilterOutputStream {

    private final byte[] key;
    private final byte[] nonce = Native.generateNonce();

    private final byte[] buffer = new byte[Native.blockSize()];
    private int bufferIndex = 0;
    private long counter = 0;

    private boolean eof = false;
    private boolean closed = false;

    public CryptoOutputStream(@NonNull OutputStream out, @NonNull byte[] key) throws IOException {
        super(out);

        if (key.length != Native.keySize()) {
            throw new IllegalArgumentException("Invalid key");
        }

        this.key = key.clone();

        out.write(nonce);
    }

    @Override
    public void write(final int b) throws IOException {
        write(new byte[] { (byte) b });
    }

    @Override
    public void write(@NonNull final byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(@NonNull final byte[] b, final int off, final int len) throws IOException {
        checkNotClosed();
        checkNotEOF();

        int currentOffset = off;
        int bytesRemaining = len;

        if (bufferIndex != 0) {
            if (bufferIndex + len < buffer.length) {
                System.arraycopy(b, currentOffset, buffer, bufferIndex, len);
                bufferIndex += len;
                return;
            } else {
                final int toCopy = buffer.length - bufferIndex;
                System.arraycopy(b, currentOffset, buffer, bufferIndex, toCopy);

                Native.encrypt(key, nonce, counter, buffer, buffer.length);
                counter += buffer.length / 64;
                out.write(buffer);

                currentOffset += toCopy;
                bytesRemaining -= toCopy;
                bufferIndex = 0;
            }
        }

        final int blocksToWrite = bytesRemaining / buffer.length;
        if (blocksToWrite > 0) {
            final int blocksToWriteInBytes = blocksToWrite * buffer.length;

            final byte[] temp = new byte[blocksToWriteInBytes];
            System.arraycopy(b, currentOffset, temp, 0, blocksToWriteInBytes);
            Native.encrypt(key, nonce, counter, temp, blocksToWriteInBytes);
            counter += blocksToWriteInBytes / 64;
            out.write(temp);

            Arrays.fill(temp, 0, temp.length, (byte) 0);

            bytesRemaining -= blocksToWriteInBytes;
            currentOffset += blocksToWriteInBytes;
        }

        if (bytesRemaining > 0) {
            System.arraycopy(b, currentOffset, buffer, 0, bytesRemaining);
            bufferIndex = bytesRemaining;
            Arrays.fill(buffer, bufferIndex, buffer.length, (byte) 0);
        }
    }

    @Override
    public void flush() throws IOException {
        // noop
    }

    public void flushFinalBlock() throws IOException {
        checkNotClosed();
        checkNotEOF();

        eof = true;

        if (bufferIndex > 0) {
            Native.encrypt(key, nonce, counter, buffer, bufferIndex);
            out.write(buffer, 0, bufferIndex);
        }

        out.flush();

        counter = 0;
        bufferIndex = 0;
        Arrays.fill(buffer, 0, buffer.length, (byte) 0);
        Arrays.fill(nonce, 0, nonce.length, (byte) 0);
        Arrays.fill(key, 0, key.length, (byte) 0);
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }

        if (!eof) {
            flushFinalBlock();
        }

        closed = true;

        super.close();
    }

    private void checkNotClosed() {
        if (closed) {
            throw new IllegalStateException("Stream closed.");
        }
    }

    private void checkNotEOF() {
        if (eof) {
            throw new IllegalStateException("Stream is already completed");
        }
    }
}
