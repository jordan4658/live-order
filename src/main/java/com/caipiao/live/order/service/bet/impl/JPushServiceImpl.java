package com.caipiao.live.order.service.bet.impl;
import com.caipiao.live.order.service.bet.JPushService;
import com.caipiao.live.order.utils.JPushClientUtil;
import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.constant.RedisKeys;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class JPushServiceImpl implements JPushService {
    private final String WIN_PUSH = "win_push";
    private final String OPEN_PUSH = "open_push";

    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;
//    @Autowired
//    private JPushInfoMapperExt jPushInfoMapperExt;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void openNmberPush(String lotteryName, String issue, String openNumber) {
        // ONELIVE TODO
//        //开启了推送设置的用户集合
//        List<Integer> userList = jPushInfoMapperExt.getPushSettingOnUser(OPEN_PUSH);
//
//        String pushMsg = String.format("尊敬的用户，%s第%s期已开奖！开奖号码为：%s", lotteryName, issue, openNumber);
//
//        for (Integer user_id : userList) {
//            taskExecutor.execute(() -> {
//                JPushClientUtil.sendPush("0", new String[]{String.valueOf(user_id)}, pushMsg, Constants.MSG_TYPE_OPENPUSH, "开奖通知");
//            });
//        }

    }

    @Override
    @Async
    public void winPush(Integer userId, String lotteryName, String issue) {
        String pushMsg = String.format("恭喜，%s第%s期已中奖！", lotteryName, issue);
//      ONELIVE TODO
//        //查看是否关闭了推送通知
//        Integer count = (Integer) redisTemplate.opsForValue().get(RedisKeys.WIN_PUSH + userId);
//        if (count == null) {
//            count = jPushInfoMapperExt.winPushIsOff(WIN_PUSH, userId);
//            redisTemplate.opsForValue().set(RedisKeys.WIN_PUSH + userId, count, 2, TimeUnit.MINUTES);
//        }
//        if (count == 0) {
//            taskExecutor.execute(() -> {
//                JPushClientUtil.sendPush("0", new String[]{String.valueOf(userId)}, pushMsg, Constants.MSG_TYPE_WINPUSH, "中奖通知");
//            });
//        }
    }
}
