package peersim;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BigObserver {

    // Constant.

    public static final BigObserver BIG_OBERVER = new BigObserver();

    // Variables.

    private Map<Long, Set<Integer>> mapNodeCSResource = new HashMap<>();

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
}
