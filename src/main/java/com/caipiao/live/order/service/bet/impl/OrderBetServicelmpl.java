package com.caipiao.live.order.service.bet.impl;

import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.constant.RedisKeys;
import com.caipiao.live.common.enums.AppMianParamEnum;
import com.caipiao.live.common.enums.lottery.LotteryTableNameEnum;
import com.caipiao.live.common.model.common.ResultInfo;

import com.caipiao.live.order.model.dto.OrderDTO;
import com.caipiao.live.common.mybatis.entity.*;
import com.caipiao.live.common.mybatis.mapper.LotteryPlayMapper;
import com.caipiao.live.common.mybatis.mapper.MemWalletMapper;
import com.caipiao.live.common.service.lottery.LotterySgServiceReadSg;
import com.caipiao.live.common.util.CollectionUtil;
import com.caipiao.live.common.util.StringUtils;
import com.caipiao.live.common.util.redis.RedisBusinessUtil;
import com.caipiao.live.order.rest.LotteryPlaySettingReadRest;
import com.caipiao.live.order.rest.LotteryReadRest;
import com.caipiao.live.order.service.bet.OrderBetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderBetServicelmpl implements OrderBetService {

    private static final Logger logger = LoggerFactory.getLogger(OrderBetServicelmpl.class);

    @Autowired
    private LotterySgServiceReadSg lotterySgServiceReadSg;
    @Autowired
    private LotteryReadRest lotteryReadRest;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private LotteryPlaySettingReadRest lotteryPlaySettingReadRest;
    @Autowired
    private LotteryPlayMapper lotteryPlayMapper;
    @Autowired
    private MemWalletMapper memWalletMapper;


    @Override
    public ResultInfo bettingInformationVerification(OrderDTO data, Long startTime, Integer start) {
        try {
            Map<String, LotteryPlaySetting> lotterySettingMap = redisTemplate.opsForHash().entries(RedisKeys.LOTTERY_PLAY_SETTING_MAP_TYPE + Constants.LOTTERY_CATEGORY_TYPE_LOTTERY);
            if (CollectionUtil.isEmpty(lotterySettingMap)) {
                lotterySettingMap = lotteryPlaySettingReadRest.queryLotteryPlaySettingMap(Constants.LOTTERY_CATEGORY_TYPE_LOTTERY);
            }
            if (null == lotterySettingMap) {
                return ResultInfo.error("???????????????????????????");
            }
            if (null != start && Constants.DEFAULT_ONE.equals(start)) {
                if (null == data || !data.isValid(lotterySettingMap)) {//???????????????
                    return ResultInfo.error("????????????????????????");
                }
            } else if (null != start && Constants.DEFAULT_TWO.equals(start)) {
                if (null == data || !data.isValidNew(lotterySettingMap)) {//???????????????
                    return ResultInfo.error("????????????????????????");
                }
            }

            // ????????????
            Integer lotteryId = data.getLotteryId();
            // ???????????????????????????
            String issue = data.getIssue();
            String tableName = LotteryTableNameEnum.getTableNameByLotteryId(lotteryId);
            if (org.apache.commons.lang.StringUtils.isBlank(tableName)) {
                return ResultInfo.error("?????????????????????");
            }

            ResultInfo<Map<String, Object>> resultInfo = lotterySgServiceReadSg.getNewestSgInfobyids(lotteryId + "");

            logger.info("lotterySgRest.getNewestSgInfobyids, [{}] milliseconds", System.currentTimeMillis() - startTime);
            Map<String, Object> mapsg = (Map<String, Object>) resultInfo.getData().get(lotteryId + "");
            // ????????????????????????????????????
            String nextIssue = String.valueOf(mapsg.get("nextIssue"));
            if (StringUtils.isBlank(nextIssue) || "null".equals(nextIssue)) {
                return ResultInfo.error("??????????????????????????????");
            }

            // ??????????????????????????????
            List<OrderBetRecord> orderBetList = data.getOrderBetList();
            if (CollectionUtils.isEmpty(orderBetList)) {
                return ResultInfo.error("???????????????????????????");
            }
            logger.info("orderDTO.getOrderBetList(), {}", System.currentTimeMillis() - startTime);

            Long nextTime = Long.valueOf(String.valueOf(mapsg.get(AppMianParamEnum.NEXTTIME.getParamEnName())));
            Lottery lottery = RedisBusinessUtil.get(RedisKeys.LOTTERY_KEY + lotteryId);
            if (null == lottery) {
                lottery = lotteryReadRest.queryLotteryByLotteryId(lotteryId);
            }
            //????????????
            int fengpan = lottery.getEndTime() == null ? 0 : lottery.getEndTime();
            if ((nextTime * 1000 - startTime) / 1000 < fengpan) {
                return ResultInfo.error("??????????????????????????????????????????");
            }

            if (lottery.getIsWork() != null && lottery.getIsWork().equals(0)) {
                return ResultInfo.error("????????????????????????");
            }
            if (StringUtils.isEmpty(issue) || issue.compareTo(nextIssue) != 0) {
                return ResultInfo.error("????????????????????????????????????????????????????????????");
            }
            // ????????????????????????
            BigDecimal amount = new BigDecimal("0.000");
            // ????????????????????????Map??????
            Map<Integer, LotteryPlay> lotteryPlays = this.getlotteryPlays();
            //???????????????????????????????????????
            for (OrderBetRecord orderBet : data.getOrderBetList()) {
                if (StringUtils.isEmpty(orderBet.getPlayName())) {
                    orderBet.setPlayName(lotteryPlays.get(orderBet.getPlayId()).getName());
                }
                amount = amount.add(orderBet.getBetAmount().multiply(new BigDecimal(orderBet.getBetCount())));
            }

            //????????????
            MemWalletExample memWalletExample = new MemWalletExample();
            MemWalletExample.Criteria walletCriteria = memWalletExample.createCriteria();
            walletCriteria.andUserIdEqualTo(Long.valueOf(data.getUserId()));
            MemWallet wallet = memWalletMapper.selectOneByExample(memWalletExample);

            if (amount.compareTo(wallet.getBalance()) > 0) {
                logger.info("??????????????????");
                ResultInfo.error("??????????????????");
            }

        } catch (Exception e) {
            logger.error("????????????,data:{}", data, e);
            return ResultInfo.error("??????????????????");
        }
        return ResultInfo.ok();
    }

    private Map<Integer, LotteryPlay> getlotteryPlays() {
        Map<Integer, LotteryPlay> map = new HashMap<>();
        // ??????????????????????????????
        List<LotteryPlay> list = this.getLotteryPlaysFromCache();
        if (CollectionUtils.isEmpty(list)) {
            return map;
        }
        for (LotteryPlay play : list) {
            map.put(play.getPlayTagId(), play);
        }
        return map;
    }

    private List<LotteryPlay> getLotteryPlaysFromCache() {
        List<LotteryPlay> lotteryPlayList = (List<LotteryPlay>) redisTemplate.opsForValue()
                .get(RedisKeys.LOTTERY_PLAY_LIST_KEY);
        if (!CollectionUtils.isEmpty(lotteryPlayList)) {
            return lotteryPlayList;
        }
        LotteryPlayExample example = new LotteryPlayExample();
        LotteryPlayExample.Criteria criteria = example.createCriteria();
        criteria.andIsDeleteEqualTo(false);
        lotteryPlayList = lotteryPlayMapper.selectByExample(example);
        redisTemplate.opsForValue().set(RedisKeys.LOTTERY_PLAY_LIST_KEY, lotteryPlayList);
        return lotteryPlayList;
    }

}
