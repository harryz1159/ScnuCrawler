package com.microblog.common.accounts.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TencentAccount {
	private static  Map<String, String> tencentAccounts = new HashMap<String, String>();
	/**
	 * 当前可用账号数
	 */
	private static int availableNum = 9;
	/**
	 * 用户输入的账号数
	 */
	private static int accountsNum = 9;
	
	//暂时用以下方法来获取测试账号信息，以后考虑读取数据库的方式
	public static void setTencentAccount()
	{
		tencentAccounts.put( "2631804320","scnu123456");
		
		Map<String, String> map2 = new HashMap<String, String>();
		map2.put("qq", "1838572665");
		map2.put("password", "scnu123456");
		tencentAccounts.add(map2);
		
		Map<String, String> map3 = new HashMap<String, String>();
		map3.put("qq", "2508355919");
		map3.put("password", "scnu123456");
		tencentAccounts.add(map3);
		
		Map<String, String> map4 = new HashMap<String, String>();
		map4.put("qq", "1766412707"/*"1838572665"*/);
		map4.put("password", "scnu123456");
		tencentAccounts.add(map4);
		
		Map<String, String> map5 = new HashMap<String, String>();
		map5.put("qq", "1721022799");
		map5.put("password", "scnu123456");
		tencentAccounts.add(map5);
		
		Map<String, String> map6 = new HashMap<String, String>();
		map6.put("qq", "1939524077");
		map6.put("password", "scnu123456");
		tencentAccounts.add(map6);
		
		Map<String, String> map7 = new HashMap<String, String>();
		map7.put("qq", "2609859304");
		map7.put("password", "scnu123456");
		tencentAccounts.add(map7);
		
		Map<String, String> map8 = new HashMap<String, String>();
		map8.put("qq", "2571134610");
		map8.put("password", "scnu123456");
		tencentAccounts.add(map8);
		
		//密码错误
		/*Map<String, String> map9 = new HashMap<String, String>();
		map9.put("qq", "1873767933");
		map9.put("password", "scnu123456");
		tencentAccounts.add(map9);*/
		
		Map<String, String> map10 = new HashMap<String, String>();
		map10.put("qq", "1806540876");
		map10.put("password", "scnu123456");
		tencentAccounts.add(map10);
		
		availableNum = 9;
		accountsNum = 9;
	}
	
	/**
	 * 获取腾讯微博账号信息
	 * 请先调用setTencentAccount()方法
	 * */
	public static  ArrayList<Map<String, String>> getTencentAccounts()
	{
		//setTencentAccount();
		return tencentAccounts;
	}
	
	
	public void setAvailableNUm(int AvailableNum)
	{
		availableNum = AvailableNum;
	}
	public  int getAvailableNum()
	{
		return availableNum;
	}
	
	public static int getAccountsNum()
	{
		return accountsNum;
	}
	public  void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
