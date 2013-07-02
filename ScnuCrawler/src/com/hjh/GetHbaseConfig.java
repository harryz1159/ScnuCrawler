package com.hjh;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.conf.Configuration;

/**
 * 
 * @author hadoop
 * Function: get the configuration file of hbase 
 *
 */

/*
 * HBaseConfiguration是每一个hbase client都会使用到的对象，它代表的是HBase配置信息。它有两种构造方式：
public HBaseConfiguration()
public HBaseConfiguration(final Configuration c)

默认的构造方式会尝试从hbase-default.xml和hbase-site.xml中读取配置。如果classpath没有这两个文件，就需要你自己设置配置。
Configuration HBASE_CONFIG = new Configuration();
HBASE_CONFIG.set(“hbase.zookeeper.quorum”, “zkServer”);
HBASE_CONFIG.set(“hbase.zookeeper.property.clientPort”, “2181″);
HBaseConfiguration cfg = new HBaseConfiguration(HBASE_CONFIG);
 */

public class GetHbaseConfig
{
	
     private static  class GetHbaseConfigHolder
     {
		@SuppressWarnings("deprecation")
		private static Configuration hbaseConfig=new HBaseConfiguration().create();
     }
     
     public static Configuration getHbaseConfig()
     {
    	 return GetHbaseConfigHolder.hbaseConfig;
     }
}
