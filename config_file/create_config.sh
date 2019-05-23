mkdir "$1"

for i in {1..80};
do
	touch "./$1/config-$i.txt"
	chmod 777 "./$1/config-$i.txt"

    echo "random.seed $RANDOM" > "./$1/config-$i.txt"
    echo "simulation.endtime 100000" >> "./$1/config-$i.txt"
    echo "network.size 32" >> "./$1/config-$i.txt"
    echo "protocol.transportPID UniformRandomTransport" >> "./$1/config-$i.txt"
    echo "protocol.transportPID.mindelay 1" >> "./$1/config-$i.txt"
    echo "protocol.transportPID.maxdelay 10" >> "./$1/config-$i.txt"
    echo "protocol.algoJL peersim.AlgoJL" >> "./$1/config-$i.txt"
    echo "protocol.algoJL.tr transportPID" >> "./$1/config-$i.txt"
    echo "protocol.algoJL.nb_resource 80" >> "./$1/config-$i.txt"
    echo "protocol.algoJL.nbCS -1" >> "./$1/config-$i.txt"
    echo "protocol.algoJL.nb_max_r_asked $i" >> "./$1/config-$i.txt"
    echo "protocol.algoJL.min_cs 50" >> "./$1/config-$i.txt"
    echo "protocol.algoJL.max_cs 350" >> "./$1/config-$i.txt"
    echo "init.algoJLInit peersim.InitJL" >> "./$1/config-$i.txt"
    echo "init.algoJLInit.jl algoJL" >> "./$1/config-$i.txt"
    echo "init.algoJLInit.min 75" >> "./$1/config-$i.txt"
    echo "init.algoJLInit.max 125" >> "./$1/config-$i.txt"
done;
