#! /bin/bash

NDKDIR=${HOME}/android-ndk-r7b
TOOLCHAIN=${HOME}/android-toolchain
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

STDCXXDIR="${NDKDIR}/sources/cxx-stl/gnu-libstdc++"
LIBSTDCXXLIB="-L${STDCXXDIR}/libs/${ARCH} -lgnustl_static"
LIBSTDCXXINC="-I${STDCXXDIR}/include -I${STDCXXDIR}/libs/${ARCH}/include"

TARGET=`pwd`/${ARCH}

AR=${MYAR} RANLIB=${MYRANLIB} NM=${MYNM} STRIP=${MYSTRIP} CC=${MYAGCC} CXX=${MYAGCC} \
    PKG_CONFIG="" \
    CPPFLAGS="-I${TOOLCHAIN}/sysroot/usr/include ${LIBSTDCXXINC}" \
    CXXFLAGS="-fexceptions -frtti" \
    LDFLAGS="${LIBSTDCXXLIB} -L$TOOLCHAIN/lib/gcc/arm-linux-androideabi/4.4.3 -lgcc" \
    ./configure \
        --prefix=${TARGET} \
        --host=arm-eabi --build=x86-linux \
        --enable-static --disable-shared \
        --disable-docs
