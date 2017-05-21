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
		// to be written
		return 0;
	}

	/***
	 * compute gradient of likelihood of data with respect to interest of u in
	 * topic k when the interest is x, i.e., if if L(data|parameters) =
	 * f(theta_u) + const-of-theta_u then this function return df/dtheta_uk at
	 * theta_uk = x
	 * 
	 * @param u
	 * @param x
	 * @param k
	 * @return
	 */
	private double gradLikelihood_topicalInterest(int u, double x, int k) {
		// to be written
		return 0;
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
				grad[k] = gradLikelihood_topicalInterest(u, currentX[k], k);
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
	 * @param u
	 * @return
	 */
	private double getLikelihood_authority(int u, double[] x) {
		// to be written
		return 0;
	}

	/***
	 * compute gradient of likelihood of data with respect to authority of u in
	 * topic k when the authority is x, i.e., if if L(data|parameters) = f(A_u)
	 * + const-of-A_u then this function return df/dA_uk at A_uk = x
	 * 
	 * @param u
	 * @param x
	 * @param k
	 * @return
	 */
	private double gradLikelihood_authority(int u, double x, int k) {
		// to be written
		return 0;
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
				grad[k] = gradLikelihood_authority(u, currentX[k], k);
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
	 * @return
	 */
	private double getLikelihood_hub(int u, double[] x) {
		// to be written
		return 0;
	}

	/***
	 * compute gradient of likelihood of data with respect to hub of u in topic
	 * k when the hub is x, i.e., if if L(data|parameters) = f(H_u) +
	 * const-of-H_u then this function return df/dH_uk at H_uk = x
	 * 
	 * @param u
	 * @param x
	 * @param k
	 * @return
	 */
	private double gradLikelihood_hub(int u, double x, int k) {
		// to be written
		return 0;
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
				grad[k] = gradLikelihood_hub(u, currentX[k], k);
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
