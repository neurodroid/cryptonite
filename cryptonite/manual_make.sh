#! /bin/bash

rm -rf obj
./cplibs-static.py

ARCH=armeabi
NDK=${HOME}/android-ndk-r7
CXX=${NDK}/toolchains/arm-linux-androideabi-4.4.3/prebuilt/linux-x86/bin/arm-linux-androideabi-g++
STRIP=${NDK}/toolchains/arm-linux-androideabi-4.4.3/prebuilt/linux-x86/bin/arm-linux-androideabi-strip
PLATFORM=android-8
TARGETDIR=./obj/local/${ARCH}
INSTALLDIR=./libs/${ARCH}
CXXFLAGS="-fpic -ffunction-sections -funwind-tables -fstack-protector -D__ARM_ARCH_5__ -D__ARM_ARCH_5T__ -D__ARM_ARCH_5E__ -D__ARM_ARCH_5TE__  -Wno-psabi -march=armv5te -mtune=xscale -msoft-float -mthumb -Os -fomit-frame-pointer -fno-strict-aliasing -finline-limit=64 -I../encfs/encfs-1.7.4 -I../encfs/encfs-1.7.4/encfs -I../encfs/encfs-1.7.4/intl -I../fuse/fuse-android/jni/include -I../boost/boost_1_46_1 -I../rlog/rlog-1.4/armeabi/include -I${NDK}/sources/cxx-stl/gnu-libstdc++/include -I${NDK}/sources/cxx-stl/gnu-libstdc++/libs/armeabi/include -Ijni -DANDROID  -Wa,--noexecstack -fexceptions -frtti -D_FILE_OFFSET_BITS=64 -DRLOG_COMPONENT=\"encfs\" -DFUSE_USE_VERSION=26 -D__STDC_FORMAT_MACROS -D__MULTI_THREAD -DOPENSSL_NO_ENGINE -DHAVE_EVP_AES -DHAVE_EVP_BF -O2 -DNDEBUG -g   -I${NDK}/platforms/${PLATFORM}/arch-arm/usr/include"
SRC=cryptonite.cpp
LDFLAGS="-Wl,-soname,libcryptonite.so -shared --sysroot=${NDK}/platforms/${PLATFORM}/arch-arm"
STATIC_LIBS="${TARGETDIR}/libencfs.a ${TARGETDIR}/libfuse.a ${TARGETDIR}/libboost_serialization.a ${TARGETDIR}/librlog.a ${TARGETDIR}/libboost_filesystem.a ${TARGETDIR}/libboost_system.a ${TARGETDIR}/libgnustl_static.a ${NDK}/toolchains/arm-linux-androideabi-4.4.3/prebuilt/linux-x86/bin/../lib/gcc/arm-linux-androideabi/4.4.3/libgcc.a"
SHARED_LIBS="-L../openssl/openssl-android/libs/armeabi/ -lssl -lcrypto"

mkdir -p ${TARGETDIR}/cryptonite/cryptonite
mkdir -p ${TARGETDIR}/objs/cryptonite

${CXX} -MMD -MP -MF ${TARGETDIR}/cryptonite/cryptonite.o.d ${CXXFLAGS} -c  jni/${SRC} -o ${TARGETDIR}/objs/cryptonite/cryptonite.o 

cp -f ${NDK}/sources/cxx-stl/gnu-libstdc++/libs/armeabi/libgnustl_static.a ${TARGETDIR}/libgnustl_static.a

${CXX} ${LDFLAGS} ${TARGETDIR}/objs/cryptonite/cryptonite.o ${STATIC_LIBS} -Wl,--no-undefined -Wl,-z,noexecstack  ${SHARED_LIBS} -lc -lm -o ${TARGETDIR}/libcryptonite.so

mkdir -p ${INSTALLDIR}

install -p ${TARGETDIR}/libcryptonite.so ${INSTALLDIR}/libcryptonite.so

${STRIP} --strip-unneeded  ${INSTALLDIR}/libcryptonite.so
