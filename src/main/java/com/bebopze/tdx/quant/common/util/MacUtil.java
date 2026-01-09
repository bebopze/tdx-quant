package com.bebopze.tdx.quant.common.util;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * Mac 启动程序
 *
 * @author: bebopze
 * @date: 2025/11/16
 */
@Slf4j
public class MacUtil {


    /**
     * 打开 Chrome 浏览器，访问指定 URL
     *
     * @param appName
     * @param url
     */
    public static void openChrome(String appName, String url) {
        openMacApp(appName, url);
    }


    /**
     * 关闭 Chrome浏览器的 指定URL
     *
     * @param appName
     * @param url
     */
    public static void closeChrome(String appName, String url) {
        closeMacApp(appName, url);
        closeChromeIfNoNonEmptyTabs(appName);
//        closeChromeIfNoUrl(appName, url);
    }


    public static void openMacApp(String appName, String... args) {

        try {
            // 把 "open" "-a" appName 和 所有参数   拼成一条命令
            List<String> cmd = Lists.newArrayList();
            cmd.add("open");     //   /usr/bin/open
            cmd.add("-a");
            cmd.add(appName);
            if (args != null) {
                cmd.addAll(Arrays.asList(args));
            }


            // 打开App
            Process p = Runtime.getRuntime().exec(cmd.toArray(new String[0]));


            // 等待5s
            p.waitFor(5, TimeUnit.SECONDS);
            log.info("---------------------------- 打开App [{}]", appName);


        } catch (Exception e) {
            log.error("openMacApp - fail     >>>     appName : {} , errMsg : {}", appName, e.getMessage(), e);
        }
    }


    public static void openMacApp2(String appName, String... args) {

        try {
            // 把 "open" "-a" appName 和 所有参数   拼成一条命令
            List<String> cmd = Lists.newArrayList();
            cmd.add("open");     //   /usr/bin/open
            cmd.add("-a");
            cmd.add(appName);
            if (args != null) {
                cmd.addAll(Arrays.asList(args));
            }


            // 打开App
            Process start = new ProcessBuilder(cmd).start();


            // 等待5s
            start.waitFor(5, TimeUnit.SECONDS);
            log.info("---------------------------- 打开App [{}]", appName);


        } catch (Exception e) {
            log.error("openMacApp2 - fail     >>>     appName : {} , errMsg : {}", appName, e.getMessage(), e);
        }
    }


    public static void closeMacApp2(String appName, String... args) {

        try {
            // 把 "pkill" "-f" appName 和 所有参数   拼成一条命令
            List<String> cmd = Lists.newArrayList();
            cmd.add("pkill");     //   /usr/bin/pkill
            cmd.add("-f");
            cmd.add(appName);
            if (args != null) {
                cmd.addAll(Arrays.asList(args));
            }


            // 关闭App
            Process p = Runtime.getRuntime().exec(cmd.toArray(new String[0]));

            // 等待5s
            p.waitFor(5, TimeUnit.SECONDS);
            log.info("---------------------------- 关闭App [{}]", appName);


        } catch (Exception e) {
            log.error("closeMacApp - fail     >>>     appName : {} , errMsg : {}", appName, e.getMessage(), e);
        }
    }


    /**
     * 关闭 mac 应用中匹配指定 URL 的 tab（支持部分匹配）
     *
     * @param appName 应用名称，例如 "Google Chrome"
     * @param args    可选参数，args[0] 为要关闭的 URL（或其包含的子串）。若不传 args，则直接 quit 应用。
     */
    public static void closeMacApp(String appName, String... args) {
        try {
            String escapedAppName = escapeForAppleScript(appName == null ? "" : appName);
            String script;
            if (args != null && args.length > 0 && args[0] != null && !args[0].isEmpty()) {
                String target = escapeForAppleScript(args[0]);

                // AppleScript：在所有窗口的所有 tabs 中，若 tab 的 URL 包含 target，则关闭该 tab
                script = ""
                        + "tell application \"" + escapedAppName + "\"\n"
                        + "  repeat with w in windows\n"
                        + "    set tabList to tabs of w\n"
                        + "    repeat with t in tabList\n"
                        + "      try\n"
                        + "        if (URL of t) contains \"" + target + "\" then\n"
                        + "          close t\n"
                        + "        end if\n"
                        + "      end try\n"
                        + "    end repeat\n"
                        + "  end repeat\n"
                        + "end tell";
            } else {
                // 若未提供 URL，则直接退出应用
                script = "tell application \"" + escapedAppName + "\" to quit";
            }

            List<String> cmd = Lists.newArrayList();
            cmd.add("osascript"); // /usr/bin/osascript
            cmd.add("-e");
            cmd.add(script);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            Process p = pb.start();

            // 等待 5s（与 openMacApp 保持一致）
            boolean finished = p.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                log.warn("closeMacApp - osascript did not finish within timeout for app [{}] and args {}", appName, Arrays.toString(args));
            } else {
                int exit = p.exitValue();
                if (exit == 0) {
                    log.info("---------------------------- 关闭匹配 URL 的 tab 处理完成 [{}] , args: {}", appName, Arrays.toString(args));
                } else {
                    String err = readStream(p.getErrorStream());
                    log.error("closeMacApp - osascript exit code {} , appName: {} , args: {} , stderr: {}", exit, appName, Arrays.toString(args), err);
                }
            }

        } catch (Exception e) {
            log.error("closeMacApp - fail     >>>     appName : {} , args : {} , errMsg : {}", appName, Arrays.toString(args), e.getMessage(), e);
        }
    }


    /**
     * 判断 Mac Chrome 浏览器中是否存在包含指定 URL 的 tab（contains 匹配）。
     * 如果存在任意匹配项：直接返回 true（不做任何关闭操作）。
     * 如果一个都不存在：直接关闭 Chrome 应用并返回 false。
     *
     * 返回值含义：
     * - true ：找到至少一个包含 target 的 tab（未退出应用）
     * - false ：未找到任何匹配项，已尝试退出应用（等同于关闭 Chrome）
     *
     * 说明：
     * - 使用 AppleScript 直接在 Chrome 进程内部判断并在没有匹配时执行 quit，
     * 这样可以避免 race condition（尽量在 AppleScript 里一次性完成判断与退出）。
     *
     * @param appName       Chrome 应用名，例如 "Google Chrome"
     * @param targetUrlPart 要查找的 URL 子串（支持部分匹配）
     * @return boolean 如上说明
     */
    public static boolean closeChromeIfNoUrl(String appName, String targetUrlPart) {
        if (appName == null) appName = "";
        if (targetUrlPart == null) targetUrlPart = "";

        try {
            String escapedAppName = escapeForAppleScript(appName);
            String target = escapeForAppleScript(targetUrlPart);

            // AppleScript：检查所有 window 的 tabs，若存在包含 target 的 URL 则返回 "FOUND"，否则 quit 并返回 "NOT_FOUND"
            String script =
                    "tell application \"" + escapedAppName + "\"\n"
                            + "  set foundFlag to false\n"
                            + "  repeat with w in windows\n"
                            + "    repeat with t in tabs of w\n"
                            + "      try\n"
                            + "        if (URL of t) contains \"" + target + "\" then\n"
                            + "          set foundFlag to true\n"
                            + "          exit repeat\n"
                            + "        end if\n"
                            + "      end try\n"
                            + "    end repeat\n"
                            + "    if foundFlag then exit repeat\n"
                            + "  end repeat\n"
                            + "  if foundFlag then\n"
                            + "    return \"FOUND\"\n"
                            + "  else\n"
                            + "    quit\n"
                            + "    return \"NOT_FOUND\"\n"
                            + "  end if\n"
                            + "end tell";

            List<String> cmd = Lists.newArrayList();
            cmd.add("osascript");
            cmd.add("-e");
            cmd.add(script);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            Process p = pb.start();

            boolean finished = p.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                // 超时：强制销毁并尝试后续处理（视为未找到并尝试退出）
                p.destroyForcibly();
                log.warn("closeChromeIfNoUrl - osascript did not finish within timeout for app [{}] and target [{}]", appName, targetUrlPart);
                return false;
            }

            int exit = p.exitValue();
            String stdout = readStream(p.getInputStream()).trim();
            String stderr = readStream(p.getErrorStream()).trim();

            if (exit != 0) {
                log.error("closeChromeIfNoUrl - osascript exit {} , app: {} , target: {} , stderr: {}", exit, appName, targetUrlPart, stderr);
                // 出错的情况下，保守处理：认为没有匹配并尝试退出（或让调用者决定）
                return false;
            }

            if ("FOUND".equalsIgnoreCase(stdout)) {
                log.info("closeChromeIfNoUrl - found matching tab for app [{}] and target [{}], keep app running.", appName, targetUrlPart);
                return true;
            } else if ("NOT_FOUND".equalsIgnoreCase(stdout) || stdout.isEmpty()) {
                // AppleScript 返回 NOT_FOUND 或无输出（某些环境可能无输出）都按未找到处理
                log.info("closeChromeIfNoUrl - no matching tab found for app [{}] and target [{}], app was (or will be) quit.", appName, targetUrlPart);
                return false;
            } else {
                // 任何其他输出，记录并按未找到处理
                log.warn("closeChromeIfNoUrl - unexpected osascript output: [{}] for app [{}] target [{}]. Treat as NOT_FOUND.", stdout, appName, targetUrlPart);
                return false;
            }

        } catch (Exception e) {
            log.error("closeChromeIfNoUrl - fail >>> appName: {}, target: {} , errMsg: {}", appName, targetUrlPart, e.getMessage(), e);
            return false;
        }
    }


    // ======================================================================
    // 新版函数：严格按照 “地址栏字符串为空视为空标签” 的标准来判定
    // - AppleScript 层面可能返回 chrome://newtab 等内部 URL，Java 层将这些内部 URL 视为“空标签”
    // - 我们在 Java 端维护一组 "empty indicators"，可自由扩展
    // ======================================================================

    /**
     * 判断 Mac Chrome 浏览器是否存在任意“地址栏非空”的标签页（按你的标准）
     * 规则：
     * - 如果任一 tab 的 URL（AppleScript 返回的字符串）不是 missing/空，并且不匹配内部新标签页标识（黑名单），则视为“地址栏非空”
     * - 若没有任何这样的 tab，则关闭 Chrome 程序
     *
     * @param appName 应用名（例如 "Google Chrome"）
     */
    public static void closeChromeIfNoNonEmptyTabs(String appName) {
        if (appName == null || appName.trim().isEmpty()) {
            log.warn("closeChromeIfNoNonEmptyTabs - appName is empty, skip");
            return;
        }

        try {
            String escapedAppName = escapeForAppleScript(appName);

            // AppleScript：收集所有 tabs 的 URL，并以特殊分隔符返回（尽量保证能拿到字符串）
            // 如果获取 URL 出错，返回空字符串表示该 tab 的 URL 视为 ""
            String script =
                    "tell application \"" + escapedAppName + "\"\n" +
                            "  if not running then return \"\"\n" +
                            "  set outStr to \"\"\n" +
                            "  repeat with w in windows\n" +
                            "    repeat with t in tabs of w\n" +
                            "      try\n" +
                            "        set u to (URL of t) as string\n" +
                            "      on error\n" +
                            "        set u to \"\"\n" +
                            "      end try\n" +
                            "      set outStr to outStr & u & \"||SEP||\"\n" +
                            "    end repeat\n" +
                            "  end repeat\n" +
                            "  return outStr\n" +
                            "end tell";

            String raw = execOsascriptWithResult(script);
            if (raw == null) raw = "";
            raw = raw.trim();

            if (raw.isEmpty()) {
                // 没有任何 tab 信息（包括未运行），按无有效 tab 处理：直接退出
                log.info("closeChromeIfNoNonEmptyTabs - no tab info returned, will attempt to quit [{}]", appName);
                execOsascript("tell application \"" + escapedAppName + "\" to quit");
                return;
            }

            // 分割并检查每个 URL
            String[] parts = raw.split("\\|\\|SEP\\|\\|", -1);
            int totalTabs = 0;
            int nonEmptyCount = 0;
            StringBuilder debugUrls = new StringBuilder();

            for (String p : parts) {
                // 最后一个 split 可能是空串 from trailing separator -> 忽略
                if (p == null) continue;
                String url = p.trim();
                if (url.isEmpty()) {
                    // 真实的空字符串 -> 视为空标签
                    debugUrls.append("[EMPTY]").append(",");
                    totalTabs++;
                    continue;
                }
                totalTabs++;
                boolean isEmptyTab = isEmptyTabUrl(url);
                if (isEmptyTab) {
                    debugUrls.append("[").append(url).append(" ->EMPTY]").append(",");
                } else {
                    debugUrls.append("[").append(url).append(" ->NON_EMPTY]").append(",");
                    nonEmptyCount++;
                }
            }

            log.info("closeChromeIfNoNonEmptyTabs - found totalTabs={} , nonEmptyCount={} , urls={}", totalTabs, nonEmptyCount, debugUrls.toString());

            if (nonEmptyCount > 0) {
                // 存在任一地址栏非空的 tab，保持 Chrome 运行
                log.info("closeChromeIfNoNonEmptyTabs - Chrome 存在地址栏非空的标签页: {} 个，保持运行", nonEmptyCount);
                return;
            }

            // 未发现任何地址栏非空的 tab -> 关闭 Chrome
            execOsascript("tell application \"" + escapedAppName + "\" to quit");
            log.info("closeChromeIfNoNonEmptyTabs - 未发现任何地址栏非空的标签页，已关闭应用 [{}]", appName);

        } catch (Exception e) {
            log.error("closeChromeIfNoNonEmptyTabs - fail >>> appName: {} , errMsg: {}", appName, e.getMessage(), e);
        }
    }


    /**
     * 判断某个 tab 的 URL 是否应被视为“空标签”的标识
     * - 你定义的“空标签”是用户点击 + 号 后的新标签页（地址栏对用户是空的）。
     * - 在实际 AppleScript/Chrome 层面，这类标签常常表现为内部 URL，比如 chrome://newtab、chrome-search://local-ntp、about:blank 等。
     * - 这里列出一组常见的内部/新标签页的前缀或精确值，满足你的视觉判定。
     *
     * 如果需要调整（添加/删除某些前缀），直接修改这里的数组。
     */
    private static boolean isEmptyTabUrl(String url) {
        if (url == null) return true;
        String u = url.trim().toLowerCase();
        if (u.isEmpty()) return true;

        // 常见 exact 值
        String[] exacts = new String[]{
                "about:blank",
                "about:newtab"
        };
        for (String e : exacts) {
            if (u.equals(e)) return true;
        }

        // 常见前缀（只要以这些前缀开头就视为新标签页/内部页）
        String[] prefixes = new String[]{
                "chrome://newtab",
                "chrome-search://",
                "chrome://",
                "edge://newtab",
                "edge://",
                "ms-browser-extension://",
                "about:",    // 包含 about:blank / about:newtab 等
                // "file:",
                "data:",
                // "view-source:",
                // "chrome-extension:",
                // "extension:",
                // "devtools://"
        };
        for (String p : prefixes) {
            if (u.startsWith(p)) return true;
        }

        // 不是上述内部/新标签页标识，视为“地址栏非空”
        return false;
    }


    // ======================================================================
    // 新增函数：判断 Chrome 是否存在任意标签页（严格：仅计数 http(s) 开头的 URL）
    // 若存在任意一个 http(s) 标签页则直接 return；若没有任何这样的标签页则关闭 Chrome 程序
    // ======================================================================

    /**
     * 判断 Mac Chrome 浏览器是否存在任意标签页（严格：标签 URL 必须以 http:// 或 https:// 开头）
     * 如果存在则直接 return；如果没有任何满足条件的标签页则关闭 Chrome 程序
     *
     * @param appName 应用名（例如 "Google Chrome"）。不能为空。
     */
    public static void closeChromeIfNoNonEmptyTabs_2(String appName) {
        if (appName == null || appName.trim().isEmpty()) {
            log.warn("closeChromeIfNoNonEmptyTabs - appName is empty, skip");
            return;
        }

        try {
            String escapedAppName = escapeForAppleScript(appName);

            /*
             AppleScript 思路（增强）：
             1. 如果应用未运行，返回 "0"
             2. 遍历所有 window 的 tabs，读取 URL（用 try 捕获可能的异常）
             3. 仅当 URL 不为 missing value、非空字符串且以 "http://" 或 "https://" 开头时计数 +1
             4. 返回计数（字符串形式）
             */
            String script =
                    "tell application \"" + escapedAppName + "\"\n" +
                            "  if not running then return \"0\"\n" +
                            "  set cnt to 0\n" +
                            "  repeat with w in windows\n" +
                            "    repeat with t in tabs of w\n" +
                            "      try\n" +
                            "        set u to URL of t\n" +
                            "        if u is not missing value and u is not \"\" and ((u starts with \"http://\") or (u starts with \"https://\")) then\n" +
                            "          set cnt to cnt + 1\n" +
                            "        end if\n" +
                            "      end try\n" +
                            "    end repeat\n" +
                            "  end repeat\n" +
                            "  return cnt as string\n" +
                            "end tell";

            String result = execOsascriptWithResult(script).trim();
            if (result.isEmpty()) {
                result = "0";
            }

            int tabCount = parseIntSafe(result, 0);

            if (tabCount > 0) {
                log.info("closeChromeIfNoNonEmptyTabs - Chrome 存在以 http(s) 开头的标签页: {} 个，保持运行", tabCount);
                return;
            }

            // 没有任何符合条件的 tab（即没有 http/https 页面），关闭 Chrome
            String quitScript = "tell application \"" + escapedAppName + "\" to quit";
            execOsascript(quitScript);
            log.info("closeChromeIfNoNonEmptyTabs - 未发现任何以 http(s) 开头的标签页，已关闭应用 [{}]", appName);

        } catch (Exception e) {
            log.error("closeChromeIfNoNonEmptyTabs - fail >>> appName: {} , errMsg: {}", appName, e.getMessage(), e);
        }
    }


    // ======================================================================
    // 工具方法
    // ======================================================================

    private static void execOsascript(String script) throws Exception {
        List<String> cmd = Lists.newArrayList("osascript", "-e", script);
        Process p = new ProcessBuilder(cmd).start();
        // 等待最多 5 秒
        boolean finished = p.waitFor(5, TimeUnit.SECONDS);
        if (!finished) {
            p.destroyForcibly();
            log.warn("execOsascript - osascript did not finish within timeout for script: {}", script);
        } else {
            int exit = p.exitValue();
            if (exit != 0) {
                String err = readStream(p.getErrorStream());
                log.warn("execOsascript - osascript exit {} , stderr: {}", exit, err);
            }
        }
    }

    private static String execOsascriptWithResult(String script) throws Exception {
        List<String> cmd = Lists.newArrayList("osascript", "-e", script);
        Process p = new ProcessBuilder(cmd).start();
        // 等待最多 5 秒
        boolean finished = p.waitFor(5, TimeUnit.SECONDS);
        if (!finished) {
            p.destroyForcibly();
            log.warn("execOsascriptWithResult - osascript did not finish within timeout for script: {}", script);
            return "";
        }
        int exit = p.exitValue();
        if (exit != 0) {
            String err = readStream(p.getErrorStream());
            log.warn("execOsascriptWithResult - osascript exit {} , stderr: {}", exit, err);
            // 仍尝试读取标准输出 (可能为空)
        }
        return readStream(p.getInputStream());
    }


    private static int parseIntSafe(String s, int defaultVal) {
        if (s == null) return defaultVal;
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return defaultVal;
        }
    }


    /**
     * 简单转义用于 AppleScript 的字符串字面量（转义反斜杠和双引号）
     */
    private static String escapeForAppleScript(String s) {
        if (s == null) return "";
        // 先转义反斜杠，再转义双引号
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * 兼容 Java8 的 InputStream -> String 读取工具
     */
    private static String readStream(InputStream is) {
        if (is == null) return "";
        ByteArrayOutputStream baos = null;
        try {
            baos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
            return baos.toString(StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            log.warn("readStream fail: {}", e.getMessage());
            return "";
        } finally {
            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException ignored) {
                }
            }
            try {
                is.close();
            } catch (IOException ignored) {
            }
        }
    }


}