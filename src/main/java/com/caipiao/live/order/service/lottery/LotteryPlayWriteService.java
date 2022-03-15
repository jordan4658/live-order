package com.caipiao.live.order.service.lottery;

import com.caipiao.live.common.mybatis.entity.LotteryPlay;

import java.util.List;
import java.util.Map;

public interface LotteryPlayWriteService {




    /**
     * 根据玩法id集合获取相应玩法信息
     * @param categoryIds 玩法集合id
     * @return
     */
    Map<Integer, LotteryPlay> selectPlayByIds(List<Integer> categoryIds);

    /**
     * 根据玩法id获取相应玩法信息
     * @param id 玩法id
     * @return
     */
    LotteryPlay selectPlayById(Integer id);

    /**
     * 获取所有未删除的玩法集合
     * @return
     */
    List<LotteryPlay> selectPlayList();

    /**
     * 获取所有未删除的玩法集合
     * @return
     */
    Map<Integer, LotteryPlay> selectPlayMap();


}
