package tree_diff;

public class test_v1 {
	void test(String name) {
		String n = name;
	}
	public static void main(String[] args) {
		int x = 5;
		int y = x + 3;
		if (y > 0) {
			y++;
		}
		else {
			x++;
		}
		test_v2 p = new test_v2();
		p.test("test");
	}
}
