package com.caipiao.live.order.model.dto;

import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.mybatis.entity.LotteryPlaySetting;
import com.caipiao.live.common.mybatis.entity.OrderBetRecord;
import com.caipiao.live.common.mybatis.entity.OrderRecord;
import com.caipiao.live.common.util.ArrayUtils;
import com.caipiao.live.common.util.CollectionUtil;
import com.caipiao.live.common.util.MathUtil;
import com.caipiao.live.common.util.StringUtils;
import com.caipiao.live.common.util.lottery.KsUtils;
import com.caipiao.live.common.util.lottery.LhcUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.*;

public class OrderDTO extends OrderRecord {
    private static final Logger logger = LoggerFactory.getLogger(com.caipiao.live.common.model.dto.order.OrderDTO.class);
    private Long familymemid;
    private Long roomId;
    private Long familyid;
    private int reOrderNum;
    private List<OrderBetRecord> orderBetList;

    public OrderDTO() {
    }

    public Long getFamilymemid() {
        return this.familymemid;
    }

    public void setFamilymemid(Long familymemid) {
        this.familymemid = familymemid;
    }

    public static Logger getLogger() {
        return logger;
    }

    public Long getRoomId() {
        return this.roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public List<OrderBetRecord> getOrderBetList() {
        return this.orderBetList;
    }

    public Long getFamilyid() {
        return this.familyid;
    }

    public void setFamilyid(Long familyid) {
        this.familyid = familyid;
    }

    public void setOrderBetList(List<OrderBetRecord> orderBetList) {
        this.orderBetList = orderBetList;
    }

    public int getReOrderNum() {
        return this.reOrderNum;
    }

    public void setReOrderNum(int reOrderNum) {
        this.reOrderNum = reOrderNum;
    }

    public boolean isValid(Map<String, LotteryPlaySetting> lotterySettingMap) {
        try {
            if (CollectionUtil.isEmpty(this.orderBetList)) {
                logger.error("下单信息为空");
                return false;
            } else {
                Iterator var2 = this.orderBetList.iterator();

                while(var2.hasNext()) {
                    OrderBetRecord record = (OrderBetRecord)var2.next();
                    if (null == record) {
                        logger.error("下单信息出错：空数据");
                        return false;
                    }

                    if (!StringUtils.isEmpty(record.getBetNumber()) && null != record.getSettingId() && null != record.getPlayId() && null != record.getBetCount() && null != record.getBetAmount()) {
                        if (!record.getBetNumber().startsWith("null") && !record.getBetNumber().startsWith("_") && !record.getBetNumber().contains("__") && record.getBetNumber().contains("@")) {
                            if (betMessageFalse(this.getLotteryId(), record.getPlayId(), record.getPlayName(), record.getBetNumber())) {
                                logger.error("下单信息出错 内容不对  betNumber:{}, settingId:{}, playId:{}, lotteryId:{}, playName:{}", new Object[]{record.getBetNumber(), record.getSettingId(), record.getPlayId(), this.getLotteryId(), record.getPlayName()});
                                return false;
                            }

                            if (!record.getPlayId().toString().contains(this.getLotteryId().toString())) {
                                logger.error("下单信息出错 lotteryId,playId不对应 betNumber:{}, settingId:{}, playId:{}, lotteryId:{}, clientType:{}", new Object[]{record.getBetNumber(), record.getSettingId(), record.getPlayId(), this.getLotteryId(), this.getSource()});
                                return false;
                            }

                            Map<String, String> settingPlayIdMap = new HashMap();
                            Iterator var5 = lotterySettingMap.keySet().iterator();

                            while(var5.hasNext()) {
                                String playId = (String)var5.next();
                                String settingId = ((LotteryPlaySetting)lotterySettingMap.get(playId)).getId().toString();
                                settingPlayIdMap.put(playId.toString(), settingId);
                            }

                            if (!this.getTureZhushu(record)) {
                                logger.error("下单信息出错注数不对 betNumber:{}, settingId:{}, playId:{}, clientType:{}, betCount:{}", new Object[]{record.getBetNumber(), record.getSettingId(), record.getPlayId(), this.getSource(), record.getBetCount()});
                                return false;
                            }

                            if (settingPlayIdMap.get(record.getPlayId().toString()) != null && ((String)settingPlayIdMap.get(record.getPlayId().toString())).equals(record.getSettingId().toString())) {
                                int betAmount = record.getBetAmount().intValue();
                                if (record.getBetAmount().doubleValue() != Double.valueOf(String.valueOf(betAmount))) {
                                    logger.error("下单信息出错单注金额非整数 betNumber:{}, settingId:{}, betAmount:{}, betCount:{}, clientType:{}", new Object[]{record.getBetNumber(), record.getSettingId(), record.getBetAmount(), record.getBetCount(), this.getSource()});
                                    return false;
                                }
                                continue;
                            }

                            logger.error("下单信息出错settingId,playId不对应 betNumber:{}, settingId:{}, playId:{}, clientType:{}", new Object[]{record.getBetNumber(), record.getSettingId(), record.getPlayId(), this.getSource()});
                            return false;
                        }

                        logger.error("下单信息出错格式 betNumber:{}, settingId:{}, playId:{}, clientType:{}", new Object[]{record.getBetNumber(), record.getSettingId(), record.getPlayId(), this.getSource()});
                        return false;
                    }

                    logger.error("下单信息出错空数据 betNumber:{}, settingId:{}, playId:{}, betCount{}, betAmount:{}, clientType:{}", new Object[]{record.getBetNumber(), record.getSettingId(), record.getPlayId(), record.getBetCount(), record.getBetAmount(), this.getSource()});
                    return false;
                }

                return true;
            }
        } catch (Exception var8) {
            logger.error("下单信息出错", var8);
            return false;
        }
    }

    public static boolean betMessageFalse(Integer lotteryId, Integer playId, String playName, String betNumber) {
        if (!Constants.playLhcList.contains(lotteryId)) {
            return false;
        } else {
            boolean isFalse = false;
            String betMessage = betNumber;
            if (!Constants.NEW_LOTTERY_ID_LIST.contains(lotteryId)) {
                betMessage = betNumber.split("@")[1];
            }

            String[] betNumberArray = betMessage.split(",");
            Set<String> numberList = new HashSet();
            String[] var8 = betNumberArray;
            int var9 = betNumberArray.length;

            int var10;
            for(var10 = 0; var10 < var9; ++var10) {
                String single = var8[var10];
                numberList.add(single);
            }

            if (numberList.size() != betNumberArray.length) {
                isFalse = true;
            }

            int[] playType1 = new int[]{11, 12, 13, 14, 15, 21, 22, 23, 24, 25, 26};
            if (ArrayUtils.toList(playType1).contains(Integer.valueOf(playId.toString().substring(4, 6)))) {
                String[] var17 = betNumberArray;
                var10 = betNumberArray.length;

                for(int var20 = 0; var20 < var10; ++var20) {
                    String single = var17[var20];
                    if (Integer.valueOf(single) < 1 || Integer.valueOf(single) > 49) {
                        isFalse = true;
                        break;
                    }
                }
            }

            int[] playType2 = new int[]{37, 38, 39, 40, 41, 42};
            int var13;
            if (ArrayUtils.toList(playType2).contains(Integer.valueOf(playId.toString().substring(4, 6)))) {
                String[] weiArray = new String[]{"0尾", "1尾", "2尾", "3尾", "4尾", "5尾", "6尾", "7尾", "8尾", "9尾"};
                List<String> weiList = CollectionUtils.arrayToList(weiArray);
                String[] var23 = betNumberArray;
                var13 = betNumberArray.length;

                for(int var14 = 0; var14 < var13; ++var14) {
                    String single = var23[var14];
                    if (!weiList.contains(single)) {
                        isFalse = true;
                        break;
                    }
                }
            }

            int[] playType3 = new int[]{29, 30};
            if (ArrayUtils.toList(playType3).contains(Integer.valueOf(playId.toString().substring(4, 6)))) {
                String[] var24 = betNumberArray;
                int var25 = betNumberArray.length;

                for(var13 = 0; var13 < var25; ++var13) {
                    String single = var24[var13];
                    if (!LhcUtils.SHENGXIAO.contains(single)) {
                        isFalse = true;
                        break;
                    }
                }
            }

            return isFalse;
        }
    }

    public boolean getTureZhushu(OrderBetRecord record) {
        record.setLotteryId(Integer.valueOf(record.getPlayId().toString().substring(0, 4)));
        boolean trueZhushu = true;
        String betNumber = null;
        int zhushu = 0;
        if (record.getBetNumber().contains("@")) {
            betNumber = record.getBetNumber().split("@")[1];
        } else {
            betNumber = record.getBetNumber();
        }

        String[] hzArray;
        if (!record.getLotteryId().toString().substring(0, 2).equals("11") && record.getLotteryId() != 1601 && record.getLotteryId() != 2202) {
            String[] lhArray;
            String[] ebthArray;
            String[] ebthdtArray;
            String[] ethdxArray;
            String[] sbthdxArray;
            String[] sbthdtdxArray;
            String[] txArray;
            String[] qianhouArray;
            String[] qianArray;
            if (record.getLotteryId().toString().substring(0, 2).equals("12")) {
                hzArray = new String[]{"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "16", "17", "18", "19", "20", "27", "28", "43", "44"};
                lhArray = new String[]{"12", "13", "14", "31", "32", "37", "38"};
                ebthArray = new String[]{"11", "15", "33", "34", "39", "40"};
                ebthdtArray = new String[]{"35", "36", "41", "42"};
                ethdxArray = new String[]{"21"};
                sbthdxArray = new String[]{"22", "29", "30"};
                sbthdtdxArray = new String[]{"23"};
                txArray = new String[]{"24"};
                qianhouArray = new String[]{"25"};
                qianArray = new String[]{"26"};
                if (ArrayUtils.toString(hzArray).contains(String.format("%02d", record.getPlayId() % 100))) {
                    zhushu = betNumber.split(",").length;
                } else if (ArrayUtils.toString(lhArray).contains(String.format("%02d", record.getPlayId() % 100))) {
                    zhushu = betNumber.split(",").length;
                    zhushu = MathUtil.getCnm(zhushu, 2);
                } else if (ArrayUtils.toString(ebthArray).contains(String.format("%02d", record.getPlayId() % 100))) {
                    zhushu = betNumber.split(",").length;
                    zhushu = MathUtil.getCnm(zhushu, 3);
                } else if (ArrayUtils.toString(ebthdtArray).contains(String.format("%02d", record.getPlayId() % 100))) {
                    zhushu = betNumber.split(",").length;
                    zhushu = MathUtil.getCnm(zhushu, 4);
                } else if (ArrayUtils.toString(ethdxArray).contains(String.format("%02d", record.getPlayId() % 100))) {
                    zhushu = betNumber.split(",").length;
                    zhushu = MathUtil.getCnm(zhushu, 5);
                } else if (ArrayUtils.toString(sbthdxArray).contains(String.format("%02d", record.getPlayId() % 100))) {
                    zhushu = betNumber.split(",").length;
                    zhushu = MathUtil.getCnm(zhushu, 6);
                } else if (ArrayUtils.toString(sbthdtdxArray).contains(String.format("%02d", record.getPlayId() % 100))) {
                    zhushu = betNumber.split(",").length;
                    zhushu = MathUtil.getCnm(zhushu, 7);
                } else if (ArrayUtils.toString(txArray).contains(String.format("%02d", record.getPlayId() % 100))) {
                    zhushu = betNumber.split(",").length;
                    zhushu = MathUtil.getCnm(zhushu, 8);
                } else if (ArrayUtils.toString(qianhouArray).contains(String.format("%02d", record.getPlayId() % 100))) {
                    zhushu = betNumber.split(",").length;
                    zhushu = MathUtil.getCnm(zhushu, 9);
                } else if (ArrayUtils.toString(qianArray).contains(String.format("%02d", record.getPlayId() % 100))) {
                    zhushu = betNumber.split(",").length;
                    zhushu = MathUtil.getCnm(zhushu, 10);
                }
            } else if (!record.getLotteryId().toString().substring(0, 2).equals("13") && record.getLotteryId() != 2203) {
                if (record.getLotteryId().toString().substring(0, 2).equals("14")) {
                    hzArray = new String[]{"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14"};
                    if (ArrayUtils.toString(hzArray).contains(String.format("%02d", record.getPlayId() % 100))) {
                        zhushu = betNumber.split(",").length;
                    }
                } else if (record.getLotteryId().toString().substring(0, 2).equals("15")) {
                    hzArray = new String[]{"01", "02", "03", "05"};
                    lhArray = new String[]{"04"};
                    if (ArrayUtils.toString(hzArray).contains(String.format("%02d", record.getPlayId() % 100))) {
                        zhushu = betNumber.split(",").length;
                    } else if (ArrayUtils.toString(lhArray).contains(String.format("%02d", record.getPlayId() % 100))) {
                        zhushu = betNumber.split(",").length;
                        zhushu = MathUtil.getCnm(zhushu, 3);
                    }
                } else if (record.getLotteryId().toString().substring(0, 2).equals("19")) {
                    hzArray = new String[]{"01"};
                    if (ArrayUtils.toString(hzArray).contains(String.format("%02d", record.getPlayId() % 100))) {
                        zhushu = betNumber.split(",").length;
                    }
                } else if (record.getLotteryId().toString().substring(0, 2).equals("20")) {
                    hzArray = new String[]{"01"};
                    if (ArrayUtils.toString(hzArray).contains(String.format("%02d", record.getPlayId() % 100))) {
                        zhushu = betNumber.split(",").length;
                    }
                } else if (record.getLotteryId() == 2201) {
                    hzArray = new String[]{"01"};
                    if (ArrayUtils.toString(hzArray).contains(String.format("%02d", record.getPlayId() % 100))) {
                        zhushu = betNumber.split(",").length;
                    }
                } else {
                    if (!record.getLotteryId().toString().substring(0, 2).equals("23")) {
                        return true;
                    }

                    hzArray = new String[]{"01", "02", "10", "07"};
                    lhArray = new String[]{"03"};
                    ebthArray = new String[]{"04"};
                    ebthdtArray = new String[]{"05"};
                    ethdxArray = new String[]{"06"};
                    sbthdxArray = new String[]{"08"};
                    sbthdtdxArray = new String[]{"09"};
                    txArray = new String[]{"11"};
                    if (ArrayUtils.toString(hzArray).contains(String.format("%02d", record.getPlayId() % 100))) {
                        zhushu = betNumber.split(",").length;
                    }

                    if (ArrayUtils.toString(lhArray).contains(String.format("%02d", record.getPlayId() % 100))) {
                        if ("三连号".equals(record.getPlayName())) {
                            zhushu = 1;
                        } else if ("二连号".equals(record.getPlayName())) {
                            zhushu = betNumber.split(",").length;
                        }
                    } else if (ArrayUtils.toString(ebthArray).contains(String.format("%02d", record.getPlayId() % 100))) {
                        zhushu = betNumber.split(",").length;
                        zhushu = MathUtil.getCnm(zhushu, 2);
                    } else {
                        String[] houArray;
                        if (ArrayUtils.toString(ebthdtArray).contains(String.format("%02d", record.getPlayId() % 100))) {
                            qianhouArray = betNumber.split("_");
                            qianArray = qianhouArray[0].split(",");
                            houArray = qianhouArray[1].split(",");
                            zhushu = qianArray.length * houArray.length;
                        } else if (ArrayUtils.toString(ethdxArray).contains(String.format("%02d", record.getPlayId() % 100))) {
                            qianhouArray = betNumber.split("_");
                            qianArray = qianhouArray[0].split(",");
                            houArray = qianhouArray[1].split(",");
                            zhushu = qianArray.length * houArray.length;
                        } else if (ArrayUtils.toString(sbthdxArray).contains(String.format("%02d", record.getPlayId() % 100))) {
                            zhushu = betNumber.split(",").length;
                            zhushu = MathUtil.getCnm(zhushu, 3);
                        } else if (ArrayUtils.toString(sbthdtdxArray).contains(String.format("%02d", record.getPlayId() % 100))) {
                            qianhouArray = betNumber.split("_");
                            int qian = qianhouArray[0].split(",").length;
                            int hou = qianhouArray[1].split(",").length;
                            zhushu = MathUtil.getCnm(qian, 2) * hou + MathUtil.getCnm(hou, 2) * qian;
                        }
                    }

                    if (ArrayUtils.toString(txArray).contains(String.format("%02d", record.getPlayId() % 100))) {
                        zhushu = 1;
                    }
                }
            } else {
                hzArray = new String[]{"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14"};
                if (ArrayUtils.toString(hzArray).contains(String.format("%02d", record.getPlayId() % 100))) {
                    zhushu = betNumber.split(",").length;
                }
            }
        } else {
            hzArray = new String[]{"01", "02", "03", "04", "05", "06", "07", "08", "09"};
            if (ArrayUtils.toString(hzArray).contains(String.format("%02d", record.getPlayId() % 100))) {
                zhushu = betNumber.split(",").length;
            }
        }

        if (zhushu != record.getBetCount()) {
            trueZhushu = false;
        }

        return trueZhushu;
    }

    public boolean isValidNew(Map<String, LotteryPlaySetting> lotterySettingMap) {
        try {
            if (CollectionUtil.isEmpty(this.orderBetList)) {
                logger.error("下单信息为空");
                return false;
            } else {
                Iterator var2 = this.orderBetList.iterator();

                while(var2.hasNext()) {
                    OrderBetRecord record = (OrderBetRecord)var2.next();
                    if (null == record) {
                        logger.error("下单信息出错：空数据");
                        return false;
                    }

                    if (!StringUtils.isEmpty(record.getBetNumber()) && null != record.getSettingId() && null != record.getPlayId() && null != record.getBetCount() && null != record.getBetAmount()) {
                        if (!record.getBetNumber().startsWith("_") && !record.getBetNumber().contains("__") && !record.getBetNumber().contains("@") ) {
                            if (betMessageFalse(this.getLotteryId(), record.getPlayId(), record.getPlayName(), record.getBetNumber())) {
                                logger.error("下单信息出错 内容不对  betNumber:{}, settingId:{}, playId:{}, lotteryId:{}", new Object[]{record.getBetNumber(), record.getSettingId(), record.getPlayId(), this.getLotteryId()});
                                return false;
                            }

                            if (!record.getPlayId().toString().contains(this.getLotteryId().toString())) {
                                logger.error("下单信息出错 lotteryId,playId不对应 betNumber:{}, settingId:{}, playId:{}, lotteryId:{}, clientType:{}", new Object[]{record.getBetNumber(), record.getSettingId(), record.getPlayId(), this.getLotteryId(), this.getSource()});
                                return false;
                            }

                            Map<String, String> settingPlayIdMap = new HashMap();
                            Iterator var5 = lotterySettingMap.keySet().iterator();

                            while(var5.hasNext()) {
                                String playId = (String)var5.next();
                                String settingId = ((LotteryPlaySetting)lotterySettingMap.get(playId)).getId().toString();
                                settingPlayIdMap.put(playId.toString(), settingId);
                            }

                            if (!this.getTureZhushu(record)) {
                                logger.error("下单信息出错注数不对 betNumber:{}, settingId:{}, playId:{}, clientType:{}, betCount:{}", new Object[]{record.getBetNumber(), record.getSettingId(), record.getPlayId(), this.getSource(), record.getBetCount()});
                                return false;
                            }

                            if (settingPlayIdMap.get(record.getPlayId().toString()) != null && ((String)settingPlayIdMap.get(record.getPlayId().toString())).equals(record.getSettingId().toString())) {
                                int betAmount = record.getBetAmount().intValue();
                                if (record.getBetAmount().doubleValue() != Double.valueOf(String.valueOf(betAmount))) {
                                    logger.error("下单信息出错单注金额非整数 betNumber:{}, settingId:{}, betAmount:{}, betCount:{}, clientType:{}", new Object[]{record.getBetNumber(), record.getSettingId(), record.getBetAmount(), record.getBetCount(), this.getSource()});
                                    return false;
                                }

                                Integer playId = Integer.parseInt(record.getPlayId().toString());
                                if (Constants.NEW_LOTTERY_DT_PLAYID_LIST.contains(playId)) {
                                    boolean judge = KsUtils.judgeDmAndTm(record.getBetNumber());
                                    if (judge) {
                                        logger.error("下单信息出错胆码拖码不能相同或为空 betNumber:{}, settingId:{}, betAmount:{}, betCount:{}, clientType:{}", new Object[]{record.getBetNumber(), record.getSettingId(), record.getBetAmount(), record.getBetCount(), this.getSource()});
                                        return false;
                                    }
                                }
                                continue;
                            }

                            logger.error("下单信息出错settingId,playId不对应 betNumber:{}, settingId:{}, playId:{}, clientType:{}", new Object[]{record.getBetNumber(), record.getSettingId(), record.getPlayId(), this.getSource()});
                            return false;
                        }

                        logger.error("下单信息出错格式 betNumber:{},playName:{}, settingId:{}, playId:{}, clientType:{}", new Object[]{record.getBetNumber(), record.getPlayName(), record.getSettingId(), record.getPlayId(), this.getSource()});
                        return false;
                    }

                    logger.error("下单信息出错空数据 betNumber:{}, settingId:{}, playId:{}, betCount{}, betAmount:{}, clientType:{}", new Object[]{record.getBetNumber(), record.getSettingId(), record.getPlayId(), record.getBetCount(), record.getBetAmount(), this.getSource()});
                    return false;
                }

                return true;
            }
        } catch (Exception var8) {
            logger.error("下单信息出错", var8);
            return false;
        }
    }
}
