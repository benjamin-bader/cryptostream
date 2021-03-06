#include <jni.h>

#include "java/ByteArray.h"
#include "java/Exceptions.h"

#include "sodium.h"

namespace {

void xor_block(
        JNIEnv* env,
        jbyteArray key,
        jbyteArray nonce,
        jlong counter,
        jbyteArray message,
        jlong length)
{
    CHECK_NOT_NULL(key, "key");
    CHECK_NOT_NULL(nonce, "nonce");
    CHECK_NOT_NULL(message, "message");
    CHECK(env->GetArrayLength(key) == crypto_stream_xchacha20_KEYBYTES, "Invalid key");
    CHECK(env->GetArrayLength(nonce) == crypto_stream_xchacha20_NONCEBYTES, "Invalid nonce");
    CHECK(length <= env->GetArrayLength(message), "Length is longer than the message");

    ByteArray keyBytes(env, key);
    ByteArray nonceBytes(env, nonce);
    ByteArray msgBytes(env, message);

    CHECK_NOT_NULL(keyBytes.Buffer(), "key bytes");
    CHECK_NOT_NULL(nonceBytes.Buffer(), "nonce bytes");
    CHECK_NOT_NULL(msgBytes.Buffer(), "msg bytes");

    crypto_stream_xchacha20_xor_ic(
            msgBytes.Buffer(),
            msgBytes.Buffer(),
            static_cast<unsigned long long>(length),
            nonceBytes.Buffer(),
            static_cast<uint64_t>(counter),
            keyBytes.Buffer());
}

}

extern "C" {

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* /* reserved */)
{
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK)
    {
        return -1;
    }

    if (sodium_init() != 0)
    {
        return -1;
    }

    cacheExceptionClasses(env);

    return JNI_VERSION_1_6;
}

JNIEXPORT jint JNICALL
Java_com_bendb_cryptostream_Native_blockSize(JNIEnv* /* env */, jclass /* klass */)
{
    return static_cast<jint>(8192);
}

JNIEXPORT jint JNICALL
Java_com_bendb_cryptostream_Native_keySize(JNIEnv* /* env */, jclass /* klass */)
{
    return static_cast<jint>(crypto_stream_xchacha20_KEYBYTES);
}

JNIEXPORT jint JNICALL
Java_com_bendb_cryptostream_Native_nonceSize(JNIEnv* /* env */, jclass /* klass */)
{
    return static_cast<jint>(crypto_stream_xchacha20_NONCEBYTES);
}

JNIEXPORT jbyteArray JNICALL
Java_com_bendb_cryptostream_Native_generateKey(JNIEnv* env, jclass /* klass */)
{
    unsigned char key[crypto_stream_xchacha20_KEYBYTES];
    crypto_stream_xchacha20_keygen(key);

    jbyteArray result = env->NewByteArray(crypto_stream_xchacha20_KEYBYTES);
    env->SetByteArrayRegion(result, 0, crypto_stream_xchacha20_KEYBYTES, reinterpret_cast<jbyte*>(key));

    return result;
}

JNIEXPORT jbyteArray JNICALL
Java_com_bendb_cryptostream_Native_generateNonce(JNIEnv* env, jclass /* klass */)
{
    unsigned char nonce[crypto_stream_xchacha20_NONCEBYTES];
    randombytes(nonce, crypto_stream_xchacha20_NONCEBYTES);

    jbyteArray result = env->NewByteArray(static_cast<jsize>(crypto_stream_xchacha20_NONCEBYTES));
    env->SetByteArrayRegion(result, 0, crypto_stream_xchacha20_NONCEBYTES, reinterpret_cast<jbyte*>(nonce));

    return result;
}

JNIEXPORT void JNICALL
Java_com_bendb_cryptostream_Native_encrypt(
        JNIEnv* env,
        jclass /* klass */,
        jbyteArray key,
        jbyteArray nonce,
        jlong counter,
        jbyteArray message,
        jlong length)
{
    xor_block(env, key, nonce, counter, message, length);
}

JNIEXPORT void JNICALL
Java_com_bendb_cryptostream_Native_decrypt(
        JNIEnv* env,
        jclass /* klass */,
        jbyteArray key,
        jbyteArray nonce,
        jlong counter,
        jbyteArray message,
        jlong length)
{
    xor_block(env, key, nonce, counter, message, length);
}

}