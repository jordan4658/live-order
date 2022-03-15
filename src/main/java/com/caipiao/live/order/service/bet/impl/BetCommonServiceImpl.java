package com.caipiao.live.order.service.bet.impl;

import com.alibaba.fastjson.JSONObject;
import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.constant.RedisKeys;
import com.caipiao.live.common.enums.SysParameterEnum;
import com.caipiao.live.common.model.dto.order.OrderBetStatus;
import com.caipiao.live.common.mybatis.entity.*;
import com.caipiao.live.common.mybatis.mapper.LotteryMapper;
import com.caipiao.live.common.mybatis.mapper.OrderBetRecordMapper;
import com.caipiao.live.common.mybatis.mapper.OrderRecordMapper;
import com.caipiao.live.common.service.sys.SysParamService;
import com.caipiao.live.common.util.http.HttpClientUtil;
import com.caipiao.live.order.service.bet.BetCommonService;
import com.caipiao.live.order.service.lottery.LotteryWriteService;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.caipiao.live.common.util.ViewUtil.getTradeOffAmount;

/**
 * @author lzy
 * @create 2018-10-09 10:14
 **/
@Service
public class BetCommonServiceImpl implements BetCommonService {

    private static final String WITHDRAWAL_KEY = "WITHDRAWAL_KEY";
    private static final String LOTTERY_KEY = "LOTTERY_KEY_";
    private static final String ODDS_KEY = "ODDS_KEY_";
    private static final String WIN_PUSH_KEY = "WIN_PUSH_KEY";
    private static final String WIN_PUSH_NUMBER_KEY = "WIN_PUSH_NUMBER_KEY";
    private static final Logger logger = LoggerFactory.getLogger(BetCommonServiceImpl.class);

    @Value("${chat.api.kick.host}")
    private String chatHost;
    @Value("${chat.api.kick.sign}")
    private String chatSign;
    @Value("${chat.api.kick.authgc}")
    private String chatAuthGC;

    @Autowired
    private SysParamService sysParamService;
    @Autowired
    private OrderBetRecordMapper orderBetRecordMapper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private LotteryMapper lotteryMapper;
    @Autowired
    private LotteryWriteService lotteryWriteService;
    @Autowired
    private OrderRecordMapper orderRecordMapper;

    @Autowired
    private RedissonClient redissonClient;

    @Override
    @Transactional
    public void winOrLose(OrderBetRecord orderBet, BigDecimal winAmount, Integer userId, String orderSn) {
        long begin = System.currentTimeMillis();

        String lockKey = RedisKeys.WIN_OR_LOSE + userId + Constants.STR_UNDERLINE + orderBet.getId();
        RReadWriteLock lock = redissonClient.getReadWriteLock(lockKey + Constants.LOCK);
        try {
            //超时时间50秒，释放时间30秒
            boolean bool = lock.writeLock().tryLock(100, 30, TimeUnit.SECONDS);
            if (bool) {
                boolean toPushMsg = false;
                // 手动结算传来的值
                if (orderBet.getTbStatus() != null && !orderBet.getTbStatus().equals(OrderBetStatus.WAIT)) {
                    if (orderBet.getTbStatus().equals(OrderBetStatus.WIN)) {
                        toPushMsg = true;
                    }
                } else {
                    // 打合
                    if (orderBet.getTbStatus() != null && orderBet.getTbStatus().equals(OrderBetStatus.HE)) {
                        logger.info("订单打和. userId:{}; orderBetId:{}", userId, orderBet.getId());
                    } else if (winAmount.compareTo(orderBet.getBetAmount()) == 0) {
                        orderBet.setTbStatus(OrderBetStatus.HE);
                    } else if (winAmount.compareTo(BigDecimal.ZERO) > 0) {
                        orderBet.setTbStatus(OrderBetStatus.WIN);
                        toPushMsg = true;
                    } else {
                        orderBet.setTbStatus(OrderBetStatus.NO_WIN);
                    }
                }

                try {
                    if (toPushMsg) {
                        //Lottery lottery = lotteryWriteService.selectLotteryById(orderBet.getLotteryId());
                        //if (lottery != null) {
                        //jPushService.winPush(orderBet.getUserId(), lottery.getName(), orderSn);
                        //}
                        logger.info("BetCommonServiceImpl.winOrLose:orderbet:{}", JSONObject.toJSONString(orderBet));
                        Long roomId = orderBet.getRoomId();
                        if (roomId != null) {
                            //onelive TODO 推送中间结果
//                            BasAnchorroom room = BasAnchorRoomRestRedis.selectByPrimaryKey(roomId, basAnchorRoomRest);
//                            if (StringUtils.isNotBlank(room.getStreamkey())) {
//                                MemBaseinfo memById = memBaseinfoService.getMemById(Long.valueOf(orderBet.getUserId()));
//                                Lottery lottery = lotteryWriteService.selectLotteryById(orderBet.getLotteryId());
//                                ChatBody msg = new ChatBody();
//                                msg.setType(ChatMsgTypeEnum.LOTTERY_WIN.getValue());
//                                msg.setStream(room.getStreamkey());
//                                msg.setAccno(memById.getAccno());
//                                msg.setMessage(memById.getNickname() + "在" + lottery.getName() + "中了" + winAmount.setScale(3, BigDecimal.ROUND_DOWN) + "播幣");
//                                publishService.publish(roomId, msg);
//                            }

                        }
                    }
                } catch (Exception e) {
                    logger.error("推送中奖失败：{}", orderSn, e);
                }
                logger.debug("BetCommonServiceImpl time, {}", System.currentTimeMillis() - begin);
                logger.info("winOrLose  订单号{}jPushService.winPush 耗时 time, {}", orderSn,
                        System.currentTimeMillis() - begin);
                // 设置中奖金额
                orderBet.setWinAmount(getTradeOffAmount(winAmount));
                orderBet.setUpdateTime(new Date());

                // 幂等校验
                OrderBetRecordExample example = new OrderBetRecordExample();
                example.createCriteria().andIsDeleteEqualTo(false).andIdEqualTo(orderBet.getId());
                OrderBetRecord record = orderBetRecordMapper.selectOneByExample(example);
                if (null == record) {
                    logger.error("OrderBetRecord id={} 不存在或已删除", orderBet.getId());
                    return;
                }
                String taStatus = StringUtils.isBlank(orderBet.getTbStatus()) ? "" : orderBet.getTbStatus();
                if (taStatus.equals(record.getTbStatus())) {
                    logger.warn("要结算的订单 OrderBetRecord[id={}] 已处理，该笔订单业务结束。", orderBet.getId());
                    return;
                }

                // 修改投注信息
                begin = System.currentTimeMillis();
                orderBetRecordMapper.updateByPrimaryKeySelective(orderBet);
                logger.debug("updateByPrimaryKeySelective time, {}", System.currentTimeMillis() - begin);
                logger.info("winOrLose  订单号{}updateByPrimaryKeySelective 耗时 time, {}", orderSn,
                        System.currentTimeMillis() - begin);

                // 修改用户余额信息
                if (orderBet.getTbStatus() != null && orderBet.getTbStatus().equals(OrderBetStatus.BACK)) { // 退单
                    winAmount = getTradeOffAmount(orderBet.getBetAmount());
                }
                begin = System.currentTimeMillis();
                updateMemberBalance(orderBet, winAmount, userId, orderSn);
                logger.debug("updateMemberBalance time, {}", System.currentTimeMillis() - begin);
                logger.info("winOrLose  订单号{}updateMemberBalance 耗时 time, {}", orderSn,
                        System.currentTimeMillis() - begin);
                /**
                 * 判断是否为推单 修改为批量更新
                 */
//                circleGodPushOrderBeanMapper.updateFinishStatusByBetId(PushOrderFinishStatusEnum.BALANCE_DRAW_FINISHED.getCode(), orderBet.getId());
//                logger.info("winOrLose  订单号{} 耗时 time, {}", orderSn, System.currentTimeMillis() - begin);

//                /**
//                 * 判断是否是分享投注订单跟单
//                 */
//                if (toPushMsg) {
//                    commWinSendOut(userId, orderBet, winAmount);
//                }
                /**
                 * 判断是否为跟单
                 */
//                Integer godOrderId = orderBet.getGodOrderId();
//                if (OrderBetStatus.NO_WIN.equals(orderBet.getTbStatus()) || 0 == godOrderId) {
//                    return;
//                }
//                logger.info("winOrLose  推单业务逻辑处理开始=====================================");
//                // 获取推单信息
//                CircleGodPushOrder godPushOrder = (CircleGodPushOrder) redisTemplate.opsForValue()
//                        .get(RedisKeys.GOD_PUSH_KEY + godOrderId);
//                if (null == godPushOrder) {
//                    godPushOrder = circleGodPushOrderMapper.selectByPrimaryKey(godOrderId);
//                    redisTemplate.opsForValue().set(RedisKeys.GOD_PUSH_KEY + godOrderId, godPushOrder);
//                }
//                logger.info("winOrLose  推单业务逻辑算法计算==================================godPushOrder{}",
//                        JSONObject.toJSONString(godPushOrder));
//                logger.info("winOrLose  推单业务逻辑算法计算=================================orderBet{}",
//                        JSONObject.toJSONString(orderBet));
//                // 计算实际赔率(保留两位小数)
//                BigDecimal winOdds = winAmount.divide(orderBet.getBetAmount(), 2, BigDecimal.ROUND_HALF_UP);
//                // 判断实际赔率是否大于保障赔率
//                logger.info("winOrLose  推单业务逻辑算法实际赔率=================================={},winAmount{},BetAmount",
//                        winOdds, winAmount, orderBet.getBetAmount());
//                if (winOdds.compareTo(godPushOrder.getEnsureOdds()) == -1) {
//                    return;
//                }
//
//                // 计算分红比例
//                BigDecimal bonusScale = godPushOrder.getBonusScale().divide(new BigDecimal(100), 2,
//                        BigDecimal.ROUND_HALF_UP);
//                // 计算分红
//                BigDecimal bonus = winAmount.multiply(bonusScale);
//                // 推单分红金额
//                BigDecimal momey = godPushOrder.getTatolAmount().add(bonus);
//                logger.info("winOrLose  推单业务逻辑算法计算完成=================================={}", momey);
//                godPushOrder.setTatolAmount(momey);
//                circleGodPushOrderMapper.updateByPrimaryKey(godPushOrder);
//                redisTemplate.delete(RedisKeys.GOD_PUSH_KEY + godOrderId);
//                redisTemplate.opsForValue().set(RedisKeys.GOD_PUSH_KEY + godOrderId, godPushOrder);
//                logger.info("winOrLose  推单业务账变记录=====================================");
//                /**
//                 * 给大神分红 1、扣除自己的余额、不可提现额度 2、给大神增加余额
//                 */
//                /** 1、扣除自己的余额及不可体现额度 **/
//                MemberBalanceChangeDTO changeDTO = new MemberBalanceChangeDTO();
//                changeDTO.setOrderBetId(orderBet.getId());
//                // 设置用户id
//                changeDTO.setUserId(userId);
//                // 设置备注
//                changeDTO.setRemark("跟单中奖，给大神分红/" + orderSn + "/" + orderBet.getId());
//                // 设置类型
//                changeDTO.setType(GoldchangeEnum.ORDER_FOLLOW_BONUS.getValue());
//                // 余额变动值
//                changeDTO.setChangeMoney(bonus.multiply(new BigDecimal(-1)));
//                // 计算不可提现金额变动值
//                changeDTO.setNoWithdrawalAmount(bonus.multiply(new BigDecimal(-1)));
//                changeDTO.setWaitAmount(new BigDecimal("0.0"));
//                // 修改用户余额信息
//                memBaseinfoWriteService.updateUserBalance(changeDTO);
//                logger.info("updateUserBalance time, {}", System.currentTimeMillis() - begin);
//
//                /** 2、给大神分红 **/
//                MemberBalanceChangeDTO godChange = new MemberBalanceChangeDTO();
//                changeDTO.setOrderBetId(orderBet.getId());
//                godChange.setUserId(godPushOrder.getUserId());
//                // 设置备注
//                godChange.setRemark("推单中奖，获得分红/" + orderSn + "/" + orderBet.getId());
//                // 设置类型
//                godChange.setType(GoldchangeEnum.ORDER_PUSH_BONUS.getValue());
//                // 余额变动值
//                godChange.setChangeMoney(bonus);
//                changeDTO.setWaitAmount(new BigDecimal("0.0"));
//                // 修改用户余额信息
//                memBaseinfoWriteService.updateUserBalance(godChange);
//                logger.info("updateUserBalance  给大神分红 time, {}", System.currentTimeMillis() - begin);
            }
        } catch (Exception e) {
            logger.error("winOrLose occur error. userId:{}; orderBetId:{}", userId, orderBet.getId(), e);
            throw new RuntimeException(e);
        } finally {
            lock.writeLock().unlock();
        }
    }


    @Override
    public void updateOrder(String number, List<OrderRecord> orderRecords, List<Integer> orderIds,
                            Map<Integer, OrderRecord> orderMap) {
        for (OrderRecord order : orderRecords) {
            Integer orderId = order.getId();
            orderMap.put(orderId, order);
            orderIds.add(orderId);
            // 获取开奖结果
            String openNumber = order.getOpenNumber();
            // 判断是否已开奖
            if (StringUtils.isNotBlank(openNumber) && number.equals(openNumber)) {
                continue;
            }
            order.setOpenNumber(number);
            orderRecordMapper.updateByPrimaryKeySelective(order);
        }
    }

//    /**
//     * 修改订单入库时未变更的额度
//     *
//     * @param orderBet
//     */
//    private void updateMemberBetAndNoWithdrawalAmount(OrderBetRecord orderBet) {
//        BigDecimal betCount = new BigDecimal(orderBet.getBetCount());
//        BigDecimal betAmount = betCount.multiply(orderBet.getBetAmount());
//        MemberBalanceChangeDTO processDTO = new MemberBalanceChangeDTO();
//        processDTO.setUserId(orderBet.getUserId());
//        processDTO.setType(BalanceChangeEnum.BET_ORDER.getValue());
//        BigDecimal noWithdrawalAmount = betAmount.multiply(new BigDecimal(-1).setScale(2, BigDecimal.ROUND_HALF_UP));
//        processDTO.setNoWithdrawalAmount(orderCommonService.calcNoWithdrawalAmount(noWithdrawalAmount));
//        processDTO.setBetAmount(betAmount);
//        processDTO.setChangeMoney(BigDecimal.ZERO);
//        appMemberWriteService.updateUserBalance(processDTO);
//    }

    @Override
    @Transactional
    public void noWinOrLose(Map<Integer, OrderRecord> orderMap, List<OrderBetRecord> orderBetRecords) {
        for (OrderBetRecord orderBet : orderBetRecords) {
            BigDecimal winAmount = getTradeOffAmount(orderBet.getBetAmount());
            // 设置中奖金额
            orderBet.setWinAmount(winAmount);
            // 设置状态
            orderBet.setTbStatus(OrderBetStatus.HE);
            orderBet.setWinCount("0");
            // 修改投注信息
            orderBetRecordMapper.updateByPrimaryKeySelective(orderBet);
            // 修改用户余额信息
            OrderRecord orderRecord = orderMap.get(orderBet.getOrderId());
            updateMemberBalance(orderBet, winAmount, orderBet.getUserId(), orderRecord.getOrderSn());
        }
    }

    @Override
    @Transactional
    public void noWinOrLose(List<OrderBetRecord> orderBetRecords) {
        for (OrderBetRecord orderBet : orderBetRecords) {
            BigDecimal winAmount = getTradeOffAmount(orderBet.getBetAmount());
            // 设置中奖金额
            orderBet.setWinAmount(winAmount);
            // 设置状态
            orderBet.setTbStatus(OrderBetStatus.HE);
            orderBet.setWinCount("0");
            // 修改投注信息
            orderBetRecordMapper.updateByPrimaryKeySelective((OrderBetRecord) orderBet);
            // 修改用户余额信息

            updateMemberBalance(orderBet, winAmount, orderBet.getUserId(), orderBet.getOrderSn());
        }
    }

    @Override
    public double getDivisor(Integer lotteryId) {
        // 获取彩种信息
        Lottery lottery = null;
        String key = LOTTERY_KEY + lotteryId;
        lottery = (Lottery) redisTemplate.opsForValue().get(key);
        if (lottery == null) {
            LotteryExample example = new LotteryExample();
            LotteryExample.Criteria criteria = example.createCriteria();
            criteria.andLotteryIdEqualTo(lotteryId);
            lottery = lotteryMapper.selectOneByExample(example);
            redisTemplate.opsForValue().set(key, lottery);
        }
        SysParameter memberOdds = (SysParameter) redisTemplate.opsForValue()
                .get(SysParameterEnum.REGISTER_MEMBER_ODDS.getCode());
        if (memberOdds == null) {
            memberOdds = sysParamService.getByCode(SysParameterEnum.REGISTER_MEMBER_ODDS);
            redisTemplate.opsForValue().set(SysParameterEnum.REGISTER_MEMBER_ODDS.getCode(), memberOdds);
        }
        // 获取赔率因子
        Double maxOdds = (null == lottery ? 0D : lottery.getMaxOdds());
        return maxOdds == 0D ? Double.parseDouble(memberOdds.getParamValue()) : maxOdds;
    }

    /**
     * 修改用户余额信息
     *
     * @param orderBet
     * @param winAmount
     * @param userId
     * @param orderSn
     */
    @Override
    @Transactional
    public void updateMemberBalance(OrderBetRecord orderBet, BigDecimal winAmount, Integer userId, String orderSn) {
        //ONELIVE TODO 更新用户余额
//        // 获取提现设置
//        long begin = System.currentTimeMillis();
//
//        SysParameter withdrawalSetting = (SysParameter) redisTemplate.opsForValue().get(WITHDRAWAL_KEY);
//        if (null == withdrawalSetting) {
//            withdrawalSetting = sysParamService.getByCode(SysParameterEnum.WITHDRAWAL_AMOUNT);
//            redisTemplate.opsForValue().set(WITHDRAWAL_KEY, withdrawalSetting, 5, TimeUnit.MINUTES);
//        }
//        String accountCache = this.getAccountCache(Long.valueOf(userId));
//        MemGoldchangeDO dto = new MemGoldchangeDO();
//        dto.setRefid(orderBet.getId().longValue());
//        dto.setUserId(userId);
//        dto.setAccno(accountCache);
//        dto.setAccount(accountCache);
//        dto.setSource(orderBet.getSource());
//        // 设置备注
//        dto.setOpnote("投注结算/" + orderSn + "/" + orderBet.getId());
//
//        if (orderBet.getRoomId() != null) {
//            dto.setChangetype(GoldchangeEnum.LIVEROOM_SETTLE.getValue());
//        } else {
//            // 设置类型
//            if (orderBet.getTbStatus().equals(OrderBetStatus.HE)) {
//                dto.setChangetype(GoldchangeEnum.BET_BALANCE.getValue());
//            } else if (orderBet.getTbStatus().equals(OrderBetStatus.NO_WIN)) {
//                dto.setChangetype(GoldchangeEnum.BET_ORDER_LOSS.getValue());
//            } else if (orderBet.getTbStatus().equals(OrderBetStatus.WIN)) {
//                dto.setChangetype(GoldchangeEnum.LOTTERY_PRIZE.getValue());
//            } else if (orderBet.getTbStatus().equals(OrderBetStatus.BACK)) {
//                dto.setChangetype(GoldchangeEnum.BET_ORDER_BAK.getValue());
//            }
//        }
//        // 余额变动值
//        dto.setQuantity(getTradeOffAmount(winAmount));
//        // 计算不可提现金额变动值
//        BigDecimal noWithdrawalAmount = getTradeOffAmount(orderBet.getBetAmount()
//                .multiply(new BigDecimal(-1)));
//        // double withdrawalSet = Double.parseDouble(withdrawalSetting.getInfo());
//        // BigDecimal noWithdrawalAmount = orderBet.getBetAmount().multiply(new
//        // BigDecimal(1 - withdrawalSet));
//        if (orderBet.getTbStatus() != null && orderBet.getTbStatus().equals(OrderBetStatus.BACK)) {
//            dto.setNoWithdrawalAmount(getTradeOffAmount(null));
//            dto.setBetAmount(getTradeOffAmount(null));
//        } else {
//            dto.setNoWithdrawalAmount(getTradeOffAmount(noWithdrawalAmount));
//            dto.setBetAmount(getTradeOffAmount(orderBet.getBetAmount()));
//        }
//        //来源
//        dto.setSource(orderBet.getSource());
//        // 修改用户余额信息
//        logger.info("winOrLose  订单号{}setNoWithdrawalAmount 耗时 time, {}", orderSn, System.currentTimeMillis() - begin);
//        begin = System.currentTimeMillis();
//        dto.setWaitAmount(getTradeOffAmount(orderBet.getBetAmount().multiply(new BigDecimal("-1"))));
//        memBaseinfoWriteService.updateUserBalance(dto);
//        logger.info("winOrLose  订单号{}appMemberWriteService.updateUserBalance(dto); 耗时 time, {}", orderSn,
//                System.currentTimeMillis() - begin);
    }


    /**
     * 公共推送中奖信息
     *
     * @param gid
     * @param content
     * @return
     */
    public boolean commonSendOut(Integer gid, JSONObject content) {
        try {
            Map<String, String> map = new HashMap<String, String>();
            map.put("gid", gid + "");
            map.put("data", content.toString());
            map.put("single", chatSign);
            Map<String, String> headMap = new HashMap<String, String>();
            headMap.put("AuthGC", chatAuthGC);
            String result = HttpClientUtil.formPost(chatHost + "/interactive/chat/winorderSendout", map, headMap);
            JSONObject jsonObject1 = JSONObject.parseObject(result);
            logger.info("chatPushSendOut 业务远程请求返回[{}]=", result);
            if (Constants.RETURN_KEY.equals(jsonObject1.getString("code"))) {
                return true;
            }
        } catch (Exception e) {
            logger.info("commonSendOut error", e);
            return false;
        }
        return false;
    }

//    public String getAccountCache(Long userId) {
//
//        if (userId == null) {
//            return "";
//        }
//        String key = Constants.MEMBER_ACCOUNT + userId;
//        String account = (String) redisTemplate.opsForValue().get(key);
//        if (StringUtils.isEmpty(account)) {
//            account = memBaseinfoService.selectAccountbyId(userId);
//            redisTemplate.opsForValue().set(key, account);
//        }
//        return account;
//    }
}
