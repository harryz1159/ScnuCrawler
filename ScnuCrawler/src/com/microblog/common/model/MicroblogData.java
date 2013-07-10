package com.microblog.common.model;

/**
 * POJO类
 * 保存从微博抓取的相关数据
 * 统一各种微博的数据类型
 */
public abstract class MicroblogData {
	/**
	 * 微博唯一标识（新浪微博中的id字段、腾讯微博中的id字段）。
	 */
	private String microblogID = "";
	/**
	 * 微博内容（新浪微博中的text字段、腾讯微博中的text字段）。
	 */
	private String text = "";
	/**
	 * 原始图片地址（新浪微博中的original_pic字段、腾讯微博中的image字段）。
	 */
	private String picSrc = "";
	
	/**
	 * 微博创建时间（新浪微博中的created_at字段、腾讯微博中的data.info.timestamp字段）。
	 */
	private long creatTime = 0;
	/**
	 * 抓取这条微博的本机时间。
	 */
	private long collectTime = 0;
	
	
	/**
	 * 转发数。
	 */
	private int repostsCount; 
	/**
	 * 评论数。
	 */
	private int commentsCount;
	/**
	 * 微博类型，1-原创发表，2-转载，3-私信，4-回复，5-空回，6-提及，7-评论。
	 */
	private String type = "";
	/**
	 * 设置微博类型。
	 * @param type 微博类型。
	 */
	public void setType(String type)
	{
		this.type = type;
	}
	/**
	 * 返回微博类型。
	 * @return 微博类型。
	 */
	public String getType()
	{
		return this.type;
	}
	
	/**
	 * 设置原微博。
	 * @param source 原微博。
	 */
	public abstract void setSource(MicroblogData source);
	/**
	 * 返回原微博（如果有的话）。
	 * @return 原微博。
	 */
	public abstract MicroblogData getSource();
	/**
	 * 设置本微博的转发数。
	 * @param repostsCount 本微博的转发数。
	 */
	public void setRepostsCount(int repostsCount)
	{
		this.repostsCount = repostsCount;
	}
	/**
	 * 返回本微博的转发数。
	 * @return 本微博的转发数。
	 */
	public int getRepostsCount()
	{
		return repostsCount;
	}
	
	/**
	 * 设置本微博的评论数。
	 * @param commentsCount 本微博的评论数。
	 */
	public void setCommentsCount(int commentsCount)
	{
		this.commentsCount = commentsCount;
	}
	/**
	 * 返回本微博的评论数。
	 * @return 本微博的评论数。
	 */
	public int getCommentsCount()
	{
		return commentsCount;
	}
	
	/**
	 * 设置微博唯一标识。
	 * @param microblogID 微博唯一标识。
	 */
	public void setMicroblogID(String microblogID)
	{
		this.microblogID = microblogID;
	}
	/**
	 * 返回微博唯一标识。
	 * @return 微博唯一标识。
	 */
	public String getMicroblogID()
	{
		return microblogID;
	}
	
	/**
	 * 设置微博内容。
	 * @param text 微博内容。
	 */
	public void setText(String text)
	{
		this.text = text;
	}
	/**
	 * 返回微博内容。
	 * @return 微博内容。
	 */
	public String getText()
	{
		return text;
	}
	
	/**
	 * 设置原始图片地址。
	 * @param picSrc 原始图片地址。
	 */
	public void setPicSrc(String picSrc)
	{
		this.picSrc = picSrc;
	}
	/**
	 * 返回原始图片地址。
	 * @return 原始图片地址。
	 */
	public String getPicSrc()
	{
		return picSrc;
	}
	
	/**
	 * 设置本微博的发布时间。
	 * @param creatTime 本微博的发布时间
	 */
	public void setCreatTime(long creatTime)
	{
		this.creatTime = creatTime;
	}
	/**
	 * 返回本微博的发布时间。
	 * @return 本微博的发布时间。
	 */
	public long getCreatTime()
	{
		return creatTime;
	}
	
	/**
	 * 设置程序抓取本微博的时间。
	 * @param collectTime 程序抓取本微博的时间。
	 */
	public void setCollectTime(long collectTime)
	{
		this.collectTime = collectTime;
	}
	/**
	 * 返回程序抓取本微博的时间。
	 * @return 程序抓取本微博的时间。
	 */
	public long getCollectTime()
	{
		return collectTime;
	}
	/**
	 * 返回本微博的发布者。
	 * @return 本微博的发布者。
	 */
	public abstract MicroblogUser getUser();
	/**
	 * 设置本微博的发布者。
	 * @param user 本微博的发布者。
	 */
	public abstract void setUser(MicroblogUser user);
}

