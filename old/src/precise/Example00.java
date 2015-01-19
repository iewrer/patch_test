package precise;

public class Example00 {
	
	public void test (int w, int x, int y, int z) {
		//nested IADD 
		if((x + (y + z)) == 0) { // modify statement
			x = (y+z);
		} else {
			x = (y-z);
		}
		
		if(w == 18) {
			w = w + 2;
		} else {
			w = w + 1;
		}
		
		if(x >= y) {
			x++;
		} else {
			x--;
		}
		
	}
	
	public static void main(String[] args) {
		Example00 ex = new Example00();
		ex.test(0, 0, 0, 0);
	}
}
