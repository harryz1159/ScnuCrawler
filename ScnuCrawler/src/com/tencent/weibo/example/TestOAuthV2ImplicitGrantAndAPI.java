package com.tencent.weibo.example;

import java.net.URI;
import java.util.Scanner;

import net.sf.json.JSONObject;

import com.tencent.weibo.api.FriendsAPI;
import com.tencent.weibo.api.InfoAPI;
import com.tencent.weibo.api.PrivateAPI;
import com.tencent.weibo.api.SearchAPI;
import com.tencent.weibo.api.StatusesAPI;
import com.tencent.weibo.api.TAPI;
import com.tencent.weibo.api.UserAPI;
import com.tencent.weibo.oauthv2.OAuthV2;
import com.tencent.weibo.oauthv2.OAuthV2Client;
import com.tencent.weibo.utils.QHttpClient;

/**
 * 使用OAuth V2 的 Implicit模式 完成授权 以及 API调用示例
 *
 */
public class TestOAuthV2ImplicitGrantAndAPI {

    private static OAuthV2 oAuth = new OAuthV2();

    public static void main(String[] args) {

    /*
     * 初始化 OAuth ，设置 app key 和 对应的 secret 
     * 重复测试时，可直接对access token等鉴权参数赋值，
     * 以便省略授权过程（完成一次正常的授权过程即可手动设置）
     */
        init(oAuth);

/*
 * -----------------------------------------授权流程 begin--------------------------------------------------
 */
      //自定制http连接管理器
        QHttpClient qHttpClient=new QHttpClient(2, 2, 5000, 5000, null, null);
        OAuthV2Client.setQHttpClient(qHttpClient);
        
        // 调用外部浏览器，请求用户授权，并直接取得AccessToken
        openBrowser(oAuth);
        
        qHttpClient.shutdownConnection();
/*
 * -----------------------------------------授权流程 end--------------------------------------------------
 */

        // 执行测试列表
        try {
            new TestOAuthV2ImplicitGrantAndAPI().testList();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void testList() throws Exception {
        System.out.println("Response from server：");
        /*
         * -----------------------------------------简化测试参数 begin-----------------------------------------------
         * 下列每个参数的具体含义和取值，请参看其所在函数的doc文档
         */
        String response;
        String format = "json";
        String clientip = "127.0.0.1";
        String jing = "";
        String wei = "";
        String syncflag = "";
        String pageflag = "0";
        String pagetime = "0";
        String reqnum = "5";
        String lastid = "'0";
        String contenttype = "0";
        String content = "3";// 注意：因为后台会对微博内容进行判重，所以在重复测试时加上变换部分++++++++
        String twitterid = "0";
        String fopenids = "";
        String fopenid = "";
        String reid = null;
        String ids = null;
        String id = null;
        String names="api_weibo,t-qq-com,vvtest1";
        String name="t-qq-com";
        String flag = "2";
        String keyword = "微博";
        String pagesize = "5";
        String page = "0";
        String searchtype = "0";
        String msgtype = "0";
        String sorttype = "0";
        String type = "0";
        String op = "0";
        String starttime = "";
        String endtime="";
        String province = "";
        String city = "";
        String longitue = "";
        String latitude = "";
        String radius = "";
        String startindex = "0";
        String mode = "0";
        String install = "0";
        String picpath = System.getProperty("user.dir") + "\\src\\main\\resources\\logo_QWeibo.jpg";
/*
 * -----------------------------------------简化测试参数 end-----------------------------------------------
 */

/*
*---------------------------------------- 微博相关测试例 begin---------------------------------------------
*   注意：
*   微博服务器对发微博的频率有限制，如果不加 sleep() 直接执行下列多条发微博操作， 
*   可能会出现 ret=4 errcode=10 的错误码，意思是：发表太快，被频率限制 
*/  
            TAPI tAPI=new TAPI(oAuth.getOauthVersion());//根据oAuth配置对应的连接管理器

            //取得返回结果
            response=tAPI.add(oAuth, format, "测试发表文字微博"+content, clientip, jing, wei, syncflag);
                 // json数据使用
                 // response的结果可能是这样，{"data":{"id":"90221131024999","time":1333002978},"errcode":0,"msg":"ok","ret":0}
                 // 下面的代码将取出 id 的对应值，并赋予 reid
                System.out.println("response = "+response);
                JSONObject responseJsonObject;
                JSONObject dataJsonObject;
                responseJsonObject= JSONObject.fromObject(response);
                dataJsonObject=(JSONObject)responseJsonObject.get("data");
                id=ids=reid=dataJsonObject.get("id").toString();//对后面用到的 reid 赋值
                System.out.println("reid = "+ reid);
            try { Thread.sleep ( 5000 ) ; } catch (InterruptedException ie){}
            
            response=tAPI.addPic(oAuth, format, "发表一条带网络图片的微博"+content, clientip, jing, wei, "http://t3.qpic.cn/mblogpic/d26d1168b2c6c25db192/460", syncflag);
                //取出返回的 id
                responseJsonObject= JSONObject.fromObject(response);
                dataJsonObject=(JSONObject)responseJsonObject.get("data");
                ids+=","+dataJsonObject.get("id").toString();//对后面用到的 ids 赋值
                System.out.println("ids = "+ ids);
            try { Thread.sleep ( 5000 ) ; } catch (InterruptedException ie){}
            
            tAPI.addPic(oAuth, format, "发表一条带本地图片的微博"+content, clientip, jing, wei, picpath, syncflag);
                try { Thread.sleep ( 5000 ) ; } catch (InterruptedException ie){}
            
            //更多微博相关API测试
            tAPI.reAdd(oAuth, format, "转播一条微博"+content, clientip, jing, wei, reid);
            try { Thread.sleep ( 5000 ) ; } catch (InterruptedException ie){}
            
            tAPI.reCount(oAuth, format, ids, flag);
            tAPI.show(oAuth, format, id);
            tAPI.comment(oAuth, format, "点评一条微博"+content, clientip, jing, wei, reid);
            try { Thread.sleep ( 5000 ) ; } catch (InterruptedException ie){}
            
            tAPI.addVideo(oAuth, format, "发表视频微博"+content, clientip, jing, wei, "http://www.tudou.com/programs/view/yx41TA6rQfE/?resourceId=0_03_05_07", syncflag);
            try { Thread.sleep ( 5000 ) ; } catch (InterruptedException ie){}
            
            tAPI.reList(oAuth, format, flag, id, pageflag, pagetime, reqnum, twitterid);
            tAPI.del(oAuth, format, id);
            
            tAPI.shutdownConnection();//关闭连接管理器

/*
*------------------------------------------ 微博相关测试例 end--------------------------------------------
*/  
 
        
/*
-------------------------------------------关系链相关测试例 begin-----------------------------------------
 */                          
            FriendsAPI friendsAPI= new FriendsAPI(oAuth.getOauthVersion());
            
            name="vvtest1";
            friendsAPI.add(oAuth, format, name, fopenids);
            friendsAPI.del(oAuth, format, name, fopenid);//该操作会删除您收听的人，请慎重使用
            name="t-qq-com";
            friendsAPI.check(oAuth, format, names, fopenids, flag);
            friendsAPI.fanslist(oAuth, format, reqnum, startindex, mode, install);
            friendsAPI.fanslistS(oAuth, format, reqnum, startindex, install);
            friendsAPI.idollist(oAuth, format, reqnum, startindex, install);
            friendsAPI.userFanslist(oAuth, format, reqnum, startindex, name, fopenid, mode, install);
            friendsAPI.userIdollist(oAuth, format, reqnum, startindex, name, fopenid, install);

            friendsAPI.shutdownConnection();
/*
-------------------------------------------关系链相关测试例 end --------------------------------------------
*/  

            
/*
----------------------------------------- 数据更新相关测试例 begin-------------------------------------------
*/              
            InfoAPI infoAPI= new InfoAPI(oAuth.getOauthVersion());
            
            infoAPI.update(oAuth, format, op, type);
            
            infoAPI.shutdownConnection();
/*
------------------------------------------- 数据更新相关测试例 end-------------------------------------------
*/          
        
        
/*
-------------------------------------------- 账户相关测试例 begin---------------------------------------------
*/              
            UserAPI userAPI= new UserAPI(oAuth.getOauthVersion());
            
            userAPI.info(oAuth, format);
            userAPI.infos(oAuth, format, names, fopenid);
            userAPI.otherInfo(oAuth, format, name, fopenid);
            
            userAPI.shutdownConnection();
/*
--------------------------------------------- 账户相关测试例 end-----------------------------------------------
*/                       
 
        
/*
-------------------------------------------- 搜索相关测试例 begin-----------------------------------------------
*/                    
            SearchAPI searchAPI= new SearchAPI(oAuth.getOauthVersion());
            
            searchAPI.t(oAuth, format, keyword, pagesize, page, contenttype, sorttype, msgtype, searchtype, starttime,endtime, province, city, longitue, latitude, radius);

            searchAPI.shutdownConnection();
/*
--------------------------------------------- 搜索相关测试例 end ------------------------------------------------
*/               
                
                                
/*
--------------------------------------------- 私信相关测试例 begin-----------------------------------------------
*/                     
            PrivateAPI privateAPI= new PrivateAPI(oAuth.getOauthVersion());
            
            privateAPI.recv(oAuth, format, pageflag, pagetime, reqnum, lastid ,contenttype);
            privateAPI.send(oAuth, format, pageflag, pagetime, reqnum, lastid ,contenttype);
            
            privateAPI.shutdownConnection();
/*
---------------------------------------------- 私信相关测试例 end------------------------------------------------
*/
                
                
/*
--------------------------------------------- 时间线相关测试例 begin----------------------------------------------
*/                                 
            StatusesAPI statusesAPI= new StatusesAPI(oAuth.getOauthVersion());
            
            statusesAPI.broadcastTimeline(oAuth, format, pageflag, pagetime, reqnum, lastid, type, contenttype);
            statusesAPI.homeTimeline(oAuth, format, pageflag, pagetime, reqnum, type, contenttype);
            statusesAPI.mentionsTimeline(oAuth, format, pageflag, pagetime, reqnum, lastid, type, contenttype);
            statusesAPI.usersTimeline(oAuth, format, pageflag, pagetime, reqnum, lastid, names, fopenids, type, contenttype);
            statusesAPI.userTimelineIds(oAuth, format, pageflag, pagetime, reqnum, lastid, name, fopenid, type, contenttype);
            statusesAPI.userTimeline(oAuth, format, pageflag, pagetime, reqnum, lastid, name, fopenid, type, contenttype);
            
            statusesAPI.shutdownConnection();
/*
--------------------------------------------- 时间线相关测试例 begin----------------------------------------------
*/  
    }

//---------------------------------------------- 以下为测试辅助方法------------------------------------------------

    private static void init(OAuthV2 oAuth) {
        oAuth.setClientId("801115505");
        oAuth.setClientSecret("be1dd1410434a9f7d5a2586bab7a6829");
        oAuth.setRedirectUri("http://www.tencent.com/zh-cn/index.shtml");
//         oAuth.setAccessToken("02204e5e59c2d22b73fb9b688e9f0c02");
//         oAuth.setOpenid("42A2ACA51C28782E854A50C5FA14160A");
//         oAuth.setOpenkey("5651A9639BD8A96BF979AE5DC1D07872");
//         oAuth.setExpiresIn("604800");
    }

    private static void openBrowser(OAuthV2 oAuth) {

        // 合成URL
        String implicitGrantUrl = OAuthV2Client.generateImplicitGrantUrl(oAuth);

        // 调用外部浏览器
        if (!java.awt.Desktop.isDesktopSupported()) {

            System.err.println("Desktop is not supported (fatal)");
            System.exit(1);
        }
        java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
        if (desktop == null || !desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {

            System.err.println("Desktop doesn't support the browse action (fatal)");
            System.exit(1);
        }
        try {
            desktop.browse(new URI(implicitGrantUrl));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("Input the authorization information (eg: access_token=ACCESS_TOKEN&expires_in=60&openid= OPENID &openkey= OPENKEY ) :");
        Scanner in = new Scanner(System.in);
        String responseData = in.nextLine();
        in.close();

        if (OAuthV2Client.parseAccessTokenAndOpenId(responseData, oAuth)) {
            System.out.println("Get Access Token Successfully");
        } else {
            System.out.println("Fail to Get Access Token");
            return;
        }
    }
}
