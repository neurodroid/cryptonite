#! /bin/bash

# Haven't managed to have Android.mk put my static libraries in front of
# libgcc.a and libgnustl_static.a so that I had to resort to this manual
# build script.

rm -rf obj
./cplibs-static.py

ARCH=armeabi
NDK=${HOME}/android-ndk-r7
HOSTOS=`uname -s`
case ${HOSTOS} in
'Darwin')
  HOSTOS="darwin"
  ;;
*)
  HOSTOS="linux"
  ;;
esac
TOOLCHAIN=${NDK}/toolchains/arm-linux-androideabi-4.4.3/prebuilt/${HOSTOS}-x86
CXX=${TOOLCHAIN}/bin/arm-linux-androideabi-g++
STRIP=${TOOLCHAIN}/bin/arm-linux-androideabi-strip
PLATFORM=android-8
TARGETDIR=./obj/local/${ARCH}
INSTALLDIR=./libs/${ARCH}
INCLUDES="
    -I../encfs/encfs-1.7.4
    -I../encfs/encfs-1.7.4/encfs
    -I../encfs/encfs-1.7.4/intl
    -I../fuse/fuse-android/jni/include
    -I../boost/boost_1_46_1
    -I../rlog/rlog-1.4/armeabi/include
    -I${NDK}/sources/cxx-stl/gnu-libstdc++/include
    -I${NDK}/sources/cxx-stl/gnu-libstdc++/libs/armeabi/include
    -Ijni
    -I${NDK}/platforms/${PLATFORM}/arch-arm/usr/include"
DEFINES="
    -D__ARM_ARCH_5__
    -D__ARM_ARCH_5T__
    -D__ARM_ARCH_5E__
    -D__ARM_ARCH_5TE__ 
    -DANDROID
    -D_FILE_OFFSET_BITS=64
    -DRLOG_COMPONENT=\"encfs\" 
    -DFUSE_USE_VERSION=26 
    -D__STDC_FORMAT_MACROS
    -D__MULTI_THREAD
    -DOPENSSL_NO_ENGINE
    -DHAVE_EVP_AES
    -DHAVE_EVP_BF
    -DNDEBUG"
CXXFLAGS="${DEFINES} -fpic -ffunction-sections -funwind-tables -fstack-protector   -Wno-psabi -march=armv5te -mtune=xscale -msoft-float -mthumb -Os -fomit-frame-pointer -fno-strict-aliasing -finline-limit=64 ${INCLUDES} -Wa,--noexecstack -fexceptions -frtti  -O2 -g"
SRC=cryptonite.cpp
LDFLAGS="-Wl,-soname,libcryptonite.so -shared --sysroot=${NDK}/platforms/${PLATFORM}/arch-arm"
STATIC_LIBS="
    ${TARGETDIR}/libencfs.a 
    ${TARGETDIR}/libfuse.a 
    ${TARGETDIR}/libboost_serialization.a 
    ${TARGETDIR}/librlog.a 
    ${TARGETDIR}/libboost_filesystem.a 
    ${TARGETDIR}/libboost_system.a 
    ${TARGETDIR}/libstdc++.a 
    ${TARGETDIR}/libgcc.a"
SHARED_LIBS="-L../openssl/openssl-android/libs/armeabi/ -lssl -lcrypto"

mkdir -p ${TARGETDIR}/cryptonite/cryptonite
mkdir -p ${TARGETDIR}/objs/cryptonite

${CXX} -MMD -MP -MF ${TARGETDIR}/cryptonite/cryptonite.o.d ${CXXFLAGS} -c  jni/${SRC} -o ${TARGETDIR}/objs/cryptonite/cryptonite.o 

${CXX} ${LDFLAGS} ${TARGETDIR}/objs/cryptonite/cryptonite.o ${STATIC_LIBS} -Wl,--no-undefined -Wl,-z,noexecstack  ${SHARED_LIBS} -lc -lm -o ${TARGETDIR}/libcryptonite.so

mkdir -p ${INSTALLDIR}

install -p ${TARGETDIR}/libcryptonite.so ${INSTALLDIR}/libcryptonite.so

${STRIP} --strip-unneeded  ${INSTALLDIR}/libcryptonite.so
