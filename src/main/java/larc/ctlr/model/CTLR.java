package larc.ctlr.model;

public class CTLR {
	public Dataset dataset;
	public int nTopics;

	// priors

	public double alpha;// prior for users' interest
	public double beta; // prior for topics
	public double sigma;// variance of users' authorities
	public double delta;// variance of users' hubs

	// options for learning

	public double learning_rate_topicalInterest = 0.01;
	public int maxIteration_topicalInterest = 10;

	public double learning_rate_authorities = 0.01;
	public int maxIteration_Authorities = 10;

	public double learning_rate_hubs = 0.01;
	public int maxIteration_Hubs = 10;

	public int max_GibbsEM_Iterations = 100;

	/***
	 * 
	 * @param _datasetPath
	 * @param _nTopics
	 */
	public CTLR(String _datasetPath, int _nTopics) {
		this.dataset = new Dataset(_datasetPath);
		this.nTopics = _nTopics;
	}

	/***
	 * get likelihood of the whole dataset
	 * 
	 * @return
	 */
	private double getLikelihood() {
		// to be written
		// Compute the likelihood to make sure that it is improving L(text) + L(link)
		// value can be more than 1
		return 0;
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
		
		//Set the current user to be u
		User currUser = dataset.users[u];
	
		for (int k =0; k<nTopics;k++){
			authorityLikelihood += -Math.pow((Math.log(currUser.authorities[k]-x[k])), 2)/(2*Math.pow(delta, 2));
		}
		
		for (int k =0; k<nTopics;k++){
			hubLikelihood += -Math.pow((Math.log(currUser.hubs[k]-x[k])), 2)/(2*Math.pow(sigma, 2));
		}
		
		for (int i=0; i<currUser.nPosts;i++){
			//Only compute post likelihood of posts which are in training batch (i.e. batch = 1)
			if (currUser.postBatches[i]==1){
				int postTopic = currUser.posts[i].topic;
				postLikelihood += x[postTopic];
			}
		}
		
		for (int k =0; k<nTopics;k++){
			topicLikelihood += Math.pow(alpha, -1)* Math.log(x[k]);
		}
		
		finalLikelihood = authorityLikelihood + hubLikelihood + postLikelihood + topicLikelihood; 
		
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
	private double gradLikelihood_topicalInterest(int u,int k, double x) {
	// Refer to Eqn 11 in Learning paper
		
		double authorityLikelihood = 0;
		double hubLikelihood = 0;
		double postLikelihood = 0;
		double gradLikelihood = 0;		
		
		//Set the current user to be u
		User currUser = dataset.users[u];
		
		authorityLikelihood = ((Math.log(currUser.authorities[k])-x)/Math.pow(delta,2)); 
		
		hubLikelihood = ((Math.log(currUser.hubs[k])-x)/Math.pow(sigma,2));
		
		for (int i=0; i<currUser.nPosts;i++){
			//Only compute post likelihood of posts which are in training batch (i.e. batch = 1)
			if (currUser.postBatches[i]==1){
				//Only consider posts which are assigned topic k (i.e. z_{v,s} = k)
				if (currUser.posts[i].topic==k){
					postLikelihood += (1+(Math.pow(alpha,-1)/x));
				}
			}
		}
		
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
	private double[] simplexProjection(double[] x, int n) {
		// to be written
		// given all the k that u have, it adds up to 1
		// Refer to https://github.com/blei-lab/ctr/blob/master/opt.cpp
		return null;
	}

	/***
	 * alternating step to optimize topical interest of u
	 * 
	 * @param u
	 */
	private void altOptimize_topicalInterest(int u) {
		// the following code is just a draft, to be corrected.
		// will need more checking, but roughly the projected gradient descent
		// for learning theta_u consists of the following main steps
		double[] grad = new double[nTopics];
		double[] currentX = dataset.users[u].topicalInterests;
		double[] x = new double[nTopics];
		double currentF = getLikelihood_topicalInterest(u, currentX);

		for (int iter = 0; iter < maxIteration_topicalInterest; iter++) {
			for (int k = 0; k < nTopics; k++) {
				grad[k] = gradLikelihood_topicalInterest(u, k, currentX[k]);
				x[k] = currentX[k] - learning_rate_topicalInterest * grad[k];
			}
			x = simplexProjection(x, nTopics);// this step to make sure that we have theta_uk summing up to 1
			double f = getLikelihood_topicalInterest(u, x);
			if (f < currentF) {
				currentF = f;
				for (int k = 0; k < nTopics; k++) {
					currentX[k] = x[k];
				}
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
		double gradLikelihood = 0;
		
		//Set the current user to be v
		User currUser = dataset.users[v];
				
		//Compute non follower likelihood
		for (int i=0; i<currUser.nonFollowers.length; i++){
			int u = currUser.nonFollowers[i];
			
			User nonFollower = dataset.users[u];
						
			//Compute H_u * A_v
			double HuAv = 0;
			for (int j=0; j<nonFollower.hubs.length; j++){
				for (int z=0; z<currUser.authorities.length; z++){
					if (j==z){
						HuAv += nonFollower.hubs[j] * currUser.authorities[z];
					}
				}
			}
			nonFollowerLikelihood += Math.log(2*((1/(Math.pow(Math.E, -HuAv)+1)) - 1/2));
		} 
		
		//Compute follower likelihood
		for (int i=0; i<currUser.followers.length; i++){
			int u = currUser.followers[i];
			User follower = dataset.users[u];
						
			//Compute H_u * A_v
			double HuAv = 0;
			for (int j=0; j<follower.hubs.length; j++){
				for (int z=0; z<currUser.authorities.length; z++){
					if (j==z){
						HuAv += follower.hubs[j] * currUser.authorities[z];
					}
				}
			}
			followerLikelihood += Math.log(1-(2*((1/(Math.pow(Math.E, -HuAv)+1)) - 1/2))) ;
		}
		
		//Compute post likelihood
		for (int k=0; k<nTopics; k++){
			postLikelihood += Math.pow(Math.log(currUser.authorities[k])-x[k], 2)/(2*Math.pow(sigma, 2));
		}
				
		gradLikelihood = nonFollowerLikelihood + followerLikelihood - postLikelihood;
				
		return gradLikelihood;
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
		
		//Set the current user to be v
		User currUser = dataset.users[v];

		//Compute non follower likelihood
		for (int i=0; i<currUser.nonFollowers.length; i++){
				int u = currUser.nonFollowers[i];
				User nonFollower = dataset.users[u];
				
				//Compute H_u * A_v
				double HuAv = 0;
				for (int j=0; j<nonFollower.hubs.length; j++){
					for (int z=0; z<currUser.authorities.length; z++){
						if (j==z){
							HuAv += nonFollower.hubs[j] * currUser.authorities[z];
						}
					}
				}
				nonFollowerLikelihood += ((1/(1-Math.pow(Math.E,-HuAv))) * (-Math.pow(Math.E,-HuAv)) * (-nonFollower.hubs[k])) 
						- ((1/(Math.pow(Math.E,-HuAv)+1)) * (Math.pow(Math.E,-HuAv)) * (-nonFollower.hubs[k]));
		} 
		
		//Compute follower likelihood
		for (int i=0; i<currUser.followers.length; i++){
				int u = currUser.followers[i];
				User follower = dataset.users[u];
				
				//Compute H_u * A_v
				double HuAv = 0;
				for (int j=0; j<follower.hubs.length; j++){
					for (int z=0; z<currUser.authorities.length; z++){
						if (j==z){
							HuAv += follower.hubs[j] * currUser.authorities[z];
						}
					}
				}
				followerLikelihood += -follower.hubs[k] - ((1/(Math.pow(Math.E, -HuAv)+1)) * Math.pow(Math.E, -HuAv) * (-follower.hubs[k])) ;
		}

		postLikelihood = ((Math.log(currUser.authorities[k]) - x)/Math.pow(sigma,2)) * (1/currUser.authorities[k]);
		
		gradLikelihood = nonFollowerLikelihood + followerLikelihood - postLikelihood;
		
		return gradLikelihood;
	}

	/***
	 * alternating step to optimize authorities of user u
	 * 
	 * @param u
	 */
	private void altOptimize_Authorities(int u) {
		// the following code is just a draft, to be corrected.
		// will need more checking, but roughly the gradient descent
		// for learning A_u consists of the following main steps
		double[] grad = new double[nTopics];
		double[] currentX = dataset.users[u].authorities;
		double[] x = new double[nTopics];
		double currentF = getLikelihood_authority(u, currentX);

		for (int iter = 0; iter < maxIteration_Authorities; iter++) {
			for (int k = 0; k < nTopics; k++) {
				grad[k] = gradLikelihood_authority(u, k, currentX[k]);
				x[k] = currentX[k] - learning_rate_authorities * grad[k];
			}
			double f = getLikelihood_authority(u, x);
			if (f < currentF) {
				currentF = f;
				for (int k = 0; k < nTopics; k++) {
					currentX[k] = x[k];
				}
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
		
		//Set the current user to be u
		User currUser = dataset.users[u];

		//Compute non following likelihood
		for (int i=0; i<currUser.nonFollowings.length; i++){
			//Only compute likelihood of non followings which are in training batch (i.e. batch = 1)
			if (currUser.nonFollowingBatches[i]==1){
				int v = currUser.nonFollowings[i];
				User nonFollowing = dataset.users[v];
						
				//Compute H_u * A_v
				double HuAv = 0;
				for (int j=0; j<currUser.hubs.length; j++){
					for (int z=0; z<nonFollowing.authorities.length; z++){
						if (j==z){
							HuAv += currUser.hubs[j] * nonFollowing.authorities[z];
						}
					}
				}
				nonFollowingLikelihood += Math.log(2*((1/(Math.pow(Math.E, -HuAv)+1)) - 1/2));;
			}
		} 
				
		//Compute following likelihood
		for (int i=0; i<currUser.followings.length; i++){
			//Only compute likelihood of followings which are in training batch (i.e. batch = 1)
			if (currUser.followingBatches[i]==1){
				int v = currUser.followings[i];
				User following = dataset.users[v];
						
				//Compute H_u * A_v
				double HuAv = 0;
				for (int j=0; j<currUser.hubs.length; j++){
					for (int z=0; z<following.authorities.length; z++){
						if (j==z){
							HuAv += currUser.hubs[j] * following.authorities[z];
						}
					}
				}
				followingLikelihood += Math.log(1-(2*((1/(Math.pow(Math.E, -HuAv)+1)) - 1/2))) ; ;
			}
		}

		//Compute post likelihood
		for (int k=0; k<nTopics; k++){
			postLikelihood += Math.pow(Math.log(currUser.hubs[k])-x[k], 2)/(2*Math.pow(delta, 2));
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
		
		//Set the current user to be u
		User currUser = dataset.users[u];

		//Compute non following likelihood
		for (int i=0; i<currUser.nonFollowings.length; i++){
			//Only compute likelihood of non followings which are in training batch (i.e. batch = 1)
			if (currUser.nonFollowingBatches[i]==1){
				int v = currUser.nonFollowings[i];
				User nonFollowing = dataset.users[v];
				
				//Compute H_u * A_v
				double HuAv = 0;
				for (int j=0; j<currUser.hubs.length; j++){
					for (int z=0; z<nonFollowing.authorities.length; z++){
						if (j==z){
							HuAv += currUser.hubs[j] * nonFollowing.authorities[z];
						}
					}
				}
				nonFollowingLikelihood += ((1/(1-Math.pow(Math.E, -HuAv))) * (-Math.pow(Math.E, -HuAv)) * (-nonFollowing.authorities[k]))
						- ((1/(Math.pow(Math.E, -HuAv)+1)) * (-Math.pow(Math.E, -HuAv)) * (-nonFollowing.authorities[k]));
			}
		} 
		
		//Compute following likelihood
		for (int i=0; i<currUser.followings.length; i++){
			//Only compute likelihood of followings which are in training batch (i.e. batch = 1)
			if (currUser.followingBatches[i]==1){
				int v = currUser.followings[i];
				User following = dataset.users[v];
				
				//Compute H_u * A_v
				double HuAv = 0;
				for (int j=0; j<currUser.hubs.length; j++){
					for (int z=0; z<following.authorities.length; z++){
						if (j==z){
							HuAv += currUser.hubs[j] * following.authorities[z];
						}
					}
				}
				followingLikelihood += -following.authorities[k] - ((1/(Math.pow(Math.E, -HuAv)+1)) * (Math.pow(Math.E, -HuAv)) * (-following.authorities[k])) ;
			}
		}

		postLikelihood = ((Math.log(currUser.hubs[k])-x)/Math.pow(delta, 2)) * (1/currUser.hubs[k]);
		
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
		double currentF = getLikelihood_hub(u, currentX);

		for (int iter = 0; iter < maxIteration_Hubs; iter++) {
			for (int k = 0; k < nTopics; k++) {
				grad[k] = gradLikelihood_hub(u, k, currentX[k]);
				x[k] = currentX[k] - learning_rate_hubs * grad[k];
			}
			double f = getLikelihood_hub(u, x);
			if (f < currentF) {
				currentF = f;
				for (int k = 0; k < nTopics; k++) {
					currentX[k] = x[k];
				}
			}
		}
	}

	/***
	 * alternating step to optimize topics' word distribution
	 */
	private void altOptimize_topics() {

	}

	/***
	 * to sample topic for post n of user u
	 * 
	 * @param u
	 * @param n
	 */
	private void sampleTopic(int u, int n) {

	}

	/***
	 * modeling learning
	 */
	public void train() {
		for (int iter = 0; iter < max_GibbsEM_Iterations; iter++) {
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
			// Gibss part
			for (int u = 0; u < dataset.nUsers; u++) {
				for (int n = 0; n < dataset.users[u].nPosts; n++) {
					sampleTopic(u, n);
				}
			}
			// tracking
			System.out.printf("likelihood after %d steps: %f", iter, getLikelihood());
		}
	}
}
