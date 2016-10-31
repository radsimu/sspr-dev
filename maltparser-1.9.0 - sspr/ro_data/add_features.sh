#!/bin/bash
export CLASSPATH="../out/production/InputEnrichFeatureAdapter/":"../out/production/maltparser-1.9.0 - sspr/":"../lib/*"
java -Xms1300m -Dfile.encoding=utf-8 AddFeatureColumns $1 $2
