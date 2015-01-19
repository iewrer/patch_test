package tree_diff;

public class test_v2 {
	static String test(String name) {
		String n = name;
		return n;
	}
	public static void main(String[] args) {
		int x = 0;
		int y = x - 3;
		if (y > 0) {
			y++;
		}
		else {
			x++;
		}
		int z = x + y;
		test("name");
	}
}
