package com.caipiao.live.order.service.bet;

public interface BetDzxyftService {

    /**
     * 结算【幸运飞艇猜名称猜前几】
     * @param issue 期号
     * @param number 开奖号码
     */
    void clearingDzxyftCmcCqj(String issue, String number) throws Exception;


    /**
     * 结算【幸运飞艇冠亚和】
     * @param issue 期号
     * @param number 开奖号码
     */
    void clearingDzxyftGyh(String issue, String number) throws Exception;

    /**
     * 结算【幸运飞艇两面】
     * @param issue 期号
     * @param number 开奖号码
     */
    void clearingDzxyftLm(String issue, String number) throws Exception;


}
