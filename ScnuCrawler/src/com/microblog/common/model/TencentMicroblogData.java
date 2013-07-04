/**
 * 
 */
package com.microblog.common.model;

/**
 * @author 云水寒
 *
 */
public class TencentMicroblogData extends MicroblogData {
	/**
	 * 本微博的发布者。
	 */
	private TencentMicroblogUser user;
	/**
	 * 本微博的原微博。如果本微博非转发则为null。
	 */
	private TencentMicroblogData source;

	/* (non-Javadoc)
	 * @see com.microblog.common.model.MicroblogData#getUser()
	 */
	@Override
	public TencentMicroblogUser getUser() {
		// TODO Auto-generated method stub
		return user;
	}

	/* (non-Javadoc)
	 * @see com.microblog.common.model.MicroblogData#setUser(com.microblog.common.model.MicroblogUser)
	 */
	@Override
	public void setUser(MicroblogUser user) {
		// TODO Auto-generated method stub
		if(user instanceof TencentMicroblogUser)
			this.user=(TencentMicroblogUser) user;
		else
			System.err.println("请使用腾讯微博用户！且user不为null！");

	}

	@Override
	public void setSource(MicroblogData source) {
		// TODO 自动生成的方法存根
		if(source instanceof TencentMicroblogData)
			this.source=(TencentMicroblogData) source;
		else
			System.err.println("请使用腾讯微博数据！且source不为null！");
		
	}

	@Override
	public TencentMicroblogData getSource() {
		// TODO 自动生成的方法存根
		return source;
	}

}
