package com.caipiao.live.order.service.order.impl;

import com.caipiao.live.common.enums.lottery.CaipiaoTypeEnum;
import com.caipiao.live.common.enums.lottery.LotteryTypeEnum;
import com.caipiao.live.common.model.common.ResultInfo;
import com.caipiao.live.common.model.dto.order.*;
import com.caipiao.live.common.mybatis.entity.*;
import com.caipiao.live.common.mybatis.mapper.*;
import com.caipiao.live.common.util.DateUtils;
import com.caipiao.live.order.service.lottery.BonusWriteService;
import com.caipiao.live.order.service.lottery.LotteryPlayOddsWriteService;
import com.caipiao.live.order.service.lottery.LotteryPlayWriteService;
import com.caipiao.live.order.service.lottery.LotteryWriteService;
import com.caipiao.live.order.service.order.OrderAppendWriteService;
import com.caipiao.live.order.service.order.OrderWriteService;
import com.caipiao.live.order.service.result.PceggLotterySgWriteService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class OrderAppendWriteServiceImpl implements OrderAppendWriteService {

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private LotteryWriteService lotteryWriteService;
    @Autowired
    private LotteryPlayWriteService lotteryPlayWriteService;
    @Autowired
    private CqsscLotterySgMapper cqsscLotterySgMapper;
    @Autowired
    private XjsscLotterySgMapper xjsscLotterySgMapper;
    @Autowired
    private TxffcLotterySgMapper txffcLotterySgMapper;
    @Autowired
    private BjpksLotterySgMapper bjpksLotterySgMapper;
    @Autowired
    private XyftLotterySgMapper xyftLotterySgMapper;
    @Autowired
    private OrderWriteService orderWriteService;
    @Autowired
    private OrderAppendRecordMapper orderAppendRecordMapper;
    @Autowired
    private BonusWriteService bonusWriteService;
    @Autowired
    private LotteryPlayOddsWriteService lotteryPlayOddsService;
    @Autowired
    private PceggLotterySgWriteService pceggLotterySgWriteService;

    @Autowired
    private BetRestrictMapper betRestrictMapper;

    private String issue;

    @Override
    public ResultInfo<List<OrderPlayDTO>> orderAppendPlan(AppendDTO appendDTO) {
        List<OrderPlayDTO> list = new ArrayList<>();
        // ??????????????????
        List<AppendBetDTO> appendBets = appendDTO.getAppendBet();

        if (CollectionUtils.isEmpty(appendBets)) {
            return ResultInfo.error("?????????????????????");
        }

        // ????????????
        Integer appendCount = appendDTO.getAppendCount();
        // ??????????????????
        Double betMultiples = appendDTO.getBetMultiples();
        // ??????????????????
        double multiples = appendDTO.getType().equals(1) ? 1 : appendDTO.getDoubleMultiples();

        OrderPlayDTO orderPlayDTO;
        for (AppendBetDTO appendBet : appendBets) {
            // ????????????id
            Integer lotteryId = appendBet.getLotteryId();
            // ??????????????????
//            Integer noOpenCount = this.getNoOpenCount(lotteryId);
//            if (appendCount > noOpenCount) {
//                return ResultInfo.error("?????????????????????????????????????????????");
//            }

            Integer playId = appendBet.getPlayId();
            // ??????????????????
            Bonus bonus = bonusWriteService.queryBonusByPlayId(playId);

            // ??????????????????
            BigDecimal betPrice = appendBet.getBetPrice();

            // ???????????????
            BigDecimal amount = betPrice;
            if (multiples != 1) {
                amount = betPrice.multiply(new BigDecimal(betMultiples))
                        .multiply(new BigDecimal(Math.pow(multiples, appendCount - 1)));
            }

            BetRestrict betRestrict = this.getBonusMap(lotteryId, 0);
            BetRestrict restrict = this.getBonusMap(lotteryId, playId);
            if (restrict != null && restrict.getMaxMoney().compareTo(BigDecimal.valueOf(0)) > 0) {
                if (amount.compareTo(restrict.getMaxMoney()) > 0) {
                    return ResultInfo.error("?????????????????????????????????????????????????????????");
                }
            } else {
                if (null != betRestrict && betRestrict.getMaxMoney().compareTo(BigDecimal.valueOf(0)) > 0) {
                    if (amount.compareTo(betRestrict.getMaxMoney()) > 0) {
                        return ResultInfo.error("?????????????????????????????????????????????????????????");
                    }
                }
            }

            // ??????????????????
            LotteryPlay lotteryPlay = lotteryPlayWriteService.selectPlayById(playId);

            // ??????????????????
            orderPlayDTO = new OrderPlayDTO();
            orderPlayDTO.setPlayName(lotteryPlay.getName());

            List<AppendPlanDTO> appendDTOList = new ArrayList<>();
            AppendPlanDTO orderAppendDTO;
            for (int i = 0; i < appendCount; i++) {
                orderAppendDTO = new AppendPlanDTO();
                orderAppendDTO.setNumber(appendBet.getBetNumber());
                // ??????????????????
                double appendMultiples = betMultiples * (Math.pow(multiples, i));
                orderAppendDTO.setMultiples(appendMultiples);
                // ??????????????????
                orderAppendDTO.setAmount(betPrice.multiply(new BigDecimal(appendMultiples)));
                // ????????????
                String newIssue = this.createNextIssue(lotteryId, appendDTO.getIssue(), i + 1);
                orderAppendDTO.setIssue(newIssue);
                appendDTOList.add(orderAppendDTO);
            }
            orderPlayDTO.setAppendInfo(appendDTOList);
            list.add(orderPlayDTO);
        }

        return ResultInfo.ok(list);
    }

    @Override
    public ResultInfo<Boolean> orderAppend(AppendDTO appendDTO) {
        // ??????????????????
        List<AppendBetDTO> appendBets = appendDTO.getAppendBet();

        if (CollectionUtils.isEmpty(appendBets)) {
            return ResultInfo.error("?????????????????????");
        }

        // ????????????
        Integer appendCount = appendDTO.getAppendCount();
        // ??????????????????
        Double betMultiples = appendDTO.getBetMultiples();
        // ??????????????????
        double multiples = appendDTO.getType().equals(1) ? 1 : appendDTO.getDoubleMultiples();
        // ????????????id
        Integer userId = appendDTO.getUserId();
        if (userId == null || userId < 1) {
            return ResultInfo.error("??????????????????");
        }
        // ??????????????????
        //ONELIVE-TODO ??????



//        MemBaseinfo appMember = memBaseinfoService.selectByPrimaryKey((long) userId);
//        // ????????????????????????
//        BigDecimal amount = new BigDecimal(0);
//
//        // ??????????????????
//        for (AppendBetDTO appendBet : appendBets) {
//
//            Integer playId = appendBet.getPlayId();
//
//            // ??????????????????
//            BigDecimal betPrice = appendBet.getBetPrice();
//
//            // ???????????????
//            BigDecimal maxBet = betPrice;
//            if (multiples != 1) {
//                maxBet = betPrice.multiply(new BigDecimal(betMultiples))
//                        .multiply(new BigDecimal(Math.pow(multiples, appendCount - 1)));
//            }
//
//            BetRestrict betRestrict = this.getBonusMap(appendBet.getLotteryId(), 0);
//            BetRestrict restrict = this.getBonusMap(appendBet.getLotteryId(), playId);
//            if (restrict != null && restrict.getMaxMoney().compareTo(BigDecimal.valueOf(0)) > 0) {
//                if (maxBet.compareTo(restrict.getMaxMoney()) > 0) {
//                    return ResultInfo.error("?????????????????????????????????????????????????????????");
//                }
//            } else {
//                if (null != betRestrict && betRestrict.getMaxMoney().compareTo(BigDecimal.valueOf(0)) > 0) {
//                    if (maxBet.compareTo(betRestrict.getMaxMoney()) > 0) {
//                        return ResultInfo.error("?????????????????????????????????????????????????????????");
//                    }
//                }
//            }
//
//            // ????????????????????????
//            for (int i = 0; i < appendCount; i++) {
//                // ??????????????????
//                double appendMultiples = betMultiples * (Math.pow(multiples, i));
//                amount = amount.add(betPrice.multiply(new BigDecimal(appendMultiples)));
//            }
//
//        }
//
//        if (appMember.getGoldnum().compareTo(amount) < 0) {
//            return ResultInfo.error("???????????????");
//        }
//
//        // ??????????????????
//        OrderAppendRecord orderAppendRecord;
//        for (AppendBetDTO betDto : appendBets) {
//            orderAppendRecord = new OrderAppendRecord();
//            orderAppendRecord.setFirstIssue(appendDTO.getIssue());
//            BeanUtils.copyProperties(appendDTO, orderAppendRecord);
//            BeanUtils.copyProperties(betDto, orderAppendRecord);
//
//            orderAppendRecord.setAppendedCount(0);
//            orderAppendRecordMapper.insertSelective(orderAppendRecord);
//
//            // ????????????????????????
//            ResultInfo<Boolean> resultInfo = orderWriteService.orderAppend(orderAppendRecord, appendDTO.getSource());
//            if ("500".equals(resultInfo.getStatus())) {
//                return ResultInfo.error((String) resultInfo.getInfo());
//            }
//            orderAppendRecord.setAppendedCount(1);
//            orderAppendRecordMapper.updateByPrimaryKeySelective(orderAppendRecord);
//        }
//
//        /**
//         * ??????????????????
//         */
//        MemGoldchangeDO dto = new MemGoldchangeDO();
//        dto.setUserId(userId);
//        // ????????????
//        dto.setOpnote("????????????");
//        // ????????????
//        dto.setChangetype(GoldchangeEnum.LOTTERY_BETTING.getValue());
//        // ???????????????????????????
//        BigDecimal tradeOffAmount = getTradeOffAmount(amount.multiply(new BigDecimal(-1)));
//        dto.setQuantity(tradeOffAmount);
//        // ?????????????????????????????????????????????
//        dto.setNoWithdrawalAmount(tradeOffAmount);
//        // ???????????????????????????
//        dto.setBetAmount(tradeOffAmount.negate());
//        dto.setWaitAmount(tradeOffAmount.negate());
//        // ????????????????????????
//        memBaseinfoWriteService.updateUserBalance(dto);

        return ResultInfo.ok(true);
    }

    /**
     * ?????????????????????
     *
     * @param lotteryId ??????id
     * @param issue     ???????????????
     * @param count     ???????????????
     * @return
     */
    @Override
    public String createNextIssue(Integer lotteryId, String issue, int count) {
        if (lotteryId.equals(Integer.parseInt(CaipiaoTypeEnum.TXFFC.getTagType()))) {
            String[] issueSplit = issue.split("-");
            String format = new DecimalFormat("0000").format(Long.valueOf(issueSplit[1]) + count);
            issue = issueSplit[0] + "-" + format;
        } else {
            issue = Long.toString(Long.valueOf(issue) + count);
        }
        return issue;
    }

    @Override
    public ResultInfo<List<OrderAppendDTO>> orderAppendList(OrderBetDTO data) {
        List<OrderAppendDTO> list = new ArrayList<>();

        Integer userId = data.getUserId();
        List<Integer> lotteryIds = data.getLotteryIds();
        Date date = data.getDate();
        Integer pageNo = data.getPageNo();
        Integer pageSize = data.getPageSize();

        // ??????????????????
        OrderAppendRecordExample orderAppendRecordExample = new OrderAppendRecordExample();
        OrderAppendRecordExample.Criteria criteria = orderAppendRecordExample.createCriteria();
        criteria.andUserIdEqualTo(userId);
        if (!CollectionUtils.isEmpty(lotteryIds)) {
            criteria.andLotteryIdIn(lotteryIds);
        }
        if (date != null) {
            criteria.andCreateTimeBetween(DateUtils.getDayBegin(date), DateUtils.getDayEnd(date));
        }
        orderAppendRecordExample.setOffset((pageNo - 1) * pageSize);
        orderAppendRecordExample.setLimit(pageSize);
        // ??????????????????
        String sortName = data.getSortName();
        String sortType = data.getSortType();
        String sort = "";
        if (!StringUtils.isEmpty(sortName) && !StringUtils.isEmpty(sortType)) {
            sort = sortName + " " + sortType + ",";
        }
        sort += "id DESC";
        orderAppendRecordExample.setOrderByClause(sort);
        // ??????????????????
        List<OrderAppendRecord> orderAppendRecords = orderAppendRecordMapper.selectByExample(orderAppendRecordExample);
        // ??????
        if (CollectionUtils.isEmpty(orderAppendRecords)) {
            return ResultInfo.ok(list);
        }

        // ??????????????????
        Map<Integer, Lottery> lotteryMap = lotteryWriteService.selectLotteryMap(LotteryTypeEnum.LOTTERY.name());
        if (CollectionUtils.isEmpty(lotteryMap)) {
            return ResultInfo.error("???????????????????????????");
        }

        // ??????????????????
        Map<Integer, LotteryPlay> lotteryPlayMap = lotteryPlayWriteService.selectPlayMap();
        if (CollectionUtils.isEmpty(lotteryPlayMap)) {
            return ResultInfo.error("???????????????????????????");
        }

        // ????????????
        OrderAppendDTO dto;
        // ??????
        for (OrderAppendRecord record : orderAppendRecords) {
            dto = new OrderAppendDTO();
            BeanUtils.copyProperties(record, dto);
            dto.setLotteryName(lotteryMap.get(record.getLotteryId()) == null ? ""
                    : lotteryMap.get(record.getLotteryId()).getName());
            dto.setPlayName(lotteryPlayMap.get(record.getPlayId()) == null ? ""
                    : lotteryPlayMap.get(record.getPlayId()).getName());
            // ????????????
            BigDecimal odds = lotteryPlayOddsService.countOdds(record.getLotteryId(), record.getSettingId(),
                    record.getBetNumber());
            dto.setOdds(odds);
            // ?????????????????????
            String issue = this.createNextIssue(record.getLotteryId(), record.getFirstIssue(),
                    record.getAppendedCount() - 1);
            dto.setIssue(issue);
            // ????????????????????????
            double appendMultiples = record.getBetMultiples()
                    * (Math.pow(record.getDoubleMultiples(), record.getAppendedCount() - 1));
            dto.setCurrentBetPrice(record.getBetPrice().multiply(new BigDecimal(appendMultiples)));
            list.add(dto);
        }
        return ResultInfo.ok(list);
    }

    @Override
    public void appendOrder(OrderRecord order, BigDecimal winAmount, Boolean isWin) {
        // ???????????????????????????
        Integer appendId = order.getAppendId();
        if (appendId.equals(0)) {
            return;
        }
        OrderAppendRecord orderAppendRecord = orderAppendRecordMapper.selectByPrimaryKey(appendId);
        Boolean isStop = orderAppendRecord.getIsStop();
        // ??????????????????
        if (isWin) {
            orderAppendRecord.setWinAmount(orderAppendRecord.getWinAmount().add(winAmount));
            orderAppendRecord.setWinCount(orderAppendRecord.getWinCount() + 1);
        }

        // ????????????????????????
        if (isStop) {
            orderAppendRecordMapper.updateByPrimaryKey(orderAppendRecord);
            return;
        }

        // ????????????????????????
        BigDecimal returnAmount = new BigDecimal(0);
        for (int i = orderAppendRecord.getAppendedCount(); i < orderAppendRecord.getAppendCount(); i++) {
            // ??????????????????
            double appendMultiples = orderAppendRecord.getBetMultiples()
                    * (Math.pow(orderAppendRecord.getDoubleMultiples(), i));
            returnAmount = returnAmount.add(orderAppendRecord.getBetPrice().multiply(new BigDecimal(appendMultiples)));
        }

        // ???????????????????????????
        if (orderAppendRecord.getWinStop() && isWin) {
            //ONELIVE TODO ??????


//            /**
//             * ?????????????????? ???????????????
//             */
//            orderAppendRecord.setIsStop(true);
//            orderAppendRecordMapper.updateByPrimaryKey(orderAppendRecord);
//
//            // ??????????????????
//            MemGoldchangeDO dto = new MemGoldchangeDO();
//            // ????????????id
//            dto.setUserId(order.getUserId());
//            // ????????????
//            dto.setOpnote("??????????????????, ?????????????????????");
//            // ????????????
//            dto.setChangetype(GoldchangeEnum.APPEND_BET_BACK.getValue());
//            // ???????????????
//            BigDecimal tradeOffAmount = getTradeOffAmount(returnAmount);
//            dto.setQuantity(tradeOffAmount);
//            // ?????????????????????????????????
//            dto.setNoWithdrawalAmount(tradeOffAmount);
//            dto.setWaitAmount(tradeOffAmount.negate());
//            // ????????????????????????
//            memBaseinfoWriteService.updateUserBalance(dto);
            return;
        }

        // ??????????????????????????????
        orderWriteService.orderAppend(orderAppendRecord, order.getSource());

        /**
         * ?????????????????? 1???????????????+1 2???????????????????????????????????? 3.1 ?????????????????????????????????????????????
         */
        // ????????????+1
        Integer appendedCount = orderAppendRecord.getAppendedCount() + 1;
        orderAppendRecord.setAppendedCount(appendedCount);
        // ?????????????????????????????????
        if (appendedCount.equals(orderAppendRecord.getAppendCount())) {
            orderAppendRecord.setIsStop(true);
        }
        orderAppendRecordMapper.updateByPrimaryKey(orderAppendRecord);
    }

    /**
     * ????????????????????????
     *
     * @param lotteryId ??????id
     * @return
     */
    private Integer getNoOpenCount(Integer lotteryId) {
        // ??????????????????
        Lottery lottery = lotteryWriteService.selectLotteryById(lotteryId);

        // ???????????????
        Integer sum = lottery.getStartlottoTimes();

        int count = 0;
        String date = DateUtils.formatDate(new Date(), "yyyy-MM-dd");
        switch (lotteryId) {
            case 1: // ???????????????
                CqsscLotterySgExample cqsscLotterySgExample = new CqsscLotterySgExample();
                CqsscLotterySgExample.Criteria cqsscCriteria = cqsscLotterySgExample.createCriteria();
                cqsscCriteria.andWanIsNotNull();
                cqsscLotterySgExample.setOrderByClause("ideal_time desc");
                CqsscLotterySg cqsscLotterySg = cqsscLotterySgMapper.selectOneByExample(cqsscLotterySgExample);
                String cqsscLotterySgIssue = cqsscLotterySg.getIssue();
                Integer cqEnd = Integer.valueOf(cqsscLotterySgIssue.substring(8));
                count = sum - cqEnd;
                if (count > 0) {
                    issue = Long.toString(Long.valueOf(cqsscLotterySgIssue) + 1);
                    ResultInfo<Boolean> cqIsOpen = orderWriteService.checkIssueIsOpen(1, issue, 1);
                    if (500==cqIsOpen.getCode()) {
                        count -= 1;
                        issue = Long.toString(Long.valueOf(cqsscLotterySgIssue) + 2);
                    }
                }
                break;

            case 2: // ???????????????
                XjsscLotterySgExample xjsscExample = new XjsscLotterySgExample();
                XjsscLotterySgExample.Criteria xjsscCriteria = xjsscExample.createCriteria();
                xjsscCriteria.andWanIsNotNull();
                xjsscExample.setOrderByClause("ideal_time desc");
                XjsscLotterySg xjsscLotterySg = xjsscLotterySgMapper.selectOneByExample(xjsscExample);
                String xjsscLotterySgIssue = xjsscLotterySg.getIssue();
                Integer xjEnd = Integer.valueOf(xjsscLotterySgIssue.substring(8));
                count = sum - xjEnd;
                if (count > 0) {
                    issue = Long.toString(Long.valueOf(xjsscLotterySgIssue) + 1);
                    ResultInfo<Boolean> xjIsOpen = orderWriteService.checkIssueIsOpen(2, issue, 1);
                    if (500==xjIsOpen.getCode()) {
                        count -= 1;
                        issue = Long.toString(Long.valueOf(xjsscLotterySgIssue) + 2);
                    }
                }
                break;

            case 3: // ??????????????????
                TxffcLotterySgExample txffcExample = new TxffcLotterySgExample();
                TxffcLotterySgExample.Criteria txffcCriteria = txffcExample.createCriteria();
                txffcCriteria.andWanIsNotNull();
                txffcExample.setOrderByClause("ideal_time desc");
                TxffcLotterySg txffcLotterySg = txffcLotterySgMapper.selectOneByExample(txffcExample);
                String txffcLotterySgIssue = txffcLotterySg.getIssue();
                Integer txEnd = Integer.valueOf(txffcLotterySgIssue.substring(9));
                count = sum - txEnd;
                if (count > 0) {
                    issue = this.createNextIssue(3, txffcLotterySgIssue, 1);
                    ResultInfo<Boolean> txIsOpen = orderWriteService.checkIssueIsOpen(3, issue, 1);
                    if (500==txIsOpen.getCode()) {
                        count -= 1;
                        issue = this.createNextIssue(3, issue, 1);
                    }
                }
                break;

            case 5: // PC??????
//                PceggLotterySgExample pceggLotterySgExample = new PceggLotterySgExample();
//                PceggLotterySgExample.Criteria pceggLotterySgExampleCriteria = pceggLotterySgExample.createCriteria();
//                pceggLotterySgExampleCriteria.andTimeLike(date+"%");
//                count = sum - pceggLotterySgMapper.countByExample(pceggLotterySgExample);
                count = sum - pceggLotterySgWriteService.queryOpenedCount();
                if (count > 0) {
//                    PceggLotterySgExample pceggLotterySgExample2 = new PceggLotterySgExample();
//                    pceggLotterySgExample2.setOrderByClause("id desc");
//                    PceggLotterySg pceggLotterySg = pceggLotterySgMapper.selectOneByExample(pceggLotterySgExample2);
//                    issue = Integer.toString(Integer.valueOf(pceggLotterySg.getIssue()) + 1);
//                    ResultInfo<Boolean> pcIsOpen = orderWriteService.checkIssueIsOpen(5, issue, 1);
//                    if (pcIsOpen.getStatus().equals("500")) {
//                        count -= 1;
//                        issue = Integer.toString(Integer.valueOf(issue) + 1);
//                    }
                    issue = pceggLotterySgWriteService.queryNextSg().getIssue();
                }
                break;

            case 6: // ??????PK10
                BjpksLotterySgExample bjpksLotterySgExample = new BjpksLotterySgExample();
                BjpksLotterySgExample.Criteria bjpksLotterySgExampleCriteria = bjpksLotterySgExample.createCriteria();
                bjpksLotterySgExampleCriteria.andTimeLike(date + "%");
                bjpksLotterySgExampleCriteria.andNumberIsNotNull();
                count = sum - bjpksLotterySgMapper.countByExample(bjpksLotterySgExample);
                if (count > 0) {
                    BjpksLotterySgExample bjpksLotterySgExample2 = new BjpksLotterySgExample();
                    BjpksLotterySgExample.Criteria bjpksCriteria = bjpksLotterySgExample2.createCriteria();
                    bjpksCriteria.andNumberIsNotNull();
                    bjpksLotterySgExample2.setOrderByClause("ideal_time desc");
                    BjpksLotterySg bjpksLotterySg = bjpksLotterySgMapper.selectOneByExample(bjpksLotterySgExample2);
                    issue = Integer.toString(Integer.valueOf(bjpksLotterySg.getIssue()) + 1);
                    ResultInfo<Boolean> bjIsOpen = orderWriteService.checkIssueIsOpen(6, issue, 1);
                    if (500==bjIsOpen.getCode()) {
                        count -= 1;
                        issue = Integer.toString(Integer.valueOf(issue) + 1);
                    }
                }
                break;

            case 7: // ????????????
                XyftLotterySgExample xyftLotterySgExample = new XyftLotterySgExample();
                XyftLotterySgExample.Criteria sgCriteria = xyftLotterySgExample.createCriteria();
                sgCriteria.andNumberIsNotNull();
                xyftLotterySgExample.setOrderByClause("ideal_time desc");
                XyftLotterySg xyftLotterySg = xyftLotterySgMapper.selectOneByExample(xyftLotterySgExample);
                String xyftLotterySgIssue = xyftLotterySg.getIssue();
                Integer xyftEnd = Integer.valueOf(xyftLotterySgIssue.substring(8));
                count = sum - xyftEnd;
                if (count > 0) {
                    issue = Long.toString(Long.valueOf(xyftLotterySgIssue) + 1);
                    ResultInfo<Boolean> xyIsOpen = orderWriteService.checkIssueIsOpen(7, issue, 1);
                    if (500==xyIsOpen.getCode()) {
                        count -= 1;
                        issue = Long.toString(Long.valueOf(xyftLotterySgIssue) + 2);
                    }
                }
                break;

            default:
                break;
        }
        return count;
    }

    private BetRestrict getBonusMap(Integer lotteryId, Integer playId) {
        BetRestrictExample betRestrictExample = new BetRestrictExample();
        BetRestrictExample.Criteria bonusCriteria = betRestrictExample.createCriteria();
        bonusCriteria.andPlayTagIdEqualTo(playId);
        bonusCriteria.andLotteryIdEqualTo(lotteryId);
        BetRestrict betRestrict = betRestrictMapper.selectOneByExample(betRestrictExample);
        return betRestrict;
    }

}
