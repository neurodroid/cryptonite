#! /bin/bash

diff -x '*.in' -x '*.o' -x '*.lo' -x '*.1' -x 'bld*' -x '*~' -ur ${HOME}/wxWidgets-2.8.12 ./wxWidgets-2.8.12 > wx-android.patch
