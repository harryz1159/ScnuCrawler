package com.microblog.common.accounts.manager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.httpclient.HttpException;

import com.microblog.common.login.SinaLogin;

public class SinaAccount {

	private static Map<String, String> weiboAccounts = new HashMap<String, String>();
	private static ArrayList<String> accessTokenList=new ArrayList<>();
	private static int availableNum = 9;//当前可用账号数
	private static int accountsNum = 9;//用户输入的账号数
	
	//以下代码用来从数据库获取用户信息（账号、密码）
	//目前暂时用以下代码来代替
	public static void setWeiboAccount()
	{
		weiboAccounts.put("scnudatamining@sina.com","scnu123456");
		
		
		weiboAccounts.put("s.cnudataminingsmarttraffic@gmail.com","scnu123456");
		
		
		weiboAccounts.put("sc.nudataminingsmarttraffic@gmail.com","scnu123456");
		
		
		weiboAccounts.put("scn.udataminingsmarttraffic@gmail.com","scnu123456");
		
		
		weiboAccounts.put("scnu.dataminingsmarttraffic@gmail.com","scnu123456");
		
		
		weiboAccounts.put("scnud.ataminingsmarttraffic@gmail.com","scnu123456");
		
		
		weiboAccounts.put("scnuda.taminingsmarttraffic@gmail.com","scnu123456");
		
		
		weiboAccounts.put("scnudat.aminingsmarttraffic@gmail.com","scnu123456");
		
		
		weiboAccounts.put("scnudata.miningsmarttraffic@gmail.com","scnu123456");
		
		availableNum = weiboAccounts.size();
		accountsNum = weiboAccounts.size();
		Set<Entry<String, String>> accounts=weiboAccounts.entrySet();
		for(Entry<String,String> account:accounts)
			try {
				accessTokenList.add(SinaLogin.getToken(account.getKey(), account.getValue()).getAccessToken());
			} catch (HttpException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	
	/**
	 * 获取新浪微博账号信息
	 * 请先调用setWeiboAccount()方法
	 * */
	public static Map<String, String> getWeiboAccounts()
	{
		//this.setWeiboAccount();
		return weiboAccounts;
	}
	
	
	public void setAvailableNUm(int availableNum)
	{
		SinaAccount.availableNum = availableNum;
	}
	public int getAvailableNum()
	{
		return availableNum;
	}
	
	public static int getAccountsNum()
	{
		return accountsNum;
	}

	/**
	 * 返回accessToken列表
	 * @return accessToken列表
	 */
	public static ArrayList<String> getAccessTokenList() {
		return accessTokenList;
	}
}
