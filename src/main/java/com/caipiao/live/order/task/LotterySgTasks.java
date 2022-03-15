package com.caipiao.live.order.task;

import com.caipiao.live.common.constant.RedisKeys;
import com.caipiao.live.common.util.redis.RedisLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@EnableScheduling
@Component
public class LotterySgTasks {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     *  分分彩
     */
//    @Scheduled(cron = "0 0/1 * * * ?")
    @Scheduled(cron = "0 0/1 * * * ?")
    public void dealOneMinute() {
        String key = "kyTaskOne";
        RedisLock lock = new RedisLock(key+"lock", RedisLock.TIMEOUT_ONE_HUNDRED_MSECS);
        logger.info("清除分分彩赛果缓存开始");
        try {
            if (lock.lock()) {
                if(redisTemplate.opsForValue().get(key) == null){
                    List<String> keyList = new ArrayList<>();
                    keyList.add(RedisKeys.JSSSC_NEXT_VALUE);
                    keyList.add(RedisKeys.ONELHC_NEXT_VALUE);
                    keyList.add(RedisKeys.JSPKS_NEXT_VALUE);
                    keyList.add(RedisKeys.TXFFC_NEXT_VALUE);
                    keyList.add(RedisKeys.JSPKFT_NEXT_VALUE);
                    keyList.add(RedisKeys.JSSSCFT_NEXT_VALUE);

                    keyList.add(RedisKeys.DZKS_NEXT_VALUE);
                    keyList.add(RedisKeys.DZPCDAND_NEXT_VALUE);
                    keyList.add(RedisKeys.DZXYFEIT_NEXT_VALUE);
                    redisTemplate.delete(keyList);
                    redisTemplate.opsForValue().set(key,1,50, TimeUnit.SECONDS);
                }

            }
        } catch (Exception e) {
            logger.error("LotterySgTasks dealOneMinute is error {} "+e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    /**
     *  5分彩
     *  从0分0秒开始的
     */
    @Scheduled(cron = "0 0/5 * * * ?")
    public void dealFiveMinute() {
        String key = "kyTaskFive";
        RedisLock lock = new RedisLock(key+"lock", RedisLock.TIMEOUT_ONE_HUNDRED_MSECS);
        try {
            if (lock.lock()) {
                if(redisTemplate.opsForValue().get(key) == null){
                    List<String> keyList = new ArrayList<>();
                    keyList.add(RedisKeys.FIVESSC_NEXT_VALUE);
                    keyList.add(RedisKeys.FIVELHC_NEXT_VALUE);
                    keyList.add(RedisKeys.FIVEPKS_NEXT_VALUE);
                    keyList.add(RedisKeys.PCDAND_NEXT_VALUE);

                    keyList.add(RedisKeys.XJPLHC_NEXT_VALUE);
                    redisTemplate.delete(keyList);
                    redisTemplate.opsForValue().set(key,1,50, TimeUnit.SECONDS);
                }
            }
        } catch (Exception e) {
            logger.error("LotterySgTasks dealFiveMinute is error {} "+e.getMessage(), e);
        } finally {
            lock.unlock();
        }

    }

    /**
     *  5分彩
     *  从4分0秒开始的
     */
    @Scheduled(cron = "0 4/5 * * * ?")
    public void dealFiveMinuteFour() {
        String key = "kyTaskFiveFour";
        RedisLock lock = new RedisLock(key+"lock", RedisLock.TIMEOUT_ONE_HUNDRED_MSECS);
        try {
            if (lock.lock()) {
                if(redisTemplate.opsForValue().get(key) == null){
                    List<String> keyList = new ArrayList<>();
                    keyList.add(RedisKeys.XYFEIT_NEXT_VALUE);
                    keyList.add(RedisKeys.XYFTFT_NEXT_VALUE);
                    redisTemplate.delete(keyList);
                    redisTemplate.opsForValue().set(key,1,50, TimeUnit.SECONDS);
                }
            }
        } catch (Exception e) {
            logger.error("LotterySgTasks dealFiveFourMinute is error {} "+e.getMessage(), e);
        } finally {
            lock.unlock();
        }

    }

    /**
     *  10分
     */
    @Scheduled(cron = "0 0/10 * * * ?")
    public void dealTenMinute() {
        String key = "kyTaskTen";
        RedisLock lock = new RedisLock(key+"lock", RedisLock.TIMEOUT_ONE_HUNDRED_MSECS);
        try {
            if (lock.lock()) {
                if(redisTemplate.opsForValue().get(key) == null){
                    List<String> keyList = new ArrayList<>();
                    keyList.add(RedisKeys.TENSSC_NEXT_VALUE);
                    keyList.add(RedisKeys.AMLHC_NEXT_VALUE);
                    keyList.add(RedisKeys.BJPKS_NEXT_VALUE);
                    keyList.add(RedisKeys.TENPKS_NEXT_VALUE);
                    keyList.add(RedisKeys.XJSSC_NEXT_VALUE);
                    keyList.add(RedisKeys.TJSSC_NEXT_VALUE);
                    keyList.add(RedisKeys.CQSSC_NEXT_VALUE);
                    redisTemplate.delete(keyList);
                    redisTemplate.opsForValue().set(key,1,50, TimeUnit.SECONDS);
                }
            }
        } catch (Exception e) {
            logger.error("LotterySgTasks dealTenMinute is error {} "+e.getMessage(), e);
        } finally {
            lock.unlock();
        }

    }

    /**
     *  澳洲系列
     *  每隔20执行一次
     */
    @Scheduled(cron = "0/20 * * * * ?")
    public void dealTwentySecond() {
        String key = "kyTaskTwentySecond";
        RedisLock lock = new RedisLock(key+"lock", RedisLock.TIMEOUT_ONE_HUNDRED_MSECS);
        try {
            if (lock.lock()) {
                if(redisTemplate.opsForValue().get(key) == null){
                    List<String> keyList = new ArrayList<>();
                    keyList.add(RedisKeys.AUSACT_NEXT_VALUE);
                    keyList.add(RedisKeys.AUZSSC_NEXT_VALUE);
                    keyList.add(RedisKeys.AUSPKS_NEXT_VALUE);

                    keyList.add(RedisKeys.AZKS_NEXT_VALUE);

                    redisTemplate.delete(keyList);
                    redisTemplate.opsForValue().set(key,1,50, TimeUnit.SECONDS);
                }
            }
        } catch (Exception e) {
            logger.error("LotterySgTasks dealTwentySecond is error {} "+e.getMessage(), e);
        } finally {
            lock.unlock();
        }

    }

    /**
     *  体彩，福彩系列
     *  每隔15分钟执行一次
     */
    @Scheduled(cron = "0 0/15 * * * ?")
    public void dealFifteenMinute() {
        String key = "kyTaskFifteenMinute";
        RedisLock lock = new RedisLock(key+"lock", RedisLock.TIMEOUT_ONE_HUNDRED_MSECS);
        try {
            if (lock.lock()) {
                if(redisTemplate.opsForValue().get(key) == null){
                    List<String> keyList = new ArrayList<>();
                    keyList.add(RedisKeys.LHC_RESULT_VALUE);

                    keyList.add(RedisKeys.DLT_NEXT_VALUE);
                    keyList.add(RedisKeys.TCPLW_RESULT_VALUE);
                    keyList.add(RedisKeys.TC7XC_RESULT_VALUE);

                    keyList.add(RedisKeys.FCSSQ_RESULT_VALUE);
                    keyList.add(RedisKeys.FC3D_RESULT_VALUE);
                    keyList.add(RedisKeys.FC7LC_RESULT_VALUE);

                    redisTemplate.delete(keyList);
                    redisTemplate.opsForValue().set(key,1,50, TimeUnit.SECONDS);
                }
            }
        } catch (Exception e) {
            logger.error("LotterySgTasks dealFifteenMinute is error {} "+e.getMessage(), e);
        } finally {
            lock.unlock();
        }

    }


}
