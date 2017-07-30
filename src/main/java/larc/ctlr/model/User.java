package larc.ctlr.model;

public class User {
	public int userIndex;
	public String userId;
	public String username;

	public int nPosts;
	public Post[] posts;
	public int[] postBatches;// batch index of posts, to be used for K-fold cross validation

	public int[] followings;// index of following
	public int[] followingBatches;// batch index of followings, to be used for K-fold cross validation
	public int[] nonFollowings;// index of non_followings
	public int[] nonFollowingBatches;// batch index of non_followings, to be used for K-fold cross validation

	public int[] followers;// index of followers
	public int[] nonFollowers;// index of non_followers

	public double[] topicalInterests;// theta, K-topics dimension
	public double[] authorities;// A, K-topics dimension
	public double[] hubs;// H, K-topics dimension
}
