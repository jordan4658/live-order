package com.caipiao.live.order.service.bet.impl;


import com.caipiao.live.common.service.read.OrderReadRestService;
import com.caipiao.live.order.service.bet.BetCommonService;
import com.caipiao.live.order.service.bet.BetNewLhcService;
import com.caipiao.live.order.service.order.OrderNewWriteService;
import com.caipiao.live.order.service.lottery.LotteryPlayOddsWriteService;
import com.caipiao.live.order.service.lottery.LotteryPlayWriteService;
import com.caipiao.live.order.service.lottery.LotteryWriteService;
import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.model.dto.order.OrderBetStatus;
import com.caipiao.live.common.mybatis.entity.*;
import com.caipiao.live.common.mybatis.mapper.OrderBetRecordMapper;
import com.caipiao.live.common.mybatis.mapper.OrderRecordMapper;
import com.caipiao.live.common.util.DateUtils;
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
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.caipiao.live.common.util.ViewUtil.getTradeOffAmount;

/**
 * @Date:Created in 21:112019/12/30
 * @Descriotion
 * @Author
 **/
@Service
public class BetNewLhcServiceImpl implements BetNewLhcService {
    private static final Logger logger = LoggerFactory.getLogger(BetNewLhcServiceImpl.class);
    @Autowired
    private LotteryPlayOddsWriteService lotteryPlayOddsService;
    @Autowired
    private LotteryPlayWriteService lotteryPlayWriteService;
    @Autowired
    private LotteryWriteService lotteryWriteService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private OrderBetRecordMapper orderBetRecordMapper;
    @Autowired
    private OrderReadRestService orderReadRestService;
    @Autowired
    private OrderNewWriteService orderNewWriteService;
    @Autowired
    private OrderRecordMapper orderRecordMapper;
    @Autowired
    private BetCommonService betCommonService;
    // 六合彩特码特码A玩法id
    private final String PLAY_ID_TM_TMA = "01";
    // 六合彩特码特码A两面玩法id
    private final String PLAY_ID_TM_TMA_LM = "02";
    // 六合彩正码正码A玩法id
    public final static String PLAY_ID_ZM_ZMA = "03";
    // 六合彩正码正码1-6玩法id集合
    private final String PLAY_IDS_ZM = "04";
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
    //private final List<Integer> PLAY_IDS_ZT_OTS_LM = Lists.newArrayList(338, 339, 331, 335, 336, 337);

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
    public void clearingLhcTeMaA(String issue, String number, int lotteryId, boolean jiesuanOrNot) throws Exception {
        // 特码特码A               // 特码特码A//六肖连中  六肖连不中//1-6龙虎// 正码正码A
        clearingLhcOnePlayOdds(issue, number, this.generationLHCPlayId(PLAY_ID_TM_TMA, lotteryId), lotteryId, jiesuanOrNot);
        // 特码特码A两面                 // 特码特码A两面   // 正码正码A两面  六合彩正码1-6 // 正(1-6)特 两面  六合彩半波  六合彩尾数  六合彩平特平特  六合彩特肖特肖
        clearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_TM_TMA_LM, lotteryId), lotteryId, jiesuanOrNot);
    }

    /**
     * 结算【六合彩正码】
     *
     * @param issue  期号
     * @param number 开奖号码
     */

    @Override
    public void clearingLhcZhengMaA(String issue, String number, int lotteryId, boolean jiesuanOrNot) throws Exception {
        // 正码正码A两面             // 特码特码A两面   // 正码正码A两面  六合彩正码1-6 // 正(1-6)特 两面  六合彩半波  六合彩尾数  六合彩平特平特  六合彩特肖特肖
        clearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_ZM_ZMA, lotteryId), lotteryId, jiesuanOrNot);
        // 正码正码A             // 特码特码A//六肖连中  六肖连不中//1-6龙虎// 正码正码A
        clearingLhcOnePlayOdds(issue, number, this.generationLHCPlayId(PLAY_ID_ZM_ZMA, lotteryId), lotteryId, jiesuanOrNot);
    }

    /**
     * 结算【六合彩正码1-6】
     *
     * @param issue 期号
     */
    @Override
    public void clearingLhcZhengMaOneToSix(String issue, String number, int lotteryId, boolean jiesuanOrNot) throws Exception {
        // 特码特码A两面   // 正码正码A两面  六合彩正码1-6 // 正(1-6)特 两面  六合彩半波  六合彩尾数  六合彩平特平特  六合彩特肖特肖
        clearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_IDS_ZM, lotteryId), lotteryId, jiesuanOrNot);
    }

    /**
     * 结算【六合彩正特】
     *
     * @param issue 期号
     */
    @Override
    public void clearingLhcZhengTe(String issue, String number, int lotteryId, boolean jiesuanOrNot) throws Exception {
        // 正(1-6)特 // 三全中,二全中,特串    六合彩不中  六合彩连肖中
        clearingOnePlayOneOdds(issue, number, this.generationLHCPlayIdList(PLAY_IDS_ZT_OTS, lotteryId), lotteryId, jiesuanOrNot);
        // 正(1-6)特 两面         // 特码特码A两面   // 正码正码A两面  六合彩正码1-6 // 正(1-6)特 两面  六合彩半波  六合彩尾数  六合彩平特平特  六合彩特肖特肖
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
    public void clearingLhcLianMa(String issue, String number, int lotteryId, boolean jiesuanOrNot) throws Exception {
        // 三全中,二全中,特串       // 正(1-6)特 // 三全中,二全中,特串    六合彩不中  六合彩连肖中
        clearingOnePlayOneOdds(issue, number, this.generationLHCPlayIdList(PLAY_IDS_LM_QZ, lotteryId), lotteryId, jiesuanOrNot);
        // 六合彩连码 三中二,二中特      六合彩连肖中
        clearingOnePlayTwoOdds(issue, number, this.generationLHCPlayIdList(PLAY_IDS_LM_EZ, lotteryId), lotteryId, jiesuanOrNot);
    }

    /**
     * 结算【六合彩半波】
     *
     * @param issue 期号
     */
    @Override
    public void clearingLhcBanBo(String issue, String number, int lotteryId, boolean jiesuanOrNot) throws Exception {
        // 特码特码A两面   // 正码正码A两面  六合彩正码1-6 // 正(1-6)特 两面  六合彩半波  六合彩尾数  六合彩平特平特  六合彩特肖特肖
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
    public void clearingLhcWs(String issue, String number, int lotteryId, boolean jiesuanOrNot) throws Exception {
        // 特码特码A两面   // 正码正码A两面  六合彩正码1-6 // 正(1-6)特 两面  六合彩半波  六合彩尾数  六合彩平特平特  六合彩特肖特肖
        clearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_WS_QW, lotteryId), lotteryId, jiesuanOrNot);
        clearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_WS_TW, lotteryId), lotteryId, jiesuanOrNot);
    }

    /**
     * 结算【六合彩不中】
     *
     * @param issue 期号
     */
    @Override
    public void clearingLhcNoOpen(String issue, String number, int lotteryId, boolean jiesuanOrNot) throws Exception {
        // 正(1-6)特 // 三全中,二全中,特串    六合彩不中  六合彩连肖中
        clearingOnePlayOneOdds(issue, number, this.generationLHCPlayIdList(PLAY_IDS_NO_OPEN, lotteryId), lotteryId, jiesuanOrNot);
    }

    /**
     * 结算【六合彩平特平特】
     *
     * @param issue 期号
     */
    @Override
    public void clearingLhcPtPt(String issue, String number, int lotteryId, boolean jiesuanOrNot) throws Exception {
        // 特码特码A两面   // 正码正码A两面  六合彩正码1-6 // 正(1-6)特 两面  六合彩半波  六合彩尾数  六合彩平特平特  六合彩特肖特肖
        clearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_PT_PT, lotteryId), lotteryId, jiesuanOrNot);
    }

    /**
     * 结算【六合彩特肖特肖】
     *
     * @param issue 期号
     */
    @Override
    public void clearingLhcTxTx(String issue, String number, int lotteryId, boolean jiesuanOrNot) throws Exception {
        // 特码特码A两面   // 正码正码A两面  六合彩正码1-6 // 正(1-6)特 两面  六合彩半波  六合彩尾数  六合彩平特平特  六合彩特肖特肖
        clearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_TX_TX, lotteryId), lotteryId, jiesuanOrNot);
    }

    /**
     * 结算【六合彩六肖】
     *
     * @param issue 期号
     */
    @Override
    public void clearingLhcLiuXiao(String issue, String number, int lotteryId, boolean jiesuanOrNot) throws Exception {
        // 特码特码A//六肖连中  六肖连不中//1-6龙虎// 正码正码A
        clearingLhcOnePlayOdds(issue, number, this.generationLHCPlayId(PLAY_ID_LX_LXLZ, lotteryId), lotteryId, jiesuanOrNot);
        clearingLhcOnePlayOdds(issue, number, this.generationLHCPlayId(PLAY_ID_LX_LXLBZ, lotteryId), lotteryId, jiesuanOrNot);
    }

    /**
     * 结算【六合彩连肖中】
     *
     * @param issue 期号
     */
    @Override
    public void clearingLhcLianXiao(String issue, String number, int lotteryId, boolean jiesuanOrNot) throws Exception {
        //六合彩连码 三中二,二中特      六合彩连肖中
        clearingOnePlayTwoOdds(issue, number, this.generationLHCPlayIdList(PLAY_IDS_LX_WIN, lotteryId), lotteryId, jiesuanOrNot);
        // 正(1-6)特 // 三全中,二全中,特串    六合彩不中  六合彩连肖不中
        clearingOnePlayOneOdds(issue, number, this.generationLHCPlayIdList(PLAY_IDS_LX_NO_WIN, lotteryId), lotteryId, jiesuanOrNot);
    }

    /**
     * 结算【六合彩连尾中】
     *
     * @param issue 期号
     */
    @Override
    public void clearingLhcLianWei(String issue, String number, int lotteryId, boolean jiesuanOrNot) throws Exception {
        // 正(1-6)特 // 三全中,二全中,特串    六合彩不中  六合彩连肖中
        clearingOnePlayOneOdds(issue, number, this.generationLHCPlayIdList(PLAY_IDS_LW_WIN, lotteryId), lotteryId, jiesuanOrNot);
        // 六合彩连码 三中二,二中特      六合彩连肖中
        clearingOnePlayTwoOdds(issue, number, this.generationLHCPlayIdList(PLAY_IDS_LW_NO_WIN, lotteryId), lotteryId, jiesuanOrNot);
    }

    /**
     * 结算【六合彩1-6龙虎】
     *
     * @param issue 期号
     */
    @Override
    public void clearingLhcOneSixLh(String issue, String number, int lotteryId, boolean jiesuanOrNot) throws Exception {
        // 特码特码A//六肖连中  六肖连不中//1-6龙虎// 正码正码A
        clearingLhcOnePlayOdds(issue, number, this.generationLHCPlayId(PLAY_ID_ONE_SIX_LH, lotteryId), lotteryId, jiesuanOrNot);
    }

    @Override
    public void clearingLhcWuxing(String issue, String number, int lotteryId, boolean jiesuanOrNot) throws Exception {
        // 特码特码A//六肖连中  六肖连不中//1-6龙虎// 正码正码A
        clearingLhcOnePlayOdds(issue, number, this.generationLHCPlayId(PLAY_ID_WUXING, lotteryId), lotteryId, jiesuanOrNot);
    }

    /**
     * 一种玩法只有一种赔率,而且投注号码可中一注或多注
     *
     * @param issue   期号
     * @param number  开奖号码
     * @param playIds 玩法id集合
     */
    @SuppressWarnings("rawtypes")  // 正(1-6)特 // 三全中,二全中,特串    六合彩不中  六合彩连肖不中                1
    private void clearingOnePlayOneOdds(String issue, String number, List<Integer> playIds, int lotteryId, boolean jiesuanOrNot) {
        // 获取该期赛果信息
        String date = DateUtils.formatDate(new Date(), "yyyy-MM-dd");
        // 获取相应的订单信息
        int ordercount = orderNewWriteService.countOrderBetList(issue, playIds, String.valueOf(lotteryId), OrderBetStatus.WAIT);
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
        List<OrderBetRecord> orderBetRecords = orderNewWriteService.selectOrderBetList(issue, String.valueOf(lotteryId), playIds, OrderBetStatus.WAIT, "OnePlayOne");
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
                String playName = orderBet.getPlayName();
                boolean playType = playName.indexOf("不中") >= 0;
                LotteryPlayOdds odds;
                Map oddsMap;
                if (playType) {
                    oddsMap = lotteryPlayOddsService.selectPlayOddsBySettingIds(settingIds);
                    odds = (LotteryPlayOdds) oddsMap.get(settingIds.get(0));
                } else {
                    oddsMap = lotteryPlayOddsService.selectPlayOddsBySettingId(settingIds.get(0));
//                    odds = (LotteryPlayOdds) oddsMap.get(betNumber.split("@")[1]);
                    odds = (LotteryPlayOdds) oddsMap.get(betNumber);
                    if (odds == null) {
                        //增加playname字段
//                       odds = (LotteryPlayOdds) oddsMap.get(betNumber.split("@")[0]);
                        odds = (LotteryPlayOdds) oddsMap.get(playName);
                    }
                    if (odds == null) {
                        // 正特1 正特2...
//                        odds = (LotteryPlayOdds) oddsMap.get(betNumber.split("@")[1].split(",")[0]);
                        odds = (LotteryPlayOdds) oddsMap.get(betNumber.split(",")[0]);
                    }
                }
                BigDecimal winAmount = BigDecimal.ZERO;
                try {
                    //增加playname字段
                    StringBuilder betNumberPlayName = new StringBuilder();
                    betNumberPlayName.append(orderBet.getPlayName()).append("@").append(orderBet.getBetNumber());
                    String betNumberLin = betNumberPlayName.toString().replace("一", "1").replace("二", "2")
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
                    // 判断是否中奖,获取中奖注数  取公共   //增加了playname字段
                    StringBuilder betNumberAppendPlayName = new StringBuilder();
                    betNumberAppendPlayName.append(orderBet.getPlayName()).append("@").append(orderBet.getBetNumber());
                    int winCounts = LhcUtils.isWinByOnePlayOneOdds(betNumberAppendPlayName.toString(), number, orderBet.getPlayId(), date, lotteryId);
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
                        logger.info("clearingOnePlayOneOdds 开奖判断信息{};{};{};{};{};{};{}", orderBet.getOrderSn(), orderBet.getPlayName(), orderBet.getBetNumber(), number, orderBet.getPlayId(), lotteryId, winAmount);
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
                        String message = orderBet.getIssue() + ";" + orderBet.getOrderSn() + ";" + lotteryName + ";" + lotteryPlayName + ";" + orderBet.getPlayName()
                                + "@" + orderBet.getBetNumber() + ";" + orderBet.getBetAmount() + ";" + winAmount + ";" + tbStatus;

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
                    logger.error("订单结算出错，lotteryId:{},issue:{},betNum:{},playName:{}", orderBet.getLotteryId(), issue, orderBet.getBetNumber(), orderBet.getPlayName(), e);
                    break;
                }

            }

            if (!jiesuanOrNot) {
                break;
            }
            orderBetRecords = orderNewWriteService.selectOrderBetList(issue, String.valueOf(lotteryId), playIds, OrderBetStatus.WAIT, "OnePlayOne");
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
     *///六合彩连码 三中二,二中特      六合彩连肖中                          2
    private void clearingOnePlayTwoOdds(String issue, String number, List<Integer> playIds, int lotteryId, boolean jiesuanOrNot) {
        String date = DateUtils.formatDate(new Date(), "yyyy-MM-dd");
//        }

        // 获取相应的订单信息
        int ordercount = orderNewWriteService.countOrderBetList(issue, playIds, String.valueOf(lotteryId), OrderBetStatus.WAIT);
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
        List<OrderBetRecord> orderBetRecords = orderNewWriteService.selectOrderBetList(issue, String.valueOf(lotteryId), playIds, OrderBetStatus.WAIT, "OnePlayTwo");
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
                    // 判断是否中奖,获取中奖注数、
                    StringBuilder betNumberAppendPlayName = new StringBuilder();
                    betNumberAppendPlayName.append(orderBet.getPlayName()).append("@").append(orderBet.getBetNumber());
                    List<Integer> twoOdds = LhcUtils.isWinByOnePlayTwoOdds(betNumberAppendPlayName.toString(), number, orderBet.getPlayId(), date, lotteryId);
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
                        logger.info("clearingOnePlayTwoOdds 开奖判断信息{};{};{};{};{};{};{};{};{}", orderBet.getOrderSn(), orderBet.getPlayName(), orderBet.getBetNumber(), number, orderBet.getPlayId(), lotteryId, twoOdds.get(0), twoOdds.get(1), winAmount);
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
                        String message = orderBet.getIssue() + ";" + orderBet.getOrderSn() + ";" + lotteryName + ";" + lotteryPlayName + ";" + orderBet.getPlayName()
                                + "@" + orderBet.getBetNumber() + ";" + orderBet.getBetAmount() + ";" + winAmount + ";" + tbStatus;

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
            orderBetRecords = orderNewWriteService.selectOrderBetList(issue, String.valueOf(lotteryId), playIds, OrderBetStatus.WAIT, "OnePlayTwo");
        }
    }

    /**
     * 一种玩法只有一种赔率,投注号码可中一注或多注
     *
     * @param issue        期号
     * @param number       开奖号码
     * @param playId       玩法id       // 特码特码A//六肖连中  六肖连不中//1-6龙虎
     * @param jiesuanOrNot 是否真实结算
     *                     // 特码特码A//六肖连中  六肖连不中//1-6龙虎                            3
     */
    private void clearingLhcOnePlayOdds(String issue, String number, Integer playId, int lotteryId, boolean jiesuanOrNot) {
        long begin = System.currentTimeMillis();
        logger.debug("结算开始......");
//        String date = DateUtils.formatDate(new Date(), "yyyy-MM-dd");
        String date = DateUtils.getLocalDateToYyyyMmDd(LocalDate.now());
        if (jiesuanOrNot) {
            int upcount = orderNewWriteService.updateOrderRecord(String.valueOf(lotteryId), issue, number);
            while (upcount > 0) {
                upcount = orderNewWriteService.updateOrderRecord(String.valueOf(lotteryId), issue, number);
            }
        }

        logger.debug("updateOrderRecord time, {}", System.currentTimeMillis() - begin);
        // 获取相应的订单信息
        List playlist = new ArrayList();
        playlist.add(playId);
        int ordercount = orderNewWriteService.countOrderBetList(issue, playlist, String.valueOf(lotteryId), OrderBetStatus.WAIT);
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
        List<OrderBetRecord> orderBetRecords = orderNewWriteService.selectOrderBetList(issue, String.valueOf(lotteryId), playlist, OrderBetStatus.WAIT, "OnePlay");
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
                        orderBetRecords = orderNewWriteService.selectOrderBetList(issue, String.valueOf(lotteryId), playlist, OrderBetStatus.WAIT, "OnePlay");
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
                StringBuilder betNumberPlayName = new StringBuilder();
                betNumberPlayName.append(orderBetRecords.get(0).getPlayName()).append("@").append(orderBetRecords.get(0).getBetNumber());
                boolean flag = betNumberPlayName.indexOf(shengXiao) >= 0;
                if (flag) {
                    odds = lotteryPlayOddsService.selectPlayOddsBySettingId(settingId).get(shengXiao);
                }
                for (OrderBetRecord orderBet : orderBetRecords) {
                    //如果是  特码  玩法
                    if (orderBet.getPlayId() != null && orderBet.getPlayId().equals(120501)) {
                        Map<String, LotteryPlayOdds> oddsMap = lotteryPlayOddsService.selectPlayOddsBySettingId(orderBet.getSettingId());
                        if (oddsMap.size() == 49) {
                            odds = oddsMap.get(String.valueOf(number.split(",")[6]));
                        }
                    }
                    order = orderBet;
                    //不结算两面
                    if (orderBet.getPlayName().indexOf("两面") >= 0) {
                        continue;
                    }
                    if (orderBet.getPlayName().contains("五行")) {
                        List<LotteryPlayOdds> oddList = lotteryPlayOddsService.selectOddsListBySettingId(settingId);
                        Map<String, LotteryPlayOdds> oddsMap = new HashMap<>();
                        for (LotteryPlayOdds lotteryPlayodds : oddList) {
                            oddsMap.put(lotteryPlayodds.getName(), lotteryPlayodds);
                        }
//                        String betName = orderBet.getBetNumber().replace("五行@", "");
                        String betName = orderBet.getBetNumber();
                        odds = oddsMap.get(betName);
                    }
                    BigDecimal winAmount = BigDecimal.ZERO;
                    orderBet.setWinCount("0");
                    // 判断是否中奖,获取中奖注数
                    StringBuilder betNumberAppendPlayName = new StringBuilder();
                    betNumberAppendPlayName.append(orderBet.getPlayName()).append("@").append(orderBet.getBetNumber());
                    int winCounts = LhcUtils.isWinByOnePlayOneOdds(betNumberAppendPlayName.toString(), number, orderBet.getPlayId(), date, lotteryId);
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
                            logger.info("clearingLhcOnePlayOdds 开奖判断信息{};{};{};{};{};{};{};{}", orderBet.getOrderSn(), orderBet.getPlayName(), orderBet.getBetNumber(), number, orderBet.getPlayId(), lotteryId, winCounts, winAmount);
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
                            String message = orderBet.getIssue() + ";" + orderBet.getOrderSn() + ";" + lotteryName + ";" + lotteryPlayName + ";" + orderBet.getPlayName() + "@" + orderBet.getBetNumber()
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
                orderBetRecords = orderNewWriteService.selectOrderBetList(issue, String.valueOf(lotteryId), playlist, OrderBetStatus.WAIT, "OnePlay");

            } catch (Exception e) {
                logger.error("订单结算出错，lotteryId:{},issue:{},betNum:{},{},playName:{}", order.getLotteryId(), issue, order.getPlayName(), order.getBetNumber(), e);
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
    // 特码特码A两面   // 正码正码A两面  六合彩正码1-6 // 正(1-6)特 两面  六合彩半波  六合彩尾数  六合彩平特平特  六合彩特肖特肖        4
    private void clearingOnePlayManyOdds(String issue, String number, Integer playId, int lotteryId, boolean jiesuanOrNot) {
        String date = DateUtils.formatDate(new Date(), "yyyy-MM-dd");
        // 获取相应的订单信息
        List playlist = new ArrayList();
        playlist.add(playId);
        int ordercount = orderNewWriteService.countOrderBetList(issue, playlist, String.valueOf(lotteryId), OrderBetStatus.WAIT);
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

        List<OrderBetRecord> orderBetRecords = orderNewWriteService.selectOrderBetList(issue, String.valueOf(lotteryId), playlist, OrderBetStatus.WAIT, "OnePlayMany");
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
                String playName = orderBetRecords.get(0).getPlayName();
                // 玩法设计...共用了一个playID....
                if (this.generationLHCPlayId(PLAY_ID_TM_TMA_LM, lotteryId).equals(playId) && playName.indexOf("特码两面") < 0
                        || this.generationLHCPlayId(PLAY_ID_TM_TMA_LM, lotteryId).equals(playId) && playName.indexOf("正码两面") > 0
                        || this.generationLHCPlayId(PLAY_ID_ZM_ZMA, lotteryId).equals(playId) && playName.indexOf("两面") < 0) {
                    return;
                }
                // 波色投注号码不存在打和的情况
                if (playName.indexOf("波色") < 0
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
                        for (OrderBetRecord orderBetRecordDTO : orderBetRecords) {
                            if (orderBetRecordDTO.getPlayName().contains("正码一") || playId.equals(this.generationLHCPlayId(PLAY_ID_ZT_ONE, lotteryId))) {
                                heList.add(orderBetRecordDTO);
                            } else {
                                noheList.add(orderBetRecordDTO);
                            }
                        }
                        noheList.addAll(noWinOrLose(heList));
                        orderBetRecords = noheList;
                    } else if ("49".equals(sgArr[1]) && (playId.equals(this.generationLHCPlayId(PLAY_IDS_ZM, lotteryId)) || playId.equals(this.generationLHCPlayId(PLAY_ID_ZT_TWO, lotteryId)))) {
                        List<OrderBetRecord> heList = new ArrayList<>();
                        List<OrderBetRecord> noheList = new ArrayList<>();
                        for (OrderBetRecord orderBetRecordDTO : orderBetRecords) {
                            if (orderBetRecordDTO.getPlayName().contains("正码二") || playId.equals(this.generationLHCPlayId(PLAY_ID_ZT_TWO, lotteryId))) {
                                heList.add(orderBetRecordDTO);
                            } else {
                                noheList.add(orderBetRecordDTO);
                            }
                        }
                        noheList.addAll(noWinOrLose(heList));
                        orderBetRecords = noheList;
                    } else if ("49".equals(sgArr[2]) && (playId.equals(this.generationLHCPlayId(PLAY_IDS_ZM, lotteryId)) || playId.equals(this.generationLHCPlayId(PLAY_ID_ZT_THREE, lotteryId)))) {
                        List<OrderBetRecord> heList = new ArrayList<>();
                        List<OrderBetRecord> noheList = new ArrayList<>();
                        for (OrderBetRecord orderBetRecordDTO : orderBetRecords) {
                            if (orderBetRecordDTO.getPlayName().contains("正码三") || playId.equals(this.generationLHCPlayId(PLAY_ID_ZT_THREE, lotteryId))) {
                                heList.add(orderBetRecordDTO);
                            } else {
                                noheList.add(orderBetRecordDTO);
                            }
                        }
                        noheList.addAll(noWinOrLose(heList));
                        orderBetRecords = noheList;
                    } else if ("49".equals(sgArr[3]) && (playId.equals(this.generationLHCPlayId(PLAY_IDS_ZM, lotteryId)) || playId.equals(this.generationLHCPlayId(PLAY_ID_ZT_FOUR, lotteryId)))) {
                        List<OrderBetRecord> heList = new ArrayList<>();
                        List<OrderBetRecord> noheList = new ArrayList<>();
                        for (OrderBetRecord orderBetRecordDTO : orderBetRecords) {
                            if (orderBetRecordDTO.getPlayName().contains("正码四") || playId.equals(this.generationLHCPlayId(PLAY_ID_ZT_FOUR, lotteryId))) {
                                heList.add(orderBetRecordDTO);
                            } else {
                                noheList.add(orderBetRecordDTO);
                            }
                        }
                        noheList.addAll(noWinOrLose(heList));
                        orderBetRecords = noheList;
                    } else if ("49".equals(sgArr[4]) && (playId.equals(this.generationLHCPlayId(PLAY_IDS_ZM, lotteryId)) || playId.equals(this.generationLHCPlayId(PLAY_ID_ZT_FIVE, lotteryId)))) {
                        List<OrderBetRecord> heList = new ArrayList<>();
                        List<OrderBetRecord> noheList = new ArrayList<>();
                        for (OrderBetRecord orderBetRecordDTO : orderBetRecords) {
                            if (orderBetRecordDTO.getPlayName().contains("正码五") || playId.equals(this.generationLHCPlayId(PLAY_ID_ZT_FIVE, lotteryId))) {
                                heList.add(orderBetRecordDTO);
                            } else {
                                noheList.add(orderBetRecordDTO);
                            }
                        }
                        noheList.addAll(noWinOrLose(heList));
                        orderBetRecords = noheList;
                    } else if ("49".equals(sgArr[5]) && (playId.equals(this.generationLHCPlayId(PLAY_IDS_ZM, lotteryId)) || playId.equals(this.generationLHCPlayId(PLAY_ID_ZT_SIX, lotteryId)))) {
                        List<OrderBetRecord> heList = new ArrayList<>();
                        List<OrderBetRecord> noheList = new ArrayList<>();
                        for (OrderBetRecord orderBetRecordDTO : orderBetRecords) {
                            if (orderBetRecordDTO.getPlayName().contains("正码六") || playId.equals(this.generationLHCPlayId(PLAY_ID_ZT_SIX, lotteryId))) {
                                heList.add(orderBetRecordDTO);
                            } else {
                                noheList.add(orderBetRecordDTO);
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
                    StringBuilder betNumberAppendPlayName = new StringBuilder();
                    betNumberAppendPlayName.append(orderBet.getPlayName()).append("@").append(orderBet.getBetNumber());
                    String winNum = LhcUtils.isWinByOnePlayManyOdds(betNumberAppendPlayName.toString(), number, orderBet.getPlayId(), date, lotteryId);
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
                            String message = orderBet.getIssue() + ";" + orderBet.getOrderSn() + ";" + lotteryName + ";" + lotteryPlayName + ";" + orderBet.getPlayName() + "@" + orderBet.getBetNumber() + ";" + orderBet.getBetAmount() + ";" + winAmount + ";" + tbStatus;

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
                orderBetRecords = orderNewWriteService.selectOrderBetList(issue, String.valueOf(lotteryId), playlist, OrderBetStatus.WAIT, "OnePlayMany");
            } catch (Exception e) {
                logger.error("订单结算出错，lotteryId:{},issue:{},betNum:{},playName{}", order.getLotteryId(), issue, order.getBetNumber(), order.getPlayName(), e);
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
     * 两面玩法开49打和的处理方式   （只针对一注的情况）
     *
     * @param orderBetRecords
     */
    private List<OrderBetRecord> noWinOrLose(List<OrderBetRecord> orderBetRecords) {
        List<OrderBetRecord> newOrderBetRecords = new ArrayList<>();
        for (OrderBetRecord orderBet : orderBetRecords) {
            // String[] betNumbetArr = orderBet.getBetNumber().split("@"); 增加了playname字段
            String betNumber = orderBet.getBetNumber();
            if (MAYBE_HE.contains(betNumber)) {
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


}
