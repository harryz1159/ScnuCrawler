package com.microblog.common.model;

import java.util.HashSet;

import com.scnu.crawler.util.web.WebInterface;

/**
 * POJO类
 * 保存微博用户相关数据
 * 统一各种微博的用户数据类型
 * 增加字段必须修改isObjInfoCompelete、toString方法
 */
public abstract class MicroblogUser implements java.io.Serializable{
	/**
	 * @param key 微博用户唯一标识。
	 */
	public MicroblogUser(String key) {
		this.key = key;
	}
	private static final long serialVersionUID = -332738032648843482L;
	/**
	 * 标识本用户是否应在之后被遍历。
	 */
	private boolean toBeView=false;
	
	/**
	 * 用户唯一标识（新浪微博用户信息的id字段、腾讯微博用户信息的name字段）。
	 */
	private String key = ""; 
	/**
	 * 省份编码（参考省份编码表） (从抓取的数据分析，这个数值应该是String)。
	 */
	private String province = "";
	/**
	 * 性别,m--男，f--女,n--未知。
	 */
	private String gender = ""; 
	/**
	 * 粉丝数（对应followers_count、fansnum）。
	 */
	private int fansCount = -1;
	/**
	 * 关注数(对应friends_count、idolnum)。
	 */
	private int idolsCount = -1;
	/**
	 * 微博数（对应statuses_count、tweetnum）。
	 */
	private int statusesCount = -1;
	//private Date createdAt = null;               //创建时间  暂不支持
	//private String lang = "";                  //用户语言版本  暂不支持
	/**
	 * 抓取用户微博时所抓到的第一条微博的创建时间。
	 */
	private long sinceCreateTime = 0;
	/**
	 * 抓取该用户微博数据完毕时的时间。
	 */
	private long sinceCollectTime = 0;
	/**
	 * 微博用户粉丝列表。
	 */
	private HashSet<MicroblogUser> fans=new HashSet<MicroblogUser>();
	/**
	 * 微博用户关注列表。
	 */
	private HashSet<MicroblogUser> idols=new HashSet<MicroblogUser>();
	public void addFan(MicroblogUser fan)
	{
		if(fans.add(fan))
			fan.addIdol(this);
	}
	public void addIdol(MicroblogUser idol)
	{
		if(idols.add(idol))
			idol.addFan(this);
	}

	
	/**
	 * 设置抓取微博数据完毕时的本地时间。
	 * @param sinceCollectTime 抓取微博数据完毕时的本地时间。
	 */
	public void setSinceCollectTime(long sinceCollectTime)
	{
		this.sinceCollectTime = sinceCollectTime;
	}
	
	/**
	 * 设置抓取用户微博时所抓到的第一条微博的创建时间。
	 * @param sinceCreateTime 抓取用户微博时所抓到的第一条微博的创建时间。
	 */
	public void setSinceCreateTime(long sinceCreateTime)
	{
		this.sinceCreateTime = sinceCreateTime;
	}
	/**
	 * 设置用户省份编码（参考省份编码表）。
	 * @param province 省份编码。
	 */
	public void setProvince(String province) {
		this.province = province;
	}
	/**
	 * 设置用户性别。,m--男，f--女,n--未知。
	 * @param gender 用户性别。
	 */
	public void setGender(String gender) {
		this.gender = gender;
	}
	/**设置用户关注数。
	 * @param idolsCount 用户关注数。
	 */
	public void setIdolsCount(int idolsCount) {
		this.idolsCount = idolsCount;
	}
	/**设置用户粉丝数。
	 * @param fansCount 用户粉丝数。
	 */
	public void setFansCount(int fansCount) {
		this.fansCount = fansCount;
	}
	/**设置用户微博数。
	 * @param statusesCount 用户微博数。
	 */
	public void setStatusesCount(int statusesCount) {
		this.statusesCount = statusesCount;
	}
	//public void setCreatedAt(Date createdAt) {
	//	this.createdAt = createdAt;
	//}
	
	/**
	 * 返回抓取该用户微博数据完毕时的时间。
	 * @return 抓取该用户微博数据完毕时的时间。
	 */
	public long getSinceCollectTime()
	{
		return sinceCollectTime;
	}
	
	/**
	 * 返回抓取用户微博时所抓到的第一条微博的创建时间。
	 * @return 抓取用户微博时所抓到的第一条微博的创建时间。
	 */
	public long getSinceCreateTime()
	{
		return sinceCreateTime;
	}

	/**
	 * 返回用户的省份编码。（参考省份编码表）
	 * @return 省份编码。
	 */
	public String getProvince() {
		return province;
	}

	/**
	 * 返回用户性别，m--男，f--女，n--未知。
	 * @return 用户性别。
	 */
	public String getGender() {
		return gender;
	}

	/**
	 * 返回用户粉丝数。
	 * @return 用户粉丝数。
	 */
	public int getFansCount() {
		return fansCount;
	}

	/**
	 * 返回用户关注数。
	 * @return 用户关注数。
	 */
	public int getIdolsCount() {
		return idolsCount;
	}

	/**
	 * 返回用户微博数。
	 * @return 用户微博数。
	 */
	public int getStatusesCount() {
		return statusesCount;
	}
	
	/**
	 * 判断对象信息是否完整。
	 * @return 完整返回 {@code true}，否则返回 {@code false}。
	 */
	public boolean isObjInfoComplete()
	{
		if(key == "")
		{
			return false;
		}
		if(this.getGender() == "")
		{
			return false;
		}
		if(this.getProvince() == "")
		{
			return false;
		}
		if(this.getFansCount() == -1)
		{
			return false;
		}
		if(this.getIdolsCount() == -1)
		{
			return false;
		}
		if(this.getStatusesCount() == -1)
		{
			return false;
		}
		return true;
	}
	
	/* 判断MicroblogUser与obj是否相等。只有在obj是MicroblogUser类型且二者key相等时返回true。
	 * 两个key均为null或均为空串也被认为是相等，返回true。
	 * @param obj 欲比较的对象。
	 * @return 如果此对象与 obj 参数相同，则返回 {@code true}；否则返回 {@code false}。
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MicroblogUser other = (MicroblogUser) obj;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		return true;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return (getClass().getName()+key).hashCode();
	}

	@Override
	public String toString() {
		return "User :" +
		"key=" + key +
		", province=" + province + 
		", gender=" + gender + 
		", fansCount=" + fansCount + 
		", idolsCount=" + idolsCount + 
		", statusesCount=" + statusesCount;
		/*", createdAt=" + createdAt + 
		", lang=" + lang +*/
	}

	/**
	 * 判断本用户是否应在之后被遍历。
	 * @return 是返回 {@code true}，否则返回 {@code false}。
	 */
	public boolean isToBeView() {
		return toBeView;
	}

	/**
	 * 设置本用户是否应在之后被遍历。
	 * @param toBeView {@code true} 表示需要被遍历，{@code false} 表示不需要被遍历。
	 */
	public void setToBeView(boolean toBeView) {
		this.toBeView = toBeView;
	}

	/**
	 * 返回用户唯一标识。
	 * @return 用户唯一标识。
	 */
	public String getKey() {
		return key;
	}

	/**
	 * 设置用户唯一标识。
	 * @param key 用户唯一标识。
	 */
	public void setKey(String key) {
		this.key = key;
	}
	/**
	 * 返回用户的网络接口。
	 * @return 用户的网络接口。
	 */
	public abstract WebInterface webInterface();
}
