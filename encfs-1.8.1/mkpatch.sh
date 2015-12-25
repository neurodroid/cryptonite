#! /bin/bash

diff -x '*.in' -x '*.o' -x '*.lo' -x '*.1' -ur ${HOME}/encfs-1.8.1/ ./encfs-1.8.1/ > encfs-android.patch.new
