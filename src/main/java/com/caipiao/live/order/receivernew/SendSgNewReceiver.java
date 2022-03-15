package com.caipiao.live.order.receivernew;

import com.alibaba.fastjson.JSONObject;
import com.caipiao.live.common.mybatis.mapperext.sg.AzksLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.DzksLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.DzpceggLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.DzxyftLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.XjplhcLotterySgMapperExt;
import com.caipiao.live.order.config.ActiveMQConfig;
import com.caipiao.live.common.enums.lottery.CaipiaoTypeEnum;
import com.caipiao.live.common.mybatis.entity.*;
import com.caipiao.live.common.mybatis.mapper.*;
import com.caipiao.live.common.util.StringUtils;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author
 * @create
 **/
@Component
public class SendSgNewReceiver {
    private static final Logger logger = LoggerFactory.getLogger(com.caipiao.live.order.receiver.SendSgReceiver.class);
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private AzksLotterySgMapperExt azksLotterySgMapperExt;
    @Autowired
    private AzksLotterySgMapper azksLotterySgMapper;
    @Autowired
    private DzksLotterySgMapperExt dzksLotterySgMapperExt;
    @Autowired
    private DzksLotterySgMapper dzksLotterySgMapper;
    @Autowired
    private DzpceggLotterySgMapperExt dzpceggLotterySgMapperExt;
    @Autowired
    private XjplhcLotterySgMapperExt xjplhcLotterySgMapperExt;
    @Autowired
    private DzpceggLotterySgMapper dzpceggLotterySgMapper;
    @Autowired
    private DzxyftLotterySgMapperExt dzxyftLotterySgMapperExt;
    @Autowired
    private DzxyftLotterySgMapper dzxyftLotterySgMapper;
    @Autowired
    private XjplhcLotterySgMapper xjplhcLotterySgMapper;

    /**
     * 结算sg 是否在数据库，如不在，则更新
     *
     * @param message 消息内容【期号】
     */
    @JmsListener(destination = ActiveMQConfig.LIVE_TOPIC_ALL_LOTTERY_SG, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgAzAct(String message) throws Exception {
        logger.info("检查sg_new开始  " + message);
        // 获取一个时间戳
        String st[] = message.split("#");

        // 获取唯一
        String key = ActiveMQConfig.LIVE_TOPIC_ALL_LOTTERY_SG + st[0];
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lockl");
        try {
            // 写锁（等待时间100s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 60, TimeUnit.SECONDS);
            if (bool) {
                if (redisTemplate.opsForValue().get(key) == null) {
                    if (st[0].equals(CaipiaoTypeEnum.AZKS.getTagType())) {
                        List<AzksLotterySg> list = JSONObject.parseArray(st[1], AzksLotterySg.class);
                        for (AzksLotterySg sg : list) {
                            AzksLotterySgExample azksLotterySgExample = new AzksLotterySgExample();
                            AzksLotterySgExample.Criteria criteria = azksLotterySgExample.createCriteria();
                            criteria.andIssueEqualTo(sg.getIssue());
                            AzksLotterySg thisSg = azksLotterySgMapper.selectOneByExample(azksLotterySgExample);
                            if (StringUtils.isEmpty(thisSg.getNumber())) {
                                azksLotterySgMapperExt.updateByIssue(sg);
                            }
                        }
                        redisTemplate.opsForValue().set(key, "1", 60L, TimeUnit.SECONDS);
                    } else if (st[0].equals(CaipiaoTypeEnum.DZKS.getTagType())) {
                        List<DzksLotterySg> list = JSONObject.parseArray(st[1], DzksLotterySg.class);
                        for (DzksLotterySg sg : list) {
                            DzksLotterySgExample dzksLotterySgExample = new DzksLotterySgExample();
                            DzksLotterySgExample.Criteria criteria = dzksLotterySgExample.createCriteria();
                            criteria.andIssueEqualTo(sg.getIssue());
                            DzksLotterySg thisSg = dzksLotterySgMapper.selectOneByExample(dzksLotterySgExample);
                            if (StringUtils.isEmpty(thisSg.getNumber())) {
                                dzksLotterySgMapperExt.updateByIssue(sg);
                            }
                        }
                        redisTemplate.opsForValue().set(key, "1", 60L, TimeUnit.SECONDS);
                    } else if (st[0].equals(CaipiaoTypeEnum.DZXYFEIT.getTagType())) {
                        List<DzxyftLotterySg> list = JSONObject.parseArray(st[1], DzxyftLotterySg.class);
                        for (DzxyftLotterySg sg : list) {
                            DzxyftLotterySgExample dzxyftLotterySgExample = new DzxyftLotterySgExample();
                            DzxyftLotterySgExample.Criteria criteria = dzxyftLotterySgExample.createCriteria();
                            criteria.andIssueEqualTo(sg.getIssue());
                            DzxyftLotterySg thisSg = dzxyftLotterySgMapper.selectOneByExample(dzxyftLotterySgExample);
                            if (StringUtils.isEmpty(thisSg.getNumber())) {
                                dzxyftLotterySgMapperExt.updateByIssue(sg);
                            }
                        }
                        redisTemplate.opsForValue().set(key, "1", 60L, TimeUnit.SECONDS);
                    } else if (st[0].equals(CaipiaoTypeEnum.DZPCDAND.getTagType())) {
                        List<DzpceggLotterySg> list = JSONObject.parseArray(st[1], DzpceggLotterySg.class);
                        for (DzpceggLotterySg sg : list) {
                            DzpceggLotterySgExample dzpceggLotterySgExample = new DzpceggLotterySgExample();
                            DzpceggLotterySgExample.Criteria criteria = dzpceggLotterySgExample.createCriteria();
                            criteria.andIssueEqualTo(sg.getIssue());
                            DzpceggLotterySg thisSg = dzpceggLotterySgMapper.selectOneByExample(dzpceggLotterySgExample);
                            if (StringUtils.isEmpty(thisSg.getNumber())) {
                                dzpceggLotterySgMapperExt.updateByIssue(sg);
                            }
                        }
                        redisTemplate.opsForValue().set(key, "1", 60L, TimeUnit.SECONDS);
                    } else if (st[0].equals(CaipiaoTypeEnum.XJPLHC.getTagType())) {
                        List<XjplhcLotterySg> list = JSONObject.parseArray(st[1], XjplhcLotterySg.class);
                        for (XjplhcLotterySg sg : list) {
                            XjplhcLotterySgExample xjplhcLotterySgExample = new XjplhcLotterySgExample();
                            XjplhcLotterySgExample.Criteria criteria = xjplhcLotterySgExample.createCriteria();
                            criteria.andIssueEqualTo(sg.getIssue());
                            XjplhcLotterySg thisSg = xjplhcLotterySgMapper.selectOneByExample(xjplhcLotterySgExample);
                            if (StringUtils.isEmpty(thisSg.getNumber())) {
                                xjplhcLotterySgMapperExt.updateByIssue(sg);
                            }
                        }
                        redisTemplate.opsForValue().set(key, "1", 60L, TimeUnit.SECONDS);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("检查sg出错", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

}

