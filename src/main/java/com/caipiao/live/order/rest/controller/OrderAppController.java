package com.caipiao.live.order.rest.controller;

import com.alibaba.fastjson.JSONObject;
import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.enums.StatusCode;
import com.caipiao.live.common.exception.BusinessException;
import com.caipiao.live.common.model.common.ResultInfo;
import com.caipiao.live.common.model.dto.order.OrderFollow;
import com.caipiao.live.common.model.vo.order.OrderPushVo;
import com.caipiao.live.common.model.vo.order.OrderTodayListVo;
import com.caipiao.live.common.mybatis.entity.OrderBetRecord;
import com.caipiao.live.common.util.StringUtils;
import com.caipiao.live.order.constant.RedisKeys;
import com.caipiao.live.order.model.dto.OrderBetDTO;
import com.caipiao.live.order.model.dto.OrderDTO;
import com.caipiao.live.order.model.vo.OrderBetVO;
import com.caipiao.live.order.rest.OrderWriteRest;
import com.caipiao.live.order.service.bet.OrderBetService;
import com.caipiao.live.order.service.lottery.LotterySgService;
import com.caipiao.live.order.service.order.OrderReadService;
import com.github.pagehelper.PageInfo;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/bet")
public class OrderAppController {

    private static final Logger logger = LoggerFactory.getLogger(OrderAppController.class);

    @Autowired
    private OrderWriteRest orderWriteRest;
    @Autowired
    private OrderBetService orderBetService;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private OrderReadService orderReadService;
    @Resource
    private LotterySgService lotterySgService;

    /**
     * 老彩种购彩投注
     *
     * @param data
     * @return
     */
    @RequestMapping(value = "/orderBet", method = RequestMethod.POST)
    public ResultInfo orderBet(@RequestBody(required = false) OrderDTO data) {
        long startTime = System.currentTimeMillis();
        ResultInfo resultInfo = new ResultInfo();

        RReadWriteLock lock = redissonClient.getReadWriteLock(RedisKeys.LOCK_LOTTERY_ORDER_BET + data.getUserId());
        try {
            boolean bool = lock.writeLock().tryLock(15, 10, TimeUnit.SECONDS);
            if (bool) {
                //购彩投注信息合法性校验
                //代表老彩种校验标识码
                Integer start = Constants.DEFAULT_ONE;
                ResultInfo betResult = orderBetService.bettingInformationVerification(data, startTime, start);
                if(betResult.getCode() != StatusCode.SUCCESSCODE.getCode()){
                    return betResult;
                }

                // 生成订单
                ResultInfo<Boolean> result = orderWriteRest.placeAnOrder(data);
                long end = System.currentTimeMillis();
                if ((end - startTime) > 3000) {
                    logger.info("orderWriteRest.placeAnOrder, [{}] milliseconds", end - startTime);
                }
                resultInfo = result;
            } else {
                ResultInfo.error("未拿到锁");
                logger.info("{} 未拿到锁", data.getUserId());
            }
            logger.info("{}.orderBet投注成功！params:{}", getClass().getName(), JSONObject.toJSONString(data));
        } catch (BusinessException e) {
            logger.error("{}.orderBet投注失败,失败信息:{},params:{}", getClass().getName(), e.getMessage(), JSONObject.toJSONString(data), e);
            resultInfo.setMsg(e.getMessage());
            resultInfo.setCode(e.getCode());
        } catch (Exception e) {
            logger.error("{}.orderBet投注出错,出错信息:{},params:{}", getClass().getName(), e.getMessage(), JSONObject.toJSONString(data), e);
            resultInfo = ResultInfo.error("投注出錯");
        } finally {
            lock.writeLock().unlock();
        }
        logger.info("/bet/orderBet耗时{}毫秒", (System.currentTimeMillis() - startTime));
        return resultInfo;
    }

    /**
     * 新彩种购彩投注
     *
     * @param data
     * @param data
     * @return
     */
    @RequestMapping(value = "/orderBetNew", method = RequestMethod.POST)
    public ResultInfo orderBetNew(@RequestBody(required = false) OrderDTO data) {
        long startTime = System.currentTimeMillis();
        ResultInfo resultInfo = new ResultInfo();
        if (data == null || StringUtils.isBlank(data.getIssue()) || null == data.getLotteryId()
                || null == data.getUserId() || data.getOrderBetList().size() == 0) {
            return ResultInfo.getInstance(StatusCode.PARAM_ERROR);
        }

        RReadWriteLock lock = redissonClient.getReadWriteLock(RedisKeys.LOCK_LOTTERY_ORDER_NEW_BET + data.getUserId());
        try {
            boolean bool = lock.writeLock().tryLock(15, 10, TimeUnit.SECONDS);
            if (bool) {
                //购彩投注信息合法性校验
                //代表新彩种校验标识码
                Integer start = Constants.DEFAULT_TWO;
                ResultInfo betResult = orderBetService.bettingInformationVerification(data, startTime, start);
                if(betResult.getCode() != StatusCode.SUCCESSCODE.getCode()){
                    return betResult;
                }
                // 生成订单
                ResultInfo<Boolean> result = orderWriteRest.placeAnOrder(data);
                long end = System.currentTimeMillis();
                if ((end - startTime) > 3000) {
                    logger.info("orderWriteRest.placeAnOrder, [{}] milliseconds", end - startTime);
                }
                resultInfo = result;
            } else {
                logger.info("{} 未拿到锁", data.getUserId());
            }
            logger.info("{}.orderBetNew新彩种购彩投注成功,params:{}", getClass().getName(), JSONObject.toJSONString(data));
        } catch (BusinessException e) {
            logger.error("{}.orderBetNew新彩种购彩投注失败,失败信息:{},params:{}", getClass().getName(), e.getMessage(), JSONObject.toJSONString(data), e);
            resultInfo.setMsg(e.getMessage());
            resultInfo.setCode(e.getCode());
        } catch (Exception e) {
            logger.error("{}.orderBetNew新彩种购彩投注出错,出错信息:{},params:{}", getClass().getName(), e.getMessage(), JSONObject.toJSONString(data), e);
            resultInfo = ResultInfo.error("新彩種購彩投注出錯");
        } finally {
            lock.writeLock().unlock();
        }
        logger.info("/bet/orderBetNew耗时{}毫秒", (System.currentTimeMillis() - startTime));
        return resultInfo;
    }

    /**
     * 直播间跟单
     *
     * @param data
     * @return
     */
    @RequestMapping(value = "/liveRoomCopy", method = RequestMethod.POST)
    public ResultInfo<OrderPushVo> liveRoomCopy(@RequestBody OrderFollow data) {
        ResultInfo resultInfo = ResultInfo.ok();
        // 获取来源

        if (null == data || null == data.getOrders() || null == data.getUserId() || data.getUserId() < 1
               || null == data.getRoomId() || data.getOrders().size() == 0) {
            return ResultInfo.getInstance(StatusCode.PARAM_ERROR);
        }
        try {
            // 生成订单
            resultInfo = orderWriteRest.liveRoomCopy(data);
        } catch (BusinessException e) {
            resultInfo.setMsg(e.getMessage());
            resultInfo.setCode(e.getCode());
            logger.error("{}.LiveRoomCopy 跟单出错,params:{}", getClass().getName(), e.getMessage(), JSONObject.toJSONString(data), e);
        } catch (Exception e) {
            logger.error("{}.LiveRoomCopy 跟单出错,params:{}", getClass().getName(), e.getMessage(), JSONObject.toJSONString(data), e);
            resultInfo = ResultInfo.error("跟單出錯");
        }
        return resultInfo;
    }

    /**
     * 彩票详情接口
     *
     * @param data
     * @return
     */
    @RequestMapping(value = "/getCaiDetail", method = RequestMethod.POST)
    public ResultInfo getCaiDetail(@RequestBody OrderFollow data) {
        ResultInfo response = ResultInfo.ok();
        if (null == data.getOrderId() || data.getOrderId() <= 0) {
            return ResultInfo.error("參數錯誤");
        }
        //订单id
        try {
            response = orderReadService.getCaiDetail(data.getOrderId());
        } catch (BusinessException e) {
            response.setMsg(e.getMessage());
            response.setCode(e.getCode());
            logger.error("{}.getCaiDetail 彩票详情接口失败,params:{}", getClass().getName(), e.getMessage(), data.getOrderId(), e);
        } catch (Exception e) {
            logger.error("{}.LiveRoomCopy 彩票详情接口出错,params:{}", getClass().getName(), e.getMessage(), data.getOrderId(), e);
            response = ResultInfo.error("彩票詳情接口失敗");
        }
        return response;
    }

    /**
     * 获取用户投注列表
     *
     * @param data
     * @return
     */
    @RequestMapping(value = "/orderList", method = RequestMethod.POST)
    public ResultInfo<PageInfo<OrderBetVO>> orderList(@RequestBody OrderBetDTO data) {
        ResultInfo<PageInfo<OrderBetVO>> resultInfo = new ResultInfo();
        Integer userId = data.getUserId();
        try{
            resultInfo.setData(orderReadService.queryOrderList(data));
        } catch (BusinessException e) {
            logger.error("{}.orderList用户:{}获取用户投注列表失败.失败信息:{}, params:{}", getClass().getName(), userId, e.getMessage(), JSONObject.toJSONString(data), e);
            resultInfo.setMsg(e.getMessage());
            resultInfo.setCode(e.getCode());
        } catch (Exception e) {
            logger.error("{}.orderList用户:{}获取用户投注列表出错,出错信息:{}, params:{}", getClass().getName(), userId, e.getMessage(), JSONObject.toJSONString(data), e);
            resultInfo = ResultInfo.error("获取用户投注列表出错");
        }
        return resultInfo;
    }

    /**
     * 今日投注金额 赢利 未结算 已结算
     */
//    @CheckLogin
    @PostMapping("/orderTodayBetList.json")
    public ResultInfo<OrderTodayListVo> orderTodayBetList(@RequestBody OrderBetDTO data) {
        long startTime = System.currentTimeMillis();
        Integer userId = data.getUserId();
        ResultInfo<OrderTodayListVo> resultInfo = new ResultInfo<>();
        if (userId == null || data.getLotteryId() == null) {
            return ResultInfo.error("查询参数有误");
        }
        try {
            resultInfo = orderReadService.queryOrderTodayBetList(data);
            logger.info("{}.orderTodayBetList用户:{}获取今日投注金额成功,params:{}", getClass().getName(), userId, JSONObject.toJSONString(data));
        } catch (BusinessException e) {
            logger.error("{}.orderTodayBetList用户:{}获取今日投注金额失败.失败信息:{}, params:{}", getClass().getName(), userId, e.getMessage(), JSONObject.toJSONString(data), e);
            resultInfo.setMsg(e.getMessage());
            resultInfo.setCode(e.getCode());
        } catch (Exception e) {
            logger.error("{}.orderTodayBetList用户:{}获取今日投注金额出错,出错信息:{}, params:{}", getClass().getName(), userId, e.getMessage(), JSONObject.toJSONString(data), e);
            resultInfo = ResultInfo.error("获取今日投注金额出错");
        }
        logger.info("/bet/orderList耗时{}毫秒", (System.currentTimeMillis() - startTime));
        return resultInfo;
    }

    /**
     * 撤单
     */
    @PostMapping("/orderBack.json")
    public ResultInfo orderBack(@RequestBody OrderBetRecord data) {
        if (null == data.getId() || data.getId() <= 0 || null == data.getUserId()) {
            return ResultInfo.error("参数错误");
        }
        try {
            return this.orderWriteRest.backAnOrder(data.getId(), data.getUserId());
        } catch (Exception e) {
            logger.error("撤单出错,params:{}", JSONObject.toJSONString(data), e);
            return ResultInfo.error("撤单出错");
        }
    }

    /**
     * 查询app获取多个彩种的开奖历史信息
     *
     * @return
     */
    @PostMapping("/sg/lishiSg.json")
    public ResultInfo<Map<String, Object>> lishiSg(@RequestParam("pageNo") Integer pageNo,
                                            @RequestParam("pageSize") Integer pageSize, @RequestParam("id") Integer id){
        if (null == id ) {
            return ResultInfo.error("参数错误");
        }
        try {
            return ResultInfo.ok(lotterySgService.lishiSg(pageNo,pageSize,id));
        }catch (Exception e){
            logger.error("查询app获取多个彩种的开奖历史信息出错,params:{}", id.toString(), e);
            return ResultInfo.error("查询app获取多个彩种的开奖历史信息出错");
        }
    }

}
