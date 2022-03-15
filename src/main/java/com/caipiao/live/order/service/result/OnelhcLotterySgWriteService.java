package com.caipiao.live.order.service.result;

import com.caipiao.live.common.mybatis.entity.OnelhcLotterySg;

public interface OnelhcLotterySgWriteService {

    /**
     * xyft 获取下一期开奖信息
     */
	OnelhcLotterySg queryNextSg();

    /**
     * xyft 根据期号获取赛果信息
     *
     * @param issue 期号
     * @return
     */
	OnelhcLotterySg selectByIssue(String issue);
    
    /**
	 * @Title: cacheIssueResultForXjssc
	 * @Description: 把一分六合彩开奖结果放入缓存
	 */
	public void cacheIssueResultForQnelhc(String issue, String number);
		
}
