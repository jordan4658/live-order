package com.caipiao.live.order.model.dto;


import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;

public class OrderBetRecordResultDTO{

    @ApiModelProperty(value = "投注id")
    private Integer betId;
    @ApiModelProperty(value = "期号")
    private String issue;
    @ApiModelProperty(value = "订单号")
    private String orderSn;
    @ApiModelProperty(value = "彩种类别id")
    private Integer cateId;
    @ApiModelProperty(value = "彩种id")
    private Integer lotteryId;
    @ApiModelProperty(value = "玩法id")
    private Integer playId;
    @ApiModelProperty(value = "玩法配置id")
    private Integer settingId;
    @ApiModelProperty(value = "玩法名称")
    private String playName;
    @ApiModelProperty(value = "投注号码")
    private String betNumber;
    @ApiModelProperty(value = "投注总注数")
    private Integer betCount;
    @ApiModelProperty(value = "投注金额")
    private BigDecimal betAmount;
    @ApiModelProperty(value = "中奖金额")
    private BigDecimal winAmount;
    @ApiModelProperty(value = "返点金额")
    private BigDecimal backAmount;
    @ApiModelProperty(value = "中奖:WIN | 未中奖:NO_WIN | 等待开奖:WAIT | 和:HE | 撤单:BACK")
    private String tbStatus;
    @ApiModelProperty(value = "中奖注数")
    private String winCount;
    @ApiModelProperty(value = "是否推单 0 否 1 是")
    private Integer isPush;
    @ApiModelProperty(value = "直播房间id")
    private Long studioId;

    public Integer getBetId() {
        return betId;
    }

    public void setBetId(Integer betId) {
        this.betId = betId;
    }

    public String getIssue() {
        return issue;
    }

    public void setIssue(String issue) {
        this.issue = issue;
    }

    public String getOrderSn() {
        return orderSn;
    }

    public void setOrderSn(String orderSn) {
        this.orderSn = orderSn;
    }

    public Integer getCateId() {
        return cateId;
    }

    public void setCateId(Integer cateId) {
        this.cateId = cateId;
    }

    public Integer getLotteryId() {
        return lotteryId;
    }

    public void setLotteryId(Integer lotteryId) {
        this.lotteryId = lotteryId;
    }

    public Integer getPlayId() {
        return playId;
    }

    public void setPlayId(Integer playId) {
        this.playId = playId;
    }

    public Integer getSettingId() {
        return settingId;
    }

    public void setSettingId(Integer settingId) {
        this.settingId = settingId;
    }

    public String getPlayName() {
        return playName;
    }

    public void setPlayName(String playName) {
        this.playName = playName;
    }

    public String getBetNumber() {
        return betNumber;
    }

    public void setBetNumber(String betNumber) {
        this.betNumber = betNumber;
    }

    public Integer getBetCount() {
        return betCount;
    }

    public void setBetCount(Integer betCount) {
        this.betCount = betCount;
    }

    public BigDecimal getBetAmount() {
        return betAmount;
    }

    public void setBetAmount(BigDecimal betAmount) {
        this.betAmount = betAmount;
    }

    public BigDecimal getWinAmount() {
        return winAmount;
    }

    public void setWinAmount(BigDecimal winAmount) {
        this.winAmount = winAmount;
    }

    public BigDecimal getBackAmount() {
        return backAmount;
    }

    public void setBackAmount(BigDecimal backAmount) {
        this.backAmount = backAmount;
    }

    public String getTbStatus() {
        return tbStatus;
    }

    public void setTbStatus(String tbStatus) {
        this.tbStatus = tbStatus;
    }

    public String getWinCount() {
        return winCount;
    }

    public void setWinCount(String winCount) {
        this.winCount = winCount;
    }

    public Integer getIsPush() {
        return isPush;
    }

    public void setIsPush(Integer isPush) {
        this.isPush = isPush;
    }

    public Long getStudioId() {
        return studioId;
    }

    public void setStudioId(Long studioId) {
        this.studioId = studioId;
    }
}
