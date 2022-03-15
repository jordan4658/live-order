package com.caipiao.live.order.service.lottery;


import com.caipiao.live.common.mybatis.entity.KillConfig;

import java.util.List;
import java.util.Map;


public interface KillConfigService {
    /**
     * 查询各个彩种的杀号配置信息
     *
     * @param
     * @return
     */
    List<KillConfig> getKillConfigList();

    KillConfig getKillConfigByLotteryId(Integer lotteryId);

    Map<String, String> getAllSetting();

}
