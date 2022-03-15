package com.caipiao.live.order.service.bet;

public interface BetActAzService {

    /**
     * 结算澳洲Act
     *
     * @param issue  期号
     * @param number 开奖号码
     */
    void countAzAct(String issue, String number, int lotteryId) throws Exception;

}
