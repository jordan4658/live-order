package com.caipiao.live.order.receivernew;

import com.alibaba.fastjson.JSONObject;
import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.util.redis.RedisBusinessUtil;
import com.caipiao.live.order.config.ActiveMQConfig;
import com.caipiao.live.order.service.bet.BetNewLhcService;
import com.caipiao.live.order.service.result.XjplhcLotterySgWriteService;
import com.caipiao.live.common.constant.RedisKeys;
import com.caipiao.live.common.enums.lottery.CaipiaoTypeEnum;
import com.caipiao.live.common.enums.lottery.LotteryTableNameEnum;
import com.caipiao.live.common.mybatis.entity.XjplhcLotterySg;
import com.caipiao.live.common.mybatis.entity.XjplhcLotterySgExample;
import com.caipiao.live.common.mybatis.mapper.XjplhcLotterySgMapper;
import com.caipiao.live.common.mybatis.mapperext.sg.XjplhcLotterySgMapperExt;
import com.caipiao.live.common.util.DateUtils;
import com.caipiao.live.common.util.StringUtils;
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
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Date:Created in 19:452019/12/30
 * @Descriotion
 * @Author
 **/
@Component
public class XjplhcReceiver {
    private static final Logger logger = LoggerFactory.getLogger(XjplhcReceiver.class);
    @Autowired
    private BetNewLhcService betNewLhcService;
    @Autowired
    private XjplhcLotterySgMapper xjplhcLotterySgMapper;
    @Autowired
    private XjplhcLotterySgMapperExt xjplhcLotterySgMapperExt;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private BasicRedisClient basicRedisClient;
    @Autowired
    private JmsMessagingTemplate jmsMessagingTemplate;
    @Autowired
    private XjplhcLotterySgWriteService xjplhcLotterySgWriteService;

    @JmsListener(destination = ActiveMQConfig.LIVE_TOPIC_XJPLHC_TM_ZT_LX, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgXJPLhcZmZtLm(String message) throws Exception {
        logger.info("新加坡六合彩 - 【特码,正特,六肖,正码1-6】结算期号：mq:{}message:{}  " ,ActiveMQConfig.LIVE_TOPIC_XJPLHC_TM_ZT_LX, message);
        String[] num = message.split(":");
        // 获取期号
        String issue = num[1];
        String key = ActiveMQConfig.LIVE_TOPIC_XJPLHC_TM_ZT_LX + num[1];
        // 【分布式读写锁】
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50L);
                    //第一步同步中奖结果
                    XjplhcLotterySg updateSg = JSONObject.parseObject(num[3].replace("$", ":"), XjplhcLotterySg.class);
                    int boolUpdate = xjplhcLotterySgMapperExt.updateByIssue(updateSg);
                    if (boolUpdate == 0) {  //说明这一天的期号数据可能不全，则做一次检查，如果真没有，则从task_server同步当天所有期号数据过来
                        XjplhcLotterySgExample sgExample = new XjplhcLotterySgExample();
                        XjplhcLotterySgExample.Criteria criteria = sgExample.createCriteria();
                        LocalDate today = LocalDate.now();//当天日期
                        LocalDate tomrrow = today.plusDays(1L);//第二天
                        criteria.andIdealTimeGreaterThanOrEqualTo(DateUtils.getLocalDateToDate(today));
                        criteria.andIdealTimeLessThan(DateUtils.getLocalDateToDate(tomrrow));
                        int afterCount = xjplhcLotterySgMapper.countByExample(sgExample);
                        if (afterCount < 288) {
                            //则发送消息
                            logger.info("同步预期数据发送通知：xjpLhcYuqiToday  mq:{}",ActiveMQConfig.LIVE_TOPIC_YUQI_TODAYT);
                            Destination destination = new ActiveMQTopic(ActiveMQConfig.LIVE_TOPIC_YUQI_TODAYT);
                            jmsMessagingTemplate.convertAndSend(destination, "xjpLhcYuqiToday");
                        }
                    } else {
                        //更新历史赛果缓存
                        RedisBusinessUtil.updateLsSgCache(Constants.LOTTERY_XJPLHC, RedisKeys.XJPLHC_SG_HS_LIST, updateSg);
                    }

                    // 缓存新加坡六合彩开奖结果
                    xjplhcLotterySgWriteService.cacheIssueResultForQnelhc(issue, num[2]);
                    // 结算六合彩- 【特码,正特,六肖,正码1-6】

                    betNewLhcService.clearingLhcTeMaA(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                    betNewLhcService.clearingLhcZhengTe(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                    betNewLhcService.clearingLhcZhengMaOneToSix(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                    betNewLhcService.clearingLhcLiuXiao(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);

                    //第三步 最近1天未开奖的数据
                    XjplhcLotterySgExample xjplhcLotterySgExample = new XjplhcLotterySgExample();
                    XjplhcLotterySgExample.Criteria criteria = xjplhcLotterySgExample.createCriteria();
                    criteria.andOpenStatusEqualTo(Constants.WAIT);
                    LocalDateTime todayTime = LocalDateTime.now();
                    LocalDateTime yesterdayTime = todayTime.minusDays(1);
                    criteria.andIdealTimeLessThan(DateUtils.getLocalDateTimeToDate(todayTime));
                    criteria.andIdealTimeGreaterThanOrEqualTo(DateUtils.getLocalDateTimeToDate(yesterdayTime));
                    xjplhcLotterySgExample.setOrderByClause("ideal_time desc");
                    xjplhcLotterySgExample.setLimit(15);
                    xjplhcLotterySgExample.setOffset(0);
                    List<XjplhcLotterySg> xjplhcLotterySgList = xjplhcLotterySgMapper.selectByExample(xjplhcLotterySgExample);
                    if (xjplhcLotterySgList.size() > 0) {
                        String issues = "";
                        for (XjplhcLotterySg sg : xjplhcLotterySgList) {
                            issues = issues + sg.getIssue() + ",";
                        }
                        issues = issues.substring(0, issues.length() - 1);
                        Destination destination = new ActiveMQQueue(ActiveMQConfig.LIVE_TOPIC_MISSING_LOTTERY_SG);
                        logger.info("missing发送通知：xjpLhcYuqiToday  mq:{}",ActiveMQConfig.LIVE_TOPIC_MISSING_LOTTERY_SG);
                        jmsMessagingTemplate.convertAndSend(destination, LotteryTableNameEnum.XJPLHC.getLotteryId() + "#" + issues);
                        logger.info("发送缺奖数据{}，{}", LotteryTableNameEnum.XJPLHC.getLotteryId(), issues);
                    }

                }
            }

        } catch (Exception e) {
            logger.error("processMsgXJPLhcZmZtLm occur error:", e);
        } finally {
            lock.writeLock().unlock();
        }

    }

    @JmsListener(destination = ActiveMQConfig.LIVE_TOPIC_XJPLHC_ZM_BB_WS, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgXJPLhcZmBbWs(String message) throws Exception {
        logger.info("六合彩 - 【正码,半波,尾数】结算期号：  " + message);
        String[] num = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.LIVE_TOPIC_XJPLHC_ZM_BB_WS + num[1];
//        String key = num[1];
        // 【分布式读写锁】
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50L);
                    // 结算六合彩- 【正码,半波,尾数】
                    betNewLhcService.clearingLhcZhengMaA(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                    betNewLhcService.clearingLhcBanBo(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                    betNewLhcService.clearingLhcWs(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                }
            }
        } catch (Exception e) {
            logger.error("processMsgONELhcZmBbWs occur error:", e);
        } finally {
            lock.writeLock().unlock();
        }

    }

    @JmsListener(destination = ActiveMQConfig.LIVE_TOPIC_XJPLHC_LM_LX_LW, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgXJPLhcLmLxLw(String message) throws Exception {
        logger.info("六合彩 - 【连码,连肖,连尾】结算期号：  " + message);
        String[] num = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.LIVE_TOPIC_XJPLHC_LM_LX_LW + num[1];
//        String key = num[1];
        // 【分布式读写锁】
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            // 判断是否获取到锁
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50L);
                    // 结算六合彩- 【连码,连肖,连尾】
                    betNewLhcService.clearingLhcLianMa(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                    betNewLhcService.clearingLhcLianXiao(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                    betNewLhcService.clearingLhcLianWei(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                }
            }

        } catch (Exception e) {
            logger.error("processMsgONELhcLmLxLw occur error:", e);
        } finally {
            lock.writeLock().unlock();
        }

    }

    @JmsListener(destination = ActiveMQConfig.TOPIC_XJPLHC_PT_TX, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgXJPLhcPtTx(String message) throws Exception {
        logger.info("六合彩 - 【平特,特肖】结算期号：  " + message);
        String[] num = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.LIVE_TOPIC_XJPLHC_PT_TX + num[1];
//        String key = num[1];
        // 【分布式读写锁】
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50L);
                    // 结算六合彩- 【平特,特肖】
                    betNewLhcService.clearingLhcPtPt(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                    betNewLhcService.clearingLhcTxTx(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                }
            }
        } catch (Exception e) {
            logger.error("processMsgONELhcPtTx occur error:", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @JmsListener(destination = ActiveMQConfig.LIVE_TOPIC_XJPLHC_BZ_LH_WX, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgXJPLhcBzLhWx(String message) throws Exception {
        logger.info("六合彩 - 【不中,1-6龙虎,五行】结算期号：  " + message);
        String[] num = message.split(":");
        // 获取唯一
        String key = ActiveMQConfig.LIVE_TOPIC_XJPLHC_BZ_LH_WX + num[1];
        // 【分布式读写锁】
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            // 判断是否获取到锁
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50L);
                    // 结算六合彩- 【不中,1-6龙虎,五行】
                    betNewLhcService.clearingLhcNoOpen(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                    betNewLhcService.clearingLhcOneSixLh(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                    betNewLhcService.clearingLhcWuxing(num[1], num[2], Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                }
            }

        } catch (Exception e) {
            logger.error("processMsgONELhcBzLhWx occur error:", e);
        } finally {
            lock.writeLock().unlock();
        }

    }

    @JmsListener(destination = ActiveMQConfig.LIVE_TOPIC_XJPLHC_YUQI_DATA, containerFactory = "jmsListenerContainerTopicDurable")
    public void sameDataYuqiXJPLhc(String message) throws Exception {
        logger.info("【1分六合彩】预期数据同步：mq：{}，{} " ,ActiveMQConfig.LIVE_TOPIC_XJPLHC_YUQI_DATA, message);
        List<XjplhcLotterySg> list = JSONObject.parseArray(message.substring(message.indexOf(":") + 1), XjplhcLotterySg.class);
        // 获取唯一
        String key = ActiveMQConfig.LIVE_TOPIC_XJPLHC_YUQI_DATA + list.get(0).getIssue();
        // 【分布式读写锁】
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 20, TimeUnit.SECONDS);
            if (bool) {
                boolean needUpdate = false;
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50L);
                    for (XjplhcLotterySg sg : list) {
                        XjplhcLotterySgExample xjplhcLotterySgExample = new XjplhcLotterySgExample();
                        XjplhcLotterySgExample.Criteria criteria = xjplhcLotterySgExample.createCriteria();
                        criteria.andIssueEqualTo(sg.getIssue());
                        if (needUpdate == false && xjplhcLotterySgMapper.selectOneByExample(xjplhcLotterySgExample) == null) {
                            needUpdate = true;
                        }

                        sg.setNumber(StringUtils.isEmpty(sg.getNumber()) ? "" : sg.getNumber());
                    }
                    if (needUpdate) {
                        xjplhcLotterySgMapperExt.insertBatch(list);
                    }
                }
            }

        } catch (Exception e) {
            logger.error("【新加坡六合彩】预期数据同步出错", e);
        } finally {
            lock.writeLock().unlock();
        }
    }



}
