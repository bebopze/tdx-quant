package com.bebopze.tdx.quant.task;

import com.bebopze.tdx.quant.common.config.ThreadPoolRegistry;
import com.bebopze.tdx.quant.common.config.anno.DistributedLock;
import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.sun.management.OperatingSystemMXBean;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.software.os.OperatingSystem;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.List;


/**
 * 内存监控 -> GC
 *
 * @author: bebopze
 * @date: 2026/11/20
 */
@Slf4j
@Service
public class MemoryMonitorTask {


    /**
     * 复用 SystemInfo 实例可优化缓存和性能
     */
    private static final SystemInfo systemInfo = new SystemInfo();


    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;


    /**
     * 每 5 分钟执行一次
     *
     *
     * 内存监控 -> GC
     */
    @TotalTime
    @DistributedLock
    @Scheduled(cron = "0 */5 * * * *")
    public void memMonitorAndGc() {
        try {
            doMonitor();
        } catch (Exception e) {
            // 监控任务自身异常不应导致调度中断
            log.error("MemoryMonitorTask - error", e);
        }
    }


    private void doMonitor() {


        // ---------- 1.1 采集 系统 CPU使用率 ----------
        CentralProcessor processor = systemInfo.getHardware().getProcessor();
        // 采集 1 秒内的 CPU 负载（会阻塞 1 秒，可接受）
        double os__cpuLoad = processor.getSystemCpuLoad(1000L) * 100;


        // ---------- 1.2 采集 Java CPU使用率 ----------
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        // Java进程 CPU 负载
        double java__cpuLoad = osBean.getProcessCpuLoad() * 100;
        //     系统 CPU 负载
        double os__cpuLoad_2 = osBean.getCpuLoad() * 100;


        // ---------- 2.1 采集 系统 内存使用率 ----------
        GlobalMemory memory = systemInfo.getHardware().getMemory();
        long totalMemory = memory.getTotal();
        long availableMemory = memory.getAvailable();
        double os__memUsePct = (double) (totalMemory - availableMemory) / totalMemory * 100;


        // ---------- 2.2 采集 Java（tdx-quant） 堆内存使用率 ----------
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();

        long heapUsed = heap.getUsed(); // 已用堆，单位字节
        long heapMax = heap.getMax();   // 堆上限，单位字节

        // heapMax > 0` 用于处理堆上限未定义的情况，此时 getMax() 返回 -1
        double java__memUsePct = heapMax > 0 ? (double) heapUsed / heapMax * 100 : (double) heapUsed / totalMemory * 100;


        // ---------- 3. 判断是否有 "Java 任务" 在执行 ----------
        boolean javaTaskBusy = isJavaTaskBusy();


        // ---------- 4. 获取正在运行的 Java 进程详情 ----------
        List<String> javaProcesses = listRunningJavaProcesses();


        // ---------- 5. 打印详情（可根据需要改为日志） ----------
        String memInfo = String.format("""
                                               [MemoryGuard] 系统CPU: %.2f%% | Java CPU: %.2f%% | OS内存: %.2f%% (%.2f GB / %.2f GB) | Java内存: %.2f%% (%.2f GB / %.2f GB) | Java任务繁忙: %s
                                               运行中的 Java 进程: %s
                                               """,

                                       os__cpuLoad,
                                       java__cpuLoad,

                                       os__memUsePct,
                                       (double) (totalMemory - availableMemory) / 1024 / 1024 / 1024,
                                       (double) totalMemory / 1024 / 1024 / 1024,

                                       java__memUsePct,
                                       (double) heapUsed / 1024 / 1024 / 1024,
                                       (double) heapMax / 1024 / 1024 / 1024,

                                       javaTaskBusy,
                                       javaProcesses
        );
        log.info(memInfo);


        // ---------- 6. 核心判断与 GC ----------
        boolean noJavaTaskRunning = !javaTaskBusy;
        boolean cpuLow = java__cpuLoad < 10.0;
        boolean memoryHigh = java__memUsePct > 90.0;

        if (noJavaTaskRunning && cpuLow && memoryHigh) {
            log.info("[MemoryGuard] 条件满足  ->  执行 System.gc()");
            System.gc();
        }
    }


//    /**
//     * 判断当前是否有 "Java 任务" 在执行。
//     * 这里给出两种可选策略，实际项目请根据 "任务" 的定义选择。
//     */
//    private boolean isJavaTaskBusy() {
//        // 策略 A（推荐）：检查业务线程池的活跃线程数
//        // 如果项目中有专门处理业务的 ThreadPoolExecutor Bean，注入后判断：
//        //   return threadPoolExecutor.getActiveCount() > 0;
//        // 来源：getActiveCount() 返回正在执行任务的线程数[reference:5][reference:6]
//
//        // 策略 B（兜底）：检查当前 JVM 中非守护、非空闲线程的数量
//        // 排除 JVM 自身后台线程和本监控线程，粗略判断是否存在业务线程
//        long busyThreads = Thread.getAllStackTraces().keySet().stream()
//                                 .filter(t -> t.isAlive() && !t.isDaemon())
//                                 .filter(t -> !t.getName().startsWith("monitor-sched-")) // 排除本任务线程
//                                 .filter(t -> !t.getName().startsWith("DestroyJavaVM"))
//                                 .count();
//        // 如果只有 main 线程和少量框架线程，视为空闲
//        // 这个阈值需要根据项目实际线程情况调整
//        return busyThreads > 5;
//    }


    private boolean isJavaTaskBusy() {
        if (taskExecutor.getActiveCount() > 0 || taskExecutor.getQueueSize() > 0) {
            return true;
        }

        return ThreadPoolRegistry.getPoolStats().entrySet().stream()
                                 .filter(e -> e.getKey().endsWith(".ActiveCount")
                                         || e.getKey().endsWith(".QueueSize"))
                                 .anyMatch(e -> Integer.parseInt(e.getValue()) > 0);
    }


    /**
     * 列出当前操作系统上正在运行的 Java 进程详情。
     */
    private List<String> listRunningJavaProcesses() {
        OperatingSystem os = systemInfo.getOperatingSystem();
        return os.getProcesses().stream()
                 .filter(p -> p.getCommandLine() != null)
                 .filter(p -> {
                     String cmd = p.getCommandLine().toLowerCase();
                     return cmd.contains("java") || cmd.contains("spring-boot");
                 })
                 .map(p -> String.format("PID=%d, CPU=%.2f%%, Mem=%dMB, Cmd=%s",
                                         p.getProcessID(),
                                         p.getProcessCpuLoadCumulative() * 100, // 或使用 getProcessCpuLoadBetweenTicks
                                         p.getResidentSetSize() / 1024 / 1024,
                                         truncate(p.getCommandLine(), 80)))
                 .toList();


        // OSHI 的 OperatingSystem.getProcesses() 可获取进程快照
        // ProcessHandle.allProcesses() 也是 JDK 9+ 的可选方案
    }


    private String truncate(String s, int max) {
        return s == null ? "" : s.length() <= max ? s : s.substring(0, max) + "...";
    }


}