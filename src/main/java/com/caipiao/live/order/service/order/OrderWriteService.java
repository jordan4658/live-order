package com.caipiao.live.order.service.order;

import com.caipiao.live.common.model.common.ResultInfo;
import com.caipiao.live.common.model.dto.order.OrderFollow;
import com.caipiao.live.common.model.dto.order.ShareOrderDTO;
import com.caipiao.live.common.mybatis.entity.OrderAppendRecord;
import com.caipiao.live.common.mybatis.entity.OrderBetRecord;
import com.caipiao.live.common.mybatis.entity.OrderRecord;
import com.caipiao.live.order.model.dto.OrderDTO;

import java.util.List;
import java.util.Map;

public interface OrderWriteService {

    /**
     * 投注
     *
     * @param orderDTO 投注信息
     * @return
     */
    ResultInfo<Boolean> placeAnOrder(OrderDTO orderDTO);

    /**
     * 跟单
     *
     * @param orderFollow 跟单信息
     * @return
     */
//    ResultInfo<OrderPushVo> followOrder(OrderFollow orderFollow);

    /**
     * 生成追号单
     *
     * @param orderAppendRecord 追号信息
     * @param source            来源
     * @return
     */
    ResultInfo<Boolean> orderAppend(OrderAppendRecord orderAppendRecord, String source);

    /**
     * 根据条件查找相关订单信息
     *
     * @param lotteryId 彩种id
     * @param issue     期号
     * @param status    状态
     * @return
     */
    List<OrderRecord> selectOrders(Integer lotteryId, String issue, String status);

    int selectOrdersCount(Integer lotteryId, String issue, String status);

    List<OrderRecord> selectOrdersPage(Integer lotteryId, String issue, String status, int pageNo);

    List<OrderBetRecord> selectOrderBetList(String issue, String lotteryId, List<Integer> playIds, String status, String type);

    int updateOrderRecord(String lotteryId, String issue, String sgnumber);

    int countOrderBetList(String issue, List<Integer> playIds, String lotteryId, String status);

    /**
     * 根据条件获取投注信息
     *
     * @param orderIds 订单id集合
     * @param playIds  玩法id集合
     * @param status   状态
     * @return
     */
    List<OrderBetRecord> selectOrderBets(List<Integer> orderIds, List<Integer> playIds, String status);

    /**
     * 根据条件获取投注信息
     *
     * @param orderIds 订单id集合
     * @param playId   玩法id
     * @param status   状态
     * @return
     */
    List<OrderBetRecord> selectOrderBets(List<Integer> orderIds, Integer playId, String status);

    /**
     * 用户主动撤单
     *
     * @param orderBetId 投注id
     * @param userId     用户id
     * @return
     */
    ResultInfo<Boolean> backAnOrder(Integer orderBetId, Integer userId);

    /**
     * 用户主动撤单(批量)
     *
     * @param orderBetIds 订单id集合
     * @param userId      用户id
     * @return
     */
    ResultInfo<String> backOrders(List<Integer> orderBetIds, Integer userId);

    /**
     * 系统撤单
     *
     * @param lotteryId 彩种id
     * @param issue     期号
     * @return
     */
    ResultInfo<Integer> backOrderByAdmin(Integer lotteryId, String issue, String openNumber);

    /**
     * 系统撤单
     *
     * @param lotteryId 彩种id
     * @param issue     期号
     * @return
     */
    ResultInfo<Integer> reopenOrder(Integer lotteryId, String issue, String openNumber);

    /**
     * 判断指定彩种指定期号是否开奖
     *
     * @param lotteryId 彩种id
     * @param issue     期号
     * @param type      类型 1：投注  2：撤单
     * @return
     */
    ResultInfo<Boolean> checkIssueIsOpen(Integer lotteryId, String issue, Integer type);

    /**
     * 更新相应订单的开奖号码
     *
     * @param number       开奖号码
     * @param orderRecords 订单集合
     */
    void updateOrder(String number, List<OrderRecord> orderRecords);

//    /**
//     * 手动开奖
//     *
//     * @param lotteryId  彩种id
//     * @param issue      期号
//     * @param openNumber 开奖号码
//     * @return
//     */
//    ResultInfo<Boolean> openByHandle(Integer lotteryId, String issue, String openNumber);

    /**
     * 手动结算
     *
     * @param id         order_bet_record :id
     * @param winAmount  中奖金额
     * @param openNumber 开奖号码
     * @return
     */
    ResultInfo<Boolean> jiesuanOrderBetById(Integer id, Double winAmount, String tbStatus, String openNumber);

    /**
     * 手动结算
     *
     * @param lotteryId
     * @param issue
     * @param openNumber 开奖号码
     * @return
     */
    ResultInfo<Boolean> jiesuanOrderBetByIssue(Integer lotteryId, String issue, String openNumber);

    /**
     * 手动撤单根据期号
     *
     * @param lotteryId
     * @param issue
     * @return
     */
    ResultInfo<Boolean> cancelOrderBetByIssue(Integer lotteryId, String issue);

    ResultInfo<Boolean> jiesuanBySg(String issue, String openNumber);

    ResultInfo<Boolean> jiesuanByHandle(String issue, String openNumber);


    ResultInfo<Boolean> jiesuanByHandleFalse(String issue, String openNumber);

    /**
     * 订单入库
     *
     * @param order 订单json
     * @return
     */
    void processOrder(String order);

    List<OrderRecord> selectOrdersNoClearn(int count);

    Map<Integer, OrderRecord> getOrderMap(List<OrderBetRecord> orderBetRecords);

    /**
     * 更新订单为退单状态
     *
     * @param orderList
     */
    void updateOrderRecordBackStatus(List<OrderRecord> orderList);

    /**
     * 分享跟单
     *
     * @param data
     * @return
     */
    ResultInfo<String> shareOrder(ShareOrderDTO data);

    /**
     * 直播间跟单
     * @param data
     * @return
     */
    ResultInfo liveRoomCopy(OrderFollow data);

    /**
     * 2021.0611 修改用户注单号码
     *
     * @param id         order_bet_record :id
     *
     * @return
     */

    ResultInfo<Boolean> changeOrderBetById(Integer id, String betNumber);

}
