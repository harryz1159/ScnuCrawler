package com.scnu.crawler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.hadoop.hbase.client.HTable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.microblog.common.login.TencentLogin;
import com.microblog.common.model.MicroblogData;
import com.microblog.common.model.MicroblogUser;
import com.scnu.crawler.util.CrawlerConfig;
import com.tencent.weibo.api.StatusesAPI;
import com.tencent.weibo.api.UserAPI;
import com.tencent.weibo.oauthv2.OAuthV2;
import com.microblog.common.dao.impl.hbase.MicroblogDataDaoImpl;
import com.microblog.common.dao.impl.hbase.MicroblogUserSimpleDaoImpl;
import com.microblog.common.dao.impl.hbase.UserListDaoImpl;
import com.tencent.weibo.api.*;

public class TencentCrawler {
	//private static int remaining_hits = rateLimitOfSingleAccount; //剩余调用次数
	private static final int delta = 0; //用以调节调用频率，暂时根据经验设一个值，腾讯服务器所记录的剩余调用次数可能小于1000
	private static OAuthV2 oAuth/*=new OAuthV2()*/;
	//private static ArrayList<OAuthV2> oAuths = new ArrayList<OAuthV2>();        //保存有accessToken的OAuthV2对象
    private static  int a = 0;				//微博条数
   // private  int b = 0;				//评论条数
    private static int apiTimes = 0;		//api调用次数
    //static int fansNum = 0; //调试用

	private static String usersSimpleInfoTableNameInHBase = "TencentUserSimpleInfo"; 	//指定HBase中用户表名称
	private static String statusesTableNameInHBase = "TencentTable"; 	//指定HBase中微博数据表名称
	private static String currentUserTableNameInHBase = "TencentCurrentUserList"; 	//当前访问的账户列表
	private static String nextUserTableNameInHBase = "TencentNextUserList"; 	//下一个循环将要访问的账户列表
    private static long MIN_INTERVAL = 60*60*1000; 	//两次抓取同一个帐号的最小时间间隔（单位为毫秒）
	
    /**
     * 登录帐号并设置oAuth成员变量
     */
    public static void getAccessToken() throws Exception
    {
    	oAuth = TencentLogin.gainAccessToken("1838572665", "scnu123456");	//登录并获得access token
    }
    
    /**
     * 时间戳转换成Date
     */
	public static String TimeStamp2Date(String timestampString){  
		Long timestamp = Long.parseLong(timestampString)*1000;  
		String date = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(timestamp));  
		return date;  
	}  
	    
	/**
	 * 获取某个用户信息(从HBase中获取sinceCreateTime字段和sinceCollectTime字段)
	 * 该方法调用UserAPI，但不做延时处理，因此使用该方法时必须考虑API调用频率
	 */
	public static MicroblogUser getUserInfoByName(String userName) throws Exception
	{//该方法未考虑腾讯服务器没有该用户名存在
		if(userName == null || userName == "")
		{//没有用户名信息则返回null
			return null;
		}
		final int tryUpperLimit = 3;		//超时重试次数上限
		UserAPI userAPI = new UserAPI(oAuth.getOauthVersion());
		MicroblogUser user = new MicroblogUser();
		/*
		 *以下for循环处理超时重试 
		 */
		for(int tryTimes = 0; /*tryTimes < tryUpperLimit*/; /*++tryTimes*/)
		{//尝试获取微博数据，最多重试tryUpperLimit次
			if(tryTimes >= tryUpperLimit)
			{//达到重试次数上限，返回null
				System.out.println("超时重试次数达到上限："+ tryTimes +"次，将跳过该用户！");
				return null;
			}
			try
			{
				String userJson = userAPI.otherInfo(oAuth, "json", userName, "");	//这一句调用了user接口
				++apiTimes;
				JSONObject userObj = new JSONObject(userJson);
				user.setUserId(userObj.getJSONObject("data").getString("openid"));
				user.setUserName(userObj.getJSONObject("data").getString("name"));
				user.setNickName(userObj.getJSONObject("data").getString("nick"));
				user.setProvince(userObj.getJSONObject("data").getString("province_code"));
				user.setGender(userObj.getJSONObject("data").getString("sex"));
				user.setFansCount(userObj.getJSONObject("data").getInt("fansnum"));
				user.setIdolsCount(userObj.getJSONObject("data").getInt("idolnum"));
				user.setStatusesCount(userObj.getJSONObject("data").getInt("tweetnum"));
				MicroblogUserSimpleDaoImpl mbusd = new MicroblogUserSimpleDaoImpl();
				user.setSinceCreateTime(mbusd.getSinceCreateTime(user, usersSimpleInfoTableNameInHBase, "Tencent")); 	//从HBase中获取sinceCreateTime字段
				user.setSinceCollectTime(mbusd.getSinceCollectTime(user, usersSimpleInfoTableNameInHBase, "Tencent")); //从HBase中获取sinceCollectTime字段
				break;	//正确获取到信息，跳出循环，不计入重试次数
			} catch(SocketTimeoutException | ConnectTimeoutException e)
			{
				++apiTimes;		//尽管发生超时，但服务器可能已经记录了调用次数
				++tryTimes;		//重试计数增加1
				System.out.println("网络超时(连接或读取超时)。。。\n将在3.6秒后重试。。。");
				Thread.sleep(3600);
			} catch(JSONException e)
			{
				System.out.println("JSON解析出错，有可能是服务器没有返回此用户信息。");
				userAPI.shutdownConnection();
				return null;
			} catch(Exception e)
			{
				System.out.println("其他未知网络错误，将跳过此用户。");
				userAPI.shutdownConnection();
				return null;
			}
		}
		//Thread.sleep(3600 + delta);
		userAPI.shutdownConnection();
		return user;
	}
	
	/**
	 * @throws JSONException 
	 **/
	private static MicroblogData Status2MicroblogData(JSONObject statusesItem) throws JSONException
	{
		SimpleDateFormat t = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		MicroblogData mdata = new MicroblogData();
		try
		{
		    mdata.setMicroblogID(statusesItem.getString("id"));
	    	mdata.setText(statusesItem.getString("text"));
	    	mdata.setPicSrc(statusesItem.getString("image"));
	    	mdata.setNickName(statusesItem.getString("nick"));
	    	mdata.setCreatTime(TimeStamp2Date(statusesItem.getString("timestamp")));
	    	mdata.setCollectTime(t.format(new Date()));
	    	mdata.setUserID(statusesItem.getString("openid"));
	    	mdata.setUserName(statusesItem.getString("name"));
	    	mdata.setRepostsCount(Integer.toString(statusesItem.getInt("count")));
	    	mdata.setCommentsCount(Integer.toString(statusesItem.getInt("mcount")));
	    	mdata.setType(statusesItem.getString("type")); 	//设置微博类型
	    	String sourceTweeter = null;
	    	try
	    	{
	    		sourceTweeter = statusesItem.getString("source");
	    		System.out.println("sourceTweeter:" + sourceTweeter);
	    	} catch(JSONException e)
	    	{
	    		//e.printStackTrace();
	    		System.out.println("该微博本身就是源微博，没有source字段！");
	    	}
	    	if(sourceTweeter != null && !sourceTweeter.equalsIgnoreCase("null"))
	    	{
	    		JSONObject sourceTweeterObj = new JSONObject(sourceTweeter);
	    		mdata.setSourceId(sourceTweeterObj.getString("id"));
	    	}
		} catch (JSONException e)
		{
			e.printStackTrace();
			System.out.println("可能是源微博被删除！");
			return null;
		}
    	return mdata;
	}
	
	/**
	 * 获得指定用户名的所有微博内容
	 * @return 
	 */
	public static ArrayList<MicroblogData> getUserStatuses(MicroblogUser user) throws Exception
	{
		SimpleDateFormat t = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		int weiboCount = 0;
		boolean isFirstTime = true; 	//每次运行时设为true，用以记录该次运行时对应user抓取的第一条微博创建时间
		String localSinceCreateTime = ""; 	//记录本次运行所抓取的第一条微博的创建时间
		if(user == null)
		{
			return null;
		}
		System.out.println("正准备获取" + user.getUserName() + "的微博内容。。。");
		Thread.sleep(3600);
		final int tryUpperLimit = 3;		//超时重试次数上限
		int StatusPageCounts = 70;         //翻页微博数,最多每页70条
		StatusesAPI statusesAPI = new StatusesAPI(oAuth.getOauthVersion());
		ArrayList<MicroblogData> statusesList = new ArrayList<MicroblogData>();		//微博数据列表
		String statusesJson = null;
		/*
		 *以下for循环处理超时重试 
		 */
		for(int tryTimes = 0; /*tryTimes < tryUpperLimit*/; /*++tryTimes*/)
		{//尝试获取微博数据，最多重试tryUpperLimit次
			if(tryTimes >= tryUpperLimit)
			{//达到重试次数上限，返回null
				System.out.println("超时重试次数达到上限："+ tryTimes +"次，将跳过当前用户剩余微博数据！");
				return null;
			}
			try{
				statusesJson = statusesAPI.userTimeline(oAuth, "json", "0", "0", Integer.toString(StatusPageCounts), 
						  "0", user.getUserName(), "", "3", "0");			//从第一页开始抓取，这一句调用了读接口
			    ++apiTimes;	
			    break;		//正确获取到信息，跳出循环，不计入重试次数
			} catch(SocketTimeoutException | ConnectTimeoutException e)
			{
				++apiTimes;		//尽管发生超时，但服务器可能已经记录了调用次数
				++tryTimes;		//重试计数增加1
				System.out.println("可能是网络超时。。。\n将在3.6秒后重试。。。");
				Thread.sleep(3600);
			} catch(Exception e)
			{
				System.out.println("其他未知网络错误，将跳过当前用户剩余信息。");
				statusesAPI.shutdownConnection();			//关闭连接
				return null;
			}
		}
		
	    /*
	     * 获取某个用户的所有微博内容
	     * 以下代码段没有调用API
	     */
	    System.out.println("大约有" + user.getStatusesCount() + "条微博。");
	    Thread.sleep(3 * 1000);
	    for(int i = 0; i < user.getStatusesCount()/StatusPageCounts+1; ++i)
	    {//循环的停止条件有点问题，当tweetnum=StatusPageCounts时，实际上只有tweetnum/StatuPagesCounts页数据，因此最后一次循环是没有数据的
	    	String pageTime = null;		//上一次请求返回的最后一条记录时间
	    	String lastID = null;			//上一次请求返回的最后一条记录id
			try
			{			    	   
			    JSONObject statusesObj = new JSONObject(statusesJson);	  
				JSONArray statusesInfo = new JSONArray(statusesObj.getJSONObject("data").getString("info"));
				JSONObject statusesItem; 
				//SimpleDateFormat t = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				    
				//获取每一次分页的微博
				for(int j = 0; j < statusesInfo.length(); j++)
				{//将json格式的对象转换成MicroblogData对象
					statusesItem = (JSONObject) statusesInfo.get(j);
					String createTime = TimeStamp2Date(statusesItem.getString("timestamp"));
					//System.out.println(createTime);
					//System.out.println(user.getSinceCreateTime());
					//Thread.sleep(3000);
            		if(isFirstTime == true)
            		{//记录本次运行所抓取的第一条微博创建时间
            			localSinceCreateTime = createTime;
            			isFirstTime = false;
            		}
            		if(createTime.compareTo(user.getSinceCreateTime()) <= 0)
            		{//上次已抓取过的数据不再抓取
            			System.out.println("本页内容已经在上次运行时抓取过，本页及以后的数据不再重复抓取。");
            	        System.out.println("接口调用" + apiTimes + "次。");
            	        System.out.println("本次获取" + weiboCount + "条微博内容。");
            	        System.out.println("到目前为止共获取了" + a + "条微博内容。");
            	        Thread.sleep(3000);
            	        /*
            	         * 将localSinceCreateTime写入数据库
            	         */
            	        MicroblogUserSimpleDaoImpl mbusd = new MicroblogUserSimpleDaoImpl();
            	        user.setSinceCreateTime(localSinceCreateTime); 	//更新user的sinceCreateTime字段
            	        user.setSinceCollectTime(t.format(new Date())); //更新user的sinceCollectTime字段
            	        mbusd.updateSimpleUserInfo(user, usersSimpleInfoTableNameInHBase, "Tencent"); 	//将sinceCreaeteTime字段和sinceCollectTime字段写入HBase
            	        return statusesList;
            		}

            		MicroblogData mdata = Status2MicroblogData(statusesItem);
	            	System.out.println(mdata.getText() + mdata.getMicroblogID());	//用来在控制台查看是否抓取到数据
	            	if(mdata != null)
	            	{
	            		statusesList.add(mdata);
	            	}
	            	String sourceTweeter = statusesItem.getString("source");
	            	if(!sourceTweeter.equalsIgnoreCase("null"))
	            	{//将源微博内容添加进列表
	            		JSONObject sourceTweeterObj = new JSONObject(sourceTweeter);
	            		System.out.println("源微博" + sourceTweeterObj.getString("text"));
	            		MicroblogData sourceData = Status2MicroblogData(sourceTweeterObj);
	            		if(sourceData != null)
	            		{//防止源微博被删除的情况
	            			statusesList.add(sourceData);
	            		}
	            	}
				    a++;		
				    ++weiboCount;
				    	  
				    //Thread.sleep((3600 / (rateLimitOfSingleAccount * tencentAccount.getAccountsNum())) * 1000 + delta);

				    if(j == statusesInfo.length() - 1)
				    {//保存微博下一页参数
				    	pageTime = statusesItem.getString("timestamp");
				    	lastID = statusesItem.getString("id");
				    }
				}
			}catch(Exception e){
				e.printStackTrace();
			    System.out.println("服务器没有返回本页内容。");		//可能是服务器返回出错，也可能是临界条件（刚好有70的整数倍条微博）
			}			
			System.out.println("正在获取" + user.getUserName() + "的微博数据。。。");
			Thread.sleep(3600 + delta);			//控制接口调用频率，大约3.6秒
			/*
			 *以下for循环处理超时重试 
			 */
			for(int tryTimes = 0; /*tryTimes < tryUpperLimit*/; /*++tryTimes*/)
			{//尝试获取微博数据，最多重试tryUpperLimit次
				if(tryTimes >= tryUpperLimit)
				{//达到重试次数上限，跳过剩余信息，并返回已获取的信息
					System.out.println("超时重试次数达到上限："+ tryTimes +"次，将跳过当前用户剩余微博数据！");
					statusesAPI.shutdownConnection();			//关闭连接
					return statusesList;		//返回已经获取的微博内容
				}
				try
				{
					statusesJson = statusesAPI.userTimeline(oAuth, "json", "1", pageTime, Integer.toString(StatusPageCounts), 
							  lastID, user.getUserName(), "", "3", "0");			//调用腾讯微博api读接口，向下翻页
					++apiTimes;
					break;		//正确获取到信息，跳出循环
				} catch(SocketTimeoutException | ConnectTimeoutException e)
				{
					++apiTimes;		//尽管发生超时，但服务器可能已经记录了调用次数
					++tryTimes;		//重试计数增加1
					System.out.println("可能是网络超时。。。\n将在3.6秒后重试。。。");
					Thread.sleep(3600);
				} catch(Exception e)
				{
					System.out.println("其他未知网络错误，将跳过当前用户剩余信息。");
					statusesAPI.shutdownConnection();			//关闭连接
					return statusesList;
				}
			}
		}
		    
	    System.out.println("接口调用" + apiTimes + "次。");
	    System.out.println("本次获取了" + weiboCount + "条微博。");
	    System.out.println("到目前为止共获取了" + a +"条微博。");		//最后一页总有若干条微博数据不会返回。即使某账户只有3条微博也只返回2条。应该是腾讯服务器那边的原因。
	    Thread.sleep(3000);
	    /*
         * 将localSinceCreateTime写入数据库
         */
        MicroblogUserSimpleDaoImpl mbusd = new MicroblogUserSimpleDaoImpl();
        user.setSinceCreateTime(localSinceCreateTime); 	//更新user的sinceCreateTime字段
        user.setSinceCollectTime(t.format(new Date())); //更新user的sinceCollectTime字段
        mbusd.updateSimpleUserInfo(user, usersSimpleInfoTableNameInHBase, "Tencent"); 	//将sinceCreaeteTime字段和sinceCollectTime字段写入HBase
	    
        statusesAPI.shutdownConnection();			//关闭连接
	    return statusesList;
	}
	
    /**
     * 获得指定用户微博内容，并写入HBase
     * 该方法依赖于方法:getUserStatuses(MicroblogUser user)
     * @return 
     **/
	public static void getUserStatuses(MicroblogUser user, String hbaseTableName) throws Exception
	{
		MicroblogDataDaoImpl mbddi = new MicroblogDataDaoImpl();
		if(mbddi.isTableExists(hbaseTableName) == false)
		{//表不存在则创建该表
			mbddi.createTable(hbaseTableName);
		}
		mbddi.addData(getUserStatuses(user), hbaseTableName); 	//调用本类中的getUserStatuse（user）方法
		System.out.println("用户" + user.getUserName() + "的微博数据写入HBase成功！");
	}
	
	/**
	 * 指定用户偶像列表
	 * 该方法调用FriendsAPI，已做延时处理
	 */
	public static ArrayList<String> getUserIdolsList(MicroblogUser user) throws Exception
	{//	    	
		if(user == null)
		{
			return null;
		}
		final int tryUpperLimit = 3;		//超时重试次数上限
	    /*
	     * 获得指定用户的偶像信息
	     */
	    FriendsAPI friendsAPI = new FriendsAPI(oAuth.getOauthVersion());
	    ArrayList<String> idolNameList = new ArrayList<String>();
	    //System.out.println("总共有" + user.getIdolsCount() + "个偶像。");
	    
	    Thread.sleep(3600);
	    int idolNum = 0;
	    int IdolPageCounts = 30;			//翻页粉丝数,最多每页30个
	    for(int i = 0; i < user.getIdolsCount()/IdolPageCounts +1; ++i)
	    {
	    	String userIdols = null;
			/*
			 *以下for循环处理超时重试 
			 */
			for(int tryTimes = 0; /*tryTimes < tryUpperLimit*/; /*++tryTimes*/)
			{//尝试获取微博数据，最多重试tryUpperLimit次
				if(tryTimes >= tryUpperLimit)
				{//达到重试次数上限，跳过剩余信息，返回已经获取的信息
				 //如果达到重试上限仍不能正确获取信息，说明网络状况很差，或其他未知且无法解决的网络错误，因此直接跳过剩余的信息
					System.out.println("超时重试次数达到上限："+ tryTimes +"次，将跳过该用户！");
					return idolNameList;
				}
				try
				{
			    	userIdols = friendsAPI.userIdollist(oAuth, "json", "30", Integer.toString(IdolPageCounts * i), user.getUserName(), "", "0");
					++apiTimes;
					break;		//正确获取到信息，跳出循环，不计入重试次数
				} catch(SocketTimeoutException | ConnectTimeoutException e)
				{
					++apiTimes;		//尽管发生超时，但服务器可能已经记录了调用次数
					++tryTimes;		//重试计数增加1
					System.out.println("网络链接超时。。。\n将在3.6秒后重试。。。");
					Thread.sleep(3600);
				} catch(Exception e)
				{
					System.out.println("其他未知网络错误，将跳过当前页。");
					break;
				}
			}
			if(userIdols == null)
			{//获取不到当前页信息则跳过本次循环，主要是针对跳过当前页的操作
				continue;
			}
			JSONObject userIdolsListObj = new JSONObject(userIdols);	
		    try
		    {
		    	JSONArray userIdolsListInfo = new JSONArray(userIdolsListObj.getJSONObject("data").getString("info"));
				JSONObject userIdolsItem;
				for(int j = 0; j < userIdolsListInfo.length(); ++j)
				{//腾讯FriendsAPI没有提供"tweetnum"字段，该字段对本程序很重要，因此只能再调用一下UserAPI来获取
					userIdolsItem = (JSONObject)userIdolsListInfo.get(j);
					String userName = userIdolsItem.getString("name");
				    if(userName != "")
				    {
				    	idolNameList.add(userName);
				    	++idolNum;
				    }
				    System.out.println(userName);
				}
		    } catch(Exception e)
		    {
		    	System.out.println("服务器没有返回更多偶像列表。");
		    	break;			//腾讯微博获取偶像有个数限制，最多大约9000个
		    }
		    System.out.println("正在获取" + user.getUserName() + "的偶像数据。。。");
		    Thread.sleep(3600 + delta);			//控制接口调用频率，大约3.6秒
	    }
	    System.out.println("共获取偶像id" + idolNum + "个。");
		friendsAPI.shutdownConnection();			//关闭连接
		return idolNameList;
	}
	
	/**
	 * 获得指定用户id的偶像列表(userName),并写入HBase（不存在的项才写入）
	 * 将应用户的sinceCreateTime信息和sinceCollectTime信息保存到HBase
	 * 该方法依赖ArrayList<String> getUserIdolsList(MicroblogUser user)方法
	 * @throws Exception 
	 * */
	public static ArrayList<String> getUserIdolsList(MicroblogUser user, String usersTableNameInHBase) throws Exception
	{
		ArrayList<String> idolsList = getUserIdolsList(user);
		MicroblogUserSimpleDaoImpl mbusd = new MicroblogUserSimpleDaoImpl();
		HTable hTable = mbusd.getTableByName(usersTableNameInHBase);
		for(String s : idolsList)
		{
			if(mbusd.isRowExists(s, hTable) == false)
			{//查询该用户的简单信息（目前只保存sinceCreateTime字段和sinceCollectTime字段）是否在HBase中存在
				/*
				 * 生成只有userName等简单信息的MicroblogUser实例
				 * */
				System.out.println("正在向HBase写入用户简单信息（sinceCreateTime字段和sinceCollectTime字段）...");
				MicroblogUser u = new MicroblogUser();
				u.setUserName(s); 	//注意腾讯微博是以用户名为唯一标识
				u.setSinceCreateTime(""); 	//默认为“”
				u.setSinceCollectTime(""); 	//默认为“”
				mbusd.updateSimpleUserInfo(u, hTable, "Tencent");
			}
		}
		System.out.println("用户信息保存完成！");
		return idolsList;
	}
	
	/**
	 * 指定用户粉丝列表
	 * 该方法调用FriendsAPI，已做延时处理
	 * 返回用户名
	 */
	public static ArrayList<String> getUserFansList(MicroblogUser user) throws Exception
	{
		if(user == null)
		{
			return null;
		}
		final int tryUpperLimit = 3;		//超时重试次数上限
	    /*
	     * 获得指定用户的偶像信息
	     */
	    FriendsAPI friendsAPI = new FriendsAPI(oAuth.getOauthVersion());
	    ArrayList<String> fansNameList = new ArrayList<String>();
	    //System.out.println("总共有" + user.getFansCount() + "个粉丝。");
	    
	    Thread.sleep(3600);
	    int fansNum = 0;
	    int FansPageCounts = 30;			//翻页粉丝数,最多每页30个
	    for(int i = 0; i < user.getFansCount()/FansPageCounts +1; ++i)
	    {
	    	String userFans = null;
			/*
			 *以下for循环处理超时重试 
			 */
			for(int tryTimes = 0; /*tryTimes < tryUpperLimit*/; /*++tryTimes*/)
			{//尝试获取微博数据，最多重试tryUpperLimit次
				if(tryTimes >= tryUpperLimit)
				{//达到重试次数上限，跳过剩余信息，返回已经获取的信息
				 //如果达到重试上限仍不能正确获取信息，说明网络状况很差，或其他未知且无法解决的网络错误，因此直接跳过剩余的信息
					System.out.println("超时重试次数达到上限："+ tryTimes +"次，将跳过该用户！");
					return fansNameList;
				}
				try
				{
					userFans = friendsAPI.userFanslist(oAuth, "json", "30", Integer.toString(FansPageCounts * i), user.getUserName(), "", "1", "0");	//此处调用FriendAPI
					++apiTimes;
					break;		//正确获取到信息，跳出循环，不计入重试次数
				} catch(SocketTimeoutException | ConnectTimeoutException e)
				{
					++apiTimes;		//尽管发生超时，但服务器可能已经记录了调用次数
					++tryTimes;		//重试计数增加1
					System.out.println("网络链接超时。。。\n将在3.6秒后重试。。。");
					Thread.sleep(3600);
				} catch(Exception e)
				{
					System.out.println("其他未知网络错误，将跳过当前页。");
					break;
				}
			}
			if(userFans == null)
			{//获取不到json则跳出本次循环，主要是针对跳过本页内容的操作
				continue;
			}
			JSONObject userFansListObj = new JSONObject(userFans);
		    try
		    {
		    	JSONArray userFansListInfo = new JSONArray(userFansListObj.getJSONObject("data").getString("info"));
				JSONObject userFansItem;
				for(int j = 0; j < userFansListInfo.length(); ++j)
				{//腾讯FriendsAPI没有提供"tweetnum"字段，该字段对本程序很重要，因此只能再调用一下UserAPI来获取
					userFansItem = (JSONObject)userFansListInfo.get(j);
					String userName = userFansItem.getString("name");
					if(userName != "")
					{//测试发现有些用户名不存在
						fansNameList.add(userName);
						++fansNum;
					}
				    System.out.println(userName);				    
				}
		    } catch(Exception e)
		    {
		    	System.out.println("服务器没有返回更多粉丝列表。");
		    	break;			//腾讯微博获取偶像有个数限制，最多大约9000个
		    }
		    System.out.println("正在获取" + user.getUserName() + "的粉丝数据。。。");
		    Thread.sleep(3600 + delta);			//控制接口调用频率，大约3.6秒
	    }
	    System.out.println("共获取粉丝" + fansNum + "个。");
		friendsAPI.shutdownConnection();			//关闭连接
		return fansNameList;
	}
		
	/**
	 * 获得指定用户id的粉丝列表(userName),并写入HBase（不存在的项才写入）
	 * 将对应用户的sinceCreateTime信息和sinceCollectTime信息保存到HBase
	 * 该方法依赖ArrayList<String> getUserFansList(MicroblogUser user)方法
	 * @throws Exception 
	 * */
	public static ArrayList<String> getUserFansList(MicroblogUser user, String usersTableNameInHBase) throws Exception
	{
		ArrayList<String> fansList = getUserFansList(user); 	//调用本类中的getUserFansList方法
		MicroblogUserSimpleDaoImpl mbusd = new MicroblogUserSimpleDaoImpl();
		HTable hTable = mbusd.getTableByName(usersTableNameInHBase);
		for(String s : fansList)
		{
			if(mbusd.isRowExists(s, hTable) == false)
			{//查询该用户的简单信息（目前只保存sinceCreateTime字段和sinceCollectTime字段）是否在HBase中存在
				/*
				 * 生成只有userName等简单信息的MicroblogUser实例
				 * */
				System.out.println("正在向HBase写入用户简单信息（sinceCreateTime字段和sinceCollectTime字段）...");
				MicroblogUser u = new MicroblogUser();
				u.setUserName(s); 	//注意腾讯微博是以用户名为唯一标识
				u.setSinceCreateTime(""); 	//默认为“”
				u.setSinceCollectTime(""); 	//默认为“”
				mbusd.updateSimpleUserInfo(u, hTable, "Tencent");
			}
		}
		System.out.println("用户信息保存完成！");
		return fansList;
	}
	
	/**
	 * 
	 * */
	private static long SimpleDate2TimeStamp(String simpleDate)
	{
		long timeStamp = 0;
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			Date date = df.parse(simpleDate);
			timeStamp = date.getTime();
		} catch (ParseException e) {
			//e.printStackTrace();
		}
		return timeStamp;
	}
	
	/**
	 * 剪枝条件
	 */
	public static boolean isPruning(MicroblogUser user) throws Exception
	{
		if(user.getFansCount() > 2)
	    {
	    	return false;
	    }
	    if(user.getIdolsCount() > 2)
	    {
	    	return false;
	    }
        Date currentDate = new Date();
        long currentTimeStamp = currentDate.getTime();
        long sinceCollectTimeStamp = SimpleDate2TimeStamp(user.getSinceCollectTime());
        if((currentTimeStamp - sinceCollectTimeStamp) > MIN_INTERVAL)
        {//两次抓取同一帐号的时间间隔（上次抓取完毕的时间和这次准备抓取的时间间隔）大于一定值则抓取该用户的微博数据，否则直接跳过
        	return false;
        }
	    return true;
	}

    
    /**
     * 访问节点（用户）：抓取该节点的微博数据，获取偶像粉丝列表，并将这些数据写入HBase
     * @throws Exception 
     * */
    public static void visit(MicroblogUser user) throws Exception
    {
    	getUserStatuses(user, statusesTableNameInHBase); 	//抓取user的微博数据并写入HBase
    	UserListDaoImpl uldi = new UserListDaoImpl(); 	
    	uldi.appendData(getUserIdolsList(user, usersSimpleInfoTableNameInHBase), 
    			nextUserTableNameInHBase); 	//获取user的偶像列表，并暂时保存到HBase
    	uldi.appendData(getUserFansList(user, usersSimpleInfoTableNameInHBase), 
    			nextUserTableNameInHBase); 	//获取user的粉丝数据，并暂时保存到HBase
    }
    
    /**
     * 初始化
     * @throws Exception 
     * */
    public static void initliaze() throws Exception
    {
    	getAccessToken(); 		//登陆测试账号并获取所有access token 	
    	if(CrawlerConfig.getValue("Tencent_CurrentUserName").equals("") == true) 	//注意这一行不要写多了个分号，否则该if语句的执行体是空的
    	{//若Tencent_CurrentUserName不为“”则说明程序上次运行时异常中断(因此更改起始帐号必须将Tencent_CurrentUserName的值清空)
    		System.out.println("是否从起始帐号开始抓取？（第一次运行或修改了起始帐号请输入Y，否则输入N）");
    		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    		String usrCommond = br.readLine();
    		if(usrCommond.equals("Y") || usrCommond.equals("y"))
    		{
    			CrawlerConfig.updateProperties("Tencent_CurrentLayer", "0"); 	//设置当前遍历的层次数
    			String userName = CrawlerConfig.getValue("Tencent_StartUserName"); 	//获取用户设置的起始帐号
    			MicroblogUser user = new MicroblogUser();
    			user.setUserName(userName); 	//腾讯微博以用户名作为唯一标识
    			user.setSinceCollectTime("");
    			user.setSinceCreateTime("");
    			MicroblogUserSimpleDaoImpl musdi = new MicroblogUserSimpleDaoImpl();
    			musdi.createTable(usersSimpleInfoTableNameInHBase); 	//创建用户简单信息表（sinceCreateTime和sinceCollectTime字段）
    			musdi.updateSimpleUserInfo(user, usersSimpleInfoTableNameInHBase, "Tencent"); 	//将起始帐号的简单信息写入TencentUserSimpleInfo表
    			ArrayList<String> userList = new ArrayList<String>();
    			userList.add(userName);
    			UserListDaoImpl uldi = new UserListDaoImpl();
    			uldi.createTable(currentUserTableNameInHBase); 	//创建currentUserList表
    			uldi.appendData(userList, currentUserTableNameInHBase); 	//将起始账户名写入currentUserList表
    		}
    	}
    }
    
    /**
     * 抓取逻辑
     * @throws Exception 
     * */
    public static void crawlLogic() throws Exception
    {
    	initliaze();
    	String currentLayerString = CrawlerConfig.getValue("Tencent_CurrentLayer");
    	long currentLayer = 0; 	//当前遍历的层次
    	if(!currentLayerString.equals(""))
    	{//
    		currentLayer = Long.parseLong(currentLayerString); 	//从文件中读取当前遍历的层次
    	}
    	for(; ; )
    	{
    		int currentLayerNodeCount = 0;
    		String startRow = CrawlerConfig.getValue("Tencent_CurrentUserName"); 	//从上次程序中断时所抓取的用户开始抓取
    		UserListDaoImpl uldi = new UserListDaoImpl();
    		if(startRow.equals("") == true)
    		{//若currentUserID不为空，说明next表中有程序中断前保存的叶子节点
    			uldi.createTable(nextUserTableNameInHBase); 	//创建nextUserList表，若已存在则删除后再创建
    		}
    		for(ArrayList<String> rows; (rows = uldi.getRows(currentUserTableNameInHBase, startRow, 100000)).size() > 0; startRow = rows.get(rows.size() - 1))
    		{//分批获取currentUserList表的内容，以免内存不足
    			System.out.println(rows.size());
    			for(int i = 0; i < rows.size(); ++i)
    			{
    				String currentUserName = rows.get(i);
    				MicroblogUser user = getUserInfoByName(currentUserName); 	//调用接口获取user的完整信息
    				if(user != null)
    				{//防止用户被删除或者其他原因获取不到用户信息
        				if(isPruning(user) == true)
        				{
        					System.out.println("达到剪枝条件，跳过该节点（用户）！");
        				}
        				else
        				{
        					visit(user); 	//访问节点（用户）,叶子节点保存到nextUserList表中
        					CrawlerConfig.updateProperties("Tencent_CurrentUserName", currentUserName); 	//将当前正在访问的节点（用户）Name持久化，以支持断点
        					++currentLayerNodeCount;
        				}
    				}
    			}
    		}
    		uldi.createTable(currentUserTableNameInHBase); 	//清空currentUserList表（删除并重新创建）
    		System.out.println("正在拷贝数据，请勿停止程序。。。");
    		uldi.copyContent(nextUserTableNameInHBase, currentUserTableNameInHBase); 	//将nextUserList表的内容复制到currentUserList
    		CrawlerConfig.updateProperties("Tencent_CurrentUserName", ""); 	//当前层次遍历完成，重置Tencent_CurrentUserName
    		System.out.println("第" + currentLayer + "层遍历完毕。。。");
    		CrawlerConfig.updateProperties("Tencent_CurrentLayer", Long.toString(++currentLayer)); 	//将当前遍历的层数持久化
    		System.out.println("共有" + currentLayerNodeCount + "个节点（用户）。");
    		Thread.sleep(3600);
    	}
    }
    
	public static void main(String[] args) throws Exception {
		crawlLogic();
	}	
}

