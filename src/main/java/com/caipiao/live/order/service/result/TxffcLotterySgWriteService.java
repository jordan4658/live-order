package com.caipiao.live.order.service.result;


import com.caipiao.live.common.mybatis.entity.TxffcLotterySg;

public interface TxffcLotterySgWriteService {

    /**
     * 获取下一期开奖信息
     *
     * @return
     */
    TxffcLotterySg queryNextSg();

    /**
     * 根据期号查询赛果信息
     *
     * @param issue 期号
     * @return
     */
    TxffcLotterySg selectByIssue(String issue);

}
