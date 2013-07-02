/**
 * 微博数据类
 * 根据sina的status类简化
 * date:2012-12-14
 * 
 */
package com.scnu.crawler.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

public class Status  implements java.io.Serializable {

	private static final long serialVersionUID = -8795691786466526420L;

	private User user = null;                            //作者信息
	private Date createdAt;                              //status创建时间
	private String id;                                   		//status id
	private String mid;                                 		 //微博MID
                    
	private String text;                                 //微博内容
	private Source source;                               //微博来源
	
	private String geo;                                  //地理信息，保存经纬度，没有时不返回此字段
	private double latitude = -1;                        //纬度
	private double longitude = -1;                       //经度
	
	private int repostsCount;                            //转发数
	private int commentsCount;                           //评论数
	private String annotations;                          //元数据，没有时不返回此字段
	
	public Status()
	{
	}
	
	/*
	public Status(Response res)throws WeiboException{
		super(res);
		JSONObject json=res.asJSONObject();
		constructJson(json);
	}
	*/

	private void constructJson(JSONObject json) throws JSONException, ParseException {
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
			createdAt =sdf.parse( json.getString("created_at"));
			id = json.getString("id");
			mid=json.getString("mid");
			text = json.getString("text");
			if(!json.getString("source").isEmpty()){
				source = new Source(json.getString("source"));
			}
			repostsCount = json.getInt("reposts_count");
			commentsCount = json.getInt("comments_count");
			annotations = json.getString("annotations");
			if(!json.isNull("user"))
				user = new User(json.getJSONObject("user"));
			geo= json.getString("geo");
			if(geo!=null &&!"".equals(geo) &&!"null".equals(geo)){
				getGeoInfo(geo);
			}
		} catch (JSONException je) {
			throw new JSONException(je.getMessage() + ":" + json.toString());
		}
	}

	private void getGeoInfo(String geo) {
		StringBuffer value= new StringBuffer();
		for(char c:geo.toCharArray()){
			if(c>45&&c<58){
				value.append(c);
			}
			if(c==44){
				if(value.length()>0){
					latitude=Double.parseDouble(value.toString());
					value.delete(0, value.length());
				}
			}
		}
		longitude=Double.parseDouble(value.toString());
	}


	public Status(JSONObject json)throws JSONException, ParseException{
		constructJson(json);
	}
	
	public Status(String str) throws  JSONException, ParseException {
		// StatusStream uses this constructor
		JSONObject json = new JSONObject(str);
		constructJson(json);
	}

	public User getUser() {
		return user;
	}
	public void setUser(User user) {
		this.user = user;
	}
	public Date getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public Source getSource() {
		return source;
	}
	public void setSource(Source source) {
		this.source = source;
	}
	public String getGeo() {
		return geo;
	}
	public void setGeo(String geo) {
		this.geo = geo;
	}
	public double getLatitude() {
		return latitude;
	}
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}
	public double getLongitude() {
		return longitude;
	}
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}
	public int getRepostsCount() {
		return repostsCount;
	}
	public void setRepostsCount(int repostsCount) {
		this.repostsCount = repostsCount;
	}
	public int getCommentsCount() {
		return commentsCount;
	}
	public void setCommentsCount(int commentsCount) {
		this.commentsCount = commentsCount;
	}
	public String getMid() {
		return mid;
	}
	public void setMid(String mid) {
		this.mid = mid;
	}
	public String getAnnotations() {
		return annotations;
	}
	public void setAnnotations(String annotations) {
		this.annotations = annotations;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Status other = (Status) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "Status [user=" + user +  ", createdAt="
				+ createdAt + ", id=" + id + ", text=" + text + ", source="
				+ source +  ", geo=" + geo
				+ ", latitude=" + latitude + ", longitude=" + longitude
				+ ", repostsCount=" + repostsCount + ", commentsCount="
				+ commentsCount + ", mid=" + mid + ", annotations="
				+ annotations+ "]";
	}

}
