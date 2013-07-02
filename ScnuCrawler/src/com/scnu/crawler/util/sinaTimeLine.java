	/**
	 * 获取某个用户最新发表的微博列表(支持按照范围提取微博数据)
	 * 
	 * @param uid
	 *            需要查询的用户ID。
	 * @param page
	 *            返回结果的页码，默认为1。
	 * @param base_app
	 *            是否只获取当前应用的数据。0为否（所有数据），1为是（仅当前应用），默认为0。
	 * @param sinceID
	 *            获取ID>sinceID的微薄数据
	 * @param maxID
	 *            获取ID<maxID的微薄数据
            
	 * @return list of the user_timeline
	 * @throws WeiboException
	 *             when Weibo service or network is unavailable
	
	 */
package com.scnu.crawler.util;

import weibo4j.Timeline;
import weibo4j.Weibo;
import weibo4j.http.Response;
import weibo4j.model.Paging;
import weibo4j.model.PostParameter;
import weibo4j.model.Status;
import weibo4j.model.StatusWapper;
import weibo4j.model.WeiboException;
import weibo4j.util.WeiboConfig;

public class sinaTimeLine extends Timeline {

	/**
	 * 根据since_ID和max_ID来读取微博数据
	 * @param uid
	 * @param page
	 * @param base_app
	 * @param feature
	 * @param maxID
	 * @param sinceID
	 * @return
	 * @throws WeiboException
	 */
	public StatusWapper getUserTimelineByUid(String uid, Paging page,
			Integer base_app, Integer feature,String maxID,String sinceID) throws WeiboException {
		Weibo weibo = new Weibo();
		return Status.constructWapperStatus(weibo.client.get(
						WeiboConfig.getValue("baseURL")	+ "statuses/user_timeline.json",
						new PostParameter[] {
								new PostParameter("uid", uid),
								new PostParameter("since_id", sinceID),
								new PostParameter("max_id", maxID),
								new PostParameter("base_app", base_app.toString()),
								new PostParameter("feature", feature.toString()) },
						page));
	}
	
	/**
	 * 获取当前登录用户的API访问频率限制情况 
	 * @param access_token
	 * @return
	 * @throws WeiboException
	 * JSON示例
			{
			    "ip_limit": 10000,
			    "limit_time_unit": "HOURS",
			    "remaining_ip_hits": 10000,
			    "remaining_user_hits": 150,
			    "reset_time": "2011-06-03 18:00:00",
			    "reset_time_in_seconds": 1415,
			    "user_limit": 150,
			}
	 */
	public Response getUserLimit(String access_token) throws WeiboException {
		Weibo weibo = new Weibo();
		return weibo.client.get(
						WeiboConfig.getValue("baseURL")	+ "account/rate_limit_status.json",
						new PostParameter[] {
								new PostParameter("access_token",access_token)}
						);
	}	
}

