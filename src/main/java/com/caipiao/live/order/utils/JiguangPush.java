package com.caipiao.live.order.utils;

import cn.jiguang.common.ClientConfig;
import cn.jiguang.common.resp.APIConnectionException;
import cn.jiguang.common.resp.APIRequestException;
import cn.jpush.api.JPushClient;
import cn.jpush.api.push.PushResult;
import cn.jpush.api.push.model.Options;
import cn.jpush.api.push.model.Platform;
import cn.jpush.api.push.model.PushPayload;
import cn.jpush.api.push.model.audience.Audience;
import cn.jpush.api.push.model.notification.AndroidNotification;
import cn.jpush.api.push.model.notification.IosNotification;
import cn.jpush.api.push.model.notification.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author: Song
 * @Description:极光推送
 * @Version: 1.0.0
 * @Date; 2017-12-21 14:54
 *
 * 废弃
 * 用JPushClientUtil工具类
 */
public class JiguangPush {

    //日志输出对象
    private static final Logger logger = LoggerFactory.getLogger(JiguangPush.class);

    //在极光注册上传应用的 appKey 和 masterSecret
    private static String APP_KEY = "";
    private static String MASTER_SECRET = "";

    // 区分线上环境和测试环境
    private static boolean REAL_TAG = true;

    /**
     * 保存离线的时长。秒为单位。最多支持10天（864000秒）。
     * 0 表示该消息不保存离线。即：用户在线马上发出，当前不在线用户将不会收到此消息。
     * 此参数不设置则表示默认，默认为保存1天的离线消息（86400秒
     */
    private static long timeToLive =  60 * 60 * 24;

    /**
     * 极光推送
     * @param alias 别名
     * @param data  扩展字段
     * @param alert  推送消息内容
     */
    public static void jiguangPush(String alias,String data,String alert){
        logger.info("对别名" + alias + "的用户推送信息");
        PushResult result = push(alias,alert,data);
        if(result != null && result.isResultOK()){
            logger.info("针对别名" + alias + "的信息推送成功！");
        }else{
            logger.info("针对别名" + alias + "的信息推送失败！");
        }
    }

    /**
     * 生成极光推送对象PushPayload（采用java SDK）
     * @param alias 别名(推送消息的对象)
     * @param alert	通知内容
     * @param data	扩展字段(这里自定义 JSON 格式的 Key/Value 信息，以供业务使用)
     * @return PushPayload
     */
    public static PushPayload buildPushObject_android_ios_alias_alert(String alias, String alert, String data){
        return PushPayload.newBuilder()
                .setPlatform(Platform.android_ios())//platform:推送平台
                .setAudience(Audience.alias(alias))//audience:推送设备指定
                .setNotification(Notification.newBuilder()//notification:通知内容体。是被推送到客户端的内容。
                        .addPlatformNotification(AndroidNotification.newBuilder()
                                .addExtra("data", data)//扩展字段(可传自定义对象的json字符串)
                                .setAlert(alert)//通知内容
                                .build())
                        .addPlatformNotification(IosNotification.newBuilder()
                                .addExtra("data", data)
                                .setAlert(alert)
                                .build())
                        .build())
                .setOptions(Options.newBuilder()
                        .setApnsProduction(true)//true-推送生产环境 false-推送开发环境
                        .setTimeToLive(timeToLive)//消息在JPush服务器的失效时间
                        .build())
                .build();
    }

    /**
     * 极光推送方法(采用java SDK)
     * @param alias 别名
     * @param alert 通知内容
     * @return PushResult
     */
    public static PushResult push(String alias,String alert,String data){
        JPushClient jpushClient = new JPushClient(MASTER_SECRET, APP_KEY, null, ClientConfig.getInstance());
        PushPayload payload = buildPushObject_android_ios_alias_alert(alias,alert,data);
        try {
            return jpushClient.sendPush(payload);
        } catch (APIConnectionException e) {
            logger.error("Connection error. Should retry later. ", e);
            return null;
        } catch (APIRequestException e) {
            logger.error("Error response from JPush server. Should review and fix it. ", e);
            logger.info("HTTP Status: " + e.getStatus());
            logger.info("Error Code: " + e.getErrorCode());
            logger.info("Error Message: " + e.getErrorMessage());
            logger.info("Msg ID: " + e.getMsgId());
            return null;
        }
    }


    /**
     * 广播 (所有平台，所有设备, 不支持附加信息)
     * @author YangJie [2016年6月17日 下午4:12:08]
     * @param pushBean 推送内容
     * @return
     */
    public static boolean pushAll(PushBean pushBean){
        return sendPush(PushPayload.newBuilder()
                .setPlatform(Platform.all())
                .setAudience(Audience.all())
                .setNotification(Notification.alert(pushBean.getAlert()))
                .setOptions(Options.newBuilder().setApnsProduction(REAL_TAG).build())
                .build());
    }

    /**
     * ios广播
     * @param pushBean 推送内容
     * @return
     */
    public static boolean pushIos(PushBean pushBean){
        return sendPush(PushPayload.newBuilder()
                .setPlatform(Platform.ios())
                .setAudience(Audience.all())
                .setNotification(Notification.ios(pushBean.getAlert(), pushBean.getExtras()))
                .setOptions(Options.newBuilder().setApnsProduction(REAL_TAG).build())
                .build());
    }

    /**
     * ios通过registid推送 (一次推送最多 1000 个)
     * @param pushBean 推送内容
     * @param registids 推送id
     * @return
     */
    public static boolean pushIos(PushBean pushBean, String ... registids){
        return sendPush(PushPayload.newBuilder()
                .setPlatform(Platform.ios())
                .setAudience(Audience.registrationId(registids))
                .setNotification(Notification.ios(pushBean.getAlert(), pushBean.getExtras()))
                .setOptions(Options.newBuilder().setApnsProduction(REAL_TAG).build())
                .build());
    }


    /**
     * android广播
     * @param pushBean 推送内容
     * @return
     */
    public static boolean pushAndroid(PushBean pushBean){
        return sendPush(PushPayload.newBuilder()
                .setPlatform(Platform.android())
                .setAudience(Audience.all())
                .setNotification(Notification.android(pushBean.getAlert(), pushBean.getTitle(), pushBean.getExtras()))
                .setOptions(Options.newBuilder().setApnsProduction(REAL_TAG).build())
                .build());
    }

    /**
     * android通过registid推送 (一次推送最多 1000 个)
     * @param pushBean 推送内容
     * @param registids 推送id
     * @return
     */
    public static boolean pushAndroid(PushBean pushBean, String ... registids){
        return sendPush(PushPayload.newBuilder()
                .setPlatform(Platform.android())
                .setAudience(Audience.registrationId(registids))
                .setNotification(Notification.android(pushBean.getAlert(), pushBean.getTitle(), pushBean.getExtras()))
                .setOptions(Options.newBuilder().setApnsProduction(REAL_TAG).build())
                .build());
    }

    /**
     * 调用api推送
     * @param pushPayload 推送实体
     * @return
     */
    private static boolean sendPush(PushPayload pushPayload){
        logger.info("发送极光推送请求: {}", pushPayload);
        JPushClient jpushClient = new JPushClient(MASTER_SECRET, APP_KEY, null, ClientConfig.getInstance());
        PushResult result = null;
        try{
            result = jpushClient.sendPush(pushPayload);
        } catch (APIConnectionException e) {
            logger.error("极光推送连接异常: ", e);
        } catch (APIRequestException e) {
            logger.error("极光推送请求异常: ", e);
        }
        if (result!=null && result.isResultOK()) {
            logger.info("极光推送请求成功: {}", result);
            return true;
        }else {
            logger.info("极光推送请求失败: {}", result);
            return false;
        }
    }


}
