package com.caipiao.live.order.service.result;

import com.caipiao.live.common.mybatis.entity.AzksLotterySg;

import java.util.List;

public interface AzksLotterySgWriteService {
    /*
    *@Title:
    *@Description:把澳洲快三开奖结果放入缓存
    */
    void cacheIssueResultForAzKs(String issue, String number);

    AzksLotterySg selectByIssue(String issue);

    AzksLotterySg queryNextSg();

    Integer getAzksOpenCountNum();

    List<AzksLotterySg> getAzksAlgorithmData();
}
