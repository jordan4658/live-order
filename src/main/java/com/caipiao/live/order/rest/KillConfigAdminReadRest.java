package com.caipiao.live.order.rest;

import com.caipiao.live.common.mybatis.entity.KillConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;


public interface KillConfigAdminReadRest {

    List<KillConfig> getKillConfigList();

    KillConfig getKillConfigByLotteryId( Integer lotteryId);

    Map<String, String> getAllSetting();
}
