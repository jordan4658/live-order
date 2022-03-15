package com.caipiao.live.order.config;

import org.springframework.context.annotation.Configuration;


@Configuration
//@ComponentScan({"com.caipiao.business.service.order"})
//@ComponentScan({"com.caipiao.business.utils"})
//@ComponentScan({"com.caipiao.business.receiver"})
public class ActiveMqConsumerListen {

//    @Autowired
//    private OrderWriteService orderWriteService;
//
//    @Autowired
//    private LhcReceiver lhcReceiver;
//
//    private static final Logger logger = LoggerFactory.getLogger(ActiveMqConsumerListen.class);
//
//
//
////    @Bean
////    public JmsMessagingTemplate jmsMessagingTemplate(PooledConnectionFactory connectionFactory){
////        return new JmsMessagingTemplate(connectionFactory);
////    }
//
//    @Value("${spring.activemq.user}")
//    private String user;
//    @Value("${spring.activemq.password}")
//    private String password;
//    @Value("${spring.activemq.broker-url}")
//    private String brokerUrl;
//    @Value("${spring.profiles.active}")
//    private String enviroment;
//    @Value("${product.order.enviroment}")
//    private String productOrderEnvi;
//
//    public static String USER;
//    public static String PASSWORD;
//    public static String BROKERURL;
//    public static String ENVIROMENT;
//    public static String PRODUCTORDERENVI;
//
//    @Override
//    public void afterPropertiesSet() {
//        USER = this.user;
//        PASSWORD = this.password;
//        BROKERURL = this.brokerUrl;
//        ENVIROMENT = this.enviroment;
//        PRODUCTORDERENVI = this.productOrderEnvi;
//        logger.info("连接mq信息："+BROKERURL+","+USER+","+PASSWORD+","+ENVIROMENT);
//        orderListen();
//    }
//
//
//    /**
//     *
//     */
//    public void orderListen() {
//        //1. 创建ConnectionFactory
//        //ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(username,password,url);
//        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(USER, PASSWORD, BROKERURL);
//        PooledConnectionFactory poolFactory = new PooledConnectionFactory(factory);
//
//
//        // 创建SSL连接器工厂类
////        ActiveMQSslConnectionFactory sslConnectionFactory = new ActiveMQSslConnectionFactory();
////        // 设置参数，并加载SSL密钥和证书信息
////        sslConnectionFactory.setBrokerURL(BROKERURL);
////        sslConnectionFactory.setUserName(USER);
////        sslConnectionFactory.setPassword(PASSWORD);
//
//        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(BROKERURL);
//        connectionFactory.setUserName(USER);
//        connectionFactory.setPassword(PASSWORD);
//
//        QueueConnection queueConnection = null;
////        Connection connection = null;
////        Session session = null;
//        QueueSession queueSession = null;
//        MessageConsumer consumer = null;
////        ConnectionConsumer connectionConsumer = null;
//        try {
//            //2. 创建Connection
////            connection = poolFactory.createConnection();
////            connection = poolFactory.createConnection();
//            queueConnection = poolFactory.createQueueConnection();
//
//            //3. 启动连接
//            queueConnection.start();
//            //4. 创建会话
//            queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
//
//            //5. 创建一个目标 - QUEUE_ORDER
////            Destination destination = session.createQueue(ActiveMQConfig.QUEUE_ORDER + PRODUCTORDERENVI);
//            Queue destination = queueSession.createQueue(ActiveMQConfig.QUEUE_ORDER + PRODUCTORDERENVI);
//            //6. 创建一个消费者
//            consumer = queueSession.createConsumer(destination);
//
//            //7. 创建一个监听器
////            ConsumerConnectionSet.add(connection);
//            consumer.setMessageListener(new MessageListener() {
//                public void onMessage(Message message) {
//                    try {
//                        String messageText = ((TextMessage) message).getText();
//                        logger.info("下注订单入库：  " + messageText);
//                        // 拆分消息内容
//                        //   String[] str = message.split(":");
//                        messageText = messageText.replace("ORDER:", "");
//                        // 下注订单入库
//                        orderWriteService.processOrder(messageText);
//                    } catch (JMSException e) {
//                        logger.error("下注订单入库：  " + e.getMessage());
//                    }
//                }
//            });
//
//            /**
//             * ===================================================================
//             * =============	AG KY 修改用户可提款 					==================
//             * ===================================================================
//             */
//            destination = queueSession.createQueue(ActiveMQConfig.AG_OR_KY_QUEUE_NOWITHDRAWALAMOUNT + PRODUCTORDERENVI);
//            consumer = queueSession.createConsumer(destination);
//            consumer.setMessageListener(new MessageListener() {
//                public void onMessage(Message message) {
//                    try {
//                        String messageText = ((TextMessage) message).getText();
//                        // kyService.changeNoWithdrawalAmount(messageText);
//                    } catch (Exception e) {
//                        logger.error("AG或KY修改用户可提款 出错" + e.getMessage());
//                    }
//                }
//            });
//
////            Thread.sleep(1000L);
////            session.commit();
//        } catch (Exception e) {
//            logger.error("消费消息出错：" + ActiveMQConfig.QUEUE_ORDER + PRODUCTORDERENVI, e);
//        } finally {
////            if (consumer != null) {
////                try {
////                    consumer.close();
////                } catch (JMSException e1) {
////                }
////            }
////            if (session != null) {
////                try {
////                    session.close();
////                } catch (JMSException e2) {
////                }
////            }
////            if (connection != null) {
////                try {
////                    connection.close();
////                } catch (JMSException e3) {
////                }
////            }
//        }
//    }


}
