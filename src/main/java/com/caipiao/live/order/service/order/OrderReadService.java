package com.caipiao.live.order.service.order;


import com.caipiao.live.common.model.common.ResultInfo;
import com.caipiao.live.common.model.vo.order.OrderTodayListVo;
import com.caipiao.live.order.model.dto.OrderBetDTO;
import com.caipiao.live.order.model.dto.OrderBetRecordResultDTO;
import com.caipiao.live.order.model.vo.OrderBetVO;
import com.github.pagehelper.PageInfo;

import java.util.List;

public interface OrderReadService {


    ResultInfo<List<OrderBetRecordResultDTO>> getCaiDetail(Integer orderId);

    /**
     * 根据信息查询用户投注订单列表
     * @param data 参数对象
     * @return
     */
    PageInfo<OrderBetVO> queryOrderList(OrderBetDTO data);

    /**
     * 根据信息查询用户当天投注指定彩种的订单列表
     * @param data 参数对象
     * @return
     */
    ResultInfo<OrderTodayListVo> queryOrderTodayBetList(OrderBetDTO data);
}


