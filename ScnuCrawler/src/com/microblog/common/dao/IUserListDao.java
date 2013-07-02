package com.microblog.common.dao;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.hadoop.hbase.client.HTable;

public interface IUserListDao {
	/**
	 * 创建表
	 * 注意：表已经存在则先删除之
	 * */
	public HTable createTable(String tableName);
	
	/** 
	 * 通过表名获取HTable
	 * 如果表不存在则返回null
	 * */
	public HTable getTableByName(String tableName);
	
	/**
	 * 批量向表中添加数据
	 * */
	public void appendData(ArrayList<String> userList, String tableName);
	
	/**
	 * 删除表
	 * */
	public void deleteTable(String tableName);
	
	/**
	 * 复制表内容
	 * @throws IOException 
	 * */
	public void copyContent(String sourceTable, String targetTable) throws IOException;
	
	/**
	 * 读取若干行
	 * 返回
	 * @throws IOException 
	 * */
	public ArrayList<String> getRows(String tableName, String startRow, int limitNum) throws IOException;
}
