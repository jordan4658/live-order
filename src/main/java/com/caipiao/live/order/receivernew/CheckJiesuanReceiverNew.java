package com.caipiao.live.order.receivernew;
import com.caipiao.live.order.config.ActiveMQConfig;
import com.caipiao.live.order.receiver.CheckJiesuanReceiver;
import com.caipiao.live.order.service.bet.BetDzpceggService;
import com.caipiao.live.order.service.bet.BetDzxyftService;
import com.caipiao.live.order.service.bet.BetKsService;
import com.caipiao.live.order.service.bet.BetNewLhcService;
import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.enums.lottery.CaipiaoTypeEnum;
import com.caipiao.live.common.mybatis.entity.*;
import com.caipiao.live.common.mybatis.mapper.*;
import com.caipiao.live.common.util.StringUtils;
import com.caipiao.live.common.util.redis.BasicRedisClient;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @Date:Created in 11:252020/3/5
 * @Descriotion
 * @Author
 **/
@Component
public class CheckJiesuanReceiverNew {
    private static final Logger logger = LoggerFactory.getLogger(CheckJiesuanReceiver.class);
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private BasicRedisClient basicRedisClient;
    @Autowired
    private OrderBetRecordMapper orderBetRecordMapper;
    @Autowired
    private OrderRecordMapper orderRecordMapper;
    @Autowired
    private BetNewLhcService betNewLhcService;
    @Autowired
    private BetKsService betKsService;
    @Autowired
    private BetDzxyftService betDzxyftService;
    @Autowired
    private BetDzpceggService betDzpceggService;
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

    /**
     * 结算检查
     *
     * @param message 消息内容【期号】
     */
//    @RabbitListener(queues = RabbitConfig.QUEUE_AUS_ACT)
    @JmsListener(destination = ActiveMQConfig.TOPIC_CHECK_ORDER, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgAzAct(String message) throws Exception {
        logger.info("检查结算开始  mq：{},{}" ,ActiveMQConfig.TOPIC_CHECK_ORDER, message);
        // 获取一个时间戳
        String time = message;

        // 获取唯一
        String key = ActiveMQConfig.TOPIC_CHECK_ORDER + "NEW" + time;
        RReadWriteLock lock = redissonClient.getReadWriteLock("newLotteryjiesuan" + time);
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 60, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 50L);
                    logger.info("结算检查新彩种获取锁");

                    OrderBetRecordExample orderBetExample = new OrderBetRecordExample();
                    OrderBetRecordExample.Criteria orderBetCriteria = orderBetExample.createCriteria();
                    orderBetCriteria.andTbStatusEqualTo(Constants.ORDER_WAIT);
                    //查询最近5天的数据
                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.DAY_OF_MONTH, -30);
                    orderBetCriteria.andCreateTimeGreaterThanOrEqualTo(calendar.getTime());
                    calendar = Calendar.getInstance();
                    calendar.add(Calendar.MINUTE, -2);
                    orderBetCriteria.andCreateTimeLessThanOrEqualTo(calendar.getTime());
//                    orderBetCriteria.andKjStatusIsNotNull();
                    orderBetCriteria.andLotteryIdNotEqualTo(Integer.valueOf(CaipiaoTypeEnum.LHC.getTagType()));

                    orderBetExample.setLimit(1000);
                    orderBetExample.setOffset(0);
                    orderBetExample.setOrderByClause("create_time desc");
                    List<OrderBetRecord> notLhcOrderBets = this.orderBetRecordMapper.selectByExample(orderBetExample);
                    Set<String> notLhcset = new HashSet<String>();  //得到不重复的期号数据
                    for (OrderBetRecord lhcRecord : notLhcOrderBets) {
                        OrderRecord orderRecord = orderRecordMapper.selectByPrimaryKey(lhcRecord.getOrderId());
                        if (orderRecord == null) {
                            continue;
                        }
                        //彩种 期号
                        notLhcset.add(orderRecord.getLotteryId() + ";" + orderRecord.getIssue());
                    }

                    for (String single : notLhcset) {  //当开奖时间 要大于开奖时间后5分钟再检查
                        OrderRecord orderRecord = null;
                        logger.info("非六合彩single信息：{}", single);
                        try {
                            String array[] = single.split(";");

                            OrderRecordExample orderRecordExample = new OrderRecordExample();
                            OrderRecordExample.Criteria orderRecordCriteria = orderRecordExample.createCriteria();
                            orderRecordCriteria.andLotteryIdEqualTo(Integer.valueOf(array[0]));
                            orderRecordCriteria.andIssueEqualTo(array[1]);
                            orderRecord = orderRecordMapper.selectOneByExample(orderRecordExample);
                            // 德州快三
                            if (orderRecord.getLotteryId().toString().equals(CaipiaoTypeEnum.DZKS.getTagType())) {

                                DzksLotterySgExample dzksLotterySgExample = new DzksLotterySgExample();
                                DzksLotterySgExample.Criteria criteria = dzksLotterySgExample.createCriteria();

                                criteria.andIssueEqualTo(orderRecord.getIssue());
                                DzksLotterySg dzksLotterySg = dzksLotterySgMapper.selectOneByExample(dzksLotterySgExample);

                                if (dzksLotterySg == null || (StringUtils.isBlank(dzksLotterySg.getNumber()))
                                        || System.currentTimeMillis() - dzksLotterySg.getIdealTime().getTime() < 2 * 60000) {
                                    continue;
                                }

                                // 结算【德州快三 两面】
                                betKsService.clearingKsLm(orderRecord.getIssue(), dzksLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.DZKS.getTagType()));
                                //结算【独胆】
                                betKsService.clearingKsDd(orderRecord.getIssue(), dzksLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.DZKS.getTagType()));
                                //结算【二连号 三连号】
                                betKsService.clearingKsLh(orderRecord.getIssue(), dzksLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.DZKS.getTagType()));
                                // 结算【三不同号、胆拖 三同号单选，三同号通选】
                                betKsService.clearingKsSbTh(orderRecord.getIssue(), dzksLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.DZKS.getTagType()));
                                // 结算【二不同号、胆拖 二同号单选，二同号通选】
                                betKsService.clearingKsEbTh(orderRecord.getIssue(), dzksLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.DZKS.getTagType()));


                            } else if (orderRecord.getLotteryId().toString().equals(CaipiaoTypeEnum.AZKS.getTagType())) {
                                //澳洲快三
                                AzksLotterySgExample azksLotterySgExample = new AzksLotterySgExample();
                                AzksLotterySgExample.Criteria criteria = azksLotterySgExample.createCriteria();
                                criteria.andIssueEqualTo(orderRecord.getIssue());
                                AzksLotterySg azksLotterySg = azksLotterySgMapper.selectOneByExample(azksLotterySgExample);

                                if (azksLotterySg == null || (StringUtils.isBlank(azksLotterySg.getNumber()))
                                        || System.currentTimeMillis() - azksLotterySg.getIdealTime().getTime() < 5 * 60000) {
                                    continue;
                                }

                                // 结算【澳州快三 两面】
                                betKsService.clearingKsLm(orderRecord.getIssue(), azksLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.AZKS.getTagType()));
                                //结算【独胆】
                                betKsService.clearingKsDd(orderRecord.getIssue(), azksLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.AZKS.getTagType()));
                                //结算【二连号 三连号】
                                betKsService.clearingKsLh(orderRecord.getIssue(), azksLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.AZKS.getTagType()));
                                // 结算【三不同号、胆拖 三同号单选，三同号通选】
                                betKsService.clearingKsSbTh(orderRecord.getIssue(), azksLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.AZKS.getTagType()));
                                // 结算【二不同号、胆拖 二同号单选，二同号通选】
                                betKsService.clearingKsEbTh(orderRecord.getIssue(), azksLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.AZKS.getTagType()));

                            } else if (orderRecord.getLotteryId().toString().equals(CaipiaoTypeEnum.DZXYFEIT.getTagType())) {

                                //德州飞艇
                                DzxyftLotterySgExample dzxyftLotterySgExample = new DzxyftLotterySgExample();
                                DzxyftLotterySgExample.Criteria criteria = dzxyftLotterySgExample.createCriteria();
                                criteria.andIssueEqualTo(orderRecord.getIssue());
                                DzxyftLotterySg dzxyftLotterySg = dzxyftLotterySgMapper.selectOneByExample(dzxyftLotterySgExample);
                                if (dzxyftLotterySg == null || (StringUtils.isBlank(dzxyftLotterySg.getNumber()))
                                        || System.currentTimeMillis() - dzxyftLotterySg.getIdealTime().getTime() < 2 * 60000) {
                                    continue;
                                }
                                // 结算【德州幸运飞艇-两面】
                                betDzxyftService.clearingDzxyftLm(orderRecord.getIssue(), dzxyftLotterySg.getNumber());
                                // 结算【德州幸运飞艇-猜名次猜前几】
                                betDzxyftService.clearingDzxyftCmcCqj(orderRecord.getIssue(), dzxyftLotterySg.getNumber());
                                // 结算【德州幸运飞艇-冠亚和】
                                betDzxyftService.clearingDzxyftGyh(orderRecord.getIssue(), dzxyftLotterySg.getNumber());

                            } else if (orderRecord.getLotteryId().toString().equals(CaipiaoTypeEnum.DZPCDAND.getTagType())) {
                                //德州蛋蛋
                                DzpceggLotterySgExample dzpceggLotterySgExample = new DzpceggLotterySgExample();
                                DzpceggLotterySgExample.Criteria criteria = dzpceggLotterySgExample.createCriteria();
                                criteria.andIssueEqualTo(orderRecord.getIssue());
                                DzpceggLotterySg dzpceggLotterySg = dzpceggLotterySgMapper.selectOneByExample(dzpceggLotterySgExample);
                                if (dzpceggLotterySg == null || (StringUtils.isBlank(dzpceggLotterySg.getNumber()))
                                        || System.currentTimeMillis() - dzpceggLotterySg.getIdealTime().getTime() < 2 * 60000) {
                                    continue;
                                }

                                // 结算【德州PPC蛋蛋-特码】
                                betDzpceggService.clearingDzpceggTm(orderRecord.getIssue(), dzpceggLotterySg.getNumber());
                                // 结算【德州PPC蛋蛋-豹子】
                                betDzpceggService.clearingDzpceggBz(orderRecord.getIssue(), dzpceggLotterySg.getNumber());
                                // 结算【德州PPC蛋蛋-特码包三】
                                betDzpceggService.clearingDzpceggTmbs(orderRecord.getIssue(), dzpceggLotterySg.getNumber());
                                // 结算【德州PPC蛋蛋-色波】
                                betDzpceggService.clearingDzpceggSb(orderRecord.getIssue(), dzpceggLotterySg.getNumber());
                                // 结算【德州PPC蛋蛋-混合】
                                betDzpceggService.clearingDzpceggHh(orderRecord.getIssue(), dzpceggLotterySg.getNumber());

                            } else if (orderRecord.getLotteryId().toString().equals(CaipiaoTypeEnum.XJPLHC.getTagType())) {
                                //新加坡六合彩
                                XjplhcLotterySgExample xjplhcLotterySgExample = new XjplhcLotterySgExample();
                                XjplhcLotterySgExample.Criteria criteria = xjplhcLotterySgExample.createCriteria();
                                criteria.andIssueEqualTo(orderRecord.getIssue());
                                XjplhcLotterySg xjplhcLotterySg = xjplhcLotterySgMapper.selectOneByExample(xjplhcLotterySgExample);

                                if (xjplhcLotterySg == null || (StringUtils.isBlank(xjplhcLotterySg.getNumber()))
                                        || System.currentTimeMillis() - xjplhcLotterySg.getIdealTime().getTime() < 5 * 60000) {
                                    continue;
                                }


                                // 结算新加坡六合彩- 【特码,正特,六肖,正码1-6】
                                betNewLhcService.clearingLhcTeMaA(orderRecord.getIssue(), xjplhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                                betNewLhcService.clearingLhcZhengTe(orderRecord.getIssue(), xjplhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                                betNewLhcService.clearingLhcZhengMaOneToSix(orderRecord.getIssue(), xjplhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                                betNewLhcService.clearingLhcLiuXiao(orderRecord.getIssue(), xjplhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);

                                // 新加坡结算六合彩- 【正码,半波,尾数】
                                betNewLhcService.clearingLhcZhengMaA(orderRecord.getIssue(), xjplhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                                betNewLhcService.clearingLhcBanBo(orderRecord.getIssue(), xjplhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                                betNewLhcService.clearingLhcWs(orderRecord.getIssue(), xjplhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);

                                // 新加坡结算六合彩- 【连码,连肖,连尾】
                                betNewLhcService.clearingLhcLianMa(orderRecord.getIssue(), xjplhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                                betNewLhcService.clearingLhcLianXiao(orderRecord.getIssue(), xjplhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                                betNewLhcService.clearingLhcLianWei(orderRecord.getIssue(), xjplhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);

                                // 新加坡结算六合彩- 【不中,1-6龙虎,五行】
                                betNewLhcService.clearingLhcNoOpen(orderRecord.getIssue(), xjplhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                                betNewLhcService.clearingLhcOneSixLh(orderRecord.getIssue(), xjplhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                                betNewLhcService.clearingLhcWuxing(orderRecord.getIssue(), xjplhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);

                                // 新加坡结算六合彩- 【平特,特肖】
                                betNewLhcService.clearingLhcPtPt(orderRecord.getIssue(), xjplhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);
                                betNewLhcService.clearingLhcTxTx(orderRecord.getIssue(), xjplhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.XJPLHC.getTagType()), true);

                            }

                        } catch (Exception e) {
                            logger.error("重新结算出错，lotteryId:{},issue:{},number:{}", orderRecord.getLotteryId(), orderRecord.getIssue(), orderRecord.getOpenNumber(), e);
                        }

                    }
                }
            }
        } catch (Exception e) {
            logger.error("重新结算出错", e);
        } finally {
            lock.writeLock().unlock();
        }

    }
}
