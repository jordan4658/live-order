package com.caipiao.live.order.receiver;

import com.alibaba.fastjson.JSONObject;
import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.util.redis.RedisBusinessUtil;
import com.caipiao.live.order.config.ActiveMQConfig;
import com.caipiao.live.order.service.bet.BetSscAzService;
import com.caipiao.live.common.constant.RedisKeys;
import com.caipiao.live.common.enums.lottery.CaipiaoTypeEnum;
import com.caipiao.live.common.enums.lottery.LotteryTableNameEnum;
import com.caipiao.live.common.mybatis.entity.AussscLotterySg;
import com.caipiao.live.common.mybatis.entity.AussscLotterySgExample;
import com.caipiao.live.common.mybatis.mapper.AussscLotterySgMapper;
import com.caipiao.live.common.mybatis.mapperext.sg.AussscLotterySgMapperExt;
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
public class AzSscReceiver {
    private static final Logger logger = LoggerFactory.getLogger(AzSscReceiver.class);

    @Autowired
    private BetSscAzService betSscAzService;
    @Autowired
    private AussscLotterySgMapper aussscLotterySgMapper;
    @Autowired
    private AussscLotterySgMapperExt aussscLotterySgMapperExt;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private BasicRedisClient basicRedisClient;
    @Autowired
    private JmsMessagingTemplate jmsMessagingTemplate;
    /**
     * 澳洲时时彩- 计算结果
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.QUEUE_AUS_ACT)
    @JmsListener(destination = ActiveMQConfig.TOPIC_AUS_SSC_NAME, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgAzNn(String message) throws Exception {
        logger.info("【澳洲时时彩】结算期号：  " + message);
        // 拆分消息内容
        String[] str = message.split(":");

        // 获取唯一
        String key = str[1];
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    //第一步同步中奖结果
                    AussscLotterySg updateSg = JSONObject.parseObject(str[3].replace("$", ":"), AussscLotterySg.class);
                    int n = aussscLotterySgMapperExt.updateByIssue(updateSg);
                    if (n > 0) {
                        //更新历史赛果缓存
                        RedisBusinessUtil.updateLsSgCache(Constants.LOTTERY_AUSSSC, RedisKeys.AUSSSC_SG_HS_LIST, updateSg);
                    }

                    betSscAzService.cacheIssueResultForAusssc(str[1], str[2]);

                    // 结算【澳洲时时彩】
                    betSscAzService.countAzSsc(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.AUSSSC.getTagType()));

                    //第三步 最近1天未开奖的数据
                    AussscLotterySgExample aussscLotterySgExample = new AussscLotterySgExample();
                    AussscLotterySgExample.Criteria criteria = aussscLotterySgExample.createCriteria();
                    criteria.andOpenStatusEqualTo(Constants.WAIT);
                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.MINUTE, -2);  //过滤掉最新一期，
                    criteria.andIdealTimeLessThan(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                    calendar.add(Calendar.DAY_OF_MONTH, -1);
                    criteria.andIdealTimeGreaterThanOrEqualTo(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                    aussscLotterySgExample.setOrderByClause("ideal_time desc");
                    aussscLotterySgExample.setLimit(15);
                    aussscLotterySgExample.setOffset(0);
                    List<AussscLotterySg> aussscLotterySgList = aussscLotterySgMapper.selectByExample(aussscLotterySgExample);
                    if (aussscLotterySgList.size() > 0) {
                        String issues = "";
                        for (AussscLotterySg sg : aussscLotterySgList) {
                            issues = issues + sg.getIssue() + ",";
                        }
                        issues = issues.substring(0, issues.length() - 1);
                        Destination destination = new ActiveMQQueue(ActiveMQConfig.TOPIC_MISSING_LOTTERY_SG);
                        jmsMessagingTemplate.convertAndSend(destination, LotteryTableNameEnum.AUSSSC.getLotteryId() + "#" + issues);
                        logger.info("发送缺奖数据{}，{}", LotteryTableNameEnum.AUSSSC.getLotteryId(), issues);
                    }

                }
            }
        } catch (Exception e) {
            logger.error("processMsgAzNn occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }

    }

    @JmsListener(destination = ActiveMQConfig.TOPIC_AUS_SSC_YUQI_DATA, containerFactory = "jmsListenerContainerTopicDurable")
    public void sameDataYuqiAusAct(String message) throws Exception {
        logger.info("【澳洲时时彩】预期数据同步：  " + message);

        List<AussscLotterySg> list = JSONObject.parseArray(message.substring(message.indexOf(":") + 1), AussscLotterySg.class);
        // 获取唯一
        String key = list.get(0).getIssue();
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    for (AussscLotterySg sg : list) {
                        AussscLotterySgExample aussscLotterySgExample = new AussscLotterySgExample();
                        AussscLotterySgExample.Criteria criteria = aussscLotterySgExample.createCriteria();
                        criteria.andIssueEqualTo(sg.getIssue());
                        if (aussscLotterySgMapper.selectOneByExample(aussscLotterySgExample) == null) {
                            aussscLotterySgMapper.insertSelective(sg);
                        }

                    }
                }
            }
        } catch (Exception e) {
            logger.error("sameDataYuqiAusAct occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }
    }


}

