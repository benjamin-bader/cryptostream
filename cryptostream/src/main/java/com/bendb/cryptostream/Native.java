package com.bendb.cryptostream;

final class Native {
    static {
        System.loadLibrary("cryptostream");
    }

    static native int blockSize();
    static native int keySize();
    static native int nonceSize();

    static native byte[] generateKey();
    static native byte[] generateNonce();
    static native void encrypt(byte[] key, byte[] nonce, long counter, byte[] message, long length);
    static native void decrypt(byte[] key, byte[] nonce, long counter, byte[] message, long length);

    private Native() {
        // no instances
    }
}
