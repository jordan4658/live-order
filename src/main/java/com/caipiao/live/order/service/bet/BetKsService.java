package com.caipiao.live.order.service.bet;

/**
 * @Date:Created in 22:112019/12/5
 * @Descriotion
 * @Author
 **/
public interface BetKsService {
    /**
     * 结算【澳洲快三两面】
     *
     * @param issue 期号
     */
    void clearingKsLm(String issue, String number, int lotteryId);

    void clearingKsDd(String issue, String number, int lotteryId);

    void clearingKsLh(String issue, String number, int lotteryId);

    void clearingKsSbTh(String issue, String number, int lotteryId);

    void clearingKsEbTh(String issue, String number, int lotteryId);
}
