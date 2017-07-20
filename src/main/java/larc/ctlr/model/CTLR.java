package larc.ctlr.model;

import java.io.BufferedWriter;
import java.util.Arrays;
import java.util.Random;
import org.apache.commons.math3.*;
import org.apache.commons.math3.distribution.NormalDistribution;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class CTLR {
	public Dataset dataset;
	public int nTopics;
	public int batch;

	// priors

	public double alpha;// prior for users' interest
	public double beta; // prior for topics
	public double sigma;// variance of users' authorities
	public double delta;// variance of users' hubs
	public double gamma; // variance of topic word distribution
	public double epsilon = 0.000001;
	public double lamda = 0.01;

	public Random rand;

	public double lineSearch_alpha;
	public double lineSearch_beta;
	public double lineSearch_lambda;
	public int lineSearch_MaxIterations;

	// Gibbs sampling variables
	// user-topic counts
	public int[][] n_zu = null; // n_zu[z][u]: number of times topic z is
								// observed in posts by user u
	public int[] sum_nzu = null; // sum_nzu[u] total number of topics that are
									// observed in posts by user u

	// topic-word counts
	public int[][] n_wz = null; // n_wz[w][z]: number of times word w is
								// generated by topic z in a post
	public int[] sum_nwz = null; // sum_nwz[z]: total number of times words that
									// are generated by topic z in a post

	// topic-word distribution
	public double[][] topicWordDist = null; // topicWordDist[w][k]: the
											// distribution of word w for topic
											// k. Sum of each words distribution
											// for each k = 1

	// options for learning

	public double learning_rate_topicalInterest = 0.001;
	public int maxIteration_topicalInterest = 10;

	public double learning_rate_authorities = 0.001;
	public int maxIteration_Authorities = 10;

	public double learning_rate_hubs = 0.001;
	public int maxIteration_Hubs = 10;

	public int max_GibbsEM_Iterations = 100;

	/***
	 * 
	 * @param _datasetPath
	 * @param _nTopics
	 */
	public CTLR(String _datasetPath, int _nTopics, int _batch) {
		this.dataset = new Dataset(_datasetPath, _nTopics);
		this.nTopics = _nTopics;
		this.batch = _batch;
		n_zu = new int[nTopics][dataset.nUsers];
		sum_nzu = new int[dataset.nUsers];
		n_wz = new int[dataset.vocabulary.length][nTopics];
		sum_nwz = new int[nTopics];
		topicWordDist = new double[dataset.vocabulary.length][nTopics]; 
	}

	/***
	 * get likelihood of the whole dataset
	 * 
	 * @return
	 */
	private double getLikelihood() {
		// to be written
		// Compute the likelihood to make sure that it is improving L(text) +
		// L(link)
		// value can be more than 1
		// eqn 1 + 4

		double linkLikelihood = 0;
		double linkRelationshipLikelihood = 0;
		double linkAuthorityLikelihood = 0;
		double linkHubLikelihood = 0;
		double postLikelihood = 0;
		double postWordLikelihood = 0;
		double postTopicLikelihood = 0;
		double postThetaLikelihood = 0;
		double postTauLikelihood = 0;

		for (int u = 0; u < dataset.nUsers; u++) {
			User currUser = dataset.users[u];

			if (currUser.nonFollowers != null) {
				for (int i = 0; i < currUser.nonFollowers.length; i++) {
					int v = currUser.nonFollowers[i];
					User nonFollower = dataset.users[v];

					// Compute H_u * A_v
					double HuAv = 0;
					for (int z = 0; z < nTopics; z++) {
						HuAv += nonFollower.hubs[z] * currUser.authorities[z];
					}
					HuAv = HuAv * lamda;
					double fHuAv = 2 * ((1 / (Math.exp(-HuAv) + 1)) - 0.5);
					linkRelationshipLikelihood += Math.log(1 - fHuAv);
				}
			}

			if (currUser.followers != null) {
				for (int i = 0; i < currUser.followers.length; i++) {
					int v = currUser.followers[i];
					User follower = dataset.users[v];

					// Compute H_u * A_v
					double HuAv = 0;
					for (int z = 0; z < nTopics; z++) {
						HuAv += follower.hubs[z] * currUser.authorities[z];
					}
					HuAv = HuAv * lamda;
					double fHuAv = 2 * ((1 / (Math.exp(-HuAv) + 1)) - 0.5);
					linkRelationshipLikelihood += Math.log(fHuAv);
				}
			}

			if (currUser.nonFollowings != null) {
				for (int i = 0; i < currUser.nonFollowings.length; i++) {
					// Only compute likelihood of non followings which are in
					// training
					// batch (i.e. batch = 1)
					if (currUser.nonFollowingBatches[i] == batch) {
						int v = currUser.nonFollowings[i];
						User nonFollowing = dataset.users[v];

						// Compute H_u * A_v
						double HuAv = 0;
						for (int z = 0; z < nTopics; z++) {
							HuAv += currUser.hubs[z] * nonFollowing.authorities[z];
						}
						HuAv = HuAv * lamda;
						double fHuAv = 2 * ((1 / (Math.exp(-HuAv) + 1)) - 0.5);
						linkRelationshipLikelihood += Math.log(1.0 - fHuAv);
						;
					}
				}
			}

			// Compute following likelihood
			if (currUser.followings != null) {
				for (int i = 0; i < currUser.followings.length; i++) {
					// Only compute likelihood of followings which are in
					// training
					// batch
					// (i.e. batch = 1)
					if (currUser.followingBatches[i] == batch) {
						int v = currUser.followings[i];
						User following = dataset.users[v];

						// Compute H_u * A_v
						double HuAv = 0;
						for (int z = 0; z < nTopics; z++) {
							HuAv += currUser.hubs[z] * following.authorities[z];
						}
						HuAv = HuAv * lamda;
						double fHuAv = 2 * ((1 / (Math.exp(-HuAv) + 1)) - 0.5);
						linkRelationshipLikelihood += Math.log(fHuAv);
						;
					}
				}
			}

			for (int k = 0; k < nTopics; k++) {
				linkAuthorityLikelihood += (-Math.log(sigma)
						- (Math.pow(Math.log(currUser.authorities[k]) - currUser.topicalInterests[k], 2)
								/ (2 * Math.pow(sigma, 2))));
				linkHubLikelihood += (-Math.log(delta)
						- (Math.pow(Math.log(currUser.hubs[k]) - currUser.topicalInterests[k], 2)
								/ (2 * Math.pow(delta, 2))));
			}

			linkLikelihood += linkRelationshipLikelihood + linkAuthorityLikelihood + linkHubLikelihood;

			for (int s = 0; s < currUser.nPosts; s++) {
				if (currUser.postBatches[s] == batch) {
					Post currPost = currUser.posts[s];
					for (int w = 0; w < currPost.words.length; w++) {
						int word = currPost.words[w];
						postWordLikelihood += topicWordDist[word][currPost.topic];
					}
					postTopicLikelihood += currUser.topicalInterests[currPost.topic];
				}
			}

			for (int k = 0; k < nTopics; k++) {
				postThetaLikelihood += (alpha - 1) * Math.log(currUser.topicalInterests[k]);
			}
		}

		for (int k = 0; k < nTopics; k++) {
			for (int w = 0; w < dataset.vocabulary.length; w++) {
				postTauLikelihood += (gamma - 1) * Math.log(topicWordDist[w][k]);
			}
		}

		postLikelihood = postWordLikelihood + postTopicLikelihood + postThetaLikelihood + postTauLikelihood;

		return (linkLikelihood + postLikelihood);
	}

	/***
	 * compute likelihood of data as a function of topical interest of u when
	 * the interest is x, i.e., if L(data|parameters) = f(theta_u) +
	 * const-of-theta_u then this function returns f(x)
	 * 
	 * @param u
	 * @return
	 */
	private double getLikelihood_topicalInterest(int u, double[] x) {
		// Refer to Eqn 9 in Learning paper for Formula

		double authorityLikelihood = 0;
		double hubLikelihood = 0;
		double postLikelihood = 0;
		double topicLikelihood = 0;
		double finalLikelihood = 0;

		// Set the current user to be u
		User currUser = dataset.users[u];

		for (int k = 0; k < nTopics; k++) {
			authorityLikelihood += -Math.pow((Math.log(currUser.authorities[k]) - x[k]), 2) / (2 * Math.pow(delta, 2));
		}

		for (int k = 0; k < nTopics; k++) {
			hubLikelihood += -Math.pow((Math.log(currUser.hubs[k]) - x[k]), 2) / (2 * Math.pow(sigma, 2));
		}

		for (int i = 0; i < currUser.nPosts; i++) {
			// Only compute post likelihood of posts which are in batch (i.e.
			// training batch = 1)
			if (currUser.postBatches[i] == batch) {
				int postTopic = currUser.posts[i].topic;
				postLikelihood += x[postTopic];
			}
		}

		for (int k = 0; k < nTopics; k++) {
			topicLikelihood += (alpha - 1) * Math.log(x[k]);
		}
		// finalLikelihood = authorityLikelihood + hubLikelihood +
		// postLikelihood + topicLikelihood;
		finalLikelihood = authorityLikelihood + hubLikelihood + postLikelihood;
		return finalLikelihood;
	}

	/***
	 * compute gradient of likelihood of data with respect to interest of u in
	 * topic k when the interest is x, i.e., if if L(data|parameters) =
	 * f(theta_u) + const-of-theta_u then this function return df/dtheta_uk at
	 * theta_uk = x
	 * 
	 * @param u
	 * @param k
	 * @param x
	 * @return
	 */
	private double gradLikelihood_topicalInterest(int u, int k, double x) {
		// Refer to Eqn 11 in Learning paper

		double authorityLikelihood = 0;
		double hubLikelihood = 0;
		double postLikelihood = 0;
		double gradLikelihood = 0;

		// Set the current user to be u
		User currUser = dataset.users[u];

		authorityLikelihood = ((Math.log(currUser.authorities[k]) - x) / Math.pow(delta, 2));

		hubLikelihood = ((Math.log(currUser.hubs[k]) - x) / Math.pow(sigma, 2));

		for (int i = 0; i < currUser.nPosts; i++) {
			// Only compute post likelihood of posts which are in batch
			// (i.e. training batch = 1)
			if (currUser.postBatches[i] == batch) {
				// Only consider posts which are assigned topic k (i.e. z_{v,s}
				// = k)
				if (currUser.posts[i].topic == k) {
					postLikelihood += 1;
				}
			}
		}

		// gradLikelihood = authorityLikelihood + hubLikelihood + postLikelihood
		// + (alpha -1) / x;

		gradLikelihood = authorityLikelihood + hubLikelihood + postLikelihood;

		return gradLikelihood;
	}

	/***
	 * get projection of x on n-dimension simplex i.e., find the n-dimension
	 * probability distribution closest to x
	 * 
	 * @param x
	 * @param n
	 * @return
	 */
	private double[] simplexProjection(double[] x, double z) {
		// given all the k that u have, it adds up to 1
		// Refer to https://github.com/blei-lab/ctr/blob/master/opt.cpp
		double[] projX = new double[x.length];

		// copy the content of x into projX
		for (int i = 0; i < x.length; i++) {
			projX[i] = x[i];
		}
		// Sort projX in asc order
		Arrays.sort(projX);

		// Compute the sum of the offset
		double cumsum = -z;
		double p = 0;
		int j = 0;
		for (int i = x.length - 1; i >= 0; i--) {
			p = x[i];
			cumsum += p;
			if (p > cumsum / (j + 1)) {
				j++;
			} else {
				break;
			}
		}

		// Compute the offset for each topic
		double theta = cumsum / j;
		for (int i = 0; i < x.length; i++) {
			p = x[i] - theta;
			if (p < 0) {
				p = 0.0;
				//p = epsilon;
			}
			projX[i] = p;
		}
		
		
		double sum_x = 0;
		for (int i = 0; i < projX.length; i++) {
			sum_x += projX[i];
		}

		for (int i = 0; i < projX.length; i++) {
			projX[i] = projX[i] / sum_x;
		}

		return projX;
	}

	/***
	 * alternating step to optimize topical interest of u
	 * 
	 * @param u
	 */
	private void altOptimize_topicalInterest(int u) {
		double[] grad = new double[nTopics];
		double[] currentX = dataset.users[u].topicalInterests;
		double[] x = new double[nTopics];
		
		double currentF = 0 - getLikelihood_topicalInterest(u, currentX);
		
		boolean flag = true;
		double delta = 0;
		double f = Double.MAX_VALUE;

		// parameters for line search
		lineSearch_alpha = 0.0001;
		lineSearch_beta = 0.1;
		lineSearch_MaxIterations = 5;

		for (int iter = 0; iter < maxIteration_topicalInterest; iter++) {
			// compute gradient
			for (int k = 0; k < nTopics; k++) {
				grad[k] = 0 - gradLikelihood_topicalInterest(u, k, currentX[k]);
			}
			// start line search
			lineSearch_lambda = 1;
			flag = false;

			for (int lineSearchIter = 0; lineSearchIter < lineSearch_MaxIterations; lineSearchIter++) {
				// find new x
				for (int k = 0; k < nTopics; k++) {
					x[k] = currentX[k] - learning_rate_topicalInterest * grad[k];
				}
				
				x = simplexProjection(x, 1);// this step to make sure that we

				// compute f at the new x
				f = 0 - getLikelihood_topicalInterest(u, x);

				// compute ||currentX - x||^2
				delta = 0;
				for (int k = 0; k < nTopics; k++) {
					delta += Math.pow(currentX[k] - x[k], 2);
				}
				// check the condition to stop line search
				if (f - currentF <= (-lineSearch_alpha / lineSearch_lambda) * delta) {
					flag = true;
					break;
				} else {
					lineSearch_lambda *= lineSearch_beta;
				}
			}
			if (flag) {// line search successful
				currentF = f;
				for (int k = 0; k < nTopics; k++) {
					currentX[k] = x[k];
				}
				// to see if F actually reduce after every iteration
				//System.out.printf("alt_topic: u = %d iter = %d f = %f\n", u, iter, f);
			} else {
				// to see if F actually reduce after every iteration
				//System.out.printf("alt_topic: u = %d iter = %d f = %f\n", u, iter, f);
				break;// cannot improve further
			}
		}
	}

	/***
	 * compute likelihood of data as a function of authority of u when the
	 * authority is x, i.e., if L(data|parameters) = f(A_u) + const-of-A_u then
	 * this function returns f(x)
	 * 
	 * @param v
	 * @param x[]
	 * @return
	 */
	private double getLikelihood_authority(int v, double[] x) {
		// Refer to Eqn 13 in Learning paper

		double followerLikelihood = 0;
		double nonFollowerLikelihood = 0;
		double postLikelihood = 0;
		double likelihood = 0;

		// Set the current user to be v
		User currUser = dataset.users[v];

		// Compute non follower likelihood
		if (currUser.nonFollowers != null) {
			for (int i = 0; i < currUser.nonFollowers.length; i++) {
				int u = currUser.nonFollowers[i];
				User nonFollower = dataset.users[u];

				// Compute H_u * A_v
				double HuAv = 0;
				for (int z = 0; z < nTopics; z++) {
					HuAv += nonFollower.hubs[z] * x[z];// now A_v is x
				}
				HuAv = HuAv * lamda;
				// System.out.println("HuAv:"+HuAv);
				double fHuAv = 2 * ((1 / (Math.exp(-HuAv) + 1)) - 0.5);
				// System.out.println("fHuAv:"+fHuAv);
				nonFollowerLikelihood += Math.log(1 - fHuAv);
			}
		}

		// Compute follower likelihood
		if (currUser.followers != null) {
			for (int i = 0; i < currUser.followers.length; i++) {
				int u = currUser.followers[i];
				User follower = dataset.users[u];

				// Compute H_u * A_v
				double HuAv = 0;
				for (int z = 0; z < nTopics; z++) {
					HuAv += follower.hubs[z] * x[z];// now A_v is x
				}
				HuAv = HuAv * lamda;
				double fHuAv = 2 * ((1 / (Math.exp(-HuAv) + 1)) - 0.5);
				followerLikelihood += Math.log(fHuAv);
			}
		}

		// Compute post likelihood
		for (int k = 0; k < nTopics; k++) {
			postLikelihood += Math.pow((Math.log(x[k]) - currUser.topicalInterests[k]), 2) / (2 * Math.pow(sigma, 2));
		}

		likelihood = nonFollowerLikelihood + followerLikelihood - postLikelihood;
		// System.out.println("Num of Non Followers:" +
		// currUser.nonFollowers.length);
		// System.out.println("NonFollowersLikelihood:" +
		// nonFollowerLikelihood);
		// System.out.println("Num of Followers:" + currUser.followers.length);
		// System.out.println("FollowerLikelihood:" + followerLikelihood);
		// System.out.println("PostLikelihood:" + postLikelihood);

		return likelihood;
	}

	/***
	 * compute gradient of likelihood of data with respect to authority of u in
	 * topic k when the authority is x, i.e., if if L(data|parameters) = f(A_u)
	 * + const-of-A_u then this function return df/dA_uk at A_uk = x
	 * 
	 * @param v
	 * @param k
	 * @param x
	 * @return
	 */
	private double gradLikelihood_authority(int v, int k, double x) {
		// Refer to Eqn 15 in Learning paper

		double followerLikelihood = 0;
		double nonFollowerLikelihood = 0;
		double postLikelihood = 0;
		double gradLikelihood = 0;

		// Set the current user to be v
		User currUser = dataset.users[v];

		// Compute non follower likelihood
		if (currUser.nonFollowers != null) {
			for (int i = 0; i < currUser.nonFollowers.length; i++) {
				int u = currUser.nonFollowers[i];
				User nonFollower = dataset.users[u];

				// Compute H_u * A_v
				double HuAv = 0;
				for (int z = 0; z < nTopics; z++) {
					if (z == k) {
						HuAv += nonFollower.hubs[z] * x;
					} else {
						HuAv += nonFollower.hubs[z] * currUser.authorities[z];
					}
				}
				HuAv = HuAv * lamda;
				nonFollowerLikelihood += -(lamda * nonFollower.hubs[k])
						- 1 / (Math.exp(-HuAv) + 1) * Math.exp(-HuAv) * -(lamda * nonFollower.hubs[k]);
			}
		}

		// Compute follower likelihood
		if (currUser.followers != null) {
			for (int i = 0; i < currUser.followers.length; i++) {
				int u = currUser.followers[i];
				User follower = dataset.users[u];

				// Compute H_u * A_v
				double HuAv = 0;
				for (int z = 0; z < nTopics; z++) {
					if (z == k) {
						HuAv += follower.hubs[z] * x;
					} else {
						HuAv += follower.hubs[z] * currUser.authorities[z];
					}
				}
				HuAv = HuAv * lamda;
				followerLikelihood += ((1 / (1 - Math.exp(-HuAv))) * -Math.exp(-HuAv) * -(lamda * follower.hubs[k]))
						- ((1 / (Math.exp(-HuAv) + 1)) * Math.exp(-HuAv) * -(lamda * follower.hubs[k]));
			}
		}

		postLikelihood = ((Math.log(x) - currUser.topicalInterests[k]) / Math.pow(sigma, 2)) * (1 / x);

		gradLikelihood = nonFollowerLikelihood + followerLikelihood - postLikelihood;

		return gradLikelihood;
	}

	/***
	 * alternating step to optimize authorities of user u
	 * 
	 * @param u
	 */
	private void altOptimize_Authorities(int u) {
		double[] grad = new double[nTopics];
		double[] currentX = dataset.users[u].authorities;
		double[] x = new double[nTopics];
		
		double currentF = 0 - getLikelihood_authority(u, currentX);
		
		boolean flag = true;
		double delta = 0;
		double f = Double.MAX_VALUE;

		// parameters for line search
		lineSearch_alpha = 0.0001;
		lineSearch_beta = 0.1;
		lineSearch_MaxIterations = 5;

		for (int iter = 0; iter < maxIteration_Authorities; iter++) {
			// compute gradient
			for (int k = 0; k < nTopics; k++) {
				grad[k] = 0 - gradLikelihood_authority(u, k, currentX[k]);
			}
			// start line search
			lineSearch_lambda = 1;
			flag = false;

			for (int lineSearchIter = 0; lineSearchIter < lineSearch_MaxIterations; lineSearchIter++) {
				// find new x
				for (int k = 0; k < nTopics; k++) {
					x[k] = currentX[k] - learning_rate_authorities * grad[k];
					if (x[k]<epsilon){
						x[k] = epsilon;
					}
				}
				
				// compute f at the new x
				f = 0 - getLikelihood_authority(u, x);

				// compute ||currentX - x||^2
				delta = 0;
				for (int k = 0; k < nTopics; k++) {
					delta += Math.pow(currentX[k] - x[k], 2);
				}
				// check the condition to stop line search
				if (f - currentF <= (-lineSearch_alpha / lineSearch_lambda) * delta) {
					flag = true;
					break;
				} else {
					lineSearch_lambda *= lineSearch_beta;
				}
			}
			if (flag) {// line search successful
				currentF = f;
				for (int k = 0; k < nTopics; k++) {
					currentX[k] = x[k];
				}
				// to see if F actually reduce after every iteration
				//System.out.printf("alt_authority: u = %d iter = %d f = %f\n", u, iter, f);
			} else {
				// to see if F actually reduce after every iteration
				//System.out.printf("alt_authority: u = %d iter = %d f = %f\n", u, iter, f);
				break;// cannot improve further
			}
		}
	}

	/***
	 * compute likelihood of data as a function of hub of u when the hub is x,
	 * i.e., if L(data|parameters) = f(H_u) + const-of-H_u then this function
	 * returns f(x)
	 * 
	 * @param u
	 * @param x[]
	 * @return
	 */
	private double getLikelihood_hub(int u, double[] x) {
		// Refer to Eqn 17 in Learning paper

		double followingLikelihood = 0;
		double nonFollowingLikelihood = 0;
		double postLikelihood = 0;
		double likelihood = 0;

		// Set the current user to be u
		User currUser = dataset.users[u];

		// Compute non following likelihood
		if (currUser.nonFollowings != null) {
			for (int i = 0; i < currUser.nonFollowings.length; i++) {
				// Only compute likelihood of non followings which are in
				// training
				// batch (i.e. batch = 1)
				if (currUser.nonFollowingBatches[i] == batch) {
					int v = currUser.nonFollowings[i];
					User nonFollowing = dataset.users[v];

					// Compute H_u * A_v
					double HuAv = 0;
					for (int z = 0; z < nTopics; z++) {
						HuAv += x[z] * nonFollowing.authorities[z];
					}
					HuAv = HuAv * lamda;
					double fHuAv = 2 * ((1 / (Math.exp(-HuAv) + 1)) - 0.5);
					nonFollowingLikelihood += Math.log(1.0 - fHuAv);
					;
				}
			}
		}

		// Compute following likelihood
		if (currUser.followings != null) {
			for (int i = 0; i < currUser.followings.length; i++) {
				// Only compute likelihood of followings which are in training
				// batch
				// (i.e. batch = 1)
				if (currUser.followingBatches[i] == batch) {
					int v = currUser.followings[i];
					User following = dataset.users[v];

					// Compute H_u * A_v
					double HuAv = 0;
					for (int z = 0; z < nTopics; z++) {
						HuAv += x[z] * following.authorities[z];
					}
					HuAv = HuAv * lamda;
					double fHuAv = 2 * ((1 / (Math.exp(-HuAv) + 1)) - 0.5);
					followingLikelihood += Math.log(fHuAv);
					;
				}
			}
		}

		// Compute post likelihood
		for (int k = 0; k < nTopics; k++) {
			postLikelihood += Math.pow(Math.log(x[k]) - currUser.topicalInterests[k], 2) / (2 * Math.pow(delta, 2));
		}

		likelihood = nonFollowingLikelihood + followingLikelihood - postLikelihood;

		return likelihood;
	}

	/***
	 * compute gradient of likelihood of data with respect to hub of u in topic
	 * k when the hub is x, i.e., if if L(data|parameters) = f(H_u) +
	 * const-of-H_u then this function return df/dH_uk at H_uk = x
	 * 
	 * @param u
	 * @param k
	 * @param x
	 * @return
	 */
	private double gradLikelihood_hub(int u, int k, double x) {
		// Refer to Eqn 19 in Learning paper

		double followingLikelihood = 0;
		double nonFollowingLikelihood = 0;
		double postLikelihood = 0;
		double gradLikelihood = 0;

		// Set the current user to be u
		User currUser = dataset.users[u];

		// Compute non following likelihood
		if (currUser.nonFollowings != null) {
			for (int i = 0; i < currUser.nonFollowings.length; i++) {
				// Only compute likelihood of non followings which are in
				// training
				// batch (i.e. batch = 1)
				if (currUser.nonFollowingBatches[i] == batch) {
					int v = currUser.nonFollowings[i];
					User nonFollowing = dataset.users[v];

					// Compute H_u * A_v
					double HuAv = 0;
					for (int z = 0; z < nTopics; z++) {
						if (z == k) {
							HuAv += x * nonFollowing.authorities[z];
						} else {
							HuAv += currUser.hubs[z] * nonFollowing.authorities[z];
						}
					}
					HuAv = HuAv * lamda;
					nonFollowingLikelihood += -(lamda * nonFollowing.authorities[k])
							- 1 / (Math.exp(-HuAv) + 1) * Math.exp(-HuAv) * -(lamda * nonFollowing.authorities[k]);
				}
			}
		}

		// Compute following likelihood
		if (currUser.followings != null) {
			for (int i = 0; i < currUser.followings.length; i++) {
				// Only compute likelihood of followings which are in training
				// batch
				// (i.e. batch = 1)
				if (currUser.followingBatches[i] == batch) {
					int v = currUser.followings[i];
					User following = dataset.users[v];

					// Compute H_u * A_v
					double HuAv = 0;
					for (int z = 0; z < nTopics; z++) {
						if (z == k) {
							HuAv += x * following.authorities[z];
						} else {
							HuAv += currUser.hubs[z] * following.authorities[z];
						}
					}
					HuAv = HuAv * lamda;
					followingLikelihood += ((1 / (1 - Math.exp(-HuAv))) * -Math.exp(-HuAv)
							* -(lamda * following.authorities[k]))
							- ((1 / (Math.exp(-HuAv) + 1)) * Math.exp(-HuAv) * -(lamda * following.authorities[k]));
				}
			}
		}

		postLikelihood = ((Math.log(x) - currUser.topicalInterests[k]) / Math.pow(delta, 2)) * (1 / x);

		gradLikelihood = nonFollowingLikelihood + followingLikelihood - postLikelihood;

		return gradLikelihood;
	}

	/***
	 * alternating step to optimize hubs of user u
	 * 
	 * @param u
	 */
	private void altOptimize_Hubs(int u) {
		// the following code is just a draft, to be corrected.
		// will need more checking, but roughly the gradient descent
		// for learning A_u consists of the following main steps
		double[] grad = new double[nTopics];
		double[] currentX = dataset.users[u].hubs;
		double[] x = new double[nTopics];
		
		double currentF = 0 - getLikelihood_hub(u, currentX);
		
		boolean flag = true;
		double delta = 0;
		double f = Double.MAX_VALUE;

		// parameters for line search
		lineSearch_alpha = 0.0001;
		lineSearch_beta = 0.1;
		lineSearch_MaxIterations = 5;

		for (int iter = 0; iter < maxIteration_Hubs; iter++) {
			// compute gradient
			for (int k = 0; k < nTopics; k++) {
				grad[k] = 0 - gradLikelihood_hub(u, k, currentX[k]);
			}
			// start line search
			lineSearch_lambda = 1;
			flag = false;

			for (int lineSearchIter = 0; lineSearchIter < lineSearch_MaxIterations; lineSearchIter++) {
				// find new x
				for (int k = 0; k < nTopics; k++) {
					x[k] = currentX[k] - learning_rate_hubs * grad[k];
					if (x[k]<epsilon){
						x[k] = epsilon;
					}
				}
				
				// compute f at the new x
				f = 0 - getLikelihood_hub(u, x);

				// compute ||currentX - x||^2
				delta = 0;
				for (int k = 0; k < nTopics; k++) {
					delta += Math.pow(currentX[k] - x[k], 2);
				}
				// check the condition to stop line search
				if (f - currentF <= (-lineSearch_alpha / lineSearch_lambda) * delta) {
					flag = true;
					break;
				} else {
					lineSearch_lambda *= lineSearch_beta;
				}
			}
			if (flag) {// line search successful
				currentF = f;
				for (int k = 0; k < nTopics; k++) {
					currentX[k] = x[k];
				}
				// to see if F actually reduce after every iteration
				//System.out.printf("alt_hub: u = %d iter = %d f = %f\n", u, iter, f);
			} else {
				// to see if F actually reduce after every iteration
				//System.out.printf("alt_hub: u = %d iter = %d f = %f\n", u, iter, f);
				break;// cannot improve further
			}
		}
	}

	/***
	 * alternating step to optimize topics' word distribution
	 */
	private void altOptimize_topics() {
		// initialize the count variables
		for (int k = 0; k < nTopics; k++) {
			sum_nwz[k] = 0;
			for (int w = 0; w < dataset.vocabulary.length; w++) {
				n_wz[w][k] = 0;
			}
		}

		// update count variable base on the post topic assigned
		for (int u = 0; u < dataset.nUsers; u++) {
			User currUser = dataset.users[u];
			for (int n = 0; n < currUser.posts.length; n++) {
				Post currPost = currUser.posts[n];

				// only consider posts in batch
				if (currUser.postBatches[n] == batch) {
					int z = currPost.topic;
					for (int w = 0; w < currPost.words.length; w++) {
						int wordIndex = currPost.words[w];
						sum_nwz[z] += 1;
						n_wz[wordIndex][z] += 1;
					}
				}
			}
		}

		// compute topic word distribution
		for (int k = 0; k < nTopics; k++) {
			for (int w = 0; w < dataset.vocabulary.length; w++) {
				topicWordDist[w][k] = (n_wz[w][k] + gamma) / (sum_nwz[k] + (gamma * dataset.vocabulary.length));
			}
		}
	}

	/***
	 * to sample topic for post n of user u
	 * 
	 * @param u
	 * @param n
	 */
	private void sampleTopic(int u, int n) {
		rand = new Random();

		// Set the current user to be u
		User currUser = dataset.users[u];

		// Get current topic
		int currz = currUser.posts[n].topic;

		double sump = 0;
		// p: p(z_u,s = z| rest)

		double[] p = new double[nTopics];
		double max = -Double.MAX_VALUE;
		for (int z = 0; z < nTopics; z++) {
			// User-topic
			p[z] = Math.log(currUser.topicalInterests[z]);

			// topic-word
			Post currPost = currUser.posts[n];
			for (int w = 0; w < currPost.words.length; w++) {
				int word = currPost.words[w];
				p[z] += Math.log(topicWordDist[word][z]);
			}

			// update min
			if (max < p[z]) {
				max = p[z];
			}

		}
		// convert log(sump) to probability
		for (int z = 0; z < nTopics; z++) {
			p[z] = p[z] - max;
			p[z] = Math.exp(p[z]);

			// cumulative
			p[z] = sump + p[z];
			sump = p[z];
		}

		sump = rand.nextDouble() * sump;
		for (int z = 0; z < nTopics; z++) {
			if (sump > p[z])
				continue;
			// Sample topic
			currUser.posts[n].topic = z;
			return;
		}

	}

	/***
	 * initialize the data before training
	 */
	public void init() {
		alpha = nTopics/50;
		gamma = 0.001;
		sigma = 0.2;
		delta = 0.2;

		rand = new Random();
		// initialize the count variables
		for (int u = 0; u < dataset.nUsers; u++) {
			sum_nzu[u] = 0;
			for (int k = 0; k < nTopics; k++) {
				n_zu[k][u] = 0;
			}
		}

		// randomly assign topics to posts
		for (int u = 0; u < dataset.nUsers; u++) {
			User currUser = dataset.users[u];
			for (int n = 0; n < currUser.posts.length; n++) {
				// only consider posts in batch
				if (currUser.postBatches[n] == batch) {
					int randTopic = rand.nextInt(nTopics);
					currUser.posts[n].topic = randTopic;
					sum_nzu[u] += 1;
					n_zu[randTopic][u] += 1;
				}
			}
		}

		// compute user topical interests base on the random topic assigment
		for (int u = 0; u < dataset.nUsers; u++) {
			User currUser = dataset.users[u];
			for (int k = 0; k < nTopics; k++) {
				currUser.topicalInterests[k] = (n_zu[k][u] + alpha) / (sum_nzu[u] + (alpha * dataset.nUsers));
			}
		}

		// randomly regress user's topical interest to initialize authority and
		// hub
		for (int u = 0; u < dataset.nUsers; u++) {
			User currUser = dataset.users[u];
			for (int k = 0; k < nTopics; k++) {
				NormalDistribution g = new NormalDistribution(currUser.topicalInterests[k], sigma);
				currUser.authorities[k] = Math.exp(g.sample());
				currUser.hubs[k] = Math.exp(g.sample());

			}
		}

		// compute topic words distribution base on the random topic assigment
		altOptimize_topics();

	}

	/***
	 * modeling learning
	 */
	public void train() {
		init();
		for (int iter = 0; iter < max_GibbsEM_Iterations; iter++) {
			/*for (int k=0;k<nTopics;k++){
				System.out.printf("%f \t", dataset.users[0].topicalInterests[k]);
			}
			System.out.println();
			for (int k=0;k<nTopics;k++){
				System.out.printf("%f \t", dataset.users[1].topicalInterests[k]);
			}
			System.out.println();
			*/
			// EM part that employs alternating optimization
			for (int u = 0; u < dataset.nUsers; u++) {
				altOptimize_topicalInterest(u);
			}
			for (int u = 0; u < dataset.nUsers; u++) {
				altOptimize_Authorities(u);
			}
			for (int u = 0; u < dataset.nUsers; u++) {
				altOptimize_Hubs(u);
			}
			altOptimize_topics();
			// Gibbs part
			for (int u = 0; u < dataset.nUsers; u++) {
				for (int n = 0; n < dataset.users[u].nPosts; n++) {
					// only consider posts in batch
					if (dataset.users[u].postBatches[n] == batch) {
						sampleTopic(u, n);
					}
				}
			}
			// Check that the likelihood increase
			System.out.printf("likelihood after %d steps: %f", iter, getLikelihood());
			System.out.println();
		}
		//print out the learned parameters
		output_topicWord();
		output_topicInterest();
		output_authority();
		output_hub();
	}
	
	public void output_topicWord(){
		try {
			File f = new File(dataset.path + "/l_topicalWordDistributions.csv");
			FileWriter fo = new FileWriter(f);
			for (int k=0; k<nTopics; k++){
				String text = Integer.toString(k);
				for (int w=0; w < dataset.vocabulary.length; w++){
					text = text + "," + Double.toString(topicWordDist[w][k]);
				}
				fo.write(text + "\n");
			}
			fo.close();
		} catch (Exception e) {
			System.out.println("Error in writing to topical word file!");
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public void output_topicInterest(){
		try {
			File f = new File(dataset.path + "/l_userTopicalInterestDistributions.csv");
			FileWriter fo = new FileWriter(f);
			for (int u=0; u<dataset.nUsers; u++){
				User currUser = dataset.users[u];
				String text = currUser.userId;
				for (int k=0; k < nTopics; k++){
					text = text + "," + Double.toString(currUser.topicalInterests[k]);
				}
				fo.write(text + "\n");
			}
			fo.close();
		} catch (Exception e) {
			System.out.println("Error in writing to topical interest file!");
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public void output_authority(){
		try {
			File f = new File(dataset.path + "/l_userAuthorityDistributions.csv");
			FileWriter fo = new FileWriter(f);
			for (int u=0; u<dataset.nUsers; u++){
				User currUser = dataset.users[u];
				String text = currUser.userId;
				for (int k=0; k < nTopics; k++){
					text = text + "," + Double.toString(currUser.authorities[k]);
				}
				fo.write(text + "\n");
			}
			fo.close();
		} catch (Exception e) {
			System.out.println("Error in writing to authority file!");
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public void output_hub(){
		try {
			File f = new File(dataset.path + "/l_userHubDistributions.csv");
			FileWriter fo = new FileWriter(f);
			for (int u=0; u<dataset.nUsers; u++){
				User currUser = dataset.users[u];
				String text = currUser.userId;
				for (int k=0; k < nTopics; k++){
					text = text + "," + Double.toString(currUser.hubs[k]);
				}
				fo.write(text + "\n");
			}
			fo.close();
		} catch (Exception e) {
			System.out.println("Error in writing to topical interest file!");
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	/***
	 * checking if the gradient computation of likelihood by user topical
	 * interest theta_{u,k} is properly implemented
	 * 
	 * @param u
	 * @param k
	 */
	public void gradCheck_TopicalInterest(int u, int k) {
		double DELTA = 1;
		Random rand = new Random(System.currentTimeMillis());

		double[] x = dataset.users[u].topicalInterests;

		double f = getLikelihood_topicalInterest(u, x);
		double g = gradLikelihood_topicalInterest(u, k, x[k]);

		for (int i = 1; i <= 20; i++) {
			// reduce DELTA
			DELTA *= 0.1;
			x[k] += DELTA;
			double DELTAF = getLikelihood_topicalInterest(u, x);
			double numGrad = (DELTAF - f) / DELTA;
			System.out.printf(String.format("TopicInterest DELTA = %f numGrad = %f grad = %f\n", DELTA, numGrad, g));
			// if grad function is implemented properly, we will see numGrad
			// gets closer to grad
			x[k] -= DELTA;

		}
	}

	/***
	 * checking if the gradient computation of likelihood by A_{v,k} is properly
	 * implemented
	 * 
	 * @param v
	 * @param k
	 */
	public void gradCheck_Authority(int v, int k) {
		double DELTA = 1;
		Random rand = new Random(System.currentTimeMillis());

		double[] x = dataset.users[v].authorities;

		double f = getLikelihood_authority(v, x);
		double g = gradLikelihood_authority(v, k, x[k]);

		for (int i = 1; i <= 20; i++) {
			// reduce DELTA
			DELTA *= 0.1;
			x[k] += DELTA;
			double DELTAF = getLikelihood_authority(v, x);
			double numGrad = (DELTAF - f) / DELTA;
			System.out.printf(String.format("Authority DELTA = %f numGrad = %f grad = %f\n", DELTA, numGrad, g));
			// if grad function is implemented properly, we will see numGrad
			// gets closer to grad
			x[k] -= DELTA;

		}
	}

	/***
	 * checking if the gradient computation of likelihood by H_{u,k} is properly
	 * implemented
	 * 
	 * @param u
	 * @param k
	 */
	public void gradCheck_Hub(int u, int k) {
		double DELTA = 1;
		Random rand = new Random(System.currentTimeMillis());

		double[] x = dataset.users[u].hubs;

		double f = getLikelihood_hub(u, x);
		double g = gradLikelihood_hub(u, k, x[k]);

		for (int i = 1; i <= 20; i++) {
			// reduce DELTA
			DELTA *= 0.1;
			x[k] += DELTA;
			double DELTAF = getLikelihood_hub(u, x);
			double numGrad = (DELTAF - f) / DELTA;
			System.out.printf(String.format("Hub DELTA = %f numGrad = %f grad = %f\n", DELTA, numGrad, g));
			// if grad function is implemented properly, we will see numGrad
			// gets closer to grad
			x[k] -= DELTA;

		}
	}

	public void altCheck_TopicalInterest(int u) {
		altOptimize_topicalInterest(u);
	}

	public void altCheck_Authority(int u) {
		altOptimize_Authorities(u);
	}

	public void altCheck_Hub(int u) {
		altOptimize_Hubs(u);
	}

	public static void main(String[] args) {
		double[] x = new double[] { 0, 0.6, 0.4, 0.1 };
		CTLR ctlr = new CTLR("", 0, 0);
		double[] y = ctlr.simplexProjection(x, 1);

		for (int i = 0; i < x.length; i++) {
			System.out.printf("x[%d] = %f, y[%d] = %f\n", i, x[i], i, y[i]);
		}
	}

}
