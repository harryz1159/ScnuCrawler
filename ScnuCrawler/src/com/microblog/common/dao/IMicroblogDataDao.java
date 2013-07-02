package com.microblog.common.dao;

import java.util.ArrayList;

import org.apache.hadoop.hbase.client.HTable;

import com.microblog.common.model.MicroblogData;

public interface IMicroblogDataDao {
	/**
	 * 初始化Hbase配置
	 */
	//public void initialize();
	
	/**
	 * 判断指定名称的表是否存在
	 * */
	public boolean isTableExists(String tableName);
	
	/**
	 * 创建Hbase表
	 * 包括3个列簇：身份（identify：用户名、ID等信息）、
	 *            内容（content：文字、图片等信息）、
	 *            时间（time：数据收集时间、微博创建时间等信息）
	 */
	public void createTable(String tableName);
	
	/**
	 * 通过表名获取HTable
	 * 如果表不存在则返回null
	 * */
	public HTable getTableByName(String tableName);
	
	/**
	 * 向Hbase表中添加数据
	 */
	public void addData(ArrayList<MicroblogData> statusesList, HTable table);
	
	/**
	 * 向Hbase表中添加数据
	 */
	public void addData(ArrayList<MicroblogData> statusesList, String tableName);
	
}
