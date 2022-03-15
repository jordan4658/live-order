package com.caipiao.live.order.service.bet;

public interface BetXyftService {

    /**
     * 结算【幸运飞艇猜名称猜前几】
     * @param issue 期号
     * @param number 开奖号码
     */
    void clearingXyftCmcCqj(String issue, String number) throws Exception;

    /**
     * 结算【幸运飞艇单式猜前几】
     * @param issue 期号
     * @param number 开奖号码
     */
    void clearingXyftDsCqj(String issue, String number) throws Exception;

    /**
     * 结算【幸运飞艇定位胆】
     * @param issue 期号
     * @param number 开奖号码
     */
    void clearingXyftDwd(String issue, String number) throws Exception;

    /**
     * 结算【幸运飞艇冠亚和】
     * @param issue 期号
     * @param number 开奖号码
     */
    void clearingXyftGyh(String issue, String number) throws Exception;

    /**
     * 结算【幸运飞艇两面】
     * @param issue 期号
     * @param number 开奖号码
     */
    void clearingXyftLm(String issue, String number) throws Exception;

}
