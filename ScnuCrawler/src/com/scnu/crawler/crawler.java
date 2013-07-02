package com.scnu.crawler;

import java.util.List;

import com.scnu.crawler.util.Comment;
import com.scnu.crawler.util.Status;
import com.scnu.crawler.util.User;

/**
 * 获取微薄数据的访问接口，设计可以访问的5类微薄类型包括：
 * 		sina
 * 		tecent
 * 		twitter
 * 		facebook
 * 		linkedin
 * @author hadoop
 *  Date:2012-12-14
 *  需要保存的数据，数据存储在weiboSchedule中：
 *  			UID			wbID				用户最后一条weibo的编号
 *  		
 *  
 *  基本的提取策略
 *  1 以当前用户的为几点，提取粉丝和关注用户
 *  2 以其中的一个用户为起点，提取粉丝和关注用户
 *     需要考虑的问题：
 *     a 用户的粉丝和关注度定期更新，不需要每次读读取
 *     b 用调度策略来控制连接数
 */

public interface crawler {
	public	Integer maxConnnects=0 ;			//允许的最大连接数
	public String	weiboType=null;				//微博类型
				
	/**
	 *初始化 
	 * @param lsWeiboType -微薄类型
	 * @return
	 */
	Boolean init(String lsWeiboType);
	
	/**
	 * 获取访问令牌
	 * @return
	 * 	存储在string中的访问令牌
	 */
	String getAccessToken();
	
	/**
	 * 按照ID获取用户微博数据
	 * @param accessToken	--访问令牌
	 * @param uid						--用户ID
	 * @param sinceID			    --获取ID>sinceID的数据
	 * @param maxID				--获取ID<maxID的数据
	 * @return
	 */
	List<Status>	getDataByID(String accessToken,String uid,String sinceID,String maxID);
	
	/**
	 * 获取当前用户及其所关注的用户的微博数据
	 * @param accessToken	--访问令牌
	 * @param uid						--用户ID
	 * @param sinceID			    --获取ID>sinceID的数据
	 * @param maxID				--获取ID<maxID的数据
	 */
	List<Status>	getData(String accessToken,String uid,String sinceID,String maxID);	
	
	/**
	 * 获取用户的关注列表
	 * @param accessToken-访问令牌
	 * @param uid					-用户ID
	 * @return
	 */
	List<User>  getFriends(String accessToken,String uid);
	
	/**
	 * 获取当前用户的的粉丝列表
	 * @param accessToken-访问令牌
	 * @param uid					--用户ID
	 * @return
	 */
	List<User>  getFollows(String accessToken,String uid);
	
	/**
	 * 获取微博的评论数据
	 * @param accessToken
	 * @param wbID
	 * @return
	 */
	List<Comment>  getComments(String accessToken,String wbID)	;
	
	/**
	 * 获取当前用户的剩余点击数
	 * @return
	 */
	Integer getRemainHits();
	
	/**
	 * 获取指定用户上次获取的最后一条WEBO ID
	 * @param UID	--用户ID
	 * @return- 微博 ID
	 */
	String getLastID(String UID);
	
	/**
	 * 设置指定用户已经获取的最大weiboID
	 * @param UID
	 * @param weiboID
	 * @return
	 */
	Boolean setLastID(String UID,String weiboID);
	
	
	/**
	 * 执行数据抓取
	 * @return
	 */
	Boolean doCrawler();
}
