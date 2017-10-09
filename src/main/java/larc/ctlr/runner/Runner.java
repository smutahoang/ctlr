/***
 * starting point for running the project
 */
package larc.ctlr.runner;

import larc.ctlr.data.Synthetic;
import larc.ctlr.evaluation.Prediction;
import larc.ctlr.model.CTLR;
import larc.ctlr.model.Configure.ModelMode;
import larc.ctlr.model.Configure.PredictionMode;
import larc.ctlr.model.HITS;
import larc.ctlr.model.MultithreadCTLR;

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

	static void multiTrain(String datasetPath, int nTopics, int batch, ModelMode mode, String outputPath) {
		larc.ctlr.model.MultithreadCTLR model = new MultithreadCTLR(datasetPath, nTopics, batch, mode, outputPath);
		// model.init();
		model.train();
	}

	static void predict(String datasetPath, String resultPath, String mode, String setting, int nTopics, int testBatch,
			PredictionMode predMode, String outputPath) {
		larc.ctlr.evaluation.Prediction prediction = new Prediction(datasetPath, resultPath, mode, setting, nTopics,
				testBatch, predMode, outputPath);
		prediction.evaluate();
	}

	static void hits(String datasetPath, int batch) {
		larc.ctlr.model.HITS hits = new HITS(datasetPath, batch);
	}
	
	static void test() {
		String datasetPath = "E:/code/java/ctlr/data/acm";
		int nTopics = 10;
		int batch = 1;
		ModelMode mode = ModelMode.TWITTER_LDA;
		// larc.ctlr.model.MultithreadCTLR model = new
		// MultithreadCTLR(datasetPath, nTopics, batch, mode);
		// model.init();
		// model.train();
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
				String output = args[5];
				if (mode == 0) {
					multiTrain(datasetPath, nTopics, batch, ModelMode.TWITTER_LDA, output);
				} else {
					multiTrain(datasetPath, nTopics, batch, ModelMode.ORIGINAL_LDA, output);
				}
			} else if (args[0].equals("3x-multiTrain")) {
				String datasetPath = args[1];
				int nTopics1 = Integer.parseInt(args[2]);
				int nTopics2 = Integer.parseInt(args[3]);
				int nTopics3 = Integer.parseInt(args[4]);
				int batch = Integer.parseInt(args[5]);
				int mode = Integer.parseInt(args[6]);
				String output1 = args[7];
				String output2 = args[8];
				String output3 = args[9];
				if (mode == 0) {
					multiTrain(datasetPath, nTopics1, batch, ModelMode.TWITTER_LDA, output1);
					multiTrain(datasetPath, nTopics2, batch, ModelMode.TWITTER_LDA, output2);
					multiTrain(datasetPath, nTopics3, batch, ModelMode.TWITTER_LDA, output3);
				} else {
					multiTrain(datasetPath, nTopics1, batch, ModelMode.ORIGINAL_LDA, output1);
					multiTrain(datasetPath, nTopics2, batch, ModelMode.ORIGINAL_LDA, output2);
					multiTrain(datasetPath, nTopics3, batch, ModelMode.ORIGINAL_LDA, output3);
				}

			} else if (args[0].equals("predict")) {
				String datasetPath = args[1];
				String resultPath = args[2];
				String mode = args[3];
				String setting = args[4];
				int topics = Integer.parseInt(args[5]);
				int testBatch = Integer.parseInt(args[6]);
				int predMode = Integer.parseInt(args[7]);
				String outputPath = args[8];
				if (predMode == 0) {
					predict(datasetPath, resultPath, mode, setting, topics, testBatch, PredictionMode.CTLR, outputPath);
				} else if (predMode == 1) {
					predict(datasetPath, resultPath, mode, setting, topics, testBatch, PredictionMode.COMMON_INTEREST,
							outputPath);
				} else if (predMode == 2) {
					predict(datasetPath, resultPath, mode, setting, topics, testBatch, PredictionMode.COMMON_NEIGHBOR,
							outputPath);
				} else if (predMode == 3) {
					predict(datasetPath, resultPath, mode, setting, topics, testBatch, PredictionMode.HITS, outputPath);
				} else if (predMode == 4) {
					predict(datasetPath, resultPath, mode, setting, topics, testBatch, PredictionMode.CTR, outputPath);
				} else if (predMode == 5) {
					predict(datasetPath, resultPath, mode, setting, topics, testBatch, PredictionMode.WTFW, outputPath);
				}
			} else if (args[0].equals("hits")) {
				String datasetPath = args[1];
				int batch = Integer.parseInt(args[2]);
				hits(datasetPath, batch);
			} else {
				System.out.printf("%s is not an option!!!");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


}
