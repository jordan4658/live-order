package com.caipiao.live.order.receiver;

import com.alibaba.fastjson.JSONObject;
import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.mybatis.mapperext.sg.CqsscLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.FivesscLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.FtJssscLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.JssscLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.TensscLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.TjsscLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.TxffcLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.XjsscLotterySgMapperExt;
import com.caipiao.live.common.util.redis.RedisBusinessUtil;
import com.caipiao.live.order.config.ActiveMQConfig;
import com.caipiao.live.order.service.bet.BetSscbmService;
import com.caipiao.live.order.service.result.XjsscLotterySgWriteService;
import com.caipiao.live.common.constant.RedisKeys;
import com.caipiao.live.common.enums.lottery.CaipiaoTypeEnum;
import com.caipiao.live.common.enums.lottery.LotteryTableNameEnum;
import com.caipiao.live.common.mybatis.entity.*;
import com.caipiao.live.common.mybatis.mapper.*;
import com.caipiao.live.common.util.DateUtils;
import com.caipiao.live.common.util.StringUtils;
import com.caipiao.live.common.util.redis.BasicRedisClient;
import com.caipiao.live.common.util.redis.RedisLock;
import org.apache.activemq.command.ActiveMQQueue;
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

@Component
public class SscbmReceiver {
    private static final Logger logger = LoggerFactory.getLogger(SscbmReceiver.class);
    @Autowired
    private BetSscbmService betSscbmService;
    @Autowired
    private XjsscLotterySgWriteService xjsscLotterySgWriteService;
    @Autowired
    private FcssqLotterySgMapper fcssqLotterySgMapper;
    @Autowired
    private TjsscLotterySgMapper tjsscLotterySgMapper;
    @Autowired
    private TjsscLotterySgMapperExt tjsscLotterySgMapperExt;
    @Autowired
    private FivesscLotterySgMapper fivesscLotterySgMapper;
    @Autowired
    private FivesscLotterySgMapperExt fivesscLotterySgMapperExt;
    @Autowired
    private JssscLotterySgMapper jssscLotterySgMapper;
    @Autowired
    private JssscLotterySgMapperExt jssscLotterySgMapperExt;
    @Autowired
    private FtjssscLotterySgMapper ftjssscLotterySgMapper;
    @Autowired
    private FtJssscLotterySgMapperExt ftJssscLotterySgMapperExt;
    @Autowired
    private CqsscLotterySgMapper cqsscLotterySgMapper;
    @Autowired
    private CqsscLotterySgMapperExt cqsscLotterySgMapperExt;
    @Autowired
    private TensscLotterySgMapper tensscLotterySgMapper;
    @Autowired
    private TensscLotterySgMapperExt tensscLotterySgMapperExt;
    @Autowired
    private TxffcLotterySgMapper txffcLotterySgMapper;
    @Autowired
    private TxffcLotterySgMapperExt txffcLotterySgMapperExt;
    @Autowired
    private XjsscLotterySgMapper xjsscLotterySgMapper;
    @Autowired
    private XjsscLotterySgMapperExt xjsscLotterySgMapperExt;
    @Autowired
    private BasicRedisClient basicRedisClient;
    @Autowired
    private JmsMessagingTemplate jmsMessagingTemplate;



    /**
     * ss彩 - 【两面】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_SSC_TX_LM)
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_SSC_TX_LM, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgTxSscLM(String message) throws Exception {
        logger.info("比特币分分彩 - 【两面】结算期号：  " + message);
        // 拆分消息内容
        String[] str = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_SSC_TX_LM + str[1];
        // 【分布式读写锁】
//        RReadWriteLock lock = redissonClient.getReadWriteLock(key+"lock");
//        RedisLock lock = new RedisLock(key+"lock", 10*1000);
        RedisLock lock = new RedisLock(key + "lock", 3 * 1000, 30 * 1000);
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
//            boolean bool = lock.writeLock().tryLock(2, 10, TimeUnit.SECONDS);
            // 判断是否获取到锁
            if (lock.lock()) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    if (str.length == 4) {   //表面需要同步数据
                        //第一步同步中奖结果
                        TxffcLotterySg updateSg = JSONObject.parseObject(str[3].replace("$", ":"), TxffcLotterySg.class);
                        int boolUpdate = txffcLotterySgMapperExt.updateByIssue(updateSg);
                        if (boolUpdate == 0) {  //说明这一天的期号数据可能不全，则做一次检查，如果真没有，则从task_server同步当天所有期号数据过来
                            TxffcLotterySgExample sgExample = new TxffcLotterySgExample();
                            TxffcLotterySgExample.Criteria criteria = sgExample.createCriteria();
                            Calendar calendar = Calendar.getInstance();
                            criteria.andIdealTimeGreaterThanOrEqualTo(DateUtils.formatDate(new Date(), DateUtils.FORMAT_YYYY_MM_DD));
                            calendar.add(Calendar.DAY_OF_MONTH, 1);
                            criteria.andIdealTimeLessThan(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD));
                            sgExample.setOrderByClause("`ideal_time` asc");
                            int afterCount = txffcLotterySgMapper.countByExample(sgExample);
                            if (afterCount < 1440) {
                                //则发送消息
                                logger.info("同步预期数据发送通知：txffcYuqiToday");
                                Destination destination = new ActiveMQQueue(ActiveMQConfig.PLATFORM_TOPIC_YUQI_TODAYT);
                                jmsMessagingTemplate.convertAndSend(destination, "txffcYuqiToday");
                            }
                        } else {
                            //更新历史赛果缓存
                            RedisBusinessUtil.updateLsSgCache(Constants.LOTTERY_TXFFC, RedisKeys.TXFFC_SG_HS_LIST, updateSg);
                        }

                    }
                    xjsscLotterySgWriteService.cacheIssueResultForTxffc(str[1], str[2]);
                    // 结算【两面】
                    betSscbmService.countlm(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.TXFFC.getTagType()));

                    //第三步 最近1天未开奖的数据
                    TxffcLotterySgExample txffcLotterySgExample = new TxffcLotterySgExample();
                    TxffcLotterySgExample.Criteria criteria = txffcLotterySgExample.createCriteria();
                    criteria.andOpenStatusEqualTo(Constants.WAIT);
                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.MINUTE, -1);  //过滤掉最新一期，
                    criteria.andIdealTimeLessThan(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                    calendar.add(Calendar.DAY_OF_MONTH, -1);
                    criteria.andIdealTimeGreaterThanOrEqualTo(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                    txffcLotterySgExample.setOrderByClause("ideal_time desc");
                    txffcLotterySgExample.setLimit(15);
                    txffcLotterySgExample.setOffset(0);
                    List<TxffcLotterySg> txffcLotterySgList = txffcLotterySgMapper.selectByExample(txffcLotterySgExample);
                    if (txffcLotterySgList.size() > 0) {
                        String issues = "";
                        for (TxffcLotterySg sg : txffcLotterySgList) {
                            issues = issues + sg.getIssue() + ",";
                        }
                        issues = issues.substring(0, issues.length() - 1);
                        Destination destination = new ActiveMQQueue(ActiveMQConfig.PLATFORM_TOPIC_MISSING_LOTTERY_SG);
                        jmsMessagingTemplate.convertAndSend(destination, LotteryTableNameEnum.TXFFC.getLotteryId() + "#" + issues);
                        logger.info("发送缺奖数据{}，{}", LotteryTableNameEnum.TXFFC.getLotteryId(), issues);
                    }
                }
            }

        } catch (Exception e) {
            logger.error("processMsgTxSscLM occur error:", e);
        } finally {
            lock.unlock();
        }


    }

    /**
     * 比特币分分彩 - 【斗牛】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_SSC_TX_DN)
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_SSC_TX_DN, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgTxSscdn(String message) throws Exception {
        logger.info("比特币分分彩 - 【斗牛】结算期号：  " + message);
        // 拆分消息内容
        String[] str = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_SSC_TX_DN + str[1];
//        RReadWriteLock lock = redissonClient.getReadWriteLock(key+"lock");
        RedisLock lock = new RedisLock(key + "lock", 3 * 1000, 30 * 1000);
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
//            boolean bool = lock.writeLock().tryLock(2, 10, TimeUnit.SECONDS);
            if (lock.lock()) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    // 基本【组选】规则
                    betSscbmService.countdn(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.TXFFC.getTagType()));
                }
            }
        } catch (Exception e) {
            logger.error("processMsgTxSscdn occur error:", e);
        } finally {
            lock.unlock();
        }

    }

    /**
     * 比特币分分彩 - 【1-5球】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_SSC_TX_15)
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_SSC_TX_15, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgTxSsc15(String message) throws Exception {
        logger.info("比特币分分彩 - 【1-5球】结算期号：  " + message);
        // 拆分消息内容
        String[] str = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_SSC_TX_15 + str[1];
        // 【分布式读写锁】
//        RReadWriteLock lock = redissonClient.getReadWriteLock(key+"lock");
        RedisLock lock = new RedisLock(key + "lock", 3 * 1000, 30 * 1000);
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
//            boolean bool = lock.writeLock().tryLock(2, 10, TimeUnit.SECONDS);
            // 判断是否获取到锁
            if (lock.lock()) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    // 【定位胆】规则
                    betSscbmService.count15(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.TXFFC.getTagType()));
                }
            }
        } catch (Exception e) {
            logger.error("processMsgTxSsc15 occur error:", e);
        } finally {
            lock.unlock();
        }

    }

    /**
     * 比特币分分彩 - 【前中后】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_SSC_TX_QZH)
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_SSC_TX_QZH, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgTxSscqzh(String message) throws Exception {
        logger.info("比特币分分彩 - 【前中后】结算期号：  " + message);
        // 拆分消息内容
        String[] str = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_SSC_TX_QZH + str[1];
//        RReadWriteLock lock = redissonClient.getReadWriteLock(key+"lock");
        RedisLock lock = new RedisLock(key + "lock", 3 * 1000, 30 * 1000);
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
//            boolean bool = lock.writeLock().tryLock(2, 10, TimeUnit.SECONDS);
            // 判断是否获取到锁
            if (lock.lock()) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    // 【定位大小单双】规则
                    betSscbmService.countqzh(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.TXFFC.getTagType()));
                }
            }
        } catch (Exception e) {
            logger.error("processMsgTxSscqzh occur error:", e);
        } finally {
            lock.unlock();
        }

    }

    /**
     * 重庆时时彩 - 【两面】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_SSC_CQ_LM)
    @JmsListener(destination = ActiveMQConfig.TOPIC_SSC_CQ_LM, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgCQSscLM(String message) throws Exception {
        logger.info("重庆时时彩 - 【两面】结算期号：  " + message);
        // 拆分消息内容
        String[] str = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.TOPIC_SSC_CQ_LM + str[1];
//        RReadWriteLock lock = redissonClient.getReadWriteLock(key+"lock");
        RedisLock lock = new RedisLock(key + "lock", 3 * 1000, 30 * 1000);
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
//            boolean bool = lock.writeLock().tryLock(2, 10, TimeUnit.SECONDS);
            if (lock.lock()) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    if (str.length == 4) {   //表面需要同步数据
                        //第一步同步中奖结果
                        CqsscLotterySg updateSg = JSONObject.parseObject(str[3].replace("$", ":"), CqsscLotterySg.class);
                        updateSg.setActualDate(DateUtils.str2date(updateSg.getTime()));
                        updateSg.setIdealDate(DateUtils.parseDate(updateSg.getIdealTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                        int boolUpdate = cqsscLotterySgMapperExt.updateByIssue(updateSg);
                        if (boolUpdate == 0) {  //说明这一天的期号数据可能不全，则做一次检查，如果真没有，则从task_server同步当天所有期号数据过来
                            CqsscLotterySgExample sgExample = new CqsscLotterySgExample();
                            CqsscLotterySgExample.Criteria criteria = sgExample.createCriteria();
                            Calendar calendar = Calendar.getInstance();
                            criteria.andIdealTimeGreaterThanOrEqualTo(DateUtils.formatDate(new Date(), DateUtils.FORMAT_YYYY_MM_DD));
                            calendar.add(Calendar.DAY_OF_MONTH, 1);
                            criteria.andIdealTimeLessThan(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD));
                            int afterCount = cqsscLotterySgMapper.countByExample(sgExample);
                            if (afterCount < 59) {
                                //则发送消息
                                logger.info("同步预期数据发送通知：cqsscYuqiToday");
                                Destination destination = new ActiveMQQueue(ActiveMQConfig.TOPIC_YUQI_TODAYT);
                                jmsMessagingTemplate.convertAndSend(destination, "cqsscYuqiToday");
                            }
                        } else {
                            //更新历史赛果缓存
                            RedisBusinessUtil.updateLsSgCache(Constants.LOTTERY_CQSSC, RedisKeys.CQSSC_SG_HS_LIST, updateSg);
                        }

                    }
                    xjsscLotterySgWriteService.cacheIssueResultForCqssc(str[1], str[2]);
                    // 结算【两面】
                    betSscbmService.countlm(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.CQSSC.getTagType()));

                    //第三步 最近1天未开奖的数据
                    CqsscLotterySgExample cqsscLotterySgExample = new CqsscLotterySgExample();
                    CqsscLotterySgExample.Criteria criteria = cqsscLotterySgExample.createCriteria();
                    criteria.andOpenStatusEqualTo(Constants.WAIT);
                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.MINUTE, -5);  //过滤掉最新一期，（这一期可能需要2分钟才抓到）
                    criteria.andIdealTimeLessThan(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                    calendar.add(Calendar.DAY_OF_MONTH, -1);
                    criteria.andIdealTimeGreaterThanOrEqualTo(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                    cqsscLotterySgExample.setOrderByClause("ideal_time desc");
                    cqsscLotterySgExample.setLimit(15);
                    cqsscLotterySgExample.setOffset(0);
                    List<CqsscLotterySg> cqsscLotterySgList = cqsscLotterySgMapper.selectByExample(cqsscLotterySgExample);
                    if (cqsscLotterySgList.size() > 0) {
                        String issues = "";
                        for (CqsscLotterySg sg : cqsscLotterySgList) {
                            issues = issues + sg.getIssue() + ",";
                        }
                        issues = issues.substring(0, issues.length() - 1);
                        Destination destination = new ActiveMQQueue(ActiveMQConfig.TOPIC_MISSING_LOTTERY_SG);
                        jmsMessagingTemplate.convertAndSend(destination, LotteryTableNameEnum.CQSSC.getLotteryId() + "#" + issues);
                        logger.info("发送缺奖数据{}，{}", LotteryTableNameEnum.CQSSC.getLotteryId(), issues);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("processMsgCQSscLM occur error:", e);
        } finally {
            // 释放锁
            lock.unlock();
        }

    }

    /**
     * 重庆时时彩 - 【斗牛】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.QUEUE_SSC_CQ_DN)
    @JmsListener(destination = ActiveMQConfig.TOPIC_SSC_CQ_DN, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgCQSscdn(String message) throws Exception {
        logger.info("重庆时时彩 - 【斗牛】结算期号：  " + message);
        // 拆分消息内容
        String[] str = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.TOPIC_SSC_CQ_DN + str[1];
//        RReadWriteLock lock = redissonClient.getReadWriteLock(key+"lock");
        RedisLock lock = new RedisLock(key + "lock", 3 * 1000, 30 * 1000);
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
//            boolean bool = lock.writeLock().tryLock(2, 10, TimeUnit.SECONDS);
            if (lock.lock()) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    // 基本【组选】规则
                    betSscbmService.countdn(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.CQSSC.getTagType()));
                }
            }
        } catch (Exception e) {
            logger.error("processMsgCQSscdn occur error:", e);
        } finally {
            lock.unlock();
        }


    }

    /**
     * 重庆时时彩 - 【1-5球】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_SSC_CQ_15)
    @JmsListener(destination = ActiveMQConfig.TOPIC_SSC_CQ_15, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgCQSsc15(String message) throws Exception {
        logger.info("重庆时时彩- 【1-5球】结算期号：  " + message);
        // 拆分消息内容
        String[] str = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.TOPIC_SSC_CQ_15 + str[1];
//        RReadWriteLock lock = redissonClient.getReadWriteLock(key+"lock");
        RedisLock lock = new RedisLock(key + "lock", 3 * 1000, 30 * 1000);
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
//            boolean bool = lock.writeLock().tryLock(2, 10, TimeUnit.SECONDS);
            if (lock.lock()) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    // 【定位胆】规则
                    betSscbmService.count15(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.CQSSC.getTagType()));
                }
            }
        } catch (Exception e) {
            logger.error("processMsgCQSsc15 occur error:", e);
        } finally {
            lock.unlock();
        }

    }

    /**
     * 重庆时时彩 - 【前中后】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.QUEUE_SSC_CQ_QZH)
    @JmsListener(destination = ActiveMQConfig.TOPIC_SSC_CQ_QZH, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgCQSscqzh(String message) throws Exception {
        logger.info("重庆时时彩 - 【前中后】结算期号：  " + message);
        // 拆分消息内容
        String[] str = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.TOPIC_SSC_CQ_QZH + str[1];
//        RReadWriteLock lock = redissonClient.getReadWriteLock(key+"lock");
        RedisLock lock = new RedisLock(key + "lock", 3 * 1000, 30 * 1000);
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
//            boolean bool = lock.writeLock().tryLock(2, 10, TimeUnit.SECONDS);
            if (lock.lock()) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    // 【定位大小单双】规则
                    betSscbmService.countqzh(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.CQSSC.getTagType()));
                }
            }
        } catch (Exception e) {
            logger.error("processMsgCQSscqzh occur error:", e);
        } finally {
            lock.unlock();
        }

    }


    /**
     * ss彩 - 新疆时时彩【直选】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_SSC_XJ_LM)
    @JmsListener(destination = ActiveMQConfig.TOPIC_SSC_XJ_LM, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgXJSscLM(String message) throws Exception {
        logger.info("XJ时时彩 - 【两面】结算期号：  " + message);
        // 拆分消息内容
        String[] str = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.TOPIC_SSC_XJ_LM + str[1];
//        RReadWriteLock lock = redissonClient.getReadWriteLock(key+"lock");
        RedisLock lock = new RedisLock(key + "lock", 3 * 1000, 30 * 1000);
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
//            boolean bool = lock.writeLock().tryLock(2, 10, TimeUnit.SECONDS);
            if (lock.lock()) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    if (str.length == 4) {   //表面需要同步数据
                        //第一步同步中奖结果
                        XjsscLotterySg updateSg = JSONObject.parseObject(str[3].replace("$", ":"), XjsscLotterySg.class);
                        updateSg.setActualDate(DateUtils.str2date(updateSg.getTime()));
                        updateSg.setIdealDate(DateUtils.parseDate(updateSg.getIdealTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                        int boolUpdate = xjsscLotterySgMapperExt.updateByIssue(updateSg);
                        if (boolUpdate == 0) {  //说明这一天的期号数据可能不全，则做一次检查，如果真没有，则从task_server同步当天所有期号数据过来
                            XjsscLotterySgExample sgExample = new XjsscLotterySgExample();
                            XjsscLotterySgExample.Criteria criteria = sgExample.createCriteria();
                            Calendar calendar = Calendar.getInstance();
                            criteria.andIdealTimeGreaterThanOrEqualTo(DateUtils.formatDate(new Date(), DateUtils.FORMAT_YYYY_MM_DD));
                            calendar.add(Calendar.DAY_OF_MONTH, 1);
                            criteria.andIdealTimeLessThan(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD));
                            int afterCount = xjsscLotterySgMapper.countByExample(sgExample);
                            if (afterCount < 48) {
                                //则发送消息
                                logger.info("同步预期数据发送通知：xjsscYuqiToday");
                                Destination destination = new ActiveMQQueue(ActiveMQConfig.TOPIC_YUQI_TODAYT);
                                jmsMessagingTemplate.convertAndSend(destination, "xjsscYuqiToday");
                            }
                        } else {
                            //更新历史赛果缓存
                            RedisBusinessUtil.updateLsSgCache(Constants.LOTTERY_XJSSC, RedisKeys.XJSSC_SG_HS_LIST, updateSg);
                        }

                    }
                    // 把开奖信息和下期放入缓存
                    xjsscLotterySgWriteService.cacheIssueResult(str[1], str[2]);
                    // 结算【两面】
                    betSscbmService.countlm(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.XJSSC.getTagType()));

                    //第三步 最近1天未开奖的数据
                    XjsscLotterySgExample xjsscLotterySgExample = new XjsscLotterySgExample();
                    XjsscLotterySgExample.Criteria criteria = xjsscLotterySgExample.createCriteria();
                    criteria.andOpenStatusEqualTo(Constants.WAIT);
                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.MINUTE, -5);  //过滤掉最新一期，（这一期可能需要2分钟才抓到）
                    criteria.andIdealTimeLessThan(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                    calendar.add(Calendar.DAY_OF_MONTH, -1);
                    criteria.andIdealTimeGreaterThanOrEqualTo(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                    xjsscLotterySgExample.setOrderByClause("ideal_time desc");
                    xjsscLotterySgExample.setLimit(15);
                    xjsscLotterySgExample.setOffset(0);
                    List<XjsscLotterySg> xjsscLotterySgList = xjsscLotterySgMapper.selectByExample(xjsscLotterySgExample);
                    if (xjsscLotterySgList.size() > 0) {
                        String issues = "";
                        for (XjsscLotterySg sg : xjsscLotterySgList) {
                            issues = issues + sg.getIssue() + ",";
                        }
                        issues = issues.substring(0, issues.length() - 1);
                        Destination destination = new ActiveMQQueue(ActiveMQConfig.TOPIC_MISSING_LOTTERY_SG);
                        jmsMessagingTemplate.convertAndSend(destination, LotteryTableNameEnum.XJSSC.getLotteryId() + "#" + issues);
                        logger.info("发送缺奖数据{}，{}", LotteryTableNameEnum.XJSSC.getLotteryId(), issues);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("processMsgXJSscLM occur error:", e);
        } finally {
            lock.unlock();
        }

    }

    /**
     * ss彩 - 新疆时时彩【组选】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_SSC_XJ_DN)
    @JmsListener(destination = ActiveMQConfig.TOPIC_SSC_XJ_DN, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgXJSscdn(String message) throws Exception {
        logger.info("XJ时时彩 - 【斗牛】结算期号：  " + message);
        // 拆分消息内容
        String[] str = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.TOPIC_SSC_XJ_DN + str[1];
//        RReadWriteLock lock = redissonClient.getReadWriteLock(key+"lock");
        RedisLock lock = new RedisLock(key + "lock", 3 * 1000, 30 * 1000);
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
//            boolean bool = lock.writeLock().tryLock(2, 10, TimeUnit.SECONDS);
            if (lock.lock()) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    // 基本【组选】规则
                    betSscbmService.countdn(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.XJSSC.getTagType()));
                }
            }
        } catch (Exception e) {
            logger.error("processMsgXJSscdn occur error:{}", e);
        } finally {
            lock.unlock();
        }

    }

    /**
     * XJ时时彩 - 【1-5球】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_SSC_XJ_15)
    @JmsListener(destination = ActiveMQConfig.TOPIC_SSC_XJ_15, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgXJSsc15(String message) throws Exception {
        logger.info("XJ时时彩- 【1-5球】结算期号：  " + message);
        // 拆分消息内容
        String[] str = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.TOPIC_SSC_XJ_15 + str[1];
//        RReadWriteLock lock = redissonClient.getReadWriteLock(key+"lock");
        RedisLock lock = new RedisLock(key + "lock", 3 * 1000, 30 * 1000);
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
//            boolean bool = lock.writeLock().tryLock(2, 10, TimeUnit.SECONDS);
            if (lock.lock()) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    // 【定位胆】规则
                    betSscbmService.count15(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.XJSSC.getTagType()));
                }
            }

        } catch (Exception e) {
            logger.error("processMsgXJSsc15 occur error:", e);
        } finally {
            lock.unlock();
        }

    }

    /**
     * XJ时时彩 - 【前中后】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_SSC_XJ_QZH)
    @JmsListener(destination = ActiveMQConfig.TOPIC_SSC_XJ_QZH, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgXJSscqzh(String message) throws Exception {
        logger.info("XJ时时彩 - 【前中后】结算期号：  " + message);
        // 拆分消息内容
        String[] str = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.TOPIC_SSC_XJ_QZH + str[1];
//        RReadWriteLock lock = redissonClient.getReadWriteLock(key+"lock");
        RedisLock lock = new RedisLock(key + "lock", 3 * 1000, 30 * 1000);
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
//            boolean bool = lock.writeLock().tryLock(2, 10, TimeUnit.SECONDS);
            if (lock.lock()) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    // 【定位大小单双】规则
                    betSscbmService.countqzh(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.XJSSC.getTagType()));
                }
            }
        } catch (Exception e) {
            logger.error("processMsgXJSscqzh occur error:", e);
        } finally {
            lock.unlock();
        }


    }


    /**
     * ss彩 - 天津时时彩【两面】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_SSC_TJ_LM)
    @JmsListener(destination = ActiveMQConfig.TOPIC_SSC_TJ_LM, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgTJSscLM(String message) throws Exception {
        logger.info("TJ时时彩 - 【两面】结算期号：  " + message);
        // 拆分消息内容
        String[] str = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.TOPIC_SSC_TJ_LM + str[1];
        // 【分布式读写锁】
//        RReadWriteLock lock = redissonClient.getReadWriteLock(key+"lock");
        RedisLock lock = new RedisLock(key + "lock", 3 * 1000, 30 * 1000);
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
//            boolean bool = lock.writeLock().tryLock(2, 10, TimeUnit.SECONDS);
            // 判断是否获取到锁
            if (lock.lock()) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    //第一步同步中奖结果
                    TjsscLotterySg updateSg = JSONObject.parseObject(str[3].replace("$", ":"), TjsscLotterySg.class);
                    updateSg.setActualDate(DateUtils.str2date(updateSg.getTime()));
                    updateSg.setIdealDate(DateUtils.parseDate(updateSg.getIdealTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                    int boolUpdate = tjsscLotterySgMapperExt.updateByIssue(updateSg);
                    if (boolUpdate == 0) {  //说明这一天的期号数据可能不全，则做一次检查，如果真没有，则从task_server同步当天所有期号数据过来
                        TjsscLotterySgExample sgExample = new TjsscLotterySgExample();
                        TjsscLotterySgExample.Criteria criteria = sgExample.createCriteria();
                        Calendar calendar = Calendar.getInstance();
                        criteria.andIdealTimeGreaterThanOrEqualTo(DateUtils.formatDate(new Date(), DateUtils.FORMAT_YYYY_MM_DD));
                        calendar.add(Calendar.DAY_OF_MONTH, 1);
                        criteria.andIdealTimeLessThan(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD));
                        int afterCount = tjsscLotterySgMapper.countByExample(sgExample);
                        if (afterCount < 42) {
                            //则发送消息
                            logger.info("同步预期数据发送通知：tjsscYuqiToday");
                            Destination destination = new ActiveMQQueue(ActiveMQConfig.TOPIC_YUQI_TODAYT);
                            jmsMessagingTemplate.convertAndSend(destination, "tjsscYuqiToday");
                        }
                    } else {
                        //更新历史赛果缓存
                        RedisBusinessUtil.updateLsSgCache(Constants.LOTTERY_TJSSC, RedisKeys.TJSSC_SG_HS_LIST, updateSg);
                    }

                    xjsscLotterySgWriteService.cacheIssueResultForXjssc(str[1], str[2]);
                    // 结算【两面】
                    betSscbmService.countlm(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.TJSSC.getTagType()));

                    //第三步 最近1天未开奖的数据
                    TjsscLotterySgExample tjsscLotterySgExample = new TjsscLotterySgExample();
                    TjsscLotterySgExample.Criteria criteria = tjsscLotterySgExample.createCriteria();
                    criteria.andOpenStatusEqualTo(Constants.WAIT);
                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.MINUTE, -5);  //过滤掉最新一期，（这一期可能需要2分钟才抓到）
                    criteria.andIdealTimeLessThan(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                    calendar.add(Calendar.DAY_OF_MONTH, -1);
                    criteria.andIdealTimeGreaterThanOrEqualTo(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                    tjsscLotterySgExample.setOrderByClause("ideal_time desc");
                    tjsscLotterySgExample.setLimit(15);
                    tjsscLotterySgExample.setOffset(0);
                    List<TjsscLotterySg> tjsscLotterySgList = tjsscLotterySgMapper.selectByExample(tjsscLotterySgExample);
                    if (tjsscLotterySgList.size() > 0) {
                        String issues = "";
                        for (TjsscLotterySg sg : tjsscLotterySgList) {
                            issues = issues + sg.getIssue() + ",";
                        }
                        issues = issues.substring(0, issues.length() - 1);
                        Destination destination = new ActiveMQQueue(ActiveMQConfig.TOPIC_MISSING_LOTTERY_SG);
                        jmsMessagingTemplate.convertAndSend(destination, LotteryTableNameEnum.TJSSC.getLotteryId() + "#" + issues);
                        logger.info("发送缺奖数据{}，{}", LotteryTableNameEnum.TJSSC.getLotteryId(), issues);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("processMsgTJSscLM occur error:", e);
        } finally {
            lock.unlock();
        }

    }

    /**
     * TJ时时彩 - 【斗牛】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_SSC_TJ_DN)
    @JmsListener(destination = ActiveMQConfig.TOPIC_SSC_TJ_DN, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgTJSscdn(String message) throws Exception {
        logger.info("TJ时时彩 - 【斗牛】结算期号：  " + message);
        // 拆分消息内容
        String[] str = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.TOPIC_SSC_TJ_DN + str[1];
//        RReadWriteLock lock = redissonClient.getReadWriteLock(key+"lock");
        RedisLock lock = new RedisLock(key + "lock", 3 * 1000, 30 * 1000);
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
//            boolean bool = lock.writeLock().tryLock(2, 10, TimeUnit.SECONDS);
            if (lock.lock()) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    // 基本【组选】规则
                    betSscbmService.countdn(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.TJSSC.getTagType()));
                }
            }

        } catch (Exception e) {
            logger.error("processMsgTJSscdn occur error:", e);
        } finally {
            lock.unlock();
        }


    }

    /**
     * TJ时时彩 - 【1-5球】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_SSC_TJ_15)
    @JmsListener(destination = ActiveMQConfig.TOPIC_SSC_TJ_15, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgTJSsc15(String message) throws Exception {
        logger.info("TJ时时彩- 【1-5球】结算期号：  " + message);
        // 拆分消息内容
        String[] str = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.TOPIC_SSC_TJ_15 + str[1];
//        RReadWriteLock lock = redissonClient.getReadWriteLock(key+"lock");
        RedisLock lock = new RedisLock(key + "lock", 3 * 1000, 30 * 1000);
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
//            boolean bool = lock.writeLock().tryLock(2, 10, TimeUnit.SECONDS);
            if (lock.lock()) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    // 【定位胆】规则
                    betSscbmService.count15(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.TJSSC.getTagType()));
                }
            }
        } catch (Exception e) {
            logger.error("processMsgTJSsc15 occur error:", e);
        } finally {
            lock.unlock();
        }


    }

    /**
     * TJ时时彩 - 【前中后】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_SSC_TJ_QZH)
    // @JmsListener(destination = ActiveMQConfig.TOPIC_SSC_TJ_QZH, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgTJSscqzh(String message) throws Exception {
//        logger.info("TJ时时彩 - 【前中后】结算期号：  " + message);
//        // 拆分消息内容
//        String[] str = message.split(":");
//        // 获取唯一
//        String key = ActiveMQConfig.TOPIC_SSC_TJ_QZH + str[1];
////        RReadWriteLock lock = redissonClient.getReadWriteLock(key+"lock");
//        RedisLock lock = new RedisLock(key + "lock", 3 * 1000, 30 * 1000);
//        try {
//            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
////            boolean bool = lock.writeLock().tryLock(2, 10, TimeUnit.SECONDS);
//            if (lock.lock()) {
//                if (basicRedisClient.get(key) == null) {
//                    basicRedisClient.set(key, "1", 50l);
//                    // 【定位大小单双】规则
//                    betSscbmService.countqzh(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.TJSSC.getTagType()));
//                }
//            }
//        } catch (Exception e) {
//            logger.error("processMsgTJSscqzh occur error:{}", e);
//        } finally {
//            lock.unlock();
//        }

    }

    /**
     * 重庆时时彩 - 更新【免费推荐】/【公式杀号】数据 队列名
     *
     * @param message 消息内容
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_SSC_TJ_UPDATE_DATA)
    @JmsListener(destination = ActiveMQConfig.TOPIC_SSC_TJ_UPDATE_DATA, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgTJSscUpdateData(String message) throws Exception {
        logger.info("更新【免费推荐】/【公式杀号】数据");

        // 拆分消息内容
        String[] str = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.TOPIC_SSC_TJ_UPDATE_DATA + str[1];
        // 【分布式读写锁】
//        RReadWriteLock lock = redissonClient.getReadWriteLock(key+"lock");
        RedisLock lock = new RedisLock(key + "lock", 3 * 1000, 30 * 1000);
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
//            boolean bool = lock.writeLock().tryLock(2, 10, TimeUnit.SECONDS);
            if (lock.lock()) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    betSscbmService.updateDataTJ(str[1], str[2]);
                }
            }
        } catch (Exception e) {
            logger.error("processMsgTJSscUpdateData occur error:", e);
        } finally {
            lock.unlock();
        }

    }


    /**
     * ss彩 - 十分时时彩【两面】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_SSC_TEN_LM)
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_SSC_TEN_LM, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgTENSscLM(String message) throws Exception {
        logger.info("TEN时时彩 - 【两面】结算期号：  " + message);
        // 拆分消息内容
        String[] str = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_SSC_TEN_LM + str[1];
        // 【分布式读写锁】
//        RReadWriteLock lock = redissonClient.getReadWriteLock(key+"lock");
        RedisLock lock = new RedisLock(key + "lock", 3 * 1000, 30 * 1000);
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
//            boolean bool = lock.writeLock().tryLock(2, 10, TimeUnit.SECONDS);
            if (lock.lock()) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    //第一步同步中奖结果
                    TensscLotterySg updateSg = JSONObject.parseObject(str[3].replace("$", ":"), TensscLotterySg.class);
                    updateSg.setActualDate(DateUtils.str2date(updateSg.getTime()));
                    updateSg.setIdealDate(DateUtils.parseDate(updateSg.getIdealTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                    int boolUpdate = tensscLotterySgMapperExt.updateByIssue(updateSg);

                    if (boolUpdate == 0) {  //，说明这一天的期号数据可能不全则做一次检查，如果真没有，则从task_server同步当天所有期号数据过来
                        TensscLotterySgExample sgExample = new TensscLotterySgExample();
                        TensscLotterySgExample.Criteria criteria = sgExample.createCriteria();
                        Calendar calendar = Calendar.getInstance();
                        criteria.andIdealTimeGreaterThanOrEqualTo(DateUtils.formatDate(new Date(), DateUtils.FORMAT_YYYY_MM_DD));
                        calendar.add(Calendar.DAY_OF_MONTH, 1);
                        criteria.andIdealTimeLessThan(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD));
                        int afterCount = tensscLotterySgMapper.countByExample(sgExample);
                        if (afterCount < 144) {
                            //则发送消息
                            logger.info("同步预期数据发送通知：tensscYuqiToday");
                            Destination destination = new ActiveMQQueue(ActiveMQConfig.PLATFORM_TOPIC_YUQI_TODAYT);
                            jmsMessagingTemplate.convertAndSend(destination, "tensscYuqiToday");
                        }
                    } else {
                        //更新历史赛果缓存
                        RedisBusinessUtil.updateLsSgCache(Constants.LOTTERY_TENSSC, RedisKeys.TENSSC_SG_HS_LIST, updateSg);
                    }

                    xjsscLotterySgWriteService.cacheIssueResultForTenssc(str[1], str[2]);
                    // 结算【两面】
                    betSscbmService.countlm(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.TENSSC.getTagType()));

                    //第三步 最近1天未开奖的数据
                    TensscLotterySgExample tensscLotterySgExample = new TensscLotterySgExample();
                    TensscLotterySgExample.Criteria criteria = tensscLotterySgExample.createCriteria();
                    criteria.andOpenStatusEqualTo(Constants.WAIT);
                    Calendar calendar = Calendar.getInstance();
                    criteria.andIdealTimeLessThan(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                    calendar.add(Calendar.DAY_OF_MONTH, -1);
                    criteria.andIdealTimeGreaterThanOrEqualTo(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                    tensscLotterySgExample.setOrderByClause("ideal_time desc");
                    tensscLotterySgExample.setLimit(15);
                    tensscLotterySgExample.setOffset(0);
                    List<TensscLotterySg> tensscLotterySgList = tensscLotterySgMapper.selectByExample(tensscLotterySgExample);
                    if (tensscLotterySgList.size() > 0) {
                        String issues = "";
                        for (TensscLotterySg sg : tensscLotterySgList) {
                            issues = issues + sg.getIssue() + ",";
                        }
                        issues = issues.substring(0, issues.length() - 1);
                        Destination destination = new ActiveMQQueue(ActiveMQConfig.PLATFORM_TOPIC_MISSING_LOTTERY_SG);
                        jmsMessagingTemplate.convertAndSend(destination, LotteryTableNameEnum.TENSSC.getLotteryId() + "#" + issues);
                        logger.info("发送缺奖数据{}，{}", LotteryTableNameEnum.TENSSC.getLotteryId(), issues);
                    }
                }
            }

        } catch (Exception e) {
            logger.error("processMsgTENSscLM occur error:", e);
        } finally {
            lock.unlock();
        }

    }

    /**
     * TEN时时彩 - 【斗牛】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_SSC_TEN_DN)
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_SSC_TEN_DN, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgTENSscdn(String message) throws Exception {
        logger.info("TEN时时彩 - 【斗牛】结算期号：  " + message);
        // 拆分消息内容
        String[] str = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_SSC_TEN_DN + str[1];
//        RReadWriteLock lock = redissonClient.getReadWriteLock(key+"lock");
        RedisLock lock = new RedisLock(key + "lock", 3 * 1000, 30 * 1000);
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
//            boolean bool = lock.writeLock().tryLock(2, 10, TimeUnit.SECONDS);
            if (lock.lock()) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    // 基本【组选】规则
                    betSscbmService.countdn(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.TENSSC.getTagType()));
                }
            }
        } catch (Exception e) {
            logger.error("processMsgTENSscdn occur error:", e);
        } finally {
            lock.unlock();
        }

    }

    /**
     * TEN时时彩 - 【1-5球】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_SSC_TEN_15)
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_SSC_TEN_15, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgTENSsc15(String message) throws Exception {
        logger.info("TEN时时彩- 【1-5球】结算期号：  " + message);
        // 拆分消息内容
        String[] str = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_SSC_TEN_15 + str[1];
        // 【分布式读写锁】
//        RReadWriteLock lock = redissonClient.getReadWriteLock(key+"lock");
        RedisLock lock = new RedisLock(key + "lock", 3 * 1000, 30 * 1000);
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
//            boolean bool = lock.writeLock().tryLock(2, 10, TimeUnit.SECONDS);
            if (lock.lock()) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    // 【定位胆】规则
                    betSscbmService.count15(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.TENSSC.getTagType()));
                }
            }
        } catch (Exception e) {
            logger.error("processMsgTENSsc15 occur error:", e);
        } finally {
            lock.unlock();
        }

    }

    /**
     * TEN时时彩 - 【前中后】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_SSC_TEN_QZH)
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_SSC_TEN_QZH, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgTENSscqzh(String message) throws Exception {
        logger.info("TEN时时彩 - 【前中后】结算期号：  " + message);
        // 拆分消息内容
        String[] str = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_SSC_TEN_QZH + str[1];
//        RReadWriteLock lock = redissonClient.getReadWriteLock(key+"lock");
        RedisLock lock = new RedisLock(key + "lock", 3 * 1000, 30 * 1000);
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
//            boolean bool = lock.writeLock().tryLock(2, 10, TimeUnit.SECONDS);
            if (lock.lock()) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    // 【定位大小单双】规则
                    betSscbmService.countqzh(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.TENSSC.getTagType()));
                }
            }
        } catch (Exception e) {
            logger.error("processMsgTENSscqzh occur error:", e);
        } finally {
            // 释放锁
            lock.unlock();
        }


    }


    /**
     * ss彩 - 【两面】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_SSC_FIVE_LM)
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_SSC_FIVE_LM, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgFIVESscLM(String message) throws Exception {
        logger.info("FIVE时时彩 - 【两面】结算期号：  " + message);
        // 拆分消息内容
        String[] str = message.split(":");

        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_SSC_FIVE_LM + str[1];
        // 【分布式读写锁】
//        RReadWriteLock lock = redissonClient.getReadWriteLock(key+"lock");
        RedisLock lock = new RedisLock(key + "lock", 3 * 1000, 30 * 1000);
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
//            boolean bool = lock.writeLock().tryLock(2, 10, TimeUnit.SECONDS);
            if (lock.lock()) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    //第一步同步中奖结果
                    FivesscLotterySg updateSg = JSONObject.parseObject(str[3].replace("$", ":"), FivesscLotterySg.class);
                    updateSg.setActualDate(DateUtils.str2date(updateSg.getTime()));
                    updateSg.setIdealDate(DateUtils.parseDate(updateSg.getIdealTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                    int boolUpdate = fivesscLotterySgMapperExt.updateByIssue(updateSg);
                    if (boolUpdate == 0) {  //说明这一天的期号数据可能不全，则做一次检查，如果真没有，则从task_server同步当天所有期号数据过来
                        FivesscLotterySgExample sgExample = new FivesscLotterySgExample();
                        FivesscLotterySgExample.Criteria criteria = sgExample.createCriteria();
                        Calendar calendar = Calendar.getInstance();
                        criteria.andIdealTimeGreaterThanOrEqualTo(DateUtils.formatDate(new Date(), DateUtils.FORMAT_YYYY_MM_DD));
                        calendar.add(Calendar.DAY_OF_MONTH, 1);
                        criteria.andIdealTimeLessThan(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD));
                        int afterCount = fivesscLotterySgMapper.countByExample(sgExample);
                        if (afterCount < 288) {
                            //则发送消息
                            logger.info("同步预期数据发送通知：fivesscYuqiToday");
                            Destination destination = new ActiveMQQueue(ActiveMQConfig.PLATFORM_TOPIC_YUQI_TODAYT);
                            jmsMessagingTemplate.convertAndSend(destination, "fivesscYuqiToday");
                        }
                    } else {
                        //更新历史赛果缓存
                        RedisBusinessUtil.updateLsSgCache(Constants.LOTTERY_FIVESSC, RedisKeys.FIVESSC_SG_HS_LIST, updateSg);
                    }

                    xjsscLotterySgWriteService.cacheIssueResultForFivessc(str[1], str[2]);
                    // 结算【两面】
                    betSscbmService.countlm(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.FIVESSC.getTagType()));

                    //第三步 最近1天未开奖的数据
                    FivesscLotterySgExample fivesscLotterySgExample = new FivesscLotterySgExample();
                    FivesscLotterySgExample.Criteria criteria = fivesscLotterySgExample.createCriteria();
                    criteria.andOpenStatusEqualTo(Constants.WAIT);
                    Calendar calendar = Calendar.getInstance();
                    criteria.andIdealTimeLessThan(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                    calendar.add(Calendar.DAY_OF_MONTH, -1);
                    criteria.andIdealTimeGreaterThanOrEqualTo(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                    fivesscLotterySgExample.setOrderByClause("ideal_time desc");
                    fivesscLotterySgExample.setLimit(15);
                    fivesscLotterySgExample.setOffset(0);
                    List<FivesscLotterySg> fivesscLotterySgList = fivesscLotterySgMapper.selectByExample(fivesscLotterySgExample);
                    if (fivesscLotterySgList.size() > 0) {
                        String issues = "";
                        for (FivesscLotterySg sg : fivesscLotterySgList) {
                            issues = issues + sg.getIssue() + ",";
                        }
                        issues = issues.substring(0, issues.length() - 1);
                        Destination destination = new ActiveMQQueue(ActiveMQConfig.PLATFORM_TOPIC_MISSING_LOTTERY_SG);
                        jmsMessagingTemplate.convertAndSend(destination, LotteryTableNameEnum.FIVESSC.getLotteryId() + "#" + issues);
                        logger.info("发送缺奖数据{}，{}", LotteryTableNameEnum.FIVESSC.getLotteryId(), issues);
                    }
                }
            }

        } catch (Exception e) {
            logger.error("processMsgFIVESscLM occur error:", e);
        } finally {
            lock.unlock();
        }

    }

    /**
     * FIVE时时彩 - 【斗牛】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_SSC_FIVE_DN)
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_SSC_FIVE_DN, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgFIVESscdn(String message) throws Exception {
        logger.info("FIVE时时彩 - 【斗牛】结算期号：  " + message);
        // 拆分消息内容
        String[] str = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_SSC_FIVE_DN + str[1];
        // 【分布式读写锁】
//        RReadWriteLock lock = redissonClient.getReadWriteLock(key+"lock");
        RedisLock lock = new RedisLock(key + "lock", 3 * 1000, 30 * 1000);
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
//            boolean bool = lock.writeLock().tryLock(2, 10, TimeUnit.SECONDS);
            if (lock.lock()) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    // 基本【组选】规则
                    betSscbmService.countdn(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.FIVESSC.getTagType()));
                }
            }

        } catch (Exception e) {
            logger.error("processMsgFIVESscdn occur error:", e);
        } finally {
            lock.unlock();
        }


    }

    /**
     * 五分时时彩 - 【1-5球】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_SSC_FIVE_15)
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_SSC_FIVE_15, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgFIVESsc15(String message) throws Exception {
        logger.info("TEN时时彩- 【1-5球】结算期号：  " + message);
        // 拆分消息内容
        String[] str = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_SSC_FIVE_15 + str[1];
//        RReadWriteLock lock = redissonClient.getReadWriteLock(key+"lock");
        RedisLock lock = new RedisLock(key + "lock", 3 * 1000, 30 * 1000);
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
//            boolean bool = lock.writeLock().tryLock(2, 10, TimeUnit.SECONDS);
            if (lock.lock()) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    // 【定位胆】规则
                    betSscbmService.count15(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.FIVESSC.getTagType()));
                }
            }
        } catch (Exception e) {
            logger.error("processMsgFIVESsc15 occur error:", e);
        } finally {
            lock.unlock();
        }

    }

    /**
     * FIVE时时彩 - 【前中后】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_SSC_FIVE_QZH)
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_SSC_FIVE_QZH, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgFIVESscqzh(String message) throws Exception {
        logger.info("FIVE时时彩 - 【前中后】结算期号：  " + message);
        // 拆分消息内容
        String[] str = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_SSC_FIVE_QZH + str[1];
        // 【分布式读写锁】
//        RReadWriteLock lock = redissonClient.getReadWriteLock(key+"lock");
        RedisLock lock = new RedisLock(key + "lock", 3 * 1000, 30 * 1000);
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
//            boolean bool = lock.writeLock().tryLock(2, 10, TimeUnit.SECONDS);
            if (lock.lock()) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    // 【定位大小单双】规则
                    betSscbmService.countqzh(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.FIVESSC.getTagType()));
                }
            }
        } catch (Exception e) {
            logger.error("processMsgFIVESscqzh occur error:", e);
        } finally {
            lock.unlock();
        }


    }


    /**
     * ss彩 - 【两面】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_SSC_JS_LM)
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_SSC_JS_LM, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgJSSscLM(String message) throws Exception {
        logger.info("JS时时彩 - 【两面】结算期号：  " + message);
        // 拆分消息内容
        String[] str = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_SSC_JS_LM + str[1];
        // 【分布式读写锁】
//        RReadWriteLock lock = redissonClient.getReadWriteLock(key+"lock");
        RedisLock lock = new RedisLock(key + "lock", 3 * 1000, 30 * 1000);
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
//            boolean bool = lock.writeLock().tryLock(2, 10, TimeUnit.SECONDS);
            if (lock.lock()) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    //第一步同步中奖结果
                    JssscLotterySg updateSg = JSONObject.parseObject(str[3].replace("$", ":"), JssscLotterySg.class);
                    updateSg.setActualDate(DateUtils.str2date(updateSg.getTime()));
                    updateSg.setIdealDate(DateUtils.parseDate(updateSg.getIdealTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                    int boolUpdate = jssscLotterySgMapperExt.updateByIssue(updateSg);
                    if (boolUpdate == 0) {  //说明这一天的期号数据可能不全，则做一次检查，如果真没有，则从task_server同步当天所有期号数据过来
                        JssscLotterySgExample sgExample = new JssscLotterySgExample();
                        JssscLotterySgExample.Criteria criteria = sgExample.createCriteria();
                        Calendar calendar = Calendar.getInstance();
                        criteria.andIdealTimeGreaterThanOrEqualTo(DateUtils.formatDate(new Date(), DateUtils.FORMAT_YYYY_MM_DD));
                        calendar.add(Calendar.DAY_OF_MONTH, 1);
                        criteria.andIdealTimeLessThan(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD));
                        int afterCount = jssscLotterySgMapper.countByExample(sgExample);
                        if (afterCount < 1440) {
                            //则发送消息
                            logger.info("同步预期数据发送通知：jssscYuqiToday");
                            Destination destination = new ActiveMQQueue(ActiveMQConfig.PLATFORM_TOPIC_YUQI_TODAYT);
                            jmsMessagingTemplate.convertAndSend(destination, "jssscYuqiToday");
                        }
                    } else {
                        //更新历史赛果缓存
                        RedisBusinessUtil.updateLsSgCache(Constants.LOTTERY_DZSSC, RedisKeys.JSSSC_SG_HS_LIST, updateSg);
                    }

                    xjsscLotterySgWriteService.cacheIssueResultForJsssc(str[1], str[2]);// 德州时时彩
//                    xjsscLotterySgWriteService.cacheIssueResultForFtjsssc(str[1], str[2]);// 德州时时彩番摊
                    // 结算【两面】
                    betSscbmService.countlm(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.JSSSC.getTagType()));

                    //第三步 最近1天未开奖的数据
                    JssscLotterySgExample jssscLotterySgExample = new JssscLotterySgExample();
                    JssscLotterySgExample.Criteria criteria = jssscLotterySgExample.createCriteria();
                    criteria.andOpenStatusEqualTo(Constants.WAIT);
                    Calendar calendar = Calendar.getInstance();
                    criteria.andIdealTimeLessThan(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                    calendar.add(Calendar.DAY_OF_MONTH, -1);
                    criteria.andIdealTimeGreaterThanOrEqualTo(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                    jssscLotterySgExample.setOrderByClause("ideal_time desc");
                    jssscLotterySgExample.setLimit(15);
                    jssscLotterySgExample.setOffset(0);
                    List<JssscLotterySg> jssscLotterySgList = jssscLotterySgMapper.selectByExample(jssscLotterySgExample);
                    if (jssscLotterySgList.size() > 0) {
                        String issues = "";
                        for (JssscLotterySg sg : jssscLotterySgList) {
                            issues = issues + sg.getIssue() + ",";
                        }
                        issues = issues.substring(0, issues.length() - 1);
                        Destination destination = new ActiveMQQueue(ActiveMQConfig.PLATFORM_TOPIC_MISSING_LOTTERY_SG);
                        jmsMessagingTemplate.convertAndSend(destination, LotteryTableNameEnum.JSSSC.getLotteryId() + "#" + issues);
                        logger.info("发送缺奖数据{}，{}", LotteryTableNameEnum.JSSSC.getLotteryId(), issues);
                    }

                }
            }

        } catch (Exception e) {
            logger.error("processMsgJSSscLM occur error:", e);
        } finally {
            lock.unlock();
        }
    }


    /**
     * JS时时彩 - 【斗牛】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_SSC_JS_DN)
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_SSC_JS_DN, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgJSSscdn(String message) throws Exception {
        logger.info("JS时时彩 - 【斗牛】结算期号：  " + message);
        // 拆分消息内容
        String[] str = message.split(":");

        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_SSC_JS_DN + str[1];
        // 【分布式读写锁】
//        RReadWriteLock lock = redissonClient.getReadWriteLock(key+"lock");
        RedisLock lock = new RedisLock(key + "lock", 3 * 1000, 30 * 1000);
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
//            boolean bool = lock.writeLock().tryLock(2, 10, TimeUnit.SECONDS);
            if (lock.lock()) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    // 基本【组选】规则
                    betSscbmService.countdn(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.JSSSC.getTagType()));
                }
            }
        } catch (Exception e) {
            logger.error("processMsgJSSscdn occur error:", e);
        } finally {
            lock.unlock();
        }

    }


    /**
     * JS时时彩 - 【1-5球】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_SSC_JS_15)
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_SSC_JS_15, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgJSSsc15(String message) throws Exception {
        logger.info("JS时时彩- 【1-5球】结算期号：  " + message);
        // 拆分消息内容
        String[] str = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_SSC_JS_15 + str[1];
        // 【分布式读写锁】
//        RReadWriteLock lock = redissonClient.getReadWriteLock(key+"lock");
        RedisLock lock = new RedisLock(key + "lock", 3 * 1000, 30 * 1000);
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
//            boolean bool = lock.writeLock().tryLock(2, 10, TimeUnit.SECONDS);
            if (lock.lock()) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 60l);
                    // 【定位胆】规则
                    betSscbmService.count15(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.JSSSC.getTagType()));
                }
            }
        } catch (Exception e) {
            logger.error("processMsgJSSsc15 occur error:", e);
        } finally {
            lock.unlock();
        }

    }

    /**
     * JS时时彩 - 【前中后】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_SSC_JS_QZH)
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_SSC_JS_QZH, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgJSSscqzh(String message) throws Exception {
        logger.info("JS时时彩 - 【前中后】结算期号：  " + message);
        // 拆分消息内容
        String[] str = message.split(":");
        // 【定位大小单双】规则
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_SSC_JS_QZH + str[1];
        // 【分布式读写锁】
//        RReadWriteLock lock = redissonClient.getReadWriteLock(key+"lock");
        RedisLock lock = new RedisLock(key + "lock", 3 * 1000, 30 * 1000);
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
//            boolean bool = lock.writeLock().tryLock(2, 10, TimeUnit.SECONDS);
            if (lock.lock()) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 60l);
                    betSscbmService.countqzh(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.JSSSC.getTagType()));
                }
            }

        } catch (Exception e) {
            logger.error("processMsgJSSscqzh occur error:", e);
        } finally {
            lock.unlock();
        }

    }

    @JmsListener(destination = ActiveMQConfig.TOPIC_FC_SSQ_YUQI_DATA, containerFactory = "jmsListenerContainerTopicDurable")
    public void sameDataYuqiFCSSQ(String message) throws Exception {
        logger.info("【福彩双色球】预期数据同步：  " + message);
        List<FcssqLotterySg> list = JSONObject.parseArray(message.substring(message.indexOf(":") + 1).substring(message.indexOf(":") + 1), FcssqLotterySg.class);
        // 获取唯一
        String key = ActiveMQConfig.TOPIC_FC_SSQ_YUQI_DATA + list.get(0).getIssue();
//        RReadWriteLock lock = redissonClient.getReadWriteLock(key+"lock");
        RedisLock lock = new RedisLock(key + "lock", 3 * 1000, 30 * 1000);
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
//            boolean bool = lock.writeLock().tryLock(2, 10, TimeUnit.SECONDS);
            if (lock.lock()) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 60l);
                    for (FcssqLotterySg sg : list) {
                        FcssqLotterySgExample fcssqLotterySgExample = new FcssqLotterySgExample();
                        FcssqLotterySgExample.Criteria criteria = fcssqLotterySgExample.createCriteria();
                        criteria.andIssueEqualTo(sg.getIssue());
                        if (fcssqLotterySgMapper.selectOneByExample(fcssqLotterySgExample) == null) {
                            fcssqLotterySgMapper.insertSelective(sg);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("sameDataYuqiFCSSQ occur error:", e);
        } finally {
            lock.unlock();
        }

    }

    /**
     * ss彩 - 天津时时彩预期数据同步
     *
     * @param message 消息内容【期号】
     */
    @JmsListener(destination = ActiveMQConfig.TOPIC_SSC_TJ_YUQI_DATA, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgTJSscYuqiData(String message) throws Exception {
        logger.info("TJ时时彩 - 预期数据同步：  " + message);
        List<TjsscLotterySg> list = JSONObject.parseArray(message.substring(message.indexOf(":") + 1), TjsscLotterySg.class);
        // 获取唯一
        String key = ActiveMQConfig.TOPIC_SSC_TJ_YUQI_DATA + list.get(0).getIssue();
//        RReadWriteLock lock = redissonClient.getReadWriteLock(key+"lock");
        RedisLock lock = new RedisLock(key + "lock", 3 * 1000, 30 * 1000);
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
//            boolean bool = lock.writeLock().tryLock(2, 10, TimeUnit.SECONDS);
            if (lock.lock()) {
                boolean needUpdate = false;
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 60l);
                    for (TjsscLotterySg sg : list) {
                        TjsscLotterySgExample tjsscLotterySgExample = new TjsscLotterySgExample();
                        TjsscLotterySgExample.Criteria criteria = tjsscLotterySgExample.createCriteria();
                        criteria.andIssueEqualTo(sg.getIssue());
                        if (needUpdate == false && tjsscLotterySgMapper.selectOneByExample(tjsscLotterySgExample) == null) {
                            needUpdate = true;
                        }

                        sg.setIdealDate(DateUtils.str2date(sg.getIdealTime()));
                    }
                    if (needUpdate) {
                        tjsscLotterySgMapperExt.insertBatch(list);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("processMsgSscFiveYuqiData occur error:", e);
        } finally {
            lock.unlock();
        }

    }

    /**
     * ss彩 - 5分时时彩预期数据同步
     *
     * @param message 消息内容【期号】
     */
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_SSC_FIVE_YUQI_DATA, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgSscFiveYuqiData(String message) throws Exception {
        logger.info("5分时时彩 - 预期数据同步：  " + message);
        List<FivesscLotterySg> list = JSONObject.parseArray(message.substring(message.indexOf(":") + 1), FivesscLotterySg.class);
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_SSC_FIVE_YUQI_DATA + list.get(0).getIssue();
//        RReadWriteLock lock = redissonClient.getReadWriteLock(key+"lock");
        RedisLock lock = new RedisLock(key + "lock", 3 * 1000, 30 * 1000);
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
//            boolean bool = lock.writeLock().tryLock(2, 10, TimeUnit.SECONDS);
            if (lock.lock()) {
                boolean needUpdate = false;
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 60l);
                    for (FivesscLotterySg sg : list) {
                        FivesscLotterySgExample fivesscLotterySgExample = new FivesscLotterySgExample();
                        FivesscLotterySgExample.Criteria criteria = fivesscLotterySgExample.createCriteria();
                        criteria.andIssueEqualTo(sg.getIssue());
                        if (needUpdate == false && fivesscLotterySgMapper.selectOneByExample(fivesscLotterySgExample) == null) {
                            needUpdate = true;
                        }

                        sg.setNumber(StringUtils.isEmpty(sg.getNumber()) ? "" : sg.getNumber());
                        sg.setIdealDate(DateUtils.str2date(sg.getIdealTime()));
                    }
                    if (needUpdate) {
                        fivesscLotterySgMapperExt.insertBatch(list);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("processMsgSscFiveYuqiData occur error:", e);
        } finally {
            lock.unlock();
        }

    }

    /**
     * ss彩 - 德州时时彩预期数据同步
     *
     * @param message 消息内容【期号】
     */
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_SSC_JS_YUQI_DATA, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgSscJsYuqiData(String message) throws Exception {
        logger.info("德州时时彩 - 预期数据同步：  " + message);
        List<JssscLotterySg> list = JSONObject.parseArray(message.substring(message.indexOf(":") + 1), JssscLotterySg.class);
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_SSC_JS_YUQI_DATA + list.get(0).getIssue();
//        RReadWriteLock lock = redissonClient.getReadWriteLock(key+"lock");
        RedisLock lock = new RedisLock(key + "lock", 3 * 1000, 30 * 1000);
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
//            boolean bool = lock.writeLock().tryLock(2, 10, TimeUnit.SECONDS);
            if (lock.lock()) {
                boolean needUpdate = false;
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 60l);
                    for (JssscLotterySg sg : list) {
                        JssscLotterySgExample jssscLotterySgExample = new JssscLotterySgExample();
                        JssscLotterySgExample.Criteria criteria = jssscLotterySgExample.createCriteria();
                        criteria.andIssueEqualTo(sg.getIssue());
                        if (needUpdate == false && jssscLotterySgMapper.selectOneByExample(jssscLotterySgExample) == null) {
                            needUpdate = true;
                        }

                        sg.setNumber(StringUtils.isEmpty(sg.getNumber()) ? "" : sg.getNumber());
                        sg.setIdealDate(DateUtils.str2date(sg.getIdealTime()));
                    }
                    if (needUpdate) {
                        jssscLotterySgMapperExt.insertBatch(list);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("processMsgSscJsYuqiData occur error:", e);
        } finally {
            lock.unlock();
        }


    }

    /**
     * ss彩 - 十分时时彩预期数据同步
     *
     * @param message 消息内容【期号】
     */
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_SSC_TEN_YUQI_DATA, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgSscTenYuqiData(String message) throws Exception {
        logger.info("十分时时彩 - 预期数据同步：  " + message);

        List<TensscLotterySg> list = JSONObject.parseArray(message.substring(message.indexOf(":") + 1), TensscLotterySg.class);
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_SSC_TEN_YUQI_DATA + list.get(0).getIssue();
//        RReadWriteLock lock = redissonClient.getReadWriteLock(key+"lock");
        RedisLock lock = new RedisLock(key + "lock", 3 * 1000, 30 * 1000);
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
//            boolean bool = lock.writeLock().tryLock(2, 10, TimeUnit.SECONDS);
            if (lock.lock()) {
                boolean needUpdate = false;
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 60l);
                    for (TensscLotterySg sg : list) {
                        TensscLotterySgExample tensscLotterySgExample = new TensscLotterySgExample();
                        TensscLotterySgExample.Criteria criteria = tensscLotterySgExample.createCriteria();
                        criteria.andIssueEqualTo(sg.getIssue());
                        if (needUpdate == false && tensscLotterySgMapper.selectOneByExample(tensscLotterySgExample) == null) {
                            needUpdate = true;
                        }

                        sg.setNumber(StringUtils.isEmpty(sg.getNumber()) ? "" : sg.getNumber());
                        sg.setIdealDate(DateUtils.str2date(sg.getIdealTime()));
                    }
                    if (needUpdate) {
                        tensscLotterySgMapperExt.insertBatch(list);
                    }
                }
            }

        } catch (Exception e) {
            logger.error("processMsgSscTenYuqiData occur error:", e);
        } finally {
            lock.unlock();
        }

    }

    /**
     * ss彩 - 番摊德州时时彩预期数据同步
     *
     * @param message 消息内容【期号】
     */
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_SSC_JS_FT_YUQI_DATA, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgSscJsFtYuqiData(String message) throws Exception {
        logger.info("番摊德州时时彩 - 预期数据同步：  " + message);

        List<FtjssscLotterySg> list = JSONObject.parseArray(message.substring(message.indexOf(":") + 1), FtjssscLotterySg.class);
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_SSC_JS_FT_YUQI_DATA + list.get(0).getIssue();
//        RReadWriteLock lock = redissonClient.getReadWriteLock(key+"lock");
        RedisLock lock = new RedisLock(key + "lock", 3 * 1000, 30 * 1000);
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
//            boolean bool = lock.writeLock().tryLock(2, 10, TimeUnit.SECONDS);
            if (lock.lock()) {
                boolean needUpdate = false;
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 60l);
                    for (FtjssscLotterySg sg : list) {
                        FtjssscLotterySgExample ftjssscLotterySgExample = new FtjssscLotterySgExample();
                        FtjssscLotterySgExample.Criteria criteria = ftjssscLotterySgExample.createCriteria();
                        criteria.andIssueEqualTo(sg.getIssue());
                        if (needUpdate == false && ftjssscLotterySgMapper.selectOneByExample(ftjssscLotterySgExample) == null) {
                            needUpdate = true;
                        }

                        sg.setNumber(StringUtils.isEmpty(sg.getNumber()) ? "" : sg.getNumber());
                        sg.setFtNumber(StringUtils.isEmpty(sg.getFtNumber()) ? "" : sg.getFtNumber());
//                        sg.setIdealDate(DateUtil.str2date(sg.getIdealTime(),DateUtil.FORMAT_YYYY_MM_DD_HH_MM_SS));
                    }
                    if (needUpdate) {
                        ftJssscLotterySgMapperExt.insertBatch(list);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("processMsgSscJsFtYuqiData occur error:", e);
        } finally {
            lock.unlock();
        }

    }

}
