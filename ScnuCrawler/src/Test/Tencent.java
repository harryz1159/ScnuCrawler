package Test;

import java.net.URI;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;

import com.tencent.weibo.api.StatusesAPI;
import com.tencent.weibo.api.TAPI;
import com.tencent.weibo.api.UserAPI;
import com.tencent.weibo.oauthv2.OAuthV2;
import com.tencent.weibo.oauthv2.OAuthV2Client;
import com.tencent.weibo.utils.QHttpClient;

public class Tencent {

    private static OAuthV2 oAuth=new OAuthV2();
	/**
	 * @param args
	 */
    private static void init(OAuthV2 oAuth) {
        oAuth.setClientId("801207932");
        oAuth.setClientSecret("e8cf2b0dd04f3b2b2e81588fddfca286");
        oAuth.setRedirectUri("http://www.qq.com/");
    }
    
    private static void openBrowser(OAuthV2 oAuth) {
		
        String authorizationUrl = OAuthV2Client.generateAuthorizationURL(oAuth);

        //调用外部浏览器
        if( !java.awt.Desktop.isDesktopSupported() ) {

            System.err.println( "Desktop is not supported (fatal)" );
            System.exit( 1 );
        }
        java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
        if(desktop == null || !desktop.isSupported( java.awt.Desktop.Action.BROWSE ) ) {

            System.err.println( "Desktop doesn't support the browse action (fatal)" );
            System.exit( 1 );
        }
        try {
            desktop.browse(new URI(authorizationUrl));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit( 1 );
        }
        
        System.out.println("Input the authorization information (eg: code=CODE&openid=OPENID&openkey=OPENKEY) :");
        Scanner in = new Scanner(System.in);
        String responseData = in.nextLine(); 
        in.close();
        
        if(OAuthV2Client.parseAuthorization(responseData, oAuth)){
            System.out.println("Parse Authorization Information Successfully");
        }else{
            System.out.println("Fail to Parse Authorization Information");
            return;
        }
    }
    
    public static String TimeStamp2Date(String timestampString){  
    	  Long timestamp = Long.parseLong(timestampString)*1000;  
    	  String date = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(timestamp));  
    	  return date;  
    	}  

	
	public static void main(String[] args) throws Exception {
		
	    init(oAuth); 

	    //自定制http连接管理器
        QHttpClient qHttpClient=new QHttpClient(2, 2, 5000, 5000, null, null);
        OAuthV2Client.setQHttpClient(qHttpClient);
        
       //调用外部浏览器，请求用户授权，并读入授权码等参数
       openBrowser(oAuth);     
       
       //检查是否正确取得授权码
       if (oAuth.getStatus() == 2) {
           System.out.println("Get Authorization Code failed!");
           return;
       }
       
       //换取access token
       oAuth.setGrantType("authorize_code");
       try {
           OAuthV2Client.accessToken(oAuth);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
       
       //检查是否正确取得access token
       if (oAuth.getStatus() == 3) {
            System.out.println("Get Access Token failed!");
            return;
        }
       
       qHttpClient.shutdownConnection();
       
       int StatusPageCounts = 70;               //翻页微博数,最多每页70条
	   int CommentsPageCounts = 100;            //翻页评论数，最多每页100条
	   String name = "jianwangkeji11";          //微博帐号名
       
       UserAPI userAPI = new UserAPI(oAuth.getOauthVersion());
       StatusesAPI statusesAPI = new StatusesAPI(oAuth.getOauthVersion());
       TAPI tAPI = new TAPI(oAuth.getOauthVersion());
       
       String Userjson = userAPI.otherInfo(oAuth, "json", name, "");      
       JSONObject Userobj = new JSONObject(Userjson);
       
     //String Statusesjson = statusesAPI.broadcastTimeline(oAuth, "json", "0", "0", "1", "0", "3", "0");
       String Statusesjson = statusesAPI.userTimeline(oAuth, "json", "0", "0", Integer.toString(StatusPageCounts), 
    		   										  "0", name, "", "3", "0");
       int a =0;
       int b =0;
       for(int i = 0; i<Userobj.getJSONObject("data").getInt("tweetnum")/StatusPageCounts+1; i++){
 	   
	       try{
	    	   JSONObject Statusesobj = new JSONObject(Statusesjson);	  
		       JSONArray Statusesinfo = new JSONArray(Statusesobj.getJSONObject("data").getString("info"));
		       JSONObject Statusesitem; 
		       
		       //获取每一次分页的微博
		       for(int j=0; j<Statusesinfo.length(); j++){
		    	   
		    	   Statusesitem = (JSONObject) Statusesinfo.get(j);
		    	   System.out.println(Statusesitem.getString("nick")+"："+Statusesitem.getString("text")+" "
		    			   +TimeStamp2Date(Statusesitem.getString("timestamp")));
		    	   a++;
		    	   
		    	   if(j == Statusesinfo.length()-1)    //设置微博下一页参数
		    		   Statusesjson = statusesAPI.userTimeline(oAuth, "json", "1", 
		    				   Statusesitem.getString("timestamp"), Integer.toString(StatusPageCounts), Statusesitem.getString("id"), 
		    				   name, "", "3", "0");	
		
		    	   //获取此条微博的评论列表
		    	  /*if(Statusesitem.getInt("mcount")>0){
		    	   
			    	   String Commentsjson = tAPI.reList(oAuth, "json", "1", 
			    			   Statusesitem.getString("id"), "0", "0", Integer.toString(CommentsPageCounts), "0");		 
			    	   
			    	   for(int k=0; k<Statusesitem.getInt("mcount")/CommentsPageCounts+1; k++){
			    	   
				    	   try{
				    		   JSONObject Commentsobj = new JSONObject(Commentsjson);				    				    	   
					           JSONArray Commentsinfo = new JSONArray(Commentsobj.getJSONObject("data").getString("info"));		           
					           JSONObject Commentsitem; 
					           
					           for(int l=0;l<Commentsinfo.length();l++){
					        	   
					        	   Commentsitem = (JSONObject) Commentsinfo.get(l);
					        	   System.out.println(Commentsitem.getString("nick")+"："+Commentsitem.getString("text")+" "
					        			   +TimeStamp2Date(Commentsitem.getString("timestamp")));
					        	   
					        	   if(l == Commentsinfo.length()-1)   //设置评论下一页参数
					        		   Commentsjson = tAPI.reList(oAuth, "json", "1", 
							    			   Statusesitem.getString("id"), "1", Commentsitem.getString("timestamp"), 
							    			   Integer.toString(CommentsPageCounts), Commentsitem.getString("id"));	
					        	   b++;
					        	   
					           }
				    	   }catch(Exception e){
				    		   System.out.println("获取评论接口调用了"+b+"次，超过限制次数！");
				    	   }
			    	   }	    	   
		    	   }*/
		       }
	       }catch(Exception e){
	    	   System.out.println("获取微博接口调用了"+a+"次，超过限制次数！");
	       }
       }
       
       System.out.println("一共有"+a+"条微博。");
       System.out.println("一共有"+b+"条评论。");
    	   
   }

}
