package com.microblog.common.accounts.manager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.httpclient.HttpException;

import weibo4j.Account;
import weibo4j.Weibo;
import weibo4j.model.RateLimitStatus;
import weibo4j.model.WeiboException;

import com.microblog.common.login.SinaLogin;

public class SinaAccount {

	private static Map<String, String> weiboAccounts = new HashMap<String, String>();
	private static ArrayList<String> accessTokenList=new ArrayList<>();
	/**
	 * 当前可用账号数
	 */
	private static int availableNum = 9;
	/**
	 * 用户输入的账号数
	 */
	private static int accountsNum = 9;
	private static Weibo weibo=new Weibo();
	/**
	 * 保存当前正在使用的accessToken剩余API调用次数。
	 */
	private static long remainingHits=0;
	
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
	static
	{
		setWeiboAccount();
	}
	
	/**
	 * 获取新浪微博账号信息
	 * 请先调用setWeiboAccount()方法
	 * */
	public static Map<String, String> getWeiboAccounts()
	{
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
	 * 改变当前正在使用的accessToken。该方法会一直阻塞直至找到一个可用的accessToken。
	 * @return 查询成功后返回 {@code true}。
	 */
	private static boolean changeAccessToken()
	{
		for(int j = 0;;j = (++j) %accessTokenList.size())
		{
			String accessToken=accessTokenList.get(j);
			if(accessToken!=null)
				weibo.setToken(accessToken);
			updateRemainingHits();
			if(remainingHits>0)
				break;
			System.out.println("正在查找可用access token，3.6秒后重试。");
			try {
				Thread.sleep(3600);
			} catch (InterruptedException e) {
				// TODO 自动生成的 catch 块
				e.printStackTrace();
			}
		}
		return true;
	}

	/**
	 * 更新当前使用的accessToken的剩余API调用次数。
	 */
	private static void updateRemainingHits()
	{
		Account am = new Account();
		RateLimitStatus json = null ;
		try {
			json = am.getAccountRateLimitStatus();
			remainingHits = json.getRemainingUserHits();
		} catch (WeiboException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
			remainingHits=0;
			System.out.println("查询剩余调用次数时获取json出错。");
		}
		try {
			Thread.sleep(3600);
		} catch (InterruptedException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		}
		System.out.println("当前accessToken剩余调用次数为：" + remainingHits + "次");
	}
	/**
	 * 使当前使用的accessToken的剩余API调用次数减一，每次调用网络API前都应该调用它。
	 * 该方法会对剩余的API次数进行检测，如果剩余次数不足以调用网络API，则该方法会更换accessToken。
	 * 在确认可以调用网络API前，该方法会一直阻塞。所以使用了这个方法后调用网络API不会因为达到访
	 * 问限制而抛出异常。
	 */
	public static void reduceRemainingHits()
	{
		if(--remainingHits<0)
		{
			changeAccessToken();
			remainingHits--;
		}
	}
}
