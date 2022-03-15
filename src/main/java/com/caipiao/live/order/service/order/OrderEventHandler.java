package com.caipiao.live.order.service.order;


import com.lmax.disruptor.WorkHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class OrderEventHandler  implements WorkHandler<OrderEvent> {
    @Autowired
    private OrderWriteService orderWriteService;
    private static final Logger logger = LoggerFactory.getLogger(OrderEventHandler.class);

    @Override
    public void onEvent(OrderEvent orderEvent)  {
        logger.info("订单队列处理订单事件:{}",orderEvent);
        try {
            orderWriteService.processOrder(orderEvent.getOrderjson());
        }catch (Exception e){
            logger.error("订单队列消费异常:{}",e);
        }
        logger.info("订单队列对象:{}",this);
      //  System.out.println(this);
    }
}
