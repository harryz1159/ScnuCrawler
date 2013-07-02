package com.scnu.crawler;

import java.io.DataOutputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Map;

public class MyHttpClient {
	
	private Socket socket;
	
	 public MyHttpClient(){
		 try{ 
			 socket = new Socket("218.192.114.4",30000);
		 }catch(Exception e){
			 e.printStackTrace();
		 }
	 }

	 public void HttpPost(ArrayList<Map<String, String>> data){
		 try{
		   //BufferedReader reader=new BufferedReader(new InputStreamReader(socket.getInputStream()));
		   DataOutputStream ios = new DataOutputStream(socket.getOutputStream()); 
		   ObjectOutputStream oos = new ObjectOutputStream(ios); 
		   oos.writeObject(data);
		   oos.flush();
		   oos.close();	
    	 }
  	     catch(Exception e){
  	       e.printStackTrace();
  	     }
	 } 
}
