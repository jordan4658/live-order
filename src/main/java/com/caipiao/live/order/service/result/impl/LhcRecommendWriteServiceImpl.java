package com.caipiao.live.order.service.result.impl;

import com.caipiao.live.order.service.result.LhcRecommendWriteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * @author lzy
 * @create 2018-08-20 14:21
 **/
@Service
public class LhcRecommendWriteServiceImpl implements LhcRecommendWriteService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private JmsMessagingTemplate jmsMessagingTemplate;

}
