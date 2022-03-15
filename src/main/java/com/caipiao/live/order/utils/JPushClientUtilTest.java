package com.caipiao.live.order.utils;

import cn.jiguang.common.ClientConfig;
import cn.jiguang.common.resp.APIConnectionException;
import cn.jiguang.common.resp.APIRequestException;
import cn.jpush.api.JPushClient;
import cn.jpush.api.push.PushResult;
import cn.jpush.api.push.model.Message;
import cn.jpush.api.push.model.Platform;
import cn.jpush.api.push.model.PushPayload;
import cn.jpush.api.push.model.audience.Audience;
import cn.jpush.api.push.model.notification.AndroidNotification;
import cn.jpush.api.push.model.notification.IosAlert;
import cn.jpush.api.push.model.notification.IosNotification;
import cn.jpush.api.push.model.notification.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 测试用！！！！！！！
 *  推送平台的工具类
 */
@Component
public class JPushClientUtilTest {

    private static final Logger log = LoggerFactory.getLogger(JPushClientUtilTest.class);

    private static boolean isInit=false;
    private static List<JPushClient> list=new ArrayList<>();



        // 初始化推送对象
    public static void  getJpushClient(){
        if(!isInit){
            ClientConfig config=ClientConfig.getInstance();
//            config.setApnsProduction(true);  //苹果的设置为生产环境
            JPushClient jpushClient = new JPushClient("962b36466028bbf50521bf2a", "32cba445f5acec33abf2994e", null, config);
            JPushClient jpushClient2 = new JPushClient("0dcedb0c5ece9ce25f78f5dc", "bdc914029a58caaacaf37e4a", null, config);
            list.add(jpushClient2);
            list.add(jpushClient);
            isInit=true;
        }

    }


    /**
     *
     * @param flag:1为推送全部，0：为一个或者部分 ，2：按照标签来推送
     * @param alias：flag=0时，用户的userId数组
     * @param alert：通知内容
     * @param msgType: 消息类型
     * @param title:标题
     */
    public static void sendPush(String flag, String alias[], String alert, String msgType, String title){
        getJpushClient();

        PushPayload payload = buildPushObject_all_alias_alert_info(flag, alias, alert, msgType, title, false);
        for (int i = 0; i <list.size() ; i++) {
            JPushClient tempJpushClient = list.get(i);
            try {
                PushResult result = tempJpushClient.sendPush(payload);
                log.info("Got result - " + result);

            } catch (APIConnectionException e) {
                // Connection error, should retry later
                log.error("Connection error, should retry later", e);
                continue;
            } catch (APIRequestException e) {
                // Should review the error, and fix the request
                log.error("Should review the error, and fix the request", e);
                log.info("HTTP Status: " + e.getStatus());
                log.info("Error Code: " + e.getErrorCode());
                log.info("Error Message: " + e.getErrorMessage());
                continue;
            } catch (Exception e) {
                log.error("sendPush Error ,errorInfo: {"+e+"},alert  :{"+alert+"}");
                continue;
            }
        }
    }

    /**
     *  发送自定义消息
     *   APP通知栏不显示
     * @param msgType: 消息类型 (需和前端协商)
     */
    public static void sendCustomePush(String msgType){
        getJpushClient();

        PushPayload payload = buildPushObject_all_alias_alert_info("1", null, "custome message content", msgType, "custome message", true);
        for (int i = 0; i <list.size() ; i++) {
            JPushClient tempJpushClient = list.get(i);
            try {
                PushResult result = tempJpushClient.sendPush(payload);
                log.info("Got result - " + result);
            } catch (APIConnectionException e) {
                // Connection error, should retry later
                log.error("Connection error, should retry later", e);
                continue;
            } catch (APIRequestException e) {
                // Should review the error, and fix the request
                log.error("Should review the error, and fix the request", e);
                log.info("HTTP Status: " + e.getStatus());
                log.info("Error Code: " + e.getErrorCode());
                log.info("Error Message: " + e.getErrorMessage());
                continue;
            } catch (Exception e) {
                log.error("sendPush Error ,errorInfo: {"+e+"}");
                continue;
            }
        }
    }

    /**
     * 构建推送对象：所有平台，推送目标是别名为 "alias1"，通知内容为 ALERT。
     * @param flag:1为全部推送，0：别名推送
     * @param alias：当flag=0时，推送别名；当flag=2时，推送标签
     * @param alert:推送内容
     * @param msgType: 1：展示窗口，0：不展示窗口
     * @param title:标题
     * @return
     */
    public static PushPayload buildPushObject_all_alias_alert_info(String flag, String [] alias, String alert, String msgType, String title, boolean isCustomMsg) {
        PushPayload.Builder builder=PushPayload.newBuilder().setPlatform(Platform.all());
        if("1".equals(flag)){
            builder.setAudience(Audience.all());
        }else if ("2".equals(flag)){
            builder.setAudience(Audience.tag(alias));
        }else {
            builder.setAudience(Audience.alias(alias));
        }

        //是否是自定义消息
        if (isCustomMsg){
            builder.setMessage(Message.newBuilder()
                    .setMsgContent(alert)
                    .setTitle(title)
                    .addExtra("msgType", msgType)
                    .build());
        }else {
            AndroidNotification androidNotification = AndroidNotification.newBuilder().setTitle(title).setAlert(alert).addExtra("msgType", msgType).build();
            IosAlert iosAlert = IosAlert.newBuilder().setTitleAndBody(title,null,alert).build();
            IosNotification iosNotification=IosNotification.newBuilder().setAlert(iosAlert).addExtra("msgType", msgType).build();

            builder.setNotification(Notification.newBuilder().addPlatformNotification(androidNotification)
                    .addPlatformNotification(iosNotification)
                    .build());
        }

        return builder.build();
    }
}
