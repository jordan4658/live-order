package com.caipiao.live.order.service.bet.impl;

import com.caipiao.live.common.service.read.OrderReadRestService;
import com.caipiao.live.order.service.bet.BetCommonService;
import com.caipiao.live.order.service.bet.BetSscAzService;
import com.caipiao.live.order.service.lottery.LotteryPlayOddsWriteService;
import com.caipiao.live.order.service.lottery.LotteryPlayWriteService;
import com.caipiao.live.order.service.order.OrderAppendWriteService;
import com.caipiao.live.common.constant.LotteryResultStatus;
import com.caipiao.live.common.constant.RedisKeys;
import com.caipiao.live.common.enums.lottery.CaipiaoRedisTimeEnum;
import com.caipiao.live.common.model.dto.order.OrderBetStatus;
import com.caipiao.live.common.model.dto.order.OrderStatus;
import com.caipiao.live.common.mybatis.entity.*;
import com.caipiao.live.common.mybatis.mapper.AussscLotterySgMapper;
import com.caipiao.live.common.mybatis.mapper.OrderRecordMapper;
import com.caipiao.live.common.mybatis.mapperext.sg.AussscLotterySgMapperExt;
import com.caipiao.live.common.util.DateUtils;
import com.caipiao.live.common.util.TimeHelper;
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

@Service
public class BetSscAzServiceImpl implements BetSscAzService {
    private static final Logger logger = LoggerFactory.getLogger(BetSscAzServiceImpl.class);
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
    private AussscLotterySgMapper aussscLotterySgMapper;
    @Autowired
    private AussscLotterySgMapperExt aussscLotterySgMapperExt;
    @Autowired
    private BetCommonService betCommonService;
    @Autowired
    private OrderReadRestService orderReadRestService;

    private final List<String> PLAY_IDS = Lists.newArrayList("01", "02", "03", "04", "05", "06", "07", "08", "09");

    @Override
    public void countAzSsc(String issue, String number, int lotteryId) throws Exception {
        // ???????????????????????????
        List<OrderRecord> orderRecords = orderReadRestService.selectOrders(lotteryId, issue, OrderStatus.NORMAL);
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
        List<Integer> playIds = this.generationSSCPlayIdList(PLAY_IDS, lotteryId);
        List<OrderBetRecord> orderBetRecords = orderReadRestService.selectOrderBets(orderIds, playIds, OrderBetStatus.WAIT);
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
            try {
                BigDecimal winAmount = new BigDecimal(0);
                // ??????????????????
                LotteryPlayOdds odds = oddsMap.get(orderBet.getSettingId());
                if (orderBet.getPlayId() == 220201 || orderBet.getPlayId() == 220202 || orderBet.getPlayId() == 220204 || orderBet.getPlayId() == 220205
                        || orderBet.getPlayId() == 220206 || orderBet.getPlayId() == 220207 || orderBet.getPlayId() == 220208 || orderBet.getPlayId() == 220209) {
                    //220201:?????? //220202:?????????220204???????????? 220205????????????  220206????????????  220207???????????? 220208????????????  220209????????????
                    Map<String, LotteryPlayOdds> oddsHeMap = lotteryPlayOddsService.selectPlayOddsBySettingId(orderBet.getSettingId());
                    odds = oddsHeMap.get(orderBet.getBetNumber().split("@")[1]);  //??????
                }
                orderBet.setWinCount("0");
                // ??????????????????
                String winNum = this.isWin(orderBet.getBetNumber(), number, orderBet.getPlayId());
                if (StringUtils.isNotBlank(winNum)) {
                    // ???????????????/????????????
                    String winCount = odds.getWinCount();
                    String totalCount = odds.getTotalCount();
                    // ????????????
                    double odd = Double.parseDouble(totalCount) * 1.0 / Double.parseDouble(winCount) * divisor;
                    //??????????????????
                    winAmount = orderBet.getBetAmount().divide(BigDecimal.valueOf(orderBet.getBetCount())).multiply(BigDecimal.valueOf(odd));
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
                logger.error("???????????????issue" + "," + issue, e);
            }

        }
    }


    /**
     * ?????????????????????????????????,????????????????????????,???????????????null
     *
     * @param betNum
     * @param sg
     * @return
     */
    private String isWin(String betNum, String sg, Integer playId) {
        //??????
        if (winOrNot(betNum, sg, playId)) {
            return betNum;
        }

        return null;
    }

    //????????????
    //????????????????????????
    private boolean winOrNot(String betNum, String sg, Integer playId) {
        String[] sgArray = sg.split(",");
        Integer[] sgIntArray = new Integer[5];
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
       ??????????????? ???????????? ????????????????????????????????????
       num:  ????????????
       sgArray:   ??????????????????
     */
    private boolean winByBigSmall(String betNum, String[] sgArray, Integer playId) {
        String num = betNum.split("@")[1];
        int sum = Integer.valueOf(sgArray[0]) + Integer.valueOf(sgArray[1]) + Integer.valueOf(sgArray[2])
                + Integer.valueOf(sgArray[3]) + Integer.valueOf(sgArray[4]);
        int first = Integer.valueOf(sgArray[0]);
        int second = Integer.valueOf(sgArray[1]);
        int third = Integer.valueOf(sgArray[2]);
        int four = Integer.valueOf(sgArray[3]);
        int five = Integer.valueOf(sgArray[4]);

        if (betNum.contains("???????????????")) {
            if (num.contains("?????????")) {
                if (sum >= 23) {
                    return true;
                }
            } else if (num.contains("?????????")) {
                if (sum <= 22) {
                    return true;
                }
            } else if (num.contains("?????????")) {
                if (sum % 2 != 0) {
                    return true;
                }
            } else if (num.contains("?????????")) {
                if (sum % 2 == 0) {
                    return true;
                }
            } else if (num.contains("???")) {
                if (first > five) {
                    return true;
                }
            } else if (num.contains("???")) {
                if (first < five) {
                    return true;
                }
            } else if (num.contains("???")) {
                if (first == five) {
                    return true;
                }
            }
        } else if (playId == 220205 || betNum.contains("?????????") || betNum.contains("???1???")) {
            if (num.contains("???")) {
                if (first % 2 != 0) {
                    return true;
                }
            } else if (num.contains("???")) {
                if (first % 2 == 0) {
                    return true;
                }
            } else if (num.contains("???")) {
                if (first >= 5) {
                    return true;
                }
            } else if (num.contains("???")) {
                if (first <= 4) {
                    return true;
                }
            } else {  //???1?????????????????????0-9
                if (num.equals(String.valueOf(first))) {
                    return true;
                }
            }
        } else if (playId == 220206 || betNum.contains("?????????") || betNum.contains("???2???")) {
            if (num.contains("???")) {
                if (second % 2 != 0) {
                    return true;
                }
            } else if (num.contains("???")) {
                if (second % 2 == 0) {
                    return true;
                }
            } else if (num.contains("???")) {
                if (second >= 5) {
                    return true;
                }
            } else if (num.contains("???")) {
                if (second <= 4) {
                    return true;
                }
            } else {  //???2?????????????????????0-9
                if (num.equals(String.valueOf(second))) {
                    return true;
                }
            }
        } else if (playId == 220207 || betNum.contains("?????????") || betNum.contains("???3???")) {
            if (num.contains("???")) {
                if (third % 2 != 0) {
                    return true;
                }
            } else if (num.contains("???")) {
                if (third % 2 == 0) {
                    return true;
                }
            } else if (num.contains("???")) {
                if (third >= 5) {
                    return true;
                }
            } else if (num.contains("???")) {
                if (third <= 4) {
                    return true;
                }
            } else {  //???3?????????????????????0-9
                if (num.equals(String.valueOf(third))) {
                    return true;
                }
            }
        } else if (playId == 220208 || betNum.contains("?????????") || betNum.contains("???4???")) {
            if (num.contains("???")) {
                if (four % 2 != 0) {
                    return true;
                }
            } else if (num.contains("???")) {
                if (four % 2 == 0) {
                    return true;
                }
            } else if (num.contains("???")) {
                if (four >= 5) {
                    return true;
                }
            } else if (num.contains("???")) {
                if (four <= 4) {
                    return true;
                }
            } else {  //???4?????????????????????0-9
                if (num.equals(String.valueOf(four))) {
                    return true;
                }
            }
        } else if (playId == 220209 || betNum.contains("?????????") || betNum.contains("???1???")) {
            if (num.contains("???")) {
                if (five % 2 != 0) {
                    return true;
                }
            } else if (num.contains("???")) {
                if (five % 2 == 0) {
                    return true;
                }
            } else if (num.contains("???")) {
                if (five >= 5) {
                    return true;
                }
            } else if (num.contains("???")) {
                if (five <= 4) {
                    return true;
                }
            } else {  //???5?????????????????????0-9
                if (num.equals(String.valueOf(five))) {
                    return true;
                }
            }
        } else if (betNum.contains("??????")) {
            String[] qianThree = {sgArray[0], sgArray[1], sgArray[2]};
            qianThree = getSortArray(qianThree);
            if (num.contains("??????")) {
                if (qianThree[0].equals(qianThree[1]) && qianThree[0].equals(qianThree[2])) {
                    return true;
                }
            } else if (num.contains("??????")) { //012,123,234,... 789,890,901 ???3?????????
                if (((Integer.valueOf(qianThree[0]) == Integer.valueOf(qianThree[1]) - 1) &&
                        (Integer.valueOf(qianThree[1]) == Integer.valueOf(qianThree[2]) - 1)) ||
                        ("0".equals(qianThree[0]) && "8".equals(qianThree[1]) && "9".equals(qianThree[2])) ||
                        ("0".equals(qianThree[0]) && "1".equals(qianThree[1]) && "9".equals(qianThree[2]))
                ) {
                    return true;
                }
            } else if (num.contains("??????")) {
                if (qianThree[0].equals(qianThree[1]) || qianThree[1].equals(qianThree[2])) {
                    if (qianThree[0].equals(qianThree[1]) && qianThree[1].equals(qianThree[2])) {
                        return false;
                    }
                    return true;
                }
            } else if (num.contains("??????")) { // 014, ???????????????????????????1????????????????????????????????????1???????????????0,9???????????????
                if ((Integer.valueOf(qianThree[0]) == Integer.valueOf(qianThree[1]) - 1) ||
                        (Integer.valueOf(qianThree[1]) == Integer.valueOf(qianThree[2]) - 1) ||
                        (("0".equals(qianThree[0]) && "9".equals(qianThree[1])) || ("0".equals(qianThree[0]) && "9".equals(qianThree[2])))) {
                    if (("0".equals(qianThree[0]) && "1".equals(qianThree[1]) && "9".equals(qianThree[2]))
                            || (qianThree[0].equals(qianThree[1]) || qianThree[1].equals(qianThree[2]))
                            || ((Integer.valueOf(qianThree[0]) == Integer.valueOf(qianThree[1]) - 1) && (Integer.valueOf(qianThree[1]) == Integer.valueOf(qianThree[2]) - 1))) { //?????? ????????? ???0,1,9?????????
                        return false;
                    }
                    return true;
                }
            } else if (num.contains("??????")) {
                boolean baozi = qianThree[0].equals(qianThree[1]) && qianThree[0].equals(qianThree[2]);
                boolean shunzi = ((Integer.valueOf(qianThree[0]) == Integer.valueOf(qianThree[1]) - 1) &&
                        (Integer.valueOf(qianThree[1]) == Integer.valueOf(qianThree[2]) - 1)) ||
                        ("0".equals(qianThree[0]) && "8".equals(qianThree[1]) && "9".equals(qianThree[2])) ||
                        ("0".equals(qianThree[0]) && "1".equals(qianThree[1]) && "9".equals(qianThree[2]));
                boolean duizi = qianThree[0].equals(qianThree[1]) || qianThree[1].equals(qianThree[2]);
                boolean banshun = (Integer.valueOf(qianThree[0]) == Integer.valueOf(qianThree[1]) - 1) ||
                        (Integer.valueOf(qianThree[1]) == Integer.valueOf(qianThree[2]) - 1) ||
                        (("0".equals(qianThree[0]) && "9".equals(qianThree[1])) || ("0".equals(qianThree[0]) && "9".equals(qianThree[2])));
                if (!baozi && !shunzi && !duizi && !banshun) {
                    return true;
                }
            }
        } else if (betNum.contains("??????")) {
            String[] zhongThree = {sgArray[1], sgArray[2], sgArray[3]};
            zhongThree = getSortArray(zhongThree);
            if (num.contains("??????")) {
                if (zhongThree[0].equals(zhongThree[1]) && zhongThree[0].equals(zhongThree[2])) {
                    return true;
                }
            } else if (num.contains("??????")) { //012,123,234,... 789,890,901 ???3?????????
                if (((Integer.valueOf(zhongThree[0]) == Integer.valueOf(zhongThree[1]) - 1) &&
                        (Integer.valueOf(zhongThree[1]) == Integer.valueOf(zhongThree[2]) - 1)) ||
                        ("0".equals(zhongThree[0]) && "8".equals(zhongThree[1]) && "9".equals(zhongThree[2])) ||
                        ("0".equals(zhongThree[0]) && "1".equals(zhongThree[1]) && "9".equals(zhongThree[2]))
                ) {
                    return true;
                }
            } else if (num.contains("??????")) {
                if (zhongThree[0].equals(zhongThree[1]) || zhongThree[1].equals(zhongThree[2])) {
                    if (zhongThree[0].equals(zhongThree[1]) && zhongThree[1].equals(zhongThree[2])) {
                        return false;
                    }
                    return true;
                }
            } else if (num.contains("??????")) { // 014, ???????????????????????????1????????????????????????????????????1???????????????0,9???????????????
                if ((Integer.valueOf(zhongThree[0]) == Integer.valueOf(zhongThree[1]) - 1) ||
                        (Integer.valueOf(zhongThree[1]) == Integer.valueOf(zhongThree[2]) - 1) ||
                        (("0".equals(zhongThree[0]) && "9".equals(zhongThree[1])) || ("0".equals(zhongThree[0]) && "9".equals(zhongThree[2])))) {
                    if (("0".equals(zhongThree[0]) && "1".equals(zhongThree[1]) && "9".equals(zhongThree[2]))
                            || (zhongThree[0].equals(zhongThree[1]) || zhongThree[1].equals(zhongThree[2]))
                            || ((Integer.valueOf(zhongThree[0]) == Integer.valueOf(zhongThree[1]) - 1) && (Integer.valueOf(zhongThree[1]) == Integer.valueOf(zhongThree[2]) - 1))) { //?????? ??????????????? ???0,1,9?????????
                        return false;
                    }
                    return true;
                }
            } else if (num.contains("??????")) {
                boolean baozi = zhongThree[0].equals(zhongThree[1]) && zhongThree[0].equals(zhongThree[2]);
                boolean shunzi = ((Integer.valueOf(zhongThree[0]) == Integer.valueOf(zhongThree[1]) - 1) &&
                        (Integer.valueOf(zhongThree[1]) == Integer.valueOf(zhongThree[2]) - 1)) ||
                        ("0".equals(zhongThree[0]) && "8".equals(zhongThree[1]) && "9".equals(zhongThree[2])) ||
                        ("0".equals(zhongThree[0]) && "1".equals(zhongThree[1]) && "9".equals(zhongThree[2]));
                boolean duizi = zhongThree[0].equals(zhongThree[1]) || zhongThree[1].equals(zhongThree[2]);
                boolean banshun = (Integer.valueOf(zhongThree[0]) == Integer.valueOf(zhongThree[1]) - 1) ||
                        (Integer.valueOf(zhongThree[1]) == Integer.valueOf(zhongThree[2]) - 1) ||
                        (("0".equals(zhongThree[0]) && "9".equals(zhongThree[1])) || ("0".equals(zhongThree[0]) && "9".equals(zhongThree[2])));
                if (!baozi && !shunzi && !duizi && !banshun) {
                    return true;
                }
            }
        } else if (betNum.contains("??????")) {
            String[] houThree = {sgArray[2], sgArray[3], sgArray[4]};
            houThree = getSortArray(houThree);
            if (num.contains("??????")) {
                if (houThree[0].equals(houThree[1]) && houThree[0].equals(houThree[2])) {
                    return true;
                }
            } else if (num.contains("??????")) { //012,123,234,... 789,890,901 ???3?????????
                if (((Integer.valueOf(houThree[0]) == Integer.valueOf(houThree[1]) - 1) &&
                        (Integer.valueOf(houThree[1]) == Integer.valueOf(houThree[2]) - 1)) ||
                        ("0".equals(houThree[0]) && "8".equals(houThree[1]) && "9".equals(houThree[2])) ||
                        ("0".equals(houThree[0]) && "1".equals(houThree[1]) && "9".equals(houThree[2]))
                ) {
                    return true;
                }
            } else if (num.contains("??????")) {
                if (houThree[0].equals(houThree[1]) || houThree[1].equals(houThree[2])) {
                    if (houThree[0].equals(houThree[1]) && houThree[1].equals(houThree[2])) {
                        return false;
                    }
                    return true;
                }
            } else if (num.contains("??????")) { // 014, ???????????????????????????1????????????????????????????????????1???????????????0,9???????????????
                if ((Integer.valueOf(houThree[0]) == Integer.valueOf(houThree[1]) - 1) ||
                        (Integer.valueOf(houThree[1]) == Integer.valueOf(houThree[2]) - 1) ||
                        (("0".equals(houThree[0]) && "9".equals(houThree[1])) || ("0".equals(houThree[0]) && "9".equals(houThree[2])))) {
                    if (("0".equals(houThree[0]) && "1".equals(houThree[1]) && "9".equals(houThree[2]))
                            || (houThree[0].equals(houThree[1]) || houThree[1].equals(houThree[2]))
                            || ((Integer.valueOf(houThree[0]) == Integer.valueOf(houThree[1]) - 1) && (Integer.valueOf(houThree[1]) == Integer.valueOf(houThree[2]) - 1))) { //?????? ????????? ???0,1,9?????????
                        return false;
                    }
                    return true;
                }
            } else if (num.contains("??????")) {
                boolean baozi = houThree[0].equals(houThree[1]) && houThree[0].equals(houThree[2]);
                boolean shunzi = ((Integer.valueOf(houThree[0]) == Integer.valueOf(houThree[1]) - 1) &&
                        (Integer.valueOf(houThree[1]) == Integer.valueOf(houThree[2]) - 1)) ||
                        ("0".equals(houThree[0]) && "8".equals(houThree[1]) && "9".equals(houThree[2])) ||
                        ("0".equals(houThree[0]) && "1".equals(houThree[1]) && "9".equals(houThree[2]));
                boolean duizi = houThree[0].equals(houThree[1]) || houThree[1].equals(houThree[2]);
                boolean banshun = (Integer.valueOf(houThree[0]) == Integer.valueOf(houThree[1]) - 1) ||
                        (Integer.valueOf(houThree[1]) == Integer.valueOf(houThree[2]) - 1) ||
                        (("0".equals(houThree[0]) && "9".equals(houThree[1])) || ("0".equals(houThree[0]) && "9".equals(houThree[2])));
                if (!baozi && !shunzi && !duizi && !banshun) {
                    return true;
                }
            }
        } else if (betNum.contains("??????")) {
            String niuNumber = getResult(new int[]{Integer.valueOf(sgArray[0]), Integer.valueOf(sgArray[1]), Integer.valueOf(sgArray[2]),
                    Integer.valueOf(sgArray[3]), Integer.valueOf(sgArray[4])});
            if (num.contains(niuNumber)) {
                return true;
            }
        }
        return false;
    }

    //    012,013,014,023,024,034,123,124,134,234
    //?????????????????????
    private String getResult(int[] data) {
        int[][] array = {{data[0], data[1], data[2], data[3], data[4]}, {data[0], data[1], data[3], data[2], data[4]},
                {data[0], data[1], data[4], data[2], data[3]}, {data[0], data[2], data[3], data[1], data[4]},
                {data[0], data[2], data[4], data[1], data[3]}, {data[0], data[3], data[4], data[1], data[2]},
                {data[1], data[2], data[3], data[0], data[4]}, {data[1], data[2], data[4], data[0], data[3]},
                {data[1], data[3], data[4], data[0], data[2]}, {data[2], data[3], data[4], data[0], data[1]}};
        for (int i = 0; i < array.length; i++) {
            if ((array[i][0] + array[i][1] + array[i][2]) % 10 == 0) {
                int xulie = array[i][3] + array[i][4];
                if (xulie % 10 == 0) {
//                    return 10; //??????
                    return "??????";
                } else {
                    if (xulie % 10 == 1) {
                        return "??????";
                    } else if (xulie % 10 == 2) {
                        return "??????";
                    } else if (xulie % 10 == 3) {
                        return "??????";
                    } else if (xulie % 10 == 4) {
                        return "??????";
                    } else if (xulie % 10 == 5) {
                        return "??????";
                    } else if (xulie % 10 == 6) {
                        return "??????";
                    } else if (xulie % 10 == 7) {
                        return "??????";
                    } else if (xulie % 10 == 8) {
                        return "??????";
                    } else if (xulie % 10 == 9) {
                        return "??????";
                    }
                }
            } else {
                continue;
            }
        }
        return "??????";
    }


    public String[] getSortArray(String[] nums) {
        for (int i = 0; i < nums.length; i++) {
            for (int j = i; j < nums.length; j++) {
                String lin = null;
                if (nums[i].compareTo(nums[j]) > 0) {
                    lin = nums[i];
                    nums[i] = nums[j];
                    nums[j] = lin;
                }
            }
        }
        return nums;
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


    /* (non Javadoc)
     * @Title: queryAussscLotteryNextSg
     * @Description: ??????????????????
     * @return
     * @see com.caipiao.business.service.bet.BetSscAzService#queryAussscLotteryNextSg()
     */
    @Override
    public AussscLotterySg queryAussscLotteryNextSg() {
        AussscLotterySgExample nextExample = new AussscLotterySgExample();
        AussscLotterySgExample.Criteria aussscCriteria = nextExample.createCriteria();
        aussscCriteria.andIdealTimeGreaterThan(DateUtils.getFullStringZeroSecond(new Date()));
        aussscCriteria.andOpenStatusEqualTo(LotteryResultStatus.WAIT);
        nextExample.setOrderByClause("ideal_time ASC");
        AussscLotterySg nextAussscLotterySg = aussscLotterySgMapper.selectOneByExample(nextExample);
        return nextAussscLotterySg;
    }


    /* (non Javadoc)
     * @Title: selectAussscLotteryByIssue
     * @Description: ????????????????????????
     * @param issue
     * @return
     * @see com.caipiao.business.service.bet.BetSscAzService#selectAussscLotteryByIssue(java.lang.String)
     */
    @Override
    public AussscLotterySg selectAussscLotteryByIssue(String issue, String number) {
        AussscLotterySgExample aussscExample = new AussscLotterySgExample();
        AussscLotterySgExample.Criteria aussscCriteria = aussscExample.createCriteria();
        aussscCriteria.andIssueEqualTo(issue);
        AussscLotterySg aussscLotterySg = aussscLotterySgMapper.selectOneByExample(aussscExample);

        if (aussscLotterySg != null && StringUtils.isNotEmpty(number) && StringUtils.isEmpty(aussscLotterySg.getNumber())) {
            aussscLotterySg.setNumber(number);
        }
        return aussscLotterySg;
    }

    /**
     * @return Integer
     * @Title: selectAusactOpenCount
     * @Description: ????????????????????????
     */
    public Integer selectAussscOpenCount() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("openStatus", LotteryResultStatus.AUTO);
        map.put("paramTime", TimeHelper.date("yyyy-MM-dd") + "%");
        Integer openCount = aussscLotterySgMapperExt.openCountByExample(map);
        return openCount;
    }


    @Override
    public void cacheIssueResultForAusssc(String issue, String number) {
        AussscLotterySg aussscLotterySg = this.selectAussscLotteryByIssue(issue, number);
        // ?????????????????????
        String redisKey = RedisKeys.AUZSSC_RESULT_VALUE;
        redisTemplate.opsForValue().set(redisKey, aussscLotterySg);
        // ??????????????????,?????????
        Long redisTime = CaipiaoRedisTimeEnum.AUZSSC.getRedisTime();
        AussscLotterySg nextAussscLotterySg = this.queryAussscLotteryNextSg();
        String nextRedisKey = RedisKeys.AUZSSC_NEXT_VALUE;
        redisTemplate.opsForValue().set(nextRedisKey, nextAussscLotterySg, redisTime, TimeUnit.MINUTES);
        // ????????????????????????
        Integer openCount = this.selectAussscOpenCount();
        String openRedisKey = RedisKeys.AUZSSC_OPEN_VALUE;
        redisTemplate.opsForValue().set(openRedisKey, openCount);
    }

}
