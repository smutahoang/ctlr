package larc.ctlr.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Scanner;

import org.apache.commons.io.FilenameUtils;

public class Dataset {
	public String path;
	public int nUsers;
	public User[] users;
	// public int nWords; // number of words in vocabulary
	public String[] vocabulary;

	public HashMap<String, Integer> userId2Index;
	public HashMap<Integer, String> userIndex2Id;

	/***
	 * read dataset from folder "path"
	 * 
	 * @param path
	 */
	public Dataset(String path, int nTopics) {
		this.path = path;
		loadUsers(path + "users.csv", nTopics);
		loadPosts(path + "posts.csv");
		loadVocabulary(path + "vocabulary.csv");
		loadRelationship(path + "relationships.csv");
		loadNonRelationship(path + "nonrelationships.csv");
		// loadFollowers(path+"followers/");
		// loadFollowings(path+"followings/");
		// loadNonFollowers(path+"nonfollowers/");
		// loadNonFollowings(path+"nonfollowings/");
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
			int u = 0;
			while ((line = br.readLine()) != null) {
				sc = new Scanner(line.toString());
				sc.useDelimiter(",");
				while (sc.hasNext()) {
					String userId = sc.next();
					String username = sc.next();
					users[u] = new User();
					users[u].userId = userId.trim();
					users[u].username = username;
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

	public void loadPosts(String filename) {
		Scanner sc = null;
		BufferedReader br = null;
		String line = null;

		HashMap<Integer, Integer> postCounts = new HashMap<Integer, Integer>();

		try {
			File file = new File(filename);

			// Get total number of users' posts

			br = new BufferedReader(new FileReader(file.getAbsolutePath()));
			while ((line = br.readLine()) != null) {
				sc = new Scanner(line.toString());
				sc.useDelimiter(",");
				while (sc.hasNext()) {
					String postId = sc.next();
					String userId = sc.next();
					sc.next().trim();// ignore words
					sc.nextInt();// ignore batch
					int user_index = userId2Index.get(userId);
					// update post count
					if (postCounts.containsKey(user_index)) {
						int count = postCounts.get(user_index) + 1;
						postCounts.put(user_index, count);
					} else {
						postCounts.put(user_index, 1);
					}
				}
			}

			br.close();

			// initalize the users'post arrays
			for (int u = 0; u < nUsers; u++) {
				if (postCounts.containsKey(u)) {
					users[u].nPosts = postCounts.get(u);
					users[u].posts = new Post[postCounts.get(u)];
					users[u].postBatches = new int[postCounts.get(u)];
				}
			}

			// Read and load user into users' follower and following array
			br = new BufferedReader(new FileReader(file.getAbsolutePath()));
			while ((line = br.readLine()) != null) {
				sc = new Scanner(line.toString());
				sc.useDelimiter(",");
				while (sc.hasNext()) {
					String postId = sc.next();// ignore postId
					String userId = sc.next();
					String words = sc.next().trim();
					int batch = sc.nextInt();
					int user_index = userId2Index.get(userId);

					// System.out.println(postCounts.get(user_index));
					users[user_index].postBatches[users[user_index].postBatches.length
							- postCounts.get(user_index)] = batch;
					String[] tokens = words.toString().split(" ");
					users[user_index].posts[users[user_index].nPosts - postCounts.get(user_index)] = new Post();
					users[user_index].posts[users[user_index].nPosts - postCounts.get(user_index)].postId = postId;
					users[user_index].posts[users[user_index].nPosts
							- postCounts.get(user_index)].nWords = tokens.length;
					users[user_index].posts[users[user_index].nPosts
							- postCounts.get(user_index)].words = new int[tokens.length];
					users[user_index].posts[users[user_index].nPosts
											- postCounts.get(user_index)].wordTopics = new int[tokens.length];
					for (int i = 0; i < tokens.length; i++) {
						users[user_index].posts[users[user_index].nPosts
								- postCounts.get(user_index)].words[i] = Integer.parseInt(tokens[i]);
					}

					int updatePostCount = postCounts.get(user_index) - 1;
					postCounts.put(user_index, updatePostCount);
				}
			}
			br.close();
		} catch (Exception e) {
			System.out.println("Error in reading user file!");
			e.printStackTrace();
			System.exit(0);
		}
	}

	public void loadVocabulary(String filename) {
		Scanner sc = null;
		BufferedReader br = null;
		String line = null;

		try {
			br = new BufferedReader(new FileReader(filename));
			int nVocabs = 0;
			while (br.readLine() != null) {
				nVocabs++;
			}
			br.close();
			vocabulary = new String[nVocabs];

			br = new BufferedReader(new FileReader(filename));
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
			// System.out.println("Number of Vocabulary loaded:" + nVocabs);
		} catch (Exception e) {
			System.out.println("Error in reading vocabulary file!");
			e.printStackTrace();
			System.exit(0);
		}
	}

	public void loadRelationship(String filename) {
		Scanner sc = null;
		BufferedReader br = null;
		String line = null;

		HashMap<Integer, Integer> followerCounts = new HashMap<Integer, Integer>();
		HashMap<Integer, Integer> followingCounts = new HashMap<Integer, Integer>();

		try {
			File file = new File(filename);

			// Get total number of users' followers and following
			br = new BufferedReader(new FileReader(file.getAbsolutePath()));
			while ((line = br.readLine()) != null) {
				sc = new Scanner(line.toString());
				sc.useDelimiter(",");
				while (sc.hasNext()) {
					String src_user = sc.next();
					String des_user = sc.next();
					sc.nextInt();// ignore batch
					int src_user_index = userId2Index.get(src_user);
					int des_user_index = userId2Index.get(des_user);
					// update follower count
					if (followerCounts.containsKey(des_user_index)) {
						int count = followerCounts.get(des_user_index) + 1;
						followerCounts.put(des_user_index, count);
					} else {
						int count = 1;
						followerCounts.put(des_user_index, count);
					}
					// update following count
					if (followingCounts.containsKey(src_user_index)) {
						int count = followingCounts.get(src_user_index) + 1;
						followingCounts.put(src_user_index, count);
					} else {
						int count = 1;
						followingCounts.put(src_user_index, count);
					}
				}
			}
			br.close();

			// initalize the users' follower and following arrays
			for (int u = 0; u < nUsers; u++) {
				if (followerCounts.containsKey(u)) {
					users[u].followers = new int[followerCounts.get(u)];
				}
				if (followingCounts.containsKey(u)) {
					users[u].followings = new int[followingCounts.get(u)];
					users[u].followingBatches = new int[followingCounts.get(u)];
				}
			}

			// Read and load user into users' follower and following array
			br = new BufferedReader(new FileReader(file.getAbsolutePath()));
			while ((line = br.readLine()) != null) {
				sc = new Scanner(line.toString());
				sc.useDelimiter(",");
				while (sc.hasNext()) {
					String src_user = sc.next();
					String des_user = sc.next();
					int batch = sc.nextInt();
					int src_user_index = userId2Index.get(src_user);
					int des_user_index = userId2Index.get(des_user);
					users[des_user_index].followers[users[des_user_index].followers.length
							- followerCounts.get(des_user_index)] = src_user_index;
					users[src_user_index].followings[users[src_user_index].followings.length
							- followingCounts.get(src_user_index)] = des_user_index;
					users[src_user_index].followingBatches[users[src_user_index].followingBatches.length
							- followingCounts.get(src_user_index)] = batch;
					int updateFollowerCount = followerCounts.get(des_user_index) - 1;
					int updateFollowingCount = followingCounts.get(src_user_index) - 1;
					followerCounts.put(des_user_index, updateFollowerCount);
					followingCounts.put(src_user_index, updateFollowingCount);
				}
			}
			br.close();
		} catch (Exception e) {
			System.out.println("Error in reading user file!");
			e.printStackTrace();
			System.exit(0);
		}
	}

	public void loadNonRelationship(String filename) {
		Scanner sc = null;
		BufferedReader br = null;
		String line = null;

		HashMap<Integer, Integer> followerNonCounts = new HashMap<Integer, Integer>();
		HashMap<Integer, Integer> followingNonCounts = new HashMap<Integer, Integer>();

		try {
			File file = new File(filename);

			// Get total number of users' followers and following
			br = new BufferedReader(new FileReader(file.getAbsolutePath()));
			while ((line = br.readLine()) != null) {
				sc = new Scanner(line.toString());
				sc.useDelimiter(",");
				while (sc.hasNext()) {
					String src_user = sc.next();
					String des_user = sc.next();
					sc.nextInt(); // ignore batch
					int src_user_index = userId2Index.get(src_user);
					int des_user_index = userId2Index.get(des_user);
					// update follower count
					if (followerNonCounts.containsKey(des_user_index)) {
						int count = followerNonCounts.get(des_user_index) + 1;
						followerNonCounts.put(des_user_index, count);
					} else {
						int count = 1;
						followerNonCounts.put(des_user_index, count);
					}
					// update following count
					if (followingNonCounts.containsKey(src_user_index)) {
						int count = followingNonCounts.get(src_user_index) + 1;
						followingNonCounts.put(src_user_index, count);
					} else {
						int count = 1;
						followingNonCounts.put(src_user_index, count);
					}
				}
			}

			br.close();

			// initalize the users' follower and following arrays
			for (int u = 0; u < nUsers; u++) {
				if (followerNonCounts.containsKey(u)) {
					users[u].nonFollowers = new int[followerNonCounts.get(u)];
				}
				if (followingNonCounts.containsKey(u)) {
					users[u].nonFollowings = new int[followingNonCounts.get(u)];
					users[u].nonFollowingBatches = new int[followingNonCounts.get(u)];
				}
			}

			// Read and load user into users' follower and following array
			br = new BufferedReader(new FileReader(file.getAbsolutePath()));
			while ((line = br.readLine()) != null) {
				sc = new Scanner(line.toString());
				sc.useDelimiter(",");
				while (sc.hasNext()) {
					String src_user = sc.next();
					String des_user = sc.next();
					int batch = sc.nextInt();
					int src_user_index = userId2Index.get(src_user);
					int des_user_index = userId2Index.get(des_user);
					users[des_user_index].nonFollowers[users[des_user_index].nonFollowers.length
							- followerNonCounts.get(des_user_index)] = src_user_index;
					users[src_user_index].nonFollowings[users[src_user_index].nonFollowings.length
							- followingNonCounts.get(src_user_index)] = des_user_index;
					users[src_user_index].nonFollowingBatches[users[src_user_index].nonFollowingBatches.length
							- followingNonCounts.get(src_user_index)] = batch;
					int updateNonFollowerCount = followerNonCounts.get(des_user_index) - 1;
					int updateNonFollowingCount = followingNonCounts.get(src_user_index) - 1;
					followerNonCounts.put(des_user_index, updateNonFollowerCount);
					followingNonCounts.put(src_user_index, updateNonFollowingCount);
				}
			}
			br.close();
		} catch (Exception e) {
			System.out.println("Error in reading user file!");
			e.printStackTrace();
			System.exit(0);
		}
	}

	public void loadFollowers(String folder) {
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
						// System.out.println("Followers loaded:" +
						// userId2Index.get(followerId));
					}
				}
				// System.out.println("Number of Followers loaded:" +
				// nFollower);
			}
		} catch (Exception e) {
			System.out.println("Error in reading follower file!");
			e.printStackTrace();
			System.exit(0);
		}
	}

	public void loadFollowings(String folder) {
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
				// Declare the number of followings batches from user
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

						// Set following's batch
						users[u].followingBatches[j] = batch;
					}
				}
				// System.out.println("Number of Following loaded:" +
				// nFollowing);
			}
		} catch (Exception e) {
			System.out.println("Error in reading post file!");
			e.printStackTrace();
			System.exit(0);
		}
	}

	public void loadNonFollowers(String folder) {
		Scanner sc = null;
		BufferedReader br = null;
		String line = null;
		File nonFollowersFolder = new File(folder);

		try {
			// Read the non followers from each user file
			for (File nonFollowerFile : nonFollowersFolder.listFiles()) {
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
						// System.out.println("Non Follower loaded:" +
						// userId2Index.get(nonFollowerId));
					}
				}
				// System.out.println("Number of Non Follower loaded:" +
				// nNonFollower);
			}
		} catch (Exception e) {
			System.out.println("Error in reading non follower file!");
			e.printStackTrace();
			System.exit(0);
		}
	}

	public void loadNonFollowings(String folder) {
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
						users[u].nonFollowingBatches[j] = batch;
					}
				}
				// System.out.println("Number of Non Following loaded:" +
				// nNonFollowing);
			}
		} catch (Exception e) {
			System.out.println("Error in reading non followee file!");
			e.printStackTrace();
			System.exit(0);
		}
	}

}
