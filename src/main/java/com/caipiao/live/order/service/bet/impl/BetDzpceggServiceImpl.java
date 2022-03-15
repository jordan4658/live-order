package com.caipiao.live.order.service.bet.impl;

import com.caipiao.live.common.service.read.OrderReadRestService;
import com.caipiao.live.order.service.bet.BetCommonService;
import com.caipiao.live.order.service.bet.BetDzpceggService;
import com.caipiao.live.order.service.order.OrderNewAppendWriteService;
import com.caipiao.live.order.service.lottery.LotteryPlayOddsWriteService;
import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.model.dto.order.OrderBetStatus;
import com.caipiao.live.common.model.dto.order.OrderStatus;
import com.caipiao.live.common.mybatis.entity.LotteryPlayOdds;
import com.caipiao.live.common.mybatis.entity.OrderBetRecord;
import com.caipiao.live.common.mybatis.entity.OrderRecord;
import com.caipiao.live.common.mybatis.mapper.OrderRecordMapper;
import com.caipiao.live.common.util.lottery.PceggUtil;
import org.apache.commons.lang3.StringUtils;
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

/**
 * @author lzy
 * @create 2018-09-14 10:16
 **/
@Service
public class BetDzpceggServiceImpl implements BetDzpceggService {
    private static final Logger logger = LoggerFactory.getLogger(BetDzpceggServiceImpl.class);
    private final Integer lotteryId = 1502;
    @Autowired
    private OrderRecordMapper orderRecordMapper;
    @Autowired
    private LotteryPlayOddsWriteService lotteryPlayOddsService;
    @Autowired
    private BetCommonService betCommonService;
    @Autowired
    private OrderNewAppendWriteService orderNewAppendWriteService;
    @Autowired
    private OrderReadRestService orderReadRestService;

    // 混合玩法id
    private final int PLAY_ID_HH = 150201;
    // 色波玩法id
    private final int PLAY_ID_SB = 150202;
    // 豹子玩法id
    private final int PLAY_ID_BZ = 150203;
    // 特码包三玩法id
    private final int PLAY_ID_TMBS = 150204;
    // 特码玩法id
    private final int PLAY_ID_TM = 150205;

    @Override
    public void clearingDzpceggTm(String issue, String number) throws Exception {
        clearingDzpcegg(issue, number, PLAY_ID_TM);
    }

    @Override
    public void clearingDzpceggBz(String issue, String number) throws Exception {
        clearingDzpcegg(issue, number, PLAY_ID_BZ);
    }

    @Override
    public void clearingDzpceggTmbs(String issue, String number) throws Exception {
        clearingDzpcegg(issue, number, PLAY_ID_TMBS);
    }

    @Override
    public void clearingDzpceggSb(String issue, String number) throws Exception {
        clearingDzpcegg(issue, number, PLAY_ID_SB);
    }

    @Override
    public void clearingDzpceggHh(String issue, String number) throws Exception {
        clearingDzpcegg(issue, number, PLAY_ID_HH);
    }

    /**
     * PC蛋蛋结算
     *
     * @param issue  期号
     * @param number 开奖号码
     * @param playId 玩法id
     */
    private void clearingDzpcegg(String issue, String number, Integer playId) {
        // 获取相应的订单信息
        List<OrderRecord> orderRecords = orderReadRestService.selectOrders(lotteryId, issue, OrderStatus.NORMAL);
        // 判空处理
        if (CollectionUtils.isEmpty(orderRecords)) {
            return;
        }
        Map<Integer, OrderRecord> orderMap = new HashMap<>();
        // 获取相关订单id集合
        List<Integer> orderIds = new ArrayList<>();
        for (OrderRecord order : orderRecords) {
            Integer orderId = order.getId();
            orderMap.put(orderId, order);
            orderIds.add(orderId);
            order.setOpenNumber(number);
            orderRecordMapper.updateByPrimaryKeySelective(order);
        }
        // 查询所有所有相关投注信息
        List<Integer> playIds = new ArrayList<>();
        playIds.add(playId);
        List<OrderBetRecord> orderBetRecords = orderReadRestService.selectOrderBets(orderIds, playIds, OrderBetStatus.WAIT);
        // 判空处理
        if (CollectionUtils.isEmpty(orderBetRecords)) {
            return;
        }
        // 获取赔率因子
        double divisor = betCommonService.getDivisor(lotteryId);
        // 获取配置id
        Integer settingId = orderBetRecords.get(0).getSettingId();
        // 获取所有赔率信息
        Map<String, LotteryPlayOdds> oddsMap = new HashMap<>();
        LotteryPlayOdds odds = new LotteryPlayOdds();
        if (playId == PLAY_ID_TM || playId == PLAY_ID_SB || playId == PLAY_ID_HH) {
            // 特码或者色波或者混合
            oddsMap = lotteryPlayOddsService.selectPlayOddsBySettingId(settingId);
        } else if (playId == PLAY_ID_BZ || playId == PLAY_ID_TMBS) {
            //豹子或者特码包三
            odds = lotteryPlayOddsService.findPlayOddsBySettingId(settingId);
        }

        for (OrderBetRecord orderBet : orderBetRecords) {
            try {
                BigDecimal winAmount = BigDecimal.ZERO;
                orderBet.setWinCount(Constants.STR_ZERO);
                // 判断是否中奖
                StringBuilder betNumber = new StringBuilder();
                betNumber.append(orderBet.getPlayName()).append("@").append(orderBet.getBetNumber());
                String winNum = PceggUtil.isWin(betNumber.toString(), number, playId);
                if (StringUtils.isNotBlank(winNum)) {
                    orderBet.setWinCount(Constants.STRING_ONE);
                    // 获取赔率信息
                    if (playId == PLAY_ID_HH) {
                        //混合
                        String[] winArr = winNum.split(",");
                        for (String winStr : winArr) {
                            odds = oddsMap.get(winStr);
                            // 获取总注数/中奖注数
                            String winCount = odds.getWinCount();
                            String totalCount = odds.getTotalCount();
                            // 计算赔率
                            double odd = Double.parseDouble(totalCount) * 1.0 / Double.parseDouble(winCount) * divisor;
                            winAmount = winAmount.add(orderBet.getBetAmount().divide(BigDecimal.valueOf(orderBet.getBetCount())).multiply(BigDecimal.valueOf(odd)));
                        }
                    } else {
                        if (playId == PLAY_ID_TM || playId == PLAY_ID_SB) {
                            //特码或者色波
                            odds = oddsMap.get(winNum);
                        }
                        // 获取总注数/中奖注数
                        String winCount = odds.getWinCount();
                        String totalCount = odds.getTotalCount();
                        // 计算赔率
                        double odd = Double.parseDouble(totalCount) * 1.0 / Double.parseDouble(winCount) * divisor;
                        if (playId == PLAY_ID_TMBS) {
                            orderBet.setWinCount(winNum);
                            winAmount = new BigDecimal(winNum).multiply(BigDecimal.valueOf(odd));
                        } else {
                            winAmount = orderBet.getBetAmount().divide(BigDecimal.valueOf(orderBet.getBetCount())).multiply(BigDecimal.valueOf(odd));
                        }
                    }
                }
                // 获取投注单
                OrderRecord orderRecord = orderMap.get(orderBet.getOrderId());


                try {
                    betCommonService.winOrLose(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
                } catch (TransactionSystemException e1) {
                    logger.error("德州pc蛋蛋订单结算出错 事务冲突 进行重试。orderSn:{},issue{},playName{},betNumber{}", orderRecord.getOrderSn(), orderRecord.getIssue(), orderBet.getPlayName(), orderBet.getBetNumber(), e1);
                    for (int i = 0; i < 20; i++) {
                        try {
                            betCommonService.winOrLose(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
                        } catch (TransactionSystemException e2) {
                            logger.error("德州pc蛋蛋订单结算出错 事务冲突 进行重试。orderSn:{},issue{},playName{},betNumber{}", orderRecord.getOrderSn(), orderRecord.getIssue(), orderBet.getPlayName(), orderBet.getBetNumber(), e2);
                            Thread.sleep(100);
                            continue;
                        }
                        break;
                    }
                }

                /** 追号 */
                orderNewAppendWriteService.appendOrder(orderRecord, winAmount, StringUtils.isNotBlank(winNum));
            } catch (Exception e) {
                logger.error("德州pc蛋蛋订单结算出错 issue{},playName{},betNumber{}", issue, orderBet.getPlayName(), orderBet.getBetNumber(), e);
            }

        }
    }


}
