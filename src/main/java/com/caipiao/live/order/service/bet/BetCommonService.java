package com.caipiao.live.order.service.bet;

import com.caipiao.live.common.mybatis.entity.OrderBetRecord;
import com.caipiao.live.common.mybatis.entity.OrderRecord;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface BetCommonService {

    /**
     * 输赢结算的处理方式(不处理打和的情况)
     * @param orderBet 投注单
     * @param winAmount 中奖金额
     * @param userId 会员id
     * @param orderSn 投注单号
     */
    void winOrLose(OrderBetRecord orderBet, BigDecimal winAmount, Integer userId, String orderSn);

    void updateOrder(String number, List<OrderRecord> orderRecords, List<Integer> orderIds, Map<Integer, OrderRecord> orderMap);

    /**
     * 打和的处理方式
     * @param orderMap
     * @param orderBetRecords
     */
    void noWinOrLose(Map<Integer, OrderRecord> orderMap, List<OrderBetRecord> orderBetRecords);

    /**
     * 获取赔率因子
     * @param lotteryId 彩种id
     * @return
     */
    double getDivisor(Integer lotteryId);

    /**
     * 修改用户余额信息
     * @param orderBet 投注单
     * @param winAmount 中奖金额
     * @param userId 会员id
     * @param orderSn 投注单号
     */
    void updateMemberBalance(OrderBetRecord orderBet, BigDecimal winAmount, Integer userId, String orderSn);


    void noWinOrLose(List<OrderBetRecord> orderBetRecords);
}
