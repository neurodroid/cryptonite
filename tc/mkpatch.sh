#! /bin/bash

diff -x '*.in' -x '*.o' -x '*.lo' -x '*.a' -x '*.xml.h' -x '*.1' -x 'bld*' -x '*~' -x '*.d' -ur ${HOME}/truecrypt-7.1a-source ./truecrypt-7.1a-source > tc-android.patch
