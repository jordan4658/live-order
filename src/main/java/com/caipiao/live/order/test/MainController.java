package com.caipiao.live.order.test;

import com.caipiao.live.order.service.bet.*;
import com.caipiao.live.order.service.bet.*;
import com.caipiao.live.common.enums.lottery.CaipiaoTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MainController implements InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    //    @Autowired
//    private BetNnAzService betNnAzService;
//    @Autowired
//    private BetNnKlService betNnKlService1;
//    @Autowired
//    private BetNnJsService betNnJsService;
    @Autowired
    private BetF1AzService betF1AzService;
    @Autowired
    private BetSscAzService betSscAzService;
    //    @Autowired
//    private Bet7xcHnService bet7xcHnService;
//    @Autowired
//    private Bet3dFcService bet3dFcService;
    @Autowired
    private BetActAzService betActAzService;
    @Autowired
    private BetSscbmService betSscbmService;
    @Autowired
    private BetBjpksService betBjpksService;
    @Autowired
    private BetXyftService betXyftService;
    //    @Autowired
//    private BetFc7lcService betFc7lcService;
//    @Autowired
//    private BetTcPlswService betTcPlswService;
//    @Autowired
//    private BetTcDltService betTcDltService;
//    @Autowired
//    private BetFcSsqService betFcSsqService;
    @Autowired
    private BetLhcService betLhcService;
    @Autowired
    private BetPceggService betPceggService;
    //    @Autowired
//    private BetNnKlService betNnKlService;
//    @Autowired
//    private BetFtJspksService betFtJspksService;
//    @Autowired
//    private BetFtXyftService betFtXyftService;
    //    @Autowired
//    private BetFtSscService betFtSscService;
    @Autowired
    private JmsMessagingTemplate jmsMessagingTemplate;


    public static String BROKERURL;

    @Override
    public void afterPropertiesSet() throws Exception {
        BROKERURL = this.enviroment;
    }


    @Value("${product.order.enviroment}")
    private String enviroment;

    //??????????????????????????????  http://13.228.29.132:7011/index/bet3dFcService

    //    http://13.228.29.132:7011/index/jiesuan/{lotteryId}/{issue}/{number}
    @GetMapping(value = "/index/jiesuan/{lotteryId}/{issue}/{number}")
    public String bet7xcHnService(@PathVariable("lotteryId") String lotteryId, @PathVariable("issue") String issue, @PathVariable("number") String number) {
        if (!"dev".equals(BROKERURL) && !"test".equals(BROKERURL) && !"prodtest".equals(BROKERURL)) {
            return "????????????";
        }
        try {
            if ("1101".equals(lotteryId)) {
                // ??????????????????
                betSscbmService.countlm(issue, number, Integer.parseInt(CaipiaoTypeEnum.CQSSC.getTagType()));
                // ????????????????????????
                betSscbmService.countdn(issue, number, Integer.parseInt(CaipiaoTypeEnum.CQSSC.getTagType()));
                // ?????????????????????
                betSscbmService.count15(issue, number, Integer.parseInt(CaipiaoTypeEnum.CQSSC.getTagType()));
                // ??????????????????????????????
                betSscbmService.countqzh(issue, number, Integer.parseInt(CaipiaoTypeEnum.CQSSC.getTagType()));
            } else if ("1102".equals(lotteryId)) {
                // ??????????????????
                betSscbmService.countlm(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJSSC.getTagType()));
                // ????????????????????????
                betSscbmService.countdn(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJSSC.getTagType()));
                // ?????????????????????
                betSscbmService.count15(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJSSC.getTagType()));
                // ??????????????????????????????
                betSscbmService.countqzh(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJSSC.getTagType()));
            } else if ("1103".equals(lotteryId)) {
                // ??????????????????
                betSscbmService.countlm(issue, number, Integer.parseInt(CaipiaoTypeEnum.TJSSC.getTagType()));
                // ????????????????????????
                betSscbmService.countdn(issue, number, Integer.parseInt(CaipiaoTypeEnum.TJSSC.getTagType()));
                // ?????????????????????
                betSscbmService.count15(issue, number, Integer.parseInt(CaipiaoTypeEnum.TJSSC.getTagType()));
                // ??????????????????????????????
                betSscbmService.countqzh(issue, number, Integer.parseInt(CaipiaoTypeEnum.TJSSC.getTagType()));
            } else if ("1104".equals(lotteryId)) {
                // ??????????????????
                betSscbmService.countlm(issue, number, Integer.parseInt(CaipiaoTypeEnum.TENSSC.getTagType()));
                // ????????????????????????
                betSscbmService.countdn(issue, number, Integer.parseInt(CaipiaoTypeEnum.TENSSC.getTagType()));
                // ?????????????????????
                betSscbmService.count15(issue, number, Integer.parseInt(CaipiaoTypeEnum.TENSSC.getTagType()));
                // ??????????????????????????????
                betSscbmService.countqzh(issue, number, Integer.parseInt(CaipiaoTypeEnum.TENSSC.getTagType()));
            } else if ("1105".equals(lotteryId)) {
                // ??????????????????
                betSscbmService.countlm(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVESSC.getTagType()));
                // ????????????????????????
                betSscbmService.countdn(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVESSC.getTagType()));
                // ?????????????????????
                betSscbmService.count15(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVESSC.getTagType()));
                // ??????????????????????????????
                betSscbmService.countqzh(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVESSC.getTagType()));
            } else if ("1106".equals(lotteryId)) {
                // ??????????????????
                betSscbmService.countlm(issue, number, Integer.parseInt(CaipiaoTypeEnum.JSSSC.getTagType()));
                // ????????????????????????
                betSscbmService.countdn(issue, number, Integer.parseInt(CaipiaoTypeEnum.JSSSC.getTagType()));
                // ?????????????????????

                betSscbmService.count15(issue, number, Integer.parseInt(CaipiaoTypeEnum.JSSSC.getTagType()));
                // ??????????????????????????????
                betSscbmService.countqzh(issue, number, Integer.parseInt(CaipiaoTypeEnum.JSSSC.getTagType()));
            } else if ("1201".equals(lotteryId)) {
                // ???????????????- ?????????,??????,??????,??????1-6???
                betLhcService.clearingLhcTeMaA(issue, number, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()), true);
                betLhcService.clearingLhcZhengTe(issue, number, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()), true);
                betLhcService.clearingLhcZhengMaOneToSix(issue, number, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()), true);
                betLhcService.clearingLhcLiuXiao(issue, number, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()), true);

                // ???????????????- ?????????,??????,?????????
                betLhcService.clearingLhcZhengMaA(issue, number, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()), true);
                betLhcService.clearingLhcBanBo(issue, number, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()), true);
                betLhcService.clearingLhcWs(issue, number, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()), true);

                // ???????????????- ?????????,??????,?????????
                betLhcService.clearingLhcLianMa(issue, number, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()), true);
                betLhcService.clearingLhcLianXiao(issue, number, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()), true);
                betLhcService.clearingLhcLianWei(issue, number, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()), true);

                // ???????????????- ?????????,1-6??????,?????????
                betLhcService.clearingLhcNoOpen(issue, number, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()), true);
                betLhcService.clearingLhcOneSixLh(issue, number, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()), true);
                betLhcService.clearingLhcWuxing(issue, number, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()), true);

                // ???????????????- ?????????,?????????
                betLhcService.clearingLhcPtPt(issue, number, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()), true);
                betLhcService.clearingLhcTxTx(issue, number, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()), true);
            } else if ("1202".equals(lotteryId)) {
                // ???????????????- ?????????,??????,??????,??????1-6???
                betLhcService.clearingLhcTeMaA(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                betLhcService.clearingLhcZhengTe(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                betLhcService.clearingLhcZhengMaOneToSix(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                betLhcService.clearingLhcLiuXiao(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);

                // ???????????????- ?????????,??????,?????????
                betLhcService.clearingLhcZhengMaA(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                betLhcService.clearingLhcBanBo(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                betLhcService.clearingLhcWs(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);

                // ???????????????- ?????????,??????,?????????
                betLhcService.clearingLhcLianMa(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                betLhcService.clearingLhcLianXiao(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                betLhcService.clearingLhcLianWei(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);

                // ???????????????- ?????????,1-6??????,?????????
                betLhcService.clearingLhcNoOpen(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                betLhcService.clearingLhcOneSixLh(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                betLhcService.clearingLhcWuxing(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);

                // ???????????????- ?????????,?????????
                betLhcService.clearingLhcPtPt(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                betLhcService.clearingLhcTxTx(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
            } else if ("1203".equals(lotteryId)) {
                // ???????????????- ?????????,??????,??????,??????1-6???
                betLhcService.clearingLhcTeMaA(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                betLhcService.clearingLhcZhengTe(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                betLhcService.clearingLhcZhengMaOneToSix(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                betLhcService.clearingLhcLiuXiao(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);

                // ???????????????- ?????????,??????,?????????
                betLhcService.clearingLhcZhengMaA(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                betLhcService.clearingLhcBanBo(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                betLhcService.clearingLhcWs(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);

                // ???????????????- ?????????,??????,?????????
                betLhcService.clearingLhcLianMa(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                betLhcService.clearingLhcLianXiao(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                betLhcService.clearingLhcLianWei(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);

                // ???????????????- ?????????,1-6??????,?????????
                betLhcService.clearingLhcNoOpen(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                betLhcService.clearingLhcOneSixLh(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                betLhcService.clearingLhcWuxing(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);

                // ???????????????- ?????????,?????????
                betLhcService.clearingLhcPtPt(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                betLhcService.clearingLhcTxTx(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
            } else if ("1204".equals(lotteryId)) {
                // ???????????????- ?????????,??????,??????,??????1-6???
                betLhcService.clearingLhcTeMaA(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                betLhcService.clearingLhcZhengTe(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                betLhcService.clearingLhcZhengMaOneToSix(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                betLhcService.clearingLhcLiuXiao(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);

                // ???????????????- ?????????,??????,?????????
                betLhcService.clearingLhcZhengMaA(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                betLhcService.clearingLhcBanBo(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                betLhcService.clearingLhcWs(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);

                // ???????????????- ?????????,??????,?????????
                betLhcService.clearingLhcLianMa(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                betLhcService.clearingLhcLianXiao(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                betLhcService.clearingLhcLianWei(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);

                // ???????????????- ?????????,1-6??????,?????????
                betLhcService.clearingLhcNoOpen(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                betLhcService.clearingLhcOneSixLh(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                betLhcService.clearingLhcWuxing(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);

                // ???????????????- ?????????,?????????
                betLhcService.clearingLhcPtPt(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                betLhcService.clearingLhcTxTx(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
            } else if ("1301".equals(lotteryId)) {
                // ???????????????PK10-?????????
                betBjpksService.clearingBjpksLm(issue, number, Integer.parseInt(CaipiaoTypeEnum.BJPKS.getTagType()));
                // ???????????????PK10-?????????????????????
                betBjpksService.clearingBjpksCmcCqj(issue, number, Integer.parseInt(CaipiaoTypeEnum.BJPKS.getTagType()));
                // ???????????????PK10-????????????
                betBjpksService.clearingBjpksGyh(issue, number, Integer.parseInt(CaipiaoTypeEnum.BJPKS.getTagType()));
            } else if ("1302".equals(lotteryId)) {
                // ???????????????PK10-?????????
                betBjpksService.clearingBjpksLm(issue, number, Integer.parseInt(CaipiaoTypeEnum.TENPKS.getTagType()));
                // ???????????????PK10-?????????????????????
                betBjpksService.clearingBjpksCmcCqj(issue, number, Integer.parseInt(CaipiaoTypeEnum.TENPKS.getTagType()));
                // ???????????????PK10-????????????
                betBjpksService.clearingBjpksGyh(issue, number, Integer.parseInt(CaipiaoTypeEnum.TENPKS.getTagType()));
            } else if ("1303".equals(lotteryId)) {
                // ???????????????PK10-?????????
                betBjpksService.clearingBjpksLm(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVEPKS.getTagType()));
                // ???????????????PK10-?????????????????????
                betBjpksService.clearingBjpksCmcCqj(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVEPKS.getTagType()));
                // ???????????????PK10-????????????
                betBjpksService.clearingBjpksGyh(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVEPKS.getTagType()));
            } else if ("1304".equals(lotteryId)) {
                // ???????????????PK10-?????????
                betBjpksService.clearingBjpksLm(issue, number, Integer.parseInt(CaipiaoTypeEnum.JSPKS.getTagType()));
                // ???????????????PK10-?????????????????????
                betBjpksService.clearingBjpksCmcCqj(issue, number, Integer.parseInt(CaipiaoTypeEnum.JSPKS.getTagType()));
                // ???????????????PK10-????????????
                betBjpksService.clearingBjpksGyh(issue, number, Integer.parseInt(CaipiaoTypeEnum.JSPKS.getTagType()));
            } else if ("1401".equals(lotteryId)) {
                // ?????????????????????-?????????
                betXyftService.clearingXyftLm(issue, number);
                // ?????????????????????-?????????????????????
                betXyftService.clearingXyftCmcCqj(issue, number);
                // ?????????????????????-????????????
                betXyftService.clearingXyftGyh(issue, number);
            } else if ("1501".equals(lotteryId)) {
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
            } else if ("1601".equals(lotteryId)) {
                // ??????????????????
                betSscbmService.countlm(issue, number, Integer.parseInt(CaipiaoTypeEnum.TXFFC.getTagType()));
                // ????????????????????????
                betSscbmService.countdn(issue, number, Integer.parseInt(CaipiaoTypeEnum.TXFFC.getTagType()));
                // ?????????????????????
                betSscbmService.count15(issue, number, Integer.parseInt(CaipiaoTypeEnum.TXFFC.getTagType()));
                // ??????????????????????????????
                betSscbmService.countqzh(issue, number, Integer.parseInt(CaipiaoTypeEnum.TXFFC.getTagType()));
            }
//            else if (lotteryId.equals("1701")) {
//                betTcDltService.clearingTCDLT(issue, number, Integer.parseInt(CaipiaoTypeEnum.DLT.getTagType()));
//            } else if (lotteryId.equals("1702")) {
//                betTcPlswService.clearingTcPlwLm(issue, number, Integer.parseInt(CaipiaoTypeEnum.TCPLW.getTagType()));
//                betTcPlswService.clearingTcPlsZx(issue, number, Integer.parseInt(CaipiaoTypeEnum.TCPLW.getTagType()));
//                betTcPlswService.clearingTcPlwZx(issue, number, Integer.parseInt(CaipiaoTypeEnum.TCPLW.getTagType()));
//                betTcPlswService.clearingTcPlwDwd(issue, number, Integer.parseInt(CaipiaoTypeEnum.TCPLW.getTagType()));
//            } else if (lotteryId.equals("1703")) {
//                bet7xcHnService.countHn7xc(issue, number, 1703);  //5,4,7,4
//            } else if (lotteryId.equals("1801")) {
//                betFcSsqService.clearingFCSSQ(issue, number, Integer.parseInt(CaipiaoTypeEnum.FCSSQ.getTagType()));
//            } else if (lotteryId.equals("1802")) {
//                bet3dFcService.countFc3d(issue, number, 1802); //1,2,0,1
//            } else if (lotteryId.equals("1803")) {
//                betFc7lcService.clearingFC7LC(issue, number, Integer.parseInt(CaipiaoTypeEnum.FC7LC.getTagType()));
//            } else if (lotteryId.equals("1901")) {
//                // ?????????????????????-?????????
//                betNnKlService.countKlXianjia(issue, number, Integer.parseInt(CaipiaoTypeEnum.KLNIU.getTagType()));
//            } else if (lotteryId.equals("1902")) {
//                // ?????????????????????-?????????
//                betNnAzService.countAzXianjia(issue, number, Integer.parseInt(CaipiaoTypeEnum.AZNIU.getTagType()));
//            } else if (lotteryId.equals("1903")) {
//                // ?????????????????????-?????????
//                betNnJsService.countJsXianjia(issue, number, Integer.parseInt(CaipiaoTypeEnum.JSNIU.getTagType()));
//            } else if (lotteryId.equals("2001")) {
//                // ???????????????pk?????????
//                betFtJspksService.clearingJspksJs(issue, number, Integer.parseInt(CaipiaoTypeEnum.JSPKFT.getTagType()));
//            } else if (lotteryId.equals("2002")) {
//                // ??????????????????
//                betFtXyftService.clearingFtXyftJs(issue, number, Integer.parseInt(CaipiaoTypeEnum.XYFTFT.getTagType()));
//            } else if (lotteryId.equals("2003")) {
//                // ?????????????????????????????????
//                betFtSscService.clearingFtSscJs(issue, number, Integer.parseInt(CaipiaoTypeEnum.JSSSCFT.getTagType()));
//            }
            else if ("2201".equals(lotteryId)) {
                //??????act??????
                betActAzService.countAzAct(issue, number, Integer.parseInt(CaipiaoTypeEnum.AUSACT.getTagType()));
            } else if ("2202".equals(lotteryId)) {
                // ???????????????????????????
                betSscAzService.countAzSsc(issue, number, Integer.parseInt(CaipiaoTypeEnum.AUSSSC.getTagType()));
            } else if ("2203".equals(lotteryId)) {
                // ???????????????F1???
                betF1AzService.countAzF1(issue, number, Integer.parseInt(CaipiaoTypeEnum.AUSPKS.getTagType()));
            }
        } catch (Exception e) {
            logger.error("bet7xcHnService occur error.", e);
            return "????????????";
        }
        return "????????????";
    }


}
