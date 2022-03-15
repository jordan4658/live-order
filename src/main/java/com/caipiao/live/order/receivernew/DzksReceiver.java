package com.caipiao.live.order.receivernew;

import com.alibaba.fastjson.JSONObject;
import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.util.redis.RedisBusinessUtil;
import com.caipiao.live.order.config.ActiveMQConfig;
import com.caipiao.live.order.service.bet.BetKsService;

import com.caipiao.live.order.service.result.DzksLotterySgWriteService;
import com.caipiao.live.common.constant.RedisKeys;
import com.caipiao.live.common.enums.lottery.CaipiaoTypeEnum;
import com.caipiao.live.common.enums.lottery.LotteryTableNameEnum;
import com.caipiao.live.common.mybatis.entity.DzksLotterySg;
import com.caipiao.live.common.mybatis.entity.DzksLotterySgExample;
import com.caipiao.live.common.mybatis.mapper.DzksLotterySgMapper;
import com.caipiao.live.common.mybatis.mapperext.sg.DzksLotterySgMapperExt;
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
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Date:Created in 19:232019/12/11
 * @Descriotion
 * @Author
 **/
@Component
public class DzksReceiver {
    private static final Logger logger = LoggerFactory.getLogger(DzksReceiver.class);
    @Autowired
    private DzksLotterySgMapper dzksLotterySgMapper;
    @Autowired
    private DzksLotterySgWriteService dzksLotterySgWriteService;
    @Autowired
    private BetKsService betKsService;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private BasicRedisClient basicRedisClient;
    @Autowired
    private JmsMessagingTemplate jmsMessagingTemplate;
    @Autowired
    private DzksLotterySgMapperExt dzksLotterySgMapperExt;


    /* 德州快三【两面】 计算结果
     * @param message 消息内容【期号】
     * */
    @JmsListener(destination = ActiveMQConfig.LIVE_TOPIC_KS_DZ_HZ, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgDzksLm(String message) {
        logger.info("【德州快三】结算期号mq:{},{}" ,ActiveMQConfig.LIVE_TOPIC_KS_DZ_HZ, message);
        String[] str = message.split(":");// 拆分消息内容
        // 获取唯一
        String key = ActiveMQConfig.LIVE_TOPIC_KS_DZ_HZ + str[1];
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50L);
                    //同步中奖结果
                    DzksLotterySg updateSg = JSONObject.parseObject(str[3].replace("$", ":"), DzksLotterySg.class);
                    int boolUpdate = dzksLotterySgMapperExt.updateByIssue(updateSg);
                    if (boolUpdate == 0) {//若为o，检查是数据库表中是否有该期号
                        DzksLotterySgExample sgExample = new DzksLotterySgExample();
                        DzksLotterySgExample.Criteria criteria = sgExample.createCriteria();
                        LocalDate today = LocalDate.now();//当天日期
                        criteria.andIdealTimeGreaterThanOrEqualTo(DateUtils.getLocalDateToDate(today));
                        LocalDate tomrrow = today.plusDays(1L);//第二天
                        criteria.andIdealTimeLessThan(DateUtils.getLocalDateToDate(tomrrow));
                        sgExample.setOrderByClause("`ideal_time` asc");
                        int issueCount = dzksLotterySgMapper.countByExample(sgExample);
                        if (issueCount < 1440) {   //1440 德州快三的 一天期数  78？
                            //则发送消息
                            logger.info("德州快三同步预期数据发送通知：dzksYuqiToday:mq{}",ActiveMQConfig.LIVE_TOPIC_YUQI_TODAYT);
                            Destination destination = new ActiveMQTopic(ActiveMQConfig.LIVE_TOPIC_YUQI_TODAYT);
                            jmsMessagingTemplate.convertAndSend(destination, "dzksYuqiToday");
                        }
                    } else {
                        //更新历史赛果缓存
                        RedisBusinessUtil.updateLsSgCache(Constants.LOTTERY_DZKS, RedisKeys.DZKS_SG_HS_LIST, updateSg);
                    }

                    dzksLotterySgWriteService.cacheIssueResultForDzKs(str[1], str[2]);
                    // 结算【德州快三】
                    logger.info("德州快三两面和值开始进入结算：期号：{},开奖号码{}", str[1], str[2]);
                    betKsService.clearingKsLm(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.DZKS.getTagType()));
                    //第三步 最近1天未开奖的
                    DzksLotterySgExample dzksLotterySgExample = new DzksLotterySgExample();
                    DzksLotterySgExample.Criteria criteria = dzksLotterySgExample.createCriteria();
                    criteria.andOpenStatusEqualTo(Constants.WAIT);
                    LocalDateTime localDateTime = LocalDateTime.now();
                    LocalDateTime localDateTimeMinusDays = localDateTime.minusDays(1);
                    Date today = DateUtils.getLocalDateTimeToDate(localDateTime);
                    Date yersterday = DateUtils.getLocalDateTimeToDate(localDateTimeMinusDays);
                    criteria.andIdealTimeLessThan(today);
                    criteria.andIdealTimeGreaterThanOrEqualTo(yersterday);
                    List<DzksLotterySg> dzksLotterySgs = dzksLotterySgMapper.selectByExample(dzksLotterySgExample);
                    if (dzksLotterySgs.size() > 0) {
                        String issues = "";
                        for (DzksLotterySg sg : dzksLotterySgs) {
                            issues = issues + sg.getIssue() + ",";
                        }
                        issues = issues.substring(0, issues.length() - 1);
                        Destination destination = new ActiveMQQueue(ActiveMQConfig.LIVE_TOPIC_MISSING_LOTTERY_SG);
                        logger.info("德州快三發送missing：mq{}",ActiveMQConfig.LIVE_TOPIC_MISSING_LOTTERY_SG);
                        jmsMessagingTemplate.convertAndSend(destination, LotteryTableNameEnum.DZKS.getLotteryId() + "#" + issues);
                    }

                }
            }
        } catch (Exception e) {
            logger.error("德州快三两面和值结算出错：结算期号" + message, e);
        } finally {
            lock.writeLock().unlock();
        }

    }

    /* 德州快三【独胆】 计算结果
     * @param message 消息内容【期号】
     * */
    @JmsListener(destination = ActiveMQConfig.LIVE_TOPIC_KS_DZ_DD, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgDzksDd(String message) {
        logger.info("【德州快三独胆】结算期号：  " + message);
        String[] str = message.split(":");// 拆分消息内容
        // 获取唯一
        String key = ActiveMQConfig.LIVE_TOPIC_KS_DZ_DD + str[1];//需改动
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50L);
                    // 结算【独胆】
                    logger.info("德州快三独胆开始进入结算：期号：{},开奖号码{}", str[1], str[2]);
                    betKsService.clearingKsDd(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.DZKS.getTagType()));
                }
            }
        } catch (Exception e) {
            logger.error("德州快三独胆结算出错：结算期号" + message, e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /* 德州快三【连号】 计算结果 三连号 二连号
     * @param message 消息内容【期号】
     * */
    @JmsListener(destination = ActiveMQConfig.LIVE_TOPIC_KS_DZ_LH, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgDzksLh(String message) {
        logger.info("【德州快三连号】结算期号：  " + message);
        String[] str = message.split(":");// 拆分消息内容
        // 获取唯一
        String key = ActiveMQConfig.LIVE_TOPIC_KS_DZ_LH + str[1];//需改动
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50L);
                    // 结算【三连号 二连号】
                    logger.info("德州快三连号开始进入结算：期号：{},开奖号码{}", str[1], str[2]);
                    betKsService.clearingKsLh(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.DZKS.getTagType()));
                }
            }
        } catch (Exception e) {
            logger.error("德州快三连号结算出错：结算期号" + message, e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /* 德州快三【三不同号、胆拖 三同号单选，三同号通选 】 计算结果
     * @param message 消息内容【期号】
     * */
    @JmsListener(destination = ActiveMQConfig.LIVE_TOPIC_KS_DZ_THREE, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgDzksSbTh(String message) {
        logger.info("【德州快三三同三不同】结算期号：  " + message);
        String[] str = message.split(":");// 拆分消息内容
        // 获取唯一
        String key = ActiveMQConfig.LIVE_TOPIC_KS_DZ_THREE + str[1];//需改动
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50L);
                    // 结算【三不同号、胆拖 三同号单选，三同号通选】
                    logger.info("德州快三同三不同开始进入结算：期号：{},开奖号码{}", str[1], str[2]);
                    betKsService.clearingKsSbTh(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.DZKS.getTagType()));
                }
            }
        } catch (Exception e) {
            logger.error("德州快三三同三不同结算出错：结算期号" + message, e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /* 德州快三【二不同号、胆拖 二同号单选，二同号通选 】 计算结果
     * @param message 消息内容【期号】
     * */
    @JmsListener(destination = ActiveMQConfig.LIVE_TOPIC_KS_DZ_TWO, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgDzksEbTh(String message) {
        logger.info("【德州快三】二同号二不同号结算期号：  " + message);
        String[] str = message.split(":");// 拆分消息内容
        // 获取唯一
        String key = ActiveMQConfig.LIVE_TOPIC_KS_DZ_TWO + str[1];//需改动
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50L);
                    // 结算【二不同号、胆拖 二同号单选，二同号通选】
                    logger.info("德州快二同号二不同号开始进入结算：期号：{},开奖号码{}", str[1], str[2]);
                    betKsService.clearingKsEbTh(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.DZKS.getTagType()));
                }
            }
        } catch (Exception e) {
            logger.error("德州快二同号二不同号三结算出错：结算期号" + message, e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /* 德州快三【预期数据同步】
     * @param message 消息内容【期号】
     * */
    @JmsListener(destination = ActiveMQConfig.LIVE_TOPIC_KS_DZ_YUQI_DATA, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgDzksYuqiData(String message) {
        logger.info("【德州快三】预期数据同步：mq:{},message:{}" ,ActiveMQConfig.LIVE_TOPIC_KS_DZ_YUQI_DATA,message);
        List<DzksLotterySg> list = JSONObject.parseArray(message.substring(message.indexOf(":") + 1), DzksLotterySg.class);
        // 获取唯一
        String key = ActiveMQConfig.LIVE_TOPIC_KS_DZ_YUQI_DATA + list.get(0).getIssue();
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50L);
                    for (DzksLotterySg sg : list) {
                        DzksLotterySgExample dzksLotterySgExample = new DzksLotterySgExample();
                        DzksLotterySgExample.Criteria criteria = dzksLotterySgExample.createCriteria();
                        criteria.andIssueEqualTo(sg.getIssue());
                        if (dzksLotterySgMapper.selectOneByExample(dzksLotterySgExample) == null) {
                            dzksLotterySgMapper.insertSelective(sg);
                        }

                    }
                }
            }

        } catch (Exception e) {
            logger.error("processMsgTenDZksYuqiData occur error:", e);
        } finally {
            lock.writeLock().unlock();
        }
    }



}
