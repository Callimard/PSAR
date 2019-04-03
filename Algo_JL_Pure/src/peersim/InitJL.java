package peersim;

import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

public class InitJL implements Control {

    // Variables.

    private int algoJL;

    // Constructors.

    public InitJL(String prefix) {
        this.algoJL = Configuration.getPid(prefix + ".jl");
    }

    // Methods.

    @Override
    public boolean execute() {

        Node firstNode = null;

        for (int i = 0; i < Network.size(); i++) {
            Node node = Network.get(i);
            AlgoJL nodeAlgoJL = (AlgoJL) node.getProtocol(this.algoJL);

            if (i == 0) {
                firstNode = node;
                nodeAlgoJL.setAllResourcesHere();
            } else {
                for (int j = 0; j < nodeAlgoJL.getNbResource(); j++) {
                    nodeAlgoJL.setNodeLink(j, firstNode);
                }
            }
        }

        System.out.println("Init fini.");

        return false;
    }
}
