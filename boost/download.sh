#! /bin/bash

BOOST_DIR="1.60.0"
BOOST_VERSION="1_60_0"

if [ ! -f ./boost_${BOOST_VERSION}.tar.bz2 ]
then
    wget http://downloads.sourceforge.net/project/boost/boost/${BOOST_DIR}/boost_${BOOST_VERSION}.tar.bz2
fi
if [ ! -d ./boost_${BOOST_VERSION} ]
then
    tar -xvjf boost_${BOOST_VERSION}.tar.bz2
    cd boost_${BOOST_VERSION}
    patch -p1 < ../boost-libs-android.patch
    ./bootstrap.sh
    cp -v ../user-config.jam ./
    cd ..
fi
