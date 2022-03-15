package com.caipiao.live.order.service.result.impl;

import com.caipiao.live.common.mybatis.mapperext.sg.BjpksLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.FivebjpksLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.FtJspksLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.JsbjpksLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.TenbjpksLotterySgMapperExt;
import com.caipiao.live.order.service.result.BjpksLotterySgWriteService;
import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.constant.LotteryResultStatus;
import com.caipiao.live.common.constant.RedisKeys;
import com.caipiao.live.common.model.dto.lottery.LotterySgModel;
import com.caipiao.live.common.mybatis.entity.*;
import com.caipiao.live.common.mybatis.mapper.*;
import com.caipiao.live.common.util.DateUtils;
import com.caipiao.live.common.util.TimeHelper;
import com.caipiao.live.order.utils.GetHttpInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * 北京PK10赛果业务实现类
 *
 * @author lzy
 * @create 2018-07-30 15:34
 **/
@Service
public class BjpksLotterySgWriteServiceImpl implements BjpksLotterySgWriteService {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private BjpksLotterySgMapper bjpksLotterySgMapper;
    @Autowired
    private BjpksLotterySgMapperExt bjpksLotterySgMapperExt;
    @Autowired
    private TenbjpksLotterySgMapper tenbjpksLotterySgMapper;
    @Autowired
    private TenbjpksLotterySgMapperExt tenbjpksLotterySgMapperExt;
    @Autowired
    private FivebjpksLotterySgMapper fivebjpksLotterySgMapper;
    @Autowired
    private FivebjpksLotterySgMapperExt fivebjpksLotterySgMapperExt;
    @Autowired
    private JsbjpksLotterySgMapper jsbjpksLotterySgMapper;
    @Autowired
    private JsbjpksLotterySgMapperExt jsbjpksLotterySgMapperExt;
    @Autowired
    private FtjspksLotterySgMapper ftjspksLotterySgMapper;
    @Autowired
    private FtJspksLotterySgMapperExt ftJspksLotterySgMapperExt;


    @Override
    public void addSg() {
        String lotteryName = "bjpks";
        // 查询第三方接口最后开奖结果
        List<LotterySgModel> sgModels = GetHttpInterface.getCpkSg(lotteryName, 1);
        // 判空
        if (CollectionUtils.isEmpty(sgModels)) {
            return;
        }
        // 获取最后开奖结果
        LotterySgModel model = sgModels.get(0);

        // 根据期号查询该数据在自己数据库中是否已入库
        BjpksLotterySgExample example = new BjpksLotterySgExample();
        BjpksLotterySgExample.Criteria criteria = example.createCriteria();
        criteria.andIssueEqualTo(model.getIssue());
        BjpksLotterySg bjpksLotterySg = bjpksLotterySgMapper.selectOneByExample(example);

        // 如果没入库,则入库
        if (bjpksLotterySg == null) {
            bjpksLotterySg = new BjpksLotterySg();
            bjpksLotterySg.setIssue(model.getIssue());
            bjpksLotterySg.setNumber(model.getSg());
            bjpksLotterySg.setTime(TimeHelper.date());
            this.bjpksLotterySgMapper.insertSelective(bjpksLotterySg);
        }

    }


    @Override
    public BjpksLotterySg queryNextSg() {
        BjpksLotterySgExample example = new BjpksLotterySgExample();
        BjpksLotterySgExample.Criteria criteria = example.createCriteria();
        criteria.andIdealTimeGreaterThan(DateUtils.formatDate(new Date(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
        criteria.andOpenStatusEqualTo(LotteryResultStatus.WAIT);
        example.setOrderByClause("issue ASC");
        return bjpksLotterySgMapper.selectOneByExample(example);
    }

    @Override
    public BjpksLotterySg selectByIssue(String issue) {
        BjpksLotterySgExample example = new BjpksLotterySgExample();
        BjpksLotterySgExample.Criteria criteria = example.createCriteria();
        criteria.andIssueEqualTo(issue);
        return bjpksLotterySgMapper.selectOneByExample(example);
    }

    public Integer getOpenCountNum() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("openStatus", LotteryResultStatus.AUTO);
        map.put("paramTime", TimeHelper.date("yyyy-MM-dd")+"%");
        Integer openCount = bjpksLotterySgMapperExt.openCountByExample(map);
        return openCount;
    }

    public List<BjpksLotterySg> selectOpenIssueList() {
        BjpksLotterySgExample example = new BjpksLotterySgExample();
        BjpksLotterySgExample.Criteria criteria = example.createCriteria();
        criteria.andOpenStatusEqualTo(LotteryResultStatus.AUTO);
        example.setOrderByClause("`ideal_time` DESC");
        example.setOffset(Constants.DEFAULT_INTEGER);
        example.setLimit(Constants.DEFAULT_ALGORITHM_PAGESIZE);
        return bjpksLotterySgMapper.selectByExample(example);
    }

    @Override
    public void cacheIssueResultForBjpks(String issue, String number) {
        List<String> keyList = new ArrayList<>();
        keyList.add(RedisKeys.BJPKS_RESULT_VALUE);
        keyList.add(RedisKeys.BJPKS_NEXT_VALUE);
        keyList.add(RedisKeys.BJPKS_OPEN_VALUE);
        keyList.add(RedisKeys.BJPKS_ALGORITHM_VALUE);
        redisTemplate.delete(keyList);
    }

    @Override
    public TenbjpksLotterySg queryTenbjpksNextSg() {
        TenbjpksLotterySgExample example = new TenbjpksLotterySgExample();
        TenbjpksLotterySgExample.Criteria bjpksCriteria = example.createCriteria();
        bjpksCriteria.andIdealTimeGreaterThan(DateUtils.formatDate(new Date(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
        bjpksCriteria.andOpenStatusEqualTo(LotteryResultStatus.WAIT);
        example.setOrderByClause("issue ASC");
        TenbjpksLotterySg bjpksLotterySg = tenbjpksLotterySgMapper.selectOneByExample(example);
        return bjpksLotterySg;
    }

//    @Override
//    public TenbjpksLotterySg selectTenbjpksByIssue(String issue) {
//        TenbjpksLotterySgExample example = new TenbjpksLotterySgExample();
//        TenbjpksLotterySgExample.Criteria bjpksCriteria = example.createCriteria();
//        bjpksCriteria.andIssueEqualTo(Integer.valueOf(issue));
//        TenbjpksLotterySg bjpksLotterySg = tenbjpksLotterySgMapper.selectOneByExample(example);
//        return bjpksLotterySg;
//    }
//
//    public Integer getTenbjpksOpenCountNum() {
//        Map<String, Object> map = new HashMap<String, Object>();
//        map.put("openStatus", LotteryResultStatus.AUTO);
//        map.put("paramTime", TimeHelper.date("yyyy-MM-dd"));
//        Integer openCount = tenbjpksLotterySgMapperExt.openCountByExample(map);
//        return openCount;
//    }

//    public List<TenbjpksLotterySg> selectTenbjpksSg() {
//        TenbjpksLotterySgExample example = new TenbjpksLotterySgExample();
//        TenbjpksLotterySgExample.Criteria tenpksCriteria = example.createCriteria();
//        tenpksCriteria.andOpenStatusEqualTo(LotteryResultStatus.AUTO);
//        example.setOrderByClause("`ideal_time` DESC");
//        example.setOffset(Constants.DEFAULT_INTEGER);
//        example.setLimit(Constants.DEFAULT_ALGORITHM_PAGESIZE);
//        List<TenbjpksLotterySg> bjpksLotterySgList = tenbjpksLotterySgMapper.selectByExample(example);
//        return bjpksLotterySgList;
//    }

    @Override
    public void cacheIssueResultForTenbjpks(String issue, String number) {
        List<String> keyList = new ArrayList<>();
        keyList.add(RedisKeys.TENPKS_RESULT_VALUE);
        keyList.add(RedisKeys.TENPKS_NEXT_VALUE);
        keyList.add(RedisKeys.TENPKS_OPEN_VALUE);
        keyList.add(RedisKeys.TENPKS_ALGORITHM_VALUE);
        redisTemplate.delete(keyList);
    }

//    @Override
//    public FivebjpksLotterySg queryFivebjpksNextSg() {
//        FivebjpksLotterySgExample nextExample = new FivebjpksLotterySgExample();
//        FivebjpksLotterySgExample.Criteria nextFivebjpksCriteria = nextExample.createCriteria();
//        nextFivebjpksCriteria.andIdealTimeGreaterThan(DateUtils.getFullStringZeroSecond(new Date()));
//        nextFivebjpksCriteria.andOpenStatusEqualTo(LotteryResultStatus.WAIT);
//        nextExample.setOrderByClause("issue ASC");
//        FivebjpksLotterySg nextFivebjpksLotterySg = this.fivebjpksLotterySgMapper.selectOneByExample(nextExample);
//        return nextFivebjpksLotterySg;
//    }

    @Override
    public FivebjpksLotterySg selectFivebjpksByIssue(String issue) {
        FivebjpksLotterySgExample example = new FivebjpksLotterySgExample();
        FivebjpksLotterySgExample.Criteria fivebjpksCriteria = example.createCriteria();
        fivebjpksCriteria.andIssueEqualTo(issue);
        FivebjpksLotterySg fivebjpksLotterySg = fivebjpksLotterySgMapper.selectOneByExample(example);
        return fivebjpksLotterySg;
    }

    public List<FivebjpksLotterySg> selectFivebjpksByIssue() {
        FivebjpksLotterySgExample example = new FivebjpksLotterySgExample();
        FivebjpksLotterySgExample.Criteria fivebjpksCriteria = example.createCriteria();
        fivebjpksCriteria.andOpenStatusEqualTo(LotteryResultStatus.AUTO);
        example.setOrderByClause("`ideal_time` DESC");
        example.setOffset(Constants.DEFAULT_INTEGER);
        example.setLimit(Constants.DEFAULT_ALGORITHM_PAGESIZE);
        List<FivebjpksLotterySg> fivebjpksLotterySgList = fivebjpksLotterySgMapper.selectByExample(example);
        return fivebjpksLotterySgList;
    }

    @Override
    public void cacheIssueResultForFivebjpks(String issue, String number) {
        List<String> keyList = new ArrayList<>();
        keyList.add(RedisKeys.FIVEPKS_RESULT_VALUE);
        keyList.add(RedisKeys.FIVEPKS_NEXT_VALUE);
        keyList.add(RedisKeys.FIVEPKS_OPEN_VALUE);
        keyList.add(RedisKeys.FIVEPKS_ALGORITHM_VALUE);
        redisTemplate.delete(keyList);
    }

    @Override
    public JsbjpksLotterySg queryJsbjpksNextSg() {
        JsbjpksLotterySgExample example = new JsbjpksLotterySgExample();
        JsbjpksLotterySgExample.Criteria jsbjpksCriteria = example.createCriteria();
        jsbjpksCriteria.andIdealTimeGreaterThan(DateUtils.getFullStringZeroSecond(new Date()));
        jsbjpksCriteria.andOpenStatusEqualTo(LotteryResultStatus.WAIT);
        example.setOrderByClause("ideal_time ASC");
        JsbjpksLotterySg nextJsbjpksLotterySg = jsbjpksLotterySgMapper.selectOneByExample(example);
        return nextJsbjpksLotterySg;
    }

    @Override
    public JsbjpksLotterySg selectJsbjpksByIssue(String issue) {
        JsbjpksLotterySgExample example = new JsbjpksLotterySgExample();
        JsbjpksLotterySgExample.Criteria jsbjpksCriteria = example.createCriteria();
        jsbjpksCriteria.andIssueEqualTo(issue);
        JsbjpksLotterySg jsbjpksLotterySg = jsbjpksLotterySgMapper.selectOneByExample(example);
        return jsbjpksLotterySg;
    }

    public Integer getJsbjpksOpenCountNum() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("openStatus", LotteryResultStatus.AUTO);
        map.put("paramTime", TimeHelper.date("yyyy-MM-dd")+"%");
        Integer openCount = jsbjpksLotterySgMapperExt.openCountByExample(map);
        return openCount;
    }

    /**
     * @return JsbjpksLotterySg
     * @Title: getJsbjpksAlgorithmData
     * @Description:缓存近期开奖数据
     * @author HANS
     * @date 2019年5月13日下午10:31:53
     */
    public List<JsbjpksLotterySg> getJsbjpksAlgorithmData() {
        JsbjpksLotterySgExample example = new JsbjpksLotterySgExample();
        JsbjpksLotterySgExample.Criteria jsbjpksCriteria = example.createCriteria();
        jsbjpksCriteria.andOpenStatusEqualTo(LotteryResultStatus.AUTO);
        example.setOrderByClause("`ideal_time` DESC");
        example.setOffset(Constants.DEFAULT_INTEGER);
        example.setLimit(Constants.DEFAULT_ALGORITHM_PAGESIZE);
        return jsbjpksLotterySgMapper.selectByExample(example);
    }

    @Override
    public void cacheIssueResultForJsbjpks(String issue, String number) {
//        JsbjpksLotterySg jsbjpksLotterySg = this.selectJsbjpksByIssue(issue);
//        // 缓存到开奖结果
//        String redisKey = RedisKeys.JSPKS_RESULT_VALUE;
//        Long redisTime = CaipiaoRedisTimeEnum.JSPKS.getRedisTime();
//        redisTemplate.opsForValue().set(redisKey, jsbjpksLotterySg);
//        // 获取下期信息
//        JsbjpksLotterySg nextJsbjpksLotterySg = this.queryJsbjpksNextSg();
//        // 缓存到下期信息
//        String nextRedisKey = RedisKeys.JSPKS_NEXT_VALUE;
//        redisTemplate.opsForValue().set(nextRedisKey, nextJsbjpksLotterySg, redisTime, TimeUnit.MINUTES);
//        // 获取已经开奖数据
//        Integer openCount = this.getJsbjpksOpenCountNum();
//        String openRedisKey = RedisKeys.JSPKS_OPEN_VALUE;
//        redisTemplate.opsForValue().set(openRedisKey, openCount);
//        // 缓存近期开奖数据
//        List<JsbjpksLotterySg> jsbjpksLotterySgList = this.getJsbjpksAlgorithmData();
//        String algorithm = RedisKeys.JSPKS_ALGORITHM_VALUE;
//        redisTemplate.opsForValue().set(algorithm, jsbjpksLotterySgList, redisTime, TimeUnit.MINUTES);
        List<String> keyList = new ArrayList<>();
        keyList.add(RedisKeys.JSPKS_RESULT_VALUE);
        keyList.add(RedisKeys.JSPKS_NEXT_VALUE);
        keyList.add(RedisKeys.JSPKS_OPEN_VALUE);
        keyList.add(RedisKeys.JSPKS_ALGORITHM_VALUE);
        redisTemplate.delete(keyList);
    }

//    @Override
//    public FtjspksLotterySg queryFtjsbjpksNextSg() {
//        FtjspksLotterySgExample ftjspksExample = new FtjspksLotterySgExample();
//        FtjspksLotterySgExample.Criteria ftjspksCriteria = ftjspksExample.createCriteria();
//        ftjspksCriteria.andIdealTimeGreaterThan(DateUtils.getFullStringZeroSecond(new Date()));
//        ftjspksCriteria.andOpenStatusEqualTo(LotteryResultStatus.WAIT);
//        ftjspksExample.setOrderByClause("ideal_time ASC");
//        FtjspksLotterySg nextFtjspksLotterySg = ftjspksLotterySgMapper.selectOneByExample(ftjspksExample);
//        return nextFtjspksLotterySg;
//    }
//
//    @Override
//    public FtjspksLotterySg selectFtjspksByIssue(String issue, String number) {
//        FtjspksLotterySgExample ftjspksExample = new FtjspksLotterySgExample();
//        FtjspksLotterySgExample.Criteria ftjspkCriteria = ftjspksExample.createCriteria();
//        ftjspkCriteria.andIssueEqualTo(issue);
//        FtjspksLotterySg ftjspksLotterySg = ftjspksLotterySgMapper.selectOneByExample(ftjspksExample);
//
//        if (ftjspksLotterySg != null && StringUtils.isNotEmpty(number) && StringUtils.isEmpty(ftjspksLotterySg.getNumber())) {
//            ftjspksLotterySg.setNumber(number);
//        }
//        return ftjspksLotterySg;
//    }

//    public Integer selectFtjspksOpenCount() {
//        Map<String, Object> map = new HashMap<String, Object>();
//        map.put("openStatus", LotteryResultStatus.AUTO);
//        map.put("paramTime", TimeHelper.date("yyyy-MM-dd"));
//        Integer openCount = ftJspksLotterySgMapperExt.openCountByExample(map);
//        return openCount;
//    }

//    @Override
//    public void cacheIssueResultForFtjspks(String issue, String number) {
//        FtjspksLotterySg ftjspksLotterySg = this.selectFtjspksByIssue(issue, number);
//        // 缓存到开奖结果
//        String redisKey = RedisKeys.JSPKFT_RESULT_VALUE;
//        Long redisTime = CaipiaoRedisTimeEnum.JSPKFT.getRedisTime();
//        redisTemplate.opsForValue().set(redisKey, ftjspksLotterySg);
//        // 获取下期信息
//        FtjspksLotterySg nextFtjspksLotterySg = this.queryFtjsbjpksNextSg();
//        // 缓存到下期信息
//        String nextRedisKey = RedisKeys.JSPKFT_NEXT_VALUE;
//        redisTemplate.opsForValue().set(nextRedisKey, nextFtjspksLotterySg, redisTime, TimeUnit.SECONDS);
//        // 获取已经开奖数据
//        Integer openCount = this.selectFtjspksOpenCount();
//        String openRedisKey = RedisKeys.JSPKFT_OPEN_VALUE;
//        redisTemplate.opsForValue().set(openRedisKey, openCount);
//    }

}
