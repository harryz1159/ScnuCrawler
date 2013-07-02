package com.hjh;

/**
 * 实现OperateHbase接口，完成对hbase中数据的读写和存储
 * HBase在0.92之后引入了协处理器(coprocessors)，实现一些激动人心的新特性：能够轻易建立二次索引、复杂过滤器(谓词下推)以及访问控制等。
 * http://www.tinggeren.com/news/170009/?feed_sn=42&orderby=id
 */
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.MasterNotRunningException;
import org.apache.hadoop.hbase.ZooKeeperConnectionException;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import com.hjh.inf.OperateHbase;
import com.hjh.tableConfig;

public class OperateHbaseImp implements OperateHbase
{
	//get the configuration file of habse
	private Configuration hbaseConfig=GetHbaseConfig.getHbaseConfig();
	
	/**
	 *    get the HBaseAdmin,在hbase中，需要通过HBaseAdmin来创建和删除table
	 * @return	 HBaseAdmin对象
	 */
	private HBaseAdmin getHBaseAdmin()
	   {
		   HBaseAdmin hAdmin=null;
		   try {
			   //获取HBaseAdmin对象
				hAdmin = new HBaseAdmin(hbaseConfig);
			} catch (MasterNotRunningException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ZooKeeperConnectionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		   return hAdmin;
	   }
	
	/**
	 * Create Table
	 */
	public void createTable(String tableName)
	{
	   try {
		   //获取HBaseAdmin对象
		   HBaseAdmin hAdmin=getHBaseAdmin();
		   
		   //检查要创建的表是否存在
		   if(hAdmin.tableExists(tableName))
			   	return;
	
		   //从配置文件中读取表名和列族名
		   tableConfig newTableConfig = new tableConfig(tableName+".properties");
		   String newTableName=null, newCF1=null,newCF2=null;
		   newTableName = newTableConfig.getValue("tableName");
		   newCF1 = newTableConfig.getValue("columnFamily1");
		   newCF2 = newTableConfig.getValue("columnFamily2");
		   
		   //创建HTableDescriptor对象
		   HTableDescriptor t=new HTableDescriptor(newTableName);
		    //增加两个列族,create和reply
		   t.addFamily(new HColumnDescriptor(newCF1));
		   t.addFamily(new HColumnDescriptor(newCF2));
		   hAdmin.createTable(t);
		   
	   } catch (IOException e) {
		   // TODO Auto-generated catch block
		   e.printStackTrace();
	   }
   	}
   
	/**
	 * drop table 
	 */
	public void dropTable(String tableName)
	{
		//获取HBaseAdmin对象
		HBaseAdmin hAdmin=getHBaseAdmin();
		try {
			
			//如果该表存在则删除
			if(hAdmin.tableExists(tableName))
				{
					hAdmin.disableTable(tableName);
					hAdmin.deleteTable(tableName);
				}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 批量插入数据，参数含义参见接口定义文件,存在问题：
	 * 	(1) 没有考虑autoflash,如可以设置1000条记录提交一次
	 * 	(2) 没有考虑splitter
	 * 
	 */
	  /** put.add函数的原型
	   *  public Put add(byte [] family, byte [] qualifier, byte [] value) 
      * @param family family name				--列族名
	   * @param qualifier column qualifier	--列名
	   * @param value column value			--值
	   * @return this
	   * 
	   *    * */

	public void insertAll(String tableName,List<Map<String ,byte[]>> data)
	{
		//获取HBaseAdmin对象
	   HBaseAdmin hAdmin = getHBaseAdmin();
	   HTable ht = null;
	   List<Put> l = new ArrayList<Put>();
	   try
		   {
		   		//如果传入的表名不存在，则创建该表
			   	if(!hAdmin.tableExists(tableName))   createTable(tableName);
			   	
			   	//获取HTable对象并设置setAutoFlush标志，不设置可能会导致客户端连接过多从而使zooKeeper断开
			   	ht = new HTable(hbaseConfig,tableName);
			   	ht.setAutoFlush(false);
			   	
				for(Map<String , byte[]> m:data)
				{
					//创建put操作对象,其中使用weiboID作为rowkey
					Put p = new Put(m.get("weiboID"));
					for(String b: m.keySet())
					   {
						   if(b.equals("weiboID"))
							   continue;
						   if(b.startsWith("reply"))
							   //reply信息写如reply列簇中，列名为reply							   
							   p.add(Bytes.toBytes("reply"),Bytes.toBytes(b),m.get(b));
						   else
							   //create信息写如create列簇中，列名为create
							   p.add(Bytes.toBytes("create"),Bytes.toBytes(b),m.get(b));
					  // p.add(Bytes.toBytes("hjh"), Bytes.toBytes(column), Bytes.toBytes(data));
					   }
					   l.add(p);
				   }
				
				//向table写入数据
				 ht.put(l);
		   }
	   	catch(IOException e)
	   	{
				e.printStackTrace();
	   	}
	   	finally
	   	{
	   		try {
	   				//close htabl并清除缓冲区
	   				ht.clearRegionCache();
	   				ht.close();
	   		} catch (IOException e) {
	   			// TODO Auto-generated catch block
	   			e.printStackTrace();
	   		}
	   	}
	   
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.hjh.inf.OperateHbase#insert(java.lang.String, java.util.Map)
	 * 参数含义和相关说明同insertAll函数
	 */
	public void insert(String tableName, Map<String , byte[]> data)
	{
		HBaseAdmin hAdmin=getHBaseAdmin();
		HTable ht = null;
		try
		{
			if(!hAdmin.tableExists(tableName))    createTable(tableName);
			
			//获取HTable对象并设置setAutoFlush标志，不设置可能会导致客户端连接过多从而使zooKeeper断开
			ht=new HTable(hbaseConfig,tableName);
			ht.setAutoFlush(false);
			
			
			Put p=new Put(data.get("weiboID"));
			for(String b: data.keySet())
			{
				if(b.equals("weiboID"))
				   continue;
				if(b.startsWith("reply"))
					p.add(Bytes.toBytes("reply"),Bytes.toBytes(b),data.get(b));
				else
					p.add(Bytes.toBytes("create"),Bytes.toBytes(b),data.get(b));
		   }
		   ht.put(p);
	   }
	   catch(IOException e)
	   {
		   e.printStackTrace();
	   }
	   finally
	   {
		   try {
  				//close htabl并清除缓冲区
  				ht.clearRegionCache();
  				ht.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	   }
   }
   
	/*
	 * (non-Javadoc)
	 * @see com.hjh.inf.OperateHbase#delete(java.lang.String, java.lang.String, java.lang.String)
	 * tableName		表名
	 * row					行id
	 * column				列簇:列名
	 */
   public void delete(String tableName, String row, String column)
   {
	   HBaseAdmin hAdmin = getHBaseAdmin();
	   HTable ht = null;
	   try
	   {
		   //检查表是否存在
		   if(!hAdmin.tableExists(tableName))
		   {
			   System.out.println("The table you want to delete dose not exist");
			   return ;
		   }
		   
		   //创建htable对象
		   ht = new HTable(hbaseConfig, tableName);
		   
		   //根据row创建delete操作对象
		   Delete d=new Delete(Bytes.toBytes(row));
		   
		   //可以直接删除整行,可以删除制定列，也可以删除列簇
		   /**
		    *   Delete deleteColumns(byte [] family, byte [] qualifier) {
		    	* Delete all versions of the specified column.
		    		* 	@param family family name
		    		* @param qualifier column qualifier
		    		* @return this for invocation chaining
	    	*/
		   if(column.startsWith("reply"))
			   d.deleteColumns(Bytes.toBytes("reply"), Bytes.toBytes(column));
		   else
			   d.deleteColumns(Bytes.toBytes("create"),Bytes.toBytes(column));
		   
		   //删除数据
		   ht.delete(d);
	   } 
	   catch (IOException e) 
	   {
		   // TODO Auto-generated catch block
		   e.printStackTrace();
	   }
	   finally
	   {
		   try
		   {
			   ht.close();
		   }
		   catch(IOException ioe)
		   {
			   ioe.printStackTrace();
		   }
	   }
	   //end of function
   	}
   
   //根据行id查询单条数据，后期会有批量查询的
   /*
    * (non-Javadoc)
    * @see com.hjh.inf.OperateHbase#query(java.lang.String, java.lang.String)
    * tableName		表名
    * row						rowid
    */
   	public Result query(String tableName, String row)
   	{
   		Result r=null;
   		HTable ht = null;
   		try
   		{
   			ht=new HTable(hbaseConfig, tableName);
   			Get get=new Get(Bytes.toBytes(row));
   			r=ht.get(get);
   		}
   		catch(IOException e)
   		{
   			e.printStackTrace();
   		}
   		finally
   			{
   				try
   				{
   					ht.close();
   				} catch (IOException e) 
   			{
   					// TODO Auto-generated catch block
   					e.printStackTrace();
		   }
	   }
   		
	   return r;
   	}

   	/**
   	 * 增加数据
   	 * @param	 ht-----------------htable对象
   	 * @param tableName		-表名
   	 * @param rowID				--行ID
   	 * @param columnFamily	列簇
   	 * @param column				列名
   	 * @param value					 值
   	 */
	public void insertData(HTable ht, String tableName, String rowID, String columnFamily,String column,String Value)
	{
		try
		{
		
			//获取put操作对象
			Put p=new Put(Bytes.toBytes(rowID));
			p.add(Bytes.toBytes(columnFamily),Bytes.toBytes(column),Bytes.toBytes(Value));

		    ht.put(p);
		    
	   }
	   catch(IOException e)
	   {
		   e.printStackTrace();
	   }
   }

	@Override
	public HTable getHTable(String tableName) {
		// TODO Auto-generated method stub
	   HBaseAdmin hAdmin = getHBaseAdmin();
	   HTable ht = null;
	   try
	   {
		   //检查表是否存在
		   if(!hAdmin.tableExists(tableName))
		   {
			   System.out.println("The table you want to operate dose not exist");
		   }
		   
		   //创建htable对象
		   ht = new HTable(hbaseConfig, tableName);
	   }
	   catch (IOException e) 
	   {
		   // TODO Auto-generated catch block
		   e.printStackTrace();
	   }
	   return ht;		   
	}

	@Override
	public Result query(HTable lsHTable, String rowID) {
		// TODO Auto-generated method stub
   		Result resultList=null;		
  		try
   		{
   			Get get=new Get(Bytes.toBytes(rowID));
   			resultList=lsHTable.get(get);
   		}
   		catch(IOException e)
   		{
   			e.printStackTrace();
   		}  		
  		return resultList;
	}
   	
   	
//end of class   	
}
