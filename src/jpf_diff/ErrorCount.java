package jpf_diff;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class ErrorCount {
	
	public String xml_not_found = "xml_not_found";
	public String class_not_found = "class_not_found";
	public String interface_class = "interface_class";
	public String null_class = "null_class";
	
	String className;
	String methodName;
	
	public Set<String> precise_analysis; //method name -> precise analysis error
	public Map<String, String> class_error; //class name -> class error

	public ErrorCount(String className) {
		// TODO Auto-generated constructor stub
		precise_analysis = new HashSet<String>();
		class_error = new HashMap<>();
		this.className = className;
	}

	public String print() {
		// TODO Auto-generated method stub
		String res = "";
		System.err.println("className: " + className);
		res += "className: " + className + "\n";
		System.err.println("precise_analysis: " + precise_analysis.size());
		res += "precise_analysis: " + precise_analysis.size() + "\n";
		System.err.println("class errors: " + class_error.size());
		res += "class errors: " + class_error.size() + "\n";
		Iterator<String> iterator = class_error.keySet().iterator();
		while (iterator.hasNext()) {
			String key = (String) iterator.next();
			String value = class_error.get(key);
			System.err.println("	" + key + "," + value);
			res += "	" + key + "," + value + "\n";
		}
		return res;
	}
}
