package com.caipiao.live.order.service.result.impl;

import com.caipiao.live.order.service.result.CqsscLotterySgWriteService;
import com.caipiao.live.common.mybatis.mapper.CqsscLotterySgMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 重庆时时彩赛果
 *
 * @author lzy
 * @create 2018-07-27 11:03
 **/
@Service
public class CqsscLotterySgWriteServiceImpl implements CqsscLotterySgWriteService {

    @Autowired
    private CqsscLotterySgMapper cqsscLotterySgMapper;

//    @Override
//    public void addSg() {
//        String lotteryName = "cqssc";
//        List<LotterySgModel> sgModels = GetHttpInterface.getCpkSg(lotteryName, 1);
//        if (sgModels != null && sgModels.size() > 0) {
//            LotterySgModel sgModel = sgModels.get(0);
//
//            // 根据期号查询该数据在自己数据库中是否已入库
//            CqsscLotterySgExample example = new CqsscLotterySgExample();
//            CqsscLotterySgExample.Criteria criteria = example.createCriteria();
//            criteria.andIssueEqualTo(sgModel.getIssue());
//            CqsscLotterySg cqsscLotterySg = cqsscLotterySgMapper.selectOneByExample(example);
//            // 如果没入库,则入库
//            if (cqsscLotterySg == null) {
//                cqsscLotterySg = new CqsscLotterySg();
//                cqsscLotterySg.setIssue(sgModel.getIssue());
//                cqsscLotterySg.setDate(TimeHelper.date("yyyy-MM-dd"));
//                cqsscLotterySg.setTime(TimeHelper.date());
//                String sg = sgModel.getSg();
//                String[] numbers = sg.split(",");
//                cqsscLotterySg.setWan(Integer.valueOf(numbers[0]));
//                cqsscLotterySg.setQian(Integer.valueOf(numbers[1]));
//                cqsscLotterySg.setBai(Integer.valueOf(numbers[2]));
//                cqsscLotterySg.setShi(Integer.valueOf(numbers[3]));
//                cqsscLotterySg.setGe(Integer.valueOf(numbers[4]));
//                this.cqsscLotterySgMapper.insertSelective(cqsscLotterySg);
//            }
//        }
//    }


//    @Override
//    public CqsscLotterySg queryNextSg() {
//        CqsscLotterySgExample example = new CqsscLotterySgExample();
//        CqsscLotterySgExample.Criteria criteria = example.createCriteria();
//        criteria.andIdealTimeGreaterThan(DateUtils.formatDate(new Date(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
//        example.setOrderByClause("issue ASC");
//        return cqsscLotterySgMapper.selectOneByExample(example);
//    }

//    @Override
//    public CqsscLotterySg selectByIssue(String issue) {
//        CqsscLotterySgExample example = new CqsscLotterySgExample();
//        CqsscLotterySgExample.Criteria criteria = example.createCriteria();
//        criteria.andIssueEqualTo(issue);
//        return cqsscLotterySgMapper.selectOneByExample(example);
//    }

}
