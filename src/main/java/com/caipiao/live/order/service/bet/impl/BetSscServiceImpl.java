package com.caipiao.live.order.service.bet.impl;

import com.caipiao.live.order.service.bet.BetSscService;
import com.caipiao.live.order.service.bet.JPushService;
import com.caipiao.live.order.service.lottery.LotteryWriteService;
import com.caipiao.live.order.service.order.OrderCommonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class BetSscServiceImpl implements BetSscService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private OrderCommonService orderCommonService;
    @Autowired
    private LotteryWriteService lotteryWriteService;
    @Autowired
    private JPushService jPushService;

    private static final String WITHDRAWAL_KEY = "WITHDRAWAL_KEY";
    private static final String GOD_PUSH_KEY = "GOD_PUSH_ORDER_";

}
