#!/bin/bash
SEPARATOR=":"
if [[ $(uname -a) == *"MINGW"* ]] 
then
SEPARATOR=";" 
fi

export CLASSPATH="../out/production/Dashboard/"$SEPARATOR"../out/production/AddConllFeatures/"$SEPARATOR"../out/production/maltparser-1.9.0-SSPR/"$SEPARATOR"../lib/*"

for f in $2; do
	if [ ! -f "$f++" ]; then
		echo ===Adding features to $f ...
		java -Xms1300m -Dfile.encoding=utf-8 AddFeatureColumns "$f" "$f++" 
	fi
	echo ===Extracting simple sentences only from $f++ ...
	java -Xms1000m -Dfile.encoding=utf-8 ExtractSimpleSentences "$f++" "simple_$f++"
	
	echo ===Parsing simple sentences from simple_$f++
	java -Xms1000m -Dfile.encoding=utf-8 org.maltparser.Malt -c "$1" -f train_options.xml -i "simple_$f++" -o "simple_$f++.parsed" -m parse -ic UTF-8	
done
