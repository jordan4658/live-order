package com.caipiao.live.order.service.lottery;

import com.caipiao.live.common.mybatis.entity.LotteryPlayOdds;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface LotteryPlayOddsWriteService {

    Map<Integer, LotteryPlayOdds> selectPlayOddsBySettingIds(List<Integer> settingIds);

    /**
     * 根据settingId来查询赔率
     * @param settingId
     * @return
     */
    Map<String, LotteryPlayOdds> selectPlayOddsBySettingId(Integer settingId);

    /**
     * 根据settingId来查询赔率
     * @param settingId
     * @return
     */
    LotteryPlayOdds findPlayOddsBySettingId(Integer settingId);

    /**
     * 根据配置id查询相关赔率信息
     * @param settingId 配置id
     * @return
     */
    List<LotteryPlayOdds> selectOddsListBySettingId(Integer settingId);

    /**
     * 精准获取指定号码的赔率
     * @param lotteryId 彩种id
     * @param settingId 配置id
     * @param betNumber 投注号码
     * @return
     */
    BigDecimal countOdds(Integer lotteryId, Integer settingId, String betNumber);
    
    BigDecimal countOddsFC(Integer lotteryId, Integer settingId, String betNumber,Integer winLevel);
    	
    /**
     * 推单赔率
     * @param lotteryId
     * @param settingId
     * @param betNumber
     * @return
     */
	 BigDecimal circleOdds(Integer lotteryId, Integer settingId, String betNumber) ;


    void deleteCaches(Integer oddsId);
}
