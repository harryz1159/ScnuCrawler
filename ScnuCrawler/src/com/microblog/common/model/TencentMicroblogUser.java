/**
 * 
 */
package com.microblog.common.model;

import java.util.ArrayList;

import com.scnu.crawler.util.web.WebInterface;

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
			// TODO 自动生成的方法存根
			return false;
		}

		@Override
		public ArrayList<? extends MicroblogData> getUserStatuses() {
			// TODO 自动生成的方法存根
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
