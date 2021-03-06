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

    // PK10??????
    private final int PLAY_ID_LM = 140201;
    // PK10?????????
    private final int PLAY_ID_GYH = 140202;

    // PK10 1-5???
    private final int PLAY_ID_15 = 140203;
    // PK10 6-10???
    private final int PLAY_ID_610 = 140204;
    // PK10 ?????????
    private final int PLAY_ID_1 = 140205;
    // PK10 ?????????
    private final int PLAY_ID_2 = 140206;
    // PK10 ?????????
    private final int PLAY_ID_3 = 140207;
    // PK10 ?????????
    private final int PLAY_ID_4 = 140208;
    // PK10 ?????????
    private final int PLAY_ID_5 = 140209;
    // PK10 ?????????
    private final int PLAY_ID_6 = 140210;
    // PK10 ?????????
    private final int PLAY_ID_7 = 140211;
    // PK10 ?????????
    private final int PLAY_ID_8 = 140212;
    // PK10 ?????????
    private final int PLAY_ID_9 = 140213;
    // PK10 ?????????
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
        // ??????????????????id??????
        List<Integer> orderIds = new ArrayList<>();
        this.updateOrder(number, orderRecords, orderIds, orderMap);
        // ??????????????????
        double divisor = betCommonService.getDivisor(lotteryId);

        // ????????????????????????????????????
        List<OrderBetRecord> orderBetRecords = orderReadRestService.selectOrderBets(orderIds, playIds, OrderBetStatus.WAIT);
        // ????????????
        if (CollectionUtils.isEmpty(orderBetRecords)) {
            return;
        }
        // ????????????????????????
        Map<Integer, LotteryPlay> playMap = lotteryPlayWriteService.selectPlayByIds(playIds);
        // ??????????????????id
        List<Integer> settingIds = new ArrayList<>();
        for (OrderBetRecord orderBet : orderBetRecords) {
            settingIds.add(orderBet.getSettingId());
        }
        // ????????????????????????
        Map<Integer, LotteryPlayOdds> oddsMap = lotteryPlayOddsService.selectPlayOddsBySettingIds(settingIds);
        for (OrderBetRecord orderBet : orderBetRecords) {
            try {
                BigDecimal winAmount = BigDecimal.ZERO;
                // ??????????????????
                LotteryPlay play = playMap.get(orderBet.getPlayId());
                // ??????????????????
                LotteryPlayOdds odds = oddsMap.get(orderBet.getSettingId());
                //140205????????????--- 140214????????????
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
                    // ???????????????/????????????
                    String winCount = odds.getWinCount();
                    String totalCount = odds.getTotalCount();

                    // ????????????
                    double odd = Double.parseDouble(totalCount) * 1.0 / Double.parseDouble(winCount) * divisor;
                    //??????????????????
                    winAmount = orderBet.getBetAmount().divide(BigDecimal.valueOf(orderBet.getBetCount())).multiply(BigDecimal.valueOf(odd));
                    orderBet.setWinCount("1");
//                    if (PLAY_IDS_DWD.contains(play.getId())) {
//                        // ???????????????,???????????????,???????????????
//                        int length = winNum.split(",").length;
//                        winAmount = winAmount.multiply(BigDecimal.valueOf(length));
//                        orderBet.setWinCount(String.valueOf(length));
//                    }

                }
                // ??????????????????,?????????????????????????????????
                OrderRecord orderRecord = orderMap.get(orderBet.getOrderId());

                try {
                    betCommonService.winOrLose(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
                } catch (TransactionSystemException e1) {
                    logger.error(String.format("?????????????????? ???????????? ????????????" + orderRecord.getOrderSn()), e1);
                    for (int i = 0; i < 20; i++) {
                        try {
                            betCommonService.winOrLose(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
                        } catch (TransactionSystemException e2) {
                            logger.error(String.format("??????????????????" + i + ",???????????? ????????????" + orderRecord.getOrderSn()), e2);
                            Thread.sleep(100);
                            continue;
                        }
                        break;
                    }
                }

                /** ?????? */
                orderNewAppendWriteService.appendOrder(orderRecord, winAmount, StringUtils.isNotBlank(winNum));
            } catch (Exception e) {
                logger.error("???????????????issue" + "," + issue, e);
            }

        }
    }

    private void clearingDzxyftOnePlayManyOdds(String issue, String number, Integer playId) throws Exception {
        // ???????????????????????????
        List<OrderRecord> orderRecords = getOrderRecord(issue);
        if (CollectionUtils.isEmpty(orderRecords)) {
            return;
        }
        Map<Integer, OrderRecord> orderMap = new HashMap<>();
        // ??????????????????id??????
        List<Integer> orderIds = new ArrayList<>();
        this.updateOrder(number, orderRecords, orderIds, orderMap);
        // ??????????????????
        double divisor = betCommonService.getDivisor(lotteryId);
        // ????????????????????????????????????
        List<Integer> playIds = new ArrayList<>();
        playIds.add(playId);
        List<OrderBetRecord> orderBetRecords = orderReadRestService.selectOrderBets(orderIds, playIds, OrderBetStatus.WAIT);
        // ????????????
        if (CollectionUtils.isEmpty(orderBetRecords)) {
            return;
        }
        // ????????????id
        Integer settingId = orderBetRecords.get(0).getSettingId();
        // ????????????????????????
        Map<String, LotteryPlayOdds> oddsMap = lotteryPlayOddsService.selectPlayOddsBySettingId(settingId);
        for (OrderBetRecord orderBet : orderBetRecords) {
            try {
                BigDecimal winAmount = BigDecimal.ZERO;
                // ??????????????????
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
                        // ??????????????????
                        LotteryPlayOdds odds = oddsMap.get(winStr);
                        // ???????????????/????????????
                        String winCount = odds.getWinCount();
                        String totalCount = odds.getTotalCount();
                        // ????????????
                        double odd = Double.parseDouble(totalCount) * 1.0 / Double.parseDouble(winCount) * divisor;
                        //?????????
                        winAmount = winAmount.add(orderBet.getBetAmount().divide(BigDecimal.valueOf(orderBet.getBetCount())).multiply(BigDecimal.valueOf(odd)));
                    }
                    orderBet.setWinCount("1");
                }
                // ??????????????????,?????????????????????????????????
                OrderRecord orderRecord = orderMap.get(orderBet.getOrderId());

                try {
                    betCommonService.winOrLose(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
                } catch (TransactionSystemException e1) {
                    logger.error(String.format("?????????????????? ???????????? ????????????" + orderRecord.getOrderSn()), e1);
                    for (int i = 0; i < 20; i++) {
                        try {
                            betCommonService.winOrLose(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
                        } catch (TransactionSystemException e2) {
                            logger.error(String.format("??????????????????" + i + ",???????????? ????????????" + orderRecord.getOrderSn()), e2);
                            Thread.sleep(100);
                            continue;
                        }
                        break;
                    }
                }
                /** ?????? */
                orderNewAppendWriteService.appendOrder(orderRecord, winAmount, StringUtils.isNotBlank(winNum));
            } catch (Exception e) {
                logger.error("???????????????issue" + "," + issue, e);
            }

        }
    }

    private void updateOrder(String number, List<OrderRecord> orderRecords, List<Integer> orderIds, Map<Integer, OrderRecord> orderMap) {
        for (OrderRecord order : orderRecords) {
            Integer orderId = order.getId();
            orderMap.put(orderId, order);
            orderIds.add(orderId);
            // ??????????????????
            String openNumber = order.getOpenNumber();
            // ?????????????????????
            if (StringUtils.isNotBlank(openNumber) && number.equals(openNumber)) {
                continue;
            }
            order.setOpenNumber(number);
            orderRecordMapper.updateByPrimaryKeySelective(order);
        }
    }


    // ################################# ?????????redis start ###################################################


    /**
     * ??????????????????????????????
     *
     * @param issue
     * @return
     */
    public List<OrderRecord> getOrderRecord(String issue) {
        List<OrderRecord> orderRecords = (List<OrderRecord>) redisTemplate.opsForValue().get(ORDER_KEY + issue);
        // ???????????????????????????
        if (CollectionUtils.isEmpty(orderRecords)) {
            orderRecords = orderReadRestService.selectOrders(lotteryId, issue, OrderStatus.NORMAL);
            redisTemplate.opsForValue().set(ORDER_KEY + issue, orderRecords, 2, TimeUnit.MINUTES);
        }
        return orderRecords;
    }

    // ################################# ?????????redis end ###################################################

}
