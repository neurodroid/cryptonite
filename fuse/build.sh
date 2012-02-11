#! /bin/bash

cd fuse-android
rm -rf libs/armeabi/* obj/local/armeabi/*
~/android-ndk-r7/ndk-build V=1 APP_STL=gnustl_static APP_ABI=armeabi -j2
rm -rf libs/armeabi-v7a/* obj/local/armeabi-v7a/*
~/android-ndk-r7/ndk-build V=1 APP_STL=gnustl_static APP_ABI=armeabi-v7a -j2
