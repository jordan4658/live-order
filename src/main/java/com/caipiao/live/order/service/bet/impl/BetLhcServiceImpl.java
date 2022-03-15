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
    // 彩种id：4 六合彩
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

    // 六合彩特码特码A玩法id
    private final String PLAY_ID_TM_TMA = "01";
    // 六合彩特码特码A两面玩法id
    private final String PLAY_ID_TM_TMA_LM = "02";
    // 六合彩正码正码A玩法id
    public final static String PLAY_ID_ZM_ZMA = "03";
    // 六合彩正码正码A两面玩法id
    //private String PLAY_ID_ZM_ZMA_LM = 244;
    // 六合彩正码正码1-6玩法id集合
    //private final List<Integer> PLAY_IDS_ZM_OTS = Lists.newArrayList(
    //	120405, 120406, 120407, 120408, 120409, 120410);
    // 六合彩正码正码1-6玩法id集合
    private final String PLAY_IDS_ZM = "04";
    // 六合彩正码正码1玩法id
    //private String PLAY_ID_ZM_ONE = 120405;
    // 六合彩正码正码2玩法id
    //private String PLAY_ID_ZM_TWO = 120406;
    // 六合彩正码正码3玩法id
    //private String PLAY_ID_ZM_THREE = 120407;
    // 六合彩正码正码4玩法id
    //private String PLAY_ID_ZM_FOUR = 120408;
    // 六合彩正码正码5玩法id
    //private String PLAY_ID_ZM_FIVE = 120409;
    // 六合彩正码正码6玩法id
    //private String PLAY_ID_ZM_SIX = 120410;
    // 六合彩正特正(1-6)特玩法id集合
    private final List<String> PLAY_IDS_ZT_OTS = Lists.newArrayList("05", "06", "07", "08", "09", "10");
    // 六合彩正特正一特玩法id
    private final String PLAY_ID_ZT_ONE = "05";
    // 六合彩正特正一特玩法id
    private final String PLAY_ID_ZT_TWO = "06";
    // 六合彩正特正一特玩法id
    private final String PLAY_ID_ZT_THREE = "07";
    // 六合彩正特正一特玩法id
    private final String PLAY_ID_ZT_FOUR = "08";
    // 六合彩正特正一特玩法id
    private final String PLAY_ID_ZT_FIVE = "09";
    // 六合彩正特正一特玩法id
    private final String PLAY_ID_ZT_SIX = "10";

    // 六合彩正特正(1-6)特两面玩法id集合
    private final List<Integer> PLAY_IDS_ZT_OTS_LM = Lists.newArrayList(338, 339, 331, 335, 336, 337);
    // 六合彩正特正一特两面玩法id
    // private String PLAY_ID_ZT_ONE_LM = 338;
    // 六合彩正特正一特两面玩法id
    //private String PLAY_ID_ZT_TWO_LM = 339;
    // 六合彩正特正一特两面玩法id
    //private String PLAY_ID_ZT_THREE_LM = 331;
    // 六合彩正特正一特两面玩法id
    //private String PLAY_ID_ZT_FOUR_LM = 335;
    // 六合彩正特正一特两面玩法id
    //private String PLAY_ID_ZT_FIVE_LM = 336;
    // 六合彩正特正一特两面玩法id
    //private String PLAY_ID_ZT_SIX_LM = 337;

    // 六合彩连码三全中,二全中,特串玩法id集合
    private final List<String> PLAY_IDS_LM_QZ = Lists.newArrayList("15", "14", "13");
    // 六合彩连码特串玩法id
    private final String PLAY_ID_LM_TC = "13";
    // 六合彩连码二全中玩法id
    private final String PLAY_ID_LM_EQZ = "14";
    // 六合彩连码三全中玩法id
    private final String PLAY_ID_LM_SQZ = "15";

    // 六合彩连码三中二,二中特玩法id集合
    private final List<String> PLAY_IDS_LM_EZ = Lists.newArrayList("11", "12");
    // 六合彩连码二中特玩法id
    private final String PLAY_ID_LM_EZT = "12";
    // 六合彩连码三中二玩法id
    private final String PLAY_ID_LM_SZE = "11";

    // 六合彩半波红波玩法id
    private final String PLAY_ID_BB_RED = "16";
    // 六合彩半波蓝波玩法id
    private final String PLAY_ID_BB_BLUE = "17";
    // 六合彩半波绿波玩法id
    private final String PLAY_ID_BB_GREEN = "18";

    // 六合彩不中玩法id集合
    private final List<String> PLAY_IDS_NO_OPEN = Lists.newArrayList("21", "22", "23", "24", "25", "26");
    // 六合彩五不中玩法id
    private final String PLAY_ID_NO_OPEN_FIVE = "21";
    // 六合彩六不中玩法id
    private final String PLAY_ID_NO_OPEN_SIX = "22";
    // 六合彩七不中玩法id
    private final String PLAY_ID_NO_OPEN_SEVEN = "23";
    // 六合彩八不中玩法id
    private final String PLAY_ID_NO_OPEN_EIGHT = "24";
    // 六合彩九不中玩法id
    private final String PLAY_ID_NO_OPEN_NINE = "25";
    // 六合彩十不中玩法id
    private final String PLAY_ID_NO_OPEN_TEN = "26";

    // 六合彩尾数全尾玩法id
    private final String PLAY_ID_WS_QW = "19";
    // 六合彩尾数特尾玩法id
    private final String PLAY_ID_WS_TW = "20";
    // 六合彩平特玩法id
    private final String PLAY_ID_PT_PT = "27";
    // 六合彩特肖玩法id
    private final String PLAY_ID_TX_TX = "28";
    // 六合彩六肖连中玩法id
    private final String PLAY_ID_LX_LXLZ = "29";
    // 六合彩六肖连不中玩法id
    private final String PLAY_ID_LX_LXLBZ = "30";

    // 六合彩连肖中玩法id集合
    private final List<String> PLAY_IDS_LX_WIN = Lists.newArrayList("31", "33", "35");
    // 六合彩二连肖中玩法id
    private final String PLAY_ID_LX_TWO_WIN = "31";
    // 六合彩三连肖中玩法id
    private final String PLAY_ID_LX_THREE_WIN = "33";
    // 六合彩四连肖中玩法id
    private String PLAY_ID_LX_FOUR_WIN = "35";
    // 六合彩连肖不中玩法id集合
    private final List<String> PLAY_IDS_LX_NO_WIN = Lists.newArrayList("32", "34", "36");
    // 六合彩二连肖不中玩法id
    private final String PLAY_ID_LX_TWO_NO_WIN = "32";
    // 六合彩三连肖不中玩法id
    private final String PLAY_ID_LX_THREE_NO_WIN = "34";
    // 六合彩四连肖不中玩法id
    private final String PLAY_ID_LX_FOUR_NO_WIN = "36";

    // 六合彩连尾中玩法id集合
    private final List<String> PLAY_IDS_LW_WIN = Lists.newArrayList("37", "39", "41");
    // 六合彩二连尾中玩法id
    private final String PLAY_ID_LW_TWO_WIN = "37";
    // 六合彩三连尾中玩法id
    private final String PLAY_ID_LW_THREE_WIN = "39";
    // 六合彩四连尾中玩法id
    private final String PLAY_ID_LW_FOUR_WIN = "41";
    // 六合彩连尾不中玩法id集合
    private final List<String> PLAY_IDS_LW_NO_WIN = Lists.newArrayList("38", "40", "42");
    // 六合彩二连尾不中玩法id
    private final String PLAY_ID_LW_TWO_NO_WIN = "38";
    // 六合彩三连尾不中玩法id
    private final String PLAY_ID_LW_THREE_NO_WIN = "40";
    // 六合彩四连尾不中玩法id
    private final String PLAY_ID_LW_FOUR_NO_WIN = "42";

    // 六合彩1-6龙虎玩法id
    private final String PLAY_ID_ONE_SIX_LH = "43";
    // 六合彩五行玩法id
    private final String PLAY_ID_WUXING = "44";

    // 可能打和的投注信息
    private final List<String> MAYBE_HE = Lists.newArrayList("大", "小", "单", "双", "合单", "合双", "尾大", "尾小", "家禽", "野兽");


    /**
     * 结算【六合彩特码特码A】
     *
     * @param issue  期号
     * @param number 开奖号码
     */
    @Override
    public void clearingLhcTeMaA(String issue, String number, int lotteryId, boolean jiesuanOrNot) {
        // 特码特码A
        clearingLhcOnePlayOdds(issue, number, this.generationLHCPlayId(PLAY_ID_TM_TMA, lotteryId), lotteryId, jiesuanOrNot);
        // 特码特码A两面
        clearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_TM_TMA_LM, lotteryId), lotteryId, jiesuanOrNot);
    }

    @Override
    public void clearingLhcZhengMaA(String issue, String number, int lotteryId, boolean jiesuanOrNot) {
        // 正码正码A两面
        clearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_ZM_ZMA, lotteryId), lotteryId, jiesuanOrNot);
        // 正码正码A
        clearingLhcOnePlayOdds(issue, number, this.generationLHCPlayId(PLAY_ID_ZM_ZMA, lotteryId), lotteryId, jiesuanOrNot);
    }

    /**
     * 结算【六合彩正码1-6】
     *
     * @param issue 期号
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
     * 结算【六合彩正特】
     *
     * @param issue 期号
     */
    @Override
    public void clearingLhcZhengTe(String issue, String number, int lotteryId, boolean jiesuanOrNot) {
        // 正(1-6)特
        clearingOnePlayOneOdds(issue, number, this.generationLHCPlayIdList(PLAY_IDS_ZT_OTS, lotteryId), lotteryId, jiesuanOrNot);
        // 正(1-6)特 两面
        clearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_ZT_ONE, lotteryId), lotteryId, jiesuanOrNot);
        clearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_ZT_TWO, lotteryId), lotteryId, jiesuanOrNot);
        clearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_ZT_THREE, lotteryId), lotteryId, jiesuanOrNot);
        clearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_ZT_FOUR, lotteryId), lotteryId, jiesuanOrNot);
        clearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_ZT_FIVE, lotteryId), lotteryId, jiesuanOrNot);
        clearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_ZT_SIX, lotteryId), lotteryId, jiesuanOrNot);
    }

    /**
     * 结算【六合彩连码】
     *
     * @param issue 期号
     */
    @Override
    public void clearingLhcLianMa(String issue, String number, int lotteryId, boolean jiesuanOrNot) {
        // 三全中,二全中,特串
        clearingOnePlayOneOdds(issue, number, this.generationLHCPlayIdList(PLAY_IDS_LM_QZ, lotteryId), lotteryId, jiesuanOrNot);
        // 三中二,二中特
        clearingOnePlayTwoOdds(issue, number, this.generationLHCPlayIdList(PLAY_IDS_LM_EZ, lotteryId), lotteryId, jiesuanOrNot);
    }

    /**
     * 结算【六合彩半波】
     *
     * @param issue 期号
     */
    @Override
    public void clearingLhcBanBo(String issue, String number, int lotteryId, boolean jiesuanOrNot) {
        clearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_BB_RED, lotteryId), lotteryId, jiesuanOrNot);
        clearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_BB_BLUE, lotteryId), lotteryId, jiesuanOrNot);
        clearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_BB_GREEN, lotteryId), lotteryId, jiesuanOrNot);
    }

    /**
     * 结算【六合彩尾数】
     *
     * @param issue 期号
     */
    @Override
    public void clearingLhcWs(String issue, String number, int lotteryId, boolean jiesuanOrNot) {
        clearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_WS_QW, lotteryId), lotteryId, jiesuanOrNot);
        clearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_WS_TW, lotteryId), lotteryId, jiesuanOrNot);
    }

    /**
     * 结算【六合彩不中】
     *
     * @param issue 期号
     */
    @Override
    public void clearingLhcNoOpen(String issue, String number, int lotteryId, boolean jiesuanOrNot) {
        clearingOnePlayOneOdds(issue, number, this.generationLHCPlayIdList(PLAY_IDS_NO_OPEN, lotteryId), lotteryId, jiesuanOrNot);
    }

    /**
     * 结算【六合彩平特平特】
     *
     * @param issue 期号
     */
    @Override
    public void clearingLhcPtPt(String issue, String number, int lotteryId, boolean jiesuanOrNot) {
        clearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_PT_PT, lotteryId), lotteryId, jiesuanOrNot);
    }

    /**
     * 结算【六合彩特肖特肖】
     *
     * @param issue 期号
     */
    @Override
    public void clearingLhcTxTx(String issue, String number, int lotteryId, boolean jiesuanOrNot) {
        clearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_TX_TX, lotteryId), lotteryId, jiesuanOrNot);
    }

    /**
     * 结算【六合彩六肖】
     *
     * @param issue 期号
     */
    @Override
    public void clearingLhcLiuXiao(String issue, String number, int lotteryId, boolean jiesuanOrNot) {
        clearingLhcOnePlayOdds(issue, number, this.generationLHCPlayId(PLAY_ID_LX_LXLZ, lotteryId), lotteryId, jiesuanOrNot);
        clearingLhcOnePlayOdds(issue, number, this.generationLHCPlayId(PLAY_ID_LX_LXLBZ, lotteryId), lotteryId, jiesuanOrNot);
    }

    /**
     * 结算【六合彩连肖中】
     *
     * @param issue 期号
     */
    @Override
    public void clearingLhcLianXiao(String issue, String number, int lotteryId, boolean jiesuanOrNot) {
        clearingOnePlayTwoOdds(issue, number, this.generationLHCPlayIdList(PLAY_IDS_LX_WIN, lotteryId), lotteryId, jiesuanOrNot);
        clearingOnePlayOneOdds(issue, number, this.generationLHCPlayIdList(PLAY_IDS_LX_NO_WIN, lotteryId), lotteryId, jiesuanOrNot);
    }

    /**
     * 结算【六合彩连尾中】
     *
     * @param issue 期号
     */
    @Override
    public void clearingLhcLianWei(String issue, String number, int lotteryId, boolean jiesuanOrNot) {
        clearingOnePlayOneOdds(issue, number, this.generationLHCPlayIdList(PLAY_IDS_LW_WIN, lotteryId), lotteryId, jiesuanOrNot);
        clearingOnePlayTwoOdds(issue, number, this.generationLHCPlayIdList(PLAY_IDS_LW_NO_WIN, lotteryId), lotteryId, jiesuanOrNot);
    }

    /**
     * 结算【六合彩1-6龙虎】
     *
     * @param issue 期号
     */
    @Override
    public void clearingLhcOneSixLh(String issue, String number, int lotteryId, boolean jiesuanOrNot) {
        clearingLhcOnePlayOdds(issue, number, this.generationLHCPlayId(PLAY_ID_ONE_SIX_LH, lotteryId), lotteryId, jiesuanOrNot);
    }

    /**
     * 结算【六合彩五行】
     *
     * @param issue 期号
     */
    @Override
    public void clearingLhcWuxing(String issue, String number, int lotteryId, boolean jiesuanOrNot) {
        clearingLhcOnePlayOdds(issue, number, this.generationLHCPlayId(PLAY_ID_WUXING, lotteryId), lotteryId, jiesuanOrNot);
    }

    /**
     * 一种玩法只有一种赔率,而且投注号码可中一注或多注
     *
     * @param issue   期号
     * @param number  开奖号码
     * @param playIds 玩法id集合
     */
    @SuppressWarnings("rawtypes")
    private void clearingOnePlayOneOdds(String issue, String number, List<Integer> playIds, int lotteryId, boolean jiesuanOrNot) {
        // 获取该期赛果信息
        String date = DateUtils.formatDate(new Date(), "yyyy-MM-dd");
        // 获取相应的订单信息
        int ordercount = orderWriteService.countOrderBetList(issue, playIds, String.valueOf(lotteryId), OrderBetStatus.WAIT);
        if (ordercount <= 0) {
            return;
        }

        //  List<OrderRecord> orderRecords = orderWriteService.selectOrdersPage(lotteryId, issue, OrderStatus.NORMAL, i);
        //   Map<Integer, OrderRecord> orderMap = new HashMap<>();
        // 获取相关订单id集合
        //    List<Integer> orderIds = new ArrayList<>();
        //    this.updateOrder(number, orderRecords, orderIds, orderMap);
        // 获取赔率因子
        double divisor = betCommonService.getDivisor(lotteryId);
        // 查询所有所有相关投注信息
        List<OrderBetRecord> orderBetRecords = orderWriteService.selectOrderBetList(issue, String.valueOf(lotteryId), playIds, OrderBetStatus.WAIT, "OnePlayOne");
        if (!jiesuanOrNot) {
            if (orderBetRecords.size() >= 2) {
                orderBetRecords = orderBetRecords.subList(0, 2);
            }
        }

        int whileMax = ordercount / Constants.CLEARNUM + 1;   //最大循环次数
        int whileNumber = 0;
        while (!CollectionUtils.isEmpty(orderBetRecords)) {
            whileNumber++;
            if (whileNumber > whileMax) {
                break;
            }
            int continueSize = 0;
            // 获取所有赔率信息
            for (OrderBetRecord orderBet : orderBetRecords) {
                // 获取所有配置id
                List<Integer> settingIds = new ArrayList<>();
                settingIds.add(orderBet.getSettingId());
                String betNumber = orderBet.getBetNumber();
                boolean playType = betNumber.indexOf("不中") >= 0;
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
                        // 正特1 正特2...
                        odds = (LotteryPlayOdds) oddsMap.get(betNumber.split("@")[1].split(",")[0]);
                    }
                }
                BigDecimal winAmount = new BigDecimal(0);
                try {
                    String betNumberLin = orderBet.getBetNumber().replace("一", "1").replace("二", "2")
                            .replace("三", "3").replace("四", "4").replace("五", "5").replace("六", "6");
                    if (betNumberLin.indexOf("两面") >= 0
                            //做一个特殊处理 正特两面过滤
                            || ((betNumberLin.indexOf("正1特") >= 0 || betNumberLin.indexOf("正2特") >= 0 || betNumberLin.indexOf("正3特") >= 0
                            || betNumberLin.indexOf("正4特") >= 0 || betNumberLin.indexOf("正5特") >= 0 || betNumberLin.indexOf("正6特") >= 0)
                            &&
                            (betNumberLin.indexOf("大") >= 0 || betNumberLin.indexOf("小") >= 0 || betNumberLin.indexOf("单") >= 0
                                    || betNumberLin.indexOf("双") >= 0 || betNumberLin.indexOf("尾大") >= 0 || betNumberLin.indexOf("尾小") >= 0
                                    || betNumberLin.indexOf("合单") >= 0 || betNumberLin.indexOf("合双") >= 0 || betNumberLin.indexOf("红波") >= 0
                                    || betNumberLin.indexOf("蓝波") >= 0 || betNumberLin.indexOf("绿波") >= 0))
                    ) {
                        continueSize++;
                        continue;
                    }
                    orderBet.setWinCount("0");
                    // 判断是否中奖,获取中奖注数
                    int winCounts = LhcUtils.isWinByOnePlayOneOdds(orderBet.getBetNumber(), number, orderBet.getPlayId(), date, lotteryId);
                    if (winCounts > 0) {
                        // 获取总注数/中奖注数
                        String winCount = odds.getWinCount();
                        String totalCount = odds.getTotalCount();
                        // 计算赔率
                        double odd = Double.parseDouble(totalCount) * 1.0 / Double.parseDouble(winCount) * divisor;
                        //一注的中奖额
                        winAmount = orderBet.getBetAmount().divide(BigDecimal.valueOf(orderBet.getBetCount()), BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(odd));
                        winAmount = winAmount.multiply(BigDecimal.valueOf(winCounts));
                        orderBet.setWinCount(String.valueOf(winCounts));
                    }
                    // 根据中奖金额,修改投注信息及相关信息
                    //  OrderRecord orderRecord = orderMap.get(orderBet.getOrderId());
                    if (jiesuanOrNot) {
                        logger.info("clearingOnePlayOneOdds 开奖判断信息{};{};{};{};{};{}", orderBet.getOrderSn(), orderBet.getBetNumber(), number, orderBet.getPlayId(), lotteryId, winAmount);
                        betCommonService.winOrLose(orderBet, winAmount, orderBet.getUserId(), orderBet.getOrderSn());
                    } else {
                        //保存结算信息
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
                    logger.error("订单结算出错 事务冲突 进行重试。orderSn:{}.", orderBet.getOrderSn(), e);
                    for (int i = 0; i < 20; i++) {
                        try {
                            if (jiesuanOrNot) {
                                betCommonService.winOrLose(orderBet, winAmount, orderBet.getUserId(), orderBet.getOrderSn());
                            }
                        } catch (TransactionSystemException e2) {
                            logger.error("订单结算出错,事务冲突 进行重试。orderSn:{}.", orderBet.getOrderSn(), e2);
                            try {
                                Thread.sleep(100);
                            } catch (Exception e3) {
                            }
                            continue;
                        }
                        break;
                    }
                } catch (Exception e) {
                    logger.error("订单结算出错，lotteryId:{},issue:{},betNum:{}", orderBet.getLotteryId(), issue, orderBet.getBetNumber(), e);
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
     * 一种玩法只有一条赔率记录,但有高低2种赔率,而且投注号码可中一注或多注
     *
     * @param issue   期号
     * @param playIds 玩法id集合
     */
    private void clearingOnePlayTwoOdds(String issue, String number, List<Integer> playIds, int lotteryId, boolean jiesuanOrNot) {
        String date = DateUtils.formatDate(new Date(), "yyyy-MM-dd");
//        }

        // 获取相应的订单信息
        int ordercount = orderWriteService.countOrderBetList(issue, playIds, String.valueOf(lotteryId), OrderBetStatus.WAIT);
        if (ordercount <= 0) {
            return;
        }

        //  List<OrderRecord> orderRecords = orderWriteService.selectOrdersPage(lotteryId, issue, OrderStatus.NORMAL, i);
        //   Map<Integer, OrderRecord> orderMap = new HashMap<>();
        // 获取相关订单id集合
        //    List<Integer> orderIds = new ArrayList<>();
        //    this.updateOrder(number, orderRecords, orderIds, orderMap);
        // 获取赔率因子
        double divisor = betCommonService.getDivisor(lotteryId);
        // 查询所有所有相关投注信息
        List<OrderBetRecord> orderBetRecords = orderWriteService.selectOrderBetList(issue, String.valueOf(lotteryId), playIds, OrderBetStatus.WAIT, "OnePlayTwo");
        if (!jiesuanOrNot) {
            if (orderBetRecords.size() >= 2) {
                orderBetRecords = orderBetRecords.subList(0, 2);
            }
        }

        int whileMax = ordercount / Constants.CLEARNUM + 1;   //最大循环次数
        int whileNumber = 0;
        while (!CollectionUtils.isEmpty(orderBetRecords)) {
            whileNumber++;
            if (whileNumber > whileMax) {
                break;
            }
            for (OrderBetRecord orderBet : orderBetRecords) {
                BigDecimal winAmount = new BigDecimal(0);
                try {
                    // 获取所有配置id
                    List<Integer> settingIds = new ArrayList<>();

                    settingIds.add(orderBet.getSettingId());


                    // 获取所有赔率信息
                    Map<String, LotteryPlayOdds> oddsMap = lotteryPlayOddsService.selectPlayOddsBySettingId(settingIds.get(0));
                    orderBet.setWinCount("0");
                    // 判断是否中奖,获取中奖注数
                    List<Integer> twoOdds = LhcUtils.isWinByOnePlayTwoOdds(orderBet.getBetNumber(), number, orderBet.getPlayId(), date, lotteryId);
                    if (twoOdds.get(0) > 0 || twoOdds.get(1) > 0) {
                        int bigOddsWins = twoOdds.get(0);
                        int smallOddsWins = twoOdds.get(1);
                        orderBet.setWinCount(String.valueOf(bigOddsWins + smallOddsWins));
                        if (bigOddsWins > 0) {
                            // 获取 高赔率生肖 赔率信息
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

                            // 获取总注数/中奖注数 & 计算赔率
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
                            // 获取 低赔率生肖 赔率信息
                            LotteryPlayOdds odds;
                            if (oddsMap.size() == 1) {
                                odds = oddsMap.get(oddsMap.keySet().iterator().next());
                            } else {
                                String shengXiao = LhcUtils.getShengXiao(date);
                                odds = oddsMap.get(shengXiao);
                            }
                            //低赔率为0尾的赔率
                            if (odds == null) {
                                odds = oddsMap.get("0尾");
                            }
                            // 获取总注数/中奖注数 & 计算赔率
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
                        logger.info("clearingOnePlayTwoOdds 开奖判断信息{};{};{};{};{};{};{};{}", orderBet.getOrderSn(), orderBet.getBetNumber(), number, orderBet.getPlayId(), lotteryId, twoOdds.get(0), twoOdds.get(1), winAmount);
                    }

                    // 根据中奖金额,修改投注信息及相关信息
                    //   OrderRecord orderRecord = orderMap.get(orderBet.getOrderId());
                    if (jiesuanOrNot) {
                        betCommonService.winOrLose(orderBet, winAmount, orderBet.getUserId(), orderBet.getOrderSn());
                    } else {
                        //保存结算信息
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
                    logger.error("订单结算出错 事务冲突 进行重试:{},{}", orderBet.getOrderSn(), e);
                    for (int i = 0; i < 20; i++) {
                        try {
                            if (jiesuanOrNot) {
                                betCommonService.winOrLose(orderBet, winAmount, orderBet.getUserId(), orderBet.getOrderSn());
                            }
                        } catch (TransactionSystemException e2) {
                            logger.error("订单结算出错:{},事务冲突 进行重试:{},{}", orderBet.getOrderSn(), e2);
                            try {
                                Thread.sleep(100);
                            } catch (Exception e3) {

                            }
                            continue;
                        }
                        break;
                    }
                } catch (Exception e) {
                    logger.error("订单结算出错，lotteryId:{},issue:{},betNum:{}", orderBet.getLotteryId(), issue, orderBet.getBetNumber(), e);
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
     * 一种玩法只有一种赔率,投注号码可中一注或多注
     *
     * @param issue        期号
     * @param number       开奖号码
     * @param playId       玩法id
     * @param jiesuanOrNot 是否真实结算
     */
    private void clearingLhcOnePlayOdds(String issue, String number, Integer playId, int lotteryId, boolean jiesuanOrNot) {
        long begin = System.currentTimeMillis();
        logger.debug("结算开始......");
        String date = DateUtils.formatDate(new Date(), "yyyy-MM-dd");
        if (jiesuanOrNot) {
            int upcount = orderWriteService.updateOrderRecord(String.valueOf(lotteryId), issue, number);
            while (upcount > 0) {
                upcount = orderWriteService.updateOrderRecord(String.valueOf(lotteryId), issue, number);
            }
        }

        logger.debug("updateOrderRecord time, {}", System.currentTimeMillis() - begin);
        // 获取相应的订单信息
        List playlist = new ArrayList();
        playlist.add(playId);
        int ordercount = orderWriteService.countOrderBetList(issue, playlist, String.valueOf(lotteryId), OrderBetStatus.WAIT);
        logger.debug("countOrderBetList time, {}", System.currentTimeMillis() - begin);
        if (ordercount <= 0) {
            return;
        }

        //  List<OrderRecord> orderRecords = orderWriteService.selectOrdersPage(lotteryId, issue, OrderStatus.NORMAL, i);
        //   Map<Integer, OrderRecord> orderMap = new HashMap<>();
        // 获取相关订单id集合
        //    List<Integer> orderIds = new ArrayList<>();
        //    this.updateOrder(number, orderRecords, orderIds, orderMap);
        // 获取赔率因子
        double divisor = betCommonService.getDivisor(lotteryId);
        // 查询所有所有相关投注信息
        // 查询所有所有相关投注信息
        List<OrderBetRecord> orderBetRecords = orderWriteService.selectOrderBetList(issue, String.valueOf(lotteryId), playlist, OrderBetStatus.WAIT, "OnePlay");
        if (!jiesuanOrNot) {
            if (orderBetRecords.size() >= 2) {
                orderBetRecords = orderBetRecords.subList(0, 2);
            }
        }

        OrderBetRecord order = new OrderBetRecord();
        int whileMax = ordercount / Constants.CLEARNUM + 1;   //最大循环次数
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
                    // 如果是六肖连中或者六肖连不中玩法,特肖开出49为和值
                    if ("49".equals(number.split(",")[6])) {
                        betCommonService.noWinOrLose(orderBetRecords);
                        orderBetRecords = orderWriteService.selectOrderBetList(issue, String.valueOf(lotteryId), playlist, OrderBetStatus.WAIT, "OnePlay");
                    }
                }

                // 获取配置id
                if (orderBetRecords.size() == 0) {
                    return;
                }
                Integer settingId = orderBetRecords.get(0).getSettingId();

                // 获取赔率信息
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

                    if (orderBet.getBetNumber().indexOf("两面") >= 0) {
                        continue;
                    }
                    if (orderBet.getBetNumber().contains("五行")) {
                        List<LotteryPlayOdds> oddList = lotteryPlayOddsService.selectOddsListBySettingId(settingId);
                        Map<String, LotteryPlayOdds> oddsMap = new HashMap<>();
                        for (LotteryPlayOdds lotteryPlayodds : oddList) {
                            oddsMap.put(lotteryPlayodds.getName(), lotteryPlayodds);
                        }
                        String betName = orderBet.getBetNumber().replace("五行@", "");
                        odds = oddsMap.get(betName);
                    }
                    BigDecimal winAmount = new BigDecimal(0);
                    orderBet.setWinCount("0");
                    // 判断是否中奖,获取中奖注数
                    int winCounts = LhcUtils.isWinByOnePlayOneOdds(orderBet.getBetNumber(), number, orderBet.getPlayId(), date, lotteryId);
                    if (winCounts > 0) {
                        // 获取总注数/中奖注数
                        String winCount = odds.getWinCount();
                        String totalCount = odds.getTotalCount();
                        // 计算赔率
                        double odd = Double.parseDouble(totalCount) * 1.0 / Double.parseDouble(winCount) * divisor;
                        //一注的中奖额
                        winAmount = orderBet.getBetAmount().divide(BigDecimal.valueOf(orderBet.getBetCount())).multiply(BigDecimal.valueOf(odd));
                        winAmount = winAmount.multiply(BigDecimal.valueOf(winCounts));
                        orderBet.setWinCount(String.valueOf(winCounts));
                    }

                    // 根据中奖金额,修改投注信息及相关信息
                    //  OrderRecord orderRecord = orderMap.get(orderBet.getOrderId());
                    try {
                        if (jiesuanOrNot) {
                            logger.info("clearingLhcOnePlayOdds 开奖判断信息{};{};{};{};{};{};{}", orderBet.getOrderSn(), orderBet.getBetNumber(), number, orderBet.getPlayId(), lotteryId, winCounts, winAmount);
                            betCommonService.winOrLose(orderBet, winAmount, orderBet.getUserId(), orderBet.getOrderSn());
                        } else {
                            //保存结算信息
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
                        logger.error("订单结算出错 事务冲突 进行重试:{},{}", orderBet.getOrderSn(), e1);
                        for (int i = 0; i < 20; i++) {
                            try {
                                if (jiesuanOrNot) {
                                    betCommonService.winOrLose(orderBet, winAmount, orderBet.getUserId(), orderBet.getOrderSn());
                                }
                            } catch (TransactionSystemException e2) {
                                logger.error("订单结算出错:{},事务冲突 进行重试:{},{}", i, orderBet.getOrderSn(), e2);
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
                logger.error("订单结算出错，lotteryId:{},issue:{},betNum:{},{}", order.getLotteryId(), issue, order.getBetNumber(), e);
                break;
            }
        }

    }

    /**
     * 一种玩法多种赔率,一条记录投注号码可中一注或多注
     *
     * @param issue
     * @param playId
     */

    private void clearingOnePlayManyOdds(String issue, String number, Integer playId, int lotteryId, boolean jiesuanOrNot) {
        String date = DateUtils.formatDate(new Date(), "yyyy-MM-dd");
        // 获取相应的订单信息
        List playlist = new ArrayList();
        playlist.add(playId);
        int ordercount = orderWriteService.countOrderBetList(issue, playlist, String.valueOf(lotteryId), OrderBetStatus.WAIT);
        if (ordercount <= 0) {
            return;
        }
        //  List<OrderRecord> orderRecords = orderWriteService.selectOrdersPage(lotteryId, issue, OrderStatus.NORMAL, i);
        //   Map<Integer, OrderRecord> orderMap = new HashMap<>();
        // 获取相关订单id集合
        //    List<Integer> orderIds = new ArrayList<>();
        //    this.updateOrder(number, orderRecords, orderIds, orderMap);
        // 获取赔率因子
        double divisor = betCommonService.getDivisor(lotteryId);
        // 查询所有所有相关投注信息

        List<OrderBetRecord> orderBetRecords = orderWriteService.selectOrderBetList(issue, String.valueOf(lotteryId), playlist, OrderBetStatus.WAIT, "OnePlayMany");
        if (!jiesuanOrNot) {
            if (orderBetRecords.size() >= 2) {
                orderBetRecords = orderBetRecords.subList(0, 2);
            }
        }

        OrderBetRecord order = new OrderBetRecord();
        int whileMax = ordercount / Constants.CLEARNUM + 1;   //最大循环次数
        int whileNumber = 0;
        while (!CollectionUtils.isEmpty(orderBetRecords)) {
            whileNumber++;
            if (whileNumber > whileMax) {
                break;
            }
            try {
                //  Map<Integer, OrderRecord> orderMap=orderWriteService.getOrderMap(orderBetRecords);
                String betNumber = orderBetRecords.get(0).getBetNumber();
                // 玩法设计...共用了一个playID....
                if (this.generationLHCPlayId(PLAY_ID_TM_TMA_LM, lotteryId).equals(playId) && betNumber.indexOf("特码两面") < 0
                        || this.generationLHCPlayId(PLAY_ID_TM_TMA_LM, lotteryId).equals(playId) && betNumber.indexOf("正码两面") > 0
                        || this.generationLHCPlayId(PLAY_ID_ZM_ZMA, lotteryId).equals(playId) && betNumber.indexOf("两面") < 0) {
                    return;
                }
                // 波色投注号码不存在打和的情况
                if (betNumber.indexOf("波色") < 0
                        && this.generationLHCPlayId(PLAY_ID_TM_TMA_LM, lotteryId).equals(playId)) {
                    // 如果是特码A两面玩法,部分投注,特肖开出49为和值
                    if ("49".equals(number.split(",")[6])) {
                        orderBetRecords = noWinOrLose(orderBetRecords);
                    }
                }

                // 正码1-6(正1-6特两面)的和值情况
                if (number.indexOf("49") >= 0) {
                    String[] sgArr = number.split(",");
                    if ("49".equals(sgArr[0]) && (playId.equals(this.generationLHCPlayId(PLAY_IDS_ZM, lotteryId)) || playId.equals(this.generationLHCPlayId(PLAY_ID_ZT_ONE, lotteryId)))) {
                        List<OrderBetRecord> heList = new ArrayList<>();
                        List<OrderBetRecord> noheList = new ArrayList<>();
                        for (OrderBetRecord orderBetRecord : orderBetRecords) {
                            if (orderBetRecord.getBetNumber().contains("正码一") || playId.equals(this.generationLHCPlayId(PLAY_ID_ZT_ONE, lotteryId))) {
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
                            if (orderBetRecord.getBetNumber().contains("正码二") || playId.equals(this.generationLHCPlayId(PLAY_ID_ZT_TWO, lotteryId))) {
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
                            if (orderBetRecord.getBetNumber().contains("正码三") || playId.equals(this.generationLHCPlayId(PLAY_ID_ZT_THREE, lotteryId))) {
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
                            if (orderBetRecord.getBetNumber().contains("正码四") || playId.equals(this.generationLHCPlayId(PLAY_ID_ZT_FOUR, lotteryId))) {
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
                            if (orderBetRecord.getBetNumber().contains("正码五") || playId.equals(this.generationLHCPlayId(PLAY_ID_ZT_FIVE, lotteryId))) {
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
                            if (orderBetRecord.getBetNumber().contains("正码六") || playId.equals(this.generationLHCPlayId(PLAY_ID_ZT_SIX, lotteryId))) {
                                heList.add(orderBetRecord);
                            } else {
                                noheList.add(orderBetRecord);
                            }
                        }
                        noheList.addAll(noWinOrLose(heList));
                        orderBetRecords = noheList;
                    }
                }

                // 获取配置id
                if (orderBetRecords.size() == 0) {
                    return;
                }
                Integer settingId = orderBetRecords.get(0).getSettingId();

                // 获取所有赔率信息
                Map<String, LotteryPlayOdds> oddsMap = lotteryPlayOddsService.selectPlayOddsBySettingId(settingId);

                for (OrderBetRecord orderBet : orderBetRecords) {
                    order = orderBet;
                    BigDecimal winAmount = new BigDecimal(0);
                    orderBet.setWinCount("0");
                    // 判断是否中奖,获取中奖信息
                    String winNum = LhcUtils.isWinByOnePlayManyOdds(orderBet.getBetNumber(), number, orderBet.getPlayId(), date, lotteryId);
                    if (StringUtils.isNotBlank(winNum)) {
                        String[] winStrArr = winNum.split(",");
                        int wincount = 0;
                        for (String winStr : winStrArr) {
                            boolean boHe = false;
                            if (winStr.contains("和单")) {
                                boHe = true;
                                winStr = winStr.replace("和单", "");
                            }
                            // 获取赔率信息
                            LotteryPlayOdds odds = oddsMap.get(winStr);
                            // 获取总注数/中奖注数
                            String winCount = odds.getWinCount();
                            String totalCount = odds.getTotalCount();
                            // 计算赔率
                            double odd = Double.parseDouble(totalCount) * 1.0 / Double.parseDouble(winCount) * divisor;
                            //中奖额
                            if (boHe) {
                                winAmount = winAmount.add(orderBet.getBetAmount().divide(BigDecimal.valueOf(orderBet.getBetCount())));
                            } else {
                                winAmount = winAmount.add(orderBet.getBetAmount().divide(BigDecimal.valueOf(orderBet.getBetCount())).multiply(BigDecimal.valueOf(odd)));
                                wincount = wincount + 1;
                            }
                        }
                        orderBet.setWinCount(String.valueOf(wincount));
                    }
                    // 根据中奖金额,修改投注信息及相关信息
                    try {
                        logger.info("clearingOnePlayManyOdds 开奖判断信息{};{};{};{};{};{};{}", orderBet.getOrderSn(), orderBet.getBetNumber(), number, orderBet.getPlayId(), lotteryId, winNum, winAmount);
                        if (jiesuanOrNot) {
                            betCommonService.winOrLose(orderBet, winAmount, orderBet.getUserId(), orderBet.getOrderSn());
                        } else {
                            //保存结算信息
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
                        logger.error("订单结算出错 事务冲突 进行重试:{},{}", orderBet.getOrderSn(), e1);
                        for (int i = 0; i < 20; i++) {
                            try {
                                if (jiesuanOrNot) {
                                    betCommonService.winOrLose(orderBet, winAmount, orderBet.getUserId(), orderBet.getOrderSn());
                                }
                            } catch (TransactionSystemException e2) {
                                logger.error("订单结算出错:{},事务冲突 进行重试:{},{}", i, orderBet.getOrderSn(), e2);
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
                logger.error("订单结算出错，lotteryId:{},issue:{},betNum:{},{}", order.getLotteryId(), issue, order.getBetNumber(), e);
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
            // 获取开奖结果
            String openNumber = order.getOpenNumber();
            // 判断是否已开奖
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
     * 两面玩法开49打和的处理方式
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
                // 设置中奖金额
                orderBet.setWinAmount(winAmount);
                // 设置状态
                orderBet.setTbStatus(OrderBetStatus.HE);
                orderBet.setWinCount("0");
                // 修改投注信息
                orderBetRecordMapper.updateByPrimaryKeySelective(orderBet);
                // 修改用户余额信息
                OrderRecord orderRecord = orderMap.get(orderBet.getOrderId());
                betCommonService.updateMemberBalance(orderBet, winAmount, orderRecord.getUserId(), orderRecord.getOrderSn());
            } else {
                newOrderBetRecords.add(orderBet);
            }
        }
        return newOrderBetRecords;
    }

    /**
     * 两面玩法开49打和的处理方式   （只针对一注的情况）
     *
     * @param orderBetRecords
     */
    private List<OrderBetRecord> noWinOrLose(List<OrderBetRecord> orderBetRecords) {
        List<OrderBetRecord> newOrderBetRecords = new ArrayList<>();
        for (OrderBetRecord orderBet : orderBetRecords) {
            String[] betNumbetArr = orderBet.getBetNumber().split("@");
            if (MAYBE_HE.contains(betNumbetArr[1])) {
                BigDecimal winAmount = getTradeOffAmount(orderBet.getBetAmount());
                // 设置中奖金额
                orderBet.setWinAmount(winAmount);
                // 设置状态
                orderBet.setTbStatus(OrderBetStatus.HE);
                orderBet.setWinCount("0");
                // 修改投注信息
                orderBetRecordMapper.updateByPrimaryKeySelective(orderBet);
                // 修改用户余额信息

                betCommonService.updateMemberBalance(orderBet, winAmount, orderBet.getUserId(), orderBet.getOrderSn());
            } else {
                newOrderBetRecords.add(orderBet);
            }
        }
        return newOrderBetRecords;
    }

    /**
     * 获取六合彩中奖注数(一种玩法,有高低两种赔率)
     * (适用玩法:连肖中,连尾不中,连码三中二,连码二中特)
     *
     * @param betNumber 投注号码
     * @param sgNumber  开奖号码
     * @param playId    玩法id
     * @param dateStr   开奖时间 yyyy-MM-dd
     * @return
     */
    private List<Integer> isWinByOnePlayTwoOdds(String betNumber, String sgNumber, Integer playId, String dateStr, Integer lotteryId) {
        if (this.generationLHCPlayIdList(PLAY_IDS_LX_WIN, lotteryId).contains(playId)) {
            // 连肖中玩法
            return isWinByLxWin(betNumber, sgNumber, playId, dateStr, lotteryId);
        } else if (this.generationLHCPlayIdList(PLAY_IDS_LW_NO_WIN, lotteryId).contains(playId)) {
            // 连尾不中玩法
            return isWinByLwNoWin(betNumber, sgNumber, playId, lotteryId);
        } else if (this.generationLHCPlayIdList(PLAY_IDS_LM_EZ, lotteryId).contains(playId)) {
            // 连码三中二,连码二中特
            return isWinLianMaEz(betNumber, sgNumber, playId, lotteryId);
        }
        return null;
    }

    /**
     * 获取六合彩中奖注数(一种玩法,只有一种赔率)
     * (适用玩法: 连码(三全中,二全中,特串,不中,连肖不中), 连尾中, 1-6龙虎, 五行)
     *
     * @param betNumber 投注号码
     * @param sgNumber  开奖号码
     * @param playId    玩法id
     * @param dateStr   开奖时间 yyyy-MM-dd
     * @return
     */
    private int isWinByOnePlayOneOdds(String betNumber, String sgNumber, Integer playId, String dateStr, Integer lotteryId) {
        if (this.generationLHCPlayIdList(PLAY_IDS_LX_NO_WIN, lotteryId).contains(playId)) {
            // 连肖不中玩法
            return isWinByLxNoWin(betNumber, sgNumber, playId, dateStr, lotteryId);
        } else if (this.generationLHCPlayIdList(PLAY_IDS_LW_WIN, lotteryId).contains(playId)) {
            // 连尾中玩法
            return isWinByLwWin(betNumber, sgNumber, playId, lotteryId);
        } else if (this.generationLHCPlayId(PLAY_ID_ONE_SIX_LH, lotteryId).equals(playId)) {
            // 1-6龙虎玩法
            return isWinByOneSixLh(betNumber, sgNumber);
        } else if (this.generationLHCPlayId(PLAY_ID_WUXING, lotteryId).equals(playId)) {
            // 五行玩法
            return isWinByWx(betNumber, sgNumber, dateStr);
        } else if (this.generationLHCPlayId(PLAY_ID_LX_LXLZ, lotteryId).equals(playId) ||
                this.generationLHCPlayId(PLAY_ID_LX_LXLBZ, lotteryId).equals(playId)) {
            // 六肖连中 或者 六肖连不中
            return isWinByLiuXiao(betNumber, sgNumber, playId, dateStr, lotteryId);
        } else if (this.generationLHCPlayIdList(PLAY_IDS_NO_OPEN, lotteryId).contains(playId)) {
            // 不中,如五不中,六不中...
            return isWinByNoOpen(betNumber, sgNumber, playId, lotteryId);
        } else if (this.generationLHCPlayIdList(PLAY_IDS_LM_QZ, lotteryId).contains(playId)) {
            // 连码三全中,二全中,特串
            return isWinLianMaQz(betNumber, sgNumber, playId, lotteryId);
        } else if (this.generationLHCPlayIdList(PLAY_IDS_ZT_OTS, lotteryId).contains(playId)) {
            // 正特正(1-6)特
            return isWinZhengTeOneToSix(betNumber, sgNumber, playId, lotteryId);
        } else if (this.generationLHCPlayId(PLAY_ID_TM_TMA, lotteryId).equals(playId)) {
            // 特码特码A
            return isWinZhengTeOneToSix(betNumber, sgNumber, playId, lotteryId);
        } else if (this.generationLHCPlayId(PLAY_ID_ZM_ZMA, lotteryId).equals(playId)) {
            // 正码正码A
            return isWinByNum(betNumber, sgNumber);
        }
        return 0;
    }

    /**
     * 判断六合彩是否中奖,中奖返回中奖信息,不中则返回null(一种玩法,多种赔率)
     * (适用玩法:尾数,平特,特肖,半波)
     *
     * @param betNumber 投注号码
     * @param sgNumber  开奖号码
     * @param playId    玩法id
     * @param dateStr   开奖时间 yyyy-MM-dd
     * @return
     */
    private String isWinByOnePlayManyOdds(String betNumber, String sgNumber, Integer playId, String dateStr, Integer lotteryId) {
        String[] betNumArr = betNumber.split("@")[1].split(",");
        String[] sgArr = sgNumber.split(",");
        StringBuffer winStr = new StringBuffer();
        if (this.generationLHCPlayId(PLAY_ID_WS_QW, lotteryId).equals(playId)) {
            // 尾数全尾
            List<Integer> sgList = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                sgList.add(Integer.valueOf(sgArr[i]) % 10);
            }
            for (String betNum : betNumArr) {
                if (sgList.contains(Integer.parseInt(betNum.replace("尾", "")))) {
                    winStr.append(betNum).append(",");
                }
            }
        } else if (this.generationLHCPlayId(PLAY_ID_WS_TW, lotteryId).equals(playId)) {
            // 尾数特尾
            int tw = Integer.valueOf(sgArr[6]) % 10;
            for (String betNum : betNumArr) {
                if (tw == Integer.parseInt(betNum.replace("尾", ""))) {
                    winStr.append(betNum).append(",");
                }
            }
        } else if (this.generationLHCPlayId(PLAY_ID_PT_PT, lotteryId).equals(playId)) {
            logger.info("平特信息：");
            // 平特
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
                logger.info("两边判断信息不准{}，{}", winStr, winStr1);
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
                logger.info("编码信息:{},{},{},{},{},{}", sgsUtf8, sgsUnicode, sgsGbk, numsUtf8, numsUnicode, numsGbk);
            } catch (Exception e) {
                logger.error("编码转换出错：{}", e);
            }
            logger.info("平特信息：{},{},{},{},{},{},{},{},{}", sgNumber, sgs, nums, winStr, betNumber, sgNumber, playId, dateStr, lotteryId);
        } else if (this.generationLHCPlayId(PLAY_ID_TX_TX, lotteryId).equals(playId)) {
            // 特肖
            String tx = LhcUtils.getShengXiao(Integer.valueOf(sgArr[6]), dateStr);
            for (String betNum : betNumArr) {
                if (tx.equals(betNum)) {
                    winStr.append(betNum).append(",");
                }
            }
        } else if (this.generationLHCPlayId(PLAY_ID_BB_RED, lotteryId).equals(playId) ||
                this.generationLHCPlayId(PLAY_ID_BB_BLUE, lotteryId).equals(playId) ||
                this.generationLHCPlayId(PLAY_ID_BB_GREEN, lotteryId).equals(playId)) {
            // 半波红波,蓝波,绿波
            List<String> numBanboList = LhcPlayRule.getNumBanboList(Integer.valueOf(sgArr[6]));
            for (String betNum : betNumArr) {
                if (numBanboList.contains(betNum)) {
                    winStr.append(betNum).append(",");
                }
            }
        } else if (this.generationLHCPlayIdList(PLAY_IDS_ZT_OTS, lotteryId).contains(playId) || this.generationLHCPlayId(PLAY_IDS_ZM, lotteryId).equals(playId)) {
            // 正特正(1-6)特 两面 或者 正码1-6
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

            if (this.generationLHCPlayId(PLAY_ID_ZT_ONE, lotteryId).equals(playId) || betNumber.indexOf("正码一") >= 0) {
                index = 0;
            } else if (this.generationLHCPlayId(PLAY_ID_ZT_TWO, lotteryId).equals(playId) || betNumber.indexOf("正码二") >= 0) {
                index = 1;
            } else if (this.generationLHCPlayId(PLAY_ID_ZT_THREE, lotteryId).equals(playId) || betNumber.indexOf("正码三") >= 0) {
                index = 2;
            } else if (this.generationLHCPlayId(PLAY_ID_ZT_FOUR, lotteryId).equals(playId) || betNumber.indexOf("正码四") >= 0) {
                index = 3;
            } else if (this.generationLHCPlayId(PLAY_ID_ZT_FIVE, lotteryId).equals(playId) || betNumber.indexOf("正码五") >= 0) {
                index = 4;
            } else if (this.generationLHCPlayId(PLAY_ID_ZT_SIX, lotteryId).equals(playId) || betNumber.indexOf("正码六") >= 0) {
                index = 5;
            }
            List<String> numLiangMianList = LhcPlayRule.getNumLiangMianList(Integer.valueOf(sgArr[index]));
            numLiangMianList.add(sgArr[index]);
            for (String betNum : betNumArr) {
                if (numLiangMianList.contains(betNum)) {
                    winStr.append(betNum).append(",");
                }
                if ("49".equals(sgArr[index])) {
                    if ("大".equals(betNum) || "小".equals(betNum) || "单".equals(betNum) || "双".equals(betNum) || "合单".equals(betNum) || "合双".equals(betNum)
                            || "尾大".equals(betNum) || "尾小".equals(betNum)) {
                        winStr.append(betNum + "和单").append(",");
                    }
                }
            }
        } else if (this.generationLHCPlayId(PLAY_ID_ZM_ZMA, lotteryId).equals(playId)) {
            // 正码正码A两面
            List<String> totalLiangMian = LhcUtils.getTotalLiangMian(sgNumber);
            for (String betNum : betNumArr) {
                if (totalLiangMian.contains(betNum)) {
                    winStr.append(betNum).append(",");
                }
            }
        } else if (this.generationLHCPlayId(PLAY_ID_TM_TMA_LM, lotteryId).equals(playId)) {
            // 特码特码A两面
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
     * 获取六合彩中奖注数
     * (适用玩法:连码三中二,连码二中特)
     *
     * @param betNumber 投注号码
     * @param sgNumber  开奖号码
     * @param playId    玩法id
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

        List<Integer> wins = new ArrayList<>(2); //默认第一个值是高赔率的中奖注数
        int size = openNum.size();
        int win1 = 0; // 高赔率的中奖注数
        int win2 = 0; // 低赔率的中奖注数
        if (this.generationLHCPlayId(PLAY_ID_LM_EZT, lotteryId).equals(playId)) {
            // 连码二中特
            if (tag) {
                win1 = size;
            }
            win2 = MathUtil.countCnm(size, 2);
        } else if (this.generationLHCPlayId(PLAY_ID_LM_SZE, lotteryId).equals(playId)) {
            // 连码三中二
            win1 = MathUtil.countCnm(size, 3);
            win2 = MathUtil.countCnm(size, 2) * (betNumArr.length - size);
        }
        wins.add(win1);
        wins.add(win2);
        return wins;
    }


    /**
     * 获取六合彩中奖注数
     * (适用玩法:正码正码A)
     *
     * @param betNumber 投注号码
     * @param sgNumber  开奖号码
     * @return
     */
    private int isWinByNum(String betNumber, String sgNumber) {
        String[] betNumArr = betNumber.split("@")[1].split(",");
        String[] sgArr = sgNumber.split(",");
        int winCount = 0;

        // 正码正码A
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
     * 获取六合彩中奖注数
     * (适用玩法:正特正(1-6)特, 特码特码A)
     *
     * @param betNumber 投注号码
     * @param sgNumber  开奖号码
     * @param playId    玩法id
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
        if (betNumber.contains("两面")) {
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
     * 获取六合彩中奖注数
     * (适用玩法:连码三全中,二全中,特串)
     *
     * @param betNumber 投注号码
     * @param sgNumber  开奖号码
     * @param playId    玩法id
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
     * 获取六合彩中奖注数
     * (适用玩法:不中)
     *
     * @param betNumber 投注号码
     * @param sgNumber  开奖号码
     * @param playId    玩法id
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
        int m = 50; //初始化m
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
     * 获取六合彩中奖注数
     * (适用玩法:六肖)
     *
     * @param betNumber 投注号码
     * @param sgNumber  开奖号码
     * @param playId    玩法id
     * @param dateStr   开奖时间 yyyy-MM-dd
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
     * 获取六合彩中奖注数
     * (适用玩法:连肖中)
     *
     * @param betNumber 投注号码
     * @param sgNumber  开奖号码
     * @param playId    玩法id
     * @param dateStr   开奖时间 yyyy-MM-dd
     * @return
     */
    private List<Integer> isWinByLxWin(String betNumber, String sgNumber, Integer playId, String dateStr, Integer lotteryId) {
        // 当期开出的生肖
        List<String> numberShengXiao = LhcUtils.getNumberShengXiao(sgNumber, dateStr);
        // 当期的低赔率的生肖
        String shengXiao = LhcUtils.getShengXiao(dateStr);
        String[] betNumArr = betNumber.split("@")[1].split(",");

        List<String> openSx = new ArrayList<>();
        for (String betNum : betNumArr) {
            if (numberShengXiao.contains(betNum)) {
                openSx.add(betNum);
            }
        }

        List<Integer> wins = new ArrayList<>(2); //默认第一个值是高赔率的中奖注数
        int size = openSx.size();
        int win1 = 0; // 高赔率的中奖注数
        int win2 = 0; // 低赔率的中奖注数

        int playWins = 2;
        if (this.generationLHCPlayId(PLAY_ID_LX_TWO_WIN, lotteryId).equals(playId)) {
            playWins = 2;
        } else if (this.generationLHCPlayId(PLAY_ID_LX_THREE_WIN, lotteryId).equals(playId)) {
            playWins = 3;
        } else if (this.generationLHCPlayId(PLAY_ID_LX_FOUR_WIN, lotteryId).equals(playId)) {
            playWins = 4;
        }

        if (size >= playWins) {
            // 中奖了
            if (openSx.contains(shengXiao)) {
                // 中了低赔率的
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
     * 获取六合彩中奖注数
     * (适用玩法:连肖不中)
     *
     * @param betNumber 投注号码
     * @param sgNumber  开奖号码
     * @param playId    玩法id
     * @param dateStr   开奖时间 yyyy-MM-dd
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
     * 获取六合彩中奖注数
     * (适用玩法:连尾中)
     *
     * @param betNumber 投注号码
     * @param sgNumber  开奖号码
     * @param playId    玩法id
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
            if (sgList.contains(Integer.valueOf(betNum.replace("尾", "")))) {
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
     * 获取六合彩中奖注数
     * (适用玩法:连尾不中)
     *
     * @param betNumber 投注号码
     * @param sgNumber  开奖号码
     * @param playId    玩法id
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
            if (!sgList.contains(Integer.valueOf(betNum.replace("尾", "")))) {
                noOpenNum.add(betNum);
            }
        }

        List<Integer> wins = new ArrayList<>(2); //默认第一个值是高赔率的中奖注数
        int size = noOpenNum.size();
        int win1 = 0; // 高赔率的中奖注数
        int win2 = 0; // 低赔率的中奖注数

        int playWins = 2;
        if (this.generationLHCPlayId(PLAY_ID_LW_TWO_NO_WIN, lotteryId).equals(playId)) {
            playWins = 2;
        } else if (this.generationLHCPlayId(PLAY_ID_LW_THREE_NO_WIN, lotteryId).equals(playId)) {
            playWins = 3;
        } else if (this.generationLHCPlayId(PLAY_ID_LW_FOUR_NO_WIN, lotteryId).equals(playId)) {
            playWins = 4;
        }

        if (size >= playWins) {
            // 中奖了
            if (noOpenNum.contains("0尾")) {
                // 中了高赔率的
                win1 = MathUtil.countCnm(size - 1, playWins);
                // 中了低赔率的
                win2 = MathUtil.countCnm(size - 1, playWins - 1);
            } else {
                // 中了高赔率的
                win1 = MathUtil.countCnm(size, playWins);
            }
        }

        wins.add(win1);
        wins.add(win2);

        return wins;

    }

    /**
     * 获取六合彩中奖注数
     * (适用玩法:1-6龙虎)
     *
     * @param betNumber 投注号码
     * @param sgNumber  开奖号码
     * @return
     */
    private int isWinByOneSixLh(String betNumber, String sgNumber) {
        // 校验参数
        if (StringUtils.isBlank(betNumber) || StringUtils.isBlank(sgNumber)) {
            return 0;
        }
        // 分析投注内容
        String[] bet = betNumber.split("@");
        String numStr = bet[1].substring(1, 4);
        String betType = bet[1].substring(0, 1);
        String[] num = numStr.split("-");
        String[] sgNum = sgNumber.split(",");
        int one = Integer.valueOf(num[0]);
        int two = Integer.valueOf(num[1]);
        String str;
        if (sgNum[one - 1].compareTo(sgNum[two - 1]) > 0) {
            str = "龙";
        } else {
            str = "虎";
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
     * 获取六合彩中奖注数
     * (适用玩法:五行)
     *
     * @param betNumber 投注号码
     * @param sgNumber  开奖号码
     * @param dateStr   开奖时间 yyyy-MM-dd
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
     * 1分、5分、时时、香港六合彩 玩法ID生成
     *
     * @param playNumber
     * @param
     * @return
     */
    private Integer generationLHCPlayId(String playNumber, Integer lotteryId) {
        return Integer.parseInt(lotteryId + playNumber);
    }

    /**
     * 1分、5分、时时、香港六合彩 玩法ID生成
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
//        // 缓存到开奖结果
//        String redisKey = RedisKeys.SSLHC_RESULT_VALUE;
//        Long redisTime = CaipiaoRedisTimeEnum.SSLHC.getRedisTime();
//        redisTemplate.opsForValue().set(redisKey, lhcLotterySg);
//        // 获取下期信息
//        SslhcLotterySg nextTjsscLotterySg = this.queryNextSslhcSg();
//        // 缓存到下期信息
//        String nextRedisKey = RedisKeys.SSLHC_NEXT_VALUE;
//        redisTemplate.opsForValue().set(nextRedisKey, nextTjsscLotterySg, redisTime, TimeUnit.MINUTES);
//        // 获取已经开奖数据
//        Integer openCount = this.getSslhcOpenCountNum();
//        String openRedisKey = RedisKeys.SSLHC_OPEN_VALUE;
//        redisTemplate.opsForValue().set(openRedisKey, openCount);
//        // 缓存近期开奖数据
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
     * @Description: 获取5分时时彩近期开奖数据
     * @author HANS
     * @date 2019年5月21日下午4:04:44
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
//        // 缓存到开奖结果
//        String redisKey = RedisKeys.FIVELHC_RESULT_VALUE;
//        Long redisTime = CaipiaoRedisTimeEnum.FIVELHC.getRedisTime();
//        redisTemplate.opsForValue().set(redisKey, fivelhcLotterySg);
//        // 获取下期信息
//        FivelhcLotterySg nextTjsscLotterySg = this.queryNextFivelhcSg();
//        // 缓存到下期信息
//        String nextRedisKey = RedisKeys.FIVELHC_NEXT_VALUE;
//        redisTemplate.opsForValue().set(nextRedisKey, nextTjsscLotterySg, redisTime, TimeUnit.MINUTES);
//        // 获取已经开奖数据
//        Integer openCount = this.getFivelhcOpenCountNum();
//        String openRedisKey = RedisKeys.FIVELHC_OPEN_VALUE;
//        redisTemplate.opsForValue().set(openRedisKey, openCount);
//        // 缓存近期开奖数据
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
     * 根据期号获取赛果
     *
     * @param issue 期号
     * @return
     */
//    private LhcLotterySg getLotterySg(String issue) {
//        // 获取该期赛果信息
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
     * 均分LIST中的数据
     */
    private <T> List<List<T>> averageAssign(List<T> source, int n) {
        List<List<T>> result = new ArrayList<List<T>>();
        int remaider = source.size() % n;  //(先计算出余数)
        int number = source.size() / n;  //然后是商
        int offset = 0;//偏移量
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
