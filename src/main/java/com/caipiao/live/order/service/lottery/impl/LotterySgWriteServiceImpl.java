package com.caipiao.live.order.service.lottery.impl;


import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.model.common.ResultInfo;
import com.caipiao.live.common.mybatis.entity.*;
import com.caipiao.live.common.mybatis.mapper.*;
import com.caipiao.live.common.util.StringUtils;
import com.caipiao.live.common.util.lottery.FanTanCalculationUtils;
import com.caipiao.live.order.service.lottery.LotterySgWriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * @Author: xiaomi
 * @CreateDate: 2018/12/10$ 18:19$
 * @Version: 1.0
 */
@Service
public class LotterySgWriteServiceImpl implements LotterySgWriteService {

    @Autowired
    private BjpksLotterySgMapper bjpksLotterySgMapper;
    @Autowired
    private BjpksKillNumberMapper bjpksKillNumberMapper;
    @Autowired
    private TenbjpksLotterySgMapper tenbjpksLotterySgMapper;
    @Autowired
    private FivebjpksLotterySgMapper fivebjpksLotterySgMapper;
    @Autowired
    private JsbjpksLotterySgMapper jsbjpksLotterySgMapper;
    @Autowired
    private XyftLotterySgMapper xyftLotterySgMapper;
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
    private TxffcLotterySgMapper txffcLotterySgMapper;
    @Autowired
    private LhcLotterySgMapper lhcLotterySgMapper;
    @Autowired
    private PceggLotterySgMapper pcegglotterySgMapper;
    @Autowired
    private TcdltLotterySgMapper tcdltlotterySgMapper;
    @Autowired
    private TcplwLotterySgMapper tcplwlotterySgMapper;
    @Autowired
    private Tc7xcLotterySgMapper tc7xclotterySgMapper;
    @Autowired
    private FcssqLotterySgMapper fcssqlotterySgMapper;
    @Autowired
    private Fc3dLotterySgMapper fc3dlotterySgMapper;
    @Autowired
    private Fc7lcLotterySgMapper fc7lclotterySgMapper;
    @Autowired
    private AuspksLotterySgMapper auspkslotterySgMapper;
    @Autowired
    private FtjspksLotterySgMapper ftjspkslotterySgMapper;
    @Autowired
    private FtxyftLotterySgMapper ftxyftlotterySgMapper;
    @Autowired
    private FtjssscLotterySgMapper ftjsssclotterySgMapper;
    @Autowired
    private AusactLotterySgMapper ausactlotterySgMapper;
    @Autowired
    private AussscLotterySgMapper ausssclotterySgMapper;
    @Autowired
    private AzksLotterySgMapper azkslotterySgMapper;
    @Autowired
    private DzksLotterySgMapper dzkslotterySgMapper;
    @Autowired
    private DzpceggLotterySgMapper dzpcegglotterySgMapper;
    @Autowired
    private DzxyftLotterySgMapper dzxyftlotterySgMapper;
    @Autowired
    private XjplhcLotterySgMapper xjplhclotterySgMapper;

    @Override
    public ResultInfo<Integer> changeNumber(Integer lotteryId, String issue, String number) {
        if (StringUtils.isEmpty(issue) || StringUtils.isEmpty(number)) {
            return ResultInfo.ok(3);
        }
        // ????????????????????????
        boolean isTrue = numberIsTrue(lotteryId, number);
        if (!isTrue) {
            return ResultInfo.error("???????????????????????????");
        }

        String[] num = number.split(",");
        //?????????????????????
        if (lotteryId == 1902) { //?????????????????????F1??????
            lotteryId = 2203;
        }
        switch (lotteryId) {
            case 1101: //???????????????
                //????????????????????????????????????????????????
                CqsscLotterySgExample example = new CqsscLotterySgExample();
                CqsscLotterySgExample.Criteria criteria = example.createCriteria();
                criteria.andIssueEqualTo(issue);
                CqsscLotterySg cqsscLotterySg = cqsscLotterySgMapper.selectOneByExample(example);
                if (cqsscLotterySg == null || cqsscLotterySg.getOpenStatus().equals(Constants.WAIT)) {
                    return ResultInfo.error("???????????????????????????????????????");
                }
                cqsscLotterySg.setWan(Integer.parseInt(num[0]));
                cqsscLotterySg.setQian(Integer.parseInt(num[1]));
                cqsscLotterySg.setBai(Integer.parseInt(num[2]));
                cqsscLotterySg.setShi(Integer.parseInt(num[3]));
                cqsscLotterySg.setGe(Integer.parseInt(num[4]));
                cqsscLotterySg.setKcwNumber(number);
                cqsscLotterySg.setCpkNumber(number);
                cqsscLotterySgMapper.updateByPrimaryKeySelective(cqsscLotterySg);

                // ????????????????????????
//                CqsscRecommendExample recommendExample = new CqsscRecommendExample();
//                CqsscRecommendExample.Criteria reExampleCriteria = recommendExample.createCriteria();
//                reExampleCriteria.andIssueEqualTo(issue);
//                CqsscRecommend cqsscRecommend = cqsscRecommendMapper.selectOneByExample(recommendExample);
//                if (cqsscRecommend != null) {
//                    cqsscRecommend.setOpenNumber(number);
//                    cqsscRecommendMapper.updateByPrimaryKeySelective(cqsscRecommend);
//                }
//
//                // ????????????????????????
//                CqsscKillNumberExample cqsscKillNumberExample = new CqsscKillNumberExample();
//                CqsscKillNumberExample.Criteria cqk = cqsscKillNumberExample.createCriteria();
//                cqk.andIssueEqualTo(issue);
//                CqsscKillNumber cqsscKillNumber = cqsscKillNumberMapper.selectOneByExample(cqsscKillNumberExample);
//                if (cqsscKillNumber != null) {
//                    cqsscKillNumber.setOpenOne(Integer.parseInt(num[0]));
//                    cqsscKillNumber.setOpenTwo(Integer.parseInt(num[1]));
//                    cqsscKillNumber.setOpenThree(Integer.parseInt(num[2]));
//                    cqsscKillNumber.setOpenFour(Integer.parseInt(num[3]));
//                    cqsscKillNumber.setOpenFive(Integer.parseInt(num[4]));
//                    cqsscKillNumberMapper.updateByPrimaryKeySelective(cqsscKillNumber);
//                }
//                break;
            case 1102: //???????????????
                //??????????????????????????????
                XjsscLotterySgExample xjsscExample = new XjsscLotterySgExample();
                XjsscLotterySgExample.Criteria criteria1 = xjsscExample.createCriteria();
                criteria1.andIssueEqualTo(issue);
                XjsscLotterySg xjsscLotterySg = xjsscLotterySgMapper.selectOneByExample(xjsscExample);
                if (xjsscLotterySg == null || xjsscLotterySg.getOpenStatus().equals(Constants.WAIT)) {
                    return ResultInfo.error("???????????????????????????????????????");
                }
                xjsscLotterySg.setWan(Integer.parseInt(num[0]));
                xjsscLotterySg.setQian(Integer.parseInt(num[1]));
                xjsscLotterySg.setBai(Integer.parseInt(num[2]));
                xjsscLotterySg.setShi(Integer.parseInt(num[3]));
                xjsscLotterySg.setGe(Integer.parseInt(num[4]));
                xjsscLotterySg.setKcwNumber(number);
                xjsscLotterySg.setCpkNumber(number);
                xjsscLotterySgMapper.updateByPrimaryKeySelective(xjsscLotterySg);

//                //???????????????????????????????????????
//                XjsscRecommendExample xjsscRecommendExample = new XjsscRecommendExample();
//                XjsscRecommendExample.Criteria xj = xjsscRecommendExample.createCriteria();
//                xj.andIssueEqualTo(issue);
//                XjsscRecommend xjsscRecommend = xjsscRecommendMapper.selectOneByExample(xjsscRecommendExample);
//                if (xjsscRecommend != null) {
//                    //??????????????????
//                    xjsscRecommend.setOpenNumber(number);
//                    xjsscRecommendMapper.updateByPrimaryKeySelective(xjsscRecommend);
//                }
//
//                XjsscKillNumberExample xjsscKillNumberExample = new XjsscKillNumberExample();
//                XjsscKillNumberExample.Criteria xjk = xjsscKillNumberExample.createCriteria();
//                xjk.andIssueEqualTo(issue);
//                XjsscKillNumber xjsscKillNumber = xjsscKillNumberMapper.selectOneByExample(xjsscKillNumberExample);
//                if (xjsscKillNumber != null) {
//                    //??????????????????
//                    xjsscKillNumber.setOpenOne(Integer.parseInt(num[0]));
//                    xjsscKillNumber.setOpenTwo(Integer.parseInt(num[1]));
//                    xjsscKillNumber.setOpenThree(Integer.parseInt(num[2]));
//                    xjsscKillNumber.setOpenFour(Integer.parseInt(num[3]));
//                    xjsscKillNumber.setOpenFive(Integer.parseInt(num[4]));
//                    xjsscKillNumberMapper.updateByPrimaryKeySelective(xjsscKillNumber);
//                }
                break;
            case 1103: //???????????????
                //??????????????????????????????
                TjsscLotterySgExample tjsscExample = new TjsscLotterySgExample();
                TjsscLotterySgExample.Criteria criteriatj = tjsscExample.createCriteria();
                criteriatj.andIssueEqualTo(issue);
                TjsscLotterySg tjsscLotterySg = tjsscLotterySgMapper.selectOneByExample(tjsscExample);
                if (tjsscLotterySg == null || tjsscLotterySg.getOpenStatus().equals(Constants.WAIT)) {
                    return ResultInfo.error("???????????????????????????????????????");
                }
                tjsscLotterySg.setWan(Integer.parseInt(num[0]));
                tjsscLotterySg.setQian(Integer.parseInt(num[1]));
                tjsscLotterySg.setBai(Integer.parseInt(num[2]));
                tjsscLotterySg.setShi(Integer.parseInt(num[3]));
                tjsscLotterySg.setGe(Integer.parseInt(num[4]));
                tjsscLotterySg.setKcwNumber(number);
                tjsscLotterySg.setCpkNumber(number);
                tjsscLotterySgMapper.updateByPrimaryKeySelective(tjsscLotterySg);

//                //???????????????????????????????????????
//                TjsscRecommendExample tjsscRecommendExample = new TjsscRecommendExample();
//                TjsscRecommendExample.Criteria tj = tjsscRecommendExample.createCriteria();
//                tj.andIssueEqualTo(issue);
//                TjsscRecommend tjsscRecommend = tjsscRecommendMapper.selectOneByExample(tjsscRecommendExample);
//                if (tjsscRecommend != null) {
//                    //??????????????????
//                    tjsscRecommend.setOpenNumber(number);
//                    tjsscRecommendMapper.updateByPrimaryKeySelective(tjsscRecommend);
//                }
//
//                TjsscKillNumberExample tjsscKillNumberExample = new TjsscKillNumberExample();
//                TjsscKillNumberExample.Criteria tjk = tjsscKillNumberExample.createCriteria();
//                tjk.andIssueEqualTo(issue);
//                TjsscKillNumber tjsscKillNumber = tjsscKillNumberMapper.selectOneByExample(tjsscKillNumberExample);
//                if (tjsscKillNumber != null) {
//                    //??????????????????
//                    tjsscKillNumber.setOpenOne(Integer.parseInt(num[0]));
//                    tjsscKillNumber.setOpenTwo(Integer.parseInt(num[1]));
//                    tjsscKillNumber.setOpenThree(Integer.parseInt(num[2]));
//                    tjsscKillNumber.setOpenFour(Integer.parseInt(num[3]));
//                    tjsscKillNumber.setOpenFive(Integer.parseInt(num[4]));
//                    tjsscKillNumberMapper.updateByPrimaryKeySelective(tjsscKillNumber);
//                }
                break;
            case 1104: //10????????????
                //??????????????????????????????
                TensscLotterySgExample tensscExample = new TensscLotterySgExample();
                TensscLotterySgExample.Criteria criteriaten = tensscExample.createCriteria();
                criteriaten.andIssueEqualTo(issue);
                TensscLotterySg tensscLotterySg = tensscLotterySgMapper.selectOneByExample(tensscExample);
                if (tensscLotterySg == null || tensscLotterySg.getOpenStatus().equals(Constants.WAIT)) {
                    return ResultInfo.error("???????????????????????????????????????");
                }
                tensscLotterySg.setWan(Integer.parseInt(num[0]));
                tensscLotterySg.setQian(Integer.parseInt(num[1]));
                tensscLotterySg.setBai(Integer.parseInt(num[2]));
                tensscLotterySg.setShi(Integer.parseInt(num[3]));
                tensscLotterySg.setGe(Integer.parseInt(num[4]));
                tensscLotterySg.setNumber(number);
                tensscLotterySgMapper.updateByPrimaryKeySelective(tensscLotterySg);
                break;
            case 1105: //5????????????
                //??????????????????????????????
                FivesscLotterySgExample fivesscExample = new FivesscLotterySgExample();
                FivesscLotterySgExample.Criteria criteriafive = fivesscExample.createCriteria();
                criteriafive.andIssueEqualTo(issue);
                FivesscLotterySg fivesscLotterySg = fivesscLotterySgMapper.selectOneByExample(fivesscExample);
                if (fivesscLotterySg == null || fivesscLotterySg.getOpenStatus().equals(Constants.WAIT)) {
                    return ResultInfo.error("???????????????????????????????????????");
                }
                fivesscLotterySg.setWan(Integer.parseInt(num[0]));
                fivesscLotterySg.setQian(Integer.parseInt(num[1]));
                fivesscLotterySg.setBai(Integer.parseInt(num[2]));
                fivesscLotterySg.setShi(Integer.parseInt(num[3]));
                fivesscLotterySg.setGe(Integer.parseInt(num[4]));
                fivesscLotterySg.setNumber(number);
                fivesscLotterySgMapper.updateByPrimaryKeySelective(fivesscLotterySg);
                break;
            case 1106: //???????????????
                //??????????????????????????????
                JssscLotterySgExample jssscExample = new JssscLotterySgExample();
                JssscLotterySgExample.Criteria criteriajs = jssscExample.createCriteria();
                criteriajs.andIssueEqualTo(issue);
                JssscLotterySg jssscLotterySg = jssscLotterySgMapper.selectOneByExample(jssscExample);
                if (jssscLotterySg == null || jssscLotterySg.getOpenStatus().equals(Constants.WAIT)) {
                    return ResultInfo.error("???????????????????????????????????????");
                }
                jssscLotterySg.setWan(Integer.parseInt(num[0]));
                jssscLotterySg.setQian(Integer.parseInt(num[1]));
                jssscLotterySg.setBai(Integer.parseInt(num[2]));
                jssscLotterySg.setShi(Integer.parseInt(num[3]));
                jssscLotterySg.setGe(Integer.parseInt(num[4]));
                jssscLotterySg.setNumber(number);
                jssscLotterySgMapper.updateByPrimaryKeySelective(jssscLotterySg);
                break;
            case 1202: //???????????????
                //??????????????????????????????
                OnelhcLotterySgExample onelhcExample = new OnelhcLotterySgExample();
                OnelhcLotterySgExample.Criteria criteriaonelhc = onelhcExample.createCriteria();
                criteriaonelhc.andIssueEqualTo(issue);
                OnelhcLotterySg onelhcLotterySg = onelhcLotterySgMapper.selectOneByExample(onelhcExample);
                if (onelhcLotterySg == null || onelhcLotterySg.getOpenStatus().equals(Constants.WAIT)) {
                    return ResultInfo.error("???????????????????????????????????????");
                }
                onelhcLotterySg.setNumber(number);
                onelhcLotterySgMapper.updateByPrimaryKeySelective(onelhcLotterySg);
                break;
            case 1203: //5????????????
                //??????????????????????????????
                FivelhcLotterySgExample fivelhcExample = new FivelhcLotterySgExample();
                FivelhcLotterySgExample.Criteria criteriafivelhc = fivelhcExample.createCriteria();
                criteriafivelhc.andIssueEqualTo(issue);
                FivelhcLotterySg fivelhcLotterySg = fivelhcLotterySgMapper.selectOneByExample(fivelhcExample);
                if (fivelhcLotterySg == null || fivelhcLotterySg.getOpenStatus().equals(Constants.WAIT)) {
                    return ResultInfo.error("???????????????????????????????????????");
                }
                fivelhcLotterySg.setNumber(number);
                fivelhcLotterySgMapper.updateByPrimaryKeySelective(fivelhcLotterySg);
                break;
            case 1204: //???????????????
                //??????????????????????????????
                AmlhcLotterySgExample sslhcExample = new AmlhcLotterySgExample();
                AmlhcLotterySgExample.Criteria criteriasslhc = sslhcExample.createCriteria();
                criteriasslhc.andIssueEqualTo(issue);
                AmlhcLotterySg amlhcLotterySg = amlhcLotterySgMapper.selectOneByExample(sslhcExample);
                if (amlhcLotterySg == null || amlhcLotterySg.getOpenStatus().equals(Constants.WAIT)) {
                    return ResultInfo.error("???????????????????????????????????????");
                }
                amlhcLotterySg.setNumber(number);
                amlhcLotterySgMapper.updateByPrimaryKeySelective(amlhcLotterySg);
                break;
            // ??????PK10
            case 1301:
                // ??????????????????
                BjpksLotterySgExample bjpksExample = new BjpksLotterySgExample();
                BjpksLotterySgExample.Criteria bjpksCriteria = bjpksExample.createCriteria();
                bjpksCriteria.andIssueEqualTo(issue);
                BjpksLotterySg bjpksLotterySg = bjpksLotterySgMapper.selectOneByExample(bjpksExample);
                if (bjpksLotterySg == null || StringUtils.isBlank(bjpksLotterySg.getNumber())) {
                    return ResultInfo.error("???????????????????????????????????????");
                }
                bjpksLotterySg.setCpkNumber(number);
                bjpksLotterySg.setKcwNumber(number);
                bjpksLotterySg.setNumber(number);
                bjpksLotterySgMapper.updateByPrimaryKeySelective(bjpksLotterySg);

                // ??????????????????
                BjpksKillNumberExample bjpksKillNumberExample = new BjpksKillNumberExample();
                BjpksKillNumberExample.Criteria bjpksKillNumberExampleCriteria = bjpksKillNumberExample.createCriteria();
                bjpksKillNumberExampleCriteria.andIssueEqualTo(issue);
                BjpksKillNumber bjpksKillNumber = bjpksKillNumberMapper.selectOneByExample(bjpksKillNumberExample);
                if (bjpksKillNumber != null) {
                    bjpksKillNumber.setNumber(number);
                    bjpksKillNumberMapper.updateByPrimaryKeySelective(bjpksKillNumber);
                }
//
//                // ??????????????????
//                BjpksRecommendExample bjpksRecommendExample = new BjpksRecommendExample();
//                BjpksRecommendExample.Criteria bjpksRecommendExampleCriteria = bjpksRecommendExample.createCriteria();
//                bjpksRecommendExampleCriteria.andIssueEqualTo(issue);
//                BjpksRecommend bjpksRecommend = bjpksRecommendMapper.selectOneByExample(bjpksRecommendExample);
//                if (bjpksRecommend != null) {
//                    bjpksRecommend.setOpenNumber(number);
//                    bjpksRecommendMapper.updateByPrimaryKey(bjpksRecommend);
//                }
                break;
            case 1302: //10???PK
                //??????????????????????????????
                TenbjpksLotterySgExample tenbjpksExample = new TenbjpksLotterySgExample();
                TenbjpksLotterySgExample.Criteria criteriatenbjpks = tenbjpksExample.createCriteria();
                criteriatenbjpks.andIssueEqualTo(issue);
                TenbjpksLotterySg tenbjpksLotterySg = tenbjpksLotterySgMapper.selectOneByExample(tenbjpksExample);
                if (tenbjpksLotterySg == null || tenbjpksLotterySg.getOpenStatus().equals(Constants.WAIT)) {
                    return ResultInfo.error("???????????????????????????????????????");
                }
                tenbjpksLotterySg.setNumber(number);
                tenbjpksLotterySgMapper.updateByPrimaryKeySelective(tenbjpksLotterySg);
                break;
            case 1303: //5???PK
                //??????????????????????????????
                FivebjpksLotterySgExample fivebjpksExample = new FivebjpksLotterySgExample();
                FivebjpksLotterySgExample.Criteria criteriatenfivepks = fivebjpksExample.createCriteria();
                criteriatenfivepks.andIssueEqualTo(issue);
                FivebjpksLotterySg fivebjpksLotterySg = fivebjpksLotterySgMapper.selectOneByExample(fivebjpksExample);
                if (fivebjpksLotterySg == null || fivebjpksLotterySg.getOpenStatus().equals(Constants.WAIT)) {
                    return ResultInfo.error("???????????????????????????????????????");
                }
                fivebjpksLotterySg.setNumber(number);
                fivebjpksLotterySgMapper.updateByPrimaryKeySelective(fivebjpksLotterySg);
                break;
            case 1304: //??????PK
                //??????????????????????????????
                JsbjpksLotterySgExample jsbjpksExample = new JsbjpksLotterySgExample();
                JsbjpksLotterySgExample.Criteria criteriatenjspks = jsbjpksExample.createCriteria();
                criteriatenjspks.andIssueEqualTo(issue);
                JsbjpksLotterySg jsbjpksLotterySg = jsbjpksLotterySgMapper.selectOneByExample(jsbjpksExample);
                if (jsbjpksLotterySg == null || jsbjpksLotterySg.getOpenStatus().equals(Constants.WAIT)) {
                    return ResultInfo.error("???????????????????????????????????????");
                }
                jsbjpksLotterySg.setNumber(number);
                jsbjpksLotterySgMapper.updateByPrimaryKeySelective(jsbjpksLotterySg);
                break;
            case 1401:  //????????????
                // ??????????????????
                XyftLotterySgExample xyftExample = new XyftLotterySgExample();
                XyftLotterySgExample.Criteria xyftCriteria = xyftExample.createCriteria();
                xyftCriteria.andIssueEqualTo(issue);
                XyftLotterySg xyftLotterySg = xyftLotterySgMapper.selectOneByExample(xyftExample);
                if (xyftLotterySg == null || xyftLotterySg.getOpenStatus().equals(Constants.WAIT)) {
                    return ResultInfo.error("???????????????????????????????????????");
                }
                xyftLotterySg.setCpkNumber(number);
                xyftLotterySg.setKcwNumber(number);
                xyftLotterySg.setNumber(number);
                xyftLotterySgMapper.updateByPrimaryKeySelective(xyftLotterySg);

//                // ??????????????????
//                XyftKillNumberExample xyftKillNumberExample = new XyftKillNumberExample();
//                XyftKillNumberExample.Criteria xyftKillNumberExampleCriteria = xyftKillNumberExample.createCriteria();
//                xyftKillNumberExampleCriteria.andIssueEqualTo(issue);
//                XyftKillNumber xyftKillNumber = xyftKillNumberMapper.selectOneByExample(xyftKillNumberExample);
//                if (xyftKillNumber != null) {
//                    xyftKillNumber.setNumber(number);
//                    xyftKillNumberMapper.updateByPrimaryKeySelective(xyftKillNumber);
//                }
//
//                // ??????????????????
//                XyftRecommendExample xyftRecommendExample = new XyftRecommendExample();
//                XyftRecommendExample.Criteria xyftRecommendExampleCriteria = xyftRecommendExample.createCriteria();
//                xyftRecommendExampleCriteria.andIssueEqualTo(issue);
//                XyftRecommend xyftRecommend = xyftRecommendMapper.selectOneByExample(xyftRecommendExample);
//                if (xyftRecommend != null) {
//                    xyftRecommend.setOpenNumber(number);
//                    xyftRecommendMapper.updateByPrimaryKey(xyftRecommend);
//                }
                break;
            case 1501: // PC??????
                // ??????????????????????????????
                PceggLotterySgExample pceggLotterySgExample = new PceggLotterySgExample();
                PceggLotterySgExample.Criteria pcEx = pceggLotterySgExample.createCriteria();
                pcEx.andIssueEqualTo(issue);
                PceggLotterySg pceggLotterySg = pcegglotterySgMapper.selectOneByExample(pceggLotterySgExample);
                if (pceggLotterySg == null || pceggLotterySg.getOpenStatus().equals(Constants.WAIT)) {
                    return ResultInfo.error("???????????????????????????????????????");
                }
                // ????????????
                pceggLotterySg.setNumber(number);
                pceggLotterySg.setCpkNumber(number);
                pceggLotterySg.setKcwNumber(number);
                pcegglotterySgMapper.updateByPrimaryKeySelective(pceggLotterySg);
                break;

            case 1601: //?????????
                // ??????
                TxffcLotterySgExample txffcExample = new TxffcLotterySgExample();
                TxffcLotterySgExample.Criteria txffcCriteria = txffcExample.createCriteria();
                txffcCriteria.andIssueEqualTo(issue);
                TxffcLotterySg txffcLotterySg = txffcLotterySgMapper.selectOneByExample(txffcExample);
                if (txffcLotterySg == null || txffcLotterySg.getOpenStatus().equals(Constants.WAIT)) {
                    return ResultInfo.error("???????????????????????????????????????");
                }

                //??????????????????
                txffcLotterySg.setWan(Integer.parseInt(num[0]));
                txffcLotterySg.setQian(Integer.parseInt(num[1]));
                txffcLotterySg.setBai(Integer.parseInt(num[2]));
                txffcLotterySg.setShi(Integer.parseInt(num[3]));
                txffcLotterySg.setGe(Integer.parseInt(num[4]));
                txffcLotterySg.setCpkNumber(number);
                txffcLotterySg.setKcwNumber(number);
                txffcLotterySgMapper.updateByPrimaryKeySelective(txffcLotterySg);
//
//                //??????
//                TxffcRecommendExample txffcRecommendExample = new TxffcRecommendExample();
//                TxffcRecommendExample.Criteria txffcRecommendCritera = txffcRecommendExample.createCriteria();
//                txffcRecommendCritera.andIssueEqualTo(issue);
//                TxffcRecommend txffcRecommend = txffcRecommendMapper.selectOneByExample(txffcRecommendExample);
//                if (txffcRecommend != null) {
//                    //???????????????????????????
//                    txffcRecommend.setOpenNumber(number);
//                    txffcRecommendMapper.updateByPrimaryKeySelective(txffcRecommend);
//                }
//
//                TxffcKillNumberExample txffcKillNumberExample = new TxffcKillNumberExample();
//                TxffcKillNumberExample.Criteria txfKill = txffcKillNumberExample.createCriteria();
//                txfKill.andIssueEqualTo(issue);
//                TxffcKillNumber txffcKillNumber = txffcKillNumberMapper.selectOneByExample(txffcKillNumberExample);
//                if (txffcKillNumber != null) {
//                    //?????????????????????
//                    txffcKillNumber.setOpenOne(Integer.parseInt(num[0]));
//                    txffcKillNumber.setOpenTwo(Integer.parseInt(num[1]));
//                    txffcKillNumber.setOpenThree(Integer.parseInt(num[2]));
//                    txffcKillNumber.setOpenFour(Integer.parseInt(num[3]));
//                    txffcKillNumber.setOpenFive(Integer.parseInt(num[4]));
//                    txffcKillNumberMapper.updateByPrimaryKeySelective(txffcKillNumber);
//                }
                break;
            case 1701: // ?????????
                // ??????????????????????????????
                TcdltLotterySgExample tcdltLotterySgExample = new TcdltLotterySgExample();
                TcdltLotterySgExample.Criteria tcdltEx = tcdltLotterySgExample.createCriteria();
                tcdltEx.andIssueEqualTo(issue);
                TcdltLotterySg tcdltLotterySg = tcdltlotterySgMapper.selectOneByExample(tcdltLotterySgExample);
                if (tcdltLotterySg == null || tcdltLotterySg.getOpenStatus().equals(Constants.WAIT)) {
                    return ResultInfo.error("???????????????????????????????????????");
                }
                // ????????????
                tcdltLotterySg.setNumber(number);
                tcdltLotterySg.setCpkNumber(number);
                tcdltLotterySg.setKcwNumber(number);
                tcdltlotterySgMapper.updateByPrimaryKeySelective(tcdltLotterySg);
                break;
            case 1702: // ??????5
                // ??????????????????????????????
                TcplwLotterySgExample tcplwLotterySgExample = new TcplwLotterySgExample();
                TcplwLotterySgExample.Criteria tcplwEx = tcplwLotterySgExample.createCriteria();
                tcplwEx.andIssueEqualTo(issue);
                TcplwLotterySg tcplwLotterySg = tcplwlotterySgMapper.selectOneByExample(tcplwLotterySgExample);
                if (tcplwLotterySg == null || tcplwLotterySg.getOpenStatus().equals(Constants.WAIT)) {
                    return ResultInfo.error("???????????????????????????????????????");
                }
                // ????????????
                tcplwLotterySg.setNumber(number);
                tcplwLotterySg.setCpkNumber(number);
                tcplwLotterySg.setKcwNumber(number);
                tcplwlotterySgMapper.updateByPrimaryKeySelective(tcplwLotterySg);
                break;
            case 1703: // ??????7??????
                // ??????????????????????????????
                Tc7xcLotterySgExample tc7xcLotterySgExample = new Tc7xcLotterySgExample();
                Tc7xcLotterySgExample.Criteria tc7xcEx = tc7xcLotterySgExample.createCriteria();
                tc7xcEx.andIssueEqualTo(issue);
                Tc7xcLotterySg tc7xcLotterySg = tc7xclotterySgMapper.selectOneByExample(tc7xcLotterySgExample);
                if (tc7xcLotterySg == null || tc7xcLotterySg.getOpenStatus().equals(Constants.WAIT)) {
                    return ResultInfo.error("???????????????????????????????????????");
                }
                // ????????????
                tc7xcLotterySg.setCpkNumber(number);
                tc7xcLotterySg.setKcwNumber(number);
                tc7xcLotterySg.setNumber(number);
                tc7xclotterySgMapper.updateByPrimaryKeySelective(tc7xcLotterySg);
                break;
            case 1801: // ???????????????
                // ??????????????????????????????
                FcssqLotterySgExample fcssqLotterySgExample = new FcssqLotterySgExample();
                FcssqLotterySgExample.Criteria fcssqEx = fcssqLotterySgExample.createCriteria();
                fcssqEx.andIssueEqualTo(issue);
                FcssqLotterySg fcssqLotterySg = fcssqlotterySgMapper.selectOneByExample(fcssqLotterySgExample);
                if (fcssqLotterySg == null || fcssqLotterySg.getOpenStatus().equals(Constants.WAIT)) {
                    return ResultInfo.error("???????????????????????????????????????");
                }
                // ????????????
                fcssqLotterySg.setCpkNumber(number);
                fcssqLotterySg.setKcwNumber(number);
                fcssqLotterySg.setNumber(number);
                fcssqlotterySgMapper.updateByPrimaryKeySelective(fcssqLotterySg);
                break;
            case 1802: // ??????3D
                // ??????????????????????????????
                Fc3dLotterySgExample fc3dLotterySgExample = new Fc3dLotterySgExample();
                Fc3dLotterySgExample.Criteria fc3dEx = fc3dLotterySgExample.createCriteria();
                fc3dEx.andIssueEqualTo(issue);
                Fc3dLotterySg fc3dLotterySg = fc3dlotterySgMapper.selectOneByExample(fc3dLotterySgExample);
                if (fc3dLotterySg == null || fc3dLotterySg.getOpenStatus().equals(Constants.WAIT)) {
                    return ResultInfo.error("???????????????????????????????????????");
                }
                // ????????????
                fc3dLotterySg.setCpkNumber(number);
                fc3dLotterySg.setKcwNumber(number);
                fc3dLotterySg.setNumber(number);
                fc3dlotterySgMapper.updateByPrimaryKeySelective(fc3dLotterySg);
                break;
            case 1803: // ??????7??????
                // ??????????????????????????????
                Fc7lcLotterySgExample fc7lcLotterySgExample = new Fc7lcLotterySgExample();
                Fc7lcLotterySgExample.Criteria fc7lcEx = fc7lcLotterySgExample.createCriteria();
                fc7lcEx.andIssueEqualTo(issue);
                Fc7lcLotterySg fc7lcLotterySg = fc7lclotterySgMapper.selectOneByExample(fc7lcLotterySgExample);
                if (fc7lcLotterySg == null || fc7lcLotterySg.getOpenStatus().equals(Constants.WAIT)) {
                    return ResultInfo.error("???????????????????????????????????????");
                }
                // ????????????
                fc7lcLotterySg.setCpkNumber(number);
                fc7lcLotterySg.setKcwNumber(number);
                fc7lcLotterySg.setNumber(number);
                fc7lclotterySgMapper.updateByPrimaryKeySelective(fc7lcLotterySg);
                break;
            case 2203: // ?????????????????????f1??????
                // ??????????????????????????????
                AuspksLotterySgExample auspksLotterySgExample = new AuspksLotterySgExample();
                AuspksLotterySgExample.Criteria auspksEx = auspksLotterySgExample.createCriteria();
                auspksEx.andIssueEqualTo(issue);
                AuspksLotterySg auspksLotterySg = auspkslotterySgMapper.selectOneByExample(auspksLotterySgExample);
                if (auspksLotterySg == null || auspksLotterySg.getOpenStatus().equals(Constants.WAIT)) {
                    return ResultInfo.error("???????????????????????????????????????");
                }
                // ????????????
                auspksLotterySg.setNumber(number);
                auspkslotterySgMapper.updateByPrimaryKeySelective(auspksLotterySg);
                break;
            case 2001: // ????????????pk
                // ??????????????????????????????
                FtjspksLotterySgExample ftjspksLotterySgExample = new FtjspksLotterySgExample();
                FtjspksLotterySgExample.Criteria ftjspkEx = ftjspksLotterySgExample.createCriteria();
                ftjspkEx.andIssueEqualTo(issue);
                FtjspksLotterySg ftjspksLotterySg = ftjspkslotterySgMapper.selectOneByExample(ftjspksLotterySgExample);
                if (ftjspksLotterySg == null || ftjspksLotterySg.getOpenStatus().equals(Constants.WAIT)) {
                    return ResultInfo.error("???????????????????????????????????????");
                }
                // ????????????
                ftjspksLotterySg.setNumber(number);
                // ???????????????
                String modValue = FanTanCalculationUtils.ftjspksSaleResult(number);
                ftjspksLotterySg.setFtNumber(modValue);
                ftjspkslotterySgMapper.updateByPrimaryKeySelective(ftjspksLotterySg);
                break;
            case 2002: // ??????????????????
                // ??????????????????????????????
                FtxyftLotterySgExample ftxyftLotterySgExample = new FtxyftLotterySgExample();
                FtxyftLotterySgExample.Criteria ftxyftEx = ftxyftLotterySgExample.createCriteria();
                ftxyftEx.andIssueEqualTo(issue);
                FtxyftLotterySg ftxyftLotterySg = ftxyftlotterySgMapper.selectOneByExample(ftxyftLotterySgExample);
                if (ftxyftLotterySg == null || ftxyftLotterySg.getOpenStatus().equals(Constants.WAIT)) {
                    return ResultInfo.error("???????????????????????????????????????");
                }
                // ????????????
                ftxyftLotterySg.setNumber(number);
                ftxyftLotterySg.setCpkNumber(number);
                ftxyftLotterySg.setKcwNumber(number);
                // ???????????????
                String xyftmodValue = FanTanCalculationUtils.ftjspksSaleResult(number);
                ftxyftLotterySg.setFtNumber(xyftmodValue);
                ftxyftlotterySgMapper.updateByPrimaryKeySelective(ftxyftLotterySg);
                break;
            case 2003: // ?????????????????????
                // ??????????????????????????????
                FtjssscLotterySgExample ftjssscLotterySgExample = new FtjssscLotterySgExample();
                FtjssscLotterySgExample.Criteria ftjssscEx = ftjssscLotterySgExample.createCriteria();
                ftjssscEx.andIssueEqualTo(issue);
                FtjssscLotterySg ftjssscLotterySg = ftjsssclotterySgMapper.selectOneByExample(ftjssscLotterySgExample);
                if (ftjssscLotterySg == null || ftjssscLotterySg.getOpenStatus().equals(Constants.WAIT)) {
                    return ResultInfo.error("???????????????????????????????????????");
                }
                // ????????????
                ftjssscLotterySg.setNumber(number);
                // ???????????????
                String jssscmodValue = FanTanCalculationUtils.ftjssscSaleResult(number);
                ftjssscLotterySg.setFtNumber(jssscmodValue);
                ftjsssclotterySgMapper.updateByPrimaryKeySelective(ftjssscLotterySg);
                break;
            case 2201: // ??????ACT
                // ??????????????????????????????
                AusactLotterySgExample ausactLotterySgExample = new AusactLotterySgExample();
                AusactLotterySgExample.Criteria ausactEx = ausactLotterySgExample.createCriteria();
                ausactEx.andIssueEqualTo(issue);
                AusactLotterySg ausactLotterySg = ausactlotterySgMapper.selectOneByExample(ausactLotterySgExample);
                if (ausactLotterySg == null || ausactLotterySg.getOpenStatus().equals(Constants.WAIT)) {
                    return ResultInfo.error("???????????????????????????????????????");
                }
                // ????????????
                ausactLotterySg.setNumber(number);
                ausactlotterySgMapper.updateByPrimaryKeySelective(ausactLotterySg);
                break;
            case 2202: // ???????????????
                // ??????????????????????????????
                AussscLotterySgExample aussscLotterySgExample = new AussscLotterySgExample();
                AussscLotterySgExample.Criteria aussscEx = aussscLotterySgExample.createCriteria();
                aussscEx.andIssueEqualTo(issue);
                AussscLotterySg aussscLotterySg = ausssclotterySgMapper.selectOneByExample(aussscLotterySgExample);
                if (aussscLotterySg == null || aussscLotterySg.getOpenStatus().equals(Constants.WAIT)) {
                    return ResultInfo.error("???????????????????????????????????????");
                }
                // ????????????
                aussscLotterySg.setNumber(number);
                ausssclotterySgMapper.updateByPrimaryKeySelective(aussscLotterySg);
                break;
            case 2301: // ????????????
                // ??????????????????????????????
                AzksLotterySgExample azksLotterySgExample = new AzksLotterySgExample();
                AzksLotterySgExample.Criteria azksEx = azksLotterySgExample.createCriteria();
                azksEx.andIssueEqualTo(issue);
                AzksLotterySg azksLotterySg = azkslotterySgMapper.selectOneByExample(azksLotterySgExample);
                if (azksLotterySg == null || azksLotterySg.getOpenStatus().equals(Constants.WAIT)) {
                    return ResultInfo.error("???????????????????????????????????????");
                }
                // ????????????
                azksLotterySg.setNumber(number);
                azkslotterySgMapper.updateByPrimaryKeySelective(azksLotterySg);
                break;
            case 2302: // ????????????
                // ??????????????????????????????
                DzksLotterySgExample dzksLotterySgExample = new DzksLotterySgExample();
                DzksLotterySgExample.Criteria dzksEx = dzksLotterySgExample.createCriteria();
                dzksEx.andIssueEqualTo(issue);
                DzksLotterySg dzksLotterySg = dzkslotterySgMapper.selectOneByExample(dzksLotterySgExample);
                if (dzksLotterySg == null || dzksLotterySg.getOpenStatus().equals(Constants.WAIT)) {
                    return ResultInfo.error("???????????????????????????????????????");
                }
                // ????????????
                dzksLotterySg.setNumber(number);
                dzkslotterySgMapper.updateByPrimaryKeySelective(dzksLotterySg);
                break;
            case 1502: // ??????Pcdd
                // ??????????????????????????????
                DzpceggLotterySgExample dzpceggLotterySgExample = new DzpceggLotterySgExample();
                DzpceggLotterySgExample.Criteria dzpceggEx = dzpceggLotterySgExample.createCriteria();
                dzpceggEx.andIssueEqualTo(issue);
                DzpceggLotterySg dzpceggLotterySg = dzpcegglotterySgMapper.selectOneByExample(dzpceggLotterySgExample);
                if (dzpceggLotterySg == null || dzpceggLotterySg.getOpenStatus().equals(Constants.WAIT)) {
                    return ResultInfo.error("???????????????????????????????????????");
                }
                // ????????????
                dzpceggLotterySg.setNumber(number);
                dzpcegglotterySgMapper.updateByPrimaryKeySelective(dzpceggLotterySg);
                break;
            case 1402: // ??????????????????
                // ??????????????????????????????
                DzxyftLotterySgExample dzxyftLotterySgExample = new DzxyftLotterySgExample();
                DzxyftLotterySgExample.Criteria dzxyftEx = dzxyftLotterySgExample.createCriteria();
                dzxyftEx.andIssueEqualTo(issue);
                DzxyftLotterySg dzxyftLotterySg = dzxyftlotterySgMapper.selectOneByExample(dzxyftLotterySgExample);
                if (dzxyftLotterySg == null || dzxyftLotterySg.getOpenStatus().equals(Constants.WAIT)) {
                    return ResultInfo.error("???????????????????????????????????????");
                }
                // ????????????
                dzxyftLotterySg.setNumber(number);
                dzxyftlotterySgMapper.updateByPrimaryKeySelective(dzxyftLotterySg);
                break;
            case 1205: // ??????????????????
                // ??????????????????????????????
                XjplhcLotterySgExample xjplhcLotterySgExample = new XjplhcLotterySgExample();
                XjplhcLotterySgExample.Criteria xjplhcEx = xjplhcLotterySgExample.createCriteria();
                xjplhcEx.andIssueEqualTo(issue);
                XjplhcLotterySg xjplhcLotterySg = xjplhclotterySgMapper.selectOneByExample(xjplhcLotterySgExample);
                if (xjplhcLotterySg == null || xjplhcLotterySg.getOpenStatus().equals(Constants.WAIT)) {
                    return ResultInfo.error("???????????????????????????????????????");
                }
                // ????????????
                xjplhcLotterySg.setNumber(number);
                xjplhclotterySgMapper.updateByPrimaryKeySelective(xjplhcLotterySg);
                break;

            case 1201: //?????????
                //??????????????????????????????
                LhcLotterySgExample lhcLotterySgExample = new LhcLotterySgExample();
                LhcLotterySgExample.Criteria lhcCriteria = lhcLotterySgExample.createCriteria();
                lhcCriteria.andYearEqualTo(issue.substring(0, 4));
                lhcCriteria.andIssueEqualTo(issue.substring(4));
                LhcLotterySg lhcLotterySg = lhcLotterySgMapper.selectOneByExample(lhcLotterySgExample);
                if (lhcLotterySg == null) {
                    return ResultInfo.error("???????????????????????????????????????");
                }
                lhcLotterySg.setNumber(number);
                lhcLotterySgMapper.updateByPrimaryKeySelective(lhcLotterySg);
//
//                //???????????????????????????????????????
//                LhcKillNumberExample lhcKillNumberExample = new LhcKillNumberExample();
//                LhcKillNumberExample.Criteria lhcKill = lhcKillNumberExample.createCriteria();
//                lhcKill.andIssueEqualTo(issue);
//                LhcKillNumber lhcKillNumber = lhcKillNumberMapper.selectOneByExample(lhcKillNumberExample);
//                if (lhcKillNumber != null) {
//                    //??????????????????
//                    lhcKillNumber.setNumber(number);
//                    lhcKillNumberMapper.updateByPrimaryKeySelective(lhcKillNumber);
//                }
                break;
        }

        return ResultInfo.ok(1);
    }

    /**
     * ?????????????????????????????????????????????
     *
     * @param lotteryId
     * @param number
     * @return
     */
    private boolean numberIsTrue(Integer lotteryId, String number) {
        String[] numbers = number.split(",");
        switch (lotteryId) {
            case 1:
            case 2:
            case 3:
                if (numbers.length != 5) {
                    return false;
                }
                int sscNum;
                for (String num : numbers) {
                    // ?????????????????????
                    if (!isInteger(num)) {
                        return false;
                    }
                    // ??????????????????0-9??????
                    sscNum = Integer.valueOf(num);
                    if (sscNum < 0 || sscNum > 9) {
                        return false;
                    }
                }
                break;

            case 4:
                if (numbers.length != 7) {
                    return false;
                }
                int lhcNum;
                for (String num : numbers) {
                    // ?????????????????????
                    if (!isInteger(num)) {
                        return false;
                    }
                    // ??????????????????1-49??????
                    lhcNum = Integer.valueOf(num);
                    if (lhcNum < 1 || lhcNum > 49) {
                        return false;
                    }
                }
                break;

            case 5:
                if (numbers.length != 3) {
                    return false;
                }
                int pcddNum;
                for (String num : numbers) {
                    // ?????????????????????
                    if (!isInteger(num)) {
                        return false;
                    }
                    // ??????????????????0-27??????
                    // ?????????????????????
                    pcddNum = Integer.valueOf(num);
                    if (pcddNum < 0 || pcddNum > 27) {
                        return false;
                    }
                }
                break;

            case 6:
            case 7:
                if (numbers.length != 10) {
                    return false;
                }
                int bjpkNum;
                for (String num : numbers) {
                    // ?????????????????????
                    if (!isInteger(num)) {
                        return false;
                    }
                    // ??????????????????1-10??????
                    bjpkNum = Integer.valueOf(num);
                    if (bjpkNum < 1 || bjpkNum > 10) {
                        return false;
                    }
                }
                break;

        }
        return true;
    }

    /**
     * ?????????????????????????????????
     * ?????????????????????
     *
     * @param str ??????????????????
     * @return ???????????????true, ????????????false
     */
    private static boolean isInteger(String str) {
        Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
        return pattern.matcher(str).matches();
    }

}
