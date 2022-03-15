package com.caipiao.live.order.service.result;

import com.caipiao.live.common.mybatis.entity.AusactLotterySg;
import com.caipiao.live.common.mybatis.entity.AuspksLotterySg;

import java.util.List;

public interface AzxlLotterySgWriteService {
	
	public AusactLotterySg queryAusactLotteryNextSg();
	
	public AusactLotterySg selectAusactLotteryByIssue(String issue, String number);
	
	public void cacheIssueResultForAusact(String issue, String number);

	List<AusactLotterySg> getAlgorithmData();
	
	public AuspksLotterySg queryAuspksLotterySgNextSg();
	
	public AuspksLotterySg selectAuspksLotterySgByIssue(String issue, String number);
	
	public void cacheIssueResultForAuspks(String issue, String number);
}
