package com.caipiao.live.order.service.bet;
import com.caipiao.live.common.mybatis.entity.AmlhcLotterySg;
import com.caipiao.live.common.mybatis.entity.FivelhcLotterySg;
public interface BetLhcService {

    /**
     * 结算【六合彩特码特码A】
     * @param issue 期号
     * @param number 开奖号码
     * @param jiesuanOrNot  是否真实结算
     */
    void clearingLhcTeMaA(String issue, String number,int lotteryId,boolean jiesuanOrNot) throws Exception;

    /**
     * 结算【六合彩正码正码A】
     * @param issue 期号
     * @param number 开奖号码
     */
    void clearingLhcZhengMaA(String issue, String number,int lotteryId,boolean jiesuanOrNot) throws Exception;

    /**
     * 结算【六合彩正码1-6】
     * @param issue 期号
     * @param number 开奖号码
     */
    void clearingLhcZhengMaOneToSix(String issue, String number,int lotteryId,boolean jiesuanOrNot) throws Exception;

    /**
     * 结算【六合彩正特】
     * @param issue 期号
     * @param number 开奖号码
     */
    void clearingLhcZhengTe(String issue, String number,int lotteryId,boolean jiesuanOrNot) throws Exception;

    /**
     * 结算【六合彩连码】
     * @param issue 期号
     * @param number 开奖号码
     */
    void clearingLhcLianMa(String issue, String number,int lotteryId,boolean jiesuanOrNot) throws Exception;

    /**
     * 结算【六合彩半波】
     * @param issue 期号
     * @param number 开奖号码
     */
    void clearingLhcBanBo(String issue, String number,int lotteryId,boolean jiesuanOrNot) throws Exception;

    /**
     * 结算【六合彩尾数】
     * @param issue 期号
     * @param number 开奖号码
     */
    void clearingLhcWs(String issue, String number,int lotteryId,boolean jiesuanOrNot) throws Exception;


    /**
     * 结算【六合彩不中】
     * @param issue 期号
     * @param number 开奖号码
     */
    void clearingLhcNoOpen(String issue, String number,int lotteryId,boolean jiesuanOrNot) throws Exception;

    /**
     * 结算【六合彩平特平特】
     * @param issue 期号
     * @param number 开奖号码
     */
    void clearingLhcPtPt(String issue, String number,int lotteryId,boolean jiesuanOrNot) throws Exception;

    /**
     * 结算【六合彩特肖特肖】
     * @param issue 期号
     * @param number 开奖号码
     */
    void clearingLhcTxTx(String issue, String number,int lotteryId,boolean jiesuanOrNot) throws Exception;

    /**
     * 结算【六合彩六肖】
     * @param issue 期号
     * @param number 开奖号码
     */
    void clearingLhcLiuXiao(String issue, String number,int lotteryId,boolean jiesuanOrNot) throws Exception;

    /**
     * 结算【六合彩连肖】
     * @param issue 期号
     * @param number 开奖号码
     */
    void clearingLhcLianXiao(String issue, String number,int lotteryId,boolean jiesuanOrNot) throws Exception;

    /**
     * 结算【六合彩连尾中】
     * @param issue 期号
     * @param number 开奖号码
     */
    void clearingLhcLianWei(String issue, String number,int lotteryId,boolean jiesuanOrNot) throws Exception;

    /**
     * 结算【六合彩1-6龙虎】
     * @param issue 期号
     * @param number 开奖号码
     */
    void clearingLhcOneSixLh(String issue, String number,int lotteryId,boolean jiesuanOrNot) throws Exception;

    /**
     * 结算【六合彩五行】
     * @param issue 期号
     * @param number 开奖号码
     */
    void clearingLhcWuxing(String issue, String number,int lotteryId,boolean jiesuanOrNot) throws Exception;
    
    /** 
	* @Title: queryNextSslhcSg 
	* @Description: 时时六合彩开奖信息
	*/ 
    AmlhcLotterySg queryNextAmlhcSg();
    
    /** 
	* @Title: selectSslhcByIssue 
	* @Description: 根据期号获取时时六合彩赛果信息
	*/ 
    AmlhcLotterySg selectAmlhcByIssue(String issue);
    
    /** 
	* @Title: cacheIssueResultForSslhc 
	* @Description: 把时时六合彩开奖结果放入缓存
	*/ 
	public void cacheIssueResultForSslhc(String issue, String number);
	
	/** 
	* @Title: queryNextFivelhcSg 
	* @Description: 五分六合彩开奖信息
	*/ 
	FivelhcLotterySg queryNextFivelhcSg();
	
	/**
	 * @Title: selectFivelhcByIssue
	 * @Description: 根据期号获取五分六合彩赛果信息
	 */
	FivelhcLotterySg selectFivelhcByIssue(String issue, String number);
	
	/** 
	* @Title: cacheIssueResultForFivelhc 
	* @Description: 把五分六合彩开奖结果放入缓存
	*/ 
	public void cacheIssueResultForFivelhc(String issue, String number);
}
