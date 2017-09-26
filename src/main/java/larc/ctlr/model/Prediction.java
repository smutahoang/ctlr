package larc.ctlr.model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FilenameUtils;


import larc.ctlr.model.Configure.ModelMode;
import larc.ctlr.model.Configure.PredictionMode;
import larc.ctlr.tool.RankingTool;
import larc.ctlr.tool.WeightedElement;

public class Prediction {
	public String path;
	public String ouput_path;
	public static HashMap<String, float[]> userAuthorities;
	public static HashMap<String, float[]> userHubs;
	public static HashMap<String, float[]> userInterests;
	public static HashMap<String, String> userNeighbors;
	public static HashMap<String, Float> traditionalAuthorities;
	public static HashMap<String, Float> traditionalHubs;
	public static HashMap<String, Integer> userPositiveLinks;
	public static HashMap<String, String> newUsers;
	public static HashMap<String, String> users;
	public static String[] testSrcUsers;
	public static String[] testDesUsers;
	public static int[] testLabels;
	public static float[] predictionScores;
	public static float[] newUserPredictionScores;
	public static int maxOverallTopK;
	
	
	private static PredictionMode pred_mode;

	/***
	 * read dataset from folder "path"
	 * 
	 * @param path
	 */
	public Prediction(String _path, String _model_mode, String _setting, int _nTopics, PredictionMode _pred_mode) {
		this.path = _path;
		this.ouput_path = String.format("%s/"+_model_mode+"/"+_setting+"/"+_nTopics, path);
		this.pred_mode = _pred_mode;
		
		System.out.println("loading testing data");
		String relationshipFile = String.format("%s/relationships.csv", path);
		String nonRelationshipFile = String.format("%s/nonrelationships.csv", path);
		String hitsFile = String.format("%s/user_hits.csv", path);
		String wtfwFile = String.format("%s/wtfw_results.csv", path);
		String newUserFile = String.format("%s/newusers.csv", path);
		loadTestData(relationshipFile, nonRelationshipFile);
		loadNewUserData(newUserFile);
		
		if (pred_mode == PredictionMode.CTLR){
			String authFilePath = String.format("%s/"+_model_mode+"/"+_setting+"/"+_nTopics+"/l_OptUserAuthorityDistributions.csv", path);
			int authSize = loadUserAuthorities(authFilePath,_nTopics);
			System.out.println("loaded authorities of "+authSize+" users");
			
			String hubFilePath = String.format("%s/"+_model_mode+"/"+_setting+"/"+_nTopics+"/l_OptUserHubDistributions.csv", path);
			int hubSize = loadUserHubs(hubFilePath,_nTopics);
			System.out.println("loaded hubs of "+hubSize+" users");
			
			System.out.println("compute prediction scores");
			computeCTLRScores();
			
		} else if (pred_mode == PredictionMode.COMMON_INTEREST){
			String interestFilePath = String.format("%s/"+_model_mode+"/"+_nTopics+"/l_GibbUserTopicalInterestDistributions.csv", path);
			int interestSize = loadUserInterests(interestFilePath,_nTopics);
			System.out.println("loaded interests of "+interestSize+" users");
			
			System.out.println("compute prediction scores");
			computeCommonInterestScores();
			
			this.ouput_path = String.format("%s/"+_model_mode+"/"+_nTopics, path);
			
		} else if (pred_mode == PredictionMode.COMMON_NEIGHBOR){
			int neighhorSize = loadUserNeighbors(relationshipFile);
			System.out.println("loaded neighbors of "+neighhorSize+" users");
			computeCommonNeighborScores();
		} else if (pred_mode == PredictionMode.HITS){
			loadTraditionalHITS(hitsFile);
			computeHITSScores();
		} else if (pred_mode == PredictionMode.WTFW){
			loadWTFWResults(wtfwFile);
		}
		
		output_PredictionScores();
		output_EvaluateOverallPrecisionRecall(5, maxOverallTopK);
		output_EvaluateUserLevelPrecisionRecall(5);
		outout_EvaluateNewUserPrecisionRecall(5);
		
		/*
		if (_model_mode.equals("TWITTER_LDA")){
			output_EvaluateOverallPrecisionRecall(5, maxOverallTopK);
			output_EvaluateUserLevelPrecisionRecall(5);
		} else{
			output_EvaluateOverallPrecisionRecall(5, maxOverallTopK);
			output_EvaluateUserLevelPrecisionRecall(5);
		}
		*/
	}
	
	public int loadUserAuthorities(String filename,int nTopics) {
		Scanner sc = null;
		BufferedReader br = null;
		String line = null;
		float[] authorities;
		userAuthorities = new HashMap<String, float[]>();
		try {
			File authFile = new File(filename);
			br = new BufferedReader(new FileReader(authFile.getAbsolutePath()));
			while ((line = br.readLine()) != null) {
				sc = new Scanner(line.toString());
				sc.useDelimiter(",");
				authorities = new float[nTopics];
				String uid= "";
				while (sc.hasNext()) {
					uid = sc.next();
					sc.next(); //Follower count not needed
					sc.next(); //Following count not needed
					sc.next(); //Post count not needed
					sc.next(); //Non follower count not needed
					sc.next(); //Non following count not needed
					for (int i=0;i<nTopics;i++){
						authorities[i] = sc.nextFloat();
					}
				}
				userAuthorities.put(uid,authorities);
			}
			br.close();
		} catch (Exception e) {
			System.out.println("Error in reading user file!");
			e.printStackTrace();
			System.exit(0);
		}
		return userAuthorities.size();
	}
	
	public int loadUserHubs(String filename, int nTopics) {
		Scanner sc = null;
		BufferedReader br = null;
		String line = null;
		float[] hubs;
		userHubs = new HashMap<String, float[]>();
		try {
			File hubFile = new File(filename);
			br = new BufferedReader(new FileReader(hubFile.getAbsolutePath()));
			while ((line = br.readLine()) != null) {
				sc = new Scanner(line.toString());
				sc.useDelimiter(",");
				hubs = new float[nTopics];
				String uid= "";
				while (sc.hasNext()) {
					uid = sc.next();
					sc.next(); //Follower count not needed
					sc.next(); //Following count not needed
					sc.next(); //Post count not needed
					sc.next(); //Non follower count not needed
					sc.next(); //Non following count not needed
					for (int i=0;i<nTopics;i++){
						hubs[i] = sc.nextFloat();
					}
				}
				userHubs.put(uid,hubs);
			}
			br.close();
		} catch (Exception e) {
			System.out.println("Error in reading user file!");
			e.printStackTrace();
			System.exit(0);
		}
		return userHubs.size();
	}
	
	public int loadUserInterests(String filename, int nTopics){
		Scanner sc = null;
		BufferedReader br = null;
		String line = null;
		float[] interests;
		userInterests = new HashMap<String, float[]>();
		try {
			File interestFile = new File(filename);
			br = new BufferedReader(new FileReader(interestFile.getAbsolutePath()));
			while ((line = br.readLine()) != null) {
				sc = new Scanner(line.toString());
				sc.useDelimiter(",");
				interests = new float[nTopics];
				String uid= "";
				while (sc.hasNext()) {
					uid = sc.next();
					for (int i=0;i<nTopics;i++){
						interests[i] = sc.nextFloat();
					}
				}
				userInterests.put(uid,interests);
			}
			br.close();
		} catch (Exception e) {
			System.out.println("Error in reading user file!");
			e.printStackTrace();
			System.exit(0);
		}
		return userInterests.size();
	}
	
	public int loadUserNeighbors(String relationshipFile){
		Scanner sc = null;
		BufferedReader br = null;
		String line = null;
		float[] interests;
		userNeighbors = new HashMap<String, String>();
		try {
			File linkFile = new File(relationshipFile);
			
			br = new BufferedReader(new FileReader(linkFile.getAbsolutePath()));
			while ((line = br.readLine()) != null) {
				sc = new Scanner(line.toString());
				sc.useDelimiter(",");
				String uid = "";
				String vid = "";
				int flag = 0;
				while (sc.hasNext()) {
					uid = sc.next();
					vid = sc.next();
					flag = sc.nextInt();
					//Only consider relationship in the train set
					if (flag == 1){
						if (userNeighbors.containsKey(uid)){
							String neighbours = userNeighbors.get(uid);
							neighbours = neighbours + ","+ vid.trim();
							userNeighbors.put(uid, neighbours);
						} else {
							userNeighbors.put(uid, vid);
						}
						if (userNeighbors.containsKey(vid)){
							String neighbours = userNeighbors.get(vid);
							neighbours = neighbours + ","+ uid.trim();
							userNeighbors.put(vid, neighbours);
						} else {
							userNeighbors.put(vid, uid);
						}
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
	
	public void loadTraditionalHITS(String filename) {
		Scanner sc = null;
		BufferedReader br = null;
		String line = null;
		traditionalAuthorities = new HashMap<String, Float>();
		traditionalHubs= new HashMap<String, Float>();
		try {
			File hitFile = new File(filename);
			br = new BufferedReader(new FileReader(hitFile.getAbsolutePath()));
			while ((line = br.readLine()) != null) {
				sc = new Scanner(line.toString());
				sc.useDelimiter(",");
				String uid= "";
				float authority = 0f;
				float hub = 0f;
				while (sc.hasNext()) {
					uid = sc.next();
					authority = sc.nextFloat();
					hub = sc.nextFloat();
					traditionalAuthorities.put(uid, authority);
					traditionalHubs.put(uid, hub);
				}
			}
			br.close();
		} catch (Exception e) {
			System.out.println("Error in reading user file!");
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public void loadTestData(String relationshipFile, String nonRelationshipFile) {
		Scanner sc = null;
		BufferedReader br = null;
		String line = null;
		int nNewUserTest = 0;
		int nTest = 0;
		int iTest = 0;
		maxOverallTopK = 0;
		userPositiveLinks = new HashMap<String, Integer>();
		users = new HashMap<String, String>();
		
		try {
			File linkFile = new File(relationshipFile);
			File nonLinkFile = new File(nonRelationshipFile);
			
			br = new BufferedReader(new FileReader(nonLinkFile.getAbsolutePath()));
			while ((line = br.readLine()) != null) {
				sc = new Scanner(line.toString());
				sc.useDelimiter(",");
				String uid = "";
				String vid = "";
				int flag = 0;
				while (sc.hasNext()) {
					uid = sc.next();
					vid = sc.next();
					flag = sc.nextInt();
					if (flag == 0){
						nTest++;
					}
				}
			}
			br.close();
			
			br = new BufferedReader(new FileReader(linkFile.getAbsolutePath()));
			while ((line = br.readLine()) != null) {
				sc = new Scanner(line.toString());
				sc.useDelimiter(",");
				String uid = "";
				String vid = "";
				int flag = 0;
				while (sc.hasNext()) {
					uid = sc.next();
					vid = sc.next();
					flag = sc.nextInt();
					if (flag == 0){
						maxOverallTopK++;
						nTest++;
						if (userPositiveLinks.containsKey(uid)){
							int count = userPositiveLinks.get(uid) +1;
							userPositiveLinks.put(uid, count);
						} else {
							userPositiveLinks.put(uid,1);
						}
					}	
				}
			}
			br.close();
			
			testSrcUsers = new String[nTest];
			testDesUsers = new String[nTest];
			testLabels = new int[nTest];
			predictionScores = new float[nTest];
			
			br = new BufferedReader(new FileReader(nonLinkFile.getAbsolutePath()));
			while ((line = br.readLine()) != null) {
				sc = new Scanner(line.toString());
				sc.useDelimiter(",");
				String uid = "";
				String vid = "";
				int flag = 0;
				while (sc.hasNext()) {
					uid = sc.next();
					vid = sc.next();
					flag = sc.nextInt();
					if (flag == 0){
						users.put(uid, uid);
						testSrcUsers[iTest] = uid; 
						testDesUsers[iTest] = vid;
						testLabels[iTest] = 0;
						iTest++;
					}
				}
			}
			br.close();
			
			br = new BufferedReader(new FileReader(linkFile.getAbsolutePath()));
			while ((line = br.readLine()) != null) {
				sc = new Scanner(line.toString());
				sc.useDelimiter(",");
				String uid = "";
				String vid = "";
				int flag = 0;
				while (sc.hasNext()) {
					uid = sc.next();
					vid = sc.next();
					flag = sc.nextInt();
					if (flag == 0){
						users.put(uid, uid);
						testSrcUsers[iTest] = uid; 
						testDesUsers[iTest] = vid;
						testLabels[iTest] = 1;
						iTest++;
					}
				}
			}
			br.close();
			
			System.out.println("Loaded "+nTest+" testing user pairs");
			
		} catch (Exception e) {
			System.out.println("Error in reading user file!");
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public void loadNewUserData(String NewUserFile){
		Scanner sc = null;
		BufferedReader br = null;
		String line = null;
		newUsers = new HashMap<String, String>();
		try {
			File linkFile = new File(NewUserFile);
			br = new BufferedReader(new FileReader(linkFile.getAbsolutePath()));
			while ((line = br.readLine()) != null) {
				sc = new Scanner(line.toString());
				sc.useDelimiter(",");
				String uid = "";
				while (sc.hasNext()) {
					uid = sc.next();
					newUsers.put(uid, uid);
				}
			}
			br.close();
		} catch (Exception e) {
			System.out.println("Error in reading user file!");
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public void loadWTFWResults(String filename) {
		Scanner sc = null;
		BufferedReader br = null;
		String line = null;
		try {
			File wtfwFile = new File(filename);
			br = new BufferedReader(new FileReader(wtfwFile.getAbsolutePath()));
			int index=0;
			while ((line = br.readLine()) != null) {
				sc = new Scanner(line.toString());
				sc.useDelimiter(",");
				String uid= "";
				float authority = 0f;
				float hub = 0f;
				while (sc.hasNext()) {
					sc.next();//src_uid
					sc.next();//des_uid
					float score = sc.nextFloat(); 
					sc.next();//label
					predictionScores[index]=score;
					index++;
				}
			}
			br.close();
		} catch (Exception e) {
			System.out.println("Error in reading user file!");
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	
	public void computeCTLRScores(){
		String uid = "";
		String vid = "";
		float[] Hu;
		float[] Av;
		float HuAv=0;
		for (int i=0; i< testLabels.length; i++){
			uid = testSrcUsers[i];
			vid = testDesUsers[i];
			Hu = userHubs.get(uid);
			Av = userAuthorities.get(vid);
			HuAv = 0;
			for (int z=0; z<Hu.length; z++){
				HuAv += Hu[z] * Av[z];
			}
			predictionScores[i] = HuAv;
			
		}
	}
	
	public void computeCommonInterestScores(){
		String uid = "";
		String vid = "";
		float[] Iu;
		float[] Iv;
		float IuIv=0;
		for (int i=0; i< testLabels.length; i++){
			uid = testSrcUsers[i];
			vid = testDesUsers[i];
			Iu = userInterests.get(uid);
			Iv = userInterests.get(vid);
			IuIv = 0;
			for (int z=0; z<Iu.length; z++){
				IuIv += Iu[z] * Iv[z];
			}
			predictionScores[i] = IuIv;
		}
	}
	
	public void computeCommonNeighborScores(){
		String uid = "";
		String vid = "";
		String[] uNeighbors;
		String[] vNeighbors;	
		for (int i=0; i< testLabels.length; i++){
			uid = testSrcUsers[i];
			vid = testDesUsers[i];
			if (userNeighbors.containsKey(uid)== false || userNeighbors.containsKey(vid)== false){
				predictionScores[i] = 0f;
				continue;
			} 
			uNeighbors = userNeighbors.get(uid).split(",");
			vNeighbors = userNeighbors.get(vid).split(",");
			
			Set uNeighborsSet = new HashSet(Arrays.asList(uNeighbors));
			Set vNeighborsSet = new HashSet(Arrays.asList(vNeighbors));
			
			Set unionSet = new HashSet();
			unionSet.addAll(uNeighborsSet);
			unionSet.addAll(vNeighborsSet);
			
			Set intersectionSet = new HashSet();
			intersectionSet.addAll(uNeighborsSet);
			intersectionSet.retainAll(vNeighborsSet);
				
			predictionScores[i] = (float)intersectionSet.size() / (float)unionSet.size();	
		}
	}
	
	public void computeHITSScores(){
		String uid = "";
		String vid = "";
		float HuAv=0;
		float Hu=0f;
		float Av=0f;
		for (int i=0; i< testLabels.length; i++){
			uid = testSrcUsers[i];
			vid = testDesUsers[i];
			Hu = traditionalHubs.get(uid);
			Av = traditionalAuthorities.get(vid);
			HuAv = Hu*Av;
			predictionScores[i] = HuAv;
		}
	}
	
	
	public void output_PredictionScores(){
		try {
			File f = new File(ouput_path + "/" + pred_mode +"_pred_scores.csv");
			FileWriter fo = new FileWriter(f,false);
			
			for (int i=0; i< testLabels.length; i++){
				fo.write(testSrcUsers[i]+ ","
						+ testDesUsers[i] + ","
						+ testLabels[i] + "," 
						+ predictionScores[i]+"\n");
			}	    
			fo.close();
		} catch (Exception e) {
			System.out.println("Error in writing out post topic top words to file!");
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public void output_EvaluateOverallPrecisionRecall(int k, int totalPositive){
		float[] precision = new float[k];
		float[] recall = new float[k];
		
		Map<Integer, Float> mapPredictionScores = new HashMap<Integer, Float>();
		for (int s=0; s<predictionScores.length; s++){
			mapPredictionScores.put(s, predictionScores[s]);
		}
		List<Entry<Integer, Float>> sortedScores =  sortByValue(mapPredictionScores);

		
		for (int i=0; i<k; i++){
			int count = 0;
			int positiveCount = 0;
			int currK = (i+1) * 1000;
			
			for (Map.Entry<Integer, Float> entry : sortedScores) {
			  count++;
			  if (count<=currK){
				  int index = (Integer) entry.getKey();
				  int label = testLabels[index];
				  if (label==1){
					positiveCount++;
				  }
			  } else{
				  break;
			  }
			}
			precision[i] = (float)positiveCount/(float)currK;
			recall[i] = (float)positiveCount/(float)totalPositive;
		}
		
		try {
			File f = new File(ouput_path + "/" + pred_mode +"_Overall_PrecisionRecall.csv");
			FileWriter fo = new FileWriter(f,false);
			
			for (int i=0; i<k ; i++){
				fo.write(i+ ","
						+ precision[i] + ","
						+ recall[i] + "\n");
			}	    
			fo.close();
		} catch (Exception e) {
			System.out.println("Error in writing out post topic top words to file!");
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public void output_EvaluateUserLevelPrecisionRecall(int k){
		float[] precision = new float[k];
		float[] recall = new float[k];
		Random rand = new Random();
		
		HashMap <String, String> UserLinkLabels = new HashMap <String, String>();
		HashMap <String, String> UserLinkZeroLabels = new HashMap <String, String>();
		
		for (int u=0; u<testSrcUsers.length; u++){
			String uid = testSrcUsers[u];
			UserLinkLabels.put(uid, "");
			UserLinkZeroLabels.put(uid, "");
		}
		
		Map<Integer, Float> mapPredictionScores = new HashMap<Integer, Float>();
		for (int s=0; s<predictionScores.length; s++){
			mapPredictionScores.put(s, predictionScores[s]);
		}
		List<Entry<Integer, Float>> sortedScores =  sortByValue(mapPredictionScores);
		for (Map.Entry<Integer, Float> entry : sortedScores) {
			int index = (Integer) entry.getKey();
			float score = (Float) entry.getValue();
			String label = Integer.toString(testLabels[index]);
			String uid = testSrcUsers[index];
			/*
			if(score == 0){
				int coin = rand.nextInt(2);
				if (coin == 0){
					String labels = UserLinkZeroLabels.get(uid)+ " "+label;
					UserLinkZeroLabels.put(uid, labels.trim());
				} else {
					String labels = label+" "+UserLinkZeroLabels.get(uid);
					UserLinkZeroLabels.put(uid, labels.trim());
				}
			} else {
				String labels = UserLinkLabels.get(uid)+ " "+label;
				UserLinkLabels.put(uid, labels.trim());
			}
			*/
			String labels = UserLinkLabels.get(uid)+ " "+label;
			UserLinkLabels.put(uid, labels.trim());
			
		}
		
		/*
		for (int u=0; u<testSrcUsers.length; u++){
			String uid = testSrcUsers[u];
			String finalLabels = UserLinkLabels.get(uid) + " " + UserLinkZeroLabels.get(uid);
			UserLinkLabels.put(uid, finalLabels.trim());
		}
		*/
		
		for (int i=0; i<k; i++){
			int currK = i+1;
			float sumPrecision = 0;
			float sumRecall = 0;
			int count = 0;
			Iterator it = users.entrySet().iterator();
		    while (it.hasNext()) {
		        Map.Entry pair = (Map.Entry)it.next();
		        int posCount = 0;
				String uid = pair.getKey().toString();
				if (userPositiveLinks.containsKey(uid) && userPositiveLinks.get(uid)>=currK){
					count++;
					String[] labels = UserLinkLabels.get(uid).split(" ");
					for (int l=0; l<currK; l++){
						if (labels[l].equals("1")){
							posCount++;
						}
					}
					sumPrecision += (float)posCount / (float)currK;
					sumRecall += (float)posCount / (float)userPositiveLinks.get(uid);
				}
		    }
			precision[i] = sumPrecision/count;
			recall[i] = sumRecall/count;
			
		}
		
		int[] rank = new int[users.size()];
		int iRank =0;
		Iterator it = users.entrySet().iterator();
	    while (it.hasNext()) {
	    	rank[iRank] = 0;
	        Map.Entry pair = (Map.Entry)it.next();
	        int posCount = 0;
			String uid = pair.getKey().toString();
			if (userPositiveLinks.containsKey(uid)){
				String[] labels = UserLinkLabels.get(uid).split(" ");
				for (int l=0; l<labels.length; l++){
					if (labels[l].equals("1")){
						posCount++;
						if (posCount == 1){
							rank[iRank] = l+1;
							break;
						}
					}
				}		
			}
			iRank++;
	        //it.remove(); // avoids a ConcurrentModificationException
	    }
	    
	    float sumMRR =0f;
	    float mrr = 0f;
	    int countMRR =0;
	    for (int i=0; i<rank.length;i++){
	    	if (rank[i]!=0){
	    		sumMRR += (float)1/ (float)rank[i];
	    		countMRR++;
	    	}
	    	
	    }
	    mrr = sumMRR/countMRR;
		
		try {
			File f = new File(ouput_path + "/" + pred_mode +"_UserLevel_PrecisionRecall.csv");
			FileWriter fo = new FileWriter(f,false);
			
			for (int i=0; i<k ; i++){
				fo.write(i+ ","
						+ precision[i] + ","
						+ recall[i] + "\n");
			}
			fo.write("MRR,"
					+ mrr + ","
					+ mrr + "\n");
			fo.close();
			fo.close();
		} catch (Exception e) {
			System.out.println("Error in writing out post topic top words to file!");
			e.printStackTrace();
			System.exit(0);
		}
		
	}
	
	public void outout_EvaluateNewUserPrecisionRecall(int k){
		float[] precision = new float[k];
		float[] recall = new float[k];
		Random rand = new Random();
		
		HashMap <String, String> UserLinkLabels = new HashMap <String, String>();
		HashMap <String, String> UserLinkZeroLabels = new HashMap <String, String>();
		
		for (int u=0; u<testSrcUsers.length; u++){
			String uid = testSrcUsers[u];
			UserLinkLabels.put(uid, "");
			UserLinkZeroLabels.put(uid, "");
		}
		
		Map<Integer, Float> mapPredictionScores = new HashMap<Integer, Float>();
		for (int s=0; s<predictionScores.length; s++){
			mapPredictionScores.put(s, predictionScores[s]);
		}
		List<Entry<Integer, Float>> sortedScores =  sortByValue(mapPredictionScores);
		
		for (Map.Entry<Integer, Float> entry : sortedScores) {
			int index = (Integer) entry.getKey();
			float score = (Float) entry.getValue();
			String label = Integer.toString(testLabels[index]);
			String uid = testSrcUsers[index];
			String labels = UserLinkLabels.get(uid)+ " "+label;
			UserLinkLabels.put(uid, labels.trim());
		}
		
		for (int i=0; i<k; i++){
			int currK = i+1;
			float sumPrecision = 0;
			float sumRecall = 0;
			int count = 0;
			Iterator it = users.entrySet().iterator();
		    while (it.hasNext()) {
		    	Map.Entry pair = (Map.Entry)it.next();
		        int posCount = 0;
				String uid = pair.getKey().toString();
				if (newUsers.containsKey(uid)){
					if (userPositiveLinks.containsKey(uid) && userPositiveLinks.get(uid)>=currK){
						count++;
						String[] labels = UserLinkLabels.get(uid).split(" ");
						for (int l=0; l<currK; l++){
							if (labels[l].equals("1")){
								posCount++;
							}
						}
						sumPrecision += (float)posCount / (float)currK;
						sumRecall += (float)posCount / (float)userPositiveLinks.get(uid);
					}
				}
		    }
			precision[i] = sumPrecision/count;
			recall[i] = sumRecall/count;
		}
		
		int[] rank = new int[users.size()];
		int iRank =0;
		Iterator it = users.entrySet().iterator();
	    while (it.hasNext()) {
	    	rank[iRank] = 0;
	        Map.Entry pair = (Map.Entry)it.next();
	        int posCount = 0;
			String uid = pair.getKey().toString();
			if (newUsers.containsKey(uid)){
				if (userPositiveLinks.containsKey(uid)){
					String[] labels = UserLinkLabels.get(uid).split(" ");
					for (int l=0; l<labels.length; l++){
						if (labels[l].equals("1")){
							posCount++;
							if (posCount == 1){
								rank[iRank] = l+1;
								break;
							}
						}
					}		
				}
			}
			iRank++;
	        //it.remove(); // avoids a ConcurrentModificationException
	    }
	    
	    float sumMRR =0f;
	    float mrr = 0f;
	    int countMRR =0;
	    for (int i=0; i<rank.length;i++){
	    	if (rank[i]!=0){
	    		sumMRR += (float)1/ (float)rank[i];
	    		countMRR++;
	    	}
	    	
	    }
	    mrr = sumMRR/countMRR;
		
		try {
			File f = new File(ouput_path + "/" + pred_mode +"_NewUserLevel_PrecisionRecall.csv");
			FileWriter fo = new FileWriter(f,false);
			
			for (int i=0; i<k ; i++){
				fo.write(i+ ","
						+ precision[i] + ","
						+ recall[i] + "\n");
			}
			fo.write("MRR,"
					+ mrr + ","
					+ mrr + "\n");
			fo.close();
		} catch (Exception e) {
			System.out.println("Error in writing out post topic top words to file!");
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public <K,V extends Comparable<? super V>> List<Entry<K, V>> sortByValue(Map<K,V> map) {
		List<Entry<K,V>> sortedEntries = new ArrayList<Entry<K,V>>(map.entrySet());
		Collections.sort(sortedEntries, 
				new Comparator<Entry<K,V>>() {
			@Override
			public int compare(Entry<K,V> e1, Entry<K,V> e2) {
				return e2.getValue().compareTo(e1.getValue());
				}
			}
		);
		
		return sortedEntries;
	}
	
}
