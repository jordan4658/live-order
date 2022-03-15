package com.caipiao.live.order.service.order.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.constant.RedisKeys;
import com.caipiao.live.common.enums.StatusCode;
import com.caipiao.live.common.enums.lottery.CaipiaoTypeEnum;
import com.caipiao.live.common.enums.lottery.LotteryTableNameEnum;

import com.caipiao.live.common.model.common.ResultInfo;
import com.caipiao.live.common.model.dto.order.*;
import com.caipiao.live.common.model.vo.lottery.LotteryCateidVo;
import com.caipiao.live.common.mybatis.entity.*;
import com.caipiao.live.common.mybatis.mapper.*;
import com.caipiao.live.common.mybatis.mapperbean.BjpksBeanMapper;
import com.caipiao.live.common.mybatis.mapperbean.OrderMapper;
import com.caipiao.live.common.mybatis.mapperext.lottery.LotteryMapperExt;
import com.caipiao.live.common.mybatis.mapperext.order.OrderBetRecordMapperExt;
import com.caipiao.live.common.mybatis.mapperext.order.OrderMapperExt;
import com.caipiao.live.common.util.*;
import com.caipiao.live.common.util.redis.BasicRedisClient;
import com.caipiao.live.common.util.redis.RedisBusinessUtil;
import com.caipiao.live.common.util.redis.RedisLock;
import com.caipiao.live.order.config.ActiveMQConfig;
import com.caipiao.live.order.rest.KillConfigAdminReadRest;
import com.caipiao.live.order.rest.LotteryPlaySettingReadRest;
import com.caipiao.live.order.rest.LotterySgWriteRest;
import com.caipiao.live.order.service.bet.*;
import com.caipiao.live.order.service.bet.impl.BetLhcServiceImpl;
import com.caipiao.live.order.service.lottery.LotteryPlayOddsWriteService;
import com.caipiao.live.order.service.order.OrderAppendWriteService;
import com.caipiao.live.order.service.order.OrderEventSentService;
import com.caipiao.live.order.service.order.OrderWriteService;
import org.apache.activemq.command.ActiveMQQueue;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.jms.Destination;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import com.caipiao.live.order.model.dto.OrderDTO;
import static com.caipiao.live.common.util.ViewUtil.getTradeOffAmount;

@Service
public class OrderWriteServiceImpl implements OrderWriteService {

    private static final Logger logger = LoggerFactory.getLogger(OrderWriteServiceImpl.class);

    private static final String ORDER_KEY = "ORDER_";
    private static final String ORDER_BET_KEY = "ORDER_BET_";

    @Autowired
    @Lazy
    private OrderRecordMapper orderRecordMapper;
    @Autowired
    @Lazy
    private OrderBetRecordMapper orderBetRecordMapper;
    @Autowired
    @Lazy
    private OrderMapper OrderMapper;
    @Autowired
    @Lazy
    private CqsscLotterySgMapper cqsscLotterySgMapper;
    @Autowired
    @Lazy
    private XjsscLotterySgMapper xjsscLotterySgMapper;
    @Autowired
    @Lazy
    private TxffcLotterySgMapper txffcLotterySgMapper;
    @Autowired
    @Lazy
    private PceggLotterySgMapper pceggLotterySgMapper;
    @Autowired
    @Lazy
    private BjpksLotterySgMapper bjpksLotterySgMapper;
    @Autowired
    @Lazy
    private XyftLotterySgMapper xyftLotterySgMapper;
    @Autowired
    @Lazy
    private LhcHandicapMapper lhcHandicapMapper;
    @Autowired
    @Lazy
    private RedissonClient redissonClient;
    @Autowired
    @Lazy
    private OrderAppendWriteService orderAppendWriteService;
    @Autowired
    @Lazy
    private LotterySgWriteRest lotterySgWriteRest;
    @Autowired
    @Lazy
    private RedisTemplate redisTemplate;
    @Autowired
    @Lazy
    private BjpksBeanMapper bjpksBeanMapper;
    @Autowired
    @Lazy
    private JmsMessagingTemplate jmsMessagingTemplate;
    @Autowired
    @Lazy
    private LotteryPlayOddsWriteService lotteryPlayOddsService;
    @Autowired
    @Lazy
    private BetCommonService betCommonService;
    @Autowired
    @Lazy
    private OrderEventSentService orderEventSentService;
    @Autowired
    private OrderMapperExt orderMapperExt;
    @Autowired
    private BetRestrictMapper betRestrictMapper;
    @Autowired
    private BetSscbmService betSscbmService;
    @Autowired
    @Lazy
    private BetLhcService betLhcService;
    @Autowired
    private BetBjpksService betBjpksService;
    @Autowired
    private BetXyftService betXyftService;
    @Autowired
    private BetPceggService betPceggService;
    @Autowired
    private BetActAzService betActAzService;
    @Autowired
    private BetSscAzService betSscAzService;

    @Autowired
    private BetF1AzService betF1AzService;
    @Autowired
    private OrderAppendRecordMapper orderAppendRecordMapper;
    @Autowired
    private LhcLotterySgMapper lhcLotterySgMapper;
    @Autowired
    private BasicRedisClient basicRedisClient;
    @Autowired
    private TjsscLotterySgMapper tjsscLotterySgMapper;
    @Autowired
    private TensscLotterySgMapper tensscLotterySgMapper;
    @Autowired
    private JssscLotterySgMapper jssscLotterySgMapper;
    @Autowired
    private OnelhcLotterySgMapper onelhcLotterySgMapper;
    @Autowired
    private FivelhcLotterySgMapper fivelhcLotterySgMapper;
    @Autowired
    private AmlhcLotterySgMapper amlhcLotterySgMapper;
    @Autowired
    private TenbjpksLotterySgMapper tenbjpksLotterySgMapper;
    @Autowired
    private FivebjpksLotterySgMapper fivebjpksLotterySgMapper;
    @Autowired
    private AusactLotterySgMapper ausactLotterySgMapper;
    @Autowired
    private AussscLotterySgMapper aussscLotterySgMapper;
    @Autowired
    private AzksLotterySgMapper azksLotterySgMapper;
    @Autowired
    private DzksLotterySgMapper dzksLotterySgMapper;
    @Autowired
    private DzpceggLotterySgMapper dzpceggLotterySgMapper;
    @Autowired
    private DzxyftLotterySgMapper dzxyftLotterySgMapper;
    @Autowired
    private XjplhcLotterySgMapper xjplhcLotterySgMapper;
    @Autowired
    private KillConfigAdminReadRest killConfigReadRest;
    @Autowired
    private LotteryMapperExt lotteryMapperExt;
    @Autowired
    private MemUserMapper memUserMapper;
    @Autowired
    private MemWalletMapper memWalletMapper;
    @Autowired
    private LotteryPlaySettingReadRest lotteryPlaySettingReadRest;
    @Autowired
    private OrderBetRecordMapperExt orderBetRecordMapperExt;
    @Autowired
    private BetKsService betksService;
    @Autowired
    private BetDzpceggService betDzpceggService;
    @Autowired
    private BetDzxyftService betDzxyftService;
    @Autowired
    private BetNewLhcService betNewLhcService;



    // 跟单分享默认
    private static final Integer KEY_FOUR = 4;
    //直播间跟单默认
    private static final Integer KEY_FIVES = 7;
    private final String WAIT_SETTLEMENT_ORDER_PREFIX = "order_settlement_order_id_";

    @Override
    public List<OrderRecord> selectOrders(Integer lotteryId, String issue, String status) {
        List<OrderRecord> list = new ArrayList<>();
        // 校验参数
        if (lotteryId == null || StringUtils.isBlank(issue) || StringUtils.isBlank(status)) {
            return list;
        }

//        // TODO 暂时去掉缓存（用于测试）
        OrderRecordExample orderExample = new OrderRecordExample();
        OrderRecordExample.Criteria orderCriteria = orderExample.createCriteria();
        orderCriteria.andIssueEqualTo(issue);
        orderCriteria.andLotteryIdEqualTo(lotteryId);
        orderCriteria.andStatusEqualTo(status);
        orderCriteria.andIsDeleteEqualTo(false);
        list = orderRecordMapper.selectByExample(orderExample);

//        // TODO 暂时去掉缓存（用于测试）end

        // 非测试start
//        // 从缓存中获取相应订单信息
//        if (redisTemplate.hasKey(ORDER_KEY + lotteryId + "_" + issue)) {
//            list = (List<OrderRecord>) redisTemplate.opsForValue().get(ORDER_KEY + lotteryId + "_" + issue);
//        }
//        // 从数据库获取订单信息
//        if (CollectionUtils.isEmpty(list)) {
//            OrderRecordExample orderExample = new OrderRecordExample();
//            OrderRecordExample.Criteria orderCriteria = orderExample.createCriteria();
//            orderCriteria.andIssueEqualTo(issue);
//            orderCriteria.andLotteryIdEqualTo(lotteryId);
//            orderCriteria.andStatusEqualTo(status);
//            orderCriteria.andIsDeleteEqualTo(false);
//            list = orderRecordMapper.selectByExample(orderExample);
//            redisTemplate.opsForValue().set(ORDER_KEY + lotteryId + "_" + issue, list, 2, TimeUnit.MINUTES);
//        }
        // 非测试end
        return list;
    }

    @Override
    public List<OrderRecord> selectOrdersPage(Integer lotteryId, String issue, String status, int pageNo) {
        List<OrderRecord> list = new ArrayList<>();
        // 校验参数
        if (lotteryId == null || StringUtils.isBlank(issue) || StringUtils.isBlank(status)) {
            return list;
        }

        OrderRecordExample orderExample = new OrderRecordExample();
        OrderRecordExample.Criteria orderCriteria = orderExample.createCriteria();
        orderCriteria.andIssueEqualTo(issue);
        orderCriteria.andLotteryIdEqualTo(lotteryId);
        orderCriteria.andStatusEqualTo(status);
        orderCriteria.andIsDeleteEqualTo(false);
        orderExample.setOffset((pageNo - 1) * Constants.CLEARNUM);
        orderExample.setLimit(Constants.CLEARNUM);
        list = orderRecordMapper.selectByExample(orderExample);

        return list;
    }

    @Override
    public int selectOrdersCount(Integer lotteryId, String issue, String status) {

        // 校验参数
        if (lotteryId == null || StringUtils.isBlank(issue) || StringUtils.isBlank(status)) {
            return 0;
        }

        OrderRecordExample orderExample = new OrderRecordExample();
        OrderRecordExample.Criteria orderCriteria = orderExample.createCriteria();
        orderCriteria.andIssueEqualTo(issue);
        orderCriteria.andLotteryIdEqualTo(lotteryId);
        orderCriteria.andStatusEqualTo(status);
        orderCriteria.andIsDeleteEqualTo(false);
        int count = orderRecordMapper.countByExample(orderExample);

        return count;
    }

    @Override
    public List<OrderBetRecord> selectOrderBets(List<Integer> orderIds, List<Integer> playIds, String status) {
        OrderBetRecordExample betExample = new OrderBetRecordExample();
        OrderBetRecordExample.Criteria betCriteria = betExample.createCriteria();
        betCriteria.andOrderIdIn(orderIds);
        betCriteria.andPlayIdIn(playIds);
        betCriteria.andIsDeleteEqualTo(false);
        betCriteria.andTbStatusEqualTo(status);
        return orderBetRecordMapper.selectByExample(betExample);
    }

    @Override
    public List<OrderBetRecord> selectOrderBets(List<Integer> orderIds, Integer playId, String status) {
        OrderBetRecordExample betExample = new OrderBetRecordExample();
        OrderBetRecordExample.Criteria betCriteria = betExample.createCriteria();
        betCriteria.andOrderIdIn(orderIds);
        betCriteria.andPlayIdEqualTo(playId);
        betCriteria.andIsDeleteEqualTo(false);
        betCriteria.andTbStatusEqualTo(status);

        return orderBetRecordMapper.selectByExample(betExample);
    }

    @Override
    public List<OrderBetRecord> selectOrderBetList(String issue, String lotteryId, List<Integer> playIds,
                                                   String status, String type) {
        if (CollectionUtils.isEmpty(playIds)) {
            return null;
        }
        List<OrderBetRecord> list = new ArrayList<>();
        String ids = "";
        for (Integer pid : playIds) {
            ids += pid + ",";
        }
        ids = ids.substring(0, ids.length() - 1);
        // ids="("+ids+")";

        String key = RedisKeys.ORDER_CLEAR + issue + "_" + lotteryId + "_" + ids + "_" + type;
        List<OrderBetRecord> clearlist = new ArrayList<>();
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lock");
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(1000, 100, TimeUnit.SECONDS);
            // 判断是否获取到锁
            if (bool) {
//				Integer id = redisTemplate.opsForValue().get(key) == null ? 0
//						: (Integer) redisTemplate.opsForValue().get(key);              //不需要用这个条件，本来有issue 做条件就够了， 如果加上的话， 六合彩会走一次假结算和真结算， 代码走两遍有问题
                Integer id = 0;
                list = orderMapperExt.selectOrderBetList(issue, playIds, lotteryId, status, id, 0, Constants.CLEARNUM);
                logger.info("查询订单数据：issue[{}],playIds[{}],lotteryId[{}],status[{}],id[{}],size[{}]", issue, playIds,
                        lotteryId, status, id, list.size());
                if (!CollectionUtils.isEmpty(list)) {
                    List<OrderBetRecord> tmpList = new ArrayList<>();
                    // 过滤重复记录
                    list.forEach(l -> {
                        String settelementKey = WAIT_SETTLEMENT_ORDER_PREFIX + l.getId();
                        if (!redisTemplate.hasKey(settelementKey)) {
                            tmpList.add(l);
                            redisTemplate.opsForValue().set(key, l.getId() + "", 5, TimeUnit.MINUTES);
                        }
                    });
                    if (list.size() != tmpList.size()) {
                        list = tmpList;
                    }
                    OrderBetRecord o = list.get(list.size() - 1);
//					int maxid = o.getId();
//					redisTemplate.opsForValue().set(key, maxid, 10, TimeUnit.MINUTES);
                    // 玩法设计...共用了一个playID....
                    if (String.valueOf(playIds.get(0)).replace(lotteryId, "")
                            .equals(BetLhcServiceImpl.PLAY_ID_ZM_ZMA)) {

                        if ("OnePlayMany".equals(type)) {
                            for (OrderBetRecord clearo : list) {
                                if (clearo.getBetNumber().contains("两面")) {
                                    clearlist.add(clearo);
                                }
                            }
                        }
                        if ("OnePlay".equals(type)) {
                            for (OrderBetRecord clearo : list) {
                                if (!clearo.getBetNumber().contains("两面")) {
                                    clearlist.add(clearo);
                                }
                            }
                        }
                        return clearlist;
                    }

                }
            }
        } catch (Exception e) {
            logger.error("selectOrderBetList occur error:{}", e);
        } finally {
            lock.writeLock().unlock();
        }
        return list;
    }

    @Override
    public int updateOrderRecord(String lotteryId, String issue, String sgnumber) {
        return OrderMapper.updateOrderRecord(lotteryId, issue, sgnumber);
    }

    @Override
    public int countOrderBetList(String issue, List<Integer> playIds, String lotteryId, String status) {
        if (CollectionUtils.isEmpty(playIds)) {
            return 0;
        }
        return orderMapperExt.countOrderBetList(issue, playIds, lotteryId, status);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public ResultInfo<Boolean> placeAnOrder(OrderDTO orderDTO) {
        // 获取用户信息
        Integer userId = orderDTO.getUserId();
        // 控制频率 一秒只能下一单
        String keySuffix = userId + "_orderBet";
        String intercept = (String) redisTemplate.opsForValue().get(keySuffix);
        if (org.apache.commons.lang3.StringUtils.isNotBlank(intercept)) {
            return ResultInfo.error("慢點，不能太快哦");
        }
        if (org.apache.commons.lang3.StringUtils.isBlank(intercept)) {
            redisTemplate.opsForValue().set(keySuffix, "1", 1, TimeUnit.SECONDS);
        }

        long startTime = System.currentTimeMillis();
        // 获取彩种
        Integer lotteryId = orderDTO.getLotteryId();
        // 获取用户购买的期号
        String issue = orderDTO.getIssue();

        // 获取投注限额信息
        logger.info("this.getBonusMap(), {}", System.currentTimeMillis() - startTime);

        try {
            logger.info("redis, {}", System.currentTimeMillis() - startTime);
            // 获取用户投注总额
            BigDecimal amount = new BigDecimal(0);
            // 获取彩种最大限制
            BetRestrict betRestrict = this.getBonusMap(lotteryId, 0);
            for (OrderBetRecord bet : orderDTO.getOrderBetList()) {
                Integer playId = bet.getPlayId();

                BigDecimal betCount = new BigDecimal(bet.getBetCount());
                BigDecimal betAmount = betCount.multiply(bet.getBetAmount());
                if (new BigDecimal(betAmount.intValue()).compareTo(betAmount) != 0
                        || betAmount.compareTo(new BigDecimal(0)) <= 0) {
                    return ResultInfo.error("投注額必須為正整數！");
                }

                BetRestrict restrict = this.getBonusMap(lotteryId, playId);
                if (restrict != null && restrict.getMaxMoney().compareTo(BigDecimal.valueOf(0)) > 0) {
                    if (betAmount.compareTo(restrict.getMaxMoney()) > 0) {
                        return ResultInfo.error("該投注超過最大限制，請注意減少投注額！");
                    }
                } else {
                    if (null != betRestrict && betRestrict.getMaxMoney().compareTo(BigDecimal.valueOf(0)) > 0) {
                        if (betAmount.compareTo(betRestrict.getMaxMoney()) > 0) {
                            return ResultInfo.error("該投注超過最大限制，請注意減少投注額！");
                        }
                    }
                }
                amount = amount.add(betAmount);
            }
            logger.info("amount, {}", System.currentTimeMillis() - startTime);
            //cMapper.selectByExample()

            MemWalletExample memWalletExample = new MemWalletExample();
            MemWalletExample.Criteria walletCriteria = memWalletExample.createCriteria();
            walletCriteria.andUserIdEqualTo(Long.valueOf(userId));
            MemWallet wallet = memWalletMapper.selectOneByExample(memWalletExample);
            BigDecimal balance =wallet.getBalance();
            if (amount.compareTo(balance) > 0) {
                return ResultInfo.error("餘額不足！");
            }
            logger.debug("redis balance : {}, bet amount : [{}]", balance, amount);
//            } else {
//                logger.info("用户下注没拿到锁{}", String.valueOf(userId));
//            }
        } catch (Exception e) {
            logger.error("order occur error:{}", e);
            return ResultInfo.error("下注出错！");
        } /*
         * finally { lock.unlock(); logger.info("用户下注释放锁{}", String.valueOf(userId)); }
         */
        // 判断该期号是否已过投注时间
        if (lotteryId == 1201) {
            ResultInfo<Boolean> resultInfo = this.checkIssueIsOpen(lotteryId, issue, 1);
            if (resultInfo.getCode() != StatusCode.SUCCESSCODE.getCode()) {
                return resultInfo;
            }
        }
        logger.info("checkIssueIsOpen, {}", System.currentTimeMillis() - startTime);
        // 订单推送到队列
//        rabbitTemplate.convertAndSend(RabbitConfig.TOPIC_EXCHANGE, RabbitConfig.BINDING_ORDER, "ORDER:" + JSON.toJSONString(orderDTO));
        // 发布事件；
        orderEventSentService.sendOrderJson(JSON.toJSONString(orderDTO));

//		String productOrderEnvt = ActiveMqConsumerListen.PRODUCTORDERENVI;
//		String queueName = ActiveMQConfig.QUEUE_ORDER + productOrderEnvt;
//		// 改为异步处理
//		//asyncSendJmsMessage.jmsSend(queueName, productOrderEnvt, orderDTO);
//		Destination QUEUE_ORDER = new ActiveMQQueue(ActiveMQConfig.QUEUE_ORDER + productOrderEnvt);
//		jmsMessagingTemplate.convertAndSend(QUEUE_ORDER, "ORDER:" + JSON.toJSONString(orderDTO));
        long endTime = System.currentTimeMillis();
        if ((endTime - startTime) > 3000) {
            logger.error("===========slowly trans, cost : {}", (endTime - startTime));
        }
        logger.info("rabbitTemplate.convertAndSend, {}", System.currentTimeMillis() - startTime);
        return ResultInfo.ok(true);
    }

//
//    public void sendMsg(OrderDTO orderDTO, Integer orderId, String ordersn) {
//        try {
//
//            BasAnchorroom room = BasAnchorRoomRestRedis.selectByPrimaryKey(orderDTO.getRoomId(), basAnchorRoomRest);
//            if (ObjectUtils.isEmpty(room)
//                    || StringUtils.isBlank(room.getStreamkey())
//                    || null == orderDTO.getLotteryId()) {
//                logger.error("{}.sendMsg 参数校验不通过。 params:{}", this.getClass().getName(), JSONObject.toJSON(room));
//                return;
//            }
//            if (CollectionUtil.isEmpty(orderDTO.getOrderBetList())) {
//                logger.error("{}.sendMsg 下注记录为空。 params:{}", this.getClass().getName(), JSONObject.toJSON(room));
//                return;
//            }
//            MemBaseinfo memBaseinfo = memBaseinfoService.getMemById(Long.valueOf(orderDTO.getUserId()));
//
//            BigDecimal zongxiazhuedu = new BigDecimal(0);
//            for (int i = 0; i < orderDTO.getOrderBetList().size(); i++) {
//                OrderBetRecord o = orderDTO.getOrderBetList().get(i);
//                zongxiazhuedu = zongxiazhuedu.add(o.getBetAmount().multiply(new BigDecimal(o.getBetCount())));
//            }
//            LotteryExample example = new LotteryExample();
//            LotteryExample.Criteria criteria = example.createCriteria();
//            criteria.andLotteryIdEqualTo(orderDTO.getLotteryId());
//            lotteryMapper.selectOneByExample(example);
//            Lottery lottery = lotteryMapper.selectOneByExample(example);
//            ChatBody talkBody = new ChatBody();
//            talkBody.setType(ChatMsgTypeEnum.ORDER_FOLLOW.getValue());
//            talkBody.setStream(room.getStreamkey());
//            talkBody.setAccno(memBaseinfo.getAccno());
//            talkBody.setMessage(memBaseinfo.getNickname() + "在" + lottery.getName() + "中下注了" + zongxiazhuedu + "播幣");
//            talkBody.setParam(orderId.toString());
//            publishService.publish(room.getRoomid(), talkBody);
//            logger.info("{}.sendMsg 消息发送成功。params:{}", this.getClass().getName(), JSONObject.toJSON(talkBody).toString());
//        } catch (Exception e) {
//            logger.error("{}.processOrder 发送WS消息失败,params:{},ordersn: {}", this.getClass().getName(), JSONObject.toJSON(orderDTO), ordersn, e);
//        }
//    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public void processOrder(String orderjson) {
        OrderDTO orderDTO = null;
        try {
            logger.info("下单日志:" + orderjson);
            // 创建新订单
            orderDTO = JSON.parseObject(orderjson, new TypeReference<OrderDTO>() {
            });
            // 使用锁
            RedisLock lock = new RedisLock(String.valueOf(orderDTO.getUserId()), 50 * 1000, 30 * 1000);
            try {
                if (lock.lock()) {

                    orderDTO.setReOrderNum(orderDTO.getReOrderNum() + 1);
                    OrderRecord order = new OrderRecord();
                    BeanUtils.copyProperties(orderDTO, order);
                    // 生成订单号
                    order.setOrderSn(SnowflakeIdWorker.createOrderSn());
                    // 持久化到数据库
                    logger.info("下单日志1:" + order.toString());
                    orderRecordMapper.insertSelective(order);
                    String source = order.getSource();
                    logger.info("下单日志2:" + order.toString());
                    List<OrderBetRecord> orderBetList = orderDTO.getOrderBetList();
                    // 获取用户投注总额
                    Integer orderId = order.getId();

                    BigDecimal amount = new BigDecimal(0);

                    for (OrderBetRecord bet : orderBetList) {
                        BigDecimal betCount = new BigDecimal(bet.getBetCount());
                        BigDecimal betAmount = betCount.multiply(bet.getBetAmount());
                        amount = getTradeOffAmount(amount.add(betAmount));
                    }

                    MemUser  memUser  =  memUserMapper.selectByPrimaryKey(Long.valueOf(orderDTO.getUserId()));
                    MemWalletExample memWalletExample = new MemWalletExample();
                    MemWalletExample.Criteria walletCriteria = memWalletExample.createCriteria();
                    walletCriteria.andUserIdEqualTo(memUser.getId());
                    MemWallet wallet = memWalletMapper.selectOneByExample(memWalletExample);

                    if (memUser == null || amount.compareTo(wallet.getBalance()) > 0) {
                        return;
                    }
                    //获取cateId
                    Integer categoryId = this.getCateIdByLotteryIdCache(order.getLotteryId());
                    if (categoryId == null) {
                        return;
                    }
                    //ONELIVE TODO 帐变记录
                    //MemGoldchangeDO dto = new MemGoldchangeDO();
                    for (OrderBetRecord bet : orderBetList) {
                        BigDecimal betCount = new BigDecimal(bet.getBetCount());
                        BigDecimal betAmount = getTradeOffAmount(betCount.multiply(bet.getBetAmount()));
                        bet.setId(null);
                        bet.setCreateTime(null);
                        bet.setUpdateTime(null);
                        bet.setIsDelete(false);
                        bet.setWinAmount(new BigDecimal(0));
                        bet.setBackAmount(new BigDecimal(0));
                        bet.setOrderId(orderId);
                        bet.setBetAmount(betAmount);
                        bet.setUserId(order.getUserId());
                        bet.setLotteryId(order.getLotteryId());
                        bet.setTbStatus(OrderBetStatus.WAIT);
                        bet.setIssue(order.getIssue());
                        bet.setOrderSn(order.getOrderSn());
                        bet.setSource(source);
                        bet.setCateId(categoryId);
                        bet.setRoomId(orderDTO.getRoomId());
                        if (orderDTO.getRoomId() != null) {
                           // dto.setChangetype(GoldchangeEnum.LIVEROOM_BET.getValue());//直播间购彩记录到帐变
                        } else {
                          //  dto.setChangetype(GoldchangeEnum.LOTTERY_BETTING.getValue());

                        }
                        orderBetRecordMapper.insertSelective(bet);
                    }

                    logger.info("下单日志3:" + orderBetList.toString());

                    //下单 的订单数据 发送消息到开奖中心项目，然后保存到redis中，供杀号使用
                    saveKillDataToRedis(String.valueOf(order.getLotteryId()), order.getIssue(), orderBetList);
                    //更新帐变变动前后打码量
//                    if (memBaseinfo.getNoWithdrawalAmount() == null || memBaseinfo.getNoWithdrawalAmount().compareTo(BigDecimal.ZERO) <= 0) {
//                        dto.setPreCgdml(getTradeOffAmount(null));
//                        dto.setAfterCgdml(getTradeOffAmount(null));
//                    } else {
//                        if (memBaseinfo.getNoWithdrawalAmount().compareTo(amount) < 0) {
//                            dto.setPreCgdml(getTradeOffAmount(memBaseinfo.getNoWithdrawalAmount()));
//                            dto.setAfterCgdml(getTradeOffAmount(null));
//                        } else {
//                            dto.setPreCgdml(getTradeOffAmount(memBaseinfo.getNoWithdrawalAmount()));
//                            dto.setAfterCgdml(getTradeOffAmount(memBaseinfo.getNoWithdrawalAmount().subtract(amount)));
//                        }
//                    }
//                    /**
//                     * 扣除用户余额
//                     */
//                    dto.setUserId(order.getUserId());
//                    // 设置备注
//                    dto.setOpnote("投注/" + order.getOrderSn());
//                    dto.setSource(source);
//                    dto.setAccno(memBaseinfo.getAccno());
//                    dto.setRefid(order.getId().longValue());
//                    dto.setRefaccno(memBaseinfo.getAccno());
//                    // 余额变动值【负数】
//                    BigDecimal tradeOffAmount = getTradeOffAmount(amount.multiply(new BigDecimal(-1)));
//                    dto.setQuantity(tradeOffAmount);
//                    // 以下逻辑移动到
//                    // BetCommonServiceImpl.updateMemberBetAmountAndNoWithdrawalAmount
//                    // 实现；结算后菜变动余额；防止打码量不准；对应 bug:2027
//                    // 计算不可提现金额变动值【负数】
//                    dto.setNoWithdrawalAmount(tradeOffAmount);
//                    // 累计投注额【正数】
//                    //dto.setBetAmount(amount);
//                    logger.info("下单日志4:" + dto.toString());
//                    dto.setWaitAmount(tradeOffAmount.negate());
//                    memBaseinfoWriteService.updateUserBalance(dto);
//                    logger.info("下单日志5:" + dto.toString());

//                    // 发送WS消息失败，不应该影响下注
//                    sendMsg(orderDTO, order.getId(), order.getOrderSn());
                } else {
                    logger.info("用户订单入库没拿到锁{}", orderDTO.getUserId());
                }
            } catch (InterruptedException e1) {
                logger.error("用户订单入库拿锁异常 error:{}", e1);
                throw new RuntimeException();
            } finally {
                lock.unlock();
                logger.info("用户订单入库释放锁{}", orderDTO.getUserId());
            }
        } catch (
                Exception e) {
            // 下单异常重试 重试两次
            if (orderDTO != null && orderDTO.getReOrderNum() <= 2) {
                orderEventSentService.sendOrderJson(JSON.toJSONString(orderDTO));
            }
            logger.error("生成订单出错，订单详情：{}", JSON.toJSONString(orderDTO), e);
            throw new RuntimeException();
        }

    }


    @Override
    public List<OrderRecord> selectOrdersNoClearn(int count) {
        OrderRecordExample orderExample = new OrderRecordExample();
        OrderRecordExample.Criteria orderCriteria = orderExample.createCriteria();
        orderCriteria.andOpenNumberIsNull();
        orderCriteria.andStatusEqualTo(OrderStatus.NORMAL);

        orderCriteria.andIsDeleteEqualTo(false);
        orderExample.setOffset(0);
        orderExample.setLimit(count);
        orderExample.setOrderByClause("create_time desc");

        List<OrderRecord> list = orderRecordMapper.selectByExample(orderExample);
        return list;
    }

    @Override
    public Map<Integer, OrderRecord> getOrderMap(List<OrderBetRecord> orderBetRecords) {
        List<OrderRecord> list = new ArrayList<>();
        Set<Integer> orderids = new HashSet<>();
        for (OrderBetRecord obr : orderBetRecords) {
            orderids.add(obr.getOrderId());
        }
        Map<Integer, OrderRecord> map = new HashMap<>();
        OrderRecordExample orderExample = new OrderRecordExample();
        OrderRecordExample.Criteria orderCriteria = orderExample.createCriteria();

        orderCriteria.andIdIn(new ArrayList<>(orderids));
        list = orderRecordMapper.selectByExample(orderExample);
        for (OrderRecord or : list) {
            map.put(or.getId(), or);
        }
        return map;
    }

    @Override
    public void updateOrderRecordBackStatus(List<OrderRecord> orderList) {
        if (CollectionUtil.isEmpty(orderList)) {
            return;
        }
        for (OrderRecord order : orderList) {
            // 判断订单状态是否需要改为BACK ——> 判断订单下有无正常的投注单
            OrderBetRecordExample orderBetExample = new OrderBetRecordExample();
            OrderBetRecordExample.Criteria orderBetCriteria = orderBetExample.createCriteria();
            orderBetCriteria.andOrderIdEqualTo(order.getId());
            orderBetCriteria.andTbStatusNotEqualTo(OrderBetStatus.BACK);
            int count = orderBetRecordMapper.countByExample(orderBetExample);
            if (count < 1) {
                // 修改订单状态
                order.setUpdateTime(new Date());
                order.setStatus(OrderStatus.BACK);
                orderRecordMapper.updateByPrimaryKeySelective(order);
            }
        }
    }

//    @Override
//    @Transactional
//    public ResultInfo liveRoomCopy(OrderFollow data) {
//        String key = "liveRoomCopy" + data.getOrders().toString();
//        RReadWriteLock lock = redissonClient.getReadWriteLock(key);
//        logger.info("{}.getReadWriteLock,params:{}", key);
//        try {
//            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
//            boolean bool = lock.writeLock().tryLock(100, 10, TimeUnit.SECONDS);
//
//            // 判断是否获取到锁
//            if (bool) {
//                Map<String, LotteryPlaySetting> lotterySettingMap = redisTemplate.opsForHash().entries(RedisKeys.LOTTERY_PLAY_SETTING_MAP_TYPE + Constants.LOTTERY_CATEGORY_TYPE_LOTTERY);
//                if (CollectionUtil.isEmpty(lotterySettingMap)) {
//                    lotterySettingMap = lotteryPlaySettingReadRest.queryLotteryPlaySettingMap(Constants.LOTTERY_CATEGORY_TYPE_LOTTERY);
//                }
//                if (null == lotterySettingMap) {
//                    return ResultInfo.error("彩種對應玩法已關閉");
//                }
//
//                AnchorMemFamilymem family = null;
//                //直播间购彩校验
//                BasAnchorroom basAnchorroom = BasAnchorRoomRestRedis.selectByPrimaryKey(data.getRoomId(), basAnchorRoomRest);
//                if (null == basAnchorroom) {
//                    throw new BusinessException(StatusCode.LIVE_ERROR_1101.getCode(), "直播間不存在");
//                }
//                //获取主播对应的用户信息
//                AnchorMemBaseinfo appMembers = AnchorMemBaseinfoRestRedis.getMemById(data.getFamilymemid(), anchorMemBaseinfoRest);
//                if (null != appMembers) {
//                    family = anchorMemFamilymemRest.getMemFamilymemByAncorAccno(appMembers.getAccno());
//                    if (null == family) {
//                        throw new BusinessException(StatusCode.LIVE_ERROR_1101.getCode(), "家族不存在");
//                    }
//                } else {
//                    throw new BusinessException(StatusCode.LIVE_ERROR_1101.getCode(), "主播不存在");
//                }
//
//                // 获取用户信息
//                Integer userId = data.getUserId();
//                MemBaseinfo appMember = memBaseinfoService.selectByPrimaryKey((long) userId);
//                if (appMember == null) {
//                    return ResultInfo.error("該用戶不存在或已註銷！");
//                }
//                // TODO 判断用户是否被冻结
//                if (appMember.getFreezeStatus().equals(1)) {
//                    return ResultInfo.error("該用戶已被凍結，暫不支持購彩！");
//                }
//                if (appMember.getBetStatus().equals(0)) {
//                    return ResultInfo.error("該用戶已被凍結，暫不支持購彩！");
//                }
//                logger.info("用户信息:{}", appMember.toString());
//                BigDecimal amount = memBaseinfoService.getOrderBetRecordAmount(data.getOrders());
//
//                // 获取用户余额
//                if (appMember.getGoldnum() == null || amount.compareTo(appMember.getGoldnum()) > 0) {
//                    logger.info("购彩余額不足");
//                    ResultInfo response = ResultInfo.ok();
//                    response.setStatus(StatusCode.LIVE_ERROR_120.getCode());
//                    response.setData(null);
//                    response.setInfo("余額不足,請充值!");
//                    return response;
//                }
//                for (Integer id : data.getOrders()) {
//                    // 获取投注信息
//                    OrderBetRecord orderBetRecord;
//                    if (redisTemplate.hasKey(ORDER_BET_KEY + id)) {
//                        orderBetRecord = (OrderBetRecord) redisTemplate.opsForValue().get(ORDER_BET_KEY + id);
//                    } else {
//                        orderBetRecord = orderBetRecordMapper.selectByPrimaryKey(id);
//                        redisTemplate.opsForValue().set(ORDER_BET_KEY + id, orderBetRecord);
//                    }
//
//                    // 判空
//                    if (orderBetRecord == null) {
//                        return ResultInfo.error("請選擇要跟投的訂單！");
//                    }
//                    Integer orderId = orderBetRecord.getOrderId();
//                    // 获取订单信息
//                    OrderRecord orderRecord;
//                    if (redisTemplate.hasKey(ORDER_KEY + orderId)) {
//                        orderRecord = (OrderRecord) redisTemplate.opsForValue().get(ORDER_KEY + orderId);
//                    } else {
//                        orderRecord = orderRecordMapper.selectByPrimaryKey(orderId);
//                        redisTemplate.opsForValue().set(ORDER_KEY + orderId, orderRecord);
//                    }
//                    if (orderRecord == null) {
//                        return ResultInfo.error("請選擇要跟投的訂單！");
//                    }
//                    logger.info("下单信息:{}", orderBetRecord.toString());
//
//
//                    if (null == lotterySettingMap.get(String.valueOf(orderBetRecord.getPlayId()))) {
//                        return ResultInfo.error("此訂單對應彩種玩法信息已關閉");
//                    }
//
//                    // 判断是否已开奖
//                    if (!orderBetRecord.getTbStatus().equals(OrderBetStatus.WAIT)) {
//                        return ResultInfo.error("該期已開獎，請注意跟投時間！");
//                    }
//
//                    // 获取用户投注总额
//                    BigDecimal betAmount = orderBetRecord.getBetAmount();
//                    // BigDecimal amount = betAmount.multiply(new BigDecimal(orderBetRecord.getBetCount()));
//
//                    BetRestrict restrict = this.getBonusMap(orderBetRecord.getLotteryId(), orderBetRecord.getPlayId());
//                    if (restrict != null && restrict.getMaxMoney().compareTo(BigDecimal.valueOf(0)) > 0) {
//                        if (betAmount.compareTo(restrict.getMaxMoney()) > 0) {
//                            return ResultInfo.error("該投注超過最大限制，請注意減少投注額！");
//                        }
//                    } else {
//                        // 获取彩种最大限制
//                        BetRestrict betRestrict = this.getBonusMap(orderBetRecord.getLotteryId(), 0);
//                        if (null != betRestrict && betRestrict.getMaxMoney().compareTo(BigDecimal.valueOf(0)) > 0) {
//                            if (betAmount.compareTo(betRestrict.getMaxMoney()) > 0) {
//                                return ResultInfo.error("該投注超過最大限制，請注意減少投注額！");
//                            }
//                        }
//                    }
//
//                    // 判断该期号是否已过投注时间
//                    ResultInfo<Boolean> resultInfo = this.checkIssueIsOpen(orderBetRecord.getLotteryId(),
//                            orderRecord.getIssue(), 1);
//                    if (resultInfo.isServerError()) {
//                        return ResultInfo.error("投注已過期！");
//                    }
//                    String orderSn = SnowflakeIdWorker.createOrderSn();
//                    // 生成订单
//                    OrderRecord order = new OrderRecord();
//                    BeanUtils.copyProperties(orderRecord, order);
//                    order.setId(null);
//                    order.setCreateTime(null);
//                    order.setUpdateTime(null);
//                    order.setStatus(OrderStatus.NORMAL);
//                    order.setIsDelete(false);
//                    order.setOrderSn(orderSn);
//                    order.setSource(data.getSource());
//                    order.setUserId(userId);
//                    order.setBuySource(KEY_FIVES);//代表直播间跟单
//                    // 持久化到数据库
//                    orderRecordMapper.insertSelective(order);
//                    logger.info("订单投注:{}", order.toString());
//                    // 生成投注信息
//                    OrderBetRecord orderBet = new OrderBetRecord();
//                    BeanUtils.copyProperties(orderBetRecord, orderBet);
//                    orderBet.setId(null);
//                    orderBet.setIsDelete(false);
//                    orderBet.setCreateTime(null);
//                    orderBet.setUpdateTime(null);
//                    orderBet.setTbStatus(OrderBetStatus.WAIT);
//                    orderBet.setUserId(userId);
//                    orderBet.setOrderId(order.getId());
//                    orderBet.setBetAmount(getTradeOffAmount(betAmount));
//                    orderBet.setIsPush(Constants.DEFAULT_ONE);
//                    orderBet.setCateId(orderBetRecord.getCateId());
//                    orderBet.setSource(data.getSource());
//                    orderBet.setFamilyid(family.getFamilyid());
//                    orderBet.setRoomId(data.getRoomId());
//                    // 持久化到数据库
//                    orderBetRecordMapper.insertSelective(orderBet);
//                    logger.info("子订单投注:{}", orderBet.toString());
//
//                    /**
//                     * 扣除用户余额
//                     */
//                    MemGoldchangeDO dto = new MemGoldchangeDO();
//
//                    //更新帐变变动前后打码量
//                    if (appMember.getNoWithdrawalAmount() == null || appMember.getNoWithdrawalAmount().compareTo(BigDecimal.ZERO) <= 0) {
//                        dto.setPreCgdml(getTradeOffAmount(null));
//                        dto.setAfterCgdml(getTradeOffAmount(null));
//                    } else {
//                        if (appMember.getNoWithdrawalAmount().compareTo(betAmount) < 0) {
//                            dto.setPreCgdml(getTradeOffAmount(appMember.getNoWithdrawalAmount()));
//                            dto.setAfterCgdml(getTradeOffAmount(null));
//                        } else {
//                            dto.setPreCgdml(getTradeOffAmount(appMember.getNoWithdrawalAmount()));
//                            dto.setAfterCgdml(getTradeOffAmount(appMember.getNoWithdrawalAmount().subtract(betAmount)));
//                        }
//                    }
//                    // 设置用户id
//                    dto.setUserId(order.getUserId());
//                    // 设置备注
//                    dto.setOpnote("直播间跟投注/" + order.getOrderSn());
//                    dto.setSource(order.getSource());
//                    dto.setAccno(appMembers.getAccno());
//                    dto.setRefid(order.getId().longValue());
//                    dto.setRefaccno(appMembers.getAccno());
//                    // 设置类型
//                    dto.setChangetype(GoldchangeEnum.LIVEROOM_BET.getValue());
//                    // 余额变动值【负数】
//                    BigDecimal tradeOffAmount = getTradeOffAmount(amount.multiply(new BigDecimal(-1)));
//                    dto.setQuantity(tradeOffAmount);
//                    // 计算不可提现金额变动值【负数】
//                    dto.setNoWithdrawalAmount(tradeOffAmount);
//                    // 累计投注额【正数】
//                    //dto.setBetAmount(betAmount);
//                    // 修改用户余额信息
//                    dto.setWaitAmount(tradeOffAmount.negate());
//                    memBaseinfoWriteService.updateUserBalance(dto);
//                    logger.info("更新余额:{}", dto.toString());
//                }
//            }
//        } catch (Exception e) {
//            logger.error("liveRoomCopy occur error.", e);
//            throw new RuntimeException(e.getMessage());
//        } finally {
//            // 释放锁
//            lock.writeLock().unlock();
//        }
//        return ResultInfo.ok();
//
//    }

//    @Transactional
//    @Override
//    public ResultInfo<OrderPushVo> followOrder(OrderFollow orderFollow) {
//
//        OrderPushVo orderPushVo = new OrderPushVo();
//        String key = "followOrder" + orderFollow.getGodPushId().toString();
//        RReadWriteLock lock = redissonClient.getReadWriteLock(key);
//        try {
//            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
//            boolean bool = lock.writeLock().tryLock(100, 10, TimeUnit.SECONDS);
//            // 判断是否获取到锁
//            if (bool) {
//                if (orderFollow.getOrderAmount().compareTo(BigDecimal.valueOf(0)) <= 0) {
//                    return ResultInfo.error("跟投金额不能为0或小于0！");
//                }
//
//                // 获取投注单id
//                CircleGodPushOrder godPushOrder = pushOrderMapper.selectByPrimaryKey(orderFollow.getGodPushId());
//                Integer orderBetId = godPushOrder.getOrderBetId();
//                // 获取投注信息
//                OrderBetRecord orderBetRecord;
//                if (redisTemplate.hasKey(ORDER_BET_KEY + orderBetId)) {
//                    orderBetRecord = (OrderBetRecord) redisTemplate.opsForValue().get(ORDER_BET_KEY + orderBetId);
//                } else {
//                    orderBetRecord = orderBetRecordMapper.selectByPrimaryKey(orderBetId);
//                }
//
//                // 判空
//                if (orderBetRecord == null) {
//                    return ResultInfo.error("请选择要跟投的订单！");
//                }
//
//                BigDecimal amount = orderFollow.getOrderAmount().divide(new BigDecimal(orderBetRecord.getBetCount()), 2,
//                        BigDecimal.ROUND_DOWN);
//                if (new BigDecimal(amount.intValue()).compareTo(amount) == -1) {
//                    return ResultInfo.error("跟投的金额有误！");
//                }
//
//                // 判断跟单人与推单人是不是同一人
//                if (orderFollow.getUserId().equals(orderBetRecord.getUserId())) {
//                    return ResultInfo.error("不能跟自己的投注单！");
//                }
//                // 判断是否已开奖
//                if (!orderBetRecord.getTbStatus().equals(OrderBetStatus.WAIT)) {
//                    return ResultInfo.error("该期已开奖，请注意跟投时间！");
//                }
//
//                // 获取订单信息
//                OrderRecord orderRecord;
//                if (redisTemplate.hasKey(ORDER_KEY + orderBetId)) {
//                    orderRecord = (OrderRecord) redisTemplate.opsForValue().get(ORDER_KEY + orderBetId);
//                } else {
//                    orderRecord = orderRecordMapper.selectByPrimaryKey(orderBetRecord.getOrderId());
//                    redisTemplate.opsForValue().set(ORDER_KEY + orderBetId, orderRecord);
//                }
//
//                // 获取投注限额信息
//
//                // 获取用户信息
//                Integer userId = orderFollow.getUserId();
//                if (userId == null || userId < 1) {
//                    logger.error("userId[{}]不能为空", userId);
//                    return ResultInfo.error("该用户不存在！");
//                }
////                AppMember appMember = appMemberMapper.selectByPrimaryKey(userId);
//                MemBaseinfo appMember = memBaseinfoService.selectByPrimaryKey((long) userId);
//                if (appMember == null) {
//                    return ResultInfo.error("该用户不存在或已注销！");
//                }
//                // TODO 判断用户是否被冻结
//                if (appMember.getFreezeStatus().equals(1)) {
//                    return ResultInfo.error("该用户已被冻结，暂不支持购彩！");
//                }
//                if (appMember.getBetStatus().equals(0)) {
//                    return ResultInfo.error("该账户已被冻结，暂不支持购彩！");
//                }
//
//                // 获取用户投注总额
//                BigDecimal betAmount = orderFollow.getOrderAmount();
//
//                BetRestrict restrict = this.getBonusMap(orderBetRecord.getLotteryId(), orderBetRecord.getPlayId());
//                if (restrict != null && restrict.getMaxMoney().compareTo(BigDecimal.valueOf(0)) > 0) {
//                    if (betAmount.compareTo(restrict.getMaxMoney()) > 0) {
//                        return ResultInfo.error("该投注超过最大限制，请注意减少投注额！");
//                    }
//                } else {
//                    // 获取彩种最大限制
//                    BetRestrict betRestrict = this.getBonusMap(orderBetRecord.getLotteryId(), 0);
//                    if (null != betRestrict && betRestrict.getMaxMoney().compareTo(BigDecimal.valueOf(0)) > 0) {
//                        if (betAmount.compareTo(betRestrict.getMaxMoney()) > 0) {
//                            return ResultInfo.error("该投注超过最大限制，请注意减少投注额！");
//                        }
//                    }
//
//                }
//
//                // 获取用户余额
//                BigDecimal balance = appMember.getGoldnum();
//                if (betAmount.compareTo(balance) > 0) {
//                    return ResultInfo.error("余额不足！");
//                }
//
//                // 判断该期号是否已过投注时间
//                ResultInfo<Boolean> resultInfo = this.checkIssueIsOpen(orderBetRecord.getLotteryId(),
//                        orderRecord.getIssue(), 1);
//                if (resultInfo.isServerError()) {
//                    return ResultInfo.error("投注已过期！");
//                }
//
//                String orderSn = SnowflakeIdWorker.createOrderSn();
//                // 生成订单
//                OrderRecord order = new OrderRecord();
//                BeanUtils.copyProperties(orderRecord, order);
//                order.setId(null);
//                order.setCreateTime(null);
//                order.setUpdateTime(null);
//                order.setStatus(OrderStatus.NORMAL);
//                order.setUserId(orderFollow.getUserId());
//                order.setIsDelete(false);
//                order.setOrderSn(orderSn);
//                order.setSource(orderFollow.getSource());
//                order.setUserId(userId);
//                order.setBuySource(KEY_FOUR);
//                // 持久化到数据库
//                orderRecordMapper.insertSelective(order);
//
//                // 生成投注信息
//                OrderBetRecord orderBet = new OrderBetRecord();
//                BeanUtils.copyProperties(orderBetRecord, orderBet);
//                orderBet.setId(null);
//                orderBet.setIsDelete(false);
//                orderBet.setCreateTime(null);
//                orderBet.setUpdateTime(null);
//                orderBet.setTbStatus(OrderBetStatus.WAIT);
//                orderBet.setUserId(userId);
//                orderBet.setOrderId(order.getId());
//                orderBet.setBetAmount(betAmount);
//                orderBet.setGodOrderId(orderFollow.getGodPushId());
//                orderBet.setIsPush(1);
//                // 持久化到数据库
//                orderBetRecordMapper.insertSelective(orderBet);
//
//                /**
//                 * 扣除用户余额
//                 */
//                MemGoldchangeDO dto = new MemGoldchangeDO();
//                // 设置用户id
//                dto.setUserId(order.getUserId());
//                // 设置备注
//                dto.setOpnote("投注/" + order.getOrderSn());
//                // 设置类型
//                dto.setType(BalanceChangeEnum.BET_ORDER.getValue());
//                // 余额变动值【负数】
//                dto.setQuantity(betAmount.multiply(new BigDecimal(-1)).setScale(2, BigDecimal.ROUND_HALF_UP));
//                // 计算不可提现金额变动值【负数】
//                dto.setNoWithdrawalAmount(betAmount.multiply(new BigDecimal(-1).setScale(2, BigDecimal.ROUND_HALF_UP)));
//                // 累计投注额【正数】
//                dto.setBetAmount(betAmount);
//                // 修改用户余额信息
//                dto.setWaitAmount(betAmount);
//                memBaseinfoWriteService.updateUserBalance(dto);
//
//                // 跟单中间表
//                CirclePushUserRecord circlePushUserRecord = new CirclePushUserRecord();
//                circlePushUserRecord.setUserId(userId);
//                circlePushUserRecord.setPushId(godPushOrder.getId());
//                circlePushUserRecord.setOrderBetId(orderBet.getId());
//                circlePushUserRecord.setOrderSn(orderSn);
//                circlePushUserRecord.setIssue(orderRecord.getIssue());
//                circlePushUserRecord.setGid(orderFollow.getGid());
//                circlePushUserRecord.setCreateTime(new Date());
//                circlePushUserRecordMapper.insertSelective(circlePushUserRecord);
//
//                // 拼装返回数据
//                Map<Integer, Lottery> lotteryMap = lotteryWriteService.selectLotteryMap(LotteryTypeEnum.LOTTERY.name());
//                orderPushVo.setLotteryName(lotteryMap.get(orderBetRecord.getLotteryId()).getName());
//                orderPushVo.setIssue(orderRecord.getIssue());
//                orderPushVo.setBetAmount(betAmount);
//                orderPushVo.setNickName(appMember.getNickname());
//                orderPushVo.setOrderSn(orderSn);
//            }
//        } catch (Exception e) {
//            logger.error("followOrder occur error.", e);
//            throw new RuntimeException(e.getMessage());
//        } finally {
//            // 释放锁
//            lock.writeLock().unlock();
//        }
//        return ResultInfo.ok(orderPushVo);
//
//    }

    /**
     * 追号
     *
     * @param orderAppend 追号信息
     * @param source      来源
     * @return
     */
    @Override
    @Transactional(rollbackFor = {Exception.class})
    public ResultInfo<Boolean> orderAppend(OrderAppendRecord orderAppend, String source) {
        // 获取相关属性
        String firstIssue = orderAppend.getFirstIssue();
        Integer lotteryId = orderAppend.getLotteryId();
        Integer playId = orderAppend.getPlayId();
        Integer userId = orderAppend.getUserId();
        Double betMultiples = orderAppend.getBetMultiples();
        Double doubleMultiples = orderAppend.getType().equals(1) ? 1 : orderAppend.getDoubleMultiples();

        // 获取当前投注期号
        String issue = orderAppendWriteService.createNextIssue(lotteryId, firstIssue, orderAppend.getAppendedCount());

        /** 生成订单信息 */
        OrderRecord order = new OrderRecord();
        order.setUserId(userId);
        order.setIssue(issue);
        // 生成订单号
        order.setOrderSn(SnowflakeIdWorker.createOrderSn());
        order.setSource(source);
        order.setAppendId(orderAppend.getId());
        order.setLotteryId(lotteryId);
        orderRecordMapper.insertSelective(order);

        /** 生成注单信息 */
        OrderBetRecord orderBet = new OrderBetRecord();
        orderBet.setOrderId(order.getId());
        orderBet.setUserId(userId);

        // 计算追号倍数
        double appendMultiples = betMultiples * (Math.pow(doubleMultiples, orderAppend.getAppendedCount()));
        BigDecimal amount = orderAppend.getBetPrice().multiply(new BigDecimal(appendMultiples));
        orderBet.setBetAmount(amount);
        orderBet.setBetCount(orderAppend.getBetCount());
        orderBet.setBetNumber(orderAppend.getBetNumber());
        orderBet.setLotteryId(lotteryId);
        orderBet.setPlayId(playId);
        orderBet.setSettingId(orderAppend.getSettingId());
        orderBetRecordMapper.insertSelective(orderBet);

        return ResultInfo.ok(true);
    }

    /**
     * 获取投注限制Map
     *
     * @param playId
     * @return
     */
    private BetRestrict getBonusMap(Integer lotteryId, Integer playId) {
        BetRestrictExample betRestrictExample = new BetRestrictExample();
        BetRestrictExample.Criteria bonusCriteria = betRestrictExample.createCriteria();
        bonusCriteria.andPlayTagIdEqualTo(playId);
        bonusCriteria.andLotteryIdEqualTo(lotteryId);
        BetRestrict betRestrict = betRestrictMapper.selectOneByExample(betRestrictExample);
        return betRestrict;
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public ResultInfo<Boolean> backAnOrder(Integer orderBetId, Integer userId) {
        logger.info("用户{},发起撤单订单号{}", userId, orderBetId);
        // 使用锁
        RedisLock lock = new RedisLock("BACK_ORDER_" + userId + "_" + orderBetId, 100, 10000);
        try {
            if (lock.lock()) {
                // 获取投注信息
                OrderBetRecord orderBet = orderBetRecordMapper.selectByPrimaryKey(orderBetId);
                if (orderBet == null || orderBet.getIsDelete()) {
                    return ResultInfo.error("该单不存在或已被撤回！");
                }

                if (!orderBet.getUserId().equals(userId)) {
                    return ResultInfo.error("用户只能撤回属于自己的订单！");
                }

                if (orderBet.getTbStatus().equals(OrderBetStatus.BACK)) {
                    return ResultInfo.error("该单已撤回！");
                }

                // 获取订单id
                Integer orderId = orderBet.getOrderId();
                // 获取订单信息
                OrderRecord order = orderRecordMapper.selectByPrimaryKey(orderId);
                // 判空
                if (order == null || order.getIsDelete()) {
                    return ResultInfo.error("该单不存在或已被撤回！");
                }

                // 获取彩种id
                Integer lotteryId = order.getLotteryId();
                // 获取期号
                String issue = order.getIssue();

                // 判断该期是否已开奖
                ResultInfo<Boolean> resultInfo = this.checkIssueIsOpen(lotteryId, issue, 2);
                if (resultInfo.getCode() != StatusCode.SUCCESSCODE.getCode()) {
                    return resultInfo;
                }

                // 控制频率
                String keySuffix = userId + "_backAnOrder";
                String intercept = (String) redisTemplate.opsForValue().get(keySuffix);
                if (org.apache.commons.lang3.StringUtils.isNotBlank(intercept)) {
                    return ResultInfo.error("慢点，不能太快哦");
                }
                if (org.apache.commons.lang3.StringUtils.isBlank(intercept)) {
                    redisTemplate.opsForValue().set(keySuffix, "1", 3, TimeUnit.SECONDS);
                }

                // status = transactionManager.getTransaction(new
                // DefaultTransactionDefinition());

                // 获取撤单延迟时间
                // RevokeOrderUtil.revokeOrder(txManager, orderBetRecordMapper,
                // orderRecordMapper, systemInfoMapper, appMemberWriteService, orderBet, order);
                // 撤单
                orderBet.setTbStatus(OrderBetStatus.BACK);
                orderBet.setUpdateTime(null);
                orderBetRecordMapper.updateByPrimaryKeySelective(orderBet);

                // 判断订单状态是否需要改为BACK ——> 判断订单下有无正常的投注单
                OrderBetRecordExample orderBetExample = new OrderBetRecordExample();
                OrderBetRecordExample.Criteria orderBetCriteria = orderBetExample.createCriteria();
                orderBetCriteria.andOrderIdEqualTo(orderId);
                orderBetCriteria.andTbStatusNotEqualTo(OrderBetStatus.BACK);
                int count = orderBetRecordMapper.countByExample(orderBetExample);
                if (count < 1) {
                    // 修改订单状态
                    order.setUpdateTime(null);
                    order.setStatus(OrderStatus.BACK);
                    orderRecordMapper.updateByPrimaryKeySelective(order);
                }

                // 是否追号撤单
                if (order.getAppendId() > 0) {
                    OrderAppendRecordExample orderAppExample = new OrderAppendRecordExample();
                    OrderAppendRecordExample.Criteria orderCriteria = orderAppExample.createCriteria();
                    orderCriteria.andIdEqualTo(order.getAppendId());
                    orderCriteria.andIsStopEqualTo(false);
                    OrderAppendRecord orderAppendRecord = orderAppendRecordMapper.selectOneByExample(orderAppExample);
                    if (orderAppendRecord != null) {
                        int sum = orderAppendRecord.getAppendCount() - orderAppendRecord.getAppendedCount() + 1;

                        BigDecimal aoumtBigDecimal = orderAppendRecord.getBetPrice()
                                .multiply(new BigDecimal(orderAppendRecord.getBetCount()));

                        BigDecimal numberBigDecimal = BigDecimal.ZERO;
                        if (orderAppendRecord.getType() == 1) {
                            numberBigDecimal = aoumtBigDecimal.multiply(new BigDecimal(sum));
                        } else if (orderAppendRecord.getType() == 2) {
                            numberBigDecimal = orderAppendRecord.getBetPrice()
                                    .multiply(new BigDecimal(orderAppendRecord.getBetMultiples()))
                                    .multiply(new BigDecimal(Math.pow(orderAppendRecord.getDoubleMultiples(), sum)));

                        }

                        // 终止追号
                        orderAppendRecord.setIsStop(true);
                        orderAppendRecordMapper.updateByPrimaryKeySelective(orderAppendRecord);

                         //ONELIVE TODO 帐变记录
//                        // 返还用户余额
//                        MemGoldchangeDO dto = new MemGoldchangeDO();
//                        // 设置用户id
//                        dto.setUserId(order.getUserId());
//                        // 设置备注
//                        dto.setOpnote("追号撤单/" + order.getOrderSn() + "/" + orderAppendRecord.getId());
//                        // 设置类型
//                        dto.setChangetype(GoldchangeEnum.ORDER_APPEND_BACK.getValue());
//                        // 余额变动值【正数】
//                        dto.setQuantity(getTradeOffAmount(numberBigDecimal));
//                        dto.setWaitAmount(getTradeOffAmount(new BigDecimal("-1").multiply(orderBet.getBetAmount())));
//                        memBaseinfoWriteService.updateUserBalance(dto);
                    }

                } else {
                    //ONELIVE TODO 帐变记录
//                    BigDecimal amount = getTradeOffAmount(orderBet.getBetAmount());
//                    // 返还用户余额
//                    MemGoldchangeDO dto = new MemGoldchangeDO();
//                    dto.setRefid((long) orderBetId);
//                    dto.setUserId(order.getUserId());
//                    // 设置备注
//                    dto.setOpnote("撤单/" + order.getOrderSn() + "/" + orderBet.getId());
//                    // 设置类型
//                    dto.setChangetype(GoldchangeEnum.BET_ORDER_BAK.getValue());
//                    // 余额变动值【正数】
//                    dto.setQuantity(amount);
//                    // 计算不可提现金额变动值【正数】
//                    // dto.setNoWithdrawalAmount(amount.setScale(2, BigDecimal.ROUND_HALF_UP));
//                    // 累计投注额【负数】
//                    // dto.setBetAmount(amount.multiply(new BigDecimal(-1)).setScale(2,
//                    // BigDecimal.ROUND_HALF_UP));
//                    dto.setWaitAmount(amount.negate());
//                    // 修改用户余额信息
//                    memBaseinfoWriteService.updateUserBalance(dto);
                    logger.info("用户{},发起撤单订单号{},撤单成功!", userId, orderBetId);
                }

                return ResultInfo.ok(true);
            } else {
                logger.info("用户{},发起撤单订单号{},没拿到锁!", userId, orderBetId);
                return ResultInfo.error();
            }
        } catch (Exception e) {
            logger.error("backOrder occur error. userId:{}, orderBetId:{}.", userId, orderBetId, e);
            return ResultInfo.error("系统繁忙！");
        } finally {
            logger.info("用户{},发起撤单订单号{},释放锁!", userId, orderBetId);
            lock.unlock();
        }

    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public ResultInfo<String> backOrders(List<Integer> orderBetIds, Integer userId) {
        // 使用锁
//		RedisLock lock = new RedisLock("BACK_ORDERS_" + userId, 0, 2000);
//		try {
//			if (lock.lock()) {
        // 获取投注单信息
        OrderBetRecordExample example = new OrderBetRecordExample();
        OrderBetRecordExample.Criteria criteria = example.createCriteria();
        criteria.andIdIn(orderBetIds);
        List<OrderBetRecord> orderBetList = orderBetRecordMapper.selectByExample(example);

        if (CollectionUtils.isEmpty(orderBetList)) {
            return ResultInfo.error("未找到相关订单信息！");
        }

        // 获取订单信息
        OrderRecordExample orderExample = new OrderRecordExample();
        OrderRecordExample.Criteria orderCriteria = orderExample.createCriteria();
        orderCriteria.andUserIdEqualTo(userId);
        orderCriteria.andIsDeleteEqualTo(false);
        orderCriteria.andStatusEqualTo(OrderStatus.NORMAL);
        orderCriteria.andOpenNumberIsNull();
        List<OrderRecord> orderRecords = orderRecordMapper.selectByExample(orderExample);
        if (CollectionUtils.isEmpty(orderRecords)) {
            return ResultInfo.error("相关单不存在或已开奖或已被撤回！");
        }

        Map<Integer, OrderRecord> orderMap = new HashMap<>();
        for (OrderRecord order : orderRecords) {
            orderMap.put(order.getId(), order);
        }

        List<String> orderSnList = new ArrayList<>();
        // 控制频率
        String keySuffix = userId + "_backOrders";
        String intercept = (String) redisTemplate.opsForValue().get(keySuffix);
        if (org.apache.commons.lang3.StringUtils.isNotBlank(intercept)) {
            return ResultInfo.error("慢点，不能太快哦");
        }
        if (org.apache.commons.lang3.StringUtils.isBlank(intercept)) {
            redisTemplate.opsForValue().set(keySuffix, "1", 3, TimeUnit.SECONDS);
        }

        for (OrderBetRecord orderBet : orderBetList) {
            RedisLock lock = new RedisLock("BACK_ORDER_" + userId + "_" + orderBet.getId(), 100, 10000);
            try {
                if (lock.lock()) {
                    // 验证用户
                    if (!orderBet.getUserId().equals(userId)) {
                        logger.error("用户只能撤回属于自己的订单！");
                        continue;
                    }
                    // 获取订单id
                    Integer orderId = orderBet.getOrderId();
                    // 获取订单信息
                    OrderRecord order = orderMap.get(orderId);
                    if (order == null || order.getIsDelete()) {
                        logger.info("未找到该订单,orderId:{}", orderId);
                        continue;
                    }

                    if (orderBet.getTbStatus().equals(OrderBetStatus.BACK)) {
                        logger.error("该单已撤回！orderId:{}", orderId);
                        continue;
                    }

                    String orderSn = order.getOrderSn();
                    // 获取彩种id
                    Integer lotteryId = order.getLotteryId();
                    // 获取期号
                    String issue = order.getIssue();

                    // 判断该期是否已开奖
                    ResultInfo<Boolean> resultInfo = this.checkIssueIsOpen(lotteryId, issue, 2);
                    if (resultInfo.getCode() != StatusCode.SUCCESSCODE.getCode()) {
                        if (!orderSnList.contains(orderSn)) {
                            orderSnList.add(orderSn);
                        }
                        continue;
                    }

                    // 撤单
                    orderBet.setTbStatus(OrderBetStatus.BACK);
                    orderBet.setUpdateTime(null);
                    orderBetRecordMapper.updateByPrimaryKeySelective(orderBet);

                    // 判断订单状态是否需要改为BACK ——> 判断订单下有无正常的投注单
                    OrderBetRecordExample orderBetExample = new OrderBetRecordExample();
                    OrderBetRecordExample.Criteria orderBetCriteria = orderBetExample.createCriteria();
                    orderBetCriteria.andOrderIdEqualTo(orderId);
                    orderBetCriteria.andTbStatusNotEqualTo(OrderBetStatus.BACK);
                    int count = orderBetRecordMapper.countByExample(orderBetExample);
                    // 修改订单状态
                    if (count < 1) {
                        order.setUpdateTime(null);
                        order.setStatus(OrderStatus.BACK);
                        orderRecordMapper.updateByPrimaryKeySelective(order);
                    }

                    BigDecimal amount = getTradeOffAmount(orderBet.getBetAmount());

                    //ONELIVE TODO 帐变记录
//                    // 返还用户余额
//                    MemGoldchangeDO dto = new MemGoldchangeDO();
//                    dto.setRefid(orderBet.getId().longValue());
//                    dto.setUserId(order.getUserId());
//                    // 设置备注
//                    dto.setOpnote("撤单/" + order.getOrderSn() + "/" + orderBet.getId());
//                    // 设置类型
//                    dto.setChangetype(GoldchangeEnum.BET_ORDER_BAK.getValue());
//                    // 余额变动值【正数】
//                    dto.setQuantity(amount);
//                    // 计算不可提现金额变动值【正数】
//                    // dto.setNoWithdrawalAmount(amount.setScale(2, BigDecimal.ROUND_HALF_UP));
//                    // 累计投注额【负数】
//                    // dto.setBetAmount(amount.multiply(new BigDecimal(-1)).setScale(2,
//                    // BigDecimal.ROUND_HALF_UP));
//                    dto.setWaitAmount(amount.negate());
//                    // 修改用户余额信息
//                    memBaseinfoWriteService.updateUserBalance(dto);
                }
            } catch (Exception e) {
                logger.error("backOrders occur error. userId:{}, orderBetIds:{}", userId,
                        JSONObject.toJSONString(orderBetIds), e);
                return ResultInfo.error("系统繁忙！");
            } finally {
                lock.unlock();
            }
        }

        if (CollectionUtils.isEmpty(orderSnList)) {
            return ResultInfo.ok("操作成功！");
        }

        StringBuilder str = new StringBuilder();
        str.append("订单号：");
        for (String orderSn : orderSnList) {
            str.append(orderSn).append("，");
        }
        str.append("已开奖或者已撤单！");
        return ResultInfo.ok(str.toString());
//			} else {
//				return ResultInfo.operateRepeatError();
//			}
//		} catch (Exception e) {
//			logger.error("backOrders occur error. userId:{}, orderBetIds:{}", userId,
//					JSONObject.toJSONString(orderBetIds), e);
//			return ResultInfo.error("系统繁忙！");
//		} finally {
//			lock.unlockWhenExpired();
//		}
    }

    /**
     * 判断指定彩种指定期号是否开奖
     *
     * @param lotteryId 彩种id
     * @param issue     期号
     * @param type      类型 1：投注 2：撤单
     * @return
     */
    @Override
    public ResultInfo<Boolean> checkIssueIsOpen(Integer lotteryId, String issue, Integer type) {
        LotteryTableNameEnum lotteryTableNameEnum = LotteryTableNameEnum.valueOfLotteryId(lotteryId);
        switch (lotteryTableNameEnum) {
            case LHC:
                LhcHandicapExample lhcHandicapExample = new LhcHandicapExample();
                LhcHandicapExample.Criteria lhcHandicapCriteria = lhcHandicapExample.createCriteria();
                lhcHandicapCriteria.andIssueEqualTo(issue);
                LhcHandicap lhcHandicap = lhcHandicapMapper.selectOneByExample(lhcHandicapExample);
                if (lhcHandicap == null) {
                    return ResultInfo.error("该期暂未开盘，请等待开盘！");
                }
                Date startDate = DateUtils.parseDate(lhcHandicap.getStartTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS);
                Date endDate = DateUtils.parseDate(lhcHandicap.getEndTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS);
                assert startDate != null;
                if (System.currentTimeMillis() < startDate.getTime()) {
                    return ResultInfo.error("该期暂未开盘，请等待开盘！");
                }
                assert endDate != null;
                if (System.currentTimeMillis() > endDate.getTime()) {
                    return ResultInfo.error("该期已封盘，请注意封盘时间！");
                }
                break;

            default:
                String tableName = LotteryTableNameEnum.getTableNameByLotteryId(lotteryId);
                if (org.apache.commons.lang.StringUtils.isBlank(tableName)) {
                    return ResultInfo.error("此彩种不存在！");
                }

                // 获取该彩种的封盘时间
                int fengpan = 1;
                try {
                    if (redisTemplate.hasKey(RedisKeys.LOTTERY_MAP_KEY)) {
                        Map<Integer, Lottery> lotteryMap = (Map<Integer, Lottery>) redisTemplate.opsForValue()
                                .get(RedisKeys.LOTTERY_MAP_KEY);
                        Lottery thisLottery = lotteryMap.get(lotteryId.toString());
                        fengpan = thisLottery.getEndTime();
                    }
                } catch (Exception e) {
                    logger.error("获取封盘时间出错lotteryId:{},e:{}", lotteryId, e);
                }
                Date addFengpanTime = DateUtils.add(new Date(), Calendar.SECOND, fengpan);
                List<String> issueNext = bjpksBeanMapper.selectByTableName(tableName,
                        DateUtils.formatDate(addFengpanTime, DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));

                // 获取下一期开奖的期号信息
                // XyftLotterySg xyftSg = xyftLotterySgWriteService.queryNextSg();
                if (issueNext != null && issueNext.size() > 0) {
                    String issueStr = issueNext.get(0);
                    if (issue.compareTo(issueStr) != 0) {
                        return type.equals(1) ? ResultInfo.error("该期已开彩或正在开彩中，请注意投注时间！")
                                : ResultInfo.error("该期已开彩或正在开彩中，无法撤单！");
                    }
                } else {
                    return ResultInfo.error("该彩种暂时关闭投注！");
                }
        }
        return ResultInfo.ok(true);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public ResultInfo<Integer> backOrderByAdmin(Integer lotteryId, String issue, String openNumber) {

        // 修改赛果信息
//        ResultInfo<Integer> result = lotterySgWriteService.changeNumber(lotteryId, issue, openNumber);
//        if (result.isServerError()) {
//            return result;
//        }
        boolean hasIssue = getIssue(issue, lotteryId);
        if (hasIssue == false) {
            return ResultInfo.ok(0);
        }

        // 给用户撤单
//        Thread thread = new Thread(() -> {
        // 获取指定期投注单信息
        OrderRecordExample orderExample = new OrderRecordExample();
        OrderRecordExample.Criteria orderCriteria = orderExample.createCriteria();
        orderCriteria.andLotteryIdEqualTo(lotteryId);
        orderCriteria.andIssueEqualTo(issue);
//            if (!CollectionUtils.isEmpty(orderSns)) {
//                orderCriteria.andOrderSnIn(orderSns);
//            }
        orderCriteria.andStatusEqualTo(OrderStatus.NORMAL);
        List<OrderRecord> orderList = orderRecordMapper.selectByExample(orderExample);
        logger.info("撤销开奖的数据个数：{}", orderList.size());
        List<Integer> orderIdList = new ArrayList<>();
        Map<Integer, OrderRecord> mapOrderRecord = new HashMap<>();
        for (OrderRecord orderRecord : orderList) {
            orderIdList.add(orderRecord.getId());
            mapOrderRecord.put(orderRecord.getId(), orderRecord);

            // 将订单修改为撤单状态
            orderRecord.setStatus(OrderStatus.BACK);
            orderRecord.setUpdateTime(null);
            orderRecordMapper.updateByPrimaryKeySelective(orderRecord);
        }

        OrderBetRecordExample orderBetRecordExample = new OrderBetRecordExample();
        OrderBetRecordExample.Criteria orderbetCriteria = orderBetRecordExample.createCriteria();
        orderbetCriteria.andOrderIdIn(orderIdList);
        List<OrderBetRecord> orderBetRecordList = new ArrayList<>();
        if (orderIdList.size() > 0) {
            orderBetRecordList = orderBetRecordMapper.selectByExample(orderBetRecordExample);
        }
        // 判空
        if (CollectionUtils.isEmpty(orderList)) {
            return ResultInfo.ok(1);
//                return;
        }

        // 组装投注单id集合
//            List<Integer> orderIds = new ArrayList<>();
        for (OrderBetRecord orderBet : orderBetRecordList) {
//                orderIds.add(order.getId());

            String tbStatus = orderBet.getTbStatus();
            orderBet.setTbStatus(OrderStatus.BACK);

//                // 获取相关账变记录
//                MemberBalanceChangeExample changeExample = new MemberBalanceChangeExample();
//                MemberBalanceChangeExample.Criteria changeCriteria = changeExample.createCriteria();
//                changeCriteria.andRemarkEqualTo("%" + order.getOrderSn());
//                List<MemberBalanceChange> changeList = memberBalanceChangeMapper.selectByExample(changeExample);
//                BigDecimal balance = new BigDecimal(0);
//                BigDecimal betMoney = new BigDecimal(0);
//                for (MemberBalanceChange change : changeList) {
//                    String[] str = change.getRemark().split("/");
//                    if ("投注".equals(str[0])) {
//                        betMoney = change.getChangeMoney();
//                        balance = balance.subtract(betMoney);
//                        continue;
//                    }
//                    balance = balance.subtract(change.getChangeMoney());
//                    memberBalanceChangeMapper.deleteByPrimaryKey(change.getId());
//                }

            //ONELIVE TODO 帐变记录
            // 第一步： 撤销开奖
            //默认金额
            BigDecimal defaultAmount = getTradeOffAmount(null);
            BigDecimal betMoney = defaultAmount;
            // 返还用户余额
          //  MemGoldchangeDO dto = new MemGoldchangeDO();
            // 设置用户id
          //  dto.setUserId(orderBet.getUserId());
            // 设置备注
         //   dto.setOpnote("撤销开奖/" + mapOrderRecord.get(orderBet.getOrderId()).getOrderSn() + "/" + orderBet.getId());
            // 设置类型
          //  dto.setChangetype(GoldchangeEnum.REVOKE_AWARD.getValue());
            // 余额变动值【正数】
//                dto.setChangeMoney(balance.setScale(2, BigDecimal.ROUND_HALF_UP));
            if (tbStatus.equals(Constants.HE)) {
              //  dto.setQuantity(betMoney);
                betMoney = defaultAmount;
            } else if (tbStatus.equals(Constants.WIN)) {
                BigDecimal winAmout = getTradeOffAmount(orderBet.getWinAmount().multiply(new BigDecimal("-1")));
              //  dto.setQuantity(winAmout);
                betMoney = winAmout;
            } else if (tbStatus.equals(Constants.NO_WIN)) {
            //    dto.setQuantity(defaultAmount);
                betMoney = defaultAmount;
            }

            orderBet.setWinAmount(defaultAmount);
            orderBet.setUpdateTime(new Date());
            orderBetRecordMapper.updateByPrimaryKeySelective(orderBet);
            BigDecimal tradeOffAmount = getTradeOffAmount(orderBet.getBetAmount());
            // 计算不可提现金额变动值【正数】
           // dto.setNoWithdrawalAmount(tradeOffAmount);
            // 累计投注额【负数】
          //  dto.setBetAmount(tradeOffAmount.negate());
            // 设置记录账变值
           // dto.setShowChange(betMoney);
          //  dto.setWaitAmount(defaultAmount);  //不修改待开奖 字段
            // 修改用户余额信息
           // memBaseinfoWriteService.updateUserBalance(dto);

            // 第二步： 撤单
            betMoney = defaultAmount;
            // 返还用户余额
          //  dto = new MemGoldchangeDO();
            // 设置用户id
          //  dto.setUserId(orderBet.getUserId());
            // 设置备注
          //  dto.setOpnote("撤单/" + mapOrderRecord.get(orderBet.getOrderId()).getOrderSn() + "/" + orderBet.getId());
            // 设置类型
         //   dto.setChangetype(GoldchangeEnum.BET_ORDER_BAK.getValue());
            // 余额变动值【正数】
//                dto.setChangeMoney(balance.setScale(2, BigDecimal.ROUND_HALF_UP));
            BigDecimal betAmount = getTradeOffAmount(orderBet.getBetAmount());
            if (tbStatus.equals(Constants.HE)) {
             //   dto.setQuantity(defaultAmount);
                betMoney = defaultAmount;
            } else if (tbStatus.equals(Constants.WIN)) {
             //   dto.setQuantity(betAmount);
                betMoney = betAmount;
            } else if (tbStatus.equals(Constants.NO_WIN)) {
           //     dto.setQuantity(betAmount);
                betMoney = betAmount;
            }

            orderBet.setWinAmount(defaultAmount);
            orderBet.setUpdateTime(new Date());
            orderBetRecordMapper.updateByPrimaryKeySelective(orderBet);
            // 计算不可提现金额变动值【正数】
            // dto.setNoWithdrawalAmount(balance.setScale(2, BigDecimal.ROUND_HALF_UP));
            // 累计投注额【负数】
            // dto.setBetAmount(balance.multiply(new BigDecimal(-1)).setScale(2,
            // BigDecimal.ROUND_HALF_UP));
            // 设置记录账变值
         //   dto.setShowChange(betMoney);
           // dto.setWaitAmount(defaultAmount);
            // 修改用户余额信息
       //     memBaseinfoWriteService.updateUserBalance(dto);

        }

//            // 获取投注单集合
//            OrderBetRecordExample orderBetExample = new OrderBetRecordExample();
//            OrderBetRecordExample.Criteria orderBetCriteria = orderBetExample.createCriteria();
//            orderBetCriteria.andOrderIdIn(orderIds);
//            orderBetCriteria.andTbStatusNotEqualTo(OrderBetStatus.BACK);
//            List<OrderBetRecord> orderBetList = orderBetRecordMapper.selectByExample(orderBetExample);
//            // 遍历撤单
//            for (OrderBetRecord orderBet : orderBetList) {
//                orderBet.setWinAmount(new BigDecimal(0));
//                orderBet.setBackAmount(new BigDecimal(0));
//                orderBet.setTbStatus(OrderBetStatus.BACK);
//                orderBet.setUpdateTime(null);
//                orderBetRecordMapper.updateByPrimaryKeySelective(orderBet);
//            }
//        });
//        thread.start();

        return ResultInfo.ok(1);
    }

    public boolean getIssue(String issue, Integer lotteryId) {
        boolean hasIssue = false;

        if (lotteryId.toString().equals(CaipiaoTypeEnum.CQSSC.getTagType())) {
            CqsscLotterySgExample cqsscLotterySgExample = new CqsscLotterySgExample();
            CqsscLotterySgExample.Criteria criteria = cqsscLotterySgExample.createCriteria();
            criteria.andIssueEqualTo(issue);
            CqsscLotterySg cqsscLotterySg = cqsscLotterySgMapper.selectOneByExample(cqsscLotterySgExample);
            if (cqsscLotterySg != null) {
                hasIssue = true;
            }
        } else if (lotteryId.toString().equals(CaipiaoTypeEnum.XJSSC.getTagType())) {
            XjsscLotterySgExample xjsscLotterySgExample = new XjsscLotterySgExample();
            XjsscLotterySgExample.Criteria criteria = xjsscLotterySgExample.createCriteria();
            criteria.andIssueEqualTo(issue);
            XjsscLotterySg xjsscLotterySg = xjsscLotterySgMapper.selectOneByExample(xjsscLotterySgExample);
            if (xjsscLotterySg != null) {
                hasIssue = true;
            }
        } else if (lotteryId.toString().equals(CaipiaoTypeEnum.TJSSC.getTagType())) {
            TjsscLotterySgExample tjsscLotterySgExample = new TjsscLotterySgExample();
            TjsscLotterySgExample.Criteria criteria = tjsscLotterySgExample.createCriteria();
            criteria.andIssueEqualTo(issue);
            TjsscLotterySg tjsscLotterySg = tjsscLotterySgMapper.selectOneByExample(tjsscLotterySgExample);
            if (tjsscLotterySg != null) {
                hasIssue = true;
            }
        } else if (lotteryId.toString().equals(CaipiaoTypeEnum.TENSSC.getTagType())) {
            TensscLotterySgExample tensscLotterySgExample = new TensscLotterySgExample();
            TensscLotterySgExample.Criteria criteria = tensscLotterySgExample.createCriteria();
            criteria.andIssueEqualTo(issue);
            TensscLotterySg tensscLotterySg = tensscLotterySgMapper.selectOneByExample(tensscLotterySgExample);
            if (tensscLotterySg != null) {
                hasIssue = true;
            }
        }
//        else if (lotteryId.toString().equals(CaipiaoTypeEnum.FIVESSC.getTagType())
//                || lotteryId.equals(CaipiaoTypeEnum.KLNIU.getTagType())) {
//            FivesscLotterySgExample fivesscLotterySgExample = new FivesscLotterySgExample();
//            FivesscLotterySgExample.Criteria criteria = fivesscLotterySgExample.createCriteria();
//            criteria.andIssueEqualTo(issue);
//            FivesscLotterySg fivesscLotterySg = fivesscLotterySgMapper.selectOneByExample(fivesscLotterySgExample);
//            if (fivesscLotterySg != null) {
//                hasIssue = true;
//            }
//        }
        else if (lotteryId.toString().equals(CaipiaoTypeEnum.JSSSC.getTagType())) {
            // 极速时时彩
            JssscLotterySgExample jssscLotterySgExample = new JssscLotterySgExample();
            JssscLotterySgExample.Criteria criteria = jssscLotterySgExample.createCriteria();
            criteria.andIssueEqualTo(issue);
            JssscLotterySg jssscLotterySg = jssscLotterySgMapper.selectOneByExample(jssscLotterySgExample);
            if (jssscLotterySg != null) {
                hasIssue = true;
            }
        } else if (lotteryId.toString().equals(CaipiaoTypeEnum.ONELHC.getTagType())) {
            OnelhcLotterySgExample onelhcLotterySgExample = new OnelhcLotterySgExample();
            OnelhcLotterySgExample.Criteria criteria = onelhcLotterySgExample.createCriteria();
            criteria.andIssueEqualTo(issue);
            OnelhcLotterySg onelhcLotterySg = onelhcLotterySgMapper.selectOneByExample(onelhcLotterySgExample);
            if (onelhcLotterySg != null) {
                hasIssue = true;
            }
        } else if (lotteryId.toString().equals(CaipiaoTypeEnum.FIVELHC.getTagType())) {
            FivelhcLotterySgExample fivelhcLotterySgExample = new FivelhcLotterySgExample();
            FivelhcLotterySgExample.Criteria criteria = fivelhcLotterySgExample.createCriteria();
            criteria.andIssueEqualTo(issue);
            FivelhcLotterySg fivelhcLotterySg = fivelhcLotterySgMapper.selectOneByExample(fivelhcLotterySgExample);
            if (fivelhcLotterySg != null) {
                hasIssue = true;
            }
        } else if (lotteryId.toString().equals(CaipiaoTypeEnum.AMLHC.getTagType())) {
            AmlhcLotterySgExample amlhcLotterySgExample = new AmlhcLotterySgExample();
            AmlhcLotterySgExample.Criteria criteria = amlhcLotterySgExample.createCriteria();
            criteria.andIssueEqualTo(issue);
            AmlhcLotterySg sslhcLotterySg = amlhcLotterySgMapper.selectOneByExample(amlhcLotterySgExample);
            if (sslhcLotterySg != null) {
                hasIssue = true;
            }
        } else if (lotteryId.toString().equals(CaipiaoTypeEnum.BJPKS.getTagType())) {
            BjpksLotterySgExample bjpksLotterySgExample = new BjpksLotterySgExample();
            BjpksLotterySgExample.Criteria criteria = bjpksLotterySgExample.createCriteria();
            criteria.andIssueEqualTo(issue);
            BjpksLotterySg bjpksLotterySg = bjpksLotterySgMapper.selectOneByExample(bjpksLotterySgExample);
            if (bjpksLotterySg != null) {
                hasIssue = true;
            }
        } else if (lotteryId.toString().equals(CaipiaoTypeEnum.TENPKS.getTagType())) {
            TenbjpksLotterySgExample tenbjpksLotterySgExample = new TenbjpksLotterySgExample();
            TenbjpksLotterySgExample.Criteria criteria = tenbjpksLotterySgExample.createCriteria();
            criteria.andIssueEqualTo(issue);
            TenbjpksLotterySg tenbjpksLotterySg = tenbjpksLotterySgMapper.selectOneByExample(tenbjpksLotterySgExample);
            if (tenbjpksLotterySg != null) {
                hasIssue = true;
            }
        } else if (lotteryId.toString().equals(CaipiaoTypeEnum.FIVEPKS.getTagType())) {
            FivebjpksLotterySgExample fivebjpksLotterySgExample = new FivebjpksLotterySgExample();
            FivebjpksLotterySgExample.Criteria criteria = fivebjpksLotterySgExample.createCriteria();
            criteria.andIssueEqualTo(issue);
            FivebjpksLotterySg fivebjpksLotterySg = fivebjpksLotterySgMapper
                    .selectOneByExample(fivebjpksLotterySgExample);
            if (fivebjpksLotterySg != null) {
                hasIssue = true;
            }
        }
//        else if (lotteryId.toString().equals(CaipiaoTypeEnum.JSPKS.getTagType())
//                || lotteryId.equals(CaipiaoTypeEnum.JSNIU.getTagType())) {
//            JsbjpksLotterySgExample jsbjpksLotterySgExample = new JsbjpksLotterySgExample();
//            JsbjpksLotterySgExample.Criteria criteria = jsbjpksLotterySgExample.createCriteria();
//            criteria.andIssueEqualTo(issue);
//            JsbjpksLotterySg jsbjpksLotterySg = jsbjpksLotterySgMapper.selectOneByExample(jsbjpksLotterySgExample);
//            if (jsbjpksLotterySg != null) {
//                hasIssue = true;
//            }
//        }
        else if (lotteryId.toString().equals(CaipiaoTypeEnum.XYFEIT.getTagType())) {
            XyftLotterySgExample xyftLotterySgExample = new XyftLotterySgExample();
            XyftLotterySgExample.Criteria criteria = xyftLotterySgExample.createCriteria();
            criteria.andIssueEqualTo(issue);
            XyftLotterySg xyftLotterySg = xyftLotterySgMapper.selectOneByExample(xyftLotterySgExample);
            if (xyftLotterySg != null) {
                hasIssue = true;
            }
        } else if (lotteryId.toString().equals(CaipiaoTypeEnum.PCDAND.getTagType())) {
            PceggLotterySgExample pceggLotterySgExample = new PceggLotterySgExample();
            PceggLotterySgExample.Criteria criteria = pceggLotterySgExample.createCriteria();
            criteria.andIssueEqualTo(issue);
            PceggLotterySg pceggLotterySg = pceggLotterySgMapper.selectOneByExample(pceggLotterySgExample);
            if (pceggLotterySg != null) {
                hasIssue = true;
            }
        } else if (lotteryId.toString().equals(CaipiaoTypeEnum.TXFFC.getTagType())) {
            TxffcLotterySgExample txffcLotterySgExample = new TxffcLotterySgExample();
            TxffcLotterySgExample.Criteria criteria = txffcLotterySgExample.createCriteria();
            criteria.andIssueEqualTo(issue);
            TxffcLotterySg txffcLotterySg = txffcLotterySgMapper.selectOneByExample(txffcLotterySgExample);
            if (txffcLotterySg != null) {
                hasIssue = true;
            }
        }
//        else if (lotteryId.toString().equals(CaipiaoTypeEnum.DLT.getTagType())) {
//            TcdltLotterySgExample tcdltLotterySgExample = new TcdltLotterySgExample();
//            TcdltLotterySgExample.Criteria criteria = tcdltLotterySgExample.createCriteria();
//            criteria.andIssueEqualTo(issue);
//            TcdltLotterySg tcdltLotterySg = tcdltLotterySgMapper.selectOneByExample(tcdltLotterySgExample);
//            if (tcdltLotterySg != null) {
//                hasIssue = true;
//            }
//        } else if (lotteryId.toString().equals(CaipiaoTypeEnum.TCPLW.getTagType())) {
//            TcplwLotterySgExample tcplwLotterySgExample = new TcplwLotterySgExample();
//            TcplwLotterySgExample.Criteria criteria = tcplwLotterySgExample.createCriteria();
//            criteria.andIssueEqualTo(issue);
//            TcplwLotterySg tcplwLotterySg = tcplwLotterySgMapper.selectOneByExample(tcplwLotterySgExample);
//            if (tcplwLotterySg != null) {
//                hasIssue = true;
//            }
//        } else if (lotteryId.toString().equals(CaipiaoTypeEnum.TC7XC.getTagType())) {
//            Tc7xcLotterySgExample tc7xcLotterySgExample = new Tc7xcLotterySgExample();
//            Tc7xcLotterySgExample.Criteria criteria = tc7xcLotterySgExample.createCriteria();
//            criteria.andIssueEqualTo(issue);
//            Tc7xcLotterySg tc7xcLotterySg = tc7xcLotterySgMapper.selectOneByExample(tc7xcLotterySgExample);
//            if (tc7xcLotterySg != null) {
//                hasIssue = true;
//            }
//        } else if (lotteryId.toString().equals(CaipiaoTypeEnum.FCSSQ.getTagType())) {
//            FcssqLotterySgExample fcssqLotterySgExample = new FcssqLotterySgExample();
//            FcssqLotterySgExample.Criteria criteria = fcssqLotterySgExample.createCriteria();
//            criteria.andIssueEqualTo(issue);
//            FcssqLotterySg fcssqLotterySg = fcssqLotterySgMapper.selectOneByExample(fcssqLotterySgExample);
//            if (fcssqLotterySg != null) {
//                hasIssue = true;
//            }
//        } else if (lotteryId.toString().equals(CaipiaoTypeEnum.FC3D.getTagType())) {
//            Fc3dLotterySgExample fc3dLotterySgExample = new Fc3dLotterySgExample();
//            Fc3dLotterySgExample.Criteria criteria = fc3dLotterySgExample.createCriteria();
//            criteria.andIssueEqualTo(issue);
//            Fc3dLotterySg fc3dLotterySg = fc3dLotterySgMapper.selectOneByExample(fc3dLotterySgExample);
//            if (fc3dLotterySg != null) {
//                hasIssue = true;
//            }
//        } else if (lotteryId.toString().equals(CaipiaoTypeEnum.FC7LC.getTagType())) {
//            Fc7lcLotterySgExample fc7lcLotterySgExample = new Fc7lcLotterySgExample();
//            Fc7lcLotterySgExample.Criteria criteria = fc7lcLotterySgExample.createCriteria();
//            criteria.andIssueEqualTo(issue);
//            Fc7lcLotterySg fc7lcLotterySg = fc7lcLotterySgMapper.selectOneByExample(fc7lcLotterySgExample);
//            if (fc7lcLotterySg != null) {
//                hasIssue = true;
//            }
//        } else if (lotteryId.toString().equals(CaipiaoTypeEnum.AZNIU.getTagType())
//                || lotteryId.equals(CaipiaoTypeEnum.AUSPKS.getTagType())) {
//            AuspksLotterySgExample auspksLotterySgExample = new AuspksLotterySgExample();
//            AuspksLotterySgExample.Criteria criteria = auspksLotterySgExample.createCriteria();
//            criteria.andIssueEqualTo(issue);
//            AuspksLotterySg auspksLotterySg = auspksLotterySgMapper.selectOneByExample(auspksLotterySgExample);
//            if (auspksLotterySg != null) {
//                hasIssue = true;
//            }
//        } else if (lotteryId.toString().equals(CaipiaoTypeEnum.JSPKFT.getTagType())) {
//            FtjspksLotterySgExample ftjspksLotterySgExample = new FtjspksLotterySgExample();
//            FtjspksLotterySgExample.Criteria criteria = ftjspksLotterySgExample.createCriteria();
//            criteria.andIssueEqualTo(issue);
//            FtjspksLotterySg ftjspksLotterySg = ftjspksLotterySgMapper.selectOneByExample(ftjspksLotterySgExample);
//            if (ftjspksLotterySg != null) {
//                hasIssue = true;
//            }
//        } else if (lotteryId.toString().equals(CaipiaoTypeEnum.XYFTFT.getTagType())) {
//            FtxyftLotterySgExample ftxyftLotterySgExample = new FtxyftLotterySgExample();
//            FtxyftLotterySgExample.Criteria criteria = ftxyftLotterySgExample.createCriteria();
//            criteria.andIssueEqualTo(issue);
//            FtxyftLotterySg ftxyftLotterySg = ftxyftLotterySgMapper.selectOneByExample(ftxyftLotterySgExample);
//            if (ftxyftLotterySg != null) {
//                hasIssue = true;
//            }
//        } else if (lotteryId.toString().equals(CaipiaoTypeEnum.JSSSCFT.getTagType())) {
//            // 番摊极速时时彩
//            FtjssscLotterySgExample ftjssscLotterySgExample = new FtjssscLotterySgExample();
//            FtjssscLotterySgExample.Criteria ftcriteria = ftjssscLotterySgExample.createCriteria();
//            ftcriteria.andIssueEqualTo(issue);
//            FtjssscLotterySg ftjssscLotterySg = ftjssscLotterySgMapper.selectOneByExample(ftjssscLotterySgExample);
//            if (ftjssscLotterySg != null) {
//                hasIssue = true;
//            }
//        }
        else if (lotteryId.toString().equals(CaipiaoTypeEnum.AUSACT.getTagType())) {
            AusactLotterySgExample ausactLotterySgExample = new AusactLotterySgExample();
            AusactLotterySgExample.Criteria criteria = ausactLotterySgExample.createCriteria();
            criteria.andIssueEqualTo(issue);
            AusactLotterySg ausactLotterySg = ausactLotterySgMapper.selectOneByExample(ausactLotterySgExample);
            if (ausactLotterySg != null) {
                hasIssue = true;
            }
        } else if (lotteryId.toString().equals(CaipiaoTypeEnum.AUSSSC.getTagType())) {
            AussscLotterySgExample aussscLotterySgExample = new AussscLotterySgExample();
            AussscLotterySgExample.Criteria criteria = aussscLotterySgExample.createCriteria();
            criteria.andIssueEqualTo(issue);
            AussscLotterySg aussscLotterySg = aussscLotterySgMapper.selectOneByExample(aussscLotterySgExample);
            if (aussscLotterySg != null) {
                hasIssue = true;
            }
        } else if (lotteryId.toString().equals(CaipiaoTypeEnum.AZKS.getTagType())) {
            AzksLotterySgExample azksLotterySgExample = new AzksLotterySgExample();
            AzksLotterySgExample.Criteria criteria = azksLotterySgExample.createCriteria();
            criteria.andIssueEqualTo(issue);
            AzksLotterySg azksLotterySg = azksLotterySgMapper.selectOneByExample(azksLotterySgExample);
            if (azksLotterySg != null) {
                hasIssue = true;
            }
        } else if (lotteryId.toString().equals(CaipiaoTypeEnum.DZKS.getTagType())) {
            DzksLotterySgExample dzksLotterySgExample = new DzksLotterySgExample();
            DzksLotterySgExample.Criteria criteria = dzksLotterySgExample.createCriteria();
            criteria.andIssueEqualTo(issue);
            DzksLotterySg dzksLotterySg = dzksLotterySgMapper.selectOneByExample(dzksLotterySgExample);
            if (dzksLotterySg != null) {
                hasIssue = true;
            }
        } else if (lotteryId.toString().equals(CaipiaoTypeEnum.DZPCDAND.getTagType())) {
            DzpceggLotterySgExample dzpceggLotterySgExample = new DzpceggLotterySgExample();
            DzpceggLotterySgExample.Criteria criteria = dzpceggLotterySgExample.createCriteria();

            criteria.andIssueEqualTo(issue);
            DzpceggLotterySg dzpceggLotterySg = dzpceggLotterySgMapper.selectOneByExample(dzpceggLotterySgExample);
            if (dzpceggLotterySg != null) {
                hasIssue = true;
            }
        } else if (lotteryId.toString().equals(CaipiaoTypeEnum.DZXYFEIT.getTagType())) {
            DzxyftLotterySgExample dzxyftLotterySgExample = new DzxyftLotterySgExample();
            DzxyftLotterySgExample.Criteria criteria = dzxyftLotterySgExample.createCriteria();

            criteria.andIssueEqualTo(issue);
            DzxyftLotterySg dzxyftLotterySg = dzxyftLotterySgMapper.selectOneByExample(dzxyftLotterySgExample);
            if (dzxyftLotterySg != null) {
                hasIssue = true;
            }
        } else if (lotteryId.toString().equals(CaipiaoTypeEnum.XJPLHC.getTagType())) {
            XjplhcLotterySgExample xjplhcLotterySgExample = new XjplhcLotterySgExample();
            XjplhcLotterySgExample.Criteria criteria = xjplhcLotterySgExample.createCriteria();

            criteria.andIssueEqualTo(issue);
            XjplhcLotterySg xjplhcLotterySg = xjplhcLotterySgMapper.selectOneByExample(xjplhcLotterySgExample);
            if (xjplhcLotterySgExample != null) {
                hasIssue = true;
            }
        }
        return hasIssue;
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public ResultInfo<Integer> reopenOrder(Integer lotteryId, String issue, String openNumber) {

        // 修改赛果信息
        ResultInfo<Integer> result = lotterySgWriteRest.changeNumber(lotteryId, issue, openNumber);
        if (result.getCode() != StatusCode.SUCCESSCODE.getCode()) {
            return result;
        }
//        boolean hasIssue = getIssue(issue,lotteryId);
//        if(hasIssue == false){
//            return ResultInfo.ok(0);
//        }

        // 设置号码
        /**
         * 回滚结算信息 创建新线程，因为如果订单数量过多，同步请求会超时导致失败，使用线程异步执行。
         */
//        Thread t = new Thread(() -> {
        // 获取指定期投注单信息
        OrderRecordExample orderExample = new OrderRecordExample();
        OrderRecordExample.Criteria orderCriteria = orderExample.createCriteria();
        orderCriteria.andLotteryIdEqualTo(lotteryId);
        orderCriteria.andIssueEqualTo(issue);
//            if (!CollectionUtils.isEmpty(orderSns)) {
//                orderCriteria.andOrderSnIn(orderSns);
//            }
        orderCriteria.andStatusEqualTo(OrderStatus.NORMAL);
        List<OrderRecord> orderList = orderRecordMapper.selectByExample(orderExample);
        logger.info("重新开奖的数据个数：{}", orderList.size());
        List<Integer> orderIdList = new ArrayList<>();
        Map<Integer, OrderRecord> mapOrderRecord = new HashMap<>();
        for (OrderRecord orderRecord : orderList) {
            orderIdList.add(orderRecord.getId());
            mapOrderRecord.put(orderRecord.getId(), orderRecord);

            // 将订单修改为待开奖状态
            orderRecord.setStatus(OrderStatus.NORMAL);
            orderRecord.setUpdateTime(null);
            orderRecord.setOpenNumber(openNumber);
            orderRecordMapper.updateByPrimaryKeySelective(orderRecord);
        }

        OrderBetRecordExample orderBetRecordExample = new OrderBetRecordExample();
        OrderBetRecordExample.Criteria orderbetCriteria = orderBetRecordExample.createCriteria();
        orderbetCriteria.andOrderIdIn(orderIdList);
        List<String> statusList = new ArrayList<>();
        statusList.add(Constants.WIN);
        statusList.add(Constants.NO_WIN);
        statusList.add(Constants.HE);
        orderbetCriteria.andTbStatusIn(statusList);
        List<OrderBetRecord> orderBetRecordList = new ArrayList<>();
        if (orderIdList.size() > 0) {
            orderBetRecordList = orderBetRecordMapper.selectByExample(orderBetRecordExample);
        }
        // 判空
        if (CollectionUtils.isEmpty(orderList)) {
//                return;
            return result;
        }
        // 组装投注单id集合
        List<Integer> orderIds = new ArrayList<>();
        for (OrderBetRecord orderBet : orderBetRecordList) {
            orderIds.add(orderBet.getId());

            String tbStatus = orderBet.getTbStatus();
            orderBet.setTbStatus(Constants.WAIT);
//                // 获取相关账变记录
//                MemberBalanceChangeExample changeExample = new MemberBalanceChangeExample();
//                MemberBalanceChangeExample.Criteria changeCriteria = changeExample.createCriteria();
//                changeCriteria.andRemarkLike("%" + order.getOrderSn());
//                List<MemberBalanceChange> changeList = memberBalanceChangeMapper.selectByExample(changeExample);
//                BigDecimal balance = new BigDecimal(0);
//                for (MemberBalanceChange change : changeList) {
//                    String[] str = change.getRemark().split("/");
//                    if ("投注".equals(str[0])) {
//                        continue;
//                    }
//                    balance = balance.subtract(change.getChangeMoney());
//                    memberBalanceChangeMapper.deleteByPrimaryKey(change.getId());
//                }

            //ONELIVE TODO 帐变记录
            // 第一步： 撤销开奖
            //默认金额
            BigDecimal defaultAmount = getTradeOffAmount(null);
            BigDecimal betMoney = defaultAmount;
            // 返还用户余额
            //MemGoldchangeDO dto = new MemGoldchangeDO();
            // 设置用户id
            //dto.setUserId(orderBet.getUserId());
            // 设置备注
            //dto.setOpnote("撤销开奖/" + mapOrderRecord.get(orderBet.getOrderId()).getOrderSn() + "/" + orderBet.getId());
            // 设置类型
            //dto.setChangetype(GoldchangeEnum.REVOKE_AWARD.getValue());
            // 余额变动值【正数】
//                dto.setChangeMoney(balance.setScale(2, BigDecimal.ROUND_HALF_UP));
            if (tbStatus.equals(Constants.HE)) {
                //dto.setQuantity(defaultAmount);
                betMoney = defaultAmount;
            } else if (tbStatus.equals(Constants.WIN)) {
                BigDecimal winAmount = orderBet.getWinAmount().multiply(new BigDecimal("-1"));
               // dto.setQuantity(winAmount);
                betMoney = winAmount;
            } else if (tbStatus.equals(Constants.NO_WIN)) {
               // dto.setQuantity(defaultAmount);
                betMoney = defaultAmount;
            }

            orderBet.setWinAmount(defaultAmount);
            orderBet.setUpdateTime(new Date());
            orderBetRecordMapper.updateByPrimaryKeySelective(orderBet);

//            // 计算不可提现金额变动值【正数】
//            BigDecimal betAmount = getTradeOffAmount(orderBet.getBetAmount());
//            dto.setNoWithdrawalAmount(betAmount);
//            // 累计投注额【负数】
//            dto.setBetAmount(betAmount.negate());
//            // 设置记录账变值
//            dto.setShowChange(betMoney);
//            dto.setWaitAmount(betAmount);
//            // 修改用户余额信息
//            memBaseinfoWriteService.updateUserBalance(dto);
        }

//            // 获取投注单集合
//            OrderBetRecordExample orderBetExample = new OrderBetRecordExample();
//            OrderBetRecordExample.Criteria orderBetCriteria = orderBetExample.createCriteria();
//            orderBetCriteria.andOrderIdIn(orderIds);
//            orderBetCriteria.andTbStatusNotEqualTo(OrderBetStatus.BACK);
//            List<OrderBetRecord> orderBetList = orderBetRecordMapper.selectByExample(orderBetExample);
//            for (OrderBetRecord orderBet : orderBetList) {
//                orderBet.setWinAmount(new BigDecimal(0));
//                orderBet.setBackAmount(new BigDecimal(0));
//                orderBet.setTbStatus(OrderBetStatus.WAIT);
//                orderBet.setUpdateTime(null);
//                orderBetRecordMapper.updateByPrimaryKeySelective(orderBet);
//            }
//        });
//        t.start();

        return ResultInfo.ok(1);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public void updateOrder(String number, List<OrderRecord> orderRecords) {
        // 判断订单是否存在
        if (CollectionUtils.isEmpty(orderRecords)) {
            return;
        }
        // 判断开奖号码是否为空
        if (StringUtils.isBlank(number)) {
            return;
        }
        // 循环更新订单信息
        for (OrderRecord order : orderRecords) {
            order.setOpenNumber(number);
            order.setUpdateTime(null);
            orderRecordMapper.updateByPrimaryKeySelective(order);
        }
    }

//    @Override
//    public ResultInfo<Boolean> openByHandle(Integer lotteryId, String issue, String openNumber) {
//        int count = 0;
//        String[] nums = openNumber.split(",");
//        switch (lotteryId) {
//            case 1:
//                // 查询并修改赛果
//                CqsscLotterySg cqsscSg = cqsscLotterySgWriteService.selectByIssue(issue);
//                if (cqsscSg == null) {
//                    return ResultInfo.error("期号有误！");
//                }
//                if (cqsscSg.getWan() != null) {
//                    return ResultInfo.error("该期已开奖！");
//                }
//                cqsscSg.setWan(Integer.valueOf(nums[0]));
//                cqsscSg.setQian(Integer.valueOf(nums[1]));
//                cqsscSg.setBai(Integer.valueOf(nums[2]));
//                cqsscSg.setShi(Integer.valueOf(nums[3]));
//                cqsscSg.setGe(Integer.valueOf(nums[4]));
//                cqsscSg.setTime(DateUtils.formatDate(new Date(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
//                cqsscSg.setOpenStatus("HANDLE");
//                count = cqsscLotterySgMapper.updateByPrimaryKeySelective(cqsscSg);
//
//                // 开奖
////                rabbitTemplate.convertAndSend(RabbitConfig.TOPIC_EXCHANGE, RabbitConfig.BINDING_SSC_CQ, "CQSSC:" + issue + ":" + openNumber);
//                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_SSC_CQ_LM, "CQSSC:" + issue + ":" + openNumber);
//                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_SSC_CQ_DN, "CQSSC:" + issue + ":" + openNumber);
//                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_SSC_CQ_15, "CQSSC:" + issue + ":" + openNumber);
//                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_SSC_CQ_QZH, "CQSSC:" + issue + ":" + openNumber);
//                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_SSC_CQ_UPDATE_DATA,
//                        "CQSSC:" + issue + ":" + openNumber);
//                break;
//            case 2:
//                // 查询并修改赛果
//                XjsscLotterySg xjsscSg = xjsscLotterySgWriteService.selectByIssue(issue);
//                if (xjsscSg == null) {
//                    return ResultInfo.error("期号有误！");
//                }
//                if (xjsscSg.getWan() != null) {
//                    return ResultInfo.error("该期已开奖！");
//                }
//                xjsscSg.setWan(Integer.valueOf(nums[0]));
//                xjsscSg.setQian(Integer.valueOf(nums[1]));
//                xjsscSg.setBai(Integer.valueOf(nums[2]));
//                xjsscSg.setShi(Integer.valueOf(nums[3]));
//                xjsscSg.setGe(Integer.valueOf(nums[4]));
//                xjsscSg.setTime(DateUtils.formatDate(new Date(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
//                xjsscSg.setOpenStatus("HANDLE");
//                count = xjsscLotterySgMapper.updateByPrimaryKeySelective(xjsscSg);
//                // 开奖
////                rabbitTemplate.convertAndSend(RabbitConfig.TOPIC_EXCHANGE, RabbitConfig.BINDING_SSC_XJ, "XJSSC:" + issue + ":" + openNumber);
//                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_SSC_XJ_LM, "XJSSC:" + issue + ":" + openNumber);
//                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_SSC_XJ_DN, "XJSSC:" + issue + ":" + openNumber);
//                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_SSC_XJ_15, "XJSSC:" + issue + ":" + openNumber);
//                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_SSC_XJ_QZH, "XJSSC:" + issue + ":" + openNumber);
//                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_SSC_XJ_UPDATE_DATA,
//                        "XJSSC:" + issue + ":" + openNumber);
//                break;
//            case 3:
//                // 查询并修改赛果
//                TxffcLotterySg txffcSg = txffcLotterySgWriteService.selectByIssue(issue);
//                if (txffcSg == null) {
//                    return ResultInfo.error("期号有误！");
//                }
//                if (txffcSg.getWan() != null) {
//                    return ResultInfo.error("该期已开奖！");
//                }
//                txffcSg.setWan(Integer.valueOf(nums[0]));
//                txffcSg.setQian(Integer.valueOf(nums[1]));
//                txffcSg.setBai(Integer.valueOf(nums[2]));
//                txffcSg.setShi(Integer.valueOf(nums[3]));
//                txffcSg.setGe(Integer.valueOf(nums[4]));
//                txffcSg.setTime(DateUtils.formatDate(new Date(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
//                txffcSg.setOpenStatus("HANDLE");
//                count = txffcLotterySgMapper.updateByPrimaryKeySelective(txffcSg);
//                // 开奖
////                rabbitTemplate.convertAndSend(RabbitConfig.TOPIC_EXCHANGE, RabbitConfig.BINDING_SSC_TX, "TXFFC:" + issue + ":" + openNumber);
//                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_SSC_TX_LM, "TXFFC:" + issue + ":" + openNumber);
//                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_SSC_TX_DN, "TXFFC:" + issue + ":" + openNumber);
//                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_SSC_TX_15, "TXFFC:" + issue + ":" + openNumber);
//                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_SSC_TX_QZH, "TXFFC:" + issue + ":" + openNumber);
//                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_SSC_TX_UPDATE_DATA,
//                        "TXFFC:" + issue + ":" + openNumber);
//                break;
//            case 4:
//
//                break;
//            case 5:
//                PceggLotterySg pcddSg = pceggLotterySgWriteService.selectByIssue(issue);
//                if (pcddSg == null) {
//                    return ResultInfo.error("期号有误！");
//                }
//                if (StringUtils.isNotBlank(pcddSg.getNumber())) {
//                    return ResultInfo.error("该期已开奖！");
//                }
//                pcddSg.setNumber(openNumber);
//                pcddSg.setTime(DateUtils.formatDate(new Date(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
//                pcddSg.setOpenStatus("HANDLE");
//                count = pceggLotterySgMapper.updateByPrimaryKeySelective(pcddSg);
//                // 开奖
////                rabbitTemplate.convertAndSend(RabbitConfig.TOPIC_EXCHANGE, RabbitConfig.BINDING_PCEGG, "PCDD:" + issue + ":" + openNumber);
//                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_PCEGG_TM, "PCDD:" + issue + ":" + openNumber);
//                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_PCEGG_BZ, "PCDD:" + issue + ":" + openNumber);
//                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_PCEGG_TMBS, "PCDD:" + issue + ":" + openNumber);
//                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_PCEGG_SB, "PCDD:" + issue + ":" + openNumber);
//                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_PCEGG_HH, "PCDD:" + issue + ":" + openNumber);
//                break;
//            case 6:
//                BjpksLotterySg bjpksSg = bjpksLotterySgWriteService.selectByIssue(issue);
//                if (bjpksSg == null) {
//                    return ResultInfo.error("期号有误！");
//                }
//                if (StringUtils.isNotBlank(bjpksSg.getNumber())) {
//                    return ResultInfo.error("该期已开奖！");
//                }
//                bjpksSg.setNumber(openNumber);
//                bjpksSg.setTime(DateUtils.formatDate(new Date(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
//                bjpksSg.setOpenStatus("HANDLE");
//                count = bjpksLotterySgMapper.updateByPrimaryKeySelective(bjpksSg);
//                // 开奖
////                rabbitTemplate.convertAndSend(RabbitConfig.TOPIC_EXCHANGE, RabbitConfig.BINDING_BJPKS, "BJPKS:" + issue + ":" + openNumber);
//                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_BJPKS_LM, "BJPKS:" + issue + ":" + openNumber);
//                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_BJPKS_CMC_CQJ,
//                        "BJPKS:" + issue + ":" + openNumber);
//                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_BJPKS_DS_CQJ, "BJPKS:" + issue + ":" + openNumber);
//                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_BJPKS_DWD, "BJPKS:" + issue + ":" + openNumber);
//                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_BJPKS_GYH, "BJPKS:" + issue + ":" + openNumber);
//                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_BJPKS_UPDATE_DATA,
//                        "BJPKS:" + issue + ":" + openNumber);
//                break;
//            case 7:
//                XyftLotterySg xyftSg = xyftLotterySgWriteService.selectByIssue(issue);
//                if (xyftSg == null) {
//                    return ResultInfo.error("期号有误！");
//                }
//                if (StringUtils.isNotBlank(xyftSg.getNumber())) {
//                    return ResultInfo.error("该期已开奖！");
//                }
//                xyftSg.setNumber(openNumber);
//                xyftSg.setTime(DateUtils.formatDate(new Date(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
//                xyftSg.setOpenStatus("HANDLE");
//                count = xyftLotterySgMapper.updateByPrimaryKeySelective(xyftSg);
//                // 开奖
////                rabbitTemplate.convertAndSend(RabbitConfig.TOPIC_EXCHANGE, RabbitConfig.BINDING_XYFT, "XYFT:" + issue + ":" + openNumber);
//                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_XYFT_LM, "XYFT:" + issue + ":" + openNumber);
//                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_XYFT_CMC_CQJ, "XYFT:" + issue + ":" + openNumber);
//                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_XYFT_DS_CQJ, "XYFT:" + issue + ":" + openNumber);
//                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_XYFT_DWD, "XYFT:" + issue + ":" + openNumber);
//                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_XYFT_GYH, "XYFT:" + issue + ":" + openNumber);
//                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_XYFT_UPDATE_DATA,
//                        "XYFT:" + issue + ":" + openNumber);
//                break;
//        }
//        return count > 0 ? ResultInfo.ok(true) : ResultInfo.ok(false);
//    }

    @Override
    public ResultInfo<Boolean> jiesuanOrderBetById(Integer id, Double winAmount, String tbStatus, String openNumber) {
        try {
            OrderBetRecord orderBetRecord = orderBetRecordMapper.selectByPrimaryKey(id);
            // 根据中奖金额,修改投注信息及相关信息
            OrderRecord orderRecord = orderRecordMapper.selectByPrimaryKey(orderBetRecord.getOrderId());
            if (orderRecord.getOpenNumber() == null || orderRecord.getOpenNumber().equals("")) {
                orderRecord.setOpenNumber(openNumber);
                orderRecordMapper.updateByPrimaryKey(orderRecord);
            }

            orderBetRecord.setTbStatus(tbStatus);
            betCommonService.winOrLose(orderBetRecord, BigDecimal.valueOf(winAmount), orderRecord.getUserId(),
                    orderRecord.getOrderSn());
            return ResultInfo.ok(true);
        } catch (Exception e) {
            logger.error("该订单结算失败：{},{},{}", id, winAmount, openNumber);
            return ResultInfo.error("该订单结算失败");
        }
    }

    @Override
    public ResultInfo<Boolean> jiesuanOrderBetByIssue(Integer lotteryId, String issue, String number) {
        try {
            if (lotteryId.toString().equals(CaipiaoTypeEnum.CQSSC.getTagType())) {
                // 结算【两面】
                betSscbmService.countlm(issue, number, Integer.parseInt(CaipiaoTypeEnum.CQSSC.getTagType()));
                // 基本【组选】规则
                betSscbmService.countdn(issue, number, Integer.parseInt(CaipiaoTypeEnum.CQSSC.getTagType()));
                // 【定位胆】规则
                betSscbmService.count15(issue, number, Integer.parseInt(CaipiaoTypeEnum.CQSSC.getTagType()));
                // 【定位大小单双】规则
                betSscbmService.countqzh(issue, number, Integer.parseInt(CaipiaoTypeEnum.CQSSC.getTagType()));
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.XJSSC.getTagType())) {
                // 结算【两面】
                betSscbmService.countlm(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJSSC.getTagType()));
                // 基本【组选】规则
                betSscbmService.countdn(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJSSC.getTagType()));
                // 【定位胆】规则
                betSscbmService.count15(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJSSC.getTagType()));
                // 【定位大小单双】规则
                betSscbmService.countqzh(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJSSC.getTagType()));
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.TJSSC.getTagType())) {
                // 结算【两面】
                betSscbmService.countlm(issue, number, Integer.parseInt(CaipiaoTypeEnum.TJSSC.getTagType()));
                // 基本【组选】规则
                betSscbmService.countdn(issue, number, Integer.parseInt(CaipiaoTypeEnum.TJSSC.getTagType()));
                // 【定位胆】规则
                betSscbmService.count15(issue, number, Integer.parseInt(CaipiaoTypeEnum.TJSSC.getTagType()));
                // 【定位大小单双】规则
                betSscbmService.countqzh(issue, number, Integer.parseInt(CaipiaoTypeEnum.TJSSC.getTagType()));
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.TENSSC.getTagType())) {
                // 结算【两面】
                betSscbmService.countlm(issue, number, Integer.parseInt(CaipiaoTypeEnum.TENSSC.getTagType()));
                // 基本【组选】规则
                betSscbmService.countdn(issue, number, Integer.parseInt(CaipiaoTypeEnum.TENSSC.getTagType()));
                // 【定位胆】规则
                betSscbmService.count15(issue, number, Integer.parseInt(CaipiaoTypeEnum.TENSSC.getTagType()));
                // 【定位大小单双】规则
                betSscbmService.countqzh(issue, number, Integer.parseInt(CaipiaoTypeEnum.TENSSC.getTagType()));
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.FIVESSC.getTagType())) {
                // 结算【两面】
                betSscbmService.countlm(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVESSC.getTagType()));
                // 基本【组选】规则
                betSscbmService.countdn(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVESSC.getTagType()));
                // 【定位胆】规则
                betSscbmService.count15(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVESSC.getTagType()));
                // 【定位大小单双】规则
                betSscbmService.countqzh(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVESSC.getTagType()));
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.JSSSC.getTagType())) {
                // 结算【两面】
                betSscbmService.countlm(issue, number, Integer.parseInt(CaipiaoTypeEnum.JSSSC.getTagType()));
                // 基本【组选】规则
                betSscbmService.countdn(issue, number, Integer.parseInt(CaipiaoTypeEnum.JSSSC.getTagType()));
                // 【定位胆】规则
                betSscbmService.count15(issue, number, Integer.parseInt(CaipiaoTypeEnum.JSSSC.getTagType()));
                // 【定位大小单双】规则
                betSscbmService.countqzh(issue, number, Integer.parseInt(CaipiaoTypeEnum.JSSSC.getTagType()));
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.LHC.getTagType())) {
                // 结算六合彩- 【特码,正特,六肖,正码1-6】
                jiesuanByHandle(issue, number);
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.ONELHC.getTagType())) {
                // 结算六合彩- 【特码,正特,六肖,正码1-6】
                betLhcService.clearingLhcTeMaA(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()),
                        true);
                betLhcService.clearingLhcZhengTe(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()),
                        true);
                betLhcService.clearingLhcZhengMaOneToSix(issue, number,
                        Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                betLhcService.clearingLhcLiuXiao(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()),
                        true);

                // 结算六合彩- 【正码,半波,尾数】
                betLhcService.clearingLhcZhengMaA(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()),
                        true);
                betLhcService.clearingLhcBanBo(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()),
                        true);
                betLhcService.clearingLhcWs(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);

                // 结算六合彩- 【连码,连肖,连尾】
                betLhcService.clearingLhcLianMa(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()),
                        true);
                betLhcService.clearingLhcLianXiao(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()),
                        true);
                betLhcService.clearingLhcLianWei(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()),
                        true);

                // 结算六合彩- 【不中,1-6龙虎,五行】
                betLhcService.clearingLhcNoOpen(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()),
                        true);
                betLhcService.clearingLhcOneSixLh(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()),
                        true);
                betLhcService.clearingLhcWuxing(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()),
                        true);

                // 结算六合彩- 【平特,特肖】
                betLhcService.clearingLhcPtPt(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()),
                        true);
                betLhcService.clearingLhcTxTx(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()),
                        true);
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.FIVELHC.getTagType())) {
                // 结算六合彩- 【特码,正特,六肖,正码1-6】
                betLhcService.clearingLhcTeMaA(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()),
                        true);
                betLhcService.clearingLhcZhengTe(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()),
                        true);
                betLhcService.clearingLhcZhengMaOneToSix(issue, number,
                        Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                betLhcService.clearingLhcLiuXiao(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()),
                        true);

                // 结算六合彩- 【正码,半波,尾数】
                betLhcService.clearingLhcZhengMaA(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()),
                        true);
                betLhcService.clearingLhcBanBo(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()),
                        true);
                betLhcService.clearingLhcWs(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()),
                        true);

                // 结算六合彩- 【连码,连肖,连尾】
                betLhcService.clearingLhcLianMa(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()),
                        true);
                betLhcService.clearingLhcLianXiao(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()),
                        true);
                betLhcService.clearingLhcLianWei(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()),
                        true);

                // 结算六合彩- 【不中,1-6龙虎,五行】
                betLhcService.clearingLhcNoOpen(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()),
                        true);
                betLhcService.clearingLhcOneSixLh(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()),
                        true);
                betLhcService.clearingLhcWuxing(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()),
                        true);

                // 结算六合彩- 【平特,特肖】
                betLhcService.clearingLhcPtPt(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()),
                        true);
                betLhcService.clearingLhcTxTx(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()),
                        true);
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.AMLHC.getTagType())) {
                // 结算六合彩- 【特码,正特,六肖,正码1-6】
                betLhcService.clearingLhcTeMaA(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()),
                        true);
                betLhcService.clearingLhcZhengTe(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()),
                        true);
                betLhcService.clearingLhcZhengMaOneToSix(issue, number,
                        Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                betLhcService.clearingLhcLiuXiao(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()),
                        true);

                // 结算六合彩- 【正码,半波,尾数】
                betLhcService.clearingLhcZhengMaA(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()),
                        true);
                betLhcService.clearingLhcBanBo(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()),
                        true);
                betLhcService.clearingLhcWs(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);

                // 结算六合彩- 【连码,连肖,连尾】
                betLhcService.clearingLhcLianMa(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()),
                        true);
                betLhcService.clearingLhcLianXiao(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()),
                        true);
                betLhcService.clearingLhcLianWei(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()),
                        true);

                // 结算六合彩- 【不中,1-6龙虎,五行】
                betLhcService.clearingLhcNoOpen(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()),
                        true);
                betLhcService.clearingLhcOneSixLh(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()),
                        true);
                betLhcService.clearingLhcWuxing(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()),
                        true);

                // 结算六合彩- 【平特,特肖】
                betLhcService.clearingLhcPtPt(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()),
                        true);
                betLhcService.clearingLhcTxTx(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()),
                        true);
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.BJPKS.getTagType())) {
                // 结算【北京PK10-两面】
                betBjpksService.clearingBjpksLm(issue, number, Integer.parseInt(CaipiaoTypeEnum.BJPKS.getTagType()));
                // 结算【北京PK10-猜名次猜前几】
                betBjpksService.clearingBjpksCmcCqj(issue, number,
                        Integer.parseInt(CaipiaoTypeEnum.BJPKS.getTagType()));
                // 结算【北京PK10-冠亚和】
                betBjpksService.clearingBjpksGyh(issue, number, Integer.parseInt(CaipiaoTypeEnum.BJPKS.getTagType()));
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.TENPKS.getTagType())) {
                // 结算【北京PK10-两面】
                betBjpksService.clearingBjpksLm(issue, number, Integer.parseInt(CaipiaoTypeEnum.TENPKS.getTagType()));
                // 结算【北京PK10-猜名次猜前几】
                betBjpksService.clearingBjpksCmcCqj(issue, number,
                        Integer.parseInt(CaipiaoTypeEnum.TENPKS.getTagType()));
                // 结算【北京PK10-冠亚和】
                betBjpksService.clearingBjpksGyh(issue, number, Integer.parseInt(CaipiaoTypeEnum.TENPKS.getTagType()));
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.FIVEPKS.getTagType())) {
                // 结算【北京PK10-两面】
                betBjpksService.clearingBjpksLm(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVEPKS.getTagType()));
                // 结算【北京PK10-猜名次猜前几】
                betBjpksService.clearingBjpksCmcCqj(issue, number,
                        Integer.parseInt(CaipiaoTypeEnum.FIVEPKS.getTagType()));
                // 结算【北京PK10-冠亚和】
                betBjpksService.clearingBjpksGyh(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVEPKS.getTagType()));
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.JSPKS.getTagType())) {
                // 结算【北京PK10-两面】
                betBjpksService.clearingBjpksLm(issue, number, Integer.parseInt(CaipiaoTypeEnum.JSPKS.getTagType()));
                // 结算【北京PK10-猜名次猜前几】
                betBjpksService.clearingBjpksCmcCqj(issue, number,
                        Integer.parseInt(CaipiaoTypeEnum.JSPKS.getTagType()));
                // 结算【北京PK10-冠亚和】
                betBjpksService.clearingBjpksGyh(issue, number, Integer.parseInt(CaipiaoTypeEnum.JSPKS.getTagType()));
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.XYFEIT.getTagType())) {
                // 结算【幸运飞艇-两面】
                betXyftService.clearingXyftLm(issue, number);
                // 结算【幸运飞艇-猜名次猜前几】
                betXyftService.clearingXyftCmcCqj(issue, number);
                // 结算【幸运飞艇-冠亚和】
                betXyftService.clearingXyftGyh(issue, number);
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.PCDAND.getTagType())) {
                // 结算【PC蛋蛋-特码】
                betPceggService.clearingPceggTm(issue, number);
                // 结算【PC蛋蛋-豹子】
                betPceggService.clearingPceggBz(issue, number);
                // 结算【PC蛋蛋-特码包三】
                betPceggService.clearingPceggTmbs(issue, number);
                // 结算【PC蛋蛋-色波】
                betPceggService.clearingPceggSb(issue, number);
                // 结算【PC蛋蛋-混合】
                betPceggService.clearingPceggHh(issue, number);
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.TXFFC.getTagType())) {
                // 结算【两面】
                betSscbmService.countlm(issue, number, Integer.parseInt(CaipiaoTypeEnum.TXFFC.getTagType()));
                // 基本【组选】规则
                betSscbmService.countdn(issue, number, Integer.parseInt(CaipiaoTypeEnum.TXFFC.getTagType()));
                // 【定位胆】规则
                betSscbmService.count15(issue, number, Integer.parseInt(CaipiaoTypeEnum.TXFFC.getTagType()));
                // 【定位大小单双】规则
                betSscbmService.countqzh(issue, number, Integer.parseInt(CaipiaoTypeEnum.TXFFC.getTagType()));
            }
//            else if (lotteryId.toString().equals(CaipiaoTypeEnum.DLT.getTagType())) {
//                betTcDltService.clearingTCDLT(issue, number, Integer.parseInt(CaipiaoTypeEnum.DLT.getTagType()));
//            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.TCPLW.getTagType())) {
//                betTcPlswService.clearingTcPlwLm(issue, number, Integer.parseInt(CaipiaoTypeEnum.TCPLW.getTagType()));
//                betTcPlswService.clearingTcPlsZx(issue, number, Integer.parseInt(CaipiaoTypeEnum.TCPLW.getTagType()));
//                betTcPlswService.clearingTcPlwZx(issue, number, Integer.parseInt(CaipiaoTypeEnum.TCPLW.getTagType()));
//                betTcPlswService.clearingTcPlwDwd(issue, number, Integer.parseInt(CaipiaoTypeEnum.TCPLW.getTagType()));
//            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.TC7XC.getTagType())) {
//                bet7xcHnService.countHn7xc(issue, number, Integer.parseInt(CaipiaoTypeEnum.TC7XC.getTagType())); // 5,4,7,4
//            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.FCSSQ.getTagType())) {
//                betFcSsqService.clearingFCSSQ(issue, number, Integer.parseInt(CaipiaoTypeEnum.FCSSQ.getTagType()));
//            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.FC3D.getTagType())) {
//                bet3dFcService.countFc3d(issue, number, Integer.parseInt(CaipiaoTypeEnum.FC3D.getTagType())); // 1,2,0,1
//            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.FC7LC.getTagType())) {
//                betFc7lcService.clearingFC7LC(issue, number, Integer.parseInt(CaipiaoTypeEnum.FC7LC.getTagType()));
//            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.KLNIU.getTagType())) {
//                // 结算【快乐牛牛-闲家】
//                betNnKlService.countKlXianjia(issue, number, Integer.parseInt(CaipiaoTypeEnum.KLNIU.getTagType()));
//            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.AZNIU.getTagType())) {
//                // 结算【澳洲牛牛-闲家】
//                betNnAzService.countAzXianjia(issue, number, Integer.parseInt(CaipiaoTypeEnum.AZNIU.getTagType()));
//            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.JSNIU.getTagType())) {
//                // 结算【德州牛牛-闲家】
//                betNnJsService.countJsXianjia(issue, number, Integer.parseInt(CaipiaoTypeEnum.JSNIU.getTagType()));
//            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.JSPKFT.getTagType())) {
//                // 结算【德州pk番摊】
//                betFtJspksService.clearingJspksJs(issue, number, Integer.parseInt(CaipiaoTypeEnum.JSPKFT.getTagType()));
//            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.XYFTFT.getTagType())) {
//                // 结算【番摊】
//                betFtXyftService.clearingFtXyftJs(issue, number, Integer.parseInt(CaipiaoTypeEnum.XYFTFT.getTagType()));
//            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.JSSSCFT.getTagType())) {
//                // 结算【德州时时彩番摊】
//                betFtSscService.clearingFtSscJs(issue, number, Integer.parseInt(CaipiaoTypeEnum.JSSSCFT.getTagType()));
//            }
            else if (lotteryId.toString().equals(CaipiaoTypeEnum.AUSACT.getTagType())) {
                // 澳洲act结算
                betActAzService.countAzAct(issue, number, Integer.parseInt(CaipiaoTypeEnum.AUSACT.getTagType()));
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.AUSSSC.getTagType())) {
                // 结算【澳洲时时彩】
                betSscAzService.countAzSsc(issue, number, Integer.parseInt(CaipiaoTypeEnum.AUSSSC.getTagType()));
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.AUSPKS.getTagType())) {
                // 结算【澳洲F1】
                betF1AzService.countAzF1(issue, number, Integer.parseInt(CaipiaoTypeEnum.AUSPKS.getTagType()));
            }else  if (lotteryId.toString().equals(CaipiaoTypeEnum.AZKS.getTagType())) {
                // 结算【澳洲快三 两面】
                betksService.clearingKsLm(issue, number, Integer.parseInt(CaipiaoTypeEnum.AZKS.getTagType()));
                // 结算【澳洲快三 独胆】
                betksService.clearingKsDd(issue, number, Integer.parseInt(CaipiaoTypeEnum.AZKS.getTagType()));
                // 结算【澳洲快三 连号】
                betksService.clearingKsLh(issue, number, Integer.parseInt(CaipiaoTypeEnum.AZKS.getTagType()));
                // 结算【澳洲快三 二不同号  二同号】
                betksService.clearingKsEbTh(issue, number, Integer.parseInt(CaipiaoTypeEnum.AZKS.getTagType()));
                // 结算【澳洲快三 三不同号 三同号】
                betksService.clearingKsSbTh(issue, number, Integer.parseInt(CaipiaoTypeEnum.AZKS.getTagType()));
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.DZKS.getTagType())) {
                // 结算【德州快三 两面】
                betksService.clearingKsLm(issue, number, Integer.parseInt(CaipiaoTypeEnum.DZKS.getTagType()));
                // 结算【德州快三 独胆】
                betksService.clearingKsDd(issue, number, Integer.parseInt(CaipiaoTypeEnum.DZKS.getTagType()));
                // 结算【德州快三 连号】
                betksService.clearingKsLh(issue, number, Integer.parseInt(CaipiaoTypeEnum.DZKS.getTagType()));
                // 结算【德州快三 二不同号  二同号】
                betksService.clearingKsEbTh(issue, number, Integer.parseInt(CaipiaoTypeEnum.DZKS.getTagType()));
                // 结算【德州快三 三不同号 三同号】
                betksService.clearingKsSbTh(issue, number, Integer.parseInt(CaipiaoTypeEnum.DZKS.getTagType()));
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.DZPCDAND.getTagType())) {
                // 结算【德州PPC蛋蛋-特码】
                betDzpceggService.clearingDzpceggTm(issue, number);
                // 结算【德州PPC蛋蛋-豹子】
                betDzpceggService.clearingDzpceggBz(issue, number);
                // 结算【德州PPC蛋蛋-特码包三】
                betDzpceggService.clearingDzpceggTmbs(issue, number);
                // 结算【德州PPC蛋蛋-色波】
                betDzpceggService.clearingDzpceggSb(issue, number);
                // 结算【德州PPC蛋蛋-混合】
                betDzpceggService.clearingDzpceggHh(issue, number);
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.DZXYFEIT.getTagType())) {
                // 结算【德州幸运飞艇-两面】
                betDzxyftService.clearingDzxyftLm(issue, number);
                // 结算【德州幸运飞艇-猜名次猜前几】
                betDzxyftService.clearingDzxyftCmcCqj(issue, number);
                // 结算【德州幸运飞艇-冠亚和】
                betDzxyftService.clearingDzxyftGyh(issue, number);
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.XJPLHC.getTagType())) {
                // 结算新加坡六合彩- 【特码,正特,六肖,正码1-6】
                betNewLhcService.clearingLhcTeMaA(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                betNewLhcService.clearingLhcZhengTe(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                betNewLhcService.clearingLhcZhengMaOneToSix(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                betNewLhcService.clearingLhcLiuXiao(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);

                // 新加坡结算六合彩- 【正码,半波,尾数】
                betNewLhcService.clearingLhcZhengMaA(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                betNewLhcService.clearingLhcBanBo(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                betNewLhcService.clearingLhcWs(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);

                // 新加坡结算六合彩- 【连码,连肖,连尾】
                betNewLhcService.clearingLhcLianMa(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                betNewLhcService.clearingLhcLianXiao(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                betNewLhcService.clearingLhcLianWei(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);

//                 新加坡结算六合彩- 【不中,1-6龙虎,五行】
                betNewLhcService.clearingLhcNoOpen(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                betNewLhcService.clearingLhcOneSixLh(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                betNewLhcService.clearingLhcWuxing(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);

                // 新加坡结算六合彩- 【平特,特肖】
                betNewLhcService.clearingLhcPtPt(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                betNewLhcService.clearingLhcTxTx(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
            }


            return ResultInfo.ok(true);
        } catch (Exception e) {
            logger.error("bet7xcHnService occur error.", e);
            return ResultInfo.error("结算失败!");
        }
    }

    @Override
    public ResultInfo cancelOrderBetByIssue(Integer lotteryId, String issue) {
        try {
            OrderBetRecordExample betExample = new OrderBetRecordExample();
            OrderBetRecordExample.Criteria betCriteria = betExample.createCriteria();
            betCriteria.andLotteryIdEqualTo(lotteryId);
            betCriteria.andTbStatusEqualTo(OrderBetStatus.WAIT);
            betCriteria.andIssueEqualTo(issue);
            betCriteria.andIsDeleteEqualTo(false);

            List<OrderBetRecord> orderBetRecordList = orderBetRecordMapper.selectByExample(betExample);
            if (orderBetRecordList == null || orderBetRecordList.size() == Constants.BANK_CARD_BINGDING_PROHABIT) {
                return ResultInfo.error("此期号彩种以开奖，不能撤单");
            }
            for (OrderBetRecord record : orderBetRecordList) {
                record.setTbStatus(OrderBetStatus.BACK);
                betCommonService.winOrLose(record, BigDecimal.ZERO, record.getUserId(), record.getOrderSn());
            }
            return ResultInfo.ok();
        } catch (Exception e) {
            logger.error("根据期号撤单失败：{},{}", lotteryId, issue, e);
            return ResultInfo.error("根据期号撤单失败");
        }

    }

    //   六合彩结算
    @Override
    public ResultInfo<Boolean> jiesuanBySg(String issue, String openNumber) {
        // 根据开奖结果进行派彩
        // 将赛果推送到六合彩相关队列
//        rabbitTemplate.convertAndSend(RabbitConfig.TOPIC_EXCHANGE, RabbitConfig.BINDING_LHC, "LHC:" + issue + ":" + number);
//		String jiesuanMessage = String.valueOf(basicRedisClient.hGet(Constants.LHC_KAIJIANG_STATUS, "JIESUAN_MESSAGE"));
        if (issue != null && openNumber != null) {
            // 查询数据库的开奖号码对不对
            LhcLotterySgExample example = new LhcLotterySgExample();
            LhcLotterySgExample.Criteria criteria = example.createCriteria();
            criteria.andYearEqualTo(issue.substring(0, 4));
            criteria.andIssueEqualTo(issue.substring(4, 7));
            LhcLotterySg sg = lhcLotterySgMapper.selectOneByExample(example);

            if (sg == null && openNumber != null) {
                String numberArray[] = openNumber.split(",");
                String numbers = "";
                for (String num : numberArray) {
                    if (num.length() == 1) {
                        num = "0" + num;
                    }
                    numbers = numbers + (num + ",");
                }
                numbers = numbers.substring(0, numbers.length() - 1);
                openNumber = numbers;

                sg = new LhcLotterySg();
                sg.setNumber(openNumber);
                sg.setTime(DateUtils.formatDate(new Date(), DateUtils.FORMAT_YYYY_MM_DD));
                sg.setYear(issue.substring(0, 4));
                sg.setIssue(issue.substring(4, 7));
                lhcLotterySgMapper.insertSelective(sg);
            }

            String key = issue + openNumber + "jiesuan_live";
            RedisLock lock = new RedisLock(key + "lock", 50 * 1000, 30 * 1000);
            try {
                // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
//            boolean bool = lock.writeLock().tryLock(2, 10, TimeUnit.SECONDS);
                // 判断是否获取到锁
                if (lock.lock()) {
                    if (basicRedisClient.get(key) == null) {
                        basicRedisClient.set(key, "1", 60 * 120l);
                        int count = 0;
                        while (true) {
                            try {
                                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_LHC_TM_ZT_LX, "LHC:" + issue + ":" + openNumber);
                                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_LHC_ZM_BB_WS, "LHC:" + issue + ":" + openNumber);
                                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_LHC_LM_LX_LW, "LHC:" + issue + ":" + openNumber);
                                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_LHC_BZ_LH_WX, "LHC:" + issue + ":" + openNumber);
                                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_LHC_PT_TX, "LHC:" + issue + ":" + openNumber);
                            } catch (Exception e1) {
                                logger.error("MQ发消息异常", e1);
                            }

                            count++;
                            if (count > 100) {
                                logger.error("系统发送了【{}】次结算通知还未结算完，不发通知了，可在页面上操作重新触发", count);
                                break;
                            }

                            try {
                                Thread.sleep(20000);
                            } catch (Exception e) {
                                logger.error("", e);
                            }

                            // 查询是否还有未结算的订单，如无则退出；
                            OrderBetRecordExample orderBetExample = new OrderBetRecordExample();
                            OrderBetRecordExample.Criteria orderBetCriteria = orderBetExample.createCriteria();
                            orderBetCriteria.andIssueEqualTo(issue);
                            orderBetCriteria.andTbStatusEqualTo(Constants.WAIT);// 等待开奖
                            int n = orderBetRecordMapper.countByExample(orderBetExample);
                            logger.info("第[{}]次发送结算通知,系统中还有[{}]记录未结算", count, n);
                            if (n < 1) {
                                break;
                            }
                        }

                        //释放锁
                        try {
                            basicRedisClient.del(key);
                            lock.unlock();
                        } catch (Exception e) {
                            logger.error("", e);
                        }
                        try {
                            lock.unlock();
                        } catch (Exception e) {
                            logger.error("", e);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("六合彩jiesuanLuc occur error.", e);
                return ResultInfo.error("结算失败!");
            }
        }
        return ResultInfo.ok(true);
    }


    @Override
    public ResultInfo<Boolean> jiesuanByHandle(String issue, String openNumber) {
        // 根据开奖结果进行派彩
        // 将赛果推送到六合彩相关队列
//        rabbitTemplate.convertAndSend(RabbitConfig.TOPIC_EXCHANGE, RabbitConfig.BINDING_LHC, "LHC:" + issue + ":" + number);
//		String jiesuanMessage = String.valueOf(basicRedisClient.hGet(Constants.LHC_KAIJIANG_STATUS, "JIESUAN_MESSAGE"));
        if (issue != null && openNumber != null) {
            // 查询数据库的开奖号码对不对
            LhcLotterySgExample example = new LhcLotterySgExample();
            LhcLotterySgExample.Criteria criteria = example.createCriteria();
            criteria.andYearEqualTo(issue.substring(0, 4));
            criteria.andIssueEqualTo(issue.substring(4, 7));
            LhcLotterySg sg = lhcLotterySgMapper.selectOneByExample(example);

            if (openNumber != null) {
                String numberArray[] = openNumber.split(",");
                String numbers = "";
                for (String num : numberArray) {
                    if (num.length() == 1) {
                        num = "0" + num;
                    }
                    numbers = numbers + (num + ",");
                }
                numbers = numbers.substring(0, numbers.length() - 1);
                openNumber = numbers;
            }

            if (sg != null && StringUtils.isNotBlank(sg.getNumber()) && !sg.getNumber().equals(openNumber)) {
                // 更新赛果结果，
                sg.setNumber(openNumber);
                int i = lhcLotterySgMapper.updateByPrimaryKey(sg);
                if (i > 0) {
                    logger.info("六合彩期号:{}更新赛果:{}成功", issue, openNumber);
                } else {
                    logger.info("六合彩期号:{}更新赛果:{}失败", issue, openNumber);
                }
            }

            String key = issue + "jiesuan";
            RedisLock lock = new RedisLock(key + "lock", 50 * 1000, 30 * 1000);
            try {
                // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
//            boolean bool = lock.writeLock().tryLock(2, 10, TimeUnit.SECONDS);
                // 判断是否获取到锁
                if (lock.lock()) {
                    if (basicRedisClient.get(key) == null) {
                        basicRedisClient.set(key, "1", 60 * 120l);
                        int count = 0;
                        while (true) {
                            try {
                                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_LHC_TM_ZT_LX, "LHC:" + issue + ":" + openNumber);
                                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_LHC_ZM_BB_WS, "LHC:" + issue + ":" + openNumber);
                                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_LHC_LM_LX_LW, "LHC:" + issue + ":" + openNumber);
                                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_LHC_BZ_LH_WX, "LHC:" + issue + ":" + openNumber);
                                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_LHC_PT_TX, "LHC:" + issue + ":" + openNumber);
                            } catch (Exception e1) {
                                logger.error("MQ发消息异常", e1);
                            }

                            count++;
                            if (count > 100) {
                                logger.error("系统发送了【{}】次结算通知还未结算完，不发通知了，可在页面上操作重新触发", count);
                                break;
                            }

                            try {
                                Thread.sleep(20);
                            } catch (Exception e) {
                                logger.error("", e);
                            }

                            // 查询是否还有未结算的订单，如无则退出；
                            OrderBetRecordExample orderBetExample = new OrderBetRecordExample();
                            OrderBetRecordExample.Criteria orderBetCriteria = orderBetExample.createCriteria();
                            orderBetCriteria.andIssueEqualTo(issue);
                            orderBetCriteria.andTbStatusEqualTo(Constants.WAIT);// 等待开奖
                            int n = orderBetRecordMapper.countByExample(orderBetExample);
                            logger.info("第[{}]次发送结算通知,系统中还有[{}]记录未结算", count, n);
                            if (n < 1) {
                                break;
                            }
                        }

                        //释放锁
                        try {
                            basicRedisClient.del(key);
                            lock.unlock();
                        } catch (Exception e) {
                            logger.error("", e);
                        }
                        try {
                            lock.unlock();
                        } catch (Exception e) {
                            logger.error("", e);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("六合彩jiesuanLuc occur error.", e);
                return ResultInfo.error("结算失败!");
            }

            redisTemplate.opsForHash().delete(Constants.LHC_KAIJIANG_STATUS, "JIESUAN_MESSAGE");

        }
        return ResultInfo.ok(true);
    }

    @Override
    public ResultInfo<Boolean> jiesuanByHandleFalse(String issue, String openNumber) { // 假结算，生成结算临时数据
        if (issue != null && openNumber != null) {
            try {
                if (openNumber != null) {
                    String numberArray[] = openNumber.split(",");
                    String numbers = "";
                    for (String num : numberArray) {
                        if (num.length() == 1) {
                            num = "0" + num;
                        }
                        numbers = numbers + (num + ",");
                    }
                    numbers = numbers.substring(0, numbers.length() - 1);
                    openNumber = numbers;
                }

                // 结算六合彩- 【特码,正特,六肖,正码1-6】
                betLhcService.clearingLhcTeMaA(issue, openNumber, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()),
                        false);
                betLhcService.clearingLhcZhengTe(issue, openNumber, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()),
                        false);
                betLhcService.clearingLhcZhengMaOneToSix(issue, openNumber,
                        Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()), false);
                betLhcService.clearingLhcLiuXiao(issue, openNumber, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()),
                        false);

                // 结算六合彩- 【正码,半波,尾数】
                betLhcService.clearingLhcZhengMaA(issue, openNumber, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()),
                        false);
                betLhcService.clearingLhcBanBo(issue, openNumber, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()),
                        false);
                betLhcService.clearingLhcWs(issue, openNumber, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()),
                        false);

                // 结算六合彩- 【连码,连肖,连尾】
                betLhcService.clearingLhcLianMa(issue, openNumber, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()),
                        false);
                betLhcService.clearingLhcLianXiao(issue, openNumber, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()),
                        false);
                betLhcService.clearingLhcLianWei(issue, openNumber, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()),
                        false);

                // 结算六合彩- 【不中,1-6龙虎,五行】
                betLhcService.clearingLhcNoOpen(issue, openNumber, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()),
                        false);
                betLhcService.clearingLhcOneSixLh(issue, openNumber, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()),
                        false);
                betLhcService.clearingLhcWuxing(issue, openNumber, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()),
                        false);

                // 结算六合彩- 【平特,特肖】
                betLhcService.clearingLhcPtPt(issue, openNumber, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()),
                        false);
                betLhcService.clearingLhcTxTx(issue, openNumber, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()),
                        false);
            } catch (Exception e) {
                logger.error("六合彩jiesuanLuc occur error.", e);
                return ResultInfo.error("结算失败!");
            }

        }
        return ResultInfo.ok(true);

    }

    /**
     * 【新疆时时彩】根据上期期号生成下期期号
     *
     * @param issue 上期期号
     * @return
     */
    private String xjsscNextIssue(String issue) {
        // 生成下一期期号
        String nextIssue;
        // 判断是否已达最大值
        if ("96".equals(issue.substring(8))) {
            String prefix = DateUtils.formatDate(new Date(), "yyyyMMdd");
            nextIssue = prefix + "01";
        } else {
            long next = Long.parseLong(issue) + 1;
            nextIssue = Long.toString(next);
        }
        return nextIssue;
    }

    /**
     * 【重庆时时彩】根据上期期号生成下期期号
     *
     * @param issue 上期期号
     * @return
     */
    private String cqsscNextIssue(String issue) {
        // 生成下一期期号
        String nextIssue;
        // 判断是否已达最大值
        if ("120".equals(issue.substring(8))) {
            String nextDay = TimeHelper.date("yyyyMMdd");
            nextIssue = nextDay + "001";
        } else {
            nextIssue = Long.toString(Long.valueOf(issue) + 1);
        }
        return nextIssue;
    }

    /**
     * 【比特币分分彩】根据上期期号生成下期期号
     *
     * @param issue 上期期号
     * @return
     */
    private String txffcNextIssue(String issue) {
        // 生成下一期期号
        String nextIssue;
        // 截取后四位
        String[] num = issue.split("-");
        // 判断是否已达最大值
        if ("1439".equals(num[1])) {
            String prefix = DateUtils.formatDate(DateUtils.getDayAfter(new Date(), 1L), "yyyyMMdd");
            nextIssue = prefix + "-0000";
        } else {
            StringBuilder next = new StringBuilder(Long.toString(Long.parseLong(num[1]) + 1));
            while (next.length() < 4) {
                next.insert(0, "0");
            }
            nextIssue = num[0] + "-" + next;
        }
        return nextIssue;
    }

    /**
     * 获取【重庆时时彩】第一期开奖时间
     *
     * @return
     */
    private Long getCqsscStartTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 10);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    /**
     * 获取【新疆时时彩】第一期开奖时间
     *
     * @return
     */
    private Long getXjsscStartTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 10);
        calendar.set(Calendar.MINUTE, 10);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    /**
     * 获取【PC蛋蛋】/ 【北京PK10】第一期开奖时间
     *
     * @return
     */
    private Long getPcddStartTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 5);
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        calendar.set(Calendar.MINUTE, 5);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    /**
     * 获取【幸运飞艇】第一期开奖时间
     *
     * @return
     */
    private Long getXyftStartTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 13);
        calendar.set(Calendar.MINUTE, 5);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private KillConfig getKillConfig(String lotteryid) {
        KillConfig killConfig = killConfigReadRest.getKillConfigByLotteryId(Integer.valueOf(lotteryid));
        return killConfig;
    }

    private void saveKillDataToRedis(String lotteryId, String issue, List<OrderBetRecord> orderBetList) {
        try {
            // 私彩杀号
            if (CaipiaoTypeEnum.SICAI.contains(lotteryId)) {
//                            1901:KLNIU  1105:FIVESSC    快乐牛牛和5分时时彩一样
//                            1903:JSNIU  1304:JSPKS    德州牛牛和极速pks 一样
//                            2001:JSPKFT  1304:JSPKS    德州PK10番摊 和极速pks 一样
//                            2003:JSSSCFT  1106:JSSSC    德州时时彩番摊 和极速时时彩一样

//                if (lotteryId.equals(CaipiaoTypeEnum.KLNIU.getTagType())) {
//                    lotteryId = CaipiaoTypeEnum.FIVESSC.getTagType();
//                } else if (lotteryId.equals(CaipiaoTypeEnum.JSNIU.getTagType()) || lotteryId.equals(CaipiaoTypeEnum.JSPKFT.getTagType())) {
//                    lotteryId = CaipiaoTypeEnum.JSPKS.getTagType();
//                } else if (lotteryId.equals(CaipiaoTypeEnum.JSSSCFT.getTagType())) {
//                    lotteryId = CaipiaoTypeEnum.JSSSC.getTagType();
//                }
                KillConfig killConfig = getKillConfig(lotteryId);
                double winratio = killConfig.getRatio();// 杀号比例
                // 获取杀号平台

                logger.info("杀号信息：杀号比例：{}，杀号根据平台：{}, 彩种：{}", winratio, killConfig.getPlatfom()
                        , lotteryId);
                logger.info("PLATFORM mq:{}", ActiveMQConfig.SICAIORDERKILL_NEW);
                if (winratio >= 0) {
                    for (OrderBetRecord betsc : orderBetList) {
                        OrderBetKillDto okill = new OrderBetKillDto();

                        if (Constants.NEW_OLDJIESUAN_LOTTERY_ID_LIST.contains(betsc.getLotteryId())) {
                            StringBuilder betNumber = new StringBuilder();
                            betNumber.append(betsc.getPlayName()).append("@")
                                    .append(betsc.getBetNumber());
                            betsc.setBetNumber(betNumber.toString());
                        }
                        BeanUtils.copyProperties(betsc, okill);

                        okill.setIssue(issue);
                        BigDecimal betCount = new BigDecimal(betsc.getBetCount());
                        BigDecimal betAmount = betsc.getBetAmount().divide(betCount);
                        okill.setBetAmount(betAmount);
                        Map<String, LotteryPlayOdds> oddsMap = lotteryPlayOddsService
                                .selectPlayOddsBySettingId(betsc.getSettingId());

                        String pname = null;
                        if (Constants.NEW_JIESUAN_LOTTERY_ID_LIST.contains(betsc.getLotteryId())) {
                            pname = betsc.getBetNumber();
                        } else {
                            pname = betsc.getBetNumber().split("@")[1];
                        }

                        double divisor = betCommonService.getDivisor(betsc.getLotteryId());
                        String odd = "";
                        if (pname.contains(",")) {
                            String[] pnamearr = pname.split(",");
                            for (String betnm : pnamearr) {
                                LotteryPlayOdds odds = oddsMap.get(betnm);
                                if (odds == null) {
                                    odds = oddsMap.get(betsc.getBetNumber().split("@")[0]);
                                    // 获取总注数/中奖注数
                                    String winCount = odds.getWinCount();
                                    String totalCount = odds.getTotalCount();
                                    if (totalCount.contains("/")) {
                                        odd = totalCount;
                                    } else {
                                        // 计算赔率
                                        odd = String.valueOf(Double.parseDouble(totalCount) * 1.0
                                                / Double.parseDouble(winCount) * divisor);
                                    }
                                    break;
                                }
                                // 获取总注数/中奖注数
                                String winCount = odds.getWinCount();
                                String totalCount = odds.getTotalCount();

                                // 计算赔率
                                String oddSingle = String.valueOf(Double.parseDouble(totalCount) * 1.0
                                        / Double.parseDouble(winCount) * divisor);
                                odd = odd + (betnm + ":" + oddSingle + ",");
                            }

                            if (odd.endsWith(",")) {
                                odd = odd.substring(0, odd.length() - 1);
                            }
                        } else {
                            LotteryPlayOdds odds = oddsMap.get(pname);
                            if (odds == null && oddsMap.size() == 1) {
                                for (Map.Entry<String, LotteryPlayOdds> entry : oddsMap.entrySet()) {
                                    odds = entry.getValue();
                                }

                            }
                            // 获取总注数/中奖注数
                            String winCount = odds.getWinCount();
                            String totalCount = odds.getTotalCount();
                            if (totalCount.contains("/")) {
                                odd = totalCount;
                            } else {
                                // 计算赔率
                                odd = String.valueOf(Double.parseDouble(totalCount) * 1.0
                                        / Double.parseDouble(winCount) * divisor);
                            }
                        }

                        okill.setOdds(odd);
                        okill.setKillConfig(killConfig);
                        //跟新杀号时间
                        if (redisTemplate.hasKey(RedisKeys.KILLORDERTIME)) {
                            okill.setWaittime(Integer.parseInt((String) redisTemplate.opsForValue().get(RedisKeys.KILLORDERTIME)) * 1000);
                        }
                        Destination sicaikill = null;
                        if (Constants.NEW_LOTTERY_ID_LIST.contains(okill.getLotteryId())) { // 新彩种
                            sicaikill = new ActiveMQQueue(ActiveMQConfig.SICAIORDERKILL_NEW);
                        } else { // 老彩种
                            sicaikill = new ActiveMQQueue(ActiveMQConfig.LIVESICAIORDERKILL);
                        }

                        // 私彩订单推送到队列
                        jmsMessagingTemplate.convertAndSend(sicaikill,
                                "orderkill:" + JSON.toJSONString(okill));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("私彩杀号传 订单数据出错", e);
        }
    }

    @Override
    public ResultInfo<String> shareOrder(ShareOrderDTO data) {
        String key = "shareOrder" + data.getOrderBetId().toString();
        RReadWriteLock lock = redissonClient.getReadWriteLock(key);
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(100, 10, TimeUnit.SECONDS);
            // 判断是否获取到锁
            if (bool) {
                if (new BigDecimal(data.getBetCount()).compareTo(BigDecimal.valueOf(0)) <= 0) {
                    return ResultInfo.error("跟投倍数不能为0或小于0！");
                }

                // 获取投注单id
                Integer orderBetId = data.getOrderBetId();
                // 获取投注信息
                OrderBetRecord orderBetRecord;
                if (redisTemplate.hasKey(ORDER_BET_KEY + orderBetId)) {
                    orderBetRecord = (OrderBetRecord) redisTemplate.opsForValue().get(ORDER_BET_KEY + orderBetId);
                } else {
                    orderBetRecord = orderBetRecordMapper.selectByPrimaryKey(orderBetId);
                }

                // 判空
                if (orderBetRecord == null) {
                    return ResultInfo.error("请选择要跟投的订单！");
                }

                // 判断是否已开奖
                if (!orderBetRecord.getTbStatus().equals(OrderBetStatus.WAIT)) {
                    return ResultInfo.error("该期已开奖，请注意跟投时间！");
                }

                // 获取订单信息
                OrderRecord orderRecord;
                if (redisTemplate.hasKey(ORDER_KEY + orderBetId)) {
                    orderRecord = (OrderRecord) redisTemplate.opsForValue().get(ORDER_KEY + orderBetId);
                } else {
                    orderRecord = orderRecordMapper.selectByPrimaryKey(orderBetRecord.getOrderId());
                    redisTemplate.opsForValue().set(ORDER_KEY + orderBetId, orderRecord);
                }

                // 获取投注限额信息

                // 获取用户信息
                Integer userId = data.getUserId();
                if (userId == null || userId < 1) {
                    logger.error("userId[{}]不能为空", userId);
                    return ResultInfo.error("该用户不存在！");
                }

                MemUser memUser = memUserMapper.selectByPrimaryKey((long) userId);

                if (memUser == null) {
                    return ResultInfo.error("该用户不存在或已注销！");
                }

                if (memUser.getIsFrozen()) {
                    return ResultInfo.error("该用户已被冻结，暂不支持购彩！");
                }


                // 获取用户投注总额
                BigDecimal betAmount = new BigDecimal(orderBetRecord.getBetCount())
                        .multiply(new BigDecimal(data.getBetCount()));

                BetRestrict restrict = this.getBonusMap(orderBetRecord.getLotteryId(), orderBetRecord.getPlayId());
                if (restrict != null && restrict.getMaxMoney().compareTo(BigDecimal.valueOf(0)) > 0) {
                    if (betAmount.compareTo(restrict.getMaxMoney()) > 0) {
                        return ResultInfo.error("该投注超过最大限制，请注意减少投注额！");
                    }
                } else {
                    // 获取彩种最大限制
                    BetRestrict betRestrict = this.getBonusMap(orderBetRecord.getLotteryId(), 0);
                    if (null != betRestrict && betRestrict.getMaxMoney().compareTo(BigDecimal.valueOf(0)) > 0) {
                        if (betAmount.compareTo(betRestrict.getMaxMoney()) > 0) {
                            return ResultInfo.error("该投注超过最大限制，请注意减少投注额！");
                        }
                    }

                }

                // 获取用户余额
                MemWalletExample memWalletExample = new MemWalletExample();
                MemWalletExample.Criteria walletCriteria = memWalletExample.createCriteria();
                walletCriteria.andUserIdEqualTo(Long.valueOf(userId));
                MemWallet wallet = memWalletMapper.selectOneByExample(memWalletExample);
                BigDecimal balance =wallet.getBalance();
                if (betAmount.compareTo(balance) > 0) {
                    return ResultInfo.error("余额不足！");
                }

                // 判断该期号是否已过投注时间
                ResultInfo<Boolean> resultInfo = this.checkIssueIsOpen(orderBetRecord.getLotteryId(),
                        orderRecord.getIssue(), 1);
                if (resultInfo.getCode() != StatusCode.SUCCESSCODE.getCode()) {
                    return ResultInfo.error("投注已过期！");
                }

                String orderSn = SnowflakeIdWorker.createOrderSn();
                // 生成订单
                OrderRecord order = new OrderRecord();
                BeanUtils.copyProperties(orderRecord, order);
                order.setId(null);
                order.setCreateTime(null);
                order.setUpdateTime(null);
                order.setStatus(OrderStatus.NORMAL);
                order.setUserId(data.getUserId());
                order.setIsDelete(false);
                order.setOrderSn(orderSn);
                order.setSource(data.getSource());
                order.setBuySource(KEY_FOUR);
                order.setUserId(userId);
                // 持久化到数据库
                orderRecordMapper.insertSelective(order);
                betAmount = getTradeOffAmount(betAmount);
                // 生成投注信息
                OrderBetRecord orderBet = new OrderBetRecord();
                BeanUtils.copyProperties(orderBetRecord, orderBet);
                orderBet.setId(null);
                orderBet.setIsDelete(false);
                orderBet.setCreateTime(null);
                orderBet.setUpdateTime(null);
                orderBet.setTbStatus(OrderBetStatus.WAIT);
                orderBet.setUserId(userId);
                orderBet.setOrderId(order.getId());
                orderBet.setBetAmount(betAmount);
                // 持久化到数据库
                orderBetRecordMapper.insertSelective(orderBet);

                //ONELIVE TODO 帐变记录
//                /**
//                 * 扣除用户余额
//                 */
//                MemGoldchangeDO dto = new MemGoldchangeDO();
//                // 设置用户id
//                dto.setUserId(order.getUserId());
//                // 设置备注
//                dto.setOpnote("投注/" + order.getOrderSn());
//                // 设置类型
//                dto.setChangetype(GoldchangeEnum.LOTTERY_BETTING.getValue());
//                // 余额变动值【负数】
//                dto.setQuantity(betAmount.negate());
//                // 计算不可提现金额变动值【负数】
//                dto.setNoWithdrawalAmount(betAmount.negate());
//                // 累计投注额【正数】
//                dto.setBetAmount(betAmount);
//                // 修改用户余额信息
//                dto.setWaitAmount(betAmount);
//                memBaseinfoWriteService.updateUserBalance(dto);

//                // 分享跟单中间表
//                ShareOrderRecord shareOrderRecord = new ShareOrderRecord();
//                shareOrderRecord.setShareId(orderRecord.getId());
//                shareOrderRecord.setShareUserId(orderRecord.getUserId());
//                shareOrderRecord.setOrderBetId(orderBet.getId());
//                shareOrderRecord.setOrderSn(orderSn);
//                shareOrderRecord.setIssue(orderRecord.getIssue());
//                shareOrderRecord.setGid(data.getGid());
//                shareOrderRecord.setCreateTime(new Date());
//                shareOrderRecordMapper.insertSelective(shareOrderRecord);

            }
        } catch (Exception e) {
            logger.error("shareOrder occur error.", e);
            throw new RuntimeException(e.getMessage());
        } finally {
            // 释放锁
            lock.writeLock().unlock();
        }
        return ResultInfo.ok("投注成功");
    }

    @Override
    @Transactional
    public ResultInfo liveRoomCopy(OrderFollow data) {
        String key = "liveRoomCopy" + data.getOrders().toString();
        RReadWriteLock lock = redissonClient.getReadWriteLock(key);
        logger.info("{}.getReadWriteLock,params:{}", key);
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(100, 10, TimeUnit.SECONDS);

            // 判断是否获取到锁
            if (bool) {
                Map<String, LotteryPlaySetting> lotterySettingMap = redisTemplate.opsForHash().entries(RedisKeys.LOTTERY_PLAY_SETTING_MAP_TYPE + Constants.LOTTERY_CATEGORY_TYPE_LOTTERY);
                if (CollectionUtil.isEmpty(lotterySettingMap)) {
                    lotterySettingMap = lotteryPlaySettingReadRest.queryLotteryPlaySettingMap(Constants.LOTTERY_CATEGORY_TYPE_LOTTERY);
                }
                if (null == lotterySettingMap) {
                    return ResultInfo.error("彩種對應玩法已關閉");
                }

                BigDecimal amount = orderBetRecordMapperExt.getOrderBetRecordAmount(data.getOrders());
                // 获取用户余额
                MemWalletExample memWalletExample = new MemWalletExample();
                MemWalletExample.Criteria walletCriteria = memWalletExample.createCriteria();
                walletCriteria.andUserIdEqualTo(Long.valueOf(data.getUserId()));
                MemWallet wallet = memWalletMapper.selectOneByExample(memWalletExample);
                BigDecimal balance =wallet.getBalance();

                // 获取用户余额
                if (balance == null || amount.compareTo(balance) > 0) {
                    logger.info("购彩余額不足");
                    ResultInfo response = ResultInfo.ok();
                    response.setCode(StatusCode.CHANGE_BALANCE_LACK.getCode());
                    response.setMsg("余額不足,請充值!");
                    return response;
                }
                for (Integer id : data.getOrders()) {
                    // 获取投注信息
                    OrderBetRecord orderBetRecord;
                    if (redisTemplate.hasKey(ORDER_BET_KEY + id)) {
                        orderBetRecord = (OrderBetRecord) redisTemplate.opsForValue().get(ORDER_BET_KEY + id);
                    } else {
                        orderBetRecord = orderBetRecordMapper.selectByPrimaryKey(id);
                        redisTemplate.opsForValue().set(ORDER_BET_KEY + id, orderBetRecord);
                    }

                    // 判空
                    if (orderBetRecord == null) {
                        return ResultInfo.error("請選擇要跟投的訂單！");
                    }
                    Integer orderId = orderBetRecord.getOrderId();
                    // 获取订单信息
                    OrderRecord orderRecord;
                    if (redisTemplate.hasKey(ORDER_KEY + orderId)) {
                        orderRecord = (OrderRecord) redisTemplate.opsForValue().get(ORDER_KEY + orderId);
                    } else {
                        orderRecord = orderRecordMapper.selectByPrimaryKey(orderId);
                        redisTemplate.opsForValue().set(ORDER_KEY + orderId, orderRecord);
                    }
                    if (orderRecord == null) {
                        return ResultInfo.error("請選擇要跟投的訂單！");
                    }
                    logger.info("下单信息:{}", orderBetRecord.toString());


                    if (null == lotterySettingMap.get(String.valueOf(orderBetRecord.getPlayId()))) {
                        return ResultInfo.error("此訂單對應彩種玩法信息已關閉");
                    }

                    // 判断是否已开奖
                    if (!orderBetRecord.getTbStatus().equals(OrderBetStatus.WAIT)) {
                        return ResultInfo.error("該期已開獎，請注意跟投時間！");
                    }

                    // 获取用户投注总额
                    BigDecimal betAmount = orderBetRecord.getBetAmount();
                    // BigDecimal amount = betAmount.multiply(new BigDecimal(orderBetRecord.getBetCount()));

                    BetRestrict restrict = this.getBonusMap(orderBetRecord.getLotteryId(), orderBetRecord.getPlayId());
                    if (restrict != null && restrict.getMaxMoney().compareTo(BigDecimal.valueOf(0)) > 0) {
                        if (betAmount.compareTo(restrict.getMaxMoney()) > 0) {
                            return ResultInfo.error("該投注超過最大限制，請注意減少投注額！");
                        }
                    } else {
                        // 获取彩种最大限制
                        BetRestrict betRestrict = this.getBonusMap(orderBetRecord.getLotteryId(), 0);
                        if (null != betRestrict && betRestrict.getMaxMoney().compareTo(BigDecimal.valueOf(0)) > 0) {
                            if (betAmount.compareTo(betRestrict.getMaxMoney()) > 0) {
                                return ResultInfo.error("該投注超過最大限制，請注意減少投注額！");
                            }
                        }
                    }

                    // 判断该期号是否已过投注时间
                    ResultInfo<Boolean> resultInfo = this.checkIssueIsOpen(orderBetRecord.getLotteryId(),
                            orderRecord.getIssue(), 1);
                    if (resultInfo.getCode() != StatusCode.SUCCESSCODE.getCode()) {
                        return ResultInfo.error("投注已過期！");
                    }
                    String orderSn = SnowflakeIdWorker.createOrderSn();
                    // 生成订单
                    OrderRecord order = new OrderRecord();
                    BeanUtils.copyProperties(orderRecord, order);
                    order.setId(null);
                    order.setCreateTime(null);
                    order.setUpdateTime(null);
                    order.setStatus(OrderStatus.NORMAL);
                    order.setIsDelete(false);
                    order.setOrderSn(orderSn);
                    order.setSource(data.getSource());
                    order.setUserId(data.getUserId());
                    order.setBuySource(KEY_FIVES);//代表直播间跟单
                    // 持久化到数据库
                    orderRecordMapper.insertSelective(order);
                    logger.info("订单投注:{}", order.toString());
                    // 生成投注信息
                    OrderBetRecord orderBet = new OrderBetRecord();
                    BeanUtils.copyProperties(orderBetRecord, orderBet);
                    orderBet.setId(null);
                    orderBet.setIsDelete(false);
                    orderBet.setCreateTime(null);
                    orderBet.setUpdateTime(null);
                    orderBet.setTbStatus(OrderBetStatus.WAIT);
                    orderBet.setUserId(data.getUserId());
                    orderBet.setOrderId(order.getId());
                    orderBet.setBetAmount(getTradeOffAmount(betAmount));
                    orderBet.setIsPush(Constants.DEFAULT_ONE);
                    orderBet.setCateId(orderBetRecord.getCateId());
                    orderBet.setSource(data.getSource());
                    orderBet.setRoomId(data.getRoomId());
                    // 持久化到数据库
                    orderBetRecordMapper.insertSelective(orderBet);
                    logger.info("子订单投注:{}", orderBet.toString());

                    /**
                     *   ONELIVE TODO 用户余额更新
                     */

                }
            }
        } catch (Exception e) {
            logger.error("liveRoomCopy occur error.", e);
            throw new RuntimeException(e.getMessage());
        } finally {
            // 释放锁
            lock.writeLock().unlock();
        }
        return ResultInfo.ok();

    }

    private Integer getCateIdByLotteryIdCache(Integer lotteryId) {
        HashMap<Integer, Integer> map = RedisBusinessUtil.get(RedisKeys.LOTTERY_CATEGORY_ID);
        HashMap<Integer, Integer> maps = new HashMap<>();
        if (map == null) {
            List<LotteryCateidVo> list = lotteryMapperExt.getCateIdBlylLotteryId();
            for (LotteryCateidVo vo : list) {
                if (null != vo) {
                    maps.put(vo.getLotteryId(), vo.getCategoryId());
                } else {
                    continue;
                }

            }
            RedisBusinessUtil.setCateIdByLotteryIdCache(RedisKeys.LOTTERY_CATEGORY_ID, maps);
            return maps.get(lotteryId);
        } else {
            if (map.get(lotteryId.toString()) == null) {
                RedisBusinessUtil.deleteByKeys(RedisKeys.LOTTERY_CATEGORY_ID);
                List<LotteryCateidVo> list = lotteryMapperExt.getCateIdBlylLotteryId();
                for (LotteryCateidVo vo : list) {
                    if (null != vo) {
                        maps.put(vo.getLotteryId(), vo.getCategoryId());
                    } else {
                        continue;
                    }
                }
                RedisBusinessUtil.setCateIdByLotteryIdCache(RedisKeys.LOTTERY_CATEGORY_ID, maps);
                return maps.get(lotteryId);
            } else {
                return map.get(lotteryId.toString());
            }

        }
    }

    @Override
    public ResultInfo<Boolean> changeOrderBetById(Integer id, String betNumber) {
        OrderBetRecord orderBetRecord = orderBetRecordMapper.selectByPrimaryKey(id);
        orderBetRecord.setBetNumber(betNumber);
        int update = orderBetRecordMapper.updateByPrimaryKey(orderBetRecord);
        if(update>0){
            return ResultInfo.ok(true);
        }
        return ResultInfo.error("该订单修改中奖号码失败");
    }

}
