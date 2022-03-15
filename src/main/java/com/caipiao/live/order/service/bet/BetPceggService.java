package com.caipiao.live.order.service.bet;

public interface BetPceggService {

    /**
     * 结算【PC蛋蛋特码】
     * @param issue 期号
     */
    void clearingPceggTm(String issue, String number) throws Exception;

    /**
     * 结算【PC蛋蛋豹子】
     * @param issue 期号
     */
    void clearingPceggBz(String issue, String number) throws Exception;

    /**
     * 结算【PC蛋蛋特码包三】
     * @param issue 期号
     */
    void clearingPceggTmbs(String issue, String number) throws Exception;

    /**
     * 结算【PC蛋蛋色波】
     * @param issue 期号
     */
    void clearingPceggSb(String issue, String number) throws Exception;

    /**
     * 结算【PC蛋蛋混合】
     * @param issue 期号
     */
    void clearingPceggHh(String issue, String number) throws Exception;
}
