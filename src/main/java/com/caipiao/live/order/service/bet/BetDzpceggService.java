package com.caipiao.live.order.service.bet;

public interface BetDzpceggService {

    /**
     * 结算【PC蛋蛋特码】
     * @param issue 期号
     */
    void clearingDzpceggTm(String issue, String number) throws Exception;

    /**
     * 结算【PC蛋蛋豹子】
     * @param issue 期号
     */
    void clearingDzpceggBz(String issue, String number) throws Exception;

    /**
     * 结算【PC蛋蛋特码包三】
     * @param issue 期号
     */
    void clearingDzpceggTmbs(String issue, String number) throws Exception;

    /**
     * 结算【PC蛋蛋色波】
     * @param issue 期号
     */
    void clearingDzpceggSb(String issue, String number) throws Exception;

    /**
     * 结算【PC蛋蛋混合】
     * @param issue 期号
     */
    void clearingDzpceggHh(String issue, String number) throws Exception;
}
