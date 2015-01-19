package gov.nasa.jpf;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Config extends Properties {

	// do we want to log the config init
	public static boolean log = false;
	// where did we initialize from
	ArrayList<Object> sources = new ArrayList<Object>();

	public static Config createConfig() {
		return new Config();
	}

	// bad - a control exception
	static class MissingRequiredKeyException extends RuntimeException {
		MissingRequiredKeyException(String details) {
			super(details);
		}
	}

	private Config() {
		// just interal, for reloading
	}

	/**
	 * single source Config constructor (does not process stack)
	 * 
	 * @param fileName
	 *            - single properties filename to initialize from
	 */
	public Config(String fileName) {
		loadProperties(fileName);
	}

	protected boolean loadProperties(String fileName) {
		if (fileName != null && fileName.length() > 0) {
			FileInputStream is = null;
			try {
				File f = new File(fileName);
				if (f.isFile()) {
					log("loading property file: " + fileName);

					setConfigPathProperties(f.getAbsolutePath());
					sources.add(f);
					is = new FileInputStream(f);
					load(is);
					return true;
				} else {
					throw exception("property file does not exist: "
							+ f.getAbsolutePath());
				}
			} catch (MissingRequiredKeyException rkx) {
				// Hmpff - control exception
				log("missing required key: " + rkx.getMessage()
						+ ", skipping: " + fileName);
			} catch (IOException iex) {
				throw exception("error reading properties: " + fileName);
			} finally {
				if (is != null) {
					try {
						is.close();
					} catch (IOException iox1) {
						log("error closing input stream for file: " + fileName);
					}
				}
			}
		}

		return false;
	}

	protected void setConfigPathProperties(String fileName) {
		put("config", fileName);
		int i = fileName.lastIndexOf(File.separatorChar);
		if (i >= 0) {
			put("config_path", fileName.substring(0, i));
		} else {
			put("config_path", ".");
		}
	}

	public JPFException exception(String msg) {
		String context = getString("config");
		if (context != null) {
			msg = "error in " + context + " : " + msg;
		}

		return new JPFConfigException(msg);
	}

	public void setTarget(String clsName) {
		put("target", clsName);
	}

	public static void enableLogging(boolean enableLogging) {
		log = enableLogging;
	}

	public void log(String msg) {
		if (log) { // very simplisitc, but we might do more in the future
			System.out.println(msg);
		}
	}

	public String getString(String key) {
		return getProperty(key);
	}

	@Override
	public Object setProperty(String key, String newValue) {
		Object oldValue = put(key, newValue);
		return oldValue;
	}
}
