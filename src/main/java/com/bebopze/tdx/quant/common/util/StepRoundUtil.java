package com.bebopze.tdx.quant.common.util;


/**
 * 步数工具类
 *
 * @author: bebopze
 * @date: 2025/12/17
 */
public class StepRoundUtil {


    /**
     * 将一个 double 值向上取整到指定步长的倍数
     *
     * @param val  要取整的值
     * @param step 步长 N
     * @return 向上取整后的值
     * @throws IllegalArgumentException 如果 step 为 0
     */
    public static double ceilToStep(double val, int step) {
        if (step == 0) {
            throw new IllegalArgumentException("Step cannot be zero.");
        }
        if (step < 0) {
            // 如果允许负步长，可以处理；如果不允许，则抛出异常或取绝对值
            // 这里按绝对值处理，因为步长通常指大小
            step = Math.abs(step);
        }
        // 1. 将值除以步长
        // 2. 对结果进行 Math.ceil 向上取整
        // 3. 将结果乘以步长
        // 注意：对于负数，Math.ceil 的行为是向 0 方向取整，这符合“向上取整到更大倍数”的逻辑
        // 例如，val=-7, step=5 -> (-7)/5 = -1.4 -> ceil(-1.4) = -1 -> -1 * 5 = -5
        // -5 确实是大于 -7 的最小步长倍数。
        double steps = Math.ceil(val / step);
        return steps * step;
    }


    /**
     * 将一个 double 值向下取整到指定步长的倍数
     *
     * @param val  要取整的值
     * @param step 步长 N
     * @return 向下取整后的值
     * @throws IllegalArgumentException 如果 step 为 0
     */
    public static double floorToStep(double val, int step) {
        if (step == 0) {
            throw new IllegalArgumentException("Step cannot be zero.");
        }
        if (step < 0) {
            // 同上，处理负步长
            step = Math.abs(step);
        }
        // 1. 将值除以步长
        // 2. 对结果进行 Math.floor 向下取整
        // 3. 将结果乘以步长
        // 注意：对于负数，Math.floor 的行为是向 -∞ 方向取整
        // 例如，val=-7, step=5 -> (-7)/5 = -1.4 -> floor(-1.4) = -2 -> -2 * 5 = -10
        // -10 确实是小于 -7 的最大步长倍数。
        double steps = Math.floor(val / step);
        return steps * step;
    }


    // -----------------------------------------------------------------------------------------------------------------


    // 示例和测试
    public static void main(String[] args) {

        double val = 3.5;
        int step = 5;

        System.out.printf("Value: %.1f, Step: %d%n", val, step);
        System.out.println("Ceil to step: " + ceilToStep(val, step));   // 输出: 5.0
        System.out.println("Floor to step: " + floorToStep(val, step)); // 输出: 0.0


        System.out.println("---");


        val = -3.5;
        System.out.printf("Value: %.1f, Step: %d%n", val, step);
        System.out.println("Ceil to step: " + ceilToStep(val, step));   // 输出: 0.0 (比-3.5大的最小步长倍数是0)
        System.out.println("Floor to step: " + floorToStep(val, step)); // 输出: -5.0 (比-3.5小的最大步长倍数是-5)


        System.out.println("---");


        val = 12.1;
        step = 10;
        System.out.printf("Value: %.1f, Step: %d%n", val, step);
        System.out.println("Ceil to step: " + ceilToStep(val, step));   // 输出: 20.0
        System.out.println("Floor to step: " + floorToStep(val, step)); // 输出: 10.0


        System.out.println("---");


        val = 10.0;
        System.out.printf("Value: %.1f, Step: %d%n", val, step);
        System.out.println("Ceil to step: " + ceilToStep(val, step));   // 输出: 10.0 (本身就是倍数)
        System.out.println("Floor to step: " + floorToStep(val, step)); // 输出: 10.0 (本身就是倍数)
    }


}