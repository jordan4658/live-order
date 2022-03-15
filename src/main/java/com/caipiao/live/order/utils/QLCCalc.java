package com.caipiao.live.order.utils;

import java.util.HashMap;
/**
 *     七乐彩
 * 2019/5/28
 * @author dlucky
 */
public class QLCCalc {
	private final static int QLC_BNUM = 7;
	private final static int[][] QLC_PRIZE = new int[][] {{7,0},{6,1},{6,0},{5,1},{5,0},{4,1},{4,0}};
	private final static HashMap<Integer, Integer> QLC_RESULT = new HashMap<Integer, Integer>();

	// 排列组合
	public int combine(int m, int n) {
		if (m < n ) {
			return 0;
		}
		return factorial(m, m - n + 1) / factorial(n, 1);
	}
	
	// 阶乘
	public int factorial(int max, int min) {
		if (max >= min && max > 1) {
			return max * factorial(max - 1, min);
		} else {
			return 1;
		}
	}
	
	// 奖级注数计算
	public HashMap<Integer, Integer> win(int max,int hit,int tm){
		int sd = 0;
		int	nh = max-hit;
		for (int i = 0; i < QLC_PRIZE.length; i++) {
			int must = QLC_PRIZE[i][0];
			int h = combine(hit,must);
			int nhx = QLC_BNUM - must;
			int snum1 = 0;
			int snum2 = 0;
			if(tm == 1){
				if(sd == 1){
					snum1 = combine(nh,nhx);
					snum2 = 0;
				}else{
					snum1 = combine(nh - 1,nhx - 1);
					snum2 = combine(nh - 1,nhx);
				}
			}else{
				snum1 = 0;
				snum2 = combine(nh, nhx);
			}
			if(QLC_PRIZE[i][1] == 1){
				QLC_RESULT.put(i + 1, h * snum1);
			}else{
				QLC_RESULT.put(i + 1, h * snum2);
				if(i == 0 && max == 7 && hit == 7 && tm == 1) {
					QLC_RESULT.put(i + 1, 1);
				}
			}
		}
		return QLC_RESULT;
	}
}
