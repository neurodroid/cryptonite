#! /bin/bash

NDKDIR=${HOME}/android-ndk-r7
TOOLCHAIN=${HOME}/android-toolchain
OPENSSL=${HOME}/openssl-android
MYAR=arm-linux-androideabi-ar
MYRANLIB=arm-linux-androideabi-ranlib
MYNM=arm-linux-androideabi-nm
MYSTRIP=arm-linux-androideabi-strip

if test -n "$1"; then
    MYAGCC=agcc-vfp
    ARCH=armeabi-v7a
else
    MYAGCC=agcc
    ARCH=armeabi
fi

LIBSTDCXXLIB="-L${NDKDIR}/sources/cxx-stl/gnu-libstdc++/libs/armeabi -lgnustl_static"

TARGET=`pwd`/${ARCH}

AR=${MYAR} RANLIB=${MYRANLIB} NM=${MYNM} STRIP=${MYSTRIP} CC=${MYAGCC} CXX=${MYAGCC} \
    PKG_CONFIG="" \
    CPPFLAGS="-I${TOOLCHAIN}/sysroot/usr/include" \
    CXXFLAGS="-fexceptions -frtti" \
    LDFLAGS="${LIBSTDCXXLIB} -L$TOOLCHAIN/lib/gcc/arm-linux-androideabi/4.4.3 -lgcc" \
    ./configure --prefix=${TARGET} --host=x86-linux --build=arm-eabi --enable-static --disable-shared
