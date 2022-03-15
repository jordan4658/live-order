package com.caipiao.live.order.rest;
import com.caipiao.live.common.model.common.ResultInfo;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;



public interface LhcHandleLotterySgWriteRest {

    /**
     * 六合彩开奖_结算
     * @return
     */
    ResultInfo<Boolean> jiesuanByHandle(@RequestParam("issue") String issue, @RequestParam("number") String number);

    /**
     * 六合彩开奖_假结算
     * @return
     */
    ResultInfo<Boolean> jiesuanByHandleFalse(@RequestParam("issue") String issue, @RequestParam("number") String number);


}
