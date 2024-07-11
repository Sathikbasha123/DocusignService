package com.saaspe.docusign.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertyFileSelector {
    private static final Logger logger = LoggerFactory.getLogger(PropertyFileSelector.class);


	PropertyFileSelector() {}
	
	public static Map<String, String> getHeaderValueFromAPI(String headerValue) {
		Map<String, String> map = new HashMap<>();
		String propertiesFileName = mapHeaderValueToPropertiesFile(headerValue);
		String propertiesFilePath = "src/main/resources/" + propertiesFileName;
		Properties properties = loadPropertiesFile(propertiesFilePath);
		String userId = properties.getProperty("userId");
		String clientId = properties.getProperty("clientId");
		String rsaKeyFile = properties.getProperty("rsaKeyFile");
		map.put("userId", userId);
		map.put("clientId", clientId);
		map.put("rsaKeyFile", rsaKeyFile);
        logger.info("Value from properties file: {}", map);
		return map;
	}

	public static String mapHeaderValueToPropertiesFile(String headerValue) {
		if (headerValue.equals("test")) {
			return "application-test.properties";
		} else if (headerValue.equals("dev")) {
			return "application-dev.properties";
		} else if (headerValue.equals("sit")) {
			return "application-sit.properties";
		} else if (headerValue.equals("jnt")) {
			return "application-jnt.properties";
		}
		return null;
	}

	public static Properties loadPropertiesFile(String filePath) {
		Properties properties = new Properties();
		try (InputStream inputStream = new FileInputStream(filePath)) {
			properties.load(inputStream);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return properties;
	}
}
