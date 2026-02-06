// Copyright 2026 Benjamin Bader
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License. You may obtain a copy
// of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// License for the specific language governing permissions and limitations
// under the License.

package com.bendb.cryptostream;

import androidx.annotation.NonNull;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class CryptoInputStream extends FilterInputStream {

    private final byte[] key;
    private final byte[] nonce = new byte[Native.nonceSize()];

    private final byte[] buffer = new byte[Native.blockSize()];
    private int bufferIndex = 0;
    private long counter = 0;

    private boolean eof = false;
    private boolean closed = false;

    public CryptoInputStream(@NonNull InputStream in, @NonNull byte[] key) throws IOException {
        super(in);

        if (key.length != Native.keySize()) {
            throw new IllegalArgumentException("Invalid key");
        }

        this.key = key.clone();

        int bytesRead = 0;
        do {
            int n = in.read(nonce, bytesRead, nonce.length - bytesRead);
            if (n < 0) {
                throw new EOFException();
            }
            bytesRead += n;
        } while (bytesRead < nonce.length);
    }

    @Override
    public int read() throws IOException {
        if (bufferIndex != 0) {
            int result = buffer[0] & 0xFF;
            System.arraycopy(buffer, 1, buffer, 0, bufferIndex - 1);
            bufferIndex--;
            return result;
        }

        byte[] temp = new byte[1];
        if (read(temp) == -1) {
            return -1;
        }

        return temp[0] & 0xFF;
    }

    @Override
    public int read(@NonNull byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(@NonNull byte[] output, final int offset, final int length) throws IOException {
        checkNotClosed();

        if (offset < 0) {
            throw new IllegalArgumentException("offset cannot be negative");
        }

        if (length < 0) {
            throw new IllegalArgumentException("length cannot be negative");
        }

        if (length + offset > output.length) {
            throw new IllegalArgumentException("invalid length + offset");
        }

        int bytesToDeliver = length;
        int currentOffset = offset;

        if (bufferIndex != 0) {
            if (bufferIndex < length) {
                int toCopy = bufferIndex;
                System.arraycopy(buffer, 0, output, currentOffset, toCopy);
                System.arraycopy(buffer, toCopy, buffer, 0, bufferIndex - toCopy);

                bufferIndex = 0;
                bytesToDeliver -= toCopy;
                currentOffset += toCopy;

                Arrays.fill(buffer, bufferIndex, buffer.length, (byte) 0);
            } else {
                System.arraycopy(buffer, 0, output, currentOffset, length);
                System.arraycopy(buffer, length, buffer, 0, bufferIndex - length);

                bufferIndex -= length;
                Arrays.fill(buffer, bufferIndex, buffer.length, (byte) 0);

                return length;
            }
        }

        if (eof) {
            if (bytesToDeliver != length) {

                // If we're here, then the buffer is empty and we've already
                // hit EOF.  We no longer need to keep any of our secret data.
                Arrays.fill(key, 0, key.length, (byte) 0);
                Arrays.fill(nonce, 0, nonce.length, (byte) 0);

                return length - bytesToDeliver;
            } else {
                return -1;
            }
        }

        // TODO: If > 0 whole blocks are requested, read them into a temp buffer
        //       and decrypt them all at once; the current implementation is simple
        //       but naive.

        while (bytesToDeliver > 0) {
            fillNextBlock();
            if (bufferIndex == 0) {
                break;
            }

            int toCopy = Math.min(bytesToDeliver, bufferIndex);
            System.arraycopy(buffer, 0, output, currentOffset, toCopy);
            System.arraycopy(buffer, toCopy, buffer, 0, bufferIndex - toCopy);
            Arrays.fill(buffer, bufferIndex, buffer.length, (byte) 0);

            bytesToDeliver -= toCopy;
            currentOffset += toCopy;
            bufferIndex -= toCopy;
        }

        return length - bytesToDeliver;
    }

    private void fillNextBlock() throws IOException {
        if (eof) {
            return;
        }

        if (bufferIndex != 0) {
            throw new IllegalStateException("Cannot fill next block before the current block has been consumed");
        }

        int bytesRead = 0;
        do {
            int n = in.read(buffer, bytesRead, buffer.length - bytesRead);
            if (n < 0) {
                break;
            }
            bytesRead += n;
        } while (bytesRead != buffer.length);

        if (bytesRead < buffer.length) {
            eof = true;
        }

        bufferIndex = bytesRead;

        Native.decrypt(key, nonce, counter, buffer, bytesRead);

        counter += bytesRead / 64;
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public synchronized void mark(int readlimit) {
        // noop
    }

    @Override
    public synchronized void reset() throws IOException {
        throw new IOException("mark/reset not supported");
    }

    @Override
    public long skip(long n) throws IOException {
        checkNotClosed();

        if (n > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("n is too large");
        }

        int toSkip = (int) n;

        if (bufferIndex != 0) {
            if (bufferIndex < toSkip) {
                toSkip -= bufferIndex;
                bufferIndex = 0;
                Arrays.fill(buffer, 0, buffer.length, (byte) 0);
            } else {
                System.arraycopy(buffer, toSkip, buffer, 0, bufferIndex - toSkip);
                bufferIndex -= toSkip;
                Arrays.fill(buffer, bufferIndex, buffer.length, (byte) 0);

                return toSkip;
            }
        }

        if (eof) {
            return n - toSkip;
        }

        final int blocksToSkip = toSkip / buffer.length;
        if (blocksToSkip > 0) {
            final int blocksToSkipInBytes = blocksToSkip * buffer.length;
            int skipped = 0;
            while (skipped < blocksToSkipInBytes) {
                long s = super.skip(blocksToSkipInBytes - skipped);
                if (s <= 0) {
                    eof = true;
                    break;
                }
                skipped += s;
                toSkip -= s;
            }

            counter += skipped / 64;
        }

        if (toSkip > 0 && !eof) {
            fillNextBlock();
            int bytesToDiscard = Math.min(bufferIndex, toSkip);
            if (bytesToDiscard > 0) {
                System.arraycopy(buffer, bytesToDiscard, buffer, 0, bufferIndex - bytesToDiscard);
                bufferIndex -= bytesToDiscard;
                Arrays.fill(buffer, bufferIndex, buffer.length, (byte) 0);
                toSkip -= bytesToDiscard;
            }
        }

        return n - toSkip;
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;

        Arrays.fill(buffer, 0, buffer.length, (byte) 0);
        Arrays.fill(nonce, 0, nonce.length, (byte) 0);
        Arrays.fill(key, 0, key.length, (byte) 0);
        bufferIndex = 0;
        counter = 0;

        super.close();
    }

    private void checkNotClosed() {
        if (closed) {
            throw new IllegalStateException("Stream closed");
        }
    }
}
