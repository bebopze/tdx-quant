package com.bebopze.tdx.quant.common.util;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;


/**
 * 机器码工具类
 *
 * @author: bebopze
 * @date: 2026/1/13
 */
@Slf4j
public class MachineUtil {


    /**
     * 生成本机唯一标识
     * 格式：MAC地址_主机名_IP地址_进程ID_时间戳
     * 用途：每次本机（JVM）重启时，可以清理本机遗留的分布式锁
     */
    @SneakyThrows
    public static String generateMachineUniqueId() {
        // 1. 获取MAC地址
        String macAddress = getMacAddress();

        // 2. 获取主机名
        String hostName = InetAddress.getLocalHost().getHostName();

        // 3. 获取IP地址
        String ipAddress = InetAddress.getLocalHost().getHostAddress();

//        // 4. 获取进程ID
//        String processId = getProcessId();
//
//        // 5. 获取当前时间戳（毫秒）
//        long timestamp = System.currentTimeMillis();


        // 组合成唯一标识，使用下划线分隔，去掉特殊字符
        String str = String.format("%s_%s_%s",
                                   macAddress.replace(":", "").replace("-", "").toLowerCase(),
                                   hostName.replaceAll("[^a-zA-Z0-9]", ""),
                                   ipAddress.replace(".", ""));

        return str.substring(0, Math.min(64, str.length()));
    }


    /**
     * 获取MAC地址
     */
    private static String getMacAddress() throws SocketException {
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();
            if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                continue;
            }
            byte[] mac = networkInterface.getHardwareAddress();
            if (mac != null && mac.length > 0) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < mac.length; i++) {
                    sb.append(String.format("%02X", mac[i]));
                    if (i < mac.length - 1) {
                        sb.append(":");
                    }
                }
                return sb.toString();
            }
        }
        return "00:00:00:00:00:00"; // 默认MAC地址
    }

    /**
     * 获取进程ID
     */
    private static String getProcessId() {
        try {
            // 方法1：通过RuntimeMXBean获取
            String name = ManagementFactory.getRuntimeMXBean().getName();
            if (name != null && name.contains("@")) {
                return name.split("@")[0];
            }

            // 方法2：通过系统属性获取
            String pidProperty = System.getProperty("pid");
            if (pidProperty != null && !pidProperty.isEmpty()) {
                return pidProperty;
            }

            // 方法3：通过反射获取（适用于不同JVM）
            try {
                Class<?> processHandleClass = Class.forName("java.lang.ProcessHandle");
                Object currentProcess = processHandleClass.getMethod("current").invoke(null);
                return processHandleClass.getMethod("pid").invoke(currentProcess).toString();
            } catch (Exception e) {
                // Java 8 or reflection failed
            }


            // 备用方案
            return String.valueOf(Thread.currentThread().threadId());

        } catch (Exception e) {
            log.warn("获取进程ID失败，使用线程ID作为备用", e);
            return String.valueOf(Thread.currentThread().threadId());
        }
    }


    // -----------------------------------------------------------------------------------------------------------------


    public static void main(String[] args) {
        System.out.println(generateMachineUniqueId());
        System.out.println(generateMachineUniqueId());
        System.out.println(generateMachineUniqueId());
    }

}