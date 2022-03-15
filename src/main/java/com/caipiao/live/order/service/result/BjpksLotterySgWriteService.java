package com.caipiao.live.order.service.result;

import com.caipiao.live.common.mybatis.entity.*;

public interface BjpksLotterySgWriteService {

    /**
     * 从第三方接口获取赛果并添加到数据库
     */
    void addSg();

    /**
     * 获取下一期开奖信息
     */
    BjpksLotterySg queryNextSg();

    /**
     * 根据期号查询赛果信息
     */
    BjpksLotterySg selectByIssue(String issue);

    /**
     * @Title: cacheIssueResultForXjssc
     * @Description: 把北京PK10开奖结果放入缓存
     */
    void cacheIssueResultForBjpks(String issue, String number);

    /**
     * @Title: queryTenbjpksNextSg
     * @Description: 10分PK10 获取下一期开奖信息
     */
    TenbjpksLotterySg queryTenbjpksNextSg();

    /**
     * @Title: cacheIssueResultForTenbjpks
     * @Description: 把10分PK10开奖结果放入缓存
     */
    public void cacheIssueResultForTenbjpks(String issue, String number);

    /**
     * @Title: selectFivebjpksByIssue
     * @Description: 5分PK10   根据期号查询赛果信息
     */
    FivebjpksLotterySg selectFivebjpksByIssue(String issue);

    /**
     * @Title: cacheIssueResultForFivebjpks
     * @Description: 把5分PK10开奖结果放入缓存
     */
    void cacheIssueResultForFivebjpks(String issue, String number);

    /**
     * @Title: queryJsbjpksNextSg
     * @Description: 德州PK10 获取下一期开奖信息
     */
    JsbjpksLotterySg queryJsbjpksNextSg();

    /**
     * @Title: selectJsbjpksByIssue
     * @Description: 德州PK10   根据期号查询赛果信息
     */
    JsbjpksLotterySg selectJsbjpksByIssue(String issue);

    /**
     * @Title: cacheIssueResultForJsbjpks
     * @Description: 把德州PK10开奖结果放入缓存
     */
    void cacheIssueResultForJsbjpks(String issue, String number);

//	FtjspksLotterySg queryFtjsbjpksNextSg();
//
//	FtjspksLotterySg selectFtjspksByIssue(String issue, String number);
//
//	void cacheIssueResultForFtjspks(String issue, String number);
}
