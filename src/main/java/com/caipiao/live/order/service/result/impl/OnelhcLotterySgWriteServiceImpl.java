package com.caipiao.live.order.service.result.impl;

import com.caipiao.live.order.service.result.OnelhcLotterySgWriteService;
import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.constant.LotteryResultStatus;
import com.caipiao.live.common.constant.RedisKeys;
import com.caipiao.live.common.mybatis.entity.OnelhcLotterySg;
import com.caipiao.live.common.mybatis.entity.OnelhcLotterySgExample;
import com.caipiao.live.common.mybatis.mapper.OnelhcLotterySgMapper;
import com.caipiao.live.common.mybatis.mapperext.sg.OnelhcLotterySgMapperExt;
import com.caipiao.live.common.util.DateUtils;
import com.caipiao.live.common.util.TimeHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @ClassName: onelhcLotterySgWriteServiceImpl
 * @Description: 一分六合彩服务类
 * @author: HANS
 * @date: 2019年5月14日 下午10:24:49
 */
@Service
public class OnelhcLotterySgWriteServiceImpl implements OnelhcLotterySgWriteService {

    @Autowired
    private OnelhcLotterySgMapper onelhcLotterySgMapper;
    @Autowired
    private OnelhcLotterySgMapperExt onelhcLotterySgMapperExt;
    @Autowired
    private RedisTemplate redisTemplate;

    /* (non Javadoc)
     * @Title: queryNextSg
     * @Description: 获取下期数据
     * @return
     * @see com.caipiao.business.service.result.onelhcLotterySgWriteService#queryNextSg()
     */
    @Override
    public OnelhcLotterySg queryNextSg() {
        OnelhcLotterySgExample onelhcExample = new OnelhcLotterySgExample();
        OnelhcLotterySgExample.Criteria onelhcCriteria = onelhcExample.createCriteria();
        onelhcCriteria.andIdealTimeGreaterThan(DateUtils.formatDate(new Date(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
        onelhcCriteria.andOpenStatusEqualTo(LotteryResultStatus.WAIT);
        onelhcExample.setOrderByClause("`ideal_time` ASC");
        OnelhcLotterySg nextOnelhcLotterySg = onelhcLotterySgMapper.selectOneByExample(onelhcExample);
        return nextOnelhcLotterySg;
    }

    /* (non Javadoc)
     * @Title: selectByIssue
     * @Description: 通过期号获取开奖数据
     * @see com.caipiao.business.service.result.onelhcLotterySgWriteService#selectByIssue(java.lang.String)
     */
    @Override
    public OnelhcLotterySg selectByIssue(String issue) {
        OnelhcLotterySgExample onelhcExample = new OnelhcLotterySgExample();
        OnelhcLotterySgExample.Criteria onelhcCriteria = onelhcExample.createCriteria();
        onelhcCriteria.andIssueEqualTo(issue);
        OnelhcLotterySg onelhcLotterySg = onelhcLotterySgMapper.selectOneByExample(onelhcExample);
        return onelhcLotterySg;
    }


    /**
     * @Title: queryOpenedCount
     * @Description: 已开期数
     * @author HANS
     * @date 2019年5月15日上午10:49:17
     */
    public Integer queryOpenedCount() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("openStatus", LotteryResultStatus.AUTO);
        map.put("paramTime", TimeHelper.date("yyyy-MM-dd")+"%");
        Integer openCount = onelhcLotterySgMapperExt.openCountByExample(map);
        return openCount;
    }

    /**
     * @Title: getAlgorithmData
     * @Description: 缓存近期数据
     * @author HANS
     * @date 2019年5月15日上午10:58:26
     */
    public List<OnelhcLotterySg> getAlgorithmData() {
        OnelhcLotterySgExample onelhcExample = new OnelhcLotterySgExample();
        OnelhcLotterySgExample.Criteria onelhcCriteria = onelhcExample.createCriteria();
        onelhcCriteria.andOpenStatusEqualTo(LotteryResultStatus.AUTO);
        onelhcExample.setOrderByClause("`ideal_time` DESC");
        onelhcExample.setOffset(Constants.DEFAULT_INTEGER);
        onelhcExample.setLimit(Constants.DEFAULT_ALGORITHM_PAGESIZE);
        return onelhcLotterySgMapper.selectByExample(onelhcExample);
    }

    /* (non Javadoc)
     * @Title: cacheIssueResultForQnelhc
     * @Description: 缓存开奖数据
     * @see com.caipiao.business.service.result.onelhcLotterySgWriteService#cacheIssueResultForQnelhc(java.lang.String, java.lang.String)
     */
    @Override
    public void cacheIssueResultForQnelhc(String issue, String number) {
//        OnelhcLotterySg onelhcLotterySg = this.selectByIssue(issue);
//        // 缓存到开奖结果
//        String redisKey = RedisKeys.ONELHC_RESULT_VALUE;
//        Long redisTime = CaipiaoRedisTimeEnum.ONELHC.getRedisTime();
//        redisTemplate.opsForValue().set(redisKey, onelhcLotterySg);
//        // 获取下期信息
//        OnelhcLotterySg nextOnelhcLotterySg = this.queryNextSg();
//        // 缓存到下期信息
//        String nextRedisKey = RedisKeys.ONELHC_NEXT_VALUE;
//        redisTemplate.opsForValue().set(nextRedisKey, nextOnelhcLotterySg, redisTime, TimeUnit.MINUTES);
//        // 获取已经开奖数据
//        Integer openCount = this.queryOpenedCount();
//        String openRedisKey = RedisKeys.ONELHC_OPEN_VALUE;
//        redisTemplate.opsForValue().set(openRedisKey, openCount);
//        // 缓存近期200条开奖数据
//        List<OnelhcLotterySg> onelhcLotterySgList = this.getAlgorithmData();
//        String algorithm = RedisKeys.ONELHC_ALGORITHM_VALUE;
//        redisTemplate.opsForValue().set(algorithm, onelhcLotterySgList, redisTime, TimeUnit.MINUTES);
        List<String> keyList = new ArrayList<>();
        keyList.add(RedisKeys.ONELHC_RESULT_VALUE);
        keyList.add(RedisKeys.ONELHC_NEXT_VALUE);
        keyList.add(RedisKeys.ONELHC_OPEN_VALUE);
        keyList.add(RedisKeys.ONELHC_ALGORITHM_VALUE);
        redisTemplate.delete(keyList);
    }


}
