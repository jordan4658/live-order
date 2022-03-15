package com.caipiao.live.order.service.bet.task;

import com.caipiao.live.common.mybatis.entity.OrderBetRecord;
import com.caipiao.live.common.util.SpringContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ClearingLhcOnePlayOddsTask implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(ClearingLhcOnePlayOddsTask.class);

	private CountDownLatch latch;
	private List<OrderBetRecord> list;
	private ClearingLhcOnePlayOddsHandler handler = SpringContextUtil
			.getBeanByClass(ClearingLhcOnePlayOddsHandler.class);
	private String issue;
	private String number;
	private Integer playId;
	private int lotteryId;
	private boolean jiesuanOrNot;
	private String date;
	private List<Integer> playlist;
	private double divisor;

	public ClearingLhcOnePlayOddsTask(CountDownLatch latch, List<OrderBetRecord> list, String issue, String number,
									  Integer playId, int lotteryId, boolean jiesuanOrNot, String date, List<Integer> playlist, double divisor) {
		this.latch = latch;
		this.list = list;
		this.issue = issue;
		this.number = number;
		this.playId = playId;
		this.lotteryId = lotteryId;
		this.jiesuanOrNot = jiesuanOrNot;
		this.date = date;
		this.playlist = playlist;
		this.divisor = divisor;
	}

	@Override
	public void run() {
		try {
			handler.handler(list, issue, number, playId, lotteryId, jiesuanOrNot, date, playlist, divisor);
		} catch (Exception e) {
			log.error("", e);
		} finally {
			latch.countDown();
		}
	}

}
