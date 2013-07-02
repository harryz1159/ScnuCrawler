/*
	微博用户类
	根据sina 微博的user类进行了简化设计	
 */
package com.scnu.crawler.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * A data class representing Basic user information element
 * 参照sina 微博的user类进行了简化设计
 *date:2012-12-14
 */
public class User  implements java.io.Serializable {

	private static final long serialVersionUID = -332738032648843482L;
	private String id;                      //用户UID
	private String screenName;            //微博昵称
	private String name;                  //友好显示名称，如Bill Gates,名称中间的空格正常显示(此特性暂不支持)
	private int province;                 //省份编码（参考省份编码表）
	private String gender;                //性别,m--男，f--女,n--未知
	private int followersCount;           //粉丝数
	private int friendsCount;             //关注数
	private int statusesCount;            //微博数
	private Date createdAt;               //创建时间
	private String lang;                  //用户语言版本

	public void setId(String id) {
		this.id = id;
	}
	public void setScreenName(String screenName) {
		this.screenName = screenName;
	}
	public void setName(String name) {
		this.name = name;
	}
	public void setProvince(int province) {
		this.province = province;
	}
	public void setGender(String gender) {
		this.gender = gender;
	}
	public void setFollowersCount(int followersCount) {
		this.followersCount = followersCount;
	}
	public void setFriendsCount(int friendsCount) {
		this.friendsCount = friendsCount;
	}
	public void setStatusesCount(int statusesCount) {
		this.statusesCount = statusesCount;
	}
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}
	/*package*/public User(JSONObject json) throws JSONException  {
		init(json);
	}

	private void init(JSONObject json) throws JSONException {
		if(json!=null){
			try {
				id = json.getString("id");
				screenName = json.getString("screen_name");
				name = json.getString("name");
				province = json.getInt("province");
				gender = json.getString("gender");
				followersCount = json.getInt("followers_count");
				friendsCount = json.getInt("friends_count");
				statusesCount = json.getInt("statuses_count");
				//date convert
				SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
				createdAt =sdf.parse( json.getString("created_at"));
				lang = json.getString("lang");
			} catch (JSONException jsone) {
				throw new JSONException(jsone);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public String getId() {
		return id;
	}

	public String getScreenName() {
		return screenName;
	}

	public String getName() {
		return name;
	}

	public int getProvince() {
		return province;
	}

	public String getGender() {
		return gender;
	}

	public int getFollowersCount() {
		return followersCount;
	}

	public int getFriendsCount() {
		return friendsCount;
	}

	public int getStatusesCount() {
		return statusesCount;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		User other = (User) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "User [" +
		"id=" + id +
		", screenName=" + screenName + 
		", name="+ name +
		", province=" + province + 
		", gender=" + gender + 
		", followersCount=" + followersCount + 
		", friendsCount=" + friendsCount + 
		", statusesCount=" + statusesCount + 
		", createdAt=" + createdAt + 
		", lang=" + lang +
		"]";
	}
}
