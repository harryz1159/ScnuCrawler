package com.microblog.common.dao.impl.hbase;
/**
 * HBase的Dao实现
 * 适用于POJO类com.microblog.common.datastructure.MicroblogData
 * 
 */
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
import com.microblog.common.dao.IMicroblogDataDao;
import com.microblog.common.model.MicroblogData;
public class MicroblogDataDaoImpl implements IMicroblogDataDao{
	private static Configuration conf;
	
	/**
	 * 初始化HBase配置
	 */
	//@Override
	/*public void initialize()
	{
		conf = HBaseConfiguration.create();
		conf.set("hbase.zookeeper.quorum", "127.0.0.1");
	}*/
	static {
		conf = HBaseConfiguration.create();		//获得默认配置 请先在eclipse的classpath变量中添加hbase-site.xml文件路径
	}
	
	/**
	 * 判断指定名称的表是否存在
	 * */
	public boolean isTableExists(String tableName)
	{
		try {
			HBaseAdmin admin = new HBaseAdmin(conf);
			if(admin.tableExists(tableName) == true)
			{
				return true;
			}
			admin.close();
		} catch (MasterNotRunningException | ZooKeeperConnectionException e) {
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * 创建HBase表
	 * 包括3个列簇：身份（identify：用户名、ID等信息）、
	 *            内容（content：文字、图片等信息）、
	 *            时间（time：数据收集时间、微博创建时间等信息）
	 */
	@Override
	public void createTable(String tableName)
	{
		//HTable table = null;
		try
		{
			HBaseAdmin admin = new HBaseAdmin(conf);
			
			/*if(admin.tableExists("table1"))
			{//表存在则删除之
				admin.disableTable("table1");
				admin.deleteTable("table1");
			}*/

			if(admin.tableExists(tableName) == true)
			{
				System.out.println("表" + tableName + "已存在！");
			}
			else
			{
				HTableDescriptor tableDescripter = new HTableDescriptor(tableName.getBytes());
				//列簇名会按首字母顺序排列，与添加顺序无关
				//tableDescripter.addFamily(new HColumnDescriptor("identify"));
				//tableDescripter.addFamily(new HColumnDescriptor("content"));
				//tableDescripter.addFamily(new HColumnDescriptor("time"));
				tableDescripter.addFamily(new HColumnDescriptor("mainInfo"));
				tableDescripter.addFamily(new HColumnDescriptor("otherInfo"));
				admin.createTable(tableDescripter);
			}
			//table = new HTable(conf, tableName);
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
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
		//return table;
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
	 * 
	 * */
	private long SimpleDate2TimeStamp(String simpleDate)
	{
		long timeStamp = 0;
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			Date date = df.parse(simpleDate);
			timeStamp = date.getTime();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return timeStamp;
	}
	
	/**
	 * 向HBase表中添加批量数据
	 */
	@Override
	public void addData(ArrayList<MicroblogData> statusesList, HTable table)
	{
		//table.setAutoFlush(autoFlush);
		if(statusesList == null || statusesList.size() <= 0)
		{//没有数据则退出函数
			return;
		}
		List<Put> putrows = new ArrayList<Put>();
		System.out.println("正准备添加数据。。。");
		System.out.println("正在向表中写入数据。。。");
		for(MicroblogData mbd : statusesList)
		{
			long relativeTime = Long.MAX_VALUE - SimpleDate2TimeStamp(mbd.getCreatTime()); 	//某个未来相对于创建时间的时间间隔
			String rowKey = mbd.getUserID() + Long.toString(relativeTime);
			Put put = new Put(rowKey.getBytes()); 	//以用户名+微博创建时到Long.Max相对数作为行关键字
			/*
			 * 用户身份信息
			 */
			put.add("mainInfo".getBytes(), "userID".getBytes(), mbd.getUserID().getBytes());
			put.add("mainInfo".getBytes(), "userName".getBytes(), mbd.getUserName().getBytes());
			put.add("mainInfo".getBytes(), "nickName".getBytes(), mbd.getNickName().getBytes());
			/*
			 * 微博内容
			 */
			put.add("mainInfo".getBytes(), "text".getBytes(), mbd.getText().getBytes());
			put.add("mainInfo".getBytes(), "picSrc".getBytes(), mbd.getPicSrc().getBytes());
			put.add("mainInfo".getBytes(), "microblogID".getBytes(), mbd.getMicroblogID().getBytes());
			/*
			 * 时间信息
			 */
			put.add("mainInfo".getBytes(), "creatTime".getBytes(), mbd.getCreatTime().getBytes());
			put.add("mainInfo".getBytes(), "collectTime".getBytes(), mbd.getCollectTime().getBytes());
			/*
			 * 其他
			 */
			put.add("otherInfo".getBytes(), "repostsCount".getBytes(), mbd.getRepostsCount().getBytes());
			put.add("otherInfo".getBytes(), "commentsCount".getBytes(), mbd.getCommentsCount().getBytes());
			put.add("otherInfo".getBytes(), "type".getBytes(), mbd.getType().getBytes());
			put.add("otherInfo".getBytes(), "sourceId".getBytes(), mbd.getSourceId().getBytes());
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
	}
	
	/**
	 * 根据表名向HBase表中添加批量数据
	 */
	public void addData(ArrayList<MicroblogData> statusesList, String tableName)
	{
		try {
			HBaseAdmin admin = new HBaseAdmin(conf);
			if(admin.tableExists(tableName) == false)
			{
				System.out.println("表" + tableName + "不存在！");
			}
			else
			{
				HTable hTable = new HTable(conf, tableName);
				addData(statusesList, hTable);
				//hTable.close(); 	//凡是有创建HTable类型对象并且不返回HTable类型的方法都必须在方法体中调用HTable的close方法。
			}
			admin.close();
		} catch (MasterNotRunningException | ZooKeeperConnectionException e) {
			// TODO Auto-generated catch block
			System.out.println("Master节点未运行或ZooKeeper链接发生错误！\n请检查集群配置是否正确。");
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e)
		{
			System.out.println("IO错误！");
			try {
				Thread.sleep(3 * 1000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
	}

}
