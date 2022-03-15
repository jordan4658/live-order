package com.caipiao.live.order.service.bet.impl;

import com.caipiao.live.common.service.read.OrderReadRestService;
import com.caipiao.live.order.service.bet.BetSscCqService;
import com.caipiao.live.order.service.bet.BetSscService;
import com.caipiao.live.order.service.lottery.LotteryPlayOddsWriteService;
import com.caipiao.live.order.service.lottery.LotteryPlayWriteService;
import com.caipiao.live.order.service.order.OrderAppendWriteService;
import com.caipiao.live.order.service.order.OrderWriteService;
import com.caipiao.live.order.service.result.CqsscLotterySgWriteService;
import com.caipiao.live.common.mybatis.mapper.CqsscLotterySgMapper;
import com.caipiao.live.common.mybatis.mapper.OrderBetRecordMapper;
import com.caipiao.live.common.mybatis.mapperbean.CqsscBeanMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class BetSscCqServiceImpl implements BetSscCqService {

    private final Integer lotteryId = 1;

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private LotteryPlayWriteService lotteryPlayWriteService;
    @Autowired
    private OrderWriteService orderWriteService;
    @Autowired
    private OrderBetRecordMapper orderBetRecordMapper;
    @Autowired
    private CqsscLotterySgMapper cqsscLotterySgMapper;
    @Autowired
    private LotteryPlayOddsWriteService lotteryPlayOddsService;
    @Autowired
    private OrderAppendWriteService orderAppendWriteService;
    @Autowired
    private BetSscService betSscService;
    @Autowired
    private CqsscLotterySgWriteService cqsscLotterySgWriteService;
    @Autowired
    private CqsscBeanMapper cqsscBeanMapper;
    @Autowired
    private OrderReadRestService orderReadRestService;
    private static final String CQ_SSC = "CQ_SSC-";

}
