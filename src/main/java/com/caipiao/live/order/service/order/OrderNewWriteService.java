package com.caipiao.live.order.service.order;


import com.caipiao.live.common.model.common.ResultInfo;
import com.caipiao.live.common.mybatis.entity.OrderAppendRecord;
import com.caipiao.live.common.mybatis.entity.OrderBetRecord;

import java.util.List;

public interface OrderNewWriteService {

    /**
     * 手动结算
     *
     * @param lotteryId
     * @param issue
     * @param openNumber 开奖号码
     * @return
     */
    ResultInfo<Boolean> jiesuanOrderBetByIssue(Integer lotteryId, String issue, String openNumber);


    /**
     * 生成追号单
     *
     * @param orderAppendRecord 追号信息
     * @param source            来源
     * @return
     */
    ResultInfo<Boolean> orderAppend(OrderAppendRecord orderAppendRecord, String source);

    int updateOrderRecord(String lotteryId, String issue, String sgnumber);

    int countOrderBetList(String issue, List<Integer> playIds, String lotteryId, String status);

    List<OrderBetRecord> selectOrderBetList(String issue, String lotteryId, List<Integer> playIds, String status, String type);

}
