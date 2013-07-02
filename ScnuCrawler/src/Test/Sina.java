package Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import weibo4j.Account;
import weibo4j.Oauth;
import weibo4j.Timeline;
import weibo4j.Users;
import weibo4j.Weibo;
import weibo4j.model.Paging;
import weibo4j.model.Status;
import weibo4j.model.StatusWapper;
import weibo4j.model.User;
import weibo4j.model.WeiboException;
import weibo4j.util.BareBonesBrowserLaunch;


/**
 * 
 * @author hadoop
 * 读取sina微薄中的数据
 *
 */
public class Sina {
	
	private static Weibo weibo = new Weibo();
	private static Account account = new Account();
	private static int StatusPageCounts = 100;        //分页微博数，最多每页100条 
	private static int weiboSize = 0;
	private static int apiTimes = 0;
	
    public static String get_access_token() throws WeiboException, IOException{
    	
    	Oauth oauth = new Oauth();
		BareBonesBrowserLaunch.openURL(oauth.authorize("code"));
		System.out.print("输入浏览器地址栏code的值，按Enter。[Enter]:");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		String code = br.readLine();
		try{
			String access_token = oauth.getAccessTokenByCode(code).getAccessToken();
			System.out.println(access_token);
			return access_token;
		} catch (WeiboException e) {
			if(401 == e.getStatusCode()){
				//Log.logInfo("Unable to get the access token.");
			}else{
				e.printStackTrace();
			}
		}
    	return null;   	
    }
    
    //获取微博列表
    public static void getSina(){
		
    	try{	    		
	    		weibo.setToken(get_access_token());        //设置access_token
	    		
	            SimpleDateFormat t = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	            //MyHttpClient mh = new MyHttpClient();
	    		
	    		String uid = "1750070171";                 //此处设置需获取微博列表的帐号ID
	    		
	    		Users um = new Users();
	            User user = um.showUserById(uid);	            
	            Timeline tm = new Timeline();	            
	            
	            //获取微博列表
	            for(int i=1;i<=user.getStatusesCount()/StatusPageCounts+1;i++){
	            	
	            	try{
	                	StatusWapper status = tm.getUserTimelineByUid(uid,new Paging(i,StatusPageCounts),0,0);
	                	apiTimes++;
	                    
	                	ArrayList<Map<String, String>>  arrayList  = new ArrayList<Map<String, String>>();
	                	for(Status s : status.getStatuses()){        
	                		Map<String, String> map = new HashMap<String, String>();
	                		map.put("weiboID", s.getId());
	                		map.put("content", s.getText());
	                		map.put("picSrc", s.getOriginalPic());
	                		map.put("nickname", s.getUser().getScreenName());
	                		map.put("createTime", s.getCreatedAt().toString());
	                		map.put("collectTime", t.format(new Date()));
	                		arrayList.add(map);
	                		System.out.println(map);
	                		weiboSize++;	     
	                	}
	            		
	                	//TestHbase.doHbase(arrayList);
	            		//mh.HttpPost(arrayList);
	                	Thread.sleep(24 * 1000); //暂停24s	
	            	}
	            	catch(Exception e){
	            	    //e.printStackTrace();	            	    
	            		break;
	            	}
	        	}    
	    }catch (Exception e) {
    		e.printStackTrace();
	    	System.out.println("接口调用出现问题，可能是超出接口限制次数或者其他原因。");
	    } finally {
	    	System.out.println("当前调用API接口次数为：" + apiTimes + "。");
	    	System.out.println("一共抓取"+weiboSize+"条微博。");
	    }
    	   	
    }
	

	public static void main(String[] args)  throws WeiboException  {
		getSina();
	}

}
