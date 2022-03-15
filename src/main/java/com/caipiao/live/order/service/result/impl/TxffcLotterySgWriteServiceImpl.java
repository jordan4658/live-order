package com.caipiao.live.order.service.result.impl;

import com.caipiao.live.order.service.result.TxffcLotterySgWriteService;
import com.caipiao.live.common.constant.LotteryResultStatus;
import com.caipiao.live.common.mybatis.entity.TxffcLotterySg;
import com.caipiao.live.common.mybatis.entity.TxffcLotterySgExample;
import com.caipiao.live.common.mybatis.mapper.TxffcLotterySgMapper;
import com.caipiao.live.common.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * @author ShaoMing
 * @version 1.0.0
 * @date 2019/1/15 15:19
 */
@Service
public class TxffcLotterySgWriteServiceImpl implements TxffcLotterySgWriteService {

    @Autowired
    private TxffcLotterySgMapper txffcLotterySgMapper;

    @Override
    public TxffcLotterySg queryNextSg() {
        TxffcLotterySgExample example = new TxffcLotterySgExample();
        TxffcLotterySgExample.Criteria criteria = example.createCriteria();
        criteria.andIdealTimeGreaterThan(DateUtils.formatDate(new Date(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
        criteria.andOpenStatusEqualTo(LotteryResultStatus.WAIT);
        example.setOrderByClause("issue ASC");
        return txffcLotterySgMapper.selectOneByExample(example);
    }

    @Override
    public TxffcLotterySg selectByIssue(String issue) {
        TxffcLotterySgExample example = new TxffcLotterySgExample();
        TxffcLotterySgExample.Criteria criteria = example.createCriteria();
        criteria.andIssueEqualTo(issue);
        return txffcLotterySgMapper.selectOneByExample(example);
    }

}
