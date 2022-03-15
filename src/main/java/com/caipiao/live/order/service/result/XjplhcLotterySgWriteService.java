package com.caipiao.live.order.service.result;

import com.caipiao.live.common.mybatis.entity.XjplhcLotterySg;

/**
 * @Date:Created in 10:362020/1/1
 * @Descriotion
 * @Author
 **/
public interface XjplhcLotterySgWriteService {

    /**
     * 新加坡六合彩 获取下一期开奖信息
     */
    XjplhcLotterySg queryNextSg();

    /**
     * 新加坡六合彩 根据期号获取赛果信息
     *
     * @param issue 期号
     * @return
     */
    XjplhcLotterySg selectByIssue(String issue);

    /**
     * @Title: cacheIssueResultForXjssc
     * @Description: 把新加坡六合彩开奖结果放入缓存
     */
    public void cacheIssueResultForQnelhc(String issue, String number);

}
