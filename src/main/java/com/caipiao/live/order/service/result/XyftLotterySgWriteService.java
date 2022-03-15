package com.caipiao.live.order.service.result;

import com.caipiao.live.common.mybatis.entity.FtxyftLotterySg;
import com.caipiao.live.common.mybatis.entity.XyftLotterySg;

public interface XyftLotterySgWriteService {

    /**
     * xyft 获取下一期开奖信息
     */
    XyftLotterySg queryNextSg();

    /**
     * xyft 根据期号获取赛果信息
     *
     * @param issue 期号
     * @return
     */
    XyftLotterySg selectByIssue(String issue);

    /**
     * @Title: cacheIssueResultForXjssc
     * @Description: 把幸运飞艇开奖结果放入缓存
     */
    public void cacheIssueResultForXyft(String issue, String number);

//	/**
//	* @Title: queryNextFtxyftSg
//	* @Description: 获取下一期幸运飞艇番摊开奖信息
//	*/
//	FtxyftLotterySg queryNextFtxyftSg();

//    /**
//     * @Title: selectFtxyftByIssue
//     * @Description: 根据期号获取幸运飞艇番摊赛果信息
//     */
//    FtxyftLotterySg selectFtxyftByIssue(String issue, String number);

//    /**
//     * @Title: cacheIssueResultForFtxyft
//     * @Description: 把幸运飞艇番摊开奖结果放入缓存
//     */
//    public void cacheIssueResultForFtxyft(String issue, String number);

}
