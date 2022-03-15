package com.caipiao.live.order.rest;


import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.Map;

/**
 * @author lucien
 * @create 2020/6/23 21:29
 */

public interface OrderBetRecordServiceRest {

    /**
     * 直播间投注金额
     * @param roomid
     * @return
     */
    BigDecimal querySumBetAmount(@RequestParam("roomid") Long roomid);


    /**
     * 会员直播间投注金额
     * @param map
     * @return
     */
    double sumBetamountbyUserid(@RequestBody Map map);
}
