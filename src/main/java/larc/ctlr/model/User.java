package larc.ctlr.model;

public class User {
	public int userIndex;
	public String userId;

	public int nPosts;
	public Post[] posts;
	public int[] postBatches;// batch index of posts, to be used for K-fold cross validation

	public int[] followees;// index of followees
	public int[] followeeBatches;// batch index of followees, to be used for K-fold cross validation
	public int[] nonFollowees;// index of non_followees
	public int[] nonFolloweeBatches;// batch index of non_followees, to be used for K-fold cross validation

	public int[] followers;// index of followers
	public int[] nonFollowers;// index of non_followers

	public double[] topicalInterests;// theta, K-topics dimension
	public double[] authorities;// A, K-topics dimension
	public double[] hubs;// H, K-topics dimension
}
