package com.caipiao.live.order.service.result.impl;

import com.caipiao.live.order.service.result.DzpceggLotterySgWriteService;
import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.constant.LotteryResultStatus;
import com.caipiao.live.common.constant.RedisKeys;
import com.caipiao.live.common.enums.lottery.CaipiaoRedisTimeEnum;
import com.caipiao.live.common.mybatis.entity.DzpceggLotterySg;
import com.caipiao.live.common.mybatis.entity.DzpceggLotterySgExample;
import com.caipiao.live.common.mybatis.mapper.DzpceggLotterySgMapper;
import com.caipiao.live.common.mybatis.mapperext.sg.DzpceggLotterySgMapperExt;
import com.caipiao.live.common.util.DateUtils;
import com.caipiao.live.common.util.StringUtils;
import com.caipiao.live.common.util.TimeHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
public class DzpceggLotterySgWriteServiceImpl implements DzpceggLotterySgWriteService {

    @Autowired
    private DzpceggLotterySgMapper dzpceggLotterySgMapper;
    @Autowired
    private DzpceggLotterySgMapperExt dzpceggLotterySgMapperExt;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 获取当前最后一期已开结果（可能还在等待开奖）
     *
     * @return
     */
    @Override
    public DzpceggLotterySg queryLastSg() {
        DzpceggLotterySgExample example = new DzpceggLotterySgExample();
        DzpceggLotterySgExample.Criteria criteria = example.createCriteria();
        criteria.andIdealTimeLessThanOrEqualTo(new Date());
        example.setOrderByClause("`ideal_time` DESC");
        return dzpceggLotterySgMapper.selectOneByExample(example);
    }

    /**
     * 获取下一期期号及时间
     *
     * @return
     */
    @Override
    public DzpceggLotterySg queryNextSg() {
        DzpceggLotterySgExample example = new DzpceggLotterySgExample();
        DzpceggLotterySgExample.Criteria criteria = example.createCriteria();
        criteria.andIdealTimeGreaterThan(new Date());
        criteria.andOpenStatusEqualTo(LotteryResultStatus.WAIT);
        example.setOrderByClause("`ideal_time` ASC");
        return dzpceggLotterySgMapper.selectOneByExample(example);
    }

    /**
     * 查询已开期数
     *
     * @return
     */
    @Override
    public Integer queryOpenedCount() {
//        DzpceggLotterySgExample example = new DzpceggLotterySgExample();
//        DzpceggLotterySgExample.Criteria criteria = example.createCriteria();
//        criteria.andIdealTimeLessThanOrEqualTo(DateUtils.formatDate(new Date(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
//        criteria.andIdealTimeLike(DateUtils.formatDate(new Date(), DateUtils.FORMAT_YYYY_MM_DD) + "%");
//        return dzpceggLotterySgMapper.countByExample(example);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("openStatus", LotteryResultStatus.AUTO);
        map.put("paramTime", TimeHelper.date("yyyy-MM-dd")+"%");
        Integer openCount = dzpceggLotterySgMapperExt.openCountByExample(map);
        return openCount;
    }

    /**
     * 查询下一期开奖时间（时间戳：秒）
     *
     * @return
     */
    @Override
    public Long queryCountDown() {
        DzpceggLotterySg sg = this.queryNextSg();
        Date dateTime = sg.getIdealTime();
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
    public List<DzpceggLotterySg> querySgList(String date, String sort, Integer pageNo, Integer pageSize) {
        DzpceggLotterySgExample example = new DzpceggLotterySgExample();
        DzpceggLotterySgExample.Criteria criteria = example.createCriteria();
        LocalDateTime nowTime = LocalDateTime.now();
        //获取当天实时时间
        Date todayNowTime = DateUtils.getLocalDateTimeToDate(nowTime);
        //获取当天零点时间
        Date todayStartTime = DateUtils.getLocalDateTimeToDate(DateUtils.getLocalDateTimeStartTime());
        //当天日期
        String dateStr = DateUtils.getLocalDateToYyyyMmDd(LocalDate.now());
        //如果未傳入時間 或者時間為當天 那麼 日期都是當天
        String dateStart = date + " 00:00:00";
        Date dateStartZero = DateUtils.parseDate(dateStart);
        String dateEnd = date + " 59:59:59";
        Date dateEndZero = DateUtils.parseDate(dateEnd);
        if (StringUtils.isEmpty(date) || dateStr.equals(date)) {
            criteria.andIdealTimeGreaterThanOrEqualTo(todayStartTime);
            criteria.andIdealTimeLessThanOrEqualTo(todayNowTime);
        } else {
            criteria.andIdealTimeGreaterThanOrEqualTo(dateStartZero);
            criteria.andIdealTimeLessThanOrEqualTo(dateEndZero);
        }
        example.setOrderByClause("`issue` " + (StringUtils.isEmpty(sort) ? "DESC" : sort));
        if (pageNo != null && pageSize != null) {
            example.setOffset((pageNo - 1) * pageSize);
            example.setLimit(pageSize);
        } else if (pageSize != null) {
            example.setLimit(pageSize);
        }
        return dzpceggLotterySgMapper.selectByExample(example);
    }

    @Override
    public DzpceggLotterySg querySgByIssue(String issue) {
        DzpceggLotterySg sg = null;
        // 判断期号是否为空
        if (StringUtils.isEmpty(issue)) {
            return sg;
        }
        // 从缓存中获取数据
        sg = (DzpceggLotterySg) redisTemplate.opsForValue().get("PC_EGG_LOTTERY_SG_" + issue);


        if (sg == null) {
            DzpceggLotterySgExample example = new DzpceggLotterySgExample();
            DzpceggLotterySgExample.Criteria criteria = example.createCriteria();
            criteria.andIssueEqualTo(issue);
            sg = dzpceggLotterySgMapper.selectOneByExample(example);
            redisTemplate.opsForValue().set("DZPC_EGG_LOTTERY_SG_" + issue, sg, 2, TimeUnit.MINUTES);
        }
        return sg;
    }

    @Override
    public DzpceggLotterySg selectByIssue(String issue) {
        DzpceggLotterySgExample example = new DzpceggLotterySgExample();
        DzpceggLotterySgExample.Criteria criteria = example.createCriteria();
        criteria.andIssueEqualTo(issue);
        return dzpceggLotterySgMapper.selectOneByExample(example);
    }

    /**
     * @return List<DzpceggLotterySg>
     * @Title: getAlgorithmData
     * @Description: 获取最近开奖的数据
     * @author HANS
     * @date 2019年5月12日下午7:50:48
     */
    public List<DzpceggLotterySg> getAlgorithmData() {
        DzpceggLotterySgExample example = new DzpceggLotterySgExample();
        DzpceggLotterySgExample.Criteria criteria = example.createCriteria();
        criteria.andOpenStatusEqualTo(LotteryResultStatus.AUTO);
        example.setOrderByClause("`ideal_time` DESC");
        example.setOffset(Constants.DEFAULT_INTEGER);
        example.setLimit(Constants.DEFAULT_ALGORITHM_PAGESIZE);
        return dzpceggLotterySgMapper.selectByExample(example);
    }

    @Override
    public void cacheIssueResultForDzpcdd(String issue, String number) {
        DzpceggLotterySg dzpceggLotterySg = this.selectByIssue(issue);
        // 缓存到开奖结果
        String redisKey = RedisKeys.DZPCDAND_RESULT_VALUE;
        Long redisTime = CaipiaoRedisTimeEnum.PCDAND.getRedisTime();
        redisTemplate.opsForValue().set(redisKey, dzpceggLotterySg);
        // 获取下期信息
        DzpceggLotterySg nextDzpceggLotterySg = this.queryNextSg();
        // 缓存到下期信息
        String nextRedisKey = RedisKeys.DZPCDAND_NEXT_VALUE;
        redisTemplate.opsForValue().set(nextRedisKey, nextDzpceggLotterySg, redisTime, TimeUnit.MINUTES);
        // 缓存近期200条开奖数据
        List<DzpceggLotterySg> dzpceggLotterySgList = this.getAlgorithmData();
        String algorithm = RedisKeys.DZPCDAND_ALGORITHM_VALUE;
        redisTemplate.opsForValue().set(algorithm, dzpceggLotterySgList, redisTime, TimeUnit.MINUTES);
    }

}
