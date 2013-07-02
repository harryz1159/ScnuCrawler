package com.scnu.crawler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import org.apache.hadoop.hbase.client.HTable;
import com.hjh.inf.OperateHbase;
import com.scnu.crawler.util.CrawlerConfig;
import weibo4j.Account;
import weibo4j.Friendships;
import weibo4j.Timeline;
import weibo4j.Users;
import weibo4j.Weibo;
import weibo4j.model.Paging;
import weibo4j.model.RateLimitStatus;
import weibo4j.model.StatusWapper;
import weibo4j.model.User;
import weibo4j.model.WeiboException;
import com.microblog.common.accounts.manager.SinaAccount;
import com.microblog.common.dao.impl.hbase.MicroblogDataDaoImpl;
import com.microblog.common.dao.impl.hbase.MicroblogUserSimpleDaoImpl;
import com.microblog.common.dao.impl.hbase.UserListDaoImpl;
import com.microblog.common.login.*;
import com.microblog.common.model.MicroblogData;
import com.microblog.common.model.MicroblogUser;

public class SinaCrawler {
	
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
	
	private static String usersSimpleInfoTableNameInHBase = "SinaUserSimpleInfo"; 	//指定HBase中用户表名称
	private static String statusesTableNameInHBase = "SinaTable"; 	//指定HBase中微博数据表名称
	private static String currentUserTableNameInHBase = "SinaCurrentUserList"; 	//当前访问的账户列表
	private static String nextUserTableNameInHBase = "SinaNextUserList"; 	//下一个循环将要访问的账户列表
	private static long MIN_INTERVAL = 30*60*1000; 	//两次抓取同一个帐号的最小时间间隔（单位为毫秒）
	
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
	 * 获得指定用户id的粉丝列表(uid),并写入HBase(不存在的项才写入)
	 * 将应用户的sinceCreateTime信息和sinceCollectTime信息保存到HBase
	 * 该方法依赖ArrayList<String> getUserFansList(MicroblogUser user)方法
	 * @throws Exception 
	 * */
	public static ArrayList<String> getUserFansList(MicroblogUser user, String usersTableNameInHBase) throws Exception
	{
		ArrayList<String> fansList = getUserFansList(user);
		MicroblogUserSimpleDaoImpl mbusd = new MicroblogUserSimpleDaoImpl();
		HTable hTable = mbusd.getTableByName(usersTableNameInHBase);
		for(String s : fansList)
		{
			if(mbusd.isRowExists(s, hTable) == false)
			{//查询该用户的简单信息（目前只保存sinceCreateTime字段和sinceCollectTime字段）是否在HBase中存在
				/*
				 * 生成只有userID等简单信息的MicroblogUser实例
				 * */
				System.out.println("正在向HBase写入用户简单信息（sinceCreateTime字段和sinceCollectTime字段）...");
				MicroblogUser u = new MicroblogUser();
				u.setUserId(s);
				u.setSinceCreateTime(""); 	//默认为“”
				u.setSinceCollectTime(""); 	//默认为“”
				mbusd.updateSimpleUserInfo(u, hTable, "Sina");
			}
		}
		System.out.println("用户信息保存完成！");
		return fansList;
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
	 * 获得指定用户id的偶像列表(uid),并写入HBase（不存在的项才写入）
	 * 将对应用户的sinceCreateTime信息和sinceCollectTime信息保存到HBase
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
				 * 生成只有userID等简单信息的MicroblogUser实例
				 * */
				System.out.println("正在向HBase写入用户简单信息（sinceCreateTime字段和sinceCollectTime字段）...");
				MicroblogUser u = new MicroblogUser();
				u.setUserId(s);
				u.setSinceCreateTime(""); 	//默认为“”
				u.setSinceCollectTime(""); 	//默认为“”
				mbusd.updateSimpleUserInfo(u, hTable, "Sina");
			}
		}
		System.out.println("用户信息保存完成！");
		return idolsList;
	}
	
	/**
	 * 获取用户信息(从HBase中获取sinceCreateTime字段和sinceCollectTime字段)
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
				MicroblogUserSimpleDaoImpl mbusd = new MicroblogUserSimpleDaoImpl();
				mUser.setSinceCreateTime(mbusd.getSinceCreateTime(mUser, usersSimpleInfoTableNameInHBase, "Sina")); 	//从HBase中获取sinceCreateTime字段
				mUser.setSinceCollectTime(mbusd.getSinceCollectTime(mUser, usersSimpleInfoTableNameInHBase, "Sina")); //从HBase中获取sinceCollectTime字段
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
	
	
    private static MicroblogData Status2MicroblogData(weibo4j.model.Status s)
	{
		SimpleDateFormat t = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		MicroblogData mdata = new MicroblogData();
		try
		{
			String createTime = t.format(s.getCreatedAt());
			mdata.setMicroblogID(s.getId());
			mdata.setText(s.getText());
			mdata.setPicSrc(s.getOriginalPic());
			mdata.setNickName(s.getUser().getScreenName());
			mdata.setCreatTime(createTime);	 //注意日期的显示格式，关系到数据库里的排序
			mdata.setCollectTime(t.format(new Date()));
			mdata.setUserID(s.getUser().getId());
			mdata.setUserName(s.getUser().getName());
			mdata.setRepostsCount(Integer.toString(s.getRepostsCount()));
			mdata.setCommentsCount(Integer.toString(s.getCommentsCount()));
			weibo4j.model.Status retweetedStatus = s.getRetweetedStatus();
			if(retweetedStatus != null)
			{
				mdata.setType("2"); 	//当前处理的微博为转发的微博
				mdata.setSourceId(retweetedStatus.getId()); 	//设置源微博Id
			}
			else
			{
				mdata.setType("1"); 	//当前处理的微博为原创微博
			}
		} catch(NullPointerException e)
		{
			System.out.println("微博被删除！");
			return null;
		}
		return mdata;
	}
	
    /**
     * 获得指定用户微博内容
     * @return 
     * */
	public static ArrayList<MicroblogData> getUserStatuses(MicroblogUser user) throws Exception
	{
		int weiboCount = 0;
		boolean isFirstTime = true; 	//每次运行时设为true，用以记录该次运行时对应user抓取的第一条微博创建时间
		String localSinceCreateTime = ""; 	//记录本次运行所抓取的第一条微博的创建时间
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
                		String createTime = t.format(s.getCreatedAt()); 	//注意日期的显示格式，关系到数据库里的排序
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
                	        System.out.println("到目前为止共获取了" + weiboSize + "条微博内容。");
                	        /*
                	         * 将localSinceCreateTime写入数据库
                	         */
                	        MicroblogUserSimpleDaoImpl mbusd = new MicroblogUserSimpleDaoImpl();
                	        user.setSinceCreateTime(localSinceCreateTime); 	//更新user的sinceCreateTime字段
                	        user.setSinceCollectTime(t.format(new Date())); //更新user的sinceCollectTime字段
                	        mbusd.updateSimpleUserInfo(user, usersSimpleInfoTableNameInHBase, "Sina"); 	//将sinceCreaeteTime字段和sinceCollectTime字段写入HBase
                			return statusesList;
                		}
                		MicroblogData mdata = Status2MicroblogData(s);
                		System.out.println(mdata.getText());	//用来在控制台查看是否抓取到数据
                		if(mdata != null)
                		{
                			statusesList.add(mdata);
                		}
                		weibo4j.model.Status retweetedStatus = s.getRetweetedStatus();
                		if(retweetedStatus != null)
                		{//将源微博内容添加进列表
                			System.out.println("源微博：" + retweetedStatus.getText());
                			MicroblogData retweet = Status2MicroblogData(retweetedStatus);
                			if(retweet != null)
                			{//防止源微博被删除的情况
                				statusesList.add(retweet);
                			}
                		}
                		weiboSize++;
                		++weiboCount;
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
        System.out.println("本次获取" + weiboCount + "条微博内容。");
        System.out.println("到目前为止共获取了" + weiboSize + "条微博内容。");
        /*
         * 将localSinceCreateTime写入数据库
         */
        MicroblogUserSimpleDaoImpl mbusd = new MicroblogUserSimpleDaoImpl();
        user.setSinceCreateTime(localSinceCreateTime); 	//更新user的sinceCreateTime字段
        user.setSinceCollectTime(t.format(new Date())); //更新user的sinceCollectTime字段
        mbusd.updateSimpleUserInfo(user, usersSimpleInfoTableNameInHBase, "Sina"); 	//将sinceCreaeteTime字段和sinceCollectTime字段写入HBase
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
		System.out.println("用户" + user.getUserId() + "的微博数据写入HBase成功！");
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
	 *判断是否达到剪枝条件
	 * */
	public static boolean isPruning(MicroblogUser user) throws Exception
	{   
        if(user.getIdolsCount() > 2)
        {
        	return false;
        }
        if(user.getFansCount() > 2)
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
    	getAccessTokenList(); 		//登陆所有测试账号并获取所有access token
    	if(CrawlerConfig.getValue("Sina_CurrentUserID").equals("") == true) 	//注意这一行不要写多了个分号，否则该if语句的执行体是空的
    	{//若Sina_CurrentUserID不为“”则说明程序上次运行时异常中断(因此更改起始帐号必须将Sina_CurrentUserID的值清空)
    		System.out.println("是否从起始帐号开始抓取？（第一次运行或修改了起始帐号请输入Y，否则输入N）");
    		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    		String usrCommond = br.readLine();
    		if(usrCommond.equals("Y") || usrCommond.equals("y"))
    		{
    			CrawlerConfig.updateProperties("Sina_CurrentLayer", "0"); 	//设置当前遍历的层次数
    			String userID = CrawlerConfig.getValue("Sina_StartUserID"); 	//获取用户设置的起始帐号
    			MicroblogUser user = new MicroblogUser();
    			user.setUserId(userID); 	//新浪微博以用户名作为唯一标识
    			user.setSinceCollectTime("");
    			user.setSinceCreateTime("");
    			MicroblogUserSimpleDaoImpl musdi = new MicroblogUserSimpleDaoImpl();
    			musdi.createTable(usersSimpleInfoTableNameInHBase); 	//创建用户简单信息表（sinceCreateTime和sinceCollectTime字段）
    			musdi.updateSimpleUserInfo(user, usersSimpleInfoTableNameInHBase, "Sina"); 	//将起始帐号的简单信息写入SinaUserSimpleInfo表
    			ArrayList<String> userList = new ArrayList<String>();
    			userList.add(userID);
    			UserListDaoImpl uldi = new UserListDaoImpl();
    			uldi.createTable(currentUserTableNameInHBase); 	//创建currentUserList表
    			uldi.appendData(userList, currentUserTableNameInHBase); 	//将起始账户id写入currentUserList表
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
    	String currentLayerString = CrawlerConfig.getValue("Sina_CurrentLayer");
    	long currentLayer = 0; 	//当前遍历的层次
    	if(!currentLayerString.equals(""))
    	{//
    		currentLayer = Long.parseLong(currentLayerString); 	//从文件中读取当前遍历的层次
    	}
    	for(; ; )
    	{
    		int currentLayerNodeCount = 0;
    		String startRow = CrawlerConfig.getValue("Sina_CurrentUserID"); 	//从上次程序中断时所抓取的用户开始抓取
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
    				String currentUserID = rows.get(i);
    				MicroblogUser user = getUserInfoByID(currentUserID); 	//调用接口获取user的完整信息
    				if(user != null)
    				{//防止用户被删除或者其他原因获取不到用户信息
        				if(isPruning(user) == true)
        				{
        					System.out.println("达到剪枝条件，跳过该节点（用户）！");
        				}
        				else
        				{
        					visit(user); 	//访问节点（用户），叶子节点写入nextUserList表
        					CrawlerConfig.updateProperties("Sina_CurrentUserID", currentUserID); 	//将当前正在访问的节点（用户）id持久化，以支持断点
        					++currentLayerNodeCount;
        				}
    				}

    			}
    		}
    		uldi.createTable(currentUserTableNameInHBase); 	//清空currentUserList表（删除并重新创建）
    		System.out.println("正在拷贝数据，请勿停止程序。。。");
    		uldi.copyContent(nextUserTableNameInHBase, currentUserTableNameInHBase); 	//将nextUserList表的内容复制到currentUserList
    		CrawlerConfig.updateProperties("Sina_CurrentUserID", ""); 	//当前层次遍历完成，重置Sina_CurrentUserID
    		System.out.println("第" + currentLayer + "层遍历完毕。。。");
    		CrawlerConfig.updateProperties("Sina_CurrentLayer", Long.toString(++currentLayer)); 	//将当前遍历的层数持久化
    		System.out.println("共有" + currentLayerNodeCount + "个节点（用户）。");
    		Thread.sleep(3600);
    	}
    }
    
	public static void main(String[] args)  throws WeiboException, Exception  {
		crawlLogic();
	}

}
