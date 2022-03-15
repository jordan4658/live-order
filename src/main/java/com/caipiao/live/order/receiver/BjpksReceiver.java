package com.caipiao.live.order.receiver;

import com.alibaba.fastjson.JSONObject;
import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.util.redis.RedisBusinessUtil;
import com.caipiao.live.order.config.ActiveMQConfig;
import com.caipiao.live.order.service.bet.BetBjpksService;
import com.caipiao.live.order.service.result.BjpksLotterySgWriteService;
import com.caipiao.live.common.constant.RedisKeys;
import com.caipiao.live.common.enums.lottery.CaipiaoTypeEnum;
import com.caipiao.live.common.enums.lottery.LotteryTableNameEnum;
import com.caipiao.live.common.mybatis.entity.*;
import com.caipiao.live.common.mybatis.mapper.*;
import com.caipiao.live.common.mybatis.mapperext.sg.BjpksLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.FivebjpksLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.JsbjpksLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.TenbjpksLotterySgMapperExt;
import com.caipiao.live.common.util.DateUtils;
import com.caipiao.live.common.util.redis.BasicRedisClient;
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
 * @create 2018-09-17 20:07
 **/
@Component
public class BjpksReceiver {

    private static final Logger logger = LoggerFactory.getLogger(BjpksReceiver.class);
    @Autowired
    private BetBjpksService betBjpksService;
    @Autowired
    private BjpksLotterySgWriteService bjpksLotterySgWriteService;
    @Autowired
    private TenbjpksLotterySgMapper tenbjpksLotterySgMapper;
    @Autowired
    private TenbjpksLotterySgMapperExt tenbjpksLotterySgMapperExt;
    @Autowired
    private FtjspksLotterySgMapper ftjspksLotterySgMapper;
    @Autowired
    private BjpksLotterySgMapper bjpksLotterySgMapper;
    @Autowired
    private BjpksLotterySgMapperExt bjpksLotterySgMapperExt;
    @Autowired
    private FivebjpksLotterySgMapper fivebjpksLotterySgMapper;
    @Autowired
    private FivebjpksLotterySgMapperExt fivebjpksLotterySgMapperExt;
    @Autowired
    private JsbjpksLotterySgMapper jsbjpksLotterySgMapper;
    @Autowired
    private JsbjpksLotterySgMapperExt jsbjpksLotterySgMapperExt;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private BasicRedisClient basicRedisClient;
    @Autowired
    private JmsMessagingTemplate jmsMessagingTemplate;


    /**
     * 北京PK10- 【两面】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.QUEUE_BJPKS_LM)
    @JmsListener(destination = ActiveMQConfig.TOPIC_BJPKS_LM, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgBjpksLm(String message) {
        logger.info("【北京PK10-两面】结算期号：  " + message);
        String[] str = message.split(":");// 拆分消息内容

        // 获取唯一
        String key = ActiveMQConfig.TOPIC_BJPKS_LM + str[1];
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    if (str.length == 4) {
                        //第一步同步中奖结果
                        BjpksLotterySg updateSg = JSONObject.parseObject(str[3].replace("$", ":"), BjpksLotterySg.class);
                        int boolUpdate = bjpksLotterySgMapperExt.updateByIssue(updateSg);
                        if (boolUpdate == 0) {  //说明这一天的期号数据可能不全，则做一次检查，如果真没有，则从task_server同步当天所有期号数据过来
                            BjpksLotterySgExample sgExample = new BjpksLotterySgExample();
                            BjpksLotterySgExample.Criteria criteria = sgExample.createCriteria();
                            Calendar calendar = Calendar.getInstance();
                            criteria.andIdealTimeGreaterThanOrEqualTo(DateUtils.formatDate(new Date(), DateUtils.FORMAT_YYYY_MM_DD));
                            calendar.add(Calendar.DAY_OF_MONTH, 1);
                            criteria.andIdealTimeLessThan(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD));
                            sgExample.setOrderByClause("`ideal_time` asc");
                            int afterCount = bjpksLotterySgMapper.countByExample(sgExample);
                            if (afterCount < 44) {
                                //则发送消息
                                logger.info("同步预期数据发送通知：bjpksYuqiToday");
                                Destination destination = new ActiveMQQueue(ActiveMQConfig.TOPIC_YUQI_TODAYT);
                                jmsMessagingTemplate.convertAndSend(destination, "bjpksYuqiToday");
                            }
                        } else {
                            //更新历史赛果缓存
                            RedisBusinessUtil.updateLsSgCache(Constants.LOTTERY_BJPKS, RedisKeys.BJPKS_SG_HS_LIST, updateSg);
                        }
                    }

                    // 结算【北京PK10-两面】
                    betBjpksService.clearingBjpksLm(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.BJPKS.getTagType()));

                    //第三步 最近1天未开奖的数据
                    BjpksLotterySgExample bjpksLotterySgExample = new BjpksLotterySgExample();
                    BjpksLotterySgExample.Criteria criteria = bjpksLotterySgExample.createCriteria();
                    criteria.andOpenStatusEqualTo(Constants.WAIT);
                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.MINUTE, -5);  //过滤掉最新一期，（这一期可能需要2分钟才抓到）
                    criteria.andIdealTimeLessThan(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                    calendar.add(Calendar.DAY_OF_MONTH, -1);
                    criteria.andIdealTimeGreaterThanOrEqualTo(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                    bjpksLotterySgExample.setOrderByClause("ideal_time desc");
                    bjpksLotterySgExample.setLimit(15);
                    bjpksLotterySgExample.setOffset(0);
                    List<BjpksLotterySg> bjpksLotterySgList = bjpksLotterySgMapper.selectByExample(bjpksLotterySgExample);
                    if (bjpksLotterySgList.size() > 0) {
                        String issues = "";
                        for (BjpksLotterySg sg : bjpksLotterySgList) {
                            issues = issues + sg.getIssue() + ",";
                        }
                        issues = issues.substring(0, issues.length() - 1);
                        Destination destination = new ActiveMQQueue(ActiveMQConfig.TOPIC_MISSING_LOTTERY_SG);
                        jmsMessagingTemplate.convertAndSend(destination, LotteryTableNameEnum.BJPKS.getLotteryId() + "#" + issues);
                        logger.info("发送缺奖数据{}，{}", LotteryTableNameEnum.BJPKS.getLotteryId(), issues);
                    }
                    bjpksLotterySgWriteService.cacheIssueResultForBjpks(str[1], str[2]);



                }
            }
        } catch (Exception e) {
            logger.error("processMsgBjpksLm occur error:{}", e);

        } finally {
            lock.writeLock().unlock();
        }
    }

    //    @RabbitListener(queues = RabbitConfig.TOPIC_FIVEBJPKS_LM)
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_FIVEBJPKS_LM, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgFiveBjpksLm(String message) throws Exception {
        logger.info("【5分PK10-两面】结算期号：  " + message);
        // 拆分消息内容
        String[] str = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_FIVEBJPKS_LM + str[1];
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    //第一步同步中奖结果
                    FivebjpksLotterySg updateSg = JSONObject.parseObject(str[3].replace("$", ":"), FivebjpksLotterySg.class);
                    int boolUpdate = fivebjpksLotterySgMapperExt.updateByIssue(updateSg);
                    if (boolUpdate == 0) {  //说明这一天的期号数据可能不全，则做一次检查，如果真没有，则从task_server同步当天所有期号数据过来
                        FivebjpksLotterySgExample sgExample = new FivebjpksLotterySgExample();
                        FivebjpksLotterySgExample.Criteria criteria = sgExample.createCriteria();
                        Calendar calendar = Calendar.getInstance();
                        criteria.andIdealTimeGreaterThanOrEqualTo(DateUtils.formatDate(new Date(), DateUtils.FORMAT_YYYY_MM_DD));
                        calendar.add(Calendar.DAY_OF_MONTH, 1);
                        criteria.andIdealTimeLessThan(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD));
                        sgExample.setOrderByClause("`ideal_time` asc");
                        int afterCount = fivebjpksLotterySgMapper.countByExample(sgExample);
                        if (afterCount < 288) {
                            //则发送消息
                            logger.info("同步预期数据发送通知：fiveBjpksYuqiToday");
                            Destination destination = new ActiveMQQueue(ActiveMQConfig.PLATFORM_TOPIC_YUQI_TODAYT);
                            jmsMessagingTemplate.convertAndSend(destination, "fiveBjpksYuqiToday");
                        }
                    } else {
                        //更新历史赛果缓存
                        RedisBusinessUtil.updateLsSgCache(Constants.LOTTERY_FIVEPKS, RedisKeys.FIVEPKS_SG_HS_LIST, updateSg);
                    }

                    bjpksLotterySgWriteService.cacheIssueResultForFivebjpks(str[1], str[2]);
                    // 结算【北京PK10-两面】
                    betBjpksService.clearingBjpksLm(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.FIVEPKS.getTagType()));

                    //第三步 最近1天未开奖的数据
                    FivebjpksLotterySgExample fivebjpksLotterySgExample = new FivebjpksLotterySgExample();
                    FivebjpksLotterySgExample.Criteria criteria = fivebjpksLotterySgExample.createCriteria();
                    criteria.andOpenStatusEqualTo(Constants.WAIT);
                    Calendar calendar = Calendar.getInstance();
                    criteria.andIdealTimeLessThan(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                    calendar.add(Calendar.DAY_OF_MONTH, -1);
                    criteria.andIdealTimeGreaterThanOrEqualTo(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                    fivebjpksLotterySgExample.setOrderByClause("ideal_time desc");
                    fivebjpksLotterySgExample.setLimit(15);
                    fivebjpksLotterySgExample.setOffset(0);
                    List<FivebjpksLotterySg> fivebjpksLotterySgList = fivebjpksLotterySgMapper.selectByExample(fivebjpksLotterySgExample);
                    if (fivebjpksLotterySgList.size() > 0) {
                        String issues = "";
                        for (FivebjpksLotterySg sg : fivebjpksLotterySgList) {
                            issues = issues + sg.getIssue() + ",";
                        }
                        issues = issues.substring(0, issues.length() - 1);
                        Destination destination = new ActiveMQQueue(ActiveMQConfig.PLATFORM_TOPIC_MISSING_LOTTERY_SG);
                        jmsMessagingTemplate.convertAndSend(destination, LotteryTableNameEnum.FIVEPKS.getLotteryId() + "#" + issues);
                        logger.info("发送缺奖数据{}，{}", LotteryTableNameEnum.FIVEPKS.getLotteryId(), issues);
                    }


                }
            }
        } catch (Exception e) {
            logger.error("processMsgFiveBjpksLm occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    //    @RabbitListener(queues = RabbitConfig.TOPIC_JSBJPKS_LM)
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_JSBJPKS_LM, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgjsBjpksLm(String message) throws Exception {
        logger.info("【德州PK10-两面】结算期号：  " + message);
        // 拆分消息内容
        String[] str = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_JSBJPKS_LM + str[1];
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    //第一步同步中奖结果
                    JsbjpksLotterySg updateSg = JSONObject.parseObject(str[3].replace("$", ":"), JsbjpksLotterySg.class);
                    int boolUpdate = jsbjpksLotterySgMapperExt.updateByIssue(updateSg);
                    if (boolUpdate == 0) {  //说明这一天的期号数据可能不全，则做一次检查，如果真没有，则从task_server同步当天所有期号数据过来
                        JsbjpksLotterySgExample sgExample = new JsbjpksLotterySgExample();
                        JsbjpksLotterySgExample.Criteria criteria = sgExample.createCriteria();
                        Calendar calendar = Calendar.getInstance();
                        criteria.andIdealTimeGreaterThanOrEqualTo(DateUtils.formatDate(new Date(), DateUtils.FORMAT_YYYY_MM_DD));
                        calendar.add(Calendar.DAY_OF_MONTH, 1);
                        criteria.andIdealTimeLessThan(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD));
                        sgExample.setOrderByClause("`ideal_time` asc");
                        int afterCount = jsbjpksLotterySgMapper.countByExample(sgExample);
                        if (afterCount < 1440) {
                            //则发送消息
                            logger.info("同步预期数据发送通知：jsBjpksYuqiToday");
                            Destination destination = new ActiveMQQueue(ActiveMQConfig.PLATFORM_TOPIC_YUQI_TODAYT);
                            jmsMessagingTemplate.convertAndSend(destination, ActiveMQConfig.LIVE_PLATFORM +"jsBjpksYuqiToday");
                        }
                    } else {
                        //更新历史赛果缓存
                        RedisBusinessUtil.updateLsSgCache(Constants.LOTTERY_DZPKS, RedisKeys.JSPKS_SG_HS_LIST, updateSg);
                    }

                    // 结算【北京PK10-两面】
                    betBjpksService.clearingBjpksLm(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.JSPKS.getTagType()));

                    //第三步 最近1天未开奖的数据
                    JsbjpksLotterySgExample jsbjpksLotterySgExample = new JsbjpksLotterySgExample();
                    JsbjpksLotterySgExample.Criteria criteria = jsbjpksLotterySgExample.createCriteria();
                    criteria.andOpenStatusEqualTo(Constants.WAIT);
                    Calendar calendar = Calendar.getInstance();
                    criteria.andIdealTimeLessThan(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                    calendar.add(Calendar.DAY_OF_MONTH, -1);
                    criteria.andIdealTimeGreaterThanOrEqualTo(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                    jsbjpksLotterySgExample.setOrderByClause("ideal_time desc");
                    jsbjpksLotterySgExample.setLimit(15);
                    jsbjpksLotterySgExample.setOffset(0);
                    List<JsbjpksLotterySg> jsbjpksLotterySgList = jsbjpksLotterySgMapper.selectByExample(jsbjpksLotterySgExample);
                    if (jsbjpksLotterySgList.size() > 0) {
                        String issues = "";
                        for (JsbjpksLotterySg sg : jsbjpksLotterySgList) {
                            issues = issues + sg.getIssue() + ",";
                        }
                        issues = issues.substring(0, issues.length() - 1);
                        Destination destination = new ActiveMQQueue(ActiveMQConfig.PLATFORM_TOPIC_MISSING_LOTTERY_SG);
                        jmsMessagingTemplate.convertAndSend(destination, LotteryTableNameEnum.JSPKS.getLotteryId() + "#" + issues);
                        logger.info("发送缺奖数据{}，{}", LotteryTableNameEnum.JSPKS.getLotteryId(), issues);
                    }
                    bjpksLotterySgWriteService.cacheIssueResultForJsbjpks(str[1], str[2]);
//                    bjpksLotterySgWriteService.cacheIssueResultForFtjspks(str[1], str[2]);


                }
            }
        } catch (Exception e) {
            logger.error("processMsgjsBjpksLm occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    //    @RabbitListener(queues = RabbitConfig.TOPIC_TENBJPKS_LM)
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_TENBJPKS_LM, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgtenBjpksLm(String message) throws Exception {
        logger.info("【十分PK10-两面】结算期号：  " + message);
        // 拆分消息内容
        String[] str = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_TENBJPKS_LM + str[1];
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    //第一步同步中奖结果
                    TenbjpksLotterySg updateSg = JSONObject.parseObject(str[3].replace("$", ":"), TenbjpksLotterySg.class);
                    int boolUpdate = tenbjpksLotterySgMapperExt.updateByIssue(updateSg);
                    if (boolUpdate == 0) {  //说明这一天的期号数据可能不全，则做一次检查，如果真没有，则从task_server同步当天所有期号数据过来
                        TenbjpksLotterySgExample sgExample = new TenbjpksLotterySgExample();
                        TenbjpksLotterySgExample.Criteria criteria = sgExample.createCriteria();
                        Calendar calendar = Calendar.getInstance();
                        criteria.andIdealTimeGreaterThanOrEqualTo(DateUtils.formatDate(new Date(), DateUtils.FORMAT_YYYY_MM_DD));
                        calendar.add(Calendar.DAY_OF_MONTH, 1);
                        criteria.andIdealTimeLessThan(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD));
                        sgExample.setOrderByClause("`ideal_time` asc");
                        int afterCount = tenbjpksLotterySgMapper.countByExample(sgExample);
                        if (afterCount < 144) {
                            //则发送消息
                            logger.info("同步预期数据发送通知：tenBjpksYuqiToday");
                            Destination destination = new ActiveMQQueue(ActiveMQConfig.TOPIC_YUQI_TODAYT);
                            jmsMessagingTemplate.convertAndSend(destination, "tenBjpksYuqiToday");
                        }
                    } else {
                        //更新历史赛果缓存
                        RedisBusinessUtil.updateLsSgCache(Constants.LOTTERY_TENPKS, RedisKeys.TENPKS_SG_HS_LIST, updateSg);
                    }

                    bjpksLotterySgWriteService.cacheIssueResultForTenbjpks(str[1], str[2]);
                    // 结算【北京PK10-两面】
                    betBjpksService.clearingBjpksLm(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.TENPKS.getTagType()));

                    //第三步 最近1天未开奖的数据
                    TenbjpksLotterySgExample tenbjpksLotterySgExample = new TenbjpksLotterySgExample();
                    TenbjpksLotterySgExample.Criteria criteria = tenbjpksLotterySgExample.createCriteria();
                    criteria.andOpenStatusEqualTo(Constants.WAIT);
                    Calendar calendar = Calendar.getInstance();
                    criteria.andIdealTimeLessThan(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                    calendar.add(Calendar.DAY_OF_MONTH, -1);
                    criteria.andIdealTimeGreaterThanOrEqualTo(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                    tenbjpksLotterySgExample.setOrderByClause("ideal_time desc");
                    tenbjpksLotterySgExample.setLimit(15);
                    tenbjpksLotterySgExample.setOffset(0);
                    List<TenbjpksLotterySg> tenbjpksLotterySgList = tenbjpksLotterySgMapper.selectByExample(tenbjpksLotterySgExample);
                    if (tenbjpksLotterySgList.size() > 0) {
                        String issues = "";
                        for (TenbjpksLotterySg sg : tenbjpksLotterySgList) {
                            issues = issues + sg.getIssue() + ",";
                        }
                        issues = issues.substring(0, issues.length() - 1);
                        Destination destination = new ActiveMQQueue(ActiveMQConfig.PLATFORM_TOPIC_MISSING_LOTTERY_SG);
                        jmsMessagingTemplate.convertAndSend(destination, LotteryTableNameEnum.TENPKS.getLotteryId() + "#" + issues);
                        logger.info("发送缺奖数据{}，{}", LotteryTableNameEnum.TENPKS.getLotteryId(), issues);
                    }


                }
            }
        } catch (Exception e) {
            logger.error("processMsgtenBjpksLm occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 北京PK10- 【猜名次猜前几】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_BJPKS_CMC_CQJ)
    @JmsListener(destination = ActiveMQConfig.TOPIC_BJPKS_CMC_CQJ, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgBjpksCmcCqj(String message) throws Exception {
        logger.info("【北京PK10-1-5名6-10名】结算期号：  " + message);
        // 拆分消息内容
        String[] str = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.TOPIC_BJPKS_CMC_CQJ + str[1];
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    // 结算【北京PK10-猜名次猜前几】
                    betBjpksService.clearingBjpksCmcCqj(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.BJPKS.getTagType()));
                }
            }
        } catch (Exception e) {
            logger.error("processMsgBjpksCmcCqj occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    //    @RabbitListener(queues = RabbitConfig.TOPIC_TENBJPKS_CMC_CQJ)
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_TENBJPKS_CMC_CQJ, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgBjpkstenCmcCqj(String message) throws Exception {
        logger.info("【北京PK10-1-5名6-10名】结算期号：  " + message);
        // 拆分消息内容
        String[] str = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_TENBJPKS_CMC_CQJ + str[1];
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    // 结算【北京PK10-猜名次猜前几】
                    betBjpksService.clearingBjpksCmcCqj(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.TENPKS.getTagType()));
                }
            }
        } catch (Exception e) {
            logger.error("processMsgBjpkstenCmcCqj occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    //    @RabbitListener(queues = RabbitConfig.TOPIC_FIVEBJPKS_CMC_CQJ)
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_FIVEBJPKS_CMC_CQJ, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgBjpksfiveCmcCqj(String message) throws Exception {
        logger.info("【北京PK10-1-5名6-10名】结算期号：  " + message);
        // 拆分消息内容
        String[] str = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_FIVEBJPKS_CMC_CQJ + str[1];
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    // 结算【北京PK10-猜名次猜前几】
                    betBjpksService.clearingBjpksCmcCqj(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.FIVEPKS.getTagType()));
                }
            }
        } catch (Exception e) {
            logger.error("processMsgBjpksfiveCmcCqj occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    //    @RabbitListener(queues = RabbitConfig.TOPIC_JSBJPKS_CMC_CQJ)
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_JSBJPKS_CMC_CQJ, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgBjpksjsCmcCqj(String message) throws Exception {
        logger.info("【北京PK10-1-5名6-10名】结算期号：  " + message);
        // 拆分消息内容
        String[] str = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_JSBJPKS_CMC_CQJ + str[1];
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    // 结算【北京PK10-猜名次猜前几】
                    betBjpksService.clearingBjpksCmcCqj(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.JSPKS.getTagType()));
                }
            }
        } catch (Exception e) {
            logger.error("processMsgBjpksjsCmcCqj occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

//    /**
//     * 北京PK10- 【单式猜前几】计算结果
//     * @param message 消息内容【期号】
//     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_BJPKS_DS_CQJ)
//    public void processMsgBjpksDsCqj(String message) throws Exception {
//        logger.info("【北京PK10-单式猜前几】结算期号：  "+message);
//        // 拆分消息内容
//        String[] str = message.split(":");
//
//        // 结算【北京PK10-单式猜前几】
//        betBjpksService.clearingBjpksDsCqj(str[1], str[2]);
//    }
//
//    /**
//     * 北京PK10- 【定位胆】计算结果
//     * @param message 消息内容【期号】
//     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_BJPKS_DWD)
//    public void processMsgBjpksDwd(String message) throws Exception {
//        logger.info("【北京PK10-定位胆】结算期号：  "+message);
//        // 拆分消息内容
//        String[] str = message.split(":");
//
//        // 结算【北京PK10-定位胆】
//        betBjpksService.clearingBjpksDwd(str[1], str[2]);
//    }

    /**
     * 北京PK10- 【冠亚和】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_BJPKS_GYH)
    @JmsListener(destination = ActiveMQConfig.TOPIC_BJPKS_GYH, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgBjpksGyh(String message) throws Exception {
        logger.info("【北京PK10-冠亚和】结算期号：  " + message);
        // 拆分消息内容
        String[] str = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.TOPIC_BJPKS_GYH + str[1];
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    // 结算【北京PK10-冠亚和】
                    betBjpksService.clearingBjpksGyh(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.BJPKS.getTagType()));
                }
            }
        } catch (Exception e) {
            logger.error("processMsgBjpksGyh occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    //    @RabbitListener(queues = RabbitConfig.TOPIC_FIVEBJPKS_GYH)
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_FIVEBJPKS_GYH, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgfiveBjpksGyh(String message) throws Exception {
        logger.info("【北京PK10-冠亚和】结算期号：  " + message);
        // 拆分消息内容
        String[] str = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_FIVEBJPKS_GYH + str[1];
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    // 结算【北京PK10-冠亚和】
                    betBjpksService.clearingBjpksGyh(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.FIVEPKS.getTagType()));
                }
            }
        } catch (Exception e) {
            logger.error("processMsgfiveBjpksGyh occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    //    @RabbitListener(queues = RabbitConfig.TOPIC_TENBJPKS_GYH)
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_TENBJPKS_GYH, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgtenBjpksGyh(String message) throws Exception {
        logger.info("【北京PK10-冠亚和】结算期号：  " + message);
        // 拆分消息内容
        String[] str = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_TENBJPKS_GYH + str[1];
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    // 结算【北京PK10-冠亚和】
                    betBjpksService.clearingBjpksGyh(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.TENPKS.getTagType()));
                }
            }
        } catch (Exception e) {
            logger.error("processMsgtenBjpksGyh occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    //    @RabbitListener(queues = RabbitConfig.TOPIC_JSBJPKS_GYH)
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_JSBJPKS_GYH, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgjsBjpksGyh(String message) throws Exception {
        logger.info("【北京PK10-冠亚和】结算期号：  " + message);
        // 拆分消息内容
        String[] str = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_JSBJPKS_GYH + str[1];
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    // 结算【北京PK10-冠亚和】
                    betBjpksService.clearingBjpksGyh(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.JSPKS.getTagType()));
                }
            }
        } catch (Exception e) {
            logger.error("processMsgjsBjpksGyh occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_JSBJPKS_YUQI_DATA, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgjsBjpksYuqiData(String message) throws Exception {
        logger.info("【德州PK10】预期数据同步：  " + message);

        List<JsbjpksLotterySg> list = JSONObject.parseArray(message.substring(message.indexOf(":") + 1), JsbjpksLotterySg.class);
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_JSBJPKS_YUQI_DATA + list.get(0).getIssue();
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 20, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    for (JsbjpksLotterySg sg : list) {
                        JsbjpksLotterySgExample jsbjpksLotterySgExample = new JsbjpksLotterySgExample();
                        JsbjpksLotterySgExample.Criteria criteria = jsbjpksLotterySgExample.createCriteria();
                        criteria.andIssueEqualTo(sg.getIssue());
                        if (jsbjpksLotterySgMapper.selectOneByExample(jsbjpksLotterySgExample) == null) {
                            jsbjpksLotterySgMapper.insertSelective(sg);
                        }

                    }
                }
            }
        } catch (Exception e) {
            logger.error("processMsgjsBjpksYuqiData occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_FT_JSBJPKS_YUQI_DATA, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgFtjsBjpksYuqiData(String message) throws Exception {
        logger.info("【番摊德州PK10】预期数据同步：  " + message);

        List<FtjspksLotterySg> list = JSONObject.parseArray(message.substring(message.indexOf(":") + 1), FtjspksLotterySg.class);
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_FT_JSBJPKS_YUQI_DATA + list.get(0).getIssue();
        // 【分布式读写锁】
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");

        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 20, TimeUnit.SECONDS);
            // 判断是否获取到锁
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    for (FtjspksLotterySg sg : list) {
                        FtjspksLotterySgExample ftjspksLotterySgExample = new FtjspksLotterySgExample();
                        FtjspksLotterySgExample.Criteria ftjspksCriteria = ftjspksLotterySgExample.createCriteria();
                        ftjspksCriteria.andIssueEqualTo(sg.getIssue());
                        if (ftjspksLotterySgMapper.selectOneByExample(ftjspksLotterySgExample) == null) {
                            ftjspksLotterySgMapper.insertSelective(sg);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("processMsgFtjsBjpksYuqiData occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }

    }

    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_FIVEBJPKS_YUQI_DATA, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgFiveBjpksYuqiData(String message) throws Exception {
        logger.info("【5分PK10】预期数据同步：  " + message);
        List<FivebjpksLotterySg> list = JSONObject.parseArray(message.substring(message.indexOf(":") + 1), FivebjpksLotterySg.class);
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_FIVEBJPKS_YUQI_DATA + list.get(0).getIssue();
        // 【分布式读写锁】
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    for (FivebjpksLotterySg sg : list) {
                        FivebjpksLotterySgExample fivebjpksLotterySgExample = new FivebjpksLotterySgExample();
                        FivebjpksLotterySgExample.Criteria criteria = fivebjpksLotterySgExample.createCriteria();
                        criteria.andIssueEqualTo(sg.getIssue());
                        if (fivebjpksLotterySgMapper.selectOneByExample(fivebjpksLotterySgExample) == null) {
                            fivebjpksLotterySgMapper.insertSelective(sg);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("processMsgFiveBjpksYuqiData occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_TENBJPKS_YUQI_DATA, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgTenBjpksYuqiData(String message) throws Exception {
        logger.info("【10分PK10】预期数据同步：  " + message);
        List<TenbjpksLotterySg> list = JSONObject.parseArray(message.substring(message.indexOf(":") + 1), TenbjpksLotterySg.class);
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_TENBJPKS_YUQI_DATA + list.get(0).getIssue();
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    for (TenbjpksLotterySg sg : list) {
                        TenbjpksLotterySgExample tenbjpksLotterySgExample = new TenbjpksLotterySgExample();
                        TenbjpksLotterySgExample.Criteria criteria = tenbjpksLotterySgExample.createCriteria();
                        criteria.andIssueEqualTo(sg.getIssue());
                        if (tenbjpksLotterySgMapper.selectOneByExample(tenbjpksLotterySgExample) == null) {
                            tenbjpksLotterySgMapper.insertSelective(sg);
                        }

                    }
                }
            }

        } catch (Exception e) {
            logger.error("processMsgTenBjpksYuqiData occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @JmsListener(destination = ActiveMQConfig.TOPIC_BJPKS_YUQI_DATA, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgBjpksYuqiData(String message) throws Exception {
        logger.info("【北京PK10】预期数据同步：  " + message);

        List<BjpksLotterySg> list = JSONObject.parseArray(message.substring(message.indexOf(":") + 1), BjpksLotterySg.class);
        // 获取唯一
        String key = ActiveMQConfig.TOPIC_BJPKS_YUQI_DATA + list.get(0).getIssue();
        // 【分布式读写锁】
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    for (BjpksLotterySg sg : list) {
                        BjpksLotterySgExample bjpksLotterySgExample = new BjpksLotterySgExample();
                        BjpksLotterySgExample.Criteria criteria = bjpksLotterySgExample.createCriteria();
                        criteria.andIssueEqualTo(sg.getIssue());
                        if (bjpksLotterySgMapper.selectOneByExample(bjpksLotterySgExample) == null) {
                            bjpksLotterySgMapper.insertSelective(sg);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("processMsgBjpksYuqiData occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }

    }




}
