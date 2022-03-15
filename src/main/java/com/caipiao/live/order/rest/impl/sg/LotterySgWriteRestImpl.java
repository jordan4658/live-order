package com.caipiao.live.order.rest.impl.sg;


import com.caipiao.live.common.model.common.ResultInfo;

import com.caipiao.live.order.rest.LotterySgWriteRest;
import com.caipiao.live.order.service.lottery.LotterySgWriteService;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * @author ShaoMing
 * @version 1.0.0
 * @date 2018/12/10 18:08
 */

public class LotterySgWriteRestImpl implements LotterySgWriteRest {

    @Autowired
    private LotterySgWriteService lotterySgWriteService;

    @Override
    public ResultInfo<Integer> changeNumber(Integer lotteryId, String issue, String number) {
        return lotterySgWriteService.changeNumber(lotteryId, issue, number);
    }

}
