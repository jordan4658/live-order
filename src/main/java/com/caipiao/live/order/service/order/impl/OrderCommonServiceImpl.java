package com.caipiao.live.order.service.order.impl;

import com.caipiao.live.common.service.sys.SysParamService;
import com.caipiao.live.common.enums.SysParameterEnum;
import com.caipiao.live.common.mybatis.entity.SysParameter;
import com.caipiao.live.order.service.order.OrderCommonService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;

@Service
public class OrderCommonServiceImpl implements OrderCommonService {

    @Resource
    private SysParamService sysParamService;

    @Override
    public BigDecimal calcNoWithdrawalAmount(BigDecimal noWithdrawalAmount) {
        if (null == noWithdrawalAmount) {
            return BigDecimal.ZERO;
        }
        //计算有效投注额度
        SysParameter sysParameter = sysParamService.getByCode(SysParameterEnum.WITHDRAWAL_AMOUNT);
        if (null != sysParameter) {
            noWithdrawalAmount = noWithdrawalAmount.multiply(new BigDecimal(sysParameter.getParamValue()));
        }
        return noWithdrawalAmount;
    }
}
