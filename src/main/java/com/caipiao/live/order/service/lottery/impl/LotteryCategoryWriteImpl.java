package com.caipiao.live.order.service.lottery.impl;

import com.caipiao.live.order.service.lottery.LotteryCategoryWriteService;
import com.caipiao.live.common.constant.RedisKeys;
import com.caipiao.live.common.mybatis.entity.LotteryCategory;
import com.caipiao.live.common.mybatis.entity.LotteryCategoryExample;
import com.caipiao.live.common.mybatis.mapper.LotteryCategoryMapper;
import com.caipiao.live.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LotteryCategoryWriteImpl implements LotteryCategoryWriteService {

    private static final Logger logger = LoggerFactory.getLogger(LotteryCategoryWriteImpl.class);
    @Autowired
    private LotteryCategoryMapper lotteryCategoryMapper;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public List<LotteryCategory> queryAllCategory(String type) {
        List<LotteryCategory> lotteryCategoryList = getLotteryCategorysFromCache();
        if (StringUtils.isNotEmpty(type)) {
            lotteryCategoryList = lotteryCategoryList
                    .parallelStream()
                    .filter(item -> type.equals(item.getType()))
                    .collect(Collectors.toList());
        }
        return lotteryCategoryList;
    }


    private List<LotteryCategory> getLotteryCategorysFromCache() {
        List<LotteryCategory> lotteryCategoryList = (List<LotteryCategory>) redisTemplate.opsForValue().get(RedisKeys.LOTTERY_CATEGORY_LIST_KEY);
        if (CollectionUtils.isEmpty(lotteryCategoryList)) {
            LotteryCategoryExample example = new LotteryCategoryExample();
            example.createCriteria().andIsDeleteEqualTo(false);
            example.setOrderByClause("sort desc");
            lotteryCategoryList = lotteryCategoryMapper.selectByExample(example);
            redisTemplate.opsForValue().set(RedisKeys.LOTTERY_CATEGORY_LIST_KEY, lotteryCategoryList);

            Map<Integer, LotteryCategory> lotteryCategoryMap = new HashMap<>();
            for (LotteryCategory category : lotteryCategoryList) {
                lotteryCategoryMap.put(category.getCategoryId(), category);
            }
            redisTemplate.opsForValue().set(RedisKeys.LOTTERY_CATEGORY_MAP_KEY, lotteryCategoryMap);
        }
        return lotteryCategoryList;
    }

}
