#ifndef CRYPTOSTREAM_BYTEARRAY_H
#define CRYPTOSTREAM_BYTEARRAY_H

#include <cstdint>

#include <jni.h>

class ByteArray
{
public:
    ByteArray(JNIEnv* env, jbyteArray javaBytes);
    ~ByteArray();

    size_t BufferLen() const;
    unsigned char* Buffer();

private:
    JNIEnv* m_env;
    jbyteArray m_javaBytes;
    size_t m_byteSize;
    unsigned char* m_bytes;

};


#endif //CRYPTOSTREAM_BYTEARRAY_H
