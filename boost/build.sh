#! /bin/bash

cd boost_1_46_1
bjam -j4 --without-python toolset=gcc-android4.4.3 link=static runtime-link=static target-os=linux --stagedir=android
cd ..
