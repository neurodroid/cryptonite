#! /bin/bash

APPKEY=`cat ./cryptonite/AndroidManifest.xml.key`
sed -i 's/'$APPKEY'/db-/' ./cryptonite/AndroidManifest.xml
