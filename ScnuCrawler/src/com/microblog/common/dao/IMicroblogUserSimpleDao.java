package com.microblog.common.dao;
/**
 * 该类完成向HBase保存MicroblogUser类中，sinceCreateTime字段的任务
 **/

import org.apache.hadoop.hbase.client.HTable;
import com.microblog.common.model.MicroblogUser;

public interface IMicroblogUserSimpleDao {
	public HTable createTable(String tableName);
	//public void addSimpleUserInfo(MicroblogUser user, String tableName);
	/**
	 * 插入数据或者修改数据.对应于shell操作中的 put 
	 * 将MicroblogUser实例中的sinceCreateTime和sinceCollectTime字段插入或者更新
	 */
	public void updateSimpleUserInfo(MicroblogUser user, String tableName, String microblogType);
	
	/**
	 * 插入数据或者修改数据.对应于shell操作中的 put 
	 * 将MicroblogUser实例中的sinceCreateTime和sinceCollectTime字段插入或者更新
	 */
	public void updateSimpleUserInfo(MicroblogUser user, HTable hTable, String microblogType);
	
	/** 
	 * 通过表名获取HTable
	 * 如果表不存在则返回null
	 * */
	public HTable getTableByName(String tableName);
	
	/**
	 * 查询MicroblogUser实例对应的sinceCreateTime字段值
	 */
	public String getSinceCreateTime(MicroblogUser user, String tableName, String microblogType);
	
	/**
	 * 查询MicroblogUser实例对应的sinceCollectTime字段值
	 * */
	public String getSinceCollectTime(MicroblogUser user, String tableName, String microblogType);
	
	/**
	 * 查询行是否存在
	 * */
	public boolean isRowExists(String rowKey, String tableName);
	
	/**
	 * 查询行是否存在
	 * */
	public boolean isTableExists(String tableName);
}
