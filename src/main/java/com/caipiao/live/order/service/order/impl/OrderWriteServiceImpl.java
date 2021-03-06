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



    // ??????????????????
    private static final Integer KEY_FOUR = 4;
    //?????????????????????
    private static final Integer KEY_FIVES = 7;
    private final String WAIT_SETTLEMENT_ORDER_PREFIX = "order_settlement_order_id_";

    @Override
    public List<OrderRecord> selectOrders(Integer lotteryId, String issue, String status) {
        List<OrderRecord> list = new ArrayList<>();
        // ????????????
        if (lotteryId == null || StringUtils.isBlank(issue) || StringUtils.isBlank(status)) {
            return list;
        }

//        // TODO ????????????????????????????????????
        OrderRecordExample orderExample = new OrderRecordExample();
        OrderRecordExample.Criteria orderCriteria = orderExample.createCriteria();
        orderCriteria.andIssueEqualTo(issue);
        orderCriteria.andLotteryIdEqualTo(lotteryId);
        orderCriteria.andStatusEqualTo(status);
        orderCriteria.andIsDeleteEqualTo(false);
        list = orderRecordMapper.selectByExample(orderExample);

//        // TODO ????????????????????????????????????end

        // ?????????start
//        // ????????????????????????????????????
//        if (redisTemplate.hasKey(ORDER_KEY + lotteryId + "_" + issue)) {
//            list = (List<OrderRecord>) redisTemplate.opsForValue().get(ORDER_KEY + lotteryId + "_" + issue);
//        }
//        // ??????????????????????????????
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
        // ?????????end
        return list;
    }

    @Override
    public List<OrderRecord> selectOrdersPage(Integer lotteryId, String issue, String status, int pageNo) {
        List<OrderRecord> list = new ArrayList<>();
        // ????????????
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

        // ????????????
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
            // ?????????????????????100s???????????????10S[????????????]????????????????????????????????????????????????????????????????????????????????????
            boolean bool = lock.writeLock().tryLock(1000, 100, TimeUnit.SECONDS);
            // ????????????????????????
            if (bool) {
//				Integer id = redisTemplate.opsForValue().get(key) == null ? 0
//						: (Integer) redisTemplate.opsForValue().get(key);              //????????????????????????????????????issue ????????????????????? ????????????????????? ????????????????????????????????????????????? ????????????????????????
                Integer id = 0;
                list = orderMapperExt.selectOrderBetList(issue, playIds, lotteryId, status, id, 0, Constants.CLEARNUM);
                logger.info("?????????????????????issue[{}],playIds[{}],lotteryId[{}],status[{}],id[{}],size[{}]", issue, playIds,
                        lotteryId, status, id, list.size());
                if (!CollectionUtils.isEmpty(list)) {
                    List<OrderBetRecord> tmpList = new ArrayList<>();
                    // ??????????????????
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
                    // ????????????...???????????????playID....
                    if (String.valueOf(playIds.get(0)).replace(lotteryId, "")
                            .equals(BetLhcServiceImpl.PLAY_ID_ZM_ZMA)) {

                        if ("OnePlayMany".equals(type)) {
                            for (OrderBetRecord clearo : list) {
                                if (clearo.getBetNumber().contains("??????")) {
                                    clearlist.add(clearo);
                                }
                            }
                        }
                        if ("OnePlay".equals(type)) {
                            for (OrderBetRecord clearo : list) {
                                if (!clearo.getBetNumber().contains("??????")) {
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
        // ??????????????????
        Integer userId = orderDTO.getUserId();
        // ???????????? ?????????????????????
        String keySuffix = userId + "_orderBet";
        String intercept = (String) redisTemplate.opsForValue().get(keySuffix);
        if (org.apache.commons.lang3.StringUtils.isNotBlank(intercept)) {
            return ResultInfo.error("????????????????????????");
        }
        if (org.apache.commons.lang3.StringUtils.isBlank(intercept)) {
            redisTemplate.opsForValue().set(keySuffix, "1", 1, TimeUnit.SECONDS);
        }

        long startTime = System.currentTimeMillis();
        // ????????????
        Integer lotteryId = orderDTO.getLotteryId();
        // ???????????????????????????
        String issue = orderDTO.getIssue();

        // ????????????????????????
        logger.info("this.getBonusMap(), {}", System.currentTimeMillis() - startTime);

        try {
            logger.info("redis, {}", System.currentTimeMillis() - startTime);
            // ????????????????????????
            BigDecimal amount = new BigDecimal(0);
            // ????????????????????????
            BetRestrict betRestrict = this.getBonusMap(lotteryId, 0);
            for (OrderBetRecord bet : orderDTO.getOrderBetList()) {
                Integer playId = bet.getPlayId();

                BigDecimal betCount = new BigDecimal(bet.getBetCount());
                BigDecimal betAmount = betCount.multiply(bet.getBetAmount());
                if (new BigDecimal(betAmount.intValue()).compareTo(betAmount) != 0
                        || betAmount.compareTo(new BigDecimal(0)) <= 0) {
                    return ResultInfo.error("??????????????????????????????");
                }

                BetRestrict restrict = this.getBonusMap(lotteryId, playId);
                if (restrict != null && restrict.getMaxMoney().compareTo(BigDecimal.valueOf(0)) > 0) {
                    if (betAmount.compareTo(restrict.getMaxMoney()) > 0) {
                        return ResultInfo.error("?????????????????????????????????????????????????????????");
                    }
                } else {
                    if (null != betRestrict && betRestrict.getMaxMoney().compareTo(BigDecimal.valueOf(0)) > 0) {
                        if (betAmount.compareTo(betRestrict.getMaxMoney()) > 0) {
                            return ResultInfo.error("?????????????????????????????????????????????????????????");
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
                return ResultInfo.error("???????????????");
            }
            logger.debug("redis balance : {}, bet amount : [{}]", balance, amount);
//            } else {
//                logger.info("????????????????????????{}", String.valueOf(userId));
//            }
        } catch (Exception e) {
            logger.error("order occur error:{}", e);
            return ResultInfo.error("???????????????");
        } /*
         * finally { lock.unlock(); logger.info("?????????????????????{}", String.valueOf(userId)); }
         */
        // ???????????????????????????????????????
        if (lotteryId == 1201) {
            ResultInfo<Boolean> resultInfo = this.checkIssueIsOpen(lotteryId, issue, 1);
            if (resultInfo.getCode() != StatusCode.SUCCESSCODE.getCode()) {
                return resultInfo;
            }
        }
        logger.info("checkIssueIsOpen, {}", System.currentTimeMillis() - startTime);
        // ?????????????????????
//        rabbitTemplate.convertAndSend(RabbitConfig.TOPIC_EXCHANGE, RabbitConfig.BINDING_ORDER, "ORDER:" + JSON.toJSONString(orderDTO));
        // ???????????????
        orderEventSentService.sendOrderJson(JSON.toJSONString(orderDTO));

//		String productOrderEnvt = ActiveMqConsumerListen.PRODUCTORDERENVI;
//		String queueName = ActiveMQConfig.QUEUE_ORDER + productOrderEnvt;
//		// ??????????????????
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
//                logger.error("{}.sendMsg ???????????????????????? params:{}", this.getClass().getName(), JSONObject.toJSON(room));
//                return;
//            }
//            if (CollectionUtil.isEmpty(orderDTO.getOrderBetList())) {
//                logger.error("{}.sendMsg ????????????????????? params:{}", this.getClass().getName(), JSONObject.toJSON(room));
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
//            talkBody.setMessage(memBaseinfo.getNickname() + "???" + lottery.getName() + "????????????" + zongxiazhuedu + "??????");
//            talkBody.setParam(orderId.toString());
//            publishService.publish(room.getRoomid(), talkBody);
//            logger.info("{}.sendMsg ?????????????????????params:{}", this.getClass().getName(), JSONObject.toJSON(talkBody).toString());
//        } catch (Exception e) {
//            logger.error("{}.processOrder ??????WS????????????,params:{},ordersn: {}", this.getClass().getName(), JSONObject.toJSON(orderDTO), ordersn, e);
//        }
//    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public void processOrder(String orderjson) {
        OrderDTO orderDTO = null;
        try {
            logger.info("????????????:" + orderjson);
            // ???????????????
            orderDTO = JSON.parseObject(orderjson, new TypeReference<OrderDTO>() {
            });
            // ?????????
            RedisLock lock = new RedisLock(String.valueOf(orderDTO.getUserId()), 50 * 1000, 30 * 1000);
            try {
                if (lock.lock()) {

                    orderDTO.setReOrderNum(orderDTO.getReOrderNum() + 1);
                    OrderRecord order = new OrderRecord();
                    BeanUtils.copyProperties(orderDTO, order);
                    // ???????????????
                    order.setOrderSn(SnowflakeIdWorker.createOrderSn());
                    // ?????????????????????
                    logger.info("????????????1:" + order.toString());
                    orderRecordMapper.insertSelective(order);
                    String source = order.getSource();
                    logger.info("????????????2:" + order.toString());
                    List<OrderBetRecord> orderBetList = orderDTO.getOrderBetList();
                    // ????????????????????????
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
                    //??????cateId
                    Integer categoryId = this.getCateIdByLotteryIdCache(order.getLotteryId());
                    if (categoryId == null) {
                        return;
                    }
                    //ONELIVE TODO ????????????
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
                           // dto.setChangetype(GoldchangeEnum.LIVEROOM_BET.getValue());//??????????????????????????????
                        } else {
                          //  dto.setChangetype(GoldchangeEnum.LOTTERY_BETTING.getValue());

                        }
                        orderBetRecordMapper.insertSelective(bet);
                    }

                    logger.info("????????????3:" + orderBetList.toString());

                    //?????? ??????????????? ???????????????????????????????????????????????????redis?????????????????????
                    saveKillDataToRedis(String.valueOf(order.getLotteryId()), order.getIssue(), orderBetList);
                    //?????????????????????????????????
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
//                     * ??????????????????
//                     */
//                    dto.setUserId(order.getUserId());
//                    // ????????????
//                    dto.setOpnote("??????/" + order.getOrderSn());
//                    dto.setSource(source);
//                    dto.setAccno(memBaseinfo.getAccno());
//                    dto.setRefid(order.getId().longValue());
//                    dto.setRefaccno(memBaseinfo.getAccno());
//                    // ???????????????????????????
//                    BigDecimal tradeOffAmount = getTradeOffAmount(amount.multiply(new BigDecimal(-1)));
//                    dto.setQuantity(tradeOffAmount);
//                    // ?????????????????????
//                    // BetCommonServiceImpl.updateMemberBetAmountAndNoWithdrawalAmount
//                    // ?????????????????????????????????????????????????????????????????? bug:2027
//                    // ?????????????????????????????????????????????
//                    dto.setNoWithdrawalAmount(tradeOffAmount);
//                    // ???????????????????????????
//                    //dto.setBetAmount(amount);
//                    logger.info("????????????4:" + dto.toString());
//                    dto.setWaitAmount(tradeOffAmount.negate());
//                    memBaseinfoWriteService.updateUserBalance(dto);
//                    logger.info("????????????5:" + dto.toString());

//                    // ??????WS????????????????????????????????????
//                    sendMsg(orderDTO, order.getId(), order.getOrderSn());
                } else {
                    logger.info("??????????????????????????????{}", orderDTO.getUserId());
                }
            } catch (InterruptedException e1) {
                logger.error("?????????????????????????????? error:{}", e1);
                throw new RuntimeException();
            } finally {
                lock.unlock();
                logger.info("???????????????????????????{}", orderDTO.getUserId());
            }
        } catch (
                Exception e) {
            // ?????????????????? ????????????
            if (orderDTO != null && orderDTO.getReOrderNum() <= 2) {
                orderEventSentService.sendOrderJson(JSON.toJSONString(orderDTO));
            }
            logger.error("????????????????????????????????????{}", JSON.toJSONString(orderDTO), e);
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
            // ????????????????????????????????????BACK ??????> ???????????????????????????????????????
            OrderBetRecordExample orderBetExample = new OrderBetRecordExample();
            OrderBetRecordExample.Criteria orderBetCriteria = orderBetExample.createCriteria();
            orderBetCriteria.andOrderIdEqualTo(order.getId());
            orderBetCriteria.andTbStatusNotEqualTo(OrderBetStatus.BACK);
            int count = orderBetRecordMapper.countByExample(orderBetExample);
            if (count < 1) {
                // ??????????????????
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
//            // ?????????????????????100s???????????????10S[????????????]????????????????????????????????????????????????????????????????????????????????????
//            boolean bool = lock.writeLock().tryLock(100, 10, TimeUnit.SECONDS);
//
//            // ????????????????????????
//            if (bool) {
//                Map<String, LotteryPlaySetting> lotterySettingMap = redisTemplate.opsForHash().entries(RedisKeys.LOTTERY_PLAY_SETTING_MAP_TYPE + Constants.LOTTERY_CATEGORY_TYPE_LOTTERY);
//                if (CollectionUtil.isEmpty(lotterySettingMap)) {
//                    lotterySettingMap = lotteryPlaySettingReadRest.queryLotteryPlaySettingMap(Constants.LOTTERY_CATEGORY_TYPE_LOTTERY);
//                }
//                if (null == lotterySettingMap) {
//                    return ResultInfo.error("???????????????????????????");
//                }
//
//                AnchorMemFamilymem family = null;
//                //?????????????????????
//                BasAnchorroom basAnchorroom = BasAnchorRoomRestRedis.selectByPrimaryKey(data.getRoomId(), basAnchorRoomRest);
//                if (null == basAnchorroom) {
//                    throw new BusinessException(StatusCode.LIVE_ERROR_1101.getCode(), "??????????????????");
//                }
//                //?????????????????????????????????
//                AnchorMemBaseinfo appMembers = AnchorMemBaseinfoRestRedis.getMemById(data.getFamilymemid(), anchorMemBaseinfoRest);
//                if (null != appMembers) {
//                    family = anchorMemFamilymemRest.getMemFamilymemByAncorAccno(appMembers.getAccno());
//                    if (null == family) {
//                        throw new BusinessException(StatusCode.LIVE_ERROR_1101.getCode(), "???????????????");
//                    }
//                } else {
//                    throw new BusinessException(StatusCode.LIVE_ERROR_1101.getCode(), "???????????????");
//                }
//
//                // ??????????????????
//                Integer userId = data.getUserId();
//                MemBaseinfo appMember = memBaseinfoService.selectByPrimaryKey((long) userId);
//                if (appMember == null) {
//                    return ResultInfo.error("?????????????????????????????????");
//                }
//                // TODO ???????????????????????????
//                if (appMember.getFreezeStatus().equals(1)) {
//                    return ResultInfo.error("?????????????????????????????????????????????");
//                }
//                if (appMember.getBetStatus().equals(0)) {
//                    return ResultInfo.error("?????????????????????????????????????????????");
//                }
//                logger.info("????????????:{}", appMember.toString());
//                BigDecimal amount = memBaseinfoService.getOrderBetRecordAmount(data.getOrders());
//
//                // ??????????????????
//                if (appMember.getGoldnum() == null || amount.compareTo(appMember.getGoldnum()) > 0) {
//                    logger.info("??????????????????");
//                    ResultInfo response = ResultInfo.ok();
//                    response.setStatus(StatusCode.LIVE_ERROR_120.getCode());
//                    response.setData(null);
//                    response.setInfo("????????????,?????????!");
//                    return response;
//                }
//                for (Integer id : data.getOrders()) {
//                    // ??????????????????
//                    OrderBetRecord orderBetRecord;
//                    if (redisTemplate.hasKey(ORDER_BET_KEY + id)) {
//                        orderBetRecord = (OrderBetRecord) redisTemplate.opsForValue().get(ORDER_BET_KEY + id);
//                    } else {
//                        orderBetRecord = orderBetRecordMapper.selectByPrimaryKey(id);
//                        redisTemplate.opsForValue().set(ORDER_BET_KEY + id, orderBetRecord);
//                    }
//
//                    // ??????
//                    if (orderBetRecord == null) {
//                        return ResultInfo.error("??????????????????????????????");
//                    }
//                    Integer orderId = orderBetRecord.getOrderId();
//                    // ??????????????????
//                    OrderRecord orderRecord;
//                    if (redisTemplate.hasKey(ORDER_KEY + orderId)) {
//                        orderRecord = (OrderRecord) redisTemplate.opsForValue().get(ORDER_KEY + orderId);
//                    } else {
//                        orderRecord = orderRecordMapper.selectByPrimaryKey(orderId);
//                        redisTemplate.opsForValue().set(ORDER_KEY + orderId, orderRecord);
//                    }
//                    if (orderRecord == null) {
//                        return ResultInfo.error("??????????????????????????????");
//                    }
//                    logger.info("????????????:{}", orderBetRecord.toString());
//
//
//                    if (null == lotterySettingMap.get(String.valueOf(orderBetRecord.getPlayId()))) {
//                        return ResultInfo.error("??????????????????????????????????????????");
//                    }
//
//                    // ?????????????????????
//                    if (!orderBetRecord.getTbStatus().equals(OrderBetStatus.WAIT)) {
//                        return ResultInfo.error("??????????????????????????????????????????");
//                    }
//
//                    // ????????????????????????
//                    BigDecimal betAmount = orderBetRecord.getBetAmount();
//                    // BigDecimal amount = betAmount.multiply(new BigDecimal(orderBetRecord.getBetCount()));
//
//                    BetRestrict restrict = this.getBonusMap(orderBetRecord.getLotteryId(), orderBetRecord.getPlayId());
//                    if (restrict != null && restrict.getMaxMoney().compareTo(BigDecimal.valueOf(0)) > 0) {
//                        if (betAmount.compareTo(restrict.getMaxMoney()) > 0) {
//                            return ResultInfo.error("?????????????????????????????????????????????????????????");
//                        }
//                    } else {
//                        // ????????????????????????
//                        BetRestrict betRestrict = this.getBonusMap(orderBetRecord.getLotteryId(), 0);
//                        if (null != betRestrict && betRestrict.getMaxMoney().compareTo(BigDecimal.valueOf(0)) > 0) {
//                            if (betAmount.compareTo(betRestrict.getMaxMoney()) > 0) {
//                                return ResultInfo.error("?????????????????????????????????????????????????????????");
//                            }
//                        }
//                    }
//
//                    // ???????????????????????????????????????
//                    ResultInfo<Boolean> resultInfo = this.checkIssueIsOpen(orderBetRecord.getLotteryId(),
//                            orderRecord.getIssue(), 1);
//                    if (resultInfo.isServerError()) {
//                        return ResultInfo.error("??????????????????");
//                    }
//                    String orderSn = SnowflakeIdWorker.createOrderSn();
//                    // ????????????
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
//                    order.setBuySource(KEY_FIVES);//?????????????????????
//                    // ?????????????????????
//                    orderRecordMapper.insertSelective(order);
//                    logger.info("????????????:{}", order.toString());
//                    // ??????????????????
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
//                    // ?????????????????????
//                    orderBetRecordMapper.insertSelective(orderBet);
//                    logger.info("???????????????:{}", orderBet.toString());
//
//                    /**
//                     * ??????????????????
//                     */
//                    MemGoldchangeDO dto = new MemGoldchangeDO();
//
//                    //?????????????????????????????????
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
//                    // ????????????id
//                    dto.setUserId(order.getUserId());
//                    // ????????????
//                    dto.setOpnote("??????????????????/" + order.getOrderSn());
//                    dto.setSource(order.getSource());
//                    dto.setAccno(appMembers.getAccno());
//                    dto.setRefid(order.getId().longValue());
//                    dto.setRefaccno(appMembers.getAccno());
//                    // ????????????
//                    dto.setChangetype(GoldchangeEnum.LIVEROOM_BET.getValue());
//                    // ???????????????????????????
//                    BigDecimal tradeOffAmount = getTradeOffAmount(amount.multiply(new BigDecimal(-1)));
//                    dto.setQuantity(tradeOffAmount);
//                    // ?????????????????????????????????????????????
//                    dto.setNoWithdrawalAmount(tradeOffAmount);
//                    // ???????????????????????????
//                    //dto.setBetAmount(betAmount);
//                    // ????????????????????????
//                    dto.setWaitAmount(tradeOffAmount.negate());
//                    memBaseinfoWriteService.updateUserBalance(dto);
//                    logger.info("????????????:{}", dto.toString());
//                }
//            }
//        } catch (Exception e) {
//            logger.error("liveRoomCopy occur error.", e);
//            throw new RuntimeException(e.getMessage());
//        } finally {
//            // ?????????
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
//            // ?????????????????????100s???????????????10S[????????????]????????????????????????????????????????????????????????????????????????????????????
//            boolean bool = lock.writeLock().tryLock(100, 10, TimeUnit.SECONDS);
//            // ????????????????????????
//            if (bool) {
//                if (orderFollow.getOrderAmount().compareTo(BigDecimal.valueOf(0)) <= 0) {
//                    return ResultInfo.error("?????????????????????0?????????0???");
//                }
//
//                // ???????????????id
//                CircleGodPushOrder godPushOrder = pushOrderMapper.selectByPrimaryKey(orderFollow.getGodPushId());
//                Integer orderBetId = godPushOrder.getOrderBetId();
//                // ??????????????????
//                OrderBetRecord orderBetRecord;
//                if (redisTemplate.hasKey(ORDER_BET_KEY + orderBetId)) {
//                    orderBetRecord = (OrderBetRecord) redisTemplate.opsForValue().get(ORDER_BET_KEY + orderBetId);
//                } else {
//                    orderBetRecord = orderBetRecordMapper.selectByPrimaryKey(orderBetId);
//                }
//
//                // ??????
//                if (orderBetRecord == null) {
//                    return ResultInfo.error("??????????????????????????????");
//                }
//
//                BigDecimal amount = orderFollow.getOrderAmount().divide(new BigDecimal(orderBetRecord.getBetCount()), 2,
//                        BigDecimal.ROUND_DOWN);
//                if (new BigDecimal(amount.intValue()).compareTo(amount) == -1) {
//                    return ResultInfo.error("????????????????????????");
//                }
//
//                // ?????????????????????????????????????????????
//                if (orderFollow.getUserId().equals(orderBetRecord.getUserId())) {
//                    return ResultInfo.error("??????????????????????????????");
//                }
//                // ?????????????????????
//                if (!orderBetRecord.getTbStatus().equals(OrderBetStatus.WAIT)) {
//                    return ResultInfo.error("??????????????????????????????????????????");
//                }
//
//                // ??????????????????
//                OrderRecord orderRecord;
//                if (redisTemplate.hasKey(ORDER_KEY + orderBetId)) {
//                    orderRecord = (OrderRecord) redisTemplate.opsForValue().get(ORDER_KEY + orderBetId);
//                } else {
//                    orderRecord = orderRecordMapper.selectByPrimaryKey(orderBetRecord.getOrderId());
//                    redisTemplate.opsForValue().set(ORDER_KEY + orderBetId, orderRecord);
//                }
//
//                // ????????????????????????
//
//                // ??????????????????
//                Integer userId = orderFollow.getUserId();
//                if (userId == null || userId < 1) {
//                    logger.error("userId[{}]????????????", userId);
//                    return ResultInfo.error("?????????????????????");
//                }
////                AppMember appMember = appMemberMapper.selectByPrimaryKey(userId);
//                MemBaseinfo appMember = memBaseinfoService.selectByPrimaryKey((long) userId);
//                if (appMember == null) {
//                    return ResultInfo.error("?????????????????????????????????");
//                }
//                // TODO ???????????????????????????
//                if (appMember.getFreezeStatus().equals(1)) {
//                    return ResultInfo.error("?????????????????????????????????????????????");
//                }
//                if (appMember.getBetStatus().equals(0)) {
//                    return ResultInfo.error("?????????????????????????????????????????????");
//                }
//
//                // ????????????????????????
//                BigDecimal betAmount = orderFollow.getOrderAmount();
//
//                BetRestrict restrict = this.getBonusMap(orderBetRecord.getLotteryId(), orderBetRecord.getPlayId());
//                if (restrict != null && restrict.getMaxMoney().compareTo(BigDecimal.valueOf(0)) > 0) {
//                    if (betAmount.compareTo(restrict.getMaxMoney()) > 0) {
//                        return ResultInfo.error("?????????????????????????????????????????????????????????");
//                    }
//                } else {
//                    // ????????????????????????
//                    BetRestrict betRestrict = this.getBonusMap(orderBetRecord.getLotteryId(), 0);
//                    if (null != betRestrict && betRestrict.getMaxMoney().compareTo(BigDecimal.valueOf(0)) > 0) {
//                        if (betAmount.compareTo(betRestrict.getMaxMoney()) > 0) {
//                            return ResultInfo.error("?????????????????????????????????????????????????????????");
//                        }
//                    }
//
//                }
//
//                // ??????????????????
//                BigDecimal balance = appMember.getGoldnum();
//                if (betAmount.compareTo(balance) > 0) {
//                    return ResultInfo.error("???????????????");
//                }
//
//                // ???????????????????????????????????????
//                ResultInfo<Boolean> resultInfo = this.checkIssueIsOpen(orderBetRecord.getLotteryId(),
//                        orderRecord.getIssue(), 1);
//                if (resultInfo.isServerError()) {
//                    return ResultInfo.error("??????????????????");
//                }
//
//                String orderSn = SnowflakeIdWorker.createOrderSn();
//                // ????????????
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
//                // ?????????????????????
//                orderRecordMapper.insertSelective(order);
//
//                // ??????????????????
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
//                // ?????????????????????
//                orderBetRecordMapper.insertSelective(orderBet);
//
//                /**
//                 * ??????????????????
//                 */
//                MemGoldchangeDO dto = new MemGoldchangeDO();
//                // ????????????id
//                dto.setUserId(order.getUserId());
//                // ????????????
//                dto.setOpnote("??????/" + order.getOrderSn());
//                // ????????????
//                dto.setType(BalanceChangeEnum.BET_ORDER.getValue());
//                // ???????????????????????????
//                dto.setQuantity(betAmount.multiply(new BigDecimal(-1)).setScale(2, BigDecimal.ROUND_HALF_UP));
//                // ?????????????????????????????????????????????
//                dto.setNoWithdrawalAmount(betAmount.multiply(new BigDecimal(-1).setScale(2, BigDecimal.ROUND_HALF_UP)));
//                // ???????????????????????????
//                dto.setBetAmount(betAmount);
//                // ????????????????????????
//                dto.setWaitAmount(betAmount);
//                memBaseinfoWriteService.updateUserBalance(dto);
//
//                // ???????????????
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
//                // ??????????????????
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
//            // ?????????
//            lock.writeLock().unlock();
//        }
//        return ResultInfo.ok(orderPushVo);
//
//    }

    /**
     * ??????
     *
     * @param orderAppend ????????????
     * @param source      ??????
     * @return
     */
    @Override
    @Transactional(rollbackFor = {Exception.class})
    public ResultInfo<Boolean> orderAppend(OrderAppendRecord orderAppend, String source) {
        // ??????????????????
        String firstIssue = orderAppend.getFirstIssue();
        Integer lotteryId = orderAppend.getLotteryId();
        Integer playId = orderAppend.getPlayId();
        Integer userId = orderAppend.getUserId();
        Double betMultiples = orderAppend.getBetMultiples();
        Double doubleMultiples = orderAppend.getType().equals(1) ? 1 : orderAppend.getDoubleMultiples();

        // ????????????????????????
        String issue = orderAppendWriteService.createNextIssue(lotteryId, firstIssue, orderAppend.getAppendedCount());

        /** ?????????????????? */
        OrderRecord order = new OrderRecord();
        order.setUserId(userId);
        order.setIssue(issue);
        // ???????????????
        order.setOrderSn(SnowflakeIdWorker.createOrderSn());
        order.setSource(source);
        order.setAppendId(orderAppend.getId());
        order.setLotteryId(lotteryId);
        orderRecordMapper.insertSelective(order);

        /** ?????????????????? */
        OrderBetRecord orderBet = new OrderBetRecord();
        orderBet.setOrderId(order.getId());
        orderBet.setUserId(userId);

        // ??????????????????
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
     * ??????????????????Map
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
        logger.info("??????{},?????????????????????{}", userId, orderBetId);
        // ?????????
        RedisLock lock = new RedisLock("BACK_ORDER_" + userId + "_" + orderBetId, 100, 10000);
        try {
            if (lock.lock()) {
                // ??????????????????
                OrderBetRecord orderBet = orderBetRecordMapper.selectByPrimaryKey(orderBetId);
                if (orderBet == null || orderBet.getIsDelete()) {
                    return ResultInfo.error("?????????????????????????????????");
                }

                if (!orderBet.getUserId().equals(userId)) {
                    return ResultInfo.error("??????????????????????????????????????????");
                }

                if (orderBet.getTbStatus().equals(OrderBetStatus.BACK)) {
                    return ResultInfo.error("??????????????????");
                }

                // ????????????id
                Integer orderId = orderBet.getOrderId();
                // ??????????????????
                OrderRecord order = orderRecordMapper.selectByPrimaryKey(orderId);
                // ??????
                if (order == null || order.getIsDelete()) {
                    return ResultInfo.error("?????????????????????????????????");
                }

                // ????????????id
                Integer lotteryId = order.getLotteryId();
                // ????????????
                String issue = order.getIssue();

                // ???????????????????????????
                ResultInfo<Boolean> resultInfo = this.checkIssueIsOpen(lotteryId, issue, 2);
                if (resultInfo.getCode() != StatusCode.SUCCESSCODE.getCode()) {
                    return resultInfo;
                }

                // ????????????
                String keySuffix = userId + "_backAnOrder";
                String intercept = (String) redisTemplate.opsForValue().get(keySuffix);
                if (org.apache.commons.lang3.StringUtils.isNotBlank(intercept)) {
                    return ResultInfo.error("????????????????????????");
                }
                if (org.apache.commons.lang3.StringUtils.isBlank(intercept)) {
                    redisTemplate.opsForValue().set(keySuffix, "1", 3, TimeUnit.SECONDS);
                }

                // status = transactionManager.getTransaction(new
                // DefaultTransactionDefinition());

                // ????????????????????????
                // RevokeOrderUtil.revokeOrder(txManager, orderBetRecordMapper,
                // orderRecordMapper, systemInfoMapper, appMemberWriteService, orderBet, order);
                // ??????
                orderBet.setTbStatus(OrderBetStatus.BACK);
                orderBet.setUpdateTime(null);
                orderBetRecordMapper.updateByPrimaryKeySelective(orderBet);

                // ????????????????????????????????????BACK ??????> ???????????????????????????????????????
                OrderBetRecordExample orderBetExample = new OrderBetRecordExample();
                OrderBetRecordExample.Criteria orderBetCriteria = orderBetExample.createCriteria();
                orderBetCriteria.andOrderIdEqualTo(orderId);
                orderBetCriteria.andTbStatusNotEqualTo(OrderBetStatus.BACK);
                int count = orderBetRecordMapper.countByExample(orderBetExample);
                if (count < 1) {
                    // ??????????????????
                    order.setUpdateTime(null);
                    order.setStatus(OrderStatus.BACK);
                    orderRecordMapper.updateByPrimaryKeySelective(order);
                }

                // ??????????????????
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

                        // ????????????
                        orderAppendRecord.setIsStop(true);
                        orderAppendRecordMapper.updateByPrimaryKeySelective(orderAppendRecord);

                         //ONELIVE TODO ????????????
//                        // ??????????????????
//                        MemGoldchangeDO dto = new MemGoldchangeDO();
//                        // ????????????id
//                        dto.setUserId(order.getUserId());
//                        // ????????????
//                        dto.setOpnote("????????????/" + order.getOrderSn() + "/" + orderAppendRecord.getId());
//                        // ????????????
//                        dto.setChangetype(GoldchangeEnum.ORDER_APPEND_BACK.getValue());
//                        // ???????????????????????????
//                        dto.setQuantity(getTradeOffAmount(numberBigDecimal));
//                        dto.setWaitAmount(getTradeOffAmount(new BigDecimal("-1").multiply(orderBet.getBetAmount())));
//                        memBaseinfoWriteService.updateUserBalance(dto);
                    }

                } else {
                    //ONELIVE TODO ????????????
//                    BigDecimal amount = getTradeOffAmount(orderBet.getBetAmount());
//                    // ??????????????????
//                    MemGoldchangeDO dto = new MemGoldchangeDO();
//                    dto.setRefid((long) orderBetId);
//                    dto.setUserId(order.getUserId());
//                    // ????????????
//                    dto.setOpnote("??????/" + order.getOrderSn() + "/" + orderBet.getId());
//                    // ????????????
//                    dto.setChangetype(GoldchangeEnum.BET_ORDER_BAK.getValue());
//                    // ???????????????????????????
//                    dto.setQuantity(amount);
//                    // ?????????????????????????????????????????????
//                    // dto.setNoWithdrawalAmount(amount.setScale(2, BigDecimal.ROUND_HALF_UP));
//                    // ???????????????????????????
//                    // dto.setBetAmount(amount.multiply(new BigDecimal(-1)).setScale(2,
//                    // BigDecimal.ROUND_HALF_UP));
//                    dto.setWaitAmount(amount.negate());
//                    // ????????????????????????
//                    memBaseinfoWriteService.updateUserBalance(dto);
                    logger.info("??????{},?????????????????????{},????????????!", userId, orderBetId);
                }

                return ResultInfo.ok(true);
            } else {
                logger.info("??????{},?????????????????????{},????????????!", userId, orderBetId);
                return ResultInfo.error();
            }
        } catch (Exception e) {
            logger.error("backOrder occur error. userId:{}, orderBetId:{}.", userId, orderBetId, e);
            return ResultInfo.error("???????????????");
        } finally {
            logger.info("??????{},?????????????????????{},?????????!", userId, orderBetId);
            lock.unlock();
        }

    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public ResultInfo<String> backOrders(List<Integer> orderBetIds, Integer userId) {
        // ?????????
//		RedisLock lock = new RedisLock("BACK_ORDERS_" + userId, 0, 2000);
//		try {
//			if (lock.lock()) {
        // ?????????????????????
        OrderBetRecordExample example = new OrderBetRecordExample();
        OrderBetRecordExample.Criteria criteria = example.createCriteria();
        criteria.andIdIn(orderBetIds);
        List<OrderBetRecord> orderBetList = orderBetRecordMapper.selectByExample(example);

        if (CollectionUtils.isEmpty(orderBetList)) {
            return ResultInfo.error("??????????????????????????????");
        }

        // ??????????????????
        OrderRecordExample orderExample = new OrderRecordExample();
        OrderRecordExample.Criteria orderCriteria = orderExample.createCriteria();
        orderCriteria.andUserIdEqualTo(userId);
        orderCriteria.andIsDeleteEqualTo(false);
        orderCriteria.andStatusEqualTo(OrderStatus.NORMAL);
        orderCriteria.andOpenNumberIsNull();
        List<OrderRecord> orderRecords = orderRecordMapper.selectByExample(orderExample);
        if (CollectionUtils.isEmpty(orderRecords)) {
            return ResultInfo.error("????????????????????????????????????????????????");
        }

        Map<Integer, OrderRecord> orderMap = new HashMap<>();
        for (OrderRecord order : orderRecords) {
            orderMap.put(order.getId(), order);
        }

        List<String> orderSnList = new ArrayList<>();
        // ????????????
        String keySuffix = userId + "_backOrders";
        String intercept = (String) redisTemplate.opsForValue().get(keySuffix);
        if (org.apache.commons.lang3.StringUtils.isNotBlank(intercept)) {
            return ResultInfo.error("????????????????????????");
        }
        if (org.apache.commons.lang3.StringUtils.isBlank(intercept)) {
            redisTemplate.opsForValue().set(keySuffix, "1", 3, TimeUnit.SECONDS);
        }

        for (OrderBetRecord orderBet : orderBetList) {
            RedisLock lock = new RedisLock("BACK_ORDER_" + userId + "_" + orderBet.getId(), 100, 10000);
            try {
                if (lock.lock()) {
                    // ????????????
                    if (!orderBet.getUserId().equals(userId)) {
                        logger.error("??????????????????????????????????????????");
                        continue;
                    }
                    // ????????????id
                    Integer orderId = orderBet.getOrderId();
                    // ??????????????????
                    OrderRecord order = orderMap.get(orderId);
                    if (order == null || order.getIsDelete()) {
                        logger.info("??????????????????,orderId:{}", orderId);
                        continue;
                    }

                    if (orderBet.getTbStatus().equals(OrderBetStatus.BACK)) {
                        logger.error("??????????????????orderId:{}", orderId);
                        continue;
                    }

                    String orderSn = order.getOrderSn();
                    // ????????????id
                    Integer lotteryId = order.getLotteryId();
                    // ????????????
                    String issue = order.getIssue();

                    // ???????????????????????????
                    ResultInfo<Boolean> resultInfo = this.checkIssueIsOpen(lotteryId, issue, 2);
                    if (resultInfo.getCode() != StatusCode.SUCCESSCODE.getCode()) {
                        if (!orderSnList.contains(orderSn)) {
                            orderSnList.add(orderSn);
                        }
                        continue;
                    }

                    // ??????
                    orderBet.setTbStatus(OrderBetStatus.BACK);
                    orderBet.setUpdateTime(null);
                    orderBetRecordMapper.updateByPrimaryKeySelective(orderBet);

                    // ????????????????????????????????????BACK ??????> ???????????????????????????????????????
                    OrderBetRecordExample orderBetExample = new OrderBetRecordExample();
                    OrderBetRecordExample.Criteria orderBetCriteria = orderBetExample.createCriteria();
                    orderBetCriteria.andOrderIdEqualTo(orderId);
                    orderBetCriteria.andTbStatusNotEqualTo(OrderBetStatus.BACK);
                    int count = orderBetRecordMapper.countByExample(orderBetExample);
                    // ??????????????????
                    if (count < 1) {
                        order.setUpdateTime(null);
                        order.setStatus(OrderStatus.BACK);
                        orderRecordMapper.updateByPrimaryKeySelective(order);
                    }

                    BigDecimal amount = getTradeOffAmount(orderBet.getBetAmount());

                    //ONELIVE TODO ????????????
//                    // ??????????????????
//                    MemGoldchangeDO dto = new MemGoldchangeDO();
//                    dto.setRefid(orderBet.getId().longValue());
//                    dto.setUserId(order.getUserId());
//                    // ????????????
//                    dto.setOpnote("??????/" + order.getOrderSn() + "/" + orderBet.getId());
//                    // ????????????
//                    dto.setChangetype(GoldchangeEnum.BET_ORDER_BAK.getValue());
//                    // ???????????????????????????
//                    dto.setQuantity(amount);
//                    // ?????????????????????????????????????????????
//                    // dto.setNoWithdrawalAmount(amount.setScale(2, BigDecimal.ROUND_HALF_UP));
//                    // ???????????????????????????
//                    // dto.setBetAmount(amount.multiply(new BigDecimal(-1)).setScale(2,
//                    // BigDecimal.ROUND_HALF_UP));
//                    dto.setWaitAmount(amount.negate());
//                    // ????????????????????????
//                    memBaseinfoWriteService.updateUserBalance(dto);
                }
            } catch (Exception e) {
                logger.error("backOrders occur error. userId:{}, orderBetIds:{}", userId,
                        JSONObject.toJSONString(orderBetIds), e);
                return ResultInfo.error("???????????????");
            } finally {
                lock.unlock();
            }
        }

        if (CollectionUtils.isEmpty(orderSnList)) {
            return ResultInfo.ok("???????????????");
        }

        StringBuilder str = new StringBuilder();
        str.append("????????????");
        for (String orderSn : orderSnList) {
            str.append(orderSn).append("???");
        }
        str.append("???????????????????????????");
        return ResultInfo.ok(str.toString());
//			} else {
//				return ResultInfo.operateRepeatError();
//			}
//		} catch (Exception e) {
//			logger.error("backOrders occur error. userId:{}, orderBetIds:{}", userId,
//					JSONObject.toJSONString(orderBetIds), e);
//			return ResultInfo.error("???????????????");
//		} finally {
//			lock.unlockWhenExpired();
//		}
    }

    /**
     * ??????????????????????????????????????????
     *
     * @param lotteryId ??????id
     * @param issue     ??????
     * @param type      ?????? 1????????? 2?????????
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
                    return ResultInfo.error("???????????????????????????????????????");
                }
                Date startDate = DateUtils.parseDate(lhcHandicap.getStartTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS);
                Date endDate = DateUtils.parseDate(lhcHandicap.getEndTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS);
                assert startDate != null;
                if (System.currentTimeMillis() < startDate.getTime()) {
                    return ResultInfo.error("???????????????????????????????????????");
                }
                assert endDate != null;
                if (System.currentTimeMillis() > endDate.getTime()) {
                    return ResultInfo.error("??????????????????????????????????????????");
                }
                break;

            default:
                String tableName = LotteryTableNameEnum.getTableNameByLotteryId(lotteryId);
                if (org.apache.commons.lang.StringUtils.isBlank(tableName)) {
                    return ResultInfo.error("?????????????????????");
                }

                // ??????????????????????????????
                int fengpan = 1;
                try {
                    if (redisTemplate.hasKey(RedisKeys.LOTTERY_MAP_KEY)) {
                        Map<Integer, Lottery> lotteryMap = (Map<Integer, Lottery>) redisTemplate.opsForValue()
                                .get(RedisKeys.LOTTERY_MAP_KEY);
                        Lottery thisLottery = lotteryMap.get(lotteryId.toString());
                        fengpan = thisLottery.getEndTime();
                    }
                } catch (Exception e) {
                    logger.error("????????????????????????lotteryId:{},e:{}", lotteryId, e);
                }
                Date addFengpanTime = DateUtils.add(new Date(), Calendar.SECOND, fengpan);
                List<String> issueNext = bjpksBeanMapper.selectByTableName(tableName,
                        DateUtils.formatDate(addFengpanTime, DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));

                // ????????????????????????????????????
                // XyftLotterySg xyftSg = xyftLotterySgWriteService.queryNextSg();
                if (issueNext != null && issueNext.size() > 0) {
                    String issueStr = issueNext.get(0);
                    if (issue.compareTo(issueStr) != 0) {
                        return type.equals(1) ? ResultInfo.error("????????????????????????????????????????????????????????????")
                                : ResultInfo.error("???????????????????????????????????????????????????");
                    }
                } else {
                    return ResultInfo.error("??????????????????????????????");
                }
        }
        return ResultInfo.ok(true);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public ResultInfo<Integer> backOrderByAdmin(Integer lotteryId, String issue, String openNumber) {

        // ??????????????????
//        ResultInfo<Integer> result = lotterySgWriteService.changeNumber(lotteryId, issue, openNumber);
//        if (result.isServerError()) {
//            return result;
//        }
        boolean hasIssue = getIssue(issue, lotteryId);
        if (hasIssue == false) {
            return ResultInfo.ok(0);
        }

        // ???????????????
//        Thread thread = new Thread(() -> {
        // ??????????????????????????????
        OrderRecordExample orderExample = new OrderRecordExample();
        OrderRecordExample.Criteria orderCriteria = orderExample.createCriteria();
        orderCriteria.andLotteryIdEqualTo(lotteryId);
        orderCriteria.andIssueEqualTo(issue);
//            if (!CollectionUtils.isEmpty(orderSns)) {
//                orderCriteria.andOrderSnIn(orderSns);
//            }
        orderCriteria.andStatusEqualTo(OrderStatus.NORMAL);
        List<OrderRecord> orderList = orderRecordMapper.selectByExample(orderExample);
        logger.info("??????????????????????????????{}", orderList.size());
        List<Integer> orderIdList = new ArrayList<>();
        Map<Integer, OrderRecord> mapOrderRecord = new HashMap<>();
        for (OrderRecord orderRecord : orderList) {
            orderIdList.add(orderRecord.getId());
            mapOrderRecord.put(orderRecord.getId(), orderRecord);

            // ??????????????????????????????
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
        // ??????
        if (CollectionUtils.isEmpty(orderList)) {
            return ResultInfo.ok(1);
//                return;
        }

        // ???????????????id??????
//            List<Integer> orderIds = new ArrayList<>();
        for (OrderBetRecord orderBet : orderBetRecordList) {
//                orderIds.add(order.getId());

            String tbStatus = orderBet.getTbStatus();
            orderBet.setTbStatus(OrderStatus.BACK);

//                // ????????????????????????
//                MemberBalanceChangeExample changeExample = new MemberBalanceChangeExample();
//                MemberBalanceChangeExample.Criteria changeCriteria = changeExample.createCriteria();
//                changeCriteria.andRemarkEqualTo("%" + order.getOrderSn());
//                List<MemberBalanceChange> changeList = memberBalanceChangeMapper.selectByExample(changeExample);
//                BigDecimal balance = new BigDecimal(0);
//                BigDecimal betMoney = new BigDecimal(0);
//                for (MemberBalanceChange change : changeList) {
//                    String[] str = change.getRemark().split("/");
//                    if ("??????".equals(str[0])) {
//                        betMoney = change.getChangeMoney();
//                        balance = balance.subtract(betMoney);
//                        continue;
//                    }
//                    balance = balance.subtract(change.getChangeMoney());
//                    memberBalanceChangeMapper.deleteByPrimaryKey(change.getId());
//                }

            //ONELIVE TODO ????????????
            // ???????????? ????????????
            //????????????
            BigDecimal defaultAmount = getTradeOffAmount(null);
            BigDecimal betMoney = defaultAmount;
            // ??????????????????
          //  MemGoldchangeDO dto = new MemGoldchangeDO();
            // ????????????id
          //  dto.setUserId(orderBet.getUserId());
            // ????????????
         //   dto.setOpnote("????????????/" + mapOrderRecord.get(orderBet.getOrderId()).getOrderSn() + "/" + orderBet.getId());
            // ????????????
          //  dto.setChangetype(GoldchangeEnum.REVOKE_AWARD.getValue());
            // ???????????????????????????
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
            // ?????????????????????????????????????????????
           // dto.setNoWithdrawalAmount(tradeOffAmount);
            // ???????????????????????????
          //  dto.setBetAmount(tradeOffAmount.negate());
            // ?????????????????????
           // dto.setShowChange(betMoney);
          //  dto.setWaitAmount(defaultAmount);  //?????????????????? ??????
            // ????????????????????????
           // memBaseinfoWriteService.updateUserBalance(dto);

            // ???????????? ??????
            betMoney = defaultAmount;
            // ??????????????????
          //  dto = new MemGoldchangeDO();
            // ????????????id
          //  dto.setUserId(orderBet.getUserId());
            // ????????????
          //  dto.setOpnote("??????/" + mapOrderRecord.get(orderBet.getOrderId()).getOrderSn() + "/" + orderBet.getId());
            // ????????????
         //   dto.setChangetype(GoldchangeEnum.BET_ORDER_BAK.getValue());
            // ???????????????????????????
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
            // ?????????????????????????????????????????????
            // dto.setNoWithdrawalAmount(balance.setScale(2, BigDecimal.ROUND_HALF_UP));
            // ???????????????????????????
            // dto.setBetAmount(balance.multiply(new BigDecimal(-1)).setScale(2,
            // BigDecimal.ROUND_HALF_UP));
            // ?????????????????????
         //   dto.setShowChange(betMoney);
           // dto.setWaitAmount(defaultAmount);
            // ????????????????????????
       //     memBaseinfoWriteService.updateUserBalance(dto);

        }

//            // ?????????????????????
//            OrderBetRecordExample orderBetExample = new OrderBetRecordExample();
//            OrderBetRecordExample.Criteria orderBetCriteria = orderBetExample.createCriteria();
//            orderBetCriteria.andOrderIdIn(orderIds);
//            orderBetCriteria.andTbStatusNotEqualTo(OrderBetStatus.BACK);
//            List<OrderBetRecord> orderBetList = orderBetRecordMapper.selectByExample(orderBetExample);
//            // ????????????
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
            // ???????????????
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
//            // ?????????????????????
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

        // ??????????????????
        ResultInfo<Integer> result = lotterySgWriteRest.changeNumber(lotteryId, issue, openNumber);
        if (result.getCode() != StatusCode.SUCCESSCODE.getCode()) {
            return result;
        }
//        boolean hasIssue = getIssue(issue,lotteryId);
//        if(hasIssue == false){
//            return ResultInfo.ok(0);
//        }

        // ????????????
        /**
         * ?????????????????? ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????
         */
//        Thread t = new Thread(() -> {
        // ??????????????????????????????
        OrderRecordExample orderExample = new OrderRecordExample();
        OrderRecordExample.Criteria orderCriteria = orderExample.createCriteria();
        orderCriteria.andLotteryIdEqualTo(lotteryId);
        orderCriteria.andIssueEqualTo(issue);
//            if (!CollectionUtils.isEmpty(orderSns)) {
//                orderCriteria.andOrderSnIn(orderSns);
//            }
        orderCriteria.andStatusEqualTo(OrderStatus.NORMAL);
        List<OrderRecord> orderList = orderRecordMapper.selectByExample(orderExample);
        logger.info("??????????????????????????????{}", orderList.size());
        List<Integer> orderIdList = new ArrayList<>();
        Map<Integer, OrderRecord> mapOrderRecord = new HashMap<>();
        for (OrderRecord orderRecord : orderList) {
            orderIdList.add(orderRecord.getId());
            mapOrderRecord.put(orderRecord.getId(), orderRecord);

            // ?????????????????????????????????
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
        // ??????
        if (CollectionUtils.isEmpty(orderList)) {
//                return;
            return result;
        }
        // ???????????????id??????
        List<Integer> orderIds = new ArrayList<>();
        for (OrderBetRecord orderBet : orderBetRecordList) {
            orderIds.add(orderBet.getId());

            String tbStatus = orderBet.getTbStatus();
            orderBet.setTbStatus(Constants.WAIT);
//                // ????????????????????????
//                MemberBalanceChangeExample changeExample = new MemberBalanceChangeExample();
//                MemberBalanceChangeExample.Criteria changeCriteria = changeExample.createCriteria();
//                changeCriteria.andRemarkLike("%" + order.getOrderSn());
//                List<MemberBalanceChange> changeList = memberBalanceChangeMapper.selectByExample(changeExample);
//                BigDecimal balance = new BigDecimal(0);
//                for (MemberBalanceChange change : changeList) {
//                    String[] str = change.getRemark().split("/");
//                    if ("??????".equals(str[0])) {
//                        continue;
//                    }
//                    balance = balance.subtract(change.getChangeMoney());
//                    memberBalanceChangeMapper.deleteByPrimaryKey(change.getId());
//                }

            //ONELIVE TODO ????????????
            // ???????????? ????????????
            //????????????
            BigDecimal defaultAmount = getTradeOffAmount(null);
            BigDecimal betMoney = defaultAmount;
            // ??????????????????
            //MemGoldchangeDO dto = new MemGoldchangeDO();
            // ????????????id
            //dto.setUserId(orderBet.getUserId());
            // ????????????
            //dto.setOpnote("????????????/" + mapOrderRecord.get(orderBet.getOrderId()).getOrderSn() + "/" + orderBet.getId());
            // ????????????
            //dto.setChangetype(GoldchangeEnum.REVOKE_AWARD.getValue());
            // ???????????????????????????
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

//            // ?????????????????????????????????????????????
//            BigDecimal betAmount = getTradeOffAmount(orderBet.getBetAmount());
//            dto.setNoWithdrawalAmount(betAmount);
//            // ???????????????????????????
//            dto.setBetAmount(betAmount.negate());
//            // ?????????????????????
//            dto.setShowChange(betMoney);
//            dto.setWaitAmount(betAmount);
//            // ????????????????????????
//            memBaseinfoWriteService.updateUserBalance(dto);
        }

//            // ?????????????????????
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
        // ????????????????????????
        if (CollectionUtils.isEmpty(orderRecords)) {
            return;
        }
        // ??????????????????????????????
        if (StringUtils.isBlank(number)) {
            return;
        }
        // ????????????????????????
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
//                // ?????????????????????
//                CqsscLotterySg cqsscSg = cqsscLotterySgWriteService.selectByIssue(issue);
//                if (cqsscSg == null) {
//                    return ResultInfo.error("???????????????");
//                }
//                if (cqsscSg.getWan() != null) {
//                    return ResultInfo.error("??????????????????");
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
//                // ??????
////                rabbitTemplate.convertAndSend(RabbitConfig.TOPIC_EXCHANGE, RabbitConfig.BINDING_SSC_CQ, "CQSSC:" + issue + ":" + openNumber);
//                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_SSC_CQ_LM, "CQSSC:" + issue + ":" + openNumber);
//                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_SSC_CQ_DN, "CQSSC:" + issue + ":" + openNumber);
//                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_SSC_CQ_15, "CQSSC:" + issue + ":" + openNumber);
//                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_SSC_CQ_QZH, "CQSSC:" + issue + ":" + openNumber);
//                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_SSC_CQ_UPDATE_DATA,
//                        "CQSSC:" + issue + ":" + openNumber);
//                break;
//            case 2:
//                // ?????????????????????
//                XjsscLotterySg xjsscSg = xjsscLotterySgWriteService.selectByIssue(issue);
//                if (xjsscSg == null) {
//                    return ResultInfo.error("???????????????");
//                }
//                if (xjsscSg.getWan() != null) {
//                    return ResultInfo.error("??????????????????");
//                }
//                xjsscSg.setWan(Integer.valueOf(nums[0]));
//                xjsscSg.setQian(Integer.valueOf(nums[1]));
//                xjsscSg.setBai(Integer.valueOf(nums[2]));
//                xjsscSg.setShi(Integer.valueOf(nums[3]));
//                xjsscSg.setGe(Integer.valueOf(nums[4]));
//                xjsscSg.setTime(DateUtils.formatDate(new Date(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
//                xjsscSg.setOpenStatus("HANDLE");
//                count = xjsscLotterySgMapper.updateByPrimaryKeySelective(xjsscSg);
//                // ??????
////                rabbitTemplate.convertAndSend(RabbitConfig.TOPIC_EXCHANGE, RabbitConfig.BINDING_SSC_XJ, "XJSSC:" + issue + ":" + openNumber);
//                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_SSC_XJ_LM, "XJSSC:" + issue + ":" + openNumber);
//                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_SSC_XJ_DN, "XJSSC:" + issue + ":" + openNumber);
//                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_SSC_XJ_15, "XJSSC:" + issue + ":" + openNumber);
//                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_SSC_XJ_QZH, "XJSSC:" + issue + ":" + openNumber);
//                jmsMessagingTemplate.convertAndSend(ActiveMQConfig.TOPIC_SSC_XJ_UPDATE_DATA,
//                        "XJSSC:" + issue + ":" + openNumber);
//                break;
//            case 3:
//                // ?????????????????????
//                TxffcLotterySg txffcSg = txffcLotterySgWriteService.selectByIssue(issue);
//                if (txffcSg == null) {
//                    return ResultInfo.error("???????????????");
//                }
//                if (txffcSg.getWan() != null) {
//                    return ResultInfo.error("??????????????????");
//                }
//                txffcSg.setWan(Integer.valueOf(nums[0]));
//                txffcSg.setQian(Integer.valueOf(nums[1]));
//                txffcSg.setBai(Integer.valueOf(nums[2]));
//                txffcSg.setShi(Integer.valueOf(nums[3]));
//                txffcSg.setGe(Integer.valueOf(nums[4]));
//                txffcSg.setTime(DateUtils.formatDate(new Date(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
//                txffcSg.setOpenStatus("HANDLE");
//                count = txffcLotterySgMapper.updateByPrimaryKeySelective(txffcSg);
//                // ??????
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
//                    return ResultInfo.error("???????????????");
//                }
//                if (StringUtils.isNotBlank(pcddSg.getNumber())) {
//                    return ResultInfo.error("??????????????????");
//                }
//                pcddSg.setNumber(openNumber);
//                pcddSg.setTime(DateUtils.formatDate(new Date(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
//                pcddSg.setOpenStatus("HANDLE");
//                count = pceggLotterySgMapper.updateByPrimaryKeySelective(pcddSg);
//                // ??????
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
//                    return ResultInfo.error("???????????????");
//                }
//                if (StringUtils.isNotBlank(bjpksSg.getNumber())) {
//                    return ResultInfo.error("??????????????????");
//                }
//                bjpksSg.setNumber(openNumber);
//                bjpksSg.setTime(DateUtils.formatDate(new Date(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
//                bjpksSg.setOpenStatus("HANDLE");
//                count = bjpksLotterySgMapper.updateByPrimaryKeySelective(bjpksSg);
//                // ??????
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
//                    return ResultInfo.error("???????????????");
//                }
//                if (StringUtils.isNotBlank(xyftSg.getNumber())) {
//                    return ResultInfo.error("??????????????????");
//                }
//                xyftSg.setNumber(openNumber);
//                xyftSg.setTime(DateUtils.formatDate(new Date(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
//                xyftSg.setOpenStatus("HANDLE");
//                count = xyftLotterySgMapper.updateByPrimaryKeySelective(xyftSg);
//                // ??????
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
            // ??????????????????,?????????????????????????????????
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
            logger.error("????????????????????????{},{},{}", id, winAmount, openNumber);
            return ResultInfo.error("?????????????????????");
        }
    }

    @Override
    public ResultInfo<Boolean> jiesuanOrderBetByIssue(Integer lotteryId, String issue, String number) {
        try {
            if (lotteryId.toString().equals(CaipiaoTypeEnum.CQSSC.getTagType())) {
                // ??????????????????
                betSscbmService.countlm(issue, number, Integer.parseInt(CaipiaoTypeEnum.CQSSC.getTagType()));
                // ????????????????????????
                betSscbmService.countdn(issue, number, Integer.parseInt(CaipiaoTypeEnum.CQSSC.getTagType()));
                // ?????????????????????
                betSscbmService.count15(issue, number, Integer.parseInt(CaipiaoTypeEnum.CQSSC.getTagType()));
                // ??????????????????????????????
                betSscbmService.countqzh(issue, number, Integer.parseInt(CaipiaoTypeEnum.CQSSC.getTagType()));
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.XJSSC.getTagType())) {
                // ??????????????????
                betSscbmService.countlm(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJSSC.getTagType()));
                // ????????????????????????
                betSscbmService.countdn(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJSSC.getTagType()));
                // ?????????????????????
                betSscbmService.count15(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJSSC.getTagType()));
                // ??????????????????????????????
                betSscbmService.countqzh(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJSSC.getTagType()));
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.TJSSC.getTagType())) {
                // ??????????????????
                betSscbmService.countlm(issue, number, Integer.parseInt(CaipiaoTypeEnum.TJSSC.getTagType()));
                // ????????????????????????
                betSscbmService.countdn(issue, number, Integer.parseInt(CaipiaoTypeEnum.TJSSC.getTagType()));
                // ?????????????????????
                betSscbmService.count15(issue, number, Integer.parseInt(CaipiaoTypeEnum.TJSSC.getTagType()));
                // ??????????????????????????????
                betSscbmService.countqzh(issue, number, Integer.parseInt(CaipiaoTypeEnum.TJSSC.getTagType()));
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.TENSSC.getTagType())) {
                // ??????????????????
                betSscbmService.countlm(issue, number, Integer.parseInt(CaipiaoTypeEnum.TENSSC.getTagType()));
                // ????????????????????????
                betSscbmService.countdn(issue, number, Integer.parseInt(CaipiaoTypeEnum.TENSSC.getTagType()));
                // ?????????????????????
                betSscbmService.count15(issue, number, Integer.parseInt(CaipiaoTypeEnum.TENSSC.getTagType()));
                // ??????????????????????????????
                betSscbmService.countqzh(issue, number, Integer.parseInt(CaipiaoTypeEnum.TENSSC.getTagType()));
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.FIVESSC.getTagType())) {
                // ??????????????????
                betSscbmService.countlm(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVESSC.getTagType()));
                // ????????????????????????
                betSscbmService.countdn(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVESSC.getTagType()));
                // ?????????????????????
                betSscbmService.count15(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVESSC.getTagType()));
                // ??????????????????????????????
                betSscbmService.countqzh(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVESSC.getTagType()));
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.JSSSC.getTagType())) {
                // ??????????????????
                betSscbmService.countlm(issue, number, Integer.parseInt(CaipiaoTypeEnum.JSSSC.getTagType()));
                // ????????????????????????
                betSscbmService.countdn(issue, number, Integer.parseInt(CaipiaoTypeEnum.JSSSC.getTagType()));
                // ?????????????????????
                betSscbmService.count15(issue, number, Integer.parseInt(CaipiaoTypeEnum.JSSSC.getTagType()));
                // ??????????????????????????????
                betSscbmService.countqzh(issue, number, Integer.parseInt(CaipiaoTypeEnum.JSSSC.getTagType()));
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.LHC.getTagType())) {
                // ???????????????- ?????????,??????,??????,??????1-6???
                jiesuanByHandle(issue, number);
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.ONELHC.getTagType())) {
                // ???????????????- ?????????,??????,??????,??????1-6???
                betLhcService.clearingLhcTeMaA(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()),
                        true);
                betLhcService.clearingLhcZhengTe(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()),
                        true);
                betLhcService.clearingLhcZhengMaOneToSix(issue, number,
                        Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                betLhcService.clearingLhcLiuXiao(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()),
                        true);

                // ???????????????- ?????????,??????,?????????
                betLhcService.clearingLhcZhengMaA(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()),
                        true);
                betLhcService.clearingLhcBanBo(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()),
                        true);
                betLhcService.clearingLhcWs(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);

                // ???????????????- ?????????,??????,?????????
                betLhcService.clearingLhcLianMa(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()),
                        true);
                betLhcService.clearingLhcLianXiao(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()),
                        true);
                betLhcService.clearingLhcLianWei(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()),
                        true);

                // ???????????????- ?????????,1-6??????,?????????
                betLhcService.clearingLhcNoOpen(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()),
                        true);
                betLhcService.clearingLhcOneSixLh(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()),
                        true);
                betLhcService.clearingLhcWuxing(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()),
                        true);

                // ???????????????- ?????????,?????????
                betLhcService.clearingLhcPtPt(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()),
                        true);
                betLhcService.clearingLhcTxTx(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()),
                        true);
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.FIVELHC.getTagType())) {
                // ???????????????- ?????????,??????,??????,??????1-6???
                betLhcService.clearingLhcTeMaA(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()),
                        true);
                betLhcService.clearingLhcZhengTe(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()),
                        true);
                betLhcService.clearingLhcZhengMaOneToSix(issue, number,
                        Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                betLhcService.clearingLhcLiuXiao(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()),
                        true);

                // ???????????????- ?????????,??????,?????????
                betLhcService.clearingLhcZhengMaA(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()),
                        true);
                betLhcService.clearingLhcBanBo(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()),
                        true);
                betLhcService.clearingLhcWs(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()),
                        true);

                // ???????????????- ?????????,??????,?????????
                betLhcService.clearingLhcLianMa(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()),
                        true);
                betLhcService.clearingLhcLianXiao(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()),
                        true);
                betLhcService.clearingLhcLianWei(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()),
                        true);

                // ???????????????- ?????????,1-6??????,?????????
                betLhcService.clearingLhcNoOpen(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()),
                        true);
                betLhcService.clearingLhcOneSixLh(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()),
                        true);
                betLhcService.clearingLhcWuxing(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()),
                        true);

                // ???????????????- ?????????,?????????
                betLhcService.clearingLhcPtPt(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()),
                        true);
                betLhcService.clearingLhcTxTx(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()),
                        true);
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.AMLHC.getTagType())) {
                // ???????????????- ?????????,??????,??????,??????1-6???
                betLhcService.clearingLhcTeMaA(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()),
                        true);
                betLhcService.clearingLhcZhengTe(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()),
                        true);
                betLhcService.clearingLhcZhengMaOneToSix(issue, number,
                        Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                betLhcService.clearingLhcLiuXiao(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()),
                        true);

                // ???????????????- ?????????,??????,?????????
                betLhcService.clearingLhcZhengMaA(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()),
                        true);
                betLhcService.clearingLhcBanBo(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()),
                        true);
                betLhcService.clearingLhcWs(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);

                // ???????????????- ?????????,??????,?????????
                betLhcService.clearingLhcLianMa(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()),
                        true);
                betLhcService.clearingLhcLianXiao(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()),
                        true);
                betLhcService.clearingLhcLianWei(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()),
                        true);

                // ???????????????- ?????????,1-6??????,?????????
                betLhcService.clearingLhcNoOpen(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()),
                        true);
                betLhcService.clearingLhcOneSixLh(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()),
                        true);
                betLhcService.clearingLhcWuxing(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()),
                        true);

                // ???????????????- ?????????,?????????
                betLhcService.clearingLhcPtPt(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()),
                        true);
                betLhcService.clearingLhcTxTx(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()),
                        true);
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.BJPKS.getTagType())) {
                // ???????????????PK10-?????????
                betBjpksService.clearingBjpksLm(issue, number, Integer.parseInt(CaipiaoTypeEnum.BJPKS.getTagType()));
                // ???????????????PK10-?????????????????????
                betBjpksService.clearingBjpksCmcCqj(issue, number,
                        Integer.parseInt(CaipiaoTypeEnum.BJPKS.getTagType()));
                // ???????????????PK10-????????????
                betBjpksService.clearingBjpksGyh(issue, number, Integer.parseInt(CaipiaoTypeEnum.BJPKS.getTagType()));
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.TENPKS.getTagType())) {
                // ???????????????PK10-?????????
                betBjpksService.clearingBjpksLm(issue, number, Integer.parseInt(CaipiaoTypeEnum.TENPKS.getTagType()));
                // ???????????????PK10-?????????????????????
                betBjpksService.clearingBjpksCmcCqj(issue, number,
                        Integer.parseInt(CaipiaoTypeEnum.TENPKS.getTagType()));
                // ???????????????PK10-????????????
                betBjpksService.clearingBjpksGyh(issue, number, Integer.parseInt(CaipiaoTypeEnum.TENPKS.getTagType()));
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.FIVEPKS.getTagType())) {
                // ???????????????PK10-?????????
                betBjpksService.clearingBjpksLm(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVEPKS.getTagType()));
                // ???????????????PK10-?????????????????????
                betBjpksService.clearingBjpksCmcCqj(issue, number,
                        Integer.parseInt(CaipiaoTypeEnum.FIVEPKS.getTagType()));
                // ???????????????PK10-????????????
                betBjpksService.clearingBjpksGyh(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVEPKS.getTagType()));
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.JSPKS.getTagType())) {
                // ???????????????PK10-?????????
                betBjpksService.clearingBjpksLm(issue, number, Integer.parseInt(CaipiaoTypeEnum.JSPKS.getTagType()));
                // ???????????????PK10-?????????????????????
                betBjpksService.clearingBjpksCmcCqj(issue, number,
                        Integer.parseInt(CaipiaoTypeEnum.JSPKS.getTagType()));
                // ???????????????PK10-????????????
                betBjpksService.clearingBjpksGyh(issue, number, Integer.parseInt(CaipiaoTypeEnum.JSPKS.getTagType()));
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.XYFEIT.getTagType())) {
                // ?????????????????????-?????????
                betXyftService.clearingXyftLm(issue, number);
                // ?????????????????????-?????????????????????
                betXyftService.clearingXyftCmcCqj(issue, number);
                // ?????????????????????-????????????
                betXyftService.clearingXyftGyh(issue, number);
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.PCDAND.getTagType())) {
                // ?????????PC??????-?????????
                betPceggService.clearingPceggTm(issue, number);
                // ?????????PC??????-?????????
                betPceggService.clearingPceggBz(issue, number);
                // ?????????PC??????-???????????????
                betPceggService.clearingPceggTmbs(issue, number);
                // ?????????PC??????-?????????
                betPceggService.clearingPceggSb(issue, number);
                // ?????????PC??????-?????????
                betPceggService.clearingPceggHh(issue, number);
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.TXFFC.getTagType())) {
                // ??????????????????
                betSscbmService.countlm(issue, number, Integer.parseInt(CaipiaoTypeEnum.TXFFC.getTagType()));
                // ????????????????????????
                betSscbmService.countdn(issue, number, Integer.parseInt(CaipiaoTypeEnum.TXFFC.getTagType()));
                // ?????????????????????
                betSscbmService.count15(issue, number, Integer.parseInt(CaipiaoTypeEnum.TXFFC.getTagType()));
                // ??????????????????????????????
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
//                // ?????????????????????-?????????
//                betNnKlService.countKlXianjia(issue, number, Integer.parseInt(CaipiaoTypeEnum.KLNIU.getTagType()));
//            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.AZNIU.getTagType())) {
//                // ?????????????????????-?????????
//                betNnAzService.countAzXianjia(issue, number, Integer.parseInt(CaipiaoTypeEnum.AZNIU.getTagType()));
//            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.JSNIU.getTagType())) {
//                // ?????????????????????-?????????
//                betNnJsService.countJsXianjia(issue, number, Integer.parseInt(CaipiaoTypeEnum.JSNIU.getTagType()));
//            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.JSPKFT.getTagType())) {
//                // ???????????????pk?????????
//                betFtJspksService.clearingJspksJs(issue, number, Integer.parseInt(CaipiaoTypeEnum.JSPKFT.getTagType()));
//            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.XYFTFT.getTagType())) {
//                // ??????????????????
//                betFtXyftService.clearingFtXyftJs(issue, number, Integer.parseInt(CaipiaoTypeEnum.XYFTFT.getTagType()));
//            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.JSSSCFT.getTagType())) {
//                // ?????????????????????????????????
//                betFtSscService.clearingFtSscJs(issue, number, Integer.parseInt(CaipiaoTypeEnum.JSSSCFT.getTagType()));
//            }
            else if (lotteryId.toString().equals(CaipiaoTypeEnum.AUSACT.getTagType())) {
                // ??????act??????
                betActAzService.countAzAct(issue, number, Integer.parseInt(CaipiaoTypeEnum.AUSACT.getTagType()));
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.AUSSSC.getTagType())) {
                // ???????????????????????????
                betSscAzService.countAzSsc(issue, number, Integer.parseInt(CaipiaoTypeEnum.AUSSSC.getTagType()));
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.AUSPKS.getTagType())) {
                // ???????????????F1???
                betF1AzService.countAzF1(issue, number, Integer.parseInt(CaipiaoTypeEnum.AUSPKS.getTagType()));
            }else  if (lotteryId.toString().equals(CaipiaoTypeEnum.AZKS.getTagType())) {
                // ????????????????????? ?????????
                betksService.clearingKsLm(issue, number, Integer.parseInt(CaipiaoTypeEnum.AZKS.getTagType()));
                // ????????????????????? ?????????
                betksService.clearingKsDd(issue, number, Integer.parseInt(CaipiaoTypeEnum.AZKS.getTagType()));
                // ????????????????????? ?????????
                betksService.clearingKsLh(issue, number, Integer.parseInt(CaipiaoTypeEnum.AZKS.getTagType()));
                // ????????????????????? ????????????  ????????????
                betksService.clearingKsEbTh(issue, number, Integer.parseInt(CaipiaoTypeEnum.AZKS.getTagType()));
                // ????????????????????? ???????????? ????????????
                betksService.clearingKsSbTh(issue, number, Integer.parseInt(CaipiaoTypeEnum.AZKS.getTagType()));
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.DZKS.getTagType())) {
                // ????????????????????? ?????????
                betksService.clearingKsLm(issue, number, Integer.parseInt(CaipiaoTypeEnum.DZKS.getTagType()));
                // ????????????????????? ?????????
                betksService.clearingKsDd(issue, number, Integer.parseInt(CaipiaoTypeEnum.DZKS.getTagType()));
                // ????????????????????? ?????????
                betksService.clearingKsLh(issue, number, Integer.parseInt(CaipiaoTypeEnum.DZKS.getTagType()));
                // ????????????????????? ????????????  ????????????
                betksService.clearingKsEbTh(issue, number, Integer.parseInt(CaipiaoTypeEnum.DZKS.getTagType()));
                // ????????????????????? ???????????? ????????????
                betksService.clearingKsSbTh(issue, number, Integer.parseInt(CaipiaoTypeEnum.DZKS.getTagType()));
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.DZPCDAND.getTagType())) {
                // ???????????????PPC??????-?????????
                betDzpceggService.clearingDzpceggTm(issue, number);
                // ???????????????PPC??????-?????????
                betDzpceggService.clearingDzpceggBz(issue, number);
                // ???????????????PPC??????-???????????????
                betDzpceggService.clearingDzpceggTmbs(issue, number);
                // ???????????????PPC??????-?????????
                betDzpceggService.clearingDzpceggSb(issue, number);
                // ???????????????PPC??????-?????????
                betDzpceggService.clearingDzpceggHh(issue, number);
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.DZXYFEIT.getTagType())) {
                // ???????????????????????????-?????????
                betDzxyftService.clearingDzxyftLm(issue, number);
                // ???????????????????????????-?????????????????????
                betDzxyftService.clearingDzxyftCmcCqj(issue, number);
                // ???????????????????????????-????????????
                betDzxyftService.clearingDzxyftGyh(issue, number);
            } else if (lotteryId.toString().equals(CaipiaoTypeEnum.XJPLHC.getTagType())) {
                // ????????????????????????- ?????????,??????,??????,??????1-6???
                betNewLhcService.clearingLhcTeMaA(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                betNewLhcService.clearingLhcZhengTe(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                betNewLhcService.clearingLhcZhengMaOneToSix(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                betNewLhcService.clearingLhcLiuXiao(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);

                // ????????????????????????- ?????????,??????,?????????
                betNewLhcService.clearingLhcZhengMaA(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                betNewLhcService.clearingLhcBanBo(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                betNewLhcService.clearingLhcWs(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);

                // ????????????????????????- ?????????,??????,?????????
                betNewLhcService.clearingLhcLianMa(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                betNewLhcService.clearingLhcLianXiao(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                betNewLhcService.clearingLhcLianWei(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);

//                 ????????????????????????- ?????????,1-6??????,?????????
                betNewLhcService.clearingLhcNoOpen(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                betNewLhcService.clearingLhcOneSixLh(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                betNewLhcService.clearingLhcWuxing(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);

                // ????????????????????????- ?????????,?????????
                betNewLhcService.clearingLhcPtPt(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                betNewLhcService.clearingLhcTxTx(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
            }


            return ResultInfo.ok(true);
        } catch (Exception e) {
            logger.error("bet7xcHnService occur error.", e);
            return ResultInfo.error("????????????!");
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
                return ResultInfo.error("???????????????????????????????????????");
            }
            for (OrderBetRecord record : orderBetRecordList) {
                record.setTbStatus(OrderBetStatus.BACK);
                betCommonService.winOrLose(record, BigDecimal.ZERO, record.getUserId(), record.getOrderSn());
            }
            return ResultInfo.ok();
        } catch (Exception e) {
            logger.error("???????????????????????????{},{}", lotteryId, issue, e);
            return ResultInfo.error("????????????????????????");
        }

    }

    //   ???????????????
    @Override
    public ResultInfo<Boolean> jiesuanBySg(String issue, String openNumber) {
        // ??????????????????????????????
        // ???????????????????????????????????????
//        rabbitTemplate.convertAndSend(RabbitConfig.TOPIC_EXCHANGE, RabbitConfig.BINDING_LHC, "LHC:" + issue + ":" + number);
//		String jiesuanMessage = String.valueOf(basicRedisClient.hGet(Constants.LHC_KAIJIANG_STATUS, "JIESUAN_MESSAGE"));
        if (issue != null && openNumber != null) {
            // ???????????????????????????????????????
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
                // ?????????????????????100s???????????????10S[????????????]????????????????????????????????????????????????????????????????????????????????????
//            boolean bool = lock.writeLock().tryLock(2, 10, TimeUnit.SECONDS);
                // ????????????????????????
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
                                logger.error("MQ???????????????", e1);
                            }

                            count++;
                            if (count > 100) {
                                logger.error("??????????????????{}???????????????????????????????????????????????????????????????????????????????????????", count);
                                break;
                            }

                            try {
                                Thread.sleep(20000);
                            } catch (Exception e) {
                                logger.error("", e);
                            }

                            // ?????????????????????????????????????????????????????????
                            OrderBetRecordExample orderBetExample = new OrderBetRecordExample();
                            OrderBetRecordExample.Criteria orderBetCriteria = orderBetExample.createCriteria();
                            orderBetCriteria.andIssueEqualTo(issue);
                            orderBetCriteria.andTbStatusEqualTo(Constants.WAIT);// ????????????
                            int n = orderBetRecordMapper.countByExample(orderBetExample);
                            logger.info("???[{}]?????????????????????,???????????????[{}]???????????????", count, n);
                            if (n < 1) {
                                break;
                            }
                        }

                        //?????????
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
                logger.error("?????????jiesuanLuc occur error.", e);
                return ResultInfo.error("????????????!");
            }
        }
        return ResultInfo.ok(true);
    }


    @Override
    public ResultInfo<Boolean> jiesuanByHandle(String issue, String openNumber) {
        // ??????????????????????????????
        // ???????????????????????????????????????
//        rabbitTemplate.convertAndSend(RabbitConfig.TOPIC_EXCHANGE, RabbitConfig.BINDING_LHC, "LHC:" + issue + ":" + number);
//		String jiesuanMessage = String.valueOf(basicRedisClient.hGet(Constants.LHC_KAIJIANG_STATUS, "JIESUAN_MESSAGE"));
        if (issue != null && openNumber != null) {
            // ???????????????????????????????????????
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
                // ?????????????????????
                sg.setNumber(openNumber);
                int i = lhcLotterySgMapper.updateByPrimaryKey(sg);
                if (i > 0) {
                    logger.info("???????????????:{}????????????:{}??????", issue, openNumber);
                } else {
                    logger.info("???????????????:{}????????????:{}??????", issue, openNumber);
                }
            }

            String key = issue + "jiesuan";
            RedisLock lock = new RedisLock(key + "lock", 50 * 1000, 30 * 1000);
            try {
                // ?????????????????????100s???????????????10S[????????????]????????????????????????????????????????????????????????????????????????????????????
//            boolean bool = lock.writeLock().tryLock(2, 10, TimeUnit.SECONDS);
                // ????????????????????????
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
                                logger.error("MQ???????????????", e1);
                            }

                            count++;
                            if (count > 100) {
                                logger.error("??????????????????{}???????????????????????????????????????????????????????????????????????????????????????", count);
                                break;
                            }

                            try {
                                Thread.sleep(20);
                            } catch (Exception e) {
                                logger.error("", e);
                            }

                            // ?????????????????????????????????????????????????????????
                            OrderBetRecordExample orderBetExample = new OrderBetRecordExample();
                            OrderBetRecordExample.Criteria orderBetCriteria = orderBetExample.createCriteria();
                            orderBetCriteria.andIssueEqualTo(issue);
                            orderBetCriteria.andTbStatusEqualTo(Constants.WAIT);// ????????????
                            int n = orderBetRecordMapper.countByExample(orderBetExample);
                            logger.info("???[{}]?????????????????????,???????????????[{}]???????????????", count, n);
                            if (n < 1) {
                                break;
                            }
                        }

                        //?????????
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
                logger.error("?????????jiesuanLuc occur error.", e);
                return ResultInfo.error("????????????!");
            }

            redisTemplate.opsForHash().delete(Constants.LHC_KAIJIANG_STATUS, "JIESUAN_MESSAGE");

        }
        return ResultInfo.ok(true);
    }

    @Override
    public ResultInfo<Boolean> jiesuanByHandleFalse(String issue, String openNumber) { // ????????????????????????????????????
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

                // ???????????????- ?????????,??????,??????,??????1-6???
                betLhcService.clearingLhcTeMaA(issue, openNumber, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()),
                        false);
                betLhcService.clearingLhcZhengTe(issue, openNumber, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()),
                        false);
                betLhcService.clearingLhcZhengMaOneToSix(issue, openNumber,
                        Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()), false);
                betLhcService.clearingLhcLiuXiao(issue, openNumber, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()),
                        false);

                // ???????????????- ?????????,??????,?????????
                betLhcService.clearingLhcZhengMaA(issue, openNumber, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()),
                        false);
                betLhcService.clearingLhcBanBo(issue, openNumber, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()),
                        false);
                betLhcService.clearingLhcWs(issue, openNumber, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()),
                        false);

                // ???????????????- ?????????,??????,?????????
                betLhcService.clearingLhcLianMa(issue, openNumber, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()),
                        false);
                betLhcService.clearingLhcLianXiao(issue, openNumber, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()),
                        false);
                betLhcService.clearingLhcLianWei(issue, openNumber, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()),
                        false);

                // ???????????????- ?????????,1-6??????,?????????
                betLhcService.clearingLhcNoOpen(issue, openNumber, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()),
                        false);
                betLhcService.clearingLhcOneSixLh(issue, openNumber, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()),
                        false);
                betLhcService.clearingLhcWuxing(issue, openNumber, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()),
                        false);

                // ???????????????- ?????????,?????????
                betLhcService.clearingLhcPtPt(issue, openNumber, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()),
                        false);
                betLhcService.clearingLhcTxTx(issue, openNumber, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()),
                        false);
            } catch (Exception e) {
                logger.error("?????????jiesuanLuc occur error.", e);
                return ResultInfo.error("????????????!");
            }

        }
        return ResultInfo.ok(true);

    }

    /**
     * ?????????????????????????????????????????????????????????
     *
     * @param issue ????????????
     * @return
     */
    private String xjsscNextIssue(String issue) {
        // ?????????????????????
        String nextIssue;
        // ???????????????????????????
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
     * ?????????????????????????????????????????????????????????
     *
     * @param issue ????????????
     * @return
     */
    private String cqsscNextIssue(String issue) {
        // ?????????????????????
        String nextIssue;
        // ???????????????????????????
        if ("120".equals(issue.substring(8))) {
            String nextDay = TimeHelper.date("yyyyMMdd");
            nextIssue = nextDay + "001";
        } else {
            nextIssue = Long.toString(Long.valueOf(issue) + 1);
        }
        return nextIssue;
    }

    /**
     * ????????????????????????????????????????????????????????????
     *
     * @param issue ????????????
     * @return
     */
    private String txffcNextIssue(String issue) {
        // ?????????????????????
        String nextIssue;
        // ???????????????
        String[] num = issue.split("-");
        // ???????????????????????????
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
     * ????????????????????????????????????????????????
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
     * ????????????????????????????????????????????????
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
     * ?????????PC?????????/ ?????????PK10????????????????????????
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
     * ?????????????????????????????????????????????
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
            // ????????????
            if (CaipiaoTypeEnum.SICAI.contains(lotteryId)) {
//                            1901:KLNIU  1105:FIVESSC    ???????????????5??????????????????
//                            1903:JSNIU  1304:JSPKS    ?????????????????????pks ??????
//                            2001:JSPKFT  1304:JSPKS    ??????PK10?????? ?????????pks ??????
//                            2003:JSSSCFT  1106:JSSSC    ????????????????????? ????????????????????????

//                if (lotteryId.equals(CaipiaoTypeEnum.KLNIU.getTagType())) {
//                    lotteryId = CaipiaoTypeEnum.FIVESSC.getTagType();
//                } else if (lotteryId.equals(CaipiaoTypeEnum.JSNIU.getTagType()) || lotteryId.equals(CaipiaoTypeEnum.JSPKFT.getTagType())) {
//                    lotteryId = CaipiaoTypeEnum.JSPKS.getTagType();
//                } else if (lotteryId.equals(CaipiaoTypeEnum.JSSSCFT.getTagType())) {
//                    lotteryId = CaipiaoTypeEnum.JSSSC.getTagType();
//                }
                KillConfig killConfig = getKillConfig(lotteryId);
                double winratio = killConfig.getRatio();// ????????????
                // ??????????????????

                logger.info("??????????????????????????????{}????????????????????????{}, ?????????{}", winratio, killConfig.getPlatfom()
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
                                    // ???????????????/????????????
                                    String winCount = odds.getWinCount();
                                    String totalCount = odds.getTotalCount();
                                    if (totalCount.contains("/")) {
                                        odd = totalCount;
                                    } else {
                                        // ????????????
                                        odd = String.valueOf(Double.parseDouble(totalCount) * 1.0
                                                / Double.parseDouble(winCount) * divisor);
                                    }
                                    break;
                                }
                                // ???????????????/????????????
                                String winCount = odds.getWinCount();
                                String totalCount = odds.getTotalCount();

                                // ????????????
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
                            // ???????????????/????????????
                            String winCount = odds.getWinCount();
                            String totalCount = odds.getTotalCount();
                            if (totalCount.contains("/")) {
                                odd = totalCount;
                            } else {
                                // ????????????
                                odd = String.valueOf(Double.parseDouble(totalCount) * 1.0
                                        / Double.parseDouble(winCount) * divisor);
                            }
                        }

                        okill.setOdds(odd);
                        okill.setKillConfig(killConfig);
                        //??????????????????
                        if (redisTemplate.hasKey(RedisKeys.KILLORDERTIME)) {
                            okill.setWaittime(Integer.parseInt((String) redisTemplate.opsForValue().get(RedisKeys.KILLORDERTIME)) * 1000);
                        }
                        Destination sicaikill = null;
                        if (Constants.NEW_LOTTERY_ID_LIST.contains(okill.getLotteryId())) { // ?????????
                            sicaikill = new ActiveMQQueue(ActiveMQConfig.SICAIORDERKILL_NEW);
                        } else { // ?????????
                            sicaikill = new ActiveMQQueue(ActiveMQConfig.LIVESICAIORDERKILL);
                        }

                        // ???????????????????????????
                        jmsMessagingTemplate.convertAndSend(sicaikill,
                                "orderkill:" + JSON.toJSONString(okill));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("??????????????? ??????????????????", e);
        }
    }

    @Override
    public ResultInfo<String> shareOrder(ShareOrderDTO data) {
        String key = "shareOrder" + data.getOrderBetId().toString();
        RReadWriteLock lock = redissonClient.getReadWriteLock(key);
        try {
            // ?????????????????????100s???????????????10S[????????????]????????????????????????????????????????????????????????????????????????????????????
            boolean bool = lock.writeLock().tryLock(100, 10, TimeUnit.SECONDS);
            // ????????????????????????
            if (bool) {
                if (new BigDecimal(data.getBetCount()).compareTo(BigDecimal.valueOf(0)) <= 0) {
                    return ResultInfo.error("?????????????????????0?????????0???");
                }

                // ???????????????id
                Integer orderBetId = data.getOrderBetId();
                // ??????????????????
                OrderBetRecord orderBetRecord;
                if (redisTemplate.hasKey(ORDER_BET_KEY + orderBetId)) {
                    orderBetRecord = (OrderBetRecord) redisTemplate.opsForValue().get(ORDER_BET_KEY + orderBetId);
                } else {
                    orderBetRecord = orderBetRecordMapper.selectByPrimaryKey(orderBetId);
                }

                // ??????
                if (orderBetRecord == null) {
                    return ResultInfo.error("??????????????????????????????");
                }

                // ?????????????????????
                if (!orderBetRecord.getTbStatus().equals(OrderBetStatus.WAIT)) {
                    return ResultInfo.error("??????????????????????????????????????????");
                }

                // ??????????????????
                OrderRecord orderRecord;
                if (redisTemplate.hasKey(ORDER_KEY + orderBetId)) {
                    orderRecord = (OrderRecord) redisTemplate.opsForValue().get(ORDER_KEY + orderBetId);
                } else {
                    orderRecord = orderRecordMapper.selectByPrimaryKey(orderBetRecord.getOrderId());
                    redisTemplate.opsForValue().set(ORDER_KEY + orderBetId, orderRecord);
                }

                // ????????????????????????

                // ??????????????????
                Integer userId = data.getUserId();
                if (userId == null || userId < 1) {
                    logger.error("userId[{}]????????????", userId);
                    return ResultInfo.error("?????????????????????");
                }

                MemUser memUser = memUserMapper.selectByPrimaryKey((long) userId);

                if (memUser == null) {
                    return ResultInfo.error("?????????????????????????????????");
                }

                if (memUser.getIsFrozen()) {
                    return ResultInfo.error("?????????????????????????????????????????????");
                }


                // ????????????????????????
                BigDecimal betAmount = new BigDecimal(orderBetRecord.getBetCount())
                        .multiply(new BigDecimal(data.getBetCount()));

                BetRestrict restrict = this.getBonusMap(orderBetRecord.getLotteryId(), orderBetRecord.getPlayId());
                if (restrict != null && restrict.getMaxMoney().compareTo(BigDecimal.valueOf(0)) > 0) {
                    if (betAmount.compareTo(restrict.getMaxMoney()) > 0) {
                        return ResultInfo.error("?????????????????????????????????????????????????????????");
                    }
                } else {
                    // ????????????????????????
                    BetRestrict betRestrict = this.getBonusMap(orderBetRecord.getLotteryId(), 0);
                    if (null != betRestrict && betRestrict.getMaxMoney().compareTo(BigDecimal.valueOf(0)) > 0) {
                        if (betAmount.compareTo(betRestrict.getMaxMoney()) > 0) {
                            return ResultInfo.error("?????????????????????????????????????????????????????????");
                        }
                    }

                }

                // ??????????????????
                MemWalletExample memWalletExample = new MemWalletExample();
                MemWalletExample.Criteria walletCriteria = memWalletExample.createCriteria();
                walletCriteria.andUserIdEqualTo(Long.valueOf(userId));
                MemWallet wallet = memWalletMapper.selectOneByExample(memWalletExample);
                BigDecimal balance =wallet.getBalance();
                if (betAmount.compareTo(balance) > 0) {
                    return ResultInfo.error("???????????????");
                }

                // ???????????????????????????????????????
                ResultInfo<Boolean> resultInfo = this.checkIssueIsOpen(orderBetRecord.getLotteryId(),
                        orderRecord.getIssue(), 1);
                if (resultInfo.getCode() != StatusCode.SUCCESSCODE.getCode()) {
                    return ResultInfo.error("??????????????????");
                }

                String orderSn = SnowflakeIdWorker.createOrderSn();
                // ????????????
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
                // ?????????????????????
                orderRecordMapper.insertSelective(order);
                betAmount = getTradeOffAmount(betAmount);
                // ??????????????????
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
                // ?????????????????????
                orderBetRecordMapper.insertSelective(orderBet);

                //ONELIVE TODO ????????????
//                /**
//                 * ??????????????????
//                 */
//                MemGoldchangeDO dto = new MemGoldchangeDO();
//                // ????????????id
//                dto.setUserId(order.getUserId());
//                // ????????????
//                dto.setOpnote("??????/" + order.getOrderSn());
//                // ????????????
//                dto.setChangetype(GoldchangeEnum.LOTTERY_BETTING.getValue());
//                // ???????????????????????????
//                dto.setQuantity(betAmount.negate());
//                // ?????????????????????????????????????????????
//                dto.setNoWithdrawalAmount(betAmount.negate());
//                // ???????????????????????????
//                dto.setBetAmount(betAmount);
//                // ????????????????????????
//                dto.setWaitAmount(betAmount);
//                memBaseinfoWriteService.updateUserBalance(dto);

//                // ?????????????????????
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
            // ?????????
            lock.writeLock().unlock();
        }
        return ResultInfo.ok("????????????");
    }

    @Override
    @Transactional
    public ResultInfo liveRoomCopy(OrderFollow data) {
        String key = "liveRoomCopy" + data.getOrders().toString();
        RReadWriteLock lock = redissonClient.getReadWriteLock(key);
        logger.info("{}.getReadWriteLock,params:{}", key);
        try {
            // ?????????????????????100s???????????????10S[????????????]????????????????????????????????????????????????????????????????????????????????????
            boolean bool = lock.writeLock().tryLock(100, 10, TimeUnit.SECONDS);

            // ????????????????????????
            if (bool) {
                Map<String, LotteryPlaySetting> lotterySettingMap = redisTemplate.opsForHash().entries(RedisKeys.LOTTERY_PLAY_SETTING_MAP_TYPE + Constants.LOTTERY_CATEGORY_TYPE_LOTTERY);
                if (CollectionUtil.isEmpty(lotterySettingMap)) {
                    lotterySettingMap = lotteryPlaySettingReadRest.queryLotteryPlaySettingMap(Constants.LOTTERY_CATEGORY_TYPE_LOTTERY);
                }
                if (null == lotterySettingMap) {
                    return ResultInfo.error("???????????????????????????");
                }

                BigDecimal amount = orderBetRecordMapperExt.getOrderBetRecordAmount(data.getOrders());
                // ??????????????????
                MemWalletExample memWalletExample = new MemWalletExample();
                MemWalletExample.Criteria walletCriteria = memWalletExample.createCriteria();
                walletCriteria.andUserIdEqualTo(Long.valueOf(data.getUserId()));
                MemWallet wallet = memWalletMapper.selectOneByExample(memWalletExample);
                BigDecimal balance =wallet.getBalance();

                // ??????????????????
                if (balance == null || amount.compareTo(balance) > 0) {
                    logger.info("??????????????????");
                    ResultInfo response = ResultInfo.ok();
                    response.setCode(StatusCode.CHANGE_BALANCE_LACK.getCode());
                    response.setMsg("????????????,?????????!");
                    return response;
                }
                for (Integer id : data.getOrders()) {
                    // ??????????????????
                    OrderBetRecord orderBetRecord;
                    if (redisTemplate.hasKey(ORDER_BET_KEY + id)) {
                        orderBetRecord = (OrderBetRecord) redisTemplate.opsForValue().get(ORDER_BET_KEY + id);
                    } else {
                        orderBetRecord = orderBetRecordMapper.selectByPrimaryKey(id);
                        redisTemplate.opsForValue().set(ORDER_BET_KEY + id, orderBetRecord);
                    }

                    // ??????
                    if (orderBetRecord == null) {
                        return ResultInfo.error("??????????????????????????????");
                    }
                    Integer orderId = orderBetRecord.getOrderId();
                    // ??????????????????
                    OrderRecord orderRecord;
                    if (redisTemplate.hasKey(ORDER_KEY + orderId)) {
                        orderRecord = (OrderRecord) redisTemplate.opsForValue().get(ORDER_KEY + orderId);
                    } else {
                        orderRecord = orderRecordMapper.selectByPrimaryKey(orderId);
                        redisTemplate.opsForValue().set(ORDER_KEY + orderId, orderRecord);
                    }
                    if (orderRecord == null) {
                        return ResultInfo.error("??????????????????????????????");
                    }
                    logger.info("????????????:{}", orderBetRecord.toString());


                    if (null == lotterySettingMap.get(String.valueOf(orderBetRecord.getPlayId()))) {
                        return ResultInfo.error("??????????????????????????????????????????");
                    }

                    // ?????????????????????
                    if (!orderBetRecord.getTbStatus().equals(OrderBetStatus.WAIT)) {
                        return ResultInfo.error("??????????????????????????????????????????");
                    }

                    // ????????????????????????
                    BigDecimal betAmount = orderBetRecord.getBetAmount();
                    // BigDecimal amount = betAmount.multiply(new BigDecimal(orderBetRecord.getBetCount()));

                    BetRestrict restrict = this.getBonusMap(orderBetRecord.getLotteryId(), orderBetRecord.getPlayId());
                    if (restrict != null && restrict.getMaxMoney().compareTo(BigDecimal.valueOf(0)) > 0) {
                        if (betAmount.compareTo(restrict.getMaxMoney()) > 0) {
                            return ResultInfo.error("?????????????????????????????????????????????????????????");
                        }
                    } else {
                        // ????????????????????????
                        BetRestrict betRestrict = this.getBonusMap(orderBetRecord.getLotteryId(), 0);
                        if (null != betRestrict && betRestrict.getMaxMoney().compareTo(BigDecimal.valueOf(0)) > 0) {
                            if (betAmount.compareTo(betRestrict.getMaxMoney()) > 0) {
                                return ResultInfo.error("?????????????????????????????????????????????????????????");
                            }
                        }
                    }

                    // ???????????????????????????????????????
                    ResultInfo<Boolean> resultInfo = this.checkIssueIsOpen(orderBetRecord.getLotteryId(),
                            orderRecord.getIssue(), 1);
                    if (resultInfo.getCode() != StatusCode.SUCCESSCODE.getCode()) {
                        return ResultInfo.error("??????????????????");
                    }
                    String orderSn = SnowflakeIdWorker.createOrderSn();
                    // ????????????
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
                    order.setBuySource(KEY_FIVES);//?????????????????????
                    // ?????????????????????
                    orderRecordMapper.insertSelective(order);
                    logger.info("????????????:{}", order.toString());
                    // ??????????????????
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
                    // ?????????????????????
                    orderBetRecordMapper.insertSelective(orderBet);
                    logger.info("???????????????:{}", orderBet.toString());

                    /**
                     *   ONELIVE TODO ??????????????????
                     */

                }
            }
        } catch (Exception e) {
            logger.error("liveRoomCopy occur error.", e);
            throw new RuntimeException(e.getMessage());
        } finally {
            // ?????????
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
        return ResultInfo.error("?????????????????????????????????");
    }

}
