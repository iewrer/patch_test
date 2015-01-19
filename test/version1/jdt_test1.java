public class DefaultCodeFormatter extends CodeFormatter { 
 	public DefaultCodeFormatter(DefaultCodeFormatterOptions defaultCodeFormatterOptions, Map options) {
		if (options == null) {
 			this.options = JavaCore.getOptions();
 			this.preferences = new DefaultCodeFormatterOptions(DefaultCodeFormatterConstants.getJavaConventionsSettings());
			setDefaultCompilerOptions();
		} else {
			setOptions(options);
 		}
 		if (defaultCodeFormatterOptions != null) {
 			this.preferences.set(defaultCodeFormatterOptions.getMap());
 		}
 	}
 }
 