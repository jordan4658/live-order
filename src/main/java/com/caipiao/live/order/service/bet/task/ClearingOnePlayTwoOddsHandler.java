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
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class ClearingOnePlayTwoOddsHandler {

	private static final Logger log = LoggerFactory.getLogger(ClearingOnePlayTwoOddsHandler.class);

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
		log.debug("OnePlayTwoOdds待结算数据共[{}]条", orderBetRecords.size());
		for (OrderBetRecord orderBet : orderBetRecords) {
			log.info("待结算的订单信息:[{}]", orderBet.toString());
			BigDecimal winAmount = new BigDecimal(0);
			try {
				// 获取所有配置id
				List<Integer> settingIds = new ArrayList<>();
				settingIds.add(orderBet.getSettingId());
				// 获取所有赔率信息
				Map<String, LotteryPlayOdds> oddsMap = lotteryPlayOddsService
						.selectPlayOddsBySettingId(settingIds.get(0));
				orderBet.setWinCount("0");
				// 判断是否中奖,获取中奖注数
				List<Integer> twoOdds = LhcUtils.isWinByOnePlayTwoOdds(orderBet.getBetNumber(), number,
						orderBet.getPlayId(), date, lotteryId);
				if (twoOdds.get(0) > 0 || twoOdds.get(1) > 0) {
					int bigOddsWins = twoOdds.get(0);
					int smallOddsWins = twoOdds.get(1);
					orderBet.setWinCount(String.valueOf(bigOddsWins + smallOddsWins));
					if (bigOddsWins > 0) {
						// 获取 高赔率生肖 赔率信息
						LotteryPlayOdds odds = oddsMap.get(oddsMap.keySet().iterator().next());
						if (oddsMap.size() > 1) {
							double oddBig = 0;
							Set<String> sets = oddsMap.keySet();
							for (String set : sets) {
								LotteryPlayOdds thisDomain = (LotteryPlayOdds) oddsMap.get(set);
								double thisOddBig = Double.parseDouble(thisDomain.getTotalCount())
										/ Double.parseDouble(thisDomain.getWinCount());
								if (thisOddBig > oddBig) {
									oddBig = thisOddBig;
									odds = thisDomain;
								}
							}
						}

						// 获取总注数/中奖注数 & 计算赔率
						List<Double> oddsList = new ArrayList<Double>();
						String[] totalCountArr = odds.getTotalCount().split("/");
						String winCount = odds.getWinCount();
						if (totalCountArr.length < 2) {
							double odd = Double.parseDouble(totalCountArr[0]) * 1.0 / Double.parseDouble(winCount)
									* divisor;
							oddsList.add(odd);
						} else {
							double oddBig = Double.parseDouble(totalCountArr[0]) * 1.0 / Double.parseDouble(winCount)
									* divisor;
							oddsList.add(oddBig);
						}
						BigDecimal bigWins = orderBet.getBetAmount().divide(BigDecimal.valueOf(orderBet.getBetCount()))
								.multiply(BigDecimal.valueOf(oddsList.get(0)));
						winAmount = winAmount.add(bigWins.multiply(BigDecimal.valueOf(bigOddsWins)));
					}
					if (smallOddsWins > 0) {
						// 获取 低赔率生肖 赔率信息
						LotteryPlayOdds odds;
						if (oddsMap.size() == 1) {
							odds = oddsMap.get(oddsMap.keySet().iterator().next());
						} else {
							String shengXiao = LhcUtils.getShengXiao(date);
							odds = oddsMap.get(shengXiao);
						}
						// 低赔率为0尾的赔率
						if (odds == null) {
							odds = oddsMap.get("0尾");
						}
						// 获取总注数/中奖注数 & 计算赔率
						List<Double> oddsList = new ArrayList<Double>();
						String[] totalCountArr = odds.getTotalCount().split("/");
						String winCount = odds.getWinCount();
						if (totalCountArr.length < 2) {
							double odd = Double.parseDouble(totalCountArr[0]) * 1.0 / Double.parseDouble(winCount)
									* divisor;
							oddsList.add(odd);
						} else {
							double oddSmall = Double.parseDouble(totalCountArr[1]) * 1.0 / Double.parseDouble(winCount)
									* divisor;
							oddsList.add(oddSmall);
						}
						BigDecimal smallWins = orderBet.getBetAmount()
								.divide(BigDecimal.valueOf(orderBet.getBetCount()))
								.multiply(BigDecimal.valueOf(oddsList.get(0)));
						winAmount = winAmount.add(smallWins.multiply(BigDecimal.valueOf(smallOddsWins)));
					}
				}
				if (twoOdds.size() == 2) {
					log.info("clearingOnePlayTwoOdds 开奖判断信息{};{};{};{};{};{};{};{}", orderBet.getOrderSn(),
							orderBet.getBetNumber(), number, orderBet.getPlayId(), lotteryId, twoOdds.get(0),
							twoOdds.get(1), winAmount);
				}

				// 根据中奖金额,修改投注信息及相关信息
				// OrderRecord orderRecord = orderMap.get(orderBet.getOrderId());
				if (jiesuanOrNot) {
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
				log.error("订单结算出错 事务冲突 进行重试:{},{}", orderBet.getOrderSn(), e);
				for (int i = 0; i < 20; i++) {
					try {
						if (jiesuanOrNot) {
							betCommonService.winOrLose(orderBet, winAmount, orderBet.getUserId(),
									orderBet.getOrderSn());
						}
					} catch (TransactionSystemException e2) {
						log.error("订单结算出错:{},事务冲突 进行重试:{},{}", orderBet.getOrderSn(), e2);
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
