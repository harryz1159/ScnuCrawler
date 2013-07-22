package com.microblog.common.accounts.manager;

import java.util.HashMap;
import java.util.Map;

import com.microblog.common.login.TencentLogin;
import com.tencent.weibo.oauthv2.OAuthV2;

public class TencentAccount {
	private static  Map<String, String> tencentAccounts = new HashMap<String, String>();
	private static OAuthV2 oa=null;
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
		
		tencentAccounts.put("1838572665","1838572665");
		
		tencentAccounts.put("2508355919","scnu123456");
		
		tencentAccounts.put("1766412707","scnu123456");
		
		tencentAccounts.put("1721022799","scnu123456");
		
		tencentAccounts.put("1939524077", "scnu123456");
		
		tencentAccounts.put("2609859304","scnu123456");
		
		tencentAccounts.put("2571134610","scnu123456");
		
		//密码错误
		/*Map<String, String> map9 = new HashMap<String, String>();
		map9.put("qq", "1873767933");
		map9.put("password", "scnu123456");
		tencentAccounts.add(map9);*/
		
		tencentAccounts.put("1806540876","scnu123456");
		
		availableNum = tencentAccounts.size();
		accountsNum = tencentAccounts.size();
		try {
			oa = TencentLogin.gainAccessToken("1838572665", "scnu123456");
		} catch (Exception e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		}
	}
	static
	{
		setTencentAccount();
	}
	
	/**
	 * 获取腾讯微博账号信息
	 * 请先调用setTencentAccount()方法
	 * */
	public static  Map<String, String> getTencentAccounts()
	{
		//setTencentAccount();
		return tencentAccounts;
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


	/**
	 * 返回腾讯微博认证参数实体类。
	 * @return oa 腾讯微博认证参数实体类
	 */
	public static OAuthV2 getOa() {
		return oa;
	}

}
