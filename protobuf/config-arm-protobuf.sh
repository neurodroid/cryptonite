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

LIBSTDCXXINC="-I${TOOLCHAIN}/arm-linux-androideabi/include/c++/4.6 -I${TOOLCHAIN}/arm-linux-androideabi/include/c++/4.6/arm-linux-androideabi"
LIBSTDCXXLIB="-lstdc++"
PROTOC=`which protoc`
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
        --with-protoc=${PROTOC}
