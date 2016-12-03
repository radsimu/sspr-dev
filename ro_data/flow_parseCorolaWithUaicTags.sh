#!/bin/bash

SEPARATOR=":"
if [[ $(uname -a) == *"MINGW"* ]]; then
SEPARATOR=";" 
fi

export CLASSPATH="../out/production/Dashboard/"$SEPARATOR"../out/production/maltparser-1.9.0-SSPR/"$SEPARATOR"../out/production/AddConllFeatures/"$SEPARATOR"../lib/*"

mkdir -p corola/1_corola_raw/
mkdir -p corola/2_corola_posUaic_xml/
mkdir -p corola/3_corola_conll++/
mkdir -p corola/4_corola_conll++_parsed/
mkdir -p corola/5_corola_conll++_parsed_simple/

java -Xms4g -Dfile.encoding=utf-8 ParseCorola
source flow_clean.sh