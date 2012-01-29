#! /bin/bash

if test -n $1; then
    if [ "$1" = "1" ]; then
        ./jni-build.sh
    fi
fi

# rm -rf bin/*
ant clean
ant debug
adb -d uninstall csh.cryptonite
adb -d install bin/cryptonite-debug.apk
adb -d shell "am start -a android.intent.action.MAIN -n csh.cryptonite/.Cryptonite"
