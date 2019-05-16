﻿for ($i = 1; $i -lt 81; $i++) {
    New-Item -Path . -Name "config-$i.txt" -ItemType "file"

    Add-Content "config-$i.txt" "random.seed 897464313"
    Add-Content "config-$i.txt" "simulation.endtime 100000"
    Add-Content "config-$i.txt" "network.size 32"
    Add-Content "config-$i.txt" "protocol.transportPID UniformRandomTransport"
    Add-Content "config-$i.txt" "protocol.transportPID.mindelay 1"
    Add-Content "config-$i.txt" "protocol.transportPID.maxdelay 1"
    Add-Content "config-$i.txt" "protocol.algoJL peersim.AlgoJL"
    Add-Content "config-$i.txt" "protocol.algoJL.tr transportPID"
    Add-Content "config-$i.txt" "protocol.algoJL.nb_resource 80"
    Add-Content "config-$i.txt" "protocol.algoJL.nbCS -1"
    Add-Content "config-$i.txt" "protocol.algoJL.nb_max_r_asked $i"
    Add-Content "config-$i.txt" "protocol.algoJL.min_cs 5"
    Add-Content "config-$i.txt" "protocol.algoJL.max_cs 35"
    Add-Content "config-$i.txt" "init.algoJLInit peersim.InitJL"
    Add-Content "config-$i.txt" "init.algoJLInit.jl algoJL"
    Add-Content "config-$i.txt" "init.algoJLInit.min 2"
    Add-Content "config-$i.txt" "init.algoJLInit.max 3"
}