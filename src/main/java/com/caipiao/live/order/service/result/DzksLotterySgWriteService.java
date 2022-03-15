package com.caipiao.live.order.service.result;

import com.caipiao.live.common.mybatis.entity.DzksLotterySg;

import java.util.List;

/**
 * @Date:Created in 19:592019/12/11
 * @Descriotion
 * @Author
 **/
public interface DzksLotterySgWriteService {
    /*
     *@Title:
     *@Description:把澳洲快三开奖结果放入缓存
     */
    void cacheIssueResultForDzKs(String issue, String number);

    DzksLotterySg selectByIssue(String issue);

    DzksLotterySg queryNextSg();

    Integer getDzksOpenCountNum();

    List<DzksLotterySg> getDzksAlgorithmData();

}
