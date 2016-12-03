#!/bin/bash

SEPARATOR=":"
if [[ $(uname -a) == *"MINGW"* ]]; then
SEPARATOR=";" 
fi

export CLASSPATH="../out/production/Dashboard/"$SEPARATOR"../lib/*"

mkdir -p batches_taggedWithUaic

if [ ! -f batches_taggedWithUaic/ro-ud-racai-uaic-9522-uaic-10.conllx.test ]; then
	echo Retagging batches/ro-ud-racai-uaic-9522-ttl-10.conllx.test
    java -Xms4g -Dfile.encoding=utf-8 RetagWithUaic batches/ro-ud-racai-uaic-9522-ttl-10.conllx.test batches_taggedWithUaic/ro-ud-racai-uaic-9522-uaic-10.conllx.test
fi

if [ ! -f batches_taggedWithUaic/ro-ud-racai-uaic-9522-uaic-10.conllx.train ]; then
	echo Retagging batches/ro-ud-racai-uaic-9522-ttl-10.conllx.train
    java -Xms4g -Dfile.encoding=utf-8 RetagWithUaic batches/ro-ud-racai-uaic-9522-ttl-10.conllx.train batches_taggedWithUaic/ro-ud-racai-uaic-9522-uaic-10.conllx.train 
fi

source scripts/add_train_eval_batch.sh batches_taggedWithUaic/ro-ud-racai-uaic-9522-uaic-10.conllx.train batches_taggedWithUaic/ro-ud-racai-uaic-9522-uaic-10.conllx.test batches_taggedWithUaic_10.uaic.model

cp -f batches_taggedWithUaic_10.uaic.model.mco batches_taggedWithUaic/batches_taggedWithUaic_10.uaic.model.mco