#! /bin/bash

rm -rf bin/*
ant debug
adb uninstall csh.cryptonite
adb install bin/cryptonite-debug.apk
