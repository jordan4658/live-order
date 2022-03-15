package com.caipiao.live.order.receiver;

import com.caipiao.live.order.config.ActiveMQConfig;
import com.caipiao.live.order.service.bet.HkLhcSettlementService;
import com.caipiao.live.common.enums.lottery.CaipiaoTypeEnum;
import com.caipiao.live.common.util.redis.RedisLock;
import com.caipiao.live.order.service.order.OrderWriteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class
HkLhcReceiver {

    private static final Logger logger = LoggerFactory.getLogger(HkLhcReceiver.class);

    private final String MQ_SETTLEMENT_KEY_PREFIX = "settlement";

    @Autowired
    @Lazy
    private HkLhcSettlementService betLhcService;
    @Autowired
    private OrderWriteService orderWriteService;
    @Autowired
    private RedisTemplate redisTemplate;


    // 六合彩(特码,正特,六肖,正码1-6) 队列名
    @JmsListener(destination = ActiveMQConfig.TOPIC_LHC_SG, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgLhcSg(String message) throws Exception {
        String[] str = decodeMessage(message);
        if (StringUtils.isEmpty(str)) {
            logger.error("通知消息异常，无法解析.message:[{}]", message);
            return;
        }
        String issue = str[0];
        String openNumber = str[1];

        // 加锁
        String key = ActiveMQConfig.TOPIC_LHC_SG + issue + openNumber;
        RedisLock lock = new RedisLock(key);
        try {
            if (lock.lock()) {
                if (!redisTemplate.hasKey(key)) {
                    redisTemplate.opsForValue().set(key, "1", 1000l);
                    orderWriteService.jiesuanBySg(issue, openNumber);
                }
            }
        } catch (Exception e) {
            logger.error("", e);
        } finally {
            try {
                if (lock.lock()) {
                    lock.unlock();
                }
            } catch (Exception e) {
                logger.error("", e);
            }
        }
    }

    // 六合彩(特码,正特,六肖,正码1-6) 队列名
    @JmsListener(destination = "${ActiveMQConfig.TOPIC_LHC_TM_ZT_LX}", containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgLhcTmZtLx(String message) throws Exception {
        logger.info("香港六合彩收到结算通知消息:[{}]", message);
        String[] str = decodeMessage(message);
        if (StringUtils.isEmpty(str)) {
            logger.error("通知消息异常，无法解析.message:[{}]", message);
            return;
        }
        String issue = str[0];
        String openNumber = str[1];
        long begin = System.currentTimeMillis();
        // 加锁
        String key = MQ_SETTLEMENT_KEY_PREFIX + ActiveMQConfig.TOPIC_LHC_TM_ZT_LX + issue + openNumber;
        RedisLock lock = new RedisLock(key);
        try {
            if (lock.lock()) {
                // 结算六合彩- 【特码,正特,六肖,正码1-6】
                betLhcService.clearingHkLhcTeMaA(issue, openNumber, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()),
                        true);
                betLhcService.clearingHkLhcZhengTe(issue, openNumber,
                        Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()), true);
                betLhcService.clearingHkLhcZhengMaOneToSix(issue, openNumber,
                        Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()), true);
                betLhcService.clearingHkLhcLiuXiao(issue, openNumber,
                        Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()), true);
            }
        } catch (Exception e) {
            logger.error("", e);
        } finally {
            try {
                if (lock.lock()) {
                    lock.unlock();
                }
            } catch (Exception e) {
                logger.error("", e);
            }
            logger.info("结算TOPIC_LHC_TM_ZT_LX完成共耗时[{}]ms", System.currentTimeMillis() - begin);
        }
    }

    // 六合彩(正码,半波,尾数) 队列名
    @JmsListener(destination = "${ActiveMQConfig.TOPIC_LHC_ZM_BB_WS}", containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgLhcZmBbWs(String message) throws Exception {
        logger.info("香港六合彩收到结算通知消息:[{}]", message);
        String[] str = decodeMessage(message);
        if (StringUtils.isEmpty(str)) {
            logger.error("通知消息异常，无法解析.message:[{}]", message);
            return;
        }
        String issue = str[0];
        String openNumber = str[1];
        long begin = System.currentTimeMillis();
        // 加锁
        String key = MQ_SETTLEMENT_KEY_PREFIX + ActiveMQConfig.TOPIC_LHC_ZM_BB_WS + issue + openNumber;
        RedisLock lock = new RedisLock(key);
        try {
            if (lock.lock()) {
                // 结算六合彩- 【正码,半波,尾数】
                betLhcService.clearingHkLhcZhengMaA(issue, openNumber,
                        Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()), true);
                betLhcService.clearingHkLhcBanBo(issue, openNumber, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()),
                        true);
                betLhcService.clearingHkLhcWs(issue, openNumber, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()),
                        true);
            }
        } catch (Exception e) {
            logger.error("", e);
        } finally {
            try {
                if (lock.lock()) {
                    lock.unlock();
                }
            } catch (Exception e) {
                logger.error("", e);
            }
            logger.info("结算TOPIC_LHC_ZM_BB_WS完成共耗时[{}]ms", System.currentTimeMillis() - begin);
        }
    }

    // 六合彩(连码,连肖,连尾) 队列名
    @JmsListener(destination = "${ActiveMQConfig.TOPIC_LHC_LM_LX_LW}", containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgLhcLmLxLw(String message) throws Exception {
        logger.info("香港六合彩收到结算通知消息:[{}]", message);
        String[] str = decodeMessage(message);
        if (StringUtils.isEmpty(str)) {
            logger.error("通知消息异常，无法解析.message:[{}]", message);
            return;
        }
        String issue = str[0];
        String openNumber = str[1];
        long begin = System.currentTimeMillis();
        // 加锁
        String key = MQ_SETTLEMENT_KEY_PREFIX + ActiveMQConfig.TOPIC_LHC_LM_LX_LW + issue + openNumber;
        RedisLock lock = new RedisLock(key);
        try {
            if (lock.lock()) {
                // 结算六合彩- 【连码,连肖,连尾】
                betLhcService.clearingHkLhcLianMa(issue, openNumber, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()),
                        true);
                betLhcService.clearingHkLhcLianXiao(issue, openNumber,
                        Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()), true);
                betLhcService.clearingHkLhcLianWei(issue, openNumber,
                        Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()), true);
            }
        } catch (Exception e) {
            logger.error("", e);
        } finally {
            try {
                if (lock.lock()) {
                    lock.unlock();
                }
            } catch (Exception e) {
                logger.error("", e);
            }
            logger.info("结算TOPIC_LHC_LM_LX_LW完成共耗时[{}]ms", System.currentTimeMillis() - begin);
        }
    }

    // 六合彩(不中,1-6龙虎,五行) 队列名
    @JmsListener(destination = "${ActiveMQConfig.TOPIC_LHC_BZ_LH_WX}", containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgLhcBzLhWx(String message) throws Exception {
        logger.info("香港六合彩收到结算通知消息:[{}]", message);
        String[] str = decodeMessage(message);
        if (StringUtils.isEmpty(str)) {
            logger.error("通知消息异常，无法解析.message:[{}]", message);
            return;
        }
        String issue = str[0];
        String openNumber = str[1];
        long begin = System.currentTimeMillis();
        // 加锁
        String key = MQ_SETTLEMENT_KEY_PREFIX + ActiveMQConfig.TOPIC_LHC_BZ_LH_WX + issue + openNumber;
        //RedisLock lock = new RedisLock(key);
        try {
            //if (lock.lock()) {
            logger.debug("耗时[{}]ms成功取到锁[{}]", System.currentTimeMillis() - begin, key);
            // 结算六合彩- 【不中,1-6龙虎,五行】
            betLhcService.clearingHkLhcNoOpen(issue, openNumber, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()),
                    true);
            betLhcService.clearingHkLhcOneSixLh(issue, openNumber,
                    Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()), true);
            betLhcService.clearingHkLhcWuxing(issue, openNumber, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()),
                    true);
            //}
        } catch (Exception e) {
            logger.error("", e);
        } finally {
            try {
                //if (lock.lock()) {
                //	lock.unlock();
                //}
            } catch (Exception e) {
                logger.error("", e);
            }
            logger.debug("结算TOPIC_LHC_BZ_LH_WX完成共耗时[{}]ms", System.currentTimeMillis() - begin);
        }
    }

    // 六合彩(平特,特肖) 队列名
    @JmsListener(destination = "${ActiveMQConfig.TOPIC_LHC_PT_TX}", containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgLhcPtTx(String message) throws Exception {
        logger.info("香港六合彩收到结算通知消息:[{}]", message);
        String[] str = decodeMessage(message);
        if (StringUtils.isEmpty(str)) {
            logger.error("通知消息异常，无法解析.message:[{}]", message);
            return;
        }
        String issue = str[0];
        String openNumber = str[1];
        long begin = System.currentTimeMillis();
        // 加锁
        String key = MQ_SETTLEMENT_KEY_PREFIX + ActiveMQConfig.TOPIC_LHC_PT_TX + issue + openNumber;
        RedisLock lock = new RedisLock(key);
        try {
            if (lock.lock()) {
                // 结算六合彩- 【平特,特肖】
                betLhcService.clearingHkLhcPtPt(issue, openNumber, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()),
                        true);
                betLhcService.clearingHkLhcTxTx(issue, openNumber, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()),
                        true);
            }
        } catch (Exception e) {
            logger.error("", e);
        } finally {
            try {
                if (lock.lock()) {
                    lock.unlock();
                }
            } catch (Exception e) {
                logger.error("", e);
            }
            logger.info("结算TOPIC_LHC_PT_TX完成共耗时[{}]ms", System.currentTimeMillis() - begin);
        }
    }

    private String[] decodeMessage(String message) {
        logger.info("待解析的结算通知消息:[{}]", message);
        if (StringUtils.isEmpty(message)) {
            logger.error("MQ通知开奖号码信息为空");
            return null;
        }
        String issue = message.split(":")[1];
        String openNumber = message.split(":")[2];
        if (StringUtils.isEmpty(issue) || StringUtils.isEmpty(openNumber)) {
            logger.error("MQ消息异常, issue:[{}], openNumber:[{}]", issue, openNumber);
            return null;
        }

        return new String[]{issue, openNumber};
    }

}
