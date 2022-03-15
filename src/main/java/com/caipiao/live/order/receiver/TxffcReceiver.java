package com.caipiao.live.order.receiver;

import com.alibaba.fastjson.JSONObject;
import com.caipiao.live.order.config.ActiveMQConfig;
import com.caipiao.live.order.service.bet.BetSscTxService;
import com.caipiao.live.common.mybatis.entity.TxffcLotterySg;
import com.caipiao.live.common.mybatis.entity.TxffcLotterySgExample;
import com.caipiao.live.common.mybatis.mapper.TxffcLotterySgMapper;
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
public class TxffcReceiver {
    private static final Logger logger = LoggerFactory.getLogger(TxffcReceiver.class);

    @Autowired
    private BetSscTxService betSscTxService;
    @Autowired
    private TxffcLotterySgMapper txffcLotterySgMapper;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private BasicRedisClient basicRedisClient;


    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_SSC_TX_YUQI_DATA, containerFactory = "jmsListenerContainerTopicDurable")
    public void sameDataYuqiFC3d(String message) throws Exception {
        logger.info("【比特币分分彩】预期数据同步：  " + message);

        List<TxffcLotterySg> list = JSONObject.parseArray(message.substring(message.indexOf(":") + 1), TxffcLotterySg.class);
        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_SSC_TX_YUQI_DATA + list.get(0).getIssue();
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 20, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50l);
                    for (TxffcLotterySg sg : list) {
                        TxffcLotterySgExample txffcLotterySgExample = new TxffcLotterySgExample();
                        TxffcLotterySgExample.Criteria criteria = txffcLotterySgExample.createCriteria();
                        criteria.andIssueEqualTo(sg.getIssue());
                        if (txffcLotterySgMapper.selectOneByExample(txffcLotterySgExample) == null) {
                            txffcLotterySgMapper.insertSelective(sg);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("sameDataYuqiFC3d occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

}
