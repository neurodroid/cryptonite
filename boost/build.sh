#! /bin/bash

cd boost_1_46_1
cp -v ../user-config.jam ./tools/build/v2/
bjam -j4 --without-python toolset=gcc-android4.4.3 link=static runtime-link=static target-os=linux --stagedir=android --user-config=./tools/build/v2/user-config.jam
cd ..
