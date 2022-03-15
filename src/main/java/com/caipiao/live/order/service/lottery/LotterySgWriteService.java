package com.caipiao.live.order.service.lottery;


import com.caipiao.live.common.model.common.ResultInfo;

public interface LotterySgWriteService {

    /**
     * 修改开奖号码
     *
     * @param lotteryId 彩种id
     * @param issue     期号
     * @param number    新开奖号码
     * @return
     */
    ResultInfo<Integer> changeNumber(Integer lotteryId, String issue, String number);

}
