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
	private int testBatch;
	private PredictionMode predMode;
	private String outputPath;
	// CTRL model
	private HashMap<String, double[]> userAuthorities;
	private HashMap<String, double[]> userHubs;
	private HashMap<String, double[]> userInterests;
	// Common-Neighbor
	private HashMap<String, HashSet<String>> userNeighbors;
	private HashMap<String, HashSet<String>> userFollowers;
	private HashMap<String, HashSet<String>> userFollowees;
	private HashMap<String, Double> traditionalAuthorities;
	// HIST
	private HashMap<String, Double> traditionalHubs;
	// CTR
	private HashMap<String, double[]> userUserLatentFactors;
	private HashMap<String, double[]> userItemLatentFactors;
	// data
	private HashMap<String, String> userAllPositiveLinks;
	private HashMap<String, Integer> userTestPositiveLinks;
	private HashMap<String, Integer> userTestNegativeLinks;
	private HashMap<String, String> userNonLinks;
	private HashSet<String> newUsers;
	private int nUsers;
	private int nTest;
	private String[] users;
	private String[] testSrcUsers;
	private String[] testDesUsers;
	private int[] testLabels;
	private double[] predictionScores;
	private int maxOverallTopK;

	/***
	 * read dataset from folder "path"
	 * 
	 * @param dataPath
	 */
	public Prediction(String _path, String _resultPath, String _modelMode, String _setting, int _nTopics,
			int _testBatch, PredictionMode _predMode, String _outputPath) {
		this.dataPath = _path;
		this.resultPath = _resultPath;
		this.modelMode = _modelMode;
		this.setting = _setting;
		this.nTopics = _nTopics;
		this.testBatch = _testBatch;
		this.predMode = _predMode;
		if (predMode == PredictionMode.CTLR) {
			this.outputPath = String.format("%s/%s/%s/%d", _outputPath, modelMode, setting, nTopics);
		} else if (predMode == PredictionMode.WTFW) {
			this.outputPath = String.format("%s/%d/%s", _outputPath, nTopics, setting);
		} else if (predMode == PredictionMode.CTR) {
			this.outputPath = String.format("%s/%d", _outputPath, nTopics);
		} else if (predMode == PredictionMode.COMMON_INTEREST) {
			this.outputPath = String.format("%s/%s/%s/%d", _outputPath, modelMode, setting, nTopics);
		}else {
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
		String userFile = String.format("%s/users.csv", dataPath);
		String hitsFile = String.format("%s/user_hits.csv", dataPath);
		String wtfwFile = String.format("%s/wtfw_results.csv", dataPath);
		//int neighhorSize = loadUserNeighbors(relationshipFile);
		int neighhorSize = loadUserDirectedNeighbors(relationshipFile);
		System.out.println("loaded neighbors of " + neighhorSize + " users");
		loadTestData(relationshipFile, userFile);
		//output_NonLinks();
		
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
			String interestFilePath = String.format("%s/%s/%s/%d/l_GibbUserTopicalInterestDistributions.csv", resultPath,
					modelMode,setting, nTopics);
			int interestSize = loadUserInterests(interestFilePath, nTopics);
			System.out.println("loaded interests of " + interestSize + " users");

			System.out.println("compute prediction scores");
			computeCommonInterestScores();
		} else if (predMode == PredictionMode.COMMON_NEIGHBOR) {
			//computeCommonNeighborScores();
			computeCommonDirectedNeighborScores();
		} else if (predMode == PredictionMode.HITS) {
			loadTraditionalHITS(hitsFile);
			computeHITSScores();
		} else if (predMode == PredictionMode.WTFW) {
			loadWTFWScores(wtfwFile);
		}

		output_PredictionScores();
		output_EvaluateOverallPrecisionRecall(5, maxOverallTopK);
		output_EvaluateUserLevelPrecisionRecall(5);
		
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

	private int loadUserDirectedNeighbors(String relationshipFile) {
		BufferedReader br = null;
		String line = null;
		userNeighbors = new HashMap<String, HashSet<String>>();
		userFollowers = new HashMap<String, HashSet<String>>();
		userFollowees = new HashMap<String, HashSet<String>>();
		try {
			File linkFile = new File(relationshipFile);

			br = new BufferedReader(new FileReader(linkFile.getAbsolutePath()));
			while ((line = br.readLine()) != null) {
				String[] tokens = line.split(",");
				String uid = tokens[0];
				String vid = tokens[1];
				int flag = Integer.parseInt(tokens[2]);
				if (flag == 1) {
					if (userFollowees.containsKey(uid)) {
						userFollowees.get(uid).add(vid);
					} else {
						HashSet<String> followees = new HashSet<String>();
						followees.add(vid);
						userFollowees.put(uid, followees);
					}
					if (userFollowers.containsKey(vid)) {
						userFollowers.get(vid).add(uid);
					} else {
						HashSet<String> followers = new HashSet<String>();
						followers.add(uid);
						userFollowers.put(vid, followers);
					}
				}
			}
			br.close();

		} catch (Exception e) {
			System.out.println("Error in reading user file!");
			e.printStackTrace();
			System.exit(0);
		}
		return userFollowers.size();
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

	private void loadWTFWScores(String filename){
		BufferedReader br = null;
		String line = null;
		int index =0;
		try {
			File wtfwFile = new File(filename);
			br = new BufferedReader(new FileReader(wtfwFile.getAbsolutePath()));
			while ((line = br.readLine()) != null) {
				String[] tokens = line.split(",");
				double score = Double.parseDouble(tokens[2]);
				predictionScores[index] = score;
				index++;
			}
			br.close();
		} catch (Exception e) {
			System.out.println("Error in reading user file!");
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	private void loadTestData(String _relationshipFile, String _userFile){
		BufferedReader br = null;
		String line = null;
		int nUser =0;
		nTest = 0;
		maxOverallTopK = 0;
		userTestPositiveLinks = new HashMap<String, Integer>();
		userAllPositiveLinks =  new HashMap<String, String>();
		userNonLinks =  new HashMap<String, String>();
		userTestNegativeLinks = new HashMap<String, Integer>();
		
		try {
			File userFile = new File(_userFile);
			br = new BufferedReader(new FileReader(userFile.getAbsolutePath()));
			while ((line = br.readLine()) != null) {
				nUser++;
			}
			br.close();
			
			users = new String[nUser];
			
			int iUser=0;
			br = new BufferedReader(new FileReader(userFile.getAbsolutePath()));
			while ((line = br.readLine()) != null) {
				String[] tokens = line.split(",");
				users[iUser] = tokens[0];
				iUser++;
			}
			br.close();
			
			File linkFile = new File(_relationshipFile);
			br = new BufferedReader(new FileReader(linkFile.getAbsolutePath()));
			while ((line = br.readLine()) != null) {
				String[] tokens = line.split(",");
				String uid = tokens[0];
				String vid = tokens[1];
				String link = uid.trim()+" "+vid.trim();
				userAllPositiveLinks.put(link, link);
				int batch = Integer.parseInt(tokens[2]);
				if (batch == testBatch) {
					maxOverallTopK++;
					nTest++;
					if (userTestPositiveLinks.containsKey(uid)) {
						int count = userTestPositiveLinks.get(uid) + 1;
						userTestPositiveLinks.put(uid, count);
					} else {
						userTestPositiveLinks.put(uid, 1);
					}
				}
			}
			br.close();
			
			//find non-links with common neighbor (2-hops)
			String nonLink = "";
			for (int u=0; u<users.length; u++){
				String uid = users[u];
				for (int v=0; v<users.length; v++){
					String vid = users[v];
					nonLink = uid.trim() + " " + vid.trim();
					if (u==v){
						continue;
					}
					if (userFollowers.containsKey(uid) == false){
						continue;
					} 
					if(userFollowees.containsKey(vid) == false){
						continue;
					}
					if (userAllPositiveLinks.containsKey(nonLink)){
						continue;
					}
			
					HashSet<String> uNeighborsSet = userFollowers.get(uid);
					HashSet<String> vNeighborsSet = userFollowees.get(vid);
					//HashSet<String> unionSet = new HashSet<String>();
					//unionSet.addAll(uNeighborsSet);
					//unionSet.addAll(vNeighborsSet);
					HashSet<String> intersectionSet = new HashSet<String>();
					intersectionSet.addAll(uNeighborsSet);
					intersectionSet.retainAll(vNeighborsSet);
					if (intersectionSet.size()>=1){
						nTest++;
						userNonLinks.put(nonLink, nonLink);
						if(userTestNegativeLinks.containsKey(uid)){
							int count = userTestNegativeLinks.get(uid)+1;
							userTestNegativeLinks.put(uid,count);
						} else {
							userTestNegativeLinks.put(uid,1);
						}
					}
				}
			}
			
//			//find non-links with common neighbor (2-hops)
//			String nonLink = "";
//			for (int u=0; u<users.length; u++){
//				String uid = users[u];
//				for (int v=0; v<users.length; v++){
//					String vid = users[v];
//					nonLink = uid.trim() + " " + vid.trim();
//					if (u==v){
//						continue;
//					}
//					if (userNeighbors.containsKey(uid) == false){
//						continue;
//					} 
//					if(userNeighbors.containsKey(vid) == false){
//						continue;
//					}
//					if (userAllPositiveLinks.containsKey(nonLink)){
//						continue;
//					}
//			
//					HashSet<String> uNeighborsSet = userNeighbors.get(uid);
//					HashSet<String> vNeighborsSet = userNeighbors.get(vid);
//					HashSet<String> unionSet = new HashSet<String>();
//					unionSet.addAll(uNeighborsSet);
//					unionSet.addAll(vNeighborsSet);
//					HashSet<String> intersectionSet = new HashSet<String>();
//					intersectionSet.addAll(uNeighborsSet);
//					intersectionSet.retainAll(vNeighborsSet);
//					float score = (float) intersectionSet.size() / (float) unionSet.size();
//					if (score>=0.008){
//						nTest++;
//						userNonLinks.put(nonLink, nonLink);
//						if(userTestNegativeLinks.containsKey(uid)){
//							int count = userTestNegativeLinks.get(uid)+1;
//							userTestNegativeLinks.put(uid,count);
//						} else {
//							userTestNegativeLinks.put(uid,1);
//						}
//					}
//				}
//			}
			
			testSrcUsers = new String[nTest];
			testDesUsers = new String[nTest];
			testLabels = new int[nTest];
			predictionScores = new double[nTest];
			
			int iTest = 0;
			Iterator it = userNonLinks.entrySet().iterator();
		    while (it.hasNext()) {
		        Map.Entry pair = (Map.Entry)it.next();
		        String[] nonLinkPair = pair.getValue().toString().split(" ");
		        testSrcUsers[iTest] = nonLinkPair[0];
				testDesUsers[iTest] = nonLinkPair[1];
				testLabels[iTest] = 0;
				iTest++;
		    }
			
			System.out.println("Generated " + iTest + " non links");

			br = new BufferedReader(new FileReader(linkFile.getAbsolutePath()));
			while ((line = br.readLine()) != null) {
				String[] tokens = line.split(",");
				String uid = tokens[0];
				String vid = tokens[1];
				int batch = Integer.parseInt(tokens[2]);
				if (batch == testBatch) {
					testSrcUsers[iTest] = uid;
					testDesUsers[iTest] = vid;
					testLabels[iTest] = 1;
					iTest++;
				}
			}
			br.close();

			System.out.println("Loaded " + nTest + " links");

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
	
	private void computeCommonDirectedNeighborScores() {
		String uid = "";
		String vid = "";
		for (int i = 0; i < testLabels.length; i++) {
			uid = testSrcUsers[i];
			vid = testDesUsers[i];
			if (userFollowees.containsKey(uid) == false || userFollowers.containsKey(vid) == false) {
				predictionScores[i] = 0f;
				continue;
			}

			HashSet<String> uNeighborsSet = userFollowees.get(uid);
			HashSet<String> vNeighborsSet = userFollowers.get(vid);

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
			int checkPosCount = 0;
			int currK = i + 1;
			float sumPrecision = 0;
			float sumRecall = 0;
			int count = 0;
			for (int u=0; u<users.length; u++){
				String uid = users[u];
				int posCount = 0;
				if (userTestPositiveLinks.containsKey(uid) && userTestPositiveLinks.get(uid) >= currK) {
					if (userTestNegativeLinks.containsKey(uid) && userTestNegativeLinks.get(uid)>=currK){
						checkPosCount += userTestPositiveLinks.get(uid);
						count++;
						ArrayList<Integer> labels = UserLinkLabels.get(uid);
						for (int j = 0; j < currK; j++) {
							if (labels.get(j) == 1) {
								posCount++;
							}
						}
						sumPrecision += (float) posCount / (float) currK;
						sumRecall += (float) posCount / (float) userTestPositiveLinks.get(uid);
					}
				}
			}
			System.out.println("#PositiveLinks@"+k+": "+checkPosCount);
			precision[i] = sumPrecision / count;
			recall[i] = sumRecall / count;
		}

		int[] rank = new int[users.length];
		int iRank = 0;
		for (int u=0; u<users.length; u++){
			String uid = users[u];	
			rank[iRank] = 0;
			int posCount = 0;
			if (userTestPositiveLinks.containsKey(uid) && userTestNegativeLinks.containsKey(uid)) {
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

	public void output_NonLinks() {
		try {
			File f = new File(outputPath + "/" + "l_predictionTestLinks.csv");
			FileWriter fo = new FileWriter(f, false);

			for (int i = 0; i < testLabels.length; i++) {
				fo.write(testSrcUsers[i] + "," + testDesUsers[i] + "," + testLabels[i] + "\n");
			}
			fo.close();
		} catch (Exception e) {
			System.out.println("Error in writing out post topic top words to file!");
			e.printStackTrace();
			System.exit(0);
		}
	}
	
}
