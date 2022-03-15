package com.caipiao.live.order.rest;

import com.caipiao.live.common.model.common.ResultInfo;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author ShaoMing
 * @version 1.0.0
 * @date 2018/12/10 18:04
 */

public interface LotterySgWriteRest {

    /**
     * 修改开奖号码
     *
     * @param lotteryId 彩种id
     * @param issue     期号
     * @param number    开奖号码
     * @return
     */
    @PostMapping("/lottery/changeNumber.json")
    ResultInfo<Integer> changeNumber(@RequestParam(value = "lotteryId", required = false) Integer lotteryId, @RequestParam("issue") String issue, @RequestParam("number") String number);


}
