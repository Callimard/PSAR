
for i in {1..80}; 
do
	java -ea -cp "./out/production/AlgoJL_Rework/:./librairies/djep-1.0.0.jar:./librairies/jep-2.3.0.jar:./librairies/peersim-1.0.5.jar" peersim.Simulator ./config/2/config-$i.txt &
done;
