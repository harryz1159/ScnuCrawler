/**
 * 
 */
package com.microblog.common.model;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.microblog.common.accounts.manager.TencentAccount;
import com.scnu.crawler.util.web.WebInterface;
import com.tencent.weibo.api.FriendsAPI;
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
		return new TencentWebInterface();
	}
	class TencentWebInterface implements WebInterface
	{

		@Override
		public boolean updateUserInfo() {
			UserAPI userAPI = new UserAPI(TencentAccount.getOa().getOauthVersion());
			int tryTimes=0;
			String userJson=null;
			for (tryTimes = 0;  tryTimes< 3; tryTimes++) {
				try {
					userJson = userAPI.otherInfo(TencentAccount.getOa(),"json", getKey(), "");
					JSONObject userObj = new JSONObject(userJson);
					setOpenId(userObj.getJSONObject("data").getString("openid"));
					setNick(userObj.getJSONObject("data").getString("nick"));
					setProvince(userObj.getJSONObject("data").getString("province_code"));
					setGender(userObj.getJSONObject("data").getString("sex"));
					setFansCount(userObj.getJSONObject("data").getInt("fansnum"));
					setIdolsCount(userObj.getJSONObject("data").getInt("idolnum"));
					setStatusesCount(userObj.getJSONObject("data").getInt("tweetnum"));
					userAPI.shutdownConnection();
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
					System.err.println("JSON解析出错，有可能是服务器没有返回" + getKey()+ "的信息。");
					System.err.println("服务器返回的信息为："+userJson);
					userAPI.shutdownConnection();
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e1) {
						// TODO 自动生成的 catch 块
						e1.printStackTrace();
					}
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
			try {
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
										System.err.println("发现一个无timestamp的JSONObject对象：");
										System.err.println(statusJsonObj);
									}
									try {
										TencentMicroblogData mdata = status2MicroblogData(statusJsonObj);
										System.out.println(mdata.getText());
										statusesList.add(mdata);
										weiboCount++;
										if (j == statusesJsonArray.length() - 1)
										{
											lastId = mdata.getMicroblogID();
											try {
												pageTime = statusJsonObj.getString("timestamp");
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
										System.err.println("发现一个无id或name的JSONObject对象，将跳过该statusJsonObj：");
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
							System.err.println("获取本页微博内容超时。。。\n将重试。。。");
							try {
								Thread.sleep(3600);
							} catch ( InterruptedException e1) {
								// TODO 自动生成的 catch 块
								e1.printStackTrace();
							}
						}
					}
				}
			} catch (JSONException e) {
				// TODO 自动生成的 catch 块
				e.printStackTrace();
				System.err.println("服务器返回非Json格式的应答或返回的应答中并无data、info字段。"+getKey()+"的微博抓取到此结束。");
			} catch (Exception e) {
				// TODO 自动生成的 catch 块
				e.printStackTrace();
				System.err.println("连接服务器获取用户微博信息时发生未定义异常。"+getKey()+"的微博抓取到此结束。");
			}
			System.out.println("本次获取" + weiboCount + "条微博内容。");
			if (firstStatusCreateTime-getSinceCreateTime()>0)
				setSinceCreateTime(firstStatusCreateTime);
			setSinceCollectTime(new Date().getTime());
			statusesAPI.shutdownConnection();
			return statusesList;
		}

		@Override
		public String[] getUserFansList() {
			FriendsAPI friendsAPI = new FriendsAPI(TencentAccount.getOa().getOauthVersion());
			HashSet<String> fansNameList = new HashSet<String>();
			int fansPerPage = 30;
			int fansPageCount=(int) Math.ceil(((double)getFansCount())/fansPerPage);
			String fansListJsonString=null;
			for(int i=1;i<=fansPageCount;i++)
			{
				try {
					for(int tryTimes=0;tryTimes<3;tryTimes++)
					{
						try {
							fansListJsonString = friendsAPI.userFanslist(TencentAccount.getOa(), "json",Integer.toString(fansPerPage),Integer.toString(fansPerPage * (i - 1)),	getKey(), "", "1", "0");
							JSONObject fansListJsonObj = new JSONObject(fansListJsonString);
							JSONObject dataJsonObj=fansListJsonObj.getJSONObject("data");
							if(dataJsonObj.getInt("hasnext")==1)
								i=fansPageCount;
							JSONArray fansJsonArray = new JSONArray(dataJsonObj.getString("info"));
							for (int j = 0; j < fansJsonArray.length(); j++)
							{
								try {
									JSONObject fanJsonObj = fansJsonArray.getJSONObject(j);
									try {
										String fanName = fanJsonObj.getString("name");
										if (fanName != null&& !fanName	.equalsIgnoreCase("null")&& !fanName.equalsIgnoreCase(""))
											fansNameList.add(fanName);
									} catch (JSONException e) {
										// TODO 自动生成的 catch 块
										e.printStackTrace();
										System.err.println("发现一个无name的JSONObject对象：");
										System.err.println(fanJsonObj);
										System.err.println("将跳过该粉丝。。。");
										continue;
									}
								} catch (JSONException e) {
									// TODO 自动生成的 catch 块
									e.printStackTrace();
									System.err.println("解析第" + i + "页第" + j+ "个粉丝时出现异常，将跳过该粉丝。。");
									continue;
								}
							}
							break;
						} catch (SocketTimeoutException | ConnectTimeoutException  e) {
							// TODO: handle exception
							e.printStackTrace();
							System.err.println("获取第"+i+"页粉丝列表超时。。。\n将重试。。。");
							try {
								Thread.sleep(3600);
							} catch (InterruptedException e1) {
								// TODO 自动生成的 catch 块
								e1.printStackTrace();
							}
						}
					}
				} catch (JSONException e) {
					// TODO 自动生成的 catch 块
					e.printStackTrace();
					System.err.println("抓取"+getKey()+"第"+i+"页粉丝时服务器返回非Json格式的应答或返回的应答中并无data、info字段，将跳过该页。。");
					System.err.println("服务器返回的信息为："+fansListJsonString);
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e1) {
						// TODO 自动生成的 catch 块
						e1.printStackTrace();
					}
					continue;
				} catch (Exception e) {
					// TODO 自动生成的 catch 块
					e.printStackTrace();
					System.err.println("抓取第"+i+"页粉丝时发生未定义异常，将跳过该页。。");
					continue;
				}
			}
			System.out.println("共获得" + fansNameList.size() + "个粉丝。");
			friendsAPI.shutdownConnection();
			if(fansNameList.isEmpty())
				return null;
			return fansNameList.toArray(new String[0]);
		}

		@Override
		public String[] getUserIdolsList() {
			FriendsAPI friendsAPI = new FriendsAPI(TencentAccount.getOa().getOauthVersion());
			HashSet<String> idolsNameList = new HashSet<String>();
			int idolsPerPage = 30;
			int idolsPageCount=(int) Math.ceil(((double)getIdolsCount())/idolsPerPage);
			String idolsListJsonString=null;
			for(int i=1;i<=idolsPageCount;i++)
			{
				try {
					for(int tryTimes=0;tryTimes<3;tryTimes++)
					{
						try {
							idolsListJsonString = friendsAPI.userIdollist(TencentAccount.getOa(), "json",Integer.toString(idolsPerPage),Integer.toString(idolsPerPage * (i - 1)),	getKey(), "", "0");
							JSONObject idolsListJsonObj = new JSONObject(idolsListJsonString);
							JSONObject dataJsonObj=idolsListJsonObj.getJSONObject("data");
							if(dataJsonObj.getInt("hasnext")==1)
								i=idolsPageCount;
							JSONArray idolsJsonArray = new JSONArray(idolsListJsonObj.getJSONObject("data").getString("info"));
							for (int j = 0; j < idolsJsonArray.length(); j++)
							{
								try {
									JSONObject idolJsonObj = idolsJsonArray.getJSONObject(j);
									try {
										String idolName = idolJsonObj.getString("name");
										if (idolName != null&& !idolName.equalsIgnoreCase("null")&& !idolName.equalsIgnoreCase(""))
											idolsNameList.add(idolName);
									} catch (JSONException e) {
										// TODO 自动生成的 catch 块
										e.printStackTrace();
										System.err.println("发现一个无name的JSONObject对象：");
										System.err.println(idolJsonObj);
										System.err.println("将跳过该关注。。。");
										continue;
									}
								} catch (JSONException e) {
									// TODO 自动生成的 catch 块
									e.printStackTrace();
									System.err.println("解析第" + i + "页第" + j+ "个关注时出现异常，将跳过该关注。。");
									continue;
								}
							}
							break;
						} catch (SocketTimeoutException | ConnectTimeoutException  e) {
							// TODO: handle exception
							e.printStackTrace();
							System.err.println("获取第"+i+"页关注列表超时。。。\n将重试。。。");
							try {
								Thread.sleep(3600);
							} catch (InterruptedException e1) {
								// TODO 自动生成的 catch 块
								e1.printStackTrace();
							}
						}
					}
				} catch (JSONException e) {
					// TODO 自动生成的 catch 块
					e.printStackTrace();
					System.err.println("抓取"+getKey()+"第"+i+"页关注时服务器返回非Json格式的应答或返回的应答中并无data、info字段，将跳过该页。。");
					System.err.println("服务器返回的信息为："+idolsListJsonString);
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e1) {
						// TODO 自动生成的 catch 块
						e1.printStackTrace();
					}
					continue;
				} catch (Exception e) {
					// TODO 自动生成的 catch 块
					e.printStackTrace();
					System.err.println("抓取第"+i+"页关注时发生未定义异常，将跳过该页。。");
					continue;
				}
			}
			System.out.println("共获得" + idolsNameList.size() + "个关注。");
			friendsAPI.shutdownConnection();
			if(idolsNameList.isEmpty())
				return null;
			return idolsNameList.toArray(new String[0]);
		}
		/**
		 * 将代表某条微博的JSONObject对象转换成TencentMicroblogData对象。
		 * @param statusJsonObj 需要转换为TencentMicroblogData的JSONObject对象。
		 * @return TencentMicroblogData对象。
		 * @throws JSONException 如果JSONObject对象中缺乏微博ID信息或作者用户唯一标识。
		 */
		private TencentMicroblogData status2MicroblogData(JSONObject statusJsonObj) throws JSONException
		{
			TencentMicroblogData mdata = new TencentMicroblogData();
			mdata.setMicroblogID(statusJsonObj.getString("id"));
			mdata.setUser(user2MicroblogUser(statusJsonObj));
			try {
				mdata.setText(statusJsonObj.getString("text").replaceAll("<a.*?>", "").replaceAll("</a>", "").replaceAll("&gt;", ">").replaceAll("&lt;", "<").replaceAll("&nbsp;", " ").replaceAll("&amp;", "&").replaceAll("&quot;", "\""));
				mdata.setPicSrc(statusJsonObj.getString("image"));
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
				if(sourceTweeterString != null)
				{
					JSONObject sourceTweeterJsonObj = new JSONObject(sourceTweeterString);
					mdata.setSource(status2MicroblogData(sourceTweeterJsonObj));
				}
			} catch (JSONException e) {
				// TODO 自动生成的 catch 块
				//e.printStackTrace();
				System.err.println("处理"+mdata.getMicroblogID()+"的原微博信息异常，可能该微博为原创微博！");
			}
			return mdata;
		}
		private TencentMicroblogUser user2MicroblogUser(JSONObject statusJsonObj) throws JSONException
		{
			String name=statusJsonObj.getString("name");
			TencentMicroblogUser user=null;
			if(name!=null&&!name.equals("")&&!name.equalsIgnoreCase("null"))
			{
				user=new TencentMicroblogUser(name);
				if(user.equals(TencentMicroblogUser.this))
					return TencentMicroblogUser.this;
				else
				{
					try {
						user.setOpenId(statusJsonObj.getString("openid"));
						user.setNick(statusJsonObj.getString("nick"));
						user.setProvince(statusJsonObj.getString("province_code"));
					} catch (JSONException e) {
						// TODO 自动生成的 catch 块
						System.err.println(e);
					}
					return user;
				}
			}
			else
				throw new JSONException("返回的微博信息中name字段不正确。");
		}
		
	}

}
