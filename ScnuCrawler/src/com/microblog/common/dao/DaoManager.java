/**
 * 
 */
package com.microblog.common.dao;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.jdo.FetchPlan;
import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.Transaction;

import com.microblog.common.model.MicroblogUser;

/**
 * @author 云水寒
 *
 */
public class DaoManager {
	private static PersistenceManagerFactory pmf;
	private PersistenceManager pm=pmf.getPersistenceManager();
	private Transaction tx=pm.currentTransaction();
	private Query q=pm.newQuery();
	static
	{
		BufferedReader in=null;
		Properties jdoPro=new Properties();
		try {
			in=new BufferedReader(new FileReader("daoconfig"));
			jdoPro.load(in);
		} catch (FileNotFoundException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
			System.err.println("找不到daoconfig文件，请确保该文件位于程序目录中！");
			System.exit(1);
		} catch (IOException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
			System.err.println("读取daoconfig文件出错！");
			System.exit(1);
		}
		finally
		{
			if(in!=null)
				try {
					in.close();
				} catch (IOException e) {
					// TODO 自动生成的 catch 块
					e.printStackTrace();
				}
		}
		jdoPro.put("javax.jdo.PersistenceManagerFactoryClass", "org.datanucleus.api.jdo.JDOPersistenceManagerFactory");
		jdoPro.put("javax.jdo.option.RetainValues", "false");
		jdoPro.put("javax.jdo.option.Optimistic", "false");
		jdoPro.put("javax.jdo.option.TransactionIsolationLevel", "repeatable-read");
		jdoPro.put("datanucleus.nontx.atomic", "true");
		jdoPro.put("datanucleus.query.jdoql.allowAll", "true");
		pmf=JDOHelper.getPersistenceManagerFactory(jdoPro);
	}
	/**
	 * 通过微博用户唯一标识获取微博用户。
	 * @param key 微博用户唯一标识。
	 * @param type 微博用户类型（新浪or腾讯）。
	 * @return key所对应的微博用户。
	 */
	public <T extends MicroblogUser> T getUserByKey(String key,Class<T> type)
	{
		try {
			return pm.getObjectById(type, key);
		} catch ( javax.jdo.JDOObjectNotFoundException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
			return null;
		}
	}
	/**
	 * 设置存储空间中所有微博用户的toBeView字段。
	 * @param type 微博用户类型（新浪or腾讯）。
	 * @param state toBeView字段的新值。
	 * @return 被设置字段的微博用户数。
	 */
	public long setUsersState(Class<? extends MicroblogUser> type,boolean state)
	{
		Query query=pm.newQuery("UPDATE "+type.getName()+" SET this.toBeView=state WHERE this.toBeView!=state PARAMETERS boolean state");
		return (Long) query.execute(state);
	}
	/**
	 * 标示事务开始。
	 */
	public void begin()
	{
		tx.begin();
	}
	/**
	 * 标示事务提交。
	 */
	public void commit()
	{
		tx.commit();
	}
	/**
	 * 标示事务回滚。
	 */
	public void rollback()
	{
		tx.rollback();
	}
	/**
	 * 把对象存储到DataStore中。
	 * @param pc 要存储的对象。
	 * @return 被存储的对象。
	 */
	public <T> T storeToDataStore(T pc)
	{
		return pm.makePersistent(pc);
	}
	/**
	 * 设置特定微博用户的状态。
	 * @param user 微博用户。
	 * @param state 状态（是否之后要被访问）。
	 */
	public void setUserState(MicroblogUser user,boolean state)
	{
		user.setToBeView(state);
		pm.makePersistent(user);
	}
	/**
	 * 获取toBeView为指定值的微博用户列表。粉丝数少于2且关注数少于2且最近抓取过的微博用户不在返回列表中。
	 * @param type 微博用户类型（新浪or腾讯）。
	 * @param state toBeView的指定值。
	 * @return 满足要求的微博用户列表。
	 */
	public <T extends MicroblogUser> Collection<T> getUserByState(Class<T> type,boolean state)
	{
		long currentTime=new Date().getTime();
		Collection<T> result=null;
		try {
			tx.begin();
			q.setClass(type);
			q.setFilter("(idolsCount>2||fansCount>2||currentTime-sinceCollectTime>min_interval)&&toBeView==state");
			q.declareParameters("long currentTime,long min_interval,boolean state");
			q.getFetchPlan().setFetchSize(FetchPlan.FETCH_SIZE_OPTIMAL);
			q.addExtension("datanucleus.query.loadResultsAtCommit", "false");
			result = (List<T>)q.execute(currentTime, 30 * 60 * 1000, state);
			System.out.println(result.size());
			tx.commit();
		} catch (Exception e) {
			// TODO: handle exception
		}
		finally
		{
			if(tx.isActive())
				tx.rollback();
		}
		return result;
	}
	/**
	 * 关闭查询，所有查询一旦使用完毕都应该被关闭。
	 * @param qresult 被关闭查询的查询结果。
	 */
	public void closeQuery(Object qresult)
	{
		q.close(qresult);
	}

}
