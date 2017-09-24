package larc.ctlr.evaluation;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

import larc.ctlr.model.Dataset;

public class ReformatData {
	public static void reformatCTR(String dataPath, String outputPath) {
		try {
			Dataset dataset = new Dataset(dataPath);
			BufferedWriter bw = new BufferedWriter(new FileWriter(String.format("%s/user_adoptions.txt", outputPath)));
			for (int u = 0; u < dataset.nUsers; u++) {
				int nItems = 0;
				for (int i = 0; i < dataset.users[u].nFollowings; i++) {
					if (dataset.users[u].followingBatches[i] == 1) {
						nItems++;
					}
				}
				bw.write(String.format("%d", nItems));

				for (int i = 0; i < dataset.users[u].nFollowings; i++) {
					if (dataset.users[u].followingBatches[i] == 1) {
						bw.write(String.format(" %d", dataset.users[u].followings[i]));
					}
				}
				bw.write("\n");
			}
			bw.close();

			bw = new BufferedWriter(new FileWriter(String.format("%s/user_index_id.txt", outputPath)));
			for (int u = 0; u < dataset.nUsers; u++) {
				bw.write(String.format("%d,%s\n", u, dataset.users[u].userId));
			}
			bw.close();

			bw = new BufferedWriter(new FileWriter(String.format("%s/item_adopters.txt", outputPath)));
			for (int u = 0; u < dataset.nUsers; u++) {
				int nAdopters = 0;
				for (int i = 0; i < dataset.users[u].nFollowers; i++) {
					if (dataset.users[u].followerBatches[i] == 1) {
						nAdopters++;
					}
				}
				bw.write(String.format("%d", nAdopters));

				for (int i = 0; i < dataset.users[u].nFollowers; i++) {
					if (dataset.users[u].followerBatches[i] == 1) {
						bw.write(String.format(" %d", dataset.users[u].followers[i]));
					}
				}
				bw.write("\n");
			}
			bw.close();

			bw = new BufferedWriter(new FileWriter(String.format("%s/item_content.txt", outputPath)));
			for (int u = 0; u < dataset.nUsers; u++) {
				HashMap<Integer, Integer> words = new HashMap<Integer, Integer>();
				for (int i = 0; i < dataset.users[u].nPosts; i++) {
					if (dataset.users[u].postBatches[i] != 1) {
						continue;
					}
					for (int j = 0; j < dataset.users[u].posts[i].nWords; j++) {
						int w = dataset.users[u].posts[i].words[j];
						if (words.containsKey(w)) {
							words.put(w, 1 + words.get(w));
						} else {
							words.put(w, 1);
						}
					}
				}
				bw.write(String.format("%d", words.size()));
				for (Map.Entry<Integer, Integer> word : words.entrySet()) {
					bw.write(String.format(" %d:%d", word.getKey(), word.getValue()));
				}
				bw.write("\n");
			}
			bw.close();

			bw = new BufferedWriter(new FileWriter(String.format("%s/item_index_id.txt", outputPath)));
			for (int u = 0; u < dataset.nUsers; u++) {
				bw.write(String.format("%d,%s\n", u, dataset.users[u].userId));
			}
			bw.close();

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public static void main(String[] args) {
		reformatCTR("E:/code/java/ctlr/data/acmpro50", "E:/code/java/ctlr/data/acmpro50/ctr");
	}
}
