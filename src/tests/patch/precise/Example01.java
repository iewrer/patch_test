package precise;

public class Example01 {
	
	public void test(int a, int b, int c, int d, int e) {
		//assignment of b is different
		//based on the branch taken by
		// after the evaluation of 
		// (a == 0)
		if(a == 0) {
			b = (c+d);
		} else {
			b = e;
		} 
		//modified statement
		if(b >= 10) {
			b = b+1;
		}
	}
	
	public static void main(String[] args) {
		Example01 ex = new Example01();
		ex.test(0, 0, 0, 0, 0);
	}
	
}
