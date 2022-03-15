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

    //测试环境的访问地址：  http://13.228.29.132:7011/index/bet3dFcService

    //    http://13.228.29.132:7011/index/jiesuan/{lotteryId}/{issue}/{number}
    @GetMapping(value = "/index/jiesuan/{lotteryId}/{issue}/{number}")
    public String bet7xcHnService(@PathVariable("lotteryId") String lotteryId, @PathVariable("issue") String issue, @PathVariable("number") String number) {
        if (!"dev".equals(BROKERURL) && !"test".equals(BROKERURL) && !"prodtest".equals(BROKERURL)) {
            return "结算跳过";
        }
        try {
            if ("1101".equals(lotteryId)) {
                // 结算【两面】
                betSscbmService.countlm(issue, number, Integer.parseInt(CaipiaoTypeEnum.CQSSC.getTagType()));
                // 基本【组选】规则
                betSscbmService.countdn(issue, number, Integer.parseInt(CaipiaoTypeEnum.CQSSC.getTagType()));
                // 【定位胆】规则
                betSscbmService.count15(issue, number, Integer.parseInt(CaipiaoTypeEnum.CQSSC.getTagType()));
                // 【定位大小单双】规则
                betSscbmService.countqzh(issue, number, Integer.parseInt(CaipiaoTypeEnum.CQSSC.getTagType()));
            } else if ("1102".equals(lotteryId)) {
                // 结算【两面】
                betSscbmService.countlm(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJSSC.getTagType()));
                // 基本【组选】规则
                betSscbmService.countdn(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJSSC.getTagType()));
                // 【定位胆】规则
                betSscbmService.count15(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJSSC.getTagType()));
                // 【定位大小单双】规则
                betSscbmService.countqzh(issue, number, Integer.parseInt(CaipiaoTypeEnum.XJSSC.getTagType()));
            } else if ("1103".equals(lotteryId)) {
                // 结算【两面】
                betSscbmService.countlm(issue, number, Integer.parseInt(CaipiaoTypeEnum.TJSSC.getTagType()));
                // 基本【组选】规则
                betSscbmService.countdn(issue, number, Integer.parseInt(CaipiaoTypeEnum.TJSSC.getTagType()));
                // 【定位胆】规则
                betSscbmService.count15(issue, number, Integer.parseInt(CaipiaoTypeEnum.TJSSC.getTagType()));
                // 【定位大小单双】规则
                betSscbmService.countqzh(issue, number, Integer.parseInt(CaipiaoTypeEnum.TJSSC.getTagType()));
            } else if ("1104".equals(lotteryId)) {
                // 结算【两面】
                betSscbmService.countlm(issue, number, Integer.parseInt(CaipiaoTypeEnum.TENSSC.getTagType()));
                // 基本【组选】规则
                betSscbmService.countdn(issue, number, Integer.parseInt(CaipiaoTypeEnum.TENSSC.getTagType()));
                // 【定位胆】规则
                betSscbmService.count15(issue, number, Integer.parseInt(CaipiaoTypeEnum.TENSSC.getTagType()));
                // 【定位大小单双】规则
                betSscbmService.countqzh(issue, number, Integer.parseInt(CaipiaoTypeEnum.TENSSC.getTagType()));
            } else if ("1105".equals(lotteryId)) {
                // 结算【两面】
                betSscbmService.countlm(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVESSC.getTagType()));
                // 基本【组选】规则
                betSscbmService.countdn(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVESSC.getTagType()));
                // 【定位胆】规则
                betSscbmService.count15(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVESSC.getTagType()));
                // 【定位大小单双】规则
                betSscbmService.countqzh(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVESSC.getTagType()));
            } else if ("1106".equals(lotteryId)) {
                // 结算【两面】
                betSscbmService.countlm(issue, number, Integer.parseInt(CaipiaoTypeEnum.JSSSC.getTagType()));
                // 基本【组选】规则
                betSscbmService.countdn(issue, number, Integer.parseInt(CaipiaoTypeEnum.JSSSC.getTagType()));
                // 【定位胆】规则

                betSscbmService.count15(issue, number, Integer.parseInt(CaipiaoTypeEnum.JSSSC.getTagType()));
                // 【定位大小单双】规则
                betSscbmService.countqzh(issue, number, Integer.parseInt(CaipiaoTypeEnum.JSSSC.getTagType()));
            } else if ("1201".equals(lotteryId)) {
                // 结算六合彩- 【特码,正特,六肖,正码1-6】
                betLhcService.clearingLhcTeMaA(issue, number, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()), true);
                betLhcService.clearingLhcZhengTe(issue, number, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()), true);
                betLhcService.clearingLhcZhengMaOneToSix(issue, number, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()), true);
                betLhcService.clearingLhcLiuXiao(issue, number, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()), true);

                // 结算六合彩- 【正码,半波,尾数】
                betLhcService.clearingLhcZhengMaA(issue, number, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()), true);
                betLhcService.clearingLhcBanBo(issue, number, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()), true);
                betLhcService.clearingLhcWs(issue, number, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()), true);

                // 结算六合彩- 【连码,连肖,连尾】
                betLhcService.clearingLhcLianMa(issue, number, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()), true);
                betLhcService.clearingLhcLianXiao(issue, number, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()), true);
                betLhcService.clearingLhcLianWei(issue, number, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()), true);

                // 结算六合彩- 【不中,1-6龙虎,五行】
                betLhcService.clearingLhcNoOpen(issue, number, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()), true);
                betLhcService.clearingLhcOneSixLh(issue, number, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()), true);
                betLhcService.clearingLhcWuxing(issue, number, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()), true);

                // 结算六合彩- 【平特,特肖】
                betLhcService.clearingLhcPtPt(issue, number, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()), true);
                betLhcService.clearingLhcTxTx(issue, number, Integer.parseInt(CaipiaoTypeEnum.LHC.getTagType()), true);
            } else if ("1202".equals(lotteryId)) {
                // 结算六合彩- 【特码,正特,六肖,正码1-6】
                betLhcService.clearingLhcTeMaA(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                betLhcService.clearingLhcZhengTe(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                betLhcService.clearingLhcZhengMaOneToSix(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                betLhcService.clearingLhcLiuXiao(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);

                // 结算六合彩- 【正码,半波,尾数】
                betLhcService.clearingLhcZhengMaA(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                betLhcService.clearingLhcBanBo(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                betLhcService.clearingLhcWs(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);

                // 结算六合彩- 【连码,连肖,连尾】
                betLhcService.clearingLhcLianMa(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                betLhcService.clearingLhcLianXiao(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                betLhcService.clearingLhcLianWei(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);

                // 结算六合彩- 【不中,1-6龙虎,五行】
                betLhcService.clearingLhcNoOpen(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                betLhcService.clearingLhcOneSixLh(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                betLhcService.clearingLhcWuxing(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);

                // 结算六合彩- 【平特,特肖】
                betLhcService.clearingLhcPtPt(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
                betLhcService.clearingLhcTxTx(issue, number, Integer.parseInt(CaipiaoTypeEnum.ONELHC.getTagType()), true);
            } else if ("1203".equals(lotteryId)) {
                // 结算六合彩- 【特码,正特,六肖,正码1-6】
                betLhcService.clearingLhcTeMaA(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                betLhcService.clearingLhcZhengTe(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                betLhcService.clearingLhcZhengMaOneToSix(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                betLhcService.clearingLhcLiuXiao(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);

                // 结算六合彩- 【正码,半波,尾数】
                betLhcService.clearingLhcZhengMaA(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                betLhcService.clearingLhcBanBo(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                betLhcService.clearingLhcWs(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);

                // 结算六合彩- 【连码,连肖,连尾】
                betLhcService.clearingLhcLianMa(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                betLhcService.clearingLhcLianXiao(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                betLhcService.clearingLhcLianWei(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);

                // 结算六合彩- 【不中,1-6龙虎,五行】
                betLhcService.clearingLhcNoOpen(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                betLhcService.clearingLhcOneSixLh(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                betLhcService.clearingLhcWuxing(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);

                // 结算六合彩- 【平特,特肖】
                betLhcService.clearingLhcPtPt(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
                betLhcService.clearingLhcTxTx(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVELHC.getTagType()), true);
            } else if ("1204".equals(lotteryId)) {
                // 结算六合彩- 【特码,正特,六肖,正码1-6】
                betLhcService.clearingLhcTeMaA(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                betLhcService.clearingLhcZhengTe(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                betLhcService.clearingLhcZhengMaOneToSix(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                betLhcService.clearingLhcLiuXiao(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);

                // 结算六合彩- 【正码,半波,尾数】
                betLhcService.clearingLhcZhengMaA(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                betLhcService.clearingLhcBanBo(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                betLhcService.clearingLhcWs(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);

                // 结算六合彩- 【连码,连肖,连尾】
                betLhcService.clearingLhcLianMa(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                betLhcService.clearingLhcLianXiao(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                betLhcService.clearingLhcLianWei(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);

                // 结算六合彩- 【不中,1-6龙虎,五行】
                betLhcService.clearingLhcNoOpen(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                betLhcService.clearingLhcOneSixLh(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                betLhcService.clearingLhcWuxing(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);

                // 结算六合彩- 【平特,特肖】
                betLhcService.clearingLhcPtPt(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
                betLhcService.clearingLhcTxTx(issue, number, Integer.parseInt(CaipiaoTypeEnum.AMLHC.getTagType()), true);
            } else if ("1301".equals(lotteryId)) {
                // 结算【北京PK10-两面】
                betBjpksService.clearingBjpksLm(issue, number, Integer.parseInt(CaipiaoTypeEnum.BJPKS.getTagType()));
                // 结算【北京PK10-猜名次猜前几】
                betBjpksService.clearingBjpksCmcCqj(issue, number, Integer.parseInt(CaipiaoTypeEnum.BJPKS.getTagType()));
                // 结算【北京PK10-冠亚和】
                betBjpksService.clearingBjpksGyh(issue, number, Integer.parseInt(CaipiaoTypeEnum.BJPKS.getTagType()));
            } else if ("1302".equals(lotteryId)) {
                // 结算【北京PK10-两面】
                betBjpksService.clearingBjpksLm(issue, number, Integer.parseInt(CaipiaoTypeEnum.TENPKS.getTagType()));
                // 结算【北京PK10-猜名次猜前几】
                betBjpksService.clearingBjpksCmcCqj(issue, number, Integer.parseInt(CaipiaoTypeEnum.TENPKS.getTagType()));
                // 结算【北京PK10-冠亚和】
                betBjpksService.clearingBjpksGyh(issue, number, Integer.parseInt(CaipiaoTypeEnum.TENPKS.getTagType()));
            } else if ("1303".equals(lotteryId)) {
                // 结算【北京PK10-两面】
                betBjpksService.clearingBjpksLm(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVEPKS.getTagType()));
                // 结算【北京PK10-猜名次猜前几】
                betBjpksService.clearingBjpksCmcCqj(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVEPKS.getTagType()));
                // 结算【北京PK10-冠亚和】
                betBjpksService.clearingBjpksGyh(issue, number, Integer.parseInt(CaipiaoTypeEnum.FIVEPKS.getTagType()));
            } else if ("1304".equals(lotteryId)) {
                // 结算【北京PK10-两面】
                betBjpksService.clearingBjpksLm(issue, number, Integer.parseInt(CaipiaoTypeEnum.JSPKS.getTagType()));
                // 结算【北京PK10-猜名次猜前几】
                betBjpksService.clearingBjpksCmcCqj(issue, number, Integer.parseInt(CaipiaoTypeEnum.JSPKS.getTagType()));
                // 结算【北京PK10-冠亚和】
                betBjpksService.clearingBjpksGyh(issue, number, Integer.parseInt(CaipiaoTypeEnum.JSPKS.getTagType()));
            } else if ("1401".equals(lotteryId)) {
                // 结算【幸运飞艇-两面】
                betXyftService.clearingXyftLm(issue, number);
                // 结算【幸运飞艇-猜名次猜前几】
                betXyftService.clearingXyftCmcCqj(issue, number);
                // 结算【幸运飞艇-冠亚和】
                betXyftService.clearingXyftGyh(issue, number);
            } else if ("1501".equals(lotteryId)) {
                // 结算【PC蛋蛋-特码】
                betPceggService.clearingPceggTm(issue, number);
                // 结算【PC蛋蛋-豹子】
                betPceggService.clearingPceggBz(issue, number);
                // 结算【PC蛋蛋-特码包三】
                betPceggService.clearingPceggTmbs(issue, number);
                // 结算【PC蛋蛋-色波】
                betPceggService.clearingPceggSb(issue, number);
                // 结算【PC蛋蛋-混合】
                betPceggService.clearingPceggHh(issue, number);
            } else if ("1601".equals(lotteryId)) {
                // 结算【两面】
                betSscbmService.countlm(issue, number, Integer.parseInt(CaipiaoTypeEnum.TXFFC.getTagType()));
                // 基本【组选】规则
                betSscbmService.countdn(issue, number, Integer.parseInt(CaipiaoTypeEnum.TXFFC.getTagType()));
                // 【定位胆】规则
                betSscbmService.count15(issue, number, Integer.parseInt(CaipiaoTypeEnum.TXFFC.getTagType()));
                // 【定位大小单双】规则
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
//                // 结算【快乐牛牛-闲家】
//                betNnKlService.countKlXianjia(issue, number, Integer.parseInt(CaipiaoTypeEnum.KLNIU.getTagType()));
//            } else if (lotteryId.equals("1902")) {
//                // 结算【澳洲牛牛-闲家】
//                betNnAzService.countAzXianjia(issue, number, Integer.parseInt(CaipiaoTypeEnum.AZNIU.getTagType()));
//            } else if (lotteryId.equals("1903")) {
//                // 结算【德州牛牛-闲家】
//                betNnJsService.countJsXianjia(issue, number, Integer.parseInt(CaipiaoTypeEnum.JSNIU.getTagType()));
//            } else if (lotteryId.equals("2001")) {
//                // 结算【德州pk番摊】
//                betFtJspksService.clearingJspksJs(issue, number, Integer.parseInt(CaipiaoTypeEnum.JSPKFT.getTagType()));
//            } else if (lotteryId.equals("2002")) {
//                // 结算【番摊】
//                betFtXyftService.clearingFtXyftJs(issue, number, Integer.parseInt(CaipiaoTypeEnum.XYFTFT.getTagType()));
//            } else if (lotteryId.equals("2003")) {
//                // 结算【德州时时彩番摊】
//                betFtSscService.clearingFtSscJs(issue, number, Integer.parseInt(CaipiaoTypeEnum.JSSSCFT.getTagType()));
//            }
            else if ("2201".equals(lotteryId)) {
                //澳洲act结算
                betActAzService.countAzAct(issue, number, Integer.parseInt(CaipiaoTypeEnum.AUSACT.getTagType()));
            } else if ("2202".equals(lotteryId)) {
                // 结算【澳洲时时彩】
                betSscAzService.countAzSsc(issue, number, Integer.parseInt(CaipiaoTypeEnum.AUSSSC.getTagType()));
            } else if ("2203".equals(lotteryId)) {
                // 结算【澳洲F1】
                betF1AzService.countAzF1(issue, number, Integer.parseInt(CaipiaoTypeEnum.AUSPKS.getTagType()));
            }
        } catch (Exception e) {
            logger.error("bet7xcHnService occur error.", e);
            return "结算失败";
        }
        return "结算完成";
    }


}
