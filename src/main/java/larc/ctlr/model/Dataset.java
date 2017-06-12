package larc.ctlr.model;

import java.io.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;

import org.apache.commons.io.FilenameUtils;

public class Dataset {
	public String path;
	public int nUsers;
	public User[] users;
	public int nWords; // number of words in vocabulary
	public String[] vocabulary;
	
	public HashMap<String, Integer> userId2Index;
	public HashMap<Integer, String> userIndex2Id;

	/***
	 * read dataset from folder "path"
	 * 
	 * @param path
	 */
	public Dataset(String path, int nTopics) {
		loadUsers(path+"users.csv", nTopics);
		loadPosts(path+"posts/");
		loadVocabulary(path+"vocabulary.csv");
		loadFollowers(path+"followers/");
		loadFollowings(path+"followings/");
		loadNonFollowers(path+"nonfollowers/");
		loadNonFollowings(path+"nonfollowings/");
	}
	
	public void loadUsers(String filename, int nTopics) {
		Scanner sc = null;
		BufferedReader br = null;
		String line = null;
		userId2Index = new HashMap<String, Integer>();
		userIndex2Id = new HashMap<Integer, String>();

		try {
			File userFile = new File(filename);
			
			// Get total number of users and initiate the Users array
			br = new BufferedReader(new FileReader(userFile.getAbsolutePath()));
			while (br.readLine() != null) {
				nUsers++;
			}
			br.close();
			
			// Declare the number of users in users array
			users = new User[nUsers];
			
			// Read and load user into Users array
			br = new BufferedReader(new FileReader(userFile.getAbsolutePath()));
			int u =0;
			while ((line = br.readLine()) != null) {
				sc = new Scanner(line.toString());
				sc.useDelimiter(",");
				while (sc.hasNext()) {
					String userId = sc.next();
					String username = sc.next();
					users[u] = new User();
					users[u].userId = userId.trim();
					users[u].userIndex = u;
					userId2Index.put(userId, u);
					userIndex2Id.put(u, userId);
					users[u].topicalInterests = new double[nTopics];
					users[u].authorities = new double[nTopics];
					users[u].hubs = new double[nTopics];
					u++;
				}
			}
			br.close();
		} catch (Exception e) {
			System.out.println("Error in reading user file!");
			e.printStackTrace();
			System.exit(0);
		}
	}

	public void loadPosts(String folder){
		Scanner sc = null;
		BufferedReader br = null;
		String line = null;
		File postFolder = new File(folder);
		
		try {
			// Read the posts from each user file
			for (File postFile : postFolder.listFiles()) {
				// Read the number of posts from user
				int nPost = 0;
				br = new BufferedReader(new FileReader(postFile.getAbsolutePath()));
				while (br.readLine() != null) {
					nPost++;
				}
				br.close();
				
				String userId = FilenameUtils.removeExtension(postFile.getName());
				int u = userId2Index.get(userId);
						
				// Declare the number of posts from user
				users[u].posts = new Post[nPost];
				
				// Declare the number of posts batches from user
				users[u].postBatches = new int[nPost]; 

				// Read each of the post
				br = new BufferedReader(new FileReader(postFile.getAbsolutePath()));
				int j = -1;
				while ((line = br.readLine()) != null) {
					j++;
					users[u].posts[j] = new Post();
					sc = new Scanner(line.toString());
					sc.useDelimiter(",");
					while (sc.hasNext()) {
						String postId = sc.next();
						String words = sc.next().trim();
						int batch = sc.nextInt();
						
						// Set batch for the post j
						users[u].postBatches[j] = batch;
						
						// Read the words in each post
						String[] tokens = words.toString().split(" ");
						users[u].posts[j].nWords = tokens.length;
						users[u].posts[j].words = new int[tokens.length];
						for (int i = 0; i < tokens.length; i++) {
							users[u].posts[j].words[i] = Integer.parseInt(tokens[i]);
						}		
					}
				}
			}
		}catch (Exception e){
			System.out.println("Error in reading post file!");
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public void loadVocabulary(String file){
		Scanner sc = null;
		BufferedReader br = null;
		String line = null;
		
		try {
			br = new BufferedReader(new FileReader(file));
			int nVocabs = 0;
			while (br.readLine() != null) {
				nVocabs++;
			}
			br.close();
			vocabulary = new String[nVocabs];
			
			br = new BufferedReader(new FileReader(file));
			while ((line = br.readLine()) != null) {
				sc = new Scanner(line.toString());
				sc.useDelimiter(",");
				while (sc.hasNext()) {
					int index = sc.nextInt();
					String vocab = sc.next();
					vocabulary[index] = vocab;
				}				
			}
			br.close();
			
		} catch (Exception e){
			System.out.println("Error in reading vocabulary file!");
			e.printStackTrace();
			System.exit(0);
		}		
	}
	
	public void loadFollowers(String folder){
		Scanner sc = null;
		BufferedReader br = null;
		String line = null;
		File followersFolder = new File(folder);
		
		try {
			// Read the followers from each user file
			for (File followerFile : followersFolder.listFiles()) {
				// Read the number of followers from user
				int nFollower = 0;
				br = new BufferedReader(new FileReader(followerFile.getAbsolutePath()));
				while (br.readLine() != null) {
					nFollower++;
				}
				br.close();
				
				String userId = FilenameUtils.removeExtension(followerFile.getName());
				int u = userId2Index.get(userId);
						
				// Declare the number of followers from user
				users[u].followers = new int[nFollower]; 

				// Read each of the followers
				br = new BufferedReader(new FileReader(followerFile.getAbsolutePath()));
				int j = -1;
				while ((line = br.readLine()) != null) {
					j++;
					sc = new Scanner(line.toString());
					sc.useDelimiter(",");
					while (sc.hasNext()) {
						String followerId = sc.next();
						// Set follower's user index
						users[u].followers[j] = userId2Index.get(followerId);		
					}
				}
			}
		}catch (Exception e){
			System.out.println("Error in reading follower file!");
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public void loadFollowings(String folder){
		Scanner sc = null;
		BufferedReader br = null;
		String line = null;
		File followingsFolder = new File(folder);
		
		try {
			// Read the followings from each user file
			for (File followingFile : followingsFolder.listFiles()) {
				// Read the number of followimgs from user
				int nFollowing = 0;
				br = new BufferedReader(new FileReader(followingFile.getAbsolutePath()));
				while (br.readLine() != null) {
					nFollowing++;
				}
				br.close();
				
				String userId = FilenameUtils.removeExtension(followingFile.getName());
				int u = userId2Index.get(userId);
			
				// Declare the number of followings from user
				users[u].followings = new int[nFollowing]; 
				// Declare the number of followees from user
				users[u].followings = new int[nFollowing]; 
				// Declare the number of followings batches from user
				users[u].followingBatches = new int[nFollowing]; 
				// Declare the number of followees batches from user
				users[u].followingBatches = new int[nFollowing]; 

				// Read each of the followings
				br = new BufferedReader(new FileReader(followingFile.getAbsolutePath()));
				int j = -1;
				while ((line = br.readLine()) != null) {
					j++;
					sc = new Scanner(line.toString());
					sc.useDelimiter(",");
					while (sc.hasNext()) {
						String followingId = sc.next();
						int batch = sc.nextInt();
						// Set following's user index
						users[u].followings[j] = userId2Index.get(followingId);
						users[u].followings[j] = userId2Index.get(followingId);

						// Set following's batch
						users[u].followingBatches[j] = batch;
					}
				}
			}
		}catch (Exception e){
			System.out.println("Error in reading post file!");
			e.printStackTrace();
			System.exit(0);
		}
	}

	public void loadNonFollowers(String folder){
		Scanner sc = null;
		BufferedReader br = null;
		String line = null;
		File followersFolder = new File(folder);
		
		try {
			// Read the non followers from each user file
			for (File nonFollowerFile : followersFolder.listFiles()) {
				// Read the number of non followers from user
				int nNonFollower = 0;
				br = new BufferedReader(new FileReader(nonFollowerFile.getAbsolutePath()));
				while (br.readLine() != null) {
					nNonFollower++;
				}
				br.close();
				
				String userId = FilenameUtils.removeExtension(nonFollowerFile.getName());
				int u = userId2Index.get(userId);
						
				// Declare the number of non followers from user
				users[u].nonFollowers = new int[nNonFollower]; 

				// Read each of the non followers
				br = new BufferedReader(new FileReader(nonFollowerFile.getAbsolutePath()));
				int j = -1;
				while ((line = br.readLine()) != null) {
					j++;
					sc = new Scanner(line.toString());
					sc.useDelimiter(",");
					while (sc.hasNext()) {
						String nonFollowerId = sc.next();
						// Set non follower's user index
						users[u].nonFollowers[j] = userId2Index.get(nonFollowerId);		
					}
				}
			}
		}catch (Exception e){
			System.out.println("Error in reading non follower file!");
			e.printStackTrace();
			System.exit(0);
		}
	}

	public void loadNonFollowings(String folder){
		Scanner sc = null;
		BufferedReader br = null;
		String line = null;
		File followingsFolder = new File(folder);
		
		try {
			// Read the non followings from each user file
			for (File nonFollowingFile : followingsFolder.listFiles()) {
				// Read the number of non followings from user
				int nNonFollowing = 0;
				br = new BufferedReader(new FileReader(nonFollowingFile.getAbsolutePath()));
				while (br.readLine() != null) {
					nNonFollowing++;
				}
				br.close();
				
				String userId = FilenameUtils.removeExtension(nonFollowingFile.getName());
				int u = userId2Index.get(userId);
			
				// Declare the number of non followeing from user
				users[u].nonFollowings = new int[nNonFollowing]; 
			
				// Declare the number of non followings batches from user
				users[u].nonFollowingBatches = new int[nNonFollowing]; 
				// Declare the number of non followees from user
				users[u].nonFollowings = new int[nNonFollowing]; 
				
				// Declare the number of non followees batches from user
				users[u].nonFollowingBatches = new int[nNonFollowing]; 

				// Read each of the non followings
				br = new BufferedReader(new FileReader(nonFollowingFile.getAbsolutePath()));
				int j = -1;
				while ((line = br.readLine()) != null) {
					j++;
					sc = new Scanner(line.toString());
					sc.useDelimiter(",");
					while (sc.hasNext()) {
						String nonFollowingId = sc.next();
						int batch = sc.nextInt();
						// Set following's user index
						users[u].nonFollowings[j] = userId2Index.get(nonFollowingId);
						// Set following's batch
						// Set followee's user index
						users[u].nonFollowings[j] = userId2Index.get(nonFollowingId);
						// Set followee's batch
						users[u].nonFollowingBatches[j] = batch;
					}
				}
			}
		}catch (Exception e){
			System.out.println("Error in reading non followee file!");
			e.printStackTrace();
			System.exit(0);
		}
	}
	
}
