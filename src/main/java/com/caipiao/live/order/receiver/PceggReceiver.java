package com.caipiao.live.order.receiver;

import com.alibaba.fastjson.JSONObject;
import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.constant.RedisKeys;
import com.caipiao.live.common.enums.lottery.LotteryTableNameEnum;
import com.caipiao.live.common.mybatis.entity.PceggLotterySg;
import com.caipiao.live.common.mybatis.entity.PceggLotterySgExample;
import com.caipiao.live.common.mybatis.mapper.PceggLotterySgMapper;
import com.caipiao.live.common.mybatis.mapperext.sg.PceggLotterySgMapperExt;
import com.caipiao.live.common.util.DateUtils;
import com.caipiao.live.common.util.redis.BasicRedisClient;
import com.caipiao.live.common.util.redis.RedisBusinessUtil;
import com.caipiao.live.order.config.ActiveMQConfig;
import com.caipiao.live.order.service.bet.BetPceggService;
import com.caipiao.live.order.service.result.PceggLotterySgWriteService;
import org.apache.activemq.command.ActiveMQQueue;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.stereotype.Component;

import javax.jms.Destination;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author lzy
 * @create 2018-09-14 10:15
 **/
@Component
public class PceggReceiver {
    private static final Logger logger = LoggerFactory.getLogger(PceggReceiver.class);

    @Autowired
    private BetPceggService betPceggService;
    @Autowired
    private PceggLotterySgWriteService pceggLotterySgWriteService;
    @Autowired
    private PceggLotterySgMapper pceggLotterySgMapper;
    @Autowired
    private PceggLotterySgMapperExt pceggLotterySgMapperExt;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private BasicRedisClient basicRedisClient;
    @Autowired
    private JmsMessagingTemplate jmsMessagingTemplate;


    /**
     * PC蛋蛋- 【特码】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_PCEGG_TM)
    @JmsListener(destination = ActiveMQConfig.TOPIC_PCEGG_TM, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgPceggTm(String message) throws Exception {
        logger.info("【PC蛋蛋-特码】结算期号：  " + message);
        String[] num = message.split(":");

        // 获取唯一
        String key = ActiveMQConfig.TOPIC_PCEGG_TM + num[1];
        // 【分布式读写锁】
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            // 判断是否获取到锁
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    if (num.length == 4) {   //表面需要同步数据
                        //第一步同步中奖结果
                        PceggLotterySg updateSg = JSONObject.parseObject(num[3].replace("$", ":"), PceggLotterySg.class);
                        int boolUpdate = pceggLotterySgMapperExt.updateByIssue(updateSg);

                        if (boolUpdate == 0) {  //说明这一天的期号数据可能不全，则做一次检查，如果真没有，则从task_server同步当天所有期号数据过来
                            PceggLotterySgExample sgExample = new PceggLotterySgExample();
                            PceggLotterySgExample.Criteria criteria = sgExample.createCriteria();
                            Calendar calendar = Calendar.getInstance();
                            criteria.andIdealTimeGreaterThanOrEqualTo(DateUtils.formatDate(new Date(), DateUtils.FORMAT_YYYY_MM_DD));
                            calendar.add(Calendar.DAY_OF_MONTH, 1);
                            criteria.andIdealTimeLessThan(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD));
                            int afterCount = pceggLotterySgMapper.countByExample(sgExample);
                            if (afterCount < 179) {
                                //则发送消息
                                logger.info("同步预期数据发送通知：pceggYuqiToday");
                                Destination destination = new ActiveMQQueue(ActiveMQConfig.TOPIC_YUQI_TODAYT);
                                jmsMessagingTemplate.convertAndSend(destination, "pceggYuqiToday");
                            }
                        } else {
                            //更新历史赛果缓存
                            RedisBusinessUtil.updateLsSgCache(Constants.LOTTERY_PCEGG, RedisKeys.PCEGG_SG_HS_LIST, updateSg);
                        }

                    }

                    pceggLotterySgWriteService.cacheIssueResultForPcdd(num[1], num[2]);
                    // 结算【PC蛋蛋-特码】
                    betPceggService.clearingPceggTm(num[1], num[2]);

                    //第三步 最近1天未开奖的数据
                    PceggLotterySgExample pceggLotterySgExample = new PceggLotterySgExample();
                    PceggLotterySgExample.Criteria criteria = pceggLotterySgExample.createCriteria();
                    criteria.andOpenStatusEqualTo(Constants.WAIT);
                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.MINUTE, -5);  //过滤掉最新一期，（这一期可能需要2分钟才抓到）
                    criteria.andIdealTimeLessThan(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                    calendar.add(Calendar.DAY_OF_MONTH, -1);
                    criteria.andIdealTimeGreaterThanOrEqualTo(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                    pceggLotterySgExample.setOrderByClause("ideal_time desc");
                    pceggLotterySgExample.setLimit(15);
                    pceggLotterySgExample.setOffset(0);
                    List<PceggLotterySg> pceggLotterySgList = pceggLotterySgMapper.selectByExample(pceggLotterySgExample);
                    if (pceggLotterySgList.size() > 0) {
                        String issues = "";
                        for (PceggLotterySg sg : pceggLotterySgList) {
                            issues = issues + sg.getIssue() + ",";
                        }
                        issues = issues.substring(0, issues.length() - 1);
                        Destination destination = new ActiveMQQueue(ActiveMQConfig.TOPIC_MISSING_LOTTERY_SG);
                        jmsMessagingTemplate.convertAndSend(destination, LotteryTableNameEnum.PCDAND.getLotteryId() + "#" + issues);
                        logger.info("发送缺奖数据{}，{}", LotteryTableNameEnum.PCDAND.getLotteryId(), issues);
                    }

                }
            }

        } catch (Exception e) {
            logger.error("processMsgPceggTm occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * PC蛋蛋- 【豹子】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_PCEGG_BZ)
    @JmsListener(destination = ActiveMQConfig.TOPIC_PCEGG_BZ, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgPceggBz(String message) throws Exception {
        logger.info("【PC蛋蛋-豹子】结算期号：  " + message);
        String[] num = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.TOPIC_PCEGG_BZ + num[1];
        // 【分布式读写锁】
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            // 判断是否获取到锁
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    // 结算【PC蛋蛋-豹子】
                    betPceggService.clearingPceggBz(num[1], num[2]);
                }
            }
        } catch (Exception e) {
            logger.error("processMsgPceggBz occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * PC蛋蛋- 【特码包三】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_PCEGG_TMBS)
    @JmsListener(destination = ActiveMQConfig.TOPIC_PCEGG_TMBS, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgPceggTmbs(String message) throws Exception {
        logger.info("【PC蛋蛋-特码包三】结算期号：  " + message);
        String[] num = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.TOPIC_PCEGG_TMBS + num[1];
        // 【分布式读写锁】
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    // 结算【PC蛋蛋-特码包三】
                    betPceggService.clearingPceggTmbs(num[1], num[2]);
                }
            }
        } catch (Exception e) {
            logger.error("processMsgPceggTmbs occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * PC蛋蛋- 【色波】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_PCEGG_SB)
    @JmsListener(destination = ActiveMQConfig.TOPIC_PCEGG_SB, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgPceggSb(String message) throws Exception {
        logger.info("【PC蛋蛋-色波】结算期号：  " + message);
        String[] num = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.TOPIC_PCEGG_SB + num[1];
        // 【分布式读写锁】
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    // 结算【PC蛋蛋-色波】
                    betPceggService.clearingPceggSb(num[1], num[2]);
                }
            }
        } catch (Exception e) {
            logger.error("processMsgPceggSb occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * PC蛋蛋- 【混合】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_PCEGG_HH)
    @JmsListener(destination = ActiveMQConfig.TOPIC_PCEGG_HH, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgPceggHh(String message) throws Exception {
        logger.info("【PC蛋蛋-混合】结算期号：  " + message);
        String[] num = message.split(":");

        // 获取唯一
        String key = ActiveMQConfig.TOPIC_PCEGG_HH + num[1];
        // 【分布式读写锁】
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    // 结算【PC蛋蛋-混合】
                    betPceggService.clearingPceggHh(num[1], num[2]);
                }
            }
        } catch (Exception e) {
            logger.error("processMsgPceggHh occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }

    }

    @JmsListener(destination = ActiveMQConfig.TOPIC_PCEGG_YUQI_DATA, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgSscCqYuqiData(String message) throws Exception {
        logger.info("【PC蛋蛋】预期数据同步：  " + message);
        List<PceggLotterySg> list = JSONObject.parseArray(message.substring(message.indexOf(":") + 1), PceggLotterySg.class);
        // 获取唯一
        String key = ActiveMQConfig.TOPIC_PCEGG_YUQI_DATA + list.get(0).getIssue();
        // 【分布式读写锁】
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    for (PceggLotterySg sg : list) {
                        PceggLotterySgExample pceggLotterySgExample = new PceggLotterySgExample();
                        PceggLotterySgExample.Criteria criteria = pceggLotterySgExample.createCriteria();
                        criteria.andIssueEqualTo(sg.getIssue());
                        if (pceggLotterySgMapper.selectOneByExample(pceggLotterySgExample) == null) {
                            pceggLotterySgMapper.insertSelective(sg);
                        }
                    }
                }
            }

        } catch (Exception e) {
            logger.error("processMsgSscCqYuqiData occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }
    }



}