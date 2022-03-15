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
import com.caipiao.live.common.mybatis.mapper.OrderBetRecordMapper;
import com.caipiao.live.common.util.lottery.LhcUtils;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
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

import static com.caipiao.live.common.util.ViewUtil.getTradeOffAmount;

@Component
public class ClearingOnePlayManyOddsHandler {

	private static final Logger log = LoggerFactory.getLogger(ClearingOnePlayManyOddsHandler.class);

	@Autowired
	private RedisTemplate<String, Object> redisTemplate;
	@Autowired
	private OrderBetRecordMapper orderBetRecordMapper;
	@Autowired
	private LotteryPlayOddsWriteService lotteryPlayOddsService;
	@Autowired
	private LotteryPlayWriteService lotteryPlayWriteService;
	@Autowired
	private LotteryWriteService lotteryWriteService;
	@Autowired
	private BetCommonService betCommonService;

	// 六合彩特码特码A两面玩法id
	private final String PLAY_ID_TM_TMA_LM = "02";
	// 六合彩正码正码A玩法id
	public final static String PLAY_ID_ZM_ZMA = "03";
	// 六合彩正码正码1-6玩法id集合
	private final String PLAY_IDS_ZM = "04";
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

	// 可能打和的投注信息
	private final List<String> MAYBE_HE = Lists.newArrayList("大", "小", "单", "双", "合单", "合双", "尾大", "尾小", "家禽", "野兽");

	public void handler(List<OrderBetRecord> orderBetRecords, String issue, String number, Integer playId,
						int lotteryId, boolean jiesuanOrNot, String date, List<Integer> playlist, double divisor) {
		if (CollectionUtils.isEmpty(orderBetRecords)) {
			log.error("待结算的数据为空");
			return;
		}
		log.debug("OnePlayManyOdds待结算数据共[{}]条", orderBetRecords.size());
		OrderBetRecord order = new OrderBetRecord();
		try {
			String betNumber = orderBetRecords.get(0).getBetNumber();
			// 玩法设计...共用了一个playID...., 集合中所有数据都是一个玩法，
			if (this.generationLHCPlayId(PLAY_ID_TM_TMA_LM, lotteryId).equals(playId) && betNumber.indexOf("特码两面") < 0
					|| this.generationLHCPlayId(PLAY_ID_TM_TMA_LM, lotteryId).equals(playId)
							&& betNumber.indexOf("正码两面") > 0
					|| this.generationLHCPlayId(PLAY_ID_ZM_ZMA, lotteryId).equals(playId)
							&& betNumber.indexOf("两面") < 0) {
				return;
			}
			// 波色投注号码不存在打和的情况
			if (betNumber.indexOf("波色") < 0 && this.generationLHCPlayId(PLAY_ID_TM_TMA_LM, lotteryId).equals(playId)) {
				// 如果是特码A两面玩法,部分投注,特肖开出49为和值
				if ("49".equals(number.split(",")[6])) {
					orderBetRecords = noWinOrLose(orderBetRecords);
				}
			}

			// 正码1-6(正1-6特两面)的和值情况
			if (number.indexOf("49") >= 0) {
				String[] sgArr = number.split(",");
				if ("49".equals(sgArr[0]) && (playId.equals(this.generationLHCPlayId(PLAY_IDS_ZM, lotteryId))
						|| playId.equals(this.generationLHCPlayId(PLAY_ID_ZT_ONE, lotteryId)))) {
					List<OrderBetRecord> heList = new ArrayList<>();
					List<OrderBetRecord> noheList = new ArrayList<>();
					for (OrderBetRecord orderBetRecordDTO : orderBetRecords) {
						if (orderBetRecordDTO.getBetNumber().contains("正码一")
								|| playId.equals(this.generationLHCPlayId(PLAY_ID_ZT_ONE, lotteryId))) {
							heList.add(orderBetRecordDTO);
						} else {
							noheList.add(orderBetRecordDTO);
						}
					}
					noheList.addAll(noWinOrLose(heList));
					orderBetRecords = noheList;
				} else if ("49".equals(sgArr[1]) && (playId.equals(this.generationLHCPlayId(PLAY_IDS_ZM, lotteryId))
						|| playId.equals(this.generationLHCPlayId(PLAY_ID_ZT_TWO, lotteryId)))) {
					List<OrderBetRecord> heList = new ArrayList<>();
					List<OrderBetRecord> noheList = new ArrayList<>();
					for (OrderBetRecord orderBetRecordDTO : orderBetRecords) {
						if (orderBetRecordDTO.getBetNumber().contains("正码二")
								|| playId.equals(this.generationLHCPlayId(PLAY_ID_ZT_TWO, lotteryId))) {
							heList.add(orderBetRecordDTO);
						} else {
							noheList.add(orderBetRecordDTO);
						}
					}
					noheList.addAll(noWinOrLose(heList));
					orderBetRecords = noheList;
				} else if ("49".equals(sgArr[2]) && (playId.equals(this.generationLHCPlayId(PLAY_IDS_ZM, lotteryId))
						|| playId.equals(this.generationLHCPlayId(PLAY_ID_ZT_THREE, lotteryId)))) {
					List<OrderBetRecord> heList = new ArrayList<>();
					List<OrderBetRecord> noheList = new ArrayList<>();
					for (OrderBetRecord orderBetRecordDTO : orderBetRecords) {
						if (orderBetRecordDTO.getBetNumber().contains("正码三")
								|| playId.equals(this.generationLHCPlayId(PLAY_ID_ZT_THREE, lotteryId))) {
							heList.add(orderBetRecordDTO);
						} else {
							noheList.add(orderBetRecordDTO);
						}
					}
					noheList.addAll(noWinOrLose(heList));
					orderBetRecords = noheList;
				} else if ("49".equals(sgArr[3]) && (playId.equals(this.generationLHCPlayId(PLAY_IDS_ZM, lotteryId))
						|| playId.equals(this.generationLHCPlayId(PLAY_ID_ZT_FOUR, lotteryId)))) {
					List<OrderBetRecord> heList = new ArrayList<>();
					List<OrderBetRecord> noheList = new ArrayList<>();
					for (OrderBetRecord orderBetRecordDTO : orderBetRecords) {
						if (orderBetRecordDTO.getBetNumber().contains("正码四")
								|| playId.equals(this.generationLHCPlayId(PLAY_ID_ZT_FOUR, lotteryId))) {
							heList.add(orderBetRecordDTO);
						} else {
							noheList.add(orderBetRecordDTO);
						}
					}
					noheList.addAll(noWinOrLose(heList));
					orderBetRecords = noheList;
				} else if ("49".equals(sgArr[4]) && (playId.equals(this.generationLHCPlayId(PLAY_IDS_ZM, lotteryId))
						|| playId.equals(this.generationLHCPlayId(PLAY_ID_ZT_FIVE, lotteryId)))) {
					List<OrderBetRecord> heList = new ArrayList<>();
					List<OrderBetRecord> noheList = new ArrayList<>();
					for (OrderBetRecord orderBetRecordDTO : orderBetRecords) {
						if (orderBetRecordDTO.getBetNumber().contains("正码五")
								|| playId.equals(this.generationLHCPlayId(PLAY_ID_ZT_FIVE, lotteryId))) {
							heList.add(orderBetRecordDTO);
						} else {
							noheList.add(orderBetRecordDTO);
						}
					}
					noheList.addAll(noWinOrLose(heList));
					orderBetRecords = noheList;
				} else if ("49".equals(sgArr[5]) && (playId.equals(this.generationLHCPlayId(PLAY_IDS_ZM, lotteryId))
						|| playId.equals(this.generationLHCPlayId(PLAY_ID_ZT_SIX, lotteryId)))) {
					List<OrderBetRecord> heList = new ArrayList<>();
					List<OrderBetRecord> noheList = new ArrayList<>();
					for (OrderBetRecord orderBetRecordDTO : orderBetRecords) {
						if (orderBetRecordDTO.getBetNumber().contains("正码六")
								|| playId.equals(this.generationLHCPlayId(PLAY_ID_ZT_SIX, lotteryId))) {
							heList.add(orderBetRecordDTO);
						} else {
							noheList.add(orderBetRecordDTO);
						}
					}
					noheList.addAll(noWinOrLose(heList));
					orderBetRecords = noheList;
				}
			}

			// 获取配置id
			if (orderBetRecords.size() == 0) {
				return;
			}
			Integer settingId = orderBetRecords.get(0).getSettingId();

			// 获取所有赔率信息
			Map<String, LotteryPlayOdds> oddsMap = lotteryPlayOddsService.selectPlayOddsBySettingId(settingId);

			for (OrderBetRecord orderBet : orderBetRecords) {
				log.info("待结算的订单信息:[{}]", orderBet.toString());
				order = orderBet;
				BigDecimal winAmount = new BigDecimal(0);
				orderBet.setWinCount("0");
				// 判断是否中奖,获取中奖信息
				String winNum = LhcUtils.isWinByOnePlayManyOdds(orderBet.getBetNumber(), number, orderBet.getPlayId(),
						date, lotteryId);
				if (StringUtils.isNotBlank(winNum)) {
					String[] winStrArr = winNum.split(",");
					int wincount = 0;
					for (String winStr : winStrArr) {
						boolean boHe = false;
						if (winStr.contains("和单")) {
							boHe = true;
							winStr = winStr.replace("和单", "");
						}
						// 获取赔率信息
						LotteryPlayOdds odds = oddsMap.get(winStr);
						// 获取总注数/中奖注数
						String winCount = odds.getWinCount();
						String totalCount = odds.getTotalCount();
						// 计算赔率
						double odd = Double.parseDouble(totalCount) * 1.0 / Double.parseDouble(winCount) * divisor;
						// 中奖额
						if (boHe) {
							winAmount = winAmount
									.add(orderBet.getBetAmount().divide(BigDecimal.valueOf(orderBet.getBetCount())));
						} else {
							winAmount = winAmount
									.add(orderBet.getBetAmount().divide(BigDecimal.valueOf(orderBet.getBetCount()))
											.multiply(BigDecimal.valueOf(odd)));
							wincount = wincount + 1;
						}
					}
					orderBet.setWinCount(String.valueOf(wincount));
				}
				// 根据中奖金额,修改投注信息及相关信息
				try {
					log.info("clearingOnePlayManyOdds 开奖判断信息{};{};{};{};{};{};{}", orderBet.getOrderSn(),
							orderBet.getBetNumber(), number, orderBet.getPlayId(), lotteryId, winNum, winAmount);
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
			}
			
		} catch (Exception e) {
			log.error("订单结算出错，lotteryId:{},issue:{},betNum:{},{}", order.getLotteryId(), issue, order.getBetNumber(),
					e);
			// break;
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
	 * 两面玩法开49打和的处理方式 （只针对一注的情况）
	 *
	 * @param orderBetRecords
	 */
	private List<OrderBetRecord> noWinOrLose(List<OrderBetRecord> orderBetRecords) {
		List<OrderBetRecord> newOrderBetRecords = new ArrayList<>();
		for (OrderBetRecord orderBet : orderBetRecords) {
			String[] betNumbetArr = orderBet.getBetNumber().split("@");
			if (MAYBE_HE.contains(betNumbetArr[1])) {
				BigDecimal winAmount = getTradeOffAmount(orderBet.getBetAmount());
				// 设置中奖金额
				orderBet.setWinAmount(winAmount);
				// 设置状态
				orderBet.setTbStatus(OrderBetStatus.HE);
				orderBet.setWinCount("0");
				// 修改投注信息
				orderBetRecordMapper.updateByPrimaryKeySelective(orderBet);
				// 修改用户余额信息

				betCommonService.updateMemberBalance(orderBet, winAmount, orderBet.getUserId(), orderBet.getOrderSn());
			} else {
				newOrderBetRecords.add(orderBet);
			}
		}
		return newOrderBetRecords;
	}

}
