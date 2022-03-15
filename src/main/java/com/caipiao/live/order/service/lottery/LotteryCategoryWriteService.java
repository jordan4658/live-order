package com.caipiao.live.order.service.lottery;

import com.caipiao.live.common.mybatis.entity.LotteryCategory;

import java.util.List;

public interface LotteryCategoryWriteService {




    /**
     * 查询全部分类列表
     *
     * @return
     */
    List<LotteryCategory> queryAllCategory(String type);
    
}
