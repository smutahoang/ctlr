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
	public Dataset(String path) {
		this.path = path;
		System.out.println("loading user list");
		loadUsers(String.format("%s/users.csv", path));
		System.out.println("loading posts");
		loadPosts(String.format("%s/posts.csv", path));
		System.out.println("loading vocabulary");
		loadVocabulary(String.format("%s/vocabulary.csv", path));
		System.out.println("loading links");
		loadRelationship(String.format("%s/relationships.csv", path));
		System.out.println("loading non-links");
		loadNonRelationship(String.format("%s/nonrelationships.csv", path));
		// loadFollowers(path+"followers/");
		// loadFollowings(path+"followings/");
		// loadNonFollowers(path+"nonfollowers/");
		// loadNonFollowings(path+"nonfollowings/");
	}

	public void loadUsers(String filename) {
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

		BufferedReader br = null;
		String line = null;

		try {
			File file = new File(filename);

			// Get total number of users' posts
			for (int u = 0; u < nUsers; u++) {
				users[u].nPosts = 0;
			}

			br = new BufferedReader(new FileReader(file.getAbsolutePath()));
			while ((line = br.readLine()) != null) {
				String userId = line.split(",")[1];
				int u = userId2Index.get(userId);
				users[u].nPosts++;
			}

			br.close();

			// initalize the users'post arrays
			for (int u = 0; u < nUsers; u++) {
				users[u].posts = new Post[users[u].nPosts];
				users[u].postBatches = new int[users[u].nPosts];
				users[u].nPosts = 0;
			}

			// Read and load user into users' follower and following array
			br = new BufferedReader(new FileReader(file.getAbsolutePath()));
			while ((line = br.readLine()) != null) {
				String[] tokens = line.split(",");
				String postId = tokens[0];
				String userId = tokens[1];
				int batch = Integer.parseInt(tokens[3]);
				int u = userId2Index.get(userId);
				users[u].postBatches[users[u].nPosts] = batch;
				users[u].posts[users[u].nPosts] = new Post();
				users[u].posts[users[u].nPosts].postId = postId;
				tokens = tokens[2].trim().split(" ");
				users[u].posts[users[u].nPosts].nWords = tokens.length;
				users[u].posts[users[u].nPosts].words = new int[tokens.length];
				for (int i = 0; i < tokens.length; i++) {
					//System.out.println(postId+","+tokens[i]);
					users[u].posts[users[u].nPosts].words[i] = Integer.parseInt(tokens[i]);
					
				}
				users[u].nPosts++;

			}
			br.close();
		} catch (Exception e) {
			System.out.println("Error in reading user file!");
			e.printStackTrace();
			System.exit(0);
		}
	}

	public void loadVocabulary(String filename) {
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
				String[] tokens = line.split(",");
				int index = Integer.parseInt(tokens[0]);
				String vocab = tokens[1];
				vocabulary[index] = vocab;
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
		BufferedReader br = null;
		String line = null;

		for (int u = 0; u < nUsers; u++) {
			users[u].nFollowers = 0;
			users[u].nFollowings = 0;
		}

		try {
			File file = new File(filename);

			// Get total number of users' followers and following
			br = new BufferedReader(new FileReader(file.getAbsolutePath()));
			while ((line = br.readLine()) != null) {
				String[] tokens = line.split(",");
				String src_user = tokens[0];
				String des_user = tokens[1];
				int src_user_index = userId2Index.get(src_user);
				int des_user_index = userId2Index.get(des_user);
				// update follower count
				users[des_user_index].nFollowers++;
				// update following count
				users[src_user_index].nFollowings++;

			}
			br.close();

			// initalize the users' follower and following arrays
			for (int u = 0; u < nUsers; u++) {
				if (users[u].nFollowers > 0) {
					users[u].followers = new int[users[u].nFollowers];
					users[u].followerBatches = new int[users[u].nFollowers];
					users[u].nFollowers = 0;
				}
				if (users[u].nFollowings > 0) {
					users[u].followings = new int[users[u].nFollowings];
					users[u].followingBatches = new int[users[u].nFollowings];
					users[u].nFollowings = 0;
				}
			}

			// Read and load user into users' follower and following array
			br = new BufferedReader(new FileReader(file.getAbsolutePath()));
			while ((line = br.readLine()) != null) {
				String[] tokens = line.split(",");
				String src_user = tokens[0];
				String des_user = tokens[1];
				int batch = Integer.parseInt(tokens[2]);

				int src_user_index = userId2Index.get(src_user);
				int des_user_index = userId2Index.get(des_user);
				users[des_user_index].followers[users[des_user_index].nFollowers] = src_user_index;
				users[des_user_index].followerBatches[users[des_user_index].nFollowers] = batch;
				users[des_user_index].nFollowers++;

				users[src_user_index].followings[users[src_user_index].nFollowings] = des_user_index;
				users[src_user_index].followingBatches[users[src_user_index].nFollowings] = batch;
				users[src_user_index].nFollowings++;

			}
			br.close();
		} catch (Exception e) {
			System.out.println("Error in reading user file!");
			e.printStackTrace();
			System.exit(0);
		}
	}

	public void loadNonRelationship(String filename) {

		BufferedReader br = null;
		String line = null;

		for (int u = 0; u < nUsers; u++) {
			users[u].nNonFollowers = 0;
			users[u].nNonFollowings = 0;
		}

		try {
			File file = new File(filename);

			// Get total number of users' followers and following
			br = new BufferedReader(new FileReader(file.getAbsolutePath()));
			while ((line = br.readLine()) != null) {
				String[] tokens = line.split(",");
				String src_user = tokens[0];
				String des_user = tokens[1];

				int src_user_index = userId2Index.get(src_user);
				int des_user_index = userId2Index.get(des_user);
				// update follower count
				users[des_user_index].nNonFollowers++;
				// update following count
				users[src_user_index].nNonFollowings++;

			}

			br.close();

			// initalize the users' follower and following arrays
			for (int u = 0; u < nUsers; u++) {
				if (users[u].nNonFollowers > 0) {
					users[u].nonFollowers = new int[users[u].nNonFollowers];
					users[u].nNonFollowers = 0;
				}
				if (users[u].nNonFollowings > 0) {
					users[u].nonFollowings = new int[users[u].nNonFollowings];
					users[u].nonFollowingBatches = new int[users[u].nNonFollowings];
					users[u].nNonFollowings = 0;
				}
			}

			// Read and load user into users' follower and following array
			br = new BufferedReader(new FileReader(file.getAbsolutePath()));
			while ((line = br.readLine()) != null) {
				String[] tokens = line.split(",");
				String src_user = tokens[0];
				String des_user = tokens[1];
				int batch = Integer.parseInt(tokens[2]);
				int src_user_index = userId2Index.get(src_user);
				int des_user_index = userId2Index.get(des_user);
				users[des_user_index].nonFollowers[users[des_user_index].nNonFollowers] = src_user_index;
				users[des_user_index].nNonFollowers++;

				users[src_user_index].nonFollowings[users[src_user_index].nNonFollowings] = des_user_index;
				users[src_user_index].nonFollowingBatches[users[src_user_index].nNonFollowings] = batch;
				users[src_user_index].nNonFollowings++;

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
