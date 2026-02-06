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

#include "ByteArray.h"

#include <jni.h>

#include <optional>

ByteArray::ByteArray(JNIEnv* env, jbyteArray javaBytes, void* buffer)
    : m_env(env)
    , m_javaBytes(javaBytes)
    , m_buffer(buffer)
{}

ByteArray::ByteArray(ByteArray&& other) noexcept
    : m_env(other.m_env)
    , m_javaBytes(other.m_javaBytes)
    , m_buffer(other.m_buffer)
{
    other.m_buffer = nullptr;
}

ByteArray::~ByteArray()
{
    Release();
}

ByteArray& ByteArray::operator=(ByteArray&& other) noexcept
{
    if (this != &other) {
        Release();
        m_env = other.m_env;
        m_javaBytes = other.m_javaBytes;
        m_buffer = other.m_buffer;
        other.m_buffer = nullptr;
    }
    return *this;
}

void ByteArray::Release()
{
    if (m_buffer != nullptr) {
        m_env->ReleasePrimitiveArrayCritical(m_javaBytes, m_buffer, 0);
        m_buffer = nullptr;
    }
}

ByteArray::operator unsigned char*()
{
    return reinterpret_cast<unsigned char*>(m_buffer);
}

ByteArray::operator const unsigned char*() const
{
    return reinterpret_cast<const unsigned char*>(m_buffer);
}

std::optional<ByteArray> ByteArray::Create(JNIEnv* env, jbyteArray javaBytes)
{
    void* buffer = env->GetPrimitiveArrayCritical(javaBytes, nullptr);
    if (buffer == nullptr) {
        return std::nullopt;
    }
    return ByteArray{env, javaBytes, buffer};
}
