package com.caipiao.live.order.service.bet.impl;


import com.caipiao.live.common.service.read.OrderReadRestService;
import com.caipiao.live.order.service.bet.BetActAzService;
import com.caipiao.live.order.service.bet.BetCommonService;
import com.caipiao.live.order.service.lottery.LotteryPlayOddsWriteService;
import com.caipiao.live.order.service.lottery.LotteryPlayWriteService;
import com.caipiao.live.order.service.order.OrderAppendWriteService;
import com.caipiao.live.common.model.dto.order.OrderBetStatus;
import com.caipiao.live.common.model.dto.order.OrderStatus;
import com.caipiao.live.common.mybatis.entity.LotteryPlay;
import com.caipiao.live.common.mybatis.entity.LotteryPlayOdds;
import com.caipiao.live.common.mybatis.entity.OrderBetRecord;
import com.caipiao.live.common.mybatis.entity.OrderRecord;
import com.caipiao.live.common.mybatis.mapper.OrderRecordMapper;
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

@Service
public class BetActAzServiceImpl implements BetActAzService {

    private static Logger logger = LoggerFactory.getLogger(BetActAzServiceImpl.class);

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private LotteryPlayWriteService lotteryPlayWriteService;
    @Autowired
    private LotteryPlayOddsWriteService lotteryPlayOddsService;
    @Autowired
    private OrderAppendWriteService orderAppendWriteService;
    @Autowired
    private OrderRecordMapper orderRecordMapper;
    @Autowired
    private BetCommonService betCommonService;
    @Autowired
    private OrderReadRestService orderReadRestService;

    private final List<String> PLAY_IDS = Lists.newArrayList("01");

    @Override
    public void countAzAct(String issue, String number, int lotteryId) throws Exception {
        // 获取相应的订单信息
        List<OrderRecord> orderRecords = orderReadRestService.selectOrders(lotteryId, issue, OrderStatus.NORMAL);
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
        List<Integer> playIds = this.generationSSCPlayIdList(PLAY_IDS, lotteryId);
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
                BigDecimal winAmount = new BigDecimal(0);
                // 获取赔率信息
                LotteryPlayOdds odds = oddsMap.get(orderBet.getSettingId());
                if (orderBet.getPlayId() == 220101) {  //220101:  表示根据name 的不同有不同的赔率
                    Map<String, LotteryPlayOdds> oddsHeMap = lotteryPlayOddsService.selectPlayOddsBySettingId(orderBet.getSettingId());
                    odds = oddsHeMap.get(orderBet.getBetNumber().split("@")[1]);
                }
                if (odds == null) {
                    continue;
                }
                orderBet.setWinCount("0");
                // 判断是否中奖
                String winNum = this.isWin(orderBet.getBetNumber(), number);
                if (StringUtils.isNotBlank(winNum)) {
                    // 获取总注数/中奖注数
                    String winCount = odds.getWinCount();
                    String totalCount = odds.getTotalCount();

                    // 计算赔率
                    double odd = Double.parseDouble(totalCount) * 1.0 / Double.parseDouble(winCount) * divisor;
                    //一注的中奖额
                    winAmount = orderBet.getBetAmount().divide(BigDecimal.valueOf(orderBet.getBetCount())).multiply(BigDecimal.valueOf(odd));
                    orderBet.setWinCount("1");
                }

                // 根据中奖金额,修改投注信息及相关信息
                OrderRecord orderRecord = orderMap.get(orderBet.getOrderId());

                try {
                    betCommonService.winOrLose(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
                } catch (TransactionSystemException e1) {
                    logger.error("订单结算出错 事务冲突 进行重试。orderSn:{}.", orderRecord.getOrderSn(), e1);
                    for (int i = 0; i < 20; i++) {
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
                orderAppendWriteService.appendOrder(orderRecord, winAmount, StringUtils.isNotBlank(winNum));
            } catch (Exception e) {
                logger.error("结算出错：issue" + "," + issue, e);
            }


        }
    }


    /**
     * 澳洲牛牛 玩法ID生成
     *
     * @param playNumbers
     * @param lotteryId
     * @return
     */
    private List<Integer> generationSSCPlayIdList(List<String> playNumbers, Integer lotteryId) {
        List<Integer> replaceToNewPlayId = new ArrayList<Integer>();
        for (String number : playNumbers) {
            replaceToNewPlayId.add(Integer.parseInt(lotteryId + number));
        }
        return replaceToNewPlayId;
    }

    /**
     * 判断澳洲牛牛是否中奖,中奖返回中奖信息,不中则返回null
     * (适用玩法:闲一，闲二，闲三，闲四，闲五)
     *
     * @param betNum
     * @param sg
     * @return
     */
    private String isWin(String betNum, String sg) {
        //中奖
        if (winOrNot(betNum, sg)) {
            return betNum;
        }
        return null;
    }

    //判断输赢
    //澳洲牛牛比大小
    //无牛《牛一《牛二《牛三《牛四《牛五《牛六《牛七《牛八《牛九《牛牛
    //对应结果数字0-20(牛牛设置为20)
    private static boolean winOrNot(String betNum, String sg) {

        String[] sgArray = sg.split(",");
        String type = betNum.split("@")[1];
        int sum = 0;
        for (int i = 0; i < 20; i++) {
            sum = sum + Integer.valueOf(sgArray[i]);
        }
        if ("大".equals(type)) {
            if (sum > 810) {
                return true;
            }
        } else if ("小".equals(type)) {
            if (sum < 810) {
                return true;
            }
        } else if ("和".equals(type)) {
            if (sum == 810) {
                return true;
            }
        } else if ("单".equals(type)) {
            if (sum % 2 != 0) {
                return true;
            }
        } else if ("双".equals(type)) {
            if (sum % 2 == 0) {
                return true;
            }
        } else if ("大单".equals(type)) {
            if (sum > 810 && sum % 2 != 0) {
                return true;
            }
        } else if ("小单".equals(type)) {
            if (sum < 810 && sum % 2 != 0) {
                return true;
            }
        } else if ("大双".equals(type)) {
            if (sum > 810 && sum % 2 == 0) {
                return true;
            }
        } else if ("小双".equals(type)) {
            if (sum < 810 && sum % 2 == 0) {
                return true;
            }
        } else if ("金".equals(type)) {
            if (sum >= 210 && sum <= 695) {
                return true;
            }
        } else if ("木".equals(type)) {
            if (sum >= 696 && sum <= 763) {
                return true;
            }
        } else if ("水".equals(type)) {
            if (sum >= 764 && sum <= 855) {
                return true;
            }
        } else if ("火".equals(type)) {
            if (sum >= 856 && sum <= 923) {
                return true;
            }
        } else if ("土".equals(type)) {
            if (sum >= 924 && sum <= 1410) {
                return true;
            }
        }
        return false;
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

}
