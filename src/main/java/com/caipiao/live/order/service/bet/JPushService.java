package com.caipiao.live.order.service.bet;

public interface JPushService {
    /**
     * 开奖号码推送
     */
    void openNmberPush(String lotteryName, String issue, String openNumber);

    /**
     * 中奖推送
     */
    void winPush(Integer userId, String lotteryName, String issue);
}
