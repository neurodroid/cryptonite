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
FUSEDIR=`pwd`/../fuse/fuse-android
OPENSSLDIR=`pwd`/../openssl/openssl-1.0.0j
RLOGDIR=`pwd`/../rlog/rlog-1.4
PROTOBUFDIR=`pwd`/../protobuf/protobuf-2.4.1
TINYXMLDIR=`pwd`/../tinyxml/tinyxml

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

TARGET=`pwd`/${ARCH}

AR=${MYAR} RANLIB=${MYRANLIB} NM=${MYNM} STRIP=${MYSTRIP} CC=${MYAGCC} CXX=${MYAGCC} \
    RLOG_CFLAGS="-I${RLOGDIR}/${ARCH}/include" \
    RLOG_LIBS="-L${RLOGDIR}/${ARCH}/lib -lrlog" \
    PROTOBUF_CFLAGS="-I${PROTOBUFDIR}/${ARCH}/include" \
    PROTOBUF_LIBS="-L${PROTOBUFDIR}/${ARCH}/lib -lprotobuf" \
    OPENSSL_CFLAGS="-DOPENSSL_NO_ENGINE -DHAVE_EVP_AES -DHAVE_EVP_BF -D__STDC_FORMAT_MACROS -I${OPENSSLDIR}/include" \
    OPENSSL_LIBS="${OPENSSLDIR}/${ARCH}/libssl.a ${OPENSSLDIR}/${ARCH}/libcrypto.a -ldl" \
    CPPFLAGS="-I${TOOLCHAIN}/sysroot/usr/include -I${FUSEDIR}/jni/include -I${TINYXMLDIR} ${RLOG_CFLAGS} ${PROTOBUF_CFLAGS} ${OPENSSL_CFLAGS}" \
    CXXFLAGS="${LIBSTDCXXINC} -fexceptions -frtti" \
    LDFLAGS="${LIBSTDCXXLIB} -L${TINYXMLDIR}/${ARCH} ${TINYXMLDIR}/${ARCH}/libtinyxml.a -L${FUSEDIR}/obj/local/${ARCH} -lgcc ${RLOG_LIBS} ${PROTOBUF_LIBS} ${OPENSSL_LIBS}" \
    ./configure \
         --prefix=${TARGET} \
         --host=arm-eabi --build=x86-linux \
         --enable-static --disable-shared
