package com.caipiao.live.order.service.order;


import com.lmax.disruptor.EventFactory;

public class OrderEventFactory implements EventFactory {
    @Override
    public OrderEvent newInstance() {
        return new OrderEvent();
    }
}
