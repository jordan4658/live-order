package com.caipiao.live.order.service.bet;

public interface HkLhcSettlementService {

    /**
     * 结算【香港六合彩特码特码A】
     * @param issue 期号
     * @param number 开奖号码
     * @param jiesuanOrNot  是否真实结算
     */
    void clearingHkLhcTeMaA(String issue, String number,int lotteryId,boolean jiesuanOrNot) throws Exception;

    /**
     * 结算【香港六合彩正码正码A】
     * @param issue 期号
     * @param number 开奖号码
     */
    void clearingHkLhcZhengMaA(String issue, String number,int lotteryId,boolean jiesuanOrNot) throws Exception;

    /**
     * 结算【香港六合彩正特】
     * @param issue 期号
     * @param openNumber 开奖号码
     */
	void clearingHkLhcZhengTe(String issue, String openNumber, int parseInt, boolean b);
	
	/**
     * 结算【香港六合彩正码1-6】
     * @param issue 期号
     * @param openNumber 开奖号码
     */
	void clearingHkLhcZhengMaOneToSix(String issue, String openNumber, int parseInt, boolean b);

    /**
     * 结算【香港六合彩六肖】
     * @param issue 期号
     * @param openNumber 开奖号码
     */
	void clearingHkLhcLiuXiao(String issue, String openNumber, int parseInt, boolean b);

    /**
     * 结算【香港六合彩半波】
     * @param issue 期号
     * @param openNumber 开奖号码
     */
	void clearingHkLhcBanBo(String issue, String openNumber, int parseInt, boolean b);

    /**
     * 结算【香港六合彩尾数】
     * @param issue 期号
     * @param openNumber 开奖号码
     */
	void clearingHkLhcWs(String issue, String openNumber, int parseInt, boolean b);

    /**
     * 结算【香港六合彩连码】
     * @param issue 期号
     * @param openNumber 开奖号码
     */
	void clearingHkLhcLianMa(String issue, String openNumber, int parseInt, boolean b);

    /**
     * 结算【香港六合彩连肖】
     * @param issue 期号
     * @param openNumber 开奖号码
     */
	void clearingHkLhcLianXiao(String issue, String openNumber, int parseInt, boolean b);

    /**
     * 结算【香港六合彩连尾中】
     * @param issue 期号
     * @param openNumber 开奖号码
     */
	void clearingHkLhcLianWei(String issue, String openNumber, int parseInt, boolean b);

    /**
     * 结算【香港六合彩不中】
     * @param issue 期号
     * @param openNumber 开奖号码
     */
	void clearingHkLhcNoOpen(String issue, String openNumber, int parseInt, boolean b);

    /**
     * 结算【六合彩1-6龙虎】
     * @param issue 期号
     * @param openNumber 开奖号码
     */
	void clearingHkLhcOneSixLh(String issue, String openNumber, int parseInt, boolean b);

    /**
     * 结算【香港六合彩五行】
     * @param issue 期号
     * @param openNumber 开奖号码
     */
	void clearingHkLhcWuxing(String issue, String openNumber, int parseInt, boolean b);

    /**
     * 结算【香港六合彩平特平特】
     * @param issue 期号
     * @param openNumber 开奖号码
     */
	void clearingHkLhcPtPt(String issue, String openNumber, int parseInt, boolean b);

    /**
     * 结算【香港六合彩特肖特肖】
     * @param issue 期号
     * @param openNumber 开奖号码
     */
	void clearingHkLhcTxTx(String issue, String openNumber, int parseInt, boolean b);


}
