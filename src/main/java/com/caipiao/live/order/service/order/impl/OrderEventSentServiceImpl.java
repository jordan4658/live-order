package com.caipiao.live.order.service.order.impl;

import com.caipiao.live.order.service.order.OrderEvent;
import com.caipiao.live.order.service.order.OrderEventFactory;
import com.caipiao.live.order.service.order.OrderEventHandler;
import com.caipiao.live.order.service.order.OrderEventSentService;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;

@Service
public class OrderEventSentServiceImpl implements OrderEventSentService, DisposableBean, InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventSentServiceImpl.class);
    private Disruptor<OrderEvent> disruptor;
    private static final int RING_BUFFER_SIZE = 1024 * 8;
    @Autowired
    private OrderEventHandler orderEventHandler1;
    @Autowired
    private OrderEventHandler orderEventHandler2;
    @Autowired
    private OrderEventHandler orderEventHandler3;
    @Autowired
    private OrderEventHandler orderEventHandler4;
    @Autowired
    private OrderEventHandler orderEventHandler5;

    @Override
    public void destroy() throws Exception {
        disruptor.shutdown();
        logger.info("订单队列销毁:{}", disruptor);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        logger.info("订单队列初始化:{}", RING_BUFFER_SIZE);
        disruptor = new Disruptor<OrderEvent>(new OrderEventFactory(), RING_BUFFER_SIZE, Executors.defaultThreadFactory(), ProducerType.SINGLE, new BlockingWaitStrategy());
        // disruptor.handleEventsWith(orderEventHandler);
        disruptor.handleEventsWithWorkerPool(orderEventHandler1, orderEventHandler2, orderEventHandler3, orderEventHandler4, orderEventHandler5);
        disruptor.start();
        logger.info("订单队列启动:{}", disruptor);
    }

    @Override
    public void sendOrderJson(String orderJson) {
        RingBuffer<OrderEvent> ringBuffer = disruptor.getRingBuffer();
        logger.info("订单推送队列1:{}", orderJson);
        ringBuffer.publishEvent(new EventTranslatorOneArg<OrderEvent, String>() {
            @Override
            public void translateTo(OrderEvent event, long sequence, String data) {
                event.setOrderjson(orderJson);
                logger.info("订单推送队列2:{}", orderJson);
            }
        }, orderJson);
        //  ringBuffer.publishEvent((event, sequence, data) -> event.setOrderjson(data), orderJson); //lambda式写法，如果是用jdk1.8以下版本使用以上注释的一段

    }


}
