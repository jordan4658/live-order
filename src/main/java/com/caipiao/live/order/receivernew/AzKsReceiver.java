package com.caipiao.live.order.receivernew;

import com.alibaba.fastjson.JSONObject;
import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.util.redis.RedisBusinessUtil;
import com.caipiao.live.order.config.ActiveMQConfig;
import com.caipiao.live.order.service.bet.BetKsService;
import com.caipiao.live.order.service.result.AzksLotterySgWriteService;
import com.caipiao.live.common.constant.RedisKeys;
import com.caipiao.live.common.enums.lottery.CaipiaoTypeEnum;
import com.caipiao.live.common.enums.lottery.LotteryTableNameEnum;
import com.caipiao.live.common.mybatis.entity.AzksLotterySg;
import com.caipiao.live.common.mybatis.entity.AzksLotterySgExample;
import com.caipiao.live.common.mybatis.mapper.AzksLotterySgMapper;
import com.caipiao.live.common.mybatis.mapperext.sg.AzksLotterySgMapperExt;
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

@Component
public class AzKsReceiver {
    private static final Logger logger = LoggerFactory.getLogger(AzKsReceiver.class);
    @Autowired
    private AzksLotterySgMapper azksLotterySgMapper;
    @Autowired
    private AzksLotterySgWriteService azksLotterySgWriteService;
    @Autowired
    private BetKsService betksService;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private BasicRedisClient basicRedisClient;
    @Autowired
    private JmsMessagingTemplate jmsMessagingTemplate;
    @Autowired
    private AzksLotterySgMapperExt azksLotterySgMapperExt;


    /* 澳洲快三【两面】 计算结果
     * @param message 消息内容【期号】
     * */
    @JmsListener(destination = ActiveMQConfig.LIVE_TOPIC_KS_AZ_HZ, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgAzksLm(String message) {
        logger.info("【澳洲快三】{}两面和值结算期号{}，",ActiveMQConfig.LIVE_TOPIC_KS_AZ_HZ, message);
        String[] str = message.split(":");// 拆分消息内容
        // 获取唯一
        String key = ActiveMQConfig.LIVE_TOPIC_KS_AZ_HZ + str[1];
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50L);
                    //同步中奖结果
                    AzksLotterySg updateSg = JSONObject.parseObject(str[3].replace("$", ":"), AzksLotterySg.class);
                    int boolUpdate = azksLotterySgMapperExt.updateByIssue(updateSg);
                    if (boolUpdate == 0) {//若为o，检查是数据库表中是否有该期号
                        AzksLotterySgExample sgExample = new AzksLotterySgExample();
                        AzksLotterySgExample.Criteria criteria = sgExample.createCriteria();
                        LocalDate today = LocalDate.now();//当天日期
                        criteria.andIdealTimeGreaterThanOrEqualTo(DateUtils.getLocalDateToDate(today));
                        LocalDate tomrrow = today.plusDays(1L);//第二天
                        criteria.andIdealTimeLessThan(DateUtils.getLocalDateToDate(tomrrow));
                        sgExample.setOrderByClause("`ideal_time` asc");
                        int issueCount = azksLotterySgMapper.countByExample(sgExample);
                        if (issueCount < 288) {
                            //则发送消息
                            Destination destination = new ActiveMQTopic(ActiveMQConfig.LIVE_TOPIC_YUQI_TODAYT);
                            jmsMessagingTemplate.convertAndSend(destination, "azksYuqiToday");
                            logger.info("澳洲快三同步预期数据发送通知：azksYuqiToday mq：{}",ActiveMQConfig.LIVE_TOPIC_YUQI_TODAYT);

                        }
                    } else {
                        //更新历史赛果缓存
                        RedisBusinessUtil.updateLsSgCache(Constants.LOTTERY_AZKS, RedisKeys.AZKS_SG_HS_LIST, updateSg);
                    }


                    azksLotterySgWriteService.cacheIssueResultForAzKs(str[1], str[2]);

                    // 结算【澳洲快三】
                    logger.info("澳洲快三两面和值开始进入结算：期号：{},开奖号码{}", str[1], str[2]);
                    betksService.clearingKsLm(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.AZKS.getTagType()));
                    //第三步 最近1天未开奖的数据
                    AzksLotterySgExample azksLotterySgExample = new AzksLotterySgExample();
                    AzksLotterySgExample.Criteria criteria = azksLotterySgExample.createCriteria();
                    criteria.andOpenStatusEqualTo(Constants.WAIT);
                    LocalDateTime localDateTime = LocalDateTime.now();
                    LocalDateTime localDateTime1 = localDateTime.minusDays(1);
                    Date today = DateUtils.getLocalDateTimeToDate(localDateTime);
                    Date yersterday = DateUtils.getLocalDateTimeToDate(localDateTime1);
                    criteria.andIdealTimeLessThan(today);
                    criteria.andIdealTimeGreaterThanOrEqualTo(yersterday);
                    List<AzksLotterySg> azksLotterySgs = azksLotterySgMapper.selectByExample(azksLotterySgExample);
                    if (azksLotterySgs.size() > 0) {
                        String issues = "";
                        for (AzksLotterySg sg : azksLotterySgs) {
                            issues = issues + sg.getIssue() + ",";
                        }
                        issues = issues.substring(0, issues.length() - 1);
                        Destination destination = new ActiveMQQueue(ActiveMQConfig.LIVE_TOPIC_MISSING_LOTTERY_SG);
                        logger.info("澳洲快三missing据发送通知： mq：{}",ActiveMQConfig.LIVE_TOPIC_MISSING_LOTTERY_SG);
                        jmsMessagingTemplate.convertAndSend(destination, LotteryTableNameEnum.AZKS.getLotteryId() + "#" + issues);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("澳洲快三两面和值结算出错：结算期号" + message, e);
        } finally {
            lock.writeLock().unlock();
        }

    }

    /* 澳洲快三【独胆】 计算结果
     * @param message 消息内容【期号】
     * */
    @JmsListener(destination = ActiveMQConfig.LIVE_TOPIC_KS_AZ_DD, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgAzksDd(String message) {
        logger.info("【澳洲快三】独胆结算期号：{},{}  ",ActiveMQConfig.LIVE_TOPIC_KS_AZ_DD,message);
        String[] str = message.split(":");// 拆分消息内容
        // 获取唯一
        String key = ActiveMQConfig.LIVE_TOPIC_KS_AZ_DD + str[1];//需改动
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50L);
                    // 结算【独胆】
                    logger.info("澳洲快三独胆开始进入结算：期号：{},开奖号码{}", str[1], str[2]);
                    betksService.clearingKsDd(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.AZKS.getTagType()));
                }
            }
        } catch (Exception e) {
            logger.error("澳洲快三独胆结算出错：结算期号" + message, e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /* 澳洲快三【连号】 计算结果 三连号 二连号
     * @param message 消息内容【期号】
     * */
    @JmsListener(destination = ActiveMQConfig.LIVE_TOPIC_KS_AZ_LH, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgAzksLh(String message) {
        logger.info("【澳洲快三】连号结算期号：{},{}",ActiveMQConfig.LIVE_TOPIC_KS_AZ_LH,  message);
        String[] str = message.split(":");// 拆分消息内容
        // 获取唯一
        String key = ActiveMQConfig.LIVE_TOPIC_KS_AZ_LH + str[1];//需改动
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50L);
                    // 结算【三连号 二连号】
                    logger.info("澳洲快三连号开始进入结算：期号：{},开奖号码{}", str[1], str[2]);
                    betksService.clearingKsLh(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.AZKS.getTagType()));
                }
            }
        } catch (Exception e) {
            logger.error("澳洲快三连号结算出错：结算期号" + message, e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /* 澳洲快三【三不同号、胆拖 三同号单选，三同号通选 】 计算结果
     * @param message 消息内容【期号】
     * */
    @JmsListener(destination = ActiveMQConfig.LIVE_TOPIC_KS_AZ_THREE, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgAzksSbTh(String message) {
        logger.info("【澳洲快三】三不同号三同号结算期号：  " + message);
        String[] str = message.split(":");// 拆分消息内容
        // 获取唯一
        String key = ActiveMQConfig.LIVE_TOPIC_KS_AZ_THREE + str[1];//需改动
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50L);
                    // 结算【三不同号、胆拖 三同号单选，三同号通选】
                    logger.info("澳洲快三三不同号三同开始进入结算：期号：{},开奖号码{}", str[1], str[2]);
                    betksService.clearingKsSbTh(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.AZKS.getTagType()));
                }
            }
        } catch (Exception e) {
            logger.error("澳洲快三三不同号三同结算出错：结算期号" + message, e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /* 澳洲快三【二不同号、胆拖 二同号单选，二同号通选 】 计算结果
     * @param message 消息内容【期号】
     * */
    @JmsListener(destination = ActiveMQConfig.LIVE_TOPIC_KS_AZ_TWO, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgAzksEbTh(String message) {
        logger.info("【澳洲快三】二不同号二同号结算期号：  " + message);
        String[] str = message.split(":");// 拆分消息内容
        // 获取唯一
        String key = ActiveMQConfig.LIVE_TOPIC_KS_AZ_TWO + str[1];//需改动
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 当前线程休息2秒
            Thread.sleep(2000);
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50L);
                    // 结算【二不同号、胆拖 二同号单选，二同号通选】
                    logger.info("澳洲快三二不同号二同号开始进入结算：期号：{},开奖号码{}", str[1], str[2]);
                    betksService.clearingKsEbTh(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.AZKS.getTagType()));
                }
            }
        } catch (Exception e) {
            logger.error("澳洲快三二不同号二同号结算出错：结算期号" + message, e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /* 澳洲快三【预期数据同步】
     * @param message 消息内容【期号】
     * */
    @JmsListener(destination = ActiveMQConfig.LIVE_TOPIC_KS_AZ_YUQI_DATA, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgAzksYuqiData(String message) {
        logger.info("【澳洲快三】预期数据同步：mq:{},message{} " , ActiveMQConfig.LIVE_TOPIC_KS_AZ_YUQI_DATA, message);
        List<AzksLotterySg> list = JSONObject.parseArray(message.substring(message.indexOf(":") + 1), AzksLotterySg.class);
        // 获取唯一
        String key = ActiveMQConfig.LIVE_TOPIC_KS_AZ_YUQI_DATA + list.get(0).getIssue();
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 当前线程休息2秒
            Thread.sleep(2000);
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50L);
                    for (AzksLotterySg sg : list) {
                        AzksLotterySgExample azksLotterySgexample = new AzksLotterySgExample();
                        AzksLotterySgExample.Criteria criteria = azksLotterySgexample.createCriteria();
                        criteria.andIssueEqualTo(sg.getIssue());
                        if (azksLotterySgMapper.selectOneByExample(azksLotterySgexample) == null) {
                            azksLotterySgMapper.insertSelective(sg);
                        }

                    }
                }
            }

        } catch (Exception e) {
            logger.error("processMsgTenAZksYuqiData occur error:", e);
        } finally {
            lock.writeLock().unlock();
        }
    }



}