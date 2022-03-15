package com.caipiao.live.order.service.bet.impl;

import com.caipiao.live.common.service.read.OrderReadRestService;
import com.caipiao.live.order.service.bet.BetCommonService;
import com.caipiao.live.order.service.bet.BetXyftService;
import com.caipiao.live.order.service.lottery.LotteryPlayOddsWriteService;
import com.caipiao.live.order.service.lottery.LotteryPlayWriteService;
import com.caipiao.live.order.service.order.OrderAppendWriteService;
import com.caipiao.live.order.service.order.OrderWriteService;
import com.caipiao.live.order.service.result.XyftLotterySgWriteService;
import com.caipiao.live.common.model.dto.order.OrderBetStatus;
import com.caipiao.live.common.model.dto.order.OrderStatus;
import com.caipiao.live.common.mybatis.entity.*;
import com.caipiao.live.common.mybatis.mapper.OrderRecordMapper;
import com.caipiao.live.common.mybatis.mapper.XyftLotterySgMapper;
import com.caipiao.live.common.mybatis.mapperbean.XyftBeanMapper;
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
public class BetXyftServiceImpl implements BetXyftService {
    private static Logger logger = LoggerFactory.getLogger(BetXyftServiceImpl.class);
    private final Integer lotteryId = 1401;

    @Autowired
    private OrderRecordMapper orderRecordMapper;
    @Autowired
    private OrderWriteService orderWriteService;
    @Autowired
    private LotteryPlayWriteService lotteryPlayWriteService;
    @Autowired
    private XyftLotterySgMapper xyftLotterySgMapper;
    @Autowired
    private LotteryPlayOddsWriteService lotteryPlayOddsService;
    @Autowired
    private BetCommonService betCommonService;
    @Autowired
    private OrderAppendWriteService orderAppendWriteService;
    @Autowired
    private XyftLotterySgWriteService xyftLotterySgWriteService;
    @Autowired
    private XyftBeanMapper xyftBeanMapper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private OrderReadRestService orderReadRestService;

    private static final String ORDER_KEY = "XYFT_ORDER_";
    private static final String XYFT_LOTTERY_SG = "XYFT_LOTTERY_SG_";

//    // PK10猜名次猜前几
//    private final List<Integer> PLAY_IDS_CMC_CQJ = Lists.newArrayList(136, 137, 139, 142, 144);
//    // PK10单式猜前几
//    private final List<Integer> PLAY_IDS_DS_CQJ = Lists.newArrayList(138, 141, 143, 145);
//    // PK10单式定位胆
//    private final List<Integer> PLAY_IDS_DWD = Lists.newArrayList(146, 147);


    // PK10两面
    private final int PLAY_ID_LM = 140101;
    // PK10冠亚和
    private final int PLAY_ID_GYH = 140102;

    // PK10 1-5名
    private final int PLAY_ID_15 = 140103;
    // PK10 6-10名
    private final int PLAY_ID_610 = 140104;
    // PK10 第一名
    private final int PLAY_ID_1 = 140105;
    // PK10 第二名
    private final int PLAY_ID_2 = 140106;
    // PK10 第三名
    private final int PLAY_ID_3 = 140107;
    // PK10 第四名
    private final int PLAY_ID_4 = 140108;
    // PK10 第五名
    private final int PLAY_ID_5 = 140109;
    // PK10 第六名
    private final int PLAY_ID_6 = 140110;
    // PK10 第七名
    private final int PLAY_ID_7 = 140111;
    // PK10 第八名
    private final int PLAY_ID_8 = 140112;
    // PK10 第九名
    private final int PLAY_ID_9 = 140113;
    // PK10 第十名
    private final int PLAY_ID_10 = 140114;


    private final List<Integer> PLAY_IDS_110 = Lists.newArrayList(140103, 140104, 140105, 140106, 140107, 140108, 140109, 140110, 140111, 140112, 140113, 140114);

    @Override
    public void clearingXyftCmcCqj(String issue, String number) throws Exception {
        // clearingXyftCqj(issue, number, PLAY_IDS_CMC_CQJ);
        clearingXyftCqj(issue, number, PLAY_IDS_110);
    }

    @Override
    public void clearingXyftDsCqj(String issue, String number) throws Exception {
        //  clearingXyftCqj(issue, number, PLAY_IDS_DS_CQJ);
    }

    @Override
    public void clearingXyftDwd(String issue, String number) throws Exception {
        //   clearingXyftCqj(issue, number, PLAY_IDS_DWD);
    }

    @Override
    public void clearingXyftGyh(String issue, String number) throws Exception {
        clearingXyftOnePlayManyOdds(issue, number, PLAY_ID_GYH);
    }

    @Override
    public void clearingXyftLm(String issue, String number) throws Exception {
        clearingXyftOnePlayManyOdds(issue, number, PLAY_ID_LM);
    }

    private void clearingXyftCqj(String issue, String number, List<Integer> playIds) {
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
                BigDecimal winAmount = new BigDecimal(0);
                // 获取玩法信息
                LotteryPlay play = playMap.get(orderBet.getPlayId());
                // 获取赔率信息
                LotteryPlayOdds odds = oddsMap.get(orderBet.getSettingId());
                //140105：第一名--- 140114：第十名
                if (orderBet.getPlayId() == 140105 || orderBet.getPlayId() == 140106 || orderBet.getPlayId() == 140107 || orderBet.getPlayId() == 140108 || orderBet.getPlayId() == 140109
                        || orderBet.getPlayId() == 140110 || orderBet.getPlayId() == 140111 || orderBet.getPlayId() == 140112 || orderBet.getPlayId() == 140113 || orderBet.getPlayId() == 140114) {
                    Map<String, LotteryPlayOdds> oddsHeMap = lotteryPlayOddsService.selectPlayOddsBySettingId(orderBet.getSettingId());
                    odds = oddsHeMap.get(orderBet.getBetNumber().split("@")[1]);
                }
                orderBet.setWinCount("0");
                // 判断是否中奖
                String winNum = XyftUtils.isWin(orderBet.getBetNumber(), number, play);
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
                    logger.error("订单结算出错 事务冲突 进行重试:{},{}", orderRecord.getOrderSn(), e1);
                    for (int i = 0; i < 20; i++) {
                        try {
                            betCommonService.winOrLose(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
                        } catch (TransactionSystemException e2) {
                            logger.error("订单结算出错:{},事务冲突 进行重试,订单号：{}，{}", i, orderRecord.getOrderSn(), e2);
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

    private void clearingXyftOnePlayManyOdds(String issue, String number, Integer playId) throws Exception {
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
        List<OrderBetRecord> orderBetRecords = orderReadRestService.selectOrderBetsSinglePlay(orderIds, playId, OrderBetStatus.WAIT);
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
                BigDecimal winAmount = new BigDecimal(0);
                // 判断是否中奖
                String winNum = XyftUtils.isWinLmAndGyh(orderBet.getBetNumber(), number, playId);
                orderBet.setWinCount("0");
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
                    logger.error("订单结算出错 事务冲突 进行重试:{},{}", orderRecord.getOrderSn(), e1);
                    for (int i = 0; i < 20; i++) {
                        try {
                            betCommonService.winOrLose(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
                        } catch (TransactionSystemException e2) {
                            logger.error("订单结算出错:{},事务冲突 进行重试:{},{}", i, orderRecord.getOrderSn(), e2);
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
     * 根据期号获取赛果
     *
     * @param issue 期号
     * @return
     */
    private XyftLotterySg getLotterySg(String issue) {
        // 获取该期赛果信息
        XyftLotterySg sg = sg = (XyftLotterySg) redisTemplate.opsForValue().get(XYFT_LOTTERY_SG + issue);
        if (sg == null) {
            XyftLotterySgExample sgExample = new XyftLotterySgExample();
            XyftLotterySgExample.Criteria sgCriteria = sgExample.createCriteria();
            sgCriteria.andIssueEqualTo(issue);
            sg = xyftLotterySgMapper.selectOneByExample(sgExample);
            redisTemplate.opsForValue().set(XYFT_LOTTERY_SG + issue, sg, 2, TimeUnit.MINUTES);
        }
        return sg;
    }

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
