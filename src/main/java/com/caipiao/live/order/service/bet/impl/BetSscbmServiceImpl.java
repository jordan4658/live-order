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
        logger.info("ss彩 - 两面：期号【" + issue + "】/ 开奖号码【" + number + "】");
        // 获取相应的订单信息
        List<OrderRecord> orderRecords = orderReadRestService.selectOrders(lotteryId, issue, OrderStatus.NORMAL);

        // 判空处理
        if (CollectionUtils.isEmpty(orderRecords)) {
            return;
        }

        // 根据彩种编号生成玩法ID
        List<Integer> playIds = new ArrayList<Integer>();
        playIds.add(Integer.parseInt(lotteryId + Constants.PLAY_01));

        Map<Integer, OrderRecord> orderMap = new HashMap<>();
        // 获取相关订单id集合
        List<Integer> orderIds = new ArrayList<>();
        //TODO 可优化
        betCommonService.updateOrder(number, orderRecords, orderIds, orderMap);

        // 查询所有所有相关投注信息
        List<OrderBetRecord> orderBetRecords = orderReadRestService.selectOrderBets(orderIds, playIds, OrderBetStatus.WAIT);

        // 判空处理
        if (CollectionUtils.isEmpty(orderBetRecords)) {
            return;
        }

        for (OrderBetRecord orderBet : orderBetRecords) {
            try {
                orderBet.setWinCount(Constants.STR_ZERO);
                // 计算赔率
                BigDecimal odds = lotteryPlayOddsService.countOdds(lotteryId, orderBet.getSettingId(), orderBet.getBetNumber().contains("@") ? orderBet.getBetNumber().split("@")[1] : orderBet.getBetNumber());
                // 判断是否中奖
                Boolean win = BetSscUtil.isWinBylm(orderBet.getBetNumber(), number);
                BigDecimal winAmount = BigDecimal.ZERO;
                if (win) {
                    BigDecimal sig = orderBet.getBetAmount().divide(BigDecimal.valueOf(orderBet.getBetCount()), BigDecimal.ROUND_HALF_UP);
                    winAmount = sig.multiply(odds);
                    orderBet.setWinCount(Constants.STRING_ONE);
                }

                // 获取订单信息
                OrderRecord orderRecord = orderMap.get(orderBet.getOrderId());

                try {
                    betCommonService.winOrLose(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
                } catch (TransactionSystemException e1) {
                    logger.error("订单结算出错 事务冲突 进行重试。orderSn:{}.", orderRecord.getOrderSn(), e1);
                    for (int i = 0; i < 10; i++) {
                        try {
                            betCommonService.winOrLose(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
                        } catch (TransactionSystemException e2) {
                            logger.error("订单结算出错,事务冲突 进行重试。orderSn:{}.", orderRecord.getOrderSn(), e2);
                            Thread.sleep(100);
                            continue;
                        }
                        break;
                    }
                }

//                /** 修改余额/分红操作 **/
//                betSscService.changeBalance(win, winAmount, orderBet, orderRecord);

                /** 追号 */
                orderAppendWriteService.appendOrder(orderRecord, winAmount, win);
            } catch (Exception e) {
                logger.error("结算出错：issue" + "," + issue, e);
            }

        }

    }

    @Override
    public void countdn(String issue, String number, int lotteryId) {
        logger.info("比特币分分彩 - 直选单式 ：期号【" + issue + "】/ 开奖号码【" + number + "】");
        // 获取相应的订单信息
        List<OrderRecord> orderRecords = orderReadRestService.selectOrders(lotteryId, issue, OrderStatus.NORMAL);

        // 判空处理
        if (CollectionUtils.isEmpty(orderRecords)) {
            return;
        }

        // 根据彩种编号生成玩法ID
        List<Integer> playIds = new ArrayList<>();
        playIds.add(Integer.parseInt(lotteryId + Constants.PLAY_02));

        Map<Integer, OrderRecord> orderMap = new HashMap<>();
        // 获取相关订单id集合
        List<Integer> orderIds = new ArrayList<>();
        betCommonService.updateOrder(number, orderRecords, orderIds, orderMap);

        // 查询所有所有相关投注信息
        List<OrderBetRecord> orderBetRecords = orderReadRestService.selectOrderBets(orderIds, playIds, OrderBetStatus.WAIT);

        // 判空处理
        if (CollectionUtils.isEmpty(orderBetRecords)) {
            return;
        }

        for (OrderBetRecord orderBet : orderBetRecords) {
            try {
                // 计算赔率
                BigDecimal odds = lotteryPlayOddsService.countOdds(lotteryId, orderBet.getSettingId(), orderBet.getBetNumber().contains("@") ? orderBet.getBetNumber().split("@")[1] : orderBet.getBetNumber());
                orderBet.setWinCount(Constants.STR_ZERO);
                // 判断是否中奖
                Boolean win = BetSscUtil.isWinByDN(orderBet.getBetNumber(), number);
                BigDecimal winAmount = BigDecimal.ZERO;
                if (win) {
                    winAmount = orderBet.getBetAmount().divide(BigDecimal.valueOf(orderBet.getBetCount()), BigDecimal.ROUND_HALF_UP).multiply(odds);
                    orderBet.setWinCount(Constants.STRING_ONE);
                }

                // 获取订单信息
                OrderRecord orderRecord = orderMap.get(orderBet.getOrderId());

                try {
                    betCommonService.winOrLose(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
                } catch (TransactionSystemException e1) {
                    logger.error("订单结算出错 事务冲突 进行重试。orderSn:{}.", orderRecord.getOrderSn(), e1);
                    for (int i = 0; i < 10; i++) {
                        try {
                            betCommonService.winOrLose(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
                        } catch (TransactionSystemException e2) {
                            logger.error("订单结算出错,事务冲突 进行重试。orderSn:{}.", orderRecord.getOrderSn(), e2);
                            Thread.sleep(100);
                            continue;
                        }
                        break;
                    }
                }


                /** 追号 */
                orderAppendWriteService.appendOrder(orderRecord, winAmount, win);
            } catch (Exception e) {
                logger.error("结算出错：issue" + "," + issue, e);
            }

        }

    }

    @Override
    public void count15(String issue, String number, int lotteryId) {
        logger.info("比特币分分彩 - 基本组选 ：期号【" + issue + "】/ 开奖号码【" + number + "】");

        // 获取相应的订单信息
        List<OrderRecord> orderRecords = orderReadRestService.selectOrders(lotteryId, issue, OrderStatus.NORMAL);

        // 判空处理
        if (CollectionUtils.isEmpty(orderRecords)) {
            return;
        }

        // 根据彩种编号生成玩法ID
        List<Integer> playIds = new ArrayList<>();
        playIds.add(Integer.parseInt(lotteryId + Constants.PLAY_03));
        playIds.add(Integer.parseInt(lotteryId + Constants.PLAY_05));
        playIds.add(Integer.parseInt(lotteryId + Constants.PLAY_06));
        playIds.add(Integer.parseInt(lotteryId + Constants.PLAY_07));
        playIds.add(Integer.parseInt(lotteryId + Constants.PLAY_08));
        playIds.add(Integer.parseInt(lotteryId + Constants.PLAY_09));

//        // 修改/获取订单信息
//        orderWriteService.updateOrder(number, orderRecords);
//
//        // 获取相关订单id集合
//        List<Integer> orderIds = new ArrayList<>();
//        // 封装订单Map
//        Map<Integer, OrderRecord> orderMap = new HashMap<>();
//        for (OrderRecord order : orderRecords) {
//            Integer id = order.getId();
//            orderIds.add(id);
//            orderMap.put(id, order);
//        }

        Map<Integer, OrderRecord> orderMap = new HashMap<>();
        // 获取相关订单id集合
        List<Integer> orderIds = new ArrayList<>();
        betCommonService.updateOrder(number, orderRecords, orderIds, orderMap);


        // 查询所有所有相关投注信息
        List<OrderBetRecord> orderBetRecords = orderReadRestService.selectOrderBets(orderIds, playIds, OrderBetStatus.WAIT);
        // 判空处理
        if (CollectionUtils.isEmpty(orderBetRecords)) {
            return;
        }
        for (OrderBetRecord orderBet : orderBetRecords) {
            try {
                // 计算赔率
                BigDecimal odds = lotteryPlayOddsService.countOdds(lotteryId, orderBet.getSettingId(), orderBet.getBetNumber());
                orderBet.setWinCount(Constants.STR_ZERO);
                // 判断是否中奖
                Boolean win = BetSscUtil.isWinBy15(orderBet.getBetNumber(), number, orderBet.getPlayId());
                BigDecimal winAmount = BigDecimal.ZERO;
                if (win) {
                    winAmount = orderBet.getBetAmount().divide(BigDecimal.valueOf(orderBet.getBetCount()), BigDecimal.ROUND_HALF_UP).multiply(odds);
                    orderBet.setWinCount(Constants.STRING_ONE);
                }
//                // 设置中奖金额
//                orderBet.setWinAmount(winAmount.setScale(2, BigDecimal.ROUND_HALF_UP));
//                // 设置状态
//                orderBet.setTbStatus(status);
//                // 修改投注信息
//                orderBetRecordMapper.updateByPrimaryKeySelective(orderBet);

                // 获取订单信息
                OrderRecord orderRecord = orderMap.get(orderBet.getOrderId());

                try {
                    betCommonService.winOrLose(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
                } catch (TransactionSystemException e1) {
                    logger.error("订单结算出错 事务冲突 进行重试。orderSn:{}.", orderRecord.getOrderSn(), e1);
                    for (int i = 0; i < 10; i++) {
                        try {
                            betCommonService.winOrLose(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
                        } catch (TransactionSystemException e2) {
                            logger.error("订单结算出错,事务冲突 进行重试。orderSn:{}.", orderRecord.getOrderSn(), e2);
                            Thread.sleep(100);
                            continue;
                        }
                        break;
                    }
                }

//                /** 修改余额/分红操作 **/
//                betSscService.changeBalance(win, winAmount, orderBet, orderRecord);
                /** 追号 */
                orderAppendWriteService.appendOrder(orderRecord, winAmount, win);
            } catch (Exception e) {
                logger.error("结算出错：issue" + "," + issue, e);
            }

        }
    }

    @Override
    public void countqzh(String issue, String number, int lotteryId) {
        logger.info("比特币分分彩 - 三星组选三 ：期号【" + issue + "】/ 开奖号码【" + number + "】");
        // 获取相应的订单信息
        List<OrderRecord> orderRecords = orderReadRestService.selectOrders(lotteryId, issue, OrderStatus.NORMAL);
        // 判空处理
        if (CollectionUtils.isEmpty(orderRecords)) {
            return;
        }
        // 根据彩种编号生成玩法ID
        List<Integer> playIds = new ArrayList<Integer>();
        playIds.add(Integer.parseInt(lotteryId + Constants.PLAY_04));

        Map<Integer, OrderRecord> orderMap = new HashMap<>();
        // 获取相关订单id集合
        List<Integer> orderIds = new ArrayList<>();
        betCommonService.updateOrder(number, orderRecords, orderIds, orderMap);

        // 查询所有所有相关投注信息
        List<OrderBetRecord> orderBetRecords = orderReadRestService.selectOrderBets(orderIds, playIds, OrderBetStatus.WAIT);
        // 判空处理
        if (CollectionUtils.isEmpty(orderBetRecords)) {
            return;
        }
        for (OrderBetRecord orderBet : orderBetRecords) {
            try {
                // 计算赔率
                BigDecimal odds = lotteryPlayOddsService.countOdds(lotteryId, orderBet.getSettingId(), orderBet.getBetNumber());
                orderBet.setWinCount(Constants.STR_ZERO);
                // 判断是否中奖
                Boolean win = BetSscUtil.isWinByqzh(orderBet.getBetNumber(), number);
                BigDecimal winAmount = BigDecimal.ZERO;
                String status = OrderBetStatus.NO_WIN;
                if (win) {
                    status = OrderBetStatus.WIN;
                    winAmount = orderBet.getBetAmount().divide(BigDecimal.valueOf(orderBet.getBetCount()), BigDecimal.ROUND_HALF_UP).multiply(odds);
                    orderBet.setWinCount(Constants.STRING_ONE);
                }

                // 获取订单信息
                OrderRecord orderRecord = orderMap.get(orderBet.getOrderId());

                try {
                    betCommonService.winOrLose(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
                } catch (TransactionSystemException e1) {
                    logger.error("订单结算出错 事务冲突 进行重试。orderSn:{}.", orderRecord.getOrderSn(), e1);
                    for (int i = 0; i < 10; i++) {
                        try {
                            betCommonService.winOrLose(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
                        } catch (TransactionSystemException e2) {
                            logger.error("订单结算出错,事务冲突 进行重试。orderSn:{}.", orderRecord.getOrderSn(), e2);
                            Thread.sleep(100);
                            continue;
                        }
                        break;
                    }
                }

                /** 追号 */
                orderAppendWriteService.appendOrder(orderRecord, winAmount, win);
            } catch (Exception e) {
                logger.error("结算出错：issue" + "," + issue, e);
            }

        }
    }


    @Override
    public void updateDataTJ(String issue, String number) {
        // 更新【免费推荐】数据
        Integer rc = cqsscBeanMapper.updateRecommendtj(issue);
        // 更新【公式杀号】数据
        Integer kc = cqsscBeanMapper.updateKillNumbertj(issue);
    }
}
