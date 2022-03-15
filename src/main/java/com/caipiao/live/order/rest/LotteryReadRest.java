package com.caipiao.live.order.rest;

import com.caipiao.live.common.model.LotterySet;
import com.caipiao.live.common.model.common.PageResult;
import com.caipiao.live.common.model.common.ResultInfo;
import com.caipiao.live.common.model.dto.lottery.LotteryDTO;
import com.caipiao.live.common.model.dto.lottery.LotteryFavoriteDTO;
import com.caipiao.live.common.model.dto.lottery.LotteryInfo;
import com.caipiao.live.common.model.dto.lottery.LotterySgModel;
import com.caipiao.live.common.model.dto.result.BjpksLiangMian;
import com.caipiao.live.common.model.dto.result.SscMissNumDTO;
import com.caipiao.live.common.model.vo.lottery.OptionSelectVo;
import com.caipiao.live.common.mybatis.entity.Lottery;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;




public interface LotteryReadRest {

    /**
     * 根据主键查询对象
     *
     * @param id 主键
     * @return
     */
    Lottery queryLotteryById( Integer id);

    /**
     * 根据 lotteryId 查询对象
     *
     * @param lotteryId 彩票id
     * @return
     */
    Lottery queryLotteryByLotteryId( Integer lotteryId);

    /**
     * 根据条件查询彩种列表
     *
     * @param cateId   分类id
     * @param name     彩票名称
     * @param pageNo   页码
     * @param pageSize 每页数量
     * @return
     */
    PageResult<List<LotteryDTO>> queryInternalLotteryList(Integer cateId, String name, Integer pageNo, Integer pageSize);

    /**
     * 获取全部彩种信息
     *
     * @return
     */
    List<Lottery> queryInternalLottery();

    /**
     * 获取所有彩种列表 - 购彩,首页
     *
     * @return
     */
    List<Lottery> selectInternalLotteryList();

    List<Map<String, Object>> queryAllList();

    /**
     * 获取非第三方彩种列表 - 购彩首页
     *
     * @return
     */
    List<Map<String, Object>> queryInternalList();

    /**
     * 获取彩种设置
     *
     * @return
     */
    List<LotterySet> queryInternalAllList();

    /**
     * 购彩 - 获取相应彩种的组选遗漏值
     *
     * @param lotteryId 彩种id
     * @return
     */
    SscMissNumDTO queryMissValByGroup(@RequestParam(name = "lotteryId") Integer lotteryId);

    /**
     * 购彩 - 获取时时彩直选遗漏值
     *
     * @param lotteryId 彩种id
     * @param start     开始位置
     * @param end       结束位置
     * @return
     */
    Map<String, SscMissNumDTO> querySscMissVal(@RequestParam(name = "lotteryId") Integer lotteryId,
                                               @RequestParam(name = "start") Integer start, @RequestParam(name = "end") Integer end);

    /**
     * 购彩 - 获取北京PK10两面遗漏值
     *
     * @return
     */
    Map<String, BjpksLiangMian> queryBjpksLmMissVal();

    /**
     * 获取开奖异常记录
     *
     * @param lotteryId 彩种id
     * @param date      日期
     * @param pageNo    页码
     * @param pageSize  每页数量
     * @return
     */
    PageResult<List<LotterySgModel>> queryOpenException(@RequestParam(name = "lotteryId", required = false) Integer lotteryId,
                                                        @RequestParam(name = "date", required = false) String date, @RequestParam(name = "pageNo") Integer pageNo,
                                                        @RequestParam(name = "pageSize") Integer pageSize);

    /**
     * 获取开奖异常记录
     *
     * @param lotteryId 彩种id
     * @param date      日期
     * @param status    状态
     * @param issue     期号
     * @param pageNo    页码
     * @param pageSize  每页数量
     * @return
     */
    PageResult<List<LotterySgModel>> queryOpenNumber(@RequestParam(name = "lotteryId", required = false) Integer lotteryId,
                                                     @RequestParam(name = "date", required = false) String date, @RequestParam(name = "status", required = false) String status,
                                                     @RequestParam(name = "issue", required = false) String issue, @RequestParam(name = "pageNo", required = false) Integer pageNo,
                                                     @RequestParam(name = "pageSize", required = false) Integer pageSize);

    /**
     * 移动端 查询所有彩种玩法赔率
     */
    List<LotteryInfo> queryLotteryAllInfo(@RequestParam("type") String type);

    List<Lottery> lotteryList(@RequestParam(name = "categoryId", required = false) Integer categoryId);

    List<Lottery> queryAlllotteryList();

//    /**
//     * 查询用户收藏的彩票
//     *
//     * @return
//     */
//    List<LotteryFavoriteDTO> queryLotteryByLotteryFavorites(@RequestParam("uid") Integer uid);
//
//    /**
//     * 查询默认收藏彩票信息
//     *
//     * @return
//     */
//    List<LotteryFavoriteDTO> queryDefaultLotteryFavorites();

   /* @PostMapping("/lottery/selectLotteryMap.json")
    Map<Integer, Lottery> selectLotteryMap(@RequestParam("name") String name);
*/
   /* @PostMapping("/lottery/getFavorite.json")
    HotFavoriteDTO getFavorite();
*/
    /**
     * 查询直播间彩票
     *
     * @param ids
     * @return
     */
    ResultInfo<List<OptionSelectVo>> queryLiveLotteryList(@RequestParam("ids") String ids);

}
