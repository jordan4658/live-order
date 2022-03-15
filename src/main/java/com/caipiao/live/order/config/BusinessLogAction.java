package com.caipiao.live.order.config;

import com.caipiao.live.common.util.BaseUtil;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;

/**
 * @Author: admin
 * @Description:aop处理
 * @Version: 1.0.0
 * @Date; 2017-10-31 10:58
 */
@Aspect
@Component
public class BusinessLogAction {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * 切业务层记录报错信息
     * 配置接入点:第一个”*“符号表示返回值的类型任意,包名后面的”..“表示当前包及子包,
     * 第二个”*“表示类名，*即所有类,.*(..)表示任何方法名，括号表示参数，两个点表示任何参数类型
     */
    @Pointcut("execution(* com.caipiao.live.order.service..*.*(..))")
    private void serviceAspect(){}

    @Before("serviceAspect()")
    public void before(JoinPoint joinPoint) {
        try {
            // 拦截的方法名称。当前正在执行的方法
            String methodName = joinPoint.getSignature().getName();
            ServletRequestAttributes servletRequestAttributes = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes());
            String url = null;
            String remoteAddr = null;
            if (null != servletRequestAttributes) {
                HttpServletRequest request = servletRequestAttributes.getRequest();
                url = request.getRequestURL().toString();
                remoteAddr = request.getRemoteAddr();
            }
            String localAddress = InetAddress.getLocalHost().getHostAddress();
            log.info("order-server execute method:{}, url:{}, remoteAddr:{}, localAddress:{}", methodName, url, remoteAddr, localAddress);
        } catch (Exception e) {
            log.error("serviceAspect occur error:{}", e);
        }
    }


    /**
     * @Author: admin
     * @Description:记录业务层异常报错信息
     * @Version: 1.0.0
     * @Date; 2017/11/1 9:29
     * @param: [joinPoint, ex]
     * @return: void
     */
    @AfterThrowing(pointcut = "serviceAspect()", throwing = "ex")
    public void doAfterThrowing(JoinPoint joinPoint, Throwable ex) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        ServletRequestAttributes servletAttributes = (ServletRequestAttributes) requestAttributes;
        if (null == servletAttributes) {
            return;
        }
        HttpServletRequest request = servletAttributes.getRequest();
        if (null != request) {
            //日志输出记录
            log.error("ip地址={}，请求方式={}，请求地址={}，参数:{}", BaseUtil.getUserIp(request), request.getMethod(), request.getRequestURL(), request.getAttribute("jsonParam"));
            log.error("调用{}类的{}方法出现了异常，异常通知为:{}", joinPoint.getTarget().getClass().getName(), joinPoint.getSignature().getName(), ex.getMessage(), ex);
        }
    }

}
