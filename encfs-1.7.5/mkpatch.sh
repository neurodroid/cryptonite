#! /bin/bash

diff -x '*.in' -x '*.o' -x '*.lo' -x '*.1' -ur ${HOME}/encfs-1.7.5/ ./encfs-1.7.5/ > encfs-android.patch.new
