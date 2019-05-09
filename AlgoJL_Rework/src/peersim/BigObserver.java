package peersim;

import common.util.Token;

import java.util.*;

public class BigObserver {

    // Constant.

    public static final BigObserver BIG_OBERVER = new BigObserver();

    // Variables.

    private Map<Long, Set<Integer>> mapNodeCSResource = new HashMap<>();

    private List<AlgoJL> listAlgoJL = new ArrayList<>();

    // Methods.

    public void setInCS(long nodeID, Set<Integer> resourceSet) {
        System.out.println("Observer---------------------------------------------------------------------------------------");

        Set<Integer> set = this.mapNodeCSResource.get(nodeID);

        assert set == null : "N = " + nodeID + " CS alors qu'il est deja en CS.";

        this.mapNodeCSResource.put(nodeID, resourceSet);

        Set<Map.Entry<Long, Set<Integer>>> setEntry = this.mapNodeCSResource.entrySet();

        for (Map.Entry<Long, Set<Integer>> entry : setEntry) {
            if (entry.getKey() != nodeID) {
                for (int resourceI : entry.getValue()) {
                    for (int resourceJ : resourceSet) {
                        assert resourceI != resourceJ : "N = " + nodeID + " en commun R = " + resourceI + " avec N = " + entry.getKey();
                    }
                }
            }
        }

        System.out.println("---------------------------------------------------------------------------------------");
    }

    public void releaseCS(long nodeID) {
        System.out.println("Observer---------------------------------------------------------------------------------------");

        Set<Integer> resourceSet = this.mapNodeCSResource.get(nodeID);

        assert resourceSet != null : "N = " + nodeID + " Release CS alors qu'il etait pas en CS";

        this.mapNodeCSResource.remove(nodeID);

        System.out.println("---------------------------------------------------------------------------------------");
    }

    public void displayArrayToken() {
        System.out.println("Observer---------------------------------------------------------------------------------------");

        for (AlgoJL algoJL : this.listAlgoJL) {
            Token array[] = algoJL.getArrayToken();

            System.out.print("N = " + algoJL.getNode().getID() + " [");
            for (int i = 0; i < array.length; i++) {
                System.out.print(" " + (array[i] != null ? 1 : 0) + " ");
            }
            System.out.println("]");
        }

        System.out.println("---------------------------------------------------------------------------------------");
    }

    public void addAlgoJL(AlgoJL algoJL) {
        if (!this.listAlgoJL.contains(algoJL) && algoJL.getNode().getID() >= 0) {
            this.listAlgoJL.add(algoJL);
        }
    }
}
