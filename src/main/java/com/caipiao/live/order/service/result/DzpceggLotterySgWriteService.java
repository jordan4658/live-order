package com.caipiao.live.order.service.result;
import com.caipiao.live.common.mybatis.entity.DzpceggLotterySg;

import java.util.List;

/**
 * @author ShaoMing
 * @datetime 2018/7/27 16:27
 */
public interface DzpceggLotterySgWriteService {

    /**
     * 获取当前最后一期赛果
     *
     * @return
     */
    DzpceggLotterySg queryLastSg();

    /**
     * 获取当前的下一期开奖期号及倒计时
     *
     * @return
     */
    DzpceggLotterySg queryNextSg();

    /**
     * 获取今日已开期数
     *
     * @return
     */
    Integer queryOpenedCount();

    /**
     * 查询下一期开奖时间（时间戳：秒）
     *
     * @return
     */
    Long queryCountDown();

    /**
     * 查询指定日期已开历史记录（默认：当天）
     *
     * @param date     日期，例如：2018-11-13
     * @param sort     排序方式：ASC 顺序 | DESC 倒序
     * @param pageNo   页码
     * @param pageSize 每页数量
     * @return
     */
    List<DzpceggLotterySg> querySgList(String date, String sort, Integer pageNo, Integer pageSize);

    /**
     * 根据期号查询赛果信息
     *
     * @param issue 期号
     * @return
     */
    DzpceggLotterySg querySgByIssue(String issue);

    /**
     * 根据期号获取赛果信息
     *
     * @param issue 期号
     * @return
     */
    DzpceggLotterySg selectByIssue(String issue);
    
    /**
	 * @Title: cacheIssueResultForXjssc
	 * @Description: 把PC蛋蛋开奖结果放入缓存
	 */
	public void cacheIssueResultForDzpcdd(String issue, String number);
}
