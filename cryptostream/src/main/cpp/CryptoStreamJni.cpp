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
    CHECK(length >= 0, "Length cannot be negative");
    
    auto keyBytes = ByteArray::Create(env, key);
    auto nonceBytes = ByteArray::Create(env, nonce);
    auto msgBytes = ByteArray::Create(env, message);

    if (!keyBytes || !nonceBytes || !msgBytes) {
        ReleaseAll(keyBytes, nonceBytes, msgBytes);
        THROW(NullPointerException, "Failed to get byte array elements");
        return;
    }

    crypto_stream_xchacha20_xor_ic(
            *msgBytes,
            *msgBytes,
            static_cast<unsigned long long>(length),
            *nonceBytes,
            static_cast<uint64_t>(counter),
            *keyBytes);
}

} // namespace

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* /* reserved */)
{
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK)
    {
        return -1;
    }

    if (sodium_init() < 0)
    {
        return -1;
    }

    cacheExceptionClasses(env);

    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* /* reserved */)
{
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK)
    {
        return;
    }

    freeExceptionClasses(env);
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
    if (result == nullptr) {
        THROW(NullPointerException, "Failed to create byte array");
        return nullptr;
    }
    env->SetByteArrayRegion(result, 0, crypto_stream_xchacha20_KEYBYTES, reinterpret_cast<jbyte*>(key));

    sodium_memzero(key, sizeof(key));

    return result;
}

JNIEXPORT jbyteArray JNICALL
Java_com_bendb_cryptostream_Native_generateNonce(JNIEnv* env, jclass /* klass */)
{
    unsigned char nonce[crypto_stream_xchacha20_NONCEBYTES];
    randombytes(nonce, crypto_stream_xchacha20_NONCEBYTES);

    jbyteArray result = env->NewByteArray(static_cast<jsize>(crypto_stream_xchacha20_NONCEBYTES));
    if (result == nullptr) {
        THROW(NullPointerException, "Failed to create byte array");
        return nullptr;
    }
    env->SetByteArrayRegion(result, 0, crypto_stream_xchacha20_NONCEBYTES, reinterpret_cast<jbyte*>(nonce));

    sodium_memzero(nonce, sizeof(nonce));

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
