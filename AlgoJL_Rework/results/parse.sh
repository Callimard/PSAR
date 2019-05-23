#!/bin/bash

for i in {1..79}; do
	NUM=$i
	FILE_PERCENT="./"$1"/total/"$i"_total.csv"
	FILE_MESSAGE="./"$1"/other/messages_"$i".csv"
	PERCENT=$(tail -n 2 "$FILE_PERCENT" | head -n 1)
	NB_MESSAGE=$(tail -n 2 "$FILE_MESSAGE" | head -n 1)
	RES="$NUM;$PERCENT;$NB_MESSAGE"
	echo "$RES" >> "./$1/res.csv"
done;
