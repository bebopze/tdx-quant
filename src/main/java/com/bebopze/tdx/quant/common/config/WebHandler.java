package com.bebopze.tdx.quant.common.config;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * web层 - 统一拦截
 *
 * @author bebopze
 * @date 2025/5/11
 */
@Slf4j
@Aspect
@Component
public class WebHandler {


    /**
     * 捕获所有 Controller层的方法
     */
    @Around("execution(* com.bebopze.tdx.quant.web.*.*(..))")
    public Object doBefore(ProceedingJoinPoint pjp) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 日志记录
        log(pjp.getArgs());

        // exec
        Object result = pjp.proceed();


        // 统计时间
        long totalTime = System.currentTimeMillis() - startTime;
        log.info("totalTime : {}s", Double.valueOf(totalTime) / 1000);

        return result;
    }


    /**
     * 记录日志
     *
     * @param args
     */
    private void log(Object[] args) {

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        List<Object> argList = Arrays.stream(args)
                .filter(arg -> !(arg instanceof HttpServletRequest) && !(arg instanceof HttpServletResponse))
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(argList)) {
            log.info(String.join(" ", Arrays.asList(request.getServletPath(), getIpAddress(request))));
        } else {
            log.info(String.join(" ", Arrays.asList(request.getServletPath(), getIpAddress(request), JSON.toJSONString(argList.get(0)))));
        }
    }


    /**
     * 获取请求主机IP地址,如果通过代理进来，则透过防火墙获取真实IP地址;
     *
     * @param request
     * @return
     * @throws IOException
     */
    public final static String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("Proxy-Client-IP");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_CLIENT_IP");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_X_FORWARDED_FOR");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
        } else if (ip.length() > 15) {
            String[] ips = ip.split(",");
            for (String strIp : ips) {
                if (!("unknown".equalsIgnoreCase(strIp))) {
                    ip = strIp;
                    break;
                }
            }
        }
        return ip;
    }

}
