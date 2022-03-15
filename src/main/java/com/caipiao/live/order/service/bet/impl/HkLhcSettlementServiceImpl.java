package com.caipiao.live.order.service.bet.impl;

import com.caipiao.live.order.service.bet.BetCommonService;
import com.caipiao.live.order.service.bet.HkLhcSettlementService;
import com.caipiao.live.order.service.bet.task.ClearingOnePlayTwoOddsTask;
import com.caipiao.live.order.service.order.OrderWriteService;
import com.caipiao.live.order.service.bet.task.ClearingLhcOnePlayOddsTask;
import com.caipiao.live.order.service.bet.task.ClearingOnePlayManyOddsTask;
import com.caipiao.live.order.service.bet.task.ClearingOnePlayOneOddsTask;
import com.caipiao.live.common.model.dto.order.OrderBetStatus;
import com.caipiao.live.common.mybatis.entity.OrderBetRecord;
import com.caipiao.live.common.util.DateUtils;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class HkLhcSettlementServiceImpl implements HkLhcSettlementService {

	private static final Logger logger = LoggerFactory.getLogger(HkLhcSettlementServiceImpl.class);

	// 彩种id：4 六合彩
	// private final Integer lotteryId = 4;
	@Autowired
	private OrderWriteService orderWriteService;
	@Autowired
	private BetCommonService betCommonService;

	// 六合彩特码特码A玩法id
	private final String PLAY_ID_TM_TMA = "01";
	// 六合彩特码特码A两面玩法id
	private final String PLAY_ID_TM_TMA_LM = "02";
	// 六合彩正码正码A玩法id
	public final static String PLAY_ID_ZM_ZMA = "03";
	// 六合彩正码正码1-6玩法id集合
	private final String PLAY_IDS_ZM = "04";
	// 六合彩正特正(1-6)特玩法id集合
	private final List<String> PLAY_IDS_ZT_OTS = Lists.newArrayList("05", "06", "07", "08", "09", "10");
	// 六合彩正特正一特玩法id
	private final String PLAY_ID_ZT_ONE = "05";
	// 六合彩正特正一特玩法id
	private final String PLAY_ID_ZT_TWO = "06";
	// 六合彩正特正一特玩法id
	private final String PLAY_ID_ZT_THREE = "07";
	// 六合彩正特正一特玩法id
	private final String PLAY_ID_ZT_FOUR = "08";
	// 六合彩正特正一特玩法id
	private final String PLAY_ID_ZT_FIVE = "09";
	// 六合彩正特正一特玩法id
	private final String PLAY_ID_ZT_SIX = "10";

	// 六合彩连码三全中,二全中,特串玩法id集合
	private final List<String> PLAY_IDS_LM_QZ = Lists.newArrayList("15", "14", "13");

	// 六合彩连码三中二,二中特玩法id集合
	private final List<String> PLAY_IDS_LM_EZ = Lists.newArrayList("11", "12");

	// 六合彩半波红波玩法id
	private final String PLAY_ID_BB_RED = "16";
	// 六合彩半波蓝波玩法id
	private final String PLAY_ID_BB_BLUE = "17";
	// 六合彩半波绿波玩法id
	private final String PLAY_ID_BB_GREEN = "18";

	// 六合彩不中玩法id集合
	private final List<String> PLAY_IDS_NO_OPEN = Lists.newArrayList("21", "22", "23", "24", "25", "26");

	// 六合彩尾数全尾玩法id
	private final String PLAY_ID_WS_QW = "19";
	// 六合彩尾数特尾玩法id
	private final String PLAY_ID_WS_TW = "20";
	// 六合彩平特玩法id
	private final String PLAY_ID_PT_PT = "27";
	// 六合彩特肖玩法id
	private final String PLAY_ID_TX_TX = "28";
	// 六合彩六肖连中玩法id
	private final String PLAY_ID_LX_LXLZ = "29";
	// 六合彩六肖连不中玩法id
	private final String PLAY_ID_LX_LXLBZ = "30";

	// 六合彩连肖中玩法id集合
	private final List<String> PLAY_IDS_LX_WIN = Lists.newArrayList("31", "33", "35");
	// 六合彩连肖不中玩法id集合
	private final List<String> PLAY_IDS_LX_NO_WIN = Lists.newArrayList("32", "34", "36");

	// 六合彩连尾中玩法id集合
	private final List<String> PLAY_IDS_LW_WIN = Lists.newArrayList("37", "39", "41");
	// 六合彩连尾不中玩法id集合
	private final List<String> PLAY_IDS_LW_NO_WIN = Lists.newArrayList("38", "40", "42");

	// 六合彩1-6龙虎玩法id
	private final String PLAY_ID_ONE_SIX_LH = "43";
	// 六合彩五行玩法id
	private final String PLAY_ID_WUXING = "44";

	// 可能打和的投注信息
	// private final List<String> MAYBE_HE = Lists.newArrayList("大", "小", "单", "双",
	// "合单", "合双", "尾大", "尾小", "家禽", "野兽");

	private final int processCount = Runtime.getRuntime().availableProcessors() * 2;
	private final int SETTLEMENT_LOCK_MINUTES = 2;

	/**
	 * 结算【六合彩特码特码A】
	 *
	 * @param issue  期号
	 * @param number 开奖号码
	 */
	@Override
	public void clearingHkLhcTeMaA(String issue, String number, int lotteryId, boolean jiesuanOrNot) throws Exception {
		// 特码特码A
		hkLhcClearingLhcOnePlayOdds(issue, number, this.generationLHCPlayId(PLAY_ID_TM_TMA, lotteryId), lotteryId,
				jiesuanOrNot);
		// 特码特码A两面
		hkLhcClearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_TM_TMA_LM, lotteryId), lotteryId,
				jiesuanOrNot);
	}

	/**
	 * 结算【六合彩正码1-6】
	 *
	 * @param issue 期号
	 */
	@Override
	public void clearingHkLhcZhengMaA(String issue, String number, int lotteryId, boolean jiesuanOrNot)
			throws Exception {
		// 正码正码A两面
		hkLhcClearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_ZM_ZMA, lotteryId), lotteryId,
				jiesuanOrNot);
		// 正码正码A
		hkLhcClearingLhcOnePlayOdds(issue, number, this.generationLHCPlayId(PLAY_ID_ZM_ZMA, lotteryId), lotteryId,
				jiesuanOrNot);
	}

	/**
	 * 结算【六合彩正特】
	 *
	 * @param issue 期号
	 */
	@Override
	public void clearingHkLhcZhengTe(String issue, String number, int lotteryId, boolean jiesuanOrNot) {
		// 正(1-6)特
		hkLhcClearingOnePlayOneOdds(issue, number, this.generationLHCPlayIdList(PLAY_IDS_ZT_OTS, lotteryId), lotteryId,
				jiesuanOrNot);
		// 正(1-6)特 两面
		hkLhcClearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_ZT_ONE, lotteryId), lotteryId,
				jiesuanOrNot);
		hkLhcClearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_ZT_TWO, lotteryId), lotteryId,
				jiesuanOrNot);
		hkLhcClearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_ZT_THREE, lotteryId), lotteryId,
				jiesuanOrNot);
		hkLhcClearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_ZT_FOUR, lotteryId), lotteryId,
				jiesuanOrNot);
		hkLhcClearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_ZT_FIVE, lotteryId), lotteryId,
				jiesuanOrNot);
		hkLhcClearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_ZT_SIX, lotteryId), lotteryId,
				jiesuanOrNot);
	}

	/**
	 * 结算【六合彩正码1-6】
	 *
	 * @param issue 期号
	 */
	@Override
	public void clearingHkLhcZhengMaOneToSix(String issue, String number, int lotteryId, boolean jiesuanOrNot) {
		hkLhcClearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_IDS_ZM, lotteryId), lotteryId,
				jiesuanOrNot);
	}

	/**
	 * 结算【六合彩六肖】
	 *
	 * @param issue 期号
	 */
	@Override
	public void clearingHkLhcLiuXiao(String issue, String number, int lotteryId, boolean jiesuanOrNot) {
		hkLhcClearingLhcOnePlayOdds(issue, number, this.generationLHCPlayId(PLAY_ID_LX_LXLZ, lotteryId), lotteryId,
				jiesuanOrNot);
		hkLhcClearingLhcOnePlayOdds(issue, number, this.generationLHCPlayId(PLAY_ID_LX_LXLBZ, lotteryId), lotteryId,
				jiesuanOrNot);
	}

	/**
	 * 结算【六合彩半波】
	 *
	 * @param issue 期号
	 */
	@Override
	public void clearingHkLhcBanBo(String issue, String number, int lotteryId, boolean jiesuanOrNot) {
		hkLhcClearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_BB_RED, lotteryId), lotteryId,
				jiesuanOrNot);
		hkLhcClearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_BB_BLUE, lotteryId), lotteryId,
				jiesuanOrNot);
		hkLhcClearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_BB_GREEN, lotteryId), lotteryId,
				jiesuanOrNot);
	}

	/**
	 * 结算【六合彩尾数】
	 *
	 * @param issue 期号
	 */
	@Override
	public void clearingHkLhcWs(String issue, String number, int lotteryId, boolean jiesuanOrNot) {
		hkLhcClearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_WS_QW, lotteryId), lotteryId,
				jiesuanOrNot);
		hkLhcClearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_WS_TW, lotteryId), lotteryId,
				jiesuanOrNot);
	}

	/**
	 * 结算【六合彩连码】
	 *
	 * @param issue 期号
	 */
	@Override
	public void clearingHkLhcLianMa(String issue, String number, int lotteryId, boolean jiesuanOrNot) {
		// 三全中,二全中,特串
		hkLhcClearingOnePlayOneOdds(issue, number, this.generationLHCPlayIdList(PLAY_IDS_LM_QZ, lotteryId), lotteryId,
				jiesuanOrNot);
		// 三中二,二中特
		hkLhcClearingOnePlayTwoOdds(issue, number, this.generationLHCPlayIdList(PLAY_IDS_LM_EZ, lotteryId), lotteryId,
				jiesuanOrNot);
	}

	/**
	 * 结算【六合彩连肖中】
	 *
	 * @param issue 期号
	 */
	@Override
	public void clearingHkLhcLianXiao(String issue, String number, int lotteryId, boolean jiesuanOrNot) {
		hkLhcClearingOnePlayTwoOdds(issue, number, this.generationLHCPlayIdList(PLAY_IDS_LX_WIN, lotteryId), lotteryId,
				jiesuanOrNot);
		hkLhcClearingOnePlayOneOdds(issue, number, this.generationLHCPlayIdList(PLAY_IDS_LX_NO_WIN, lotteryId),
				lotteryId, jiesuanOrNot);
	}

	/**
	 * 结算【六合彩连尾中】
	 *
	 * @param issue 期号
	 */
	@Override
	public void clearingHkLhcLianWei(String issue, String number, int lotteryId, boolean jiesuanOrNot) {
		hkLhcClearingOnePlayOneOdds(issue, number, this.generationLHCPlayIdList(PLAY_IDS_LW_WIN, lotteryId), lotteryId,
				jiesuanOrNot);
		hkLhcClearingOnePlayTwoOdds(issue, number, this.generationLHCPlayIdList(PLAY_IDS_LW_NO_WIN, lotteryId),
				lotteryId, jiesuanOrNot);
	}

	/**
	 * 结算【六合彩不中】
	 *
	 * @param issue 期号
	 */
	@Override
	public void clearingHkLhcNoOpen(String issue, String number, int lotteryId, boolean jiesuanOrNot) {
		hkLhcClearingOnePlayOneOdds(issue, number, this.generationLHCPlayIdList(PLAY_IDS_NO_OPEN, lotteryId), lotteryId,
				jiesuanOrNot);
	}

	/**
	 * 结算【六合彩1-6龙虎】
	 *
	 * @param issue 期号
	 */
	@Override
	public void clearingHkLhcOneSixLh(String issue, String number, int lotteryId, boolean jiesuanOrNot) {
		hkLhcClearingLhcOnePlayOdds(issue, number, this.generationLHCPlayId(PLAY_ID_ONE_SIX_LH, lotteryId), lotteryId,
				jiesuanOrNot);
	}

	/**
	 * 结算【六合彩五行】
	 *
	 * @param issue 期号
	 */
	@Override
	public void clearingHkLhcWuxing(String issue, String number, int lotteryId, boolean jiesuanOrNot) {
		hkLhcClearingLhcOnePlayOdds(issue, number, this.generationLHCPlayId(PLAY_ID_WUXING, lotteryId), lotteryId,
				jiesuanOrNot);
	}

	/**
	 * 结算【六合彩平特平特】
	 *
	 * @param issue 期号
	 */
	@Override
	public void clearingHkLhcPtPt(String issue, String number, int lotteryId, boolean jiesuanOrNot) {
		hkLhcClearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_PT_PT, lotteryId), lotteryId,
				jiesuanOrNot);
	}

	/**
	 * 结算【六合彩特肖特肖】
	 *
	 * @param issue 期号
	 */
	@Override
	public void clearingHkLhcTxTx(String issue, String number, int lotteryId, boolean jiesuanOrNot) {
		hkLhcClearingOnePlayManyOdds(issue, number, this.generationLHCPlayId(PLAY_ID_TX_TX, lotteryId), lotteryId,
				jiesuanOrNot);
	}

	/**
	 * 一种玩法只有一种赔率,而且投注号码可中一注或多注
	 *
	 * @param issue   期号
	 * @param number  开奖号码
	 * @param playIds 玩法id集合
	 */
	private void hkLhcClearingOnePlayOneOdds(String issue, String number, List<Integer> playIds, int lotteryId,
			boolean jiesuanOrNot) {
		// 获取该期赛果信息
		String date = DateUtils.formatDate(new Date(), "yyyy-MM-dd");
		// 获取相应的订单信息
		int ordercount = orderWriteService.countOrderBetList(issue, playIds, String.valueOf(lotteryId),
				OrderBetStatus.WAIT);
		logger.info("=hkLhcClearingOnePlayOneOdds查询到等待结算数据[{}]条=", ordercount);
		if (ordercount <= 0) {
			return;
		}

		// List<OrderRecord> orderRecords =
		// orderWriteService.selectOrdersPage(lotteryId, issue, OrderStatus.NORMAL, i);
		// Map<Integer, OrderRecord> orderMap = new HashMap<>();
		// 获取相关订单id集合
		// List<Integer> orderIds = new ArrayList<>();
		// this.updateOrder(number, orderRecords, orderIds, orderMap);
		// 获取赔率因子
		double divisor = betCommonService.getDivisor(lotteryId);
		// 查询所有所有相关投注信息
		List<OrderBetRecord> orderBetRecords = orderWriteService.selectOrderBetList(issue, String.valueOf(lotteryId),
				playIds, OrderBetStatus.WAIT, "OnePlayOne");
		if (!jiesuanOrNot) {
			if (orderBetRecords.size() >= 2) {
				orderBetRecords = orderBetRecords.subList(0, 2);
			}
		}

		// 多线程处理 最多processCount个线程
		int taskCount = orderBetRecords.size() < processCount ? orderBetRecords.size() : processCount;
		if (taskCount < 1)
			taskCount = 1;
		CountDownLatch latch = new CountDownLatch(taskCount);
		ExecutorService threadPool = Executors.newFixedThreadPool(taskCount);
		List<List<OrderBetRecord>> l = averageAssign(orderBetRecords, taskCount);
		l.forEach(o -> {
			threadPool.execute(new ClearingOnePlayOneOddsTask(latch, o, issue, number, playIds, lotteryId, jiesuanOrNot,
					date, divisor));
		});
		try {
			// 阻塞当前线程，直到所有任务均完成
			// latch.await();
			// 使当前线程在锁存器倒计数至零之前一直等待，除非线程被中断或超出了指定的等待时间。
			latch.await(3, TimeUnit.MINUTES);// 3分钟后超时
		} catch (InterruptedException e) {
			logger.error("", e);
		} finally {
			// 最后关闭线程池，但执行以前提交的任务，不接受新任务
			threadPool.shutdown();
			// 关闭线程池，停止所有正在执行的活动任务，暂停处理正在等待的任务，并返回等待执行的任务列表。
			// threadPool.shutdownNow();
		}
	}

	/**
	 * 一种玩法只有一条赔率记录,但有高低2种赔率,而且投注号码可中一注或多注
	 *
	 * @param issue   期号
	 * @param playIds 玩法id集合
	 */
	private void hkLhcClearingOnePlayTwoOdds(String issue, String number, List<Integer> playIds, int lotteryId,
			boolean jiesuanOrNot) {
		String date = DateUtils.formatDate(new Date(), "yyyy-MM-dd");

		// 获取相应的订单信息
		int ordercount = orderWriteService.countOrderBetList(issue, playIds, String.valueOf(lotteryId),
				OrderBetStatus.WAIT);
		if (ordercount <= 0) {
			return;
		}

		// List<OrderRecord> orderRecords =
		// orderWriteService.selectOrdersPage(lotteryId, issue, OrderStatus.NORMAL, i);
		// Map<Integer, OrderRecord> orderMap = new HashMap<>();
		// 获取相关订单id集合
		// List<Integer> orderIds = new ArrayList<>();
		// this.updateOrder(number, orderRecords, orderIds, orderMap);
		// 获取赔率因子
		double divisor = betCommonService.getDivisor(lotteryId);
		// 查询所有所有相关投注信息
		List<OrderBetRecord> orderBetRecords = orderWriteService.selectOrderBetList(issue, String.valueOf(lotteryId),
				playIds, OrderBetStatus.WAIT, "OnePlayTwo");
		if (!jiesuanOrNot) {
			if (orderBetRecords.size() >= 2) {
				orderBetRecords = orderBetRecords.subList(0, 2);
			}
		}

		// 多线程处理 最多processCount个线程
		int taskCount = orderBetRecords.size() < processCount ? orderBetRecords.size() : processCount;
		if (taskCount < 1)
			taskCount = 1;
		CountDownLatch latch = new CountDownLatch(taskCount);
		ExecutorService threadPool = Executors.newFixedThreadPool(taskCount);
		List<List<OrderBetRecord>> l = averageAssign(orderBetRecords, taskCount);
		l.forEach(o -> {
			threadPool.execute(new ClearingOnePlayTwoOddsTask(latch, o, issue, number, playIds, lotteryId, jiesuanOrNot,
					date, divisor));
		});
		try {
			// 阻塞当前线程，直到所有任务均完成
			// latch.await();
			// 使当前线程在锁存器倒计数至零之前一直等待，除非线程被中断或超出了指定的等待时间。
			latch.await(SETTLEMENT_LOCK_MINUTES, TimeUnit.MINUTES);// 3分钟后超时
		} catch (InterruptedException e) {
			logger.error("", e);
		} finally {
			// 最后关闭线程池，但执行以前提交的任务，不接受新任务
			threadPool.shutdown();
			// 关闭线程池，停止所有正在执行的活动任务，暂停处理正在等待的任务，并返回等待执行的任务列表。
			// threadPool.shutdownNow();
		}
	}

	/**
	 * 一种玩法只有一种赔率,投注号码可中一注或多注
	 *
	 * @param issue        期号
	 * @param number       开奖号码
	 * @param playId       玩法id
	 * @param jiesuanOrNot 是否真实结算
	 */
	private void hkLhcClearingLhcOnePlayOdds(String issue, String number, Integer playId, int lotteryId,
			boolean jiesuanOrNot) {
		long begin = System.currentTimeMillis();
		logger.debug("OnePlayOdds结算开始......");
		String date = DateUtils.formatDate(new Date(), "yyyy-MM-dd");
		if (jiesuanOrNot) {
			int upcount = orderWriteService.updateOrderRecord(String.valueOf(lotteryId), issue, number);
			while (upcount > 0) {
				upcount = orderWriteService.updateOrderRecord(String.valueOf(lotteryId), issue, number);
			}
		}

		logger.debug("updateOrderRecord time, {}", System.currentTimeMillis() - begin);
		// 获取相应的订单信息
		List<Integer> playlist = new ArrayList<Integer>();
		playlist.add(playId);
		int ordercount = orderWriteService.countOrderBetList(issue, playlist, String.valueOf(lotteryId),
				OrderBetStatus.WAIT);
		logger.debug("countOrderBetList time, {}, 未结算的订单[{}]条", System.currentTimeMillis() - begin, ordercount);
		if (ordercount <= 0) {
			return;
		}

		// List<OrderRecord> orderRecords =
		// orderWriteService.selectOrdersPage(lotteryId, issue, OrderStatus.NORMAL, i);
		// Map<Integer, OrderRecord> orderMap = new HashMap<>();
		// 获取相关订单id集合
		// List<Integer> orderIds = new ArrayList<>();
		// this.updateOrder(number, orderRecords, orderIds, orderMap);
		// 获取赔率因子
		double divisor = betCommonService.getDivisor(lotteryId);
		// 查询所有所有相关投注信息
		// 查询所有所有相关投注信息
		List<OrderBetRecord> orderBetRecords = orderWriteService.selectOrderBetList(issue, String.valueOf(lotteryId),
				playlist, OrderBetStatus.WAIT, "OnePlay");
		if (!jiesuanOrNot) {
			if (orderBetRecords.size() >= 2) {
				orderBetRecords = orderBetRecords.subList(0, 2);
			}
		}

		// 多线程处理 最多processCount个线程
		int taskCount = orderBetRecords.size() < processCount ? orderBetRecords.size() : processCount;
		if (taskCount < 1)
			taskCount = 1;
		CountDownLatch latch = new CountDownLatch(taskCount);
		ExecutorService threadPool = Executors.newFixedThreadPool(taskCount);
		List<List<OrderBetRecord>> l = averageAssign(orderBetRecords, taskCount);
		l.forEach(o -> {
			threadPool.execute(new ClearingLhcOnePlayOddsTask(latch, o, issue, number, playId, lotteryId, jiesuanOrNot,
					date, playlist, divisor));
		});
		try {
			// 阻塞当前线程，直到所有任务均完成
			// latch.await();
			// 使当前线程在锁存器倒计数至零之前一直等待，除非线程被中断或超出了指定的等待时间。
			latch.await(SETTLEMENT_LOCK_MINUTES, TimeUnit.MINUTES);// 3分钟后超时
		} catch (InterruptedException e) {
			logger.error("", e);
		} finally {
			// 最后关闭线程池，但执行以前提交的任务，不接受新任务
			threadPool.shutdown();
			// 关闭线程池，停止所有正在执行的活动任务，暂停处理正在等待的任务，并返回等待执行的任务列表。
			// threadPool.shutdownNow();
		}
	}

	/**
	 * 一种玩法多种赔率,一条记录投注号码可中一注或多注
	 *
	 * @param issue
	 * @param playId
	 */

	private void hkLhcClearingOnePlayManyOdds(String issue, String number, Integer playId, int lotteryId,
			boolean jiesuanOrNot) {
		String date = DateUtils.formatDate(new Date(), "yyyy-MM-dd");
		// 获取相应的订单信息
		List<Integer> playlist = new ArrayList<Integer>();
		playlist.add(playId);
		int ordercount = orderWriteService.countOrderBetList(issue, playlist, String.valueOf(lotteryId),
				OrderBetStatus.WAIT);
		if (ordercount <= 0) {
			return;
		}
		// List<OrderRecord> orderRecords =
		// orderWriteService.selectOrdersPage(lotteryId, issue, OrderStatus.NORMAL, i);
		// Map<Integer, OrderRecord> orderMap = new HashMap<>();
		// 获取相关订单id集合
		// List<Integer> orderIds = new ArrayList<>();
		// this.updateOrder(number, orderRecords, orderIds, orderMap);
		// 获取赔率因子
		double divisor = betCommonService.getDivisor(lotteryId);
		// 查询所有所有相关投注信息

		List<OrderBetRecord> orderBetRecords = orderWriteService.selectOrderBetList(issue, String.valueOf(lotteryId),
				playlist, OrderBetStatus.WAIT, "OnePlayMany");
		if (!jiesuanOrNot) {
			if (orderBetRecords.size() >= 2) {
				orderBetRecords = orderBetRecords.subList(0, 2);
			}
		}

		// 多线程处理 最多processCount个线程
		int taskCount = orderBetRecords.size() < processCount ? orderBetRecords.size() : processCount;
		if (taskCount < 1)
			taskCount = 1;
		CountDownLatch latch = new CountDownLatch(taskCount);
		ExecutorService threadPool = Executors.newFixedThreadPool(taskCount);
		List<List<OrderBetRecord>> l = averageAssign(orderBetRecords, taskCount);
		l.forEach(o -> {
			threadPool.execute(new ClearingOnePlayManyOddsTask(latch, o, issue, number, playId, lotteryId, jiesuanOrNot,
					date, playlist, divisor));
		});
		try {
			// 阻塞当前线程，直到所有任务均完成
			// latch.await();
			// 使当前线程在锁存器倒计数至零之前一直等待，除非线程被中断或超出了指定的等待时间。
			latch.await(SETTLEMENT_LOCK_MINUTES, TimeUnit.MINUTES);// 3分钟后超时
		} catch (InterruptedException e) {
			logger.error("", e);
		} finally {
			// 最后关闭线程池，但执行以前提交的任务，不接受新任务
			threadPool.shutdown();
			// 关闭线程池，停止所有正在执行的活动任务，暂停处理正在等待的任务，并返回等待执行的任务列表。
			// threadPool.shutdownNow();
		}
	}

	/**
	 * 1分、5分、时时、香港六合彩 玩法ID生成
	 *
	 * @param playNumber
	 * @param
	 * @return
	 */
	private Integer generationLHCPlayId(String playNumber, Integer lotteryId) {
		return Integer.parseInt(lotteryId + playNumber);
	}

	/**
	 * 1分、5分、时时、香港六合彩 玩法ID生成
	 *
	 * @param
	 * @param
	 * @return
	 */
	private List<Integer> generationLHCPlayIdList(List<String> playNumbers, Integer lotteryId) {
		List<Integer> replaceToNewPlayId = new ArrayList<Integer>();
		for (String number : playNumbers) {
			replaceToNewPlayId.add(Integer.parseInt(lotteryId + number));
		}
		return replaceToNewPlayId;
	}

	/**
	 * 均分LIST中的数据
	 */
	private <T> List<List<T>> averageAssign(List<T> source, int n) {
		List<List<T>> result = new ArrayList<List<T>>();
		int remaider = source.size() % n; // (先计算出余数)
		int number = source.size() / n; // 然后是商
		int offset = 0;// 偏移量
		for (int i = 0; i < n; i++) {
			List<T> value = null;
			if (remaider > 0) {
				value = source.subList(i * number + offset, (i + 1) * number + offset + 1);
				remaider--;
				offset++;
			} else {
				value = source.subList(i * number + offset, (i + 1) * number + offset);
			}
			result.add(value);
		}
		return result;
	}

}
