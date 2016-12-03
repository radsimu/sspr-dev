#!/bin/bash

SEPARATOR=":"
if [[ $(uname -a) == *"MINGW"* ]]; then
SEPARATOR=";" 
fi

export CLASSPATH="../out/production/Dashboard/"$SEPARATOR"../out/production/AddConllFeatures/"$SEPARATOR"../out/production/maltparser-1.9.0-SSPR/"$SEPARATOR"../lib/*"

echo ===Adding features
java -Xms1300m -Dfile.encoding=utf-8 AddFeatureColumns "batches_taggedWithUaic/ro-ud-racai-uaic-9522-uaic-10.conllx.test" "ro-ud-racai-uaic-9522-uaic-10.conllx.test++"

java -Xms1000m -Dfile.encoding=utf-8 ExtractSimpleSentences "ro-ud-racai-uaic-9522-uaic-10.conllx.test++" "simple_ro-ud-racai-uaic-9522-uaic-10.conllx.test++"

echo ===Evaluating batch on simple sentences only ...
cp "batches_taggedWithUaic/batches_taggedWithUaic_10.uaic.model.mco" "batches_taggedWithUaic_10.uaic.model.mco"
java -Xms1000m -Dfile.encoding=utf-8 org.maltparser.Malt -c "batches_taggedWithUaic_10.uaic.model.mco" -f train_options.xml -i "simple_ro-ud-racai-uaic-9522-uaic-10.conllx.test++" -o "simple_ro-ud-racai-uaic-9522-uaic-10.conllx.test++.parsed" -m parse -ic UTF-8
ret=$(java -Dfile.encoding=utf-8 maltProcessing.EvaluateMaltModel "simple_ro-ud-racai-uaic-9522-uaic-10.conllx.test++" "simple_ro-ud-racai-uaic-9522-uaic-10.conllx.test++.parsed")
echo simple_$test $ret

source flow_clean.sh