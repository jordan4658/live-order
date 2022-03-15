package com.caipiao.live.order.service.bet.task;

import com.caipiao.live.common.mybatis.entity.OrderBetRecord;
import com.caipiao.live.common.util.SpringContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ClearingOnePlayOneOddsTask  implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(ClearingLhcOnePlayOddsTask.class);

	private CountDownLatch latch;
	private List<OrderBetRecord> list;
	private ClearingOnePlayOneOddsHandler handler = SpringContextUtil
			.getBeanByClass(ClearingOnePlayOneOddsHandler.class);
	private String issue;
	private String number;
	private List<Integer> playIds;
	private int lotteryId;
	private boolean jiesuanOrNot;
	private String date;
	private double divisor;

	public ClearingOnePlayOneOddsTask(CountDownLatch latch, List<OrderBetRecord> list, String issue, String number,
									  List<Integer> playIds, int lotteryId, boolean jiesuanOrNot, String date, double divisor) {
		this.latch = latch;
		this.list = list;
		this.issue = issue;
		this.number = number;
		this.playIds = playIds;
		this.lotteryId = lotteryId;
		this.jiesuanOrNot = jiesuanOrNot;
		this.date = date;
		this.divisor = divisor;
	}

	@Override
	public void run() {
		try {
			handler.handler(list, issue, number, playIds, lotteryId, jiesuanOrNot, date, divisor);
		} catch (Exception e) {
			log.error("", e);
		} finally {
			latch.countDown();
		}
	}
}
