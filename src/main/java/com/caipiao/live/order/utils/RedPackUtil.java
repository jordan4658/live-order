package com.caipiao.live.order.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RedPackUtil {

	/**
	 * 返回一次抽奖在指定中奖概率下是否中奖
	 * 
	 * @param rate 中奖概率
	 * @return
	 */
	public static boolean canReward(double rate) {
		return Math.random() <= rate;
	}

	/**
	 * 返回min~max区间内随机数，含min和max
	 * 
	 * @param min
	 * @param max
	 * @return
	 */
	private static int getRandomVal(int min, int max) {
		Random random = new Random();
		int num = max - min;
		if (max > 0 && num > 1) {
			return random.nextInt(max - min + 1) + min;
		} else {
			return min;
		}
	}

	/**
	 * 带概率偏向的随机算法，概率偏向subMin~subMax区间
	 * 返回boundMin~boundMax区间内随机数（含boundMin和boundMax），同时可以指定子区间subMin~subMax的优先概率
	 * 例：传入参数(10, 50, 20, 30, 0.8)，则随机结果有80%概率从20~30中随机返回，有20%概率从10~50中随机返回
	 * 
	 * @param boundMin 边界
	 * @param boundMax
	 * @param subMin
	 * @param subMax
	 * @param subRate
	 * @return
	 */
	public static int getRandomValWithSpecifySubRate(int boundMin, int boundMax, int subMin, int subMax,
			double subRate) {
		if (canReward(subRate)) {
			return getRandomVal(subMin, subMax);
		}
		return getRandomVal(boundMin, boundMax);
	}

	/**
	 * 随机分配第n个红包
	 * 
	 * @param totalBonus  总红包量
	 * @param totalNum    总份数
	 * @param sendedBonus 已发送红包量
	 * @param sendedNum   已发送份数
	 * @param rdMin       随机下限
	 * @param rdMax       随机上限
	 * @return
	 */
	private static Integer randomBonusWithSpecifyBound(Integer totalBonus, Integer totalNum, Integer sendedBonus,
			Integer sendedNum, Integer rdMin, Integer rdMax) {
		Integer avg = totalBonus / totalNum; // 平均值
		Integer leftLen = avg - rdMin;
		Integer rightLen = rdMax - avg;
		Integer boundMin = 0, boundMax = 0;

		// 大范围设置小概率
		if (leftLen.equals(rightLen)) {
			boundMin = Math.max((totalBonus - sendedBonus - (totalNum - sendedNum - 1) * rdMax), rdMin);
			boundMax = Math.min((totalBonus - sendedBonus - (totalNum - sendedNum - 1) * rdMin), rdMax);
		} else if (rightLen.compareTo(leftLen) > 0) {
			// 上限偏离
			double bigRate = leftLen / (double) (leftLen + rightLen);
			Integer standardRdMax = avg + leftLen; // 右侧对称上限点
			Integer _rdMax = canReward(bigRate) ? rdMax : standardRdMax;
			boundMin = Math.max((totalBonus - sendedBonus - (totalNum - sendedNum - 1) * standardRdMax), rdMin);
			boundMax = Math.min((totalBonus - sendedBonus - (totalNum - sendedNum - 1) * rdMin), _rdMax);
		} else {
			// 下限偏离
			double smallRate = rightLen / (double) (leftLen + rightLen);
			Integer standardRdMin = avg - rightLen; // 左侧对称下限点
			Integer _rdMin = canReward(smallRate) ? rdMin : standardRdMin;
			boundMin = Math.max((totalBonus - sendedBonus - (totalNum - sendedNum - 1) * rdMax), _rdMin);
			boundMax = Math.min((totalBonus - sendedBonus - (totalNum - sendedNum - 1) * standardRdMin), rdMax);
		}

		// 已发平均值偏移修正-动态比例
		if (boundMin.equals(boundMax)) {
			return getRandomVal(boundMin, boundMax);
		}
		double currAvg = sendedNum == 0 ? (double) avg : (sendedBonus / (double) sendedNum); // 当前已发平均值
		double middle = (boundMin + boundMax) / 2.0;
		Integer subMin = boundMin, subMax = boundMax;
		// 期望值
		double exp = avg - (currAvg - avg) * sendedNum / (double) (totalNum - sendedNum);
		if (middle > exp) {
			subMax = (int) Math.round((boundMin + exp) / 2.0);
		} else {
			subMin = (int) Math.round((exp + boundMax) / 2.0);
		}
		Integer expBound = (boundMin + boundMax) / 2;
		Integer expSub = (subMin + subMax) / 2;
		double subRate = (exp - expBound) / (double) (expSub - expBound);
		return getRandomValWithSpecifySubRate(boundMin, boundMax, subMin, subMax, subRate);
	}

	/**
	 * 生成红包一次分配结果
	 * 
	 * @param totalBonus 总红包量
	 * @param totalNum   总份数
	 * @return
	 */
	public static List<Integer> createBonusList(Integer totalBonus, Integer totalNum, Integer rdMin, Integer rdMax) {
		Integer sendedBonus = 0;
		Integer sendedNum = 0;
		List<Integer> bonusList = new ArrayList<>();
		while (sendedNum < totalNum) {
			Integer bonus = randomBonusWithSpecifyBound(totalBonus, totalNum, sendedBonus, sendedNum, rdMin, rdMax);
			bonusList.add(bonus);
			sendedNum++;
			sendedBonus += bonus;
		}
		return bonusList;
	}

}
