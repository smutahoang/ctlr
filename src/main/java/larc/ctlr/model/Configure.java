package larc.ctlr.model;

public class Configure {
	public static enum ModelMode {
		TWITTER_LDA, // single topic per post/document
		ORIGINAL_LDA,// each word has its own topic
	}
}
