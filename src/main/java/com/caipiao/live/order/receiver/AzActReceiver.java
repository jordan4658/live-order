package com.caipiao.live.order.receiver;

import com.alibaba.fastjson.JSONObject;
import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.util.redis.RedisBusinessUtil;
import com.caipiao.live.order.config.ActiveMQConfig;
import com.caipiao.live.order.service.bet.BetActAzService;
import com.caipiao.live.order.service.result.AzxlLotterySgWriteService;
import com.caipiao.live.common.constant.RedisKeys;
import com.caipiao.live.common.enums.lottery.CaipiaoTypeEnum;
import com.caipiao.live.common.enums.lottery.LotteryTableNameEnum;
import com.caipiao.live.common.mybatis.entity.AusactLotterySg;
import com.caipiao.live.common.mybatis.entity.AusactLotterySgExample;
import com.caipiao.live.common.mybatis.mapper.AusactLotterySgMapper;
import com.caipiao.live.common.mybatis.mapperext.sg.AusactLotterySgMapperExt;
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
public class AzActReceiver {
    private static final Logger logger = LoggerFactory.getLogger(AzActReceiver.class);

    @Autowired
    private AusactLotterySgMapper ausactLotterySgMapper;
    @Autowired
    private AusactLotterySgMapperExt ausactLotterySgMapperExt;
    @Autowired
    private AzxlLotterySgWriteService cptAzxlLotterySgWriteService;
    @Autowired
    private BetActAzService betActAzService;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private BasicRedisClient basicRedisClient;
    @Autowired
    private JmsMessagingTemplate jmsMessagingTemplate;


    /**
     * ??????Act- ???????????????
     *
     * @param message ????????????????????????
     */
//    @RabbitListener(queues = RabbitConfig.QUEUE_AUS_ACT)
    @JmsListener(destination = ActiveMQConfig.TOPIC_AUS_ACT_NAME, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgAzAct(String message) throws Exception {
        logger.info("?????????Act????????????????????????????????????  " + message);
        // ??????????????????
        String[] str = message.split(":");
        // ????????????
        String key = ActiveMQConfig.TOPIC_AUS_ACT_NAME + str[1];
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // ?????????????????????1s???????????????10S[????????????]????????????????????????????????????????????????????????????????????????????????????
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, 1, 60l);
                    //???????????????????????????
                    AusactLotterySg updateSg = JSONObject.parseObject(str[3].replace("$", ":"), AusactLotterySg.class);
                    int n = ausactLotterySgMapperExt.updateByIssue(updateSg);
                    if (n > 0) {
                        //????????????????????????
                        RedisBusinessUtil.updateLsSgCache(Constants.LOTTERY_AUSACT, RedisKeys.AUSACT_SG_HS_LIST, updateSg);
                    }

                    cptAzxlLotterySgWriteService.cacheIssueResultForAusact(str[1], str[2]);
                    //??????act??????
                    betActAzService.countAzAct(str[1], str[2], Integer.parseInt(CaipiaoTypeEnum.AUSACT.getTagType()));

                    //????????? ??????1?????????????????????
                    AusactLotterySgExample ausactLotterySgExample = new AusactLotterySgExample();
                    AusactLotterySgExample.Criteria criteria = ausactLotterySgExample.createCriteria();
                    criteria.andOpenStatusEqualTo(Constants.WAIT);
                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.MINUTE, -2);  //????????????????????????
                    criteria.andIdealTimeLessThan(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                    calendar.add(Calendar.DAY_OF_MONTH, -1);
                    criteria.andIdealTimeGreaterThanOrEqualTo(DateUtils.formatDate(calendar.getTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
                    ausactLotterySgExample.setOrderByClause("ideal_time desc");
                    ausactLotterySgExample.setLimit(15);
                    ausactLotterySgExample.setOffset(0);
                    List<AusactLotterySg> ausactLotterySgList = ausactLotterySgMapper.selectByExample(ausactLotterySgExample);
                    if (ausactLotterySgList.size() > 0) {
                        String issues = "";
                        for (AusactLotterySg sg : ausactLotterySgList) {
                            issues = issues + sg.getIssue() + ",";
                        }
                        issues = issues.substring(0, issues.length() - 1);
                        Destination destination = new ActiveMQQueue(ActiveMQConfig.TOPIC_MISSING_LOTTERY_SG);
                        jmsMessagingTemplate.convertAndSend(destination, LotteryTableNameEnum.AUSACT.getLotteryId() + "#" + issues);
                        logger.info("??????????????????{}???{}", LotteryTableNameEnum.AUSACT.getLotteryId(), issues);
                    }


                }
            } else {
                logger.info("??????Act?????????????????? ????????????");
            }
        } catch (Exception e) {
            logger.error("??????Act????????????", e);
        } finally {
            lock.writeLock().unlock();
        }

    }

    @JmsListener(destination = ActiveMQConfig.TOPIC_AUS_ACT_YUQI_DATA, containerFactory = "jmsListenerContainerTopicDurable")
    public void sameDataYuqiAusAct(String message) throws Exception {
        logger.info("?????????Act????????????????????????  " + message);
        List<AusactLotterySg> list = JSONObject.parseArray(message.substring(message.indexOf(":") + 1), AusactLotterySg.class);
        // ????????????
        String key = ActiveMQConfig.TOPIC_AUS_ACT_YUQI_DATA + list.get(0).getIssue();
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // ?????????????????????100s???????????????10S[????????????]????????????????????????????????????????????????????????????????????????????????????
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    for (AusactLotterySg sg : list) {
                        AusactLotterySgExample ausactLotterySgExample = new AusactLotterySgExample();
                        AusactLotterySgExample.Criteria criteria = ausactLotterySgExample.createCriteria();
                        criteria.andIssueEqualTo(sg.getIssue());
                        if (ausactLotterySgMapper.selectOneByExample(ausactLotterySgExample) == null) {
                            ausactLotterySgMapper.insertSelective(sg);
                        }

                    }
                }
            } else {
                logger.info("??????Act?????????????????? ????????????");
            }
        } catch (Exception e) {
            logger.error("sameDataYuqiAusAct occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }
    }


}

