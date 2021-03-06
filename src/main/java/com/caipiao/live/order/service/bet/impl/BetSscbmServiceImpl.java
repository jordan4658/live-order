package com.caipiao.live.order.service.bet.impl;

import com.caipiao.live.common.service.read.OrderReadRestService;
import com.caipiao.live.common.util.lottery.BetSscUtil;
import com.caipiao.live.order.service.bet.BetCommonService;
import com.caipiao.live.order.service.bet.BetSscService;
import com.caipiao.live.order.service.bet.BetSscbmService;
import com.caipiao.live.order.service.lottery.LotteryPlayOddsWriteService;
import com.caipiao.live.order.service.order.OrderAppendWriteService;
import com.caipiao.live.order.service.order.OrderWriteService;
import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.model.dto.order.OrderBetStatus;
import com.caipiao.live.common.model.dto.order.OrderStatus;
import com.caipiao.live.common.mybatis.entity.OrderBetRecord;
import com.caipiao.live.common.mybatis.entity.OrderRecord;
import com.caipiao.live.common.mybatis.mapper.OrderBetRecordMapper;
import com.caipiao.live.common.mybatis.mapper.OrderRecordMapper;
import com.caipiao.live.common.mybatis.mapperbean.CqsscBeanMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BetSscbmServiceImpl implements BetSscbmService {

    private static final Logger logger = LoggerFactory.getLogger(BetSscbmServiceImpl.class);

    @Autowired
    private OrderWriteService orderWriteService;
    @Autowired
    private OrderBetRecordMapper orderBetRecordMapper;
    @Autowired
    private LotteryPlayOddsWriteService lotteryPlayOddsService;
    @Autowired
    private OrderAppendWriteService orderAppendWriteService;
    @Autowired
    private BetSscService betSscService;
    @Autowired
    private CqsscBeanMapper cqsscBeanMapper;
    @Autowired
    private OrderRecordMapper orderRecordMapper;
    @Autowired
    private BetCommonService betCommonService;
    @Autowired
    private OrderReadRestService orderReadRestService;

    @Override
    public void countlm(String issue, String number, int lotteryId) {
        logger.info("ss??? - ??????????????????" + issue + "???/ ???????????????" + number + "???");
        // ???????????????????????????
        List<OrderRecord> orderRecords = orderReadRestService.selectOrders(lotteryId, issue, OrderStatus.NORMAL);

        // ????????????
        if (CollectionUtils.isEmpty(orderRecords)) {
            return;
        }

        // ??????????????????????????????ID
        List<Integer> playIds = new ArrayList<Integer>();
        playIds.add(Integer.parseInt(lotteryId + Constants.PLAY_01));

        Map<Integer, OrderRecord> orderMap = new HashMap<>();
        // ??????????????????id??????
        List<Integer> orderIds = new ArrayList<>();
        //TODO ?????????
        betCommonService.updateOrder(number, orderRecords, orderIds, orderMap);

        // ????????????????????????????????????
        List<OrderBetRecord> orderBetRecords = orderReadRestService.selectOrderBets(orderIds, playIds, OrderBetStatus.WAIT);

        // ????????????
        if (CollectionUtils.isEmpty(orderBetRecords)) {
            return;
        }

        for (OrderBetRecord orderBet : orderBetRecords) {
            try {
                orderBet.setWinCount(Constants.STR_ZERO);
                // ????????????
                BigDecimal odds = lotteryPlayOddsService.countOdds(lotteryId, orderBet.getSettingId(), orderBet.getBetNumber().contains("@") ? orderBet.getBetNumber().split("@")[1] : orderBet.getBetNumber());
                // ??????????????????
                Boolean win = BetSscUtil.isWinBylm(orderBet.getBetNumber(), number);
                BigDecimal winAmount = BigDecimal.ZERO;
                if (win) {
                    BigDecimal sig = orderBet.getBetAmount().divide(BigDecimal.valueOf(orderBet.getBetCount()), BigDecimal.ROUND_HALF_UP);
                    winAmount = sig.multiply(odds);
                    orderBet.setWinCount(Constants.STRING_ONE);
                }

                // ??????????????????
                OrderRecord orderRecord = orderMap.get(orderBet.getOrderId());

                try {
                    betCommonService.winOrLose(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
                } catch (TransactionSystemException e1) {
                    logger.error("?????????????????? ???????????? ???????????????orderSn:{}.", orderRecord.getOrderSn(), e1);
                    for (int i = 0; i < 10; i++) {
                        try {
                            betCommonService.winOrLose(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
                        } catch (TransactionSystemException e2) {
                            logger.error("??????????????????,???????????? ???????????????orderSn:{}.", orderRecord.getOrderSn(), e2);
                            Thread.sleep(100);
                            continue;
                        }
                        break;
                    }
                }

//                /** ????????????/???????????? **/
//                betSscService.changeBalance(win, winAmount, orderBet, orderRecord);

                /** ?????? */
                orderAppendWriteService.appendOrder(orderRecord, winAmount, win);
            } catch (Exception e) {
                logger.error("???????????????issue" + "," + issue, e);
            }

        }

    }

    @Override
    public void countdn(String issue, String number, int lotteryId) {
        logger.info("?????????????????? - ???????????? ????????????" + issue + "???/ ???????????????" + number + "???");
        // ???????????????????????????
        List<OrderRecord> orderRecords = orderReadRestService.selectOrders(lotteryId, issue, OrderStatus.NORMAL);

        // ????????????
        if (CollectionUtils.isEmpty(orderRecords)) {
            return;
        }

        // ??????????????????????????????ID
        List<Integer> playIds = new ArrayList<>();
        playIds.add(Integer.parseInt(lotteryId + Constants.PLAY_02));

        Map<Integer, OrderRecord> orderMap = new HashMap<>();
        // ??????????????????id??????
        List<Integer> orderIds = new ArrayList<>();
        betCommonService.updateOrder(number, orderRecords, orderIds, orderMap);

        // ????????????????????????????????????
        List<OrderBetRecord> orderBetRecords = orderReadRestService.selectOrderBets(orderIds, playIds, OrderBetStatus.WAIT);

        // ????????????
        if (CollectionUtils.isEmpty(orderBetRecords)) {
            return;
        }

        for (OrderBetRecord orderBet : orderBetRecords) {
            try {
                // ????????????
                BigDecimal odds = lotteryPlayOddsService.countOdds(lotteryId, orderBet.getSettingId(), orderBet.getBetNumber().contains("@") ? orderBet.getBetNumber().split("@")[1] : orderBet.getBetNumber());
                orderBet.setWinCount(Constants.STR_ZERO);
                // ??????????????????
                Boolean win = BetSscUtil.isWinByDN(orderBet.getBetNumber(), number);
                BigDecimal winAmount = BigDecimal.ZERO;
                if (win) {
                    winAmount = orderBet.getBetAmount().divide(BigDecimal.valueOf(orderBet.getBetCount()), BigDecimal.ROUND_HALF_UP).multiply(odds);
                    orderBet.setWinCount(Constants.STRING_ONE);
                }

                // ??????????????????
                OrderRecord orderRecord = orderMap.get(orderBet.getOrderId());

                try {
                    betCommonService.winOrLose(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
                } catch (TransactionSystemException e1) {
                    logger.error("?????????????????? ???????????? ???????????????orderSn:{}.", orderRecord.getOrderSn(), e1);
                    for (int i = 0; i < 10; i++) {
                        try {
                            betCommonService.winOrLose(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
                        } catch (TransactionSystemException e2) {
                            logger.error("??????????????????,???????????? ???????????????orderSn:{}.", orderRecord.getOrderSn(), e2);
                            Thread.sleep(100);
                            continue;
                        }
                        break;
                    }
                }


                /** ?????? */
                orderAppendWriteService.appendOrder(orderRecord, winAmount, win);
            } catch (Exception e) {
                logger.error("???????????????issue" + "," + issue, e);
            }

        }

    }

    @Override
    public void count15(String issue, String number, int lotteryId) {
        logger.info("?????????????????? - ???????????? ????????????" + issue + "???/ ???????????????" + number + "???");

        // ???????????????????????????
        List<OrderRecord> orderRecords = orderReadRestService.selectOrders(lotteryId, issue, OrderStatus.NORMAL);

        // ????????????
        if (CollectionUtils.isEmpty(orderRecords)) {
            return;
        }

        // ??????????????????????????????ID
        List<Integer> playIds = new ArrayList<>();
        playIds.add(Integer.parseInt(lotteryId + Constants.PLAY_03));
        playIds.add(Integer.parseInt(lotteryId + Constants.PLAY_05));
        playIds.add(Integer.parseInt(lotteryId + Constants.PLAY_06));
        playIds.add(Integer.parseInt(lotteryId + Constants.PLAY_07));
        playIds.add(Integer.parseInt(lotteryId + Constants.PLAY_08));
        playIds.add(Integer.parseInt(lotteryId + Constants.PLAY_09));

//        // ??????/??????????????????
//        orderWriteService.updateOrder(number, orderRecords);
//
//        // ??????????????????id??????
//        List<Integer> orderIds = new ArrayList<>();
//        // ????????????Map
//        Map<Integer, OrderRecord> orderMap = new HashMap<>();
//        for (OrderRecord order : orderRecords) {
//            Integer id = order.getId();
//            orderIds.add(id);
//            orderMap.put(id, order);
//        }

        Map<Integer, OrderRecord> orderMap = new HashMap<>();
        // ??????????????????id??????
        List<Integer> orderIds = new ArrayList<>();
        betCommonService.updateOrder(number, orderRecords, orderIds, orderMap);


        // ????????????????????????????????????
        List<OrderBetRecord> orderBetRecords = orderReadRestService.selectOrderBets(orderIds, playIds, OrderBetStatus.WAIT);
        // ????????????
        if (CollectionUtils.isEmpty(orderBetRecords)) {
            return;
        }
        for (OrderBetRecord orderBet : orderBetRecords) {
            try {
                // ????????????
                BigDecimal odds = lotteryPlayOddsService.countOdds(lotteryId, orderBet.getSettingId(), orderBet.getBetNumber());
                orderBet.setWinCount(Constants.STR_ZERO);
                // ??????????????????
                Boolean win = BetSscUtil.isWinBy15(orderBet.getBetNumber(), number, orderBet.getPlayId());
                BigDecimal winAmount = BigDecimal.ZERO;
                if (win) {
                    winAmount = orderBet.getBetAmount().divide(BigDecimal.valueOf(orderBet.getBetCount()), BigDecimal.ROUND_HALF_UP).multiply(odds);
                    orderBet.setWinCount(Constants.STRING_ONE);
                }
//                // ??????????????????
//                orderBet.setWinAmount(winAmount.setScale(2, BigDecimal.ROUND_HALF_UP));
//                // ????????????
//                orderBet.setTbStatus(status);
//                // ??????????????????
//                orderBetRecordMapper.updateByPrimaryKeySelective(orderBet);

                // ??????????????????
                OrderRecord orderRecord = orderMap.get(orderBet.getOrderId());

                try {
                    betCommonService.winOrLose(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
                } catch (TransactionSystemException e1) {
                    logger.error("?????????????????? ???????????? ???????????????orderSn:{}.", orderRecord.getOrderSn(), e1);
                    for (int i = 0; i < 10; i++) {
                        try {
                            betCommonService.winOrLose(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
                        } catch (TransactionSystemException e2) {
                            logger.error("??????????????????,???????????? ???????????????orderSn:{}.", orderRecord.getOrderSn(), e2);
                            Thread.sleep(100);
                            continue;
                        }
                        break;
                    }
                }

//                /** ????????????/???????????? **/
//                betSscService.changeBalance(win, winAmount, orderBet, orderRecord);
                /** ?????? */
                orderAppendWriteService.appendOrder(orderRecord, winAmount, win);
            } catch (Exception e) {
                logger.error("???????????????issue" + "," + issue, e);
            }

        }
    }

    @Override
    public void countqzh(String issue, String number, int lotteryId) {
        logger.info("?????????????????? - ??????????????? ????????????" + issue + "???/ ???????????????" + number + "???");
        // ???????????????????????????
        List<OrderRecord> orderRecords = orderReadRestService.selectOrders(lotteryId, issue, OrderStatus.NORMAL);
        // ????????????
        if (CollectionUtils.isEmpty(orderRecords)) {
            return;
        }
        // ??????????????????????????????ID
        List<Integer> playIds = new ArrayList<Integer>();
        playIds.add(Integer.parseInt(lotteryId + Constants.PLAY_04));

        Map<Integer, OrderRecord> orderMap = new HashMap<>();
        // ??????????????????id??????
        List<Integer> orderIds = new ArrayList<>();
        betCommonService.updateOrder(number, orderRecords, orderIds, orderMap);

        // ????????????????????????????????????
        List<OrderBetRecord> orderBetRecords = orderReadRestService.selectOrderBets(orderIds, playIds, OrderBetStatus.WAIT);
        // ????????????
        if (CollectionUtils.isEmpty(orderBetRecords)) {
            return;
        }
        for (OrderBetRecord orderBet : orderBetRecords) {
            try {
                // ????????????
                BigDecimal odds = lotteryPlayOddsService.countOdds(lotteryId, orderBet.getSettingId(), orderBet.getBetNumber());
                orderBet.setWinCount(Constants.STR_ZERO);
                // ??????????????????
                Boolean win = BetSscUtil.isWinByqzh(orderBet.getBetNumber(), number);
                BigDecimal winAmount = BigDecimal.ZERO;
                String status = OrderBetStatus.NO_WIN;
                if (win) {
                    status = OrderBetStatus.WIN;
                    winAmount = orderBet.getBetAmount().divide(BigDecimal.valueOf(orderBet.getBetCount()), BigDecimal.ROUND_HALF_UP).multiply(odds);
                    orderBet.setWinCount(Constants.STRING_ONE);
                }

                // ??????????????????
                OrderRecord orderRecord = orderMap.get(orderBet.getOrderId());

                try {
                    betCommonService.winOrLose(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
                } catch (TransactionSystemException e1) {
                    logger.error("?????????????????? ???????????? ???????????????orderSn:{}.", orderRecord.getOrderSn(), e1);
                    for (int i = 0; i < 10; i++) {
                        try {
                            betCommonService.winOrLose(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
                        } catch (TransactionSystemException e2) {
                            logger.error("??????????????????,???????????? ???????????????orderSn:{}.", orderRecord.getOrderSn(), e2);
                            Thread.sleep(100);
                            continue;
                        }
                        break;
                    }
                }

                /** ?????? */
                orderAppendWriteService.appendOrder(orderRecord, winAmount, win);
            } catch (Exception e) {
                logger.error("???????????????issue" + "," + issue, e);
            }

        }
    }


    @Override
    public void updateDataTJ(String issue, String number) {
        // ??????????????????????????????
        Integer rc = cqsscBeanMapper.updateRecommendtj(issue);
        // ??????????????????????????????
        Integer kc = cqsscBeanMapper.updateKillNumbertj(issue);
    }
}
