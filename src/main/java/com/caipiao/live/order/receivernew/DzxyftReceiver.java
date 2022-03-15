package com.caipiao.live.order.receivernew;

import com.alibaba.fastjson.JSONObject;
import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.constant.RedisKeys;
import com.caipiao.live.common.enums.lottery.LotteryTableNameEnum;
import com.caipiao.live.common.mybatis.entity.DzxyftLotterySg;
import com.caipiao.live.common.mybatis.entity.DzxyftLotterySgExample;
import com.caipiao.live.common.mybatis.mapper.DzxyftLotterySgMapper;
import com.caipiao.live.common.mybatis.mapperext.sg.DzxyftLotterySgMapperExt;
import com.caipiao.live.common.util.DateUtils;
import com.caipiao.live.common.util.redis.BasicRedisClient;
import com.caipiao.live.common.util.redis.RedisBusinessUtil;
import com.caipiao.live.order.config.ActiveMQConfig;
import com.caipiao.live.order.service.bet.BetDzxyftService;
import com.caipiao.live.order.service.result.DzxyftLotterySgWriteService;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.stereotype.Component;

import javax.jms.Destination;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author lzy
 * @create 2018-09-17 20:07
 **/
@Component
public class DzxyftReceiver {
    private static final Logger logger = LoggerFactory.getLogger(DzxyftReceiver.class);

    @Autowired
    private BetDzxyftService betDzxyftService;
    @Autowired
    private DzxyftLotterySgWriteService dzxyftLotterySgWriteService;
    @Autowired
    private DzxyftLotterySgMapper dzxyftLotterySgMapper;
    @Autowired
    private DzxyftLotterySgMapperExt dzxyftLotterySgMapperExt;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private BasicRedisClient basicRedisClient;
    @Autowired
    private JmsMessagingTemplate jmsMessagingTemplate;


    /**
     * 德州幸运飞艇- 【两面】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_XYFT_LM)
    @JmsListener(destination = ActiveMQConfig.LIVE_TOPIC_DZXYFT_LM, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgDzxyftLm(String message) throws Exception {
        logger.info("【德州幸运飞艇-两面】结算期号mq{}：{}  " ,ActiveMQConfig.LIVE_TOPIC_DZXYFT_LM, message);
        String[] str = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.LIVE_TOPIC_DZXYFT_LM + str[1];
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50L);
                    if (str.length == 4) {   //表面需要同步数据
                        //第一步同步中奖结果
                        DzxyftLotterySg updateSg = JSONObject.parseObject(str[3].replace("$", ":"), DzxyftLotterySg.class);
                        int boolUpdate = dzxyftLotterySgMapperExt.updateByIssue(updateSg);
                        if (boolUpdate == 0) {  //说明这一天的期号数据可能不全，则做一次检查，如果真没有，则从task_server同步当天所有期号数据过来
                            DzxyftLotterySgExample sgExample = new DzxyftLotterySgExample();
                            DzxyftLotterySgExample.Criteria criteria = sgExample.createCriteria();
                            LocalDate today = LocalDate.now();//当天日期
                            criteria.andIdealTimeGreaterThan(DateUtils.getLocalDateToDate(today));
                            LocalDate tomrrow = today.plusDays(1L);//第二天
                            criteria.andIdealTimeLessThan(DateUtils.getLocalDateToDate(tomrrow));
                            int afterCount = dzxyftLotterySgMapper.countByExample(sgExample);
                            if (afterCount < 1440) {
                                //则发送消息
                                logger.info("同步预期数据发送通知：xyftYuqiToday mq:{}",ActiveMQConfig.LIVE_TOPIC_YUQI_TODAYT);
                                Destination destination = new ActiveMQTopic(ActiveMQConfig.LIVE_TOPIC_YUQI_TODAYT);
                                jmsMessagingTemplate.convertAndSend(destination, "dzXyftYuqiToday");
                            }
                        } else {
                            //更新历史赛果缓存
                            RedisBusinessUtil.updateLsSgCache(Constants.LOTTERY_DZXYFT, RedisKeys.DZXYFT_SG_HS_LIST, updateSg);
                        }

                    }
                    dzxyftLotterySgWriteService.cacheIssueResultForDzxyft(str[1], str[2]);//德州幸运飞艇
                    // 结算【德州幸运飞艇-两面】
                    betDzxyftService.clearingDzxyftLm(str[1], str[2]);

                    //第三步 最近1天未开奖的数据
                    DzxyftLotterySgExample dzxyftLotterySgExample = new DzxyftLotterySgExample();
                    DzxyftLotterySgExample.Criteria criteria = dzxyftLotterySgExample.createCriteria();
                    criteria.andOpenStatusEqualTo(Constants.WAIT);
                    LocalDateTime localDateTime = LocalDateTime.now();
                    LocalDateTime yerterday = localDateTime.minusDays(1);
                    criteria.andIdealTimeLessThan(DateUtils.getLocalDateTimeToDate(localDateTime));
                    criteria.andIdealTimeGreaterThanOrEqualTo(DateUtils.getLocalDateTimeToDate(yerterday));
                    dzxyftLotterySgExample.setOrderByClause("ideal_time desc");
                    List<DzxyftLotterySg> dzxyftLotterySgList = dzxyftLotterySgMapper.selectByExample(dzxyftLotterySgExample);
                    if (dzxyftLotterySgList.size() > 0) {
                        String issues = "";
                        for (DzxyftLotterySg sg : dzxyftLotterySgList) {
                            issues = issues + sg.getIssue() + ",";
                        }
                        issues = issues.substring(0, issues.length() - 1);
                        Destination destination = new ActiveMQQueue(ActiveMQConfig.LIVE_TOPIC_MISSING_LOTTERY_SG);
                        logger.info("missing发送通知：xyftYuqiToday mq:{}",ActiveMQConfig.LIVE_TOPIC_MISSING_LOTTERY_SG);
                        jmsMessagingTemplate.convertAndSend(destination, LotteryTableNameEnum.DZXYFEIT.getLotteryId() + "#" + issues);
                        logger.info("发送缺奖数据{}，{}", LotteryTableNameEnum.DZXYFEIT.getLotteryId(), issues);
                    }

                }
            }

        } catch (Exception e) {
            logger.error("processMsgDzxyftLm occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * 德州幸运飞艇- 【猜名次猜前几】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_XYFT_CMC_CQJ)
    @JmsListener(destination = ActiveMQConfig.LIVE_TOPIC_DZXYFT_CMC_CQJ, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgDzxyftCmcCqj(String message) throws Exception {
        logger.info("【德州幸运飞艇-猜名次猜前几】结算期号：  " + message);
        String[] str = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.LIVE_TOPIC_DZXYFT_CMC_CQJ + str[1];
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50L);
                    // 结算【德州幸运飞艇-猜名次猜前几】
                    betDzxyftService.clearingDzxyftCmcCqj(str[1], str[2]);
                }
            }

        } catch (Exception e) {
            logger.error("processMsgDzxyftCmcCqj occur error:", e);
        } finally {
            lock.writeLock().unlock();
        }

    }


    /**
     * 德州幸运飞艇- 【冠亚和】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_XYFT_GYH)
    @JmsListener(destination = ActiveMQConfig.LIVE_TOPIC_DZXYFT_GYH, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgDzxyftGyh(String message) throws Exception {
        logger.info("【德州幸运飞艇-冠亚和】结算期号：  " + message);
        String[] str = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.LIVE_TOPIC_DZXYFT_GYH + str[1];
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(2, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.hGet("", key) == null) {
                    // 结算【德州幸运飞艇-冠亚和】
                    betDzxyftService.clearingDzxyftGyh(str[1], str[2]);
                    basicRedisClient.set(key, "1", 50L);
                }
            }
        } catch (Exception e) {
            logger.error("processMsgDzxyftGyh occur error:", e);
        } finally {
            lock.writeLock().unlock();
        }

    }

    @JmsListener(destination = ActiveMQConfig.LIVE_TOPIC_DZXYFT_YUQI_DATA, containerFactory = "jmsListenerContainerTopicDurable")
    public void sameDataYuqiDzxyft(String message) throws Exception {
        logger.info("【德州幸运飞艇】预期数据同步：mq:{},{}" ,ActiveMQConfig.LIVE_TOPIC_DZXYFT_YUQI_DATA, message);
        List<DzxyftLotterySg> list = JSONObject.parseArray(message.substring(message.indexOf(":") + 1), DzxyftLotterySg.class);

        // 获取唯一
        String key = ActiveMQConfig.LIVE_TOPIC_DZXYFT_YUQI_DATA + list.get(0).getIssue();
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50L);
                    for (DzxyftLotterySg sg : list) {
                        DzxyftLotterySgExample xyftLotterySgExample = new DzxyftLotterySgExample();
                        DzxyftLotterySgExample.Criteria criteria = xyftLotterySgExample.createCriteria();
                        criteria.andIssueEqualTo(sg.getIssue());
                        if (dzxyftLotterySgMapper.selectOneByExample(xyftLotterySgExample) == null) {
                            dzxyftLotterySgMapper.insertSelective(sg);
                        }

                    }
                }
            }
        } catch (Exception e) {
            logger.error("sameDataYuqiDzxyft occur error:", e);
        } finally {
            lock.writeLock().unlock();
        }
    }



}
