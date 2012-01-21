#! /bin/bash

APPKEY=`cat ./cryptonite/AndroidManifest.xml.key`
sed -i 's/db-/'$APPKEY'/' ./cryptonite/AndroidManifest.xml
