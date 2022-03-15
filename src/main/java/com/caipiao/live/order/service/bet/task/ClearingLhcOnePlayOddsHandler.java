package com.caipiao.live.order.service.bet.task;

import com.caipiao.live.order.service.bet.BetCommonService;
import com.caipiao.live.order.service.lottery.LotteryPlayOddsWriteService;
import com.caipiao.live.order.service.lottery.LotteryPlayWriteService;
import com.caipiao.live.order.service.lottery.LotteryWriteService;
import com.caipiao.live.common.model.dto.order.OrderBetStatus;
import com.caipiao.live.common.mybatis.entity.Lottery;
import com.caipiao.live.common.mybatis.entity.LotteryPlay;
import com.caipiao.live.common.mybatis.entity.LotteryPlayOdds;
import com.caipiao.live.common.mybatis.entity.OrderBetRecord;
import com.caipiao.live.common.util.DateUtils;
import com.caipiao.live.common.util.lottery.LhcUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class ClearingLhcOnePlayOddsHandler {

	private static final Logger log = LoggerFactory.getLogger(ClearingLhcOnePlayOddsHandler.class);

	@Autowired
	private RedisTemplate<String, Object> redisTemplate;
	@Autowired
	private LotteryPlayOddsWriteService lotteryPlayOddsService;
	@Autowired
	private LotteryPlayWriteService lotteryPlayWriteService;
	@Autowired
	private LotteryWriteService lotteryWriteService;
	@Autowired
	private BetCommonService betCommonService;
	// 六合彩正码正码A玩法id
	public final static String PLAY_ID_ZM_ZMA = "03";

	// 六合彩六肖连中玩法id
	private final String PLAY_ID_LX_LXLZ = "29";
	// 六合彩六肖连不中玩法id
	private final String PLAY_ID_LX_LXLBZ = "30";

	public void handler(List<OrderBetRecord> orderBetRecords, String issue, String number, Integer playId,
						int lotteryId, boolean jiesuanOrNot, String date, List<Integer> playlist, double divisor) {
		long begin = System.currentTimeMillis();
		if (CollectionUtils.isEmpty(orderBetRecords)) {
			log.error("待结算的数据为空");
			return;
		}
		OrderBetRecord order = new OrderBetRecord();
		try {
			log.debug("OnePlayOdds待结算数据共[{}]条", orderBetRecords.size());
			if (this.generationLHCPlayId(PLAY_ID_LX_LXLZ, lotteryId).equals(playId)
					|| this.generationLHCPlayId(PLAY_ID_LX_LXLBZ, lotteryId).equals(playId)) {
				// 如果是六肖连中或者六肖连不中玩法,特肖开出49为和值
				if ("49".equals(number.split(",")[6])) {
					betCommonService.noWinOrLose(orderBetRecords);
				}
			}
			// 获取配置id
			if (orderBetRecords == null || orderBetRecords.size() == 0) {
				return;
			}
			Integer settingId = orderBetRecords.get(0).getSettingId();

			// 获取赔率信息
			LotteryPlayOdds odds = lotteryPlayOddsService.findPlayOddsBySettingId(settingId);
			String shengXiao = LhcUtils.getShengXiao(DateUtils.formatDate(new Date(), "yyyy-MM-dd"));
			boolean flag = orderBetRecords.get(0).getBetNumber().indexOf(shengXiao) >= 0;
			if (flag) {
				odds = lotteryPlayOddsService.selectPlayOddsBySettingId(settingId).get(shengXiao);
			}
			for (OrderBetRecord orderBet : orderBetRecords) {
				log.info("待结算的订单信息:[{}]", orderBet.toString());
				if (orderBet.getPlayId() != null && orderBet.getPlayId().equals(120101)) {
					Map<String, LotteryPlayOdds> oddsMap = lotteryPlayOddsService
							.selectPlayOddsBySettingId(orderBet.getSettingId());
					if (oddsMap.size() == 49) {
						odds = oddsMap.get(String.valueOf(number.split(",")[6]));
					}
				}

				order = orderBet;

				if (orderBet.getBetNumber().indexOf("两面") >= 0) {
					continue;
				}
				if (orderBet.getBetNumber().contains("五行")) {
					List<LotteryPlayOdds> oddList = lotteryPlayOddsService.selectOddsListBySettingId(settingId);
					Map<String, LotteryPlayOdds> oddsMap = new HashMap<>();
					for (LotteryPlayOdds lotteryPlayodds : oddList) {
						oddsMap.put(lotteryPlayodds.getName(), lotteryPlayodds);
					}
					String betName = orderBet.getBetNumber().replace("五行@", "");
					odds = oddsMap.get(betName);
				}
				BigDecimal winAmount = new BigDecimal(0);
				orderBet.setWinCount("0");
				// 判断是否中奖,获取中奖注数
				int winCounts = LhcUtils.isWinByOnePlayOneOdds(orderBet.getBetNumber(), number, orderBet.getPlayId(),
						date, lotteryId);
				if (winCounts > 0) {
					// 获取总注数/中奖注数
					String winCount = odds.getWinCount();
					String totalCount = odds.getTotalCount();
					// 计算赔率
					double odd = Double.parseDouble(totalCount) * 1.0 / Double.parseDouble(winCount) * divisor;
					// 一注的中奖额
					winAmount = orderBet.getBetAmount().divide(BigDecimal.valueOf(orderBet.getBetCount()))
							.multiply(BigDecimal.valueOf(odd));
					winAmount = winAmount.multiply(BigDecimal.valueOf(winCounts));
					orderBet.setWinCount(String.valueOf(winCounts));
				}

				// 根据中奖金额,修改投注信息及相关信息
				// OrderRecord orderRecord = orderMap.get(orderBet.getOrderId());
				try {
					if (jiesuanOrNot) {
						log.info("clearingLhcOnePlayOdds 开奖判断信息{};{};{};{};{};{};{}", orderBet.getOrderSn(),
								orderBet.getBetNumber(), number, orderBet.getPlayId(), lotteryId, winCounts, winAmount);
						betCommonService.winOrLose(orderBet, winAmount, orderBet.getUserId(), orderBet.getOrderSn());
					} else {
						// 保存结算信息
						String tbStatus = "";
						if (winAmount.compareTo(orderBet.getBetAmount()) == 0) {
							tbStatus = OrderBetStatus.HE;
						} else if (winAmount.compareTo(BigDecimal.ZERO) > 0) {
							tbStatus = OrderBetStatus.WIN;
						} else {
							tbStatus = OrderBetStatus.NO_WIN;
						}
						String lotteryName = "";
						String lotteryPlayName = "";
						LotteryPlay lotteryPlay = lotteryPlayWriteService.selectPlayById(orderBet.getPlayId());
						Lottery lottery = lotteryWriteService.selectLotteryById(orderBet.getLotteryId());
						if (lotteryPlay != null) {
							lotteryName = lotteryPlay.getName();
						}
						if (lottery != null) {
							lotteryPlayName = lottery.getName();
						}
						String message = orderBet.getIssue() + ";" + orderBet.getOrderSn() + ";" + lotteryName + ";"
								+ lotteryPlayName + ";" + orderBet.getBetNumber() + ";" + orderBet.getBetAmount() + ";"
								+ winAmount + ";" + tbStatus;

						redisTemplate.opsForHash().put("JIESUANORDER", issue + orderBet.getPlayId() + orderBet.getId(),
								message);
						redisTemplate.expire("JIESUANORDER", 6, TimeUnit.HOURS);
					}

				} catch (TransactionSystemException e1) {
					log.error("订单结算出错 事务冲突 进行重试:{},{}", orderBet.getOrderSn(), e1);
					for (int i = 0; i < 20; i++) {
						try {
							if (jiesuanOrNot) {
								betCommonService.winOrLose(orderBet, winAmount, orderBet.getUserId(),
										orderBet.getOrderSn());
							}
						} catch (TransactionSystemException e2) {
							log.error("订单结算出错:{},事务冲突 进行重试:{},{}", i, orderBet.getOrderSn(), e2);
							Thread.sleep(100);
							continue;
						}
						break;
					}
				}
				log.debug("winOrLose time, {}", System.currentTimeMillis() - begin);
			}
		} catch (Exception e) {
			log.error("订单结算出错，lotteryId:{},issue:{},betNum:{},{}", order.getLotteryId(), issue, order.getBetNumber(),
					e);
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

}
