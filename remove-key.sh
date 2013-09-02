#! /bin/bash

GSED=`which gsed`
if [ "${GSED}" = "" ]
then
    GSED=`which sed`
fi

APPKEY1=`cat ./cryptonite/AndroidManifest.xml.key`
APPKEY2=`cat ./cryptonite/AndroidManifest.xml.key2`

${GSED} -i 's/'$APPKEY1'/db1-/' ./cryptonite/AndroidManifest.xml
${GSED} -i 's/'$APPKEY2'/db2-/' ./cryptonite/AndroidManifest.xml
