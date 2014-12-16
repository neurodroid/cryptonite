# Don't attempt a parallel build! (-jN)

OPENSSL_VERSION="1.0.0o"

cp -v Makefile.android openssl-${OPENSSL_VERSION}
cp -v Makefile.android.armv7 openssl-${OPENSSL_VERSION}
cd openssl-${OPENSSL_VERSION}

rm -rf libcrypto.a libssl.a armeabi

make -f Makefile.android clean
make -f Makefile.android
mkdir -p armeabi
mv libssl.a armeabi
mv libcrypto.a armeabi

rm -rf libcrypto.a libssl.a armeabi-v7a

make -f Makefile.android.armv7 clean
make -f Makefile.android.armv7
mkdir -p armeabi-v7a
mv libssl.a armeabi-v7a
mv libcrypto.a armeabi-v7a
