#!/bin/bash

for i in {1..79}; do
	FILE_MESSAGE="./"$1"/other/messages_"$i".csv"
	NB_MESSAGE=$(tail -n 2 "$FILE_MESSAGE" | head -n 1)
	echo "$NB_MESSAGE" > "./"$1"/other/messages_"$i".csv"
done
