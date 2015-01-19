public class DefaultCodeFormatter extends CodeFormatter { 
 	public DefaultCodeFormatter(DefaultCodeFormatterOptions defaultCodeFormatterOptions, Map options) {
		if (options != null) {
			this.options = options;
			this.preferences = new DefaultCodeFormatterOptions(options);
		} else {
			this.options = JavaCore.getOptions();
 			this.preferences = new DefaultCodeFormatterOptions(DefaultCodeFormatterConstants.getJavaConventionsSettings());
 		}
		this.defaultCompilerOptions = getDefaultCompilerOptions();
 		if (defaultCodeFormatterOptions != null) {
 			this.preferences.set(defaultCodeFormatterOptions.getMap());
 		}
 	}
 }
 