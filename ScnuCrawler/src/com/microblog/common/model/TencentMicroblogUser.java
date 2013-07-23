/**
 * 
 */
package com.microblog.common.model;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.microblog.common.accounts.manager.TencentAccount;
import com.scnu.crawler.util.web.WebInterface;
import com.tencent.weibo.api.StatusesAPI;
import com.tencent.weibo.api.UserAPI;

/**
 * @author 云水寒
 *
 */
public class TencentMicroblogUser extends MicroblogUser {

	/**
	 * 腾讯微博用户标识。
	 * @param key
	 */
	public TencentMicroblogUser(String key) {
		super(key);
		// TODO 自动生成的构造函数存根
	}
	/**
	 * 
	 */
	private static final long serialVersionUID = 9199822593462686281L;
	/**
	 * 用户唯一id，对应腾讯微博用户信息的openid字段。
	 */
	private String openId="";
	/**
	 * 用户昵称，对应腾讯微博用户信息的nick字段。
	 */
	private String nick="";
	/**
	 * @return openId字段
	 */
	public String getOpenId() {
		return openId;
	}
	/**
	 * @param openId openId的新值。
	 */
	public void setOpenId(String openId) {
		this.openId = openId;
	}
	/**
	 * @return nick字段
	 */
	public String getNick() {
		return nick;
	}
	/**
	 * @param nick nick的新值。
	 */
	public void setNick(String nick) {
		this.nick = nick;
	}
	/* (non-Javadoc)
	 * @see com.microblog.common.model.MicroblogUser#isObjInfoComplete()
	 */
	@Override
	public boolean isObjInfoComplete() {
		// TODO Auto-generated method stub
		if(!super.isObjInfoComplete())
			return false;
		else if(openId=="")
			return false;
		else if(nick=="")
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
		return super.toString()+", opedid="+openId+", nick="+nick;
	}
	@Override
	public WebInterface webInterface() {
		// TODO 自动生成的方法存根
		return null;
	}
	class TencentWebInterface implements WebInterface
	{

		@Override
		public boolean updateUserInfo() {
			UserAPI userAPI = new UserAPI(TencentAccount.getOa().getOauthVersion());
			int tryTimes=0;
			for (tryTimes = 0;  tryTimes< 3; tryTimes++) {
				try {
					String userJson = userAPI.otherInfo(TencentAccount.getOa(),
							"json", getKey(), "");
					JSONObject userObj = new JSONObject(userJson);
					setOpenId(userObj.getJSONObject("data").getString("openid"));
					setNick(userObj.getJSONObject("data").getString("nick"));
					setProvince(userObj.getJSONObject("data").getString(
							"province_code"));
					setGender(userObj.getJSONObject("data").getString("sex"));
					setFansCount(userObj.getJSONObject("data")
							.getInt("fansnum"));
					setIdolsCount(userObj.getJSONObject("data").getInt(
							"idolnum"));
					setStatusesCount(userObj.getJSONObject("data").getInt(
							"tweetnum"));
					return true;
				} catch (SocketTimeoutException | ConnectTimeoutException e) {
					System.out.println("网络超时(连接或读取超时)。。。\n将在3.6秒后重试。。。");
					try {
						Thread.sleep(3600);
					} catch (InterruptedException e1) {
						// TODO 自动生成的 catch 块
						e1.printStackTrace();
					}
				} catch (JSONException e) {
					// TODO 自动生成的 catch 块
					e.printStackTrace();
					System.out.println("JSON解析出错，有可能是服务器没有返回" + getKey()
							+ "的信息。");
					userAPI.shutdownConnection();
					return false;
				} catch (Exception e) {
					// TODO 自动生成的 catch 块
					e.printStackTrace();
					System.out.println("其他未知错误！");
					userAPI.shutdownConnection();
					return false;
				}
			}
			System.out.println("超时重试次数达到上限："+ tryTimes +"次，将跳过该用户！");
			userAPI.shutdownConnection();
			return false;
		}

		@Override
		public ArrayList<? extends MicroblogData> getUserStatuses() {
			int weiboCount=0;
			int statusesPerPage=70;
			boolean isFirstTime = true;
			long firstStatusCreateTime=0;
			String pageFlag="0";
			String pageTime="0";
			String lastId="0";
			System.out.println("正准备获取" + getKey() + "的微博内容。。。");
			StatusesAPI statusesAPI = new StatusesAPI(TencentAccount.getOa().getOauthVersion());
			ArrayList<TencentMicroblogData>  statusesList  = new ArrayList<TencentMicroblogData>();
			int statusesPageCount=(int) Math.ceil(((double)getStatusesCount())/statusesPerPage);
			for(int i=1;i<=statusesPageCount;i++)
			{
				for(int tryTimes=0;tryTimes<3;tryTimes++)
				{
					try {
						String statusesJsonString = statusesAPI.userTimeline(TencentAccount.getOa(), "json", pageFlag,pageTime, Integer.toString(statusesPerPage),lastId, getKey(), "", "3", "0");
						JSONObject statusesJsonObj = new JSONObject(statusesJsonString);
						JSONArray statusesJsonArray = new JSONArray(statusesJsonObj.getJSONObject("data").getString("info"));
						for (int j = 0; j < statusesJsonArray.length(); j++)
						{
							try {
								JSONObject statusJsonObj = statusesJsonArray.getJSONObject(j);
								try {
									long createTime = statusJsonObj.getLong("timestamp") * 1000;
									if (isFirstTime) {
										firstStatusCreateTime = createTime;
										pageFlag = "1";
										isFirstTime = false;
									}
									if (createTime - getSinceCreateTime() <= 0) {
										System.out.println("本页内容已经在上次运行时抓取过，本页及以后的数据不再重复抓取。");
										System.out.println("本次获取" + weiboCount+ "条微博内容。");
										if (firstStatusCreateTime- getSinceCreateTime() > 0)
											setSinceCreateTime(firstStatusCreateTime);
										setSinceCollectTime(new Date().getTime());
										statusesAPI.shutdownConnection();
										return statusesList;
									}
								} catch (JSONException e1) {
									// TODO 自动生成的 catch 块
									e1.printStackTrace();
									System.err
											.println("发现一个无timestamp的JSONObject对象：");
									System.err.println(statusJsonObj);
								}
								try {
									TencentMicroblogData mdata = Status2MicroblogData(statusJsonObj);
									System.out.println(mdata.getText());
									statusesList.add(mdata);
									weiboCount++;
									if (j == statusesJsonArray.length() - 1) {
										lastId = mdata.getMicroblogID();
										try {
											pageTime = statusJsonObj
													.getString("timestamp");
										} catch (JSONException e) {
											// TODO 自动生成的 catch 块
											e.printStackTrace();
											System.err.println("解析"+ getKey()+ "第"+ i+ "页微博的最后一条微博时无法获取微博创建时间，后续分页微博可能会错乱，因而本页之后的数据将不再抓取！");
											System.out.println("本次获取" + weiboCount+ "条微博内容。");
											if (firstStatusCreateTime- getSinceCreateTime() > 0)
												setSinceCreateTime(firstStatusCreateTime);
											setSinceCollectTime(new Date().getTime());
											statusesAPI.shutdownConnection();
											return statusesList;
										}
									}
								} catch (JSONException e) {
									// TODO 自动生成的 catch 块
									e.printStackTrace();
									System.err.println("发现一个无id的JSONObject对象，将跳过该statusJsonObj：");
									System.err.println(statusJsonObj);
									continue;
								}
							} catch (JSONException e) {
								// TODO 自动生成的 catch 块
								e.printStackTrace();
								System.err.println("解析第"+i+"页第"+(j+1)+"条微博出现异常，将跳过该statusJsonObj。");
								continue;
							}
						}
						break;
					} catch (SocketTimeoutException | ConnectTimeoutException  e) {
						// TODO: handle exception
						e.printStackTrace();
						System.out.println("获取本页微博内容超时。。。\n将重试。。。");
					}
				}
			}
			return null;
		}

		@Override
		public String[] getUserFansList() {
			// TODO 自动生成的方法存根
			return null;
		}

		@Override
		public String[] getUserIdolsList() {
			// TODO 自动生成的方法存根
			return null;
		}
		/**
		 * 将代表某条微博的JSONObject对象转换成TencentMicroblogData对象。
		 * @param statusJsonObj 需要转换为TencentMicroblogData的JSONObject对象。
		 * @return TencentMicroblogData对象。
		 * @throws JSONException 如果JSONObject对象中缺乏微博ID信息。
		 */
		private TencentMicroblogData Status2MicroblogData(JSONObject statusJsonObj) throws JSONException
		{
			TencentMicroblogData mdata = new TencentMicroblogData();
			mdata.setMicroblogID(statusJsonObj.getString("id"));
			try {
				mdata.setText(statusJsonObj.getString("text"));
				mdata.setPicSrc(statusJsonObj.getString("image"));
				mdata.setUser(TencentMicroblogUser.this);
				long createTime =statusJsonObj.getLong("timestamp")*1000;
				mdata.setCreatTime(createTime);
				mdata.setCollectTime(new Date().getTime());
				mdata.setRepostsCount(statusJsonObj.getInt("count"));
				mdata.setCommentsCount(statusJsonObj.getInt("mcount"));
				mdata.setType(statusJsonObj.getString("type"));
			} catch (JSONException e) {
				// TODO 自动生成的 catch 块
				e.printStackTrace();
				System.err.println(mdata.getMicroblogID()+"的微博信息不完整！");
			}
			String sourceTweeterString = null;
			try {
				sourceTweeterString=statusJsonObj.getString("source");
				if(sourceTweeterString != null && !sourceTweeterString.equalsIgnoreCase("null"))
				{
					JSONObject sourceTweeterJsonObj = new JSONObject(sourceTweeterString);
					mdata.setSource(Status2MicroblogData(sourceTweeterJsonObj));
				}
			} catch (JSONException e) {
				// TODO 自动生成的 catch 块
				e.printStackTrace();
				System.err.println("处理"+mdata.getMicroblogID()+"的原微博信息异常，可能该微博为原创微博！");
			}
			return mdata;
		}
		
	}

}
