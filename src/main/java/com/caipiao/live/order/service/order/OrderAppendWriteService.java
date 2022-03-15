package com.caipiao.live.order.service.order;
import com.caipiao.live.common.model.common.ResultInfo;
import com.caipiao.live.common.model.dto.order.AppendDTO;
import com.caipiao.live.common.model.dto.order.OrderAppendDTO;
import com.caipiao.live.common.model.dto.order.OrderBetDTO;
import com.caipiao.live.common.model.dto.order.OrderPlayDTO;
import com.caipiao.live.common.mybatis.entity.OrderRecord;

import java.math.BigDecimal;
import java.util.List;

public interface OrderAppendWriteService {

    ResultInfo<List<OrderPlayDTO>> orderAppendPlan(AppendDTO appendDTO);

    ResultInfo<Boolean> orderAppend(AppendDTO appendDTO);

    String createNextIssue(Integer lotteryId, String issue, int count);

    ResultInfo<List<OrderAppendDTO>> orderAppendList(OrderBetDTO data);

    void appendOrder(OrderRecord order, BigDecimal winAmount, Boolean isWin);
}
