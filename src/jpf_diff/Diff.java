package jpf_diff;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;


class Path {
	static String[] prop = {"./script/patch/precise/Example01.jpf",
							"./script/new/precise/Example01.jpf"};
	static String[] dot = {"./dotFiles/patch/precise/Example01.dot",
							"./dotFiles/new/precise/Example01.dot"};
}

class impactSet {
	ArrayList<Integer> condLoc;
	ArrayList<Integer> writeLoc;
	ArrayList<Integer> changeLoc;
	ArrayList<Integer> allLoc;
	public impactSet() {
		// TODO Auto-generated constructor stub
		condLoc = new ArrayList<Integer>();
		writeLoc = new ArrayList<Integer>();
		changeLoc = new ArrayList<Integer>();
		allLoc = new ArrayList<Integer>();;
	}
}

public class Diff {
	void analyzeDot(String path, impactSet now) throws IOException {
		File diff = new File(path);
		BufferedReader reader = new BufferedReader(new FileReader(diff));
		String line;
		String file = "";
		while((line = reader.readLine()) != null) {
			file += line + "\n";
		}
		reader.close();
		String[] text = file.split("\n");
		for (String string : text) {
			if (string.contains("color")) {
				String label = string.substring(string.indexOf(")") + 1, string.lastIndexOf("\""));
				String[] nums = label.split("-");
				Integer begin = new Integer(nums[0]);
				Integer end = new Integer(nums[1]);
				if (string.contains("**")) {
					for (int i = begin; i <= end; i++) {
						now.changeLoc.add(i);
						now.allLoc.add(i);
					}
				}
				else if (string.contains("red")) {
					for (int i = begin; i <= end; i++) {
						now.condLoc.add(i);
						now.allLoc.add(i);
					}	
				}
				else {
					for (int i = begin; i <= end; i++) {
						now.writeLoc.add(i);
						now.allLoc.add(i);
					}
				}			
			}
		}
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		Diff diff = new Diff();
		impactSet s = new impactSet();
		impactSet s1 = new impactSet();
		try {
			diff.analyzeDot(Path.dot[0], s);
			diff.analyzeDot(Path.dot[1], s1);
			for (int i = 0; i < s.allLoc.size(); i++) {
				for (int j = 0; j < s1.allLoc.size(); j++) {
					if (s.allLoc.get(i) == s1.allLoc.get(j)) {
						System.out.println("impact set cross at:" + s.allLoc.get(i));
					}
				}
			}
			System.out.println("no cross over!");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
