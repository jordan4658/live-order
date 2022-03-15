package com.caipiao.live.order.config;

//import org.springframework.amqp.core.Binding;
//import org.springframework.amqp.core.BindingBuilder;
//import org.springframework.amqp.core.Queue;
//import org.springframework.amqp.core.TopicExchange;

/**
 * 消息队列方式：Topic
 * Topic 按规则转发消息（最灵活）。
 */
//@Configuration
public class RabbitConfig {

    // 死信的交换机名
    public static final String DEAD_LETTER_EXCHANGE = "DEAD_LETTER_EXCHANGE";
    // 死信队列名（替补队列）
    public static final String DEAD_LETTER_QUEUE = "DEAD_LETTER_QUEUE";
    // 死信队列 ROUTING_KEY
    public static final String DEAD_ROUTING_KEY = "DEAD_ROUTING_KEY";

//    // 声明死信队列（替补作用）
//    @Bean
//    public Queue deadLetterQueue() {
//        return new Queue(DEAD_LETTER_QUEUE);
//    }
//
//    // 声明死信队列交互器
//    @Bean
//    public TopicExchange deadLetterExchange() {
//        return new TopicExchange(DEAD_LETTER_EXCHANGE, true, false);
//    }
//
//    // 绑定死信队列
//    @Bean
//    public Binding deadLetterBinding(Queue deadLetterQueue, TopicExchange deadLetterExchange) {
//        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(DEAD_ROUTING_KEY);
//    }


    /**
     * 定义业务交换机
     */

    // 交换机名称
    public static final String TOPIC_EXCHANGE = "TOPIC_EXCHANGE";

//    // 声明 交换机
//    @Bean
//    TopicExchange topicExchange() {
//        return new TopicExchange(TOPIC_EXCHANGE);
//    }

    //##############################################################################################
    //###                                  重庆时时彩开采结算                                       ###
    //##############################################################################################

    // 重庆时时彩 绑定值
    public final static String BINDING_SSC_CQ = "BINDING_SSC_CQ";

//    // 重庆时时彩【直选】 队列名
//    public final static String QUEUE_SSC_CQ_DE = "QUEUE_SSC_CQ_DE";
//
//    // 重庆时时彩【组选】 队列名
//    public final static String QUEUE_SSC_CQ_GS = "QUEUE_SSC_CQ_GS";
//
//    // 重庆时时彩【定位胆】 队列名
//    public final static String QUEUE_SSC_CQ_DW = "QUEUE_SSC_CQ_DW";
//
//    // 重庆时时彩【大小单双】 队列名
//    public final static String QUEUE_SSC_CQ_DW_BS = "QUEUE_SSC_CQ_DW_BS";
    
    // 重庆时时彩【直选】 队列名
    public final static String QUEUE_SSC_CQ_LM = "QUEUE_SSC_CQ_LM";

    // 重庆时时彩【组选】 队列名
    public final static String QUEUE_SSC_CQ_DN = "QUEUE_SSC_CQ_DN";

    // 重庆时时彩【定位胆】 队列名
    public final static String QUEUE_SSC_CQ_15 = "QUEUE_SSC_CQ_15";

    // 重庆时时彩【大小单双】 队列名
    public final static String QUEUE_SSC_CQ_QZH = "QUEUE_SSC_CQ_QZH";

    // 重庆时时彩 - 更新【免费推荐】/【公式杀号】数据 队列名
    public final static String QUEUE_SSC_CQ_UPDATE_DATA = "QUEUE_SSC_CQ_UPDATE_DATA";

    /**
     * 重庆时时彩 - 【直选】队列
     */
    // 定义：时时彩直选队列
//    @Bean
//    Queue queueCqSscDe() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_SSC_CQ_LM, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定 时时彩直选队列
//    @Bean
//    Binding bindingCqSscDe(Queue queueCqSscDe, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueCqSscDe).to(topicExchange).with(BINDING_SSC_CQ);
//    }
//
//    /**
//     * 重庆时时彩 - 【组选】队列
//     */
//    // 定义：时时彩组选选队列
//    @Bean
//    Queue queueCqSscGs() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_SSC_CQ_DN, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定：时时彩组选队列
//    @Bean
//    Binding bindingCqSscGs(Queue queueCqSscGs, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueCqSscGs).to(topicExchange).with(BINDING_SSC_CQ);
//    }
//
//    /**
//     * 重庆时时彩 - 【定位胆】队列
//     */
//    // 定义：时时彩组选选队列
//    @Bean
//    Queue queueCqSscDw() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_SSC_CQ_15, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定：时时彩组选队列
//    @Bean
//    Binding bindingCqSscDw(Queue queueCqSscDw, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueCqSscDw).to(topicExchange).with(BINDING_SSC_CQ);
//    }
//
//    /**
//     * 重庆时时彩 - 【大下单双】队列
//     */
//    // 定义：时时彩组选选队列
//    @Bean
//    Queue queueCqSscBs() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_SSC_CQ_QZH, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定：时时彩组选队列
//    @Bean
//    Binding bindingCqSscBs(Queue queueCqSscBs, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueCqSscBs).to(topicExchange).with(BINDING_SSC_CQ);
//    }
//
//    /**
//     * 重庆时时彩 - 更新【免费推荐】/【公式杀号】数据 队列名
//     */
//    // 定义：时时彩组选选队列
//    @Bean
//    Queue queueCqSscUpdateData() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_SSC_CQ_UPDATE_DATA, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定：时时彩组选队列
//    @Bean
//    Binding bindingCqSscUpdateData(Queue queueCqSscUpdateData, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueCqSscUpdateData).to(topicExchange).with(BINDING_SSC_CQ);
//    }
    
    //##############################################################################################
    //###                                  天津时时彩开采结算                                       ###
    //##############################################################################################

    // 天津时时彩 绑定值
    public final static String BINDING_SSC_TJ= "BINDING_SSC_TJ";

    // 天津时时彩【直选】 队列名
    public final static String QUEUE_SSC_TJ_LM = "QUEUE_SSC_TJ_LM";

    // 天津时时彩【组选】 队列名
    public final static String QUEUE_SSC_TJ_DN = "QUEUE_SSC_TJ_DN";

    // 天津时时彩【定位胆】 队列名
    public final static String QUEUE_SSC_TJ_15 = "QUEUE_SSC_TJ_15";

    // 天津时时彩【大小单双】 队列名
    public final static String QUEUE_SSC_TJ_QZH = "QUEUE_SSC_TJ_QZH";

    // 天津时时彩 - 更新【免费推荐】/【公式杀号】数据 队列名
    public final static String QUEUE_SSC_TJ_UPDATE_DATA = "QUEUE_SSC_TJ_UPDATE_DATA";

//    /**
//     * 天津时时彩 - 【直选】队列
//     */
//    // 定义：时时彩直选队列
//    @Bean
//    Queue queueTjSscDe() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_SSC_TJ_LM, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定 时时彩直选队列
//    @Bean
//    Binding bindingTjSscDe(Queue queueTjSscDe, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueTjSscDe).to(topicExchange).with(BINDING_SSC_TJ);
//    }
//
//    /**
//     * 天津时时彩 - 【组选】队列
//     */
//    // 定义：时时彩组选选队列
//    @Bean
//    Queue queueTjSscGs() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_SSC_TJ_DN, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定：时时彩组选队列
//    @Bean
//    Binding bindingTjSscGs(Queue queueTjSscGs, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueTjSscGs).to(topicExchange).with(BINDING_SSC_TJ);
//    }
//
//    /**
//     * 天津时时彩 - 【定位胆】队列
//     */
//    // 定义：时时彩组选选队列
//    @Bean
//    Queue queueTjSscDw() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_SSC_TJ_15, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定：时时彩组选队列
//    @Bean
//    Binding bindingTjSscDw(Queue queueTjSscDw, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueTjSscDw).to(topicExchange).with(BINDING_SSC_TJ);
//    }
//
//    /**
//     * 天津时时彩 - 【大下单双】队列
//     */
//    // 定义：时时彩组选选队列
//    @Bean
//    Queue queueTjSscBs() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_SSC_TJ_QZH, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定：时时彩组选队列
//    @Bean
//    Binding bindingTjSscBs(Queue queueTjSscBs, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueTjSscBs).to(topicExchange).with(BINDING_SSC_TJ);
//    }
//
//    /**
//     * 天津时时彩 - 更新【免费推荐】/【公式杀号】数据 队列名·
//     */
//    // 定义：时时彩组选选队列
//    @Bean
//    Queue queueTjSscUpdateData() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_SSC_TJ_UPDATE_DATA, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定：时时彩组选队列
//    @Bean
//    Binding bindingTjSscUpdateData(Queue queueXjSscUpdateData, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueXjSscUpdateData).to(topicExchange).with(BINDING_SSC_TJ);
//    }
//
    
  
    
    
    //##############################################################################################
    //###                                  新疆时时彩开采结算                                       ###
    //##############################################################################################

    // 新疆时时彩 绑定值
    public final static String BINDING_SSC_XJ = "BINDING_SSC_XJ";

    // 新疆时时彩【直选】 队列名
    public final static String QUEUE_SSC_XJ_LM = "QUEUE_SSC_XJ_LM";

    // 新疆时时彩【组选】 队列名
    public final static String QUEUE_SSC_XJ_DN = "QUEUE_SSC_XJ_DN";

    // 新疆时时彩【定位胆】 队列名
    public final static String QUEUE_SSC_XJ_15 = "QUEUE_SSC_XJ_15";

    // 新疆时时彩【大小单双】 队列名
    public final static String QUEUE_SSC_XJ_QZH = "QUEUE_SSC_XJ_QZH";

    // 新疆时时彩 - 更新【免费推荐】/【公式杀号】数据 队列名
    public final static String QUEUE_SSC_XJ_UPDATE_DATA = "QUEUE_SSC_XJ_UPDATE_DATA";

//    /**
//     * 新疆时时彩 - 【直选】队列
//     */
//    // 定义：时时彩直选队列
//    @Bean
//    Queue queueXjSscDe() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_SSC_XJ_LM, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定 时时彩直选队列
//    @Bean
//    Binding bindingXjSscDe(Queue queueXjSscDe, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueXjSscDe).to(topicExchange).with(BINDING_SSC_XJ);
//    }
//
//    /**
//     * 新疆时时彩 - 【组选】队列
//     */
//    // 定义：时时彩组选选队列
//    @Bean
//    Queue queueXjSscGs() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_SSC_XJ_DN, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定：时时彩组选队列
//    @Bean
//    Binding bindingXjSscGs(Queue queueXjSscGs, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueXjSscGs).to(topicExchange).with(BINDING_SSC_XJ);
//    }
//
//    /**
//     * 新疆时时彩 - 【定位胆】队列
//     */
//    // 定义：时时彩组选选队列
//    @Bean
//    Queue queueXjSscDw() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_SSC_XJ_15, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定：时时彩组选队列
//    @Bean
//    Binding bindingXjSscDw(Queue queueXjSscDw, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueXjSscDw).to(topicExchange).with(BINDING_SSC_XJ);
//    }
//
//    /**
//     * 新疆时时彩 - 【大下单双】队列
//     */
//    // 定义：时时彩组选选队列
//    @Bean
//    Queue queueXjSscBs() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_SSC_XJ_QZH, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定：时时彩组选队列
//    @Bean
//    Binding bindingXjSscBs(Queue queueXjSscBs, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueXjSscBs).to(topicExchange).with(BINDING_SSC_XJ);
//    }
//
//    /**
//     * 新疆时时彩 - 更新【免费推荐】/【公式杀号】数据 队列名
//     */
//    // 定义：时时彩组选选队列
//    @Bean
//    Queue queueXjSscUpdateData() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_SSC_XJ_UPDATE_DATA, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定：时时彩组选队列
//    @Bean
//    Binding bindingXjSscUpdateData(Queue queueXjSscUpdateData, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueXjSscUpdateData).to(topicExchange).with(BINDING_SSC_XJ);
//    }
//


    //##############################################################################################
    //###                                  比特币分分彩开采结算                                       ###
    //##############################################################################################

    // 比特币分分彩 绑定值
    public final static String BINDING_SSC_TX = "BINDING_SSC_TX";

    // 比特币分分彩【直选】 队列名
    public final static String QUEUE_SSC_TX_LM = "QUEUE_SSC_TX_LM";

    // 比特币分分彩【组选】 队列名
    public final static String QUEUE_SSC_TX_DN = "QUEUE_SSC_TX_DN";

    // 比特币分分彩【定位胆】 队列名
    public final static String QUEUE_SSC_TX_15 = "QUEUE_SSC_TX_15";

    // 比特币分分彩【大小单双】 队列名
    public final static String QUEUE_SSC_TX_QZH = "QUEUE_SSC_TX_QZH";

    // 比特币分分彩 - 更新【免费推荐】/【公式杀号】数据 队列名
    public final static String QUEUE_SSC_TX_UPDATE_DATA = "QUEUE_SSC_TX_UPDATE_DATA";


//    /**
//     * 比特币分分彩 - 【直选】队列
//     */
//    // 定义：时时彩直选队列
//    @Bean
//    Queue queueTxSscDe() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_SSC_TX_LM, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定 时时彩直选队列
//    @Bean
//    Binding bindingTxSscDe(Queue queueTxSscDe, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueTxSscDe).to(topicExchange).with(BINDING_SSC_TX);
//    }
//
//    /**
//     * 比特币分分彩 - 【组选】队列
//     */
//    // 定义：时时彩组选选队列
//    @Bean
//    Queue queueTxSscGs() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_SSC_TX_DN, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定：时时彩组选队列
//    @Bean
//    Binding bindingTxSscGs(Queue queueTxSscGs, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueTxSscGs).to(topicExchange).with(BINDING_SSC_TX);
//    }
//
//    /**
//     * 比特币分分彩 - 【定位胆】队列
//     */
//    // 定义：时时彩组选选队列
//    @Bean
//    Queue queueTxSscDw() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_SSC_TX_15, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定：时时彩组选队列
//    @Bean
//    Binding bindingTxSscDw(Queue queueTxSscDw, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueTxSscDw).to(topicExchange).with(BINDING_SSC_TX);
//    }
//
//    /**
//     * 比特币分分彩 - 【大下单双】队列
//     */
//    // 定义：时时彩组选选队列
//    @Bean
//    Queue queueTxSscBs() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_SSC_TX_QZH, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定：时时彩组选队列
//    @Bean
//    Binding bindingTxSscBs(Queue queueTxSscBs, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueTxSscBs).to(topicExchange).with(BINDING_SSC_TX);
//    }
//
//    /**
//     * 比特币分分彩 - 更新【免费推荐】/【公式杀号】数据 队列名
//     */
//    // 定义：时时彩组选选队列
//    @Bean
//    Queue queueTxSscUpdateData() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_SSC_TX_UPDATE_DATA, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定：时时彩组选队列
//    @Bean
//    Binding bindingTxSscUpdateData(Queue queueTxSscUpdateData, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueTxSscUpdateData).to(topicExchange).with(BINDING_SSC_TX);
//    }


    //##############################################################################################
    //###                                   PC蛋蛋开采结算                                         ###
    //##############################################################################################

    // PC蛋蛋 绑定值
    public final static String BINDING_PCEGG = "BINDING_PCEGG";

    // PC蛋蛋特码 队列名
    public final static String QUEUE_PCEGG_TM = "QUEUE_PCEGG_TM";

    // PC蛋蛋豹子 队列名
    public final static String QUEUE_PCEGG_BZ = "QUEUE_PCEGG_BZ";

    // PC蛋蛋特码包三 队列名
    public final static String QUEUE_PCEGG_TMBS = "QUEUE_PCEGG_TMBS";

    // PC蛋蛋色波 队列名
    public final static String QUEUE_PCEGG_SB = "QUEUE_PCEGG_SB";

    // PC蛋蛋混合 队列名
    public final static String QUEUE_PCEGG_HH = "QUEUE_PCEGG_HH";

//    /**
//     * PC蛋蛋 - 【特码】队列
//     */
//    // 定义：PC蛋蛋特码队列
//    @Bean
//    Queue queuePceggTm() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_PCEGG_TM, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定 PC蛋蛋特码队列
//    @Bean
//    Binding bindingPceggTm(Queue queuePceggTm, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queuePceggTm).to(topicExchange).with(BINDING_PCEGG);
//    }
//
//    /**
//     * PC蛋蛋 - 【豹子】队列
//     */
//    // 定义：PC蛋蛋豹子队列
//    @Bean
//    Queue queuePceggBz() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_PCEGG_BZ, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定 PC蛋蛋豹子队列
//    @Bean
//    Binding bindingPceggBz(Queue queuePceggBz, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queuePceggBz).to(topicExchange).with(BINDING_PCEGG);
//    }
//
//    /**
//     * PC蛋蛋 - 【特码包三】队列
//     */
//    // 定义：PC蛋蛋特码包三队列
//    @Bean
//    Queue queuePceggTmbs() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_PCEGG_TMBS, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定 PC蛋蛋特码包三队列
//    @Bean
//    Binding bindingPceggTmbs(Queue queuePceggTmbs, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queuePceggTmbs).to(topicExchange).with(BINDING_PCEGG);
//    }
//
//    /**
//     * PC蛋蛋 - 【色波】队列
//     */
//    // 定义：PC蛋蛋色波队列
//    @Bean
//    Queue queuePceggSb() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_PCEGG_SB, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定 PC蛋蛋色波队列
//    @Bean
//    Binding bindingPceggSb(Queue queuePceggSb, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queuePceggSb).to(topicExchange).with(BINDING_PCEGG);
//    }
//
//    /**
//     * PC蛋蛋 - 【混合】队列
//     */
//    // 定义：PC蛋蛋混合队列
//    @Bean
//    Queue queuePceggHh() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_PCEGG_HH, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定 PC蛋蛋混合队列
//    @Bean
//    Binding bindingPceggHh(Queue queuePceggHh, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queuePceggHh).to(topicExchange).with(BINDING_PCEGG);
//    }

    //##############################################################################################
    //###                                   北京PK10开采结算                                       ###
    //##############################################################################################

    // 北京PK10 绑定值
    public final static String BINDING_BJPKS = "BINDING_BJPKS";

    // 北京PK10两面 队列名
    public final static String QUEUE_BJPKS_LM = "QUEUE_BJPKS_LM";
    // 北京PK10猜名次猜前几 队列名
    public final static String QUEUE_BJPKS_CMC_CQJ = "QUEUE_BJPKS_CMC_CQJ";
    // 北京PK10单式猜前几 队列名
    public final static String QUEUE_BJPKS_DS_CQJ = "QUEUE_BJPKS_DS_CQJ";
    // 北京PK10定位胆 队列名
    public final static String QUEUE_BJPKS_DWD = "QUEUE_BJPKS_DWD";
    // 北京PK10冠亚和 队列名
    public final static String QUEUE_BJPKS_GYH = "QUEUE_BJPKS_GYH";
    // 北京PK10 - 更新【免费推荐】/【公式杀号】数据 队列名
    public final static String QUEUE_BJPKS_UPDATE_DATA = "QUEUE_BJPKS_UPDATE_DATA";

//    /**
//     * 北京PK10 - 【两面】队列
//     */
//    // 定义：北京PK10两面队列
//    @Bean
//    Queue queueBjpksLm() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_BJPKS_LM, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定 北京PK10两面队列
//    @Bean
//    Binding bindingBjpksLm(Queue queueBjpksLm, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueBjpksLm).to(topicExchange).with(BINDING_BJPKS);
//    }
//
//    /**
//     * 北京PK10 - 【猜名次猜前几】队列
//     */
//    // 定义：北京PK10猜名次猜前几队列
//    @Bean
//    Queue queueBjpksCmcCqj() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_BJPKS_CMC_CQJ, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定 北京PK10猜名次猜前几队列
//    @Bean
//    Binding bindingBjpksCmcCqj(Queue queueBjpksCmcCqj, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueBjpksCmcCqj).to(topicExchange).with(BINDING_BJPKS);
//    }
//
//    /**
//     * 北京PK10 - 【单式猜前几】队列
//     */
//    // 定义：北京PK10单式猜前几队列
////    @Bean
////    Queue queueBjpksDsCqj() {
////        Map<String, Object> map = new HashMap<>();
////        // 设置该Queue的死信的信箱
////        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
////        // 设置死信routingKey
////        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
////        return new Queue(QUEUE_BJPKS_DS_CQJ, true, false, false, map); // true 表示持久化该队列
////    }
//
//    // 绑定 北京PK10猜名次猜前几队列
////    @Bean
////    Binding bindingBjpksDsCqj(Queue queueBjpksDsCqj, TopicExchange topicExchange) {
////        return BindingBuilder.bind(queueBjpksDsCqj).to(topicExchange).with(BINDING_BJPKS);
////    }
//
//    /**
//     * 北京PK10 - 【定位胆】队列
//     */
//    // 定义：北京PK10定位胆队列
////    @Bean
////    Queue queueBjpksDwd() {
////        Map<String, Object> map = new HashMap<>();
////        // 设置该Queue的死信的信箱
////        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
////        // 设置死信routingKey
////        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
////        return new Queue(QUEUE_BJPKS_DWD, true, false, false, map); // true 表示持久化该队列
////    }
//
//    // 绑定 北京PK10定位胆队列
////    @Bean
////    Binding bindingBjpksDwd(Queue queueBjpksDwd, TopicExchange topicExchange) {
////        return BindingBuilder.bind(queueBjpksDwd).to(topicExchange).with(BINDING_BJPKS);
////    }
//
//    /**
//     * 北京PK10 - 【冠亚和】队列
//     */
//    // 定义：北京PK10冠亚和队列
//    @Bean
//    Queue queueBjpksGyh() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_BJPKS_GYH, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定 北京PK10冠亚和队列
//    @Bean
//    Binding bindingBjpksGyh(Queue queueBjpksGyh, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueBjpksGyh).to(topicExchange).with(BINDING_BJPKS);
//    }
//
//    /**
//     * 北京PK10 - 更新【免费推荐】/【公式杀号】数据 队列名
//     */
//    // 定义：时时彩组选选队列
//    @Bean
//    Queue queueBjpksUpdateData() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_BJPKS_UPDATE_DATA, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定：时时彩组选队列
//    @Bean
//    Binding bindingBjpksUpdateData(Queue queueBjpksUpdateData, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueBjpksUpdateData).to(topicExchange).with(BINDING_BJPKS);
//    }


    //##############################################################################################
    //###                                   澳洲牛牛开采结算                                       ###
    //##############################################################################################
    //澳洲ACT绑定值   （澳洲牛牛）
    public static final String BINDING_AUS_ACT = "BINDING_AUS_ACT";

    // 澳洲ACT 队列名
    public final static String QUEUE_AUS_ACT= "QUEUE_AUS_ACT";

    // 澳洲F1 队列名
    public final static String QUEUE_AUS_F1= "QUEUE_AUS_F1";

    // 澳洲时时彩 队列名
    public final static String QUEUE_AUS_SSC= "QUEUE_AUS_SSC";


//    /**
//     *  澳洲ACT数据 队列
//     */
//    @Bean
//    Queue queueAUSACT() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        // true 表示持久化该队列
//        return new Queue(QUEUE_AUS_ACT, true, false, false, map);
//    }
//    @Bean
//    Binding bindingAUSACT(Queue queueAUSACT, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueAUSACT).to(topicExchange).with(BINDING_AUS_ACT);
//    }
//
//    /**
//     *  澳洲F1数据 队列
//     */
//    @Bean
//    Queue queueAUSF1() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        // true 表示持久化该队列
//        return new Queue(QUEUE_AUS_F1, true, false, false, map);
//    }
//    @Bean
//    Binding bindingAUSF1(Queue queueAUSF1, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueAUSF1).to(topicExchange).with(BINDING_AUS_ACT);
//    }
//
//    /**
//     *  澳洲时时彩数据 队列
//     */
//    @Bean
//    Queue queueAUSSSC() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        // true 表示持久化该队列
//        return new Queue(QUEUE_AUS_SSC, true, false, false, map);
//    }
//    @Bean
//    Binding bindingAUSSSC(Queue queueAUSSSC, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueAUSSSC).to(topicExchange).with(BINDING_AUS_ACT);
//    }


    //##############################################################################################
    //###                                   快乐牛牛开采结算                                       ###
    //##############################################################################################

    // 快乐牛牛 绑定值   五分时时彩【直选】 队列名
//    public final static String QUEUE_SSC_FIVE_LM = "QUEUE_SSC_FIVE_LM";


    //##############################################################################################
    //###                                   德州牛牛开采结算                                       ###
    //##############################################################################################


    // 德州牛牛 绑定值
//    public final static String QUEUE_JSBJPKS_LM = "QUEUE_JSBJPKS_LM";

    
    //##############################################################################################
    //###                                   十分PK10开采结算                                       ###
    //##############################################################################################

    // 北京PK10 绑定值
    public final static String BINDING_TENBJPKS = "BINDING_TENBJPKS";

    // 北京PK10两面 队列名
    public final static String QUEUE_TENBJPKS_LM = "QUEUE_TENBJPKS_LM";
    // 北京PK10猜名次猜前几 队列名
    public final static String QUEUE_TENBJPKS_CMC_CQJ = "QUEUE_TENBJPKS_CMC_CQJ";
    // 北京PK10单式猜前几 队列名
    public final static String QUEUE_TENBJPKS_DS_CQJ = "QUEUE_TENBJPKS_DS_CQJ";
    // 北京PK10定位胆 队列名
    public final static String QUEUE_TENBJPKS_DWD = "QUEUE_TENBJPKS_DWD";
    // 北京PK10冠亚和 队列名
    public final static String QUEUE_TENBJPKS_GYH = "QUEUE_TENBJPKS_GYH";
    // 北京PK10 - 更新【免费推荐】/【公式杀号】数据 队列名
    public final static String QUEUE_TENBJPKS_UPDATE_DATA = "QUEUE_TENBJPKS_UPDATE_DATA";

//    /**
//     * 北京PK10 - 【两面】队列
//     */
//    // 定义：北京PK10两面队列
//    @Bean
//    Queue queueTenbjpksLm() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_TENBJPKS_LM, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定 北京PK10两面队列
//    @Bean
//    Binding bindingTenbjpksLm(Queue queueTenbjpksLm, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueTenbjpksLm).to(topicExchange).with(BINDING_TENBJPKS);
//    }
//
//    /**
//     * 北京PK10 - 【猜名次猜前几】队列
//     */
//    // 定义：北京PK10猜名次猜前几队列
//    @Bean
//    Queue queueTenbjpksCmcCqj() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_TENBJPKS_CMC_CQJ, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定 北京PK10猜名次猜前几队列
//    @Bean
//    Binding bindingTenbjpksCmcCqj(Queue queueTenbjpksCmcCqj, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueTenbjpksCmcCqj).to(topicExchange).with(BINDING_TENBJPKS);
//    }
//
//    /**
//     * 北京PK10 - 【单式猜前几】队列
//     */
//    // 定义：北京PK10单式猜前几队列
//    @Bean
//    Queue queueTenbjpksDsCqj() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_TENBJPKS_DS_CQJ, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定 北京PK10猜名次猜前几队列
//    @Bean
//    Binding bindingTenbjpksDsCqj(Queue queueTenbjpksDsCqj, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueTenbjpksDsCqj).to(topicExchange).with(BINDING_TENBJPKS);
//    }
//
//    /**
//     * 北京PK10 - 【定位胆】队列
//     */
//    // 定义：北京PK10定位胆队列
//    @Bean
//    Queue queueTenbjpksDwd() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_TENBJPKS_DWD, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定 北京PK10定位胆队列
//    @Bean
//    Binding bindingTenbjpksDwd(Queue queueTenbjpksDwd, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueTenbjpksDwd).to(topicExchange).with(BINDING_TENBJPKS);
//    }
//
//    /**
//     * 北京PK10 - 【冠亚和】队列
//     */
//    // 定义：北京PK10冠亚和队列
//    @Bean
//    Queue queueTenbjpksGyh() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_TENBJPKS_GYH, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定 北京PK10冠亚和队列
//    @Bean
//    Binding bindingTenbjpksGyh(Queue queueTenbjpksGyh, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueTenbjpksGyh).to(topicExchange).with(BINDING_TENBJPKS);
//    }
//
//    /**
//     * 北京PK10 - 更新【免费推荐】/【公式杀号】数据 队列名
//     */
//    // 定义：时时彩组选选队列
//    @Bean
//    Queue queueTenbjpksUpdateData() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_TENBJPKS_UPDATE_DATA, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定：时时彩组选队列
//    @Bean
//    Binding bindingTenbjpksUpdateData(Queue queueTenbjpksUpdateData, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueTenbjpksUpdateData).to(topicExchange).with(BINDING_TENBJPKS);
//    }
    
  //##############################################################################################
    //###                                   五分PK10开采结算                                       ###
    //##############################################################################################

    // 北京PK10 绑定值
    public final static String BINDING_FIVEBJPKS = "BINDING_FIVEBJPKS";

    // 北京PK10两面 队列名
    public final static String QUEUE_FIVEBJPKS_LM = "QUEUE_FIVEBJPKS_LM";
    // 北京PK10猜名次猜前几 队列名
    public final static String QUEUE_FIVEBJPKS_CMC_CQJ = "QUEUE_FIVEBJPKS_CMC_CQJ";
    // 北京PK10单式猜前几 队列名
    public final static String QUEUE_FIVEBJPKS_DS_CQJ = "QUEUE_FIVEBJPKS_DS_CQJ";
    // 北京PK10定位胆 队列名
    public final static String QUEUE_FIVEBJPKS_DWD = "QUEUE_FIVEBJPKS_DWD";
    // 北京PK10冠亚和 队列名
    public final static String QUEUE_FIVEBJPKS_GYH = "QUEUE_FIVEBJPKS_GYH";
    // 北京PK10 - 更新【免费推荐】/【公式杀号】数据 队列名
    public final static String QUEUE_FIVEBJPKS_UPDATE_DATA = "QUEUE_FIVEBJPKS_UPDATE_DATA";

//    /**
//     * 北京PK10 - 【两面】队列
//     */
//    // 定义：北京PK10两面队列
//    @Bean
//    Queue queueFivebjpksLm() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_FIVEBJPKS_LM, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定 北京PK10两面队列
//    @Bean
//    Binding bindingFivebjpksLm(Queue queueFivebjpksLm, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueFivebjpksLm).to(topicExchange).with(BINDING_FIVEBJPKS);
//    }
//
//    /**
//     * 北京PK10 - 【猜名次猜前几】队列
//     */
//    // 定义：北京PK10猜名次猜前几队列
//    @Bean
//    Queue queueFivebjpksCmcCqj() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_FIVEBJPKS_CMC_CQJ, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定 北京PK10猜名次猜前几队列
//    @Bean
//    Binding bindingFivebjpksCmcCqj(Queue queueFivebjpksCmcCqj, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueFivebjpksCmcCqj).to(topicExchange).with(BINDING_FIVEBJPKS);
//    }
//
//    /**
//     * 北京PK10 - 【单式猜前几】队列
//     */
//    // 定义：北京PK10单式猜前几队列
//    @Bean
//    Queue queueFivebjpksDsCqj() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_FIVEBJPKS_DS_CQJ, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定 北京PK10猜名次猜前几队列
//    @Bean
//    Binding bindingFivebjpksDsCqj(Queue queueFivebjpksDsCqj, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueFivebjpksDsCqj).to(topicExchange).with(BINDING_FIVEBJPKS);
//    }
//
//    /**
//     * 北京PK10 - 【定位胆】队列
//     */
//    // 定义：北京PK10定位胆队列
//    @Bean
//    Queue queueFivebjpksDwd() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_FIVEBJPKS_DWD, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定 北京PK10定位胆队列
//    @Bean
//    Binding bindingFivebjpksDwd(Queue queueFivebjpksDwd, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueFivebjpksDwd).to(topicExchange).with(BINDING_FIVEBJPKS);
//    }
//
//    /**
//     * 北京PK10 - 【冠亚和】队列
//     */
//    // 定义：北京PK10冠亚和队列
//    @Bean
//    Queue queueFivebjpksGyh() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_FIVEBJPKS_GYH, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定 北京PK10冠亚和队列
//    @Bean
//    Binding bindingFivebjpksGyh(Queue queueFivebjpksGyh, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueFivebjpksGyh).to(topicExchange).with(BINDING_FIVEBJPKS);
//    }
//
//    /**
//     * 北京PK10 - 更新【免费推荐】/【公式杀号】数据 队列名
//     */
//    // 定义：时时彩组选选队列
//    @Bean
//    Queue queueFivebjpksUpdateData() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_FIVEBJPKS_UPDATE_DATA, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定：时时彩组选队列
//    @Bean
//    Binding bindingFivebjpksUpdateData(Queue queueFivebjpksUpdateData, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueFivebjpksUpdateData).to(topicExchange).with(BINDING_FIVEBJPKS);
//    }
    
    
  //##############################################################################################
    //###                                   德州PK10开采结算                                       ###
    //##############################################################################################

    // 北京PK10 绑定值
    public final static String BINDING_JSBJPKS = "BINDING_JSBJPKS";

    // 北京PK10两面 队列名
    public final static String QUEUE_JSBJPKS_LM = "QUEUE_JSBJPKS_LM";
    // 德州牛牛 队列名
    public final static String QUEUE_JSNN = "QUEUE_JSNN";
    // 北京PK10猜名次猜前几 队列名
    public final static String QUEUE_JSBJPKS_CMC_CQJ = "QUEUE_JSBJPKS_CMC_CQJ";
    // 北京PK10单式猜前几 队列名
    public final static String QUEUE_JSBJPKS_DS_CQJ = "QUEUE_JSBJPKS_DS_CQJ";
    // 北京PK10定位胆 队列名
    public final static String QUEUE_JSBJPKS_DWD = "QUEUE_JSBJPKS_DWD";
    // 北京PK10冠亚和 队列名
    public final static String QUEUE_JSBJPKS_GYH = "QUEUE_JSBJPKS_GYH";
    // 北京PK10 - 更新【免费推荐】/【公式杀号】数据 队列名
    public final static String QUEUE_JSBJPKS_UPDATE_DATA = "QUEUE_JSBJPKS_UPDATE_DATA";

//    /**
//     * 北京PK10 - 【两面】队列
//     */
//    // 定义：北京PK10两面队列
//    @Bean
//    Queue queueJsbjpksLm() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_JSBJPKS_LM, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定 北京PK10两面队列
//    @Bean
//    Binding bindingJsbjpksLm(Queue queueJsbjpksLm, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueJsbjpksLm).to(topicExchange).with(BINDING_JSBJPKS);
//    }
//
//    /**
//     * 德州牛牛 - 队列
//     */
//    // 定义：德州牛牛队列
//    @Bean
//    Queue queueJsNn() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_JSNN, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定 德州牛牛队列
//    @Bean
//    Binding bindingJsNn(Queue queueJsNn, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueJsNn).to(topicExchange).with(BINDING_JSBJPKS);
//    }
//
//    /**
//     * 北京PK10 - 【猜名次猜前几】队列
//     */
//    // 定义：北京PK10猜名次猜前几队列
//    @Bean
//    Queue queueJsbjpksCmcCqj() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_JSBJPKS_CMC_CQJ, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定 北京PK10猜名次猜前几队列
//    @Bean
//    Binding bindingJsbjpksCmcCqj(Queue queueJsbjpksCmcCqj, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueJsbjpksCmcCqj).to(topicExchange).with(BINDING_JSBJPKS);
//    }
//
//    /**
//     * 北京PK10 - 【单式猜前几】队列
//     */
//    // 定义：北京PK10单式猜前几队列
//    @Bean
//    Queue queueJsbjpksDsCqj() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_JSBJPKS_DS_CQJ, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定 北京PK10猜名次猜前几队列
//    @Bean
//    Binding bindingJsbjpksDsCqj(Queue queueJsbjpksDsCqj, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueJsbjpksDsCqj).to(topicExchange).with(BINDING_JSBJPKS);
//    }
//
//    /**
//     * 北京PK10 - 【定位胆】队列
//     */
//    // 定义：北京PK10定位胆队列
//    @Bean
//    Queue queueJsbjpksDwd() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_JSBJPKS_DWD, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定 北京PK10定位胆队列
//    @Bean
//    Binding bindingJsbjpksDwd(Queue queueJsbjpksDwd, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueJsbjpksDwd).to(topicExchange).with(BINDING_JSBJPKS);
//    }
//
//    /**
//     * 北京PK10 - 【冠亚和】队列
//     */
//    // 定义：北京PK10冠亚和队列
//    @Bean
//    Queue queueJsbjpksGyh() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_JSBJPKS_GYH, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定 北京PK10冠亚和队列
//    @Bean
//    Binding bindingJsbjpksGyh(Queue queueJsbjpksGyh, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueJsbjpksGyh).to(topicExchange).with(BINDING_JSBJPKS);
//    }
//
//    /**
//     * 北京PK10 - 更新【免费推荐】/【公式杀号】数据 队列名
//     */
//    // 定义：时时彩组选选队列
//    @Bean
//    Queue queueJsbjpksUpdateData() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_JSBJPKS_UPDATE_DATA, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定：时时彩组选队列
//    @Bean
//    Binding bindingJsbjpksUpdateData(Queue queueJsbjpksUpdateData, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueJsbjpksUpdateData).to(topicExchange).with(BINDING_JSBJPKS);
//    }
    
    //##############################################################################################
    //###                                   时时六合彩开采结算                                         ###
    //##############################################################################################

    // TcPlw 绑定值
    public final static String BINDING_SSLHC = "BINDING_SSLHC";

    // 六合彩(特码,正特,六肖,正码1-6) 队列名
    public final static String QUEUE_SSLHC_TM_ZT_LX = "QUEUE_SSLHC_TM_ZT_LX";
    // 六合彩(正码,半波,尾数) 队列名
    public final static String QUEUE_SSLHC_ZM_BB_WS = "QUEUE_SSLHC_ZM_BB_WS";
    // 六合彩(连码,连肖,连尾) 队列名
    public final static String QUEUE_SSLHC_LM_LX_LW = "QUEUE_SSLHC_LM_LX_LW";
    // 六合彩(不中,1-6龙虎,五行) 队列名
    public final static String QUEUE_SSLHC_BZ_LH_WX = "QUEUE_SSLHC_BZ_LH_WX";
    // 六合彩(平特,特肖) 队列名
    public final static String QUEUE_SSLHC_PT_TX = "QUEUE_SSLHC_PT_TX";

//    /**
//     * 六合彩 - 【特码,正特,六肖,正码1-6】队列
//     */
//    @Bean
//    Queue queueSslhcTmZtLx() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_SSLHC_TM_ZT_LX, true, false, false, map); // true 表示持久化该队列
//    }
//
//    @Bean
//    Binding bindingSslhcTmZtLx(Queue queueSslhcTmZtLx, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueSslhcTmZtLx).to(topicExchange).with(BINDING_SSLHC);
//    }
//
//    /**
//     * 六合彩 - 【正码,半波,尾数】队列
//     */
//    @Bean
//    Queue queueSslhcZmBbWs() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_SSLHC_ZM_BB_WS, true, false, false, map); // true 表示持久化该队列
//    }
//
//    @Bean
//    Binding bindingSslhcZmBbWs(Queue queueSslhcZmBbWs, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueSslhcZmBbWs).to(topicExchange).with(BINDING_SSLHC);
//    }
//
//    /**
//     * 六合彩 - 【连码,连肖,连尾】队列
//     */
//    @Bean
//    Queue queueSslhcLmLxLw() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_SSLHC_LM_LX_LW, true, false, false, map); // true 表示持久化该队列
//    }
//
//    @Bean
//    Binding bindingSslhcLmLxLw(Queue queueSslhcLmLxLw, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueSslhcLmLxLw).to(topicExchange).with(BINDING_SSLHC);
//    }
//
//    /**
//     * 六合彩 - 【不中,1-6龙虎,五行】队列
//     */
//    @Bean
//    Queue queueSslhcBzLhWx() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_SSLHC_BZ_LH_WX, true, false, false, map); // true 表示持久化该队列
//    }
//
//    @Bean
//    Binding bindingSslhcBzLhWx(Queue queueSslhcBzLhWx, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueSslhcBzLhWx).to(topicExchange).with(BINDING_SSLHC);
//    }
//
//    /**
//     * 六合彩 - 【平特,特肖】队列
//     */
//    @Bean
//    Queue queueSslhcPtTx() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_SSLHC_PT_TX, true, false, false, map); // true 表示持久化该队列
//    }
//
//    @Bean
//    Binding bindingSslhcPtTx(Queue queueSslhcPtTx, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueSslhcPtTx).to(topicExchange).with(BINDING_SSLHC);
//    }
    
    
  //##############################################################################################
    //###                                   五分六合彩开采结算                                         ###
    //##############################################################################################

    // 六合彩 绑定值
    public final static String BINDING_FIVELHC = "BINDING_FIVELHC";

    // 六合彩(特码,正特,六肖,正码1-6) 队列名
    public final static String QUEUE_FIVELHC_TM_ZT_LX = "QUEUE_FIVELHC_TM_ZT_LX";
    // 六合彩(正码,半波,尾数) 队列名
    public final static String QUEUE_FIVELHC_ZM_BB_WS = "QUEUE_FIVELHC_ZM_BB_WS";
    // 六合彩(连码,连肖,连尾) 队列名
    public final static String QUEUE_FIVELHC_LM_LX_LW = "QUEUE_FIVELHC_LM_LX_LW";
    // 六合彩(不中,1-6龙虎,五行) 队列名
    public final static String QUEUE_FIVELHC_BZ_LH_WX = "QUEUE_FIVELHC_BZ_LH_WX";
    // 六合彩(平特,特肖) 队列名
    public final static String QUEUE_FIVELHC_PT_TX = "QUEUE_FIVELHC_PT_TX";

//    /**
//     * 六合彩 - 【特码,正特,六肖,正码1-6】队列
//     */
//    @Bean
//    Queue queueFivelhcTmZtLx() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_FIVELHC_TM_ZT_LX, true, false, false, map); // true 表示持久化该队列
//    }
//
//    @Bean
//    Binding bindingFivelhcTmZtLx(Queue queueFivelhcTmZtLx, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueFivelhcTmZtLx).to(topicExchange).with(BINDING_FIVELHC);
//    }
//
//    /**
//     * 六合彩 - 【正码,半波,尾数】队列
//     */
//    @Bean
//    Queue queueFivelhcZmBbWs() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_FIVELHC_ZM_BB_WS, true, false, false, map); // true 表示持久化该队列
//    }
//
//    @Bean
//    Binding bindingFivelhcZmBbWs(Queue queueFivelhcZmBbWs, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueFivelhcZmBbWs).to(topicExchange).with(BINDING_FIVELHC);
//    }
//
//    /**
//     * 六合彩 - 【连码,连肖,连尾】队列
//     */
//    @Bean
//    Queue queueFivelhcLmLxLw() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_FIVELHC_LM_LX_LW, true, false, false, map); // true 表示持久化该队列
//    }
//
//    @Bean
//    Binding bindingFivelhcLmLxLw(Queue queueFivelhcLmLxLw, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueFivelhcLmLxLw).to(topicExchange).with(BINDING_FIVELHC);
//    }
//
//    /**
//     * 六合彩 - 【不中,1-6龙虎,五行】队列
//     */
//    @Bean
//    Queue queueFivelhcBzLhWx() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_FIVELHC_BZ_LH_WX, true, false, false, map); // true 表示持久化该队列
//    }
//
//    @Bean
//    Binding bindingFivelhcBzLhWx(Queue queueFivelhcBzLhWx, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueFivelhcBzLhWx).to(topicExchange).with(BINDING_FIVELHC);
//    }
//
//    /**
//     * 六合彩 - 【平特,特肖】队列
//     */
//    @Bean
//    Queue queueFivelhcPtTx() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_FIVELHC_PT_TX, true, false, false, map); // true 表示持久化该队列
//    }
//
//    @Bean
//    Binding bindingFivelhcPtTx(Queue queueFivelhcPtTx, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueFivelhcPtTx).to(topicExchange).with(BINDING_FIVELHC);
//    }
    
    
  //##############################################################################################
    //###                                   一分六合彩开采结算                                         ###
    //##############################################################################################

    // 六合彩 绑定值
    public final static String BINDING_ONELHC = "BINDING_ONELHC";

    // 六合彩(特码,正特,六肖,正码1-6) 队列名
    public final static String QUEUE_ONELHC_TM_ZT_LX = "QUEUE_ONELHC_TM_ZT_LX";
    // 六合彩(正码,半波,尾数) 队列名
    public final static String QUEUE_ONELHC_ZM_BB_WS = "QUEUE_ONELHC_ZM_BB_WS";
    // 六合彩(连码,连肖,连尾) 队列名
    public final static String QUEUE_ONELHC_LM_LX_LW = "QUEUE_ONELHC_LM_LX_LW";
    // 六合彩(不中,1-6龙虎,五行) 队列名
    public final static String QUEUE_ONELHC_BZ_LH_WX = "QUEUE_ONELHC_BZ_LH_WX";
    // 六合彩(平特,特肖) 队列名
    public final static String QUEUE_ONELHC_PT_TX = "QUEUE_ONELHC_PT_TX";

//    /**
//     * 六合彩 - 【特码,正特,六肖,正码1-6】队列
//     */
//    @Bean
//    Queue queueOnelhcTmZtLx() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_ONELHC_TM_ZT_LX, true, false, false, map); // true 表示持久化该队列
//    }
//
//    @Bean
//    Binding bindingOnelhcTmZtLx(Queue queueOnelhcTmZtLx, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueOnelhcTmZtLx).to(topicExchange).with(BINDING_ONELHC);
//    }
//
//    /**
//     * 六合彩 - 【正码,半波,尾数】队列
//     */
//    @Bean
//    Queue queueOnelhcZmBbWs() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_ONELHC_ZM_BB_WS, true, false, false, map); // true 表示持久化该队列
//    }
//
//    @Bean
//    Binding bindingOnelhcZmBbWs(Queue queueOnelhcZmBbWs, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueOnelhcZmBbWs).to(topicExchange).with(BINDING_ONELHC);
//    }
//
//    /**
//     * 六合彩 - 【连码,连肖,连尾】队列
//     */
//    @Bean
//    Queue queueOnelhcLmLxLw() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_ONELHC_LM_LX_LW, true, false, false, map); // true 表示持久化该队列
//    }
//
//    @Bean
//    Binding bindingOnelhcLmLxLw(Queue queueOnelhcLmLxLw, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueOnelhcLmLxLw).to(topicExchange).with(BINDING_ONELHC);
//    }
//
//    /**
//     * 六合彩 - 【不中,1-6龙虎,五行】队列
//     */
//    @Bean
//    Queue queueOnelhcBzLhWx() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_ONELHC_BZ_LH_WX, true, false, false, map); // true 表示持久化该队列
//    }
//
//    @Bean
//    Binding bindingOnelhcBzLhWx(Queue queueOnelhcBzLhWx, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueOnelhcBzLhWx).to(topicExchange).with(BINDING_ONELHC);
//    }
//
//    /**
//     * 六合彩 - 【平特,特肖】队列
//     */
//    @Bean
//    Queue queueOnelhcPtTx() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_ONELHC_PT_TX, true, false, false, map); // true 表示持久化该队列
//    }
//
//    @Bean
//    Binding bindingOnelhcPtTx(Queue queueOnelhcPtTx, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueOnelhcPtTx).to(topicExchange).with(BINDING_ONELHC);
//    }
    

    //##############################################################################################
    //###                                   六合彩开采结算                                         ###
    //##############################################################################################

    // 六合彩 绑定值
    public final static String BINDING_LHC = "BINDING_LHC";

    // 六合彩(特码,正特,六肖,正码1-6) 队列名
    public final static String QUEUE_LHC_TM_ZT_LX = "QUEUE_LHC_TM_ZT_LX";
    // 六合彩(正码,半波,尾数) 队列名
    public final static String QUEUE_LHC_ZM_BB_WS = "QUEUE_LHC_ZM_BB_WS";
    // 六合彩(连码,连肖,连尾) 队列名
    public final static String QUEUE_LHC_LM_LX_LW = "QUEUE_LHC_LM_LX_LW";
    // 六合彩(不中,1-6龙虎,五行) 队列名
    public final static String QUEUE_LHC_BZ_LH_WX = "QUEUE_LHC_BZ_LH_WX";
    // 六合彩(平特,特肖) 队列名
    public final static String QUEUE_LHC_PT_TX = "QUEUE_LHC_PT_TX";

//    /**
//     * 六合彩 - 【特码,正特,六肖,正码1-6】队列
//     */
//    @Bean
//    Queue queueLhcTmZtLx() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_LHC_TM_ZT_LX, true, false, false, map); // true 表示持久化该队列
//    }
//
//    @Bean
//    Binding bindingLhcTmZtLx(Queue queueLhcTmZtLx, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueLhcTmZtLx).to(topicExchange).with(BINDING_LHC);
//    }
//
//    /**
//     * 六合彩 - 【正码,半波,尾数】队列
//     */
//    @Bean
//    Queue queueLhcZmBbWs() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_LHC_ZM_BB_WS, true, false, false, map); // true 表示持久化该队列
//    }
//
//    @Bean
//    Binding bindingLhcZmBbWs(Queue queueLhcZmBbWs, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueLhcZmBbWs).to(topicExchange).with(BINDING_LHC);
//    }
//
//    /**
//     * 六合彩 - 【连码,连肖,连尾】队列
//     */
//    @Bean
//    Queue queueLhcLmLxLw() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_LHC_LM_LX_LW, true, false, false, map); // true 表示持久化该队列
//    }
//
//    @Bean
//    Binding bindingLhcLmLxLw(Queue queueLhcLmLxLw, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueLhcLmLxLw).to(topicExchange).with(BINDING_LHC);
//    }
//
//    /**
//     * 六合彩 - 【不中,1-6龙虎,五行】队列
//     */
//    @Bean
//    Queue queueLhcBzLhWx() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_LHC_BZ_LH_WX, true, false, false, map); // true 表示持久化该队列
//    }
//
//    @Bean
//    Binding bindingLhcBzLhWx(Queue queueLhcBzLhWx, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueLhcBzLhWx).to(topicExchange).with(BINDING_LHC);
//    }
//
//    /**
//     * 六合彩 - 【平特,特肖】队列
//     */
//    @Bean
//    Queue queueLhcPtTx() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_LHC_PT_TX, true, false, false, map); // true 表示持久化该队列
//    }
//
//    @Bean
//    Binding bindingLhcPtTx(Queue queueLhcPtTx, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueLhcPtTx).to(topicExchange).with(BINDING_LHC);
//    }

    //##############################################################################################
    //###                                   幸运飞艇开采结算                                         ###
    //##############################################################################################

    // 幸运飞艇 绑定值
    public final static String BINDING_XYFT = "BINDING_XYFT";

    // 幸运飞艇两面 队列名
    public final static String QUEUE_XYFT_LM = "QUEUE_XYFT_LM";
    // 幸运飞艇猜名次猜前几 队列名
    public final static String QUEUE_XYFT_CMC_CQJ = "QUEUE_XYFT_CMC_CQJ";
    // 幸运飞艇单式猜前几 队列名
    public final static String QUEUE_XYFT_DS_CQJ = "QUEUE_XYFT_DS_CQJ";
    // 幸运飞艇定位胆 队列名
    public final static String QUEUE_XYFT_DWD = "QUEUE_XYFT_DWD";
    // 幸运飞艇冠亚和 队列名
    public final static String QUEUE_XYFT_GYH = "QUEUE_XYFT_GYH";
    // 幸运飞艇 - 更新【免费推荐】/【公式杀号】数据 队列名
    public final static String QUEUE_XYFT_UPDATE_DATA = "QUEUE_XYFT_UPDATE_DATA";

//    /**
//     * 幸运飞艇 - 【两面】队列
//     */
//    // 定义：幸运飞艇两面队列
//    @Bean
//    Queue queueXyftLm() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_XYFT_LM, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定 幸运飞艇两面队列
//    @Bean
//    Binding bindingXyftLm(Queue queueXyftLm, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueXyftLm).to(topicExchange).with(BINDING_XYFT);
//    }
//
//    /**
//     * 幸运飞艇 - 【猜名次猜前几】队列
//     */
//    // 定义：幸运飞艇猜名次猜前几队列
//    @Bean
//    Queue queueXyftCmcCqj() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_XYFT_CMC_CQJ, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定 幸运飞艇猜名次猜前几队列
//    @Bean
//    Binding bindingXyftCmcCqj(Queue queueXyftCmcCqj, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueXyftCmcCqj).to(topicExchange).with(BINDING_XYFT);
//    }
//
//    /**
//     * 幸运飞艇 - 【单式猜前几】队列
//     */
//    // 定义：幸运飞艇单式猜前几队列
//    @Bean
//    Queue queueXyftDsCqj() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_XYFT_DS_CQJ, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定 幸运飞艇猜名次猜前几队列
//    @Bean
//    Binding bindingXyftDsCqj(Queue queueXyftDsCqj, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueXyftDsCqj).to(topicExchange).with(BINDING_XYFT);
//    }
//
//    /**
//     * 幸运飞艇 - 【定位胆】队列
//     */
//    // 定义：幸运飞艇定位胆队列
//    @Bean
//    Queue queueXyftDwd() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_XYFT_DWD, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定 幸运飞艇定位胆队列
//    @Bean
//    Binding bindingXyftDwd(Queue queueXyftDwd, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueXyftDwd).to(topicExchange).with(BINDING_XYFT);
//    }
//
//    /**
//     * 幸运飞艇 - 【冠亚和】队列
//     */
//    // 定义：幸运飞艇冠亚和队列
//    @Bean
//    Queue queueXyftGyh() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_XYFT_GYH, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定 幸运飞艇冠亚和队列
//    @Bean
//    Binding bindingXyftGyh(Queue queueXyftGyh, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueXyftGyh).to(topicExchange).with(BINDING_XYFT);
//    }
//
//    /**
//     * 北京PK10 - 更新【免费推荐】/【公式杀号】数据 队列名
//     */
//    // 定义：时时彩组选选队列
//    @Bean
//    Queue queueXyftUpdateData() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_XYFT_UPDATE_DATA, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定：时时彩组选队列
//    @Bean
//    Binding bindingXyftUpdateData(Queue queueXyftUpdateData, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueXyftUpdateData).to(topicExchange).with(BINDING_XYFT);
//    }


    //##############################################################################################
    //###                                     WEB结果推送                                         ###
    //##############################################################################################

    // 结果推送 绑定值
    public final static String BINDING_RESULT_PUSH = "BINDING_RESULT_PUSH";

    // WEB结果推送 队列名
    public final static String QUEUE_WEB_RESULT_PUSH = "QUEUE_WEB_RESULT_PUSH";

//    /**
//     * 消息推送 - 【冠亚和】队列
//     */
//    // 定义：幸运飞艇冠亚和队列
//    @Bean
//    Queue queueWebSocketPush() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_WEB_RESULT_PUSH, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定 WebSocket推送队列
//    @Bean
//    Binding bindingWebSocketPush(Queue queueWebSocketPush, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueWebSocketPush).to(topicExchange).with(BINDING_RESULT_PUSH);
//    }
    
    
    //##############################################################################################
    //###                                  德州时时彩开采结算                                       ###
    //##############################################################################################

    // 德州时时彩 绑定值
    public final static String BINDING_SSC_JS= "BINDING_SSC_JS";

    // 德州时时彩【直选】 队列名
    public final static String QUEUE_SSC_JS_LM = "QUEUE_SSC_JS_LM";

    // 德州时时彩【组选】 队列名
    public final static String QUEUE_SSC_JS_DN = "QUEUE_SSC_JS_DN";

    // 德州时时彩【定位胆】 队列名
    public final static String QUEUE_SSC_JS_15 = "QUEUE_SSC_JS_15";

    // 德州时时彩【大小单双】 队列名
    public final static String QUEUE_SSC_JS_QZH = "QUEUE_SSC_JS_QZH";

    // 德州时时彩 - 更新【免费推荐】/【公式杀号】数据 队列名
//    public final static String QUEUE_SSC_JS_UPDATE_DATA = "QUEUE_SSC_JS_UPDATE_DATA";

//    /**
//     * 德州时时彩 - 【直选】队列
//     */
//    // 定义：时时彩直选队列
//    @Bean
//    Queue queueJsSscDe() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_SSC_JS_LM, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定 时时彩直选队列
//    @Bean
//    Binding bindingJsSscDe(Queue queueJsSscDe, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueJsSscDe).to(topicExchange).with(BINDING_SSC_JS);
//    }
//
//    /**
//     * 德州时时彩 - 【组选】队列
//     */
//    // 定义：时时彩组选选队列
//    @Bean
//    Queue queueJsSscGs() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_SSC_JS_DN, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定：时时彩组选队列
//    @Bean
//    Binding bindingJsSscGs(Queue queueJsSscGs, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueJsSscGs).to(topicExchange).with(BINDING_SSC_JS);
//    }
//
//    /**
//     * 德州时时彩 - 【定位胆】队列
//     */
//    // 定义：时时彩组选选队列
//    @Bean
//    Queue queueJsSscDw() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_SSC_JS_15, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定：时时彩组选队列
//    @Bean
//    Binding bindingJsSscDw(Queue queueJsSscDw, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueJsSscDw).to(topicExchange).with(BINDING_SSC_JS);
//    }
//
//    /**
//     * 德州时时彩 - 【大下单双】队列
//     */
//    // 定义：时时彩组选选队列
//    @Bean
//    Queue queueJsSscBs() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_SSC_JS_QZH, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定：时时彩组选队列
//    @Bean
//    Binding bindingJsSscBs(Queue queueJsSscBs, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueJsSscBs).to(topicExchange).with(BINDING_SSC_JS);
//    }
//
//    /**
//     * 德州时时彩 - 更新【免费推荐】/【公式杀号】数据 队列名·
//     */
//    // 定义：时时彩组选选队列
////    @Bean
////    Queue queueJsSscUpdateData() {
////        Map<String, Object> map = new HashMap<>();
////        // 设置该Queue的死信的信箱
////        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
////        // 设置死信routingKey
////        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
////        return new Queue(QUEUE_SSC_JS_UPDATE_DATA, true, false, false, map); // true 表示持久化该队列
////    }
//
//    // 绑定：时时彩组选队列
////    @Bean
////    Binding bindingJsSscUpdateData(Queue queueJsSscUpdateData, TopicExchange topicExchange) {
////        return BindingBuilder.bind(queueJsSscUpdateData).to(topicExchange).with(BINDING_SSC_JS);
////    }
    
    
    //##############################################################################################
    //###                                  五分时时彩开采结算                                       ###
    //##############################################################################################

    // 五分时时彩 绑定值
    public final static String BINDING_SSC_FIVE = "BINDING_SSC_FIVE";

    // 五分时时彩【直选】 队列名
    public final static String QUEUE_SSC_FIVE_LM = "QUEUE_SSC_FIVE_LM";

    // 快乐牛牛  队列名
    public final static String QUEUE_KLNN_FIVE = "QUEUE_KLNN_FIVE";

    // 五分时时彩【组选】 队列名
    public final static String QUEUE_SSC_FIVE_DN = "QUEUE_SSC_FIVE_DN";

    // 五分时时彩【定位胆】 队列名
    public final static String QUEUE_SSC_FIVE_15 = "QUEUE_SSC_FIVE_15";

    // 五分时时彩【大小单双】 队列名
    public final static String QUEUE_SSC_FIVE_QZH = "QUEUE_SSC_FIVE_QZH";

    // 五分时时彩 - 更新【免费推荐】/【公式杀号】数据 队列名
    public final static String QUEUE_SSC_FIVE_UPDATE_DATA = "QUEUE_SSC_FIVE_UPDATE_DATA";

//    /**
//     * 五分时时彩 - 【直选】队列
//     */
//    // 定义：时时彩直选队列
//    @Bean
//    Queue queueFiveSscDe() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_SSC_FIVE_LM, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定 时时彩直选队列
//    @Bean
//    Binding bindingFiveSscDe(Queue queueFiveSscDe, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueFiveSscDe).to(topicExchange).with(BINDING_SSC_FIVE);
//    }
//
//    /**
//     * 快乐牛牛 - 队列
//     */
//    // 定义：快乐牛牛队列
//    @Bean
//    Queue queueKlSscDe() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_KLNN_FIVE, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定 快乐牛牛队列
//    @Bean
//    Binding bindingKlSscDe(Queue queueKlSscDe, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueKlSscDe).to(topicExchange).with(BINDING_SSC_FIVE);
//    }
//
//    /**
//     * 五分时时彩 - 【组选】队列
//     */
//    // 定义：时时彩组选选队列
//    @Bean
//    Queue queueFiveSscGs() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_SSC_FIVE_DN, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定：时时彩组选队列
//    @Bean
//    Binding bindingFiveSscGs(Queue queueFiveSscGs, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueFiveSscGs).to(topicExchange).with(BINDING_SSC_FIVE);
//    }
//
//    /**
//     * 五分时时彩 - 【定位胆】队列
//     */
//    // 定义：时时彩组选选队列
//    @Bean
//    Queue queueFiveSscDw() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_SSC_FIVE_15, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定：时时彩组选队列
//    @Bean
//    Binding bindingFiveSscDw(Queue queueFiveSscDw, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueFiveSscDw).to(topicExchange).with(BINDING_SSC_FIVE);
//    }
//
//    /**
//     * 五分时时彩 - 【大下单双】队列
//     */
//    // 定义：时时彩组选选队列
//    @Bean
//    Queue queueFiveSscBs() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_SSC_FIVE_QZH, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定：时时彩组选队列
//    @Bean
//    Binding bindingFiveSscBs(Queue queueFiveSscBs, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueFiveSscBs).to(topicExchange).with(BINDING_SSC_FIVE);
//    }
//
//    /**
//     * 五分时时彩 - 更新【免费推荐】/【公式杀号】数据 队列名·
//     */
//    // 定义：时时彩组选选队列
//    @Bean
//    Queue queueFiveSscUpdateData() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_SSC_FIVE_UPDATE_DATA, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定：时时彩组选队列
//    @Bean
//    Binding bindingFiveSscUpdateData(Queue queueFiveSscUpdateData, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueFiveSscUpdateData).to(topicExchange).with(BINDING_SSC_FIVE);
//    }


    //##############################################################################################
    //###                                  十分时时彩开采结算                                       ###
    //##############################################################################################

    // 十分时时彩 绑定值
    public final static String BINDING_SSC_TEN = "BINDING_SSC_TEN";

    // 十分时时彩【直选】 队列名
    public final static String QUEUE_SSC_TEN_LM = "QUEUE_SSC_TEN_LM";

    // 十分时时彩【组选】 队列名
    public final static String QUEUE_SSC_TEN_DN = "QUEUE_SSC_TEN_DN";

    // 十分时时彩【定位胆】 队列名
    public final static String QUEUE_SSC_TEN_15 = "QUEUE_SSC_TEN_15";

    // 十分时时彩【大小单双】 队列名
    public final static String QUEUE_SSC_TEN_QZH = "QUEUE_SSC_TEN_QZH";

    // 十分时时彩 - 更新【免费推荐】/【公式杀号】数据 队列名
    public final static String QUEUE_SSC_TEN_UPDATE_DATA = "QUEUE_SSC_TEN_UPDATE_DATA";

//    /**
//     * 十分时时彩 - 【直选】队列
//     */
//    // 定义：时时彩直选队列
//    @Bean
//    Queue queueTenSscDe() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_SSC_TEN_LM, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定 时时彩直选队列
//    @Bean
//    Binding bindingTenSscDe(Queue queueTenSscDe, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueTenSscDe).to(topicExchange).with(BINDING_SSC_TEN);
//    }
//
//    /**
//     * 十分时时彩 - 【组选】队列
//     */
//    // 定义：时时彩组选选队列
//    @Bean
//    Queue queueTenSscGs() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_SSC_TEN_DN, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定：时时彩组选队列
//    @Bean
//    Binding bindingTenSscGs(Queue queueTenSscGs, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueTenSscGs).to(topicExchange).with(BINDING_SSC_TEN);
//    }
//
//    /**
//     * 十分时时彩 - 【定位胆】队列
//     */
//    // 定义：时时彩组选选队列
//    @Bean
//    Queue queueTenSscDw() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_SSC_TEN_15, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定：时时彩组选队列
//    @Bean
//    Binding bindingTenSscDw(Queue queueTenSscDw, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueTenSscDw).to(topicExchange).with(BINDING_SSC_TEN);
//    }
//
//    /**
//     * 十分时时彩 - 【大下单双】队列
//     */
//    // 定义：时时彩组选选队列
//    @Bean
//    Queue queueTenSscBs() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_SSC_TEN_QZH, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定：时时彩组选队列
//    @Bean
//    Binding bindingTenSscBs(Queue queueTenSscBs, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueTenSscBs).to(topicExchange).with(BINDING_SSC_TEN);
//    }
//
//    /**
//     * 十分时时彩 - 更新【免费推荐】/【公式杀号】数据 队列名·
//     */
//    // 定义：时时彩组选选队列
//    @Bean
//    Queue queueTenSscUpdateData() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_SSC_TEN_UPDATE_DATA, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定：时时彩组选队列
//    @Bean
//    Binding bindingTenSscUpdateData(Queue queueTenSscUpdateData, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueTenSscUpdateData).to(topicExchange).with(BINDING_SSC_TEN);
//    }

    //##############################################################################################
    //###                                  体彩排列三开采结算                                       ###
    //##############################################################################################

    //  体彩排列三 绑定值
    public final static String BINDING_TCPLS = "BINDING_TCPLS";

    //  体彩排列三两面 队列名
    public final static String QUEUE_TCPLS_ZX = "QUEUE_TCPLS_ZX";

//    /**
//     *  体彩排列三 - 【两面】队列
//     */
//    // 定义： 体彩排列三直选组选队列
//    @Bean
//    Queue queueTcPlsZx() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_TCPLS_ZX, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定  体彩排列三直选组选队列
//    @Bean
//    Binding bindingTcPlsZx(Queue queueTcPlsZx, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueTcPlsZx).to(topicExchange).with(BINDING_TCPLS);
//    }
    
    
    //##############################################################################################
    //###                                  体彩排列五开采结算                                       ###
    //##############################################################################################

    //  体彩排列五 绑定值
    public final static String BINDING_TCPLW = "BINDING_TCPLW";
    //  体彩排列五直选组选 队列名
    public final static String QUEUE_TCPLW_ZX = "QUEUE_TCPLW_ZX";
    //  体彩排列五定位胆 队列名
    public final static String QUEUE_TCPLW_DWD = "QUEUE_TCPLW_DWD";
    //体彩排列五两面 队列名
    public final static String QUEUE_TCPLW_LM = "QUEUE_TCPLW_LM";
    
//    /**
//     *  体彩排列五 - 【直选组选】队列
//     */
//    // 定义： 体彩排列五直选组选队列
//    @Bean
//    Queue queueTcPlwZx() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_TCPLW_ZX, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定  体彩排列五两面队列
//    @Bean
//    Binding bindingTcPlwZx(Queue queueTcPlwZx, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueTcPlwZx).to(topicExchange).with(BINDING_TCPLW);
//    }
//
//    /**
//     *  体彩排列五 - 【两面】队列
//     */
//    // 定义： 体彩排列五两面队列
//    @Bean
//    Queue queueTcPlwLm() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_TCPLW_LM, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定  体彩排列五两面队列
//    @Bean
//    Binding bindingTcPlwLm(Queue queueTcPlwLm, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueTcPlwLm).to(topicExchange).with(BINDING_TCPLW);
//    }
//
//    /**
//     *  体彩排列五 - 【定位胆】队列
//     */
//    // 定义： 体彩排列五定位胆队列
//    @Bean
//    Queue queueTcPlwDwd() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_TCPLW_DWD, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定  体彩排列五定位胆队列
//    @Bean
//    Binding bindingTcPlwDwd(Queue queueTcPlwDwd, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueTcPlwDwd).to(topicExchange).with(BINDING_TCPLW);
//    }

    
    //##############################################################################################
    //###                                  体彩大乐透开采结算                                       ###
    //##############################################################################################

    //  体彩大乐透 绑定值
    public final static String BINDING_TCDLT = "BINDING_TCDLT";

    //  体彩大乐透两面 队列名
    public final static String QUEUE_TCDLT_LM = "QUEUE_TCDLT_LM";
    //  体彩大乐透猜名次猜前几 队列名
    public final static String QUEUE_TCDLT_CMC_CQJ = "QUEUE_TCDLT_CMC_CQJ";
    //  体彩大乐透单式猜前几 队列名
    public final static String QUEUE_TCDLT_DS_CQJ = "QUEUE_TCDLT_DS_CQJ";
    //  体彩大乐透定位胆 队列名
    public final static String QUEUE_TCDLT_DWD = "QUEUE_TCDLT_DWD";
    //  体彩大乐透冠亚和 队列名
    public final static String QUEUE_TCDLT_GYH = "QUEUE_TCDLT_GYH";
    //  体彩大乐透 - 更新【免费推荐】/【公式杀号】数据 队列名
    public final static String QUEUE_TCDLT_UPDATE_DATA = "QUEUE_TCDLT_UPDATE_DATA";

//    /**
//     *  体彩大乐透 - 【两面】队列
//     */
//    // 定义： 体彩大乐透两面队列
//    @Bean
//    Queue queueTcDltLm() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_TCDLT_LM, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定  体彩大乐透两面队列
//    @Bean
//    Binding bindingTcDltLm(Queue queueTcDltLm, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueTcDltLm).to(topicExchange).with(BINDING_TCDLT);
//    }
//
//    /**
//     *  体彩大乐透 - 【猜名次猜前几】队列
//     */
//    // 定义： 体彩大乐透猜名次猜前几队列
//    @Bean
//    Queue queueTcDltCmcCqj() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_TCDLT_CMC_CQJ, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定  体彩大乐透猜名次猜前几队列
//    @Bean
//    Binding bindingTcDltCmcCqj(Queue queueTcDltCmcCqj, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueTcDltCmcCqj).to(topicExchange).with(BINDING_TCDLT);
//    }
//
//    /**
//     *  体彩大乐透 - 【单式猜前几】队列
//     */
//    // 定义： 体彩大乐透单式猜前几队列
//    @Bean
//    Queue queueTcDltDsCqj() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_TCDLT_DS_CQJ, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定  体彩大乐透猜名次猜前几队列
//    @Bean
//    Binding bindingTcDltDsCqj(Queue queueTcDltDsCqj, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueTcDltDsCqj).to(topicExchange).with(BINDING_TCDLT);
//    }
//
//    /**
//     *  体彩大乐透 - 【定位胆】队列
//     */
//    // 定义： 体彩大乐透定位胆队列
//    @Bean
//    Queue queueTcDltDwd() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_TCDLT_DWD, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定  体彩大乐透定位胆队列
//    @Bean
//    Binding bindingTcDltDwd(Queue queueTcDltDwd, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueTcDltDwd).to(topicExchange).with(BINDING_TCDLT);
//    }
//
//    /**
//     *  体彩大乐透 - 【冠亚和】队列
//     */
//    // 定义： 体彩大乐透冠亚和队列
//    @Bean
//    Queue queueTcDltGyh() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_TCDLT_GYH, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定  体彩大乐透冠亚和队列
//    @Bean
//    Binding bindingTcDltGyh(Queue queueTcDltGyh, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueTcDltGyh).to(topicExchange).with(BINDING_TCDLT);
//    }
//
//    /**
//     * 北京PK10 - 更新【免费推荐】/【公式杀号】数据 队列名
//     */
//    // 定义：时时彩组选选队列
//    @Bean
//    Queue queueTcDltUpdateData() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_TCDLT_UPDATE_DATA, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定：时时彩组选队列
//    @Bean
//    Binding bindingTcDltUpdateData(Queue queueTcDltUpdateData, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueTcDltUpdateData).to(topicExchange).with(BINDING_TCDLT);
//    }
   
    //##############################################################################################
    //###                                  体彩大乐透开采结算                                       ###
    //##############################################################################################

    //  体彩大乐透 绑定值
    public final static String BINDING_TCSSQ = "BINDING_TCSSQ";

    //  体彩大乐透两面 队列名
    public final static String QUEUE_TCSSQ_LM = "QUEUE_TCSSQ_LM";
    //  体彩大乐透猜名次猜前几 队列名
    public final static String QUEUE_TCSSQ_CMC_CQJ = "QUEUE_TCSSQ_CMC_CQJ";
    //  体彩大乐透单式猜前几 队列名
    public final static String QUEUE_TCSSQ_DS_CQJ = "QUEUE_TCSSQ_DS_CQJ";
    //  体彩大乐透定位胆 队列名
    public final static String QUEUE_TCSSQ_DWD = "QUEUE_TCSSQ_DWD";
    //  体彩大乐透冠亚和 队列名
    public final static String QUEUE_TCSSQ_GYH = "QUEUE_TCSSQ_GYH";
    //  体彩大乐透 - 更新【免费推荐】/【公式杀号】数据 队列名
    public final static String QUEUE_TCSSQ_UPDATE_DATA = "QUEUE_TCSSQ_UPDATE_DATA";

//    /**
//     *  体彩大乐透 - 【两面】队列
//     */
//    // 定义： 体彩大乐透两面队列
//    @Bean
//    Queue queueTcSsqLm() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_TCSSQ_LM, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定  体彩大乐透两面队列
//    @Bean
//    Binding bindingTcSsqLm(Queue queueTcSsqLm, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueTcSsqLm).to(topicExchange).with(BINDING_TCSSQ);
//    }
//
//    /**
//     *  体彩大乐透 - 【猜名次猜前几】队列
//     */
//    // 定义： 体彩大乐透猜名次猜前几队列
//    @Bean
//    Queue queueTcSsqCmcCqj() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_TCSSQ_CMC_CQJ, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定  体彩大乐透猜名次猜前几队列
//    @Bean
//    Binding bindingTcSsqCmcCqj(Queue queueTcSsqCmcCqj, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueTcSsqCmcCqj).to(topicExchange).with(BINDING_TCSSQ);
//    }
//
//    /**
//     *  体彩大乐透 - 【单式猜前几】队列
//     */
//    // 定义： 体彩大乐透单式猜前几队列
//    @Bean
//    Queue queueTcSsqDsCqj() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_TCSSQ_DS_CQJ, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定  体彩大乐透猜名次猜前几队列
//    @Bean
//    Binding bindingTcSsqDsCqj(Queue queueTcSsqDsCqj, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueTcSsqDsCqj).to(topicExchange).with(BINDING_TCSSQ);
//    }
//
//    /**
//     *  体彩大乐透 - 【定位胆】队列
//     */
//    // 定义： 体彩大乐透定位胆队列
//    @Bean
//    Queue queueTcSsqDwd() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_TCSSQ_DWD, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定  体彩大乐透定位胆队列
//    @Bean
//    Binding bindingTcSsqDwd(Queue queueTcSsqDwd, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueTcSsqDwd).to(topicExchange).with(BINDING_TCSSQ);
//    }
//
//    /**
//     *  体彩大乐透 - 【冠亚和】队列
//     */
//    // 定义： 体彩大乐透冠亚和队列
//    @Bean
//    Queue queueTcSsqGyh() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_TCSSQ_GYH, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定  体彩大乐透冠亚和队列
//    @Bean
//    Binding bindingTcSsqGyh(Queue queueTcSsqGyh, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueTcSsqGyh).to(topicExchange).with(BINDING_TCSSQ);
//    }
//
//    /**
//     * 北京PK10 - 更新【免费推荐】/【公式杀号】数据 队列名
//     */
//    // 定义：时时彩组选选队列
//    @Bean
//    Queue queueTcSsqUpdateData() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_TCSSQ_UPDATE_DATA, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定：时时彩组选队列
//    @Bean
//    Binding bindingTcSsqUpdateData(Queue queueTcSsqUpdateData, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueTcSsqUpdateData).to(topicExchange).with(BINDING_TCSSQ);
//    }
    
    //##############################################################################################
    //###                                  福彩双色球开彩结算                                       ###
    //##############################################################################################
    // 福彩双色球 队列名 
    public final static String QUEUE_FC_SSQ = "QUEUE_FC_SSQ";
    
    // 体彩大乐透 队列名
    public final static String QUEUE_TC_DLT = "QUEUE_TC_DLT";
    
    // 福彩七乐彩
    public final static String QUEUE_FC_7LC = "QUEUE_FC_7LC";
    
    
    //##############################################################################################
    //###                                 番摊开彩结算                                       ###
    //##############################################################################################
    // 德州pk10 番摊 队列名
    public final static String QUEUE_JSBJPKS_FT = "QUEUE_JS_FT";
    // 幸运飞艇番摊 队列名
    public final static String QUEUE_XYFT_FT = "QUEUE_XYFT_FT";
    // 德州时时彩【番摊】 队列名
    public final static String QUEUE_SSC_JS_FT = "QUEUE_SSC_JS_FT";


   //订单绑定值
    public final static String BINDING_ORDER = "BINDING_ORDER";

    // 订单队列名
    public final static String QUEUE_ORDER = "QUEUE_ORDER";


    
//    /**
//     * 德州pk10 - 番摊 队列
//     */
//    // 定义：德州pk10番摊队列
//    @Bean
//    Queue queueJsbjpksFt () {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_JSBJPKS_FT, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定 德州pk10番摊队列
//    @Bean
//    Binding bindingJsbjpksFt(Queue queueJsbjpksFt, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueJsbjpksFt).to(topicExchange).with(BINDING_JSBJPKS);
//    }
//
//
//
//    /**
//     * 幸运飞艇 - 【番摊】队列
//     */
//    // 定义：幸运飞艇番摊队列
//    @Bean
//    Queue queueXyftFt() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_XYFT_FT, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定 幸运飞艇番摊队列
//    @Bean
//    Binding bindingXyftFt(Queue queueXyftFt, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueXyftFt).to(topicExchange).with(BINDING_XYFT);
//    }
//
//
//    /**
//     * 德州时时彩 - 【番摊】队列
//     */
//    // 定义：时时彩番摊队列
//    @Bean
//    Queue queueJsSscFT() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_SSC_JS_FT, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定 时时彩番摊队列
//    @Bean
//    Binding bindingJsSscFT(Queue queueJsSscFT, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueJsSscFT).to(topicExchange).with(BINDING_SSC_JS);
//    }
//
//
//    /**
//     * 重庆时时彩 - 【直选】队列
//     */
//    // 定义：时时彩直选队列
//    @Bean
//    Queue queueOrder() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        return new Queue(QUEUE_ORDER, true, false, false, map); // true 表示持久化该队列
//    }
//
//    // 绑定 订单队列
//    @Bean
//    Binding bindingOrder(Queue queueOrder, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueOrder).to(topicExchange).with(BINDING_ORDER);
//    }

    //##############################################################################################
    //###                                  体彩七星彩开采结算                                       ###
    //##############################################################################################
    public static final String BINDING_TC_7XC = "BINDING_TC_7XC";

    // 海南7星彩队列名
    public final static String QUEUE_HN_7XC = "QUEUE_HN_7XC";

//    /**
//     *  海南7星彩 队列
//     */
//    @Bean
//    Queue queueHN7XC() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        // true 表示持久化该队列
//        return new Queue(QUEUE_HN_7XC, true, false, false, map);
//    }
//    @Bean
//    Binding bindingHN7XC(Queue queueHN7XC, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueHN7XC).to(topicExchange).with(BINDING_TC_7XC);
//    }

    //##############################################################################################
    //###                                  福彩3D开采结算                                       ###
    //##############################################################################################
    public static final String BINDING_FC_3D = "BINDING_FC_3D";

    // 福彩3D队列名
    public final static String QUEUE_FC_3D = "QUEUE_FC_3D";

//    /**
//     *  福彩3D 队列
//     */
//    @Bean
//    Queue queueFC3D() {
//        Map<String, Object> map = new HashMap<>();
//        // 设置该Queue的死信的信箱
//        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//        // 设置死信routingKey
//        map.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
//        // true 表示持久化该队列
//        return new Queue(QUEUE_FC_3D, true, false, false, map);
//    }
//    @Bean
//    Binding bindingFC3D(Queue queueFC3D, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queueFC3D).to(topicExchange).with(BINDING_FC_3D);
//    }
}