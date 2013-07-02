/**
 * 
 */
package com.microblog.common.model;

/**
 * @author 云水寒
 *
 */
public class SinaMicroblogData extends MicroblogData {
	/**
	 * 本微博的发布者。
	 */
	private SinaMicroblogUser user;

	/* (non-Javadoc)
	 * @see com.microblog.common.model.MicroblogData#getUser()
	 */
	@Override
	public SinaMicroblogUser getUser() {
		// TODO Auto-generated method stub
		return user;
	}

	/* (non-Javadoc)
	 * @see com.microblog.common.model.MicroblogData#setUser(com.microblog.common.model.MicroblogUser)
	 */
	@Override
	public void setUser(MicroblogUser user) {
		// TODO Auto-generated method stub
		if(user instanceof SinaMicroblogUser)
			this.user=(SinaMicroblogUser) user;
		else
			System.err.println("请使用新浪微博用户！且user不为null！");

	}

}
