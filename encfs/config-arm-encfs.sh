#! /bin/bash

NDKDIR=${HOME}/android-ndk-r7b
TOOLCHAIN=${NDKDIR}/platforms/android-7/arch-arm
FUSEDIR=`pwd`/../../fuse/fuse-android
OPENSSLDIR=`pwd`/../../openssl/openssl-1.0.0h
BOOSTDIR=`pwd`/../../boost/boost_1_46_1
RLOGDIR=`pwd`/../../rlog/rlog-1.4
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
    RLOG_CFLAGS="-I${RLOGDIR}/${ARCH}/include" \
    RLOG_LIBS="-L${RLOGDIR}/${ARCH}/lib -lrlog" \
    OPENSSL_CFLAGS="-DOPENSSL_NO_ENGINE -DHAVE_EVP_AES -DHAVE_EVP_BF -D__STDC_FORMAT_MACROS -I${OPENSSLDIR}/include" \
    OPENSSL_LIBS="${OPENSSLDIR}/${ARCH}/libssl.a ${OPENSSLDIR}/${ARCH}/libcrypto.a -ldl" \
    CPPFLAGS="-DBOOST_FILESYSTEM_VERSION=2 -I${TOOLCHAIN}/usr/include -I${FUSEDIR}/jni/include -I${BOOSTDIR} ${RLOG_CFLAGS} ${OPENSSL_CFLAGS}" \
    CXXFLAGS="${LIBSTDCXXINC} -fexceptions -frtti" \
    LDFLAGS="${LIBSTDCXXLIB}  ${OPENSSL_LIBS} -L${BOOSTDIR}/android/lib -L${FUSEDIR}/obj/local/${ARCH} -lgcc ${RLOG_LIBS}" \
    ./configure \
         --prefix=${TARGET} \
         --host=x86-linux --build=arm-eabi \
         --enable-static --disable-shared \
         --with-boost=${BOOSTDIR}/android

