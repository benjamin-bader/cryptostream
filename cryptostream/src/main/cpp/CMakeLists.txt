cmake_minimum_required(VERSION 3.22)

project(cryptostream)

set(SODIUM_ROOT ${CMAKE_CURRENT_LIST_DIR}/../../../../libsodium)

add_library(sodium STATIC IMPORTED)
set_target_properties(sodium PROPERTIES
        IMPORTED_LOCATION ${SODIUM_ROOT}/lib/${CMAKE_ANDROID_ARCH_ABI}/libsodium.a
        INTERFACE_INCLUDE_DIRECTORIES ${SODIUM_ROOT}/include
)

add_library(
    cryptostream
    SHARED
    java/ByteArray.cpp
    java/Exceptions.cpp
    CryptoStreamJni.cpp
)

target_link_libraries(cryptostream
    sodium
    "-Wl,--gc-sections"
    "-Wl,-O3"
)
