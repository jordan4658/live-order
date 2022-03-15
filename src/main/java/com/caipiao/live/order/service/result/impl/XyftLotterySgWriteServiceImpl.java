package com.caipiao.live.order.service.result.impl;

import com.caipiao.live.order.service.result.XyftLotterySgWriteService;
import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.constant.LotteryResultStatus;
import com.caipiao.live.common.constant.RedisKeys;
import com.caipiao.live.common.mybatis.entity.XyftLotterySg;
import com.caipiao.live.common.mybatis.entity.XyftLotterySgExample;
import com.caipiao.live.common.mybatis.mapper.FtxyftLotterySgMapper;
import com.caipiao.live.common.mybatis.mapper.XyftLotterySgMapper;
import com.caipiao.live.common.mybatis.mapperext.sg.FtXyftLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.XyftLotterySgMapperExt;
import com.caipiao.live.common.util.DateUtils;
import com.caipiao.live.common.util.TimeHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author ShaoMing
 * @version 1.0.0
 * @date 2019/1/14 18:03
 */
@Service
public class XyftLotterySgWriteServiceImpl implements XyftLotterySgWriteService {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private XyftLotterySgMapper xyftLotterySgMapper;
    @Autowired
    private XyftLotterySgMapperExt xyftLotterySgMapperExt;
    @Autowired
    private FtxyftLotterySgMapper ftxyftLotterySgMapper;
    @Autowired
    private FtXyftLotterySgMapperExt ftXyftLotterySgMapperExt;

    @Override
    public XyftLotterySg queryNextSg() {
        XyftLotterySgExample example = new XyftLotterySgExample();
        XyftLotterySgExample.Criteria criteria = example.createCriteria();
        criteria.andIdealTimeGreaterThan(DateUtils.formatDate(new Date(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
        criteria.andOpenStatusEqualTo(LotteryResultStatus.WAIT);
        example.setOrderByClause("ideal_time ASC");
        return xyftLotterySgMapper.selectOneByExample(example);
    }

    @Override
    public XyftLotterySg selectByIssue(String issue) {
        XyftLotterySgExample example = new XyftLotterySgExample();
        XyftLotterySgExample.Criteria criteria = example.createCriteria();
        criteria.andIssueEqualTo(issue);
        return xyftLotterySgMapper.selectOneByExample(example);
    }

    public Integer getXyftOpenCountNum() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("openStatus", LotteryResultStatus.AUTO);
        map.put("paramTime", TimeHelper.date("yyyyMMdd")+"%");
        Integer openCount = xyftLotterySgMapperExt.openCountByExample(map);
        return openCount;
    }

    /**
     * @Title: getXyftpksAlgorithmData
     * @Description: 查询幸运飞艇近期开奖数据
     * @author HANS
     * @date 2019年5月29日下午10:26:51
     */
    public List<XyftLotterySg> getXyftpksAlgorithmData() {
        XyftLotterySgExample example = new XyftLotterySgExample();
        XyftLotterySgExample.Criteria criteria = example.createCriteria();
        criteria.andOpenStatusEqualTo(LotteryResultStatus.AUTO);
        example.setOrderByClause("`ideal_time` DESC");
        example.setOffset(Constants.DEFAULT_INTEGER);
        example.setLimit(Constants.DEFAULT_ALGORITHM_PAGESIZE);
        return xyftLotterySgMapper.selectByExample(example);
    }

    @Override
    public void cacheIssueResultForXyft(String issue, String number) {
//        XyftLotterySg xyftLotterySg = this.selectByIssue(issue);
//        // 缓存到开奖结果
//        String redisKey = RedisKeys.XYFEIT_RESULT_VALUE;
//        Long redisTime = CaipiaoRedisTimeEnum.XYFEIT.getRedisTime();
//        redisTemplate.opsForValue().set(redisKey, xyftLotterySg);
//        // 获取下期信息
//        XyftLotterySg nextXyftLotterySg = queryNextSg();
//        // 缓存到下期信息
//        String nextRedisKey = RedisKeys.XYFEIT_NEXT_VALUE;
//        redisTemplate.opsForValue().set(nextRedisKey, nextXyftLotterySg, redisTime, TimeUnit.MINUTES);
//        // 获取已经开奖数据
//        Integer openCount = this.getXyftOpenCountNum();
//        String openRedisKey = RedisKeys.XYFEIT_OPEN_VALUE;
//        redisTemplate.opsForValue().set(openRedisKey, openCount);
//        // 缓存近期开奖数据
//        List<XyftLotterySg> xyftLotterySgList = this.getXyftpksAlgorithmData();
//        String algorithm = RedisKeys.XYFEIT_ALGORITHM_VALUE;
//        redisTemplate.opsForValue().set(algorithm, xyftLotterySgList, redisTime, TimeUnit.MINUTES);

        List<String> keyList = new ArrayList<>();
        keyList.add(RedisKeys.XYFEIT_RESULT_VALUE);
        keyList.add(RedisKeys.XYFEIT_NEXT_VALUE);
        keyList.add(RedisKeys.XYFEIT_OPEN_VALUE);
        keyList.add(RedisKeys.XYFEIT_ALGORITHM_VALUE);
        redisTemplate.delete(keyList);
    }

//    @Override
//    public FtxyftLotterySg queryNextFtxyftSg() {
//        FtxyftLotterySgExample nextFtxyftExample = new FtxyftLotterySgExample();
//        FtxyftLotterySgExample.Criteria nextFtxyftCriteria = nextFtxyftExample.createCriteria();
//        nextFtxyftCriteria.andIdealTimeGreaterThan(DateUtils.getFullStringZeroSecond(new Date()));
//        nextFtxyftCriteria.andOpenStatusEqualTo(LotteryResultStatus.WAIT);
//        nextFtxyftExample.setOrderByClause("issue ASC");
//        // 查询
//        FtxyftLotterySg nextFtxyftLotterySg = ftxyftLotterySgMapper.selectOneByExample(nextFtxyftExample);
//        return nextFtxyftLotterySg;
//    }

//    @Override
//    public FtxyftLotterySg selectFtxyftByIssue(String issue, String number) {
//        FtxyftLotterySgExample ftxyftExample = new FtxyftLotterySgExample();
//        FtxyftLotterySgExample.Criteria ftxyftCriteria = ftxyftExample.createCriteria();
//        ftxyftCriteria.andIssueEqualTo(issue);
//        // 查询
//        FtxyftLotterySg ftxyftLotterySg = ftxyftLotterySgMapper.selectOneByExample(ftxyftExample);
//
//        if (ftxyftLotterySg != null && StringUtils.isNotEmpty(number) && StringUtils.isEmpty(ftxyftLotterySg.getNumber())) {
//            ftxyftLotterySg.setNumber(number);
//        }
//        return ftxyftLotterySg;
//    }

    public Integer getFtxyftOpenCountNum() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("openStatus", LotteryResultStatus.AUTO);
        map.put("paramTime", TimeHelper.date("yyyyMMdd")+"%");
        Integer openCount = ftXyftLotterySgMapperExt.openCountByExample(map);
        return openCount;
    }

//    @Override
//    public void cacheIssueResultForFtxyft(String issue, String number) {
//        FtxyftLotterySg ftxyftLotterySg = this.selectFtxyftByIssue(issue, number);
//        // 缓存到开奖结果
//        String redisKey = RedisKeys.XYFTFT_RESULT_VALUE;
//        Long redisTime = CaipiaoRedisTimeEnum.XYFTFT.getRedisTime();
//        redisTemplate.opsForValue().set(redisKey, ftxyftLotterySg);
//        // 获取下期信息
//        FtxyftLotterySg nextFtxyftLotterySg = this.queryNextFtxyftSg();
//        // 缓存到下期信息
//        String nextRedisKey = RedisKeys.XYFTFT_NEXT_VALUE;
//        redisTemplate.opsForValue().set(nextRedisKey, nextFtxyftLotterySg, redisTime, TimeUnit.SECONDS);
//        // 获取已经开奖数据
//        Integer openCount = this.getFtxyftOpenCountNum();
//        String openRedisKey = RedisKeys.XYFTFT_OPEN_VALUE;
//        redisTemplate.opsForValue().set(openRedisKey, openCount);
//    }


}
