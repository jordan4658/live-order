package com.caipiao.live.order.receiver;

import com.alibaba.fastjson.JSONObject;
import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.util.redis.RedisBusinessUtil;
import com.caipiao.live.order.config.ActiveMQConfig;
import com.caipiao.live.order.service.bet.BetF1AzService;
import com.caipiao.live.order.service.result.AzxlLotterySgWriteService;
import com.caipiao.live.common.constant.RedisKeys;
import com.caipiao.live.common.enums.lottery.CaipiaoTypeEnum;
import com.caipiao.live.common.enums.lottery.LotteryTableNameEnum;
import com.caipiao.live.common.mybatis.entity.AuspksLotterySg;
import com.caipiao.live.common.mybatis.entity.AuspksLotterySgExample;
import com.caipiao.live.common.mybatis.mapper.AuspksLotterySgMapper;
import com.caipiao.live.common.mybatis.mapperext.sg.AuspksLotterySgMapperExt;
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
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author lzy
 * @create 2018-09-17 20:07
 **/
@Component
public class AzF1Receiver {
    private static final Logger logger = LoggerFactory.getLogger(AzF1Receiver.class);

    @Autowired
    private BetF1AzService betF1AzService;
    @Autowired
    private AzxlLotterySgWriteService cptAzxlLotterySgWriteService;
    @Autowired
    private AuspksLotterySgMapper auspksLotterySgMapper;
    @Autowired
    private AuspksLotterySgMapperExt auspksLotterySgMapperExt;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private BasicRedisClient basicRedisClient;
    @Autowired
    private JmsMessagingTemplate jmsMessagingTemplate;


    /**
     * 澳洲F1- 计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.QUEUE_AUS_F1)
    @JmsListener(destination = ActiveMQConfig.TOPIC_AUS_F1_NAME, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgAzNn(String message) throws Exception {
        logger.info("【澳洲F1】结算期号：  " + message);
        // 拆分消息内容
        String[] str = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.TOPIC_AUS_F1_NAME + str[1];
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(1, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key + "F1") == null) {
                    //第一步同步中奖结果
                    AuspksLotterySg updateSg = JSONObject.parseObject(str[3].replace("$", ":"), AuspksLotterySg.class);
                    int n = auspksLotterySgMapperExt.updateByIssue(updateSg);
                    if (n > 0) {
                        //更新历史赛果缓存
                        RedisBusinessUtil.updateLsSgCache(Constants.LOTTERY_AUSPKS, RedisKeys.AUSPKS_SG_HS_LIST, updateSg);
                    }

                    // 缓存赛果
                    cptAzxlLotterySgWriteService.cacheIssueResultForAuspks(str[1], str[2]);
                    // 结算【澳洲F1】
                    betF1AzService.countAzF1(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.AUSPKS.getTagType()));
                    basicRedisClient.set(key + "F1", "1", 60l);

                    //第三步 最近1天未开奖的数据
                    AuspksLotterySgExample auspksLotterySgExample = new AuspksLotterySgExample();
                    AuspksLotterySgExample.Criteria criteria = auspksLotterySgExample.createCriteria();
                    criteria.andOpenStatusEqualTo(Constants.WAIT);
                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.MINUTE, -2);  //过滤掉最新一期，
                    criteria.andIdealTimeLessThan(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                    calendar.add(Calendar.DAY_OF_MONTH, -1);
                    criteria.andIdealTimeGreaterThanOrEqualTo(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                    auspksLotterySgExample.setOrderByClause("ideal_time desc");
                    auspksLotterySgExample.setLimit(15);
                    auspksLotterySgExample.setOffset(0);
                    List<AuspksLotterySg> auspksLotterySgList = auspksLotterySgMapper.selectByExample(auspksLotterySgExample);
                    if (auspksLotterySgList.size() > 0) {
                        String issues = "";
                        for (AuspksLotterySg sg : auspksLotterySgList) {
                            issues = issues + sg.getIssue() + ",";
                        }
                        issues = issues.substring(0, issues.length() - 1);
                        Destination destination = new ActiveMQQueue(ActiveMQConfig.TOPIC_MISSING_LOTTERY_SG);
                        jmsMessagingTemplate.convertAndSend(destination, LotteryTableNameEnum.AUSPKS.getLotteryId() + "#" + issues);
                    }


                }
            }

        } catch (Exception e) {
            logger.error("processMsgAzNn occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }

    }

    @JmsListener(destination = ActiveMQConfig.TOPIC_AUS_PKS_YUQI_DATA, containerFactory = "jmsListenerContainerTopicDurable")
    public void sameDataYuqiAusPks(String message) throws Exception {
        logger.info("【澳洲F1】预期数据同步：  " + message);
        List<AuspksLotterySg> list = JSONObject.parseArray(message.substring(message.indexOf(":") + 1), AuspksLotterySg.class);
        // 获取唯一
        String key = ActiveMQConfig.TOPIC_AUS_PKS_YUQI_DATA + list.get(0).getIssue();
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    for (AuspksLotterySg sg : list) {
                        AuspksLotterySgExample auspksLotterySgExample = new AuspksLotterySgExample();
                        AuspksLotterySgExample.Criteria criteria = auspksLotterySgExample.createCriteria();
                        criteria.andIssueEqualTo(sg.getIssue());
                        if (auspksLotterySgMapper.selectOneByExample(auspksLotterySgExample) == null) {
                            auspksLotterySgMapper.insertSelective(sg);
                        }

                    }
                }
            }
        } catch (Exception e) {
            logger.error("sameDataYuqiAusPks occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

}

