#!/usr/bin/env bash

for i in {1..80}; 
do
	java -ea -cp "./bin/:./librairies/djep-1.0.0.jar:./librairies/jep-2.3.0.jar:./librairies/peersim-1.0.5.jar" peersim.Simulator ./config/config-$i.txt &
done;