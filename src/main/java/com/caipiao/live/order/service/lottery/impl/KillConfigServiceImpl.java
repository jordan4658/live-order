package com.caipiao.live.order.service.lottery.impl;

import com.alibaba.fastjson.JSONObject;
import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.constant.RedisKeys;
import com.caipiao.live.common.enums.SysParameterEnum;
import com.caipiao.live.common.mybatis.entity.KillConfig;
import com.caipiao.live.common.mybatis.entity.KillConfigExample;
import com.caipiao.live.common.mybatis.entity.SysParameter;
import com.caipiao.live.common.mybatis.mapper.KillConfigMapper;
import com.caipiao.live.common.service.sys.SysParamService;
import com.caipiao.live.common.util.StringUtils;

import com.caipiao.live.order.service.lottery.KillConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class KillConfigServiceImpl implements KillConfigService {

    @Autowired
    private KillConfigMapper killConfigMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private SysParamService sysParamService;

    @Override
    public List<KillConfig> getKillConfigList() {
        KillConfigExample killConfigExample = new KillConfigExample();
        return killConfigMapper.selectByExample(killConfigExample);
    }

    @Override
    public KillConfig getKillConfigByLotteryId(Integer lotteryId) {
        if (redisTemplate.hasKey("kill" + lotteryId)) {
            KillConfig killConfig = JSONObject.parseObject(redisTemplate.opsForValue().get("kill" + lotteryId).toString(), KillConfig.class);
            return killConfig;
        } else {
            KillConfigExample killConfigExample = new KillConfigExample();
            KillConfigExample.Criteria criteria = killConfigExample.createCriteria();
            criteria.andLotteryIdEqualTo(lotteryId);
            KillConfig killConfig = killConfigMapper.selectOneByExample(killConfigExample);
            redisTemplate.opsForValue().set("kill" + lotteryId, JSONObject.toJSONString(killConfig));
            return killConfig;
        }
    }

    @Override
    public Map<String, String> getAllSetting() {
        Map<String, String> map = new HashMap<>();
        SysParameter platforms = sysParamService.getByCode(SysParameterEnum.PLATFORM_NAME.getCode());//获取平台标识码
        if (null == platforms || StringUtils.isEmpty(platforms.getParamValue())) {
            return null;
        }
        String platformName = platforms.getParamValue();//获取平台标识号
        String onelhc = setKIllValue(Constants.LOTTERY_ONELHC, platformName + RedisKeys.SICAIONELHCRATE);
        String fivelhc = setKIllValue(Constants.LOTTERY_FIVELHC, platformName + RedisKeys.SICAIFIVELHCRATE);
        String tenlhc = setKIllValue(Constants.LOTTERY_AMLHC, platformName + RedisKeys.SICAITENLHCRATE);
        String onessc = setKIllValue(Constants.LOTTERY_DZSSC, platformName + RedisKeys.SICAIONESSCRATE);
        String fivessc = setKIllValue(Constants.LOTTERY_FIVESSC, platformName + RedisKeys.SICAIFIVESSCRATE);
        String tenssc = setKIllValue(Constants.LOTTERY_TENSSC, platformName + RedisKeys.SICAITENSSCRATE);
        String onepks = setKIllValue(Constants.LOTTERY_DZPKS, platformName + RedisKeys.SICAIONEPKSRATE);
        String fivepks = setKIllValue(Constants.LOTTERY_FIVEPKS, platformName + RedisKeys.SICAIFIVEPKSRATE);
        String tenpks = setKIllValue(Constants.LOTTERY_TENPKS, platformName + RedisKeys.SICAITENPKSRATE);
        String txffc = setKIllValue(Constants.LOTTERY_TXFFC, platformName + RedisKeys.SICAITXFFCRATE);
        String azks = setKIllValue(Constants.LOTTERY_AZKS, platformName + RedisKeys.SICAIAZKSRATE);
        String dzks = setKIllValue(Constants.LOTTERY_DZKS, platformName + RedisKeys.SICAIDZKSRATE);
        String dzpcegg = setKIllValue(Constants.LOTTERY_DZPCEGG, platformName + RedisKeys.SICAIDZPCEGGRATE);
        String dzxyft = setKIllValue(Constants.LOTTERY_DZXYFT, platformName + RedisKeys.SICAIDZXYFTRATE);
        String xjplhc = setKIllValue(Constants.LOTTERY_XJPLHC, platformName + RedisKeys.SICAIXJPLHCRATE);
        String time = (String) redisTemplate.opsForValue().get(platformName + RedisKeys.KILLORDERTIME);

        map.put("onelhc", onelhc);
        map.put("fivelhc", fivelhc);
        map.put("tenlhc", tenlhc);
        map.put("onessc", onessc);
        map.put("fivessc", fivessc);
        map.put("tenssc", tenssc);
        map.put("onepks", onepks);
        map.put("fivepks", fivepks);
        map.put("tenpks", tenpks);
        map.put("txffc", txffc);
        map.put("azks", azks);
        map.put("dzks", dzks);
        map.put("dzpcegg", dzpcegg);
        map.put("dzxyft", dzxyft);
        map.put("xjplhc", xjplhc);
        map.put("time", time);

        return map;
    }

    public String setKIllValue(Integer lotteryId, String killKey) { //设置杀号值
        String kind = (String) redisTemplate.opsForValue().get(killKey);
        if (StringUtils.isEmpty(kind)) {
            KillConfig killConfig = getKillConfigByLotteryId(lotteryId);
            if (null != killConfig) {
                kind = String.valueOf(killConfig.getRatio());
                redisTemplate.opsForValue().set(killKey, kind);
            }
        }
        return kind;
    }
}
