for i in {1..80}; do
	NUM=$i
	FILE="loan_1_log_"$i"_total.csv"
	PERCENT=$(tail -n 2 $FILE)
	RES="$NUM;$PERCENT;"
	echo "$RES" >> res.csv
done;
