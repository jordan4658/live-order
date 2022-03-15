package com.caipiao.live.order.rest;


import com.caipiao.live.common.model.common.ResultInfo;
import org.springframework.web.bind.annotation.RequestParam;


public interface OrderNewWriteRest {



    /**
     * 根据issue,lottery结算订单
     *
     * @param issue      彩种issue
     * @param lotteryId  lotteryId
     * @param openNumber 开奖号码
     * @return
     */
    ResultInfo<Boolean> jiesuanOrderBetByIssue(@RequestParam(value = "lotteryId", required = false) Integer lotteryId, @RequestParam("issue") String issue, @RequestParam("openNumber") String openNumber);


}
