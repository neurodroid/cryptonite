if [ ! -d truecrypt-7.1a-source ]
then
    tar -xzf TrueCrypt\ 7.1a\ Source.tar.gz
    cd truecrypt-7.1a-source
    patch -p1 < ../tc-android.patch
    mkdir -p Pkcs11
    cd Pkcs11
    wget ftp://ftp.rsasecurity.com/pub/pkcs/pkcs-11/v2-20/pkcs11.h
    wget ftp://ftp.rsasecurity.com/pub/pkcs/pkcs-11/v2-20/pkcs11f.h
    wget ftp://ftp.rsasecurity.com/pub/pkcs/pkcs-11/v2-20/pkcs11t.h
    cd ..
fi
