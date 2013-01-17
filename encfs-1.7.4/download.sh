#! /bin/bash
GSED=`which gsed`
if [ "${GSED}" = "" ]
then
    GSED=`which sed`
fi

if [ ! -f encfs-1.7.4.tgz ]
then
    wget http://encfs.googlecode.com/files/encfs-1.7.4.tgz
fi

rm -rf encfs-1.7.4
tar -xzf encfs-1.7.4.tgz
cd encfs-1.7.4
patch -p1 < ../encfs-android.patch
find ./ \( -name "*.c" -o -name "*.h" -o -name "*.cpp" \) -exec ${GSED} -i"" 's/off_t/loff_t/g' {} \;
autoreconf --force
cd ..
