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
	/**
	 * 本微博的原微博。如果本微博非转发则为null。
	 */
	private SinaMicroblogData source;

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

	@Override
	public void setSource(MicroblogData source) {
		// TODO 自动生成的方法存根
		if(source instanceof SinaMicroblogData)
			this.source=(SinaMicroblogData) source;
		else
			System.err.println("请使用新浪微博数据！且source不为null！");
		
	}

	@Override
	public SinaMicroblogData getSource() {
		// TODO 自动生成的方法存根
		return source;
	}

}
