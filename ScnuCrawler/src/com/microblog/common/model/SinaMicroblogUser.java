/**
 * 
 */
package com.microblog.common.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

import weibo4j.Friendships;
import weibo4j.Timeline;
import weibo4j.Users;
import weibo4j.model.Paging;
import weibo4j.model.Status;
import weibo4j.model.StatusWapper;
import weibo4j.model.User;
import weibo4j.model.WeiboException;

import com.microblog.common.accounts.manager.SinaAccount;
import com.scnu.crawler.util.web.WebInterface;

/**
 * @author 云水寒
 *
 */
public class SinaMicroblogUser extends MicroblogUser {
	/**
	 * @param key 新浪微博用户标识。
	 */
	public SinaMicroblogUser(String key) {
		super(key);
	}
	/**
	 * 
	 */
	private static final long serialVersionUID = 6916974162305626067L;
	/**
	 * 友好显示名称，对应新浪微博用户信息的name字段。
	 */
	private String name=""; 
	/**
	 * 用户昵称，对应新浪微博用户信息的screen_name字段。
	 */
	private String screenName="";
	/**
	 * 微博用户粉丝列表。
	 */
	private HashSet<SinaMicroblogUser> fans=new HashSet<SinaMicroblogUser>();
	/**
	 * 微博用户关注列表。
	 */
	private HashSet<SinaMicroblogUser> idols=new HashSet<SinaMicroblogUser>();
	/**
	 * 返回新浪微博用户的友好显示名称，对应新浪微博用户信息的name字段。
	 * @return name字段
	 */
	public String getName() {
		return name;
	}
	/**
	 * 设置新浪微博用户的友好显示名称，对应新浪微博用户信息的name字段。
	 * @param name name的新值。
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * 返回新浪微博用户的用户昵称，对应新浪微博用户信息的screen_name字段。
	 * @return screenName字段。
	 */
	public String getScreenName() {
		return screenName;
	}
	/**
	 * 设置新浪微博用户的用户昵称，对应新浪微博用户信息的screen_name字段。
	 * @param screenName screenName的新值。
	 */
	public void setScreenName(String screenName) {
		this.screenName = screenName;
	}
	/* (non-Javadoc)
	 * @see com.microblog.common.model.MicroblogUser#isObjInfoComplete()
	 */
	@Override
	public boolean isObjInfoComplete() {
		// TODO Auto-generated method stub
		if(!super.isObjInfoComplete())
			return false;
		else if(name=="")
			return false;
		else if(screenName=="")
			return false;
		else
			return true;
	}
	/* (non-Javadoc)
	 * @see com.microblog.common.model.MicroblogUser#toString()
	 */
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return super.toString()+", name="+name+", screen_name="+screenName;
	}
	@Override
	public WebInterface webInterface() {
		// TODO Auto-generated method stub
		return new SinaWebInterface();
	}
	class SinaWebInterface implements WebInterface
	{

		@Override
		public boolean updateUserInfo() {
			// TODO Auto-generated method stub
			Users um=new Users();
			int tryTimes=0;
			for (tryTimes = 0; tryTimes < 3; tryTimes++) {
				try {
					SinaAccount.reduceRemainingHits();
					User user = um.showUserById(getKey());
					setName(user.getName());
					setScreenName(user.getScreenName());
					setGender(user.getGender());
					setProvince(Integer.toString(user.getProvince()));
					setFansCount(user.getFollowersCount());
					setIdolsCount(user.getFriendsCount());
					setStatusesCount(user.getStatusesCount());
					return true;
				} catch (WeiboException e) {
					// TODO 自动生成的 catch 块
					e.printStackTrace();
					System.out.println("获取用户信息错误或者网络链接超时。。。\n将在3.6秒后重试。。。");
					try {
						Thread.sleep(3600);
					} catch (InterruptedException e1) {
						// TODO 自动生成的 catch 块
						e1.printStackTrace();
					}
				}
			}
			System.out.println("超时重试次数达到上限："+ tryTimes +"次，将跳过该用户！");
			return false;
		}

		@Override
		public ArrayList<SinaMicroblogData> getUserStatuses() {
			int weiboCount=0;
			int statusesPerPage=100;
			boolean isFirstTime = true;
			long firstStatusCreateTime=0;
			System.out.println("正准备获取" + getKey() + "的微博内容。。。");
			//SimpleDateFormat t = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Timeline tm = new Timeline();
			ArrayList<SinaMicroblogData>  statusesList  = new ArrayList<SinaMicroblogData>();
			int statusesPageCount=(int) Math.ceil(((double)getStatusesCount())/statusesPerPage);
			for(int i=1;i<=statusesPageCount;i++)
			{
				for(int tryTimes=0;tryTimes<3;tryTimes++)
				{
					try {
						SinaAccount.reduceRemainingHits();
						StatusWapper statuses = tm.getUserTimelineByUid(getKey(),new Paging(i,statusesPerPage),0,0);
						for(Status status:statuses.getStatuses())
						{
							long createTime=status.getCreatedAt().getTime(); 	//注意日期的显示格式，关系到数据库里的排序
							if(isFirstTime)
							{
								firstStatusCreateTime=createTime;
								isFirstTime=false;
							}
							if(createTime-getSinceCreateTime() <= 0)
							{
								System.out.println("本页内容已经在上次运行时抓取过，本页及以后的数据不再重复抓取。");
								System.out.println("本次获取" + weiboCount + "条微博内容。");
								if (firstStatusCreateTime-getSinceCreateTime()>0) {
									setSinceCreateTime(firstStatusCreateTime);
								}
								setSinceCollectTime(new Date().getTime());
								return statusesList;
							}
							SinaMicroblogData mdata = status2MicroblogData(status);
							System.out.println(mdata.getText());
							statusesList.add(mdata);
							weiboCount++;
						}
						break;
					} catch (WeiboException e) {
						// TODO 自动生成的 catch 块
						e.printStackTrace();
						System.out.println("获取本页微博内容出错。。。\n将重试。。。");
					}
				}
			}
			System.out.println("本次获取" + weiboCount + "条微博内容。");
			if (firstStatusCreateTime-getSinceCreateTime()>0)
				setSinceCreateTime(firstStatusCreateTime);
			setSinceCollectTime(new Date().getTime());
			return statusesList;
		}

		@Override
		public String[] getUserFansList() {
			int fansPerPage=5000;
			Friendships fm = new Friendships();
			int tryTimes;
			for(tryTimes=0;tryTimes<3;tryTimes++)
			{
				try {
					SinaAccount.reduceRemainingHits();
					String[] ids = fm.getFollowersIdsById(getKey(), fansPerPage, 0);
					System.out.println("共获得" + ids.length + "个粉丝。"); 
					return ids.length==0?null:ids;
				} catch (WeiboException e) {
					// TODO 自动生成的 catch 块
					e.printStackTrace();
					System.out.println("获取用户粉丝列表出错或者网络链接超时。。。\n将在3.6秒后重试。。。");
					try {
						Thread.sleep(3600);
					} catch (InterruptedException e1) {
						// TODO 自动生成的 catch 块
						e1.printStackTrace();
					}
				}
			}
			System.out.println("超时重试次数达到上限："+ tryTimes +"次，将跳过该用户！");
			return null;
		}

		@Override
		public String[] getUserIdolsList() {
			int FriendsPageCount = 5000;
			Friendships fm = new Friendships();
			int tryTimes;
			for(tryTimes=0;tryTimes<3;tryTimes++)
			{
				try {
					SinaAccount.reduceRemainingHits();
					String[] ids = fm.getFriendsIdsByUid(getKey(), FriendsPageCount, 0);
					System.out.println("共获得" + ids.length + "个关注对象。"); 
					return ids.length==0?null:ids;
				} catch (WeiboException e) {
					// TODO 自动生成的 catch 块
					e.printStackTrace();
					System.out.println("获取用户关注列表出错或者网络链接超时。。。\n将在3.6秒后重试。。。");
					try {
						Thread.sleep(3600);
					} catch (InterruptedException e1) {
						// TODO 自动生成的 catch 块
						e1.printStackTrace();
					}
				}
			}
			System.out.println("超时重试次数达到上限："+ tryTimes +"次，将跳过该用户！");
			return null;
		}
		/**
		 * 将新浪微博Status对象转化为SinaMicroblogData对象。
		 * @param status 新浪微博Status对象。
		 * @return SinaMicroblogData对象。
		 */
		private SinaMicroblogData status2MicroblogData(Status status)
		{
			//SimpleDateFormat t = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			SinaMicroblogData mdata = new SinaMicroblogData();
			long createTime = status.getCreatedAt().getTime();
			mdata.setMicroblogID(status.getId());
			mdata.setText(status.getText());
			mdata.setPicSrc(status.getOriginalPic());
			mdata.setUser(user2MicroblogUser(status.getUser()));
			mdata.setCreatTime(createTime);	 //注意日期的显示格式，关系到数据库里的排序
			mdata.setCollectTime(new Date().getTime());
			mdata.setCommentsCount(status.getCommentsCount());
			mdata.setRepostsCount(status.getRepostsCount());
			Status retweetedStatus=status.getRetweetedStatus();
			if(retweetedStatus!=null)
			{
				mdata.setType("2");
				mdata.setSource(status2MicroblogData(retweetedStatus));
			}
			else
				mdata.setType("1");
			return mdata;
		}
		/**
		 * 将新浪微博User对象转化为SinaMicroblogUser对象。
		 * @param user 新浪微博User对象。
		 * @return SinaMicroblogUser对象。
		 */
		private SinaMicroblogUser user2MicroblogUser(User user)
		{
			SinaMicroblogUser u=new SinaMicroblogUser(user.getId());
			u.setName(user.getName());
			u.setScreenName(user.getScreenName());
			u.setGender(user.getGender());
			u.setProvince(Integer.toString(user.getProvince()));
			u.setFansCount(user.getFollowersCount());
			u.setIdolsCount(user.getFriendsCount());
			u.setStatusesCount(user.getStatusesCount());
			return u;
		}
	}
	@Override
	public void addFan(MicroblogUser fan) {
		if (fan instanceof SinaMicroblogUser)
		{
			SinaMicroblogUser suser=(SinaMicroblogUser) fan;
			if (fans.add(suser))
				suser.addIdol(this);
		}
		else
			System.err.println("请使用新浪微博用户！且fan不为null！");
		
	}
	@Override
	public void addIdol(MicroblogUser idol) {
		if(idol instanceof SinaMicroblogUser)
		{
			SinaMicroblogUser suser=(SinaMicroblogUser)idol;
			if(idols.add(suser))
				suser.addFan(this);
		}
		else
			System.err.println("请使用新浪微博用户！且idol不为null！");
		
	}

}
