package peersim;

import common.util.Token;
import peersim.core.CommonState;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class BigObserver {

    // Constant.

    public static final BigObserver BIG_OBSERVER = new BigObserver();

    private static final String FILE = "results/pure_1_log_";

    // Variables.

    private Map<Long, Set<Integer>> mapNodeCSResource = new HashMap<>();
    private Map<Long, Long> mapNodeTimeBeginCS = new HashMap<>();

    private List<AlgoJL> listAlgoJL = new ArrayList<>();

    private BufferedWriter writerCSV;
    private BufferedWriter writerTotal;

    private long total = 0;

    private int nbMaxResourceAsked = -1;

    // Constructors.

    // Methods.

    public void setInCS(long nodeID, Set<Integer> resourceSet) {
        System.out.println("Observer---------------------------------------------------------------------------------------");

        Set<Integer> set = this.mapNodeCSResource.get(nodeID);

        assert set == null : "N = " + nodeID + " CS alors qu'il est deja en CS.";

        this.mapNodeCSResource.put(nodeID, resourceSet);

        this.mapNodeTimeBeginCS.put(nodeID, CommonState.getTime());

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

        Long beginTime = this.mapNodeTimeBeginCS.get(nodeID);
        Long endTime = CommonState.getTime();
        assert endTime > beginTime;
        Long timeCS = endTime - beginTime;
        this.total += (((long) resourceSet.size()) * timeCS);
        double percent = (((double) (this.total) * 100.0d) / 8_000_000.0d);

        if (this.writerCSV == null) {
            try {
                File csvFile = new File(FILE + this.nbMaxResourceAsked + ".csv");
                this.writerCSV = new BufferedWriter(new FileWriter(csvFile));
                File totalFile = new File(FILE + this.nbMaxResourceAsked + "_total.csv");
                this.writerTotal = new BufferedWriter(new FileWriter(totalFile));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            this.writerCSV.newLine();
            this.writerCSV.write(nodeID + ";" + this.mapNodeCSResource.size() + ";" + timeCS + ";");

            this.writerTotal.newLine();
            this.writerTotal.write(this.total + ";" + percent + ";");
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.mapNodeCSResource.remove(nodeID);
        this.mapNodeTimeBeginCS.remove(nodeID);

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

    public void setNbMaxResourceAsked(int nbMaxResourceAsked) {
        this.nbMaxResourceAsked = nbMaxResourceAsked;
    }
}
