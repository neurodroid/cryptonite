#! /bin/bash

# This program is free software; you can redistribute it and/or
# modify it under the terms of the GNU General Public License
# as published by the Free Software Foundation; either version 2
# of the License, or (at your option) any later version.

# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.

# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

# Copyright (c) 2012, Christoph Schmidt-Hieber

TOOLCHAIN=${HOME}/android-toolchain-crystax
SYSROOT=${TOOLCHAIN}/sysroot
MYAR=${TOOLCHAIN}/bin/arm-linux-androideabi-ar
MYRANLIB=${TOOLCHAIN}/bin/arm-linux-androideabi-ranlib
MYNM=${TOOLCHAIN}/bin/arm-linux-androideabi-nm
MYSTRIP=${TOOLCHAIN}/bin/arm-linux-androideabi-strip

if test -n "$1"; then
    MYAGCC=agcc-vfp-crystax
    ARCH=armeabi-v7a
else
    MYAGCC=agcc-crystax
    ARCH=armeabi
fi

# LIBSTDCXXINC="-I${NDKDIR}/sources/cxx-stl/gnu-libstdc++/include -I${NDKDIR}/sources/cxx-stl/gnu-libstdc++/libs/${ARCH}/include"
# LIBSTDCXXLIB="-L${NDKDIR}/sources/cxx-stl/gnu-libstdc++/libs/${ARCH} -lgnustl_static"
# LIBSTDCXXINC="-I${NDKDIR}/sources/cxx-stl/stlport/stlport"
# LIBSTDCXXLIB="-L${NDKDIR}/sources/cxx-stl/stlport/libs/${ARCH} -lstlport_static"
LIBSTDCXXINC=""
LIBSTDCXXLIB="-L${TOOLCHAIN}/arm-linux-androideabi/lib/${ARCH} -lcrystax"

TARGET=`pwd`/${ARCH}

AR=${MYAR} RANLIB=${MYRANLIB} NM=${MYNM} STRIP=${MYSTRIP} CC=${MYAGCC} CXX=${MYAGCC} \
    PKG_CONFIG="" \
    CPPFLAGS="-I${SYSROOT}/usr/include" \
    CXXFLAGS="${LIBSTDCXXINC} -fexceptions -frtti" \
    LDFLAGS="${LIBSTDCXXLIB} -lgcc" \
    ../configure \
        --disable-gui \
        --disable-debug \
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

mkdir -p ./lib/wx/include/arm-eabi-base-unicode-release-static-2.8/wx
cp ./../../setup.h.0 ./lib/wx/include/arm-eabi-base-unicode-release-static-2.8/wx/setup.h
