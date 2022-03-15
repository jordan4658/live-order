package com.caipiao.live.order.service.result;

import com.caipiao.live.common.mybatis.entity.*;

public interface XjsscLotterySgWriteService {

    /**
     * xjssc新疆时时彩获取下一期开奖信息
     */
    XjsscLotterySg queryNextSg();

    /**
     * xjssc新疆时时彩根据期号查询赛果信息
     */
    XjsscLotterySg selectByIssue(String issue);

    /**
     * @Title: cacheIssueResult
     * @Description: 把开奖结果放入缓存
     */
    public void cacheIssueResult(String issue, String number);

    /**
     * @Title: queryTjsscNextSg
     * @Description: Tjssc 天津时时彩 获取当前开奖数据
     */
    TjsscLotterySg queryTjsscNextSg();

    /**
     * @Title: selectXjsscByIssue
     * @Description: Tjssc 天津时时彩 获取下期数据
     */
    TjsscLotterySg selectXjsscByIssue(String issue);

    /**
     * @Title: cacheIssueResultForXjssc
     * @Description: 把天津时时彩开奖结果放入缓存
     */
    public void cacheIssueResultForXjssc(String issue, String number);

    /**
     * @Title: queryTensscNextSg
     * @Description: Tenssc 10分时时彩 获取当前开奖数据
     */
    TensscLotterySg queryTensscNextSg();

    /**
     * @Title: selectTensscByIssue
     * @Description: Tenssc 10分时时彩获取下期数据
     */
    TensscLotterySg selectTensscByIssue(String issue, String number);

    /**
     * @Title: cacheIssueResultForTenssc
     * @Description: 把10分时时彩开奖结果放入缓存
     */
    public void cacheIssueResultForTenssc(String issue, String number);

    /**
     * @Title: queryFivesscNextSg
     * @Description: Fivessc 5分时时彩 获取当前开奖数据
     */
    FivesscLotterySg queryFivesscNextSg();

    /**
     * @Title: selectFivesscByIssue
     * @Description: Fivessc 5分时时彩获取下期数据
     */
    FivesscLotterySg selectFivesscByIssue(String issue);

    /**
     * @Title: cacheIssueResultForFivessc
     * @Description: 把5分时时彩开奖结果放入缓存
     */
    public void cacheIssueResultForFivessc(String issue, String number);

    /**
     * @Title: queryJssscNextSg
     * @Description: Jsssc 德州时时彩 获取当前开奖数据
     */
    JssscLotterySg queryJssscNextSg();

    /**
     * @Title: selectJssscByIssue
     * @Description: Jsssc 德州时时彩获取下期数据
     */
    JssscLotterySg selectJssscByIssue(String issue);

    /**
     * @Title: cacheIssueResultForFivessc
     * @Description: 把德州时时彩开奖结果放入缓存
     */
    public void cacheIssueResultForJsssc(String issue, String number);

//	/**
//	 * @Title: queryJssscNextSg
//	 * @Description: ftJsssc 德州时时彩番摊 获取当前开奖数据
//	 */
//	FtjssscLotterySg queryFtjssscNextSg();
//
//	/**
//	 * @Title: selectJssscByIssue
//	 * @Description: ftJsssc 德州时时彩番摊获取下期数据
//	 */
//	FtjssscLotterySg selectftJssscByIssue(String issue, String number);

//    /**
//     * @Title: cacheIssueResultForFivessc
//     * @Description: 把德州时时彩番摊开奖结果放入缓存
//     */
//    public void cacheIssueResultForFtjsssc(String issue, String number);

    /**
     * @Title: queryCqsscNextSg
     * @Description: Cqssc 重庆时时彩 获取当前开奖数据
     */
    CqsscLotterySg queryCqsscNextSg();

    /**
     * @Title: selectJssscByIssue
     * @Description: Cqssc 重庆时时彩获取下期数据
     */
    CqsscLotterySg selectCqsscByIssue(String issue);

    /**
     * @Title: cacheIssueResultForCqssc
     * @Description: 把重庆时时彩开奖结果放入缓存
     */
    public void cacheIssueResultForCqssc(String issue, String number);

    TxffcLotterySg queryTxffcNextSg();

    TxffcLotterySg selectTxffcByIssue(String issue);

    public void cacheIssueResultForTxffc(String issue, String number);

}
