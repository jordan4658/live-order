package com.caipiao.live.order.service.result.impl;

import com.caipiao.live.order.service.result.XjplhcLotterySgWriteService;
import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.constant.LotteryResultStatus;
import com.caipiao.live.common.constant.RedisKeys;
import com.caipiao.live.common.enums.lottery.CaipiaoRedisTimeEnum;
import com.caipiao.live.common.mybatis.entity.XjplhcLotterySg;
import com.caipiao.live.common.mybatis.entity.XjplhcLotterySgExample;
import com.caipiao.live.common.mybatis.mapper.XjplhcLotterySgMapper;
import com.caipiao.live.common.mybatis.mapperext.sg.XjplhcLotterySgMapperExt;
import com.caipiao.live.common.util.DateUtils;
import com.caipiao.live.common.util.TimeHelper;
import com.caipiao.live.common.util.redis.RedisBusinessUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @Date:Created in 10:372020/1/1
 * @Descriotion
 * @Author
 **/
@Service
public class XjplhcLotterySgWriteServiceImpl implements XjplhcLotterySgWriteService {
    @Autowired
    private XjplhcLotterySgMapperExt xjplhcLotterySgMapperExt;
    @Autowired
    private XjplhcLotterySgMapper xjplhcLotterySgMapper;
//    @Autowired
//    private RedisTemplate redisTemplate;

    /* (non Javadoc)
     * @Title: queryNextSg
     * @Description: 获取下期数据
     * @return
     *
     */
    @Override
    public XjplhcLotterySg queryNextSg() {
        XjplhcLotterySgExample xjplhcExample = new XjplhcLotterySgExample();
        XjplhcLotterySgExample.Criteria Criteria = xjplhcExample.createCriteria();
        LocalDateTime localDateTime = LocalDateTime.now();
        Date localDateTimeToDate = DateUtils.getLocalDateTimeToDate(localDateTime);
        Criteria.andIdealTimeGreaterThan(localDateTimeToDate);
        Criteria.andOpenStatusEqualTo(LotteryResultStatus.WAIT);
        xjplhcExample.setOrderByClause("`ideal_time` ASC");
        XjplhcLotterySg nextXjplhcLotterySg = xjplhcLotterySgMapper.selectOneByExample(xjplhcExample);
        return nextXjplhcLotterySg;
    }

    /* (non Javadoc)
     * @Title: selectByIssue
     * @Description: 通过期号获取开奖数据
     *
     */
    @Override
    public XjplhcLotterySg selectByIssue(String issue) {
        XjplhcLotterySgExample xjplhcExample = new XjplhcLotterySgExample();
        XjplhcLotterySgExample.Criteria Criteria = xjplhcExample.createCriteria();
        Criteria.andIssueEqualTo(issue);
        XjplhcLotterySg xjplhcLotterySg = xjplhcLotterySgMapper.selectOneByExample(xjplhcExample);
        return xjplhcLotterySg;
    }

    /**
     * @Title: queryOpenedCount
     * @Description: 已开期数
     * @author
     * @date
     */
    public Integer queryOpenedCount() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("openStatus", LotteryResultStatus.AUTO);
        map.put("paramTime", TimeHelper.date("yyyy-MM-dd")+"%");
        Integer openCount = xjplhcLotterySgMapperExt.openCountByExample(map);
        return openCount;
    }

    /**
     * @Title: getAlgorithmData
     * @Description: 缓存近期数据
     * @author
     * @date
     */
    public List<XjplhcLotterySg> getAlgorithmData() {
        XjplhcLotterySgExample xjplhcExample = new XjplhcLotterySgExample();
        XjplhcLotterySgExample.Criteria Criteria = xjplhcExample.createCriteria();
        Criteria.andOpenStatusEqualTo(LotteryResultStatus.AUTO);
        xjplhcExample.setOrderByClause("`ideal_time` DESC");
        xjplhcExample.setOffset(Constants.DEFAULT_INTEGER);
        xjplhcExample.setLimit(Constants.DEFAULT_ALGORITHM_PAGESIZE);
        return xjplhcLotterySgMapper.selectByExample(xjplhcExample);
    }

    /* (non Javadoc)
     * @Title: cacheIssueResultForQnelhc
     * @Description: 缓存开奖数据
     *
     */
    @Override
    public void cacheIssueResultForQnelhc(String issue, String number) {
        XjplhcLotterySg xjplhcLotterySg = this.selectByIssue(issue);
        // 缓存到开奖结果
        String redisKey = RedisKeys.XJPLHC_RESULT_VALUE;
        Long redisTime = CaipiaoRedisTimeEnum.XJPLHC.getRedisTime();
        RedisBusinessUtil.set(redisKey, xjplhcLotterySg);
        // 获取下期信息
        XjplhcLotterySg nextOnelhcLotterySg = this.queryNextSg();
        // 缓存到下期信息
        String nextRedisKey = RedisKeys.XJPLHC_NEXT_VALUE;
        RedisBusinessUtil.addCacheForValueAndMinutes(nextRedisKey, nextOnelhcLotterySg, redisTime, TimeUnit.MINUTES);
        // 缓存近期200条开奖数据
        List<XjplhcLotterySg> xjplhcLotterySgList = this.getAlgorithmData();
        String algorithm = RedisKeys.XJPLHC_ALGORITHM_VALUE;
        RedisBusinessUtil.addCacheForValueAndMinutes(algorithm, xjplhcLotterySgList, redisTime, TimeUnit.MINUTES);
    }
}
