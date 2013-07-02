package com.scnu.crawler.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class CrawlerConfig {
	public CrawlerConfig(){}
	private static Properties props = new Properties();
	static{
		try {
			//props.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("crawlerConfig.properties"));
			props.load(new FileInputStream("crawlerConfig.properties"));	//该文件在工程根目录下
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String getValue(String key){
		return props.getProperty(key);
	}
	//public static int getValue(String key)
	//{
	//	return props.getProperty(key);
	//}

    public static void updateProperties(String key,String value) {    
    	props.setProperty(key, value); 
    	try {
			props.store(new FileOutputStream("crawlerConfig.properties"), "Update '" + key + "' value");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    } 
}
