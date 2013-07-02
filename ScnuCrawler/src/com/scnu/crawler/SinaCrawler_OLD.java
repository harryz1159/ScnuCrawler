package com.scnu.crawler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

import com.hjh.HBaseFactory;
import com.hjh.inf.OperateHbase;
import com.scnu.crawler.util.Status;

import weibo4j.Account;
import weibo4j.Friendships;
import weibo4j.Oauth;
import weibo4j.Timeline;
import weibo4j.Users;
import weibo4j.Weibo;
//import weibo4j.examples.oauth2.Log;
import weibo4j.http.Response;
import weibo4j.model.Paging;
import weibo4j.model.RateLimitStatus;
import weibo4j.model.StatusWapper;
import weibo4j.model.User;
import weibo4j.model.WeiboException;
import weibo4j.org.json.JSONException;
import weibo4j.org.json.JSONObject;
import weibo4j.util.BareBonesBrowserLaunch;

import com.scnu.crawler.util.sinaTimeLine;
import com.microblog.common.accounts.manager.SinaAccount;
import com.microblog.common.dao.impl.hbase.MicroblogDataDaoImpl;
import com.microblog.common.login.*;
import com.microblog.common.model.MicroblogData;
import com.microblog.common.model.MicroblogUser;
/**
 * 
 * @author hadoop
 * sina数据读取工具,需要抓取的数据包括:
 * 1 微博数据： 来源  用户ID 微博ID 内容  时间 评论数 转发数
 * 2 用户评论： 来源  用户ID 微博ID 评论用户ID 评论内容 
 * 3 调度数据： 来源  用户ID 微薄ID 抓取时间 微博条数  
 *
 *调度的主要依据:
 *1 已经抓取的数据不再重复抓取
 *2上次抓取微薄条数小于阀值的ID延长间隔时间
 *3 用户的粉丝数据和关注数据存储起来，经过间隔时间后才更新数据
 */
public class SinaCrawler_OLD implements crawler{
	
	private static Weibo weibo = new Weibo();
	private static Account account = new Account();
	private static int StatusPageCounts = 100;        //分页微博数，最多每页100条 
	private static int weiboSize = 0;
	private static int apiTimes = 0;
	private OperateHbase hbaseObject = null;
	private static String accessToken = null;
	//private int apiTimesInSingleAccount = 0; //单个账户调用API的次数
	private static long remaining_hits = 0; //记录当前用户调用接口的剩余次数
	private static ArrayList<String> accessTokenList = new ArrayList<String>(); 		//保存所有账号的accessToken

  
	/**
	 * 登陆账号并获取access token
	 */
	public static void getAccessTokenList() throws Exception
	{
		SinaAccount.setWeiboAccount();			//获取新浪微博账号信息
		for(int i = 0; i < SinaAccount.getAccountsNum(); ++i)
		{
			accessTokenList.add(SinaLogin.getToken(SinaAccount.getWeiboAccounts().get(i).get("username"), SinaAccount.getWeiboAccounts().get(i).get("password")).getAccessToken());
		}
	}
	
    /**
     * 更新剩余访问次数
     * 该方法会重写类成员变量remaining_hits
     * 新浪微博api的访问次数包含了读和写接口
     * 获取剩余调用次数会消耗基于IP的剩余调用次数，每小时1000次
     */
	private static void updateRemaining_hits() throws Exception
	{
		Account am = new Account();
		RateLimitStatus json = null ;
		try {
            json = am.getAccountRateLimitStatus();
            remaining_hits = json.getRemainingUserHits(); //将remaining_hits重置为正确值
            Thread.sleep(3600);		//防止遇到大量没有微博内容的僵尸帐号，这些帐号会消耗基于IP的剩余次数查询
		} catch (WeiboException e) {
			e.printStackTrace();
			--remaining_hits; //防止刚好达到访问限制的时候获取json出错，当成功获取时该变量会被重置为正确值
			System.out.println("查询剩余调用次数时获取json出错。");
			Thread.sleep(3600);
		}
		System.out.println("当前剩余调用次数为：" + remaining_hits + "次");
	}
	
	/**
	 * 查找可用的access token
	 * 该方法会消耗基于IP的API调用次数若干次
	 */
	private static boolean changeAccessToken() throws Exception
	{
		/*
		 * 测试应用的access token有效期只有1天
		 * 以下花括号为改进方案（暂无方案）
		 */
		{}
		
		/*
		 * 获取第一个可用的access token
		 * 若暂时找不到则让用户等待
		 * */
		for(int j = 0; /*j <sinaAccount.getAccountsNum()*/; j = ++j % SinaAccount.getAccountsNum())
		{//这是一个死循环 只有找到可用的账户才会跳出 可以考虑循环一定次数停止程序
			accessToken = accessTokenList.get(j);
			if(accessToken != null)
			{
				weibo.setToken(accessToken);
			}
			updateRemaining_hits();			//更新剩余次数的值
			if(remaining_hits > 0)
			{
				break;
			}
			System.out.println("正在查找可用access token，3.6秒后重试。");
			Thread.sleep(3600); 		//remaining_hits的查询是基于IP的，一小时1000次，根据运行情况，间隔时间可能可以设置得更小
		}
		return true;
	}
	
	/**
	 * 获得指定用户id的粉丝列表(uid)
	 * @return 粉丝用户id列表
	 * @throws Exception 
	 */
	public static ArrayList<String> getFollowersIdListById(String uid) throws Exception
	{//目前由于新浪的限制，最多只能获取5000个左右
		changeAccessToken();
		ArrayList<String> followersList = new ArrayList<String>();
		int followersNum = 0;
		int FollowersPageCount = 5000;			//一页最多只能返回5000个
		Friendships fm = new Friendships();
		try {
			String[] ids = fm.getFollowersIdsById(uid, FollowersPageCount, 0);
			--remaining_hits;
			for(String u : ids){
				followersList.add(u);
				System.out.println(u);
				++followersNum;
				//Log.logInfo(u.toString());
			}
		} catch (WeiboException e) {
			e.printStackTrace();
		}
		System.out.println("共获得" + followersNum + "个粉丝。"); 
		return followersList;
	}
	
	/**
	 * 获得指定用户id的粉丝列表(uid)
	 * 该方法会消耗基于用户的API调用次数（1个单位）
	 * @return 粉丝用户id列表
	 * @throws Exception 
	 */
	public static ArrayList<String> getUserFansList(MicroblogUser user) throws Exception
	{//目前由于新浪的限制，最多只能获取5000个左右
		if(user == null)
		{
			return null;
		}
		final int tryUpperLimit = 3;		//超时重试次数上限
		changeAccessToken();	//查找可用access token
		ArrayList<String> followersList = new ArrayList<String>();
		int followersNum = 0;
		int FollowersPageCount = 5000;			//一页最多只能返回5000个
		Friendships fm = new Friendships();
		for(int tryTimes = 0; /*tryTimes < tryUpperLimit*/; /*++tryTimes*/)
		{//尝试获取微博数据，最多重试tryUpperLimit次
			if(tryTimes >= tryUpperLimit)
			{//达到重试次数上限，返回null
				System.out.println("超时重试次数达到上限："+ tryTimes +"次，将跳过该用户！");
				return null;
			}
			--remaining_hits;
			++apiTimes;
			try {
				String[] ids = fm.getFollowersIdsById(user.getUserId(), FollowersPageCount, 0);
				//--remaining_hits;
				//++apiTimes;
				for(String u : ids){
					if(u != "")
					{//防止抓取到空id
						followersList.add(u);
						++followersNum;
					}
					System.out.println(u);
				}
				break;		//正确获取到信息，跳出循环，不计入重试次数
			} catch (WeiboException e) {
				//++apiTimes;		
				//--remaining_hits;		//尽管发生超时，但服务器可能已经记录了调用次数
				++tryTimes;		//重试计数增加1
				System.out.println("获取用户粉丝列表出错或者网络链接超时。。。\n将在3.6秒后重试。。。");
				Thread.sleep(3600);
				e.printStackTrace();
			}
		}
		System.out.println("共获得" + followersNum + "个粉丝。"); 
		return followersList;
	}
	
	/**
	 * 获得指定用户id的关注列表(uid)
	 * @return 关注用户id列表
	 * @throws Exception 
	 * */
	public static ArrayList<String> getFriendsIdListById(String uid) throws Exception
	{//目前由于新浪的限制，最多只能获取5000个左右
		changeAccessToken();
		ArrayList<String> friendsList = new ArrayList<String>();
		int friendsNum = 0;           
		int FriendsPageCount = 5000;			//一页最多只能返回5000个
		Friendships fm = new Friendships();
		try {
			String[] ids = fm.getFriendsIdsByUid(uid, FriendsPageCount, 0);
			--remaining_hits;
			for(String s : ids){
				friendsList.add(s);
				System.out.println(s);
				++friendsNum;
				//Log.logInfo(s);
			}
		} catch (WeiboException e) {
			e.printStackTrace();
		}
		System.out.println("共获得" + friendsNum + "个关注对象。");
		return friendsList;
	}
	
	/**
	 * 获得指定用户id的关注列表(uid)
	 * 该方法会消耗基于用户的API调用次数（1个单位）
	 * @return 关注用户id列表
	 * @throws Exception 
	 * */
	public static ArrayList<String> getUserIdolsList(MicroblogUser user) throws Exception
	{//目前由于新浪的限制，最多只能获取5000个左右
		if(user == null)
		{
			return null;
		}
		final int tryUpperLimit = 3;		//超时重试次数上限
		changeAccessToken();
		ArrayList<String> friendsList = new ArrayList<String>();
		int friendsNum = 0;           
		int FriendsPageCount = 5000;			//一页最多只能返回5000个
		Friendships fm = new Friendships();
		for(int tryTimes = 0; /*tryTimes < tryUpperLimit*/; /*++tryTimes*/)
		{//尝试获取微博数据，最多重试tryUpperLimit次
			if(tryTimes >= tryUpperLimit)
			{//达到重试次数上限，返回null
				System.out.println("超时重试次数达到上限："+ tryTimes +"次，将跳过该用户！");
				return null;
			}
			--remaining_hits;
			++apiTimes;
			try {
				String[] ids = fm.getFriendsIdsByUid(user.getUserId(), FriendsPageCount, 0);
				//--remaining_hits;
				//++apiTimes;
				for(String s : ids){
					if(s != "")
					{
						friendsList.add(s);
						++friendsNum;
					}
					System.out.println(s);
				}
				break;		//正确获取到信息，跳出循环，不计入重试次数
			} catch (WeiboException e) {
				//++apiTimes;		
				//--remaining_hits;		//尽管发生超时，但服务器可能已经记录了调用次数
				++tryTimes;		//重试计数增加1
				System.out.println("获取用户粉丝列表出错或者网络链接超时。。。\n将在3.6秒后重试。。。");
				Thread.sleep(3600);
				e.printStackTrace();
			}
		}
		System.out.println("共获得" + friendsNum + "个关注对象。");
		return friendsList;
	}
	
	/**
	 * 获取用户信息
	 */
	public static MicroblogUser getUserInfoByID(String userId) throws Exception
	{//新浪服务器没有该Id存在的情况在捕捉WeiboException异常的代码中一并处理了
		if(userId == null || userId.equals(""))
		{
			return null;
		}
		changeAccessToken();	//查找可用access token
		final int tryUpperLimit = 3;		//超时重试次数上限
		MicroblogUser mUser = new MicroblogUser();
		Users um = new Users();
		for(int tryTimes = 0; /*tryTimes < tryUpperLimit*/; /*++tryTimes*/)
		{//尝试获取微博数据，最多重试tryUpperLimit次
			if(tryTimes >= tryUpperLimit)
			{//达到重试次数上限，返回null
				System.out.println("超时重试次数达到上限："+ tryTimes +"次，将跳过该用户！");
				return null;
			}
			try {
				User user = um.showUserById(userId);	//这一句调用了Users接口
				mUser.setUserId(user.getId());
				mUser.setUserName(user.getName());
				mUser.setNickName(user.getScreenName());
				mUser.setGender(user.getGender());
				mUser.setProvince(Integer.toString(user.getProvince()));
				mUser.setFansCount(user.getbiFollowersCount());
				mUser.setIdolsCount(user.getFriendsCount());
				mUser.setStatusesCount(user.getStatusesCount());
				++apiTimes;
				--remaining_hits;
				break;	//正确获取到信息，跳出循环，不计入重试次数
			} catch (WeiboException e) {
				++apiTimes;		
				--remaining_hits;		//尽管发生超时，但服务器可能已经记录了调用次数
				++tryTimes;		//重试计数增加1
				System.out.println("获取用户信息错误或者网络链接超时。。。\n将在3.6秒后重试。。。");
				Thread.sleep(3600);
				e.printStackTrace();
			}	 
		}
        return mUser;
	}
	
    /**
     * 获得指定用户id的所有微博内容
     * @return 
     * */
	public static ArrayList<MicroblogData> getUserStatusesByID(String uid) throws Exception
	{
		changeAccessToken();		//设置access token
		SimpleDateFormat t = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Users um = new Users();
        User user = um.showUserById(uid);	 //这一句调用了Users接口
        Timeline tm = new Timeline();
        ArrayList<MicroblogData>  statusesList  = new ArrayList<MicroblogData>();
       
        /*
         * 获取用户的所有微博内容 
         * */
        for(int i=1; i<=user.getStatusesCount()/StatusPageCounts+1; ++i)
        {
        	/*
        	 * 当前用户调用接口次数超出限制，更换用户
        	 * */
        	if(remaining_hits <= 0)
        	{
        		System.out.println("超出当前用户调用API次数限制。");
        		System.out.println("正在更换access token。。。");
        		Thread.sleep(5 * 1000);
        		changeAccessToken();		//更换access token
        	}
        	try           	
        	{
        		StatusWapper status = tm.getUserTimelineByUid(uid,new Paging(i,StatusPageCounts),0,0);
            	apiTimes++;
            	--remaining_hits;
            	
                /**
                 * 获取分页内容
                 * */
            	for(weibo4j.model.Status s : status.getStatuses())
            	{        
            		MicroblogData mdata = new MicroblogData();
            		mdata.setMicroblogID(s.getId());
            		mdata.setText(s.getText());
            		mdata.setPicSrc(s.getOriginalPic());
            		mdata.setNickName(s.getUser().getScreenName());
            		mdata.setCreatTime(s.getCreatedAt().toString());
            		mdata.setCollectTime(t.format(new Date()));
            		mdata.setUserID(uid);
            		mdata.setUserName(s.getUser().getName());
            		System.out.println(mdata.getText());	//用来在控制台查看是否抓取到数据
            		statusesList.add(mdata);
            		weiboSize++;	                		 
            	}
            	//TestHbase.doHbase(arrayList);
        		//mh.HttpPost(arrayList);
            	//++i;		//获取微博内容可能会出错，写在try模块里以便重试
            }
        	catch(Exception e)
        	{
        	    //e.printStackTrace();	            	    
        		//break;
        		System.out.println("获取本页微博内容出错。");
        	}
        	
        	//不必每次翻页都刷新reamining_hits
        	//updateRemaining_hits();		//更新剩余次数的值
        	
        }
        System.out.println("接口调用" + apiTimes + "次。");
        System.out.println("共获取" + weiboSize + "条微博内容。");
		return statusesList;
	}
	
    /**
     * 获得指定用户微博内容
     * @return 
     * */
	public static ArrayList<MicroblogData> getUserStatuses(MicroblogUser user) throws Exception
	{
		if(user == null)
		{
			return null;
		}
		System.out.println("正准备获取" + user.getUserId() + "的微博内容。。。");
		final int tryUpperLimit = 3;		//超时重试次数上限
		changeAccessToken();		//查找可用的access token
		SimpleDateFormat t = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Timeline tm = new Timeline();
        ArrayList<MicroblogData>  statusesList  = new ArrayList<MicroblogData>();
       
        /*
         * 获取用户的所有微博内容 
         */
        for(int i=1; i<=user.getStatusesCount()/StatusPageCounts+1; /*++i*/)
        {
        	/*
        	 * 当前用户调用接口次数超出限制，更换用户
        	 */
        	if(remaining_hits <= 0)
        	{
        		System.out.println("超出当前用户调用API次数限制。");
        		System.out.println("正在更换access token。。。");
        		Thread.sleep(3600);
        		changeAccessToken();		//更换access token
        	}
    		for(int tryTimes = 0; /*tryTimes < tryUpperLimit*/; /*++tryTimes*/)
    		{//尝试获取微博数据，最多重试tryUpperLimit次
    			if(tryTimes >= tryUpperLimit)
    			{//达到重试次数上限，跳过当前页
    				System.out.println("重试次数达到上限："+ tryTimes +"次，将跳过当前页！");
    				++i;	//设置下一页参数
    				break;	//跳过当前页
    			}
				--remaining_hits;
				++apiTimes;
            	try           	
            	{
            		StatusWapper status = tm.getUserTimelineByUid(user.getUserId(),new Paging(i,StatusPageCounts),0,0);		//调用API
                	//apiTimes++;
                	//--remaining_hits;
                	
                    /**
                     * 获取分页内容
                     */
                	for(weibo4j.model.Status s : status.getStatuses())
                	{        
                		MicroblogData mdata = new MicroblogData();
                		mdata.setMicroblogID(s.getId());
                		mdata.setText(s.getText());
                		mdata.setPicSrc(s.getOriginalPic());
                		mdata.setNickName(s.getUser().getScreenName());
                		mdata.setCreatTime(t.format(s.getCreatedAt()));	 //注意日期的显示格式，关系到数据库里的排序
                		mdata.setCollectTime(t.format(new Date()));
                		mdata.setUserID(user.getUserId());
                		mdata.setUserName(s.getUser().getName());
                		System.out.println(mdata.getText());	//用来在控制台查看是否抓取到数据
                		statusesList.add(mdata);
                		weiboSize++;	                		 
                	}
                	++i;		//获取微博内容可能会出错，写在try模块里以便重试
                	break;		//获取到正确信息，跳出重试循环，不计入重试次数
                } catch(WeiboException e)
            	{
            	    //e.printStackTrace();	            	    
            		//break;
    				//++apiTimes;		
    				//--remaining_hits;		//尽管发生超时，但服务器可能已经记录了调用次数
    				++tryTimes;		//重试计数增加1
    				System.out.println("获取本页微博内容出错。。。\n将重试。。。");
    				//remaining_hits -= 10;	//服务器返回的剩余调用次数未必正确，如果返回错误的数，会使得程序因为可用调用次数为0而一直出现异常，这一句是为了让程序尽快跳出循环，重新检查可用调用次数
    				changeAccessToken();		//捕捉到异常有可能是超出调用次数限制了，因此更换access token
    				e.printStackTrace();
            	}
    		}        	
        	//updateRemaining_hits();		//更新剩余次数的值
        	System.out.println("正在获取" + user.getUserId() + "的微博内容。。。");
        }
        System.out.println("接口调用" + apiTimes + "次。");
        System.out.println("共获取" + weiboSize + "条微博内容。");
		return statusesList;
	}
	
	/**
	 *判断是否达到剪枝条件
	 * */
	public static boolean isPruning(MicroblogUser user) throws Exception
	{   
        if((user.getIdolsCount() < 4) && (user.getFansCount() < 4))
        {
        	return true;
        }
		return false;
	}
	
	
	/**
	 * 抓取逻辑
	 * */
	public static void getSina_OLD2() throws Exception
	{
		getAccessTokenList(); 		//登陆所有测试账号并获取所有access token
		//抓取逻辑
		MicroblogDataDaoImpl mbddi = new MicroblogDataDaoImpl();
		/*HTable hTable = */mbddi.createTable("SinaTable");		//创建SinaTable表
		System.out.println("正准备获取" + "1750070171" + "的微博数据。");
		Thread.sleep(3 * 1000);
		ArrayList<MicroblogData> statusesList = getUserStatusesByID("1750070171");		//抓取用户1750070171的微博内容
		mbddi.addData(statusesList, "SinaTable");	//将微博数据写入SinaTable
		
		ArrayList<String> friendsIds =  getFriendsIdListById("1750070171");			//获得1750070171的关注列表
		/*
		 * 获取关注对象微博内容
		 */
		for(String s : friendsIds)
		{
			System.out.println("正准备获取" + s + "的微博数据。");
			Thread.sleep(3 * 1000);
			//if(isPruning(s) != true)
			//{
				try{
				//按照下面这一句写似乎会出现程序运行很慢的情况 硬盘灯狂闪 电脑卡死 原因未明
				//因此考虑每个用户实例化一次dao
				//mbddi.addData(getUserStatusesByID(s), "SinaTable");	//关注对象的微博数据写入SinaTable
				MicroblogDataDaoImpl mbddi_fr = new MicroblogDataDaoImpl();
				ArrayList<MicroblogData> statusesList_fr = getUserStatusesByID(s);
				mbddi_fr.addData(statusesList_fr, "SinaTable");	//关注对象的微博数据写入SinaTable
				} catch(Exception e)
				{
					System.out.println("可能是超时异常，将跳过此帐户。。。");
					e.printStackTrace();
					Thread.sleep(3600);
				}
			//}
			//else
			//{
				System.out.println("用户" + s + "满足剪枝条件，不获取其微博数据。");
				System.out.println("正等待获取下个用户微博数据。");
				Thread.sleep(3 * 1000);
			//}
		}
		ArrayList<String> followersIds = getFollowersIdListById("1750070171");			//获得1750070171的粉丝列表
		int fcount = 0;		//粉丝计数器
		/*
		 * 获取粉丝微博内容
		 */
		for(String s : followersIds)
		{
			System.out.println("正准备获取" + s + "的微博数据。");
			Thread.sleep(3 * 1000);
			//if(isPruning(s) != true)
			//{
				try{
				MicroblogDataDaoImpl mbddi_fo = new MicroblogDataDaoImpl();
				ArrayList<MicroblogData> statusesList_fo = getUserStatusesByID(s);
				mbddi_fo.addData(statusesList_fo, "SinaTable");	//粉丝的微博数据写入SinaTable
				} catch(Exception e)
				{
					System.out.println("可能是超时异常，将跳过此账户。。。");
					e.printStackTrace();
					Thread.sleep(3600);
				}
			//}
			//else
			//{
				System.out.println("用户" + s + "满足剪枝条件，不获取其微博数据。");
				System.out.println("正等待获取下个用户微博数据。");
				Thread.sleep(3 * 1000);
			//}
			if(++fcount > 600)
			{//目前access token有效期只有一天，因此只能获取适当数量的粉丝微博信息
				break;
			}
		}
	}
	
    //获取微博列表
    public void getSina_OLD1(){
    	//登录账户并抓取
    	//暂时假定自动登录不会失败
    	try
    	{	    		
    		//weibo.setToken(getAccessToken());        //设置access_token
    		//String accessToken = SinaLogin.getToken(weiboAccounts.get(testAccount).get("username"), 
    		//		weiboAccounts.get(testAccount).
    		//		get("password")).getAccessToken(); //getAccessToken是weibo4j.http.AccessToken中的方法，返回String类型
		    //SinaLogin.sinaSendWeibo(accessToken, "This is a message of API testing. It means the crawler is starting to collect  information by user's ID. From Java app.");
    		
    		//获取用户测试账号信息
    		//int accountIterator = 0; //账号集循环迭代器
    		SinaAccount sinaAccount = new SinaAccount();
    		ArrayList<Map<String, String>> weiboAccounts = sinaAccount.getWeiboAccounts();//获得所有账户信息
    		//Iterator iter = weiboAccounts.iterator();
    		//获取第一个账号的accesstoken
    		//accessToken = SinaLogin.getToken(weiboAccounts.get(accountIterator).get("username"), weiboAccounts.get(accountIterator).get("password")).getAccessToken();
    		//System.out.println(weiboAccounts.get(accountIterator).get("username"));
    		
    		//登录所有可用账户并获取相应的access token
    		ArrayList<String> ats = new ArrayList<String>();
    		for(int i = 0; i < sinaAccount.getAccountsNum(); ++i)
    		{
    			ats.add(SinaLogin.getToken(weiboAccounts.get(i).get("username"), weiboAccounts.get(i).get("password")).getAccessToken());
    		}
    		
    		long remaining_hits = 0; //记录当前用户调用接口的剩余次数
    		for(int i = 0; i <sinaAccount.getAccountsNum(); ++i)
    		{//获取第一个可用的accessToken
    			accessToken = ats.get(i);
    			if(accessToken != null)
    			{
    				weibo.setToken(accessToken);
    			}
    			//remaining_hits = updateRemaining_hits(remaining_hits);//更新剩余次数的值
    			if(remaining_hits > 0)
    			{
    				break;
    			}
    			System.out.println("Please wait......");
    		}
    		
            SimpleDateFormat t = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            //MyHttpClient mh = new MyHttpClient();
    		
    		String uid = /*"1831398050"*/ /*"1850235592"*/ "1750070171";                 //此处设置需获取微博列表的帐号ID
    		
    		Users um = new Users();
            User user = um.showUserById(uid);	            
            Timeline tm = new Timeline();	                   
           
            //获取微博列表 
            for(int i=1; /*apiTimes <450/*i<=user.getStatusesCount()/StatusPageCounts+1*/;/*i++*/)
            {
            	try           	
            	{//调用接口获取微博数据可能会失败，因此可以将i++写在try模块里，以便重试
            		StatusWapper status = tm.getUserTimelineByUid(uid,new Paging(i,StatusPageCounts),0,0);
                	apiTimes++;
                	//++apiTimesInSingleAccount;
                    
                	ArrayList<Map<String, String>>  arrayList  = new ArrayList<Map<String, String>>();
                	for(weibo4j.model.Status s : status.getStatuses())
                	{        
                		Map<String, String> map = new HashMap<String, String>();
                		map.put("weiboID", s.getId());
                		map.put("content", s.getText());
                		map.put("picSrc", s.getOriginalPic());
                		map.put("nickname", s.getUser().getScreenName());
                		map.put("createTime", s.getCreatedAt().toString());
                		map.put("collectTime", t.format(new Date()));
                		arrayList.add(map);
                		System.out.println(map);
                		weiboSize++;	                		
                		//weibo.rateLimitStatus();
                	}
                	//TestHbase.doHbase(arrayList);
            		//mh.HttpPost(arrayList);
                }
            	catch(Exception e)
            	{
            	    //e.printStackTrace();	            	    
            		//break;
            		System.out.println("获取微博内容出错。");
            	}
            	
            	//remaining_hits = updateRemaining_hits(remaining_hits);//更新剩余次数的值
            	if(remaining_hits <= 0)
            	{//当前用户调用接口次数超出限制，更换用户
            		System.out.println("超出当前用户调用API次数限制。");
            		Thread.sleep(10*1000);
            		for(int j = 0; /*j <sinaAccount.getAccountsNum()*/; j = ++j % sinaAccount.getAccountsNum())
            		{//获取第一个可用的accessToken
            			//这是一个死循环 只有找到可用的账户才会跳出 可以考虑循环一定次数停止程序
            			accessToken = ats.get(j);
            			if(accessToken != null)
            			{
            				weibo.setToken(accessToken);
            			}
            		//	remaining_hits = updateRemaining_hits(remaining_hits);//更新剩余次数的值
            			if(remaining_hits > 0)
            			{
            				break;
            			}
            			System.out.println("Please wait......");
            		}
            	}
            	//判断当前账户调用接口次数是否超出限制   	       
        		/*Account am = new Account();
        		RateLimitStatus json = null ;
        		long remaining_hits = 0; //记录当前用户调用接口的剩余次数
        		try {
                    json = am.getAccountRateLimitStatus();
                    remaining_hits = json.getRemainingUserHits();
        		} catch (WeiboException e) {
        			e.printStackTrace();
        		}
        		//超出次数限制，更换账户
            	if(remaining_hits <= 0)
            	{
            		//apiTimesInSingleAccount = 0;
            		accountIterator = ++accountIterator%sinaAccount.getAccountsNum();
            		sinaAccount.setAvailableNUm(sinaAccount.getAvailableNum() - 1);//可用的账户数减1
            		//什么时候要重置 明天再写
    	    		accessToken = SinaLogin.getToken(weiboAccounts.get(accountIterator).get("username"), weiboAccounts.get(accountIterator).get("password")).getAccessToken();
    	    		System.out.println(weiboAccounts.get(accountIterator).get("username"));
    	    		weibo.setToken(accessToken);
            	}
            	
                //调度算法，每次调用API有一段间隔时间
                //Thread.sleep(8 * 1000); //暂停8s
                Calendar c = Calendar.getInstance();
                c.add(Calendar.HOUR, 1);
                c.set(Calendar.MINUTE, 0);
                c.set(Calendar.SECOND, 0);
                long ss = (c.getTimeInMillis() - System.currentTimeMillis());//当前时间到下一个整点的时间
                Thread.sleep(ss / (remaining_hits + sinaAccount.getAvailableNum() * json.getUserLimit()));*/
            }   
        }catch (Exception e) {
            	e.printStackTrace();
            	System.out.println("接口调用出现问题，可能是超出接口限制次数或者其他原因。");
        } finally{
            		System.out.println("当前调用API接口次数为：" + apiTimes + "。");
            		System.out.println("一共抓取"+weiboSize+"条微博。");
        }
    }   	

    /**
     * 抓取逻辑
     */
    public static void getSina() throws Exception
    {
		String startPoint = "1831398050";
		getAccessTokenList(); 		//登陆所有测试账号并获取所有access token
		//MicroblogDataDaoImpl mbddi = new MicroblogDataDaoImpl();
		//mbddi.createTable("SinaTable");		//创建SinaTable表
		ArrayList<String> totalList = new ArrayList<String>();
		totalList.add(startPoint);
		MicroblogUser startUser = getUserInfoByID(startPoint);
		ArrayList<String> idolsIDs = getUserIdolsList(startUser);
		totalList.addAll(idolsIDs);
		ArrayList<String> fansNames = getUserFansList(startUser);
		totalList.addAll(fansNames);
		MicroblogDataDaoImpl mbddi = new MicroblogDataDaoImpl();
		mbddi.createTable("SinaTable");		//创建SinaTable表
		for(String idx : totalList)
		{
			MicroblogUser user = getUserInfoByID(idx);
			MicroblogDataDaoImpl mbddi_a = new MicroblogDataDaoImpl();
			ArrayList<MicroblogData> statusesList = getUserStatuses(user);
			if(statusesList != null)
			{
				mbddi_a.addData(statusesList, "SinaTable");
			}
		}
    }

	public static void main(String[] args)  throws WeiboException, Exception  {
		getSina();
	}

	@Override
	public Boolean init(String lsWeiboType) {
		// 获取访问令牌并初始化weibo对象
		accessToken = getAccessToken();
		weibo.setToken(accessToken);        //设置access_token		
		
		//初始化操作HBase的对象
		HBaseFactory hf=new HBaseFactory();
		hbaseObject=hf.getInstance();		
		return true;
	}

	@Override
	public String getAccessToken() {
		// TODO Auto-generated method stub
    	Oauth oauth = new Oauth();
		try {
			//打开浏览器窗口
			BareBonesBrowserLaunch.openURL(oauth.authorize("code"));
		} catch (WeiboException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		//从控制台读取授权码
		System.out.print("输入浏览器地址栏code的值，按Enter。[Enter]:");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String code=null;
		try {
			code = br.readLine();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		//根据授权码获取访问令牌
		try{
			String access_token = oauth.getAccessTokenByCode(code).getAccessToken();
			System.out.println(access_token);
			return access_token;
		} catch (WeiboException e) {
			if(401 == e.getStatusCode()){
				//Log.logInfo("Unable to get the access token.");
			}else{
				e.printStackTrace();
			}
		}
    	return null;
	}

	@Override
	public List<com.scnu.crawler.util.Status> getDataByID(String accessToken,
			String uid, String sinceID, String maxID) {

		//定义返回结果的对象
		List<Status> dataList = new LinkedList<Status>();
        SimpleDateFormat t = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        Integer pageNo=0,pageCount =0;
        String localSinceID=sinceID;
        
	
        //创建用户对象
        try {
			User user = new Users().showUserById(uid);
		} catch (WeiboException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        
         //创建timeline对象(在  sina的TimeLine对象上增加了sinceID和maxID函数)
        sinaTimeLine tm = new sinaTimeLine();
        
        //初始化循环退出标志
        Boolean stopFlag = true;		
    	//通过sina的微博开放接口读取weibo数据        
        do {
        	
        	//初始化变量
        	StatusWapper statusList;
        	pageNo = pageNo+1;
        	
			try {
				
				//获取数据并检查返回的数据条数，没有返回数据则循环终止
				statusList = tm.getUserTimelineByUid(uid,new Paging(pageNo,StatusPageCounts),0,0,localSinceID,maxID);
				if (statusList.getTotalNumber()<1) {
					stopFlag = false;
				}
				
				pageCount = 0;
				//循环处理结果
	         	for(weibo4j.model.Status s : statusList.getStatuses()){        
	        		
	         		//记录一次读取的weibo条数
	         		pageCount = pageCount+1;
	         		
	         		//从 sina的status对象中复制数据到系统的status类
	         		Status statusTemp  = new Status();
	         		//记录读取范围内最后一条weibo的id
	         		localSinceID = s.getId();
	         		statusTemp.setId(localSinceID);
	         		
	         		statusTemp.setText(s.getText());
	         		statusTemp.setAnnotations(s.getAnnotations());
	         		statusTemp.setCreatedAt(s.getCreatedAt());
	         		statusTemp.setGeo(s.getGeo());
	         		statusTemp.setLatitude(s.getLatitude());
	         		statusTemp.setLongitude(s.getLongitude());
	         		statusTemp.setMid(s.getMid());
	         		//statusTemp.setSource(s.getSource());
	         		statusTemp.setRepostsCount(s.getRepostsCount());
	         		//statusTemp.setUser(s.getUser());
	         		
	         		//写入数据到List中
	         		dataList.add(statusTemp);
	         	}
	         	
			} catch (WeiboException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
      
	
        } while(stopFlag);
        
        //返回数据
        return dataList;
	}

	@Override
	public List<com.scnu.crawler.util.Status> getData(String accessToken,
			String uid, String sinceID, String maxID) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<com.scnu.crawler.util.User> getFriends(String accessToken,
			String uid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<com.scnu.crawler.util.User> getFollows(String accessToken,
			String uid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<com.scnu.crawler.util.Comment> getComments(String accessToken,
			String wbID) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer getRemainHits() {
		// TODO Auto-generated method stub
        //创建sinaTimeline对象
        sinaTimeLine tm = new sinaTimeLine();	
        
        //初始化变量
        Integer remainHits = 1000;
        String ls_remainHits = null;
        
        //获取可访问次数
		try {
			Response sinaLimit  = tm.getUserLimit(accessToken);
			JSONObject jsonStatus = sinaLimit.asJSONObject();
			ls_remainHits = jsonStatus.getString("remaining_user_hits");
			remainHits= Integer.getInteger(ls_remainHits);
			
		} catch (WeiboException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return remainHits;
	}

	@Override
	public String getLastID(String UID) {
		// TODO Auto-generated method stub
		
		String lastID=null;
		//(1)检查weiboSchedule这个HBase表是否存在
		hbaseObject.createTable("weiboSchedule");
		
		//(2) 获取HTable对象
		HTable tempHT = hbaseObject.getHTable("weiboSchedule");
		
		//(3) 获取数据
		Result tempResult = hbaseObject.query(tempHT, UID);
   	  	for(KeyValue kv:tempResult.raw()){
   	  		if (kv.matchingColumn(Bytes.toBytes("data"), Bytes.toBytes("sinceID"))) {
   	  			lastID = kv.getValue().toString();
   		        //System.out.println(new String(kv.getValue()));   	  			
   	  		}
	    }		
		//tempResult.
		
		return lastID;
	}


	@Override
	public Boolean setLastID(String UID, String weiboID) {
		// TODO Auto-generated method stub
		//(1)检查weiboSchedule这个HBase表是否存在
		hbaseObject.createTable("weiboSchedule");
		
		//(2) 获取HTable对象
		HTable tempHT = hbaseObject.getHTable("weiboSchedule");
		
		Date now = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");		
		
		//(3)写数据到HTable中（ weiboID、accessDate）
		hbaseObject.insertData(tempHT, "weiboSchedule", UID, "data", "sinceID",weiboID);
		hbaseObject.insertData(tempHT, "weiboSchedule", UID, "data", "accessDate",sdf.format(now));		
		return true;
	}


	@Override
	/**
	 * 按照制定的调度策略执行数据抓取策略
	 */
	public Boolean doCrawler() {
		// TODO Auto-generated method stub
		
		//1、获取需要读取数据的微博账户列表,该列表周期性进行更新
		return true;
	}

}
