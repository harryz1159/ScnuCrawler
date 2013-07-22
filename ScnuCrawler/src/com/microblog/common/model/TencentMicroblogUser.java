/**
 * 
 */
package com.microblog.common.model;

import java.net.SocketTimeoutException;
import java.util.ArrayList;

import org.apache.commons.httpclient.ConnectTimeoutException;
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
		
	}

}
