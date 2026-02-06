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

#include "Exceptions.h"

#include <jni.h>

namespace {

jclass GetGlobalClassRef(JNIEnv* env, const char* className)
{
    return reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass(className)));
}

}

namespace ExceptionNames {

constexpr const char* IllegalArgumentException = "java/lang/IllegalArgumentException";
constexpr const char* IllegalStateException = "java/lang/IllegalStateException";
constexpr const char* NullPointerException = "java/lang/NullPointerException";

}

namespace Exceptions {

jclass IllegalArgumentException;
jclass IllegalStateException;
jclass NullPointerException;

}

void cacheExceptionClasses(JNIEnv* env)
{
    Exceptions::IllegalArgumentException = GetGlobalClassRef(env, ExceptionNames::IllegalArgumentException);
    Exceptions::IllegalStateException = GetGlobalClassRef(env, ExceptionNames::IllegalStateException);
    Exceptions::NullPointerException = GetGlobalClassRef(env, ExceptionNames::NullPointerException);
}

void freeExceptionClasses(JNIEnv* env)
{
    env->DeleteGlobalRef(Exceptions::IllegalArgumentException);
    env->DeleteGlobalRef(Exceptions::IllegalStateException);
    env->DeleteGlobalRef(Exceptions::NullPointerException);
}

void throwIllegalArgumentException(JNIEnv* env, const char* message)
{
    env->ThrowNew(Exceptions::IllegalArgumentException, message);
}

void throwIllegalStateException(JNIEnv* env, const char* message)
{
    env->ThrowNew(Exceptions::IllegalStateException, message);
}

void throwNullPointerException(JNIEnv* env, const char* message)
{
    env->ThrowNew(Exceptions::NullPointerException, message);
}

// OutOfMemoryError is not cached like other exceptions because:
// 1. It's rarely thrown in practice
// 2. When memory is exhausted, creating new global refs may fail anyway
// 3. FindClass + DeleteLocalRef is safer in low-memory situations
void throwOutOfMemoryError(JNIEnv* env)
{
    jclass cls = env->FindClass("java/lang/OutOfMemoryError");
    if (cls != nullptr) {
        env->ThrowNew(cls, "Native allocation failed");
        env->DeleteLocalRef(cls);
    }
}
