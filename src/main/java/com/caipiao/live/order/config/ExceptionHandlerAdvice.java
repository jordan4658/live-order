package com.caipiao.live.order.config;

import com.caipiao.live.common.exception.BusinessException;
import com.caipiao.live.common.model.common.ResultInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

@RestControllerAdvice
@Slf4j
public class ExceptionHandlerAdvice {


    /**
     * rest请求全局异常处理
     *
     * @return
     */
    @ExceptionHandler(value = Exception.class)
    public ResultInfo<Object> handle(Exception e, HttpServletRequest request) {
        log.error(request.getRequestURI() + ",全局异常Exception", e);
        //log.error(request.getRequestURI() + ",全局异常Exception", e);
        return ResultInfo.error();
    }

    @ExceptionHandler(value = BusinessException.class)
    public ResultInfo<Object> businessException(BusinessException e, HttpServletRequest request) {
        log.error(request.getRequestURI() + ", BusinessException", e);
        return ResultInfo.getInstance(e.getCode(), e.getMessage());
    }




}
