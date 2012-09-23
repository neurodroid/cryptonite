#! /bin/bash

git clone https://github.com/seth-hg/fuse-android.git
find ./fuse-android \( -name "*.c" -o -name "*.h" \) -exec sed -i 's/off_t/loff_t/g' {} \;
patch -p1 < ./fuse-android.patch
