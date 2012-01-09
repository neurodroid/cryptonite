#! /bin/bash

cd rlog-1.4
cp ../config-arm-rlog.sh ./
./config-arm-rlog.sh
make -j4
cd ..
