/***
 * starting point for running the project
 */
package larc.ctlr.runner;

import larc.ctlr.data.Synthetic;
import larc.ctlr.model.CTLR;
import larc.ctlr.model.Configure.ModelMode;
import larc.ctlr.model.Configure.PredictionMode;
import larc.ctlr.model.HITS;
import larc.ctlr.model.MultithreadCTLR;
import larc.ctlr.model.Prediction;

public class Runner {

	static void syntheticDataGeneration(int nUsers, int nTopics, int nWords, ModelMode mode, String outputPath) {
		larc.ctlr.data.Synthetic synthetic = new Synthetic(mode);
		synthetic.genData(nUsers, nTopics, nWords, outputPath);
	}

	static void runCTLR(String datasetPath, int nTopics, int batch) {
		larc.ctlr.model.CTLR model = new CTLR(datasetPath, nTopics, batch);
		model.train();
	}

	static void gradCheck(String datasetPath, int nTopics, int batch) {
		larc.ctlr.model.CTLR model = new CTLR(datasetPath, nTopics, batch);
		model.init();
		for (int k = 0; k < nTopics; k++) {
			// model.gradCheck_Authority(0, k);
			// model.gradCheck_Hub(5,k);
			model.gradCheck_TopicalInterest(5, k);
		}
	}

	static void altOptCheck(String datasetPath, int nTopics, int batch) {
		larc.ctlr.model.CTLR model = new CTLR(datasetPath, nTopics, batch);
		model.init();
		model.altCheck_TopicalInterest(15);
		// model.altCheck_Authority(15);
		// model.altCheck_Hub(15);
	}

	static void train(String datasetPath, int nTopics, int batch) {
		larc.ctlr.model.CTLR model = new CTLR(datasetPath, nTopics, batch);
		// model.init();
		model.train();
	}

	static void multiTrain(String datasetPath, int nTopics, int batch, ModelMode mode) {
		larc.ctlr.model.MultithreadCTLR model = new MultithreadCTLR(datasetPath, nTopics, batch, mode);
		// model.init();
		model.train();
	}
	
	static void predict(String datasetPath, String mode, String setting, int nTopics, PredictionMode pred_mode) {
		larc.ctlr.model.Prediction prediction = new Prediction(datasetPath, mode, setting, nTopics, pred_mode);
		
	}
	
	static void hits(String datasetPath) {
		larc.ctlr.model.HITS hits = new HITS(datasetPath);
		
	}

	static void test() {
		String datasetPath = "E:/code/java/ctlr/data/acm";
		int nTopics = 10;
		int batch = 1;
		ModelMode mode = ModelMode.TWITTER_LDA;
		larc.ctlr.model.MultithreadCTLR model = new MultithreadCTLR(datasetPath, nTopics, batch, mode);
		// model.init();
		model.train();
	}

	public static void main(String[] args) {
		try {
			if (args[0].equals("gen")) {
				int nUsers = Integer.parseInt(args[1]);
				int nTopics = Integer.parseInt(args[2]);
				int nWords = Integer.parseInt(args[3]);
				int mode = Integer.parseInt(args[4]);
				String outputPath = args[5];
				if (mode == 0) {
					syntheticDataGeneration(nUsers, nTopics, nWords, ModelMode.TWITTER_LDA, outputPath);
				} else {
					syntheticDataGeneration(nUsers, nTopics, nWords, ModelMode.ORIGINAL_LDA, outputPath);
				}
			} else if (args[0].equals("ctrl")) {
				String datasetPath = args[1];
				int nTopics = Integer.parseInt(args[2]);
				int batch = Integer.parseInt(args[3]);
				runCTLR(datasetPath, nTopics, batch);
			} else if (args[0].equals("gradCheck")) {
				String datasetPath = args[1];
				int nTopics = Integer.parseInt(args[2]);
				int batch = Integer.parseInt(args[3]);
				gradCheck(datasetPath, nTopics, batch);
			} else if (args[0].equals("altCheck")) {
				String datasetPath = args[1];
				int nTopics = Integer.parseInt(args[2]);
				int batch = Integer.parseInt(args[3]);
				altOptCheck(datasetPath, nTopics, batch);
			} else if (args[0].equals("train")) {
				String datasetPath = args[1];
				int nTopics = Integer.parseInt(args[2]);
				int batch = Integer.parseInt(args[3]);
				train(datasetPath, nTopics, batch);
			} else if (args[0].equals("multiTrain")) {
				String datasetPath = args[1];
				int nTopics = Integer.parseInt(args[2]);
				int batch = Integer.parseInt(args[3]);
				int mode = Integer.parseInt(args[4]);
				if (mode == 0) {
					multiTrain(datasetPath, nTopics, batch, ModelMode.TWITTER_LDA);
				} else {
					multiTrain(datasetPath, nTopics, batch, ModelMode.ORIGINAL_LDA);
				}
			} else if (args[0].equals("3x-multiTrain")) {
				String datasetPath = args[1];
				int nTopics1 = Integer.parseInt(args[2]);
				int nTopics2 = Integer.parseInt(args[3]);
				int nTopics3 = Integer.parseInt(args[4]);
				int batch = Integer.parseInt(args[5]);
				int mode = Integer.parseInt(args[6]);
				if (mode == 0) {
					multiTrain(datasetPath, nTopics1, batch, ModelMode.TWITTER_LDA);
					multiTrain(datasetPath, nTopics2, batch, ModelMode.TWITTER_LDA);
					multiTrain(datasetPath, nTopics3, batch, ModelMode.TWITTER_LDA);
				} else {
					multiTrain(datasetPath, nTopics1, batch, ModelMode.ORIGINAL_LDA);
					multiTrain(datasetPath, nTopics2, batch, ModelMode.ORIGINAL_LDA);
					multiTrain(datasetPath, nTopics3, batch, ModelMode.ORIGINAL_LDA);
				}

			} else if (args[0].equals("predict")) {
				String datasetPath = args[1];
				String mode = args[2];
				String setting = args[3];
				int topics = Integer.parseInt(args[4]);
				int pred_mode = Integer.parseInt(args[5]);
				if (pred_mode == 0){
					predict(datasetPath, mode, setting, topics,PredictionMode.CTLR);
				} else if (pred_mode == 1){
					predict(datasetPath, mode, setting, topics,PredictionMode.COMMON_INTEREST);
				} else if (pred_mode == 2){
					predict(datasetPath, mode, setting, topics,PredictionMode.COMMON_NEIGHBOR);
				} else if (pred_mode == 3){
					predict(datasetPath, mode, setting, topics,PredictionMode.HITS);
				} else if (pred_mode == 4){
					predict(datasetPath, mode, setting, topics,PredictionMode.WTFW);
				}
			} else if (args[0].equals("hits")) {
				String datasetPath = args[1];
				hits(datasetPath);
			} else {
				System.out.printf("%s is not an option!!!");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
