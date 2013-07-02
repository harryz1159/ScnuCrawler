package com.hjh.inf;

import java.util.List;
import java.util.Map;

import org.apache.hadoop.hbase.client.HTable;

import org.apache.hadoop.hbase.client.Result;
/**
 *  * @author hadoop
 *	   hbase的操作接口
 */

public interface OperateHbase
{
	//建表操作
	/**
	 * 
	 * @param tableName--存在表名和列族信息的配置文件或者是表名，可以用tabelConfig类来读取
	 */
    public void createTable(String tableName);
            
    /**
     * 
     * @param tableName-表名(建议不要太长)
     */
    //删表操作
    public void dropTable(String tableName);
    
    /**
     * 
     * @param tableName-表名(建议不要太长)
     * @param data-类型为:Map<String , byte[]>,其中地一个参数形为列族:列名,第二个参数为对应的数据
     */
    //插入记录操作
    public void insert(String tableName,Map<String , byte[]> data);
    
    /**
     * 
     * @param tableName-表名(建议不要太长)
     * @param data-类型为list，list中的元素为类型为map，Map<String , byte[]>,其中地一个参数形为列族:列名,第二个参数为对应的数据
     */
    //多条记录插入
    public void insertAll(String tableName,List<Map<String ,byte[]>> data);
    
    /**
     * 
     * @param tableName-表名(建议不要太长)
     * @param row - 行ID
     * @param column--列族名或列族名:列名
     */
    //删除记录操作
    public void delete(String tableName,String row, String column);
    
    
    /**
     * 
     * @param tableName- 表名(建议不要太长)
     * @param row-  行ID
     * @return
     */
    //查询数据操作
    public Result query(String tableName, String row);
    
    /**
     * 根据rowID查询数据
     * @param lsHTable		---HTable对象
     * @param rowID			--行ID
     * @return
     */
    public Result query(HTable lsHTable,String rowID);
    
    //测试时用的函数
	public void insertData(HTable ht,String tableName, String rowID, String columnFamily,String column,String Value);
	
	/**
	 * 获取访问HTable的对象
	 * @param tabeName
	 * @return
	 */
	public HTable getHTable(String tableName);
	
}
