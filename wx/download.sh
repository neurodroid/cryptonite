#! /bin/bash
if [ ! -f wxWidgets-2.8.12.tar.bz2 ]
then
    wget http://downloads.sourceforge.net/project/wxwindows/2.8.12/wxWidgets-2.8.12.tar.bz2
fi
if [ ! -d wxWidgets-2.8.12 ]
then
    tar -xjf wxWidgets-2.8.12.tar.bz2
    cd wxWidgets-2.8.12
    patch -p1 < ../wx-android.patch
    cd ..
fi
