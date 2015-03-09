package imperial.modaclouds.fg.modelUpdater;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


public class ResourceMapping {
public static Map<String,String> resourceMap;
	
	public ResourceMapping(String fileName) {
		resourceMap = new HashMap<String,String>();
		
		Properties prop = new Properties();
		InputStream input = null;
		
		try {		 
			input = new FileInputStream(fileName);
			prop.load(input);
			
			for (String key : prop.stringPropertyNames()) {
			    String value = prop.getProperty(key);
			    resourceMap.put(key, value);
			}
	 
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
