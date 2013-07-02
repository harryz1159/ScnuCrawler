package com.scnu.crawler;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import com.microblog.common.login.TencentLogin;
import com.tencent.weibo.api.StatusesAPI;
import com.tencent.weibo.api.UserAPI;
import com.tencent.weibo.oauthv2.OAuthV2;
import com.tencent.weibo.oauthv2.OAuthV2Client;
import com.microblog.common.accounts.manager.TencentAccount;
import com.tencent.weibo.api.*;

public class TencentCrawler_OLD {
	/*
	 * 腾讯改了接口调用权限 更换账号的方法已不凑效 2013/4/1记
	 * */
	private static final int rateLimitOfSingleAccount = 1000; //单用户每小时的调用次数限制（暂时找不到从服务器获取这个值的方法，因此手动指定）
	private static int remaining_hits = rateLimitOfSingleAccount; //剩余调用次数
	private static final int delta = 200; //用以调节调用频率，暂时根据经验设一个值，腾讯服务器所记录的剩余调用次数可能小于1000
	private static OAuthV2 oAuth/*=new OAuthV2()*/;
	private static ArrayList<OAuthV2> oAuths = new ArrayList<OAuthV2>();        //保存有accessToken的OAuthV2对象
    private static  int a = 0;				//微博条数
   // private  int b = 0;				//评论条数
    private static int apiTimes = 0;		//api调用次数
    private static int accountSetIterator = 0;        //账号集循环迭代器
    //static int fansNum = 0; //调试用
		/**
		 * @param args
		 */
	    private static void init(OAuthV2 oAuth) {
	        oAuth.setClientId("801207932");
	        oAuth.setClientSecret("e8cf2b0dd04f3b2b2e81588fddfca286");
	        oAuth.setRedirectUri("http://www.qq.com/");
	    }
	    
	    private static void openBrowser(OAuthV2 oAuth) {
			
	        String authorizationUrl = OAuthV2Client.generateAuthorizationURL(oAuth);

	        //调用外部浏览器
	        if( !java.awt.Desktop.isDesktopSupported() ) {

	            System.err.println( "Desktop is not supported (fatal)" );
	            System.exit( 1 );
	        }
	        java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
	        if(desktop == null || !desktop.isSupported( java.awt.Desktop.Action.BROWSE ) ) {

	            System.err.println( "Desktop doesn't support the browse action (fatal)" );
	            System.exit( 1 );
	        }
	        try {
	            desktop.browse(new URI(authorizationUrl));
	        } catch (Exception e) {
	            e.printStackTrace();
	            System.exit( 1 );
	        }
	        
	        System.out.println("Input the authorization information (eg: code=CODE&openid=OPENID&openkey=OPENKEY) :");
	        Scanner in = new Scanner(System.in);
	        String responseData = in.nextLine(); 
	        in.close();
	        
	        if(OAuthV2Client.parseAuthorization(responseData, oAuth)){
	            System.out.println("Parse Authorization Information Successfully");
	        }else{
	            System.out.println("Fail to Parse Authorization Information");
	            return;
	        }
	    }
	    
	    public static String TimeStamp2Date(String timestampString){  
	    	  Long timestamp = Long.parseLong(timestampString)*1000;  
	    	  String date = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(timestamp));  
	    	  return date;  
	    	}  

	    /**
	     * 账号登陆，换取access token
	     * */
		private static void getOAuthList() throws Exception
		{//未调试
			TencentAccount.setTencentAccount();			//获取腾讯微博账号信息
			for(int i = 0; i < TencentAccount.getAccountsNum(); ++i)
			{
				/*
				 * 登陆所有账号并换取access token
				 * */
				oAuths.add(TencentLogin.gainAccessToken(TencentAccount.getTencentAccounts().get(i).get("qq"), 
						TencentAccount.getTencentAccounts().get(i).get("password")));
				System.out.println("第" + i + "个Access Token已获取。");
			}
		}
		
	    /**
	     * 检查当前读接口调用剩余次数
	     * 超出限制则更换账号（修改accountSetIterator变量、重置remaining_hits）
	     * 否则返回false；
	     * */
	    private static boolean changeAccountForRead() throws Exception
	    {//未调试
	    	   if(remaining_hits <= 0)
	    	   {
	    		   System.out.println("获取微博读接口调用了" + apiTimes + "次。");
	    		   System.out.println("获取了" + a +"条信息。");
	    		   System.out.println("正在更换账号======================================");
	    		   Thread.sleep(2*1000);			 //更换账号测试
	    		   accountSetIterator = (accountSetIterator + 1) % TencentAccount.getAccountsNum();
	    		   System.out.println("第" + accountSetIterator + "个账号。");
	    		   Thread.sleep(2 * 1000);
	    		   //oAuth = oAuths.get(accountSetIterator);
	    		   remaining_hits = rateLimitOfSingleAccount; //重置剩余次数
	    		   //statusesAPI = new StatusesAPI(oAuth.getOauthVersion());
	    		  // Statusesjson = statusesAPI.userTimeline(oAuth, "json", "0", "0", Integer.toString(StatusPageCounts), 
					//			  "0", name, "", "3", "0");
		    	   //--remaining_hits;
		    	   //++apiTimes;
	    		   return true;
	    	   }
	    	   return false;
	    }
	    
	    /**
	     * 获得指定用户名的所有微博内容
	     * @return 
	     * */
	    public static ArrayList<Map<String, String>> getUserStatusesByName(ArrayList<OAuthV2> oAuths, String name) throws Exception
	    {//未调试
	    	if(oAuths != null)
	    	{
	    		changeAccountForRead();			//检查当前测试账号的读接口剩余调用次数
	    		oAuth = oAuths.get(accountSetIterator); 
	    	}
	    	else
	    	{
	    		System.out.println("请检查账号登陆以及Access Token换取是否成功！");
	    		System.exit(0);
	    	}
	    	int StatusPageCounts = 70;               //翻页微博数,最多每页70条
	    	UserAPI userAPI = new UserAPI(oAuth.getOauthVersion());
		    StatusesAPI statusesAPI = new StatusesAPI(oAuth.getOauthVersion());
		    String Userjson = userAPI.otherInfo(oAuth, "json", name, "");      
		    JSONObject Userobj = new JSONObject(Userjson);
		    String Statusesjson = statusesAPI.userTimeline(oAuth, "json", "0", "0", Integer.toString(StatusPageCounts), 
						  "0", name, "", "3", "0");			//从第一页开始抓取，这一句调用了读接口
	    	--remaining_hits;
	    	++apiTimes;	
	    	ArrayList<Map<String, String>>  statusesList  = new ArrayList<Map<String, String>>();
	    	
	    	/*
	    	 * 获取某个用户的所有微博内容
	    	 * 以下代码段没有调用API
	    	 * */
	    	for(int i = 0; i<Userobj.getJSONObject("data").getInt("tweetnum")/StatusPageCounts+1; i++)
	    	{
	    		String pageTime = null;		//上一次请求返回的最后一条记录时间
	    		String lastID = null;			//上一次请求返回的最后一条记录id
			    try
			    {			    	   
			    	JSONObject Statusesobj = new JSONObject(Statusesjson);	  
				    JSONArray Statusesinfo = new JSONArray(Statusesobj.getJSONObject("data").getString("info"));
				    JSONObject Statusesitem; 

				    //获取每一次分页的微博
				    for(int j=0; j<Statusesinfo.length(); j++)
				    {
				    	Map<String, String> map = new HashMap<String, String>();
				    	Statusesitem = (JSONObject) Statusesinfo.get(j);
				    	map.put("nick", Statusesitem.getString("nick"));
				    	map.put("text", Statusesitem.getString("text"));
				    	map.put("timestamp", TimeStamp2Date(Statusesitem.getString("timestamp")));
				    	statusesList.add(map);
				    	//System.out.println(Statusesitem.getString("nick")+"："+Statusesitem.getString("text")+" "
				    	//		   +TimeStamp2Date(Statusesitem.getString("timestamp")));
				    	System.out.println(map);
				    	a++;			    	   
				    	  
				    //Thread.sleep((3600 / (rateLimitOfSingleAccount * tencentAccount.getAccountsNum())) * 1000 + delta);

				    	if(j == Statusesinfo.length() - 1)
				    	{//保存微博下一页参数
				    		pageTime = Statusesitem.getString("timestamp");
				    		lastID = Statusesitem.getString("id");
				    	}
				    }
				       
			    }catch(Exception e){
			    	System.out.println("获取微博接口调用了"+apiTimes+"次，可能是超过限制次数！");
			    	System.out.println(remaining_hits);
			    }
			       
		    	   /*
		    	    * 异常情况的向下翻页处理
		    	    * 用户可能发表了非常多的微博内容，分页超过1000次
		    	    * 检查当前API剩余调用次数
		    	    * 超出次数限制则更换账号
		    	    * */
		    	  /* if(remaining_hits <= 0)
		    	   {
		    		   System.out.println("获取微博接口调用了" + apiTimes + "次。");
		    		   System.out.println("获取了" + a +"条微博。");
		    		   System.out.println("更换账号测试！正在更换账号。======================================");
		    		   Thread.sleep(2*1000); //更换账号测试
		    		   accountSetIterator = (accountSetIterator + 1) % TencentAccount.getAccountsNum();
		    		   System.out.println("第" + accountSetIterator + "个账号。");
		    		   Thread.sleep(2 * 1000);
		    		   oAuth = oAuths.get(accountSetIterator);
		    		   remaining_hits = rateLimitOfSingleAccount; //重置剩余次数
		    		   statusesAPI = new StatusesAPI(oAuth.getOauthVersion());
		    		   Statusesjson = statusesAPI.userTimeline(oAuth, "json", "1", pageTime, Integer.toString(StatusPageCounts), 
									  lastID, name, "", "3", "0");			//向下翻页
			    	   --remaining_hits;
			    	   ++apiTimes;
		    	   }*/
		    	   
			    if(changeAccountForRead() == true)
			    {/*
			    	    * 异常情况的向下翻页处理
			    	    * 用户可能发表了非常多的微博内容，分页超过1000次
			    	    * 检查当前API剩余调用次数
			    	    * 超出次数限制则更换账号
			    	    * */
			    	oAuth = oAuths.get(accountSetIterator);
			    	statusesAPI = new StatusesAPI(oAuth.getOauthVersion());
			    }
			    
	    		Statusesjson = statusesAPI.userTimeline(oAuth, "json", "1", pageTime, Integer.toString(StatusPageCounts), 
								  lastID, name, "", "3", "0");			//调用腾讯微博api读接口，向下翻页
		    	--remaining_hits;
		    	++apiTimes;       
	    	}
	    	return statusesList;
	    }
	    
	    public static void getTencent_OLD() throws Exception
	    {
			/*
			 * 获取用户测试账号信息
			 * */
			TencentAccount tencentAccount = new TencentAccount();
			tencentAccount.setTencentAccount();
			
			
			/*
			 * 登录所有qq账号并换取相应的access token
			 * 暂时假设所有账号都能成功登录
			 */
			
			for(int i = 0; i < tencentAccount.getAccountsNum(); ++i)
			{
				oAuths.add(TencentLogin.gainAccessToken(tencentAccount.getTencentAccounts().get(i).get("qq"), 
						tencentAccount.getTencentAccounts().get(i).get("password")));
				System.out.println("第" + i + "个Access Token已获取。");
			}
			
	       int StatusPageCounts = 70;               //翻页微博数,最多每页70条
		   int CommentsPageCounts = 100;            //翻页评论数，最多每页100条
		   String name = "jianwangkeji11";          //微博帐号名
	       
		   oAuth = oAuths.get(accountSetIterator);		//获取第一个账号对应的OAuthV2对象
	       //UserAPI userAPI = new UserAPI(oAuth.getOauthVersion());
	      // StatusesAPI statusesAPI = new StatusesAPI(oAuth.getOauthVersion());
	       //TAPI tAPI = new TAPI(oAuth.getOauthVersion());
	       //String Userjson = userAPI.otherInfo(oAuth, "json", name, "");      
	       //JSONObject Userobj = new JSONObject(Userjson);
	       
	     //String Statusesjson = statusesAPI.broadcastTimeline(oAuth, "json", "0", "0", "1", "0", "3", "0");
	       //String Statusesjson = statusesAPI.userTimeline(oAuth, "json", "0", "0", Integer.toString(StatusPageCounts), 
	    	//	   										  "0", name, "", "3", "0");
		   
		   /*
		    * 从某个用户名开始抓取微博内容数据
		    * 该用户所有微博内容抓取完毕，跳转到他所关注的用户继续抓取
		    * */
	       for(; ; )
	       {		       
	    	   /*
	    	    * 检查当前API剩余调用次数
	    	    * 超出次数限制则更换账号
	    	    * */
	    	   if(remaining_hits <= 0)
	    	   {
	    		   System.out.println("获取微博接口调用了" + apiTimes + "次。");
	    		   System.out.println("获取了" + a +"条微博。");
	    		   System.out.println("正在更换账号======================================");
	    		   Thread.sleep(2*1000);			 //更换账号测试
	    		   accountSetIterator = (accountSetIterator + 1) % tencentAccount.getAccountsNum();
	    		   System.out.println("第" + accountSetIterator + "个账号。");
	    		   Thread.sleep(2 * 1000);
	    		   oAuth = oAuths.get(accountSetIterator);
	    		   remaining_hits = rateLimitOfSingleAccount; //重置剩余次数
	    		   //statusesAPI = new StatusesAPI(oAuth.getOauthVersion());
	    		  // Statusesjson = statusesAPI.userTimeline(oAuth, "json", "0", "0", Integer.toString(StatusPageCounts), 
					//			  "0", name, "", "3", "0");
		    	   //--remaining_hits;
		    	   //++apiTimes;
	    	   }
	    	   
		       UserAPI userAPI = new UserAPI(oAuth.getOauthVersion());
		       StatusesAPI statusesAPI = new StatusesAPI(oAuth.getOauthVersion());
		       String Userjson = userAPI.otherInfo(oAuth, "json", name, "");      
		       JSONObject Userobj = new JSONObject(Userjson);
		       String Statusesjson = statusesAPI.userTimeline(oAuth, "json", "0", "0", Integer.toString(StatusPageCounts), 
						  "0", name, "", "3", "0");			//从第一页开始抓取
	    	   --remaining_hits;
	    	   ++apiTimes;
	    	   
	    	   /*
	    	    * 获取某个用户的所有微博内容
	    	    * 以下代码段没有调用API
	    	    * */
	    	   for(int i = 0; i<Userobj.getJSONObject("data").getInt("tweetnum")/StatusPageCounts+1; i++)
	    	   {
	    		   String pageTime = null;		//上一次请求返回的最后一条记录时间
	    		   String lastID = null;			//上一次请求返回的最后一条记录id
			       try
			       {			    	   
			    	   JSONObject Statusesobj = new JSONObject(Statusesjson);	  
				       JSONArray Statusesinfo = new JSONArray(Statusesobj.getJSONObject("data").getString("info"));
				       JSONObject Statusesitem; 

				       //获取每一次分页的微博
				       for(int j=0; j<Statusesinfo.length(); j++)
				       {
				    	   Statusesitem = (JSONObject) Statusesinfo.get(j);
				    	   System.out.println(Statusesitem.getString("nick")+"："+Statusesitem.getString("text")+" "
				    			   +TimeStamp2Date(Statusesitem.getString("timestamp")));
				    	   a++;			    	   
				    	  
				    	  //Thread.sleep((3600 / (rateLimitOfSingleAccount * tencentAccount.getAccountsNum())) * 1000 + delta);

				    	   if(j == Statusesinfo.length() - 1)
				    	   {//保存微博下一页参数
				    		   pageTime = Statusesitem.getString("timestamp");
				    		   lastID = Statusesitem.getString("id");
				    	   }
				       }
				       
			       }catch(Exception e){
			    	   System.out.println("获取微博接口调用了"+apiTimes+"次，可能是超过限制次数！");
			    	   System.out.println(remaining_hits);
			       }
			       
		    	   /*
		    	    * 异常情况的向下翻页处理
		    	    * 用户可能发表了非常多的微博内容，分页超过1000次
		    	    * 检查当前API剩余调用次数
		    	    * 超出次数限制则更换账号
		    	    * */
		    	   if(remaining_hits <= 0)
		    	   {
		    		   System.out.println("获取微博接口调用了" + apiTimes + "次。");
		    		   System.out.println("获取了" + a +"条微博。");
		    		   System.out.println("更换账号测试！正在更换账号。======================================");
		    		   Thread.sleep(2*1000); //更换账号测试
		    		   accountSetIterator = (accountSetIterator + 1) % tencentAccount.getAccountsNum();
		    		   System.out.println("第" + accountSetIterator + "个账号。");
		    		   Thread.sleep(2 * 1000);
		    		   oAuth = oAuths.get(accountSetIterator);
		    		   remaining_hits = rateLimitOfSingleAccount; //重置剩余次数
		    		   statusesAPI = new StatusesAPI(oAuth.getOauthVersion());
		    		   Statusesjson = statusesAPI.userTimeline(oAuth, "json", "1", pageTime, Integer.toString(StatusPageCounts), 
									  lastID, name, "", "3", "0");			//向下翻页
			    	   --remaining_hits;
			    	   ++apiTimes;
		    	   }
		    	   
		    	   /*
		    	    * 正常情况的向下翻页处理
		    	    * */
	    		   Statusesjson = statusesAPI.userTimeline(oAuth, "json", "1", pageTime, Integer.toString(StatusPageCounts), 
								  lastID, name, "", "3", "0");			//向下翻页
		    	   --remaining_hits;
		    	   ++apiTimes;
			       
	    	   }
	    	   
	    	   //下一个用户
	       }
	       
	       
	       
	      //System.out.println("一共有"+a+"条微博。");
	    // System.out.println("一共有"+b+"条评论。");
	    }
	    
	    /**
	     * 指定用户偶像列表
	     * */
	    private static ArrayList<String> userIdolList(OAuthV2 oAuth/*我认为这个参数是不需要的*/, String name) throws Exception
	    {//正调试
	    	int IdolPageCounts = 30;			//翻页粉丝数,最多每页30个
	    	
	    	/*
	    	 * 获得指定用户的信息
	    	 * */
	    	UserAPI userAPI = new UserAPI(oAuth.getOauthVersion());
	    	String userJson = userAPI.otherInfo(oAuth, "json", name, "");
	    	JSONObject userObj = new JSONObject(userJson);
	    	
	    	/*
	    	 * 获得指定用户的偶像信息
	    	 * */
	    	FriendsAPI friendsAPI = new FriendsAPI(oAuth.getOauthVersion());
	    	ArrayList<String> idolNameList = new ArrayList<String>();
	    	System.out.println("总共有" + userObj.getJSONObject("data").getInt("idolnum") + "个偶像。");
	    	Thread.sleep(5*1000);
	    	int idolNum = 0;
	    	for(int i = 0; i < userObj.getJSONObject("data").getInt("idolnum")/IdolPageCounts +1; ++i)
	    	{
			       String userIdols = friendsAPI.userIdollist(oAuth, "json", "30", Integer.toString(IdolPageCounts * i), name, "", "0");
			       ++apiTimes;//翻到下一页
		    	   JSONObject userFansListObj = new JSONObject(userIdols);	  
			       JSONArray userFansListInfo = new JSONArray(userFansListObj.getJSONObject("data").getString("info"));
			       JSONObject userFansItem;
			       int j;			//当前第j页
			       for(j = 0; j < userFansListInfo.length(); ++j)
			       {
			    	   userFansItem = (JSONObject)userFansListInfo.get(j);
			    	   idolNameList.add(userFansItem.getString("name"));
			    	   System.out.println(idolNameList.get(j + IdolPageCounts*i));
			    	   ++idolNum;
			       }
	    	}
	    	System.out.println("共调用API" + apiTimes + "次。" );
	    	System.out.println("共获取偶像id" + idolNum + "个。");
	    	return idolNameList;
	    }
	    
	    /**
	     * 指定用户粉丝列表
	     * */
	    private static ArrayList<String> userFansList(/*OAuthV2 oAuth/*我认为这个参数是不需要的, */String name) throws Exception
	    {//正调试
	    	/*
	    	 * 获取某个可用的access token
	    	 * */
	    	/*if(oAuths != null)
	    	{
	    		changeAccountForRead();			//检查当前测试账号的读接口剩余调用次数
	    		oAuth = oAuths.get(accountSetIterator); 
	    	}
	    	else
	    	{
	    		System.out.println("请检查账号登陆以及Access Token换取是否成功！");
	    		System.exit(0);
	    	}*/
	    	
	    	/*
	    	 * 获得指定用户的信息
	    	 * */
	    	UserAPI userAPI = new UserAPI(oAuth.getOauthVersion());
	    	String Userjson = userAPI.otherInfo(oAuth, "json", name, "");			//此处调用了某个读接口
	    	--remaining_hits;
	    	++apiTimes;
	    	JSONObject Userobj = new JSONObject(Userjson);
	    	
	    	/*
	    	 * 获得指定用户的粉丝信息
	    	 * */
	    	FriendsAPI friendsAPI = new FriendsAPI(oAuth.getOauthVersion());
	    	ArrayList<String> fansNameList = new ArrayList<String>();
	    	System.out.println("总共有" + Userobj.getJSONObject("data").getInt("fansnum") + "个粉丝。");
	    	//Thread.sleep(5*1000);
	    	
	    	int fansNum = 0;
	    	int FansPageCounts = 30;			//翻页粉丝数,最多每页30个
	    	try
	    	{
		    	for(int i = 0; i < Userobj.getJSONObject("data").getInt("fansnum")/FansPageCounts +1; ++i)
		    	{
				       /*
				        * 检查当前是否超出次数限制
				        * */
					    /*if(changeAccountForRead() == true)
					    {/*
					    	    * 异常情况的向下翻页处理
					    	    * 用户可能有非常多的粉丝，分页超过1000次
					    	    * 检查当前API剩余调用次数
					    	    * 超出次数限制则更换账号
					    	    * */
					   //	oAuth = oAuths.get(accountSetIterator);
					   // }
			    	   String userFans = friendsAPI.userFanslist(oAuth, "json", "30", Integer.toString(FansPageCounts * i), name, "", "1", "0");			//此处调用了某个读接口
				    	--remaining_hits;
				    	++apiTimes;
			    	   JSONObject userFansListObj = new JSONObject(userFans);	  
				       JSONArray userFansListInfo = new JSONArray(userFansListObj.getJSONObject("data").getString("info")); 		//当info为null时抛出异常
				       JSONObject userFansItem;
				       for(int j = 0; j < userFansListInfo.length(); ++j)
				       {
				    	   userFansItem = (JSONObject)userFansListInfo.get(j);
				    	   fansNameList.add(userFansItem.getString("name"));
				    	   System.out.println(fansNameList.get(j + FansPageCounts*i));
				    	   ++fansNum;
				       }
				       //Thread.sleep(5 * 1000);
		    	}
	    	}
	    	catch(Exception e)
	    	{//id为jianwangkeji11的账号显示粉丝数有21万以上，但是用腾讯提供的api只能查找到9000多个，具体原因不明确
		    	System.out.println("共调用API" + apiTimes + "次。" );
		    	System.out.println("共获取粉丝id" + fansNum + "个。");
	    	}
	    	return fansNameList;
	    }
	    
	    /**
	     * 抓取逻辑
	     * */
	    public static void getTencent() throws Exception
	    {
	    	getOAuthList();			//换取access token，结果保存在oAuths列表
	    	
	    	//抓取逻辑
	    }
	    
		public static void main(String[] args) throws Exception {
			//getTencent();
			OAuthV2 oauth = TencentLogin.gainAccessToken("2508355919", "scnu123456");
			//userFansList(oauth, "lzhj2g");
			userIdolList(oauth, "jianwangkeji11");
			getOAuthList();			//换取所有账号的access token
			userFansList( "jianwangkeji11");


	   }
		
}

