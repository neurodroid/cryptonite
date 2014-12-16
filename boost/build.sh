#! /bin/bash

cd boost_1_46_1
cp -v ../user-config.jam ./tools/build/v2/
bjam -j12 --without-python --disable-filesystem3 toolset=gcc-android4.8 link=static runtime-link=static target-os=linux --stagedir=android --user-config=./tools/build/v2/user-config.jam
cd ..
