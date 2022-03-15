package com.caipiao.live.order.receiver;

import com.alibaba.fastjson.JSONObject;
import com.caipiao.live.common.mybatis.mapperext.sg.AmlhcLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.AusactLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.AuspksLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.AussscLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.BjpksLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.CqsscLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.Fc3dLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.Fc7lcLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.FcssqLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.FivebjpksLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.FivelhcLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.FivesscLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.FtJspksLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.FtJssscLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.FtXyftLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.JsbjpksLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.JssscLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.OnelhcLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.PceggLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.Tc7xcLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.TcdltLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.TcplwLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.TenbjpksLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.TensscLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.TjsscLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.TxffcLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.XjsscLotterySgMapperExt;
import com.caipiao.live.common.mybatis.mapperext.sg.XyftLotterySgMapperExt;
import com.caipiao.live.order.config.ActiveMQConfig;
import com.caipiao.live.common.enums.lottery.CaipiaoTypeEnum;
import com.caipiao.live.common.mybatis.entity.*;
import com.caipiao.live.common.mybatis.mapper.*;
import com.caipiao.live.common.util.DateUtils;
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
 * @author lzy
 * @create 2018-09-17 20:07
 **/
@Component
public class SendSgReceiver {
    private static final Logger logger = LoggerFactory.getLogger(SendSgReceiver.class);

    @Autowired
    private AusactLotterySgMapper ausactLotterySgMapper;
    @Autowired
    private AusactLotterySgMapperExt ausactLotterySgMapperExt;
    @Autowired
    private AussscLotterySgMapper aussscLotterySgMapper;
    @Autowired
    private AussscLotterySgMapperExt aussscLotterySgMapperExt;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private CqsscLotterySgMapper cqsscLotterySgMapper;
    @Autowired
    private CqsscLotterySgMapperExt cqsscLotterySgMapperExt;
    @Autowired
    private XjsscLotterySgMapper xjsscLotterySgMapper;
    @Autowired
    private XjsscLotterySgMapperExt xjsscLotterySgMapperExt;
    @Autowired
    private TjsscLotterySgMapper tjsscLotterySgMapper;
    @Autowired
    private TjsscLotterySgMapperExt tjsscLotterySgMapperExt;
    @Autowired
    private TensscLotterySgMapper tensscLotterySgMapper;
    @Autowired
    private TensscLotterySgMapperExt tensscLotterySgMapperExt;
    @Autowired
    private FivesscLotterySgMapper fivesscLotterySgMapper;
    @Autowired
    private FivesscLotterySgMapperExt fivesscLotterySgMapperExt;
    @Autowired
    private JssscLotterySgMapper jssscLotterySgMapper;
    @Autowired
    private JssscLotterySgMapperExt jssscLotterySgMapperExt;
    @Autowired
    private OnelhcLotterySgMapper onelhcLotterySgMapper;
    @Autowired
    private OnelhcLotterySgMapperExt onelhcLotterySgMapperExt;
    @Autowired
    private FivelhcLotterySgMapper fivelhcLotterySgMapper;
    @Autowired
    private FivelhcLotterySgMapperExt fivelhcLotterySgMapperExt;
    @Autowired
    private AmlhcLotterySgMapper amlhcLotterySgMapper;
    @Autowired
    private AmlhcLotterySgMapperExt amlhcLotterySgMapperExt;
    @Autowired
    private BjpksLotterySgMapper bjpksLotterySgMapper;
    @Autowired
    private BjpksLotterySgMapperExt bjpksLotterySgMapperExt;
    @Autowired
    private TenbjpksLotterySgMapper tenbjpksLotterySgMapper;
    @Autowired
    private TenbjpksLotterySgMapperExt tenbjpksLotterySgMapperExt;
    @Autowired
    private FivebjpksLotterySgMapper fivebjpksLotterySgMapper;
    @Autowired
    private FivebjpksLotterySgMapperExt fivebjpksLotterySgMapperExt;
    @Autowired
    private JsbjpksLotterySgMapper jsbjpksLotterySgMapper;
    @Autowired
    private JsbjpksLotterySgMapperExt jsbjpksLotterySgMapperExt;
    @Autowired
    private XyftLotterySgMapper xyftLotterySgMapper;
    @Autowired
    private XyftLotterySgMapperExt xyftLotterySgMapperExt;
    @Autowired
    private PceggLotterySgMapper pceggLotterySgMapper;
    @Autowired
    private PceggLotterySgMapperExt pceggLotterySgMapperExt;
    @Autowired
    private TxffcLotterySgMapper txffcLotterySgMapper;
    @Autowired
    private TxffcLotterySgMapperExt txffcLotterySgMapperExt;
    @Autowired
    private TcdltLotterySgMapper tcdltLotterySgMapper;
    @Autowired
    private TcdltLotterySgMapperExt tcdltLotterySgMapperExt;
    @Autowired
    private TcplwLotterySgMapper tcplwLotterySgMapper;
    @Autowired
    private TcplwLotterySgMapperExt tcplwLotterySgMapperExt;
    @Autowired
    private Tc7xcLotterySgMapper tc7xcLotterySgMapper;
    @Autowired
    private Tc7xcLotterySgMapperExt tc7xcLotterySgMapperExt;
    @Autowired
    private FcssqLotterySgMapper fcssqLotterySgMapper;
    @Autowired
    private FcssqLotterySgMapperExt fcssqLotterySgMapperExt;
    @Autowired
    private Fc3dLotterySgMapper fc3dLotterySgMapper;
    @Autowired
    private Fc3dLotterySgMapperExt fc3dLotterySgMapperExt;
    @Autowired
    private Fc7lcLotterySgMapper fc7lcLotterySgMapper;
    @Autowired
    private Fc7lcLotterySgMapperExt fc7lcLotterySgMapperExt;
    @Autowired
    private AuspksLotterySgMapper auspksLotterySgMapper;
    @Autowired
    private AuspksLotterySgMapperExt auspksLotterySgMapperExt;
    @Autowired
    private FtxyftLotterySgMapper ftxyftLotterySgMapper;
    @Autowired
    private FtXyftLotterySgMapperExt ftXyftLotterySgMapperExt;
    @Autowired
    private FtjspksLotterySgMapper ftjspksLotterySgMapper;
    @Autowired
    private FtJspksLotterySgMapperExt ftJspksLotterySgMapperExt;
    @Autowired
    private FtjssscLotterySgMapper ftjssscLotterySgMapper;
    @Autowired
    private FtJssscLotterySgMapperExt ftJssscLotterySgMapperExt;
    @Autowired
    private RedisTemplate redisTemplate;


    /**
     * 结算sg 是否在数据库，如不在，则更新
     * 检查官彩
     * @param message 消息内容【期号】
     */
    @JmsListener(destination = ActiveMQConfig.TOPIC_ALL_LOTTERY_SG, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgAzAct(String message) throws Exception {
        logger.info("检查sg开始  " + message);
        // 获取一个时间戳
        String st[] = message.split("#");

        // 获取唯一
        String key = ActiveMQConfig.TOPIC_ALL_LOTTERY_SG + st[0];
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lockl");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 30, TimeUnit.SECONDS);
            if (bool) {
                if (redisTemplate.opsForValue().get(key) == null) {
                    if (st[0].equals(CaipiaoTypeEnum.CQSSC.getTagType())) {
                        List<CqsscLotterySg> list = JSONObject.parseArray(st[1], CqsscLotterySg.class);
                        for (CqsscLotterySg sg : list) {
                            CqsscLotterySgExample cqsscLotterySgExample = new CqsscLotterySgExample();
                            CqsscLotterySgExample.Criteria criteria = cqsscLotterySgExample.createCriteria();
                            criteria.andIssueEqualTo(sg.getIssue());
                            CqsscLotterySg thisSg = cqsscLotterySgMapper.selectOneByExample(cqsscLotterySgExample);
                            if (thisSg.getWan() == null) {
                                sg.setActualDate(DateUtils.str2date(sg.getTime()));
                                cqsscLotterySgMapperExt.updateByIssue(sg);
                            }
                        }
                        redisTemplate.opsForValue().set(key, "1", 60l, TimeUnit.SECONDS);
                    } else if (st[0].equals(CaipiaoTypeEnum.XJSSC.getTagType())) {
                        List<XjsscLotterySg> list = JSONObject.parseArray(st[1], XjsscLotterySg.class);
                        for (XjsscLotterySg sg : list) {
                            XjsscLotterySgExample xjsscLotterySgExample = new XjsscLotterySgExample();
                            XjsscLotterySgExample.Criteria criteria = xjsscLotterySgExample.createCriteria();
                            criteria.andIssueEqualTo(sg.getIssue());
                            XjsscLotterySg thisSg = xjsscLotterySgMapper.selectOneByExample(xjsscLotterySgExample);
                            if (thisSg.getWan() == null) {
                                sg.setActualDate(DateUtils.str2date(sg.getTime()));
                                xjsscLotterySgMapperExt.updateByIssue(sg);
                            }
                        }
                        redisTemplate.opsForValue().set(key, "1", 60l, TimeUnit.SECONDS);
                    } else if (st[0].equals(CaipiaoTypeEnum.TJSSC.getTagType())) {
                        List<TjsscLotterySg> list = JSONObject.parseArray(st[1], TjsscLotterySg.class);
                        for (TjsscLotterySg sg : list) {
                            TjsscLotterySgExample tjsscLotterySgExample = new TjsscLotterySgExample();
                            TjsscLotterySgExample.Criteria criteria = tjsscLotterySgExample.createCriteria();
                            criteria.andIssueEqualTo(sg.getIssue());
                            TjsscLotterySg thisSg = tjsscLotterySgMapper.selectOneByExample(tjsscLotterySgExample);
                            if (thisSg.getWan() == null) {
                                sg.setActualDate(DateUtils.str2date(sg.getTime()));
                                tjsscLotterySgMapperExt.updateByIssue(sg);
                            }
                        }
                        redisTemplate.opsForValue().set(key, "1", 60l, TimeUnit.SECONDS);
                    } else if (st[0].equals(CaipiaoTypeEnum.BJPKS.getTagType())) {
                        List<BjpksLotterySg> list = JSONObject.parseArray(st[1], BjpksLotterySg.class);
                        for (BjpksLotterySg sg : list) {
                            BjpksLotterySgExample bjpksLotterySgExample = new BjpksLotterySgExample();
                            BjpksLotterySgExample.Criteria criteria = bjpksLotterySgExample.createCriteria();
                            criteria.andIssueEqualTo(sg.getIssue());
                            BjpksLotterySg thisSg = bjpksLotterySgMapper.selectOneByExample(bjpksLotterySgExample);
                            if (thisSg.getNumber() == null || "".equals(thisSg.getNumber())) {
                                bjpksLotterySgMapperExt.updateByIssue(sg);
                            }
                        }
                        redisTemplate.opsForValue().set(key, "1", 60l, TimeUnit.SECONDS);
                    } else if (st[0].equals(CaipiaoTypeEnum.XYFEIT.getTagType())) {
                        List<XyftLotterySg> list = JSONObject.parseArray(st[1], XyftLotterySg.class);
                        for (XyftLotterySg sg : list) {
                            XyftLotterySgExample xyftLotterySgExample = new XyftLotterySgExample();
                            XyftLotterySgExample.Criteria criteria = xyftLotterySgExample.createCriteria();
                            criteria.andIssueEqualTo(sg.getIssue());
                            XyftLotterySg thisSg = xyftLotterySgMapper.selectOneByExample(xyftLotterySgExample);
                            if (thisSg.getNumber() == null || "".equals(thisSg.getNumber())) {
                                xyftLotterySgMapperExt.updateByIssue(sg);
                            }
                        }
                        redisTemplate.opsForValue().set(key, "1", 60l, TimeUnit.SECONDS);
                    } else if (st[0].equals(CaipiaoTypeEnum.PCDAND.getTagType())) {
                        List<PceggLotterySg> list = JSONObject.parseArray(st[1], PceggLotterySg.class);
                        for (PceggLotterySg sg : list) {
                            PceggLotterySgExample pceggLotterySgExample = new PceggLotterySgExample();
                            PceggLotterySgExample.Criteria criteria = pceggLotterySgExample.createCriteria();
                            criteria.andIssueEqualTo(sg.getIssue());
                            PceggLotterySg thisSg = pceggLotterySgMapper.selectOneByExample(pceggLotterySgExample);
                            if (thisSg.getNumber() == null || "".equals(thisSg.getNumber())) {
                                pceggLotterySgMapperExt.updateByIssue(sg);
                            }
                        }
                        redisTemplate.opsForValue().set(key, "1", 60l, TimeUnit.SECONDS);
                    }
//                    else if (st[0].equals(CaipiaoTypeEnum.DLT.getTagType())) {
//                        List<TcdltLotterySg> list = JSONObject.parseArray(st[1], TcdltLotterySg.class);
//                        for (TcdltLotterySg sg : list) {
//                            TcdltLotterySgExample tcdltLotterySgExample = new TcdltLotterySgExample();
//                            TcdltLotterySgExample.Criteria criteria = tcdltLotterySgExample.createCriteria();
//                            criteria.andIssueEqualTo(sg.getIssue());
//                            TcdltLotterySg thisSg = tcdltLotterySgMapper.selectOneByExample(tcdltLotterySgExample);
//                            if (thisSg.getNumber() == null || thisSg.getNumber().equals("")) {
//                                tcdltLotterySgMapperExt.updateByIssue(sg);
//                            }
//                        }
//                    } else if (st[0].equals(CaipiaoTypeEnum.TCPLW.getTagType())) {
//                        List<TcplwLotterySg> list = JSONObject.parseArray(st[1], TcplwLotterySg.class);
//                        for (TcplwLotterySg sg : list) {
//                            TcplwLotterySgExample tcplwLotterySgExample = new TcplwLotterySgExample();
//                            TcplwLotterySgExample.Criteria criteria = tcplwLotterySgExample.createCriteria();
//                            criteria.andIssueEqualTo(sg.getIssue());
//                            TcplwLotterySg thisSg = tcplwLotterySgMapper.selectOneByExample(tcplwLotterySgExample);
//                            if (thisSg.getNumber() == null) {
//                                tcplwLotterySgMapperExt.updateByIssue(sg);
//                            }
//                        }
//                    } else if (st[0].equals(CaipiaoTypeEnum.TC7XC.getTagType())) {
//                        List<Tc7xcLotterySg> list = JSONObject.parseArray(st[1], Tc7xcLotterySg.class);
//                        for (Tc7xcLotterySg sg : list) {
//                            Tc7xcLotterySgExample tc7xcLotterySgExample = new Tc7xcLotterySgExample();
//                            Tc7xcLotterySgExample.Criteria criteria = tc7xcLotterySgExample.createCriteria();
//                            criteria.andIssueEqualTo(sg.getIssue());
//                            Tc7xcLotterySg thisSg = tc7xcLotterySgMapper.selectOneByExample(tc7xcLotterySgExample);
//                            if (thisSg.getNumber() == null || thisSg.getNumber().equals("")) {
//                                tc7xcLotterySgMapperExt.updateByIssue(sg);
//                            }
//                        }
//                    } else if (st[0].equals(CaipiaoTypeEnum.FCSSQ.getTagType())) {
//                        List<FcssqLotterySg> list = JSONObject.parseArray(st[1], FcssqLotterySg.class);
//                        for (FcssqLotterySg sg : list) {
//                            FcssqLotterySgExample fcssqLotterySgExample = new FcssqLotterySgExample();
//                            FcssqLotterySgExample.Criteria criteria = fcssqLotterySgExample.createCriteria();
//                            criteria.andIssueEqualTo(sg.getIssue());
//                            FcssqLotterySg thisSg = fcssqLotterySgMapper.selectOneByExample(fcssqLotterySgExample);
//                            if (thisSg.getNumber() == null || thisSg.getNumber().equals("")) {
//                                fcssqLotterySgMapperExt.updateByIssue(sg);
//                            }
//                        }
//                    } else if (st[0].equals(CaipiaoTypeEnum.FC3D.getTagType())) {
//                        List<Fc3dLotterySg> list = JSONObject.parseArray(st[1], Fc3dLotterySg.class);
//                        for (Fc3dLotterySg sg : list) {
//                            Fc3dLotterySgExample fc3dLotterySgExample = new Fc3dLotterySgExample();
//                            Fc3dLotterySgExample.Criteria criteria = fc3dLotterySgExample.createCriteria();
//                            criteria.andIssueEqualTo(sg.getIssue());
//                            Fc3dLotterySg thisSg = fc3dLotterySgMapper.selectOneByExample(fc3dLotterySgExample);
//                            if (thisSg.getNumber() == null || thisSg.getNumber().equals("")) {
//                                fc3dLotterySgMapperExt.updateByIssue(sg);
//                            }
//                        }
//                    } else if (st[0].equals(CaipiaoTypeEnum.FC7LC.getTagType())) {
//                        List<Fc7lcLotterySg> list = JSONObject.parseArray(st[1], Fc7lcLotterySg.class);
//                        for (Fc7lcLotterySg sg : list) {
//                            Fc7lcLotterySgExample fc7lcLotterySgExample = new Fc7lcLotterySgExample();
//                            Fc7lcLotterySgExample.Criteria criteria = fc7lcLotterySgExample.createCriteria();
//                            criteria.andIssueEqualTo(sg.getIssue());
//                            Fc7lcLotterySg thisSg = fc7lcLotterySgMapper.selectOneByExample(fc7lcLotterySgExample);
//                            if (thisSg.getNumber() == null || thisSg.getNumber().equals("")) {
//                                fc7lcLotterySgMapperExt.updateByIssue(sg);
//                            }
//                        }
//                    } else if (st[0].equals(CaipiaoTypeEnum.AZNIU.getTagType())) {
//                        List<AuspksLotterySg> list = JSONObject.parseArray(st[1], AuspksLotterySg.class);
//                        for (AuspksLotterySg sg : list) {
//                            AuspksLotterySgExample auspksLotterySgExample = new AuspksLotterySgExample();
//                            AuspksLotterySgExample.Criteria criteria = auspksLotterySgExample.createCriteria();
//                            criteria.andIssueEqualTo(sg.getIssue());
//                            AuspksLotterySg thisSg = auspksLotterySgMapper.selectOneByExample(auspksLotterySgExample);
//                            if (thisSg == null) {
//                                auspksLotterySgMapper.insert(sg);
//                            } else if (thisSg.getNumber() == null || thisSg.getNumber().equals("")) {
//                                auspksLotterySgMapperExt.updateByIssue(sg);
//                            }
//                        }
//                    } else if (st[0].equals(CaipiaoTypeEnum.JSPKFT)) {
//                        List<FtjspksLotterySg> list = JSONObject.parseArray(st[1], FtjspksLotterySg.class);
//                        for (FtjspksLotterySg sg : list) {
//                            FtjspksLotterySgExample ftjspksLotterySgExample = new FtjspksLotterySgExample();
//                            FtjspksLotterySgExample.Criteria criteria = ftjspksLotterySgExample.createCriteria();
//                            criteria.andIssueEqualTo(sg.getIssue());
//                            FtjspksLotterySg thisSg = ftjspksLotterySgMapper.selectOneByExample(ftjspksLotterySgExample);
//                            if (thisSg.getNumber() == null || thisSg.getNumber().equals("")) {
//                                ftJspksLotterySgMapperExt.updateByIssue(sg);
//                            }
//                        }
//                    } else if (st[0].equals(CaipiaoTypeEnum.XYFTFT.getTagType())) {
//                        List<FtxyftLotterySg> list = JSONObject.parseArray(st[1], FtxyftLotterySg.class);
//                        for (FtxyftLotterySg sg : list) {
//                            FtxyftLotterySgExample ftxyftLotterySgExample = new FtxyftLotterySgExample();
//                            FtxyftLotterySgExample.Criteria criteria = ftxyftLotterySgExample.createCriteria();
//                            criteria.andIssueEqualTo(sg.getIssue());
//                            FtxyftLotterySg thisSg = ftxyftLotterySgMapper.selectOneByExample(ftxyftLotterySgExample);
//                            if (thisSg.getNumber() == null || thisSg.getNumber().equals("")) {
//                                ftXyftLotterySgMapperExt.updateByIssue(sg);
//                            }
//                        }
//                    } else if (st[0].equals(CaipiaoTypeEnum.JSSSCFT.getTagType())) {
//                        List<FtjssscLotterySg> list = JSONObject.parseArray(st[1], FtjssscLotterySg.class);
//                        for (FtjssscLotterySg sg : list) {
//                            FtjssscLotterySgExample ftjssscLotterySgExample = new FtjssscLotterySgExample();
//                            FtjssscLotterySgExample.Criteria criteria = ftjssscLotterySgExample.createCriteria();
//                            criteria.andIssueEqualTo(sg.getIssue());
//                            FtjssscLotterySg thisSg = ftjssscLotterySgMapper.selectOneByExample(ftjssscLotterySgExample);
//                            if (thisSg.getNumber() == null || thisSg.getNumber().equals("")) {
//                                ftJssscLotterySgMapperExt.updateByIssue(sg);
//                            }
//                        }
//                    }
                    else if (st[0].equals(CaipiaoTypeEnum.AUSACT.getTagType())) {
                        List<AusactLotterySg> list = JSONObject.parseArray(st[1], AusactLotterySg.class);
                        for (AusactLotterySg sg : list) {
                            AusactLotterySgExample ausactLotterySgExample = new AusactLotterySgExample();
                            AusactLotterySgExample.Criteria criteria = ausactLotterySgExample.createCriteria();
                            criteria.andIssueEqualTo(sg.getIssue());
                            AusactLotterySg thisSg = ausactLotterySgMapper.selectOneByExample(ausactLotterySgExample);
                            if (thisSg == null) {
                                ausactLotterySgMapper.insert(sg);
                            } else if (thisSg.getNumber() == null || "".equals(thisSg.getNumber())) {
                                ausactLotterySgMapperExt.updateByIssue(sg);
                            }
                        }
                        redisTemplate.opsForValue().set(key, "1", 60l, TimeUnit.SECONDS);
                    } else if (st[0].equals(CaipiaoTypeEnum.AUSSSC.getTagType())) {
                        List<AussscLotterySg> list = JSONObject.parseArray(st[1], AussscLotterySg.class);
                        for (AussscLotterySg sg : list) {
                            AussscLotterySgExample aussscLotterySgExample = new AussscLotterySgExample();
                            AussscLotterySgExample.Criteria criteria = aussscLotterySgExample.createCriteria();
                            criteria.andIssueEqualTo(sg.getIssue());
                            AussscLotterySg thisSg = aussscLotterySgMapper.selectOneByExample(aussscLotterySgExample);
                            if (thisSg == null) {
                                aussscLotterySgMapper.insert(sg);
                            } else if (thisSg.getNumber() == null || "".equals(thisSg.getNumber())) {
                                aussscLotterySgMapperExt.updateByIssue(sg);
                            }
                        }
                        redisTemplate.opsForValue().set(key, "1", 60l, TimeUnit.SECONDS);
                    }

                }
            }
        } catch (Exception e) {
            logger.error("检查sg出错", e);
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * 结算sg 是否在数据库，如不在，则更新
     * 检查私彩
     * @param message 消息内容【期号】
     */
    @JmsListener(destination = ActiveMQConfig.PLATFORM_TOPIC_ALL_LOTTERY_SG, containerFactory = "jmsListenerContainerTopicDurable")
    public void processMsgAzActSc(String message) throws Exception {
        logger.info("检查sg开始  " + message);
        // 获取一个时间戳
        String st[] = message.split("#");

        // 获取唯一
        String key = ActiveMQConfig.PLATFORM_TOPIC_ALL_LOTTERY_SG + st[0];
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + "lockl");
        try {
            // 写锁（等待时间1s，超时时间10S[自动解锁]，单位：秒）【设定超时时间，超时后自动释放锁，防止死锁】
            boolean bool = lock.writeLock().tryLock(3, 30, TimeUnit.SECONDS);
            if (bool) {
                if (redisTemplate.opsForValue().get(key) == null) {
                    if (st[0].equals(CaipiaoTypeEnum.FIVEPKS.getTagType())) {
                        List<FivebjpksLotterySg> list = JSONObject.parseArray(st[1], FivebjpksLotterySg.class);
                        for (FivebjpksLotterySg sg : list) {
                            FivebjpksLotterySgExample fivebjpksLotterySgExample = new FivebjpksLotterySgExample();
                            FivebjpksLotterySgExample.Criteria criteria = fivebjpksLotterySgExample.createCriteria();
                            criteria.andIssueEqualTo(sg.getIssue());
                            FivebjpksLotterySg thisSg = fivebjpksLotterySgMapper.selectOneByExample(fivebjpksLotterySgExample);
                            if (thisSg.getNumber() == null || "".equals(thisSg.getNumber())) {
                                fivebjpksLotterySgMapperExt.updateByIssue(sg);
                            }
                        }
                        redisTemplate.opsForValue().set(key, "1", 60l, TimeUnit.SECONDS);
                    } else if (st[0].equals(CaipiaoTypeEnum.JSPKS.getTagType())) {
                        List<JsbjpksLotterySg> list = JSONObject.parseArray(st[1], JsbjpksLotterySg.class);
                        for (JsbjpksLotterySg sg : list) {
                            JsbjpksLotterySgExample jsbjpksLotterySgExample = new JsbjpksLotterySgExample();
                            JsbjpksLotterySgExample.Criteria criteria = jsbjpksLotterySgExample.createCriteria();
                            criteria.andIssueEqualTo(sg.getIssue());
                            JsbjpksLotterySg thisSg = jsbjpksLotterySgMapper.selectOneByExample(jsbjpksLotterySgExample);
                            if (thisSg.getNumber() == null || "".equals(thisSg.getNumber())) {
                                jsbjpksLotterySgMapperExt.updateByIssue(sg);
                            }
                        }
                        redisTemplate.opsForValue().set(key, "1", 60l, TimeUnit.SECONDS);
                    } else if (st[0].equals(CaipiaoTypeEnum.TENSSC.getTagType())) {
                        List<TensscLotterySg> list = JSONObject.parseArray(st[1], TensscLotterySg.class);
                        for (TensscLotterySg sg : list) {
                            TensscLotterySgExample tensscLotterySgExample = new TensscLotterySgExample();
                            TensscLotterySgExample.Criteria criteria = tensscLotterySgExample.createCriteria();
                            criteria.andIssueEqualTo(sg.getIssue());
                            TensscLotterySg thisSg = tensscLotterySgMapper.selectOneByExample(tensscLotterySgExample);
                            if (thisSg.getWan() == null) {
                                sg.setActualDate(DateUtils.str2date(sg.getTime()));
                                tensscLotterySgMapperExt.updateByIssue(sg);
                            }
                        }
                        redisTemplate.opsForValue().set(key, "1", 60l, TimeUnit.SECONDS);
                    } else if (st[0].equals(CaipiaoTypeEnum.FIVESSC.getTagType())) {
                        List<FivesscLotterySg> list = JSONObject.parseArray(st[1], FivesscLotterySg.class);
                        for (FivesscLotterySg sg : list) {
                            FivesscLotterySgExample fivesscLotterySgExample = new FivesscLotterySgExample();
                            FivesscLotterySgExample.Criteria criteria = fivesscLotterySgExample.createCriteria();
                            criteria.andIssueEqualTo(sg.getIssue());
                            FivesscLotterySg thisSg = fivesscLotterySgMapper.selectOneByExample(fivesscLotterySgExample);
                            if (thisSg.getWan() == null) {
                                sg.setActualDate(DateUtils.str2date(sg.getTime()));
                                fivesscLotterySgMapperExt.updateByIssue(sg);
                            }
                        }
                        redisTemplate.opsForValue().set(key, "1", 60l, TimeUnit.SECONDS);
                    } else if (st[0].equals(CaipiaoTypeEnum.JSSSC.getTagType())) {
                        List<JssscLotterySg> list = JSONObject.parseArray(st[1], JssscLotterySg.class);
                        for (JssscLotterySg sg : list) {
                            JssscLotterySgExample jssscLotterySgExample = new JssscLotterySgExample();
                            JssscLotterySgExample.Criteria criteria = jssscLotterySgExample.createCriteria();
                            criteria.andIssueEqualTo(sg.getIssue());
                            JssscLotterySg thisSg = jssscLotterySgMapper.selectOneByExample(jssscLotterySgExample);
                            if (thisSg.getWan() == null) {
                                sg.setActualDate(DateUtils.str2date(sg.getTime()));
                                jssscLotterySgMapperExt.updateByIssue(sg);
                            }
                        }
                        redisTemplate.opsForValue().set(key, "1", 60l, TimeUnit.SECONDS);
                    } else if (st[0].equals(CaipiaoTypeEnum.ONELHC.getTagType())) {
                        List<OnelhcLotterySg> list = JSONObject.parseArray(st[1], OnelhcLotterySg.class);
                        for (OnelhcLotterySg sg : list) {
                            OnelhcLotterySgExample onelhcLotterySgExample = new OnelhcLotterySgExample();
                            OnelhcLotterySgExample.Criteria criteria = onelhcLotterySgExample.createCriteria();
                            criteria.andIssueEqualTo(sg.getIssue());
                            OnelhcLotterySg thisSg = onelhcLotterySgMapper.selectOneByExample(onelhcLotterySgExample);
                            if (thisSg.getNumber() == null || "".equals(thisSg.getNumber())) {
                                onelhcLotterySgMapperExt.updateByIssue(sg);
                            }
                        }
                        redisTemplate.opsForValue().set(key, "1", 60l, TimeUnit.SECONDS);
                    } else if (st[0].equals(CaipiaoTypeEnum.FIVELHC.getTagType())) {
                        List<FivelhcLotterySg> list = JSONObject.parseArray(st[1], FivelhcLotterySg.class);
                        for (FivelhcLotterySg sg : list) {
                            FivelhcLotterySgExample fivelhcLotterySgExample = new FivelhcLotterySgExample();
                            FivelhcLotterySgExample.Criteria criteria = fivelhcLotterySgExample.createCriteria();
                            criteria.andIssueEqualTo(sg.getIssue());
                            FivelhcLotterySg thisSg = fivelhcLotterySgMapper.selectOneByExample(fivelhcLotterySgExample);
                            if (thisSg.getNumber() == null || "".equals(thisSg.getNumber())) {
                                fivelhcLotterySgMapperExt.updateByIssue(sg);
                            }
                        }
                        redisTemplate.opsForValue().set(key, "1", 60l, TimeUnit.SECONDS);
                    } else if (st[0].equals(CaipiaoTypeEnum.AMLHC.getTagType())) {
                        List<AmlhcLotterySg> list = JSONObject.parseArray(st[1], AmlhcLotterySg.class);
                        for (AmlhcLotterySg sg : list) {
                            AmlhcLotterySgExample sslhcLotterySgExample = new AmlhcLotterySgExample();
                            AmlhcLotterySgExample.Criteria criteria = sslhcLotterySgExample.createCriteria();
                            criteria.andIssueEqualTo(sg.getIssue());
                            AmlhcLotterySg thisSg = amlhcLotterySgMapper.selectOneByExample(sslhcLotterySgExample);
                            if (thisSg.getNumber() == null || "".equals(thisSg.getNumber())) {
                                amlhcLotterySgMapperExt.updateByIssue(sg);
                            }
                        }
                        redisTemplate.opsForValue().set(key, "1", 60l, TimeUnit.SECONDS);
                    } else if (st[0].equals(CaipiaoTypeEnum.TENPKS.getTagType())) {
                        List<TenbjpksLotterySg> list = JSONObject.parseArray(st[1], TenbjpksLotterySg.class);
                        for (TenbjpksLotterySg sg : list) {
                            TenbjpksLotterySgExample tenbjpksLotterySgExample = new TenbjpksLotterySgExample();
                            TenbjpksLotterySgExample.Criteria criteria = tenbjpksLotterySgExample.createCriteria();
                            criteria.andIssueEqualTo(sg.getIssue());
                            TenbjpksLotterySg thisSg = tenbjpksLotterySgMapper.selectOneByExample(tenbjpksLotterySgExample);
                            if (thisSg.getNumber() == null || "".equals(thisSg.getNumber())) {
                                tenbjpksLotterySgMapperExt.updateByIssue(sg);
                            }
                        }
                        redisTemplate.opsForValue().set(key, "1", 60l, TimeUnit.SECONDS);
                    } else  if (st[0].equals(CaipiaoTypeEnum.TXFFC.getTagType())) {
                        List<TxffcLotterySg> list = JSONObject.parseArray(st[1], TxffcLotterySg.class);
                        for (TxffcLotterySg sg : list) {
                            TxffcLotterySgExample txffcLotterySgExample = new TxffcLotterySgExample();
                            TxffcLotterySgExample.Criteria criteria = txffcLotterySgExample.createCriteria();
                            criteria.andIssueEqualTo(sg.getIssue());
                            TxffcLotterySg thisSg = txffcLotterySgMapper.selectOneByExample(txffcLotterySgExample);
                            if (thisSg.getWan() == null) {
                                txffcLotterySgMapperExt.updateByIssue(sg);
                            }
                        }
                        redisTemplate.opsForValue().set(key, "1", 60l, TimeUnit.SECONDS);
                    }
//                    else if (st[0].equals(CaipiaoTypeEnum.DLT.getTagType())) {
//                        List<TcdltLotterySg> list = JSONObject.parseArray(st[1], TcdltLotterySg.class);
//                        for (TcdltLotterySg sg : list) {
//                            TcdltLotterySgExample tcdltLotterySgExample = new TcdltLotterySgExample();
//                            TcdltLotterySgExample.Criteria criteria = tcdltLotterySgExample.createCriteria();
//                            criteria.andIssueEqualTo(sg.getIssue());
//                            TcdltLotterySg thisSg = tcdltLotterySgMapper.selectOneByExample(tcdltLotterySgExample);
//                            if (thisSg.getNumber() == null || thisSg.getNumber().equals("")) {
//                                tcdltLotterySgMapperExt.updateByIssue(sg);
//                            }
//                        }
//                    } else if (st[0].equals(CaipiaoTypeEnum.TCPLW.getTagType())) {
//                        List<TcplwLotterySg> list = JSONObject.parseArray(st[1], TcplwLotterySg.class);
//                        for (TcplwLotterySg sg : list) {
//                            TcplwLotterySgExample tcplwLotterySgExample = new TcplwLotterySgExample();
//                            TcplwLotterySgExample.Criteria criteria = tcplwLotterySgExample.createCriteria();
//                            criteria.andIssueEqualTo(sg.getIssue());
//                            TcplwLotterySg thisSg = tcplwLotterySgMapper.selectOneByExample(tcplwLotterySgExample);
//                            if (thisSg.getNumber() == null) {
//                                tcplwLotterySgMapperExt.updateByIssue(sg);
//                            }
//                        }
//                    } else if (st[0].equals(CaipiaoTypeEnum.TC7XC.getTagType())) {
//                        List<Tc7xcLotterySg> list = JSONObject.parseArray(st[1], Tc7xcLotterySg.class);
//                        for (Tc7xcLotterySg sg : list) {
//                            Tc7xcLotterySgExample tc7xcLotterySgExample = new Tc7xcLotterySgExample();
//                            Tc7xcLotterySgExample.Criteria criteria = tc7xcLotterySgExample.createCriteria();
//                            criteria.andIssueEqualTo(sg.getIssue());
//                            Tc7xcLotterySg thisSg = tc7xcLotterySgMapper.selectOneByExample(tc7xcLotterySgExample);
//                            if (thisSg.getNumber() == null || thisSg.getNumber().equals("")) {
//                                tc7xcLotterySgMapperExt.updateByIssue(sg);
//                            }
//                        }
//                    } else if (st[0].equals(CaipiaoTypeEnum.FCSSQ.getTagType())) {
//                        List<FcssqLotterySg> list = JSONObject.parseArray(st[1], FcssqLotterySg.class);
//                        for (FcssqLotterySg sg : list) {
//                            FcssqLotterySgExample fcssqLotterySgExample = new FcssqLotterySgExample();
//                            FcssqLotterySgExample.Criteria criteria = fcssqLotterySgExample.createCriteria();
//                            criteria.andIssueEqualTo(sg.getIssue());
//                            FcssqLotterySg thisSg = fcssqLotterySgMapper.selectOneByExample(fcssqLotterySgExample);
//                            if (thisSg.getNumber() == null || thisSg.getNumber().equals("")) {
//                                fcssqLotterySgMapperExt.updateByIssue(sg);
//                            }
//                        }
//                    } else if (st[0].equals(CaipiaoTypeEnum.FC3D.getTagType())) {
//                        List<Fc3dLotterySg> list = JSONObject.parseArray(st[1], Fc3dLotterySg.class);
//                        for (Fc3dLotterySg sg : list) {
//                            Fc3dLotterySgExample fc3dLotterySgExample = new Fc3dLotterySgExample();
//                            Fc3dLotterySgExample.Criteria criteria = fc3dLotterySgExample.createCriteria();
//                            criteria.andIssueEqualTo(sg.getIssue());
//                            Fc3dLotterySg thisSg = fc3dLotterySgMapper.selectOneByExample(fc3dLotterySgExample);
//                            if (thisSg.getNumber() == null || thisSg.getNumber().equals("")) {
//                                fc3dLotterySgMapperExt.updateByIssue(sg);
//                            }
//                        }
//                    } else if (st[0].equals(CaipiaoTypeEnum.FC7LC.getTagType())) {
//                        List<Fc7lcLotterySg> list = JSONObject.parseArray(st[1], Fc7lcLotterySg.class);
//                        for (Fc7lcLotterySg sg : list) {
//                            Fc7lcLotterySgExample fc7lcLotterySgExample = new Fc7lcLotterySgExample();
//                            Fc7lcLotterySgExample.Criteria criteria = fc7lcLotterySgExample.createCriteria();
//                            criteria.andIssueEqualTo(sg.getIssue());
//                            Fc7lcLotterySg thisSg = fc7lcLotterySgMapper.selectOneByExample(fc7lcLotterySgExample);
//                            if (thisSg.getNumber() == null || thisSg.getNumber().equals("")) {
//                                fc7lcLotterySgMapperExt.updateByIssue(sg);
//                            }
//                        }
//                    } else if (st[0].equals(CaipiaoTypeEnum.AZNIU.getTagType())) {
//                        List<AuspksLotterySg> list = JSONObject.parseArray(st[1], AuspksLotterySg.class);
//                        for (AuspksLotterySg sg : list) {
//                            AuspksLotterySgExample auspksLotterySgExample = new AuspksLotterySgExample();
//                            AuspksLotterySgExample.Criteria criteria = auspksLotterySgExample.createCriteria();
//                            criteria.andIssueEqualTo(sg.getIssue());
//                            AuspksLotterySg thisSg = auspksLotterySgMapper.selectOneByExample(auspksLotterySgExample);
//                            if (thisSg == null) {
//                                auspksLotterySgMapper.insert(sg);
//                            } else if (thisSg.getNumber() == null || thisSg.getNumber().equals("")) {
//                                auspksLotterySgMapperExt.updateByIssue(sg);
//                            }
//                        }
//                    } else if (st[0].equals(CaipiaoTypeEnum.JSPKFT)) {
//                        List<FtjspksLotterySg> list = JSONObject.parseArray(st[1], FtjspksLotterySg.class);
//                        for (FtjspksLotterySg sg : list) {
//                            FtjspksLotterySgExample ftjspksLotterySgExample = new FtjspksLotterySgExample();
//                            FtjspksLotterySgExample.Criteria criteria = ftjspksLotterySgExample.createCriteria();
//                            criteria.andIssueEqualTo(sg.getIssue());
//                            FtjspksLotterySg thisSg = ftjspksLotterySgMapper.selectOneByExample(ftjspksLotterySgExample);
//                            if (thisSg.getNumber() == null || thisSg.getNumber().equals("")) {
//                                ftJspksLotterySgMapperExt.updateByIssue(sg);
//                            }
//                        }
//                    } else if (st[0].equals(CaipiaoTypeEnum.XYFTFT.getTagType())) {
//                        List<FtxyftLotterySg> list = JSONObject.parseArray(st[1], FtxyftLotterySg.class);
//                        for (FtxyftLotterySg sg : list) {
//                            FtxyftLotterySgExample ftxyftLotterySgExample = new FtxyftLotterySgExample();
//                            FtxyftLotterySgExample.Criteria criteria = ftxyftLotterySgExample.createCriteria();
//                            criteria.andIssueEqualTo(sg.getIssue());
//                            FtxyftLotterySg thisSg = ftxyftLotterySgMapper.selectOneByExample(ftxyftLotterySgExample);
//                            if (thisSg.getNumber() == null || thisSg.getNumber().equals("")) {
//                                ftXyftLotterySgMapperExt.updateByIssue(sg);
//                            }
//                        }
//                    } else if (st[0].equals(CaipiaoTypeEnum.JSSSCFT.getTagType())) {
//                        List<FtjssscLotterySg> list = JSONObject.parseArray(st[1], FtjssscLotterySg.class);
//                        for (FtjssscLotterySg sg : list) {
//                            FtjssscLotterySgExample ftjssscLotterySgExample = new FtjssscLotterySgExample();
//                            FtjssscLotterySgExample.Criteria criteria = ftjssscLotterySgExample.createCriteria();
//                            criteria.andIssueEqualTo(sg.getIssue());
//                            FtjssscLotterySg thisSg = ftjssscLotterySgMapper.selectOneByExample(ftjssscLotterySgExample);
//                            if (thisSg.getNumber() == null || thisSg.getNumber().equals("")) {
//                                ftJssscLotterySgMapperExt.updateByIssue(sg);
//                            }
//                        }
//                    }

                }
            }
        } catch (Exception e) {
            logger.error("检查sg出错", e);
        } finally {
            lock.writeLock().unlock();
        }

    }


}

