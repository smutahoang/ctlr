package larc.ctlr.evaluation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

import larc.ctlr.model.Dataset;

public class ReformatData {
	public static void reformatCTR(String dataPath, int batch) {
		try {
			// create "ctr" directory inside dataPath directory
			File theDir = new File(String.format("%s/ctr", dataPath));
			// if the directory does not exist, create it
			if (!theDir.exists()) {
				System.out.println("creating directory: " + theDir.getAbsolutePath());
				try {
					theDir.mkdirs();
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(-1);
				}
			}
			// readin the dataset
			Dataset dataset = new Dataset(dataPath, batch,false);

			// reformatting

			// adoption file
			BufferedWriter bw = new BufferedWriter(
					new FileWriter(String.format("%s/ctr/user_adoptions.txt", dataPath)));
			for (int u = 0; u < dataset.nUsers; u++) {
				int nItems = 0;
				for (int i = 0; i < dataset.users[u].nFollowings; i++) {
					if (dataset.users[u].followingBatches[i] == batch) {
						nItems++;
					}
				}
				bw.write(String.format("%d", nItems));

				for (int i = 0; i < dataset.users[u].nFollowings; i++) {
					if (dataset.users[u].followingBatches[i] == batch) {
						bw.write(String.format(" %d", dataset.users[u].followings[i]));
					}
				}
				bw.write("\n");
			}
			bw.close();

			// users' index-2-id map file
			bw = new BufferedWriter(new FileWriter(String.format("%s/ctr/user_index_id.txt", dataPath)));
			for (int u = 0; u < dataset.nUsers; u++) {
				bw.write(String.format("%d,%s\n", u, dataset.users[u].userId));
			}
			bw.close();

			// adopter file
			bw = new BufferedWriter(new FileWriter(String.format("%s/ctr/item_adopters.txt", dataPath)));
			for (int u = 0; u < dataset.nUsers; u++) {
				int nAdopters = 0;
				for (int i = 0; i < dataset.users[u].nFollowers; i++) {
					if (dataset.users[u].followerBatches[i] == batch) {
						nAdopters++;
					}
				}
				bw.write(String.format("%d", nAdopters));

				for (int i = 0; i < dataset.users[u].nFollowers; i++) {
					if (dataset.users[u].followerBatches[i] == batch) {
						bw.write(String.format(" %d", dataset.users[u].followers[i]));
					}
				}
				bw.write("\n");
			}
			bw.close();

			// items' content file
			bw = new BufferedWriter(new FileWriter(String.format("%s/ctr/item_content.txt", dataPath)));
			for (int u = 0; u < dataset.nUsers; u++) {
				HashMap<Integer, Integer> words = new HashMap<Integer, Integer>();
				for (int i = 0; i < dataset.users[u].nPosts; i++) {
					if (dataset.users[u].postBatches[i] != batch) {
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

			// items' index-2-id map file
			bw = new BufferedWriter(new FileWriter(String.format("%s/ctr/item_index_id.txt", dataPath)));
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
		reformatCTR("E:/code/java/ctlr/data/acmpro50", 1);
	}
}
