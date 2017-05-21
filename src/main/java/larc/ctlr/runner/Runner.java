/***
 * starting point for running the project
 */
package larc.ctlr.runner;

import larc.ctlr.model.CTLR;

public class Runner {

	static void syntheticDataGeneration(int nUsers) {
		larc.ctlr.data.Synthetic.genData(nUsers);
	}

	static void runCTLR(String datasetPath, int nTopics) {
		larc.ctlr.model.CTLR model = new CTLR(datasetPath, nTopics);
		model.train();
	}

	public static void main(String[] args) {
		
		try {
			if (args[0].equals("gen")) {
				int nUsers = Integer.parseInt(args[1]);
				syntheticDataGeneration(nUsers);
			} else if (args[0].equals("ctrl")) {
				String datasetPath = args[1];
				int nTopics = Integer.parseInt(args[2]);
				runCTLR(datasetPath, nTopics);
			} else {
				System.out.printf("%s is not an option!!!");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
