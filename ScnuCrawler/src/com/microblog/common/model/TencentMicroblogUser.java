/**
 * 
 */
package com.microblog.common.model;

/**
 * @author 云水寒
 *
 */
public class TencentMicroblogUser extends MicroblogUser {

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

}
