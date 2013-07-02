package com.scnu.crawler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import com.scnu.crawler.util.TaskWatcher;

public class MicroblogCrawler {
	
	public static void main(String[] args) throws Exception{
		// TODO Auto-generated method stub
		System.out.print("请通过输入数字选择待处理的微博类型（1.新浪微博；2.腾讯微博）：");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String microblogType = br.readLine();
		TaskWatcher tw=new TaskWatcher();
		switch(microblogType)
		{
			case "1" : 
				System.out.println("正准备抓取Sina微博数据。。。");
				Thread.sleep(3000);
				tw.schedule(new Runnable()
				{
					public void run()
					{
						try {
							SinaCrawler.crawlLogic();
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}, 0, 20000);
				break;
			case "2" : 
				System.out.println("正准备抓取Tencent微博数据。。。");
				Thread.sleep(3000);
				tw.schedule(new Runnable()
				{
					public void run()
					{
						try {
							TencentCrawler.crawlLogic();
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}, 0, 20000);
				break;
			default : System.out.println("请选择正确的值。");
		}
		//System.out.println("数据抓取完毕。");
	}

}
