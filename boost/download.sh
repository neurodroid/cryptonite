#! /bin/bash

if [ ! -f ./boost_1_46_1.tar.bz2 ]
then
    wget http://downloads.sourceforge.net/project/boost/boost/1.46.1/boost_1_46_1.tar.bz2
fi
if [ ! -d ./boost_1_46_1 ]
then
    tar -xvjf boost_1_46_1.tar.bz2
    cd boost_1_46_1
    patch -p1 < ../boost-libs-android.patch
    cp -v ../user-config.jam ./tools/build/v2/
    cd ..
fi
