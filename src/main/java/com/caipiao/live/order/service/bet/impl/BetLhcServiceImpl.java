package com.caipiao.live.order.service.bet.impl;
import com.caipiao.live.common.mybatis.mapper.AmlhcLotterySgMapper;
import com.caipiao.live.common.mybatis.mapperext.sg.AmlhcLotterySgMapperExt;
import com.caipiao.live.order.service.bet.BetCommonService;
import com.caipiao.live.order.service.bet.BetLhcService;
import com.caipiao.live.order.service.lottery.LotteryPlayOddsWriteService;
import com.caipiao.live.order.service.lottery.LotteryPlayWriteService;
import com.caipiao.live.order.service.lottery.LotteryWriteService;
import com.caipiao.live.order.service.order.OrderWriteService;
import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.constant.LotteryResultStatus;
import com.caipiao.live.common.constant.RedisKeys;
import com.caipiao.live.common.model.dto.order.OrderBetStatus;
import com.caipiao.live.common.mybatis.entity.*;
import com.caipiao.live.common.mybatis.mapper.FivelhcLotterySgMapper;
import com.caipiao.live.common.mybatis.mapper.OrderBetRecordMapper;
import com.caipiao.live.common.mybatis.mapper.OrderRecordMapper;
import com.caipiao.live.common.mybatis.mapperext.sg.FivelhcLotterySgMapperExt;
import com.caipiao.live.common.util.DateUtils;
import com.caipiao.live.common.util.MathUtil;
import com.caipiao.live.common.util.TimeHelper;
import com.caipiao.live.common.util.lottery.LhcPlayRule;
import com.caipiao.live.common.util.lottery.LhcUtils;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.util.CollectionUtils;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.caipiao.live.common.util.ViewUtil.getTradeOffAmount;

/**
 * @author lzy
 * @create 2018-09-18 15:21
 **/
@Service
public class BetLhcServiceImpl implements BetLhcService {
    private static final Logger logger = LoggerFactory.getLogger(BetLhcServiceImpl.class);
    // ??????id???4 ?????????
    // private final Integer lotteryId = 4;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private OrderRecordMapper orderRecordMapper;
    @Autowired
    private AmlhcLotterySgMapper amlhcLotterySgMapper;
    @Autowired
    private AmlhcLotterySgMapperExt amlhcLotterySgMapperExt;
    @Autowired
    private OrderWriteService orderWriteService;
    @Autowired
    private OrderBetRecordMapper orderBetRecordMapper;
    @Autowired
    private LotteryPlayOddsWriteService lotteryPlayOddsService;
    @Autowired
    private LotteryPlayWriteService lotteryPlayWriteService;
    @Autowired
    private LotteryWriteService lotteryWriteService;
    @Autowired
    private BetCommonService betCommonService;
    @Autowired
    private FivelhcLotterySgMapper fivelhcLotterySgMapper;
    @Autowired
    private FivelhcLotterySgMapperExt fivelhcLotterySgMapperExt;

    // ?????????????????????A??????id
    private final String PLAY_ID_TM_TMA = "01";
    // ?????????????????????A????????????id
    private final String PLAY_ID_TM_TMA_LM = "02";
    // ?????????????????????A??????id
    public final static String PLAY_ID_ZM_ZMA = "03";
    // ?????????????????????A????????????id
    //private String PLAY_ID_ZM_ZMA_LM = 244;
    // ?????????????????????1-6??????id??????
    //private final List<Integer> PLAY_IDS_ZM_OTS = Lists.newArrayList(
    //	120405, 120406, 120407, 120408, 120409, 120410);
    // ?????????????????????1-6??????id??????
    private final String PLAY_IDS_ZM = "04";
    // ?????????????????????1??????id
    //private String PLAY_ID_ZM_ONE = 120405;
    // ?????????????????????2??????id
    //private String PLAY_ID_ZM_TWO = 120406;
    // ?????????????????????3??????id
    //private String PLAY_ID_ZM_THREE = 120407;
    // ?????????????????????4??????id
    //private String PLAY_ID_ZM_FOUR = 120408;
    // ?????????????????????5??????id
    //private String PLAY_ID_ZM_FIVE = 120409;
    // ?????????????????????6??????id
    //private String PLAY_ID_ZM_SIX = 120410;
    // ??????????????????(1-6)?????????id??????
    private final List<String> PLAY_IDS_ZT_OTS = Lists.newArrayList("05", "06", "07", "08", "09", "10");
    // ??????????????????????????????id
    private final String PLAY_ID_ZT_ONE = "05";
    // ??????????????????????????????id
    private final String PLAY_ID_ZT_TWO = "06";
    // ??????????????????????????????id
    private final String PLAY_ID_ZT_THREE = "07";
    // ??????????????????????????????id
    private final String PLAY_ID_ZT_FOUR = "08";
    // ??????????????????????????????id
    private final String PLAY_ID_ZT_FIVE = "09";
    // ??????????????????????????????id
    private final String PLAY_ID_ZT_SIX = "10";

    // ??????????????????(1-6)???????????????id??????
    private final List<Integer> PLAY_IDS_ZT_OTS_LM = Lists.newArrayList(338, 339, 331, 335, 336, 337);
    // ????????????????????????????????????id
    // private String PLAY_ID_ZT_ONE_LM = 338;
    // ????????????????????????????????????id
    //private String PLAY_ID_ZT_TWO_LM = 339;
    // ????????????????????????????????????id
    //private String PLAY_ID_ZT_THREE_LM = 331;
    // ????????????????????????????????????id
    //private String PLAY_ID_ZT_FOUR_LM = 335;
    // ????????????????????????????????????id
    //private String PLAY_ID_ZT_FIVE_LM = 336;
    // ????????????????????????????????????id
    //private String PLAY_ID_ZT_SIX_LM = 337;

    // ????????????????????????,?????????,????????????id??????
    private final List<String> PLAY_IDS_LM_QZ = Lists.newArrayList("15", "14", "13");
    // ???????????????????????????id
    private final String PLAY_ID_LM_TC = "13";
    // ??????????????????????????????id
    private final String PLAY_ID_LM_EQZ = "14";
    // ??????????????????????????????id
    private final String PLAY_ID_LM_SQZ = "15";

    // ????????????????????????,???????????????id??????
    private final List<String> PLAY_IDS_LM_EZ = Lists.newArrayList("11", "12");
    // ??????????????????????????????id
    private final String PLAY_ID_LM_EZT = "12";
    // ??????????????????????????????id
    private final String PLAY_ID_LM_SZE = "11";

    // ???????????????????????????id
    private final String PLAY_ID_BB_RED = "16";
    // ???????????????????????????id
    private final String PLAY_ID_BB_BLUE = "17";
    // ???????????????????????????id
    private final String PLAY_ID_BB_GREEN = "18";

    // ?????????????????????id??????
    private final List<String> PLAY_IDS_NO_OPEN = Lists.newArrayList("21", "22", "23", "24", "25", "26");
    // ????????????????????????id
    private final String PLAY_ID_NO_OPEN_FIVE = "21";
    // ????????????????????????id
    private final String PLAY_ID_NO_OPEN_SIX = "22";
    // ????????????????????????id
    private final String PLAY_ID_NO_OPEN_SEVEN = "23";
    // ????????????????????????id
    private final String PLAY_ID_NO_OPEN_EIGHT = "24";
    // ????????????????????????id
    private final String PLAY_ID_NO_OPEN_NINE = "25";
    // ????????????????????????id
    private final String PLAY_ID_NO_OPEN_TEN = "26";

    // ???????????????????????????id
    private final String PLAY_ID_WS_QW = "19";
    // ???????????????????????????id
    private final String PLAY_ID_WS_TW = "20";
    // ?????????????????????id
    private final String PLAY_ID_PT_PT = "27";
    // ?????????????????????id
    private final String PLAY_ID_TX_TX = "28";
    // ???????????????????????????id
    private final String PLAY_ID_LX_LXLZ = "29";
    // ??????????????????????????????id
    private final String PLAY_ID_LX_LXLBZ = "30";

    // ????????????????????????id??????
    private final List<String> PLAY_IDS_LX_WIN = Lists.newArrayList("31", "33", "35");
    // ???????????????????????????id
    private final String PLAY_ID_LX_TWO_WIN = "31";
    // ???????????????????????????id
    private final String PLAY_ID_LX_THREE_WIN = "33";
    // ???????????????????????????id
    private String PLAY_ID_LX_FOUR_WIN = "35";
    // ???????????????????????????id??????
    private final List<String> PLAY_IDS_LX_NO_WIN = Lists.newArrayList("32", "34", "36");
    // ??????????????????????????????id
    private final String PLAY_ID_LX_TWO_NO_WIN = "32";
    // ??????????????????????????????id
    private final String PLAY_ID_LX_THREE_NO_WIN = "34";
    // ??????????????????????????????id
    private final String PLAY_ID_LX_FOUR_NO_WIN = "36";

    // ????????????????????????id??????
    private final List<String> PLAY_IDS_LW_WIN = Lists.newArrayList("37", "39", "41");
    // ???????????????????????????id
    private final String PLAY_ID_LW_TWO_WIN = "37";
    // ???????????????????????????id
    private final String PLAY_ID_LW_THREE_WIN = "39";
    // ???????????????????????????id
    private final String PLAY_ID_LW_FOUR_WIN = "41";
    // ???????????????????????????id??????
    private final List<String> PLAY_IDS_LW_NO_WIN = Lists.newArrayList("38", "40", "42");
    // ??????????????????????????????id
    private final String PLAY_ID_LW_TWO_NO_WIN = "38";
    // ??????????????????????????????id
    private final String PLAY_ID_LW_THREE_NO_WIN = "40";
    // ??????????????????????????????id
    private final String PLAY_ID_LW_FOUR_NO_WIN = "42";

    // ?????????1-6????????????id
    private final String PLAY_ID_ONE_SIX_LH = "43";
    // ?????????????????????id
    private final String PLAY_ID_WUXING = "44";

    // ???????????????????????????
    private final List<String> MAYBE_HE = Lists.newArrayList("???", "???", "???", "???", "??????", "??????", "??????", "??????", "??????", "??????");


    /**
     * ??????????????????????????????A???
     *
     * @param issue  ??????
     * @param number ????????????
     */
    @Override
    public void clearingLhcTeMaA(String issue, String number, int lotteryId, boolean jiesuanOrNot) {
        // ????????????A
        clearingLhcOnePlayOdds(issue, number, this.generationLHCPlayId(PLAY_ID_TM_TMA, lotteryId), lotteryId, jiesuanOrNot);
        // ????????????A??????
        clearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_TM_TMA_LM, lotteryId), lotteryId, jiesuanOrNot);
    }

    @Override
    public void clearingLhcZhengMaA(String issue, String number, int lotteryId, boolean jiesuanOrNot) {
        // ????????????A??????
        clearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_ZM_ZMA, lotteryId), lotteryId, jiesuanOrNot);
        // ????????????A
        clearingLhcOnePlayOdds(issue, number, this.generationLHCPlayId(PLAY_ID_ZM_ZMA, lotteryId), lotteryId, jiesuanOrNot);
    }

    /**
     * ????????????????????????1-6???
     *
     * @param issue ??????
     */
    @Override
    public void clearingLhcZhengMaOneToSix(String issue, String number, int lotteryId, boolean jiesuanOrNot) {
        clearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_IDS_ZM, lotteryId), lotteryId, jiesuanOrNot);
        /**
         clearingOnePlayManyOdds(issue, number, PLAY_ID_ZM_THREE,lotteryId);
         clearingOnePlayManyOdds(issue, number, PLAY_ID_ZM_FOUR,lotteryId);
         clearingOnePlayManyOdds(issue, number, PLAY_ID_ZM_FIVE,lotteryId);
         clearingOnePlayManyOdds(issue, number, PLAY_ID_ZM_SIX,lotteryId);
         **/
    }

    /**
     * ???????????????????????????
     *
     * @param issue ??????
     */
    @Override
    public void clearingLhcZhengTe(String issue, String number, int lotteryId, boolean jiesuanOrNot) {
        // ???(1-6)???
        clearingOnePlayOneOdds(issue, number, this.generationLHCPlayIdList(PLAY_IDS_ZT_OTS, lotteryId), lotteryId, jiesuanOrNot);
        // ???(1-6)??? ??????
        clearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_ZT_ONE, lotteryId), lotteryId, jiesuanOrNot);
        clearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_ZT_TWO, lotteryId), lotteryId, jiesuanOrNot);
        clearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_ZT_THREE, lotteryId), lotteryId, jiesuanOrNot);
        clearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_ZT_FOUR, lotteryId), lotteryId, jiesuanOrNot);
        clearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_ZT_FIVE, lotteryId), lotteryId, jiesuanOrNot);
        clearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_ZT_SIX, lotteryId), lotteryId, jiesuanOrNot);
    }

    /**
     * ???????????????????????????
     *
     * @param issue ??????
     */
    @Override
    public void clearingLhcLianMa(String issue, String number, int lotteryId, boolean jiesuanOrNot) {
        // ?????????,?????????,??????
        clearingOnePlayOneOdds(issue, number, this.generationLHCPlayIdList(PLAY_IDS_LM_QZ, lotteryId), lotteryId, jiesuanOrNot);
        // ?????????,?????????
        clearingOnePlayTwoOdds(issue, number, this.generationLHCPlayIdList(PLAY_IDS_LM_EZ, lotteryId), lotteryId, jiesuanOrNot);
    }

    /**
     * ???????????????????????????
     *
     * @param issue ??????
     */
    @Override
    public void clearingLhcBanBo(String issue, String number, int lotteryId, boolean jiesuanOrNot) {
        clearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_BB_RED, lotteryId), lotteryId, jiesuanOrNot);
        clearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_BB_BLUE, lotteryId), lotteryId, jiesuanOrNot);
        clearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_BB_GREEN, lotteryId), lotteryId, jiesuanOrNot);
    }

    /**
     * ???????????????????????????
     *
     * @param issue ??????
     */
    @Override
    public void clearingLhcWs(String issue, String number, int lotteryId, boolean jiesuanOrNot) {
        clearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_WS_QW, lotteryId), lotteryId, jiesuanOrNot);
        clearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_WS_TW, lotteryId), lotteryId, jiesuanOrNot);
    }

    /**
     * ???????????????????????????
     *
     * @param issue ??????
     */
    @Override
    public void clearingLhcNoOpen(String issue, String number, int lotteryId, boolean jiesuanOrNot) {
        clearingOnePlayOneOdds(issue, number, this.generationLHCPlayIdList(PLAY_IDS_NO_OPEN, lotteryId), lotteryId, jiesuanOrNot);
    }

    /**
     * ?????????????????????????????????
     *
     * @param issue ??????
     */
    @Override
    public void clearingLhcPtPt(String issue, String number, int lotteryId, boolean jiesuanOrNot) {
        clearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_PT_PT, lotteryId), lotteryId, jiesuanOrNot);
    }

    /**
     * ?????????????????????????????????
     *
     * @param issue ??????
     */
    @Override
    public void clearingLhcTxTx(String issue, String number, int lotteryId, boolean jiesuanOrNot) {
        clearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_TX_TX, lotteryId), lotteryId, jiesuanOrNot);
    }

    /**
     * ???????????????????????????
     *
     * @param issue ??????
     */
    @Override
    public void clearingLhcLiuXiao(String issue, String number, int lotteryId, boolean jiesuanOrNot) {
        clearingLhcOnePlayOdds(issue, number, this.generationLHCPlayId(PLAY_ID_LX_LXLZ, lotteryId), lotteryId, jiesuanOrNot);
        clearingLhcOnePlayOdds(issue, number, this.generationLHCPlayId(PLAY_ID_LX_LXLBZ, lotteryId), lotteryId, jiesuanOrNot);
    }

    /**
     * ??????????????????????????????
     *
     * @param issue ??????
     */
    @Override
    public void clearingLhcLianXiao(String issue, String number, int lotteryId, boolean jiesuanOrNot) {
        clearingOnePlayTwoOdds(issue, number, this.generationLHCPlayIdList(PLAY_IDS_LX_WIN, lotteryId), lotteryId, jiesuanOrNot);
        clearingOnePlayOneOdds(issue, number, this.generationLHCPlayIdList(PLAY_IDS_LX_NO_WIN, lotteryId), lotteryId, jiesuanOrNot);
    }

    /**
     * ??????????????????????????????
     *
     * @param issue ??????
     */
    @Override
    public void clearingLhcLianWei(String issue, String number, int lotteryId, boolean jiesuanOrNot) {
        clearingOnePlayOneOdds(issue, number, this.generationLHCPlayIdList(PLAY_IDS_LW_WIN, lotteryId), lotteryId, jiesuanOrNot);
        clearingOnePlayTwoOdds(issue, number, this.generationLHCPlayIdList(PLAY_IDS_LW_NO_WIN, lotteryId), lotteryId, jiesuanOrNot);
    }

    /**
     * ??????????????????1-6?????????
     *
     * @param issue ??????
     */
    @Override
    public void clearingLhcOneSixLh(String issue, String number, int lotteryId, boolean jiesuanOrNot) {
        clearingLhcOnePlayOdds(issue, number, this.generationLHCPlayId(PLAY_ID_ONE_SIX_LH, lotteryId), lotteryId, jiesuanOrNot);
    }

    /**
     * ???????????????????????????
     *
     * @param issue ??????
     */
    @Override
    public void clearingLhcWuxing(String issue, String number, int lotteryId, boolean jiesuanOrNot) {
        clearingLhcOnePlayOdds(issue, number, this.generationLHCPlayId(PLAY_ID_WUXING, lotteryId), lotteryId, jiesuanOrNot);
    }

    /**
     * ??????????????????????????????,???????????????????????????????????????
     *
     * @param issue   ??????
     * @param number  ????????????
     * @param playIds ??????id??????
     */
    @SuppressWarnings("rawtypes")
    private void clearingOnePlayOneOdds(String issue, String number, List<Integer> playIds, int lotteryId, boolean jiesuanOrNot) {
        // ????????????????????????
        String date = DateUtils.formatDate(new Date(), "yyyy-MM-dd");
        // ???????????????????????????
        int ordercount = orderWriteService.countOrderBetList(issue, playIds, String.valueOf(lotteryId), OrderBetStatus.WAIT);
        if (ordercount <= 0) {
            return;
        }

        //  List<OrderRecord> orderRecords = orderWriteService.selectOrdersPage(lotteryId, issue, OrderStatus.NORMAL, i);
        //   Map<Integer, OrderRecord> orderMap = new HashMap<>();
        // ??????????????????id??????
        //    List<Integer> orderIds = new ArrayList<>();
        //    this.updateOrder(number, orderRecords, orderIds, orderMap);
        // ??????????????????
        double divisor = betCommonService.getDivisor(lotteryId);
        // ????????????????????????????????????
        List<OrderBetRecord> orderBetRecords = orderWriteService.selectOrderBetList(issue, String.valueOf(lotteryId), playIds, OrderBetStatus.WAIT, "OnePlayOne");
        if (!jiesuanOrNot) {
            if (orderBetRecords.size() >= 2) {
                orderBetRecords = orderBetRecords.subList(0, 2);
            }
        }

        int whileMax = ordercount / Constants.CLEARNUM + 1;   //??????????????????
        int whileNumber = 0;
        while (!CollectionUtils.isEmpty(orderBetRecords)) {
            whileNumber++;
            if (whileNumber > whileMax) {
                break;
            }
            int continueSize = 0;
            // ????????????????????????
            for (OrderBetRecord orderBet : orderBetRecords) {
                // ??????????????????id
                List<Integer> settingIds = new ArrayList<>();
                settingIds.add(orderBet.getSettingId());
                String betNumber = orderBet.getBetNumber();
                boolean playType = betNumber.indexOf("??????") >= 0;
                LotteryPlayOdds odds;
                Map oddsMap;
                if (playType) {
                    oddsMap = lotteryPlayOddsService.selectPlayOddsBySettingIds(settingIds);
                    odds = (LotteryPlayOdds) oddsMap.get(settingIds.get(0));
                } else {
                    oddsMap = lotteryPlayOddsService.selectPlayOddsBySettingId(settingIds.get(0));
                    odds = (LotteryPlayOdds) oddsMap.get(betNumber.split("@")[1]);
                    if (odds == null) {
                        odds = (LotteryPlayOdds) oddsMap.get(betNumber.split("@")[0]);
                    }
                    if (odds == null) {
                        // ??????1 ??????2...
                        odds = (LotteryPlayOdds) oddsMap.get(betNumber.split("@")[1].split(",")[0]);
                    }
                }
                BigDecimal winAmount = new BigDecimal(0);
                try {
                    String betNumberLin = orderBet.getBetNumber().replace("???", "1").replace("???", "2")
                            .replace("???", "3").replace("???", "4").replace("???", "5").replace("???", "6");
                    if (betNumberLin.indexOf("??????") >= 0
                            //????????????????????? ??????????????????
                            || ((betNumberLin.indexOf("???1???") >= 0 || betNumberLin.indexOf("???2???") >= 0 || betNumberLin.indexOf("???3???") >= 0
                            || betNumberLin.indexOf("???4???") >= 0 || betNumberLin.indexOf("???5???") >= 0 || betNumberLin.indexOf("???6???") >= 0)
                            &&
                            (betNumberLin.indexOf("???") >= 0 || betNumberLin.indexOf("???") >= 0 || betNumberLin.indexOf("???") >= 0
                                    || betNumberLin.indexOf("???") >= 0 || betNumberLin.indexOf("??????") >= 0 || betNumberLin.indexOf("??????") >= 0
                                    || betNumberLin.indexOf("??????") >= 0 || betNumberLin.indexOf("??????") >= 0 || betNumberLin.indexOf("??????") >= 0
                                    || betNumberLin.indexOf("??????") >= 0 || betNumberLin.indexOf("??????") >= 0))
                    ) {
                        continueSize++;
                        continue;
                    }
                    orderBet.setWinCount("0");
                    // ??????????????????,??????????????????
                    int winCounts = LhcUtils.isWinByOnePlayOneOdds(orderBet.getBetNumber(), number, orderBet.getPlayId(), date, lotteryId);
                    if (winCounts > 0) {
                        // ???????????????/????????????
                        String winCount = odds.getWinCount();
                        String totalCount = odds.getTotalCount();
                        // ????????????
                        double odd = Double.parseDouble(totalCount) * 1.0 / Double.parseDouble(winCount) * divisor;
                        //??????????????????
                        winAmount = orderBet.getBetAmount().divide(BigDecimal.valueOf(orderBet.getBetCount()), BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(odd));
                        winAmount = winAmount.multiply(BigDecimal.valueOf(winCounts));
                        orderBet.setWinCount(String.valueOf(winCounts));
                    }
                    // ??????????????????,?????????????????????????????????
                    //  OrderRecord orderRecord = orderMap.get(orderBet.getOrderId());
                    if (jiesuanOrNot) {
                        logger.info("clearingOnePlayOneOdds ??????????????????{};{};{};{};{};{}", orderBet.getOrderSn(), orderBet.getBetNumber(), number, orderBet.getPlayId(), lotteryId, winAmount);
                        betCommonService.winOrLose(orderBet, winAmount, orderBet.getUserId(), orderBet.getOrderSn());
                    } else {
                        //??????????????????
                        String tbStatus = "";
                        if (winAmount.compareTo(orderBet.getBetAmount()) == 0) {
                            tbStatus = OrderBetStatus.HE;
                        } else if (winAmount.compareTo(BigDecimal.ZERO) > 0) {
                            tbStatus = OrderBetStatus.WIN;
                        } else {
                            tbStatus = OrderBetStatus.NO_WIN;
                        }
                        String lotteryName = "";
                        String lotteryPlayName = "";
                        LotteryPlay lotteryPlay = lotteryPlayWriteService.selectPlayById(orderBet.getPlayId());
                        Lottery lottery = lotteryWriteService.selectLotteryById(orderBet.getLotteryId());
                        if (lotteryPlay != null) {
                            lotteryName = lotteryPlay.getName();
                        }
                        if (lottery != null) {
                            lotteryPlayName = lottery.getName();
                        }
                        String message = orderBet.getIssue() + ";" + orderBet.getOrderSn() + ";" + lotteryName + ";" + lotteryPlayName + ";" + orderBet.getBetNumber()
                                + ";" + orderBet.getBetAmount() + ";" + winAmount + ";" + tbStatus;

                        redisTemplate.opsForHash().put("JIESUANORDER", issue + orderBet.getPlayId() + orderBet.getId(), message);
                        redisTemplate.expire("JIESUANORDER", 6, TimeUnit.HOURS);

                    }

                } catch (TransactionSystemException e) {
                    logger.error("?????????????????? ???????????? ???????????????orderSn:{}.", orderBet.getOrderSn(), e);
                    for (int i = 0; i < 20; i++) {
                        try {
                            if (jiesuanOrNot) {
                                betCommonService.winOrLose(orderBet, winAmount, orderBet.getUserId(), orderBet.getOrderSn());
                            }
                        } catch (TransactionSystemException e2) {
                            logger.error("??????????????????,???????????? ???????????????orderSn:{}.", orderBet.getOrderSn(), e2);
                            try {
                                Thread.sleep(100);
                            } catch (Exception e3) {
                            }
                            continue;
                        }
                        break;
                    }
                } catch (Exception e) {
                    logger.error("?????????????????????lotteryId:{},issue:{},betNum:{}", orderBet.getLotteryId(), issue, orderBet.getBetNumber(), e);
                    break;
                }

            }

            if (!jiesuanOrNot) {
                break;
            }
            orderBetRecords = orderWriteService.selectOrderBetList(issue, String.valueOf(lotteryId), playIds, OrderBetStatus.WAIT, "OnePlayOne");
            if (orderBetRecords.size() == continueSize) {
                break;
            }
        }

    }

    /**
     * ????????????????????????????????????,????????????2?????????,???????????????????????????????????????
     *
     * @param issue   ??????
     * @param playIds ??????id??????
     */
    private void clearingOnePlayTwoOdds(String issue, String number, List<Integer> playIds, int lotteryId, boolean jiesuanOrNot) {
        String date = DateUtils.formatDate(new Date(), "yyyy-MM-dd");
//        }

        // ???????????????????????????
        int ordercount = orderWriteService.countOrderBetList(issue, playIds, String.valueOf(lotteryId), OrderBetStatus.WAIT);
        if (ordercount <= 0) {
            return;
        }

        //  List<OrderRecord> orderRecords = orderWriteService.selectOrdersPage(lotteryId, issue, OrderStatus.NORMAL, i);
        //   Map<Integer, OrderRecord> orderMap = new HashMap<>();
        // ??????????????????id??????
        //    List<Integer> orderIds = new ArrayList<>();
        //    this.updateOrder(number, orderRecords, orderIds, orderMap);
        // ??????????????????
        double divisor = betCommonService.getDivisor(lotteryId);
        // ????????????????????????????????????
        List<OrderBetRecord> orderBetRecords = orderWriteService.selectOrderBetList(issue, String.valueOf(lotteryId), playIds, OrderBetStatus.WAIT, "OnePlayTwo");
        if (!jiesuanOrNot) {
            if (orderBetRecords.size() >= 2) {
                orderBetRecords = orderBetRecords.subList(0, 2);
            }
        }

        int whileMax = ordercount / Constants.CLEARNUM + 1;   //??????????????????
        int whileNumber = 0;
        while (!CollectionUtils.isEmpty(orderBetRecords)) {
            whileNumber++;
            if (whileNumber > whileMax) {
                break;
            }
            for (OrderBetRecord orderBet : orderBetRecords) {
                BigDecimal winAmount = new BigDecimal(0);
                try {
                    // ??????????????????id
                    List<Integer> settingIds = new ArrayList<>();

                    settingIds.add(orderBet.getSettingId());


                    // ????????????????????????
                    Map<String, LotteryPlayOdds> oddsMap = lotteryPlayOddsService.selectPlayOddsBySettingId(settingIds.get(0));
                    orderBet.setWinCount("0");
                    // ??????????????????,??????????????????
                    List<Integer> twoOdds = LhcUtils.isWinByOnePlayTwoOdds(orderBet.getBetNumber(), number, orderBet.getPlayId(), date, lotteryId);
                    if (twoOdds.get(0) > 0 || twoOdds.get(1) > 0) {
                        int bigOddsWins = twoOdds.get(0);
                        int smallOddsWins = twoOdds.get(1);
                        orderBet.setWinCount(String.valueOf(bigOddsWins + smallOddsWins));
                        if (bigOddsWins > 0) {
                            // ?????? ??????????????? ????????????
                            LotteryPlayOdds odds = oddsMap.get(oddsMap.keySet().iterator().next());
                            if (oddsMap.size() > 1) {
                                double oddBig = 0;
                                Set<String> sets = oddsMap.keySet();
                                for (String set : sets) {
                                    LotteryPlayOdds thisDomain = (LotteryPlayOdds) oddsMap.get(set);
                                    double thisOddBig = Double.parseDouble(thisDomain.getTotalCount()) / Double.parseDouble(thisDomain.getWinCount());
                                    if (thisOddBig > oddBig) {
                                        oddBig = thisOddBig;
                                        odds = thisDomain;
                                    }
                                }
                            }

                            // ???????????????/???????????? & ????????????
                            List<Double> oddsList = new ArrayList<Double>();
                            String[] totalCountArr = odds.getTotalCount().split("/");
                            String winCount = odds.getWinCount();
                            if (totalCountArr.length < 2) {
                                double odd = Double.parseDouble(totalCountArr[0]) * 1.0 / Double.parseDouble(winCount) * divisor;
                                oddsList.add(odd);
                            } else {
                                double oddBig = Double.parseDouble(totalCountArr[0]) * 1.0 / Double.parseDouble(winCount) * divisor;
                                oddsList.add(oddBig);
                            }
                            BigDecimal bigWins = orderBet.getBetAmount().divide(BigDecimal.valueOf(orderBet.getBetCount())).multiply(BigDecimal.valueOf(oddsList.get(0)));
                            winAmount = winAmount.add(bigWins.multiply(BigDecimal.valueOf(bigOddsWins)));
                        }
                        if (smallOddsWins > 0) {
                            // ?????? ??????????????? ????????????
                            LotteryPlayOdds odds;
                            if (oddsMap.size() == 1) {
                                odds = oddsMap.get(oddsMap.keySet().iterator().next());
                            } else {
                                String shengXiao = LhcUtils.getShengXiao(date);
                                odds = oddsMap.get(shengXiao);
                            }
                            //????????????0????????????
                            if (odds == null) {
                                odds = oddsMap.get("0???");
                            }
                            // ???????????????/???????????? & ????????????
                            List<Double> oddsList = new ArrayList<Double>();
                            String[] totalCountArr = odds.getTotalCount().split("/");
                            String winCount = odds.getWinCount();
                            if (totalCountArr.length < 2) {
                                double odd = Double.parseDouble(totalCountArr[0]) * 1.0 / Double.parseDouble(winCount) * divisor;
                                oddsList.add(odd);
                            } else {
                                double oddSmall = Double.parseDouble(totalCountArr[1]) * 1.0 / Double.parseDouble(winCount) * divisor;
                                oddsList.add(oddSmall);
                            }
                            BigDecimal smallWins = orderBet.getBetAmount().divide(BigDecimal.valueOf(orderBet.getBetCount())).multiply(BigDecimal.valueOf(oddsList.get(0)));
                            winAmount = winAmount.add(smallWins.multiply(BigDecimal.valueOf(smallOddsWins)));
                        }
                    }
                    if (twoOdds.size() == 2) {
                        logger.info("clearingOnePlayTwoOdds ??????????????????{};{};{};{};{};{};{};{}", orderBet.getOrderSn(), orderBet.getBetNumber(), number, orderBet.getPlayId(), lotteryId, twoOdds.get(0), twoOdds.get(1), winAmount);
                    }

                    // ??????????????????,?????????????????????????????????
                    //   OrderRecord orderRecord = orderMap.get(orderBet.getOrderId());
                    if (jiesuanOrNot) {
                        betCommonService.winOrLose(orderBet, winAmount, orderBet.getUserId(), orderBet.getOrderSn());
                    } else {
                        //??????????????????
                        String tbStatus = "";
                        if (winAmount.compareTo(orderBet.getBetAmount()) == 0) {
                            tbStatus = OrderBetStatus.HE;
                        } else if (winAmount.compareTo(BigDecimal.ZERO) > 0) {
                            tbStatus = OrderBetStatus.WIN;
                        } else {
                            tbStatus = OrderBetStatus.NO_WIN;
                        }
                        String lotteryName = "";
                        String lotteryPlayName = "";
                        LotteryPlay lotteryPlay = lotteryPlayWriteService.selectPlayById(orderBet.getPlayId());
                        Lottery lottery = lotteryWriteService.selectLotteryById(orderBet.getLotteryId());
                        if (lotteryPlay != null) {
                            lotteryName = lotteryPlay.getName();
                        }
                        if (lottery != null) {
                            lotteryPlayName = lottery.getName();
                        }
                        String message = orderBet.getIssue() + ";" + orderBet.getOrderSn() + ";" + lotteryName + ";" + lotteryPlayName + ";" + orderBet.getBetNumber()
                                + ";" + orderBet.getBetAmount() + ";" + winAmount + ";" + tbStatus;

                        redisTemplate.opsForHash().put("JIESUANORDER", issue + orderBet.getPlayId() + orderBet.getId(), message);
                        redisTemplate.expire("JIESUANORDER", 6, TimeUnit.HOURS);

                    }
                } catch (TransactionSystemException e) {
                    logger.error("?????????????????? ???????????? ????????????:{},{}", orderBet.getOrderSn(), e);
                    for (int i = 0; i < 20; i++) {
                        try {
                            if (jiesuanOrNot) {
                                betCommonService.winOrLose(orderBet, winAmount, orderBet.getUserId(), orderBet.getOrderSn());
                            }
                        } catch (TransactionSystemException e2) {
                            logger.error("??????????????????:{},???????????? ????????????:{},{}", orderBet.getOrderSn(), e2);
                            try {
                                Thread.sleep(100);
                            } catch (Exception e3) {

                            }
                            continue;
                        }
                        break;
                    }
                } catch (Exception e) {
                    logger.error("?????????????????????lotteryId:{},issue:{},betNum:{}", orderBet.getLotteryId(), issue, orderBet.getBetNumber(), e);
                    break;
                }
            }

            if (!jiesuanOrNot) {
                break;
            }
            orderBetRecords = orderWriteService.selectOrderBetList(issue, String.valueOf(lotteryId), playIds, OrderBetStatus.WAIT, "OnePlayTwo");
        }

    }

    /**
     * ??????????????????????????????,?????????????????????????????????
     *
     * @param issue        ??????
     * @param number       ????????????
     * @param playId       ??????id
     * @param jiesuanOrNot ??????????????????
     */
    private void clearingLhcOnePlayOdds(String issue, String number, Integer playId, int lotteryId, boolean jiesuanOrNot) {
        long begin = System.currentTimeMillis();
        logger.debug("????????????......");
        String date = DateUtils.formatDate(new Date(), "yyyy-MM-dd");
        if (jiesuanOrNot) {
            int upcount = orderWriteService.updateOrderRecord(String.valueOf(lotteryId), issue, number);
            while (upcount > 0) {
                upcount = orderWriteService.updateOrderRecord(String.valueOf(lotteryId), issue, number);
            }
        }

        logger.debug("updateOrderRecord time, {}", System.currentTimeMillis() - begin);
        // ???????????????????????????
        List playlist = new ArrayList();
        playlist.add(playId);
        int ordercount = orderWriteService.countOrderBetList(issue, playlist, String.valueOf(lotteryId), OrderBetStatus.WAIT);
        logger.debug("countOrderBetList time, {}", System.currentTimeMillis() - begin);
        if (ordercount <= 0) {
            return;
        }

        //  List<OrderRecord> orderRecords = orderWriteService.selectOrdersPage(lotteryId, issue, OrderStatus.NORMAL, i);
        //   Map<Integer, OrderRecord> orderMap = new HashMap<>();
        // ??????????????????id??????
        //    List<Integer> orderIds = new ArrayList<>();
        //    this.updateOrder(number, orderRecords, orderIds, orderMap);
        // ??????????????????
        double divisor = betCommonService.getDivisor(lotteryId);
        // ????????????????????????????????????
        // ????????????????????????????????????
        List<OrderBetRecord> orderBetRecords = orderWriteService.selectOrderBetList(issue, String.valueOf(lotteryId), playlist, OrderBetStatus.WAIT, "OnePlay");
        if (!jiesuanOrNot) {
            if (orderBetRecords.size() >= 2) {
                orderBetRecords = orderBetRecords.subList(0, 2);
            }
        }

        OrderBetRecord order = new OrderBetRecord();
        int whileMax = ordercount / Constants.CLEARNUM + 1;   //??????????????????
        int whileNumber = 0;
        while (!CollectionUtils.isEmpty(orderBetRecords)) {
            whileNumber++;
            if (whileNumber > whileMax) {
                break;
            }
            try {
                logger.debug("selectOrderBetList time, {}", System.currentTimeMillis() - begin);

                if (this.generationLHCPlayId(PLAY_ID_LX_LXLZ, lotteryId).equals(playId) ||
                        this.generationLHCPlayId(PLAY_ID_LX_LXLBZ, lotteryId).equals(playId)) {
                    // ????????????????????????????????????????????????,????????????49?????????
                    if ("49".equals(number.split(",")[6])) {
                        betCommonService.noWinOrLose(orderBetRecords);
                        orderBetRecords = orderWriteService.selectOrderBetList(issue, String.valueOf(lotteryId), playlist, OrderBetStatus.WAIT, "OnePlay");
                    }
                }

                // ????????????id
                if (orderBetRecords.size() == 0) {
                    return;
                }
                Integer settingId = orderBetRecords.get(0).getSettingId();

                // ??????????????????
                LotteryPlayOdds odds = lotteryPlayOddsService.findPlayOddsBySettingId(settingId);
                String shengXiao = LhcUtils.getShengXiao(DateUtils.formatDate(new Date(), "yyyy-MM-dd"));
                boolean flag = orderBetRecords.get(0).getBetNumber().indexOf(shengXiao) >= 0;
                if (flag) {
                    odds = lotteryPlayOddsService.selectPlayOddsBySettingId(settingId).get(shengXiao);
                }
                for (OrderBetRecord orderBet : orderBetRecords) {
                    if (orderBet.getPlayId() != null && orderBet.getPlayId().equals(120101)) {
                        Map<String, LotteryPlayOdds> oddsMap = lotteryPlayOddsService.selectPlayOddsBySettingId(orderBet.getSettingId());
                        if (oddsMap.size() == 49) {
                            odds = oddsMap.get(String.valueOf(number.split(",")[6]));
                        }
                    }

                    order = orderBet;

                    if (orderBet.getBetNumber().indexOf("??????") >= 0) {
                        continue;
                    }
                    if (orderBet.getBetNumber().contains("??????")) {
                        List<LotteryPlayOdds> oddList = lotteryPlayOddsService.selectOddsListBySettingId(settingId);
                        Map<String, LotteryPlayOdds> oddsMap = new HashMap<>();
                        for (LotteryPlayOdds lotteryPlayodds : oddList) {
                            oddsMap.put(lotteryPlayodds.getName(), lotteryPlayodds);
                        }
                        String betName = orderBet.getBetNumber().replace("??????@", "");
                        odds = oddsMap.get(betName);
                    }
                    BigDecimal winAmount = new BigDecimal(0);
                    orderBet.setWinCount("0");
                    // ??????????????????,??????????????????
                    int winCounts = LhcUtils.isWinByOnePlayOneOdds(orderBet.getBetNumber(), number, orderBet.getPlayId(), date, lotteryId);
                    if (winCounts > 0) {
                        // ???????????????/????????????
                        String winCount = odds.getWinCount();
                        String totalCount = odds.getTotalCount();
                        // ????????????
                        double odd = Double.parseDouble(totalCount) * 1.0 / Double.parseDouble(winCount) * divisor;
                        //??????????????????
                        winAmount = orderBet.getBetAmount().divide(BigDecimal.valueOf(orderBet.getBetCount())).multiply(BigDecimal.valueOf(odd));
                        winAmount = winAmount.multiply(BigDecimal.valueOf(winCounts));
                        orderBet.setWinCount(String.valueOf(winCounts));
                    }

                    // ??????????????????,?????????????????????????????????
                    //  OrderRecord orderRecord = orderMap.get(orderBet.getOrderId());
                    try {
                        if (jiesuanOrNot) {
                            logger.info("clearingLhcOnePlayOdds ??????????????????{};{};{};{};{};{};{}", orderBet.getOrderSn(), orderBet.getBetNumber(), number, orderBet.getPlayId(), lotteryId, winCounts, winAmount);
                            betCommonService.winOrLose(orderBet, winAmount, orderBet.getUserId(), orderBet.getOrderSn());
                        } else {
                            //??????????????????
                            String tbStatus = "";
                            if (winAmount.compareTo(orderBet.getBetAmount()) == 0) {
                                tbStatus = OrderBetStatus.HE;
                            } else if (winAmount.compareTo(BigDecimal.ZERO) > 0) {
                                tbStatus = OrderBetStatus.WIN;
                            } else {
                                tbStatus = OrderBetStatus.NO_WIN;
                            }
                            String lotteryName = "";
                            String lotteryPlayName = "";
                            LotteryPlay lotteryPlay = lotteryPlayWriteService.selectPlayById(orderBet.getPlayId());
                            Lottery lottery = lotteryWriteService.selectLotteryById(orderBet.getLotteryId());
                            if (lotteryPlay != null) {
                                lotteryName = lotteryPlay.getName();
                            }
                            if (lottery != null) {
                                lotteryPlayName = lottery.getName();
                            }
                            String message = orderBet.getIssue() + ";" + orderBet.getOrderSn() + ";" + lotteryName + ";" + lotteryPlayName + ";" + orderBet.getBetNumber()
                                    + ";" + orderBet.getBetAmount() + ";" + winAmount + ";" + tbStatus;

                            redisTemplate.opsForHash().put("JIESUANORDER", issue + orderBet.getPlayId() + orderBet.getId(), message);
                            redisTemplate.expire("JIESUANORDER", 6, TimeUnit.HOURS);
                        }

                    } catch (TransactionSystemException e1) {
                        logger.error("?????????????????? ???????????? ????????????:{},{}", orderBet.getOrderSn(), e1);
                        for (int i = 0; i < 20; i++) {
                            try {
                                if (jiesuanOrNot) {
                                    betCommonService.winOrLose(orderBet, winAmount, orderBet.getUserId(), orderBet.getOrderSn());
                                }
                            } catch (TransactionSystemException e2) {
                                logger.error("??????????????????:{},???????????? ????????????:{},{}", i, orderBet.getOrderSn(), e2);
                                Thread.sleep(100);
                                continue;
                            }
                            break;
                        }
                    }
                    logger.debug("winOrLose time, {}", System.currentTimeMillis() - begin);
                }

                if (!jiesuanOrNot) {
                    break;
                }
                orderBetRecords = orderWriteService.selectOrderBetList(issue, String.valueOf(lotteryId), playlist, OrderBetStatus.WAIT, "OnePlay");

            } catch (Exception e) {
                logger.error("?????????????????????lotteryId:{},issue:{},betNum:{},{}", order.getLotteryId(), issue, order.getBetNumber(), e);
                break;
            }
        }

    }

    /**
     * ????????????????????????,?????????????????????????????????????????????
     *
     * @param issue
     * @param playId
     */

    private void clearingOnePlayManyOdds(String issue, String number, Integer playId, int lotteryId, boolean jiesuanOrNot) {
        String date = DateUtils.formatDate(new Date(), "yyyy-MM-dd");
        // ???????????????????????????
        List playlist = new ArrayList();
        playlist.add(playId);
        int ordercount = orderWriteService.countOrderBetList(issue, playlist, String.valueOf(lotteryId), OrderBetStatus.WAIT);
        if (ordercount <= 0) {
            return;
        }
        //  List<OrderRecord> orderRecords = orderWriteService.selectOrdersPage(lotteryId, issue, OrderStatus.NORMAL, i);
        //   Map<Integer, OrderRecord> orderMap = new HashMap<>();
        // ??????????????????id??????
        //    List<Integer> orderIds = new ArrayList<>();
        //    this.updateOrder(number, orderRecords, orderIds, orderMap);
        // ??????????????????
        double divisor = betCommonService.getDivisor(lotteryId);
        // ????????????????????????????????????

        List<OrderBetRecord> orderBetRecords = orderWriteService.selectOrderBetList(issue, String.valueOf(lotteryId), playlist, OrderBetStatus.WAIT, "OnePlayMany");
        if (!jiesuanOrNot) {
            if (orderBetRecords.size() >= 2) {
                orderBetRecords = orderBetRecords.subList(0, 2);
            }
        }

        OrderBetRecord order = new OrderBetRecord();
        int whileMax = ordercount / Constants.CLEARNUM + 1;   //??????????????????
        int whileNumber = 0;
        while (!CollectionUtils.isEmpty(orderBetRecords)) {
            whileNumber++;
            if (whileNumber > whileMax) {
                break;
            }
            try {
                //  Map<Integer, OrderRecord> orderMap=orderWriteService.getOrderMap(orderBetRecords);
                String betNumber = orderBetRecords.get(0).getBetNumber();
                // ????????????...???????????????playID....
                if (this.generationLHCPlayId(PLAY_ID_TM_TMA_LM, lotteryId).equals(playId) && betNumber.indexOf("????????????") < 0
                        || this.generationLHCPlayId(PLAY_ID_TM_TMA_LM, lotteryId).equals(playId) && betNumber.indexOf("????????????") > 0
                        || this.generationLHCPlayId(PLAY_ID_ZM_ZMA, lotteryId).equals(playId) && betNumber.indexOf("??????") < 0) {
                    return;
                }
                // ??????????????????????????????????????????
                if (betNumber.indexOf("??????") < 0
                        && this.generationLHCPlayId(PLAY_ID_TM_TMA_LM, lotteryId).equals(playId)) {
                    // ???????????????A????????????,????????????,????????????49?????????
                    if ("49".equals(number.split(",")[6])) {
                        orderBetRecords = noWinOrLose(orderBetRecords);
                    }
                }

                // ??????1-6(???1-6?????????)???????????????
                if (number.indexOf("49") >= 0) {
                    String[] sgArr = number.split(",");
                    if ("49".equals(sgArr[0]) && (playId.equals(this.generationLHCPlayId(PLAY_IDS_ZM, lotteryId)) || playId.equals(this.generationLHCPlayId(PLAY_ID_ZT_ONE, lotteryId)))) {
                        List<OrderBetRecord> heList = new ArrayList<>();
                        List<OrderBetRecord> noheList = new ArrayList<>();
                        for (OrderBetRecord orderBetRecord : orderBetRecords) {
                            if (orderBetRecord.getBetNumber().contains("?????????") || playId.equals(this.generationLHCPlayId(PLAY_ID_ZT_ONE, lotteryId))) {
                                heList.add(orderBetRecord);
                            } else {
                                noheList.add(orderBetRecord);
                            }
                        }
                        noheList.addAll(noWinOrLose(heList));
                        orderBetRecords = noheList;
                    } else if ("49".equals(sgArr[1]) && (playId.equals(this.generationLHCPlayId(PLAY_IDS_ZM, lotteryId)) || playId.equals(this.generationLHCPlayId(PLAY_ID_ZT_TWO, lotteryId)))) {
                        List<OrderBetRecord> heList = new ArrayList<>();
                        List<OrderBetRecord> noheList = new ArrayList<>();
                        for (OrderBetRecord orderBetRecord : orderBetRecords) {
                            if (orderBetRecord.getBetNumber().contains("?????????") || playId.equals(this.generationLHCPlayId(PLAY_ID_ZT_TWO, lotteryId))) {
                                heList.add(orderBetRecord);
                            } else {
                                noheList.add(orderBetRecord);
                            }
                        }
                        noheList.addAll(noWinOrLose(heList));
                        orderBetRecords = noheList;
                    } else if ("49".equals(sgArr[2]) && (playId.equals(this.generationLHCPlayId(PLAY_IDS_ZM, lotteryId)) || playId.equals(this.generationLHCPlayId(PLAY_ID_ZT_THREE, lotteryId)))) {
                        List<OrderBetRecord> heList = new ArrayList<>();
                        List<OrderBetRecord> noheList = new ArrayList<>();
                        for (OrderBetRecord orderBetRecord : orderBetRecords) {
                            if (orderBetRecord.getBetNumber().contains("?????????") || playId.equals(this.generationLHCPlayId(PLAY_ID_ZT_THREE, lotteryId))) {
                                heList.add(orderBetRecord);
                            } else {
                                noheList.add(orderBetRecord);
                            }
                        }
                        noheList.addAll(noWinOrLose(heList));
                        orderBetRecords = noheList;
                    } else if ("49".equals(sgArr[3]) && (playId.equals(this.generationLHCPlayId(PLAY_IDS_ZM, lotteryId)) || playId.equals(this.generationLHCPlayId(PLAY_ID_ZT_FOUR, lotteryId)))) {
                        List<OrderBetRecord> heList = new ArrayList<>();
                        List<OrderBetRecord> noheList = new ArrayList<>();
                        for (OrderBetRecord orderBetRecord : orderBetRecords) {
                            if (orderBetRecord.getBetNumber().contains("?????????") || playId.equals(this.generationLHCPlayId(PLAY_ID_ZT_FOUR, lotteryId))) {
                                heList.add(orderBetRecord);
                            } else {
                                noheList.add(orderBetRecord);
                            }
                        }
                        noheList.addAll(noWinOrLose(heList));
                        orderBetRecords = noheList;
                    } else if ("49".equals(sgArr[4]) && (playId.equals(this.generationLHCPlayId(PLAY_IDS_ZM, lotteryId)) || playId.equals(this.generationLHCPlayId(PLAY_ID_ZT_FIVE, lotteryId)))) {
                        List<OrderBetRecord> heList = new ArrayList<>();
                        List<OrderBetRecord> noheList = new ArrayList<>();
                        for (OrderBetRecord orderBetRecord : orderBetRecords) {
                            if (orderBetRecord.getBetNumber().contains("?????????") || playId.equals(this.generationLHCPlayId(PLAY_ID_ZT_FIVE, lotteryId))) {
                                heList.add(orderBetRecord);
                            } else {
                                noheList.add(orderBetRecord);
                            }
                        }
                        noheList.addAll(noWinOrLose(heList));
                        orderBetRecords = noheList;
                    } else if ("49".equals(sgArr[5]) && (playId.equals(this.generationLHCPlayId(PLAY_IDS_ZM, lotteryId)) || playId.equals(this.generationLHCPlayId(PLAY_ID_ZT_SIX, lotteryId)))) {
                        List<OrderBetRecord> heList = new ArrayList<>();
                        List<OrderBetRecord> noheList = new ArrayList<>();
                        for (OrderBetRecord orderBetRecord : orderBetRecords) {
                            if (orderBetRecord.getBetNumber().contains("?????????") || playId.equals(this.generationLHCPlayId(PLAY_ID_ZT_SIX, lotteryId))) {
                                heList.add(orderBetRecord);
                            } else {
                                noheList.add(orderBetRecord);
                            }
                        }
                        noheList.addAll(noWinOrLose(heList));
                        orderBetRecords = noheList;
                    }
                }

                // ????????????id
                if (orderBetRecords.size() == 0) {
                    return;
                }
                Integer settingId = orderBetRecords.get(0).getSettingId();

                // ????????????????????????
                Map<String, LotteryPlayOdds> oddsMap = lotteryPlayOddsService.selectPlayOddsBySettingId(settingId);

                for (OrderBetRecord orderBet : orderBetRecords) {
                    order = orderBet;
                    BigDecimal winAmount = new BigDecimal(0);
                    orderBet.setWinCount("0");
                    // ??????????????????,??????????????????
                    String winNum = LhcUtils.isWinByOnePlayManyOdds(orderBet.getBetNumber(), number, orderBet.getPlayId(), date, lotteryId);
                    if (StringUtils.isNotBlank(winNum)) {
                        String[] winStrArr = winNum.split(",");
                        int wincount = 0;
                        for (String winStr : winStrArr) {
                            boolean boHe = false;
                            if (winStr.contains("??????")) {
                                boHe = true;
                                winStr = winStr.replace("??????", "");
                            }
                            // ??????????????????
                            LotteryPlayOdds odds = oddsMap.get(winStr);
                            // ???????????????/????????????
                            String winCount = odds.getWinCount();
                            String totalCount = odds.getTotalCount();
                            // ????????????
                            double odd = Double.parseDouble(totalCount) * 1.0 / Double.parseDouble(winCount) * divisor;
                            //?????????
                            if (boHe) {
                                winAmount = winAmount.add(orderBet.getBetAmount().divide(BigDecimal.valueOf(orderBet.getBetCount())));
                            } else {
                                winAmount = winAmount.add(orderBet.getBetAmount().divide(BigDecimal.valueOf(orderBet.getBetCount())).multiply(BigDecimal.valueOf(odd)));
                                wincount = wincount + 1;
                            }
                        }
                        orderBet.setWinCount(String.valueOf(wincount));
                    }
                    // ??????????????????,?????????????????????????????????
                    try {
                        logger.info("clearingOnePlayManyOdds ??????????????????{};{};{};{};{};{};{}", orderBet.getOrderSn(), orderBet.getBetNumber(), number, orderBet.getPlayId(), lotteryId, winNum, winAmount);
                        if (jiesuanOrNot) {
                            betCommonService.winOrLose(orderBet, winAmount, orderBet.getUserId(), orderBet.getOrderSn());
                        } else {
                            //??????????????????
                            String tbStatus = "";
                            if (winAmount.compareTo(orderBet.getBetAmount()) == 0) {
                                tbStatus = OrderBetStatus.HE;
                            } else if (winAmount.compareTo(BigDecimal.ZERO) > 0) {
                                tbStatus = OrderBetStatus.WIN;
                            } else {
                                tbStatus = OrderBetStatus.NO_WIN;
                            }
                            String lotteryName = "";
                            String lotteryPlayName = "";
                            LotteryPlay lotteryPlay = lotteryPlayWriteService.selectPlayById(orderBet.getPlayId());
                            Lottery lottery = lotteryWriteService.selectLotteryById(orderBet.getLotteryId());
                            if (lotteryPlay != null) {
                                lotteryName = lotteryPlay.getName();
                            }
                            if (lottery != null) {
                                lotteryPlayName = lottery.getName();
                            }
                            String message = orderBet.getIssue() + ";" + orderBet.getOrderSn() + ";" + lotteryName + ";" + lotteryPlayName + ";" + orderBet.getBetNumber()
                                    + ";" + orderBet.getBetAmount() + ";" + winAmount + ";" + tbStatus;

                            redisTemplate.opsForHash().put("JIESUANORDER", issue + orderBet.getPlayId() + orderBet.getId(), message);
                            redisTemplate.expire("JIESUANORDER", 6, TimeUnit.HOURS);
                        }
                    } catch (TransactionSystemException e1) {
                        logger.error("?????????????????? ???????????? ????????????:{},{}", orderBet.getOrderSn(), e1);
                        for (int i = 0; i < 20; i++) {
                            try {
                                if (jiesuanOrNot) {
                                    betCommonService.winOrLose(orderBet, winAmount, orderBet.getUserId(), orderBet.getOrderSn());
                                }
                            } catch (TransactionSystemException e2) {
                                logger.error("??????????????????:{},???????????? ????????????:{},{}", i, orderBet.getOrderSn(), e2);
                                Thread.sleep(100);
                                continue;
                            }
                            break;
                        }
                    }
                }

                if (!jiesuanOrNot) {
                    break;
                }
                orderBetRecords = orderWriteService.selectOrderBetList(issue, String.valueOf(lotteryId), playlist, OrderBetStatus.WAIT, "OnePlayMany");
            } catch (Exception e) {
                logger.error("?????????????????????lotteryId:{},issue:{},betNum:{},{}", order.getLotteryId(), issue, order.getBetNumber(), e);
                break;
            }

        }
    }

    @Async
    public void updateOrder(String number, List<OrderRecord> orderRecords, List<Integer> orderIds, Map<Integer, OrderRecord> orderMap) {
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
            OrderRecord orderupdate = new OrderRecord();
            orderupdate.setOpenNumber(number);
            orderupdate.setId(order.getId());
            orderRecordMapper.updateByPrimaryKeySelective(orderupdate);
        }
    }

    /**
     * ???????????????49?????????????????????
     *
     * @param orderMap
     * @param orderBetRecords
     */
    private List<OrderBetRecord> noWinOrLose(Map<Integer, OrderRecord> orderMap, List<OrderBetRecord> orderBetRecords) {
        List<OrderBetRecord> newOrderBetRecords = new ArrayList<>();
        for (OrderBetRecord orderBet : orderBetRecords) {
            String[] betNumbetArr = orderBet.getBetNumber().split("@");
            if (MAYBE_HE.contains(betNumbetArr[0])) {
                BigDecimal winAmount = getTradeOffAmount(orderBet.getBetAmount());
                // ??????????????????
                orderBet.setWinAmount(winAmount);
                // ????????????
                orderBet.setTbStatus(OrderBetStatus.HE);
                orderBet.setWinCount("0");
                // ??????????????????
                orderBetRecordMapper.updateByPrimaryKeySelective(orderBet);
                // ????????????????????????
                OrderRecord orderRecord = orderMap.get(orderBet.getOrderId());
                betCommonService.updateMemberBalance(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
            } else {
                newOrderBetRecords.add(orderBet);
            }
        }
        return newOrderBetRecords;
    }

    /**
     * ???????????????49?????????????????????   ??????????????????????????????
     *
     * @param orderBetRecords
     */
    private List<OrderBetRecord> noWinOrLose(List<OrderBetRecord> orderBetRecords) {
        List<OrderBetRecord> newOrderBetRecords = new ArrayList<>();
        for (OrderBetRecord orderBet : orderBetRecords) {
            String[] betNumbetArr = orderBet.getBetNumber().split("@");
            if (MAYBE_HE.contains(betNumbetArr[1])) {
                BigDecimal winAmount = getTradeOffAmount(orderBet.getBetAmount());
                // ??????????????????
                orderBet.setWinAmount(winAmount);
                // ????????????
                orderBet.setTbStatus(OrderBetStatus.HE);
                orderBet.setWinCount("0");
                // ??????????????????
                orderBetRecordMapper.updateByPrimaryKeySelective(orderBet);
                // ????????????????????????

                betCommonService.updateMemberBalance(orderBet, winAmount, orderBet.getUserId(), orderBet.getOrderSn());
            } else {
                newOrderBetRecords.add(orderBet);
            }
        }
        return newOrderBetRecords;
    }

    /**
     * ???????????????????????????(????????????,?????????????????????)
     * (????????????:?????????,????????????,???????????????,???????????????)
     *
     * @param betNumber ????????????
     * @param sgNumber  ????????????
     * @param playId    ??????id
     * @param dateStr   ???????????? yyyy-MM-dd
     * @return
     */
    private List<Integer> isWinByOnePlayTwoOdds(String betNumber, String sgNumber, Integer playId, String dateStr, Integer lotteryId) {
        if (this.generationLHCPlayIdList(PLAY_IDS_LX_WIN, lotteryId).contains(playId)) {
            // ???????????????
            return isWinByLxWin(betNumber, sgNumber, playId, dateStr, lotteryId);
        } else if (this.generationLHCPlayIdList(PLAY_IDS_LW_NO_WIN, lotteryId).contains(playId)) {
            // ??????????????????
            return isWinByLwNoWin(betNumber, sgNumber, playId, lotteryId);
        } else if (this.generationLHCPlayIdList(PLAY_IDS_LM_EZ, lotteryId).contains(playId)) {
            // ???????????????,???????????????
            return isWinLianMaEz(betNumber, sgNumber, playId, lotteryId);
        }
        return null;
    }

    /**
     * ???????????????????????????(????????????,??????????????????)
     * (????????????: ??????(?????????,?????????,??????,??????,????????????), ?????????, 1-6??????, ??????)
     *
     * @param betNumber ????????????
     * @param sgNumber  ????????????
     * @param playId    ??????id
     * @param dateStr   ???????????? yyyy-MM-dd
     * @return
     */
    private int isWinByOnePlayOneOdds(String betNumber, String sgNumber, Integer playId, String dateStr, Integer lotteryId) {
        if (this.generationLHCPlayIdList(PLAY_IDS_LX_NO_WIN, lotteryId).contains(playId)) {
            // ??????????????????
            return isWinByLxNoWin(betNumber, sgNumber, playId, dateStr, lotteryId);
        } else if (this.generationLHCPlayIdList(PLAY_IDS_LW_WIN, lotteryId).contains(playId)) {
            // ???????????????
            return isWinByLwWin(betNumber, sgNumber, playId, lotteryId);
        } else if (this.generationLHCPlayId(PLAY_ID_ONE_SIX_LH, lotteryId).equals(playId)) {
            // 1-6????????????
            return isWinByOneSixLh(betNumber, sgNumber);
        } else if (this.generationLHCPlayId(PLAY_ID_WUXING, lotteryId).equals(playId)) {
            // ????????????
            return isWinByWx(betNumber, sgNumber, dateStr);
        } else if (this.generationLHCPlayId(PLAY_ID_LX_LXLZ, lotteryId).equals(playId) ||
                this.generationLHCPlayId(PLAY_ID_LX_LXLBZ, lotteryId).equals(playId)) {
            // ???????????? ?????? ???????????????
            return isWinByLiuXiao(betNumber, sgNumber, playId, dateStr, lotteryId);
        } else if (this.generationLHCPlayIdList(PLAY_IDS_NO_OPEN, lotteryId).contains(playId)) {
            // ??????,????????????,?????????...
            return isWinByNoOpen(betNumber, sgNumber, playId, lotteryId);
        } else if (this.generationLHCPlayIdList(PLAY_IDS_LM_QZ, lotteryId).contains(playId)) {
            // ???????????????,?????????,??????
            return isWinLianMaQz(betNumber, sgNumber, playId, lotteryId);
        } else if (this.generationLHCPlayIdList(PLAY_IDS_ZT_OTS, lotteryId).contains(playId)) {
            // ?????????(1-6)???
            return isWinZhengTeOneToSix(betNumber, sgNumber, playId, lotteryId);
        } else if (this.generationLHCPlayId(PLAY_ID_TM_TMA, lotteryId).equals(playId)) {
            // ????????????A
            return isWinZhengTeOneToSix(betNumber, sgNumber, playId, lotteryId);
        } else if (this.generationLHCPlayId(PLAY_ID_ZM_ZMA, lotteryId).equals(playId)) {
            // ????????????A
            return isWinByNum(betNumber, sgNumber);
        }
        return 0;
    }

    /**
     * ???????????????????????????,????????????????????????,???????????????null(????????????,????????????)
     * (????????????:??????,??????,??????,??????)
     *
     * @param betNumber ????????????
     * @param sgNumber  ????????????
     * @param playId    ??????id
     * @param dateStr   ???????????? yyyy-MM-dd
     * @return
     */
    private String isWinByOnePlayManyOdds(String betNumber, String sgNumber, Integer playId, String dateStr, Integer lotteryId) {
        String[] betNumArr = betNumber.split("@")[1].split(",");
        String[] sgArr = sgNumber.split(",");
        StringBuffer winStr = new StringBuffer();
        if (this.generationLHCPlayId(PLAY_ID_WS_QW, lotteryId).equals(playId)) {
            // ????????????
            List<Integer> sgList = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                sgList.add(Integer.valueOf(sgArr[i]) % 10);
            }
            for (String betNum : betNumArr) {
                if (sgList.contains(Integer.parseInt(betNum.replace("???", "")))) {
                    winStr.append(betNum).append(",");
                }
            }
        } else if (this.generationLHCPlayId(PLAY_ID_WS_TW, lotteryId).equals(playId)) {
            // ????????????
            int tw = Integer.valueOf(sgArr[6]) % 10;
            for (String betNum : betNumArr) {
                if (tw == Integer.parseInt(betNum.replace("???", ""))) {
                    winStr.append(betNum).append(",");
                }
            }
        } else if (this.generationLHCPlayId(PLAY_ID_PT_PT, lotteryId).equals(playId)) {
            logger.info("???????????????");
            // ??????
            List<String> sgList = LhcUtils.getNumberShengXiao(sgNumber, dateStr);
            String sgs = "";
            for (String sg : sgList) {
                sgs = sgs + sg;
            }
            for (String betNum : betNumArr) {
                if (sgs.contains(betNum)) {
                    winStr.append(betNum).append(",");
                }
            }

            StringBuffer winStr1 = new StringBuffer();
            for (String betNum : betNumArr) {
                if (sgList.contains(betNum)) {
                    winStr1.append(betNum).append(",");
                }
            }
            if (!winStr.toString().equals(winStr1.toString())) {
                logger.info("????????????????????????{}???{}", winStr, winStr1);
            }

            String nums = "";
            for (String num : betNumArr) {
                nums = nums + num;
            }
            try {
                String sgsUtf8 = new String(sgs.getBytes("UTF-8"));
                String sgsUnicode = new String(sgsUtf8.getBytes(), "UTF-8");
                String sgsGbk = new String(sgsUnicode.getBytes("GBK"));
                String numsUtf8 = new String(nums.getBytes("UTF-8"));
                String numsUnicode = new String(numsUtf8.getBytes(), "UTF-8");
                String numsGbk = new String(numsUnicode.getBytes("GBK"));
                logger.info("????????????:{},{},{},{},{},{}", sgsUtf8, sgsUnicode, sgsGbk, numsUtf8, numsUnicode, numsGbk);
            } catch (Exception e) {
                logger.error("?????????????????????{}", e);
            }
            logger.info("???????????????{},{},{},{},{},{},{},{},{}", sgNumber, sgs, nums, winStr, betNumber, sgNumber, playId, dateStr, lotteryId);
        } else if (this.generationLHCPlayId(PLAY_ID_TX_TX, lotteryId).equals(playId)) {
            // ??????
            String tx = LhcUtils.getShengXiao(Integer.valueOf(sgArr[6]), dateStr);
            for (String betNum : betNumArr) {
                if (tx.equals(betNum)) {
                    winStr.append(betNum).append(",");
                }
            }
        } else if (this.generationLHCPlayId(PLAY_ID_BB_RED, lotteryId).equals(playId) ||
                this.generationLHCPlayId(PLAY_ID_BB_BLUE, lotteryId).equals(playId) ||
                this.generationLHCPlayId(PLAY_ID_BB_GREEN, lotteryId).equals(playId)) {
            // ????????????,??????,??????
            List<String> numBanboList = LhcPlayRule.getNumBanboList(Integer.valueOf(sgArr[6]));
            for (String betNum : betNumArr) {
                if (numBanboList.contains(betNum)) {
                    winStr.append(betNum).append(",");
                }
            }
        } else if (this.generationLHCPlayIdList(PLAY_IDS_ZT_OTS, lotteryId).contains(playId) || this.generationLHCPlayId(PLAY_IDS_ZM, lotteryId).equals(playId)) {
            // ?????????(1-6)??? ?????? ?????? ??????1-6
            int index = 6;
            /**if (PLAY_ID_ZT_ONE_LM.equals(playId) || PLAY_ID_ZM_ONE.equals(playId)) {
             index = 0;
             } else if (PLAY_ID_ZT_TWO_LM.equals(playId) || PLAY_ID_ZM_TWO.equals(playId)) {
             index = 1;
             } else if (PLAY_ID_ZT_THREE_LM.equals(playId) || PLAY_ID_ZM_THREE.equals(playId)) {
             index = 2;
             } else if (PLAY_ID_ZT_FOUR_LM.equals(playId) || PLAY_ID_ZM_FOUR.equals(playId)) {
             index = 3;
             } else if (PLAY_ID_ZT_FIVE_LM.equals(playId) || PLAY_ID_ZM_FIVE.equals(playId)) {
             index = 4;
             } else if (PLAY_ID_ZT_SIX_LM.equals(playId) || PLAY_ID_ZM_SIX.equals(playId)) {
             index = 5;
             }**/

            if (this.generationLHCPlayId(PLAY_ID_ZT_ONE, lotteryId).equals(playId) || betNumber.indexOf("?????????") >= 0) {
                index = 0;
            } else if (this.generationLHCPlayId(PLAY_ID_ZT_TWO, lotteryId).equals(playId) || betNumber.indexOf("?????????") >= 0) {
                index = 1;
            } else if (this.generationLHCPlayId(PLAY_ID_ZT_THREE, lotteryId).equals(playId) || betNumber.indexOf("?????????") >= 0) {
                index = 2;
            } else if (this.generationLHCPlayId(PLAY_ID_ZT_FOUR, lotteryId).equals(playId) || betNumber.indexOf("?????????") >= 0) {
                index = 3;
            } else if (this.generationLHCPlayId(PLAY_ID_ZT_FIVE, lotteryId).equals(playId) || betNumber.indexOf("?????????") >= 0) {
                index = 4;
            } else if (this.generationLHCPlayId(PLAY_ID_ZT_SIX, lotteryId).equals(playId) || betNumber.indexOf("?????????") >= 0) {
                index = 5;
            }
            List<String> numLiangMianList = LhcPlayRule.getNumLiangMianList(Integer.valueOf(sgArr[index]));
            numLiangMianList.add(sgArr[index]);
            for (String betNum : betNumArr) {
                if (numLiangMianList.contains(betNum)) {
                    winStr.append(betNum).append(",");
                }
                if ("49".equals(sgArr[index])) {
                    if ("???".equals(betNum) || "???".equals(betNum) || "???".equals(betNum) || "???".equals(betNum) || "??????".equals(betNum) || "??????".equals(betNum)
                            || "??????".equals(betNum) || "??????".equals(betNum)) {
                        winStr.append(betNum + "??????").append(",");
                    }
                }
            }
        } else if (this.generationLHCPlayId(PLAY_ID_ZM_ZMA, lotteryId).equals(playId)) {
            // ????????????A??????
            List<String> totalLiangMian = LhcUtils.getTotalLiangMian(sgNumber);
            for (String betNum : betNumArr) {
                if (totalLiangMian.contains(betNum)) {
                    winStr.append(betNum).append(",");
                }
            }
        } else if (this.generationLHCPlayId(PLAY_ID_TM_TMA_LM, lotteryId).equals(playId)) {
            // ????????????A??????
            List<String> temaLiangMian = LhcUtils.getTemaLiangMian(Integer.valueOf(sgArr[6]), dateStr);
            for (String betNum : betNumArr) {
                if (temaLiangMian.contains(betNum)) {
                    winStr.append(betNum).append(",");
                }
            }
        }
        if (winStr.length() > 0) {
            return winStr.substring(0, winStr.length() - 1);
        }
        return null;
    }

    /**
     * ???????????????????????????
     * (????????????:???????????????,???????????????)
     *
     * @param betNumber ????????????
     * @param sgNumber  ????????????
     * @param playId    ??????id
     * @return
     */
    private List<Integer> isWinLianMaEz(String betNumber, String sgNumber, Integer playId, Integer lotteryId) {
        String[] betNumArr = betNumber.split("@")[1].split(",");
        String[] sgArr = sgNumber.split(",");
        List<Integer> sgList = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            sgList.add(Integer.valueOf(sgArr[i]));
        }
        int tema = Integer.valueOf(sgArr[6]);
        boolean tag = false;
        List<Integer> openNum = new ArrayList<>();
        for (String betNum : betNumArr) {
            if (sgList.contains(Integer.valueOf(betNum))) {
                openNum.add(Integer.valueOf(betNum));
            }
            if (tema == Integer.valueOf(betNum)) {
                tag = true;
            }
        }

        List<Integer> wins = new ArrayList<>(2); //?????????????????????????????????????????????
        int size = openNum.size();
        int win1 = 0; // ????????????????????????
        int win2 = 0; // ????????????????????????
        if (this.generationLHCPlayId(PLAY_ID_LM_EZT, lotteryId).equals(playId)) {
            // ???????????????
            if (tag) {
                win1 = size;
            }
            win2 = MathUtil.countCnm(size, 2);
        } else if (this.generationLHCPlayId(PLAY_ID_LM_SZE, lotteryId).equals(playId)) {
            // ???????????????
            win1 = MathUtil.countCnm(size, 3);
            win2 = MathUtil.countCnm(size, 2) * (betNumArr.length - size);
        }
        wins.add(win1);
        wins.add(win2);
        return wins;
    }


    /**
     * ???????????????????????????
     * (????????????:????????????A)
     *
     * @param betNumber ????????????
     * @param sgNumber  ????????????
     * @return
     */
    private int isWinByNum(String betNumber, String sgNumber) {
        String[] betNumArr = betNumber.split("@")[1].split(",");
        String[] sgArr = sgNumber.split(",");
        int winCount = 0;

        // ????????????A
        List<Integer> sgList = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            sgList.add(Integer.valueOf(sgArr[i]));
        }
        for (String betNum : betNumArr) {
            if (sgList.contains(Integer.valueOf(betNum))) {
                ++winCount;
            }
        }

        return winCount;
    }


    /**
     * ???????????????????????????
     * (????????????:?????????(1-6)???, ????????????A)
     *
     * @param betNumber ????????????
     * @param sgNumber  ????????????
     * @param playId    ??????id
     * @return
     */
    private int isWinZhengTeOneToSix(String betNumber, String sgNumber, Integer playId, Integer lotteryId) {
        String[] sgArr = sgNumber.split(",");
        int index = 6;
        if (this.generationLHCPlayId(PLAY_ID_ZT_ONE, lotteryId).equals(playId)) {
            index = 0;
        } else if (this.generationLHCPlayId(PLAY_ID_ZT_TWO, lotteryId).equals(playId)) {
            index = 1;
        } else if (this.generationLHCPlayId(PLAY_ID_ZT_THREE, lotteryId).equals(playId)) {
            index = 2;
        } else if (this.generationLHCPlayId(PLAY_ID_ZT_FOUR, lotteryId).equals(playId)) {
            index = 3;
        } else if (this.generationLHCPlayId(PLAY_ID_ZT_FIVE, lotteryId).equals(playId)) {
            index = 4;
        } else if (this.generationLHCPlayId(PLAY_ID_ZT_SIX, lotteryId).equals(playId)) {
            index = 5;
        } else if (this.generationLHCPlayId(PLAY_ID_TM_TMA, lotteryId).equals(playId)) {
            index = 6;
        }

        int winCount = 0;
        if (betNumber.contains("??????")) {
            int number = Integer.valueOf(sgArr[index]);
            String open = LhcUtils.numDetalis(number + "");
            String betNum = String.valueOf(betNumber.split("@")[1]);

            if (open.contains(betNum)) {
                winCount = 1;
            }
        } else {
            String number = sgArr[index];
            List<String> betList = Arrays.asList(betNumber.split("@")[1].split(","));

            if (betList.contains(number)) {
                winCount = 1;
            }
        }

        return winCount;
    }

    /**
     * ???????????????????????????
     * (????????????:???????????????,?????????,??????)
     *
     * @param betNumber ????????????
     * @param sgNumber  ????????????
     * @param playId    ??????id
     * @return
     */
    private int isWinLianMaQz(String betNumber, String sgNumber, Integer playId, Integer lotteryId) {
        String[] betNumArr = betNumber.split("@")[1].split(",");
        String[] sgArr = sgNumber.split(",");
        List<Integer> sgList = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            sgList.add(Integer.valueOf(sgArr[i]));
        }
        int tema = Integer.valueOf(sgArr[6]);
        boolean tag = false;
        List<Integer> openNum = new ArrayList<>();
        for (String betNum : betNumArr) {
            if (sgList.contains(Integer.valueOf(betNum))) {
                openNum.add(Integer.valueOf(betNum));
            }
            if (tema == Integer.valueOf(betNum)) {
                tag = true;
            }
        }
        if (this.generationLHCPlayId(PLAY_ID_LM_TC, lotteryId).equals(playId) && tag) {
            return openNum.size();
        } else if (this.generationLHCPlayId(PLAY_ID_LM_EQZ, lotteryId).equals(playId)) {
            return MathUtil.countCnm(openNum.size(), 2);
        } else if (this.generationLHCPlayId(PLAY_ID_LM_SQZ, lotteryId).equals(playId)) {
            return MathUtil.countCnm(openNum.size(), 3);
        }
        return 0;
    }

    /**
     * ???????????????????????????
     * (????????????:??????)
     *
     * @param betNumber ????????????
     * @param sgNumber  ????????????
     * @param playId    ??????id
     * @return
     */
    private int isWinByNoOpen(String betNumber, String sgNumber, Integer playId, Integer lotteryId) {
        String[] betNumArr = betNumber.split("@")[1].split(",");
        String[] sgArr = sgNumber.split(",");
        List<Integer> sgList = new ArrayList<>();
        for (String sg : sgArr) {
            sgList.add(Integer.valueOf(sg));
        }
        List<String> noOpenNum = new ArrayList<>();
        for (String betNum : betNumArr) {
            if (!sgList.contains(Integer.valueOf(betNum))) {
                noOpenNum.add(betNum);
            }
        }
        int m = 50; //?????????m
        if (this.generationLHCPlayId(PLAY_ID_NO_OPEN_FIVE, lotteryId).equals(playId)) {
            m = 5;
        } else if (this.generationLHCPlayId(PLAY_ID_NO_OPEN_SIX, lotteryId).equals(playId)) {
            m = 6;
        } else if (this.generationLHCPlayId(PLAY_ID_NO_OPEN_SEVEN, lotteryId).equals(playId)) {
            m = 7;
        } else if (this.generationLHCPlayId(PLAY_ID_NO_OPEN_EIGHT, lotteryId).equals(playId)) {
            m = 8;
        } else if (this.generationLHCPlayId(PLAY_ID_NO_OPEN_NINE, lotteryId).equals(playId)) {
            m = 9;
        } else if (this.generationLHCPlayId(PLAY_ID_NO_OPEN_TEN, lotteryId).equals(playId)) {
            m = 10;
        }
        return MathUtil.countCnmIsLong(noOpenNum.size(), m);
    }

    /**
     * ???????????????????????????
     * (????????????:??????)
     *
     * @param betNumber ????????????
     * @param sgNumber  ????????????
     * @param playId    ??????id
     * @param dateStr   ???????????? yyyy-MM-dd
     * @return
     */
    private int isWinByLiuXiao(String betNumber, String sgNumber, Integer playId, String dateStr, Integer lotteryId) {
        String teXiao = LhcUtils.getShengXiao(Integer.valueOf(sgNumber.split(",")[6]), dateStr);
        String[] betNumArr = betNumber.split("@")[1].split(",");
        List<String> noOpenSx = new ArrayList<>();
        for (String betNum : betNumArr) {
            if (!teXiao.equals(betNum)) {
                noOpenSx.add(betNum);
            }
        }
        if (betNumArr.length != noOpenSx.size() && this.generationLHCPlayId(PLAY_ID_LX_LXLZ, lotteryId).equals(playId)) {
            // return MathUtil.countCnm(betNumArr.length, noOpenSx.size() + 1);
            return MathUtil.countCnm(betNumArr.length, 6) - MathUtil.countCnm(betNumArr.length - 1, 6);
        } else if (this.generationLHCPlayId(PLAY_ID_LX_LXLBZ, lotteryId).equals(playId)) {
            return MathUtil.countCnm(noOpenSx.size(), 6);
        }
        return 0;
    }

    /**
     * ???????????????????????????
     * (????????????:?????????)
     *
     * @param betNumber ????????????
     * @param sgNumber  ????????????
     * @param playId    ??????id
     * @param dateStr   ???????????? yyyy-MM-dd
     * @return
     */
    private List<Integer> isWinByLxWin(String betNumber, String sgNumber, Integer playId, String dateStr, Integer lotteryId) {
        // ?????????????????????
        List<String> numberShengXiao = LhcUtils.getNumberShengXiao(sgNumber, dateStr);
        // ???????????????????????????
        String shengXiao = LhcUtils.getShengXiao(dateStr);
        String[] betNumArr = betNumber.split("@")[1].split(",");

        List<String> openSx = new ArrayList<>();
        for (String betNum : betNumArr) {
            if (numberShengXiao.contains(betNum)) {
                openSx.add(betNum);
            }
        }

        List<Integer> wins = new ArrayList<>(2); //?????????????????????????????????????????????
        int size = openSx.size();
        int win1 = 0; // ????????????????????????
        int win2 = 0; // ????????????????????????

        int playWins = 2;
        if (this.generationLHCPlayId(PLAY_ID_LX_TWO_WIN, lotteryId).equals(playId)) {
            playWins = 2;
        } else if (this.generationLHCPlayId(PLAY_ID_LX_THREE_WIN, lotteryId).equals(playId)) {
            playWins = 3;
        } else if (this.generationLHCPlayId(PLAY_ID_LX_FOUR_WIN, lotteryId).equals(playId)) {
            playWins = 4;
        }

        if (size >= playWins) {
            // ?????????
            if (openSx.contains(shengXiao)) {
                // ??????????????????
                win1 = MathUtil.countCnm(size - 1, playWins);
                win2 = MathUtil.countCnm(size - 1, playWins - 1);
            } else {
                win1 = MathUtil.countCnm(size, playWins);
            }
        }

        wins.add(win1);
        wins.add(win2);

        return wins;
    }

    /**
     * ???????????????????????????
     * (????????????:????????????)
     *
     * @param betNumber ????????????
     * @param sgNumber  ????????????
     * @param playId    ??????id
     * @param dateStr   ???????????? yyyy-MM-dd
     * @return
     */
    private int isWinByLxNoWin(String betNumber, String sgNumber, Integer playId, String dateStr, Integer lotteryId) {
        List<String> numberShengXiao = LhcUtils.getNumberShengXiao(sgNumber, dateStr);
        String[] betNumArr = betNumber.split("@")[1].split(",");
        List<String> noOpenSx = new ArrayList<>();
        for (String betNum : betNumArr) {
            if (!numberShengXiao.contains(betNum)) {
                noOpenSx.add(betNum);
            }
        }
        if (this.generationLHCPlayId(PLAY_ID_LX_TWO_NO_WIN, lotteryId).equals(playId)) {
            return MathUtil.countCnm(noOpenSx.size(), 2);
        } else if (this.generationLHCPlayId(PLAY_ID_LX_THREE_NO_WIN, lotteryId).equals(playId)) {
            return MathUtil.countCnm(noOpenSx.size(), 3);
        } else if (this.generationLHCPlayId(PLAY_ID_LX_FOUR_NO_WIN, lotteryId).equals(playId)) {
            return MathUtil.countCnm(noOpenSx.size(), 4);
        }
        return 0;
    }

    /**
     * ???????????????????????????
     * (????????????:?????????)
     *
     * @param betNumber ????????????
     * @param sgNumber  ????????????
     * @param playId    ??????id
     * @return
     */
    private int isWinByLwWin(String betNumber, String sgNumber, Integer playId, Integer lotteryId) {
        String[] betNumArr = betNumber.split("@")[1].split(",");
        String[] sgArr = sgNumber.split(",");
        List<Integer> sgList = new ArrayList<>();
        for (String sg : sgArr) {
            sgList.add(Integer.valueOf(sg) % 10);
        }
        List<String> openNum = new ArrayList<>();
        for (String betNum : betNumArr) {
            if (sgList.contains(Integer.valueOf(betNum.replace("???", "")))) {
                openNum.add(betNum);
            }
        }
        if (this.generationLHCPlayId(PLAY_ID_LW_TWO_WIN, lotteryId).equals(playId)) {
            return MathUtil.countCnm(openNum.size(), 2);
        } else if (this.generationLHCPlayId(PLAY_ID_LW_THREE_WIN, lotteryId).equals(playId)) {
            return MathUtil.countCnm(openNum.size(), 3);
        } else if (this.generationLHCPlayId(PLAY_ID_LW_FOUR_WIN, lotteryId).equals(playId)) {
            return MathUtil.countCnm(openNum.size(), 4);
        }
        return 0;
    }

    /**
     * ???????????????????????????
     * (????????????:????????????)
     *
     * @param betNumber ????????????
     * @param sgNumber  ????????????
     * @param playId    ??????id
     * @return
     */
    private List<Integer> isWinByLwNoWin(String betNumber, String sgNumber, Integer playId, Integer lotteryId) {
        String[] betNumArr = betNumber.split("@")[1].split(",");
        String[] sgArr = sgNumber.split(",");
        List<Integer> sgList = new ArrayList<>();
        for (String sg : sgArr) {
            sgList.add(Integer.valueOf(sg) % 10);
        }
        List<String> noOpenNum = new ArrayList<>();
        for (String betNum : betNumArr) {
            if (!sgList.contains(Integer.valueOf(betNum.replace("???", "")))) {
                noOpenNum.add(betNum);
            }
        }

        List<Integer> wins = new ArrayList<>(2); //?????????????????????????????????????????????
        int size = noOpenNum.size();
        int win1 = 0; // ????????????????????????
        int win2 = 0; // ????????????????????????

        int playWins = 2;
        if (this.generationLHCPlayId(PLAY_ID_LW_TWO_NO_WIN, lotteryId).equals(playId)) {
            playWins = 2;
        } else if (this.generationLHCPlayId(PLAY_ID_LW_THREE_NO_WIN, lotteryId).equals(playId)) {
            playWins = 3;
        } else if (this.generationLHCPlayId(PLAY_ID_LW_FOUR_NO_WIN, lotteryId).equals(playId)) {
            playWins = 4;
        }

        if (size >= playWins) {
            // ?????????
            if (noOpenNum.contains("0???")) {
                // ??????????????????
                win1 = MathUtil.countCnm(size - 1, playWins);
                // ??????????????????
                win2 = MathUtil.countCnm(size - 1, playWins - 1);
            } else {
                // ??????????????????
                win1 = MathUtil.countCnm(size, playWins);
            }
        }

        wins.add(win1);
        wins.add(win2);

        return wins;

    }

    /**
     * ???????????????????????????
     * (????????????:1-6??????)
     *
     * @param betNumber ????????????
     * @param sgNumber  ????????????
     * @return
     */
    private int isWinByOneSixLh(String betNumber, String sgNumber) {
        // ????????????
        if (StringUtils.isBlank(betNumber) || StringUtils.isBlank(sgNumber)) {
            return 0;
        }
        // ??????????????????
        String[] bet = betNumber.split("@");
        String numStr = bet[1].substring(1, 4);
        String betType = bet[1].substring(0, 1);
        String[] num = numStr.split("-");
        String[] sgNum = sgNumber.split(",");
        int one = Integer.valueOf(num[0]);
        int two = Integer.valueOf(num[1]);
        String str;
        if (sgNum[one - 1].compareTo(sgNum[two - 1]) > 0) {
            str = "???";
        } else {
            str = "???";
        }
        return str.equals(betType) ? 1 : 0;

//        String[] betNumArr = betNumber.split(";");
//        String longStr = betNumArr[0].length() > 2 ? betNumArr[0].substring(2) : "";
//        String huStr = betNumArr[1].length() > 2 ? betNumArr[1].substring(2) : "";
//        String[] sgArr = sgNumber.split(",");
//        List<Integer> sgList = new ArrayList<>();
//        for (String sg : sgArr) {
//            sgList.add(Integer.valueOf(sg));
//        }
//        int count = 0;
//        if (StringUtils.isNotBlank(longStr)) {
//            String[] longArr = longStr.split(",");
//            for (String betStr : longArr) {
//                int one = Integer.valueOf(betStr.charAt(0) + "");
//                int two = Integer.valueOf(betStr.charAt(2) + "");
//                if (sgList.get(one - 1) > sgList.get(two - 1)) {
//                    ++count;
//                }
//            }
//        }
//
//        if (StringUtils.isNotBlank(huStr)) {
//            String[] huArr = huStr.split(",");
//            for (String betStr : huArr) {
//                int one = Integer.valueOf(betStr.charAt(0) + "");
//                int two = Integer.valueOf(betStr.charAt(2) + "");
//                if (sgList.get(one - 1) < sgList.get(two - 1)) {
//                    ++count;
//                }
//            }
//        }
//
//        return count;
    }

    /**
     * ???????????????????????????
     * (????????????:??????)
     *
     * @param betNumber ????????????
     * @param sgNumber  ????????????
     * @param dateStr   ???????????? yyyy-MM-dd
     * @return
     */
    private int isWinByWx(String betNumber, String sgNumber, String dateStr) {
        String wuXing = LhcUtils.getNumWuXing(Integer.valueOf(sgNumber.split(",")[6]), dateStr);
        String[] betNumArr = betNumber.split("@")[1].split(",");
        for (String betNum : betNumArr) {
            if (wuXing.equals(betNum)) {
                return 1;
            }
        }
        return 0;
    }

    /**
     * 1??????5?????????????????????????????? ??????ID??????
     *
     * @param playNumber
     * @param
     * @return
     */
    private Integer generationLHCPlayId(String playNumber, Integer lotteryId) {
        return Integer.parseInt(lotteryId + playNumber);
    }

    /**
     * 1??????5?????????????????????????????? ??????ID??????
     *
     * @param
     * @param
     * @return
     */
    private List<Integer> generationLHCPlayIdList(List<String> playNumbers, Integer lotteryId) {
        List<Integer> replaceToNewPlayId = new ArrayList<Integer>();
        for (String number : playNumbers) {
            replaceToNewPlayId.add(Integer.parseInt(lotteryId + number));
        }
        return replaceToNewPlayId;
    }

    @Override
    public AmlhcLotterySg queryNextAmlhcSg() {
        AmlhcLotterySgExample nextExample = new AmlhcLotterySgExample();
        AmlhcLotterySgExample.Criteria nextTjsscCriteria = nextExample.createCriteria();
        nextTjsscCriteria.andIdealTimeGreaterThan(DateUtils.getFullStringZeroSecond(new Date()));
        nextTjsscCriteria.andOpenStatusEqualTo(LotteryResultStatus.WAIT);
        nextExample.setOrderByClause("ideal_time ASC");
        AmlhcLotterySg nextTjsscLotterySg = this.amlhcLotterySgMapper.selectOneByExample(nextExample);
        return nextTjsscLotterySg;
    }

    @Override
    public AmlhcLotterySg selectAmlhcByIssue(String issue) {
        AmlhcLotterySgExample example = new AmlhcLotterySgExample();
        AmlhcLotterySgExample.Criteria lhcCriteria = example.createCriteria();
        lhcCriteria.andIssueEqualTo(issue);
        AmlhcLotterySg lhcLotterySg = amlhcLotterySgMapper.selectOneByExample(example);
        return lhcLotterySg;
    }

    public Integer getSslhcOpenCountNum() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("openStatus", LotteryResultStatus.AUTO);
        map.put("paramTime", TimeHelper.date("yyyy-MM-dd") + "%");
        Integer openCount = amlhcLotterySgMapperExt.openCountByExample(map);
        return openCount;
    }

    public List<AmlhcLotterySg> getAmlhcAlgorithmData() {
        AmlhcLotterySgExample sslhcExample = new AmlhcLotterySgExample();
        AmlhcLotterySgExample.Criteria sslhcCriteria = sslhcExample.createCriteria();
        sslhcCriteria.andOpenStatusEqualTo(LotteryResultStatus.AUTO);
        sslhcExample.setOrderByClause("`issue` DESC");
        sslhcExample.setOffset(Constants.DEFAULT_INTEGER);
        sslhcExample.setLimit(Constants.DEFAULT_ALGORITHM_PAGESIZE);
        List<AmlhcLotterySg> lhcLotterySgList = amlhcLotterySgMapper.selectByExample(sslhcExample);
        return lhcLotterySgList;
    }

    @Override
    public void cacheIssueResultForSslhc(String issue, String number) {
        List<String> keys = new ArrayList<>();
        keys.add(RedisKeys.AMLHC_RESULT_VALUE);
        keys.add(RedisKeys.AMLHC_NEXT_VALUE);
        keys.add(RedisKeys.AMLHC_OPEN_VALUE);
        keys.add(RedisKeys.AMLHC_ALGORITHM_VALUE);
        redisTemplate.delete(keys);
//        SslhcLotterySg lhcLotterySg = this.selectSslhcByIssue(issue);
//        // ?????????????????????
//        String redisKey = RedisKeys.SSLHC_RESULT_VALUE;
//        Long redisTime = CaipiaoRedisTimeEnum.SSLHC.getRedisTime();
//        redisTemplate.opsForValue().set(redisKey, lhcLotterySg);
//        // ??????????????????
//        SslhcLotterySg nextTjsscLotterySg = this.queryNextSslhcSg();
//        // ?????????????????????
//        String nextRedisKey = RedisKeys.SSLHC_NEXT_VALUE;
//        redisTemplate.opsForValue().set(nextRedisKey, nextTjsscLotterySg, redisTime, TimeUnit.MINUTES);
//        // ????????????????????????
//        Integer openCount = this.getSslhcOpenCountNum();
//        String openRedisKey = RedisKeys.SSLHC_OPEN_VALUE;
//        redisTemplate.opsForValue().set(openRedisKey, openCount);
//        // ????????????????????????
//        List<SslhcLotterySg> lhcLotterySgList = this.getSslhcAlgorithmData();
//        String algorithm = RedisKeys.SSLHC_ALGORITHM_VALUE;
//        redisTemplate.opsForValue().set(algorithm, lhcLotterySgList, redisTime, TimeUnit.MINUTES);
    }

    @Override
    public FivelhcLotterySg queryNextFivelhcSg() {
        FivelhcLotterySgExample nextExample = new FivelhcLotterySgExample();
        FivelhcLotterySgExample.Criteria nextTFivelhcCriteria = nextExample.createCriteria();
        nextTFivelhcCriteria.andIdealTimeGreaterThan(DateUtils.getFullStringZeroSecond(new Date()));
        nextTFivelhcCriteria.andOpenStatusEqualTo(LotteryResultStatus.WAIT);
        nextExample.setOrderByClause("issue ASC");
        FivelhcLotterySg nextTjsscLotterySg = this.fivelhcLotterySgMapper.selectOneByExample(nextExample);
        return nextTjsscLotterySg;
    }

    @Override
    public FivelhcLotterySg selectFivelhcByIssue(String issue, String number) {
        FivelhcLotterySgExample fivelhcExample = new FivelhcLotterySgExample();
        FivelhcLotterySgExample.Criteria fivelhcCriteria = fivelhcExample.createCriteria();
        fivelhcCriteria.andIssueEqualTo(issue);
        FivelhcLotterySg fivelhcLotterySg = fivelhcLotterySgMapper.selectOneByExample(fivelhcExample);

        if (fivelhcLotterySg != null && StringUtils.isNotEmpty(number) && StringUtils.isEmpty(fivelhcLotterySg.getNumber())) {
            fivelhcLotterySg.setNumber(number);
        }
        return fivelhcLotterySg;
    }

    public Integer getFivelhcOpenCountNum() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("openStatus", LotteryResultStatus.AUTO);
        map.put("paramTime", TimeHelper.date("yyyy-MM-dd") + "%");
        Integer openCount = fivelhcLotterySgMapperExt.openCountByExample(map);
        return openCount;
    }

    /**
     * @Title: getFivelhcAlgorithmData
     * @Description: ??????5??????????????????????????????
     * @author HANS
     * @date 2019???5???21?????????4:04:44
     */
    public List<FivelhcLotterySg> getFivelhcAlgorithmData() {
        FivelhcLotterySgExample fivelhcExample = new FivelhcLotterySgExample();
        FivelhcLotterySgExample.Criteria fivelhcCriteria = fivelhcExample.createCriteria();
        fivelhcCriteria.andOpenStatusEqualTo(LotteryResultStatus.AUTO);
        fivelhcExample.setOrderByClause("`ideal_time` DESC");
        fivelhcExample.setOffset(Constants.DEFAULT_INTEGER);
        fivelhcExample.setLimit(Constants.DEFAULT_ALGORITHM_PAGESIZE);
        List<FivelhcLotterySg> fivelhcLotterySgList = fivelhcLotterySgMapper.selectByExample(fivelhcExample);
        return fivelhcLotterySgList;
    }

    @Override
    public void cacheIssueResultForFivelhc(String issue, String number) {
//        FivelhcLotterySg fivelhcLotterySg = this.selectFivelhcByIssue(issue, number);
//        // ?????????????????????
//        String redisKey = RedisKeys.FIVELHC_RESULT_VALUE;
//        Long redisTime = CaipiaoRedisTimeEnum.FIVELHC.getRedisTime();
//        redisTemplate.opsForValue().set(redisKey, fivelhcLotterySg);
//        // ??????????????????
//        FivelhcLotterySg nextTjsscLotterySg = this.queryNextFivelhcSg();
//        // ?????????????????????
//        String nextRedisKey = RedisKeys.FIVELHC_NEXT_VALUE;
//        redisTemplate.opsForValue().set(nextRedisKey, nextTjsscLotterySg, redisTime, TimeUnit.MINUTES);
//        // ????????????????????????
//        Integer openCount = this.getFivelhcOpenCountNum();
//        String openRedisKey = RedisKeys.FIVELHC_OPEN_VALUE;
//        redisTemplate.opsForValue().set(openRedisKey, openCount);
//        // ????????????????????????
//        List<FivelhcLotterySg> fivelhcLotterySgList = this.getFivelhcAlgorithmData();
//        String algorithm = RedisKeys.FIVELHC_ALGORITHM_VALUE;
//        redisTemplate.opsForValue().set(algorithm, fivelhcLotterySgList, redisTime, TimeUnit.MINUTES);

        List<String> keys = new ArrayList<>();
        keys.add(RedisKeys.FIVELHC_RESULT_VALUE);
        keys.add(RedisKeys.FIVELHC_NEXT_VALUE);
        keys.add(RedisKeys.FIVELHC_OPEN_VALUE);
        keys.add(RedisKeys.FIVELHC_ALGORITHM_VALUE);
        redisTemplate.delete(keys);
    }

    /**
     * ????????????????????????
     *
     * @param issue ??????
     * @return
     */
//    private LhcLotterySg getLotterySg(String issue) {
//        // ????????????????????????
//        LhcLotterySg sg = null;
//        if (redisTemplate.hasKey(LHC_LOTTERY_SG + issue)) {
//            sg = (LhcLotterySg) redisTemplate.opsForValue().get(LHC_LOTTERY_SG + issue);
//        }
//        if (sg == null) {
//            LhcLotterySgExample sgExample = new LhcLotterySgExample();
//            LhcLotterySgExample.Criteria sgCriteria = sgExample.createCriteria();
//            sgCriteria.andYearEqualTo(issue.substring(0, 4));
//            sgCriteria.andIssueEqualTo(issue.substring(4));
//            sg = lhcLotterySgMapper.selectOneByExample(sgExample);
//            redisTemplate.opsForValue().set(LHC_LOTTERY_SG + issue, sg, 5, TimeUnit.MINUTES);
//        }
//        return sg;
//    }

    /**
     * ??????LIST????????????
     */
    private <T> List<List<T>> averageAssign(List<T> source, int n) {
        List<List<T>> result = new ArrayList<List<T>>();
        int remaider = source.size() % n;  //(??????????????????)
        int number = source.size() / n;  //????????????
        int offset = 0;//?????????
        for (int i = 0; i < n; i++) {
            List<T> value = null;
            if (remaider > 0) {
                value = source.subList(i * number + offset, (i + 1) * number + offset + 1);
                remaider--;
                offset++;
            } else {
                value = source.subList(i * number + offset, (i + 1) * number + offset);
            }
            result.add(value);
        }
        return result;
    }
}
