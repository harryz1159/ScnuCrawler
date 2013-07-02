package com.microblog.common.dao.impl.hbase;

import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.MasterNotRunningException;
import org.apache.hadoop.hbase.ZooKeeperConnectionException;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import com.microblog.common.dao.IMicroblogUserSimpleDao;
import com.microblog.common.model.MicroblogUser;

public class MicroblogUserSimpleDaoImpl implements IMicroblogUserSimpleDao {
	private static Configuration conf;
	static {
		conf = HBaseConfiguration.create();		//获得默认配置 请先在eclipse的classpath变量中添加hbase-site.xml文件路径
	}
	
	@Override
	public HTable createTable(String tableName) {
		HTable table = null;
		try
		{
			HBaseAdmin admin = new HBaseAdmin(conf);

			if(admin.tableExists(tableName) == true)
			{
				System.out.println("表" + tableName + "已存在！");
			}
			else
			{
				HTableDescriptor tableDescripter = new HTableDescriptor(tableName.getBytes());
				//列簇名会按首字母顺序排列，与添加顺序无关
				tableDescripter.addFamily(new HColumnDescriptor("mainInfo"));
				tableDescripter.addFamily(new HColumnDescriptor("otherInfo"));
				admin.createTable(tableDescripter);
			}
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
	 * 尽量不要使用这个重载，实例化HTable很费时间
	 * */
	@Override
	public void updateSimpleUserInfo(MicroblogUser user, String tableName, String microblogType) {
		if(user == null)
		{//参数不正确则退出函数
			return;
		}
		HTable hTable = null;
		try {
			HBaseAdmin admin = new HBaseAdmin(conf);
			if(admin.tableExists(tableName) == false)
			{
				System.out.println("表" + tableName + "不存在！");
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
		Put put = null;
		if(microblogType.equals("Sina"))
		{//微博类型为Sina则将用户ID作为行键
			put = new Put(user.getUserId().getBytes());
		}
		else if(microblogType.equals("Tencent"))
		{//微博类型为Tencent则将用户名作为行键
			put = new Put(user.getUserName().getBytes());
		}
		if(put == null)
		{
			System.out.println("请检查微博类型参数是否正确！");
			System.exit(1);
		}
		put.add("mainInfo".getBytes(), "sinceCreateTime".getBytes(), user.getSinceCreateTime().getBytes());
		put.add("mainInfo".getBytes(), "sinceCollectTime".getBytes(), user.getSinceCollectTime().getBytes());
		try {
			hTable.put(put);
		} catch (IOException e) {
			System.out.println("IO错误！");
			e.printStackTrace();
		}
	}

	public void updateSimpleUserInfo(MicroblogUser user, HTable hTable, String microblogType) {
		if(user == null)
		{//参数不正确则退出函数
			return;
		}

		Put put = null;
		if(microblogType.equals("Sina"))
		{//微博类型为Sina则将用户ID作为行键
			put = new Put(user.getUserId().getBytes());
		}
		else if(microblogType.equals("Tencent"))
		{//微博类型为Tencent则将用户名作为行键
			put = new Put(user.getUserName().getBytes());
		}
		if(put == null)
		{
			System.out.println("请检查微博类型参数是否正确！");
			System.exit(1);
		}
		put.add("mainInfo".getBytes(), "sinceCreateTime".getBytes(), user.getSinceCreateTime().getBytes());
		put.add("mainInfo".getBytes(), "sinceCollectTime".getBytes(), user.getSinceCollectTime().getBytes());
		try {
			hTable.put(put);
		} catch (IOException e) {
			System.out.println("IO错误！");
			e.printStackTrace();
		}
	}
	
	@Override
	public String getSinceCreateTime(MicroblogUser user, String tableName, String microblogType) {
		//若所查询的行不存在则返回“”
		if(user == null)
		{//参数不正确则退出函数
			return null;
		}
		HTable hTable = null;
		String sinceCreateTime = "";
		try {
			HBaseAdmin admin = new HBaseAdmin(conf);
			if(admin.tableExists(tableName) == false)
			{
				System.out.println("表" + tableName + "不存在！");
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
			System.out.println("IO错误！");
			e.printStackTrace();
		}
		Get get = null;
		if(microblogType.equals("Sina"))
		{//微博类型为Sina则通过用户ID来查找
			get = new Get(user.getUserId().getBytes());
		}
		else if(microblogType.equals("Tencent"))
		{//微博类型为Tencent则通过用户名来查找
			get = new Get(user.getUserName().getBytes());
		}
		if(get == null)
		{
			System.out.println("请检查微博类型参数是否正确！");
			System.exit(1);
		}
		try {
			Result result = hTable.get(get);
			byte[] temp = result.getValue("mainInfo".getBytes(), "sinceCreateTime".getBytes());
			if(temp != null)
			{
				sinceCreateTime = new String(temp, "UTF-8");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sinceCreateTime;
	}

	@Override
	public String getSinceCollectTime(MicroblogUser user, String tableName,
			String microblogType) {
		//若所查询的行不存在则返回“”
		if(user == null)
		{//参数不正确则退出函数
			return null;
		}
		HTable hTable = null;
		String sinceCollectTime = "";
		try {
			HBaseAdmin admin = new HBaseAdmin(conf);
			if(admin.tableExists(tableName) == false)
			{
				System.out.println("表" + tableName + "不存在！");
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
			System.out.println("IO错误！");
			e.printStackTrace();
		}
		Get get = null;
		if(microblogType.equals("Sina"))
		{//微博类型为Sina则通过用户ID来查找
			get = new Get(user.getUserId().getBytes());
		}
		else if(microblogType.equals("Tencent"))
		{//微博类型为Tencent则通过用户名来查找
			get = new Get(user.getUserName().getBytes());
		}
		if(get == null)
		{
			System.out.println("请检查微博类型参数是否正确！");
			System.exit(1);
		}
		try {
			Result result = hTable.get(get);
			byte[] temp = result.getValue("mainInfo".getBytes(), "sinceCollectTime".getBytes());
			if(temp != null)
			{
				sinceCollectTime = new String(temp, "UTF-8");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sinceCollectTime;
	}
	/**
	 * 尽量不要使用这个重载，实例化HTable很费时间
	 * */
	@Override
	public boolean isRowExists(String rowKey, String tableName)
	{
		HTable hTable = null;
		try {
			HBaseAdmin admin = new HBaseAdmin(conf);
			if(admin.tableExists(tableName) == false)
			{
				System.out.println("表" + tableName + "不存在！");
				return false;
			}
			else
			{
				hTable = new HTable(conf, tableName);
			}
			admin.close();
		} catch (MasterNotRunningException | ZooKeeperConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		Get get = new Get(rowKey.getBytes());
		try {
			Result result = hTable.get(get);
			if(result.getRow() != null)
			{
				return true;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	public boolean isRowExists(String rowKey, HTable hTable)
	{
		Get get = new Get(rowKey.getBytes());
		try {
			Result result = hTable.get(get);
			if(result.getRow() != null)
			{
				return true;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
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

}
