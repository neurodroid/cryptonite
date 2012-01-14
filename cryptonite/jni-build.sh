#! /bin/bash

rm -rf obj
./cplibs-static.py
~/android-ndk-r7/ndk-build V=1 APP_STL=gnustl_static
