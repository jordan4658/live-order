package com.caipiao.live.order.service.bet.impl;


import com.caipiao.live.common.service.read.OrderReadRestService;
import com.caipiao.live.order.service.bet.BetKsService;
import com.caipiao.live.order.service.bet.BetCommonService;
import com.caipiao.live.order.service.lottery.LotteryPlayOddsWriteService;
import com.caipiao.live.order.service.lottery.LotteryPlayWriteService;
import com.caipiao.live.order.service.order.OrderNewAppendWriteService;
import com.caipiao.live.order.service.order.OrderNewWriteService;
import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.model.dto.order.OrderBetStatus;
import com.caipiao.live.common.model.dto.order.OrderStatus;
import com.caipiao.live.common.mybatis.entity.LotteryPlayOdds;
import com.caipiao.live.common.mybatis.entity.OrderBetRecord;
import com.caipiao.live.common.mybatis.entity.OrderRecord;
import com.caipiao.live.common.mybatis.mapper.OrderRecordMapper;
import com.caipiao.live.common.util.redis.RedisBusinessUtil;
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
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @Date:Created in 22:122019/12/5
 * @Descriotion
 * @Author
 **/
@Service
public class BetKsServiceImpl implements BetKsService {
    private static Logger logger = LoggerFactory.getLogger(BetKsServiceImpl.class);
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private OrderNewWriteService orderWriteService;
    @Autowired
    private LotteryPlayWriteService lotteryPlayWriteService;
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

    private static final String ORDER_KEY = "AZKS_ORDER_";
    private static final String AZKS_LOTTERY_SG = "AZKS_LOTTERY_SG_";
    // 澳洲快三 大小和值
    private final String PLAY_ID_LM = "01";
    private final String PLAY_ID_DD = "02";
    private final String PLAY_ID_LH = "03";
    private final String PLAY_ID_EBT = "04";
    private final String PLAY_ID_EBT_DT = "05";
    private final String PLAY_ID_ET_DX = "06";
    private final String PLAY_ID_ET_FX = "07";
    private final String PLAY_ID_SBT = "08";
    private final String PLAY_ID_SBT_DT = "09";
    private final String PLAY_ID_ST_DX = "10";
    private final String PLAY_ID_ST_TX = "11";
    private final List<String> PLAY_IDS_SBT_ST = Lists.newArrayList(PLAY_ID_SBT, PLAY_ID_SBT_DT, PLAY_ID_ST_DX, PLAY_ID_ST_TX);
    private final List<String> PLAY_IDS_EBT_ET = Lists.newArrayList(PLAY_ID_EBT, PLAY_ID_EBT_DT, PLAY_ID_ET_DX, PLAY_ID_ET_FX);


    //两面
    @Override
    public void clearingKsLm(String issue, String number, int lotteryId) {
        clearingKsLmPlay(issue, number, this.generationKsPlayId(PLAY_ID_LM, lotteryId), lotteryId);
    }

    //独胆
    @Override
    public void clearingKsDd(String issue, String number, int lotteryId) {
        clearingKsDdPlay(issue, number, this.generationKsPlayId(PLAY_ID_DD, lotteryId), lotteryId);
    }

    //连号
    @Override
    public void clearingKsLh(String issue, String number, int lotteryId) {
        clearingKsLhPlay(issue, number, this.generationKsPlayId(PLAY_ID_LH, lotteryId), lotteryId);
    }

    //三不同号，胆拖，通选，单选
    @Override
    public void clearingKsSbTh(String issue, String number, int lotteryId) {
        clearingKsSbThAndSthPlay(issue, number, this.generationKsPlayIdList(PLAY_IDS_SBT_ST, lotteryId), lotteryId);
    }

    //二不同号，胆拖，复选，单选
    @Override
    public void clearingKsEbTh(String issue, String number, int lotteryId) {
        clearingKsEbThAndEthPlay(issue, number, this.generationKsPlayIdList(PLAY_IDS_EBT_ET, lotteryId), lotteryId);
    }

    /*结算二不同号，二不同号胆拖，二同号复选，二同号单选
     */
    private void clearingKsEbThAndEthPlay(String issue, String number, List<Integer> playIds, int lotteryId) {
        List<OrderRecord> orderRecords = getOrderRecord(issue, lotteryId);
        if (CollectionUtils.isEmpty(orderRecords)) {
            return;
        }
        Map<Integer, OrderRecord> orderMap = new HashMap<>();
        // 获取相关订单id集合 //更新订单开奖号码
        List<Integer> orderIds = new ArrayList<>();
        this.updateOrder(number, orderRecords, orderIds, orderMap);
        // 获取赔率因子
        double divisor = betCommonService.getDivisor(lotteryId);
        // 查询所有所有相关投注信息
        List<OrderBetRecord> orderBetRecords = orderReadRestService.selectOrderBets(orderIds, playIds, OrderBetStatus.WAIT);
        logger.info("快三系列二同号二不同号相关投注数量{},彩种id{}, 期号{}, 开奖号码{}", orderBetRecords.size(), lotteryId, issue, number);
        // 判空处理
        if (CollectionUtils.isEmpty(orderBetRecords)) {
            return;
        }
        // 获取相关玩法信息
//        Map<Integer, LotteryPlay> playMap = lotteryPlayWriteService.selectPlayByIds(playIds);
        // 获取所有配置id
//        List<Integer> settingIds = new ArrayList<>();
//        for (OrderBetRecord orderBet : orderBetRecords) {
//            settingIds.add(orderBet.getSettingId());
//        }
        // 获取所有赔率信息
        for (OrderBetRecord orderBet : orderBetRecords) {
            try {
                BigDecimal winAmount = BigDecimal.ZERO;
                // 获取玩法信息
//                LotteryPlay play = playMap.get(orderBet.getPlayId());
                // 根据settings获取赔率集合
                LotteryPlayOdds odds = new LotteryPlayOdds();

                try {
                    if (orderBet.getPlayId().equals(this.generationKsPlayId(PLAY_ID_EBT, lotteryId))) {
                        //通过投注订单的settingid 获取赔率表的 name  和  赔率
                        Map<String, LotteryPlayOdds> oddsHeMap = lotteryPlayOddsService.selectPlayOddsBySettingId(orderBet.getSettingId());
                        //赔率都一样,任选
                        odds = oddsHeMap.get("1");
                    } else if (orderBet.getPlayId().equals(this.generationKsPlayId(PLAY_ID_EBT_DT, lotteryId))) {
                        //通过投注订单的settingid 获取赔率表的 name  和  赔率
                        Map<String, LotteryPlayOdds> oddsHeMap = lotteryPlayOddsService.selectPlayOddsBySettingId(orderBet.getSettingId());
                        //二不同胆拖 settingid  30375
                        odds = oddsHeMap.get("胆拖");
                    } else if (orderBet.getPlayId().equals(this.generationKsPlayId(PLAY_ID_ET_DX, lotteryId))) {
                        //通过投注订单的settingid 获取赔率表的 name  和  赔率
                        Map<String, LotteryPlayOdds> oddsHeMap = lotteryPlayOddsService.selectPlayOddsBySettingId(orderBet.getSettingId());
                        //二同号单选的odds需要根据 name二同号单选获取
                        odds = oddsHeMap.get("单选");
                    } else if (orderBet.getPlayId().equals(this.generationKsPlayId(PLAY_ID_ET_FX, lotteryId))) {
                        //通过投注订单的settingid 获取赔率表的 name  和  赔率
                        Map<String, LotteryPlayOdds> oddsHeMap = lotteryPlayOddsService.selectPlayOddsBySettingId(orderBet.getSettingId());
                        //二同号复选
                        odds = oddsHeMap.get("复选");
                    }
                } catch (Exception e) {
                    logger.error("快三结算错误二不同号/胆拖/单选/复选根据name获取玩法赔率失败订单号" + orderBet.getOrderId() + "玩法" + orderBet.getPlayId() + "彩种" + lotteryId, e);
                }
                orderBet.setWinCount(Constants.STR_ZERO);
                // 判断是否中奖
                String winNum = null;
                try {
                    winNum = this.judgeWinEbThAndEthPlay(orderBet.getBetNumber(), number, orderBet.getPlayId(), lotteryId);
                } catch (Exception e) {
                    logger.error("快三结算错误二同号二不同号判断是否中奖错误彩种{},订单号{},用户id{},投注号码{}", lotteryId, orderBet.getOrderId(), orderBet.getUserId(), orderBet.getBetNumber(), e);
                }
                if (StringUtils.isNotBlank(winNum)) {
                    String[] winNumArr = winNum.split(Constants.STR_UNDERLINE);
                    String winNumArrLength = winNumArr.length + "";
                    for (String num : winNumArr) {
                        // 获取总注数/中奖注数
                        String winCount = odds.getWinCount();
                        String totalCount = odds.getTotalCount();
                        // 计算赔率
                        double odd = Double.parseDouble(totalCount) * 1.0 / Double.parseDouble(winCount) * divisor;
                        //一注的中奖额
                        winAmount = winAmount.add(orderBet.getBetAmount().divide(BigDecimal.valueOf(orderBet.getBetCount())).multiply(BigDecimal.valueOf(odd)));
                    }
                    orderBet.setWinCount(winNumArrLength);
                }
                logger.info("快三二同号二不同彩种{},订单号{},是否中奖{},奖金{}", lotteryId, orderBet.getOrderId(), winNum, winAmount);
                // 根据中奖金额,修改投注信息及相关信息
                OrderRecord orderRecord = orderMap.get(orderBet.getOrderId());
                try {
                    betCommonService.winOrLose(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
                } catch (TransactionSystemException e1) {
                    logger.error("快三结算错误二同号二不同号betCommonService结算出错 事务冲突 进行重试" + orderRecord.getOrderSn(), e1);
                    for (int i = 0; i < 20; i++) {
                        try {
                            betCommonService.winOrLose(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
                        } catch (TransactionSystemException e2) {
                            logger.error("快三结算错误二同号二不同号订单betCommonService出错" + i + ",事务冲突 进行重试" + orderRecord.getOrderSn(), e2);
                            Thread.sleep(100);
                            continue;
                        }
                        break;
                    }
                }
                /** 追号 */
                orderNewAppendWriteService.appendOrder(orderRecord, winAmount, StringUtils.isNotBlank(winNum));
            } catch (Exception e) {
                logger.error("快三结算错误二同号二不同号结算出错：期号：" + issue + "彩种：" + lotteryId, e);
            }
        }
    }


    /*结算三不同号，三不同号胆拖，三同号通选，三同号单选
     */
    private void clearingKsSbThAndSthPlay(String issue, String number, List<Integer> playIds, int lotteryId) {
        List<OrderRecord> orderRecords = getOrderRecord(issue, lotteryId);
        if (CollectionUtils.isEmpty(orderRecords)) {
            return;
        }
        Map<Integer, OrderRecord> orderMap = new HashMap<>();
        // 获取相关订单id集合 //更新订单开奖号码
        List<Integer> orderIds = new ArrayList<>();
        this.updateOrder(number, orderRecords, orderIds, orderMap);
        // 获取赔率因子
        double divisor = betCommonService.getDivisor(lotteryId);
        // 查询所有所有相关投注信息
        List<OrderBetRecord> orderBetRecords = orderReadRestService.selectOrderBets(orderIds, playIds, OrderBetStatus.WAIT);
        logger.info("快三系列三同号三不同号相关投注数量{},彩种id{}, 期号{}, 开奖号码{}", orderBetRecords.size(), lotteryId, issue, number);
        // 判空处理
        if (CollectionUtils.isEmpty(orderBetRecords)) {
            return;
        }
        // 获取相关玩法信息
//        Map<Integer, LotteryPlay> playMap = lotteryPlayWriteService.selectPlayByIds(playIds);
        // 获取所有配置id
//        List<Integer> settingIds = new ArrayList<>();
//        for (OrderBetRecord orderBet : orderBetRecords) {
//            settingIds.add(orderBet.getSettingId());
//        }
        // 获取所有赔率信息
//        Map<Integer, LotteryPlayOdds> oddsMap = lotteryPlayOddsService.selectPlayOddsBySettingIds(settingIds);
        for (OrderBetRecord orderBet : orderBetRecords) {
            try {
                BigDecimal winAmount = BigDecimal.ZERO;
                LotteryPlayOdds odds = new LotteryPlayOdds();
                orderBet.setWinCount(Constants.STR_ZERO);
                // 判断是否中奖
                String winNum = null;
                try {
                    winNum = this.judgeWinSbThAndSthPlay(orderBet.getBetNumber(), number, orderBet.getPlayId(), lotteryId);
                } catch (Exception e) {
                    logger.error("快三结算错误三同号三不同号判断是否中奖错误彩种{},订单号{},用户id{},投注号码{}", lotteryId, orderBet.getOrderId(), orderBet.getUserId(), orderBet.getBetNumber());
                }
                try {
                    if (orderBet.getPlayId().equals(this.generationKsPlayId(PLAY_ID_SBT, lotteryId))) {
                        //通过投注订单的settingid 获取赔率表的 name  和  赔率
                        Map<String, LotteryPlayOdds> oddsHeMap = lotteryPlayOddsService.selectPlayOddsBySettingId(orderBet.getSettingId());
                        //三不同号 对应的setting赔率都一样，任选一个
                        odds = oddsHeMap.get("三不同号");
                    } else if (orderBet.getPlayId().equals(this.generationKsPlayId(PLAY_ID_SBT_DT, lotteryId))) {
                        //通过投注订单的settingid 获取赔率表的 name  和  赔率
                        Map<String, LotteryPlayOdds> oddsHeMap = lotteryPlayOddsService.selectPlayOddsBySettingId(orderBet.getSettingId());
                        odds = oddsHeMap.get("胆拖");
                    } else if (orderBet.getPlayId().equals(this.generationKsPlayId(PLAY_ID_ST_DX, lotteryId))) {
                        //通过投注订单的settingid 获取赔率表的 name  和  赔率
                        Map<String, LotteryPlayOdds> oddsHeMap = lotteryPlayOddsService.selectPlayOddsBySettingId(orderBet.getSettingId());
                        //三同号 单选 对应的setting赔率都一样，任选一个
                        odds = oddsHeMap.get("单选");
                    } else if (orderBet.getPlayId().equals(this.generationKsPlayId(PLAY_ID_ST_TX, lotteryId))) {
                        //通过投注订单的settingid 获取赔率表的 name  和  赔率
                        Map<String, LotteryPlayOdds> oddsHeMap = lotteryPlayOddsService.selectPlayOddsBySettingId(orderBet.getSettingId());
                        odds = oddsHeMap.get("通选");
                    }
                } catch (Exception e) {
                    logger.error("快三结算错误三不同号/胆拖/单选/通选根据name获取玩法赔率失败" + orderBet.getOrderId(), e);
                }
                if (StringUtils.isNotBlank(winNum)) {
                    // 获取总注数/中奖注数
                    String winCount = odds.getWinCount();
                    String totalCount = odds.getTotalCount();
                    // 计算赔率
                    double odd = Double.parseDouble(totalCount) * 1.0 / Double.parseDouble(winCount) * divisor;
                    //一注的中奖额
                    winAmount = orderBet.getBetAmount().divide(BigDecimal.valueOf(orderBet.getBetCount())).multiply(BigDecimal.valueOf(odd));
                    orderBet.setWinCount(Constants.STRING_ONE);
                }
                // 根据中奖金额,修改投注信息及相关信息
                OrderRecord orderRecord = orderMap.get(orderBet.getOrderId());
                logger.info("快三三同号三不同号彩种{},订单号{},是否中奖{},奖金{}", lotteryId, orderBet.getOrderId(), winNum, winAmount);
                try {
                    betCommonService.winOrLose(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
                } catch (TransactionSystemException e1) {
                    logger.error("快三结算错误三同号三不同号betCommonService结算出错 事务冲突 进行重试" + orderRecord.getOrderSn(), e1);
                    for (int i = 0; i < 20; i++) {
                        try {
                            betCommonService.winOrLose(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
                        } catch (TransactionSystemException e2) {
                            logger.error("快三结算错误三同号三不同号订单betCommonService出错" + i + ",事务冲突 进行重试" + orderRecord.getOrderSn(), e2);
                            Thread.sleep(100);
                            continue;
                        }
                        break;
                    }
                }
                /** 追号 */
                orderNewAppendWriteService.appendOrder(orderRecord, winAmount, StringUtils.isNotBlank(winNum));
            } catch (Exception e) {
                logger.error("快三结算错误三同号三不同号结算出错：期号：" + issue + "彩种：" + lotteryId, e);
            }
        }

    }

    /*结算连号三连号二连号
     */
    private void clearingKsLhPlay(String issue, String number, int playId, int lotteryId) {
        List<OrderRecord> orderRecords = getOrderRecord(issue, lotteryId);
        if (CollectionUtils.isEmpty(orderRecords)) {
            return;
        }
        Map<Integer, OrderRecord> orderMap = new HashMap<>();
        // 获取相关订单id集合 //更新订单开奖号码
        List<Integer> orderIds = new ArrayList<>();
        this.updateOrder(number, orderRecords, orderIds, orderMap);
        // 获取赔率因子
        double divisor = betCommonService.getDivisor(lotteryId);
        //playId 放入集合 传参
        List<Integer> playIds = new ArrayList<>();
        playIds.add(playId);
        // 查询所有所有相关投注信息
        List<OrderBetRecord> orderBetRecords = orderReadRestService.selectOrderBets(orderIds, playIds, OrderBetStatus.WAIT);
        logger.info("快三系列连号相关投注数量{},彩种id{}, 期号{}, 开奖号码{}", orderBetRecords.size(), lotteryId, issue, number);
        // 判空处理
        if (CollectionUtils.isEmpty(orderBetRecords)) {
            return;
        }
        // 获取相关玩法信息
//        Map<Integer, LotteryPlay> playMap = lotteryPlayWriteService.selectPlayByIds(playIds);

        // 获取配置id
        Integer settingId = orderBetRecords.get(0).getSettingId();
        // 获取所有赔率信息
        for (OrderBetRecord orderBet : orderBetRecords) {
            try {
                BigDecimal winAmount = BigDecimal.ZERO;
                LotteryPlayOdds odds;
                orderBet.setWinCount(Constants.STR_ZERO);
                // 判断是否中奖
                String winNum = null;
                try {
                    winNum = this.judgeWinLh(orderBet.getBetNumber(), number, orderBet.getPlayId(), lotteryId);
                } catch (Exception e) {
                    logger.error("快三结算错误连号判断是否中奖错误彩种{},订单号{},用户id{},投注号码{}", lotteryId, orderBet.getOrderId(), orderBet.getUserId(), orderBet.getBetNumber());
                }
                if (orderBet.getPlayId().equals(this.generationKsPlayId(PLAY_ID_LH, lotteryId)) && StringUtils.isNotBlank(winNum)) {
                    //通过投注订单的settingid 获取赔率表的 name  和  赔率
                    Map<String, LotteryPlayOdds> oddsHeMap = lotteryPlayOddsService.selectPlayOddsBySettingId(settingId);
                    //连号需要根据name orderBet.getBetNumber()  来获取赔率表数据 12 13 14 15   ||  123,234

                    String[] winNumSplit = winNum.split(Constants.STR_UNDERLINE);
                    String Length = winNumSplit.length + "";
                    for (String winNumString : winNumSplit) {
                        odds = oddsHeMap.get(winNumString);
                        String winCount = odds.getWinCount();
                        String totalCount = odds.getTotalCount();
                        // 计算赔率
                        double odd = Double.parseDouble(totalCount) * 1.0 / Double.parseDouble(winCount) * divisor;
                        //一注的中奖额
                        winAmount = winAmount.add(orderBet.getBetAmount().divide(BigDecimal.valueOf(orderBet.getBetCount())).multiply(BigDecimal.valueOf(odd)));

                    }
                    orderBet.setWinCount(Length);
                }
                logger.info("快三连号彩种{},订单号{},是否中奖{},奖金{}", lotteryId, orderBet.getOrderId(), winNum, winAmount);
                // 根据中奖金额,修改投注信息及相关信息
                OrderRecord orderRecord = orderMap.get(orderBet.getOrderId());

                try {
                    betCommonService.winOrLose(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
                } catch (TransactionSystemException e1) {
                    logger.error("快三结算错误连号订单结算出错 事务冲突 进行重试" + orderRecord.getOrderSn(), e1);
                    for (int i = 0; i < 20; i++) {
                        try {
                            betCommonService.winOrLose(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
                        } catch (TransactionSystemException e2) {
                            logger.error("快三结算错误连号订单betCommonService出错" + i + ",事务冲突 进行重试" + orderRecord.getOrderSn(), e2);
                            Thread.sleep(100);
                            continue;
                        }
                        break;
                    }
                }
                /** 追号 */
                orderNewAppendWriteService.appendOrder(orderRecord, winAmount, StringUtils.isNotBlank(winNum));
            } catch (Exception e) {
                logger.error("快三结算错误连号结算出错：期号：" + issue + "彩种：" + lotteryId, e);
            }
        }
    }


    /*结算独胆
     */
    private void clearingKsDdPlay(String issue, String number, int playId, int lotteryId) {
        List<OrderRecord> orderRecords = getOrderRecord(issue, lotteryId);
        if (CollectionUtils.isEmpty(orderRecords)) {
            return;
        }
        Map<Integer, OrderRecord> orderMap = new HashMap<>();
        // 获取相关订单id集合 //更新订单开奖号码
        List<Integer> orderIds = new ArrayList<>();
        this.updateOrder(number, orderRecords, orderIds, orderMap);
        // 获取赔率因子
        double divisor = betCommonService.getDivisor(lotteryId);
        // 查询所有所有相关投注信息//根据玩法id  订单ids  订单状态
        List<Integer> playIds = new ArrayList<>();
        playIds.add(playId);
        List<OrderBetRecord> orderBetRecords = orderReadRestService.selectOrderBets(orderIds, playIds, OrderBetStatus.WAIT);
        logger.info("快三系列独胆相关投注数量{},彩种id{}, 期号{}, 开奖号码{}", orderBetRecords.size(), lotteryId, issue, number);
        // 判空处理
        if (CollectionUtils.isEmpty(orderBetRecords)) {
            return;
        }
        // 获取配置id
        Integer settingId = orderBetRecords.get(0).getSettingId();
        // 获取所有赔率信息
        Map<String, LotteryPlayOdds> oddsMap = lotteryPlayOddsService.selectPlayOddsBySettingId(settingId);
        //遍历所有投注订单
        for (OrderBetRecord orderBet : orderBetRecords) {
            try {
                BigDecimal winAmount = BigDecimal.ZERO;
                //   判断是否中奖
                String winNum = null;
                try {
                    winNum = this.judgeWinDd(orderBet.getBetNumber(), number, playId, lotteryId);
                } catch (Exception e) {
                    logger.error("快三结算错误独胆判断是否中奖错误彩种{},订单号{},用户id{},投注号码{}", lotteryId, orderBet.getOrderId(), orderBet.getUserId(), orderBet.getBetNumber());
                }
                orderBet.setWinCount(Constants.STR_ZERO);
                if (StringUtils.isNotBlank(winNum)) {
                    String[] winStrArr = winNum.split(Constants.STR_UNDERLINE);
                    String winStrArrLength = winStrArr.length + "";
                    for (String winStr : winStrArr) {
                        // 获取赔率信息
                        LotteryPlayOdds odds = oddsMap.get(winStr);
                        // 获取总注数/中奖注数
                        String winCount = odds.getWinCount();
                        String totalCount = odds.getTotalCount();
                        // 计算赔率
                        double odd = Double.parseDouble(totalCount) * 1.0 / Double.parseDouble(winCount) * divisor;
                        //中奖额      投注额除以投注数量乘以赔率
                        winAmount = winAmount.add(orderBet.getBetAmount().divide(BigDecimal.valueOf(orderBet.getBetCount())).multiply(BigDecimal.valueOf(odd)));
                    }
                    orderBet.setWinCount(winStrArrLength);
                }
                logger.info("快三独胆彩种{},用户id{},订单号{},是否中奖{},奖金{}", lotteryId, orderBet.getUserId(), orderBet.getOrderId(), winNum, winAmount);
                // 根据中奖金额,修改投注信息及相关信息
                OrderRecord orderRecord = orderMap.get(orderBet.getOrderId());

                try {
                    betCommonService.winOrLose(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
                } catch (TransactionSystemException e1) {
                    logger.error("快三结算错误独胆订单号" + orderRecord.getOrderSn() + "结算出错 事务冲突 进行重试", e1);
                    for (int i = 0; i < 20; i++) {
                        try {
                            betCommonService.winOrLose(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
                        } catch (TransactionSystemException e2) {
                            logger.error("快三结算错误独胆订单betCommonService出错" + i + ",事务冲突 进行重试" + orderRecord.getOrderSn(), e2);
                            Thread.sleep(100);
                            continue;
                        }
                        break;
                    }
                }
                /** 追号 */
                orderNewAppendWriteService.appendOrder(orderRecord, winAmount, StringUtils.isNotBlank(winNum));
            } catch (Exception e) {
                logger.error("快三结算错误独胆结算出错：期号：" + issue + "彩种：" + lotteryId, e);
            }
        }
    }

    //结算两面
    private void clearingKsLmPlay(String issue, String number, int playId, int lotteryId) {
        List<OrderRecord> orderRecords = getOrderRecord(issue, lotteryId);
        if (CollectionUtils.isEmpty(orderRecords)) {
            return;
        }
        Map<Integer, OrderRecord> orderMap = new HashMap<>();
        // 获取相关订单id集合 //更新订单开奖号码
        List<Integer> orderIds = new ArrayList<>();
        this.updateOrder(number, orderRecords, orderIds, orderMap);
        // 获取赔率因子
        double divisor = betCommonService.getDivisor(lotteryId);
        // 查询所有所有相关投注信息//根据玩法id  订单ids  订单状态
        List<Integer> playIds = new ArrayList<>();
        playIds.add(playId);
        List<OrderBetRecord> orderBetRecords = orderReadRestService.selectOrderBets(orderIds, playIds, OrderBetStatus.WAIT);
        logger.info("快三系列两面相关投注数量{},彩种id{}, 期号{}, 开奖号码{}", orderBetRecords.size(), lotteryId, issue, number);
        // 判空处理
        if (CollectionUtils.isEmpty(orderBetRecords)) {
            return;
        }
        // 获取配置id
        Integer settingId = orderBetRecords.get(0).getSettingId();
        // 获取所有赔率信息
        Map<String, LotteryPlayOdds> oddsMap = lotteryPlayOddsService.selectPlayOddsBySettingId(settingId);
        //遍历所有投注订单
        for (OrderBetRecord orderBet : orderBetRecords) {
            try {
                BigDecimal winAmount = BigDecimal.ZERO;
                //   判断是否中奖
                String winNum = null;
                try {
                    winNum = this.judgeWinLm(orderBet.getBetNumber(), number, playId, lotteryId);
                } catch (Exception e) {
                    logger.error("快三结算错误两面判断是否中奖错误彩种{},订单号{},用户id{},投注号码{}", lotteryId, orderBet.getOrderId(), orderBet.getUserId(), orderBet.getBetNumber());
                }
                orderBet.setWinCount(Constants.STR_ZERO);
                if (StringUtils.isNotBlank(winNum)) {
                    String[] winStrArr = winNum.split(Constants.STR_UNDERLINE);
                    String winStrArrLength = winStrArr.length + "";
                    for (String winStr : winStrArr) {
                        // 获取赔率信息
                        LotteryPlayOdds odds = oddsMap.get(winStr);
                        // 获取总注数/中奖注数
                        String winCount = odds.getWinCount();
                        String totalCount = odds.getTotalCount();
                        // 计算赔率
                        double odd = Double.parseDouble(totalCount) * 1.0 / Double.parseDouble(winCount) * divisor;
                        //中奖额      投注额除以投注数量乘以赔率
                        winAmount = winAmount.add(orderBet.getBetAmount().divide(BigDecimal.valueOf(orderBet.getBetCount())).multiply(BigDecimal.valueOf(odd)));
                    }
                    orderBet.setWinCount(winStrArrLength);
                }
                logger.info("快三两面彩种{},订单号{},用户id{},是否中奖{},奖金{}", lotteryId, orderBet.getOrderId(), orderBet.getUserId(), winNum, winAmount);
                // 根据中奖金额,修改投注信息及相关信息
                OrderRecord orderRecord = orderMap.get(orderBet.getOrderId());

                try {
                    betCommonService.winOrLose(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
                } catch (TransactionSystemException e1) {
                    logger.error("快三结算错误两面订单结算出错 事务冲突 进行重试订单号" + orderRecord.getOrderSn() + "用户id" + orderBet.getUserId(), e1);
                    for (int i = 0; i < 20; i++) {
                        try {
                            betCommonService.winOrLose(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
                        } catch (TransactionSystemException e2) {
                            logger.error("快三结算错误两面订单betCommonService出错" + i + ",事务冲突 进行重试" + orderRecord.getOrderSn(), e2);
                            Thread.sleep(100);
                            continue;
                        }
                        break;
                    }
                }
                /** 追号 */
                orderNewAppendWriteService.appendOrder(orderRecord, winAmount, StringUtils.isNotBlank(winNum));
            } catch (Exception e) {
                logger.error("快三结算错误两面结算出错：期号：" + issue + "彩种：" + lotteryId, e);
            }
        }
    }

    /*
     *@Title:(适用玩法:两面)
     *@Description:判断是否中奖,中奖返回中奖信息,不中则返回null
     * @Param  betnum 用户下注号码，sg，开奖号码 playname 玩法名称
     */
    private String judgeWinLm(String betNum, String sg, Integer playId, int lotteryId) {
        String[] betNumArr = betNum.split(",");//3,4 ,大，小
        String[] sgNumArr = sg.split(",");
        Integer num1 = Integer.valueOf(sgNumArr[0]);
        Integer num2 = Integer.valueOf(sgNumArr[1]);
        Integer num3 = Integer.valueOf(sgNumArr[2]);
        int he = num1 + num2 + num3;
        StringBuilder winStr = new StringBuilder();
        if (playId.equals(this.generationKsPlayId(PLAY_ID_LM, lotteryId))) {
            List<String> sumAndLm = this.getLm(he);
            for (String betStr : betNumArr) {
                if (sumAndLm.contains(betStr)) {
                    winStr.append(betStr + "_");
                }

            }
            //如果出现三同号 ，那么去掉大小单双
            if (num1.equals(num2) && num1.equals(num3)) {
                String winterString = winStr.toString();
                String winterStringReplace = winterString.replaceAll("大_", "").replaceAll("小_", "").replaceAll("单_", "").replaceAll("双_", "");
                winStr = new StringBuilder(winterStringReplace);
            }
        }
        if (winStr.length() > 0) {
            return winStr.substring(0, winStr.length() - 1);
        }
        return null;
    }

    /*
     *@Title:(适用玩法:独胆)
     *@Description:判断是否中奖,中奖返回中奖信息,不中则返回null
     * @Param  betnum 用户下注号码，sg，开奖号码 playname 玩法名称 eg: 3,4,5
     */
    private String judgeWinDd(String betNum, String sg, Integer playId, int lotteryId) {
        String[] sgNumArr = sg.split(",");
        StringBuilder winStr = new StringBuilder();
        if (playId.equals(this.generationKsPlayId(PLAY_ID_DD, lotteryId))) {
            String[] betDdNumber = betNum.split(",");
            for (String number : betDdNumber) {
                if (sgNumArr[0].equals(number) || sgNumArr[1].equals(number) || sgNumArr[2].equals(number)) {
                    winStr.append(number + "_");
                }
            }
        }
        if (winStr.length() > 0) {
            return winStr.substring(0, winStr.length() - 1);
        }
        return null;
    }


    /*
     *@Title:(适用玩法:二连号三连号)二连号  12,23,14,15   三连号 123,234,345
     *@Description:判断是否中奖,中奖返回中奖信息,不中则返回null      sg：3,4,5
     * @Param  betnum 用户下注号码，sg，开奖号码 playname 玩法名称
     */
    private String judgeWinLh(String betNumber, String sg, Integer playId, Integer lotteryId) {
        StringBuilder winStr = new StringBuilder();
        String[] sgArr = sg.split(",");
//        int[] intSgArr = Stream.of(sgArr).mapToInt(Integer::parseInt).toArray();
//        String sgStr = intSgArr[0] + "," + intSgArr[1] + "," + intSgArr[2];
        int length = betNumber.split(",")[0].length();
        //若为三连号
        if (playId.equals(this.generationKsPlayId(PLAY_ID_LH, lotteryId)) && length == 3) {
            if ("1,2,3".equals(sg) || "2,3,4".equals(sg) || "3,4,5".equals(sg) || "4,5,6".equals(sg)) {
                winStr.append("123,234,345,456" + "_");
            } //若为二连号       sg 中包含 两个连号，12 23 必须按顺序
        } else if (playId.equals(this.generationKsPlayId(PLAY_ID_LH, lotteryId)) && length == 2) {
            String[] betElhNumber = betNumber.split(",");
            for (String bet : betElhNumber) {
                String twoNumberFirst = sgArr[0] + sgArr[1];
                String twoNumberSecond = sgArr[1] + sgArr[2];
                if (bet.equals(twoNumberFirst) || bet.equals(twoNumberSecond)) {
                    winStr.append(bet + "_");
                }
            }
        }
        if (winStr.length() > 0) {
            return winStr.substring(0, winStr.length() - 1);
        }
        return null;
    }


    /*
     *@Title:(适用玩法:三不同号，胆拖，三同号单选 三同号通选)
     *@Description:判断是否中奖,中奖返回中奖信息,不中则返回null
     * @Param  betnum 用户下注号码，sg，开奖号码 playname 玩法名称
     */
    private String judgeWinSbThAndSthPlay(String betNumber, String sg, Integer playId, Integer lotteryId) {
        StringBuilder winStr = new StringBuilder();// betnumber 1,2,3，5,6,9,10   sg 4，5，6
        String[] sgNum = sg.split(",");
        if (playId.equals(this.generationKsPlayId(PLAY_ID_SBT, lotteryId)) && !sgNum[0].equals(sgNum[1]) && !sgNum[1].equals(sgNum[2]) && !sgNum[0].equals(sgNum[2])) {// betnumber 1,2,3，5,6,9,10   sg 4，5，6
            //判断所选择的号码当中是否包含赛果的三个数据    无论选多少个号码  只有一注会中  就是 所选号码中包含sg  sg必须是三个不同的号码
            String[] betSbtNumberSplit = betNumber.split(",");
            List<String> betSbtNumberList = Arrays.asList(betSbtNumberSplit);
            if (betSbtNumberList.contains(sgNum[0]) && betSbtNumberList.contains(sgNum[1]) && betSbtNumberList.contains(sgNum[2])) {
                winStr.append(sg + "_");
            }
        } else if (playId.equals(this.generationKsPlayId(PLAY_ID_SBT_DT, lotteryId)) && !sgNum[0].equals(sgNum[1]) && !sgNum[1].equals(sgNum[2]) && !sgNum[0].equals(sgNum[2])) {
//判断所选择的号码当中是否包含赛果的三个数据    无论选多少个号码  只有一注会中  就是 所选号码中包含sg  sg必须是三个不同的号码
//两种情况两个胆码和一个胆码    betnumber格式 1，2_ 3 ,4 ,5      1_3,4,5    1 4  5
            String[] betSbtDtNumber = betNumber.split("_");
            String betDm = betSbtDtNumber[0];
            String[] betDmsplit = betDm.split(",");
            String betTm = betSbtDtNumber[1];
            String[] betTmsplit = betTm.split(",");
            List<String> sgNumList = Stream.of(sgNum).collect(Collectors.toList());//sg 集合
            List<String> betDmsplitList = Stream.of(betDmsplit).collect(Collectors.toList());//胆码集合
            List<String> betTmsplitList = Stream.of(betTmsplit).collect(Collectors.toList());//拖码集合
            //胆码 和 拖码 不会重复，若赛果包含 胆码 ，那么把 符合的sg 数字 去掉，去掉0个 去掉 1个 去掉2个 去掉 3 个 ，只有 去掉一个 和去掉两个 的情况才能进下面的判断
            for (String dmNumber : betDmsplitList) {
                if (sgNumList.contains(dmNumber)) {
                    sgNumList.remove(dmNumber);
                }
            }
            //赛果中的一个胆码被 去掉   判断是否 剩下的sg 是否都在 托马当中 ， 判断拖码中是否包含剩下的两个sg 数字
            if (sgNumList.size() == 2) {
                if (betTmsplitList.contains(sgNumList.get(0)) && betTmsplitList.contains(sgNumList.get(1))) {
                    winStr.append(sg + "_");
                }
            }
            //赛果中包含的两个胆码被去掉 ，去掉这两个胆码  判断拖码中是否包含剩下的1个sg 数字
            else if (sgNumList.size() == 1) {
                if (betTmsplitList.contains(sgNumList.get(0))) {
                    winStr.append(sg + "_");
                }
            }
        } else if (playId.equals(this.generationKsPlayId(PLAY_ID_ST_DX, lotteryId)) && sgNum[0].equals(sgNum[1]) && sgNum[1].equals(sgNum[2])) {
            //三同号单选  例如 1 ，2 ， 3   sg 3,3,3        只要  只有sg都相同才能进入   循环遍历  1 . 2  . 3 看是否有相同
            String[] betSthsingleNumberSplit = betNumber.split(",");
            for (String number : betSthsingleNumberSplit) {
                if (number.equals(sgNum[0])) {
                    winStr.append(sg + "_");
                }
            }

        } else if (playId.equals(this.generationKsPlayId(PLAY_ID_ST_TX, lotteryId))) {
            if ("1,1,1".equals(sg) || "2,2,2".equals(sg) || "3,3,3".equals(sg) || "4,4,4".equals(sg) || "5,5,5".equals(sg) || "6,6,6".equals(sg)) {
                winStr.append(sg + "_");
            }
        }

        if (winStr.length() > 0) {
            return winStr.substring(0, winStr.length() - 1);
        }
        return null;
    }

    /*
     *@Title:(适用玩法:二不同号，胆拖，二同号单选 二同号复选 )
     *@Description:判断是否中奖,中奖返回中奖信息,不中则返回null
     * @Param    每个订单的  betNumber 用户下注号码，sg，开奖号码 playname 玩法名称
     */
    private String judgeWinEbThAndEthPlay(String betNumber, String sg, Integer playId, Integer lotteryId) {
        // sg 1,2,3
        StringBuilder winStr = new StringBuilder();
        String betNumberreplace = betNumber.replaceAll("_", ",");
        String[] betNum = betNumberreplace.split(",");
        String[] sgNum = sg.split(",");
        //转为int数组 进行排序
        int[] betNumInt = Stream.of(betNum).mapToInt(Integer::parseInt).toArray();
        int[] sgNumInt = Stream.of(sgNum).mapToInt(Integer::parseInt).toArray();
        Arrays.sort(betNumInt);
        Arrays.sort(sgNumInt);//从小到大排序
        List<String> betList = new ArrayList<String>();
        List<String> sgList = new ArrayList<String>();

        // 赛果三个数里有两个就赢   eg:  betnumber = 1,2,3,4  sg = 1,3,4// 1,2,  2,3     赛果三个数里有两个就赢
        if (playId.equals(this.generationKsPlayId(PLAY_ID_EBT, lotteryId))) {
            String[] betNumberArr = betNumber.split(",");
            //对 二不同betnumber 进行两两排列，列出所有下注 放入集合 ，
            for (int i = 0; i < betNumberArr.length - 1; i++) {
                for (int j = i + 1; j < betNumberArr.length; j++) {
                    betList.add(betNumberArr[i] + "," + betNumberArr[j]);
                }
            }
            //遍历集合，判断两个数字是否都在 sg中 是则该注赢了
            for (String num : betList) {
                String[] numSplit = num.split(",");
                if (sg.contains(numSplit[0]) && sg.contains(numSplit[1])) {
                    winStr.append(num + "_");
                }
            }
            // 1,4_3,1,2  2_2
        } else if (playId.equals(this.generationKsPlayId(PLAY_ID_EBT_DT, lotteryId))) {
            String[] split = betNumber.split("_");
            String betDmSplit = split[0];
            String[] betDm = betDmSplit.split(",");
            String betTmSplit = split[1];
            String[] betTm = betTmSplit.split(",");
            for (String dmNumber : betDm) {
                for (String tmNumber : betTm) {
                    if (dmNumber.equals(tmNumber)) {
                        continue;
                    }
                    if (sg.contains(tmNumber) && sg.contains(dmNumber)) {
                        String betWin = betDm + tmNumber;
                        winStr.append(betWin + "_");
                    }
                }
            }
        } else if (playId.equals(this.generationKsPlayId(PLAY_ID_ET_DX, lotteryId))) {
            String[] splitbetNumber = betNumber.split("_");
            String sameBet = splitbetNumber[0];
            String differentBet = splitbetNumber[1];
            String[] sameBetArr = sameBet.split(",");
            String[] differentBetArr = differentBet.split(",");
            ArrayList<String> List = new ArrayList<>();
            for (String sameNumber : sameBetArr) {
                for (String differentNumber : differentBetArr) {
                    if (sameNumber.equals(differentNumber)) {
                        continue;
                    }
                    String number = sameNumber + "," + sameNumber + "," + differentNumber;
                    String[] numbersplit = number.split(",");
                    int[] intBetnumbers = Stream.of(numbersplit).mapToInt(Integer::parseInt).toArray();//转为int数组 进行排序
                    Arrays.sort(intBetnumbers);
                    if (Arrays.equals(intBetnumbers, sgNumInt)) {
                        winStr.append(number + "_");
                    }
                }
            }
        } //二同号复选     betnumber 1,2，3  只要sg里有两个数是 betnumber 则赢
        else if (playId.equals(this.generationKsPlayId(PLAY_ID_ET_FX, lotteryId))) {
            String[] betNumberSplit = betNumber.split(",");
            Boolean flag;
            for (String number : betNumberSplit) {
                int sameNumber = Integer.parseInt(number);
                flag = false;
                if (sgNumInt[0] == sameNumber && sgNumInt[1] == sameNumber) {
                    flag = true;
                } else if (sgNumInt[0] == sameNumber && sgNumInt[2] == sameNumber) {
                    flag = true;
                } else if (sgNumInt[1] == sameNumber && sgNumInt[2] == sameNumber) {
                    flag = true;
                }
                if (flag) {
                    winStr.append(number + "_");
                }
            }
        }
        if (winStr.length() > 0) {
            return winStr.substring(0, winStr.length() - 1);
        }
        return null;
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

    //获取该彩种的该期号订单
    private List<OrderRecord> getOrderRecord(String issue, int lotteryId) {
        List<OrderRecord> orderRecords = (List<OrderRecord>) redisTemplate.opsForValue().get(ORDER_KEY + issue);
        // 获取相应的订单信息
        if (CollectionUtils.isEmpty(orderRecords)) {
            orderRecords = orderReadRestService.selectOrders(lotteryId, issue, OrderStatus.NORMAL);
            redisTemplate.opsForValue().set(ORDER_KEY + issue, orderRecords, 2, TimeUnit.MINUTES);
            RedisBusinessUtil.addCacheForValueAndMinutes(ORDER_KEY + issue, orderRecords, 2L, TimeUnit.MINUTES);
        }
        return orderRecords;
    }

    /**
     * 快三玩法ID生成
     *
     * @param playNumber
     * @param
     * @return
     */
    private Integer generationKsPlayId(String playNumber, Integer lotteryId) {
        return Integer.parseInt(lotteryId + playNumber);
    }

    /**
     * 快三玩法ID生成
     *
     * @param
     * @param
     * @return
     */
    private List<Integer> generationKsPlayIdList(List<String> playNumbers, Integer lotteryId) {
        List<Integer> replaceToNewPlayId = new ArrayList<>();
        for (String number : playNumbers) {
            replaceToNewPlayId.add(Integer.parseInt(lotteryId + number));
        }
        return replaceToNewPlayId;
    }

    /**
     * 大小单双和值
     *
     * @param he
     * @param
     * @return
     */
    private List<String> getLm(int he) {
        List<String> sumandLmList = new ArrayList<>();
        sumandLmList.add(he + "");
        if (he > 10) { //需根据玩法规则改动
            sumandLmList.add("大");
        }
        if (he <= 10) {
            sumandLmList.add("小");
        }
        if (he % 2 == 1) {
            sumandLmList.add("单");
        }
        if (he % 2 == 0) {
            sumandLmList.add("双");
        }
        return sumandLmList;
    }
}
