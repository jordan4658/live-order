package com.caipiao.live.order.rest.impl.order;

import com.caipiao.live.order.rest.LhcHandleLotterySgWriteRest;
import com.caipiao.live.order.service.order.OrderWriteService;
import com.caipiao.live.common.model.common.ResultInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author lzy
 * @create 2018-08-20 14:18
 **/
@RestController
public class LhcHandleLotterySgWriteRestImpl implements LhcHandleLotterySgWriteRest {

    @Autowired
    private OrderWriteService orderWriteService;

    @Override
    public ResultInfo<Boolean> jiesuanByHandle(String issue, String number) {
        return orderWriteService.jiesuanByHandle(issue,number);
    }

    @Override
    public ResultInfo<Boolean> jiesuanByHandleFalse(String issue, String number) {
        return orderWriteService.jiesuanByHandleFalse(issue,number);
    }
}
