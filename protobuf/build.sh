#! /bin/bash

cd protobuf-2.4.1
cp ../config-arm-protobuf.sh ./
./config-arm-protobuf.sh
make clean
make -j4
make install
./config-arm-protobuf.sh 1
make clean
make -j4
make install
cd ..
