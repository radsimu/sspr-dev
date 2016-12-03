#!/bin/bash

>cross_eval_report.txt
(source scripts/add_train_eval_batch.sh batches/ro-ud-racai-uaic-9522-ttl-1.conllx.train batches/ro-ud-racai-uaic-9522-ttl-1.conllx.test 1.model >> cross_eval_report.txt &
source scripts/add_train_eval_batch.sh batches/ro-ud-racai-uaic-9522-ttl-2.conllx.train batches/ro-ud-racai-uaic-9522-ttl-2.conllx.test 2.model >> cross_eval_report.txt &
source scripts/add_train_eval_batch.sh batches/ro-ud-racai-uaic-9522-ttl-3.conllx.train batches/ro-ud-racai-uaic-9522-ttl-3.conllx.test 3.model >> cross_eval_report.txt &
source scripts/add_train_eval_batch.sh batches/ro-ud-racai-uaic-9522-ttl-4.conllx.train batches/ro-ud-racai-uaic-9522-ttl-4.conllx.test 4.model >> cross_eval_report.txt &
source scripts/add_train_eval_batch.sh batches/ro-ud-racai-uaic-9522-ttl-5.conllx.train batches/ro-ud-racai-uaic-9522-ttl-5.conllx.test 5.model >> cross_eval_report.txt)
wait $!
(source scripts/add_train_eval_batch.sh batches/ro-ud-racai-uaic-9522-ttl-6.conllx.train batches/ro-ud-racai-uaic-9522-ttl-6.conllx.test 6.model >> cross_eval_report.txt &
source scripts/add_train_eval_batch.sh batches/ro-ud-racai-uaic-9522-ttl-7.conllx.train batches/ro-ud-racai-uaic-9522-ttl-7.conllx.test 7.model >> cross_eval_report.txt &
source scripts/add_train_eval_batch.sh batches/ro-ud-racai-uaic-9522-ttl-8.conllx.train batches/ro-ud-racai-uaic-9522-ttl-8.conllx.test 8.model >> cross_eval_report.txt &
source scripts/add_train_eval_batch.sh batches/ro-ud-racai-uaic-9522-ttl-9.conllx.train batches/ro-ud-racai-uaic-9522-ttl-9.conllx.test 9.model >> cross_eval_report.txt &
source scripts/add_train_eval_batch.sh batches/ro-ud-racai-uaic-9522-ttl-10.conllx.train batches/ro-ud-racai-uaic-9522-ttl-10.conllx.test 10.model >> cross_eval_report.txt)
wait $!
source flow_clean.sh