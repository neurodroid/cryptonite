#! /bin/bash

TOOLCHAIN="${HOME}/android-toolchain"
AR="${TOOLCHAIN}/bin/arm-linux-androideabi-ar crs"
RANLIB="${TOOLCHAIN}/bin/arm-linux-androideabi-ranlib"
STRIP="${TOOLCHAIN}/bin/arm-linux-androideabi-strip"

cd tinyxml
MARCH=armeabi
make -f ../Makefile.android ARCH=${MARCH}
rm -rf ${MARCH}
mkdir -p ${MARCH}
mv *.o ${MARCH}
cd ${MARCH}
${AR} libtinyxml.a tinyxml.o tinyxmlparser.o tinyxmlerror.o tinystr.o
# ${RANLIB} libtinyxml.a
# ${STRIP} libtinyxml.a
cd ..

MARCH=armeabi-v7a
make -f ../Makefile.android ARCH=${MARCH}
rm -rf ${MARCH}
mkdir -p ${MARCH}
mv *.o ${MARCH}
cd ${MARCH}
${AR} libtinyxml.a tinyxml.o tinyxmlparser.o tinyxmlerror.o tinystr.o
# ${RANLIB} libtinyxml.a
# ${STRIP} libtinyxml.a
cd ..
