/***
 * Utilities for synthetic data generation
 */
package larc.ctlr.data;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import org.apache.commons.math3.distribution.NormalDistribution;

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

	private int minNPosts = 20;
	private int maxNPosts = 100;

	private int minNWords = 10;
	private int maxNWords = 20;

	private StatTool statTool = new StatTool();

	public double alpha = 1;
	public double beta = 1;
	public double gamma = 2;
	public double sigma = 0.1;
	public double delta = 0.1;

	private double[][] genTopics(int nTopics, int nWords) {
		Random rand = new Random(System.currentTimeMillis());
		System.out.println("nTopics = " + nTopics);
		double[][] topics = new double[nTopics][];
		for (int z = 0; z < nTopics; z++) {
			topics[z] = statTool.sampleDirichletSkew(beta, nWords, topicSkewness, mass, rand);
		}
		return topics;
	}

	private double[][] genUserInterest(int nUsers, int nTopics) {
		Random rand = new Random(System.currentTimeMillis());
		double[][] userInterest = new double[nUsers][];
		for (int u = 0; u < nUsers; u++) {
			userInterest[u] = statTool.sampleDirichletSkew(alpha, nTopics, userSkewness, mass, rand);
		}
		return userInterest;
	}

	private int[] genPost(double[] interest, double[][] topics) {
		Random rand = new Random(System.currentTimeMillis());
		// topic
		int z = statTool.sampleMult(interest, false, rand);
		// #words in the post
		int nTweetWords = rand.nextInt(maxNWords - minNWords) + minNWords;
		int[] post = new int[nTweetWords];
		// words
		for (int j = 0; j < nTweetWords; j++) {
			post[j] = statTool.sampleMult(topics[z], false, rand);
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
		System.out.println("p = " + p);
		Random rand = new Random(System.currentTimeMillis());
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
			BufferedWriter bw = new BufferedWriter(new FileWriter(String.format("%s/users.csv", outputPath)));
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
			BufferedWriter bw = new BufferedWriter(new FileWriter(String.format("%s/vocabulary.csv", outputPath)));
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
			File file = new File(String.format("%s/posts", outputpath));
			if (!file.exists()) {
				file.mkdir();
			}
			
			int nPosts = 0;
			Random rand = new Random(System.currentTimeMillis());
			for (int u = 0; u < nUsers; u++) {
				BufferedWriter bw = new BufferedWriter(new FileWriter(String.format("%s/posts/%d.csv", outputpath, u)));
				int n = rand.nextInt(maxNPosts - minNPosts) + minNPosts;
				for (int i = 0; i < n; i++) {
					int[] post = genPost(userInterest[u], topics);
					bw.write(nPosts + ",");
					for (int j = 0; j < post.length; j++) {
						bw.write(" "+post[j]);
					}
					bw.newLine();
					nPosts++;
				}
				bw.close();
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private void genAndsaveNetwork(String outputpath, int nUsers, int nTopics, double[][] userAuthorities,
			double[][] userHubs) {
		try {
			HashMap<Integer, HashSet<Integer>> followings = genNetwork(nUsers, nTopics, userAuthorities, userHubs);
			File file = new File(String.format("%s/followings", outputpath));
			if (!file.exists()) {
				file.mkdir();
			}

			Random rand = new Random(System.currentTimeMillis());
			for (int u = 0; u < nUsers; u++) {
				if (!followings.containsKey(u)) {
					continue;
				}
				BufferedWriter bw = new BufferedWriter(
						new FileWriter(String.format("%s/followings/%d.csv", outputpath, u)));
				Iterator<Integer> vIter = followings.get(u).iterator();
				while (vIter.hasNext()) {
					int v = vIter.next();
					int batch = rand.nextInt(10);
					bw.write(String.format("%d\n", v));
				}
				bw.close();
			}

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
	}

	public static void main(String[] args) {
		Synthetic generator = new Synthetic();
		generator.genData(100, 10, 1000, "E:/code/java/ctlr/output");
	}
}
