package com.caipiao.live.order.service.lottery.impl;
import com.caipiao.live.order.service.lottery.LotteryPlayWriteService;
import com.caipiao.live.common.constant.RedisKeys;
import com.caipiao.live.common.mybatis.entity.*;
import com.caipiao.live.common.mybatis.mapper.LotteryPlayMapper;
import com.caipiao.live.common.mybatis.mapper.LotteryPlayOddsMapper;
import com.caipiao.live.common.mybatis.mapper.LotteryPlaySettingMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LotteryPlayWriteServiceImpl implements LotteryPlayWriteService {

    @Autowired
    private LotteryPlayMapper lotteryPlayMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private LotteryPlaySettingMapper lotteryPlaySettingMapper;
    @Autowired
    private LotteryPlayOddsMapper lotteryPlayOddsMapper;

    @Override
    public Map<Integer, LotteryPlay> selectPlayByIds(List<Integer> categoryIds) {
        Map<Integer, LotteryPlay> playMap = new HashMap<>();
        // 判空
        if (CollectionUtils.isEmpty(categoryIds)) {
            return playMap;
        }

        Map<Integer, LotteryPlay> lotteryPlayMap = selectPlayMap();
        for (Integer categoryId : categoryIds) {
            playMap.put(categoryId, lotteryPlayMap.get(categoryId));
        }

        return playMap;
    }

    @Override
    public LotteryPlay selectPlayById(Integer id) {
        if (id == null) {
            return null;
        }
        return selectPlayMap().get(id);
    }

    @Override
    public List<LotteryPlay> selectPlayList() {
        List<LotteryPlay> list = (List<LotteryPlay>) redisTemplate.opsForValue().get(RedisKeys.LOTTERY_PLAY_LIST_KEY);
        if (CollectionUtils.isEmpty(list)) {
            LotteryPlayExample playExample = new LotteryPlayExample();
            LotteryPlayExample.Criteria playCriteria = playExample.createCriteria();
            playCriteria.andIsDeleteEqualTo(false);
            list = lotteryPlayMapper.selectByExample(playExample);
            redisTemplate.opsForValue().set(RedisKeys.LOTTERY_PLAY_LIST_KEY, list);
        }
        return list;
    }

    @Override
    public Map<Integer, LotteryPlay> selectPlayMap() {
        Map<Integer, LotteryPlay> map = new HashMap<>();
        // 获取所有玩法集合信息
        List<LotteryPlay> list = this.selectPlayList();
        if (CollectionUtils.isEmpty(list)) {
            return map;
        }
        for (LotteryPlay play : list) {
            map.put(play.getPlayTagId(), play);
        }
        return map;
    }


    /**
     * 玩法
     *
     * @param lotteryPlay
     * @return
     */
    private int saveLotteryPlay(LotteryPlay lotteryPlay, List<LotteryPlay> updatePlayList) {
        LotteryPlay checkPlay = null;
        if (lotteryPlay.getPlayTagId() != null) {
            LotteryPlayExample example = new LotteryPlayExample();
            LotteryPlayExample.Criteria criteria = example.createCriteria();
            criteria.andPlayTagIdEqualTo(lotteryPlay.getPlayTagId());
            criteria.andIsDeleteEqualTo(false);
            checkPlay = lotteryPlayMapper.selectOneByExample(example);
            if (checkPlay != null) {
                lotteryPlay.setId(checkPlay.getId());
                lotteryPlayMapper.updateByPrimaryKeySelective(lotteryPlay);
                return lotteryPlay.getId();
            } else {
                lotteryPlayMapper.insertSelective(lotteryPlay);
                return lotteryPlay.getId();
            }
        } else {
            LotteryPlayExample example = new LotteryPlayExample();
            LotteryPlayExample.Criteria criteria = example.createCriteria();
            criteria.andLotteryIdEqualTo(lotteryPlay.getLotteryId());
            criteria.andLevelEqualTo(lotteryPlay.getLevel());
            criteria.andNameEqualTo(lotteryPlay.getName());
            criteria.andIsDeleteEqualTo(false);
            checkPlay = lotteryPlayMapper.selectOneByExample(example);
            if (checkPlay == null) {
                lotteryPlayMapper.insertSelective(lotteryPlay);
                return lotteryPlay.getId();
            }
            return checkPlay.getId();
        }
    }

    /**
     * 玩法规则
     *
     * @param setting
     * @return
     */
    private int insertOrUpdatePlaySetting(LotteryPlaySetting setting, List<LotteryPlaySetting> updatePlaySettingList) {
        LotteryPlaySetting checkPlaysSetting = null;
        LotteryPlaySettingExample example = new LotteryPlaySettingExample();
        LotteryPlaySettingExample.Criteria criteria = example.createCriteria();
        criteria.andPlayTagIdEqualTo(setting.getPlayTagId());
        criteria.andIsDeleteEqualTo(false);
        checkPlaysSetting = lotteryPlaySettingMapper.selectOneByExample(example);
        if (checkPlaysSetting != null) {
            setting.setId(checkPlaysSetting.getId());
            lotteryPlaySettingMapper.updateByPrimaryKeySelective(setting);
            //updatePlaySettingList.add(setting);
            return setting.getId();
        } else {
            lotteryPlaySettingMapper.insertSelective(setting);
            return setting.getId();
        }
    }

    /**
     * 赔率
     *
     * @param odds
     * @param easyImportFlag
     */
    private void insertOrUpdatePlayOdds(LotteryPlayOdds odds, Integer easyImportFlag,
                                        List<LotteryPlayOdds> insertLotteryPlayOddsList,
                                        List<LotteryPlayOdds> updateLotteryPlayOddsList) {
        LotteryPlayOdds checkPlayOdds = null;
        LotteryPlayOddsExample example = new LotteryPlayOddsExample();
        LotteryPlayOddsExample.Criteria criteria = example.createCriteria();
        criteria.andEasyImportFlagEqualTo(easyImportFlag);
        criteria.andNameEqualTo(odds.getName());
        criteria.andIsDeleteEqualTo(false);
        checkPlayOdds = lotteryPlayOddsMapper.selectOneByExample(example);
        if (checkPlayOdds != null) {
            odds.setId(checkPlayOdds.getId());
            lotteryPlayOddsMapper.updateByPrimaryKeySelective(odds);
            // nsertLotteryPlayOddsList.add(odds);
        } else {
            lotteryPlayOddsMapper.insertSelective(odds);
            // updateLotteryPlayOddsList.add(odds);
        }
    }
}
