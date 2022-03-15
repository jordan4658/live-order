package com.caipiao.live.order.rest.impl.order;

import com.caipiao.live.common.model.common.ResultInfo;
import com.caipiao.live.order.model.dto.OrderDTO;
import com.caipiao.live.common.model.dto.order.OrderFollow;
import com.caipiao.live.common.model.dto.order.ShareOrderDTO;

import com.caipiao.live.order.rest.OrderWriteRest;
import com.caipiao.live.order.service.order.OrderWriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class OrderWriteRestImpl implements OrderWriteRest {

    @Autowired
    private OrderWriteService orderWriteService;

    @Override
    public ResultInfo<Boolean> placeAnOrder(@RequestBody OrderDTO orderDTO) {
        return orderWriteService.placeAnOrder(orderDTO);
    }

    @Override
    public ResultInfo liveRoomCopy(@RequestBody OrderFollow data) {
        return orderWriteService.liveRoomCopy(data);
    }

//    @Override
//    public ResultInfo<OrderPushVo> followOrder(@RequestBody OrderFollow orderFollow) {
//        return orderWriteService.followOrder(orderFollow);
//    }

    @Override
    public ResultInfo<Boolean> backAnOrder(@RequestParam("orderBetId") Integer orderBetId, @RequestParam("userId") Integer userId) {
        return orderWriteService.backAnOrder(orderBetId, userId);
    }

    @Override
    public ResultInfo<String> backOrders(@RequestParam("orderBetIds") List<Integer> orderBetIds, @RequestParam("userId") Integer userId) {
        return orderWriteService.backOrders(orderBetIds, userId);
    }

    @Override
    public ResultInfo<Integer> backOrderByAdmin(@RequestParam(value = "lotteryId", required = false) Integer lotteryId, String issue, String openNumber) {
        return orderWriteService.backOrderByAdmin(lotteryId, issue, openNumber);
    }

    @Override
    public ResultInfo<Integer> reopenOrder(Integer lotteryId, String issue, String openNumber) {
        return orderWriteService.reopenOrder(lotteryId, issue, openNumber);
    }

    @Override
    public ResultInfo<Boolean> jiesuanOrderBetById(Integer id, Double winAmount, String tbStatus, String openNumber) {
        return orderWriteService.jiesuanOrderBetById(id, winAmount, tbStatus, openNumber);
    }

    @Override
    public ResultInfo<Boolean> jiesuanOrderBetByIssue(Integer lotteryId, String issue, String openNumber) {
        return orderWriteService.jiesuanOrderBetByIssue(lotteryId, issue, openNumber);
    }

    @Override
    public ResultInfo cancelOrderBetByIssue(Integer lotteryId, String issue) {
        return orderWriteService.cancelOrderBetByIssue(lotteryId, issue);
    }

    @Override
    public ResultInfo<Boolean> jiesuanByHandle(String issue, String openNumber) {
        return orderWriteService.jiesuanByHandle(issue, openNumber);
    }

    @Override
    public ResultInfo<String> shareOrder(@RequestBody ShareOrderDTO data) {
        return orderWriteService.shareOrder(data);
    }

    //   根据id 结算用户订单 或者修改用户 注单号码
    @Override
    public ResultInfo<Boolean> changeOrderBetById(Integer id, String betNumber) {
        return orderWriteService.changeOrderBetById(id, betNumber);
    }
}
