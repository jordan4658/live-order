package com.caipiao.live.order.service.lottery;

import com.caipiao.live.common.mybatis.entity.Lottery;

import java.util.List;
import java.util.Map;

public interface LotteryWriteService {
    /**
     * 根据彩种id查询彩种信息
     *
     * @param lotteryId 彩种id
     * @return
     */
    Lottery selectLotteryById(Integer lotteryId);

    /**
     * 查询所有彩种信息
     *
     * @return
     */
    List<Lottery> selectLotteryList(String categoryType);

    /**
     * 查询所有彩票信息
     *
     * @param categoryType
     * @return key:lotteryId, value:Lottery
     */
    Map<Integer, Lottery> selectLotteryMap(String categoryType);


    List<Lottery> queryAllLotteryFromCache();

}
