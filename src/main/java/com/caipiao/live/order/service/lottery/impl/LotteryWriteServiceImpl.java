package com.caipiao.live.order.service.lottery.impl;

import com.caipiao.live.order.service.lottery.LotteryCategoryWriteService;
import com.caipiao.live.order.service.lottery.LotteryWriteService;
import com.caipiao.live.common.constant.RedisKeys;
import com.caipiao.live.common.mybatis.entity.Lottery;
import com.caipiao.live.common.mybatis.entity.LotteryCategory;
import com.caipiao.live.common.mybatis.entity.LotteryExample;
import com.caipiao.live.common.mybatis.mapper.*;
import com.caipiao.live.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LotteryWriteServiceImpl implements LotteryWriteService {
    @Autowired
    private LotteryMapper lotteryMapper;
    @Autowired
    private LotteryPlayMapper lotteryPlayMapper;
    @Autowired
    private CqsscLotterySgMapper cqsscLotterySgMapper;
    @Autowired
    private BjpksLotterySgMapper bjpksLotterySgMapper;
    @Autowired
    private XjsscLotterySgMapper xjsscLotterySgMapper;
    @Autowired
    private TxffcLotterySgMapper txffcLotterySgMapper;
    @Autowired
    private XyftLotterySgMapper xyftLotterySgMapper;
    @Autowired
    private PceggLotterySgMapper pceggLotterySgMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private LotteryCategoryWriteService lotteryCategoryWriteService;


    @Override
    public Lottery selectLotteryById(Integer lotteryId) {
        if (null == lotteryId || lotteryId <= 0) {
            return null;
        }
        this.queryAllLotteryFromCache();
        Map<Integer, Lottery> lotteryMap = (Map<Integer, Lottery>) redisTemplate.opsForValue().get(RedisKeys.LOTTERY_MAP_KEY);

        if (lotteryMap == null) {
            lotteryMap = new HashMap<>();
            LotteryExample example = new LotteryExample();
            example.createCriteria().andIsDeleteEqualTo(false);
            example.setOrderByClause("sort desc");
            List<Lottery> list = lotteryMapper.selectByExample(example);

            for (Lottery lottery : list) {
                lotteryMap.put(lottery.getId(), lottery);
                lotteryMap.put(lottery.getLotteryId(), lottery);
            }
            redisTemplate.opsForValue().set(RedisKeys.LOTTERY_MAP_KEY, lotteryMap);
        }
        Lottery lottery = lotteryMap.get(lotteryId);
        return null == lottery ? lotteryMap.get(String.valueOf(lotteryId)) : lottery;
    }

    @Override
    public List<Lottery> selectLotteryList(String categoryType) {
        List<Lottery> list = this.queryAllLotteryFromCache();
        if (StringUtils.isNotEmpty(categoryType)) {
            List<LotteryCategory> categories = lotteryCategoryWriteService.queryAllCategory(categoryType);
            List<Integer> categoryIdList = categories.stream().map(item -> item.getCategoryId()).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(categoryIdList)) {
                list = list.parallelStream()
                        .filter(item -> categoryIdList.contains(item.getCategoryId()))
                        .collect(Collectors.toList());
            }
        }
        return list;
    }

    @Override
    public Map<Integer, Lottery> selectLotteryMap(String categoryType) {
        Map<Integer, Lottery> map = new HashMap<>();

        List<Lottery> lotteries = this.selectLotteryList(categoryType);
        if (CollectionUtils.isEmpty(lotteries)) {
            return map;
        }

        for (Lottery lottery : lotteries) {
            map.put(lottery.getLotteryId(), lottery);
        }
        return map;
    }

    @Override
    public List<Lottery> queryAllLotteryFromCache() {
        List<Lottery> list = (List<Lottery>) redisTemplate.opsForValue().get(RedisKeys.LOTTERY_LIST_KEY);
        if (CollectionUtils.isEmpty(list)) {
            LotteryExample example = new LotteryExample();
            example.createCriteria().andIsDeleteEqualTo(false);
            example.setOrderByClause("sort desc");
            list = lotteryMapper.selectByExample(example);
            redisTemplate.opsForValue().set(RedisKeys.LOTTERY_LIST_KEY, list);

            Map<Integer, Lottery> lotteryMap = new HashMap<>();
            for (Lottery lottery : list) {
                lotteryMap.put(lottery.getId(), lottery);
                lotteryMap.put(lottery.getLotteryId(), lottery);
            }
            redisTemplate.opsForValue().set(RedisKeys.LOTTERY_MAP_KEY, lotteryMap);
        }
        return list;
    }

}
