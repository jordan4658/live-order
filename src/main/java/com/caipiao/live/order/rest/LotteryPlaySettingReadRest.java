package com.caipiao.live.order.rest;

import com.caipiao.live.common.mybatis.entity.LotteryPlaySetting;

import java.util.Map;


public interface LotteryPlaySettingReadRest {

    Map<String, LotteryPlaySetting> queryLotteryPlaySettingMap(String type);

}







