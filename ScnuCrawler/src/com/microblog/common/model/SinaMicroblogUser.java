/**
 * 
 */
package com.microblog.common.model;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

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
		// TODO Auto-generated constructor stub
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
	 * @return name字段
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name name的新值。
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return screenName字段。
	 */
	public String getScreenName() {
		return screenName;
	}
	/**
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
		return null;
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
			String firstStatusCreateTime=null;
			System.out.println("正准备获取" + getKey() + "的微博内容。。。");
			SimpleDateFormat t = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
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
							String createTime=t.format(status.getCreatedAt()); 	//注意日期的显示格式，关系到数据库里的排序
							if(isFirstTime)
							{
								firstStatusCreateTime=createTime;
								isFirstTime=false;
							}
							if(createTime.compareTo(getSinceCreateTime()) <= 0)
							{
								System.out.println("本页内容已经在上次运行时抓取过，本页及以后的数据不再重复抓取。");
								System.out.println("本次获取" + weiboCount + "条微博内容。");
								if (firstStatusCreateTime.compareTo(getSinceCreateTime())>0) {
									setSinceCreateTime(firstStatusCreateTime);
								}
								setSinceCollectTime(t.format(new Date()));
								return statusesList;
							}
							SinaMicroblogData mdata = Status2MicroblogData(status);
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
			if (firstStatusCreateTime.compareTo(getSinceCreateTime())>0)
				setSinceCreateTime(firstStatusCreateTime);
			setSinceCollectTime(t.format(new Date()));
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
					return ids;
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
			return null;
		}
		/**
		 * 将新浪微博Status类转化为SinaMicroblogData类。
		 * @param status 新浪微博Status类。
		 * @return SinaMicroblogData类。
		 */
		private SinaMicroblogData Status2MicroblogData(Status status)
		{
			SimpleDateFormat t = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			SinaMicroblogData mdata = new SinaMicroblogData();
			String createTime = t.format(status.getCreatedAt());
			mdata.setMicroblogID(status.getId());
			mdata.setText(status.getText());
			mdata.setPicSrc(status.getOriginalPic());
			mdata.setUser(SinaMicroblogUser.this);
			mdata.setCreatTime(createTime);	 //注意日期的显示格式，关系到数据库里的排序
			mdata.setCollectTime(t.format(new Date()));
			mdata.setCommentsCount(status.getCommentsCount());
			mdata.setRepostsCount(status.getRepostsCount());
			Status retweetedStatus=status.getRetweetedStatus();
			if(retweetedStatus!=null)
			{
				mdata.setType("2");
				mdata.setSource(Status2MicroblogData(retweetedStatus));
			}
			else
				mdata.setType("1");
			return mdata;
		}
	}

}
