package com.caipiao.live.order.service.result.impl;

import com.caipiao.live.order.service.result.XjsscLotterySgWriteService;
import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.constant.LotteryResultStatus;
import com.caipiao.live.common.constant.RedisKeys;
import com.caipiao.live.common.enums.lottery.CaipiaoRedisTimeEnum;
import com.caipiao.live.common.mybatis.entity.*;
import com.caipiao.live.common.mybatis.mapper.*;
import com.caipiao.live.common.mybatis.mapperext.sg.TxffcLotterySgMapperExt;
import com.caipiao.live.common.util.DateUtils;
import com.caipiao.live.common.util.TimeHelper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author ShaoMing
 * @version 1.0.0
 * @date 2019/1/14 17:52
 */
@Service
public class XjsscLotterySgWriteServiceImpl implements XjsscLotterySgWriteService {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private CqsscLotterySgMapper cqsscLotterySgMapper;
    @Autowired
    private XjsscLotterySgMapper xjsscLotterySgMapper;
    @Autowired
    private TjsscLotterySgMapper tjsscLotterySgMapper;
    @Autowired
    private TensscLotterySgMapper tensscLotterySgMapper;
    @Autowired
    private FivesscLotterySgMapper fivesscLotterySgMapper;
    @Autowired
    private JssscLotterySgMapper jssscLotterySgMapper;
    @Autowired
    private TxffcLotterySgMapper txffcLotterySgMapper;
    @Autowired
    private TxffcLotterySgMapperExt txffcLotterySgMapperExt;
    @Autowired
    private FtjssscLotterySgMapper ftjssscLotterySgMapper;

    @Override
    public XjsscLotterySg queryNextSg() {
        XjsscLotterySgExample example = new XjsscLotterySgExample();
        XjsscLotterySgExample.Criteria criteria = example.createCriteria();
        criteria.andIdealTimeGreaterThan(DateUtils.formatDate(new Date(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
        criteria.andOpenStatusEqualTo(LotteryResultStatus.WAIT);
        example.setOrderByClause("ideal_time ASC");
        return xjsscLotterySgMapper.selectOneByExample(example);
    }

    @Override
    public XjsscLotterySg selectByIssue(String issue) {
        XjsscLotterySgExample example = new XjsscLotterySgExample();
        XjsscLotterySgExample.Criteria criteria = example.createCriteria();
        criteria.andIssueEqualTo(issue);
        return xjsscLotterySgMapper.selectOneByExample(example);
    }

    public List<XjsscLotterySg> getxjsscAlgorithmData() {
        XjsscLotterySgExample xjsscExample = new XjsscLotterySgExample();
        XjsscLotterySgExample.Criteria xjsscCriteria = xjsscExample.createCriteria();
        xjsscCriteria.andOpenStatusEqualTo(LotteryResultStatus.AUTO);
        xjsscExample.setOrderByClause("`ideal_time` DESC");
        xjsscExample.setOffset(Constants.DEFAULT_INTEGER);
        xjsscExample.setLimit(Constants.DEFAULT_ALGORITHM_PAGESIZE);
        return xjsscLotterySgMapper.selectByExample(xjsscExample);
    }

    @Override
    public void cacheIssueResult(String issue, String number) {
        // 获取到当期开奖结果
        XjsscLotterySg xjsscLotterySg = this.selectByIssue(issue);
        // 缓存到开奖结果
        String redisKey = RedisKeys.XJSSC_RESULT_VALUE;
        Long redisTime = CaipiaoRedisTimeEnum.XJSSC.getRedisTime();
        redisTemplate.opsForValue().set(redisKey, xjsscLotterySg);
        // 获取下期信息
        XjsscLotterySg nextXjsscLotterySg = this.queryNextSg();
        // 缓存到下期信息
        String nextRedisKey = RedisKeys.XJSSC_NEXT_VALUE;
        redisTemplate.opsForValue().set(nextRedisKey, nextXjsscLotterySg, redisTime, TimeUnit.MINUTES);
        // 缓存近期信息
        List<XjsscLotterySg> xjsscLotterySgList = this.getxjsscAlgorithmData();
        String algorithm = RedisKeys.XJSSC_ALGORITHM_VALUE;
        redisTemplate.opsForValue().set(algorithm, xjsscLotterySgList, redisTime, TimeUnit.MINUTES);
    }

    @Override
    public TjsscLotterySg queryTjsscNextSg() {
        TjsscLotterySgExample example = new TjsscLotterySgExample();
        TjsscLotterySgExample.Criteria tqsscCriteria = example.createCriteria();
        tqsscCriteria.andIdealTimeGreaterThan(DateUtils.formatDate(new Date(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
        tqsscCriteria.andOpenStatusEqualTo(LotteryResultStatus.WAIT);
        example.setOrderByClause("ideal_time ASC");
        TjsscLotterySg tjsscLotterySg = tjsscLotterySgMapper.selectOneByExample(example);
        return tjsscLotterySg;
    }

    @Override
    public TjsscLotterySg selectXjsscByIssue(String issue) {
        TjsscLotterySgExample example = new TjsscLotterySgExample();
        TjsscLotterySgExample.Criteria tqsscCriteria = example.createCriteria();
        tqsscCriteria.andIssueEqualTo(issue);
        TjsscLotterySg tjsscLotterySg = this.tjsscLotterySgMapper.selectOneByExample(example);
        return tjsscLotterySg;
    }

    /**
     * @Title: getTjsscAlgorithmData
     * @Description: 获取近期天津时时彩开奖数据
     * @author HANS
     * @date 2019年5月17日上午10:53:14
     */
    public List<TjsscLotterySg> getTjsscAlgorithmData() {
        TjsscLotterySgExample tjExample = new TjsscLotterySgExample();
        TjsscLotterySgExample.Criteria tjsscCriteria = tjExample.createCriteria();
        tjsscCriteria.andOpenStatusEqualTo(LotteryResultStatus.AUTO);
        tjExample.setOrderByClause("`ideal_time` DESC");
        tjExample.setOffset(Constants.DEFAULT_INTEGER);
        tjExample.setLimit(Constants.DEFAULT_ALGORITHM_PAGESIZE);
        List<TjsscLotterySg> tjsscLotterySgList = tjsscLotterySgMapper.selectByExample(tjExample);
        return tjsscLotterySgList;
    }

    @Override
    public void cacheIssueResultForXjssc(String issue, String number) {
        // 获取到当期开奖结果
        TjsscLotterySg tjsscLotterySg = this.selectXjsscByIssue(issue);
        // 缓存到开奖结果
        String redisKey = RedisKeys.TJSSC_RESULT_VALUE;
        Long redisTime = CaipiaoRedisTimeEnum.TJSSC.getRedisTime();
        redisTemplate.opsForValue().set(redisKey, tjsscLotterySg);
        // 获取下期信息
        TjsscLotterySg nextTjsscLotterySg = this.queryTjsscNextSg();
        // 缓存到下期信息
        String nextRedisKey = RedisKeys.TJSSC_NEXT_VALUE;
        redisTemplate.opsForValue().set(nextRedisKey, nextTjsscLotterySg, redisTime, TimeUnit.MINUTES);
        // 缓存近期信息
        List<TjsscLotterySg> tjsscLotterySgList = this.getTjsscAlgorithmData();
        String algorithm = RedisKeys.TJSSC_ALGORITHM_VALUE;
        redisTemplate.opsForValue().set(algorithm, tjsscLotterySgList, redisTime, TimeUnit.MINUTES);
    }

    @Override
    public TensscLotterySg queryTensscNextSg() {
        TensscLotterySgExample nextExample = new TensscLotterySgExample();
        TensscLotterySgExample.Criteria nextTensscCriteria = nextExample.createCriteria();
        nextTensscCriteria.andIdealTimeGreaterThan(DateUtils.formatDate(new Date(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
        nextTensscCriteria.andOpenStatusEqualTo(LotteryResultStatus.WAIT);
        nextExample.setOrderByClause("ideal_time ASC");
        TensscLotterySg next_TensscLotterySg = tensscLotterySgMapper.selectOneByExample(nextExample);
        return next_TensscLotterySg;
    }

    @Override
    public TensscLotterySg selectTensscByIssue(String issue, String number) {
        TensscLotterySgExample example = new TensscLotterySgExample();
        TensscLotterySgExample.Criteria tensscCriteria = example.createCriteria();
        tensscCriteria.andIssueEqualTo(issue);
        TensscLotterySg tensscLotterySg = this.tensscLotterySgMapper.selectOneByExample(example);

        if (tensscLotterySg != null && StringUtils.isNotEmpty(number) && StringUtils.isEmpty(tensscLotterySg.getNumber())) {
            tensscLotterySg.setNumber(number);
        }
        return tensscLotterySg;
    }

    /**
     * @Title: getAlgorithmData
     * @Description: 缓存近期数据
     * @author HANS
     * @date 2019年5月15日上午10:58:26
     */
    public List<TensscLotterySg> getTensscAlgorithmData() {
        TensscLotterySgExample tensscExample = new TensscLotterySgExample();
        TensscLotterySgExample.Criteria tensscCriteria = tensscExample.createCriteria();
        tensscCriteria.andOpenStatusEqualTo(LotteryResultStatus.AUTO);
        tensscExample.setOrderByClause("`ideal_time` DESC");
        tensscExample.setOffset(Constants.DEFAULT_INTEGER);
        tensscExample.setLimit(Constants.DEFAULT_ALGORITHM_PAGESIZE);
        List<TensscLotterySg> tensscLotterySqList = tensscLotterySgMapper.selectByExample(tensscExample);
        return tensscLotterySqList;
    }

    @Override
    public void cacheIssueResultForTenssc(String issue, String number) {
        // 获取到当期开奖结果
        TensscLotterySg tensscLotterySg = this.selectTensscByIssue(issue, number);
        // 缓存到开奖结果
        String redisKey = RedisKeys.TENSSC_RESULT_VALUE;
        Long redisTime = CaipiaoRedisTimeEnum.TENSSC.getRedisTime();
        redisTemplate.opsForValue().set(redisKey, tensscLotterySg);
        // 获取下期信息
        TensscLotterySg nextTensscLotterySg = this.queryTensscNextSg();
        // 缓存到下期信息
        String nextRedisKey = RedisKeys.TENSSC_NEXT_VALUE;
        redisTemplate.opsForValue().set(nextRedisKey, nextTensscLotterySg, redisTime, TimeUnit.MINUTES);
        // 缓存近期数据
        List<TensscLotterySg> tensscLotterySqList = this.getTensscAlgorithmData();
        String algorithm = RedisKeys.TENSSC_ALGORITHM_VALUE;
        redisTemplate.opsForValue().set(algorithm, tensscLotterySqList, redisTime, TimeUnit.MINUTES);
    }

    @Override
    public FivesscLotterySg queryFivesscNextSg() {
        FivesscLotterySgExample nextExample = new FivesscLotterySgExample();
        FivesscLotterySgExample.Criteria nextFivesscCriteria = nextExample.createCriteria();
        nextFivesscCriteria.andIdealTimeGreaterThan(DateUtils.formatDate(new Date(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
        nextFivesscCriteria.andOpenStatusEqualTo(LotteryResultStatus.WAIT);
        nextExample.setOrderByClause("ideal_time ASC");
        FivesscLotterySg nextFivesscLotterySg = fivesscLotterySgMapper.selectOneByExample(nextExample);
        return nextFivesscLotterySg;
    }

    @Override
    public FivesscLotterySg selectFivesscByIssue(String issue) {
        FivesscLotterySgExample example = new FivesscLotterySgExample();
        FivesscLotterySgExample.Criteria fivesscCriteria = example.createCriteria();
        fivesscCriteria.andIssueEqualTo(issue);
        FivesscLotterySg fivesscLotterySg = this.fivesscLotterySgMapper.selectOneByExample(example);
        return fivesscLotterySg;
    }

    /**
     * @Title: selectFivesscByIssue
     * @Description: 缓存近期数据
     * @author HANS
     * @date 2019年5月16日下午4:38:42
     */
    public List<FivesscLotterySg> selectFivesscByIssue() {
        FivesscLotterySgExample example = new FivesscLotterySgExample();
        FivesscLotterySgExample.Criteria fivesscCriteria = example.createCriteria();
        fivesscCriteria.andOpenStatusEqualTo(LotteryResultStatus.AUTO);
        example.setOrderByClause("`ideal_time` DESC");
        example.setOffset(Constants.DEFAULT_INTEGER);
        example.setLimit(Constants.DEFAULT_ALGORITHM_PAGESIZE);
        List<FivesscLotterySg> fivesscLotterySgList = this.fivesscLotterySgMapper.selectByExample(example);
        return fivesscLotterySgList;
    }


    @Override
    public void cacheIssueResultForFivessc(String issue, String number) {
        FivesscLotterySg fivesscLotterySg = this.selectFivesscByIssue(issue);
        // 缓存到开奖结果
        String redisKey = RedisKeys.FIVESSC_RESULT_VALUE;
        Long redisTime = CaipiaoRedisTimeEnum.FIVESSC.getRedisTime();
        redisTemplate.opsForValue().set(redisKey, fivesscLotterySg);
        // 获取下期信息
        FivesscLotterySg nextFivesscLotterySg = this.queryFivesscNextSg();
        // 缓存到下期信息
        String nextRedisKey = RedisKeys.FIVESSC_NEXT_VALUE;
        redisTemplate.opsForValue().set(nextRedisKey, nextFivesscLotterySg, redisTime, TimeUnit.MINUTES);
        // 缓存近期数据
        List<FivesscLotterySg> fivesscLotterySgList = this.selectFivesscByIssue();
        String algorithm = RedisKeys.FIVESSC_ALGORITHM_VALUE;
        redisTemplate.opsForValue().set(algorithm, fivesscLotterySgList, redisTime, TimeUnit.MINUTES);
    }

    @Override
    public JssscLotterySg queryJssscNextSg() {
        JssscLotterySgExample nextExample = new JssscLotterySgExample();
        JssscLotterySgExample.Criteria nextTjsscCriteria = nextExample.createCriteria();
        nextTjsscCriteria.andIdealTimeGreaterThan(DateUtils.getFullStringZeroSecond(new Date()));
        nextTjsscCriteria.andOpenStatusEqualTo(LotteryResultStatus.WAIT);
        nextExample.setOrderByClause("ideal_time ASC");
        JssscLotterySg nextTjsscLotterySg = this.jssscLotterySgMapper.selectOneByExample(nextExample);
        return nextTjsscLotterySg;
    }

    @Override
    public JssscLotterySg selectJssscByIssue(String issue) {
        JssscLotterySgExample example = new JssscLotterySgExample();
        JssscLotterySgExample.Criteria tqsscCriteria = example.createCriteria();
        tqsscCriteria.andIssueEqualTo(issue);
        JssscLotterySg jssscLotterySg = this.jssscLotterySgMapper.selectOneByExample(example);
        return jssscLotterySg;
    }

    public List<JssscLotterySg> getAlgorithmData() {
        JssscLotterySgExample example = new JssscLotterySgExample();
        JssscLotterySgExample.Criteria jssscCriteria = example.createCriteria();
        jssscCriteria.andOpenStatusEqualTo(LotteryResultStatus.AUTO);
        example.setOrderByClause("`ideal_time` DESC");
        example.setOffset(Constants.DEFAULT_INTEGER);
        example.setLimit(Constants.DEFAULT_ALGORITHM_PAGESIZE);
        List<JssscLotterySg> jssscLotterySgList = this.jssscLotterySgMapper.selectByExample(example);
        return jssscLotterySgList;
    }

    @Override
    public void cacheIssueResultForJsssc(String issue, String number) {
        JssscLotterySg jssscLotterySg = this.selectJssscByIssue(issue);
        // 缓存到开奖结果
        String redisKey = RedisKeys.JSSSC_RESULT_VALUE;
        Long redisTime = CaipiaoRedisTimeEnum.JSSSC.getRedisTime();
        redisTemplate.opsForValue().set(redisKey, jssscLotterySg);
        // 获取下期信息
        JssscLotterySg next_TjsscLotterySg = this.queryJssscNextSg();
        // 缓存到下期信息
        String nextRedisKey = RedisKeys.JSSSC_NEXT_VALUE;
        redisTemplate.opsForValue().set(nextRedisKey, next_TjsscLotterySg, redisTime, TimeUnit.MINUTES);
        // 缓存近期200条数据
        List<JssscLotterySg> jssscLotterySgList = this.getAlgorithmData();
        String algorithm = RedisKeys.JSSSC_ALGORITHM_VALUE;
        redisTemplate.opsForValue().set(algorithm, jssscLotterySgList, redisTime, TimeUnit.MINUTES);
    }

    @Override
    public CqsscLotterySg queryCqsscNextSg() {
        CqsscLotterySgExample next_example = new CqsscLotterySgExample();
        CqsscLotterySgExample.Criteria next_cqsscCriteria = next_example.createCriteria();
        next_cqsscCriteria.andIdealTimeGreaterThan(DateUtils.getFullStringZeroSecond(new Date()));
        next_cqsscCriteria.andOpenStatusEqualTo(LotteryResultStatus.WAIT);
        next_example.setOrderByClause("ideal_time ASC");
        CqsscLotterySg next_cqsscLotterySg = this.cqsscLotterySgMapper.selectOneByExample(next_example);
        return next_cqsscLotterySg;
    }

    @Override
    public CqsscLotterySg selectCqsscByIssue(String issue) {
        CqsscLotterySgExample example = new CqsscLotterySgExample();
        CqsscLotterySgExample.Criteria cqsscCriteria = example.createCriteria();
        cqsscCriteria.andIssueEqualTo(issue);
        CqsscLotterySg cqsscLotterySg = this.cqsscLotterySgMapper.selectOneByExample(example);
        return cqsscLotterySg;
    }

    /**
     * @Title: getCqsscAlgorithmData
     * @Description: 缓存重庆时时彩数据
     * @author HANS
     * @date 2019年5月17日下午4:20:03
     */
    public List<CqsscLotterySg> getCqsscAlgorithmData() {
        CqsscLotterySgExample cqsscExample = new CqsscLotterySgExample();
        CqsscLotterySgExample.Criteria cqsscCriteria = cqsscExample.createCriteria();
        cqsscCriteria.andOpenStatusEqualTo(LotteryResultStatus.AUTO);
        cqsscExample.setOrderByClause("`ideal_time` DESC");
        cqsscExample.setOffset(Constants.DEFAULT_INTEGER);
        cqsscExample.setLimit(Constants.DEFAULT_ALGORITHM_PAGESIZE);
        return this.cqsscLotterySgMapper.selectByExample(cqsscExample);
    }

    @Override
    public void cacheIssueResultForCqssc(String issue, String number) {
//        CqsscLotterySg cqsscLotterySg = this.selectCqsscByIssue(issue);
//        // 缓存到开奖结果
//        String redisKey = RedisKeys.CQSSC_RESULT_VALUE;
//        Long redisTime = CaipiaoRedisTimeEnum.CQSSC.getRedisTime();
//        redisTemplate.opsForValue().set(redisKey, cqsscLotterySg);
//        // 获取下期信息
//        CqsscLotterySg next_cqsscLotterySg = this.queryCqsscNextSg();
//        // 缓存到下期信息
//        String nextRedisKey = RedisKeys.CQSSC_NEXT_VALUE;
//        redisTemplate.opsForValue().set(nextRedisKey, next_cqsscLotterySg, redisTime, TimeUnit.MINUTES);
//        // 缓存近期200条数据
//        List<CqsscLotterySg> cqsscLotterySgList = this.getCqsscAlgorithmData();
//        String algorithm = RedisKeys.CQSSC_ALGORITHM_VALUE;
//        redisTemplate.opsForValue().set(algorithm, cqsscLotterySgList, redisTime, TimeUnit.MINUTES);
        List<String> keyList = new ArrayList<>();
        keyList.add(RedisKeys.CQSSC_RESULT_VALUE);
        keyList.add(RedisKeys.CQSSC_NEXT_VALUE);
        keyList.add(RedisKeys.CQSSC_ALGORITHM_VALUE);
        redisTemplate.delete(keyList);
    }

    @Override
    public TxffcLotterySg queryTxffcNextSg() {
        TxffcLotterySgExample example = new TxffcLotterySgExample();
        TxffcLotterySgExample.Criteria txffcCriteria = example.createCriteria();
        txffcCriteria.andIdealTimeGreaterThan(DateUtils.getFullStringZeroSecond(new Date()));
        txffcCriteria.andOpenStatusEqualTo(LotteryResultStatus.WAIT);
        example.setOrderByClause("ideal_time ASC");
        TxffcLotterySg txffcLotterySg = txffcLotterySgMapper.selectOneByExample(example);
        return txffcLotterySg;
    }

    @Override
    public TxffcLotterySg selectTxffcByIssue(String issue) {
        TxffcLotterySgExample example = new TxffcLotterySgExample();
        TxffcLotterySgExample.Criteria txffcCriteria = example.createCriteria();
        txffcCriteria.andIssueEqualTo(issue);
        TxffcLotterySg txffcLotterySg = txffcLotterySgMapper.selectOneByExample(example);
        return txffcLotterySg;
    }

    public Integer queryOpenedCount() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("openStatus", LotteryResultStatus.AUTO);
        map.put("paramTime", TimeHelper.date("yyyy-MM-dd") + "%");
        Integer openCount = txffcLotterySgMapperExt.openCountByExample(map);
        return openCount;
    }

    /**
     * @Title: selectNearTxffcIssue
     * @Description: 查询近期数据
     * @author HANS
     * @date 2019年5月28日下午8:26:41
     */
    public List<TxffcLotterySg> selectNearTxffcIssue() {
        TxffcLotterySgExample txffcExample = new TxffcLotterySgExample();
        TxffcLotterySgExample.Criteria txffcCriteria = txffcExample.createCriteria();
        txffcCriteria.andOpenStatusEqualTo(LotteryResultStatus.AUTO);
        txffcExample.setOrderByClause("`ideal_time` DESC");
        txffcExample.setOffset(Constants.DEFAULT_INTEGER);
        txffcExample.setLimit(Constants.DEFAULT_ALGORITHM_PAGESIZE);
        List<TxffcLotterySg> txffcLotterySgList = txffcLotterySgMapper.selectByExample(txffcExample);
        return txffcLotterySgList;
    }

    @Override
    public void cacheIssueResultForTxffc(String issue, String number) {
        TxffcLotterySg txffcLotterySg = this.selectTxffcByIssue(issue);
        // 缓存到开奖结果
        String redisKey = RedisKeys.TXFFC_RESULT_VALUE;
        Long redisTime = CaipiaoRedisTimeEnum.TXFFC.getRedisTime();
        redisTemplate.opsForValue().set(redisKey, txffcLotterySg);
        // 获取下期信息
        TxffcLotterySg nextTxffcLotterySg = this.queryTxffcNextSg();
        // 缓存到下期信息
        String nextRedisKey = RedisKeys.TXFFC_NEXT_VALUE;
        redisTemplate.opsForValue().set(nextRedisKey, nextTxffcLotterySg, redisTime, TimeUnit.SECONDS);
        // 获取已经开奖数据
        Integer openCount = this.queryOpenedCount();
        String openRedisKey = RedisKeys.TXFFC_OPEN_VALUE;
        redisTemplate.opsForValue().set(openRedisKey, openCount);
        // 缓存近期数据
        List<TxffcLotterySg> txffcLotterySgList = this.selectNearTxffcIssue();
        String algorithm = RedisKeys.TXFFC_ALGORITHM_VALUE;
        redisTemplate.opsForValue().set(algorithm, txffcLotterySgList, redisTime, TimeUnit.SECONDS);
    }

//    @Override
//    public FtjssscLotterySg queryFtjssscNextSg() {
//        FtjssscLotterySgExample nextFtjssscExample = new FtjssscLotterySgExample();
//        FtjssscLotterySgExample.Criteria nextFtJsssCriteria = nextFtjssscExample.createCriteria();
//        nextFtJsssCriteria.andOpenStatusEqualTo(LotteryResultStatus.WAIT);
//        nextFtJsssCriteria.andIdealTimeGreaterThan(DateUtils.getFullStringZeroSecond(new Date()));
//        nextFtjssscExample.setOrderByClause("ideal_time ASC");
//        FtjssscLotterySg nextFtjssscLotterySg = this.ftjssscLotterySgMapper.selectOneByExample(nextFtjssscExample);
//        return nextFtjssscLotterySg;
//    }

//    @Override
//    public FtjssscLotterySg selectftJssscByIssue(String issue, String number) {
//        FtjssscLotterySgExample ftJsssExample = new FtjssscLotterySgExample();
//        FtjssscLotterySgExample.Criteria ftJsssCriteria = ftJsssExample.createCriteria();
//        ftJsssCriteria.andIssueEqualTo(issue);
//        FtjssscLotterySg ftjssscLotterySg = ftjssscLotterySgMapper.selectOneByExample(ftJsssExample);
//
//        if (ftjssscLotterySg != null && StringUtils.isNotEmpty(number) && StringUtils.isEmpty(ftjssscLotterySg.getNumber())) {
//            ftjssscLotterySg.setNumber(number);
//        }
//        return ftjssscLotterySg;
//    }

//    @Override
//    public void cacheIssueResultForFtjsssc(String issue, String number) {
//        FtjssscLotterySg ftjssscLotterySg = this.selectftJssscByIssue(issue, number);
//        // 缓存到开奖结果
//        String redisKey = RedisKeys.JSSSCFT_RESULT_VALUE;
//        Long redisTime = CaipiaoRedisTimeEnum.JSSSCFT.getRedisTime();
//        redisTemplate.opsForValue().set(redisKey, ftjssscLotterySg);
//        // 获取下期信息
//        FtjssscLotterySg nextFtjssscLotterySg = this.queryFtjssscNextSg();
//        // 缓存到下期信息
//        String nextRedisKey = RedisKeys.JSSSCFT_NEXT_VALUE;
//        redisTemplate.opsForValue().set(nextRedisKey, nextFtjssscLotterySg, redisTime, TimeUnit.SECONDS);
//    }

}
