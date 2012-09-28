#! /bin/bash

ant clean
ant debug
adb uninstall csh.cryptonite.test
adb install bin/cryptonitetest-debug.apk
adb shell am instrument -w csh.cryptonite.test/android.test.InstrumentationTestRunner
