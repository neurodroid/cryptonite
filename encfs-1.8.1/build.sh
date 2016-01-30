#! /bin/bash

TOOLCHAIN=${HOME}/android-toolchain
MYSTRIP=${TOOLCHAIN}/bin/arm-linux-androideabi-strip
GLIBTOOLIZE=`which glibtoolize`
if [ "${GLIBTOOLIZE}" = "" ]
then
    GLIBTOOLIZE=`which libtoolize`
fi

cd encfs

mkdir -p build-aux
touch build-aux/config.rpath
touch build-aux/ltmain.sh
AUTOMAKE="automake --add-missing" autoreconf --force
${GLIBTOOLIZE}
rm -rf armeabi-v7a
./config-arm-encfs.sh 1
make clean
make -j4
make install
${MYSTRIP} --strip-unneeded armeabi-v7a/bin/encfs
cd ..

rm -rf ../cryptonite/assets/armeabi/encfs*
./cplibs-static.py
