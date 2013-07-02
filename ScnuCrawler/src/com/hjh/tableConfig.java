package com.hjh;

/**
 * Hbase创建表中使用的配置文件
 * Author:auge pang
 * Date:2012-12-12
 */
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class tableConfig {
	private String configFileName = null;		//配置文件名称
	private  Properties props = new Properties();			
	
	//类的构造函数
	public tableConfig(){}
	public tableConfig(String lsFileName){
		configFileName = lsFileName;
		try {
			props.load(Thread.currentThread().getContextClassLoader().getResourceAsStream(configFileName));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public  String getValue(String key){
		return props.getProperty(key);
	}

    public void updateProperties(String key,String value) {    
            props.setProperty(key, value); 
    }

	public String getConfigFileName() {
		return configFileName;
	}

	public void setConfigFileName(String configFileName) {
		this.configFileName = configFileName;
	} 
}
