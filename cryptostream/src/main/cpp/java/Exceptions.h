#ifndef CRYPTOSTREAM_EXCEPTIONS_H
#define CRYPTOSTREAM_EXCEPTIONS_H

#include <jni.h>

void cacheExceptionClasses(JNIEnv *env);

void throwIllegalStateException(JNIEnv* env, const char* message = "");
void throwIllegalArgumentException(JNIEnv* env, const char* message = "");
void throwNullPointerException(JNIEnv* env, const char* message = "");

#define THROW(type, message) throw##type(env, message)

#define CHECK(condition, message) do { \
  if (!(condition)) { \
    THROW(IllegalArgumentException, (message)); \
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
  } \
} while(false)

#endif //CRYPTOSTREAM_EXCEPTIONS_H
