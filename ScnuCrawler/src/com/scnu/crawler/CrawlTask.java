/**
 * 
 */
package com.scnu.crawler;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import com.microblog.common.dao.DaoManager;
import com.microblog.common.model.MicroblogData;
import com.microblog.common.model.MicroblogUser;
import com.scnu.crawler.util.web.WebInterface;

/**
 * @author 云水寒
 *
 */
public class CrawlTask {
	/**
	 * 记录所有正在运行的CrawlTask所抓取的微博类型，防止同时抓取相同类型的微博。
	 */
	private static Set<Class<? extends MicroblogUser>> runningType=new HashSet<>();
	/**
	 * 本CrawlTask所抓取的微博类型（用微博用户代替）。
	 */
	private final Class<? extends MicroblogUser> type;
	/**
	 * 标识是否已从runningType中移除本CrawlTask所抓取的微博类型。
	 */
	private boolean hasRemove=false;
	private CrawlTask(Class<? extends MicroblogUser> type)
	{
		this.type=type;
	}
	/**
	 * CrawlTask的工厂方法，创建抓取微博类型为type的CrawlTask实例，如果微博类型为type的CrawlTask实例已创建，则该方法返回null。
	 * @param type 微博用户类型。
	 * @return 抓取微博类型为type的CrawlTask实例。
	 */
	public static <T extends MicroblogUser> CrawlTask createTask(Class<T> type)
	{
		if(runningType.add(type))
			return new CrawlTask(type);
		else
		{
			System.out.println("已经有一个用于"+type.getName()+"的抓取任务在执行，请等待该任务执行完毕再重试！");
			return null;
		}
	}
	/**
	 * CrawlTask生命周期结束前调用执行的清理操作。
	 */
	private void clean()
	{
		if(!hasRemove)
		{
			runningType.remove(type);
			hasRemove=true;
		}
	}
	/* （非 Javadoc）
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected void finalize() throws Throwable {
		clean();
		super.finalize();
	}
	/**
	 * 通过微博用户唯一标识构造MicroblogUser。只有当数据库中没有key所代表的用户时才应该使用本方法。
	 * @param key 微博用户标识。
	 * @return 以key为用户唯一标识的MicroblogUser。
	 * @throws NoSuchMethodException 如果因不匹配而无法获得构造MicroblogUser的构造方法。
	 * @throws SecurityException 如果因安全管理器的原因无法获得构造MicroblogUser的构造方法。
	 * @throws InstantiationException 如果CrawlTask.type是MicroblogUser的Class。
	 * @throws IllegalAccessException 如果MicroblogUser的构造方法因访问控制而无法访问。
	 * @throws IllegalArgumentException 如果传递的参数有问题。
	 * @throws InvocationTargetException 如果MicroblogUser的构造方法抛出异常。
	 */
	private MicroblogUser createUser(String key) throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		Constructor<? extends MicroblogUser> con;
		con=type.getConstructor(String.class);
		return con.newInstance(key);
	}
	public void run()
	{
		System.out.println("是否从配置文件中的起始帐号开始抓取？（重新抓取请输入Y，紧接上次抓取请输入其他任意字符）");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String choice="Y";
		try {
			choice = br.readLine();
		} catch (IOException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
			System.err.println("从命令行读取输入失败，将从配置文件中的起始帐号开始抓取微博数据。");
			choice="Y";
		}
		DaoManager dm=new DaoManager();
		if(choice.equalsIgnoreCase("Y"))
		{
			BufferedReader in=null;
			Properties crawlPro=new Properties();
			try {
				in=new BufferedReader(new FileReader("crawlerConfig.properties"));
				crawlPro.load(in);
			} catch (FileNotFoundException e) {
				// TODO 自动生成的 catch 块
				e.printStackTrace();
				System.err.println("找不到crawlerConfig.properties文件，请确保该文件位于程序目录中！");
				return;
			} catch (IOException e) {
				// TODO 自动生成的 catch 块
				e.printStackTrace();
				System.err.println("读取crawlerConfig.properties文件出错！");
				return;
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
			String startKey=crawlPro.getProperty(type.getSimpleName()+"_Start");
			if(startKey==null)
			{
				System.err.println("crawlerConfig.properties文件中缺少"+type.getSimpleName()+"_Start属性，请编辑crawlerConfig.properties文件添加该属性。");
				return;
			}
			dm.setUsersState(type, false);
			MicroblogUser startUser=dm.getUserByKey(startKey, type);
			if(startUser==null)
			{
				try {
					startUser=createUser(startKey);
				} catch (NoSuchMethodException | SecurityException
						| InstantiationException | IllegalAccessException
						| IllegalArgumentException | InvocationTargetException e) {
					// TODO 自动生成的 catch 块
					e.printStackTrace();
					return;
				}
			}
			dm.setUserState(startUser, true);
		}
		int nullTimes=0;
		while(nullTimes<2)
		{
			Collection<? extends MicroblogUser> toBeView=dm.getUserByState(type, true);
			if(toBeView==null||toBeView.isEmpty())
			{
				nullTimes++;
				dm.closeQuery(toBeView);
				try {
					Thread.sleep(30*60*1000);
				} catch (InterruptedException e) {
					// TODO 自动生成的 catch 块
					e.printStackTrace();
					return;
				}
				continue;
			}
			nullTimes=0;
			for(MicroblogUser user:toBeView)
			{
				dm.begin();
				WebInterface witf=user.webInterface();
				if(!witf.updateUserInfo())
				{
					dm.rollback();
					continue;
				}
				ArrayList<? extends MicroblogData> microblogs=witf.getUserStatuses();
				for(MicroblogData microblog:microblogs)
				{
					System.out.println(microblog.getMicroblogID());
					dm.storeToDataStore(microblog);
				}
				String[] fans=witf.getUserFansList();
				if (fans!=null) {
					for (String fanKey : fans) {
						MicroblogUser fan = dm.getUserByKey(fanKey, type);
						if (fan == null)
							try {
								fan = createUser(fanKey);
							} catch (NoSuchMethodException | SecurityException
									| InstantiationException
									| IllegalAccessException
									| IllegalArgumentException
									| InvocationTargetException e) {
								// TODO 自动生成的 catch 块
								e.printStackTrace();
								continue;
							}
						dm.setUserState(fan, true);
					}
				}
				String[] idols=witf.getUserIdolsList();
				if (idols!=null) {
					for (String idolKey : idols) {
						MicroblogUser idol = dm.getUserByKey(idolKey, type);
						if (idol == null)
							try {
								idol = createUser(idolKey);
							} catch (NoSuchMethodException | SecurityException
									| InstantiationException
									| IllegalAccessException
									| IllegalArgumentException
									| InvocationTargetException e) {
								// TODO 自动生成的 catch 块
								e.printStackTrace();
								continue;
							}
						dm.setUserState(idol, true);
					}
				}
				dm.setUserState(user, false);
				dm.commit();
			}
			dm.closeQuery(toBeView);
			try {
				Thread.sleep(3600);
			} catch (InterruptedException e) {
				// TODO 自动生成的 catch 块
				e.printStackTrace();
				return;
			}
		}
	}

}
