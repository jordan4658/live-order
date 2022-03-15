package com.caipiao.live.order.service.result.impl;

import com.caipiao.live.order.service.result.PceggLotterySgWriteService;
import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.constant.LotteryResultStatus;
import com.caipiao.live.common.constant.RedisKeys;
import com.caipiao.live.common.enums.lottery.CaipiaoRedisTimeEnum;
import com.caipiao.live.common.mybatis.entity.PceggLotterySg;
import com.caipiao.live.common.mybatis.entity.PceggLotterySgExample;
import com.caipiao.live.common.mybatis.mapper.PceggLotterySgMapper;
import com.caipiao.live.common.mybatis.mapperext.sg.PceggLotterySgMapperExt;
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
 * PC蛋蛋赛果
 *
 * @author ShaoMing
 * @datetime 2018/7/27 16:28
 */
@Service
public class PceggLotterySgWriteServiceImpl implements PceggLotterySgWriteService {
    @Autowired
    private PceggLotterySgMapper pceggLotterySgMapper;
    @Autowired
    private PceggLotterySgMapperExt pceggLotterySgMapperExt;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 获取当前最后一期已开结果（可能还在等待开奖）
     *
     * @return
     */
    @Override
    public PceggLotterySg queryLastSg() {
        PceggLotterySgExample example = new PceggLotterySgExample();
        PceggLotterySgExample.Criteria criteria = example.createCriteria();
        criteria.andIdealTimeLessThanOrEqualTo(DateUtils.formatDate(new Date(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
        example.setOrderByClause("`ideal_time` DESC");
        return pceggLotterySgMapper.selectOneByExample(example);
    }

    /**
     * 获取下一期期号及时间
     *
     * @return
     */
    @Override
    public PceggLotterySg queryNextSg() {
        PceggLotterySgExample example = new PceggLotterySgExample();
        PceggLotterySgExample.Criteria criteria = example.createCriteria();
        criteria.andIdealTimeGreaterThan(DateUtils.formatDate(new Date(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
        criteria.andOpenStatusEqualTo(LotteryResultStatus.WAIT);
        example.setOrderByClause("`ideal_time` ASC");
        return pceggLotterySgMapper.selectOneByExample(example);
    }

    /**
     * 查询已开期数
     *
     * @return
     */
    @Override
    public Integer queryOpenedCount() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("openStatus", LotteryResultStatus.AUTO);
        map.put("paramTime", TimeHelper.date("yyyy-MM-dd")+"%");
        Integer openCount = pceggLotterySgMapperExt.openCountByExample(map);
        return openCount;
    }

    /**
     * 查询下一期开奖时间（时间戳：秒）
     *
     * @return
     */
    @Override
    public Long queryCountDown() {
        PceggLotterySg sg = this.queryNextSg();
        Date dateTime = DateUtils.dateStringToDate(sg.getIdealTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS);
        return dateTime.getTime() / 1000;
    }

    /**
     * 查询指定日期已开历史记录（默认：当天）
     *
     * @param date     日期，例如：2018-11-13
     * @param sort     排序方式：ASC 顺序 | DESC 倒序
     * @param pageNo   页码
     * @param pageSize 每页数量
     * @return
     */
    @Override
    public List<PceggLotterySg> querySgList(String date, String sort, Integer pageNo, Integer pageSize) {
        PceggLotterySgExample example = new PceggLotterySgExample();
        PceggLotterySgExample.Criteria criteria = example.createCriteria();
        Date nowTime = new Date();
        String dateStr = DateUtils.formatDate(nowTime, DateUtils.FORMAT_YYYY_MM_DD);
        if (StringUtils.isEmpty(date) || dateStr.equals(date)) {
            criteria.andIdealTimeLessThanOrEqualTo(DateUtils.formatDate(nowTime, DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
            criteria.andIdealTimeLike(dateStr + "%");
        } else {
            criteria.andIdealTimeLike(date + "%");
        }
        example.setOrderByClause("`issue` " + (StringUtils.isEmpty(sort) ? "DESC" : sort));
        if (pageNo != null && pageSize != null) {
            example.setOffset((pageNo - 1) * pageSize);
            example.setLimit(pageSize);
        } else if (pageSize != null) {
            example.setLimit(pageSize);
        }
        return pceggLotterySgMapper.selectByExample(example);
    }

    @Override
    public PceggLotterySg querySgByIssue(String issue) {
        PceggLotterySg sg = null;
        // 判断期号是否为空
        if (StringUtils.isEmpty(issue)) {
            return sg;
        }
        // 从缓存中获取数据
        sg = (PceggLotterySg) redisTemplate.opsForValue().get("PC_EGG_LOTTERY_SG_" + issue);


        if (sg == null) {
            PceggLotterySgExample example = new PceggLotterySgExample();
            PceggLotterySgExample.Criteria criteria = example.createCriteria();
            criteria.andIssueEqualTo(issue);
            sg = pceggLotterySgMapper.selectOneByExample(example);
            redisTemplate.opsForValue().set("PC_EGG_LOTTERY_SG_" + issue, sg, 2, TimeUnit.MINUTES);
        }
        return sg;
    }

    @Override
    public PceggLotterySg selectByIssue(String issue) {
        PceggLotterySgExample example = new PceggLotterySgExample();
        PceggLotterySgExample.Criteria criteria = example.createCriteria();
        criteria.andIssueEqualTo(issue);
        return pceggLotterySgMapper.selectOneByExample(example);
    }

    /**
     * @return List<PceggLotterySg>
     * @Title: getAlgorithmData
     * @Description: 获取最近开奖的数据
     * @author HANS
     * @date 2019年5月12日下午7:50:48
     */
    public List<PceggLotterySg> getAlgorithmData() {
        PceggLotterySgExample example = new PceggLotterySgExample();
        PceggLotterySgExample.Criteria criteria = example.createCriteria();
        criteria.andOpenStatusEqualTo(LotteryResultStatus.AUTO);
        example.setOrderByClause("`ideal_time` DESC");
        example.setOffset(Constants.DEFAULT_INTEGER);
        example.setLimit(Constants.DEFAULT_ALGORITHM_PAGESIZE);
        return pceggLotterySgMapper.selectByExample(example);
    }

    @Override
    public void cacheIssueResultForPcdd(String issue, String number) {
        PceggLotterySg pceggLotterySg = this.selectByIssue(issue);
        // 缓存到开奖结果
        String redisKey = RedisKeys.PCDAND_RESULT_VALUE;
        Long redisTime = CaipiaoRedisTimeEnum.PCDAND.getRedisTime();
        redisTemplate.opsForValue().set(redisKey, pceggLotterySg);
        // 获取下期信息
        PceggLotterySg nextPceggLotterySg = this.queryNextSg();
        // 缓存到下期信息
        String nextRedisKey = RedisKeys.PCDAND_NEXT_VALUE;
        redisTemplate.opsForValue().set(nextRedisKey, nextPceggLotterySg, redisTime, TimeUnit.MINUTES);
        // 获取已经开奖数据
        Integer openCount = this.queryOpenedCount();
        String openRedisKey = RedisKeys.PCDAND_OPEN_VALUE;
        redisTemplate.opsForValue().set(openRedisKey, openCount);
        // 缓存近期200条开奖数据
        List<PceggLotterySg> pceggLotterySgList = this.getAlgorithmData();
        String algorithm = RedisKeys.PCDAND_ALGORITHM_VALUE;
        redisTemplate.opsForValue().set(algorithm, pceggLotterySgList, redisTime, TimeUnit.MINUTES);
    }

}
