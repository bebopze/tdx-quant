#!/bin/bash
# ============================================================
# stop.sh - Mac/Linux 双系统兼容版本
# 实时查询进程并停止
# ============================================================

# ==================== 配置 ====================
JAR_NAME="tdx-quant-1.0-SNAPSHOT.jar"
SHUTDOWN_TIMEOUT=10
# ==============================================

echo "🛑 停止量化系统 [$(date)]"

# 实时查询所有匹配的进程
PIDS=$(pgrep -f "$JAR_NAME" 2>/dev/null || true)

if [ -z "$PIDS" ]; then
    echo "⚠️  未找到运行中的 $JAR_NAME 进程"
    exit 0
fi

echo "🔍 找到进程: $PIDS"

# 优雅关闭所有匹配进程 (SIGTERM)
echo "⏳ 优雅关闭应用 (最多${SHUTDOWN_TIMEOUT}秒)..."
for PID in $PIDS; do
    kill -15 "$PID" 2>/dev/null || true
done

# 等待进程退出
for i in $(seq 1 $SHUTDOWN_TIMEOUT); do
    REMAINING=$(pgrep -f "$JAR_NAME" 2>/dev/null || true)
    if [ -z "$REMAINING" ]; then
        echo ""
        echo "✅ 应用已成功停止"
        exit 0
    fi
    echo -n "."
    sleep 1
done

# 强制终止
echo ""
echo "⚠️  优雅关闭超时，强制终止..."
pkill -9 -f "$JAR_NAME" 2>/dev/null || true
sleep 1

# 最终检查
FINAL_CHECK=$(pgrep -f "$JAR_NAME" 2>/dev/null || true)
if [ -z "$FINAL_CHECK" ]; then
    echo "✅ 应用已强制停止"
else
    echo "❌ 应用无法终止! 残留进程: $FINAL_CHECK"
    exit 1
fi







##!/bin/bash
#PID=$(pgrep -f "tdx-quant-1.0-SNAPSHOT.jar")
#if [[ -n "${PID}" ]]; then
#    kill -9 "${PID}"
#    echo "已终止进程 ${PID}"
#else
#    echo "进程未运行"
#fi