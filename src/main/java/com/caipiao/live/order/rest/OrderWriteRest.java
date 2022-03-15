package com.caipiao.live.order.rest;

import com.caipiao.live.common.model.common.ResultInfo;
import com.caipiao.live.order.model.dto.OrderDTO;
import com.caipiao.live.common.model.dto.order.OrderFollow;
import com.caipiao.live.common.model.dto.order.ShareOrderDTO;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;



public interface OrderWriteRest {

    /**
     * 投注
     *
     * @param orderDTO 投注信息
     * @return
     */
    ResultInfo<Boolean> placeAnOrder(@RequestBody(required = false) OrderDTO orderDTO);

    /**
     * 跟单
     *
     * @param orderFollow 跟投信息
     * @return
     */
//    @PostMapping("/order/followOrder.json")
//    ResultInfo<OrderPushVo> followOrder(@RequestBody OrderFollow orderFollow);

    /**
     * 撤单
     *
     * @param orderBetId 投注id
     * @return
     */
    ResultInfo<Boolean> backAnOrder(@RequestParam("orderBetId") Integer orderBetId, @RequestParam("userId") Integer userId);

    /**
     * 批量撤单
     *
     * @param orderBetIds 投注id集合
     * @param userId      用户id集合
     * @return
     */
    ResultInfo<String> backOrders(@RequestParam("orderBetIds") List<Integer> orderBetIds, @RequestParam("userId") Integer userId);

    /**
     * 系统撤单
     *
     * @param lotteryId 彩种id
     * @param issue     期号
     * @return
     */
    ResultInfo<Integer> backOrderByAdmin(@RequestParam(value = "lotteryId", required = false) Integer lotteryId, @RequestParam("issue") String issue,
                                         @RequestParam(value = "openNumber", required = false) String openNumber);

    /**
     * 系统撤单
     *
     * @param lotteryId  彩种id
     * @param issue      期号
     * @param openNumber 开奖号码
     * @return
     */
    ResultInfo<Integer> reopenOrder(@RequestParam(value = "lotteryId", required = false) Integer lotteryId, @RequestParam("issue") String issue,
                                    @RequestParam("openNumber") String openNumber);

    /**
     * 根据id结算订单
     *
     * @param id         彩种id
     * @param winAmount  中奖金额
     * @param openNumber 开奖号码
     * @return
     */
    ResultInfo<Boolean> jiesuanOrderBetById(@RequestParam("id") Integer id, @RequestParam("winAmount") Double winAmount, @RequestParam("tbStatus") String tbStatus, @RequestParam(value = "openNumber", required = false) String openNumber);

    /**
     * 根据issue,lottery结算订单
     *
     * @param issue      彩种issue
     * @param lotteryId  lotteryId
     * @param openNumber 开奖号码
     * @return
     */
    ResultInfo<Boolean> jiesuanOrderBetByIssue(@RequestParam(value = "lotteryId", required = false) Integer lotteryId, @RequestParam("issue") String issue, @RequestParam("openNumber") String openNumber);

    /**
     * 根据issue,lottery结算订单
     *
     * @param issue     彩种issue
     * @param lotteryId lotteryId
     * @return
     */
    ResultInfo<Boolean> cancelOrderBetByIssue(@RequestParam(value = "lotteryId", required = false) Integer lotteryId, @RequestParam("issue") String issue);

    /**
     * 六合彩手动结算
     *
     * @param
     * @return
     */
    ResultInfo<Boolean> jiesuanByHandle(@RequestParam("issue") String issue, @RequestParam("openNumber") String openNumber);

    /**
     * 分享跟单
     *
     * @param data
     * @return
     */
    ResultInfo<String> shareOrder(@RequestBody ShareOrderDTO data);


    /**
     * 直播间跟单
     * @param data
     * @return
     */
    ResultInfo liveRoomCopy(@RequestBody OrderFollow data);

    /**
     * 后台订单管理修改用户注单号码
     */
    ResultInfo<Boolean> changeOrderBetById(@RequestParam("id") Integer id, @RequestParam("betNumber") String betNumber);

}
