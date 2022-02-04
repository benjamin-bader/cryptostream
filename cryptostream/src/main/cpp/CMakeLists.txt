cmake_minimum_required(VERSION 3.10.2)

set(SODIUM_ROOT ${CMAKE_CURRENT_LIST_DIR}/../../../../libsodium)

add_library(sodium STATIC IMPORTED)
set_target_properties(sodium PROPERTIES
        IMPORTED_LOCATION ${SODIUM_ROOT}/lib/${CMAKE_ANDROID_ARCH_ABI}/libsodium.a
        INTERFACE_INCLUDE_DIRECTORIES ${SODIUM_ROOT}/include
)

set(
    CMAKE_CXX_FLAGS_RELEASE
    "${CMAKE_CXX_FLAGS_RELEASE} -O3 -flto"
)

set(VERSION_SCRIPT "${CMAKE_CURRENT_SOURCE_DIR}/libcryptostream.map")

set(
    CMAKE_SHARED_LINKER_FLAGS_RELEASE
    "${CMAKE_SHARED_LINKER_FLAGS_RELEASE} -Wl,--version-script=${VERSION_SCRIPT}"
)

add_library(
    cryptostream
    SHARED
    java/ByteArray.cpp
    java/Exceptions.cpp
    CryptoStreamJni.cpp
)


include(CheckIPOSupported)
check_ipo_supported(RESULT supported OUTPUT error)

if(supported)
    set_property(TARGET cryptostream PROPERTY INTERPROCEDURAL_OPTIMIZATION TRUE)
else()
    message(No IPO for you)
endif()

target_link_libraries(cryptostream
    sodium
    "-Wl,--gc-sections"
#    "-Wl,--exclude-libs,ALL"
    "-Wl,-O3"
)
