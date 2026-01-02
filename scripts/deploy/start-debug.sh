#!/bin/bash
# ============================================================
# start-debug.sh - Mac/Linux 双系统兼容版本（JAR 查找优先：同目录 -> target -> 默认 IDEA 模式）
# Debug 模式启动（支持 Java 8+）
# 强制 JVM 使用 UTF-8，导出 LANG/LC_ALL（解决远端日志乱码）
# ============================================================

# ==================== 配置 ====================
JAR_NAME="tdx-quant-1.0-SNAPSHOT.jar"
DEBUG_PORT=5005
SERVER_PORT=7001
PROFILE="prod"
STARTUP_TIMEOUT=150
# ==============================================

# 获取脚本所在目录
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)

# 尝试推断可能的项目根（IDEA 下为 SCRIPT_DIR/../..）
POSSIBLE_APP_HOME=$(cd "$SCRIPT_DIR/../.." 2>/dev/null && pwd || echo "")

echo "🔍 开始查找 JAR: $JAR_NAME"
echo "📁 脚本所在目录: $SCRIPT_DIR"
echo "📁 推测的项目根 (possible): ${POSSIBLE_APP_HOME:-<none>}"

# 优先：脚本同目录
echo "1) 检查脚本同目录: $SCRIPT_DIR/$JAR_NAME"
if [ -f "$SCRIPT_DIR/$JAR_NAME" ]; then
    MODE_HINT="deployed-remote"
    APP_HOME="$SCRIPT_DIR"
    JAR_FILE="$SCRIPT_DIR/$JAR_NAME"
    echo "   -> 找到：使用脚本同目录的 JAR（模式: $MODE_HINT）"
else
    echo "   -> 未找到同目录 JAR"
    if [ -n "$POSSIBLE_APP_HOME" ]; then
        echo "2) 检查 IDEA target: $POSSIBLE_APP_HOME/target/$JAR_NAME"
        if [ -f "$POSSIBLE_APP_HOME/target/$JAR_NAME" ]; then
            MODE_HINT="local-IDEA"
            APP_HOME="$POSSIBLE_APP_HOME"
            JAR_FILE="$APP_HOME/target/$JAR_NAME"
            echo "   -> 找到：使用 IDEA target 下的 JAR（模式: $MODE_HINT）"
        else
            echo "   -> 未在 target 中找到 JAR"
            MODE_HINT="local-IDEA-default"
            APP_HOME="$POSSIBLE_APP_HOME"
            JAR_FILE="$APP_HOME/target/$JAR_NAME"
            echo "   -> 未找到任何 JAR，缺省回到 IDEA 模式（将尝试路径: $JAR_FILE）"
        fi
    else
        MODE_HINT="local-IDEA-default"
        APP_HOME="$SCRIPT_DIR/../.."
        JAR_FILE="$APP_HOME/target/$JAR_NAME"
        echo "2) 无法推断 IDEA 项目根，缺省回到 IDEA 模式（将尝试路径: $JAR_FILE）"
    fi
fi

PID_FILE="$APP_HOME/logs/app.pid"
STARTUP_LOG="$APP_HOME/logs/startup.log"

echo "🔎 最终决定 -> 模式: $MODE_HINT"
echo "   APP_HOME = $APP_HOME"
echo "   JAR_FILE = $JAR_FILE"

# 验证 JAR 文件存在
if [ ! -f "$JAR_FILE" ]; then
    echo "❌ JAR文件不存在: $JAR_FILE"
    echo "💡 本地执行请先：mvn clean package -DskipTests（或确认 target/$JAR_NAME 存在）"
    echo "💡 部署执行请确认 deploy.sh 已把 $JAR_NAME 上传到脚本同目录（$SCRIPT_DIR）"
    exit 1
fi

# 创建日志目录
mkdir -p "$APP_HOME/logs"

# --- 强制设置环境 locale（防止 ssh 非交互 shell 无 locale，引发日志编码问题） ---
export LANG="${LANG:-zh_CN.UTF-8}"
export LC_ALL="${LC_ALL:-$LANG}"
echo "🗺️  当前环境 LANG=$LANG LC_ALL=$LC_ALL"

# 智能内存配置 - Mac/Linux 兼容
if [[ "$OSTYPE" == "darwin"* ]]; then
    TOTAL_MEM=$(sysctl -n hw.memsize 2>/dev/null | awk '{printf "%.0f", $1/1024/1024/1024}')
else
    TOTAL_MEM=$(free -g 2>/dev/null | awk '/^Mem:/{print $2}')
fi

if ! [[ "$TOTAL_MEM" =~ ^[0-9]+$ ]] || [ -z "$TOTAL_MEM" ] || [ "$TOTAL_MEM" -lt 1 ]; then
    TOTAL_MEM=4
fi

MAX_HEAP=$(( TOTAL_MEM * 3 / 4 ))
if [ "$MAX_HEAP" -gt 36 ]; then MAX_HEAP=36; fi
if [ "$MAX_HEAP" -lt 1 ]; then MAX_HEAP=1; fi

# 检测 Java 版本，设置调试参数（Java 8 vs Java 9+）
JAVA_RAW_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' || true)
JAVA_MAJOR=$(echo "$JAVA_RAW_VER" | awk -F. '{print $1}')
if [ "$JAVA_MAJOR" = "1" ]; then
    JAVA_MAJOR=$(echo "$JAVA_RAW_VER" | awk -F. '{print $2}')
fi
if [ -z "$JAVA_MAJOR" ]; then
    JAVA_MAJOR=8
fi

if [ "$JAVA_MAJOR" -lt 9 ]; then
    DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=$DEBUG_PORT"
else
    DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:$DEBUG_PORT"
fi

echo "🖥️  启动量化系统 [DEBUG 模式] [$(date)]"
echo "📊 物理内存: ${TOTAL_MEM}GB | 分配堆内存: ${MAX_HEAP}GB"
echo "☕ Java版本: ${JAVA_RAW_VER:-unknown} (major: $JAVA_MAJOR)"
echo "🚀 JAR文件: $JAR_FILE"
echo "🔗 应用地址: http://localhost:$SERVER_PORT"
echo "🐛 Debug端口: $DEBUG_PORT"

# 检查是否已在运行（根据 jar 名称）
EXISTING_PID=$(pgrep -f "$JAR_NAME" 2>/dev/null | head -1 || true)
if [ -n "$EXISTING_PID" ]; then
    echo "⚠️  应用已在运行中 (PID: $EXISTING_PID)"
    echo "💡 如需重启，请先执行: ./stop.sh"
    exit 1
fi

# 切换到 APP_HOME
cd "$APP_HOME" || {
    echo "❌ 无法切换到目录: $APP_HOME"
    exit 1
}

: > "$STARTUP_LOG"

nohup java \
    $DEBUG_OPTS \
    -Dfile.encoding=UTF-8 \
    -Xms8g -Xmx${MAX_HEAP}g \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+AlwaysPreTouch \
    -XX:InitiatingHeapOccupancyPercent=30 \
    -XX:G1ReservePercent=10 \
    -XX:MaxDirectMemorySize=4g \
    -jar "$JAR_FILE" \
    --spring.profiles.active="$PROFILE" \
    --server.port="$SERVER_PORT" \
    >> "$STARTUP_LOG" 2>&1 &

JAVA_PID=$!
disown $JAVA_PID 2>/dev/null || true

echo "⏳ 启动中 (PID: $JAVA_PID)，等待服务就绪..."

for i in $(seq 1 $STARTUP_TIMEOUT); do
    if nc -z localhost $SERVER_PORT 2>/dev/null; then
        echo ""
        echo "✅ 应用启动成功 (PID: $JAVA_PID)"
        echo "📄 启动日志: $STARTUP_LOG"
        echo ""
        echo "🔌 IDEA 远程调试连接:"
        echo "   Host: localhost / 服务器IP"
        echo "   Port: $DEBUG_PORT"
        echo "$JAVA_PID" > "$PID_FILE"
        exit 0
    fi

    if ! kill -0 $JAVA_PID 2>/dev/null; then
        echo ""
        echo "❌ 应用启动失败（进程已退出）"
        echo "📄 最近错误日志:"
        echo "─────────────────────────────────"
        tail -50 "$STARTUP_LOG"
        echo "─────────────────────────────────"
        exit 1
    fi

    if [ $((i % 10)) -eq 0 ]; then
        echo ""
        echo "⏳ 已等待 ${i}s..."
    else
        echo -n "."
    fi
    sleep 1
done

echo ""
echo "⚠️  启动超时（${STARTUP_TIMEOUT}秒）"
echo "📄 请检查日志: $STARTUP_LOG"
tail -50 "$STARTUP_LOG"
exit 1