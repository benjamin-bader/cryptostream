#include "ByteArray.h"

//#include <stdexcept>

ByteArray::ByteArray(JNIEnv *env, jbyteArray javaBytes)
    : m_env(env)
    , m_javaBytes(javaBytes)
    , m_byteSize(0)
    , m_bytes(nullptr)
{
    jboolean isCopy;
    jsize cbJavaBytes = env->GetArrayLength(javaBytes);
    void* bytes = env->GetPrimitiveArrayCritical(javaBytes, &isCopy);
    if (bytes == nullptr || env->ExceptionCheck())
    {
        //throw std::runtime_error("Failed to get byte array elements");
        m_byteSize = 0;
        m_bytes = nullptr;
    }
    else {

        m_byteSize = static_cast<size_t>(cbJavaBytes);
        m_bytes = reinterpret_cast<unsigned char *>(bytes);
    }
}

ByteArray::~ByteArray()
{
    if (m_bytes != nullptr) {
        m_env->ReleasePrimitiveArrayCritical(m_javaBytes, reinterpret_cast<void *>(m_bytes), 0);
    }
}

size_t ByteArray::BufferLen() const
{
    return m_byteSize;
}

unsigned char* ByteArray::Buffer()
{
    return m_bytes;
}