package com.caipiao.live.order.receiver;

import com.alibaba.fastjson.JSONObject;
import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.constant.RedisKeys;
import com.caipiao.live.common.enums.lottery.CaipiaoTypeEnum;
import com.caipiao.live.common.enums.lottery.LotteryTableNameEnum;
import com.caipiao.live.common.mybatis.entity.*;
import com.caipiao.live.common.mybatis.mapper.AmlhcLotterySgMapper;
import com.caipiao.live.common.mybatis.mapper.FivelhcLotterySgMapper;
import com.caipiao.live.common.mybatis.mapper.LhcHandicapMapper;
import com.caipiao.live.common.mybatis.mapper.OnelhcLotterySgMapper;
import com.caipiao.live.common.mybatis.mapperext.sg.AmlhcLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.FivelhcLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.OnelhcLotterySgMapperExt;
import com.caipiao.live.common.util.DateUtils;
import com.caipiao.live.common.util.StringUtils;
import com.caipiao.live.common.util.redis.BasicRedisClient;
import com.caipiao.live.common.util.redis.RedisBusinessUtil;
import com.caipiao.live.order.config.ActiveMQConfig;
import com.caipiao.live.order.service.bet.BetLhcService;
import com.caipiao.live.order.service.result.OnelhcLotterySgWriteService;
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
 * @create 2018-09-21 17:41
 **/
@Component
public class LhcReceiver {
    private static final Logger logger = LoggerFactory.getLogger(LhcReceiver.class);

    @Autowired
    private BetLhcService betLhcService;
    @Autowired
    private OnelhcLotterySgMapper onelhcLotterySgMapper;
    @Autowired
    private OnelhcLotterySgMapperExt onelhcLotterySgMapperExt;
    @Autowired
    private FivelhcLotterySgMapper fivelhcLotterySgMapper;
    @Autowired
    private FivelhcLotterySgMapperExt fivelhcLotterySgMapperExt;
    @Autowired
    private AmlhcLotterySgMapper amlhcLotterySgMapper;
    @Autowired
    private AmlhcLotterySgMapperExt amlhcLotterySgMapperExt;
    @Autowired
    private OnelhcLotterySgWriteService onelhcLotterySgWriteService;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private BasicRedisClient basicRedisClient;
    @Autowired
    private LhcHandicapMapper lhcHandicapMapper;
    @Autowired
    private JmsMessagingTemplate jmsMessagingTemplate;


    //    @RabbitListener(queues = RabbitConfig.TOPIC_ONELHC_TM_ZT_LX)
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_ONELHC_TM_ZT_LX, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgONELhcZmZtLm(String message) throws Exception {
        logger.info("1分六合彩 - 【特码,正特,六肖,正码1-6】结算期号：  " + message);
        String[] num = message.split(":");
        // 获取期号
        String issue = num[1];
        String key = ActiveMQConfig.PLATFORM_TOPIC_ONELHC_TM_ZT_LX + num[1];

        // 【分布式读写锁】
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    //第一步同步中奖结果
                    OnelhcLotterySg updateSg = JSONObject.parseObject(num[3].replace("$", ":"), OnelhcLotterySg.class);
                    int boolUpdate = onelhcLotterySgMapperExt.updateByIssue(updateSg);
                    if (boolUpdate == 0) {  //说明这一天的期号数据可能不全，则做一次检查，如果真没有，则从task_server同步当天所有期号数据过来
                        OnelhcLotterySgExample sgExample = new OnelhcLotterySgExample();
                        OnelhcLotterySgExample.Criteria criteria = sgExample.createCriteria();
                        Calendar calendar = Calendar.getInstance();
                        criteria.andIdealTimeGreaterThanOrEqualTo(DateUtils.formatDate(new Date(), DateUtils.FORMAT_YYYY_MM_DD));
                        calendar.add(Calendar.DAY_OF_MONTH, 1);
                        criteria.andIdealTimeLessThan(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD));
                        int afterCount = onelhcLotterySgMapper.countByExample(sgExample);
                        if (afterCount < 1440) {
                            //则发送消息
                            logger.info("同步预期数据发送通知：oneLhcYuqiToday");
                            Destination destination = new ActiveMQQueue(ActiveMQConfig.PLATFORM_TOPIC_YUQI_TODAYT);
                            jmsMessagingTemplate.convertAndSend(destination, "oneLhcYuqiToday");
                        }
                    } else {
                        //更新历史赛果缓存
                        RedisBusinessUtil.updateLsSgCache(Constants.LOTTERY_ONELHC, RedisKeys.ONELHC_SG_HS_LIST, updateSg);
                    }

                    // 结算六合彩- 【特码,正特,六肖,正码1-6】
                    betLhcService.clearingLhcTeMaA(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                    betLhcService.clearingLhcZhengTe(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                    betLhcService.clearingLhcZhengMaOneToSix(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                    betLhcService.clearingLhcLiuXiao(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);

                    //第三步 最近1天未开奖的数据
                    OnelhcLotterySgExample onelhcLotterySgExample = new OnelhcLotterySgExample();
                    OnelhcLotterySgExample.Criteria criteria = onelhcLotterySgExample.createCriteria();
                    criteria.andOpenStatusEqualTo(Constants.WAIT);
                    Calendar calendar = Calendar.getInstance();
                    criteria.andIdealTimeLessThan(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                    calendar.add(Calendar.DAY_OF_MONTH, -1);
                    criteria.andIdealTimeGreaterThanOrEqualTo(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                    onelhcLotterySgExample.setOrderByClause("ideal_time desc");
                    onelhcLotterySgExample.setLimit(15);
                    onelhcLotterySgExample.setOffset(0);
                    List<OnelhcLotterySg> onelhcLotterySgList = onelhcLotterySgMapper.selectByExample(onelhcLotterySgExample);
                    if (onelhcLotterySgList.size() > 0) {
                        String issues = "";
                        for (OnelhcLotterySg sg : onelhcLotterySgList) {
                            issues = issues + sg.getIssue() + ",";
                        }
                        issues = issues.substring(0, issues.length() - 1);
                        Destination destination = new ActiveMQQueue(ActiveMQConfig.PLATFORM_TOPIC_MISSING_LOTTERY_SG);
                        jmsMessagingTemplate.convertAndSend(destination, LotteryTableNameEnum.ONELHC.getLotteryId() + "#" + issues);
                        logger.info("发送缺奖数据{}，{}", LotteryTableNameEnum.ONELHC.getLotteryId(), issues);
                    }
                    // 缓存一分六合彩开奖结果
                    onelhcLotterySgWriteService.cacheIssueResultForQnelhc(issue, num[2]);

                }
            }

        } catch (Exception e) {
            logger.error("processMsgONELhcZmZtLm occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }

    }

    //    @RabbitListener(queues = RabbitConfig.TOPIC_FIVELHC_TM_ZT_LX)
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_FIVELHC_TM_ZT_LX, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgFIVELhcZmZtLm(String message) throws Exception {
        logger.info("六合彩 - 【特码,正特,六肖,正码1-6】结算期号：  " + message);
        String[] num = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_FIVELHC_TM_ZT_LX + num[1];
        // 【分布式读写锁】
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    //第一步同步中奖结果
                    FivelhcLotterySg updateSg = JSONObject.parseObject(num[3].replace("$", ":"), FivelhcLotterySg.class);
                    int boolUpdate = fivelhcLotterySgMapperExt.updateByIssue(updateSg);
                    if (boolUpdate == 0) {  //说明这一天的期号数据可能不全，则做一次检查，如果真没有，则从task_server同步当天所有期号数据过来
                        FivelhcLotterySgExample sgExample = new FivelhcLotterySgExample();
                        FivelhcLotterySgExample.Criteria criteria = sgExample.createCriteria();
                        Calendar calendar = Calendar.getInstance();
                        criteria.andIdealTimeGreaterThanOrEqualTo(DateUtils.formatDate(new Date(), DateUtils.FORMAT_YYYY_MM_DD));
                        calendar.add(Calendar.DAY_OF_MONTH, 1);
                        criteria.andIdealTimeLessThan(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD));
                        int afterCount = fivelhcLotterySgMapper.countByExample(sgExample);
                        if (afterCount < 288) {
                            //则发送消息
                            logger.info("同步预期数据发送通知：fiveLhcYuqiToday");
                            Destination destination = new ActiveMQQueue(ActiveMQConfig.PLATFORM_TOPIC_YUQI_TODAYT);
                            jmsMessagingTemplate.convertAndSend(destination, "fiveLhcYuqiToday");
                        }
                    } else {
                        //更新历史赛果缓存
                        RedisBusinessUtil.updateLsSgCache(Constants.LOTTERY_FIVELHC, RedisKeys.FIVELHC_SG_HS_LIST, updateSg);
                    }

                    // 结算六合彩- 【特码,正特,六肖,正码1-6】
                    betLhcService.clearingLhcTeMaA(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                    betLhcService.clearingLhcZhengTe(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                    betLhcService.clearingLhcZhengMaOneToSix(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                    betLhcService.clearingLhcLiuXiao(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);

                    //第三步 最近1天未开奖的数据
                    FivelhcLotterySgExample fivelhcLotterySgExample = new FivelhcLotterySgExample();
                    FivelhcLotterySgExample.Criteria criteria = fivelhcLotterySgExample.createCriteria();
                    criteria.andOpenStatusEqualTo(Constants.WAIT);
                    Calendar calendar = Calendar.getInstance();
                    criteria.andIdealTimeLessThan(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                    calendar.add(Calendar.DAY_OF_MONTH, -1);
                    criteria.andIdealTimeGreaterThanOrEqualTo(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                    fivelhcLotterySgExample.setOrderByClause("ideal_time desc");
                    fivelhcLotterySgExample.setLimit(15);
                    fivelhcLotterySgExample.setOffset(0);
                    List<FivelhcLotterySg> fivelhcLotterySgList = fivelhcLotterySgMapper.selectByExample(fivelhcLotterySgExample);
                    if (fivelhcLotterySgList.size() > 0) {
                        String issues = "";
                        for (FivelhcLotterySg sg : fivelhcLotterySgList) {
                            issues = issues + sg.getIssue() + ",";
                        }
                        issues = issues.substring(0, issues.length() - 1);
                        Destination destination = new ActiveMQQueue(ActiveMQConfig.PLATFORM_TOPIC_MISSING_LOTTERY_SG);
                        jmsMessagingTemplate.convertAndSend(destination, LotteryTableNameEnum.FIVELHC.getLotteryId() + "#" + issues);
                        logger.info("发送缺奖数据{}，{}", LotteryTableNameEnum.FIVELHC.getLotteryId(), issues);
                    }
                    betLhcService.cacheIssueResultForFivelhc(num[1], num[2]);

                }
            }

        } catch (Exception e) {
            logger.error("processMsgFIVELhcZmZtLm occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }


    }

    //    @RabbitListener(queues = RabbitConfig.TOPIC_SSLHC_TM_ZT_LX)
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_AMLHC_TM_ZT_LX, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgSSLhcZmZtLm(String message) throws Exception {
        logger.info("六合彩 - 【特码,正特,六肖,正码1-6】结算期号：  " + message);
        String[] num = message.split(":");

        // 获取期唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_AMLHC_TM_ZT_LX + num[1];
        // 【分布式读写锁】
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    //第一步同步中奖结果
                    AmlhcLotterySg updateSg = JSONObject.parseObject(num[3].replace("$", ":"), AmlhcLotterySg.class);
                    int boolUpdate = amlhcLotterySgMapperExt.updateByIssue(updateSg);
                    if (boolUpdate == 0) {  //说明这一天的期号数据可能不全，则做一次检查，如果真没有，则从task_server同步当天所有期号数据过来
                        AmlhcLotterySgExample sgExample = new AmlhcLotterySgExample();
                        AmlhcLotterySgExample.Criteria criteria = sgExample.createCriteria();
                        Calendar calendar = Calendar.getInstance();
                        criteria.andIdealTimeGreaterThanOrEqualTo(DateUtils.formatDate(new Date(), DateUtils.FORMAT_YYYY_MM_DD));
                        calendar.add(Calendar.DAY_OF_MONTH, 1);
                        criteria.andIdealTimeLessThan(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD));
                        int afterCount = amlhcLotterySgMapper.countByExample(sgExample);
                        if (afterCount < 144) {
                            //则发送消息
                            logger.info("同步预期数据发送通知：ssLhcYuqiToday");
                            Destination destination = new ActiveMQQueue(ActiveMQConfig.PLATFORM_TOPIC_YUQI_TODAYT);
                            jmsMessagingTemplate.convertAndSend(destination, "ssLhcYuqiToday");
                        }
                    } else {
                        //更新历史赛果缓存
                        RedisBusinessUtil.updateLsSgCache(Constants.LOTTERY_AMLHC, RedisKeys.AMLHC_SG_HS_LIST, updateSg);
                    }

                    // 结算六合彩- 【特码,正特,六肖,正码1-6】
                    betLhcService.clearingLhcTeMaA(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                    betLhcService.clearingLhcZhengTe(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                    betLhcService.clearingLhcZhengMaOneToSix(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                    betLhcService.clearingLhcLiuXiao(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);

                    //第三步 最近1天未开奖的数据
                    AmlhcLotterySgExample amlhcLotterySgExample = new AmlhcLotterySgExample();
                    AmlhcLotterySgExample.Criteria criteria = amlhcLotterySgExample.createCriteria();
                    criteria.andOpenStatusEqualTo(Constants.WAIT);
                    Calendar calendar = Calendar.getInstance();
                    criteria.andIdealTimeLessThan(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                    calendar.add(Calendar.DAY_OF_MONTH, -1);
                    criteria.andIdealTimeGreaterThanOrEqualTo(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                    amlhcLotterySgExample.setOrderByClause("ideal_time desc");
                    amlhcLotterySgExample.setLimit(15);
                    amlhcLotterySgExample.setOffset(0);
                    List<AmlhcLotterySg> sslhcLotterySgList = amlhcLotterySgMapper.selectByExample(amlhcLotterySgExample);
                    if (sslhcLotterySgList.size() > 0) {
                        String issues = "";
                        for (AmlhcLotterySg sg : sslhcLotterySgList) {
                            issues = issues + sg.getIssue() + ",";
                        }
                        issues = issues.substring(0, issues.length() - 1);
                        Destination destination = new ActiveMQQueue(ActiveMQConfig.PLATFORM_TOPIC_MISSING_LOTTERY_SG);
                        jmsMessagingTemplate.convertAndSend(destination, LotteryTableNameEnum.AMLHC.getLotteryId() + "#" + issues);
                        logger.info("发送缺奖数据{}，{}", LotteryTableNameEnum.AMLHC.getLotteryId(), issues);
                    }
                    // 更改赛果
                    betLhcService.cacheIssueResultForSslhc(num[1], num[2]);

                }
            }

        } catch (Exception e) {
            logger.error("processMsgSSLhcZmZtLm occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }
    }


    //    @RabbitListener(queues = RabbitConfig.TOPIC_ONELHC_ZM_BB_WS)
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_ONELHC_ZM_BB_WS, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgONELhcZmBbWs(String message) throws Exception {
        logger.info("六合彩 - 【正码,半波,尾数】结算期号：  " + message);
        String[] num = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_ONELHC_ZM_BB_WS + num[1];
        // 【分布式读写锁】
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    // 结算六合彩- 【正码,半波,尾数】
                    betLhcService.clearingLhcZhengMaA(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                    betLhcService.clearingLhcBanBo(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                    betLhcService.clearingLhcWs(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                }
            }
        } catch (Exception e) {
            logger.error("processMsgONELhcZmBbWs occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }

    }

    //    @RabbitListener(queues = RabbitConfig.TOPIC_FIVELHC_ZM_BB_WS)
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_FIVELHC_ZM_BB_WS, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgFIVELhcZmBbWs(String message) throws Exception {
        logger.info("六合彩 - 【正码,半波,尾数】结算期号：  " + message);
        String[] num = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_FIVELHC_ZM_BB_WS + num[1];
        // 【分布式读写锁】
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    // 结算六合彩- 【正码,半波,尾数】
                    betLhcService.clearingLhcZhengMaA(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                    betLhcService.clearingLhcBanBo(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                    betLhcService.clearingLhcWs(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                }
            }
        } catch (Exception e) {
            logger.error("processMsgFIVELhcZmBbWs occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    //    @RabbitListener(queues = RabbitConfig.TOPIC_SSLHC_ZM_BB_WS)
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_AMLHC_ZM_BB_WS, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgSSLhcZmBbWs(String message) throws Exception {
        logger.info("六合彩 - 【正码,半波,尾数】结算期号：  " + message);
        String[] num = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_AMLHC_ZM_BB_WS + num[1];
        // 【分布式读写锁】
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    // 结算六合彩- 【正码,半波,尾数】
                    betLhcService.clearingLhcZhengMaA(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                    betLhcService.clearingLhcBanBo(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                    betLhcService.clearingLhcWs(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                }
            }
        } catch (Exception e) {
            logger.error("processMsgSSLhcZmBbWs occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    //    @RabbitListener(queues = RabbitConfig.TOPIC_ONELHC_LM_LX_LW)
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_ONELHC_LM_LX_LW, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgONELhcLmLxLw(String message) throws Exception {
        logger.info("六合彩 - 【连码,连肖,连尾】结算期号：  " + message);
        String[] num = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_ONELHC_LM_LX_LW + num[1];
        // 【分布式读写锁】
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            // 判断是否获取到锁
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    // 结算六合彩- 【连码,连肖,连尾】
                    betLhcService.clearingLhcLianMa(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                    betLhcService.clearingLhcLianXiao(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                    betLhcService.clearingLhcLianWei(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                }
            }

        } catch (Exception e) {
            logger.error("processMsgONELhcLmLxLw occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }

    }

    //    @RabbitListener(queues = RabbitConfig.TOPIC_FIVELHC_LM_LX_LW)
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_FIVELHC_LM_LX_LW, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgFIVELhcLmLxLw(String message) throws Exception {
        logger.info("六合彩 - 【连码,连肖,连尾】结算期号：  " + message);
        String[] num = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_FIVELHC_LM_LX_LW + num[1];
        // 【分布式读写锁】
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");

        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            // 判断是否获取到锁
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    // 结算六合彩- 【连码,连肖,连尾】
                    betLhcService.clearingLhcLianMa(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                    betLhcService.clearingLhcLianXiao(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                    betLhcService.clearingLhcLianWei(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                }
            }

        } catch (Exception e) {
            logger.error("processMsgFIVELhcLmLxLw occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }

    }

    //    @RabbitListener(queues = RabbitConfig.TOPIC_SSLHC_LM_LX_LW)
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_AMLHC_LM_LX_LW, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgSSLhcLmLxLw(String message) throws Exception {
        logger.info("六合彩 - 【连码,连肖,连尾】结算期号：  " + message);
        String[] num = message.split(":");

        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_AMLHC_LM_LX_LW + num[1];
        // 【分布式读写锁】
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    // 结算六合彩- 【连码,连肖,连尾】
                    betLhcService.clearingLhcLianMa(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                    betLhcService.clearingLhcLianXiao(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                    betLhcService.clearingLhcLianWei(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                }
            }

        } catch (Exception e) {
            logger.error("processMsgSSLhcLmLxLw occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }
    }


    //    @RabbitListener(queues = RabbitConfig.TOPIC_ONELHC_BZ_LH_WX)
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_ONELHC_BZ_LH_WX, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgONELhcBzLhWx(String message) throws Exception {
        logger.info("六合彩 - 【不中,1-6龙虎,五行】结算期号：  " + message);
        String[] num = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_ONELHC_BZ_LH_WX + num[1];
        // 【分布式读写锁】
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            // 判断是否获取到锁
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    // 结算六合彩- 【不中,1-6龙虎,五行】
                    betLhcService.clearingLhcNoOpen(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                    betLhcService.clearingLhcOneSixLh(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                    betLhcService.clearingLhcWuxing(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                }
            }

        } catch (Exception e) {
            logger.error("processMsgONELhcBzLhWx occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }

    }

    //    @RabbitListener(queues = RabbitConfig.TOPIC_FIVELHC_BZ_LH_WX)
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_FIVELHC_BZ_LH_WX, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgFIVELhcBzLhWx(String message) throws Exception {
        logger.info("六合彩 - 【不中,1-6龙虎,五行】结算期号：  " + message);
        String[] num = message.split(":");

        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_FIVELHC_BZ_LH_WX + num[1];
        // 【分布式读写锁】
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            // 判断是否获取到锁
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    // 结算六合彩- 【不中,1-6龙虎,五行】
                    betLhcService.clearingLhcNoOpen(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                    betLhcService.clearingLhcOneSixLh(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                    betLhcService.clearingLhcWuxing(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                }
            }

        } catch (Exception e) {
            logger.error("processMsgFIVELhcBzLhWx occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }

    }

    //    @RabbitListener(queues = RabbitConfig.TOPIC_SSLHC_BZ_LH_WX)
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_AMLHC_BZ_LH_WX, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgSSLhcBzLhWx(String message) throws Exception {
        logger.info("六合彩 - 【不中,1-6龙虎,五行】结算期号：  " + message);
        String[] num = message.split(":");

        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_AMLHC_BZ_LH_WX + num[1];
        // 【分布式读写锁】
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            // 判断是否获取到锁
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    // 结算六合彩- 【不中,1-6龙虎,五行】
                    betLhcService.clearingLhcNoOpen(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                    betLhcService.clearingLhcOneSixLh(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                    betLhcService.clearingLhcWuxing(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                }
            }

        } catch (Exception e) {
            logger.error("processMsgSSLhcBzLhWx occur error:{}", e);
        } finally {
            // 释放锁
            lock.writeLock().unlock();
        }
    }

    //    @RabbitListener(queues = RabbitConfig.TOPIC_ONELHC_PT_TX)
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_ONELHC_PT_TX, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgONELhcPtTx(String message) throws Exception {
        logger.info("六合彩 - 【平特,特肖】结算期号：  " + message);
        String[] num = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_ONELHC_PT_TX + num[1];
        // 【分布式读写锁】
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    // 结算六合彩- 【平特,特肖】
                    betLhcService.clearingLhcPtPt(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                    betLhcService.clearingLhcTxTx(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                }
            }
        } catch (Exception e) {
            logger.error("processMsgONELhcPtTx occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    //    @RabbitListener(queues = RabbitConfig.TOPIC_FIVELHC_PT_TX)
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_FIVELHC_PT_TX, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgFIVELhcPtTx(String message) throws Exception {
        logger.info("六合彩 - 【平特,特肖】结算期号：  " + message);
        String[] num = message.split(":");

        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_FIVELHC_PT_TX + num[1];
        // 【分布式读写锁】
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    // 结算六合彩- 【平特,特肖】
                    betLhcService.clearingLhcPtPt(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                    betLhcService.clearingLhcTxTx(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                }
            }
        } catch (Exception e) {
            logger.error("processMsgFIVELhcPtTx occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }

    }

    //    @RabbitListener(queues = RabbitConfig.TOPIC_SSLHC_PT_TX)
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_AMLHC_PT_TX, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgSSLhcPtTx(String message) throws Exception {
        logger.info("六合彩 - 【平特,特肖】结算期号：  " + message);
        String[] num = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_AMLHC_PT_TX + num[1];
        // 【分布式读写锁】
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    // 结算六合彩- 【平特,特肖】
                    betLhcService.clearingLhcPtPt(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                    betLhcService.clearingLhcTxTx(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                }
            }

        } catch (Exception e) {
            logger.error("processMsgSSLhcPtTx occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }

    }

    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_ONELHC_YUQI_DATA, containerFactory = "jmsListenerContainerTopicDurable")
    public void sameDataYuqiOneLhc(String message) throws Exception {
        logger.info("【1分六合彩】预期数据同步：  " + message);
        List<OnelhcLotterySg> list = JSONObject.parseArray(message.substring(message.indexOf(":") + 1), OnelhcLotterySg.class);
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_ONELHC_YUQI_DATA + list.get(0).getIssue();

        // 【分布式读写锁】
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 20, TimeUnit.SECONDS);
            if (bool) {
                boolean needUpdate = false;
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    for (OnelhcLotterySg sg : list) {
                        OnelhcLotterySgExample onelhcLotterySgExample = new OnelhcLotterySgExample();
                        OnelhcLotterySgExample.Criteria criteria = onelhcLotterySgExample.createCriteria();
                        criteria.andIssueEqualTo(sg.getIssue());
                        if (needUpdate == false && onelhcLotterySgMapper.selectOneByExample(onelhcLotterySgExample) == null) {
                            needUpdate = true;
                        }

                        sg.setNumber(StringUtils.isEmpty(sg.getNumber()) ? "" : sg.getNumber());
                        sg.setTime(StringUtils.isEmpty(sg.getTime()) ? "" : sg.getTime());

                    }
                    if (needUpdate) {
                        onelhcLotterySgMapperExt.insertBatch(list);
                    }
                }
            }

        } catch (Exception e) {
            logger.error("【1分六合彩】预期数据同步出错", e);
        } finally {
            lock.writeLock().unlock();
        }

    }

    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_FIVELHC_YUQI_DATA, containerFactory = "jmsListenerContainerTopicDurable")
    public void sameDataYuqiFiveLhc(String message) throws Exception {
        logger.info("【5分六合彩】预期数据同步：  " + message);

        List<FivelhcLotterySg> list = JSONObject.parseArray(message.substring(message.indexOf(":") + 1), FivelhcLotterySg.class);
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_FIVELHC_YUQI_DATA + list.get(0).getIssue();
        // 【分布式读写锁】
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 60l);
                    for (FivelhcLotterySg sg : list) {
                        FivelhcLotterySgExample fivelhcLotterySgExample = new FivelhcLotterySgExample();
                        FivelhcLotterySgExample.Criteria criteria = fivelhcLotterySgExample.createCriteria();
                        criteria.andIssueEqualTo(sg.getIssue());
                        if (fivelhcLotterySgMapper.selectOneByExample(fivelhcLotterySgExample) == null) {
                            fivelhcLotterySgMapper.insertSelective(sg);
                        }
                    }
                }
            }

        } catch (Exception e) {
            logger.error("sameDataYuqiFiveLhc occur error:{}", e);
        } finally {
            // 释放锁
            lock.writeLock().unlock();
        }
    }


    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_AMLHC_YUQI_DATA, containerFactory = "jmsListenerContainerTopicDurable")
    public void sameDataYuqiSsLhc(String message) throws Exception {
        logger.info("【时时六合彩】预期数据同步：  " + message);

        List<AmlhcLotterySg> list = JSONObject.parseArray(message.substring(message.indexOf(":") + 1), AmlhcLotterySg.class);
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_AMLHC_YUQI_DATA + list.get(0).getIssue();
        // 【分布式读写锁】
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 60l);
                    for (AmlhcLotterySg sg : list) {
                        AmlhcLotterySgExample amlhcLotterySgExample = new AmlhcLotterySgExample();
                        AmlhcLotterySgExample.Criteria criteria = amlhcLotterySgExample.createCriteria();
                        criteria.andIssueEqualTo(sg.getIssue());
                        if (amlhcLotterySgMapper.selectOneByExample(amlhcLotterySgExample) == null) {
                            amlhcLotterySgMapper.insertSelective(sg);
                        }

                    }
                }
            }
        } catch (Exception e) {
            logger.error("sameDataYuqiSsLhc occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }
    }


    @JmsListener(destination = ActiveMQConfig.TOPIC_XGLHC_YUQI_DATA, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgXgLhcYuqiData(String message) throws Exception {
        logger.info("香港六合彩 - 预期数据同步：  " + message);
        String[] num = message.split("@");
        // 获取期号
        LhcHandicap lhcHandicap = JSONObject.parseObject(num[1], LhcHandicap.class);
        String issue = lhcHandicap.getIssue();

        // 获取唯一
        String key = ActiveMQConfig.TOPIC_XGLHC_YUQI_DATA + issue;
        // 【分布式读写锁】
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 15, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 60l);

//                    if(lhcHandicap.getIssue().equals("2019061")){
//                        LhcHandicapExample example = new LhcHandicapExample();
//                        LhcHandicapExample.Criteria criteriaLhc = example.createCriteria();
//                        criteriaLhc.andIssueGreaterThan("2019060");
//                        lhcHandicapMapper.deleteByExample(example);
//                    }

                    //判断逻辑，如果没有推送过来的数据没有就插入，如有，不更新（因为以后台管理界面操作数据为准）
                    LhcHandicapExample lhcHandicapExample = new LhcHandicapExample();
                    LhcHandicapExample.Criteria criteria = lhcHandicapExample.createCriteria();
                    criteria.andIssueEqualTo(issue);
                    criteria.andIsDeleteEqualTo(false);
                    LhcHandicap lhcHandicapThis = lhcHandicapMapper.selectOneByExample(lhcHandicapExample);
                    if (lhcHandicapThis == null) {
                        lhcHandicapMapper.insertSelective(lhcHandicap);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("processMsgXgLhcYuqiData occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }
    }





}
