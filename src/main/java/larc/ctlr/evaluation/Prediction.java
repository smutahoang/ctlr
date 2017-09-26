package larc.ctlr.evaluation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.List;

import larc.ctlr.model.Configure.PredictionMode;

public class Prediction {
	private String dataPath;
	private String resultPath;
	private String modelMode;
	private String setting;
	private int nTopics;
	private PredictionMode predMode;
	private String outputPath;
	// CTRL model
	private HashMap<String, double[]> userAuthorities;
	private HashMap<String, double[]> userHubs;
	private HashMap<String, double[]> userInterests;
	// Common-Neighbor
	private HashMap<String, HashSet<String>> userNeighbors;
	private HashMap<String, Double> traditionalAuthorities;
	// HIST
	private HashMap<String, Double> traditionalHubs;
	// CTR
	private HashMap<String, double[]> userUserLatentFactors;
	private HashMap<String, double[]> userItemLatentFactors;
	// data
	private HashMap<String, Integer> userPositiveLinks;
	private HashSet<String> newUsers;
	private HashSet<String> users;
	private String[] testSrcUsers;
	private String[] testDesUsers;
	private int[] testLabels;
	private double[] predictionScores;
	private double[] newUserPredictionScores;
	private int maxOverallTopK;

	/***
	 * read dataset from folder "path"
	 * 
	 * @param dataPath
	 */
	public Prediction(String _path, String _resultPath, String _modelMode, String _setting, int _nTopics,
			PredictionMode _predMode, String _outputPath) {
		this.dataPath = _path;
		this.resultPath = _resultPath;
		this.modelMode = _modelMode;
		this.setting = _setting;
		this.nTopics = _nTopics;
		this.predMode = _predMode;
		if (predMode == PredictionMode.CTLR) {
			this.outputPath = String.format("%s/%d/%s/%s", _outputPath, nTopics, setting, modelMode);
		} else if (predMode == PredictionMode.WTFW) {
			this.outputPath = String.format("%s/%s/%d", _outputPath, nTopics, setting);
		} else if (predMode == PredictionMode.CTR) {
			this.outputPath = String.format("%s/%d", _outputPath, nTopics);
		} else {
			this.outputPath = _outputPath;
		}

		File theDir = new File(outputPath);

		// if the directory does not exist, create it
		if (!theDir.exists()) {
			System.out.println("creating directory: " + theDir.getAbsolutePath());
			try {
				theDir.mkdirs();
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}

	public void evaluate() {
		System.out.println("loading testing data");
		String relationshipFile = String.format("%s/relationships.csv", dataPath);
		String nonRelationshipFile = String.format("%s/nonrelationships.csv", dataPath);
		String hitsFile = String.format("%s/user_hits.csv", dataPath);
		//String newUserFile = String.format("%s/newusers.csv", dataPath);
		loadTestData(relationshipFile, nonRelationshipFile);
		//loadNewUserData(newUserFile);

		if (predMode == PredictionMode.CTLR) {
			String authFilePath = String.format("%s/%s/%s/%d/l_OptUserAuthorityDistributions.csv", resultPath,
					modelMode, setting, nTopics);
			int authSize = loadUserAuthorities(authFilePath, nTopics);
			System.out.println("loaded authorities of " + authSize + " users");

			String hubFilePath = String.format("%s/%s/%s/%d/l_OptUserHubDistributions.csv", resultPath, modelMode,
					setting, nTopics);
			int hubSize = loadUserHubs(hubFilePath, nTopics);
			System.out.println("loaded hubs of " + hubSize + " users");

			System.out.println("compute prediction scores");
			computeCTLRScores();

		} else if (predMode == PredictionMode.CTR) {
			loadCTR();
			computeCTRScores();
		}

		else if (predMode == PredictionMode.COMMON_INTEREST) {
			String interestFilePath = String.format("%s/%s/%d/l_GibbUserTopicalInterestDistributions.csv", resultPath,
					modelMode, nTopics);
			int interestSize = loadUserInterests(interestFilePath, nTopics);
			System.out.println("loaded interests of " + interestSize + " users");

			System.out.println("compute prediction scores");
			computeCommonInterestScores();

			this.outputPath = String.format("%s/%s/%s", dataPath, modelMode, nTopics);

		} else if (predMode == PredictionMode.COMMON_NEIGHBOR) {
			int neighhorSize = loadUserNeighbors(relationshipFile);
			System.out.println("loaded neighbors of " + neighhorSize + " users");
			computeCommonNeighborScores();
		} else if (predMode == PredictionMode.HITS) {
			loadTraditionalHITS(hitsFile);
			computeHITSScores();
		}

		output_PredictionScores();
		output_EvaluateOverallPrecisionRecall(5, maxOverallTopK);
		output_EvaluateUserLevelPrecisionRecall(5);
		//outout_EvaluateNewUserPrecisionRecall(5);

		/*
		 * if (_model_mode.equals("TWITTER_LDA")){
		 * output_EvaluateOverallPrecisionRecall(5, maxOverallTopK);
		 * output_EvaluateUserLevelPrecisionRecall(5); } else{
		 * output_EvaluateOverallPrecisionRecall(5, maxOverallTopK);
		 * output_EvaluateUserLevelPrecisionRecall(5); }
		 */
	}

	private int loadUserAuthorities(String filename, int nTopics) {
		BufferedReader br = null;
		String line = null;
		double[] authorities;
		userAuthorities = new HashMap<String, double[]>();
		try {
			File authFile = new File(filename);
			br = new BufferedReader(new FileReader(authFile.getAbsolutePath()));
			while ((line = br.readLine()) != null) {
				String[] tokens = line.split(",");
				String uid = tokens[0];
				authorities = new double[nTopics];
				for (int i = 0; i < nTopics; i++) {
					authorities[i] = Double.parseDouble(tokens[i + 6]);
				}
				userAuthorities.put(uid, authorities);
			}
			br.close();
		} catch (Exception e) {
			System.out.println("Error in reading user file!");
			e.printStackTrace();
			System.exit(0);
		}
		return userAuthorities.size();
	}

	private int loadUserHubs(String filename, int nTopics) {
		BufferedReader br = null;
		String line = null;
		double[] hubs;
		userHubs = new HashMap<String, double[]>();
		try {
			File hubFile = new File(filename);
			br = new BufferedReader(new FileReader(hubFile.getAbsolutePath()));
			while ((line = br.readLine()) != null) {
				String[] tokens = line.split(",");
				String uid = tokens[0];
				hubs = new double[nTopics];
				for (int i = 0; i < nTopics; i++) {
					hubs[i] = Double.parseDouble(tokens[i + 6]);
				}
				userHubs.put(uid, hubs);
			}
			br.close();
		} catch (Exception e) {
			System.out.println("Error in reading user file!");
			e.printStackTrace();
			System.exit(0);
		}
		return userHubs.size();
	}

	private int loadUserInterests(String filename, int nTopics) {
		BufferedReader br = null;
		String line = null;
		double[] interests;
		userInterests = new HashMap<String, double[]>();
		try {
			File interestFile = new File(filename);
			br = new BufferedReader(new FileReader(interestFile.getAbsolutePath()));
			while ((line = br.readLine()) != null) {
				String[] tokens = line.split(",");
				String uid = tokens[0];
				interests = new double[nTopics];
				for (int i = 0; i < nTopics; i++) {
					interests[i] = Double.parseDouble(tokens[i + 1]);
				}
				userInterests.put(uid, interests);
			}
			br.close();
		} catch (Exception e) {
			System.out.println("Error in reading user file!");
			e.printStackTrace();
			System.exit(0);
		}
		return userInterests.size();
	}

	private int loadUserNeighbors(String relationshipFile) {
		BufferedReader br = null;
		String line = null;
		userNeighbors = new HashMap<String, HashSet<String>>();
		try {
			File linkFile = new File(relationshipFile);

			br = new BufferedReader(new FileReader(linkFile.getAbsolutePath()));
			while ((line = br.readLine()) != null) {
				String[] tokens = line.split(",");
				String uid = tokens[0];
				String vid = tokens[1];
				;
				int flag = Integer.parseInt(tokens[2]);
				if (flag == 1) {
					if (userNeighbors.containsKey(uid)) {
						userNeighbors.get(uid).add(vid);
					} else {
						HashSet<String> neighbors = new HashSet<String>();
						neighbors.add(vid);
						userNeighbors.put(uid, neighbors);
					}
					if (userNeighbors.containsKey(vid)) {
						userNeighbors.get(vid).add(uid);
					} else {
						HashSet<String> neighbors = new HashSet<String>();
						neighbors.add(uid);
						userNeighbors.put(vid, neighbors);
					}
				}
			}
			br.close();

		} catch (Exception e) {
			System.out.println("Error in reading user file!");
			e.printStackTrace();
			System.exit(0);
		}
		return userNeighbors.size();
	}

	private void loadTraditionalHITS(String filename) {
		BufferedReader br = null;
		String line = null;
		traditionalAuthorities = new HashMap<String, Double>();
		traditionalHubs = new HashMap<String, Double>();
		try {
			File hitFile = new File(filename);
			br = new BufferedReader(new FileReader(hitFile.getAbsolutePath()));
			while ((line = br.readLine()) != null) {
				String[] tokens = line.split(",");
				String uid = tokens[0];
				double authority = Double.parseDouble(tokens[1]);
				double hub = Double.parseDouble(tokens[2]);
				traditionalAuthorities.put(uid, authority);
				traditionalHubs.put(uid, hub);
			}
			br.close();
		} catch (Exception e) {
			System.out.println("Error in reading user file!");
			e.printStackTrace();
			System.exit(0);
		}
	}

	private void loadCTR() {
		try {
			userUserLatentFactors = new HashMap<String, double[]>();
			HashMap<Integer, String> userIndex2Id = new HashMap<Integer, String>();
			String filename = String.format("%s/ctr/user_index_id.txt", dataPath);
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line = null;
			while ((line = br.readLine()) != null) {
				String[] tokens = line.split(",");
				userIndex2Id.put(Integer.parseInt(tokens[0]), tokens[1]);
			}
			br.close();

			filename = String.format("%s/final-U.dat", resultPath);
			int uIndex = 0;
			br = new BufferedReader(new FileReader(filename));
			while ((line = br.readLine()) != null) {
				String[] tokens = line.split(" ");
				double[] factors = new double[nTopics];
				for (int i = 0; i < nTopics; i++) {
					factors[i] = Double.parseDouble(tokens[i]);
				}
				userUserLatentFactors.put(userIndex2Id.get(uIndex), factors);
				uIndex++;
			}
			br.close();

			userItemLatentFactors = new HashMap<String, double[]>();
			userIndex2Id = new HashMap<Integer, String>();
			filename = String.format("%s/ctr/item_index_id.txt", dataPath);
			br = new BufferedReader(new FileReader(filename));
			while ((line = br.readLine()) != null) {
				String[] tokens = line.split(",");
				userIndex2Id.put(Integer.parseInt(tokens[0]), tokens[1]);
			}
			br.close();

			filename = String.format("%s/final-V.dat", resultPath);
			uIndex = 0;
			br = new BufferedReader(new FileReader(filename));
			while ((line = br.readLine()) != null) {
				String[] tokens = line.split(" ");
				double[] factors = new double[nTopics];
				for (int i = 0; i < nTopics; i++) {
					factors[i] = Double.parseDouble(tokens[i]);
				}
				userItemLatentFactors.put(userIndex2Id.get(uIndex), factors);
				uIndex++;
			}
			br.close();

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private void loadTestData(String relationshipFile, String nonRelationshipFile) {
		BufferedReader br = null;
		String line = null;
		int nTest = 0;
		int iTest = 0;
		maxOverallTopK = 0;
		userPositiveLinks = new HashMap<String, Integer>();
		users = new HashSet<String>();

		try {
			File linkFile = new File(relationshipFile);
			File nonLinkFile = new File(nonRelationshipFile);

			br = new BufferedReader(new FileReader(nonLinkFile.getAbsolutePath()));
			while ((line = br.readLine()) != null) {
				if (Integer.parseInt(line.split(",")[2]) == 0) {
					nTest++;
				}
			}
			br.close();

			br = new BufferedReader(new FileReader(linkFile.getAbsolutePath()));
			while ((line = br.readLine()) != null) {
				String[] tokens = line.split(",");
				String uid = tokens[0];
				int flag = Integer.parseInt(tokens[2]);
				if (flag == 0) {
					maxOverallTopK++;
					nTest++;
					if (userPositiveLinks.containsKey(uid)) {
						int count = userPositiveLinks.get(uid) + 1;
						userPositiveLinks.put(uid, count);
					} else {
						userPositiveLinks.put(uid, 1);
					}
				}
			}
			br.close();

			testSrcUsers = new String[nTest];
			testDesUsers = new String[nTest];
			testLabels = new int[nTest];

			predictionScores = new double[nTest];

			br = new BufferedReader(new FileReader(nonLinkFile.getAbsolutePath()));
			while ((line = br.readLine()) != null) {
				String[] tokens = line.split(",");
				String uid = tokens[0];
				String vid = tokens[1];
				int flag = Integer.parseInt(tokens[2]);
				if (flag == 0) {
					users.add(uid);
					testSrcUsers[iTest] = uid;
					testDesUsers[iTest] = vid;
					testLabels[iTest] = 0;
					iTest++;
				}

			}
			br.close();

			br = new BufferedReader(new FileReader(linkFile.getAbsolutePath()));
			while ((line = br.readLine()) != null) {
				String[] tokens = line.split(",");
				String uid = tokens[0];
				String vid = tokens[1];
				int flag = Integer.parseInt(tokens[2]);
				if (flag == 0) {
					users.add(uid);
					testSrcUsers[iTest] = uid;
					testDesUsers[iTest] = vid;
					testLabels[iTest] = 1;
					iTest++;
				}
			}
			br.close();

			System.out.println("Loaded " + nTest + " testing user pairs");

		} catch (Exception e) {
			System.out.println("Error in reading user file!");
			e.printStackTrace();
			System.exit(0);
		}
	}

	private void loadNewUserData(String NewUserFile) {
		BufferedReader br = null;
		String line = null;
		newUsers = new HashSet<String>();
		try {
			File linkFile = new File(NewUserFile);
			br = new BufferedReader(new FileReader(linkFile.getAbsolutePath()));
			while ((line = br.readLine()) != null) {
				newUsers.add(line);
			}
			br.close();
		} catch (Exception e) {
			System.out.println("Error in reading user file!");
			e.printStackTrace();
			System.exit(0);
		}
	}

	private void computeCTLRScores() {
		String uid = "";
		String vid = "";
		double[] Hu;
		double[] Av;
		double HuAv = 0;
		for (int i = 0; i < testLabels.length; i++) {
			uid = testSrcUsers[i];
			vid = testDesUsers[i];
			Hu = userHubs.get(uid);
			Av = userAuthorities.get(vid);
			HuAv = 0;
			for (int z = 0; z < Hu.length; z++) {
				HuAv += Hu[z] * Av[z];
			}
			predictionScores[i] = HuAv;

		}
	}

	private void computeCTRScores() {
		String uid = null;
		String vid = null;
		double[] uU;
		double[] vV;
		double dotProduct = 0;
		for (int i = 0; i < testLabels.length; i++) {
			uid = testSrcUsers[i];
			vid = testDesUsers[i];
			uU = userUserLatentFactors.get(uid);
			vV = userItemLatentFactors.get(vid);

			// System.out.printf("u = %s v = %s isNull(uU) = %s isNull(vV) =
			// %s\n", uid, vid, (uU == null), (vV == null));

			dotProduct = 0;
			for (int z = 0; z < nTopics; z++) {
				dotProduct += uU[z] * vV[z];
			}
			predictionScores[i] = dotProduct;
		}
	}

	private void computeCommonInterestScores() {
		String uid = "";
		String vid = "";
		double[] Iu;
		double[] Iv;
		double IuIv = 0;
		for (int i = 0; i < testLabels.length; i++) {
			uid = testSrcUsers[i];
			vid = testDesUsers[i];
			Iu = userInterests.get(uid);
			Iv = userInterests.get(vid);
			IuIv = 0;
			for (int z = 0; z < Iu.length; z++) {
				IuIv += Iu[z] * Iv[z];
			}
			predictionScores[i] = IuIv;
		}
	}

	private void computeCommonNeighborScores() {
		String uid = "";
		String vid = "";
		for (int i = 0; i < testLabels.length; i++) {
			uid = testSrcUsers[i];
			vid = testDesUsers[i];
			if (userNeighbors.containsKey(uid) == false || userNeighbors.containsKey(vid) == false) {
				predictionScores[i] = 0f;
				continue;
			}

			HashSet<String> uNeighborsSet = userNeighbors.get(uid);
			HashSet<String> vNeighborsSet = userNeighbors.get(vid);

			HashSet<String> unionSet = new HashSet<String>();
			unionSet.addAll(uNeighborsSet);
			unionSet.addAll(vNeighborsSet);

			HashSet<String> intersectionSet = new HashSet<String>();
			intersectionSet.addAll(uNeighborsSet);
			intersectionSet.retainAll(vNeighborsSet);

			predictionScores[i] = (float) intersectionSet.size() / (float) unionSet.size();
		}
	}

	private void computeHITSScores() {
		String uid = "";
		String vid = "";
		double HuAv = 0;
		double Hu = 0f;
		double Av = 0f;
		for (int i = 0; i < testLabels.length; i++) {
			uid = testSrcUsers[i];
			vid = testDesUsers[i];
			Hu = traditionalHubs.get(uid);
			Av = traditionalAuthorities.get(vid);
			HuAv = Hu * Av;
			predictionScores[i] = HuAv;
		}
	}

	private void output_PredictionScores() {
		try {
			File f = new File(outputPath + "/" + predMode + "_pred_scores.csv");
			FileWriter fo = new FileWriter(f, false);

			for (int i = 0; i < testLabels.length; i++) {
				fo.write(testSrcUsers[i] + "," + testDesUsers[i] + "," + testLabels[i] + "," + predictionScores[i]
						+ "\n");
			}
			fo.close();
		} catch (Exception e) {
			System.out.println("Error in writing out post topic top words to file!");
			e.printStackTrace();
			System.exit(0);
		}
	}

	private void output_EvaluateOverallPrecisionRecall(int k, int totalPositive) {
		float[] precision = new float[k];
		float[] recall = new float[k];

		Map<Integer, Double> mapPredictionScores = new HashMap<Integer, Double>();
		for (int s = 0; s < predictionScores.length; s++) {
			mapPredictionScores.put(s, predictionScores[s]);
		}
		List<Entry<Integer, Double>> sortedScores = sortByValue(mapPredictionScores);

		for (int i = 0; i < k; i++) {
			int count = 0;
			int positiveCount = 0;
			int currK = (i + 1) * 1000;

			for (Map.Entry<Integer, Double> entry : sortedScores) {
				count++;
				if (count <= currK) {
					int index = (Integer) entry.getKey();
					int label = testLabels[index];
					if (label == 1) {
						positiveCount++;
					}
				} else {
					break;
				}
			}
			precision[i] = (float) positiveCount / (float) currK;
			recall[i] = (float) positiveCount / (float) totalPositive;
		}

		try {
			File f = new File(outputPath + "/" + predMode + "_Overall_PrecisionRecall.csv");
			FileWriter fo = new FileWriter(f, false);

			for (int i = 0; i < k; i++) {
				fo.write(i + "," + precision[i] + "," + recall[i] + "\n");
			}
			fo.close();
		} catch (Exception e) {
			System.out.println("Error in writing out post topic top words to file!");
			e.printStackTrace();
			System.exit(0);
		}
	}

	private void output_EvaluateUserLevelPrecisionRecall(int k) {
		double[] precision = new double[k];
		double[] recall = new double[k];

		HashMap<String, ArrayList<Integer>> UserLinkLabels = new HashMap<String, ArrayList<Integer>>();
		for (int u = 0; u < testSrcUsers.length; u++) {
			String uid = testSrcUsers[u];
			UserLinkLabels.put(uid, new ArrayList<Integer>());
		}

		Map<Integer, Double> mapPredictionScores = new HashMap<Integer, Double>();
		for (int s = 0; s < predictionScores.length; s++) {
			mapPredictionScores.put(s, predictionScores[s]);
		}
		List<Entry<Integer, Double>> sortedScores = sortByValue(mapPredictionScores);
		for (Map.Entry<Integer, Double> entry : sortedScores) {
			int index = entry.getKey();
			String uid = testSrcUsers[index];
			UserLinkLabels.get(uid).add(testLabels[index]);
		}

		for (int i = 0; i < k; i++) {
			int currK = i + 1;
			float sumPrecision = 0;
			float sumRecall = 0;
			int count = 0;
			Iterator<String> it = users.iterator();
			while (it.hasNext()) {
				String uid = it.next();
				int posCount = 0;
				if (userPositiveLinks.containsKey(uid) && userPositiveLinks.get(uid) >= currK) {
					count++;
					ArrayList<Integer> labels = UserLinkLabels.get(uid);
					for (int j = 0; j < currK; j++) {
						if (labels.get(j) == 1) {
							posCount++;
						}
					}
					sumPrecision += (float) posCount / (float) currK;
					sumRecall += (float) posCount / (float) userPositiveLinks.get(uid);
				}
			}
			precision[i] = sumPrecision / count;
			recall[i] = sumRecall / count;
		}

		int[] rank = new int[users.size()];
		int iRank = 0;
		Iterator<String> it = users.iterator();
		while (it.hasNext()) {
			rank[iRank] = 0;
			String uid = it.next();
			int posCount = 0;
			if (userPositiveLinks.containsKey(uid)) {
				ArrayList<Integer> labels = UserLinkLabels.get(uid);
				for (int j = 0; j < labels.size(); j++) {
					if (labels.get(j) == 1) {
						posCount++;
						if (posCount == 1) {
							rank[iRank] = j + 1;
							break;
						}
					}
				}
			}
			iRank++;
		}

		double sumMRR = 0f;
		double mrr = 0f;
		int countMRR = 0;
		for (int i = 0; i < rank.length; i++) {
			if (rank[i] != 0) {
				sumMRR += (double) 1 / (double) rank[i];
				countMRR++;
			}

		}
		mrr = sumMRR / countMRR;

		try {
			File f = new File(outputPath + "/" + predMode + "_UserLevel_PrecisionRecall.csv");
			FileWriter fo = new FileWriter(f, false);

			for (int i = 0; i < k; i++) {
				fo.write(i + "," + precision[i] + "," + recall[i] + "\n");
			}
			fo.write("MRR," + mrr + "," + mrr + "\n");
			fo.close();
			fo.close();
		} catch (Exception e) {
			System.out.println("Error in writing out post topic top words to file!");
			e.printStackTrace();
			System.exit(0);
		}

	}

	private void outout_EvaluateNewUserPrecisionRecall(int k) {
		double[] precision = new double[k];
		double[] recall = new double[k];
		HashMap<String, ArrayList<Integer>> UserLinkLabels = new HashMap<String, ArrayList<Integer>>();

		for (int u = 0; u < testSrcUsers.length; u++) {
			String uid = testSrcUsers[u];
			UserLinkLabels.put(uid, new ArrayList<Integer>());
		}

		Map<Integer, Double> mapPredictionScores = new HashMap<Integer, Double>();
		for (int s = 0; s < predictionScores.length; s++) {
			mapPredictionScores.put(s, predictionScores[s]);
		}
		List<Entry<Integer, Double>> sortedScores = sortByValue(mapPredictionScores);

		for (Map.Entry<Integer, Double> entry : sortedScores) {
			int index = entry.getKey();
			String uid = testSrcUsers[index];
			UserLinkLabels.get(uid).add(testLabels[index]);
		}

		for (int i = 0; i < k; i++) {
			int currK = i + 1;
			float sumPrecision = 0;
			float sumRecall = 0;
			int count = 0;
			Iterator<String> it = users.iterator();
			while (it.hasNext()) {
				String uid = it.next();
				int posCount = 0;
				if (newUsers.contains(uid)) {
					if (userPositiveLinks.containsKey(uid) && userPositiveLinks.get(uid) >= currK) {
						count++;
						ArrayList<Integer> labels = UserLinkLabels.get(uid);
						for (int j = 0; j < currK; j++) {
							if (labels.get(j) == 1) {
								posCount++;
							}
						}
						sumPrecision += (double) posCount / currK;
						sumRecall += (double) posCount / userPositiveLinks.get(uid);
					}
				}
			}
			precision[i] = sumPrecision / count;
			recall[i] = sumRecall / count;
		}

		int[] rank = new int[users.size()];
		int iRank = 0;
		Iterator<String> it = users.iterator();
		while (it.hasNext()) {
			rank[iRank] = 0;
			String uid = it.next();
			int posCount = 0;
			if (newUsers.contains(uid)) {
				if (userPositiveLinks.containsKey(uid)) {
					ArrayList<Integer> labels = UserLinkLabels.get(uid);
					for (int j = 0; j < labels.size(); j++) {
						if (labels.get(j) == 1) {
							posCount++;
							if (posCount == 1) {
								rank[iRank] = j + 1;
								break;
							}
						}
					}
				}
			}
			iRank++;
			// it.remove(); // avoids a ConcurrentModificationException
		}

		float sumMRR = 0f;
		float mrr = 0f;
		int countMRR = 0;
		for (int i = 0; i < rank.length; i++) {
			if (rank[i] != 0) {
				sumMRR += (float) 1 / (float) rank[i];
				countMRR++;
			}

		}
		mrr = sumMRR / countMRR;

		try {
			File f = new File(outputPath + "/" + predMode + "_NewUserLevel_PrecisionRecall.csv");
			FileWriter fo = new FileWriter(f, false);

			for (int i = 0; i < k; i++) {
				fo.write(i + "," + precision[i] + "," + recall[i] + "\n");
			}
			fo.write("MRR," + mrr + "," + mrr + "\n");
			fo.close();
		} catch (Exception e) {
			System.out.println("Error in writing out post topic top words to file!");
			e.printStackTrace();
			System.exit(0);
		}
	}

	private <K, V extends Comparable<? super V>> List<Entry<K, V>> sortByValue(Map<K, V> map) {
		List<Entry<K, V>> sortedEntries = new ArrayList<Entry<K, V>>(map.entrySet());
		Collections.sort(sortedEntries, new Comparator<Entry<K, V>>() {
			@Override
			public int compare(Entry<K, V> e1, Entry<K, V> e2) {
				return e2.getValue().compareTo(e1.getValue());
			}
		});

		return sortedEntries;
	}

}
