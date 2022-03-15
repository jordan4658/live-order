package com.caipiao.live.order.receivernew;

import com.alibaba.fastjson.JSONObject;
import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.util.redis.RedisBusinessUtil;
import com.caipiao.live.order.config.ActiveMQConfig;
import com.caipiao.live.order.service.bet.BetDzpceggService;

import com.caipiao.live.order.service.result.DzpceggLotterySgWriteService;
import com.caipiao.live.common.constant.RedisKeys;

import com.caipiao.live.common.enums.lottery.LotteryTableNameEnum;
import com.caipiao.live.common.mybatis.entity.DzpceggLotterySg;
import com.caipiao.live.common.mybatis.entity.DzpceggLotterySgExample;
import com.caipiao.live.common.mybatis.mapper.DzpceggLotterySgMapper;
import com.caipiao.live.common.mybatis.mapperext.sg.DzpceggLotterySgMapperExt;
import com.caipiao.live.common.util.DateUtils;
import com.caipiao.live.common.util.redis.BasicRedisClient;
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
 * @create 2018-09-14 10:15
 **/
@Component
public class DzPceggReceiver {
    private static final Logger logger = LoggerFactory.getLogger(DzPceggReceiver.class);

    @Autowired
    private BetDzpceggService dzbetPceggService;
    @Autowired
    private DzpceggLotterySgWriteService dzpceggLotterySgWriteService;
    @Autowired
    private DzpceggLotterySgMapper dzpceggLotterySgMapper;
    @Autowired
    private DzpceggLotterySgMapperExt dzpceggLotterySgMapperExt;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private BasicRedisClient basicRedisClient;
    @Autowired
    private JmsMessagingTemplate jmsMessagingTemplate;

    /**
     * 德州PC蛋蛋- 【特码】计算结果
     *
     * @param message 消息内容【期号】
     */
    @JmsListener(destination = ActiveMQConfig.LIVE_TOPIC_DZPCEGG_TM, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgPceggTm(String message) throws Exception {
        logger.info("【德州PC蛋蛋-特码】结算期号：mq{},{}  " ,ActiveMQConfig.LIVE_TOPIC_DZPCEGG_TM, message);
        String[] num = message.split(":");

        // 获取唯一
        String key = ActiveMQConfig.LIVE_TOPIC_DZPCEGG_TM + num[1];
        // 【分布式读写锁】
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间2s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            // 判断是否获取到锁
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50L);
                    if (num.length == 4) {   //表面需要同步数据
                        //第一步同步中奖结果
                        DzpceggLotterySg updateSg = JSONObject.parseObject(num[3].replace("$", ":"), DzpceggLotterySg.class);
                        int boolUpdate = dzpceggLotterySgMapperExt.updateByIssue(updateSg);

//                        //同步到推荐表中          2019.12.16  暂时先不添加
//                        PceggRecommendExample pceggRecommendExample = new PceggRecommendExample();
//                        PceggRecommendExample.Criteria criteriaRe = pceggRecommendExample.createCriteria();
//                        criteriaRe.andIssueEqualTo(updateSg.getIssue());
//                        PceggRecommend pceggRecommend = pceggRecommendMapper.selectOneByExample(pceggRecommendExample);
//                        if(pceggRecommend != null){
//                            pceggRecommend.setOpenNumber(updateSg.getNumber());
//                            pceggRecommendMapper.updateByPrimaryKey(pceggRecommend);
//                        }

                        if (boolUpdate == 0) {  //说明这一天的期号数据可能不全，则做一次检查，如果真没有，则从task_server同步当天所有期号数据过来
                            DzpceggLotterySgExample sgExample = new DzpceggLotterySgExample();
                            DzpceggLotterySgExample.Criteria criteria = sgExample.createCriteria();
                            LocalDate today = LocalDate.now();//当天日期
                            criteria.andIdealTimeGreaterThanOrEqualTo(DateUtils.getLocalDateToDate(today));
                            LocalDate tomrrow = today.plusDays(1L);//第二天
                            criteria.andIdealTimeLessThan(DateUtils.getLocalDateToDate(tomrrow));
                            int afterCount = dzpceggLotterySgMapper.countByExample(sgExample);
                            if (afterCount < 1440) {
                                //则发送消息
                                logger.info("德州PC蛋蛋同步预期数据发送通知：dzpceggYuqiToday,mq{}",ActiveMQConfig.LIVE_TOPIC_YUQI_TODAYT);
                                Destination destination = new ActiveMQTopic(ActiveMQConfig.LIVE_TOPIC_YUQI_TODAYT);
                                jmsMessagingTemplate.convertAndSend(destination, "dzPceggYuqiToday");
                            }
                        } else {
                            //更新历史赛果缓存
                            RedisBusinessUtil.updateLsSgCache(Constants.LOTTERY_DZPCEGG, RedisKeys.DZPCEGG_SG_HS_LIST, updateSg);
                        }

                    }
                    dzpceggLotterySgWriteService.cacheIssueResultForDzpcdd(num[1], num[2]);
                    // 结算【德州PC蛋蛋-特码】
                    dzbetPceggService.clearingDzpceggTm(num[1], num[2]);

                    //第三步 最近1天未开奖的数据
                    DzpceggLotterySgExample dzpceggLotterySgExample = new DzpceggLotterySgExample();
                    DzpceggLotterySgExample.Criteria criteria = dzpceggLotterySgExample.createCriteria();
                    criteria.andOpenStatusEqualTo(Constants.WAIT);
                    LocalDateTime localDateTime = LocalDateTime.now();
                    LocalDateTime localDateTimeMinusMinutes = localDateTime.minusMinutes(5);//过滤掉最新一期，（这一期可能需要2分钟才抓到）
                    LocalDateTime localDateTimeMinusDays = localDateTime.minusDays(1);
                    criteria.andIdealTimeLessThan(DateUtils.getLocalDateTimeToDate(localDateTimeMinusMinutes));
                    criteria.andIdealTimeGreaterThanOrEqualTo(DateUtils.getLocalDateTimeToDate(localDateTimeMinusDays));
                    dzpceggLotterySgExample.setOrderByClause("ideal_time desc");
                    dzpceggLotterySgExample.setLimit(15);
                    dzpceggLotterySgExample.setOffset(0);
                    List<DzpceggLotterySg> dzpceggLotterySgList = dzpceggLotterySgMapper.selectByExample(dzpceggLotterySgExample);
                    if (dzpceggLotterySgList.size() > 0) {
                        String issues = "";
                        for (DzpceggLotterySg sg : dzpceggLotterySgList) {
                            issues = issues + sg.getIssue() + ",";
                        }
                        issues = issues.substring(0, issues.length() - 1);
                        Destination destination = new ActiveMQQueue(ActiveMQConfig.LIVE_TOPIC_MISSING_LOTTERY_SG);
                        jmsMessagingTemplate.convertAndSend(destination, LotteryTableNameEnum.DZPCDAND.getLotteryId() + "#" + issues);
                        logger.info("发送缺奖数据{}，{}", LotteryTableNameEnum.DZPCDAND.getLotteryId(), issues);
                    }

                }
            }

        } catch (Exception e) {
            logger.error("processMsgDZPceggTm occur error:", e);
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * 德州PC蛋蛋- 【豹子】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_PCEGG_BZ)
    @JmsListener(destination = ActiveMQConfig.LIVE_TOPIC_DZPCEGG_BZ, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgPceggBz(String message) throws Exception {
        logger.info("【德州PC蛋蛋-豹子】结算期号：  " + message);
        String[] num = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.LIVE_TOPIC_DZPCEGG_BZ + num[1];
        // 【分布式读写锁】
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            // 判断是否获取到锁
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50L);
                    // 结算【德州PC蛋蛋-豹子】
                    dzbetPceggService.clearingDzpceggBz(num[1], num[2]);
                }
            }
        } catch (Exception e) {
            logger.error("processMsgDZPceggBz occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * 德州PC蛋蛋- 【特码包三】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_PCEGG_TMBS)
    @JmsListener(destination = ActiveMQConfig.LIVE_TOPIC_DZPCEGG_TMBS, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgPceggTmbs(String message) throws Exception {
        logger.info("【德州PC蛋蛋-特码包三】结算期号：  " + message);
        String[] num = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.LIVE_TOPIC_DZPCEGG_TMBS + num[1];
        // 【分布式读写锁】
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50L);
                    // 结算【德州PC蛋蛋-特码包三】
                    dzbetPceggService.clearingDzpceggTmbs(num[1], num[2]);
                }
            }
        } catch (Exception e) {
            logger.error("processMsgDZPceggTmbs occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * 德州PC蛋蛋- 【色波】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_PCEGG_SB)
    @JmsListener(destination = ActiveMQConfig.LIVE_TOPIC_DZPCEGG_SB, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgPceggSb(String message) throws Exception {
        logger.info("【德州PC蛋蛋-色波】结算期号：  " + message);
        String[] num = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.LIVE_TOPIC_DZPCEGG_SB + num[1];
        // 【分布式读写锁】
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50L);
                    // 结算【德州PC蛋蛋-色波】
                    dzbetPceggService.clearingDzpceggSb(num[1], num[2]);
                }
            }
        } catch (Exception e) {
            logger.error("processMsgDZPceggSb occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * 德州PC蛋蛋- 【混合】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_PCEGG_HH)
    @JmsListener(destination = ActiveMQConfig.LIVE_TOPIC_DZPCEGG_HH, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgPceggHh(String message) throws Exception {
        logger.info("【德州PC蛋蛋-混合】结算期号：  " + message);
        String[] num = message.split(":");

        // 获取唯一
        String key = ActiveMQConfig.LIVE_TOPIC_DZPCEGG_HH + num[1];
        // 【分布式读写锁】
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50L);
                    // 结算【德州PC蛋蛋-混合】
                    dzbetPceggService.clearingDzpceggHh(num[1], num[2]);
                }
            }
        } catch (Exception e) {
            logger.error("processMsgDZPceggHh occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }

    }

    @JmsListener(destination = ActiveMQConfig.LIVE_TOPIC_DZPCEGG_YUQI_DATA, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgSscCqYuqiData(String message) throws Exception {
        logger.info("【德州PC蛋蛋】预期数据同步：mq:{},{}",ActiveMQConfig.LIVE_TOPIC_DZPCEGG_YUQI_DATA,message);
        List<DzpceggLotterySg> list = JSONObject.parseArray(message.substring(message.indexOf(":") + 1), DzpceggLotterySg.class);
        // 获取唯一
        String key = ActiveMQConfig.LIVE_TOPIC_DZPCEGG_YUQI_DATA + list.get(0).getIssue();
        // 【分布式读写锁】
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50L);
                    for (DzpceggLotterySg sg : list) {
                        DzpceggLotterySgExample dzpceggLotterySgExample = new DzpceggLotterySgExample();
                        DzpceggLotterySgExample.Criteria criteria = dzpceggLotterySgExample.createCriteria();
                        criteria.andIssueEqualTo(sg.getIssue());
                        if (dzpceggLotterySgMapper.selectOneByExample(dzpceggLotterySgExample) == null) {
                            dzpceggLotterySgMapper.insertSelective(sg);
                        }
                    }
                }
            }

        } catch (Exception e) {
            logger.error("processMsgDZPceggYuqiData occur error", e);
        } finally {
            lock.writeLock().unlock();
        }
    }


}
