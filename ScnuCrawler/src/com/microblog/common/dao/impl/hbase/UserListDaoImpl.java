package com.microblog.common.dao.impl.hbase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.MasterNotRunningException;
import org.apache.hadoop.hbase.ZooKeeperConnectionException;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.PageFilter;
import org.apache.hadoop.hbase.util.Bytes;

import com.microblog.common.dao.IUserListDao;


public class UserListDaoImpl implements IUserListDao {
	private static Configuration conf;
	static {
		conf = HBaseConfiguration.create();		//获得默认配置 请先在eclipse的classpath变量中添加hbase-site.xml文件路径
	}
	/**
	 * 创建表
	 * 注意：表已经存在则先删除之
	 * */
	public HTable createTable(String tableName) {
		HTable table = null;
		try
		{
			HBaseAdmin admin = new HBaseAdmin(conf);

			if(admin.tableExists(tableName) == true)
			{
				System.out.println("表" + tableName + "已存在！将删除并重新创建！");
				admin.disableTable(tableName);
				admin.deleteTable(tableName);
			}
			HTableDescriptor tableDescripter = new HTableDescriptor(tableName.getBytes());
			//列簇名会按首字母顺序排列，与添加顺序无关
			tableDescripter.addFamily(new HColumnDescriptor("mainInfo"));
			tableDescripter.addFamily(new HColumnDescriptor("otherInfo"));
			admin.createTable(tableDescripter);
			
			table = new HTable(conf, tableName);
			System.out.println("表" + tableName + "创建成功！");
			admin.close();
		} catch(MasterNotRunningException | ZooKeeperConnectionException e)
		{
			System.out.println("Master节点未运行或ZooKeeper链接发生错误！\n请检查集群配置是否正确。");
			e.printStackTrace();
			System.exit(1);
		}
		catch(IOException e)
		{
			System.out.println("IO错误！");
			try {
				Thread.sleep(3 * 1000);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
		return table;
	}
	
	/** 
	 * 通过表名获取HTable
	 * 如果表不存在则返回null
	 * */
	public HTable getTableByName(String tableName)
	{
		HTable hTable = null;
		try {
			HBaseAdmin admin = new HBaseAdmin(conf);
			if(admin.tableExists(tableName) == false)
			{//表不存在则返回null
				hTable = null;
			}
			else
			{
				hTable = new HTable(conf, tableName);
			}
			admin.close();
		} catch (MasterNotRunningException | ZooKeeperConnectionException e) {
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		return hTable;
	}
	
	/**
	 * 批量向表中添加数据
	 * */
	public void appendData(ArrayList<String> userList, String tableName)
	{
		HTable table = getTableByName(tableName);
		if(table == null)
		{
			System.out.println("表名不存在！");
			System.exit(1);
		}
		//table.setAutoFlush(autoFlush);
		if(userList == null || userList.size() <= 0)
		{//没有数据则退出函数
			return;
		}
		List<Put> putrows = new ArrayList<Put>();
		System.out.println("正准备添加数据。。。");
		System.out.println("正在向表中写入数据。。。");
		for(String user : userList)
		{
			Put put = new Put(Bytes.toBytes(user)); 	
			put.add(Bytes.toBytes("mainInfo"), Bytes.toBytes("noValue"), Bytes.toBytes("")); 	//如果不写入列会抛出异常
			//p.setWriteToWAL(wal);
			putrows.add(put);
		}
		try {
			table.put(putrows);		//写入HBase表
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("IO错误！");
			try {
				Thread.sleep(3 * 1000);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
		System.out.println("数据添加完成。");
		//table.close();
	}
	
	/**
	 * 删除表
	 * */
	public void deleteTable(String tableName)
	{
		try {
			HBaseAdmin admin = new HBaseAdmin(conf);
			admin.disableTable(tableName);
			admin.deleteTable(tableName);
		} catch (MasterNotRunningException | ZooKeeperConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * 复制表内容
	 * @throws IOException 
	 * */
	public void copyContent(String sourceTable, String targetTable) throws IOException
	{
		//HTable table_source = new HTable(conf, sourceTable);
		//HTable table_target = new HTable(conf, targetTable);
		String startRow = "";
		ArrayList<String> rows;
		for(; (rows = getRows(sourceTable, startRow, 100000)).size() > 0; startRow = rows.get(rows.size() - 1))
		{
			appendData(rows, targetTable);
		}
	}
	
	/**
	 * 读取若干行
	 * 返回
	 * @throws IOException 
	 * */
	public ArrayList<String> getRows(String tableName, String startRow, int limitNum) throws IOException
	{
		HTable table = getTableByName(tableName);
		final byte[] POSTFIX = {0x00};
		if(table == null)
		{
			System.out.println("表名不存在！");
			System.exit(1);
		}
		Scan scan = new Scan();
		if(limitNum > 0)
		{
			Filter filter = new PageFilter(limitNum);
			scan.setFilter(filter);
		}
		if(!startRow.equals(""))
		{//注意这里添加了POSTFIX操作，不然死循环了
			byte[] startRowBytes = Bytes.add(Bytes.toBytes(startRow), POSTFIX); //在末尾添加0x00，则在表中找不到这个rowkey，因此会从最接近这个rowkey的下一个row开始
			scan.setStartRow(startRowBytes);
		}
		ArrayList<String> userList = new ArrayList<String>();
 		ResultScanner scanner = table.getScanner(scan);
		for(Result result; (result = scanner.next()) != null;)
		{
			userList.add(new String(result.getRow(), "UTF-8"));
		}
		scanner.close();
		return userList;
	}
	
}
