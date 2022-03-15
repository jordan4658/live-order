package com.caipiao.live.order.service.bet.impl;

import com.caipiao.live.common.model.dto.order.OrderBetStatus;
import com.caipiao.live.common.model.dto.order.OrderStatus;
import com.caipiao.live.common.mybatis.entity.LotteryPlay;
import com.caipiao.live.common.mybatis.entity.LotteryPlayOdds;
import com.caipiao.live.common.mybatis.entity.OrderBetRecord;
import com.caipiao.live.common.mybatis.entity.OrderRecord;
import com.caipiao.live.common.mybatis.mapper.OrderRecordMapper;
import com.caipiao.live.common.service.read.OrderReadRestService;
import com.caipiao.live.order.service.bet.BetCommonService;
import com.caipiao.live.order.service.bet.BetF1AzService;
import com.caipiao.live.order.service.lottery.LotteryPlayOddsWriteService;
import com.caipiao.live.order.service.lottery.LotteryPlayWriteService;
import com.caipiao.live.order.service.order.OrderAppendWriteService;
import com.google.common.collect.Lists;
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

@Service
public class BetF1AzServiceImpl implements BetF1AzService {
    private static Logger logger = LoggerFactory.getLogger(BetF1AzServiceImpl.class);

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

    //两面
    private final Integer PLAY_ID_LM = 220301;

    //第一球
    private final Integer PLAY_ID_1 = 220305;
    //第二球
    private final Integer PLAY_ID_2 = 220306;
    //第三球
    private final Integer PLAY_ID_3 = 220307;
    //第四球
    private final Integer PLAY_ID_4 = 220308;
    //第五球
    private final Integer PLAY_ID_5 = 220309;
    //第六球
    private final Integer PLAY_ID_6 = 220310;
    //第七球
    private final Integer PLAY_ID_7 = 220311;
    //第八球
    private final Integer PLAY_ID_8 = 220312;
    //第九球
    private final Integer PLAY_ID_9 = 220313;
    //第十球
    private final Integer PLAY_ID_10 = 220314;

    private final List<String> PLAY_IDS = Lists.newArrayList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14");

    @Override
    public void countAzF1(String issue, String number, int lotteryId) throws Exception {
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

                // 获取玩法信息
                LotteryPlay play = playMap.get(orderBet.getPlayId());
                // 获取赔率信息
                LotteryPlayOdds odds = oddsMap.get(orderBet.getSettingId());
                List<Integer> playIdList = new ArrayList<>();

                //220301:两面 //220302:冠亚和，220305：第一球  220306：第二球  220307：第三球 220308：第四球  220309：第五球  220310：第六球
                // 220311：第七球  220312：第八球  220313：第九球  220314：第十球
                playIdList.add(220301);
                playIdList.add(220302);
                playIdList.add(220305);
                playIdList.add(220306);
                playIdList.add(220307);
                playIdList.add(220308);
                playIdList.add(220309);
                playIdList.add(220310);
                playIdList.add(220311);
                playIdList.add(220312);
                playIdList.add(220313);
                playIdList.add(220314);
                if (playIdList.contains(orderBet.getPlayId())) {
                    Map<String, LotteryPlayOdds> oddsHeMap = lotteryPlayOddsService.selectPlayOddsBySettingId(orderBet.getSettingId());
                    odds = oddsHeMap.get(orderBet.getBetNumber().split("@")[1]);  //斗牛
                }
                orderBet.setWinCount("0");
                // 判断是否中奖
                String winNum = this.isWin(orderBet.getBetNumber(), number, orderBet.getPlayId());
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
                    logger.error("订单结算出错 事务冲突 进行重试:{},{}", orderRecord.getOrderSn(), e1);
                    for (int i = 0; i < 20; i++) {
                        try {
                            betCommonService.winOrLose(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
                        } catch (TransactionSystemException e2) {
                            logger.error("订单结算出错:{},事务冲突 进行重试", i, orderRecord.getOrderSn(), e2);
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
     * 判断澳洲F1是否中奖,中奖返回中奖信息,不中则返回null
     *
     * @param betNum
     * @param sg
     * @return
     */
    private String isWin(String betNum, String sg, Integer playId) {
        //中奖
        if (winOrNot(betNum, sg, playId)) {
            return betNum;
        }

        return null;
    }

    //判断输赢
    //澳洲F1比大小
    private boolean winOrNot(String betNum, String sg, Integer playId) {
        String[] sgArray = sg.split(",");
        Integer[] sgIntArray = new Integer[10];
        for (int i = 0; i < sgArray.length; i++) {
            sgIntArray[i] = Integer.valueOf(sgArray[i]);
        }
//        try{
        return winByBigSmall(betNum, sg.split(","), playId);
//        }catch (Exception e){
//        }
//        return false;
    }

    /*
       两面算法： 所选号码 与开奖号码对应，则为中奖
       num:  选择号码
       sgArray:   开奖号码数组
     */
    private boolean winByBigSmall(String betNum, String[] sgArray, Integer playId) {
        String num = betNum.split("@")[1];
        int sumGuanya = Integer.valueOf(sgArray[0]) + Integer.valueOf(sgArray[1]);
        int first = Integer.valueOf(sgArray[0]);
        int second = Integer.valueOf(sgArray[1]);
        int third = Integer.valueOf(sgArray[2]);
        int four = Integer.valueOf(sgArray[3]);
        int five = Integer.valueOf(sgArray[4]);
        int six = Integer.valueOf(sgArray[5]);
        int seven = Integer.valueOf(sgArray[6]);
        int eight = Integer.valueOf(sgArray[7]);
        int nine = Integer.valueOf(sgArray[8]);
        int ten = Integer.valueOf(sgArray[9]);

        if (betNum.contains("冠军") || playId.equals(PLAY_ID_1) || betNum.contains("第一名") || betNum.contains("第1名")) {
            if (num.contains("单")) {
                if (first % 2 != 0) {
                    return true;
                }
            } else if (num.contains("双")) {
                if (first % 2 == 0) {
                    return true;
                }
            } else if (num.contains("大")) {
                if (first >= 6) {
                    return true;
                }
            } else if (num.contains("小")) {
                if (first <= 5) {
                    return true;
                }
            } else if (num.contains("龙")) {
                if (first > ten) {
                    return true;
                }
            } else if (num.contains("虎")) {
                if (first < ten) {
                    return true;
                }
            } else {  //冠军数字的情况，1-10
                if (Integer.valueOf(num).equals(first)) {
                    return true;
                }
            }
        } else if (betNum.contains("亚军") || playId.equals(PLAY_ID_2) || betNum.contains("第二名") || betNum.contains("第2名")) {
            if (num.contains("单")) {
                if (second % 2 != 0) {
                    return true;
                }
            } else if (num.contains("双")) {
                if (second % 2 == 0) {
                    return true;
                }
            } else if (num.contains("大")) {
                if (second >= 6) {
                    return true;
                }
            } else if (num.contains("小")) {
                if (second <= 5) {
                    return true;
                }
            } else if (num.contains("龙")) {
                if (second > nine) {
                    return true;
                }
            } else if (num.contains("虎")) {
                if (second < nine) {
                    return true;
                }
            } else {  //亚军数字的情况，1-10
                if (Integer.valueOf(num).equals(second)) {
                    return true;
                }
            }
        } else if (betNum.contains("季军") || betNum.contains("第三名") || playId.equals(PLAY_ID_3) || betNum.contains("第3名")) {
            if (num.contains("单")) {
                if (third % 2 != 0) {
                    return true;
                }
            } else if (num.contains("双")) {
                if (third % 2 == 0) {
                    return true;
                }
            } else if (num.contains("大")) {
                if (third >= 6) {
                    return true;
                }
            } else if (num.contains("小")) {
                if (third <= 5) {
                    return true;
                }
            } else if (num.contains("龙")) {
                if (third > eight) {
                    return true;
                }
            } else if (num.contains("虎")) {
                if (third < eight) {
                    return true;
                }
            } else {  //第三名数字的情况，1-10
                if (Integer.valueOf(num).equals(third)) {
                    return true;
                }
            }
        } else if (betNum.contains("第四名") || playId.equals(PLAY_ID_4) || betNum.contains("第4名")) {
            if (num.contains("单")) {
                if (four % 2 != 0) {
                    return true;
                }
            } else if (num.contains("双")) {
                if (four % 2 == 0) {
                    return true;
                }
            } else if (num.contains("大")) {
                if (four >= 6) {
                    return true;
                }
            } else if (num.contains("小")) {
                if (four <= 5) {
                    return true;
                }
            } else if (num.contains("龙")) {
                if (four > seven) {
                    return true;
                }
            } else if (num.contains("虎")) {
                if (four < seven) {
                    return true;
                }
            } else {  //第四名数字的情况，1-10
                if (Integer.valueOf(num).equals(four)) {
                    return true;
                }
            }
        } else if (betNum.contains("第五名") || playId.equals(PLAY_ID_5) || betNum.contains("第5名")) {
            if (num.contains("单")) {
                if (five % 2 != 0) {
                    return true;
                }
            } else if (num.contains("双")) {
                if (five % 2 == 0) {
                    return true;
                }
            } else if (num.contains("大")) {
                if (five >= 6) {
                    return true;
                }
            } else if (num.contains("小")) {
                if (five <= 5) {
                    return true;
                }
            } else if (num.contains("龙")) {
                if (five > six) {
                    return true;
                }
            } else if (num.contains("虎")) {
                if (five < six) {
                    return true;
                }
            } else {  //第5名数字的情况，1-10
                if (Integer.valueOf(num).equals(five)) {
                    return true;
                }
            }
        } else if (betNum.contains("第六名") || playId.equals(PLAY_ID_6) || betNum.contains("第6名")) {
            if (num.contains("单")) {
                if (six % 2 != 0) {
                    return true;
                }
            } else if (num.contains("双")) {
                if (six % 2 == 0) {
                    return true;
                }
            } else if (num.contains("大")) {
                if (six >= 6) {
                    return true;
                }
            } else if (num.contains("小")) {
                if (six <= 5) {
                    return true;
                }
            } else {  //第6名数字的情况，1-10
                if (Integer.valueOf(num).equals(six)) {
                    return true;
                }
            }
        } else if (betNum.contains("第七名") || playId.equals(PLAY_ID_7) || betNum.contains("第7名")) {
            if (num.contains("单")) {
                if (seven % 2 != 0) {
                    return true;
                }
            } else if (num.contains("双")) {
                if (seven % 2 == 0) {
                    return true;
                }
            } else if (num.contains("大")) {
                if (seven >= 6) {
                    return true;
                }
            } else if (num.contains("小")) {
                if (seven <= 5) {
                    return true;
                }
            } else {  //第7名数字的情况，1-10
                if (Integer.valueOf(num).equals(seven)) {
                    return true;
                }
            }
        } else if (betNum.contains("第八名") || playId.equals(PLAY_ID_8) || betNum.contains("第8名")) {
            if (num.contains("单")) {
                if (eight % 2 != 0) {
                    return true;
                }
            } else if (num.contains("双")) {
                if (eight % 2 == 0) {
                    return true;
                }
            } else if (num.contains("大")) {
                if (eight >= 6) {
                    return true;
                }
            } else if (num.contains("小")) {
                if (eight <= 5) {
                    return true;
                }
            } else {  //第8名数字的情况，1-10
                if (Integer.valueOf(num).equals(eight)) {
                    return true;
                }
            }
        } else if (betNum.contains("第九名") || playId.equals(PLAY_ID_9) || betNum.contains("第9名")) {
            if (num.contains("单")) {
                if (nine % 2 != 0) {
                    return true;
                }
            } else if (num.contains("双")) {
                if (nine % 2 == 0) {
                    return true;
                }
            } else if (num.contains("大")) {
                if (nine >= 6) {
                    return true;
                }
            } else if (num.contains("小")) {
                if (nine <= 5) {
                    return true;
                }
            } else {  //第9名数字的情况，1-10
                if (Integer.valueOf(num).equals(nine)) {
                    return true;
                }
            }
        } else if (betNum.contains("第十名") || playId.equals(PLAY_ID_10) || betNum.contains("第10名")) {
            if (num.contains("单")) {
                if (ten % 2 != 0) {
                    return true;
                }
            } else if (num.contains("双")) {
                if (ten % 2 == 0) {
                    return true;
                }
            } else if (num.contains("大")) {
                if (ten >= 6) {
                    return true;
                }
            } else if (num.contains("小")) {
                if (ten <= 5) {
                    return true;
                }
            } else {  //第10名数字的情况，1-10
                if (Integer.valueOf(num).equals(ten)) {
                    return true;
                }
            }
        } else if (betNum.contains("冠亚和") || playId.equals(PLAY_ID_LM)) {
            if (num.contains("冠亚大")) {
                if (sumGuanya >= 12) {
                    return true;
                }
            } else if (num.contains("冠亚小")) {
                if (sumGuanya <= 11) {
                    return true;
                }
            } else if (num.contains("冠亚单")) {
                if (sumGuanya % 2 != 0) {
                    return true;
                }
            } else if (num.contains("冠亚双")) {
                if (sumGuanya % 2 == 0) {
                    return true;
                }
            } else {  //冠亚和的另外情况，和3-19
                if (Integer.valueOf(num) == sumGuanya) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<Integer> generationSSCPlayIdList(List<String> playNumbers, Integer lotteryId) {
        List<Integer> replaceToNewPlayId = new ArrayList<Integer>();
        for (String number : playNumbers) {
            replaceToNewPlayId.add(Integer.parseInt(lotteryId + number));
        }
        return replaceToNewPlayId;
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
