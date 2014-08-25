#! /bin/bash

GSED=`which gsed`
if [ "${GSED}" = "" ]
then
    GSED=`which sed`
fi

APPKEY1=`cat ./cryptonite/AndroidManifest.xml.key`
APPKEY2=`cat ./cryptonite/AndroidManifest.xml.key2`

${GSED} -i 's/db1-/'$APPKEY1'/' ./cryptonite/AndroidManifest.xml
${GSED} -i 's/db2-/'$APPKEY2'/' ./cryptonite/AndroidManifest.xml
