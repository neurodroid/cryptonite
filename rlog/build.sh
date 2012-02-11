#! /bin/bash

cd rlog-1.4
cp ../config-arm-rlog.sh ./
./config-arm-rlog.sh
make clean
make -j4
make install
./config-arm-rlog.sh 1
make clean
make -j4
make install
cd ..
