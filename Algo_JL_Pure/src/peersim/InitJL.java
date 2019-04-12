package peersim;

import common.message.BeginMessage;
import common.util.Util;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;

import javax.sound.midi.SysexMessage;

public class InitJL implements Control {

    // Variables.

    private int algoJL;

    private int min;
    private int max;

    // Constructors.

    public InitJL(String prefix) {
        this.algoJL = Configuration.getPid(prefix + ".jl");
        this.min = Configuration.getInt(prefix + ".min");
        this.max = Configuration.getInt(prefix + ".max");
    }

    // Methods.

    @Override
    public boolean execute() {
        Node firstNode = null;

        for (int i = 0; i < Network.size(); i++) {
            Node node = Network.get(i);
            AlgoJL nodeAlgoJL = (AlgoJL) node.getProtocol(this.algoJL);
            nodeAlgoJL.setNode(node);

            if (i == 0) {
                firstNode = node;
                nodeAlgoJL.setAllResourcesHere();
            } else {
                for (int j = 0; j < nodeAlgoJL.getNbResource(); j++) {
                    nodeAlgoJL.setNodeLink(j, firstNode);
                }
            }

            int delay = Util.generateRandom(this.min, this.max);
            EDSimulator.add(delay, new BeginMessage(-1, null, null), node, this.algoJL);
        }

        System.out.println("Init fini.");

        return false;
    }

}
