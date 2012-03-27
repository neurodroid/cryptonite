#! /bin/bash

TOOLCHAIN=${HOME}/android-toolchain
MYSTRIP=${TOOLCHAIN}/bin/arm-linux-androideabi-strip

cd encfs-1.7.4
cp ../config-arm-encfs.sh ./

rm -rf armeabi
./config-arm-encfs.sh
make clean
make -j4
make install
${MYSTRIP} --strip-unneeded armeabi/bin/encfs

rm -rf armeabi-v7a
./config-arm-encfs.sh 1
make clean
make -j4
make install
${MYSTRIP} --strip-unneeded armeabi-v7a/bin/encfs
cd ..

rm -rf ../cryptonite/assets/armeabi/encfs*
./cplibs-static.py
