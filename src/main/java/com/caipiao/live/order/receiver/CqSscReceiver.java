package com.caipiao.live.order.receiver;

import com.alibaba.fastjson.JSONObject;
import com.caipiao.live.order.config.ActiveMQConfig;
import com.caipiao.live.order.service.bet.BetSscCqService;
import com.caipiao.live.common.mybatis.entity.CqsscLotterySg;
import com.caipiao.live.common.mybatis.entity.CqsscLotterySgExample;
import com.caipiao.live.common.mybatis.mapper.CqsscLotterySgMapper;
import com.caipiao.live.common.mybatis.mapperext.sg.CqsscLotterySgMapperExt;
import com.caipiao.live.common.util.DateUtils;
import com.caipiao.live.common.util.redis.BasicRedisClient;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class CqSscReceiver {
    private static final Logger logger = LoggerFactory.getLogger(CqSscReceiver.class);

    @Autowired
    private BetSscCqService betSscCqService;
    @Autowired
    private CqsscLotterySgMapper cqsscLotterySgMapper;
    @Autowired
    private CqsscLotterySgMapperExt cqsscLotterySgMapperExt;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private BasicRedisClient basicRedisClient;

    @JmsListener(destination = ActiveMQConfig.TOPIC_SSC_CQ_YUQI_DATA, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgSscCqYuqiData(String message) throws Exception {
        logger.info("【重庆时时彩】预期数据同步：  " + message);
        List<CqsscLotterySg> list = JSONObject.parseArray(message.substring(message.indexOf(":") + 1), CqsscLotterySg.class);
        // 获取唯一
        String key = ActiveMQConfig.TOPIC_SSC_CQ_YUQI_DATA + list.get(0).getIssue();
        // 【分布式读写锁】
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 10, TimeUnit.SECONDS);
            if (bool) {
                boolean needUpdate = false;
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    for (CqsscLotterySg sg : list) {
                        CqsscLotterySgExample cqsscLotterySgExample = new CqsscLotterySgExample();
                        CqsscLotterySgExample.Criteria criteria = cqsscLotterySgExample.createCriteria();
                        criteria.andIssueEqualTo(sg.getIssue());
                        if (needUpdate == false && cqsscLotterySgMapper.selectOneByExample(cqsscLotterySgExample) == null) {
                            needUpdate = true;
                        }

                        sg.setIdealDate(DateUtils.str2date(sg.getIdealTime()));
                    }
                    if (needUpdate) {
                        cqsscLotterySgMapperExt.insertBatch(list);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("processMsgSscCqYuqiData occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
