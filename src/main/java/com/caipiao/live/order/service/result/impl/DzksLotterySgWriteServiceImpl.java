package com.caipiao.live.order.service.result.impl;
import com.caipiao.live.order.service.result.DzksLotterySgWriteService;
import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.constant.LotteryResultStatus;
import com.caipiao.live.common.constant.RedisKeys;
import com.caipiao.live.common.enums.lottery.CaipiaoRedisTimeEnum;
import com.caipiao.live.common.mybatis.entity.DzksLotterySg;
import com.caipiao.live.common.mybatis.entity.DzksLotterySgExample;
import com.caipiao.live.common.mybatis.mapper.DzksLotterySgMapper;
import com.caipiao.live.common.mybatis.mapperext.sg.DzksLotterySgMapperExt;
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

/**
 * @Date:Created in 20:002019/12/11
 * @Descriotion
 * @Author
 **/
@Service
public class DzksLotterySgWriteServiceImpl implements DzksLotterySgWriteService {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    DzksLotterySgMapper dzksLotterySgMapper;
    @Autowired
    DzksLotterySgMapperExt dzksLotterySgMapperExt;

    @Override
    public void cacheIssueResultForDzKs(String issue, String number) {
        DzksLotterySg dzksLotterySg = this.selectByIssue(issue);
        // 缓存到开奖结果
        String redisKey = RedisKeys.DZKS_RESULT_VALUE;
        Long redisTime = CaipiaoRedisTimeEnum.AZKS.getRedisTime();
        redisTemplate.opsForValue().set(redisKey, dzksLotterySg);
        // 获取下期信息
        DzksLotterySg nextDzksLotterySg = this.queryNextSg();
        // 缓存到下期信息
        String nextRedisKey = RedisKeys.DZKS_NEXT_VALUE;
        redisTemplate.opsForValue().set(nextRedisKey, nextDzksLotterySg, redisTime, TimeUnit.MINUTES);
        // 缓存近期开奖数据
        List<DzksLotterySg> dzksLotterySgList = this.getDzksAlgorithmData();
        String algorithm = RedisKeys.DZKS_ALGORITHM_VALUE;
        redisTemplate.opsForValue().set(algorithm, dzksLotterySgList, redisTime, TimeUnit.MINUTES);

    }

    @Override
    public DzksLotterySg selectByIssue(String issue) {
        DzksLotterySgExample example = new DzksLotterySgExample();
        DzksLotterySgExample.Criteria criteria = example.createCriteria();
        criteria.andIssueEqualTo(issue);
        return dzksLotterySgMapper.selectOneByExample(example);
    }

    @Override
    public DzksLotterySg queryNextSg() {
        DzksLotterySgExample example = new DzksLotterySgExample();
        DzksLotterySgExample.Criteria criteria = example.createCriteria();
        LocalDateTime localDateTime = LocalDateTime.now();
        Date localDateTimeToDate = DateUtils.getLocalDateTimeToDate(localDateTime);
        criteria.andIdealTimeGreaterThan(localDateTimeToDate);
        criteria.andOpenStatusEqualTo(LotteryResultStatus.WAIT);
        example.setOrderByClause("ideal_time ASC");
        return dzksLotterySgMapper.selectOneByExample(example);
    }

    /*获取已开奖的数量*/
    @Override
    public Integer getDzksOpenCountNum() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("openStatus", LotteryResultStatus.AUTO);
        map.put("paramTime", TimeHelper.date("yyyyMMdd")+"%");
        Integer openCount = dzksLotterySgMapperExt.openCountByExample(map);
        return openCount;
    }


    /**
     * @Title: getDzksAlgorithmData
     * @Description: 查询德州快三近期开奖数据
     * @author
     * @date 20191207
     */
    @Override
    public List<DzksLotterySg> getDzksAlgorithmData() {
        DzksLotterySgExample example = new DzksLotterySgExample();
        DzksLotterySgExample.Criteria criteria = example.createCriteria();
        criteria.andOpenStatusEqualTo(LotteryResultStatus.AUTO);
        example.setOrderByClause("`ideal_time` DESC");
        example.setOffset(Constants.DEFAULT_INTEGER);
        example.setLimit(Constants.DEFAULT_ALGORITHM_PAGESIZE);
        return dzksLotterySgMapper.selectByExample(example);
    }
}

