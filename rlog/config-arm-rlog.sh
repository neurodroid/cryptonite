#! /bin/bash

TOOLCHAIN=${HOME}/android-toolchain
SYSROOT=${TOOLCHAIN}/sysroot
MYAR=${TOOLCHAIN}/bin/arm-linux-androideabi-ar
MYRANLIB=${TOOLCHAIN}/bin/arm-linux-androideabi-ranlib
MYNM=${TOOLCHAIN}/bin/arm-linux-androideabi-nm
MYSTRIP=${TOOLCHAIN}/bin/arm-linux-androideabi-strip

if test -n "$1"; then
    MYAGCC=agcc-vfp
    ARCH=armeabi-v7a
else
    MYAGCC=agcc
    ARCH=armeabi
fi

LIBSTDCXXINC=""
LIBSTDCXXLIB="-lstdc++"

TARGET=`pwd`/${ARCH}

AR=${MYAR} RANLIB=${MYRANLIB} NM=${MYNM} STRIP=${MYSTRIP} CC=${MYAGCC} CXX=${MYAGCC} \
    PKG_CONFIG="" \
    CPPFLAGS="-I${SYSROOT}/usr/include" \
    CXXFLAGS="${LIBSTDCXXINC} -fexceptions -frtti" \
    LDFLAGS="${LIBSTDCXXLIB} -lgcc" \
    ./configure \
        --prefix=${TARGET} \
        --host=arm-eabi --build=x86-linux \
        --enable-static --disable-shared \
        --disable-docs
