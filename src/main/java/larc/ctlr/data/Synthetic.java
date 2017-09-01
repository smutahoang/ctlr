/***
 * Utilities for synthetic data generation
 */
package larc.ctlr.data;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

import org.apache.commons.math3.distribution.NormalDistribution;

import larc.ctlr.model.Configure.ModelMode;
import larc.ctlr.tool.MathTool;
import larc.ctlr.tool.StatTool;

public class Synthetic {

	private double mass = 0.9;
	private double userSkewness = 0.1;// together with mass, this means, for
										// each user, 90% of her posts are about
										// 10% of topics
	private double topicSkewness = 0.01;// similarly, each topic focuses on 1%
										// of words whose probabilities summing
										// up to 90%

	private int minNPosts = 100;
	private int maxNPosts = 200;

	private int minNWords = 10;
	private int maxNWords = 20;

	private StatTool statTool = new StatTool();

	public double alpha = 1;
	public double beta = 1;
	public double gamma = 2;
	public double sigma = 0.1;
	public double delta = 0.1;

	private Random rand = new Random();

	private int[] nTopicCounts;

	private ModelMode mode;

	public Synthetic(ModelMode _mode) {
		mode = _mode;
	}

	private double[][] genTopics(int nTopics, int nWords) {
		System.out.println("nTopics = " + nTopics);
		double[][] topics = new double[nTopics][];
		for (int z = 0; z < nTopics; z++) {
			topics[z] = statTool.sampleDirichletSkew(beta, nWords, topicSkewness, mass, rand);
		}
		return topics;
	}

	private double[][] genUserInterest(int nUsers, int nTopics) {
		double[][] userInterest = new double[nUsers][];
		for (int u = 0; u < nUsers; u++) {
			userInterest[u] = statTool.sampleDirichletSkew(alpha, nTopics, userSkewness, mass, rand);
		}
		return userInterest;
	}

	private int[] genPost(int u, double[] interest, double[][] topics) {
		// #words in the post
		int nTweetWords = rand.nextInt(maxNWords - minNWords) + minNWords;
		int[] post = new int[nTweetWords];
		if (mode == ModelMode.TWITTER_LDA) {
			// topic
			int z = statTool.sampleMult(interest, false, rand);
			nTopicCounts[z]++;
			// words
			for (int j = 0; j < nTweetWords; j++) {
				post[j] = statTool.sampleMult(topics[z], false, rand);
			}
		} else {
			for (int j = 0; j < nTweetWords; j++) {
				// topic
				int z = statTool.sampleMult(interest, false, rand);
				// word
				post[j] = statTool.sampleMult(topics[z], false, rand);
			}
		}
		return post;
	}

	private double[][] genUserAuthority(int nUsers, int nTopics, double[][] userInterest) {
		double[][] authorities = new double[nUsers][nTopics];
		for (int u = 0; u < nUsers; u++) {
			for (int z = 0; z < nTopics; z++) {
				NormalDistribution normalDistribution = new NormalDistribution(userInterest[u][z], sigma);
				double x = normalDistribution.sample();
				authorities[u][z] = Math.exp(x);
			}
		}
		return authorities;
	}

	private double[][] genUserHub(int nUsers, int nTopics, double[][] userInterest) {
		double[][] hubs = new double[nUsers][nTopics];
		for (int u = 0; u < nUsers; u++) {
			for (int z = 0; z < nTopics; z++) {
				NormalDistribution normalDistribution = new NormalDistribution(userInterest[u][z], delta);
				double x = normalDistribution.sample();
				hubs[u][z] = Math.exp(x);
			}
		}
		return hubs;
	}

	private int genLink(int nTopics, double[] userAuthority, double[] userHub) {
		double p = MathTool.normalizationFunction(MathTool.dotProduct(nTopics, userAuthority, userHub));
		// System.out.println("p = " + p);
		if (rand.nextDouble() < p) {
			return 1;
		}
		return 0;
	}

	private HashMap<Integer, HashSet<Integer>> genNetwork(int nUsers, int nTopics, double[][] userAuthorities,
			double[][] userHubs) {
		HashMap<Integer, HashSet<Integer>> followings = new HashMap<Integer, HashSet<Integer>>();
		for (int u = 0; u < nUsers; u++) {
			HashSet<Integer> uFollowings = new HashSet<Integer>();
			for (int v = 0; v < nUsers; v++) {
				if (u == v)
					continue;
				int link = genLink(nTopics, userAuthorities[v], userHubs[u]);
				if (link == 1) {
					uFollowings.add(v);
				}
			}
			followings.put(u, uFollowings);
		}

		return followings;
	}

	private void saveUsers(int nUsers, String outputPath) {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(String.format("%s/syn_users.csv", outputPath)));
			for (int u = 0; u < nUsers; u++) {
				bw.write(String.format("%d,user_%d\n", u, u));
			}
			bw.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private void saveWords(int nWords, String outputPath) {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(String.format("%s/syn_vocabulary.csv", outputPath)));
			for (int w = 0; w < nWords; w++) {
				bw.write(String.format("%d,word_%d\n", w, w));
			}
			bw.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private void genAndsaveTweet(String outputpath, int nUsers, int nTopics, double[][] userInterest,
			double[][] topics) {
		try {
			// File file = new File(String.format("%s/posts", outputpath));
			// if (!file.exists()) {
			// file.mkdir();
			// }

			nTopicCounts = new int[nTopics];

			int nPosts = 0;
			BufferedWriter bw = new BufferedWriter(new FileWriter(String.format("%s/syn_posts.csv", outputpath)));
			BufferedWriter bw_empirical = new BufferedWriter(
					new FileWriter(String.format("%s/syn_userEmpiricalTopicDistribution.csv", outputpath)));
			for (int u = 0; u < nUsers; u++) {
				int n = rand.nextInt(maxNPosts - minNPosts) + minNPosts;

				for (int z = 0; z < nTopics; z++) {
					nTopicCounts[z] = 0;
				}

				for (int i = 0; i < n; i++) {
					int[] post = genPost(u, userInterest[u], topics);
					bw.write(nPosts + "," + u + ",");
					for (int j = 0; j < post.length; j++) {
						bw.write(" " + post[j]);
					}
					// batch
					bw.write(",1");
					bw.newLine();
					nPosts++;
				}

				bw_empirical.write(String.format("%f", ((double) nTopicCounts[0]) / n));
				for (int z = 1; z < nTopics; z++) {
					bw_empirical.write(String.format(",%f", ((double) nTopicCounts[z]) / n));
				}
				bw_empirical.write("\n");
			}
			bw.close();
			bw_empirical.close();

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private void genAndsaveNetwork(String outputpath, int nUsers, int nTopics, double[][] userAuthorities,
			double[][] userHubs) {
		try {
			HashMap<Integer, HashSet<Integer>> followings = genNetwork(nUsers, nTopics, userAuthorities, userHubs);
			// File file = new File(String.format("%s/followings", outputpath));
			// if (!file.exists()) {
			// file.mkdir();
			// }

			BufferedWriter bw = new BufferedWriter(
					new FileWriter(String.format("%s/syn_relationships.csv", outputpath)));
			for (int u = 0; u < nUsers; u++) {
				if (!followings.containsKey(u)) {
					System.out.printf("no-followings u %d\n", u);
					continue;
				}
				Iterator<Integer> vIter = followings.get(u).iterator();
				while (vIter.hasNext()) {
					int v = vIter.next();
					bw.write(u + "," + v);
					// batch
					bw.write(",1");
					bw.newLine();
					System.out.println(u + "," + v);
				}
			}
			bw.close();

			bw = new BufferedWriter(new FileWriter(String.format("%s/syn_nonrelationships.csv", outputpath)));
			for (int u = 0; u < nUsers; u++) {
				for (int v = 0; v < nUsers; v++) {
					if (v == u) {
						continue;
					}
					HashSet<Integer> followees = followings.get(u);
					// sample 10% of the non-followees
					if (followees != null) {
						if (followees.contains(v))
							continue;
					}
					if (rand.nextDouble() > 0.1) {
						continue;
					}
					bw.write(u + "," + v);
					// batch
					bw.write(",1");
					bw.newLine();
				}
			}
			bw.close();

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private void saveGroundTruth(double[][] topics, double[][] userInterest, double[][] userAuthorities,
			double[][] userHubs, String outputPath) {
		try {
			BufferedWriter bw;
			String filename = null;

			// topics
			filename = String.format("%s/topicWordDistributions.csv", outputPath);
			bw = new BufferedWriter(new FileWriter(filename));
			for (int z = 0; z < topics.length; z++) {
				bw.write(String.format("%d", z));
				for (int w = 0; w < topics[z].length; w++) {
					bw.write(String.format(",%f", topics[z][w]));
				}
				bw.write("\n");
			}
			bw.close();

			// user interest
			filename = String.format("%s/userTopicInterestDistribution.csv", outputPath);
			bw = new BufferedWriter(new FileWriter(filename));
			for (int u = 0; u < userInterest.length; u++) {
				bw.write(String.format("%d", u));
				for (int z = 0; z < userInterest[u].length; z++) {
					bw.write(String.format(",%f", userInterest[u][z]));
				}
				bw.write("\n");
			}
			bw.close();

			// user authorities
			filename = String.format("%s/userAuthorityDistribution.csv", outputPath);
			bw = new BufferedWriter(new FileWriter(filename));
			for (int u = 0; u < userAuthorities.length; u++) {
				bw.write(String.format("%d", u));
				for (int z = 0; z < userAuthorities[u].length; z++) {
					bw.write(String.format(",%f", userAuthorities[u][z]));
				}
				bw.write("\n");
			}
			bw.close();

			// user authorities
			filename = String.format("%s/userHubDistributions.csv", outputPath);
			bw = new BufferedWriter(new FileWriter(filename));
			for (int u = 0; u < userHubs.length; u++) {
				bw.write(String.format("%d", u));
				for (int z = 0; z < userHubs[u].length; z++) {
					bw.write(String.format(",%f", userHubs[u][z]));
				}
				bw.write("\n");
			}
			bw.close();

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public void genData(int nUsers, int nTopics, int nWords, String outputPath) {
		double[][] topics = genTopics(nTopics, nWords);
		double[][] userInterest = genUserInterest(nUsers, nTopics);
		double[][] userAuthorities = genUserAuthority(nUsers, nTopics, userInterest);
		double[][] userHubs = genUserHub(nUsers, nTopics, userInterest);
		saveUsers(nUsers, outputPath);
		saveWords(nWords, outputPath);
		genAndsaveTweet(outputPath, nUsers, nTopics, userInterest, topics);
		genAndsaveNetwork(outputPath, nUsers, nTopics, userAuthorities, userHubs);
		saveGroundTruth(topics, userInterest, userAuthorities, userHubs, outputPath);
	}

	public static void main(String[] args) {
		Synthetic generator = new Synthetic(ModelMode.TWITTER_LDA);
		generator.genData(1000, 10, 1000, "E:/code/java/ctlr/output");
	}
}
