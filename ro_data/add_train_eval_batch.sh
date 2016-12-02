#!/bin/bash
train="${1##*/}"
test="${2##*/}"

SEPARATOR=":"
if [[ $(uname -a) == *"MINGW"* ]] 
then
SEPARATOR=";" 
fi

export CLASSPATH="../out/production/AddConllFeatures/"$SEPARATOR"../out/production/maltparser-1.9.0 - sspr/"$SEPARATOR"../lib/*"
echo Adding features to batch ...
java -Xms1300m -Dfile.encoding=utf-8 AddFeatureColumns $1 $train++ 
java -Xms1300m -Dfile.encoding=utf-8 AddFeatureColumns $2 $test++ 
echo Training batch ...
java -Xms1000m -Dfile.encoding=utf-8 org.maltparser.Malt -c $3 -f train_options.xml -i $train++ -m learn -ic UTF-8 
echo Evaluating batch ...
java -Xms1000m -Dfile.encoding=utf-8 org.maltparser.Malt -c $3 -f train_options.xml -i $test++ -o $test++.parsed -m parse -ic UTF-8 
ret=$(java -Dfile.encoding=utf-8 maltProcessing.EvaluateMaltModel $test++ $test++.parsed)
rm -rf $test++ $test++.parsed $train++
echo $test $ret