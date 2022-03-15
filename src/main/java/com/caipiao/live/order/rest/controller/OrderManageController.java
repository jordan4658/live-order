package com.caipiao.live.order.rest.controller;

import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.constant.RedisKeys;
import com.caipiao.live.common.model.common.ResultInfo;
import com.caipiao.live.common.util.redis.RedisBusinessUtil;
import com.caipiao.live.order.service.order.OrderWriteService;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/manage")
public class OrderManageController {

    private static final Logger logger = LoggerFactory.getLogger(OrderManageController.class);

    @Resource
    private OrderWriteService orderWriteService;
    @Resource
    private RedissonClient redissonClient;


    /**
     * 根据用户id结算注单
     *
     * @return
     */
    @RequestMapping(name = "根据用户id结算注单", value = "/jiesuanOrderBetById", method = RequestMethod.POST)
    public ResultInfo jiesuanOrderBetById(@RequestParam("id") Integer id, @RequestParam("winAmount") Double winAmount,
                                          @RequestParam("tbStatus") String tbStatus, @RequestParam("openNumber") String openNumber) {

        logger.info("根据用户id结算注单,id:{},winAmount:{},tbStatus:{},openNumber:{}",id,winAmount,tbStatus,openNumber);
        String key = RedisKeys.ORDER_CLEAR + id + Constants.SETTLE_ID;
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + Constants.LOCK);
        boolean bool = false;
        try {
            //一分钟内不能重复结算
            if (RedisBusinessUtil.exists(key)) {
                return ResultInfo.error("每次用户结算相隔1分钟才能操作，请稍后重试");
            } else {
                //超时时间50秒，释放时间30秒
                bool = lock.writeLock().tryLock(50, 30, TimeUnit.SECONDS);
                if (bool) {
                    //一分钟内不能重复结算
                    RedisBusinessUtil.set(key, 1, 60l);
                    ResultInfo<Boolean> result = orderWriteService.jiesuanOrderBetById(id, winAmount, tbStatus, openNumber);
                    return result;
                } else {
                    return ResultInfo.error("系统繁忙，请稍后重试！");
                }
            }
        } catch (Exception e) {
            logger.error("{}.jiesuanOrderBetById,出错:{},params:{}", getClass().getName(), e.getMessage(), "id=" + id + ";winAmount=" + winAmount + ";tbStatus=" + tbStatus + ";openNumber=" + openNumber, e);
            return ResultInfo.error(String.format("手动根据用户id结算注单错误:%s，%s，%s，%s", id, winAmount, tbStatus, openNumber));
        } finally {
            if (bool) {
                lock.writeLock().unlock();
            }
        }
    }


    /**
     * 根据issue结算注单, 10分钟后才能 进行第二次期号结算
     *
     * @return
     */
    @RequestMapping(name = "根据issue结算注单, 10分钟后才能 进行第二次期号结算", value = "/jiesuanOrderBetByIssue", method = RequestMethod.POST)
    public ResultInfo jiesuanOrderBetByIssue(@RequestParam("lotteryId") Integer lotteryId, @RequestParam("issue") String issue,
                                             @RequestParam("openNumber") String openNumber) {
        logger.info("根据issue结算注单,lotteryId:{},issue:{},openNumber:{}",lotteryId,issue,openNumber);
        String key = RedisKeys.ORDER_CLEAR + issue + lotteryId + Constants.SETTLE_ISSUE;
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + Constants.LOCK);
        boolean bool = false;
        try {
            //10分钟内不能重复结算
            if (RedisBusinessUtil.exists(key)) {
                return ResultInfo.error("每次期号结算相隔10分钟才能操作，请稍后重试");
            } else {
                RedisBusinessUtil.set(key, 1, 600l);
                //超时时间100秒，释放时间30秒
                bool = lock.writeLock().tryLock(100, 80, TimeUnit.SECONDS);
                if (bool) {
                    ResultInfo<Boolean> result = orderWriteService.jiesuanOrderBetByIssue(lotteryId, issue, openNumber);
                    return result;
                } else {
                    return ResultInfo.error("系统繁忙，请稍后重试！");
                }
            }
        } catch (Exception e) {
            logger.error("{}.jiesuanOrderBetByIssue,出错:{},params:{}", getClass().getName(), e.getMessage(), "issue=" + issue + ";lotteryId=" + lotteryId + ";openNumber=" + openNumber, e);
            return ResultInfo.error(String.format("手动根据期号结算失败:%s，%s，%s", issue, lotteryId, openNumber));
        } finally {
            if (bool) {
                lock.writeLock().unlock();
            }
        }
    }

    /**
     * 根据issue撤销注单
     *
     * @return
     */
    @RequestMapping(name = "根据issue撤销注单", value = "/cancelOrderBetByIssue", method = RequestMethod.POST)
    public ResultInfo cancelOrderBetByIssue(@RequestParam(value = "lotteryId", required = false) Integer lotteryId, @RequestParam("issue") String issue) {
        logger.info("根据issue撤销注单,lotteryId:{},issue:{}",lotteryId,issue);
        String key = RedisKeys.ORDER_CLEAR + issue + Constants.SETTLE_CANCLE;
        RReadWriteLock lock = redissonClient.getReadWriteLock(key + Constants.LOCK);
        boolean bool = false;
        try {
            //一分钟内不能重复结算
            if (RedisBusinessUtil.exists(key)) {
                return ResultInfo.error("每次期号撤销相隔1分钟才能操作，请稍后重试");
            } else {
                RedisBusinessUtil.set(key, 1, 60l);
                //超时时间100秒，释放时间50秒
                bool = lock.writeLock().tryLock(100, 50, TimeUnit.SECONDS);
                if (bool) {
                    ResultInfo result = orderWriteService.cancelOrderBetByIssue(lotteryId, issue);
                    return result;
                } else {
                    return ResultInfo.error("系统繁忙，请稍后重试！");
                }
            }
        } catch (Exception e) {
            logger.error("{}.cancelOrderBetById,出错:{},params:{}", getClass().getName(), e.getMessage(), "lotteryId=" + lotteryId + ";issue=" + issue, e);
            return ResultInfo.error(String.format("手动根据期号撤销注单失败:%s，%s", issue, lotteryId));
        } finally {
            if (bool) {
                lock.writeLock().unlock();
            }
        }
    }

}
