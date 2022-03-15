package com.caipiao.live.order.service.order;


import com.caipiao.live.common.mybatis.entity.OrderRecord;

import java.math.BigDecimal;


public interface OrderNewAppendWriteService {

    String createNextIssue(Integer lotteryId, String issue, int count);

    void appendOrder(OrderRecord order, BigDecimal winAmount, Boolean isWin);
}
