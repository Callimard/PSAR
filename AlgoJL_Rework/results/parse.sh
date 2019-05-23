#!/bin/bash

for i in {1..80}; do
	NUM=$i
	FILE="./"$1"/total/"$i"_total.csv"
	PERCENT=$(tail -n 2 "$FILE")
	RES="$NUM;$PERCENT"
	echo "$RES" >> "./$1/res.csv"
done;
