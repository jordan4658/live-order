package com.caipiao.live.order.service.bet;

public interface BetBjpksService {

    /**
     * 结算【北京PK10猜名称猜前几】
     *
     * @param issue 期号
     */
    void clearingBjpksCmcCqj(String issue, String number,int lotteryId) throws Exception;

    /**
     * 结算【北京PK10单式猜前几】
     *
     * @param issue 期号
     */
    void clearingBjpksDsCqj(String issue, String number) throws Exception;

    /**
     * 结算【北京PK10定位胆】
     *
     * @param issue 期号
     */
    void clearingBjpksDwd(String issue, String number) throws Exception;

    /**
     * 结算【北京PK10冠亚和】
     *
     * @param issue 期号
     */
    void clearingBjpksGyh(String issue, String number,int lotteryId) throws Exception;

    /**
     * 结算【北京PK10两面】
     *
     * @param issue 期号
     */
    void clearingBjpksLm(String issue, String number,int lotteryId) throws Exception;
}
