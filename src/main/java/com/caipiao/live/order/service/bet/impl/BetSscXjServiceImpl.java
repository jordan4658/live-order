package com.caipiao.live.order.service.bet.impl;
import com.caipiao.live.order.service.bet.BetSscService;
import com.caipiao.live.order.service.bet.BetSscXjService;
import com.caipiao.live.order.service.lottery.LotteryPlayOddsWriteService;
import com.caipiao.live.order.service.lottery.LotteryPlayWriteService;
import com.caipiao.live.order.service.order.OrderAppendWriteService;
import com.caipiao.live.order.service.order.OrderWriteService;
import com.caipiao.live.order.service.result.XjsscLotterySgWriteService;
import com.caipiao.live.common.mybatis.mapper.OrderBetRecordMapper;
import com.caipiao.live.common.mybatis.mapper.XjsscLotterySgMapper;
import com.caipiao.live.common.mybatis.mapperbean.XjsscBeanMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
@Service
public class BetSscXjServiceImpl implements BetSscXjService {

    private final Integer lotteryId = 2;

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private LotteryPlayWriteService lotteryPlayWriteService;
    @Autowired
    private OrderWriteService orderWriteService;
    @Autowired
    private BetSscService betSscService;
    @Autowired
    private OrderBetRecordMapper orderBetRecordMapper;
    @Autowired
    private XjsscLotterySgMapper xjsscLotterySgMapper;
    @Autowired
    private LotteryPlayOddsWriteService lotteryPlayOddsService;
    @Autowired
    private OrderAppendWriteService orderAppendWriteService;
    @Autowired
    private XjsscBeanMapper xjsscBeanMapper;
    @Autowired
    private XjsscLotterySgWriteService xjsscLotterySgWriteService;

    private static final String XJ_SSC = "XJ_SSC-";


}
