package com.caipiao.live.order.service.bet;

import com.caipiao.live.common.model.common.ResultInfo;
import com.caipiao.live.order.model.dto.OrderDTO;

public interface OrderBetService {

    ResultInfo bettingInformationVerification(OrderDTO data, Long startTime, Integer start);

}
