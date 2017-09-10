package larc.ctlr.model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Iterator;

import org.apache.commons.io.FilenameUtils;

import larc.ctlr.tool.RankingTool;
import larc.ctlr.tool.WeightedElement;

public class Prediction {
	public String path;
	public HashMap<String, float[]> userAuthorities;
	public HashMap<String, float[]> userHubs;
	public String[] trainSrcUsers;
	public String[] trainDesUsers;
	public String[] testSrcUsers;
	public String[] testDesUsers;
	public HashMap<String, Integer> trainLabels;
	public HashMap<String, Integer> testLabels;
	public HashMap<String, Float> predictionScores;
	

	/***
	 * read dataset from folder "path"
	 * 
	 * @param path
	 */
	public Prediction(String path, String mode, String setting, int nTopics) {
		this.path = path;
		
		int authSize = loadUserAuthorities(String.format("%s/"+mode+"/"+setting+"/"+nTopics+"/l_OptUserAuthorityDistributions.csv", path),nTopics);
		System.out.println("loaded authorities of "+authSize+" users");
		
		int hubSize = loadUserHubs(String.format("%s/"+mode+"/"+setting+"/"+nTopics+"/l_OptUserHubDistributions.csv", path),nTopics);
		System.out.println("loaded hubs of "+hubSize+" users");
		
		System.out.println("loading training and testing data");
		String relationshipFile = String.format("%s/relationships.csv", path);
		String nonRelationshipFile = String.format("%s/nonrelationships.csv", path);
		loadTrainTestData(relationshipFile, nonRelationshipFile);
		
		System.out.println("compute prediction scores");
		computePredictionScores();
		output_Prediction();
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
	
	public void loadTrainTestData(String relationshipFile, String nonRelationshipFile) {
		Scanner sc = null;
		BufferedReader br = null;
		String line = null;
		int nTrain = 0;
		int nTest = 0;
		int iTrain = 0;
		int iTest = 0;
		
		try {
			File linkFile = new File(relationshipFile);
			File nonLinkFile = new File(nonRelationshipFile);
			
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
					if (flag == 1){
						nTrain++;
					} else {
						nTest++;
					}
				}
			}
			br.close();
			
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
					if (flag == 1){
						nTrain++;
					} else {
						nTest++;
					}
				}
			}
			br.close();
			
			trainSrcUsers = new String[nTrain];
			trainDesUsers = new String[nTrain];
			testSrcUsers = new String[nTrain];
			testDesUsers = new String[nTrain];
			trainLabels = new HashMap<String, Integer>();
			testLabels = new HashMap<String, Integer>();
			
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
					if (flag == 1){
						trainSrcUsers[iTrain] = uid; 
						trainDesUsers[iTrain] = vid;
						trainLabels.put(uid+ ' '+vid , 1);
						iTrain++;
					} else {
						testSrcUsers[iTest] = uid; 
						testDesUsers[iTest] = vid;
						testLabels.put(uid+ ' '+vid , 1);
						iTest++;
					}
				}
			}
			br.close();
			
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
					if (flag == 1){
						trainSrcUsers[iTrain] = uid; 
						trainDesUsers[iTrain] = vid;
						trainLabels.put(uid+ ' '+vid , 0);
						iTrain++;
					} else {
						testSrcUsers[iTest] = uid; 
						testDesUsers[iTest] = vid;
						testLabels.put(uid+ ' '+vid , 0);
						iTest++;
					}
				}
			}
			br.close();
			System.out.println("Loaded "+nTrain+" training user pairs");
			System.out.println("Loaded "+nTest+" training user pairs");
			
		} catch (Exception e) {
			System.out.println("Error in reading user file!");
			e.printStackTrace();
			System.exit(0);
		}
	}
		
	public void computePredictionScores(){
		String uid = "";
		String vid ="";
		float[] Hu;
		float[] Av;
		for (int i=0; i< testLabels.size(); i++){
			uid = testSrcUsers[i];
			vid = testDesUsers[i];
			Hu = userHubs.get(uid);
			Av = userAuthorities.get(vid);
			float HuAv = 0;
			for (int z=0; z<Hu.length; z++){
				HuAv += Hu[z] * Av[z];
			}
			predictionScores.put(uid+" "+vid, HuAv);
		}
	}
	
	public void output_Prediction(){
		try {
			File f = new File(path + "/" + "pred_scores.csv");
			FileWriter fo = new FileWriter(f);
			
			Iterator it = predictionScores.entrySet().iterator();
		    while (it.hasNext()) {
		        Map.Entry pair = (Map.Entry)it.next();
		        String link = (String) pair.getKey();
		        int label = testLabels.get(link);
		        fo.write(link+ ","+ (String)pair.getValue() +","+label+ "\n");
		        it.remove(); // avoids a ConcurrentModificationException
		    }		    
			fo.close();
		} catch (Exception e) {
			System.out.println("Error in writing out post topic top words to file!");
			e.printStackTrace();
			System.exit(0);
		}
	}
	
}
