package com.caipiao.live.order.rest.impl.lottery;

import com.caipiao.live.common.enums.lottery.LotteryTypeEnum;
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

import com.caipiao.live.order.rest.LotteryReadRest;
import com.caipiao.live.order.service.lottery.LotteryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;


@RestController
public class LotteryReadRestImpl implements LotteryReadRest {

    @Autowired
    private LotteryService lotteryService;

    @Override
    public Lottery queryLotteryById(Integer id) {
        return lotteryService.selectLotteryById(id);
    }

    @Override
    public Lottery queryLotteryByLotteryId(Integer lotteryId) {
        return lotteryService.selectLotteryByLotteryId(lotteryId);
    }

    @Override
    public PageResult<List<LotteryDTO>> queryInternalLotteryList(Integer cateId, String name, Integer pageNo, Integer pageSize) {
        return lotteryService.queryAllLotteryList(cateId, name, pageNo, pageSize, null);
    }

    @Override
    public List<Lottery> queryInternalLottery() {
        return lotteryService.queryLotteryByCategoryType(LotteryTypeEnum.LOTTERY.getType());
    }

    @Override
    public List<Map<String, Object>> queryAllList() {
        return lotteryService.queryAllList();
    }

    @Override
    public List<Map<String, Object>> queryInternalList() {
        return lotteryService.queryInternalList();
    }

    @Override
    public List<LotterySet> queryInternalAllList() {
        return lotteryService.queryInternalAllList(LotteryTypeEnum.LOTTERY.name());
    }

    @Override
    public SscMissNumDTO queryMissValByGroup(Integer lotteryId) {
        return lotteryService.queryMissValByGroup(lotteryId);
    }

    @Override
    public Map<String, SscMissNumDTO> querySscMissVal(Integer lotteryId, Integer start, Integer end) {
        return lotteryService.querySscMissVal(lotteryId, start, end);
    }

    @Override
    public Map<String, BjpksLiangMian> queryBjpksLmMissVal() {
        return this.lotteryService.queryBjpksLmMissVal();
    }

    @Override
    public PageResult<List<LotterySgModel>> queryOpenException(Integer lotteryId, String date, Integer pageNo, Integer pageSize) {
        return lotteryService.queryOpenException(lotteryId, date, pageNo, pageSize);
    }

    @Override
    public PageResult<List<LotterySgModel>> queryOpenNumber(Integer lotteryId, String date, String status, String issue, Integer pageNo, Integer pageSize) {
        return lotteryService.queryOpenNumber(lotteryId, date, status, issue, pageNo, pageSize);
    }

    @Override
    public List<Lottery> selectInternalLotteryList() {
        return lotteryService.selectLotteryList(LotteryTypeEnum.LOTTERY.name());
    }

    @Override
    public List<LotteryInfo> queryLotteryAllInfo(String type) {
        return lotteryService.queryLotteryAllInfo(type);
    }

    @Override
    public List<Lottery> lotteryList(Integer categoryId) {
        return lotteryService.lotteryList(categoryId);
    }

    @Override
    public List<Lottery> queryAlllotteryList() {
        return lotteryService.queryAlllotteryList();
    }

//    @Override
//    public List<LotteryFavoriteDTO> queryLotteryByLotteryFavorites(Integer uid) {
//        return lotteryService.queryLotteryByLotteryFavorites(uid);
//    }
//
//    @Override
//    public List<LotteryFavoriteDTO> queryDefaultLotteryFavorites() {
//        return lotteryService.queryDefaultLotteryFavorites();
//    }

//    @Override
//    public Map<Integer, Lottery> selectLotteryMap(String name) {
//        return lotteryService.selectLotteryMap(name);
//    }
//
//    @Override
//    public HotFavoriteDTO getFavorite() {
//        return lotteryService.getFavorite();
//    }

    /**
     * 查询直播间彩票
     *
     * @param ids
     * @return
     */
    @Override
    public ResultInfo<List<OptionSelectVo>> queryLiveLotteryList(String ids) {
        return ResultInfo.ok(lotteryService.queryLiveLotteryList(ids));

    }
}
