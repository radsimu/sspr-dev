#!/bin/bash

SEPARATOR=":"
if [[ $(uname -a) == *"MINGW"* ]] 
then
SEPARATOR=";" 
fi

export CLASSPATH="../out/production/InputEnrichFeatureAdapter/"$SEPARATOR"../out/production/maltparser-1.9.0 - sspr/"$SEPARATOR"../lib/*"
java -Xms1300m -Dfile.encoding=utf-8 AddFeatureColumns $1 $2
