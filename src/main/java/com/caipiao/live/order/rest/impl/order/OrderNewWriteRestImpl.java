package com.caipiao.live.order.rest.impl.order;


import com.caipiao.live.order.rest.OrderNewWriteRest;
import com.caipiao.live.order.service.order.OrderNewWriteService;
import com.caipiao.live.common.model.common.ResultInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class OrderNewWriteRestImpl implements OrderNewWriteRest {

    @Autowired
    private OrderNewWriteService orderWriteService;


    @Override
    public ResultInfo<Boolean> jiesuanOrderBetByIssue(Integer lotteryId, String issue, String openNumber) {
        return orderWriteService.jiesuanOrderBetByIssue(lotteryId,issue,openNumber);
    }

}
