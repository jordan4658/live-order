package com.caipiao.live.order.service.bet.impl;

import com.caipiao.live.common.service.read.OrderReadRestService;
import com.caipiao.live.order.service.bet.BetSscService;
import com.caipiao.live.order.service.bet.BetSscTxService;
import com.caipiao.live.order.service.lottery.LotteryPlayOddsWriteService;
import com.caipiao.live.order.service.lottery.LotteryPlayWriteService;
import com.caipiao.live.order.service.order.OrderAppendWriteService;
import com.caipiao.live.order.service.order.OrderWriteService;
import com.caipiao.live.order.service.result.TxffcLotterySgWriteService;
import com.caipiao.live.common.mybatis.mapper.OrderBetRecordMapper;
import com.caipiao.live.common.mybatis.mapper.TxffcLotterySgMapper;
import com.caipiao.live.common.mybatis.mapperbean.TxffcBeanMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class BetSscTxServiceImpl implements BetSscTxService {
    private static final Logger logger = LoggerFactory.getLogger(BetSscTxServiceImpl.class);

    // 彩种id【定死-不支持修改】
    private final Integer lotteryId = 3;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private LotteryPlayWriteService lotteryPlayWriteService;
    @Autowired
    private OrderWriteService orderWriteService;
    @Autowired
    private OrderBetRecordMapper orderBetRecordMapper;
    @Autowired
    private TxffcLotterySgMapper txffcLotterySgMapper;
    @Autowired
    private LotteryPlayOddsWriteService lotteryPlayOddsService;
    @Autowired
    private OrderAppendWriteService orderAppendWriteService;
    @Autowired
    private BetSscService betSscService;
    @Autowired
    private TxffcBeanMapper txffcBeanMapper;
    @Autowired
    private OrderReadRestService orderReadRestService;

    @Autowired
    private TxffcLotterySgWriteService txffcLotterySgWriteService;

    private static final String TX_SSC = "TX_SSC-";


}
