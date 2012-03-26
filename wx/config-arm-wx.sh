#! /bin/bash

TOOLCHAIN=${HOME}/android-toolchain-crystax
SYSROOT=${TOOLCHAIN}/sysroot
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

# LIBSTDCXXINC="-I${NDKDIR}/sources/cxx-stl/gnu-libstdc++/include -I${NDKDIR}/sources/cxx-stl/gnu-libstdc++/libs/${ARCH}/include"
# LIBSTDCXXLIB="-L${NDKDIR}/sources/cxx-stl/gnu-libstdc++/libs/${ARCH} -lgnustl_static"
# LIBSTDCXXINC="-I${NDKDIR}/sources/cxx-stl/stlport/stlport"
# LIBSTDCXXLIB="-L${NDKDIR}/sources/cxx-stl/stlport/libs/${ARCH} -lstlport_static"
LIBSTDCXXINC="-L${TOOLCHAIN}/arm-linux-androideabi/include/c++/4.6.3"
LIBSTDCXXLIB="-L${TOOLCHAIN}/arm-linux-androideabi/lib/${ARCH} -lstdc++"

TARGET=`pwd`/${ARCH}

AR=${MYAR} RANLIB=${MYRANLIB} NM=${MYNM} STRIP=${MYSTRIP} CC=${MYAGCC} CXX=${MYAGCC} \
    PKG_CONFIG="" \
    CPPFLAGS="-I${SYSROOT}/usr/include -DUNICODE" \
    CXXFLAGS="${LIBSTDCXXINC} -fexceptions -frtti" \
    LDFLAGS="${LIBSTDCXXLIB} -lgcc" \
    ../configure \
        --disable-gui \
        --enable-unicode \
        --without-libjpeg \
        --without-libpng \
        --without-regex \
        --without-libtiff \
        --without-zlib \
        --without-expat \
        --without-sdl \
        --with-sdl-prefix=${TOOLCHAIN}/usr \
        --enable-cmdline \
        --disable-svg \
        --disable-dragimage \
        --disable-toolbar \
        --disable-toolbook \
        --disable-fontmap \
        --disable-precomp-headers \
        --enable-stl \
        --prefix=${TARGET} \
        --host=arm-eabi \
        --build=x86-linux \
        --enable-static \
        --disable-shared

# cp ./setup.h.0 ./lib/wx/include/arm-eabi-base-unicode-release-static-2.8/wx/setup.h
