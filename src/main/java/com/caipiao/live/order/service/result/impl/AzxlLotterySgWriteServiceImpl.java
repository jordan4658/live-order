package com.caipiao.live.order.service.result.impl;

import com.caipiao.live.order.service.result.AzxlLotterySgWriteService;
import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.constant.LotteryResultStatus;
import com.caipiao.live.common.constant.RedisKeys;
import com.caipiao.live.common.enums.lottery.CaipiaoRedisTimeEnum;
import com.caipiao.live.common.mybatis.entity.AusactLotterySg;
import com.caipiao.live.common.mybatis.entity.AusactLotterySgExample;
import com.caipiao.live.common.mybatis.entity.AuspksLotterySg;
import com.caipiao.live.common.mybatis.entity.AuspksLotterySgExample;
import com.caipiao.live.common.mybatis.mapper.AusactLotterySgMapper;
import com.caipiao.live.common.mybatis.mapper.AuspksLotterySgMapper;
import com.caipiao.live.common.mybatis.mapperext.sg.AusactLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.AuspksLotterySgMapperExt;
import com.caipiao.live.common.util.DateUtils;
import com.caipiao.live.common.util.StringUtils;
import com.caipiao.live.common.util.TimeHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName: AzxlLotterySgWriteServiceImpl
 * @Description: 澳洲系列处理服务
 * @author: HANS
 * @date: 2019年4月30日 下午1:33:43
 */
@Service("cptAzxlLotterySgWriteService")
public class AzxlLotterySgWriteServiceImpl implements AzxlLotterySgWriteService {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private AusactLotterySgMapper ausactLotterySgMapper;
    @Autowired
    private AusactLotterySgMapperExt ausactLotterySgMapperExt;
    @Autowired
    private AuspksLotterySgMapper AuspksLotterySgMapper;
    @Autowired
    private AuspksLotterySgMapperExt auspksLotterySgMapperExt;


    /**
     * @return
     * @Title: queryAusactLotteryNextSg
     * @Description: 获取下期数据
     */
    @Override
    public AusactLotterySg queryAusactLotteryNextSg() {
        AusactLotterySgExample ausactExample = new AusactLotterySgExample();
        AusactLotterySgExample.Criteria ausactCriteria = ausactExample.createCriteria();
        ausactCriteria.andIdealTimeGreaterThan(DateUtils.getFullStringZeroSecond(new Date()));
        ausactCriteria.andOpenStatusEqualTo(LotteryResultStatus.WAIT);
        ausactExample.setOrderByClause("ideal_time ASC");
        AusactLotterySg nextAusactLotterySg = ausactLotterySgMapper.selectOneByExample(ausactExample);
        return nextAusactLotterySg;
    }
	

    /**
     * @return
     * @Title: selectAusactLotteryByIssue
     * @Description: 通过期号查询数据
     */
    @Override
    public AusactLotterySg selectAusactLotteryByIssue(String issue, String number) {
        AusactLotterySgExample ausactExample = new AusactLotterySgExample();
        AusactLotterySgExample.Criteria ausactCriteria = ausactExample.createCriteria();
        ausactCriteria.andIssueEqualTo(issue);
        AusactLotterySg ausactLotterySg = ausactLotterySgMapper.selectOneByExample(ausactExample);

        if (ausactLotterySg != null && StringUtils.isNotEmpty(number) && StringUtils.isEmpty(ausactLotterySg.getNumber())) {
            ausactLotterySg.setNumber(number);
        }
        return ausactLotterySg;
    }

    /**
     * @return Integer
     * @Title: selectAusactOpenCount
     * @Description: 查询当天开奖数量
     */
    public Integer selectAusactOpenCount() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("openStatus", LotteryResultStatus.AUTO);
        map.put("paramTime", TimeHelper.date("yyyy-MM-dd")+"%");
        Integer openCount = ausactLotterySgMapperExt.openCountByExample(map);
        return openCount;
    }

    /**
     * @param issue
     * @param number
     * @Title: cacheIssueResultForAusact
     * @Description: 缓存数据
     */
    @Override
    public void cacheIssueResultForAusact(String issue, String number) {
        AusactLotterySg ausactLotterySg = this.selectAusactLotteryByIssue(issue, number);
        // 缓存到开奖结果
        String redisKey = RedisKeys.AUSACT_RESULT_VALUE;
        redisTemplate.opsForValue().set(redisKey, ausactLotterySg);
        // 获取下期信息,并缓存
        Long redisTime = CaipiaoRedisTimeEnum.AUSACT.getRedisTime();
        AusactLotterySg nextAusactLotterySg = this.queryAusactLotteryNextSg();
        String nextRedisKey = RedisKeys.AUSACT_NEXT_VALUE;
        redisTemplate.opsForValue().set(nextRedisKey, nextAusactLotterySg, redisTime, TimeUnit.MINUTES);
        // 获取已经开奖数据
        Integer openCount = this.selectAusactOpenCount();
        String openRedisKey = RedisKeys.AUSACT_OPEN_VALUE;
        redisTemplate.opsForValue().set(openRedisKey, openCount);
        // 缓存近期200条开奖数据
        List<AusactLotterySg> ausactLotterySgList = this.getAlgorithmData();
        String algorithm = RedisKeys.AUSACT_ALGORITHM_VALUE;
        redisTemplate.opsForValue().set(algorithm, ausactLotterySgList, redisTime, TimeUnit.MINUTES);
    }

    /**
     * 查询近期200条开奖数据
     */
    @Override
    public List<AusactLotterySg> getAlgorithmData() {
        AusactLotterySgExample ausactExample = new AusactLotterySgExample();
        AusactLotterySgExample.Criteria ausactCriteria = ausactExample.createCriteria();
        ausactCriteria.andOpenStatusEqualTo(LotteryResultStatus.AUTO);
        ausactExample.setOrderByClause("ideal_time DESC");
        ausactExample.setOffset(Constants.DEFAULT_INTEGER);
        ausactExample.setLimit(Constants.DEFAULT_ALGORITHM_PAGESIZE);
        List<AusactLotterySg> ausactLotterySgList = ausactLotterySgMapper.selectByExample(ausactExample);
        return ausactLotterySgList;
    }


    @Override
    public AuspksLotterySg queryAuspksLotterySgNextSg() {
        AuspksLotterySgExample auspksNextExample = new AuspksLotterySgExample();
        AuspksLotterySgExample.Criteria auspksNextCriteria = auspksNextExample.createCriteria();
        auspksNextCriteria.andIdealTimeGreaterThan(DateUtils.getFullStringZeroSecond(new Date()));
        auspksNextCriteria.andOpenStatusEqualTo(LotteryResultStatus.WAIT);
        auspksNextExample.setOrderByClause("ideal_time ASC");
        AuspksLotterySg auspksNextLotterySg = AuspksLotterySgMapper.selectOneByExample(auspksNextExample);
        return auspksNextLotterySg;
    }


    @Override
    public AuspksLotterySg selectAuspksLotterySgByIssue(String issue, String number) {
        AuspksLotterySgExample auspksExample = new AuspksLotterySgExample();
        AuspksLotterySgExample.Criteria auspksCriteria = auspksExample.createCriteria();
        auspksCriteria.andIssueEqualTo(issue);
        AuspksLotterySg auspksLotterySg = AuspksLotterySgMapper.selectOneByExample(auspksExample);

        if (auspksLotterySg != null && StringUtils.isNotEmpty(number) && StringUtils.isEmpty(auspksLotterySg.getNumber())) {
            auspksLotterySg.setNumber(number);
        }
        return auspksLotterySg;
    }

    /**
     * @return Integer
     * @Title: selectAusactOpenCount
     * @Description: 查询当天开奖数量
     */
    public Integer selectAuspksOpenCount() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("openStatus", LotteryResultStatus.AUTO);
        map.put("paramTime", TimeHelper.date("yyyy-MM-dd")+"%");
        Integer openCount = auspksLotterySgMapperExt.openCountByExample(map);
        return openCount;
    }


    @Override
    public void cacheIssueResultForAuspks(String issue, String number) {
        AuspksLotterySg auspksLotterySg = this.selectAuspksLotterySgByIssue(issue, number);
        // 缓存到开奖结果
        String redisKey = RedisKeys.AUSPKS_RESULT_VALUE;
        redisTemplate.opsForValue().set(redisKey, auspksLotterySg);
        // 获取下期信息,并缓存
        Long redisTime = CaipiaoRedisTimeEnum.AUSPKS.getRedisTime();
        AuspksLotterySg auspksNextLotterySg = this.queryAuspksLotterySgNextSg();
        String nextRedisKey = RedisKeys.AUSPKS_NEXT_VALUE;
        redisTemplate.opsForValue().set(nextRedisKey, auspksNextLotterySg, redisTime, TimeUnit.MINUTES);
        // 获取已经开奖数据
        Integer openCount = this.selectAuspksOpenCount();
        String openRedisKey = RedisKeys.AUSPKS_OPEN_VALUE;
        redisTemplate.opsForValue().set(openRedisKey, openCount);
    }
}
