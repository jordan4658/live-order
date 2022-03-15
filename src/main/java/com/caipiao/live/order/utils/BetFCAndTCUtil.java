package com.caipiao.live.order.utils;

import java.util.HashMap;

public class BetFCAndTCUtil {
	
	public final static int RED = 6;

	public final static int BLUE = 1;

	public final static int MAX_BLUE = 16;

	public final static int MAX_RED = 33;
	
	/**
	 * #福彩双色球
	 * @param openNubmer 开奖号码
	 * @param betNubmer  投注号码
	 * @return
	 */
	public static HashMap<Integer, Integer> isWinByFCSSQ(String openNubmer,String betNubmer){
		int redCount = 0;// 红球中奖个数
		int blueCount = 0;// 蓝球
		// 开奖红球和蓝球
		String openNumberRed = openNubmer.substring(0,openNubmer.lastIndexOf(","));
		openNumberRed = getBiaoString(openNumberRed);
		String openNumberBlue = openNubmer.substring(openNubmer.lastIndexOf(",")+1);
		openNumberBlue = getBiaoString(openNumberBlue);
		// 分解红球、蓝球投注号码
		String[] betNumberArray = betNubmer.replaceAll("(?:红球区@|蓝球区@)", "").split("_");
		String[] betNumberRed = betNumberArray[0].split(",");
		String[] betNumberBlue = betNumberArray[1].split(",");
		// 统计中奖红球
		for (String number : betNumberRed) {
			if(openNumberRed.contains(number.length()==1?("0"+number):number)) {
				redCount++;
			}
		}
		// 统计中奖蓝球
		for (String number : betNumberBlue) {
			if(openNumberBlue.equals(number.length()==1?("0"+number):number)) {
				blueCount = 1;
			}
		}
		return calculateRed(betNumberRed.length,redCount,betNumberBlue.length,blueCount);
	}

	//格式化为两位数号码
	public static String getBiaoString(String number){
//		红球区@01,02,03,04,05,06,07,08   或者 01,02,03,04,05,06,07,08

		String totalArray[] = number.split("@");
		if(totalArray.length == 1){
			String biao = "";
			String array[] = totalArray[0].split(",");
			for(String single:array){
				if(single.length() == 1){
					biao = biao + ("0" + single) + ",";
				}else{
					biao = biao + single + ",";
				}
			}
			return biao.substring(0,biao.length()-1);
		}else if(totalArray.length == 2){
			String biao = "";
			String array[] = totalArray[1].split(",");
			for(String single:array){
				if(single.length() == 1){
					biao = biao + ("0" + single) + ",";
				}else{
					biao = biao + single + ",";
				}
			}
			return totalArray[0] + "@" + biao.substring(0,biao.length()-1);
		}
		return null;
	}
	
	/**
	 * #体彩大乐透
	 * @param openNubmer 开奖号码
	 * @param betNubmer  投注号码
	 * @return
	 */
	public static HashMap<Integer, Integer> isWinByTCDLT(String openNubmer,String betNubmer){
		openNubmer = getBiaoString(openNubmer);
		String numberArray[] = betNubmer.split("_");
		betNubmer = getBiaoString(numberArray[0])+"_"+getBiaoString(numberArray[1]);
		int redCount = 0;// 红球中奖个数
		int blueCount = 0;// 蓝球
		// 开奖红球和蓝球
		int subIndex = openNubmer.lastIndexOf (',',(openNubmer.lastIndexOf (",")-1));
		String openNumberRed = openNubmer.substring(0,subIndex);
		String openNumberBlue = openNubmer.substring(subIndex+1);
		// 分解红球、蓝球投注号码
		String[] betNumberArray = betNubmer.replaceAll("(?:红球区@|蓝球区@)", "").split("_");
		String[] betNumberRed = betNumberArray[0].split(",");
		String[] betNumberBlue = betNumberArray[1].split(",");
		// 统计中奖红球
		for (String number : betNumberRed) {
			if(openNumberRed.contains(number)) {
				redCount++;
			}
		}
		// 统计中奖蓝球
		for (String number : betNumberBlue) {
			if(openNumberBlue.contains(number)) {
				blueCount++;
			}
		}
		// 中奖等级匹配
		DLTCalc dltCalc = new DLTCalc();
		return dltCalc.win(betNumberRed.length, redCount, betNumberBlue.length, blueCount);
	}
	
	/**
	 * #福彩七乐彩
	 * @param openNubmer 开奖号码
	 * @param betNubmer  投注号码
	 * @return
	 */
	public static HashMap<Integer, Integer> isWinByFCQLC(String openNubmer,String betNubmer){
		int countZM = 0;// 正码
		int countTM = 0;// 特码
		openNubmer = getBiaoString(openNubmer);
		betNubmer = getBiaoString(betNubmer);

		// 开奖正码和特码
		String openNumberZM = openNubmer.substring(0,openNubmer.lastIndexOf(","));
		String openNumberTM = openNubmer.substring(openNubmer.lastIndexOf(",")+1);


		// 投注正码
		String[] betNumberArray = betNubmer.replaceAll("(?:红球区@|蓝球区@)", "").split(",");
		// 统计中奖正码
		for (String number : betNumberArray) {
			if(openNumberZM.contains(number)) {
				countZM++;
			}
		}
		// 统计中奖特码
		if(betNubmer.contains(openNumberTM)) {
			countTM = 1;
		}
		// 中奖等级匹配
		QLCCalc qlcCalc = new QLCCalc();
		return qlcCalc.win(betNumberArray.length, countZM, countTM);
	}
	
	/**
	 * @Description: 计算双色球中奖情况    
	 * @param betRed 选中的红球个数     
	 * @param guessRed 猜中的红球个数   
	 * @param betBlue 选中的篮球个数   
	 * @param guessBlue 猜中的篮球个数     
	 */
	public static HashMap<Integer, Integer> calculateRed(int betRed, int guessRed, int betBlue, int guessBlue) {
		if (betRed > MAX_RED || betRed < RED || guessRed > RED || guessRed < 0 || betBlue < BLUE || betBlue > MAX_BLUE
				|| guessBlue > BLUE || guessBlue < 0) {
			throw new IllegalArgumentException("参数不合法！");
		}
		HashMap<Integer,Integer> result = new HashMap<>();
		result.put(1, 0);
		result.put(2, 0);
		result.put(3, 0);
		result.put(4, 0);
		result.put(5, 0);
		result.put(6, 0);
		int notGuessBule = betBlue - guessBlue;
		for (int i = guessRed; i >= 0; i--) {
			if (betRed - guessRed + i < 6) {
				break;
			}
			int recoreds = combination(guessRed, i) * combination(betRed - guessRed, RED - i);

			if (recoreds * guessBlue != 0) {
				// 中奖等级匹配
				if(i == 6&&guessBlue == 1){
					// 一等奖
					result.put(1, result.get(1) + recoreds * guessBlue);
				}
				else if(i == 6&&guessBlue == 0){
					// 二等奖
					result.put(2, result.get(2) + recoreds * guessBlue);
				}
				else if(i == 5&&guessBlue == 1){
					// 三等奖
					result.put(3, result.get(3) + recoreds * guessBlue);
				}
				else if((i == 5&&guessBlue == 0)||(i == 4&&guessBlue == 1)){
					// 四等奖
					result.put(4, result.get(4) + recoreds * guessBlue);
				}
				else if((i == 4&&guessBlue == 0)||(i == 3&&guessBlue == 1)){
					// 五等奖
					result.put(5, result.get(5) + recoreds * guessBlue);
				}
				else if(i == 0&&guessBlue == 1||(i == 1&&guessBlue == 1)
						||(i == 2&&guessBlue == 1)){
					// 六等奖
					result.put(6, result.get(6) + recoreds * guessBlue);
				}	
			}
			if (recoreds * notGuessBule != 0) {
				// 中奖等级匹配
				if(i == 6){
					// 二等奖
					result.put(2, result.get(2) + recoreds * notGuessBule);
				}
				else if(i == 5){
					// 四等奖
					result.put(4, result.get(4) + recoreds * notGuessBule);
				}
				else if(i == 4){
					// 五等奖
					result.put(5, result.get(5) + recoreds * notGuessBule);
				}
			}
		}
		return result;
	}

	public static int combination(int m, int n) {
		int k = 1;
		int j = 1;
		for (int i = n; i >= 1; i--) {
			k = k * m;
			j = j * n;
			m--;
			n--;
		}
		return k / j;
	}
}
