/**
 * 
 */
package com.scnu.crawler.util.web;

import java.util.ArrayList;

import com.microblog.common.model.MicroblogData;

/**
 * 通过网络获取各种与用户有关的信息的接口。
 * @author 云水寒
 *
 */
public interface WebInterface {
	/**
	 * 从服务器更新用户的信息。
	 * @return 更新成功则返回 {@code true}，否则返回 {@code false}。
	 */
	boolean updateUserInfo();
	/**
	 * 获取用户的所有微博信息。
	 * @return 用户微博信息列表。
	 */
	ArrayList<? extends MicroblogData> getUserStatuses();
	/**
	 * 返回用户粉丝列表。
	 * @return 用户粉丝的唯一标识列表。
	 */
	ArrayList<String> getUserFansList();
	/**
	 * 返回用户关注列表。
	 * @return 用户关注的唯一标识列表。
	 */
	ArrayList<String> getUserIdolsList();

}
