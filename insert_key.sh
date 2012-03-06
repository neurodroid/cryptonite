#! /bin/bash

APPKEY1=`cat ./cryptonite/AndroidManifest.xml.key`
APPKEY2=`cat ./cryptonite/AndroidManifest.xml.key2`

sed -i 's/db1-/'$APPKEY1'/' ./cryptonite/AndroidManifest.xml
sed -i 's/db2-/'$APPKEY2'/' ./cryptonite/AndroidManifest.xml
