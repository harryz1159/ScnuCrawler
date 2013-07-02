package com.hjh;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.util.Bytes;
import com.hjh.inf.OperateHbase;

public class TestHbase 
{

	 public static void main (String[] args)throws IOException
	  {
		  
		   //以下被注释掉的代码都是我开始用来测试与Hbase的连接情况写的
		  HBaseFactory hf=new HBaseFactory();
		  OperateHbase oh=hf.getInstance();
		  String tableName="table20121128";
		  
		  //noted by Oscar Pang at 2012-11-29,主要是测试中文显示是够正确
		  /*
		  Result resultData = oh.query(tableName, "row20121128_003");
		  
     	  for(KeyValue kv:resultData.raw()){
		        System.out.println(new String(kv.getValue()));
		    }
		    */
     	  
		  //Noted by Oscar Pang at 2012-11-29
		  /*
		  oh.dropTable(tableName);		  
		  oh.createTable(tableName);
		  */
		  
		  HTable ht= new HTable(GetHbaseConfig.getHbaseConfig(),tableName);
		 
		  //Noted by Oscar Pang at 2012-11-29,主要是测试数据的批量插入
		  /*
		  ht.setAutoFlush(false);		  
		 //要存入的数据的row
		 for (int i=0;i<100000;i++) {

			 String rowID= "row20121129"+Integer.toString(i);
			 String column = "userno";
			 String columnFamily = "create";
			 String Value = "PeterPang"+Integer.toString(i);
			 oh.insertData(ht,tableName, rowID, columnFamily, column, Value);
		 }
		 
		 //没有下面的两条语句会发生很多莫名奇妙的错误
		 ht.clearRegionCache();
		 ht.close();
		 
		 */
	  }	
  
  @SuppressWarnings("unchecked")
private static Map transefer(Map<String ,String> data)
  {
	  Map m = new HashMap<String , byte[]>();
	  for(Object s:data.keySet())
	  {	
		  m.put(s, Bytes.toBytes(data.get(s)));
	  }
	  return m;
  	
  }
  
  @SuppressWarnings("unused")
  private List<Map<String , String>> toGet(List<Map<String , String>> data)
  {
	  return data;
  }
  
  @SuppressWarnings("unchecked")
  public  static  void doHbase(List<Map<String , String>> data)
  {
	  //创建HBase的实现实例
	  HBaseFactory hf=new HBaseFactory();
	  OperateHbase oh=hf.getInstance();
	  
	  //创建HBase Table
	  oh.createTable("victor");
	  
	  //写入数据
	  List<Map<String , byte[]>> realData = new ArrayList<Map<String , byte[]>>();
	  for(Map<String ,String> m:data)
		  realData.add(transefer(m));
	  
	 oh.insertAll("victor", realData);
	}
}
