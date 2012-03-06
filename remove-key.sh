#! /bin/bash

APPKEY1=`cat ./cryptonite/AndroidManifest.xml.key`
APPKEY2=`cat ./cryptonite/AndroidManifest.xml.key2`

sed -i 's/'$APPKEY1'/db1-/' ./cryptonite/AndroidManifest.xml
sed -i 's/'$APPKEY2'/db2-/' ./cryptonite/AndroidManifest.xml
