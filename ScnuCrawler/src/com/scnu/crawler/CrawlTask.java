/**
 * 
 */
package com.scnu.crawler;

import java.util.HashSet;
import java.util.Set;

import com.microblog.common.model.MicroblogUser;

/**
 * @author 云水寒
 *
 */
public class CrawlTask {
	private static Set<Class<? extends MicroblogUser>> runningType=new HashSet<>();
	private final Class<? extends MicroblogUser> type;
	private CrawlTask(Class<? extends MicroblogUser> type)
	{
		this.type=type;
	}
	public static <T extends MicroblogUser> CrawlTask createTask(Class<T> type)
	{
		if(runningType.contains(type))
		{
			System.out.println("已经有一个用于"+type.getName()+"的抓取任务在执行，请等待该任务执行完毕再重试！");
			return null;
		}
		runningType.add(type);
		return new CrawlTask(type);
	}

}
