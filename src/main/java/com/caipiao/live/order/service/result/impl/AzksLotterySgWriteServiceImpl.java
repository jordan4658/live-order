package com.caipiao.live.order.service.result.impl;
import com.caipiao.live.order.service.result.AzksLotterySgWriteService;
import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.constant.LotteryResultStatus;
import com.caipiao.live.common.constant.RedisKeys;
import com.caipiao.live.common.enums.lottery.CaipiaoRedisTimeEnum;
import com.caipiao.live.common.mybatis.entity.AzksLotterySg;
import com.caipiao.live.common.mybatis.entity.AzksLotterySgExample;
import com.caipiao.live.common.mybatis.mapper.AzksLotterySgMapper;
import com.caipiao.live.common.mybatis.mapperext.sg.AzksLotterySgMapperExt;
import com.caipiao.live.common.util.DateUtils;
import com.caipiao.live.common.util.TimeHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class AzksLotterySgWriteServiceImpl implements AzksLotterySgWriteService {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    AzksLotterySgMapper azksLotterySgMapper;
    @Autowired
    AzksLotterySgMapperExt azksLotterySgMapperExt;

    @Override
    public void cacheIssueResultForAzKs(String issue, String number) {
        AzksLotterySg azksLotterySg = this.selectByIssue(issue);
        // 缓存到开奖结果
        String redisKey = RedisKeys.AZKS_RESULT_VALUE;
        Long redisTime = CaipiaoRedisTimeEnum.AZKS.getRedisTime();
        redisTemplate.opsForValue().set(redisKey, azksLotterySg);
        // 获取下期信息
        AzksLotterySg nextAzksLotterySg = this.queryNextSg();
        // 缓存到下期信息
        String nextRedisKey = RedisKeys.AZKS_NEXT_VALUE;
        redisTemplate.opsForValue().set(nextRedisKey, nextAzksLotterySg, redisTime, TimeUnit.MINUTES);
        // 缓存近期开奖数据
        List<AzksLotterySg> azksLotterySgList = this.getAzksAlgorithmData();
        String algorithm = RedisKeys.AZKS_ALGORITHM_VALUE;
        redisTemplate.opsForValue().set(algorithm, azksLotterySgList, redisTime, TimeUnit.MINUTES);

    }

    @Override
    public AzksLotterySg selectByIssue(String issue) {
        AzksLotterySgExample example = new AzksLotterySgExample();
        AzksLotterySgExample.Criteria criteria = example.createCriteria();
        criteria.andIssueEqualTo(issue);
        return azksLotterySgMapper.selectOneByExample(example);
    }

    @Override
    public AzksLotterySg queryNextSg() {
        AzksLotterySgExample example = new AzksLotterySgExample();
        AzksLotterySgExample.Criteria criteria = example.createCriteria();
        LocalDateTime localDateTime = LocalDateTime.now();
        Date localDateTimeToDate = DateUtils.getLocalDateTimeToDate(localDateTime);
        criteria.andIdealTimeGreaterThan(localDateTimeToDate);
        criteria.andOpenStatusEqualTo(LotteryResultStatus.WAIT);
        example.setOrderByClause("ideal_time ASC");
        return azksLotterySgMapper.selectOneByExample(example);
    }

    /*获取已开奖的数量*/
    @Override
    public Integer getAzksOpenCountNum() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("openStatus", LotteryResultStatus.AUTO);
        map.put("paramTime", TimeHelper.date("yyyyMMdd")+"%");
        Integer openCount = azksLotterySgMapperExt.openCountByExample(map);
        return openCount;
    }


    /**
     * @Title: getAzksAlgorithmData
     * @Description: 查询澳洲快三近期开奖数据
     * @author
     * @date 20191207
     */
    @Override
    public List<AzksLotterySg> getAzksAlgorithmData() {
        AzksLotterySgExample example = new AzksLotterySgExample();
        AzksLotterySgExample.Criteria criteria = example.createCriteria();
        criteria.andOpenStatusEqualTo(LotteryResultStatus.AUTO);
        example.setOrderByClause("`ideal_time` DESC");
        example.setOffset(Constants.DEFAULT_INTEGER);
        example.setLimit(Constants.DEFAULT_ALGORITHM_PAGESIZE);
        return azksLotterySgMapper.selectByExample(example);
    }
}
