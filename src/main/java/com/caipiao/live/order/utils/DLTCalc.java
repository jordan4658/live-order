package com.caipiao.live.order.utils;

import java.util.HashMap;

/**
 *	大乐透
 * 2019/5/28
 * @author dlucky
 */
public class DLTCalc {
	private final static int F_NUM = 5;
	private final static int B_NUM = 2;
	private final static int F_MAX = 35;
	private final static int B_MAX = 12;
	private final static int[][] DLT_PRIZE = new int[][] {{5,2},{5,1},{5,0},{4,2},{4,1},{3,2},{4,0},{3,1,2,2},{3,0,1,2,2,1,0,2}};

	// 排列组合
	public static int combine(int m, int n) {
		if (m < n ) {
			return 0;
		}
		return factorial(m, m - n + 1) / factorial(n, 1);
	}
	
	// 阶乘
	public static int factorial(int max, int min) {
		if (max >= min && max > 1) {
			return max * factorial(max - 1, min);
		} else {
			return 1;
		}
	}
	
	public void checkNum(int fOpt,int fOptHit,int bOpt,int bOptHit) {
		if (fOpt > F_MAX) {
			throw new IllegalArgumentException("红球号码数量不能超过" + F_MAX + "个");
		}
		if (bOpt > B_MAX) {
			throw new IllegalArgumentException("篮球号码数量不能超过" + B_MAX + "个");

		}
		if (fOpt < F_NUM) {
			throw new IllegalArgumentException("红球号码数量不能少于" + F_NUM + "个");
		}
		if (bOpt < B_NUM) {
			throw new IllegalArgumentException("篮球号码数量不能少于" + B_NUM + "个");
		}
		if (fOptHit > F_NUM) {
			throw new IllegalArgumentException("猜中红球号码数量不能超过" + F_NUM + "个");
		}
		if (bOptHit > B_NUM) {
			throw new IllegalArgumentException("猜中篮球号码数量不能超过" + B_NUM + "个");
		}
	}
	
	// 计算篮球或红球指定中奖个数注数
	public int[] solveHits(int num,int req,int opt,int reqHit,int optHit) {
		int optLeft = num - req;
		int optMiss = opt - optHit;
		int	max = reqHit + optHit;
		int[] hits = new int[9];
		for (int i = 0; i <= num; ++ i) {
			if (i < reqHit || i > max) {
				hits[i] = 0;
			} else {
				int optNeed = i - reqHit;
				hits[i] = combine(optHit, optNeed) * combine(optMiss, optLeft - optNeed);
			}
		}
		return hits;
	}
	
	// 计算各奖项注数
	public HashMap<Integer, Integer> win(int fOpt,int fOptHit,int bOpt,int bOptHit) {
		checkNum(fOpt,fOptHit,bOpt,bOptHit);
		int[] fHits = solveHits(F_NUM, 0, fOpt, 0, fOptHit);
		int[] bHits = solveHits(B_NUM, 0, bOpt, 0, bOptHit);
		HashMap<Integer, Integer> result = new HashMap<Integer, Integer>();
		for (int i = 0; i < DLT_PRIZE.length; i++) {
			int count = 0;
			if(i < 7) {
				count += fHits[DLT_PRIZE[i][0]] * bHits[DLT_PRIZE[i][1]];
			}
			if(i == 7) {
				count += (fHits[DLT_PRIZE[i][0]] * bHits[DLT_PRIZE[i][1]]) + (fHits[DLT_PRIZE[i][2]] * bHits[DLT_PRIZE[i][3]]);
			}
			if(i == 8) {
				count += (fHits[DLT_PRIZE[i][0]] * bHits[DLT_PRIZE[i][1]]) + (fHits[DLT_PRIZE[i][2]] * bHits[DLT_PRIZE[i][3]]);
				count += (fHits[DLT_PRIZE[i][4]] * bHits[DLT_PRIZE[i][5]]) + (fHits[DLT_PRIZE[i][6]] * bHits[DLT_PRIZE[i][7]]);
			}
			result.put(i+1, count);
		}
		return result;
	}

}
