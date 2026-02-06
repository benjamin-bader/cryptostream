cmake_minimum_required(VERSION 3.22)

project(cryptostream)

add_library(sodium STATIC IMPORTED)
set_target_properties(sodium PROPERTIES
    IMPORTED_LOCATION ${SODIUM_DIR}/lib/${CMAKE_ANDROID_ARCH_ABI}/libsodium.a
    INTERFACE_INCLUDE_DIRECTORIES ${SODIUM_DIR}/include
)

add_library(
    cryptostream
    SHARED
    java/ByteArray.cpp
    java/Exceptions.cpp
    CryptoStreamJni.cpp
)

target_compile_definitions(
    cryptostream
    PRIVATE
    -DSODIUM_STATIC=1
)

target_link_libraries(cryptostream
    sodium
)

target_link_options(
    cryptostream
    PRIVATE
    -flto
    -Wl,--gc-sections
    -Wl,-O3
)
