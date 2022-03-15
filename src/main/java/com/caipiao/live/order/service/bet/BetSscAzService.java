package com.caipiao.live.order.service.bet;

import com.caipiao.live.common.mybatis.entity.AussscLotterySg;

public interface BetSscAzService {

	/**
	 * 结算澳洲时时彩
	 *
	 * @param issue  期号
	 * @param number 开奖号码
	 */
	void countAzSsc(String issue, String number, int lotteryId) throws Exception;

	public AussscLotterySg queryAussscLotteryNextSg();

	public AussscLotterySg selectAussscLotteryByIssue(String issue, String number);

	public void cacheIssueResultForAusssc(String issue, String number);

}
