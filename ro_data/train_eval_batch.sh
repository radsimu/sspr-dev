#!/bin/bash
train="${1##*/}"
test="${2##*/}"

export CLASSPATH="../out/production/AddConllFeatures/":"../out/production/maltparser-1.9.0 - sspr/":"../lib/*"
java -Xms1300m -Dfile.encoding=utf-8 AddFeatureColumns $1 $train++ 
java -Xms1300m -Dfile.encoding=utf-8 AddFeatureColumns $2 $test++ 
java -Xms1000m -Dfile.encoding=utf-8 org.maltparser.Malt -c $3 -f train_options.xml -i $train++ -m learn -ic UTF-8 
java -Xms1000m -Dfile.encoding=utf-8 org.maltparser.Malt -c $3 -f train_options.xml -i $test++ -o $test++.parsed -m parse -ic UTF-8 
ret=$(java -Dfile.encoding=utf-8 maltProcessing.EvaluateMaltModel $test++ $test++.parsed)
rm -rf $test++ $test++.parsed $train++ $3.mco
echo $test $ret