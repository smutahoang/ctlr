package larc.ctlr.model;

import java.io.File;
import java.io.FileWriter;

public class HITS {
	public static String path;
	private static Dataset dataset;
	public String ouput_path;
	public static int[][] aInMatrix;
	public static int[][] aOutMatrix;
	public static float[] authorities;
	public static float[] hubs;
	public static int nUsers;

	public HITS(String _path, int _batch) {
		path = _path;
		HITS.dataset = new Dataset(_path, _batch,false);
		nUsers = dataset.nUsers;
		init();
		updateAuthorityHub(5);
		output_HITS();
	}

	public static void init() {
		aInMatrix = new int[nUsers][nUsers];
		aOutMatrix = new int[nUsers][nUsers];
		authorities = new float[nUsers];
		hubs = new float[dataset.nUsers];
		for (int i = 0; i < nUsers; i++) {
			authorities[i] = 0.0001f;
			hubs[i] = 0.0001f;
			for (int j = 0; j < nUsers; j++) {
				aInMatrix[i][j] = 0;
				aOutMatrix[i][j] = 0;
			}
		}
		for (int u = 0; u < nUsers; u++) {
			User currUser = dataset.users[u];
			for (int i = 0; i < currUser.followers.length; i++) {
				if (currUser.followerBatches[i] == 1) {
					int in_link_index = currUser.followers[i];
					aInMatrix[u][in_link_index] = 1;
				}
			}
			for (int i = 0; i < currUser.followings.length; i++) {
				if (currUser.followingBatches[i] == 1) {
					int out_link_index = currUser.followings[i];
					aOutMatrix[u][out_link_index] = 1;
				}
			}
		}
	}

	public static void updateAuthorityHub(int runs) {
		for (int r = 0; r < runs; r++) {
			for (int i = 0; i < nUsers; i++) {
				float authority = 0f;
				float hub = 0f;
				for (int j = 0; j < nUsers; j++) {
					authority += (float) aInMatrix[i][j] * hubs[i];
					hub += (float) aOutMatrix[i][j] * authorities[i];
				}
				authorities[i] = authority;
				hubs[i] = hub;
			}
		}
	}

	public static void output_HITS() {
		try {
			File f = new File(dataset.path + "/user_hits.csv");
			FileWriter fo = new FileWriter(f);
			for (int u = 0; u < nUsers; u++) {
				User currUser = dataset.users[u];
				String text = currUser.userId + "," + authorities[u] + "," + hubs[u];
				fo.write(text + "\n");
			}
			fo.close();
		} catch (Exception e) {
			System.out.println("Error in writing to topical word file!");
			e.printStackTrace();
			System.exit(0);
		}
	}

}
