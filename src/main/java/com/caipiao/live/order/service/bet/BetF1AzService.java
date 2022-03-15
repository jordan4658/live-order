package com.caipiao.live.order.service.bet;

public interface BetF1AzService {

    /**
     * 结算澳洲F1
     *
     * @param issue  期号
     * @param number 开奖号码
     */
    void countAzF1(String issue, String number, int lotteryId) throws Exception;

}
