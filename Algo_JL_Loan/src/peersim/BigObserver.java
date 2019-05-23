package peersim;

import common.util.Token;
import peersim.core.CommonState;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class BigObserver {

	// Constant.

	public static final int TIME_BEGIN = 5_000;

	public static final BigObserver BIG_OBSERVER = new BigObserver();

	private static final int TEST_NUMBER = 777;

	private static final String RESULTS_DIRECTORY = "results/" + TEST_NUMBER + "/";

	private static final String TOTAL_FILE = "total/";
	private static final String OTHER_FILE = "other/";
	private static final String LOAN_FILE = "loan/";

	// Variables.

	private Map<Long, Set<Integer>> mapNodeCSResource = new HashMap<>();
	private Map<Long, Long> mapNodeTimeBeginCS = new HashMap<>();
	private Map<Long, Long> mapNodeLoan = new HashMap<>();

	private List<AlgoJL> listAlgoJLS = new ArrayList<>();

	private BufferedWriter writerCSV;
	private BufferedWriter writerTotal;
	private BufferedWriter writerNbMessage;
	private BufferedWriter writerLoan;

	private long total = 0;

	private long nbMessage = 0;

	private int nbMaxResourceAsked = -1;

	// Constructors.

	private BigObserver() {
		File resultsDirectory = new File(RESULTS_DIRECTORY);
		File fileTotal = new File(RESULTS_DIRECTORY + TOTAL_FILE);
		File fileOther = new File(RESULTS_DIRECTORY + OTHER_FILE);

		try {
			this.createDirectory(resultsDirectory);
			this.createDirectory(fileTotal);
			this.createDirectory(fileOther);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Methods.

	public void setInCS(long nodeID, Set<Integer> resourceSet) {
		/*
		 * System.out.println(
		 * "Observer---------------------------------------------------------------------------------------"
		 * );
		 */

		if (CommonState.getIntTime() >= TIME_BEGIN) {

			Set<Integer> set = this.mapNodeCSResource.get(nodeID);

			assert set == null : "N = " + nodeID + " CS alors qu'il est deja en CS.";

			this.mapNodeCSResource.put(nodeID, resourceSet);

			this.mapNodeTimeBeginCS.put(nodeID, CommonState.getTime());

			Set<Map.Entry<Long, Set<Integer>>> setEntry = this.mapNodeCSResource.entrySet();

			for (Map.Entry<Long, Set<Integer>> entry : setEntry) {
				if (entry.getKey() != nodeID) {
					for (int resourceI : entry.getValue()) {
						for (int resourceJ : resourceSet) {
							assert resourceI != resourceJ : "N = " + nodeID + " en commun R = " + resourceI
									+ " avec N = " + entry.getKey();
						}
					}
				}
			}
		}

		/*
		 * System.out.println(
		 * "---------------------------------------------------------------------------------------"
		 * );
		 */
	}

	public void releaseCS(long nodeID) {
		/*
		 * System.out.println(
		 * "Observer---------------------------------------------------------------------------------------"
		 * );
		 */

		if (CommonState.getIntTime() >= TIME_BEGIN) {
			Set<Integer> resourceSet = this.mapNodeCSResource.get(nodeID);

//			assert resourceSet != null : "N = " + nodeID + " Release CS alors qu'il etait pas en CS";

			if (resourceSet != null) {

				Long beginTime = this.mapNodeTimeBeginCS.get(nodeID);
				Long endTime = CommonState.getTime();
				assert endTime > beginTime;
				Long timeCS = endTime - beginTime;
				this.total += (((long) resourceSet.size()) * timeCS);
				double percent = (((double) (this.total) * 100.0d) / 8_000_000.0d);

				File resultsDirectory = new File(RESULTS_DIRECTORY);
				File fileTotal = new File(RESULTS_DIRECTORY + TOTAL_FILE);
				File fileOther = new File(RESULTS_DIRECTORY + OTHER_FILE);
				File fileLoan = new File(RESULTS_DIRECTORY + LOAN_FILE);

				try {
					this.createDirectory(resultsDirectory);
					this.createDirectory(fileTotal);
					this.createDirectory(fileOther);
					this.createDirectory(fileLoan);
				} catch (IOException e) {
					e.printStackTrace();
				}

				if (this.writerCSV == null) {
					try {
						File csvFile = new File(RESULTS_DIRECTORY + OTHER_FILE + this.nbMaxResourceAsked + ".csv");
						this.writerCSV = new BufferedWriter(new FileWriter(csvFile));
						File totalFile = new File(
								RESULTS_DIRECTORY + TOTAL_FILE + this.nbMaxResourceAsked + "_total.csv");
						this.writerTotal = new BufferedWriter(new FileWriter(totalFile));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				try {
					this.writerCSV.newLine();
					this.writerCSV.write(nodeID + ";" + this.mapNodeCSResource.size() + ";" + timeCS);

					this.writerTotal.newLine();
					this.writerTotal.write(this.total + ";" + percent);
				} catch (IOException e) {
					e.printStackTrace();
				}

				this.mapNodeCSResource.remove(nodeID);
				this.mapNodeTimeBeginCS.remove(nodeID);
			}
		}

		/*
		 * System.out.println(
		 * "---------------------------------------------------------------------------------------"
		 * );
		 */
	}

	public void displayArrayToken() {
		/*
		 * System.out.println(
		 * "Observer---------------------------------------------------------------------------------------"
		 * );
		 */

		if (CommonState.getIntTime() >= TIME_BEGIN) {
			for (AlgoJL algoJL : this.listAlgoJLS) {
				Token[] array = algoJL.getArrayToken();

				System.out.print("N = " + algoJL.getNode().getID() + " [");
				for (int i = 0; i < array.length; i++) {
					System.out.print(" " + (array[i] != null ? 1 : 0) + " ");
				}
				System.out.println("]");
			}
		}

		/*
		 * System.out.println(
		 * "---------------------------------------------------------------------------------------"
		 * );
		 */
	}

	public void messageSend() {
		if (CommonState.getIntTime() >= TIME_BEGIN) {
			try {
				if (this.writerNbMessage == null) {
					File nbMessageFile = new File(RESULTS_DIRECTORY + OTHER_FILE + "messages.csv");
					this.writerNbMessage = new BufferedWriter(new FileWriter(nbMessageFile));
				}

				this.nbMessage++;

				this.writerNbMessage.newLine();
				this.writerNbMessage.write(String.valueOf(this.nbMessage));

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void createDirectory(File directory) throws IOException {
		if (!directory.exists())
			Files.createDirectory(Paths.get(directory.getAbsolutePath()));
	}

	public void addAlgoJL(AlgoJL algoJL) {
		if (!this.listAlgoJLS.contains(algoJL) && algoJL.getNode().getID() >= 0) {
			this.listAlgoJLS.add(algoJL);
		}
	}

	public void loanSuccess(long nodeID) {
		Long nbLoan = this.mapNodeLoan.get(nodeID);
		if (nbLoan == null) {
			this.mapNodeLoan.put(nodeID, 1L);
		} else {
			this.mapNodeLoan.put(nodeID, nbLoan + 1);
		}

		if (this.writerLoan == null) {
			try {
				this.writerLoan = new BufferedWriter(
						new FileWriter(new File(RESULTS_DIRECTORY + LOAN_FILE + "loan.csv")));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		try {
			this.writerLoan.write(nodeID + ";" + (nbLoan + 1L));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setNbMaxResourceAsked(int nbMaxResourceAsked) {
		this.nbMaxResourceAsked = nbMaxResourceAsked;
	}
}
