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

#pragma once

#include <jni.h>

void cacheExceptionClasses(JNIEnv *env);
void freeExceptionClasses(JNIEnv *env);

void throwIllegalStateException(JNIEnv* env, const char* message = "");
void throwIllegalArgumentException(JNIEnv* env, const char* message = "");
void throwNullPointerException(JNIEnv* env, const char* message = "");
void throwOutOfMemoryError(JNIEnv* env);

#define THROW(type, message) throw##type(env, message)

#define CHECK(condition, message) do { \
  if (!(condition)) { \
    THROW(IllegalArgumentException, (message)); \
    return; \
  } \
} while(false)

#define REQUIRE(condition, message) do { \
  if (!(condition)) { \
    THROW(IllegalStateException, (message)); \
    return; \
  } \
} while(false)

#define CHECK_NOT_NULL(jobj, message) do { \
  if (jobj == nullptr) { \
    THROW(NullPointerException, (message)); \
    return; \
  } \
} while(false)
