#! /bin/bash

rm -rf bin/*
ant debug
adb uninstall csh.encfsandroid
adb install bin/encfs-android-debug.apk
