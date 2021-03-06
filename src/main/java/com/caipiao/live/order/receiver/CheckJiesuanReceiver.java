package com.caipiao.live.order.receiver;

import com.caipiao.live.order.config.ActiveMQConfig;
import com.caipiao.live.order.service.bet.*;
import com.caipiao.live.order.service.bet.*;
import com.caipiao.live.order.service.result.AzxlLotterySgWriteService;
import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.enums.lottery.CaipiaoTypeEnum;
import com.caipiao.live.common.mybatis.entity.*;
import com.caipiao.live.common.mybatis.mapper.*;
import com.caipiao.live.common.util.DateUtils;
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
 * @author lzy
 * @create 2018-09-17 20:07
 **/
@Component
public class CheckJiesuanReceiver {
    private static final Logger logger = LoggerFactory.getLogger(CheckJiesuanReceiver.class);

    @Autowired
    private AusactLotterySgMapper ausactLotterySgMapper;
    @Autowired
    private AussscLotterySgMapper aussscLotterySgMapper;
    @Autowired
    private AzxlLotterySgWriteService cptAzxlLotterySgWriteService;
    @Autowired
    private BetActAzService betActAzService;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private BasicRedisClient basicRedisClient;
    @Autowired
    private OrderBetRecordMapper orderBetRecordMapper;
    @Autowired
    private OrderRecordMapper orderRecordMapper;
    @Autowired
    private BetSscbmService betSscbmService;
    @Autowired
    private BetLhcService betLhcService;
    @Autowired
    private BetBjpksService betBjpksService;
    @Autowired
    private BetF1AzService betF1AzService;
    //    @Autowired
//    private BetNnAzService betNnAzService;
    @Autowired
    private BetXyftService betXyftService;
    @Autowired
    private BetPceggService betPceggService;
    //    @Autowired
//    private BetTcDltService betTcDltService;
//    @Autowired
//    private BetTcPlswService betTcPlswService;
//    @Autowired
//    private Bet7xcHnService bet7xcHnService;
//    @Autowired
//    private BetFcSsqService betFcSsqService;
//    @Autowired
//    private Bet3dFcService bet3dFcService;
//    @Autowired
//    private BetFc7lcService betFc7lcService;
//    @Autowired
//    private BetNnKlService betNnKlService;
//    @Autowired
//    private BetNnJsService betNnJsService;
//    @Autowired
//    private BetFtJspksService betFtJspksService;
//    @Autowired
//    private BetFtXyftService betFtXyftService;
//    @Autowired
//    private BetFtSscService betFtSscService;
    @Autowired
    private BetSscAzService betSscAzService;
    @Autowired
    private CqsscLotterySgMapper cqsscLotterySgMapper;
    @Autowired
    private XjsscLotterySgMapper xjsscLotterySgMapper;
    @Autowired
    private TjsscLotterySgMapper tjsscLotterySgMapper;
    @Autowired
    private TensscLotterySgMapper tensscLotterySgMapper;
    @Autowired
    private FivesscLotterySgMapper fivesscLotterySgMapper;
    @Autowired
    private JssscLotterySgMapper jssscLotterySgMapper;
    @Autowired
    private OnelhcLotterySgMapper onelhcLotterySgMapper;
    @Autowired
    private FivelhcLotterySgMapper fivelhcLotterySgMapper;
    @Autowired
    private AmlhcLotterySgMapper amlhcLotterySgMapper;
    @Autowired
    private BjpksLotterySgMapper bjpksLotterySgMapper;
    @Autowired
    private TenbjpksLotterySgMapper tenbjpksLotterySgMapper;
    @Autowired
    private FivebjpksLotterySgMapper fivebjpksLotterySgMapper;
    @Autowired
    private JsbjpksLotterySgMapper jsbjpksLotterySgMapper;
    @Autowired
    private XyftLotterySgMapper xyftLotterySgMapper;
    @Autowired
    private PceggLotterySgMapper pceggLotterySgMapper;
    @Autowired
    private TxffcLotterySgMapper txffcLotterySgMapper;
    @Autowired
    private TcdltLotterySgMapper tcdltLotterySgMapper;
    @Autowired
    private TcplwLotterySgMapper tcplwLotterySgMapper;
    @Autowired
    private Tc7xcLotterySgMapper tc7xcLotterySgMapper;
    @Autowired
    private FcssqLotterySgMapper fcssqLotterySgMapper;
    @Autowired
    private Fc3dLotterySgMapper fc3dLotterySgMapper;
    @Autowired
    private Fc7lcLotterySgMapper fc7lcLotterySgMapper;
    @Autowired
    private AuspksLotterySgMapper auspksLotterySgMapper;
    @Autowired
    private FtxyftLotterySgMapper ftxyftLotterySgMapper;
    @Autowired
    private FtjspksLotterySgMapper ftjspksLotterySgMapper;


    /**
     * ????????????
     *
     * @param message ????????????????????????
     */
//    @RabbitListener(queues = RabbitConfig.QUEUE_AUS_ACT)
    @JmsListener(destination = ActiveMQConfig.TOPIC_CHECK_ORDER, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgAzAct(String message) throws Exception {
        logger.info("??????????????????  " + message);
        // ?????????????????????
        String time = message;

        // ????????????
        String key = ActiveMQConfig.TOPIC_CHECK_ORDER + time;
        RReadWriteLock lock = redissonClient.getReadWriteLock("jiesuan" + time);
        try {
            // ?????????????????????1s???????????????10S[????????????]????????????????????????????????????????????????????????????????????????????????????
            boolean bool = lock.writeLock().tryLock(3, 60, TimeUnit.SECONDS);
            if (bool) {
                if (basicRedisClient.get(key) == null) {
                    basicRedisClient.set(key, "1", 60l);
                    //????????????
                    OrderBetRecordExample orderBetExample = new OrderBetRecordExample();
                    OrderBetRecordExample.Criteria orderBetCriteria = orderBetExample.createCriteria();
                    orderBetCriteria.andTbStatusEqualTo(Constants.ORDER_WAIT);
                    //????????????5????????????
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
                    Set<String> notLhcset = new HashSet<String>();  //??????????????????????????????
                    for (OrderBetRecord lhcRecord : notLhcOrderBets) {
                        OrderRecord orderRecord = orderRecordMapper.selectByPrimaryKey(lhcRecord.getOrderId());
                        if (orderRecord == null) {
                            continue;
                        }
                        notLhcset.add(orderRecord.getLotteryId() + ";" + orderRecord.getIssue());
                    }

                    for (String single : notLhcset) {  //??????????????? ????????????????????????5???????????????
                        OrderRecord orderRecord = null;
                        logger.info("????????????single?????????{}", single);
                        try {
                            String array[] = single.split(";");
//                            if(!array[1].equals("20190615231")){
//                                continue;
//                            }

                            OrderRecordExample orderRecordExample = new OrderRecordExample();
                            OrderRecordExample.Criteria orderRecordCriteria = orderRecordExample.createCriteria();
                            orderRecordCriteria.andLotteryIdEqualTo(Integer.valueOf(array[0]));
                            orderRecordCriteria.andIssueEqualTo(array[1]);
                            orderRecord = orderRecordMapper.selectOneByExample(orderRecordExample);

                            if (orderRecord.getLotteryId().toString().equals(CaipiaoTypeEnum.CQSSC.getTagType())) {
                                CqsscLotterySgExample cqsscLotterySgExample = new CqsscLotterySgExample();
                                CqsscLotterySgExample.Criteria criteria = cqsscLotterySgExample.createCriteria();
                                criteria.andIssueEqualTo(orderRecord.getIssue());
                                CqsscLotterySg cqsscLotterySg = cqsscLotterySgMapper.selectOneByExample(cqsscLotterySgExample);
                                if (cqsscLotterySg == null || (StringUtils.isBlank(cqsscLotterySg.getCpkNumber()) && StringUtils.isBlank(cqsscLotterySg.getKcwNumber()))
                                        || System.currentTimeMillis() - DateUtils.parseDate(cqsscLotterySg.getIdealTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS).getTime() < 5 * 60000) {
                                    continue;
                                }
                                // ??????????????????
                                betSscbmService.countlm(orderRecord.getIssue(), StringUtils.isNotEmpty(cqsscLotterySg.getCpkNumber()) ? cqsscLotterySg.getCpkNumber() : cqsscLotterySg.getKcwNumber(), Integer.parseInt(CaipiaoTypeEnum.CQSSC.getTagType()));
                                // ????????????????????????
                                betSscbmService.countdn(orderRecord.getIssue(), StringUtils.isNotEmpty(cqsscLotterySg.getCpkNumber()) ? cqsscLotterySg.getCpkNumber() : cqsscLotterySg.getKcwNumber(), Integer.parseInt(CaipiaoTypeEnum.CQSSC.getTagType()));
                                // ?????????????????????
                                betSscbmService.count15(orderRecord.getIssue(), StringUtils.isNotEmpty(cqsscLotterySg.getCpkNumber()) ? cqsscLotterySg.getCpkNumber() : cqsscLotterySg.getKcwNumber(), Integer.parseInt(CaipiaoTypeEnum.CQSSC.getTagType()));
                                // ??????????????????????????????
                                betSscbmService.countqzh(orderRecord.getIssue(), StringUtils.isNotEmpty(cqsscLotterySg.getCpkNumber()) ? cqsscLotterySg.getCpkNumber() : cqsscLotterySg.getKcwNumber(), Integer.parseInt(CaipiaoTypeEnum.CQSSC.getTagType()));
                            } else if (orderRecord.getLotteryId().toString().equals(CaipiaoTypeEnum.XJSSC.getTagType())) {
                                XjsscLotterySgExample xjsscLotterySgExample = new XjsscLotterySgExample();
                                XjsscLotterySgExample.Criteria criteria = xjsscLotterySgExample.createCriteria();
                                criteria.andIssueEqualTo(orderRecord.getIssue());
                                XjsscLotterySg xjsscLotterySg = xjsscLotterySgMapper.selectOneByExample(xjsscLotterySgExample);
                                if (xjsscLotterySg == null || (StringUtils.isBlank(xjsscLotterySg.getCpkNumber()) && StringUtils.isBlank(xjsscLotterySg.getKcwNumber()))
                                        || System.currentTimeMillis() - DateUtils.parseDate(xjsscLotterySg.getIdealTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS).getTime() < 5 * 60000) {
                                    continue;
                                }

                                // ??????????????????
                                betSscbmService.countlm(orderRecord.getIssue(), StringUtils.isNotEmpty(xjsscLotterySg.getCpkNumber()) ? xjsscLotterySg.getCpkNumber() : xjsscLotterySg.getKcwNumber(), Integer.parseInt(CaipiaoTypeEnum.XJSSC.getTagType()));
                                // ????????????????????????
                                betSscbmService.countdn(orderRecord.getIssue(), StringUtils.isNotEmpty(xjsscLotterySg.getCpkNumber()) ? xjsscLotterySg.getCpkNumber() : xjsscLotterySg.getKcwNumber(), Integer.parseInt(CaipiaoTypeEnum.XJSSC.getTagType()));
                                // ?????????????????????
                                betSscbmService.count15(orderRecord.getIssue(), StringUtils.isNotEmpty(xjsscLotterySg.getCpkNumber()) ? xjsscLotterySg.getCpkNumber() : xjsscLotterySg.getKcwNumber(), Integer.parseInt(CaipiaoTypeEnum.XJSSC.getTagType()));
                                // ??????????????????????????????
                                betSscbmService.countqzh(orderRecord.getIssue(), StringUtils.isNotEmpty(xjsscLotterySg.getCpkNumber()) ? xjsscLotterySg.getCpkNumber() : xjsscLotterySg.getKcwNumber(), Integer.parseInt(CaipiaoTypeEnum.XJSSC.getTagType()));
                            } else if (orderRecord.getLotteryId().toString().equals(CaipiaoTypeEnum.TJSSC.getTagType())) {
                                TjsscLotterySgExample tjsscLotterySgExample = new TjsscLotterySgExample();
                                TjsscLotterySgExample.Criteria criteria = tjsscLotterySgExample.createCriteria();
                                criteria.andIssueEqualTo(orderRecord.getIssue());
                                TjsscLotterySg tjsscLotterySg = tjsscLotterySgMapper.selectOneByExample(tjsscLotterySgExample);
                                if (tjsscLotterySg == null || (StringUtils.isBlank(tjsscLotterySg.getCpkNumber()) && StringUtils.isBlank(tjsscLotterySg.getKcwNumber()))
                                        || System.currentTimeMillis() - DateUtils.parseDate(tjsscLotterySg.getIdealTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS).getTime() < 5 * 60000) {
                                    continue;
                                }

                                // ??????????????????
                                betSscbmService.countlm(orderRecord.getIssue(), StringUtils.isNotEmpty(tjsscLotterySg.getCpkNumber()) ? tjsscLotterySg.getCpkNumber() : tjsscLotterySg.getKcwNumber(), Integer.parseInt(CaipiaoTypeEnum.TJSSC.getTagType()));
                                // ????????????????????????
                                betSscbmService.countdn(orderRecord.getIssue(), StringUtils.isNotEmpty(tjsscLotterySg.getCpkNumber()) ? tjsscLotterySg.getCpkNumber() : tjsscLotterySg.getKcwNumber(), Integer.parseInt(CaipiaoTypeEnum.TJSSC.getTagType()));
                                // ?????????????????????
                                betSscbmService.count15(orderRecord.getIssue(), StringUtils.isNotEmpty(tjsscLotterySg.getCpkNumber()) ? tjsscLotterySg.getCpkNumber() : tjsscLotterySg.getKcwNumber(), Integer.parseInt(CaipiaoTypeEnum.TJSSC.getTagType()));
                                // ??????????????????????????????
                                betSscbmService.countqzh(orderRecord.getIssue(), StringUtils.isNotEmpty(tjsscLotterySg.getCpkNumber()) ? tjsscLotterySg.getCpkNumber() : tjsscLotterySg.getKcwNumber(), Integer.parseInt(CaipiaoTypeEnum.TJSSC.getTagType()));
                            } else if (orderRecord.getLotteryId().toString().equals(CaipiaoTypeEnum.TENSSC.getTagType())) {
                                TensscLotterySgExample tensscLotterySgExample = new TensscLotterySgExample();
                                TensscLotterySgExample.Criteria criteria = tensscLotterySgExample.createCriteria();
                                criteria.andIssueEqualTo(orderRecord.getIssue());
                                TensscLotterySg tensscLotterySg = tensscLotterySgMapper.selectOneByExample(tensscLotterySgExample);
                                if (tensscLotterySg == null || StringUtils.isBlank(tensscLotterySg.getNumber())
                                        || System.currentTimeMillis() - DateUtils.parseDate(tensscLotterySg.getIdealTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS).getTime() < 1 * 60000) {
                                    continue;
                                }

                                // ??????????????????
                                betSscbmService.countlm(orderRecord.getIssue(), tensscLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.TENSSC.getTagType()));
                                // ????????????????????????
                                betSscbmService.countdn(orderRecord.getIssue(), tensscLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.TENSSC.getTagType()));
                                // ?????????????????????
                                betSscbmService.count15(orderRecord.getIssue(), tensscLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.TENSSC.getTagType()));
                                // ??????????????????????????????
                                betSscbmService.countqzh(orderRecord.getIssue(), tensscLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.TENSSC.getTagType()));
                            } else if (orderRecord.getLotteryId().toString().equals(CaipiaoTypeEnum.FIVESSC.getTagType())) {  // ???????????????????????? TOPIC_KLNN_FIVE
                                FivesscLotterySgExample fivesscLotterySgExample = new FivesscLotterySgExample();
                                FivesscLotterySgExample.Criteria criteria = fivesscLotterySgExample.createCriteria();
                                criteria.andIssueEqualTo(orderRecord.getIssue());
                                FivesscLotterySg fivesscLotterySg = fivesscLotterySgMapper.selectOneByExample(fivesscLotterySgExample);
                                if (fivesscLotterySg == null || StringUtils.isBlank(fivesscLotterySg.getNumber())
                                        || System.currentTimeMillis() - DateUtils.parseDate(fivesscLotterySg.getIdealTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS).getTime() < 1 * 60000) {
                                    continue;
                                }

                                // ??????????????????
                                betSscbmService.countlm(orderRecord.getIssue(), fivesscLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.FIVESSC.getTagType()));
                                // ????????????????????????
                                betSscbmService.countdn(orderRecord.getIssue(), fivesscLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.FIVESSC.getTagType()));
                                // ?????????????????????
                                betSscbmService.count15(orderRecord.getIssue(), fivesscLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.FIVESSC.getTagType()));
                                // ??????????????????????????????
                                betSscbmService.countqzh(orderRecord.getIssue(), fivesscLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.FIVESSC.getTagType()));

                            } else if (orderRecord.getLotteryId().toString().equals(CaipiaoTypeEnum.JSSSC.getTagType())) {
                                JssscLotterySgExample jssscLotterySgExample = new JssscLotterySgExample();
                                JssscLotterySgExample.Criteria criteria = jssscLotterySgExample.createCriteria();
                                criteria.andIssueEqualTo(orderRecord.getIssue());
                                JssscLotterySg jssscLotterySg = jssscLotterySgMapper.selectOneByExample(jssscLotterySgExample);
                                if (jssscLotterySg == null || StringUtils.isBlank(jssscLotterySg.getNumber())
                                        || System.currentTimeMillis() - DateUtils.parseDate(jssscLotterySg.getIdealTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS).getTime() < 1 * 60000) {
                                    continue;
                                }

                                // ??????????????????
                                betSscbmService.countlm(orderRecord.getIssue(), jssscLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.JSSSC.getTagType()));
                                // ????????????????????????
                                betSscbmService.countdn(orderRecord.getIssue(), jssscLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.JSSSC.getTagType()));
                                // ?????????????????????
                                betSscbmService.count15(orderRecord.getIssue(), jssscLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.JSSSC.getTagType()));
                                // ??????????????????????????????
                                betSscbmService.countqzh(orderRecord.getIssue(), jssscLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.JSSSC.getTagType()));

                            } else if (orderRecord.getLotteryId().toString().equals(CaipiaoTypeEnum.ONELHC.getTagType())) {
                                OnelhcLotterySgExample onelhcLotterySgExample = new OnelhcLotterySgExample();
                                OnelhcLotterySgExample.Criteria criteria = onelhcLotterySgExample.createCriteria();
                                criteria.andIssueEqualTo(orderRecord.getIssue());
                                OnelhcLotterySg onelhcLotterySg = onelhcLotterySgMapper.selectOneByExample(onelhcLotterySgExample);
                                if (onelhcLotterySg == null || StringUtils.isBlank(onelhcLotterySg.getNumber())
                                        || System.currentTimeMillis() - DateUtils.parseDate(onelhcLotterySg.getIdealTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS).getTime() < 1 * 60000) {
                                    continue;
                                }

                                // ???????????????- ?????????,??????,??????,??????1-6???
                                betLhcService.clearingLhcTeMaA(orderRecord.getIssue(), onelhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                                betLhcService.clearingLhcZhengTe(orderRecord.getIssue(), onelhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                                betLhcService.clearingLhcZhengMaOneToSix(orderRecord.getIssue(), onelhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                                betLhcService.clearingLhcLiuXiao(orderRecord.getIssue(), onelhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);

                                // ???????????????- ?????????,??????,?????????
                                betLhcService.clearingLhcZhengMaA(orderRecord.getIssue(), onelhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                                betLhcService.clearingLhcBanBo(orderRecord.getIssue(), onelhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                                betLhcService.clearingLhcWs(orderRecord.getIssue(), onelhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);

                                // ???????????????- ?????????,??????,?????????
                                betLhcService.clearingLhcLianMa(orderRecord.getIssue(), onelhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                                betLhcService.clearingLhcLianXiao(orderRecord.getIssue(), onelhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                                betLhcService.clearingLhcLianWei(orderRecord.getIssue(), onelhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);

                                // ???????????????- ?????????,1-6??????,?????????
                                betLhcService.clearingLhcNoOpen(orderRecord.getIssue(), onelhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                                betLhcService.clearingLhcOneSixLh(orderRecord.getIssue(), onelhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                                betLhcService.clearingLhcWuxing(orderRecord.getIssue(), onelhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);

                                // ???????????????- ?????????,?????????
                                betLhcService.clearingLhcPtPt(orderRecord.getIssue(), onelhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                                betLhcService.clearingLhcTxTx(orderRecord.getIssue(), onelhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                            } else if (orderRecord.getLotteryId().toString().equals(CaipiaoTypeEnum.FIVELHC.getTagType())) {
                                FivelhcLotterySgExample fivelhcLotterySgExample = new FivelhcLotterySgExample();
                                FivelhcLotterySgExample.Criteria criteria = fivelhcLotterySgExample.createCriteria();
                                criteria.andIssueEqualTo(orderRecord.getIssue());
                                FivelhcLotterySg fivelhcLotterySg = fivelhcLotterySgMapper.selectOneByExample(fivelhcLotterySgExample);
                                if (fivelhcLotterySg == null || StringUtils.isBlank(fivelhcLotterySg.getNumber())
                                        || System.currentTimeMillis() - DateUtils.parseDate(fivelhcLotterySg.getIdealTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS).getTime() < 1 * 60000) {
                                    continue;
                                }

                                // ???????????????- ?????????,??????,??????,??????1-6???
                                betLhcService.clearingLhcTeMaA(orderRecord.getIssue(), fivelhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                                betLhcService.clearingLhcZhengTe(orderRecord.getIssue(), fivelhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                                betLhcService.clearingLhcZhengMaOneToSix(orderRecord.getIssue(), fivelhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                                betLhcService.clearingLhcLiuXiao(orderRecord.getIssue(), fivelhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);

                                // ???????????????- ?????????,??????,?????????
                                betLhcService.clearingLhcZhengMaA(orderRecord.getIssue(), fivelhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                                betLhcService.clearingLhcBanBo(orderRecord.getIssue(), fivelhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                                betLhcService.clearingLhcWs(orderRecord.getIssue(), fivelhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);

                                // ???????????????- ?????????,??????,?????????
                                betLhcService.clearingLhcLianMa(orderRecord.getIssue(), fivelhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                                betLhcService.clearingLhcLianXiao(orderRecord.getIssue(), fivelhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                                betLhcService.clearingLhcLianWei(orderRecord.getIssue(), fivelhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);

                                // ???????????????- ?????????,1-6??????,?????????
                                betLhcService.clearingLhcNoOpen(orderRecord.getIssue(), fivelhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                                betLhcService.clearingLhcOneSixLh(orderRecord.getIssue(), fivelhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                                betLhcService.clearingLhcWuxing(orderRecord.getIssue(), fivelhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);

                                // ???????????????- ?????????,?????????
                                betLhcService.clearingLhcPtPt(orderRecord.getIssue(), fivelhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                                betLhcService.clearingLhcTxTx(orderRecord.getIssue(), fivelhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                            } else if (orderRecord.getLotteryId().toString().equals(CaipiaoTypeEnum.AMLHC.getTagType())) {
                                AmlhcLotterySgExample amlhcLotterySgExample = new AmlhcLotterySgExample();
                                AmlhcLotterySgExample.Criteria criteria = amlhcLotterySgExample.createCriteria();
                                criteria.andIssueEqualTo(orderRecord.getIssue());
                                AmlhcLotterySg amlhcLotterySg = amlhcLotterySgMapper.selectOneByExample(amlhcLotterySgExample);
                                if (amlhcLotterySg == null || StringUtils.isBlank(amlhcLotterySg.getNumber())
                                        || System.currentTimeMillis() - DateUtils.parseDate(amlhcLotterySg.getIdealTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS).getTime() < 1 * 60000) {
                                    continue;
                                }

                                // ???????????????- ?????????,??????,??????,??????1-6???
                                betLhcService.clearingLhcTeMaA(orderRecord.getIssue(), amlhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                                betLhcService.clearingLhcZhengTe(orderRecord.getIssue(), amlhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                                betLhcService.clearingLhcZhengMaOneToSix(orderRecord.getIssue(), amlhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                                betLhcService.clearingLhcLiuXiao(orderRecord.getIssue(), amlhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);

                                // ???????????????- ?????????,??????,?????????
                                betLhcService.clearingLhcZhengMaA(orderRecord.getIssue(), amlhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                                betLhcService.clearingLhcBanBo(orderRecord.getIssue(), amlhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                                betLhcService.clearingLhcWs(orderRecord.getIssue(), amlhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);

                                // ???????????????- ?????????,??????,?????????
                                betLhcService.clearingLhcLianMa(orderRecord.getIssue(), amlhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                                betLhcService.clearingLhcLianXiao(orderRecord.getIssue(), amlhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                                betLhcService.clearingLhcLianWei(orderRecord.getIssue(), amlhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);

                                // ???????????????- ?????????,1-6??????,?????????
                                betLhcService.clearingLhcNoOpen(orderRecord.getIssue(), amlhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                                betLhcService.clearingLhcOneSixLh(orderRecord.getIssue(), amlhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                                betLhcService.clearingLhcWuxing(orderRecord.getIssue(), amlhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);

                                // ???????????????- ?????????,?????????
                                betLhcService.clearingLhcPtPt(orderRecord.getIssue(), amlhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                                betLhcService.clearingLhcTxTx(orderRecord.getIssue(), amlhcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                            } else if (orderRecord.getLotteryId().toString().equals(CaipiaoTypeEnum.BJPKS.getTagType())) {
                                BjpksLotterySgExample bjpksLotterySgExample = new BjpksLotterySgExample();
                                BjpksLotterySgExample.Criteria criteria = bjpksLotterySgExample.createCriteria();
                                criteria.andIssueEqualTo(orderRecord.getIssue());
                                BjpksLotterySg bjpksLotterySg = bjpksLotterySgMapper.selectOneByExample(bjpksLotterySgExample);
                                if (bjpksLotterySg == null || StringUtils.isBlank(bjpksLotterySg.getNumber())
                                        || System.currentTimeMillis() - DateUtils.parseDate(bjpksLotterySg.getIdealTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS).getTime() < 5 * 60000) {
                                    continue;
                                }

                                // ???????????????PK10-?????????
                                betBjpksService.clearingBjpksLm(orderRecord.getIssue(), bjpksLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.BJPKS.getTagType()));
                                // ???????????????PK10-?????????????????????
                                betBjpksService.clearingBjpksCmcCqj(orderRecord.getIssue(), bjpksLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.BJPKS.getTagType()));
                                // ???????????????PK10-????????????
                                betBjpksService.clearingBjpksGyh(orderRecord.getIssue(), bjpksLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.BJPKS.getTagType()));
                            } else if (orderRecord.getLotteryId().toString().equals(CaipiaoTypeEnum.TENPKS.getTagType())) {
                                TenbjpksLotterySgExample tenbjpksLotterySgExample = new TenbjpksLotterySgExample();
                                TenbjpksLotterySgExample.Criteria criteria = tenbjpksLotterySgExample.createCriteria();
                                criteria.andIssueEqualTo(orderRecord.getIssue());
                                TenbjpksLotterySg tenbjpksLotterySg = tenbjpksLotterySgMapper.selectOneByExample(tenbjpksLotterySgExample);
                                if (tenbjpksLotterySg == null || StringUtils.isBlank(tenbjpksLotterySg.getNumber())
                                        || System.currentTimeMillis() - DateUtils.parseDate(tenbjpksLotterySg.getIdealTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS).getTime() < 1 * 60000) {
                                    continue;
                                }

                                // ???????????????PK10-?????????
                                betBjpksService.clearingBjpksLm(orderRecord.getIssue(), tenbjpksLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.TENPKS.getTagType()));
                                // ???????????????PK10-?????????????????????
                                betBjpksService.clearingBjpksCmcCqj(orderRecord.getIssue(), tenbjpksLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.TENPKS.getTagType()));
                                // ???????????????PK10-????????????
                                betBjpksService.clearingBjpksGyh(orderRecord.getIssue(), tenbjpksLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.TENPKS.getTagType()));
                            } else if (orderRecord.getLotteryId().toString().equals(CaipiaoTypeEnum.FIVEPKS.getTagType())) {
                                FivebjpksLotterySgExample fivebjpksLotterySgExample = new FivebjpksLotterySgExample();
                                FivebjpksLotterySgExample.Criteria criteria = fivebjpksLotterySgExample.createCriteria();
                                criteria.andIssueEqualTo(orderRecord.getIssue());
                                FivebjpksLotterySg fivebjpksLotterySg = fivebjpksLotterySgMapper.selectOneByExample(fivebjpksLotterySgExample);
                                if (fivebjpksLotterySg == null || StringUtils.isBlank(fivebjpksLotterySg.getNumber())
                                        || System.currentTimeMillis() - DateUtils.parseDate(fivebjpksLotterySg.getIdealTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS).getTime() < 1 * 60000) {
                                    continue;
                                }

                                // ???????????????PK10-?????????
                                betBjpksService.clearingBjpksLm(orderRecord.getIssue(), fivebjpksLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.FIVEPKS.getTagType()));
                                // ???????????????PK10-?????????????????????
                                betBjpksService.clearingBjpksCmcCqj(orderRecord.getIssue(), fivebjpksLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.FIVEPKS.getTagType()));
                                // ???????????????PK10-????????????
                                betBjpksService.clearingBjpksGyh(orderRecord.getIssue(), fivebjpksLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.FIVEPKS.getTagType()));
                            } else if (orderRecord.getLotteryId().toString().equals(CaipiaoTypeEnum.JSPKS.getTagType())) {
                                JsbjpksLotterySgExample jsbjpksLotterySgExample = new JsbjpksLotterySgExample();
                                JsbjpksLotterySgExample.Criteria criteria = jsbjpksLotterySgExample.createCriteria();
                                criteria.andIssueEqualTo(orderRecord.getIssue());
                                JsbjpksLotterySg jsbjpksLotterySg = jsbjpksLotterySgMapper.selectOneByExample(jsbjpksLotterySgExample);
                                if (jsbjpksLotterySg == null || StringUtils.isBlank(jsbjpksLotterySg.getNumber())
                                        || System.currentTimeMillis() - DateUtils.parseDate(jsbjpksLotterySg.getIdealTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS).getTime() < 1 * 60000) {
                                    continue;
                                }

                                // ???????????????PK10-?????????
                                betBjpksService.clearingBjpksLm(orderRecord.getIssue(), jsbjpksLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.JSPKS.getTagType()));
                                // ???????????????PK10-?????????????????????
                                betBjpksService.clearingBjpksCmcCqj(orderRecord.getIssue(), jsbjpksLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.JSPKS.getTagType()));
                                // ???????????????PK10-????????????
                                betBjpksService.clearingBjpksGyh(orderRecord.getIssue(), jsbjpksLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.JSPKS.getTagType()));
                            } else if (orderRecord.getLotteryId().toString().equals(CaipiaoTypeEnum.XYFEIT.getTagType())) {
                                XyftLotterySgExample xyftLotterySgExample = new XyftLotterySgExample();
                                XyftLotterySgExample.Criteria criteria = xyftLotterySgExample.createCriteria();
                                criteria.andIssueEqualTo(orderRecord.getIssue());
                                XyftLotterySg xyftLotterySg = xyftLotterySgMapper.selectOneByExample(xyftLotterySgExample);
                                if (xyftLotterySg == null || StringUtils.isBlank(xyftLotterySg.getNumber())
                                        || System.currentTimeMillis() - DateUtils.parseDate(xyftLotterySg.getIdealTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS).getTime() < 5 * 60000) {
                                    continue;
                                }

                                // ?????????????????????-?????????
                                betXyftService.clearingXyftLm(orderRecord.getIssue(), xyftLotterySg.getNumber());
                                // ?????????????????????-?????????????????????
                                betXyftService.clearingXyftCmcCqj(orderRecord.getIssue(), xyftLotterySg.getNumber());
                                // ?????????????????????-????????????
                                betXyftService.clearingXyftGyh(orderRecord.getIssue(), xyftLotterySg.getNumber());
                            } else if (orderRecord.getLotteryId().toString().equals(CaipiaoTypeEnum.PCDAND.getTagType())) {
                                PceggLotterySgExample pceggLotterySgExample = new PceggLotterySgExample();
                                PceggLotterySgExample.Criteria criteria = pceggLotterySgExample.createCriteria();
                                criteria.andIssueEqualTo(orderRecord.getIssue());
                                PceggLotterySg pceggLotterySg = pceggLotterySgMapper.selectOneByExample(pceggLotterySgExample);
                                if (pceggLotterySg == null || StringUtils.isBlank(pceggLotterySg.getNumber())
                                        || System.currentTimeMillis() - DateUtils.parseDate(pceggLotterySg.getIdealTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS).getTime() < 5 * 60000) {
                                    continue;
                                }

                                // ?????????PC??????-?????????
                                betPceggService.clearingPceggTm(orderRecord.getIssue(), pceggLotterySg.getNumber());
                                // ?????????PC??????-?????????
                                betPceggService.clearingPceggBz(orderRecord.getIssue(), pceggLotterySg.getNumber());
                                // ?????????PC??????-???????????????
                                betPceggService.clearingPceggTmbs(orderRecord.getIssue(), pceggLotterySg.getNumber());
                                // ?????????PC??????-?????????
                                betPceggService.clearingPceggSb(orderRecord.getIssue(), pceggLotterySg.getNumber());
                                // ?????????PC??????-?????????
                                betPceggService.clearingPceggHh(orderRecord.getIssue(), pceggLotterySg.getNumber());
                            } else if (orderRecord.getLotteryId().toString().equals(CaipiaoTypeEnum.TXFFC.getTagType())) {
                                TxffcLotterySgExample txffcLotterySgExample = new TxffcLotterySgExample();
                                TxffcLotterySgExample.Criteria criteria = txffcLotterySgExample.createCriteria();
                                criteria.andIssueEqualTo(orderRecord.getIssue());
                                TxffcLotterySg txffcLotterySg = txffcLotterySgMapper.selectOneByExample(txffcLotterySgExample);
                                if (txffcLotterySg == null || (StringUtils.isBlank(txffcLotterySg.getCpkNumber()) && StringUtils.isBlank(txffcLotterySg.getKcwNumber()))
                                        || System.currentTimeMillis() - DateUtils.parseDate(txffcLotterySg.getIdealTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS).getTime() < 2 * 60000) {
                                    continue;
                                }

                                // ??????????????????
                                betSscbmService.countlm(orderRecord.getIssue(), StringUtils.isNotEmpty(txffcLotterySg.getCpkNumber()) ? txffcLotterySg.getCpkNumber() : txffcLotterySg.getKcwNumber(), Integer.parseInt(CaipiaoTypeEnum.TXFFC.getTagType()));
                                // ????????????????????????
                                betSscbmService.countdn(orderRecord.getIssue(), StringUtils.isNotEmpty(txffcLotterySg.getCpkNumber()) ? txffcLotterySg.getCpkNumber() : txffcLotterySg.getKcwNumber(), Integer.parseInt(CaipiaoTypeEnum.TXFFC.getTagType()));
                                // ?????????????????????
                                betSscbmService.count15(orderRecord.getIssue(), StringUtils.isNotEmpty(txffcLotterySg.getCpkNumber()) ? txffcLotterySg.getCpkNumber() : txffcLotterySg.getKcwNumber(), Integer.parseInt(CaipiaoTypeEnum.TXFFC.getTagType()));
                                // ??????????????????????????????
                                betSscbmService.countqzh(orderRecord.getIssue(), StringUtils.isNotEmpty(txffcLotterySg.getCpkNumber()) ? txffcLotterySg.getCpkNumber() : txffcLotterySg.getKcwNumber(), Integer.parseInt(CaipiaoTypeEnum.TXFFC.getTagType()));
                            }
//                            else if (orderRecord.getLotteryId().toString().equals(CaipiaoTypeEnum.DLT.getTagType())) {
//                                TcdltLotterySgExample tcdltLotterySgExample = new TcdltLotterySgExample();
//                                TcdltLotterySgExample.Criteria criteria = tcdltLotterySgExample.createCriteria();
//                                criteria.andIssueEqualTo(orderRecord.getIssue());
//                                TcdltLotterySg tcdltLotterySg = tcdltLotterySgMapper.selectOneByExample(tcdltLotterySgExample);
//                                if (tcdltLotterySg == null || StringUtils.isBlank(tcdltLotterySg.getNumber())
//                                        || System.currentTimeMillis() - DateUtils.parseDate(tcdltLotterySg.getIdealTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS).getTime() < 5 * 60000) {
//                                    continue;
//                                }
//
//                                // ???????????????????????????
//                                betTcDltService.clearingTCDLT(orderRecord.getIssue(), tcdltLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.DLT.getTagType()));
//                            } else if (orderRecord.getLotteryId().toString().equals(CaipiaoTypeEnum.TCPLW.getTagType())) {
//                                TcplwLotterySgExample tcdltLotterySgExample = new TcplwLotterySgExample();
//                                TcplwLotterySgExample.Criteria criteria = tcdltLotterySgExample.createCriteria();
//                                criteria.andIssueEqualTo(orderRecord.getIssue());
//                                TcplwLotterySg tcplwLotterySg = tcplwLotterySgMapper.selectOneByExample(tcdltLotterySgExample);
//                                if (tcplwLotterySg == null || StringUtils.isBlank(tcplwLotterySg.getNumber())
//                                        || System.currentTimeMillis() - DateUtils.parseDate(tcplwLotterySg.getIdealTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS).getTime() < 5 * 60000) {
//                                    continue;
//                                }
//
//                                // ??????????????????-???????????????
//                                betTcPlswService.clearingTcPlwZx(orderRecord.getIssue(), tcplwLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.TCPLW.getTagType()));
//                                // ??????????????????-?????????
//                                betTcPlswService.clearingTcPlwLm(orderRecord.getIssue(), tcplwLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.TCPLW.getTagType()));
//                                // ??????????????????-???????????????
//                                betTcPlswService.clearingTcPlsZx(orderRecord.getIssue(), tcplwLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.TCPLW.getTagType()));
//                            } else if (orderRecord.getLotteryId().toString().equals(CaipiaoTypeEnum.TC7XC.getTagType())) {
//                                Tc7xcLotterySgExample tcdltLotterySgExample = new Tc7xcLotterySgExample();
//                                Tc7xcLotterySgExample.Criteria criteria = tcdltLotterySgExample.createCriteria();
//                                criteria.andIssueEqualTo(orderRecord.getIssue());
//                                Tc7xcLotterySg tc7xcLotterySg = tc7xcLotterySgMapper.selectOneByExample(tcdltLotterySgExample);
//                                if (tc7xcLotterySg == null || StringUtils.isBlank(tc7xcLotterySg.getNumber())
//                                        || System.currentTimeMillis() - DateUtils.parseDate(tc7xcLotterySg.getIdealTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS).getTime() < 5 * 60000) {
//                                    continue;
//                                }
//
//                                // ???????????????7?????????
//                                bet7xcHnService.countHn7xc(orderRecord.getIssue(), tc7xcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.TC7XC.getTagType()));
//                            } else if (orderRecord.getLotteryId().toString().equals(CaipiaoTypeEnum.FCSSQ.getTagType())) {
//                                FcssqLotterySgExample fcssqLotterySgExample = new FcssqLotterySgExample();
//                                FcssqLotterySgExample.Criteria criteria = fcssqLotterySgExample.createCriteria();
//                                criteria.andIssueEqualTo(orderRecord.getIssue());
//                                FcssqLotterySg fcssqLotterySg = fcssqLotterySgMapper.selectOneByExample(fcssqLotterySgExample);
//                                if (fcssqLotterySg == null || StringUtils.isBlank(fcssqLotterySg.getNumber())
//                                        || System.currentTimeMillis() - DateUtils.parseDate(fcssqLotterySg.getIdealTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS).getTime() < 5 * 60000) {
//                                    continue;
//                                }
//
//                                // ????????????????????????
//                                betFcSsqService.clearingFCSSQ(orderRecord.getIssue(), fcssqLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.FCSSQ.getTagType()));
//                            } else if (orderRecord.getLotteryId().toString().equals(CaipiaoTypeEnum.FC3D.getTagType())) {
//                                Fc3dLotterySgExample fc3dLotterySgExample = new Fc3dLotterySgExample();
//                                Fc3dLotterySgExample.Criteria criteria = fc3dLotterySgExample.createCriteria();
//                                criteria.andIssueEqualTo(orderRecord.getIssue());
//                                Fc3dLotterySg fc3dLotterySg = fc3dLotterySgMapper.selectOneByExample(fc3dLotterySgExample);
//                                if (fc3dLotterySg == null || StringUtils.isBlank(fc3dLotterySg.getNumber())
//                                        || System.currentTimeMillis() - DateUtils.parseDate(fc3dLotterySg.getIdealTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS).getTime() < 5 * 60000) {
//                                    continue;
//                                }
//
//                                // ???????????????3D???
//                                bet3dFcService.countFc3d(orderRecord.getIssue(), fc3dLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.FC3D.getTagType()));
//                            } else if (orderRecord.getLotteryId().toString().equals(CaipiaoTypeEnum.FC7LC.getTagType())) {
//                                Fc7lcLotterySgExample fc7lcLotterySgExample = new Fc7lcLotterySgExample();
//                                Fc7lcLotterySgExample.Criteria criteria = fc7lcLotterySgExample.createCriteria();
//                                criteria.andIssueEqualTo(orderRecord.getIssue());
//                                Fc7lcLotterySg fc7lcLotterySg = fc7lcLotterySgMapper.selectOneByExample(fc7lcLotterySgExample);
//                                if (fc7lcLotterySg == null || StringUtils.isBlank(fc7lcLotterySg.getNumber())
//                                        || System.currentTimeMillis() - DateUtils.parseDate(fc7lcLotterySg.getIdealTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS).getTime() < 5 * 60000) {
//                                    continue;
//                                }
//
//                                // ???????????????????????????
//                                betFc7lcService.clearingFC7LC(orderRecord.getIssue(), fc7lcLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.FC7LC.getTagType()));
//                            } else if (orderRecord.getLotteryId().toString().equals(CaipiaoTypeEnum.KLNIU.getTagType())) {
//                                FivesscLotterySgExample fivesscLotterySgExample = new FivesscLotterySgExample();
//                                FivesscLotterySgExample.Criteria criteria = fivesscLotterySgExample.createCriteria();
//                                criteria.andIssueEqualTo(orderRecord.getIssue());
//                                FivesscLotterySg fivesscLotterySg = fivesscLotterySgMapper.selectOneByExample(fivesscLotterySgExample);
//                                if (fivesscLotterySg == null || StringUtils.isBlank(fivesscLotterySg.getNumber())
//                                        || System.currentTimeMillis() - DateUtils.parseDate(fivesscLotterySg.getIdealTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS).getTime() < 5 * 60000) {
//                                    continue;
//                                }
//
//                                // ?????????????????????-?????????
//                                betNnKlService.countKlXianjia(orderRecord.getIssue(), fivesscLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.KLNIU.getTagType()));
//                            } else if (orderRecord.getLotteryId().toString().equals(CaipiaoTypeEnum.AZNIU.getTagType())) {
//                                AuspksLotterySgExample auspksLotterySgExample = new AuspksLotterySgExample();
//                                AuspksLotterySgExample.Criteria criteria = auspksLotterySgExample.createCriteria();
//                                criteria.andIssueEqualTo(orderRecord.getIssue());
//                                AuspksLotterySg auspksLotterySg = auspksLotterySgMapper.selectOneByExample(auspksLotterySgExample);
//                                if (auspksLotterySg == null || StringUtils.isBlank(auspksLotterySg.getNumber())
//                                        || System.currentTimeMillis() - DateUtils.parseDate(auspksLotterySg.getIdealTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS).getTime() < 5 * 60000) {
//                                    continue;
//                                }
//
//                                // ?????????????????????-?????????
//                                betNnAzService.countAzXianjia(orderRecord.getIssue(), auspksLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.AZNIU.getTagType()));
//                            } else if (orderRecord.getLotteryId().toString().equals(CaipiaoTypeEnum.JSNIU.getTagType())) {
//                                JsbjpksLotterySgExample jsbjpksLotterySgExample = new JsbjpksLotterySgExample();
//                                JsbjpksLotterySgExample.Criteria criteria = jsbjpksLotterySgExample.createCriteria();
//                                criteria.andIssueEqualTo(orderRecord.getIssue());
//                                JsbjpksLotterySg jsbjpksLotterySg = jsbjpksLotterySgMapper.selectOneByExample(jsbjpksLotterySgExample);
//                                if (jsbjpksLotterySg == null || StringUtils.isBlank(jsbjpksLotterySg.getNumber())
//                                        || System.currentTimeMillis() - DateUtils.parseDate(jsbjpksLotterySg.getIdealTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS).getTime() < 1 * 60000) {
//                                    continue;
//                                }
//
//                                // ?????????????????????-?????????
//                                betNnJsService.countJsXianjia(orderRecord.getIssue(), jsbjpksLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.JSNIU.getTagType()));
//                            } else if (orderRecord.getLotteryId().toString().equals(CaipiaoTypeEnum.JSPKFT.getTagType())) {
//                                FtjspksLotterySgExample ftjspksLotterySgExample = new FtjspksLotterySgExample();
//                                FtjspksLotterySgExample.Criteria criteria = ftjspksLotterySgExample.createCriteria();
//                                criteria.andIssueEqualTo(orderRecord.getIssue());
//                                FtjspksLotterySg ftjspksLotterySg = ftjspksLotterySgMapper.selectOneByExample(ftjspksLotterySgExample);
//                                if (ftjspksLotterySg == null || StringUtils.isBlank(ftjspksLotterySg.getNumber())
//                                        || System.currentTimeMillis() - DateUtils.parseDate(ftjspksLotterySg.getIdealTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS).getTime() < 1 * 60000) {
//                                    continue;
//                                }
//
//                                // ???????????????pk?????????
//                                betFtJspksService.clearingJspksJs(orderRecord.getIssue(), ftjspksLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.JSPKFT.getTagType()));
//                            } else if (orderRecord.getLotteryId().toString().equals(CaipiaoTypeEnum.XYFTFT.getTagType())) {
//                                FtxyftLotterySgExample ftxyftLotterySgExample = new FtxyftLotterySgExample();
//                                FtxyftLotterySgExample.Criteria criteria = ftxyftLotterySgExample.createCriteria();
//                                criteria.andIssueEqualTo(orderRecord.getIssue());
//                                FtxyftLotterySg ftxyftLotterySg = ftxyftLotterySgMapper.selectOneByExample(ftxyftLotterySgExample);
//                                if (ftxyftLotterySg == null || StringUtils.isBlank(ftxyftLotterySg.getNumber())
//                                        || System.currentTimeMillis() - DateUtils.parseDate(ftxyftLotterySg.getIdealTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS).getTime() < 5 * 60000) {
//                                    continue;
//                                }
//
//                                // ??????????????????
//                                betFtXyftService.clearingFtXyftJs(orderRecord.getIssue(), ftxyftLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.XYFTFT.getTagType()));
//                            }
//                            else if (orderRecord.getLotteryId().toString().equals(CaipiaoTypeEnum.JSSSCFT.getTagType())) {
//                                JssscLotterySgExample jssscLotterySgExample = new JssscLotterySgExample();
//                                JssscLotterySgExample.Criteria criteria = jssscLotterySgExample.createCriteria();
//                                criteria.andIssueEqualTo(orderRecord.getIssue());
//                                JssscLotterySg jssscLotterySg = jssscLotterySgMapper.selectOneByExample(jssscLotterySgExample);
//                                if (jssscLotterySg == null || StringUtils.isBlank(jssscLotterySg.getNumber())
//                                        || System.currentTimeMillis() - DateUtils.parseDate(jssscLotterySg.getIdealTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS).getTime() < 1 * 60000) {
//                                    continue;
//                                }
//
//                                // ?????????????????????????????????
//                                betFtSscService.clearingFtSscJs(orderRecord.getIssue(), jssscLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.JSSSCFT.getTagType()));
//                            }
                            else if (orderRecord.getLotteryId().toString().equals(CaipiaoTypeEnum.AUSACT.getTagType())) {
                                AusactLotterySgExample ausactLotterySgExample = new AusactLotterySgExample();
                                AusactLotterySgExample.Criteria criteria = ausactLotterySgExample.createCriteria();
                                criteria.andIssueEqualTo(orderRecord.getIssue());
                                AusactLotterySg ausactLotterySg = ausactLotterySgMapper.selectOneByExample(ausactLotterySgExample);
                                if (ausactLotterySg == null || ausactLotterySg == null || StringUtils.isBlank(ausactLotterySg.getNumber())
                                        || System.currentTimeMillis() - DateUtils.parseDate(ausactLotterySg.getIdealTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS).getTime() < 2 * 60000) {
                                    continue;
                                }

                                //??????act??????
                                betActAzService.countAzAct(orderRecord.getIssue(), ausactLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.AUSACT.getTagType()));
                            } else if (orderRecord.getLotteryId().toString().equals(CaipiaoTypeEnum.AUSSSC.getTagType())) {
                                AussscLotterySgExample aussscLotterySgExample = new AussscLotterySgExample();
                                AussscLotterySgExample.Criteria criteria = aussscLotterySgExample.createCriteria();
                                criteria.andIssueEqualTo(orderRecord.getIssue());
                                AussscLotterySg ausactLotterySg = aussscLotterySgMapper.selectOneByExample(aussscLotterySgExample);
                                if (ausactLotterySg == null || StringUtils.isBlank(ausactLotterySg.getNumber())
                                        || System.currentTimeMillis() - DateUtils.parseDate(ausactLotterySg.getIdealTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS).getTime() < 2 * 60000) {
                                    continue;
                                }

                                // ???????????????????????????
                                betSscAzService.countAzSsc(orderRecord.getIssue(), ausactLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.AUSSSC.getTagType()));
                            } else if (orderRecord.getLotteryId().toString().equals(CaipiaoTypeEnum.AUSPKS.getTagType())) {
                                AuspksLotterySgExample auspksLotterySgExample = new AuspksLotterySgExample();
                                AuspksLotterySgExample.Criteria criteria = auspksLotterySgExample.createCriteria();
                                criteria.andIssueEqualTo(orderRecord.getIssue());
                                AuspksLotterySg auspksLotterySg = auspksLotterySgMapper.selectOneByExample(auspksLotterySgExample);
                                if (auspksLotterySg == null || StringUtils.isBlank(auspksLotterySg.getNumber())
                                        || System.currentTimeMillis() - DateUtils.parseDate(auspksLotterySg.getIdealTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS).getTime() < 2 * 60000) {
                                    continue;
                                }

                                // ???????????????F1???
                                betF1AzService.countAzF1(orderRecord.getIssue(), auspksLotterySg.getNumber(), Integer.parseInt(CaipiaoTypeEnum.AUSPKS.getTagType()));
                            }
                        } catch (Exception e) {
                            logger.error("?????????????????????lotteryId:{},issue:{},number:{}", orderRecord.getLotteryId(), orderRecord.getIssue(), orderRecord.getOpenNumber(), e);
                        }

                    }
                }
            }
        } catch (Exception e) {
            logger.error("??????????????????", e);
        } finally {
            lock.writeLock().unlock();
        }

    }


}

