package com.caipiao.live.order.service.result;
import com.caipiao.live.common.mybatis.entity.DzxyftLotterySg;

/**
 * @Date:Created in 22:192019/12/12
 * @Descriotion
 * @Author
 **/

public interface DzxyftLotterySgWriteService {
    /**
     * xyft 获取下一期开奖信息
     */
    DzxyftLotterySg queryNextSg();

    /**
     * xyft 根据期号获取赛果信息
     *
     * @param issue 期号
     * @return
     */
    DzxyftLotterySg selectByIssue(String issue);
    /**
     * @Title: cacheIssueResultForXjssc
     * @Description: 把幸运飞艇开奖结果放入缓存
     */
    public void cacheIssueResultForDzxyft(String issue, String number);
}
