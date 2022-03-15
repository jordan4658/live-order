package com.caipiao.live.order.service.lottery.impl;

import com.caipiao.live.common.service.sys.SysParamService;
import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.constant.RedisKeys;
import com.caipiao.live.common.enums.SysParameterEnum;
import com.caipiao.live.common.mybatis.entity.Lottery;
import com.caipiao.live.common.mybatis.entity.LotteryExample;
import com.caipiao.live.common.mybatis.entity.LotteryPlayOdds;
import com.caipiao.live.common.mybatis.entity.LotteryPlayOddsExample;
import com.caipiao.live.common.mybatis.entity.SysParameter;
import com.caipiao.live.common.mybatis.mapper.LotteryMapper;
import com.caipiao.live.common.mybatis.mapper.LotteryPlayOddsMapper;
import com.caipiao.live.common.util.CollectionUtil;
import com.caipiao.live.common.util.redis.RedisBusinessUtil;
import com.caipiao.live.order.service.lottery.LotteryPlayOddsWriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

@Service
public class LotteryPlayOddsWriteServiceImpl implements LotteryPlayOddsWriteService {

    @Autowired
    private LotteryPlayOddsMapper lotteryPlayOddsMapper;
    @Resource
    private SysParamService sysParamService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private LotteryMapper lotteryMapper;

    @Override
    public Map<Integer, LotteryPlayOdds> selectPlayOddsBySettingIds(List<Integer> settingIds) {
        LotteryPlayOddsExample oddsExample = new LotteryPlayOddsExample();
        LotteryPlayOddsExample.Criteria oddsCriteria = oddsExample.createCriteria();
        oddsCriteria.andIsDeleteEqualTo(false);
        oddsCriteria.andSettingIdIn(settingIds);
        List<LotteryPlayOdds> oddsList = lotteryPlayOddsMapper.selectByExample(oddsExample);
        Map<Integer, LotteryPlayOdds> oddsMap = new HashMap<>();
        for (LotteryPlayOdds odds : oddsList) {
            oddsMap.put(odds.getSettingId(), odds);
        }
        return oddsMap;
    }

    @Override
    public Map<String, LotteryPlayOdds> selectPlayOddsBySettingId(Integer settingId) {
        // 获取赔率信息
        List<LotteryPlayOdds> oddsList = this.selectOddsListBySettingId(settingId);
        Map<String, LotteryPlayOdds> oddsMap = new HashMap<>();
        for (LotteryPlayOdds odds : oddsList) {
            oddsMap.put(odds.getName(), odds);
        }
        return oddsMap;
    }

    @Override
    public LotteryPlayOdds findPlayOddsBySettingId(Integer settingId) {
        LotteryPlayOddsExample oddsExample = new LotteryPlayOddsExample();
        LotteryPlayOddsExample.Criteria oddsCriteria = oddsExample.createCriteria();
        oddsCriteria.andIsDeleteEqualTo(false);
        oddsCriteria.andSettingIdEqualTo(settingId);
        LotteryPlayOdds lotteryPlayOdds = lotteryPlayOddsMapper.selectOneByExample(oddsExample);
        return lotteryPlayOdds;
    }

    @Override
    public List<LotteryPlayOdds> selectOddsListBySettingId(Integer settingId) {
        List<LotteryPlayOdds> oddsList = RedisBusinessUtil.getOddsSettingList(settingId);
        if (CollectionUtils.isEmpty(oddsList)) {
            LotteryPlayOddsExample oddsExample = new LotteryPlayOddsExample();
            LotteryPlayOddsExample.Criteria oddsCriteria = oddsExample.createCriteria();
            oddsCriteria.andSettingIdEqualTo(settingId);
            oddsCriteria.andIsDeleteEqualTo(false);
            oddsList = lotteryPlayOddsMapper.selectByExample(oddsExample);

            RedisBusinessUtil.setOddsSettingList(oddsList,settingId);
        }
        return oddsList;
    }

    @Override
    public BigDecimal countOdds(Integer lotteryId, Integer settingId, String betNumber) {
        if (betNumber.contains(Constants.STR_AT)) {
            betNumber = betNumber.split(Constants.STR_AT)[1];
        }
        // 获取彩种信息
        // Lottery lottery = lotteryWriteService.selectLotteryById(lotteryId);

        LotteryExample example = new LotteryExample();
        LotteryExample.Criteria criteria = example.createCriteria();
        criteria.andLotteryIdEqualTo(lotteryId);
        Lottery lottery = lotteryMapper.selectOneByExample(example);
        // 获取赔率因子
        SysParameter systemInfo = sysParamService.getByCode(SysParameterEnum.REGISTER_MEMBER_ODDS);
        BigDecimal divisor = new BigDecimal(systemInfo.getParamValue());

        // 获取赔率因子
        Double maxOdds = lottery.getMaxOdds();
        divisor = maxOdds.equals(0D) ? divisor : new BigDecimal(maxOdds);

        // 获取赔率信息
        List<LotteryPlayOdds> oddsList = this.selectOddsListBySettingId(settingId);

        BigDecimal odds = new BigDecimal(0);
        if (CollectionUtil.isNotEmpty(oddsList)) {
            if (oddsList.size() > 1) {
                for (LotteryPlayOdds playOdds : oddsList) {
                    if (!playOdds.getName().equals(betNumber.trim())) {
                        continue;
                    }
                    BigDecimal total = new BigDecimal(playOdds.getTotalCount().replace(" ", ""));
                    BigDecimal win = new BigDecimal(playOdds.getWinCount().replace(" ", ""));
                    odds = total.divide(win, 10, BigDecimal.ROUND_HALF_UP);
                    break;
                }
            } else {
                LotteryPlayOdds playOdds = oddsList.get(0);
                BigDecimal total;
                if (playOdds.getTotalCount().indexOf("/") > -1) {
                    String str = playOdds.getTotalCount().substring(0, playOdds.getTotalCount().indexOf("/"));
                    total = new BigDecimal(str);
                } else {
                    total = new BigDecimal(playOdds.getTotalCount());
                }
                BigDecimal win = new BigDecimal(playOdds.getWinCount());
                odds = total.divide(win, 10, BigDecimal.ROUND_HALF_UP);
            }
        }
        return odds.multiply(divisor).setScale(3, BigDecimal.ROUND_HALF_UP);
    }

    @Override
    public BigDecimal countOddsFC(Integer lotteryId, Integer settingId, String betNumber, Integer winLevel) {
        // 获取彩种信息
        // Lottery lottery = lotteryWriteService.selectLotteryById(lotteryId);

        LotteryExample example = new LotteryExample();
        LotteryExample.Criteria criteria = example.createCriteria();
        criteria.andLotteryIdEqualTo(lotteryId);
        Lottery lottery = lotteryMapper.selectOneByExample(example);
        // 获取赔率因子
        SysParameter systemInfo = sysParamService.getByCode(SysParameterEnum.REGISTER_MEMBER_ODDS);
        BigDecimal divisor = new BigDecimal(systemInfo.getParamValue());

        // 获取赔率因子
        Double maxOdds = lottery.getMaxOdds();
        divisor = maxOdds.equals(0D) ? divisor : new BigDecimal(maxOdds);

        // 获取赔率信息
        List<LotteryPlayOdds> oddsList = this.selectOddsListBySettingId(settingId);

        BigDecimal odds = new BigDecimal(0);
        LotteryPlayOdds playOdds = oddsList.get(0);
        String[] totalCounts = playOdds.getTotalCount().split("/");
        BigDecimal total = new BigDecimal(totalCounts[winLevel - 1]);
        BigDecimal win = new BigDecimal(playOdds.getWinCount());
        odds = total.divide(win, 10, BigDecimal.ROUND_HALF_UP);
        return odds.multiply(divisor).setScale(3, BigDecimal.ROUND_HALF_UP);
    }

    @Override
    public void deleteCaches(Integer oddsId) {
        if (null != oddsId) {
            redisTemplate.delete(RedisKeys.ODDS_LIST_SETTING_KEY + oddsId);
        }
        redisTemplate.delete(RedisKeys.LOTTERY_PLAY_ODDS_ALL_DATA);
    }

    /**
     * 跟单赔率
     *
     * @param lotteryId
     * @param settingId
     * @param betNumber
     * @return
     */
    public BigDecimal circleOdds(Integer lotteryId, Integer settingId, String betNumber) {
        if (betNumber.contains("@")) {
            betNumber = betNumber.split("@")[1];
        }
        LotteryExample example = new LotteryExample();
        LotteryExample.Criteria criteria = example.createCriteria();
        criteria.andLotteryIdEqualTo(lotteryId);
        Lottery lottery = lotteryMapper.selectOneByExample(example);
        // 获取赔率因子
        SysParameter systemInfo = sysParamService.getByCode(SysParameterEnum.REGISTER_MEMBER_ODDS);
        BigDecimal divisor = new BigDecimal(systemInfo.getParamValue());

        // 获取赔率因子
        Double maxOdds = lottery.getMaxOdds();
        divisor = maxOdds.equals(0D) ? divisor : new BigDecimal(maxOdds);

        // 获取赔率信息
        List<LotteryPlayOdds> oddsList = this.selectOddsListBySettingId(settingId);

        // 判空
        if (CollectionUtils.isEmpty(oddsList)) {
            return BigDecimal.ZERO;
        }
        LotteryPlayOdds odds = null;
        if (oddsList.size() == 1) {
            odds = oddsList.get(0);
        } else {
            TreeMap<Double, LotteryPlayOdds> maxOddstTreeMap = new TreeMap<>();
            for (LotteryPlayOdds playOdds : oddsList) {
                String[] splitBetNum = betNumber.split(",");
                // 普通玩法
                if (splitBetNum.length <= 1 && playOdds.getName().equals(betNumber)) {
                    maxOddstTreeMap.put(
                            Double.parseDouble(playOdds.getTotalCount()) / Double.parseDouble(playOdds.getWinCount()),
                            playOdds);
                } else {// TODO 特殊玩法（投注号码与后台设置内容不符）
                    for (String betContent : splitBetNum) {
                        if (playOdds.getName().equals(betContent)) {
                            // odds = playOdds;
                            maxOddstTreeMap.put(Double.parseDouble(playOdds.getTotalCount())
                                    / Double.parseDouble(playOdds.getWinCount()), playOdds);
                        }
                    }
                }
            }
            // 获取最大odds
            int treeMapSize = maxOddstTreeMap.size();
            if (treeMapSize > 0) {
                odds = maxOddstTreeMap.get(maxOddstTreeMap.lastKey());
            }
        }

        if (odds == null) {
            return BigDecimal.ZERO;
        }

        String totalCount = odds.getTotalCount();
        BigDecimal winCount = BigDecimal.valueOf(Double.valueOf(odds.getWinCount()));
        BigDecimal oddsStr = BigDecimal.ZERO;
        if (totalCount.contains("/")) {
            String[] str = totalCount.split("/");
            oddsStr = BigDecimal.valueOf(Double.valueOf(str[0])).multiply(divisor).divide(winCount, 2,
                    BigDecimal.ROUND_HALF_UP);
        } else {
            oddsStr = BigDecimal.valueOf(Double.valueOf(totalCount)).multiply(divisor).divide(winCount, 2,
                    BigDecimal.ROUND_HALF_UP);
        }
        return oddsStr;
    }

}
