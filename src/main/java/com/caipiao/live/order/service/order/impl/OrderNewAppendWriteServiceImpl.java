package com.caipiao.live.order.service.order.impl;


import com.caipiao.live.common.enums.lottery.CaipiaoTypeEnum;
import com.caipiao.live.common.mybatis.entity.OrderAppendRecord;
import com.caipiao.live.common.mybatis.entity.OrderRecord;
import com.caipiao.live.common.mybatis.mapper.BetRestrictMapper;
import com.caipiao.live.common.mybatis.mapper.OrderAppendRecordMapper;
import com.caipiao.live.order.service.order.OrderNewAppendWriteService;
import com.caipiao.live.order.service.order.OrderNewWriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;

@Service
public class OrderNewAppendWriteServiceImpl implements OrderNewAppendWriteService {

    @Autowired
    private OrderNewWriteService orderNewWriteService;
    @Autowired
    private OrderAppendRecordMapper orderAppendRecordMapper;
//    @Autowired
//    private MemBaseinfoWriteService memBaseinfoWriteService;
    @Autowired
    private BetRestrictMapper betRestrictMapper;

    /**
     * 生成相应的期号
     *
     * @param lotteryId 彩种id
     * @param issue     第一期期号
     * @param count     下几期期号
     * @return
     */
    @Override
    public String createNextIssue(Integer lotteryId, String issue, int count) {
        if (lotteryId.equals(Integer.parseInt(CaipiaoTypeEnum.TXFFC.getTagType()))) {
            String[] issueSplit = issue.split("-");
            String format = new DecimalFormat("0000").format(Long.valueOf(issueSplit[1]) + count);
            issue = issueSplit[0] + "-" + format;
        } else {
            issue = Long.toString(Long.valueOf(issue) + count);
        }
        return issue;
    }


    @Override
    public void appendOrder(OrderRecord order, BigDecimal winAmount, Boolean isWin) {
        // 判断是否为追号订单
        Integer appendId = order.getAppendId();
        if (appendId.equals(0)) {
            return;
        }
        OrderAppendRecord orderAppendRecord = orderAppendRecordMapper.selectByPrimaryKey(appendId);
        Boolean isStop = orderAppendRecord.getIsStop();
        // 判断是否中奖
        if (isWin) {
            orderAppendRecord.setWinAmount(orderAppendRecord.getWinAmount().add(winAmount));
            orderAppendRecord.setWinCount(orderAppendRecord.getWinCount() + 1);
        }

        // 判断是否停止追号
        if (isStop) {
            orderAppendRecordMapper.updateByPrimaryKey(orderAppendRecord);
            return;
        }

        // 计算当前已投金额
        BigDecimal returnAmount = new BigDecimal(0);
        for (int i = orderAppendRecord.getAppendedCount(); i < orderAppendRecord.getAppendCount(); i++) {
            // 计算追号倍数
            double appendMultiples = orderAppendRecord.getBetMultiples()
                    * (Math.pow(orderAppendRecord.getDoubleMultiples(), i));
            returnAmount = returnAmount.add(orderAppendRecord.getBetPrice().multiply(new BigDecimal(appendMultiples)));
        }

        // 判断中奖后是否追停
        if (orderAppendRecord.getWinStop() && isWin) {
            /**
             * 修改追号信息 已停止追号
             */
            orderAppendRecord.setIsStop(true);
            orderAppendRecordMapper.updateByPrimaryKey(orderAppendRecord);
            //ONELIVE TODO
//
//            // 返还剩余金额
//            MemGoldchangeDO dto = new MemGoldchangeDO();
//            // 设置用户id
//            dto.setUserId(order.getUserId());
//            // 设置备注
//            dto.setOpnote("中奖停止追号, 返还剩余金额！");
//            // 设置类型
//            dto.setChangetype(GoldchangeEnum.APPEND_BET_BACK.getValue());
//            // 余额变动值
//            BigDecimal tradeOffAmount = getTradeOffAmount(returnAmount);
//            dto.setQuantity(tradeOffAmount);
//            // 计算不可提现金额变动值
//            dto.setNoWithdrawalAmount(tradeOffAmount);
//            // 修改用户余额信息
//            memBaseinfoWriteService.updateUserBalance(dto);
            return;
        }

        // 继续追号（生成订单）
        orderNewWriteService.orderAppend(orderAppendRecord, order.getSource());

        /**
         * 修改追号信息 1、已追期数+1 2、判断是否已追到最后一期 3.1 若为最后一期，修改为已停止追号
         */
        // 已追期数+1
        Integer appendedCount = orderAppendRecord.getAppendedCount() + 1;
        orderAppendRecord.setAppendedCount(appendedCount);
        // 判断是否已追到最后一期
        if (appendedCount.equals(orderAppendRecord.getAppendCount())) {
            orderAppendRecord.setIsStop(true);
        }
        orderAppendRecordMapper.updateByPrimaryKey(orderAppendRecord);
    }


}
