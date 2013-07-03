/**
 * 
 */
package com.microblog.common.model;

import java.util.ArrayList;

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
		//private  ArrayList<String> accessTokenList=SinaAccount.getAccessTokenList();

		@Override
		public boolean updateUserInfo() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public ArrayList<? extends MicroblogData> getUserStatuses() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ArrayList<String> getUserFansList() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ArrayList<String> getUserIdolsList() {
			// TODO Auto-generated method stub
			return null;
		}
	}

}
