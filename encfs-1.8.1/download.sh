#! /bin/bash
GSED=`which gsed`
if [ "${GSED}" = "" ]
then
    GSED=`which sed`
fi

GLIBTOOLIZE=`which glibtoolize`
if [ "${GLIBTOOLIZE}" = "" ]
then
    GLIBTOOLIZE=`which libtoolize`
fi

if [ ! -f encfs-1.8.1.tar.gz ]
then
    wget https://github.com/vgough/encfs/releases/download/v1.8.1/encfs-1.8.1.tar.gz
fi

rm -rf encfs-1.8.1
tar -xzf encfs-1.8.1.tar.gz
cd encfs-1.8.1

patch -p1 < ../encfs-android.patch
mkdir -p build-aux
touch build-aux/config.rpath
touch build-aux/ltmain.sh
# find ./ \( -name "*.c" -o -name "*.h" -o -name "*.cpp" \) -exec ${GSED} -i"" 's/off_t/loff_t/g' {} \;
AUTOMAKE="automake --add-missing" autoreconf --force
${GLIBTOOLIZE}
cd ..
