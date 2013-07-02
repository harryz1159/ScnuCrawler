package com.scnu.crawler.util;

import java.util.Timer;
import java.util.TimerTask;

/**
 *һ������������������԰���������ʱ��������ÿ��һ��ʱ���������Ƿ�������ϣ���������������������������
 *����ͨ�����stop������ֹ�˼������ͼ������е�����
 */
public class TaskWatcher
{
	private Timer tm=new Timer();
	private boolean stop=false;
	/**
	 *�趨��Ҫ���е������������ָ����ʱ�����������Ҽ�������������������ÿ��һ����ʱ���������Ƿ�������ϣ���������������������������
	 * @param      rn   �����е�����
	 * @param      delay   ������ʱ����λ���룩��
	 * @param      pri   ���������λ���룩��
	 */
	public void schedule(final Runnable rn,long delay,long pri)
	{
		tm.schedule(new TimerTask()
		{
			private Thread target;
			private Runnable task=rn;
			public void run()
			{
				if(stop)
					tm.cancel();
				else
					if(target==null||target.getState()==Thread.State.TERMINATED)
					{
						target=new Thread(task);
						target.start();
					}
			}
		}, delay, pri);
	}
	/**
	 *��ֹ�˼������ͼ������е�����
	 */
	public void stop()
	{
		stop=true;
	}
}