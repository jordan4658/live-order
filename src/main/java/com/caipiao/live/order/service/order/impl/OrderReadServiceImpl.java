package com.caipiao.live.order.service.order.impl;

import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.enums.SysParameterEnum;
import com.caipiao.live.common.enums.lottery.LotteryTypeEnum;
import com.caipiao.live.common.model.common.ResultInfo;


import com.caipiao.live.common.model.dto.order.OrderBetStatus;
import com.caipiao.live.common.model.dto.order.OrderStatus;
import com.caipiao.live.common.model.vo.order.OrderTodayListVo;
import com.caipiao.live.common.mybatis.entity.*;
import com.caipiao.live.common.mybatis.mapper.OrderBetRecordMapper;
import com.caipiao.live.common.mybatis.mapper.OrderRecordMapper;
import com.caipiao.live.common.service.sys.SysParamService;
import com.caipiao.live.common.util.DateUtils;
import com.caipiao.live.common.util.StringUtils;
import com.caipiao.live.order.model.dto.OrderBetDTO;
import com.caipiao.live.order.model.dto.OrderBetRecordResultDTO;
import com.caipiao.live.order.model.vo.OrderBetVO;
import com.caipiao.live.order.service.lottery.LotteryPlayOddsService;
import com.caipiao.live.order.service.lottery.LotteryPlayService;
import com.caipiao.live.order.service.lottery.LotteryService;
import com.caipiao.live.order.service.order.OrderReadService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;

@Service
public class OrderReadServiceImpl implements OrderReadService {

    private static final Logger logger = LoggerFactory.getLogger(OrderReadServiceImpl.class);

    @Resource
    private OrderRecordMapper orderRecordMapper;
    @Resource
    private LotteryService lotteryService;
    @Resource
    private LotteryPlayService lotteryPlayService;
    @Resource
    private OrderBetRecordMapper orderBetRecordMapper;
    @Resource
    private SysParamService sysParamService;
    @Autowired
    private LotteryPlayOddsService lotteryPlayOddsService;

    @Override
    public ResultInfo getCaiDetail(Integer orderId) {
        OrderRecord orderRecord = orderRecordMapper.selectByPrimaryKey(orderId);
        if (orderRecord == null) {
            return ResultInfo.error("??????????????????????????????");
        }
        // ??????????????????
        Map<Integer, Lottery> lotteryMap = lotteryService.selectLotteryMap(LotteryTypeEnum.LOTTERY.name());
        // ??????
        if (CollectionUtils.isEmpty(lotteryMap) || lotteryMap.get(orderRecord.getLotteryId()) == null) {
            return ResultInfo.error("????????????????????????");
        }
        // ????????????????????????Map??????
        Map<Integer, LotteryPlay> playMap = lotteryPlayService.selectPlayMap();
        // ??????
        if (CollectionUtils.isEmpty(playMap)) {
            return ResultInfo.error("?????????????????????????????????");
        }
        OrderBetRecordExample orderRecordExample = new OrderBetRecordExample();
        OrderBetRecordExample.Criteria criteria = orderRecordExample.createCriteria();
        criteria.andOrderIdEqualTo(orderId);
        criteria.andIsDeleteEqualTo(false);
        List<OrderBetRecord> orderBetRecords = orderBetRecordMapper.selectByExample(orderRecordExample);
        // ??????
        if (orderBetRecords == null || orderBetRecords.size() == Constants.DEFAULT_INTEGER) {
            return ResultInfo.error("??????????????????????????????");
        }
        List<OrderBetRecordResultDTO> betRecords = new ArrayList<>();
        for (OrderBetRecord vo : orderBetRecords) {
            if (playMap.get(vo.getPlayId()) == null) { //????????????????????????????????????
                continue;
            } else {
                OrderBetRecordResultDTO orderBetRecordResultDTO = new OrderBetRecordResultDTO();
                BeanUtils.copyProperties(vo, orderBetRecordResultDTO);
                orderBetRecordResultDTO.setBetId(vo.getId());
                orderBetRecordResultDTO.setStudioId(vo.getRoomId());
                betRecords.add(orderBetRecordResultDTO);
            }
        }
        return ResultInfo.ok(betRecords);
    }

    @Override
    public PageInfo<OrderBetVO> queryOrderList(OrderBetDTO data) {
        List<OrderBetVO> list = new ArrayList<>();
        // ????????????
        Map<Integer, Lottery> lotteryMap = lotteryService.selectLotteryMap(LotteryTypeEnum.LOTTERY.name());
        // ??????
        if (CollectionUtils.isEmpty(lotteryMap)) {
            return new PageInfo();
        }
        // ????????????????????????Map??????
        Map<Integer, LotteryPlay> playMap = lotteryPlayService.selectPlayMap();
        // ??????
        if (CollectionUtils.isEmpty(playMap)) {
            return new PageInfo();
        }
        // ????????????????????????
        Integer pageNo = data.getPageNo() == null ? 1 : data.getPageNo();
        Integer pageSize = data.getPageSize() == null || data.getPageSize().equals(0) ? 10 : data.getPageSize();
        Integer firstResult = (pageNo - 1) * pageSize;

        // ??????????????????
        OrderBetRecordExample betRecordExample = new OrderBetRecordExample();
//        betRecordExample.setOffset(firstResult);
//        betRecordExample.setLimit(pageSize);

        // ??????????????????
        String sortName = data.getSortName();
        String sortType = data.getSortType();
        String sort = "";
        if (!StringUtils.isEmpty(sortName) && !StringUtils.isEmpty(sortType)) {
            sort = sortName + " " + sortType + ",";
        }
        if (!sort.contains("create_time")) {
            sort += "create_time DESC";
        }
        if (",".equals(sort.substring(sort.length() - 1, sort.length()))) {
            sort = sort.substring(0, sort.length() - 1);
        }
        betRecordExample.setOrderByClause(sort);

        OrderBetRecordExample.Criteria betRecordCriteria = betRecordExample.createCriteria();
        betRecordCriteria.andUserIdEqualTo(data.getUserId());
        betRecordCriteria.andTbStatusNotEqualTo(OrderStatus.BACK);
        /*if (OrderStatus.NORMAL.equals(type)) {
            betRecordCriteria.andTbStatusNotEqualTo(OrderStatus.BACK);
        } else {
            betRecordCriteria.andTbStatusEqualTo(OrderStatus.BACK);
        }*/
        // ??????????????????
        String status = data.getStatus().trim();
        if (!StringUtils.isEmpty(status)) {
            if (!("HAS_LOTTERY".equals(status))) {
                if ("Lottery".equals(status)) {
                    List<String> statusList = new ArrayList<>();
                    statusList.add(Constants.WIN);
                    statusList.add(Constants.NO_WIN);
                    statusList.add(Constants.HE);
                    betRecordCriteria.andTbStatusIn(statusList);
                } else {
                    betRecordCriteria.andTbStatusEqualTo(status);
                }

            }

       /* if (!StringUtils.isEmpty(status)) {
            if (status.equals("HAS_LOTTERY")) {
                List<String> statusList = new ArrayList<>();
                statusList.add("WIN");
                statusList.add("NO_WIN");
                statusList.add("HE");
                betRecordCriteria.andTbStatusIn(statusList);
            } else {
                betRecordCriteria.andTbStatusEqualTo(status);
            }*/
        }
        // ??????????????????
        Date date = data.getDate();
        if (date != null) {
            Date startDate = DateUtils.getDayBegin(date);
            Date endDate = DateUtils.getDayEnd(date);
            betRecordCriteria.andCreateTimeBetween(startDate, endDate);
        }
        // ??????????????????
        List<Integer> lotteryIds = data.getLotteryIds();
        if (!CollectionUtils.isEmpty(lotteryIds)) {
            betRecordCriteria.andLotteryIdIn(lotteryIds);
        }
        //??????????????????
        //betRecordCriteria.andIsPushEqualTo(0);

        // ??????????????????
        PageHelper.startPage(pageNo, pageSize);
        List<OrderBetRecord> orderBetRecords = orderBetRecordMapper.selectByExample(betRecordExample);

        if (CollectionUtils.isEmpty(orderBetRecords)) {
            return new PageInfo();
        }

        // ????????????id
        List<Integer> orderIds = new ArrayList<>();
        // ????????????id
        for (OrderBetRecord orderBet : orderBetRecords) {
            orderIds.add(orderBet.getOrderId());
        }

        // ??????????????????
        OrderRecordExample orderExample = new OrderRecordExample();
        OrderRecordExample.Criteria orderCriteria = orderExample.createCriteria();
        orderCriteria.andIdIn(orderIds);
        List<OrderRecord> orderRecords = orderRecordMapper.selectByExample(orderExample);
        if (CollectionUtils.isEmpty(orderRecords)) {
            return new PageInfo();
        }
        // ????????????Map
        Map<Integer, OrderRecord> orderRecordMap = new HashMap<>();
        for (OrderRecord order : orderRecords) {
            orderRecordMap.put(order.getId(), order);
        }

        // ??????????????????
        SysParameter systemInfo = sysParamService.getByCode(SysParameterEnum.REGISTER_MEMBER_ODDS);
        double divisor = Double.parseDouble(systemInfo.getParamValue());

        OrderBetVO betVO;
        // ??????????????????
        for (OrderBetRecord orderBet : orderBetRecords) {
            betVO = new OrderBetVO();
            // ??????????????????
            BeanUtils.copyProperties(orderBet, betVO);

            // ??????????????????
            OrderRecord order = orderRecordMap.get(orderBet.getOrderId());
            betVO.setIssue(order.getIssue());
            betVO.setOrderSn(order.getOrderSn());
            betVO.setOpenNumber(order.getOpenNumber());

            // ??????????????????
            Lottery lottery = lotteryMap.get(orderBet.getLotteryId());
            if(lottery != null){
                betVO.setLotteryName(lottery.getName());
                // ??????????????????
                Double maxOdds = lottery.getMaxOdds();
                divisor = maxOdds.equals(0D) ? divisor : maxOdds;
            }

            //????????????????????????
            LotteryPlay parentlp = lotteryPlayService.getParentPlaybyId(playMap.get(orderBet.getPlayId()) == null ? 0 : playMap.get(orderBet.getPlayId()).getId());
            if (null != parentlp) {
                // ??????????????????
                betVO.setPlayName(parentlp.getName());
            } else {
                betVO.setPlayName(playMap.get(orderBet.getPlayId()) == null ? "" : playMap.get(orderBet.getPlayId()).getName());
            }

            // ????????????????????????????????????
            String odds = null;
            if (Constants.NEW_LOTTERY_ID_LIST.contains(orderBet.getLotteryId())) {
                StringBuilder betNumber = new StringBuilder();
                betNumber.append(orderBet.getPlayName()).append("@").append(orderBet.getBetNumber());
                odds = this.countOddsWithDivisor(orderBet.getSettingId(), betNumber.toString(), divisor);
            } else {
                odds = this.countOddsWithDivisor(orderBet.getSettingId(), orderBet.getBetNumber(), divisor);
            }
            betVO.setOdds(odds);

            /**
             * ?????????????????????
             *   ?????????????????????????????????????????????????????????????????????
             *   ?????????????????????????????????????????????????????????????????????????????????????????????????????????
             */
            String betNumber = orderBet.getBetNumber();
            //  2020/1/8 ??????????????????  ?????????betnumber???playname??????
            if (Constants.NEW_LOTTERY_ID_LIST.contains(orderBet.getLotteryId())) {
                betNumber = orderBet.getPlayName() + "@" + orderBet.getBetNumber();
                betVO.setPlayName(orderBet.getPlayName());
            }
            //Integer godOrderId = orderBet.getGodOrderId();
//            if (!godOrderId.equals(0) && orderBet.getTbStatus().equals(OrderBetStatus.WAIT)) {
//                // ?????? && ?????????
//                // ??????????????????????????????????????????
//                CircleGodPushOrder godPushOrder = circleGodPushOrderMapper.selectByPrimaryKey(godOrderId);
//                if (godPushOrder.getSecretStatus().equals(2)) {
//                    betNumber = "???????????????";
//                }
//            }
            betVO.setBetAmountIos(betVO.getBetAmount().toString());
            betVO.setBetNumber(betNumber);
            list.add(betVO);
        }

        PageInfo<OrderBetVO> pageInfo = new PageInfo(list);
        return pageInfo;
    }

    /**
     * ????????????????????????????????????
     *
     * @param settingId ??????id
     * @param betNumber ????????????
     * @param divisor   ????????????
     * @return
     */
    private String countOddsWithDivisor(Integer settingId, String betNumber, double divisor) {
        if (betNumber.contains("@")) {
            betNumber = betNumber.split("@")[1];
        }

        // ??????????????????????????????
        List<LotteryPlayOdds> oddsList = lotteryPlayOddsService.selectOddsListBySettingId(settingId);

        // ??????
        if (CollectionUtils.isEmpty(oddsList)) {
            return "";
        }
        LotteryPlayOdds odds = null;
        if (oddsList.size() == 1) {
            odds = oddsList.get(0);
        } else {
            TreeMap<Double, LotteryPlayOdds> maxOddstTreeMap = new TreeMap<>();
            for (LotteryPlayOdds playOdds : oddsList) {
                String[] splitBetNum = null;
                if ("123,234,345,456".equals(betNumber)) {
                    splitBetNum = betNumber.split(";");
                } else {
                    splitBetNum = betNumber.split(",");
                }

                // ????????????
                if (splitBetNum.length <= 1 && playOdds.getName().equals(betNumber)) {
                    // odds = playOdds;
                    maxOddstTreeMap.put(Double.parseDouble(playOdds.getTotalCount()) / Double.parseDouble(playOdds.getWinCount()), playOdds);
                } else {// TODO ?????????????????????????????????????????????????????????
                    for (String betContent : splitBetNum) {
                        if (playOdds.getName().equals(betContent)) {
                            // odds = playOdds;
                            maxOddstTreeMap.put(Double.parseDouble(playOdds.getTotalCount()) / Double.parseDouble(playOdds.getWinCount()), playOdds);
                        }
                    }
                }
            }
            // ????????????odds
            int treeMapSize = maxOddstTreeMap.size();
            if (treeMapSize > 0) {
                odds = maxOddstTreeMap.get(maxOddstTreeMap.lastKey());
            }
        }

        if (odds == null) {
            return "";
        }

        String totalCount = odds.getTotalCount();
        BigDecimal winCount = BigDecimal.valueOf(Double.valueOf(odds.getWinCount()));
        String oddsStr;
        if (totalCount.contains("/")) {
            String[] str = totalCount.split("/");
            oddsStr = BigDecimal.valueOf(Double.valueOf(str[0])).multiply(BigDecimal.valueOf(divisor)).divide(winCount, 3, BigDecimal.ROUND_HALF_UP).toString();
            // oddsStr += "/";
            // oddsStr += BigDecimal.valueOf(Double.valueOf(str[1])).multiply(BigDecimal.valueOf(divisor)).divide(winCount, 2, BigDecimal.ROUND_HALF_UP).toString();
        } else {
            oddsStr = BigDecimal.valueOf(Double.valueOf(totalCount)).multiply(BigDecimal.valueOf(divisor)).divide(winCount, 3, BigDecimal.ROUND_HALF_UP).toString();
        }
        return oddsStr;
    }

    @Override
    public ResultInfo<OrderTodayListVo> queryOrderTodayBetList(OrderBetDTO data) {
        OrderTodayListVo orderTodayListVo = new OrderTodayListVo();
        BigDecimal todayEarnAmount = new BigDecimal(0.00);//????????????
        BigDecimal todayAllBetAmount = new BigDecimal(0.00);//???????????????????????????
        BigDecimal todayWinAmount = new BigDecimal(0.00);//??????????????????
        BigDecimal todayHasSettle = new BigDecimal(0.00);//??????????????? ????????????
        BigDecimal todayNoSettle = new BigDecimal(0.00);//????????????  ????????????
        OrderBetRecordExample betRecordExample = new OrderBetRecordExample();
        OrderBetRecordExample.Criteria betRecordCriteria = betRecordExample.createCriteria();
        betRecordCriteria.andUserIdEqualTo(data.getUserId());
        // ??????????????????
        betRecordCriteria.andTbStatusNotEqualTo(OrderBetStatus.BACK);
        // ??????????????????
        betRecordCriteria.andLotteryIdEqualTo(data.getLotteryId());
        // ??????????????????
        Date todayEnd = DateUtils.todayEndTime();
        Date todayStart = DateUtils.todayStartTime();
        betRecordCriteria.andCreateTimeBetween(todayStart, todayEnd);
        // ??????????????????
        List<OrderBetRecord> orderBetRecords = orderBetRecordMapper.selectByExample(betRecordExample);
        if (orderBetRecords.size() > 0) {
            for (OrderBetRecord orderBetRecord : orderBetRecords) {
                todayWinAmount = todayWinAmount.add(orderBetRecord.getWinAmount());//??????????????????
                if (orderBetRecord.getTbStatus().equals(Constants.WAIT)) {
                    todayNoSettle = todayNoSettle.add(orderBetRecord.getBetAmount());//????????????????????????
                } else if (orderBetRecord.getTbStatus().equals(Constants.WIN) || orderBetRecord.getTbStatus().equals(Constants.NO_WIN)
                        || orderBetRecord.getTbStatus().equals(Constants.HE) || "HAS_LOTTERY".equals(orderBetRecord.getTbStatus())) {
                    todayHasSettle = todayHasSettle.add(orderBetRecord.getBetAmount());//???????????????????????????
                }
            }
        }
        //???????????? = ???????????? - ?????????????????????   //??????String??????
        String todayWinAmountString = todayWinAmount.toString();
        String todayNoSettleString = todayNoSettle.toString();
        String todayHasSettleString = todayHasSettle.toString();
        todayEarnAmount = todayWinAmount.subtract(todayHasSettle);
        String todayEarnAmountString = todayEarnAmount.toString();
        orderTodayListVo.setTodayWinAmount(todayWinAmountString);
        orderTodayListVo.setTodayNoSettle(todayNoSettleString);
        orderTodayListVo.setTodayHasSettle(todayHasSettleString);
        orderTodayListVo.setTodayEarnAmount(todayEarnAmountString);
        return ResultInfo.ok(orderTodayListVo);
    }


}
