package com.caipiao.live.order.rest.impl.lottery;

import com.caipiao.live.common.constant.RedisKeys;
import com.caipiao.live.common.model.dto.lottery.LotteryPlayAllDTO;
import com.caipiao.live.common.mybatis.entity.LotteryCategory;
import com.caipiao.live.common.mybatis.entity.LotteryPlaySetting;

import com.caipiao.live.common.util.CollectionUtil;

import com.caipiao.live.order.rest.LotteryPlaySettingReadRest;
import com.caipiao.live.order.service.lottery.LotteryCategoryService;
import com.caipiao.live.order.service.lottery.LotteryPlayService;
import com.caipiao.live.order.service.lottery.LotteryPlaySettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class LotteryPlaySettingReadRestImpl implements LotteryPlaySettingReadRest {

    private static final Logger logger = LoggerFactory.getLogger(LotteryPlaySettingReadRestImpl.class);
    @Autowired
    private LotteryPlayService lotteryPlayService;
    @Autowired
    private LotteryPlaySettingService lotteryPlaySettingService;
    @Autowired
    private LotteryCategoryService lotteryCategoryService;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public Map<String, LotteryPlaySetting> queryLotteryPlaySettingMap(String type) {
        Map<String, LotteryPlaySetting> data = redisTemplate.opsForHash().entries(RedisKeys.LOTTERY_PLAY_SETTING_MAP_TYPE + type);
        if (CollectionUtil.isNotEmpty(data)) {
            return data;
        }
        //查询所有彩种
        List<LotteryCategory> categoryList = lotteryCategoryService.queryAllCategory(type);

        //查询玩法，一级玩法；二级玩法
        List<Integer> categoryIdList = categoryList.stream().map(item -> item.getCategoryId()).collect(Collectors.toList());
        Map<Integer, List<LotteryPlayAllDTO>> lotteryPlayAllDTOMap = lotteryPlayService.selectAllLotteryPlayDTOByCategoryIds(categoryIdList);

        //查询玩法设置
        List<Integer> playIdList = getPlayIdList(lotteryPlayAllDTOMap);
        Map<String, LotteryPlaySetting> dataMap = lotteryPlaySettingService.queryLotteryPlaySettingMap(playIdList);
        if (CollectionUtil.isNotEmpty(dataMap)) {
            redisTemplate.opsForHash().putAll(RedisKeys.LOTTERY_PLAY_SETTING_MAP_TYPE + type, dataMap);
        }
        return dataMap;
    }

    private List<Integer> getPlayIdList(Map<Integer, List<LotteryPlayAllDTO>> lotteryPlayAllDTOMap) {
        List<Integer> playIdList = new ArrayList<>();
        for (Map.Entry<Integer, List<LotteryPlayAllDTO>> entry : lotteryPlayAllDTOMap.entrySet()) {
            for (LotteryPlayAllDTO dto : entry.getValue()) {
                getAllPlayId(dto, playIdList);
            }
        }
        playIdList = playIdList.stream().distinct().collect(Collectors.toList());
        logger.info("getPlayIdList size:{}.", playIdList.size());
        return playIdList;
    }

//	@Override
//	public LotteryPlaySettingDTO queryByPlayId(@RequestParam(name = "playId") Integer playId) {
//		return lotteryPlaySettingService.queryByPlayId(playId);
//	}
//
//	@Override
//	public PageResult<List<LotteryPlaySettingDTO>> querySettingListByCateId(@RequestParam("cateId") Integer cateId, @RequestParam("pageNo") Integer pageNo, @RequestParam("pageSize") Integer pageSize) {
//		return lotteryPlaySettingService.querySettingListByCateId(cateId, pageNo, pageSize);
//	}

    private void getAllPlayId(LotteryPlayAllDTO dto, List<Integer> playIdList) {
        playIdList.add(dto.getId());
        List<LotteryPlayAllDTO> playChildren = dto.getPlayChildren();
        if (null != playChildren && playChildren.size() > 0) {
            for (LotteryPlayAllDTO child : playChildren) {
                getAllPlayId(child, playIdList);
            }
        }
    }
}
