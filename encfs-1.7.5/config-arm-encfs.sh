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
FUSEDIR=`pwd`/../../fuse28
OPENSSLDIR=`pwd`/../../openssl/openssl-1.0.0q
BOOSTDIR=`pwd`/../../boost/boost_1_46_1
RLOGDIR=`pwd`/../../rlog/rlog-1.4
MYAR=${TOOLCHAIN}/bin/arm-linux-androideabi-ar
MYRANLIB=${TOOLCHAIN}/bin/arm-linux-androideabi-ranlib
MYNM=${TOOLCHAIN}/bin/arm-linux-androideabi-nm
MYSTRIP=${TOOLCHAIN}/bin/arm-linux-androideabi-strip

if test -n "$1"; then
    MYAGCC=agcc-vfp
    ARCH=armeabi-v7a
    CXXDIR="armv7-a/"
else
    MYAGCC=agcc
    ARCH=armeabi
    CXXDIR=""
fi

LIBSTDCXXLIB="${TOOLCHAIN}/arm-linux-androideabi/lib/${CXXDIR}libstdc++.a"

TARGET=`pwd`/${ARCH}

AR=${MYAR} RANLIB=${MYRANLIB} NM=${MYNM} STRIP=${MYSTRIP} CC=${MYAGCC} CXX=${MYAGCC} \
    RLOG_CFLAGS="-I${RLOGDIR}/${ARCH}/include" \
    RLOG_LIBS="-L${RLOGDIR}/${ARCH}/lib -lrlog" \
    OPENSSL_CFLAGS="-DOPENSSL_NO_ENGINE -DHAVE_EVP_AES -DHAVE_EVP_BF -D__STDC_FORMAT_MACROS -I${OPENSSLDIR}/include" \
    OPENSSL_LIBS="${OPENSSLDIR}/${ARCH}/libssl.a ${OPENSSLDIR}/${ARCH}/libcrypto.a -ldl" \
    CPPFLAGS="-DBOOST_FILESYSTEM_VERSION=2 -I${TOOLCHAIN}/sysroot/usr/include -I${FUSEDIR}/jni/include -I${BOOSTDIR} ${RLOG_CFLAGS} ${OPENSSL_CFLAGS}" \
    CXXFLAGS="-fexceptions -frtti -fPIE" \
    LDFLAGS="${LIBSTDCXXLIB} -L${BOOSTDIR}/android/lib -L${FUSEDIR}/obj/local/${ARCH} -lgcc -lfuse -fPIE -pie ${RLOG_LIBS} ${OPENSSL_LIBS}" \
    ./configure \
         --prefix=${TARGET} \
         --host=arm-eabi --build=x86-linux \
         --enable-static --disable-shared \
         --with-boost=${BOOSTDIR}/android
