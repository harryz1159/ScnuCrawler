package com.hjh;
import com.hjh.inf.*;

public class HBaseFactory 
{
	
	private OperateHbase hf = null;

	public OperateHbase getInstance()
	   {
		   if(hf==null)
			   hf = new OperateHbaseImp();
		   return hf ;
	   }
}
