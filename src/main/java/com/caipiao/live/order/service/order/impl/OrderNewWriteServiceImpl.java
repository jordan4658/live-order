package com.caipiao.live.order.service.order.impl;

import com.caipiao.live.order.service.bet.BetDzpceggService;
import com.caipiao.live.order.service.bet.BetDzxyftService;
import com.caipiao.live.order.service.bet.BetKsService;
import com.caipiao.live.order.service.bet.BetNewLhcService;
import com.caipiao.live.order.service.bet.impl.BetNewLhcServiceImpl;
import com.caipiao.live.order.service.order.OrderNewAppendWriteService;
import com.caipiao.live.order.service.order.OrderNewWriteService;
import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.constant.RedisKeys;
import com.caipiao.live.common.enums.lottery.CaipiaoTypeEnum;
import com.caipiao.live.common.model.common.ResultInfo;
import com.caipiao.live.common.mybatis.entity.OrderAppendRecord;
import com.caipiao.live.common.mybatis.entity.OrderBetRecord;
import com.caipiao.live.common.mybatis.entity.OrderRecord;
import com.caipiao.live.common.mybatis.mapper.OrderBetRecordMapper;
import com.caipiao.live.common.mybatis.mapper.OrderRecordMapper;
import com.caipiao.live.common.mybatis.mapperbean.OrderMapper;
import com.caipiao.live.common.mybatis.mapperext.order.OrderMapperExt;
import com.caipiao.live.common.util.SnowflakeIdWorker;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class OrderNewWriteServiceImpl implements OrderNewWriteService {

    private static final Logger logger = LoggerFactory.getLogger(OrderNewWriteServiceImpl.class);
    @Autowired
    private BetKsService betksService;
    @Autowired
    @Lazy
    private BetNewLhcService betNewLhcService;
    @Autowired
    private BetDzpceggService betDzpceggService;
    @Autowired
    private BetDzxyftService betDzxyftService;
    @Autowired
    private OrderNewAppendWriteService orderNewAppendWriteService;
    @Autowired
    private OrderRecordMapper orderRecordMapper;
    @Autowired
    private OrderBetRecordMapper orderBetRecordMapper;
    @Autowired
    private OrderMapperExt orderMapperExt;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private RedissonClient redissonClient;

    @Override
    public ResultInfo<Boolean> jiesuanOrderBetByIssue(Integer lotteryId, String issue, String number) {
        try {
            if (lotteryId.toString().equals(CaipiaoTypeEnum.AZKS.getTagType())) {
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
            logger.error("新彩种LotteryId{}, 期号{}结算失败 ", lotteryId, issue,e);
            return ResultInfo.error("结算失败!");
        }
    }

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
        String issue = orderNewAppendWriteService.createNextIssue(lotteryId, firstIssue, orderAppend.getAppendedCount());

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


    @Override
    public int countOrderBetList(String issue, List<Integer> playIds, String lotteryId, String status) {
        if (CollectionUtils.isEmpty(playIds)) {
            return 0;
        }
        return orderMapperExt.countOrderBetList(issue, playIds, lotteryId, status);
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
                    OrderBetRecord o = list.get(list.size() - 1);
//					int maxid = o.getId();
//					redisTemplate.opsForValue().set(key, maxid, 10, TimeUnit.MINUTES);
                    // 玩法设计...共用了一个playID....
                    if (String.valueOf(playIds.get(0)).replace(lotteryId, "")
                            .equals(BetNewLhcServiceImpl.PLAY_ID_ZM_ZMA)) {

                        if ("OnePlayMany".equals(type)) {
                            for (OrderBetRecord clearo : list) {
                                //   if (clearo.getBetNumber().contains("两面")) 增加了playname字段
                                if (clearo.getPlayName().contains("两面")) {
                                    clearlist.add(clearo);
                                }
                            }
                        }
                        if ("OnePlay".equals(type)) {
                            for (OrderBetRecord clearo : list) {
                                if (!clearo.getPlayName().contains("两面")) {
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
        return orderMapper.updateOrderRecord(lotteryId, issue, sgnumber);
    }

}

