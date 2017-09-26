package larc.ctlr.model;

public class Configure {
	public static enum ModelMode {
		TWITTER_LDA, // single topic per post/document
		ORIGINAL_LDA,// each word has its own topic
	}

	public static enum PredictionMode {
		CTLR, // Using u's hub and v's authority
		CTR, // Collaborative topic regression model
		WTFW, // KDD2014,Who to follow and Why
		COMMON_INTEREST, // Using u and v topical interests
		COMMON_NEIGHBOR, // Using Jaccard Coefficient of u and v common neighborhood
		HITS,
	}
}
