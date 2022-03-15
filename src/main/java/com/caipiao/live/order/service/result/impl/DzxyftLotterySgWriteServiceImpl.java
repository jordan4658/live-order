package com.caipiao.live.order.service.result.impl;
import com.caipiao.live.order.service.result.DzxyftLotterySgWriteService;
import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.constant.LotteryResultStatus;
import com.caipiao.live.common.constant.RedisKeys;
import com.caipiao.live.common.enums.lottery.CaipiaoRedisTimeEnum;
import com.caipiao.live.common.mybatis.entity.DzxyftLotterySg;
import com.caipiao.live.common.mybatis.entity.DzxyftLotterySgExample;
import com.caipiao.live.common.mybatis.mapper.DzxyftLotterySgMapper;
import com.caipiao.live.common.mybatis.mapperext.sg.DzxyftLotterySgMapperExt;
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
 * @Date:Created in 22:192019/12/12
 * @Descriotion
 * @Author
 **/
@Service
public class DzxyftLotterySgWriteServiceImpl implements DzxyftLotterySgWriteService {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private DzxyftLotterySgMapper dzxyftLotterySgMapper;
    @Autowired
    private DzxyftLotterySgMapperExt dzxyftLotterySgMapperExt;

    @Override
    public DzxyftLotterySg selectByIssue(String issue) {
        DzxyftLotterySgExample example = new DzxyftLotterySgExample();
        DzxyftLotterySgExample.Criteria criteria = example.createCriteria();
        criteria.andIssueEqualTo(issue);
        return dzxyftLotterySgMapper.selectOneByExample(example);
    }

    @Override
    public DzxyftLotterySg queryNextSg() {
        DzxyftLotterySgExample example = new DzxyftLotterySgExample();
        DzxyftLotterySgExample.Criteria criteria = example.createCriteria();
        LocalDateTime localDateTime = LocalDateTime.now();
        Date localDateTimeToDate = DateUtils.getLocalDateTimeToDate(localDateTime);
        criteria.andIdealTimeGreaterThan(localDateTimeToDate);
        criteria.andOpenStatusEqualTo(LotteryResultStatus.WAIT);
        example.setOrderByClause("ideal_time ASC");
        return dzxyftLotterySgMapper.selectOneByExample(example);
    }

    public Integer getDzxyftOpenCountNum() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("openStatus", LotteryResultStatus.AUTO);
        map.put("paramTime", TimeHelper.date("yyyyMMdd")+"%");
        Integer openCount = dzxyftLotterySgMapperExt.openCountByExample(map);
        return openCount;
    }

    /**
     * @Title: getXyftpksAlgorithmData
     * @Description: 查询德州幸运飞艇近期开奖数据
     */
    public List<DzxyftLotterySg> getDzxyftpksAlgorithmData() {
        DzxyftLotterySgExample example = new DzxyftLotterySgExample();
        DzxyftLotterySgExample.Criteria criteria = example.createCriteria();
        criteria.andOpenStatusEqualTo(LotteryResultStatus.AUTO);
        example.setOrderByClause("`ideal_time` DESC");
        example.setOffset(Constants.DEFAULT_INTEGER);
        example.setLimit(Constants.DEFAULT_ALGORITHM_PAGESIZE);
        return dzxyftLotterySgMapper.selectByExample(example);
    }

    /**
     * @Title: getXyftpksAlgorithmData
     * @Description: 查询德州幸运飞艇近期开奖数据
     * @author
     * @date
     */
    public List<DzxyftLotterySg> getXyftpksAlgorithmData() {
        DzxyftLotterySgExample example = new DzxyftLotterySgExample();
        DzxyftLotterySgExample.Criteria criteria = example.createCriteria();
        criteria.andOpenStatusEqualTo(LotteryResultStatus.AUTO);
        example.setOrderByClause("`ideal_time` DESC");
        example.setOffset(Constants.DEFAULT_INTEGER);
        example.setLimit(Constants.DEFAULT_ALGORITHM_PAGESIZE);
        return dzxyftLotterySgMapper.selectByExample(example);
    }

    @Override
    public void cacheIssueResultForDzxyft(String issue, String number) {
        DzxyftLotterySg xyftLotterySg = this.selectByIssue(issue);
        // 缓存到开奖结果
        String redisKey = RedisKeys.DZXYFEIT_RESULT_VALUE;
        Long redisTime = CaipiaoRedisTimeEnum.DZXYFEIT.getRedisTime();
        redisTemplate.opsForValue().set(redisKey, xyftLotterySg);
        // 获取下期信息
        DzxyftLotterySg nextDzxyftLotterySg = queryNextSg();
        // 缓存到下期信息
        String nextRedisKey = RedisKeys.DZXYFEIT_NEXT_VALUE;
        redisTemplate.opsForValue().set(nextRedisKey, nextDzxyftLotterySg, redisTime, TimeUnit.MINUTES);
        // 缓存近期开奖数据
        List<DzxyftLotterySg> xyftLotterySgList = this.getDzxyftpksAlgorithmData();
        String algorithm = RedisKeys.DZXYFEIT_ALGORITHM_VALUE;
        redisTemplate.opsForValue().set(algorithm, xyftLotterySgList, redisTime, TimeUnit.MINUTES);
    }
}
