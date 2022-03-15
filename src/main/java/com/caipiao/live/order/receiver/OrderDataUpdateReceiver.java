package com.caipiao.live.order.receiver;

import com.caipiao.live.order.config.ActiveMQConfig;
import com.caipiao.live.common.mybatis.entity.OrderBetRecord;
import com.caipiao.live.common.mybatis.entity.OrderBetRecordExample;
import com.caipiao.live.common.mybatis.entity.OrderRecord;
import com.caipiao.live.common.mybatis.entity.OrderRecordExample;
import com.caipiao.live.common.mybatis.mapper.OrderBetRecordMapper;
import com.caipiao.live.common.mybatis.mapper.OrderRecordMapper;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author lzy
 * @create 2018-09-17 20:07
 **/
@Component
public class OrderDataUpdateReceiver {
    private static final Logger logger = LoggerFactory.getLogger(OrderDataUpdateReceiver.class);

    @Autowired
    private OrderBetRecordMapper orderBetRecordMapper;
    @Autowired
    private OrderRecordMapper orderRecordMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;


    /**
     * 发送一个消息 到order_server服务中，更新 order_bet_record的字段issue，order_sn
     *
     * @param message 消息内容【期号】
     */
    @JmsListener(destination = ActiveMQConfig.TOPIC_ORDER_DATA_UPDATE, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgAzAct(String message) throws Exception {
        logger.info("订单数据同步：" + message);
        // 获取唯一
        String key = ActiveMQConfig.TOPIC_ORDER_DATA_UPDATE;
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(1, 10, TimeUnit.SECONDS);
            if (bool) {
                if (!redisTemplate.hasKey(key)) {
                    for(int i = 0;i < 1000;i++){
                        OrderBetRecordExample orderBetRecordExample = new OrderBetRecordExample();
                        OrderBetRecordExample.Criteria criteria = orderBetRecordExample.createCriteria();
                        criteria.andIssueEqualTo("");
                        orderBetRecordExample.setOffset(0);
                        orderBetRecordExample.setLimit(1000);
                        List<OrderBetRecord> orderBetRecordList = orderBetRecordMapper.selectByExample(orderBetRecordExample);
                        for(OrderBetRecord orderBetRecord:orderBetRecordList){
                            OrderRecordExample orderRecordExample = new OrderRecordExample();
                            OrderRecordExample.Criteria recordCriterial = orderRecordExample.createCriteria();
                            recordCriterial.andIdEqualTo(orderBetRecord.getOrderId());
                            OrderRecord orderRecord = orderRecordMapper.selectOneByExample(orderRecordExample);
                            if(null != orderRecord){
                                orderBetRecord.setIssue(orderRecord.getIssue());
                                orderBetRecord.setOrderSn(orderRecord.getOrderSn());
                                orderBetRecordMapper.updateByPrimaryKey(orderBetRecord);
                            }
                        }
                    }

                    redisTemplate.opsForValue().set(key,1,60,TimeUnit.SECONDS);
                }
            } else {
                logger.info("订单数据同步 拿不到锁");
            }
        } catch (Exception e) {
            logger.error("订单数据同步出错", e);
        } finally {
            lock.writeLock().unlock();
        }
    }


}

