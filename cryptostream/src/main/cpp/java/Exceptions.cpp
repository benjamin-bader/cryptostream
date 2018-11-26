#include "Exceptions.h"

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