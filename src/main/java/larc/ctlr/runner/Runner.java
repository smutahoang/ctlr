/***
 * starting point for running the project
 */
package larc.ctlr.runner;

import larc.ctlr.data.Synthetic;
import larc.ctlr.model.CTLR;

public class Runner {

	static void syntheticDataGeneration(int nUsers, int nTopics, int nWords, String outputPath) {
		larc.ctlr.data.Synthetic synthetic = new Synthetic();
		synthetic.genData(nUsers, nTopics, nWords, outputPath);
	}

	static void runCTLR(String datasetPath, int nTopics, int batch) {
		larc.ctlr.model.CTLR model = new CTLR(datasetPath, nTopics, batch);
		model.train();
	}
	
	static void gradCheck(String datasetPath, int nTopics, int batch) {
		larc.ctlr.model.CTLR model = new CTLR(datasetPath, nTopics, batch);
		model.init();
		//for (int v=0; v < model.dataset.nUsers; v++){
			//model.gradCheck_Authority(5, 5);
			//model.gradCheck_Hub(5,5);
			model.gradCheck_TopicalInterest(5, 5);
		//}
	}

	public static void main(String[] args) {
		
		try {
			if (args[0].equals("gen")) {
				int nUsers = Integer.parseInt(args[1]);
				int nTopics = Integer.parseInt(args[2]);
				int nWords = Integer.parseInt(args[3]);
				String outputPath = args[4];
				syntheticDataGeneration(nUsers, nTopics, nWords, outputPath);
			} else if (args[0].equals("ctrl")) {
				String datasetPath = args[1];
				int nTopics = Integer.parseInt(args[2]);
				int batch = Integer.parseInt(args[3]);
				runCTLR(datasetPath, nTopics, batch);
			} else if (args[0].equals("gradCheck")){
				String datasetPath = args[1];
				int nTopics = Integer.parseInt(args[2]);
				int batch = Integer.parseInt(args[3]);
				gradCheck(datasetPath, nTopics, batch);
			}else {
				System.out.printf("%s is not an option!!!");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
