package com.bebopze.tdx.quant.common.config.aspect;

import com.bebopze.tdx.quant.common.constant.UpdateTypeEnum;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.service.impl.TopBlockServiceImpl;
import com.sun.management.OperatingSystemMXBean;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.time.LocalDate;


/**
 * - @UpdateAll 拦截器       =>       全量更新 OOM优化  ->  分段执行
 *
 * @author: bebopze
 * @date: 2026/1/24
 */
@Slf4j
@Aspect
@Component
public class UpdateAllAspect {


    @Autowired
    private TopBlockServiceImpl topBlockService;


    @Around("@annotation(com.bebopze.tdx.quant.common.config.anno.UpdateAll)")
    public Object aroundUpdateAllMethod(ProceedingJoinPoint point) throws Throwable {


        Method method = ((MethodSignature) point.getSignature()).getMethod();
        String serviceName = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();


        // 获取方法参数
        Object[] args = point.getArgs();


        // -------------------------------------------------------------------------------------------------------------


        // 物理机内存
        long physicalMemorySize = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class).getTotalMemorySize() / (1024 * 1024 * 1024);
        // JVM内存
        long totalMemory = Runtime.getRuntime().totalMemory() / (1024 * 1024 * 1024);


        // 物理机内存 和 JVM内存 均大于50GB   ->   无需分段，直接执行
        if (physicalMemorySize > 50 && totalMemory > 50) {
            log.info("[{} - {}]     >>>     物理机内存：{} GB , JVM内存：{} GB , 均大于50GB   ->   无需分段，直接执行", serviceName, methodName, physicalMemorySize, totalMemory);
            return point.proceed(args);
        }
        log.info("[{} - {}]     >>>     物理机内存：{} GB , JVM内存：{} GB , 小于50GB   ->   分段执行", serviceName, methodName, physicalMemorySize, totalMemory);


        // -------------------------------------------------------------------------------------------------------------


        // 遍历参数，找到UpdateTypeEnum类型的参数
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof UpdateTypeEnum) {
                Object originalVal = args[i];

                // 修改参数值
                if (originalVal == UpdateTypeEnum.ALL) {

                    // 全量更新 -> 分段执行
                    args[i] = UpdateTypeEnum.ALL_RANGE;
                    log.info("[{} - {}]     >>>     参数 UpdateTypeEnum，已从 [{}] 修改为 [{}]", serviceName, methodName, originalVal, args[i]);

                    // 执行分段方法
                    return execMethodByRange(point, serviceName, methodName, args);
                }
            }
        }


        // 其他类型  ->  无需修改，直接执行
        log.info("[{} - {}]     >>>     参数 UpdateTypeEnum = [{}]，无需修改，直接执行（无需分段）", serviceName, methodName, args);
        return point.proceed(args);
    }


    @SneakyThrows
    private Object execMethodByRange(ProceedingJoinPoint point, String serviceName, String methodName, Object[] args) {
        log.info("分段更新 - start     >>>     [{} - {}]", serviceName, methodName);
        long start = System.currentTimeMillis();


        // -------------------------------------------------------------------------------------------------------------
        LocalDate startDate_0 = LocalDate.of(2019, 1, 1);
        LocalDate now = LocalDate.now();


        LocalDate startDate = startDate_0;
        // 计算当前区间的结束日期（2年或到当前日期）
        LocalDate endDate = startDate.plusYears(2).isAfter(now) ? now : startDate.plusYears(2);


        log.info("分段更新 - start     >>>     [{} - {}] , 区间：{}  ～  {}", serviceName, methodName, startDate_0, now);
        // -------------------------------------------------------------------------------------------------------------


        // 190101 - 210101
        // 210102 - 230101
        // 230102 - 250101
        // 250102 - 270101（260124）
        // 260125 - 260124
        Object result = null;
        while (!startDate.isAfter(now)) {
            log.info("分段更新区间：{}  ～  {}", startDate, endDate);


            // 分段更新 -> Cache初始化
            topBlockService.initCache__range(startDate, endDate);


            // ---------------------------------------------------------------------------------------------------------
            log.info("开始执行分段更新     >>>     [{} - {}] , 区间：{}  ～  {}", serviceName, methodName, startDate, endDate);
            long s_range = System.currentTimeMillis();


            // 执行目标方法，传入修改后的参数
            result = point.proceed(args);


            log.info("完成分段更新     >>>     [{} - {}] , 区间：{}  ～  {} , range_time : {} , totalTime : {}",
                     serviceName, methodName, startDate, endDate, DateTimeUtil.formatNow2Hms(s_range), DateTimeUtil.formatNow2Hms(start));
            // ---------------------------------------------------------------------------------------------------------


            // 更新起始日期：跳到下一个区间的起始点（避免重叠）
            startDate = endDate.plusDays(1); // 关键：从下一天开始新周期
            endDate = startDate.plusYears(2).isAfter(now) ? now : startDate.plusYears(2);
        }


        log.info("分段更新 - suc     >>>     [{} - {}] , 区间：{}  ～  {} , totalTime : {}",
                 serviceName, methodName, startDate_0, endDate, DateTimeUtil.formatNow2Hms(start));

        return result;
    }


}