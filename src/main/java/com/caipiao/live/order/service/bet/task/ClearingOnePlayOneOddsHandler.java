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
import com.caipiao.live.common.util.lottery.LhcUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class ClearingOnePlayOneOddsHandler {

	private static final Logger log = LoggerFactory.getLogger(ClearingOnePlayOneOddsHandler.class);

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

	public void handler(List<OrderBetRecord> orderBetRecords, String issue, String number, List<Integer> playIds,
						int lotteryId, boolean jiesuanOrNot, String date, double divisor) {
		if (CollectionUtils.isEmpty(orderBetRecords)) {
			log.error("待结算的数据为空");
			return;
		}
		log.debug("OnePlayOneOdds待结算数据共[{}]条", orderBetRecords.size());
		// 获取所有赔率信息
		for (OrderBetRecord orderBet : orderBetRecords) {
			log.info("待结算的订单信息:[{}]", orderBet.toString());
			// 获取所有配置id
			List<Integer> settingIds = new ArrayList<>();
			settingIds.add(orderBet.getSettingId());
			String betNumber = orderBet.getBetNumber();
			boolean playType = betNumber.indexOf("不中") >= 0;
			LotteryPlayOdds odds;
			Map<?, ?> oddsMap;
			if (playType) {
				oddsMap = lotteryPlayOddsService.selectPlayOddsBySettingIds(settingIds);
				odds = (LotteryPlayOdds) oddsMap.get(settingIds.get(0));
			} else {
				oddsMap = lotteryPlayOddsService.selectPlayOddsBySettingId(settingIds.get(0));
				odds = (LotteryPlayOdds) oddsMap.get(betNumber.split("@")[1]);
				if (odds == null) {
					odds = (LotteryPlayOdds) oddsMap.get(betNumber.split("@")[0]);
				}
				if (odds == null) {
					// 正特1 正特2...
					odds = (LotteryPlayOdds) oddsMap.get(betNumber.split("@")[1].split(",")[0]);
				}
			}
			BigDecimal winAmount = new BigDecimal(0);
			try {
				String betNumberLin = orderBet.getBetNumber().replace("一", "1").replace("二", "2").replace("三", "3")
						.replace("四", "4").replace("五", "5").replace("六", "6");
				if (betNumberLin.indexOf("两面") >= 0
						// 做一个特殊处理 正特两面过滤
						|| ((betNumberLin.indexOf("正1特") >= 0 || betNumberLin.indexOf("正2特") >= 0
								|| betNumberLin.indexOf("正3特") >= 0 || betNumberLin.indexOf("正4特") >= 0
								|| betNumberLin.indexOf("正5特") >= 0 || betNumberLin.indexOf("正6特") >= 0)
								&& (betNumberLin.indexOf("大") >= 0 || betNumberLin.indexOf("小") >= 0
										|| betNumberLin.indexOf("单") >= 0 || betNumberLin.indexOf("双") >= 0
										|| betNumberLin.indexOf("尾大") >= 0 || betNumberLin.indexOf("尾小") >= 0
										|| betNumberLin.indexOf("合单") >= 0 || betNumberLin.indexOf("合双") >= 0
										|| betNumberLin.indexOf("红波") >= 0 || betNumberLin.indexOf("蓝波") >= 0
										|| betNumberLin.indexOf("绿波") >= 0))) {
					continue;
				}
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
					winAmount = orderBet.getBetAmount()
							.divide(BigDecimal.valueOf(orderBet.getBetCount()), BigDecimal.ROUND_HALF_UP)
							.multiply(BigDecimal.valueOf(odd));
					winAmount = winAmount.multiply(BigDecimal.valueOf(winCounts));
					orderBet.setWinCount(String.valueOf(winCounts));
				}
				// 根据中奖金额,修改投注信息及相关信息
				// OrderRecord orderRecord = orderMap.get(orderBet.getOrderId());
				if (jiesuanOrNot) {
					log.info("clearingOnePlayOneOdds 开奖判断信息{};{};{};{};{};{}", orderBet.getOrderSn(),
							orderBet.getBetNumber(), number, orderBet.getPlayId(), lotteryId, winAmount);
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

			} catch (TransactionSystemException e) {
				log.error("订单结算出错 事务冲突 进行重试。orderSn:{}.", orderBet.getOrderSn(), e);
				for (int i = 0; i < 20; i++) {
					try {
						if (jiesuanOrNot) {
							betCommonService.winOrLose(orderBet, winAmount, orderBet.getUserId(),
									orderBet.getOrderSn());
						}
					} catch (TransactionSystemException e2) {
						log.error("订单结算出错,事务冲突 进行重试。orderSn:{}.", orderBet.getOrderSn(), e2);
						try {
							Thread.sleep(100);
						} catch (Exception e3) {
						}
						continue;
					}
					break;
				}
			} catch (Exception e) {
				log.error("订单结算出错，lotteryId:{},issue:{},betNum:{}", orderBet.getLotteryId(), issue,
						orderBet.getBetNumber(), e);
			}
		}

	}

}
