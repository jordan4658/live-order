package com.caipiao.live.order.service.order;

import java.math.BigDecimal;

public interface OrderCommonService {
    /**
     * 计算实际操作的不可提额度：额度 * 系统设置的倍数
     *
     * @param noWithdrawalAmount
     * @return
     */
    BigDecimal calcNoWithdrawalAmount(BigDecimal noWithdrawalAmount);
}
