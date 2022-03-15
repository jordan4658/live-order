package com.caipiao.live.order.service.lottery;
import com.caipiao.live.common.model.common.ResultInfo;
import com.caipiao.live.common.mybatis.entity.Bonus;


public interface BonusWriteService {

	Boolean doEditBonus(Bonus bonus);

	Bonus queryBonusByPlayId(Integer playId);

	ResultInfo<String> savaBetRestrict(Integer lotteryId, Integer playId, Integer maxMoney);

}
