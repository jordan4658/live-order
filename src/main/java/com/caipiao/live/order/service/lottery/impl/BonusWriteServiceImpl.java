package com.caipiao.live.order.service.lottery.impl;

import com.caipiao.live.order.service.lottery.BonusWriteService;
import com.caipiao.live.common.constant.RedisKeys;
import com.caipiao.live.common.model.common.ResultInfo;
import com.caipiao.live.common.mybatis.entity.*;
import com.caipiao.live.common.mybatis.mapper.BetRestrictMapper;
import com.caipiao.live.common.mybatis.mapper.BonusMapper;
import com.caipiao.live.common.mybatis.mapper.LotteryPlayMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BonusWriteServiceImpl implements BonusWriteService {

    @Autowired
    private BonusMapper bonusMapper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private BetRestrictMapper betRestrictMapper;
    @Autowired
    private LotteryPlayMapper lotteryPlayMapper;

    @Override
    public Boolean doEditBonus(Bonus bonus) {
        int count;
        BonusExample bonusExample = new BonusExample();
        BonusExample.Criteria criteria = bonusExample.createCriteria();
        criteria.andPlayIdEqualTo(bonus.getPlayId());
        criteria.andCategoryIdEqualTo(bonus.getCategoryId());
        Bonus thisBonus = bonusMapper.selectOneByExample(bonusExample);
        if (thisBonus == null) {
            count = bonusMapper.insertSelective(bonus);
            // redisTemplate.delete(RedisKeys.BONUS_LIST_KEY);
        } else {
            bonus.setId(thisBonus.getId());
            count = bonusMapper.updateByPrimaryKeySelective(bonus);
            // redisTemplate.delete(RedisKeys.BONUS_LIST_KEY);
        }
        return count > 0;
    }

    @Override
    public Bonus queryBonusByPlayId(Integer playId) {
        // 获取投注限制
        Bonus bonus = (Bonus) redisTemplate.opsForValue().get(RedisKeys.BONUS_KEY + playId);
        if (null == bonus) {
            BonusExample bonusExample = new BonusExample();
            BonusExample.Criteria criteria = bonusExample.createCriteria();
            criteria.andPlayIdEqualTo(playId);
            criteria.andIsDeleteEqualTo(false);
            bonus = bonusMapper.selectOneByExample(bonusExample);
            redisTemplate.opsForValue().set(RedisKeys.BONUS_KEY + playId, bonus);
        }
        return bonus;
    }

    @Override
    public ResultInfo<String> savaBetRestrict(Integer lotteryId, Integer playId, Integer maxMoney) {
        LotteryPlay lotteryPlay = new LotteryPlay();
        Integer playTagId = 0;
        if (playId != 0) {
            lotteryPlay = lotteryPlayMapper.selectByPrimaryKey(playId);
            if (null == lotteryPlay) {
                ResultInfo.error();
            } else {
                playTagId = lotteryPlay.getPlayTagId();
            }
        }
        BetRestrictExample betRestrictExample = new BetRestrictExample();
        BetRestrictExample.Criteria criteria = betRestrictExample.createCriteria();
        criteria.andLotteryIdEqualTo(lotteryId);
        criteria.andPlayTagIdEqualTo(playTagId);

        BetRestrict betRestrict = betRestrictMapper.selectOneByExample(betRestrictExample);
        if (null == betRestrict) {
            BetRestrict restrict = new BetRestrict();
            restrict.setLotteryId(lotteryId);
            restrict.setPlayTagId(playTagId);
            restrict.setMaxMoney(new BigDecimal(maxMoney));
            restrict.setCrateTime(new Date());
            int num = betRestrictMapper.insertSelective(restrict);
            if (num <= 0) {
                ResultInfo.error("失败");
            }
        } else {
            if (null == maxMoney) {
                betRestrict.setMaxMoney(BigDecimal.ZERO);
            } else {
                betRestrict.setMaxMoney(new BigDecimal(maxMoney));

            }
            int sum = betRestrictMapper.updateByPrimaryKeySelective(betRestrict);
            if (sum <= 0) {
                ResultInfo.error("失败");
            }
        }
        this.getBonusMap();
        return ResultInfo.ok("成功");
    }

    private void getBonusMap() {
        BetRestrictExample betRestrictExample = new BetRestrictExample();
        List<BetRestrict> betRestrictList = betRestrictMapper.selectByExample(betRestrictExample);
        Map<Integer, BetRestrict> bonusMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(betRestrictList)) {
            for (BetRestrict betRestrict : betRestrictList) {
                bonusMap.put(betRestrict.getPlayTagId(), betRestrict);
            }
            redisTemplate.opsForValue().set(RedisKeys.RESTRICT_LIST_KEY, bonusMap);
        }
    }
}
