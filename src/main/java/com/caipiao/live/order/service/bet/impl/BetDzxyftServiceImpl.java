package com.caipiao.live.order.service.bet.impl;


import com.caipiao.live.common.service.read.OrderReadRestService;
import com.caipiao.live.order.service.bet.BetCommonService;
import com.caipiao.live.order.service.bet.BetDzxyftService;
import com.caipiao.live.order.service.lottery.LotteryPlayOddsWriteService;
import com.caipiao.live.order.service.lottery.LotteryPlayWriteService;
import com.caipiao.live.order.service.order.OrderNewAppendWriteService;
import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.model.dto.order.OrderBetStatus;
import com.caipiao.live.common.model.dto.order.OrderStatus;
import com.caipiao.live.common.mybatis.entity.LotteryPlay;
import com.caipiao.live.common.mybatis.entity.LotteryPlayOdds;
import com.caipiao.live.common.mybatis.entity.OrderBetRecord;
import com.caipiao.live.common.mybatis.entity.OrderRecord;
import com.caipiao.live.common.mybatis.mapper.DzxyftLotterySgMapper;
import com.caipiao.live.common.mybatis.mapper.OrderRecordMapper;
import com.caipiao.live.common.util.lottery.XyftUtils;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author lzy
 * @create 2018-09-15 17:07
 **/
@Service
public class BetDzxyftServiceImpl implements BetDzxyftService {
    private static Logger logger = LoggerFactory.getLogger(BetDzxyftServiceImpl.class);
    private final Integer lotteryId = 1402;

    @Autowired
    private OrderRecordMapper orderRecordMapper;
    @Autowired
    private OrderReadRestService orderReadRestService;
    @Autowired
    private LotteryPlayWriteService lotteryPlayWriteService;
    @Autowired
    private DzxyftLotterySgMapper dzxyftLotterySgMapper;
    @Autowired
    private LotteryPlayOddsWriteService lotteryPlayOddsService;
    @Autowired
    private BetCommonService betCommonService;
    @Autowired
    private OrderNewAppendWriteService orderNewAppendWriteService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String ORDER_KEY = "DZXYFT_ORDER_";
    private static final String DZXYFT_LOTTERY_SG = "DZXYFT_LOTTERY_SG_";

    // PK10两面
    private final int PLAY_ID_LM = 140201;
    // PK10冠亚和
    private final int PLAY_ID_GYH = 140202;

    // PK10 1-5名
    private final int PLAY_ID_15 = 140203;
    // PK10 6-10名
    private final int PLAY_ID_610 = 140204;
    // PK10 第一名
    private final int PLAY_ID_1 = 140205;
    // PK10 第二名
    private final int PLAY_ID_2 = 140206;
    // PK10 第三名
    private final int PLAY_ID_3 = 140207;
    // PK10 第四名
    private final int PLAY_ID_4 = 140208;
    // PK10 第五名
    private final int PLAY_ID_5 = 140209;
    // PK10 第六名
    private final int PLAY_ID_6 = 140210;
    // PK10 第七名
    private final int PLAY_ID_7 = 140211;
    // PK10 第八名
    private final int PLAY_ID_8 = 140212;
    // PK10 第九名
    private final int PLAY_ID_9 = 140213;
    // PK10 第十名
    private final int PLAY_ID_10 = 140214;


    private final List<Integer> PLAY_IDS_110 = Lists.newArrayList(140203, 140204, 140205, 140206, 140207, 140208, 140209, 140210, 140211, 140212, 140213, 140214);

    @Override
    public void clearingDzxyftCmcCqj(String issue, String number) throws Exception {
        // clearingDzxyftCqj(issue, number, PLAY_IDS_CMC_CQJ);
        clearingDzxyftCqj(issue, number, PLAY_IDS_110);
    }


    @Override
    public void clearingDzxyftGyh(String issue, String number) throws Exception {
        clearingDzxyftOnePlayManyOdds(issue, number, PLAY_ID_GYH);
    }

    @Override
    public void clearingDzxyftLm(String issue, String number) throws Exception {
        clearingDzxyftOnePlayManyOdds(issue, number, PLAY_ID_LM);
    }

    private void clearingDzxyftCqj(String issue, String number, List<Integer> playIds) {
        List<OrderRecord> orderRecords = getOrderRecord(issue);
        if (CollectionUtils.isEmpty(orderRecords)) {
            return;
        }
        Map<Integer, OrderRecord> orderMap = new HashMap<>();
        // 获取相关订单id集合
        List<Integer> orderIds = new ArrayList<>();
        this.updateOrder(number, orderRecords, orderIds, orderMap);
        // 获取赔率因子
        double divisor = betCommonService.getDivisor(lotteryId);

        // 查询所有所有相关投注信息
        List<OrderBetRecord> orderBetRecords = orderReadRestService.selectOrderBets(orderIds, playIds, OrderBetStatus.WAIT);
        // 判空处理
        if (CollectionUtils.isEmpty(orderBetRecords)) {
            return;
        }
        // 获取相关玩法信息
        Map<Integer, LotteryPlay> playMap = lotteryPlayWriteService.selectPlayByIds(playIds);
        // 获取所有配置id
        List<Integer> settingIds = new ArrayList<>();
        for (OrderBetRecord orderBet : orderBetRecords) {
            settingIds.add(orderBet.getSettingId());
        }
        // 获取所有赔率信息
        Map<Integer, LotteryPlayOdds> oddsMap = lotteryPlayOddsService.selectPlayOddsBySettingIds(settingIds);
        for (OrderBetRecord orderBet : orderBetRecords) {
            try {
                BigDecimal winAmount = BigDecimal.ZERO;
                // 获取玩法信息
                LotteryPlay play = playMap.get(orderBet.getPlayId());
                // 获取赔率信息
                LotteryPlayOdds odds = oddsMap.get(orderBet.getSettingId());
                //140205：第一名--- 140214：第十名
                if (orderBet.getPlayId() == 140205 || orderBet.getPlayId() == 140206 || orderBet.getPlayId() == 140207 || orderBet.getPlayId() == 140208 || orderBet.getPlayId() == 140209
                        || orderBet.getPlayId() == 140210 || orderBet.getPlayId() == 140211 || orderBet.getPlayId() == 140212 || orderBet.getPlayId() == 140213 || orderBet.getPlayId() == 140214) {
                    Map<String, LotteryPlayOdds> oddsHeMap = lotteryPlayOddsService.selectPlayOddsBySettingId(orderBet.getSettingId());
                    odds = oddsHeMap.get(orderBet.getBetNumber());
                }
                orderBet.setWinCount(Constants.STR_ZERO);
                StringBuilder betNumber = new StringBuilder();
                String winNum;
                if (orderBet.getBetNumber().contains(Constants.STR_AT)) {
                    winNum = XyftUtils.isWin(orderBet.getBetNumber(), number, play);
                } else {
                    betNumber.append(orderBet.getPlayName()).append("@").append(orderBet.getBetNumber());
                    winNum = XyftUtils.isWin(betNumber.toString(), number, play);
                }
                if (StringUtils.isNotBlank(winNum)) {
                    // 获取总注数/中奖注数
                    String winCount = odds.getWinCount();
                    String totalCount = odds.getTotalCount();

                    // 计算赔率
                    double odd = Double.parseDouble(totalCount) * 1.0 / Double.parseDouble(winCount) * divisor;
                    //一注的中奖额
                    winAmount = orderBet.getBetAmount().divide(BigDecimal.valueOf(orderBet.getBetCount())).multiply(BigDecimal.valueOf(odd));
                    orderBet.setWinCount("1");
//                    if (PLAY_IDS_DWD.contains(play.getId())) {
//                        // 定位胆前五,后五的玩法,可能中多注
//                        int length = winNum.split(",").length;
//                        winAmount = winAmount.multiply(BigDecimal.valueOf(length));
//                        orderBet.setWinCount(String.valueOf(length));
//                    }

                }
                // 根据中奖金额,修改投注信息及相关信息
                OrderRecord orderRecord = orderMap.get(orderBet.getOrderId());

                try {
                    betCommonService.winOrLose(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
                } catch (TransactionSystemException e1) {
                    logger.error(String.format("订单结算出错 事务冲突 进行重试" + orderRecord.getOrderSn()), e1);
                    for (int i = 0; i < 20; i++) {
                        try {
                            betCommonService.winOrLose(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
                        } catch (TransactionSystemException e2) {
                            logger.error(String.format("订单结算出错" + i + ",事务冲突 进行重试" + orderRecord.getOrderSn()), e2);
                            Thread.sleep(100);
                            continue;
                        }
                        break;
                    }
                }

                /** 追号 */
                orderNewAppendWriteService.appendOrder(orderRecord, winAmount, StringUtils.isNotBlank(winNum));
            } catch (Exception e) {
                logger.error("结算出错：issue" + "," + issue, e);
            }

        }
    }

    private void clearingDzxyftOnePlayManyOdds(String issue, String number, Integer playId) throws Exception {
        // 获取相应的订单信息
        List<OrderRecord> orderRecords = getOrderRecord(issue);
        if (CollectionUtils.isEmpty(orderRecords)) {
            return;
        }
        Map<Integer, OrderRecord> orderMap = new HashMap<>();
        // 获取相关订单id集合
        List<Integer> orderIds = new ArrayList<>();
        this.updateOrder(number, orderRecords, orderIds, orderMap);
        // 获取赔率因子
        double divisor = betCommonService.getDivisor(lotteryId);
        // 查询所有所有相关投注信息
        List<Integer> playIds = new ArrayList<>();
        playIds.add(playId);
        List<OrderBetRecord> orderBetRecords = orderReadRestService.selectOrderBets(orderIds, playIds, OrderBetStatus.WAIT);
        // 判空处理
        if (CollectionUtils.isEmpty(orderBetRecords)) {
            return;
        }
        // 获取配置id
        Integer settingId = orderBetRecords.get(0).getSettingId();
        // 获取所有赔率信息
        Map<String, LotteryPlayOdds> oddsMap = lotteryPlayOddsService.selectPlayOddsBySettingId(settingId);
        for (OrderBetRecord orderBet : orderBetRecords) {
            try {
                BigDecimal winAmount = BigDecimal.ZERO;
                // 判断是否中奖
                String winNum;
                StringBuilder betNumber = new StringBuilder();
                if (orderBet.getBetNumber().contains(Constants.STR_AT)) {
                    winNum = XyftUtils.isWinLmAndGyh(orderBet.getBetNumber(), number, playId);
                } else {
                    betNumber.append(orderBet.getPlayName()).append("@").append(orderBet.getBetNumber());
                    winNum = XyftUtils.isWinLmAndGyh(betNumber.toString(), number, playId);
                }

                orderBet.setWinCount(Constants.STR_ZERO);
                if (StringUtils.isNotBlank(winNum)) {
                    String[] winStrArr = winNum.split(",");
                    for (String winStr : winStrArr) {
                        // 获取赔率信息
                        LotteryPlayOdds odds = oddsMap.get(winStr);
                        // 获取总注数/中奖注数
                        String winCount = odds.getWinCount();
                        String totalCount = odds.getTotalCount();
                        // 计算赔率
                        double odd = Double.parseDouble(totalCount) * 1.0 / Double.parseDouble(winCount) * divisor;
                        //中奖额
                        winAmount = winAmount.add(orderBet.getBetAmount().divide(BigDecimal.valueOf(orderBet.getBetCount())).multiply(BigDecimal.valueOf(odd)));
                    }
                    orderBet.setWinCount("1");
                }
                // 根据中奖金额,修改投注信息及相关信息
                OrderRecord orderRecord = orderMap.get(orderBet.getOrderId());

                try {
                    betCommonService.winOrLose(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
                } catch (TransactionSystemException e1) {
                    logger.error(String.format("订单结算出错 事务冲突 进行重试" + orderRecord.getOrderSn()), e1);
                    for (int i = 0; i < 20; i++) {
                        try {
                            betCommonService.winOrLose(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
                        } catch (TransactionSystemException e2) {
                            logger.error(String.format("订单结算出错" + i + ",事务冲突 进行重试" + orderRecord.getOrderSn()), e2);
                            Thread.sleep(100);
                            continue;
                        }
                        break;
                    }
                }
                /** 追号 */
                orderNewAppendWriteService.appendOrder(orderRecord, winAmount, StringUtils.isNotBlank(winNum));
            } catch (Exception e) {
                logger.error("结算出错：issue" + "," + issue, e);
            }

        }
    }

    private void updateOrder(String number, List<OrderRecord> orderRecords, List<Integer> orderIds, Map<Integer, OrderRecord> orderMap) {
        for (OrderRecord order : orderRecords) {
            Integer orderId = order.getId();
            orderMap.put(orderId, order);
            orderIds.add(orderId);
            // 获取开奖结果
            String openNumber = order.getOpenNumber();
            // 判断是否已开奖
            if (StringUtils.isNotBlank(openNumber) && number.equals(openNumber)) {
                continue;
            }
            order.setOpenNumber(number);
            orderRecordMapper.updateByPrimaryKeySelective(order);
        }
    }


    // ################################# 使用了redis start ###################################################


    /**
     * 根据期号获取订单信息
     *
     * @param issue
     * @return
     */
    public List<OrderRecord> getOrderRecord(String issue) {
        List<OrderRecord> orderRecords = (List<OrderRecord>) redisTemplate.opsForValue().get(ORDER_KEY + issue);
        // 获取相应的订单信息
        if (CollectionUtils.isEmpty(orderRecords)) {
            orderRecords = orderReadRestService.selectOrders(lotteryId, issue, OrderStatus.NORMAL);
            redisTemplate.opsForValue().set(ORDER_KEY + issue, orderRecords, 2, TimeUnit.MINUTES);
        }
        return orderRecords;
    }

    // ################################# 使用了redis end ###################################################

}
