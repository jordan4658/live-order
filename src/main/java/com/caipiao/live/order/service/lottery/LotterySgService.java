package com.caipiao.live.order.service.lottery;

import java.util.Map;

public interface LotterySgService {
	/**
	 * 获取历史赛果
	 * @param pageNo
	 * @param pageSize
	 * @param id
	 * @return
	 */
	Map<String, Object> lishiSg(Integer pageNo, Integer pageSize, Integer id);

	/**
	 * 十分时时彩 开奖结果记录
	 * @param pageNo
	 * @param pageSize
	 * @return
	 */
	Map<String, Object> lishitensscSg(Integer pageNo, Integer pageSize);

	/**
	 * 五分时时彩 开奖结果记录
	 * @param pageNo
	 * @param pageSize
	 * @return
	 */
    Map<String, Object> lishiFivesscSg(Integer pageNo, Integer pageSize);

	/**
	 * 急速时时彩 开奖结果记录
	 * @param pageNo
	 * @param pageSize
	 * @return
	 */
    Map<String, Object> lishijssscSg(Integer pageNo, Integer pageSize);

	/**
	 * 十分北京pk10 开奖结果记录
	 * @param pageNo
	 * @param pageSize
	 * @return
	 */
    Map<String, Object> lishitenpksSg(Integer pageNo, Integer pageSize);

	/**
	 * 五分北京pk10  开奖结果记录
	 * @param pageNo
	 * @param pageSize
	 * @return
	 */
	Map<String, Object> lishifivepksSg(Integer pageNo, Integer pageSize);

	/**
	 * 急速北京pk10 开奖结果记录
	 * @param pageNo
	 * @param pageSize
	 * @return
	 */
	Map<String, Object> lishijspksSg(Integer pageNo, Integer pageSize);

	/**
	 * 幸运飞艇 开奖结果记录
	 * @param pageNo
	 * @param pageSize
	 * @return
	 */
	Map<String, Object> lishixyfeitSg(Integer pageNo, Integer pageSize);

	/**
	 * 德州幸运飞艇 开奖结果记录
	 * @param pageNo
	 * @param pageSize
	 * @return
	 */
	Map<String, Object> lishiDzxyfeitSg(Integer pageNo, Integer pageSize);

	/**
	 * 德州pc蛋蛋 开奖结果记录
	 * @param pageNo
	 * @param pageSize
	 * @return
	 */
	Map<String, Object> lishiDzpcdandSg(Integer pageNo, Integer pageSize);

}
