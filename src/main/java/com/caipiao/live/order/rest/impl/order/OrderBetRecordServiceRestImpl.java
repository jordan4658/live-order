package com.caipiao.live.order.rest.impl.order;



import com.caipiao.live.common.service.order.OrderBetRecordService;
import com.caipiao.live.order.rest.OrderBetRecordServiceRest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;


@RestController
public class OrderBetRecordServiceRestImpl implements OrderBetRecordServiceRest {


    @Autowired
    private OrderBetRecordService orderBetRecordService;


    @Override
    public BigDecimal querySumBetAmount(Long roomid) {
        return orderBetRecordService.querySumBetAmount(roomid);
    }

    /**
     * 会员直播间投注金额
     *
     * @param map
     * @return
     */
    @Override
    public double sumBetamountbyUserid(Map map) {
        return orderBetRecordService.sumBetamountbyUserid(map);
    }
}
