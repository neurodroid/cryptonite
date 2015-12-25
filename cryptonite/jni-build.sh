#! /bin/bash

~/android-ndk-r10e/ndk-build V=1 APP_STL=gnustl_static NDK_TOOLCHAIN_VERSION=4.8 APP_ABI="armeabi armeabi-v7a" clean
rm -rf libs/armeabi/* obj/local/armeabi/*
rm -rf libs/armeabi-v7a/* obj/local/armeabi-v7a/*
./cplibs-static.py
~/android-ndk-r10e/ndk-build V=1 APP_STL=gnustl_static NDK_TOOLCHAIN_VERSION=4.8 APP_ABI="armeabi armeabi-v7a" -j2
