package com.caipiao.live.order.receiver;

import com.alibaba.fastjson.JSONObject;
import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.util.redis.RedisBusinessUtil;
import com.caipiao.live.order.config.ActiveMQConfig;
import com.caipiao.live.order.service.bet.BetXyftService;
import com.caipiao.live.order.service.result.XyftLotterySgWriteService;
import com.caipiao.live.common.constant.RedisKeys;
import com.caipiao.live.common.enums.lottery.LotteryTableNameEnum;
import com.caipiao.live.common.mybatis.entity.FtxyftLotterySg;
import com.caipiao.live.common.mybatis.entity.FtxyftLotterySgExample;
import com.caipiao.live.common.mybatis.entity.XyftLotterySg;
import com.caipiao.live.common.mybatis.entity.XyftLotterySgExample;
import com.caipiao.live.common.mybatis.mapper.FtxyftLotterySgMapper;
import com.caipiao.live.common.mybatis.mapper.XyftLotterySgMapper;
import com.caipiao.live.common.mybatis.mapperext.sg.XyftLotterySgMapperExt;
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
public class XyftReceiver {
    private static final Logger logger = LoggerFactory.getLogger(XyftReceiver.class);

    @Autowired
    private BetXyftService betXyftService;
    @Autowired
    private XyftLotterySgWriteService xyftLotterySgWriteService;
    @Autowired
    private XyftLotterySgMapper xyftLotterySgMapper;
    @Autowired
    private XyftLotterySgMapperExt xyftLotterySgMapperExt;
    @Autowired
    private FtxyftLotterySgMapper ftxyftLotterySgMapper;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private BasicRedisClient basicRedisClient;
    @Autowired
    private JmsMessagingTemplate jmsMessagingTemplate;

    /**
     * 幸运飞艇- 【两面】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_XYFT_LM)
    @JmsListener(destination = ActiveMQConfig.TOPIC_XYFT_LM, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgXyftLm(String message) throws Exception {
        logger.info("【幸运飞艇-两面】结算期号：  " + message);
        String[] str = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.TOPIC_XYFT_LM + str[1];
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    if (str.length == 4) {   //表面需要同步数据
                        //第一步同步中奖结果
                        XyftLotterySg updateSg = JSONObject.parseObject(str[3].replace("$", ":"), XyftLotterySg.class);
                        int boolUpdate = xyftLotterySgMapperExt.updateByIssue(updateSg);
                        if (boolUpdate == 0) {  //说明这一天的期号数据可能不全，则做一次检查，如果真没有，则从task_server同步当天所有期号数据过来
                            XyftLotterySgExample sgExample = new XyftLotterySgExample();
                            XyftLotterySgExample.Criteria criteria = sgExample.createCriteria();
                            Calendar calendar = Calendar.getInstance();
                            criteria.andIdealTimeGreaterThan(DateUtils.formatDate(new Date(), DateUtils.FORMAT_YYYY_MM_DD));
                            calendar.add(Calendar.DAY_OF_MONTH, 1);
                            criteria.andIdealTimeLessThan(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD));
                            int afterCount = xyftLotterySgMapper.countByExample(sgExample);
                            if (afterCount < 180) {
                                //则发送消息
                                logger.info("同步预期数据发送通知：xyftYuqiToday");
                                Destination destination = new ActiveMQQueue(ActiveMQConfig.TOPIC_YUQI_TODAYT);
                                jmsMessagingTemplate.convertAndSend(destination, "xyftYuqiToday");
                            }
                        } else {
                            //更新历史赛果缓存
                            RedisBusinessUtil.updateLsSgCache(Constants.LOTTERY_XYFT, RedisKeys.XYFT_SG_HS_LIST, updateSg);
                        }

                    }
                    // 结算【幸运飞艇-两面】
                    betXyftService.clearingXyftLm(str[1], str[2]);

                    //第三步 最近1天未开奖的数据
                    XyftLotterySgExample xyftLotterySgExample = new XyftLotterySgExample();
                    XyftLotterySgExample.Criteria criteria = xyftLotterySgExample.createCriteria();
                    criteria.andOpenStatusEqualTo(Constants.WAIT);
                    Calendar calendar = Calendar.getInstance();
                    criteria.andIdealTimeLessThan(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                    calendar.add(Calendar.DAY_OF_MONTH, -1);
                    criteria.andIdealTimeGreaterThanOrEqualTo(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                    xyftLotterySgExample.setOrderByClause("ideal_time desc");
                    List<XyftLotterySg> xyftLotterySgList = xyftLotterySgMapper.selectByExample(xyftLotterySgExample);
                    if (xyftLotterySgList.size() > 0) {
                        String issues = "";
                        for (XyftLotterySg sg : xyftLotterySgList) {
                            issues = issues + sg.getIssue() + ",";
                        }
                        issues = issues.substring(0, issues.length() - 1);
                        Destination destination = new ActiveMQQueue(ActiveMQConfig.TOPIC_MISSING_LOTTERY_SG);
                        jmsMessagingTemplate.convertAndSend(destination, LotteryTableNameEnum.XYFEIT.getLotteryId() + "#" + issues);
                        logger.info("发送缺奖数据{}，{}", LotteryTableNameEnum.XYFEIT.getLotteryId(), issues);
                    }
                    xyftLotterySgWriteService.cacheIssueResultForXyft(str[1], str[2]);//幸运飞艇
                }
            }

        } catch (Exception e) {
            logger.error("processMsgXyftLm occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * 幸运飞艇- 【猜名次猜前几】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_XYFT_CMC_CQJ)
    @JmsListener(destination = ActiveMQConfig.TOPIC_XYFT_CMC_CQJ, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgXyftCmcCqj(String message) throws Exception {
        logger.info("【幸运飞艇-猜名次猜前几】结算期号：  " + message);
        String[] str = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.TOPIC_XYFT_CMC_CQJ + str[1];
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    // 结算【幸运飞艇-猜名次猜前几】
                    betXyftService.clearingXyftCmcCqj(str[1], str[2]);
                }
            }

        } catch (Exception e) {
            logger.error("processMsgXyftCmcCqj occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * 幸运飞艇- 【单式猜前几】计算结果
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_XYFT_DS_CQJ)
//    public void processMsgXyftDsCqj(String message) throws Exception {
//        logger.info("【幸运飞艇-单式猜前几】结算期号：  "+message);
//        String[] str = message.split(":");
//
//        // 结算【幸运飞艇-单式猜前几】
//        betXyftService.clearingXyftDsCqj(str[1], str[2]);
//    }

    /**
     * 幸运飞艇- 【定位胆】计算结果
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_XYFT_DWD)
//    public void processMsgXyftDwd(String message) throws Exception {
//        logger.info("【幸运飞艇-定位胆】结算期号：  "+message);
//        String[] str = message.split(":");
//
//        // 结算【幸运飞艇-定位胆】
//        betXyftService.clearingXyftDwd(str[1], str[2]);
//    }

    /**
     * 幸运飞艇- 【冠亚和】计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.TOPIC_XYFT_GYH)
    @JmsListener(destination = ActiveMQConfig.TOPIC_XYFT_GYH, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgXyftGyh(String message) throws Exception {
        logger.info("【幸运飞艇-冠亚和】结算期号：  " + message);
        String[] str = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.TOPIC_XYFT_GYH + str[1];
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(1, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.hGet("", key) == null) {
                    // 结算【幸运飞艇-冠亚和】
                    betXyftService.clearingXyftGyh(str[1], str[2]);
                    basicRedisClient.set(key, "1", 50l);
                }
            }
        } catch (Exception e) {
            logger.error("processMsgXyftGyh occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @JmsListener(destination = ActiveMQConfig.TOPIC_XYFT_YUQI_DATA, containerFactory = "jmsListenerContainerTopicDurable")
    public void sameDataYuqiXyft(String message) throws Exception {
        logger.info("【幸运飞艇】预期数据同步：  " + message);
        List<XyftLotterySg> list = JSONObject.parseArray(message.substring(message.indexOf(":") + 1), XyftLotterySg.class);

        // 获取唯一
        String key = ActiveMQConfig.TOPIC_XYFT_YUQI_DATA + list.get(0).getIssue();
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    for (XyftLotterySg sg : list) {
                        XyftLotterySgExample xyftLotterySgExample = new XyftLotterySgExample();
                        XyftLotterySgExample.Criteria criteria = xyftLotterySgExample.createCriteria();
                        criteria.andIssueEqualTo(sg.getIssue());
                        if (xyftLotterySgMapper.selectOneByExample(xyftLotterySgExample) == null) {
                            xyftLotterySgMapper.insertSelective(sg);
                        }

                    }
                }
            }
        } catch (Exception e) {
            logger.error("sameDataYuqiXyft occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }

    }

    @JmsListener(destination = ActiveMQConfig.TOPIC_FT_XYFT_YUQI_DATA, containerFactory = "jmsListenerContainerTopicDurable")
    public void sameDataYuqiFtXyft(String message) throws Exception {
        logger.info("【番摊幸运飞艇】预期数据同步：  " + message);
        List<FtxyftLotterySg> list = JSONObject.parseArray(message.substring(message.indexOf(":") + 1), FtxyftLotterySg.class);
        // 获取唯一
        String key = ActiveMQConfig.TOPIC_FT_XYFT_YUQI_DATA + list.get(0).getIssue();
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    for (FtxyftLotterySg sg : list) {
                        FtxyftLotterySgExample ftxyftLotterySgExample = new FtxyftLotterySgExample();
                        FtxyftLotterySgExample.Criteria criteria = ftxyftLotterySgExample.createCriteria();
                        criteria.andIssueEqualTo(sg.getIssue());
                        if (ftxyftLotterySgMapper.selectOneByExample(ftxyftLotterySgExample) == null) {
                            ftxyftLotterySgMapper.insertSelective(sg);
                        }

                    }
                }
            }

        } catch (Exception e) {
            logger.error("sameDataYuqiFtXyft occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }
    }


}
