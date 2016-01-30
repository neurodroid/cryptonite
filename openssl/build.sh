# Don't attempt a parallel build! (-jN)

OPENSSL_VERSION="1.0.2f"

cd openssl-${OPENSSL_VERSION}

./Configure no-hw no-asm android-armv7

cp -v ../Makefile.android ./

rm -rf libcrypto.a libssl.a armeabi-v7a

make -f Makefile.android clean
make -f Makefile.android
mkdir -p armeabi-v7a
mv libssl.a armeabi-v7a
mv libcrypto.a armeabi-v7a
