package com.caipiao.live.order.service.bet.impl;

import com.caipiao.live.common.model.dto.order.OrderBetStatus;
import com.caipiao.live.common.model.dto.order.OrderStatus;
import com.caipiao.live.common.mybatis.entity.LotteryPlay;
import com.caipiao.live.common.mybatis.entity.LotteryPlayOdds;
import com.caipiao.live.common.mybatis.entity.OrderBetRecord;
import com.caipiao.live.common.mybatis.entity.OrderRecord;
import com.caipiao.live.common.mybatis.mapper.OrderRecordMapper;
import com.caipiao.live.common.service.read.OrderReadRestService;
import com.caipiao.live.order.service.bet.BetBjpksService;
import com.caipiao.live.order.service.bet.BetCommonService;
import com.caipiao.live.order.service.lottery.LotteryPlayOddsWriteService;
import com.caipiao.live.order.service.lottery.LotteryPlayWriteService;
import com.caipiao.live.order.service.order.OrderAppendWriteService;
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
//import com.rabbitmq.client.Channel;
//import com.rabbitmq.client.Connection;
//import com.rabbitmq.client.ConnectionFactory;

/**
 * @author lzy
 * @create 2018-09-15 17:07
 **/
@Service
public class BetBjpksServiceImpl implements BetBjpksService {
    private static Logger logger = LoggerFactory.getLogger(BetBjpksServiceImpl.class);

    @Autowired
    private OrderRecordMapper orderRecordMapper;
    @Autowired
    private LotteryPlayWriteService lotteryPlayWriteService;
    @Autowired
    private LotteryPlayOddsWriteService lotteryPlayOddsService;
    @Autowired
    private BetCommonService betCommonService;
    @Autowired
    private OrderAppendWriteService orderAppendWriteService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private OrderReadRestService orderReadRestService;

    private static final String ORDER_KEY = "BJPKS_ORDER_";
    private static final String BJPKS_LOTTERY_SG = "BJPKS_LOTTERY_SG_";

    // PK10??????????????????
    private final List<Integer> PLAY_IDS_CMC_CQJ = Lists.newArrayList(136, 142, 144);
    // PK10???????????????
    private final List<Integer> PLAY_IDS_DS_CQJ = Lists.newArrayList(138, 141, 143, 145);
    // PK10???????????????
    private final List<Integer> PLAY_IDS_DWD = Lists.newArrayList(146, 147);

    // PK10??????
    private final String PLAY_ID_LM = "01";
    // PK10?????????
    private final String PLAY_ID_GYH = "02";
    // PK10 1-5???
    private final String PLAY_ID_15 = "03";
    // PK10 6-10???
    private final String PLAY_ID_610 = "04";
    // PK10 ?????????
    private final String PLAY_ID_1 = "05";
    // PK10 ?????????
    private final String PLAY_ID_2 = "06";
    // PK10 ?????????
    private final String PLAY_ID_3 = "07";
    // PK10 ?????????
    private final String PLAY_ID_4 = "08";
    // PK10 ?????????
    private final String PLAY_ID_5 = "09";
    // PK10 ?????????
    private final String PLAY_ID_6 = "10";
    // PK10 ?????????
    private final String PLAY_ID_7 = "11";
    // PK10 ?????????
    private final String PLAY_ID_8 = "12";
    // PK10 ?????????
    private final String PLAY_ID_9 = "13";
    // PK10 ?????????
    private final String PLAY_ID_10 = "14";


    private final List<String> PLAY_IDS_110 = Lists.newArrayList("03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14");

    @Override
    public void clearingBjpksCmcCqj(String issue, String number, int lotteryId) throws Exception {
        //    clearingBjpksCqj(issue, number, PLAY_IDS_CMC_CQJ);
        clearingBjpksCqj(issue, number, this.generationSSCPlayIdList(PLAY_IDS_110, lotteryId), lotteryId);

    }

    @Override
    public void clearingBjpksDsCqj(String issue, String number) throws Exception {
        //      clearingBjpksCqj(issue, number, PLAY_IDS_DS_CQJ);
    }

    @Override
    public void clearingBjpksDwd(String issue, String number) throws Exception {
        //   clearingBjpksCqj(issue, number, PLAY_IDS_DWD);
    }

    @Override
    public void clearingBjpksGyh(String issue, String number, int lotteryId) throws Exception {
        clearingBjpksOnePlayManyOdds(issue, number, this.generationSSCPlayId(PLAY_ID_GYH, lotteryId), lotteryId);
    }

    @Override
    public void clearingBjpksLm(String issue, String number, int lotteryId) throws Exception {
        clearingBjpksOnePlayManyOdds(issue, number, this.generationSSCPlayId(PLAY_ID_LM, lotteryId), lotteryId);
    }

    private void clearingBjpksCqj(String issue, String number, List<Integer> playIds, int lotteryId) {
//        // ????????????????????????
//        BjpksLotterySg sg = getLotterySg(issue);
//        if (sg == null) {
//            throw new Exception("???????????????????????????");
//        }
        // ???????????????????????????
        List<OrderRecord> orderRecords = getOrderRecord(issue, lotteryId);
        logger.info("5???pk10?????????recordSize" + orderRecords.size());
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
        logger.info("5???pk10?????????betSize" + orderBetRecords.size());
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
            logger.info("5???pk10?????????" + orderBet.getOrderId());
            try {
                BigDecimal winAmount = new BigDecimal(0);

                // ??????????????????
                LotteryPlay play = playMap.get(orderBet.getPlayId());
                // ??????????????????
                LotteryPlayOdds odds = oddsMap.get(orderBet.getSettingId());

                if (orderBet.getPlayId().equals(this.generationSSCPlayId(PLAY_ID_1, lotteryId)) || orderBet.getPlayId().equals(this.generationSSCPlayId(PLAY_ID_2, lotteryId))
                        || orderBet.getPlayId().equals(this.generationSSCPlayId(PLAY_ID_3, lotteryId)) || orderBet.getPlayId().equals(this.generationSSCPlayId(PLAY_ID_4, lotteryId))
                        || orderBet.getPlayId().equals(this.generationSSCPlayId(PLAY_ID_5, lotteryId)) || orderBet.getPlayId().equals(this.generationSSCPlayId(PLAY_ID_6, lotteryId))
                        || orderBet.getPlayId().equals(this.generationSSCPlayId(PLAY_ID_7, lotteryId)) || orderBet.getPlayId().equals(this.generationSSCPlayId(PLAY_ID_8, lotteryId))
                        || orderBet.getPlayId().equals(this.generationSSCPlayId(PLAY_ID_9, lotteryId)) || orderBet.getPlayId().equals(this.generationSSCPlayId(PLAY_ID_10, lotteryId))) {
                    //130405:?????????   ??? 130414????????????
                    Map<String, LotteryPlayOdds> oddsHeMap = lotteryPlayOddsService.selectPlayOddsBySettingId(orderBet.getSettingId());
                    odds = oddsHeMap.get(orderBet.getBetNumber().split("@")[1]);
                }
                logger.info("5???pk10??????1???" + orderBet.getOrderId());
                orderBet.setWinCount("0");
                // ??????????????????
                String winNum = this.isWin(orderBet.getBetNumber(), number, play, lotteryId);
                if (StringUtils.isNotBlank(winNum)) {
                    // ???????????????/????????????
                    String winCount = odds.getWinCount();
                    String totalCount = odds.getTotalCount();

                    // ????????????
                    double odd = Double.parseDouble(totalCount) * 1.0 / Double.parseDouble(winCount) * divisor;
                    //??????????????????
                    winAmount = orderBet.getBetAmount().divide(BigDecimal.valueOf(orderBet.getBetCount())).multiply(BigDecimal.valueOf(odd));

                    if (PLAY_IDS_DWD.contains(play.getId())) {
                        // ???????????????,???????????????,???????????????
                        int length = winNum.split(",").length;
                        winAmount = winAmount.multiply(BigDecimal.valueOf(length));
                    }
                    orderBet.setWinCount("1");
                }

                // ??????????????????,?????????????????????????????????
                OrderRecord orderRecord = orderMap.get(orderBet.getOrderId());

                try {
                    betCommonService.winOrLose(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
                    logger.info("5???pk10??????2???" + orderBet.getOrderId());
                } catch (TransactionSystemException e1) {
                    logger.error("5???pk10??????3???" + orderBet.getOrderId());
                    logger.error("?????????????????? ???????????? ???????????????orderSn:{}.", orderRecord.getOrderSn(), e1);
                    for (int i = 0; i < 20; i++) {
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
                orderAppendWriteService.appendOrder(orderRecord, winAmount, StringUtils.isNotBlank(winNum));
            } catch (Exception e) {
                logger.info("5???pk10??????4???{}", orderBet.getOrderId());
                logger.error("???????????????issue {}", issue, e);
            }

        }
    }

    private void clearingBjpksOnePlayManyOdds(String issue, String number, Integer playId, int lotteryId) throws Exception {
//        // ????????????????????????
//        BjpksLotterySg sg = getLotterySg(issue);
//        if (sg == null) {
//            throw new Exception("???????????????????????????");
//        }
        // ???????????????????????????
        List<OrderRecord> orderRecords = getOrderRecord(issue, lotteryId);
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
        List<OrderBetRecord> orderBetRecords = orderReadRestService.selectOrderBetsSinglePlay(orderIds, playId, OrderBetStatus.WAIT);
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
                BigDecimal winAmount = new BigDecimal(0);
                orderBet.setWinCount("0");
                // ??????????????????
                String winNum = this.isWinLmAndGyh(orderBet.getBetNumber(), number, playId, lotteryId);
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
                        winAmount = winAmount.add(orderBet.getBetAmount().divide(BigDecimal.valueOf(orderBet.getBetCount()), 4, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(odd)));
                    }
                    orderBet.setWinCount("1");
                }
                // ??????????????????,?????????????????????????????????
                OrderRecord orderRecord = orderMap.get(orderBet.getOrderId());

                try {
                    betCommonService.winOrLose(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
                } catch (TransactionSystemException e1) {
                    logger.error("?????????????????? ???????????? ???????????????orderSn:{}.", orderRecord.getOrderSn(), e1);
                    for (int i = 0; i < 20; i++) {
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
                orderAppendWriteService.appendOrder(orderRecord, winAmount, StringUtils.isNotBlank(winNum));
            } catch (Exception e) {
                logger.error("???????????????issue:{}", issue, e);
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

    /**
     * ????????????pk10????????????,????????????????????????,???????????????null
     * (????????????:??????????????????,???????????????,?????????)
     *
     * @param betNum
     * @param sg
     * @return
     */
    private String isWin(String betNum, String sg, LotteryPlay play, Integer lotteryId) {
        String section = play.getSection();
        Integer start = 1;
        Integer end = 10;
        if (section != null) {
            String[] sections = section.split(",");
            start = Integer.parseInt(sections[0]);
            end = Integer.parseInt(sections[1]);
        }

        String[] betNumArrs = betNum.split(",");
        String[] sgNumArr = sg.split(",");
        Integer playId = play.getId();
        Integer playTagId = play.getPlayTagId();

//        if (PLAY_IDS_CMC_CQJ.contains(playId)) {
//            int count = 0;
//            for (int i = start - 1; i < end; i++) {
//                String[] betNumArr = betNumArrs[i].split(" ");
//                String sgNum = sgNumArr[i];
//                for (String betNumber : betNumArr) {
//                    if (betNumber.equals(sgNum)) {
//                        count++;
//                    }
//                }
//            }
//            //??????
//            if (count == (end - start + 1)) {
//                return betNum;
//            }
//        } else if (PLAY_IDS_DS_CQJ.contains(playId)) {
//            for (int i = 0, len = betNumArrs.length; i < len; i++) {
//                int count = 0;
//                String[] betNumArr = betNumArrs[i].split(" ");
//                for (int j = start - 1; j < end; j++) {
//                    String sgNum = sgNumArr[j];
//                    if (betNumArr[j - start + 1].equals(sgNum)) {
//                        count++;
//                    }
//
//                }
//                //??????
//                if (count == (end - start + 1)) {
//                    return betNum;
//                }
//            }
//        } else if (PLAY_IDS_DWD.contains(playId)) {
//            // ???????????????,???????????????
//            StringBuffer winNum = new StringBuffer();
//            for (int i = 0, len = betNumArrs.length; i < len; i++) {
//                String[] betNumArr = betNumArrs[i].split(" ");
//                for (String betNumber : betNumArr) {
//                    if (StringUtils.isBlank(betNumber)) {
//                        continue;
//                    }
//                    if (Integer.valueOf(sgNumArr[i + start - 1]).equals(Integer.valueOf(betNumber))) {
//                        winNum.append(betNumber).append(",");
//                    }
//                }
//            }
//            if (winNum.length() > 0) {
//                return winNum.substring(0, winNum.length() - 1);
//            }
//        }else
        if (this.generationSSCPlayId(PLAY_ID_15, lotteryId).equals(playTagId) ||
                this.generationSSCPlayId(PLAY_ID_610, lotteryId).equals(playTagId)) {
            // 1-5?????????
            StringBuffer winNum = new StringBuffer();
            for (int i = 0, len = betNumArrs.length; i < len; i++) {
                String[] betNumArr = betNumArrs[i].split(" ");
                for (String betNumber : betNumArr) {
                    if (StringUtils.isBlank(betNumber)) {
                        continue;
                    }
                    String betn = betNumber;
                    if (betNumber.contains("@")) {
                        betn = betNumber.split("@")[1];
                    }
                    if (betNumber.contains("??????") || betNumber.contains("?????????") || betNumber.contains("???1???")) {
                        if (Integer.parseInt(betn) == Integer.parseInt(sgNumArr[0])) {
                            winNum.append(betNumber).append(",");
                        }

                    } else if (betNumber.contains("??????") || betNumber.contains("?????????") || betNumber.contains("???2???")) {
                        if (Integer.parseInt(betn) == Integer.parseInt(sgNumArr[1])) {
                            winNum.append(betNumber).append(",");
                        }

                    } else if (betNumber.contains("?????????") || betNumber.contains("???3???")) {
                        if (Integer.parseInt(betn) == Integer.parseInt(sgNumArr[2])) {
                            winNum.append(betNumber).append(",");
                        }

                    } else if (betNumber.contains("?????????") || betNumber.contains("???4???")) {
                        if (Integer.parseInt(betn) == Integer.parseInt(sgNumArr[3])) {
                            winNum.append(betNumber).append(",");
                        }

                    } else if (betNumber.contains("?????????") || betNumber.contains("???5???")) {
                        if (Integer.parseInt(betn) == Integer.parseInt(sgNumArr[4])) {
                            winNum.append(betNumber).append(",");
                        }

                    } else if (betNumber.contains("?????????") || betNumber.contains("???6???")) {
                        if (Integer.parseInt(betn) == Integer.parseInt(sgNumArr[5])) {
                            winNum.append(betNumber).append(",");
                        }

                    } else if (betNumber.contains("?????????") || betNumber.contains("???7???")) {
                        if (Integer.parseInt(betn) == Integer.parseInt(sgNumArr[6])) {
                            winNum.append(betNumber).append(",");
                        }

                    } else if (betNumber.contains("?????????") || betNumber.contains("???8???")) {
                        if (Integer.parseInt(betn) == Integer.parseInt(sgNumArr[7])) {
                            winNum.append(betNumber).append(",");
                        }

                    } else if (betNumber.contains("?????????") || betNumber.contains("???9???")) {
                        if (Integer.parseInt(betn) == Integer.parseInt(sgNumArr[8])) {
                            winNum.append(betNumber).append(",");
                        }

                    } else if (betNumber.contains("?????????") || betNumber.contains("???10???")) {
                        if (Integer.parseInt(betn) == Integer.parseInt(sgNumArr[9])) {
                            winNum.append(betNumber).append(",");
                        }

                    }
                }
            }
            if (winNum.length() > 0) {
                return winNum.substring(0, winNum.length() - 1);
            }
        } else if (this.generationSSCPlayId(PLAY_ID_1, lotteryId).equals(playTagId) || this.generationSSCPlayId(PLAY_ID_2, lotteryId).equals(playTagId) || this.generationSSCPlayId(PLAY_ID_3, lotteryId).equals(playTagId) ||
                this.generationSSCPlayId(PLAY_ID_4, lotteryId).equals(playTagId) || this.generationSSCPlayId(PLAY_ID_5, lotteryId).equals(playTagId) || this.generationSSCPlayId(PLAY_ID_6, lotteryId).equals(playTagId) ||
                this.generationSSCPlayId(PLAY_ID_7, lotteryId).equals(playTagId) || this.generationSSCPlayId(PLAY_ID_8, lotteryId).equals(playTagId) || this.generationSSCPlayId(PLAY_ID_9, lotteryId).equals(playTagId) ||
                this.generationSSCPlayId(PLAY_ID_10, lotteryId).equals(playTagId)) {
            // ?????????-???????????????
//            StringBuffer winNum = new StringBuffer();
            String winNum = null;
            for (int i = 0, len = betNumArrs.length; i < len; i++) {
//                String[] betNumArr = betNumArrs[i].split(" ")
//                for (String betNumber : betNumArr) {
//                if (StringUtils.isBlank(betNumber)) {
//                    continue;
//                }
                String betn = null;
                if (betNum.contains("@")) {
                    betn = betNum.split("@")[1];
                }
                if ("???".equals(betn) || "???".equals(betn) || "???".equals(betn) || "???".equals(betn) || "???".equals(betn) || "???".equals(betn)) {
                    if (this.generationSSCPlayId(PLAY_ID_1, lotteryId).equals(playTagId)) {
                        if ("???".equals(betn) && Integer.parseInt(sgNumArr[0]) > 5) {
                            winNum = betNum;
                        } else if ("???".equals(betn) && Integer.parseInt(sgNumArr[0]) <= 5) {
                            winNum = betNum;
                        } else if ("???".equals(betn) && Integer.parseInt(sgNumArr[0]) % 2 != 0) {
                            winNum = betNum;
                        } else if ("???".equals(betn) && Integer.parseInt(sgNumArr[0]) % 2 == 0) {
                            winNum = betNum;
                        } else if ("???".equals(betn) && Integer.parseInt(sgNumArr[0]) > Integer.parseInt(sgNumArr[9])) {
                            winNum = betNum;
                        } else if ("???".equals(betn) && Integer.parseInt(sgNumArr[0]) < Integer.parseInt(sgNumArr[9])) {
                            winNum = betNum;
                        }
                    } else if (this.generationSSCPlayId(PLAY_ID_2, lotteryId).equals(playTagId)) {
                        if ("???".equals(betn) && Integer.parseInt(sgNumArr[1]) > 5) {
                            winNum = betNum;
                        } else if ("???".equals(betn) && Integer.parseInt(sgNumArr[1]) <= 5) {
                            winNum = betNum;
                        } else if ("???".equals(betn) && Integer.parseInt(sgNumArr[1]) % 2 != 0) {
                            winNum = betNum;
                        } else if ("???".equals(betn) && Integer.parseInt(sgNumArr[1]) % 2 == 0) {
                            winNum = betNum;
                        } else if ("???".equals(betn) && Integer.parseInt(sgNumArr[1]) > Integer.parseInt(sgNumArr[8])) {
                            winNum = betNum;
                        } else if ("???".equals(betn) && Integer.parseInt(sgNumArr[1]) < Integer.parseInt(sgNumArr[8])) {
                            winNum = betNum;
                        }
                    } else if (this.generationSSCPlayId(PLAY_ID_3, lotteryId).equals(playTagId)) {
                        if ("???".equals(betn) && Integer.parseInt(sgNumArr[2]) > 5) {
                            winNum = betNum;
                        } else if ("???".equals(betn) && Integer.parseInt(sgNumArr[2]) <= 5) {
                            winNum = betNum;
                        } else if ("???".equals(betn) && Integer.parseInt(sgNumArr[2]) % 2 != 0) {
                            winNum = betNum;
                        } else if ("???".equals(betn) && Integer.parseInt(sgNumArr[2]) % 2 == 0) {
                            winNum = betNum;
                        } else if ("???".equals(betn) && Integer.parseInt(sgNumArr[2]) > Integer.parseInt(sgNumArr[7])) {
                            winNum = betNum;
                        } else if ("???".equals(betn) && Integer.parseInt(sgNumArr[2]) < Integer.parseInt(sgNumArr[7])) {
                            winNum = betNum;
                        }
                    } else if (this.generationSSCPlayId(PLAY_ID_4, lotteryId).equals(playTagId)) {
                        if ("???".equals(betn) && Integer.parseInt(sgNumArr[3]) > 5) {
                            winNum = betNum;
                        } else if ("???".equals(betn) && Integer.parseInt(sgNumArr[3]) <= 5) {
                            winNum = betNum;
                        } else if ("???".equals(betn) && Integer.parseInt(sgNumArr[3]) % 2 != 0) {
                            winNum = betNum;
                        } else if ("???".equals(betn) && Integer.parseInt(sgNumArr[3]) % 2 == 0) {
                            winNum = betNum;
                        } else if ("???".equals(betn) && Integer.parseInt(sgNumArr[3]) > Integer.parseInt(sgNumArr[6])) {
                            winNum = betNum;
                        } else if ("???".equals(betn) && Integer.parseInt(sgNumArr[3]) < Integer.parseInt(sgNumArr[6])) {
                            winNum = betNum;
                        }
                    } else if (this.generationSSCPlayId(PLAY_ID_5, lotteryId).equals(playTagId)) {
                        if ("???".equals(betn) && Integer.parseInt(sgNumArr[4]) > 5) {
                            winNum = betNum;
                        } else if ("???".equals(betn) && Integer.parseInt(sgNumArr[4]) <= 5) {
                            winNum = betNum;
                        } else if ("???".equals(betn) && Integer.parseInt(sgNumArr[4]) % 2 != 0) {
                            winNum = betNum;
                        } else if ("???".equals(betn) && Integer.parseInt(sgNumArr[4]) % 2 == 0) {
                            winNum = betNum;
                        } else if ("???".equals(betn) && Integer.parseInt(sgNumArr[4]) > Integer.parseInt(sgNumArr[5])) {
                            winNum = betNum;
                        } else if ("???".equals(betn) && Integer.parseInt(sgNumArr[4]) < Integer.parseInt(sgNumArr[5])) {
                            winNum = betNum;
                        }
                    } else if (this.generationSSCPlayId(PLAY_ID_6, lotteryId).equals(playTagId)) {
                        if ("???".equals(betn) && Integer.parseInt(sgNumArr[5]) > 5) {
                            winNum = betNum;
                        } else if ("???".equals(betn) && Integer.parseInt(sgNumArr[5]) <= 5) {
                            winNum = betNum;
                        } else if ("???".equals(betn) && Integer.parseInt(sgNumArr[5]) % 2 != 0) {
                            winNum = betNum;
                        } else if ("???".equals(betn) && Integer.parseInt(sgNumArr[5]) % 2 == 0) {
                            winNum = betNum;
                        }
                    } else if (this.generationSSCPlayId(PLAY_ID_7, lotteryId).equals(playTagId)) {
                        if ("???".equals(betn) && Integer.parseInt(sgNumArr[6]) > 5) {
                            winNum = betNum;
                        } else if ("???".equals(betn) && Integer.parseInt(sgNumArr[6]) <= 5) {
                            winNum = betNum;
                        } else if ("???".equals(betn) && Integer.parseInt(sgNumArr[6]) % 2 != 0) {
                            winNum = betNum;
                        } else if ("???".equals(betn) && Integer.parseInt(sgNumArr[6]) % 2 == 0) {
                            winNum = betNum;
                        }
                    } else if (this.generationSSCPlayId(PLAY_ID_8, lotteryId).equals(playTagId)) {
                        if ("???".equals(betn) && Integer.parseInt(sgNumArr[7]) > 5) {
                            winNum = betNum;
                        } else if ("???".equals(betn) && Integer.parseInt(sgNumArr[7]) <= 5) {
                            winNum = betNum;
                        } else if ("???".equals(betn) && Integer.parseInt(sgNumArr[7]) % 2 != 0) {
                            winNum = betNum;
                        } else if ("???".equals(betn) && Integer.parseInt(sgNumArr[7]) % 2 == 0) {
                            winNum = betNum;
                        }
                    } else if (this.generationSSCPlayId(PLAY_ID_9, lotteryId).equals(playTagId)) {
                        if ("???".equals(betn) && Integer.parseInt(sgNumArr[8]) > 5) {
                            winNum = betNum;
                        } else if ("???".equals(betn) && Integer.parseInt(sgNumArr[8]) <= 5) {
                            winNum = betNum;
                        } else if ("???".equals(betn) && Integer.parseInt(sgNumArr[8]) % 2 != 0) {
                            winNum = betNum;
                        } else if ("???".equals(betn) && Integer.parseInt(sgNumArr[8]) % 2 == 0) {
                            winNum = betNum;
                        }
                    } else if (this.generationSSCPlayId(PLAY_ID_10, lotteryId).equals(playTagId)) {
                        if ("???".equals(betn) && Integer.parseInt(sgNumArr[9]) > 5) {
                            winNum = betNum;
                        } else if ("???".equals(betn) && Integer.parseInt(sgNumArr[9]) <= 5) {
                            winNum = betNum;
                        } else if ("???".equals(betn) && Integer.parseInt(sgNumArr[9]) % 2 != 0) {
                            winNum = betNum;
                        } else if ("???".equals(betn) && Integer.parseInt(sgNumArr[9]) % 2 == 0) {
                            winNum = betNum;
                        }
                    }

                } else {
                    if (betNum.contains("??????") || betNum.contains("?????????") || betNum.contains("???1???")) {
                        if (Integer.parseInt(betn) == Integer.parseInt(sgNumArr[0])) {
                            winNum = betNum;
                        }

                    } else if (betNum.contains("??????") || betNum.contains("?????????") || betNum.contains("???2???")) {
                        if (Integer.parseInt(betn) == Integer.parseInt(sgNumArr[1])) {
                            winNum = betNum;
                        }

                    } else if (betNum.contains("?????????") || betNum.contains("???3???")) {
                        if (Integer.parseInt(betn) == Integer.parseInt(sgNumArr[2])) {
                            winNum = betNum;
                        }

                    } else if (betNum.contains("?????????") || betNum.contains("???4???")) {
                        if (Integer.parseInt(betn) == Integer.parseInt(sgNumArr[3])) {
                            winNum = betNum;
                        }

                    } else if (betNum.contains("?????????") || betNum.contains("???5???")) {
                        if (Integer.parseInt(betn) == Integer.parseInt(sgNumArr[4])) {
                            winNum = betNum;
                        }

                    } else if (betNum.contains("?????????") || betNum.contains("???6???")) {
                        if (Integer.parseInt(betn) == Integer.parseInt(sgNumArr[5])) {
                            winNum = betNum;
                        }

                    } else if (betNum.contains("?????????") || betNum.contains("???7???")) {
                        if (Integer.parseInt(betn) == Integer.parseInt(sgNumArr[6])) {
                            winNum = betNum;
                        }

                    } else if (betNum.contains("?????????") || betNum.contains("???8???")) {
                        if (Integer.parseInt(betn) == Integer.parseInt(sgNumArr[7])) {
                            winNum = betNum;
                        }

                    } else if (betNum.contains("?????????") || betNum.contains("???9???")) {
                        if (Integer.parseInt(betn) == Integer.parseInt(sgNumArr[8])) {
                            winNum = betNum;
                        }

                    } else if (betNum.contains("?????????") || betNum.contains("???10???")) {
                        if (Integer.parseInt(betn) == Integer.parseInt(sgNumArr[9])) {
                            winNum = betNum;
                        }
                    }
                }

            }
            if (winNum != null && winNum.length() > 0) {
                return winNum.substring(0, winNum.length() - 1);
            }

        }

        return null;
    }

    // ################################# ?????????redis start ###################################################

    /**
     * ????????????????????????
     *
     * @param issue ??????
     * @return
     */
//    private BjpksLotterySg getLotterySg(String issue) {
//        BjpksLotterySg sg = null;
//        // ????????????????????????
//        if (redisTemplate.hasKey(BJPKS_LOTTERY_SG + issue)) {
//            sg = (BjpksLotterySg) redisTemplate.opsForValue().get(BJPKS_LOTTERY_SG + issue);
//        }
//        if (sg == null) {
//            BjpksLotterySgExample sgExample = new BjpksLotterySgExample();
//            BjpksLotterySgExample.Criteria sgCriteria = sgExample.createCriteria();
//            sgCriteria.andIssueEqualTo(issue);
//            sg = bjpksLotterySgMapper.selectOneByExample(sgExample);
//            redisTemplate.opsForValue().set(BJPKS_LOTTERY_SG + issue, sg, 2, TimeUnit.MINUTES);
//        }
//        return sg;
//    }

    /**
     * ??????????????????????????????
     *
     * @param issue
     * @return
     */
    public List<OrderRecord> getOrderRecord(String issue, int lotteryId) {
        List<OrderRecord> orderRecords = (List<OrderRecord>) redisTemplate.opsForValue().get(ORDER_KEY + lotteryId + issue);
        // ???????????????????????????
        if (CollectionUtils.isEmpty(orderRecords)) {
            orderRecords = orderReadRestService.selectOrders(lotteryId, issue, OrderStatus.NORMAL);
            redisTemplate.opsForValue().set(ORDER_KEY + lotteryId + issue, orderRecords, 2, TimeUnit.MINUTES);
        }
        return orderRecords;
    }

    // ################################# ?????????redis end ###################################################

    /**
     * ????????????pk10????????????,????????????????????????,???????????????null
     * (????????????:??????, ?????????)
     *
     * @param betNum ????????????
     * @param sg     ????????????
     * @param playId ??????id
     * @return
     */
    private String isWinLmAndGyh(String betNum, String sg, Integer playId, Integer lotteryId) {
        String[] betNumArr = betNum.split(",");
        String[] sgNumArr = sg.split(",");
        Integer num1 = Integer.valueOf(sgNumArr[0]);
        Integer num2 = Integer.valueOf(sgNumArr[1]);
        int he = num1 + num2;
        StringBuilder winStr = new StringBuilder();
        String betStr0 = "";
        if (this.generationSSCPlayId(PLAY_ID_LM, lotteryId).equals(playId)) {
            for (String betStr : betNumArr) {
                if (betStr.contains("@")) {
                    betStr0 = betStr.split("@")[0];
                    betStr = betStr.split("@")[1];
                }

                if ("?????????".equals(betStr) && he > 11) {
                    winStr.append(betStr).append(",");
                } else if ("?????????".equals(betStr) && he <= 11) {
                    winStr.append(betStr).append(",");
                } else if ("?????????".equals(betStr) && he % 2 == 1) {
                    winStr.append(betStr).append(",");
                } else if ("?????????".equals(betStr) && he % 2 == 0) {
                    winStr.append(betStr).append(",");
                } else if ("?????????".equals(betStr0 + betStr) && num1 > 5) {
                    winStr.append(betStr).append(",");
                } else if ("?????????".equals(betStr0 + betStr) && num1 <= 5) {
                    winStr.append(betStr).append(",");
                } else if ("?????????".equals(betStr0 + betStr) && num1 % 2 == 1) {
                    winStr.append(betStr).append(",");
                } else if ("?????????".equals(betStr0 + betStr) && num1 % 2 == 0) {
                    winStr.append(betStr).append(",");
                } else if ("?????????".equals(betStr0 + betStr) && num1 > Integer.valueOf(sgNumArr[9])) {
                    winStr.append(betStr).append(",");
                } else if ("?????????".equals(betStr0 + betStr) && num1 < Integer.valueOf(sgNumArr[9])) {
                    winStr.append(betStr).append(",");
                } else if ("?????????".equals(betStr0 + betStr) && num2 > 5) {
                    winStr.append(betStr).append(",");
                } else if ("?????????".equals(betStr0 + betStr) && num2 <= 5) {
                    winStr.append(betStr).append(",");
                } else if ("?????????".equals(betStr0 + betStr) && num2 % 2 == 1) {
                    winStr.append(betStr).append(",");
                } else if ("?????????".equals(betStr0 + betStr) && num2 % 2 == 0) {
                    winStr.append(betStr).append(",");
                } else if ("?????????".equals(betStr0 + betStr) && num2 > Integer.valueOf(sgNumArr[8])) {
                    winStr.append(betStr).append(",");
                } else if ("?????????".equals(betStr0 + betStr) && num2 < Integer.valueOf(sgNumArr[8])) {
                    winStr.append(betStr).append(",");
                } else if ("????????????".equals(betStr0 + betStr) && da(sgNumArr[2])) {
                    winStr.append(betStr).append(",");
                } else if ("????????????".equals(betStr0 + betStr) && !da(sgNumArr[2])) {
                    winStr.append(betStr).append(",");
                } else if ("????????????".equals(betStr0 + betStr) && dan(sgNumArr[2])) {
                    winStr.append(betStr).append(",");
                } else if ("????????????".equals(betStr0 + betStr) && !dan(sgNumArr[2])) {
                    winStr.append(betStr).append(",");
                } else if ("????????????".equals(betStr0 + betStr) && !hu(sgNumArr[2], sgNumArr[7])) {
                    winStr.append(betStr).append(",");
                } else if ("????????????".equals(betStr0 + betStr) && hu(sgNumArr[2], sgNumArr[7])) {
                    winStr.append(betStr).append(",");
                } else if ("????????????".equals(betStr0 + betStr) && da(sgNumArr[3])) {
                    winStr.append(betStr).append(",");
                } else if ("????????????".equals(betStr0 + betStr) && !da(sgNumArr[3])) {
                    winStr.append(betStr).append(",");
                } else if ("????????????".equals(betStr0 + betStr) && dan(sgNumArr[3])) {
                    winStr.append(betStr).append(",");
                } else if ("????????????".equals(betStr0 + betStr) && !dan(sgNumArr[3])) {
                    winStr.append(betStr).append(",");
                } else if ("????????????".equals(betStr0 + betStr) && !hu(sgNumArr[3], sgNumArr[6])) {
                    winStr.append(betStr).append(",");
                } else if ("????????????".equals(betStr0 + betStr) && hu(sgNumArr[3], sgNumArr[6])) {
                    winStr.append(betStr).append(",");
                } else if ("????????????".equals(betStr0 + betStr) && da(sgNumArr[4])) {
                    winStr.append(betStr).append(",");
                } else if ("????????????".equals(betStr0 + betStr) && !da(sgNumArr[4])) {
                    winStr.append(betStr).append(",");
                } else if ("????????????".equals(betStr0 + betStr) && dan(sgNumArr[4])) {
                    winStr.append(betStr).append(",");
                } else if ("????????????".equals(betStr0 + betStr) && !dan(sgNumArr[4])) {
                    winStr.append(betStr).append(",");
                } else if ("????????????".equals(betStr0 + betStr) && !hu(sgNumArr[4], sgNumArr[5])) {
                    winStr.append(betStr).append(",");
                } else if ("????????????".equals(betStr0 + betStr) && hu(sgNumArr[4], sgNumArr[5])) {
                    winStr.append(betStr).append(",");
                } else if ("????????????".equals(betStr0 + betStr) && da(sgNumArr[5])) {
                    winStr.append(betStr).append(",");
                } else if ("????????????".equals(betStr0 + betStr) && !da(sgNumArr[5])) {
                    winStr.append(betStr).append(",");
                } else if ("????????????".equals(betStr0 + betStr) && dan(sgNumArr[5])) {
                    winStr.append(betStr).append(",");
                } else if ("????????????".equals(betStr0 + betStr) && !dan(sgNumArr[5])) {
                    winStr.append(betStr).append(",");
                } else if ("????????????".equals(betStr0 + betStr) && da(sgNumArr[6])) {
                    winStr.append(betStr).append(",");
                } else if ("????????????".equals(betStr0 + betStr) && !da(sgNumArr[6])) {
                    winStr.append(betStr).append(",");
                } else if ("????????????".equals(betStr0 + betStr) && dan(sgNumArr[6])) {
                    winStr.append(betStr).append(",");
                } else if ("????????????".equals(betStr0 + betStr) && !dan(sgNumArr[6])) {
                    winStr.append(betStr).append(",");
                } else if ("????????????".equals(betStr0 + betStr) && da(sgNumArr[7])) {
                    winStr.append(betStr).append(",");
                } else if ("????????????".equals(betStr0 + betStr) && !da(sgNumArr[7])) {
                    winStr.append(betStr).append(",");
                } else if ("????????????".equals(betStr0 + betStr) && dan(sgNumArr[7])) {
                    winStr.append(betStr).append(",");
                } else if ("????????????".equals(betStr0 + betStr) && !dan(sgNumArr[7])) {
                    winStr.append(betStr).append(",");
                } else if ("????????????".equals(betStr0 + betStr) && da(sgNumArr[8])) {
                    winStr.append(betStr).append(",");
                } else if ("????????????".equals(betStr0 + betStr) && !da(sgNumArr[8])) {
                    winStr.append(betStr).append(",");
                } else if ("????????????".equals(betStr0 + betStr) && dan(sgNumArr[8])) {
                    winStr.append(betStr).append(",");
                } else if ("????????????".equals(betStr0 + betStr) && !dan(sgNumArr[8])) {
                    winStr.append(betStr).append(",");
                } else if ("????????????".equals(betStr0 + betStr) && da(sgNumArr[9])) {
                    winStr.append(betStr).append(",");
                } else if ("????????????".equals(betStr0 + betStr) && !da(sgNumArr[9])) {
                    winStr.append(betStr).append(",");
                } else if ("????????????".equals(betStr0 + betStr) && dan(sgNumArr[9])) {
                    winStr.append(betStr).append(",");
                } else if ("????????????".equals(betStr0 + betStr) && !dan(sgNumArr[9])) {
                    winStr.append(betStr).append(",");
                }
            }
        } else if (this.generationSSCPlayId(PLAY_ID_GYH, lotteryId).equals(playId)) {
            for (String betStr : betNumArr) {
                if (betStr.contains("@")) {
                    betStr = betStr.split("@")[1];
                }
                switch (betStr) {
                    case "?????????":
                        if (he > 11) {
                            winStr.append(betStr).append(",");
                        }
                        break;
                    case "?????????":
                        if (he <= 11) {
                            winStr.append(betStr).append(",");
                        }
                        break;
                    case "?????????":
                        if (he % 2 == 1) {
                            winStr.append(betStr).append(",");
                        }
                        break;
                    case "?????????":
                        if (he % 2 == 0) {
                            winStr.append(betStr).append(",");
                        }
                        break;
                    default:
                        try {
                            int betInt = Integer.valueOf(betStr);
                            if (he == betInt) {
                                winStr.append(he).append(",");
                            }
                        } catch (Exception e) {
                            logger.error("???????????????????????? {} ", betStr, e);
                        }

                        break;
                }
            }
        }

        if (winStr.length() > 0) {

            return winStr.substring(0, winStr.length() - 1);
        }
        return null;
    }

    /**
     * ????????????????????????
     *
     * @param num
     * @return
     */
    public boolean dan(String num) {
        return Integer.valueOf(num) % 2 == 1;
    }

    /**
     * ????????????????????????
     *
     * @param num
     * @return
     */
    public boolean da(String num) {
        return Integer.valueOf(num) > 5;
    }

    /**
     * ????????????????????????
     *
     * @param num1
     * @param num2
     * @return
     */
    public boolean hu(String num1, String num2) {
        return Integer.valueOf(num1) < Integer.valueOf(num2);
    }

    /**
     * -????????? ??????ID??????
     *
     * @param playNumber
     * @param lotteryId
     * @return
     */
    private Integer generationSSCPlayId(String playNumber, Integer lotteryId) {
        return Integer.parseInt(lotteryId + playNumber);
    }

    /**
     * ????????? ??????ID??????
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

}
