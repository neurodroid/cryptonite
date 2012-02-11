#! /bin/bash

cd encfs-1.7.4
cp ../config-arm-encfs.sh ./
./config-arm-encfs.sh
make clean
make -j4
make install
./config-arm-encfs.sh 1
make clean
make -j4
make install
cd ..
./cplibs-static.py
