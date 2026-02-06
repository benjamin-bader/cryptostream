// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

#pragma once

#include <jni.h>
#include <optional>

class ByteArray {
    JNIEnv* m_env;
    jbyteArray m_javaBytes;
    void* m_buffer;

public:
    ByteArray(JNIEnv* env, jbyteArray javaBytes, void* buffer);

    ByteArray(ByteArray&& other) noexcept;
    ~ByteArray();

    ByteArray(const ByteArray& other) = delete;
    ByteArray& operator=(const ByteArray& other) = delete;
    ByteArray& operator=(ByteArray&& other) noexcept;

    void Release();

    operator unsigned char*();
    operator const unsigned char*() const;

    static std::optional<ByteArray> Create(JNIEnv* env, jbyteArray javaBytes);
};

template <typename... Args>
void ReleaseAll(const std::optional<ByteArray>& first, const Args&... rest) {
    if (first.has_value()) {
        // Use const_cast because Release() is non-const, but std::optional<ByteArray> is const-ref.
        const_cast<ByteArray&>(first.value()).Release();
    }
    if constexpr (sizeof...(rest) > 0) {
        ReleaseAll(rest...);
    }
}
