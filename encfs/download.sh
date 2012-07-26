#! /bin/bash
if [ ! -f encfs-1.7.4.tgz ]
then
    wget http://encfs.googlecode.com/files/encfs-1.7.4.tgz
fi
if [ ! -d encfs-1.7.4 ]
then
    tar -xzf encfs-1.7.4.tgz
    cd encfs-1.7.4
    patch -p1 < ../encfs-android.patch
    find ./ \( -name "*.c" -o -name "*.h" -o -name "*.cpp" \) -exec sed -i 's/off_t/loff_t/g' {} \;
    autoreconf --force
    cd ..
fi
