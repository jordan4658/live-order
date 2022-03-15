package com.caipiao.live.order.rest.impl.lottery;

import com.caipiao.live.common.mybatis.entity.KillConfig;

import com.caipiao.live.order.rest.KillConfigAdminReadRest;
import com.caipiao.live.order.service.lottery.KillConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class KillConfigReadRestImpl implements KillConfigAdminReadRest {

    @Autowired
    private KillConfigService killConfigService;

    @Override
    public List<KillConfig> getKillConfigList() {
        return killConfigService.getKillConfigList();
    }

    @Override
    public KillConfig getKillConfigByLotteryId(Integer lotteryId) {
        return killConfigService.getKillConfigByLotteryId(lotteryId);
    }

    @Override
    public Map<String, String> getAllSetting() {
        return killConfigService.getAllSetting();
    }
}
