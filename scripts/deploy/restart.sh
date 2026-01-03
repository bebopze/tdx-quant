#!/bin/bash
# ============================================================
# restart.sh - Mac/Linux 双系统兼容版本
# 用于重启 TdxQuant 量化系统
# 支持 start_model 参数：debug | run (缺省 debug)
# 使用方式:
#   ./restart.sh            # 默认 debug
#   ./restart.sh debug      # 使用 debug 模式（调用 start-debug.sh）
#   START_MODEL=debug ./restart.sh
# ============================================================

set -o errexit
set -o nounset
set -o pipefail

# 获取脚本所在目录
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)

# 优先使用第一个参数，其次使用环境变量 START_MODEL，缺省为 debug
if [ "${1:-}" != "" ]; then
    START_MODEL="$1"
else
    START_MODEL="${START_MODEL:-debug}"
fi

# 规范化小写
START_MODEL=$(echo "$START_MODEL" | tr 'A-Z' 'a-z')

echo "🔄 重启量化系统 [$(date)]"
echo "================================"
echo "🔧 启动模式: $START_MODEL"

# 调用 stop.sh（若 stop.sh 返回非零则退出）
if [ -x "$SCRIPT_DIR/stop.sh" ]; then
    echo "⏹️ 停止服务..."
    "$SCRIPT_DIR/stop.sh"
else
    echo "⚠️ 未找到可执行的 stop.sh ($SCRIPT_DIR/stop.sh)，跳过停止步骤"
fi

# 等待端口释放
echo "⏳ 等待端口释放..."
sleep 2

# 启动：根据 START_MODEL 选择脚本
case "$START_MODEL" in
    debug)
        if [ -x "$SCRIPT_DIR/start-debug.sh" ]; then
            echo "▶️ 使用 start-debug.sh 启动 (DEBUG 模式)"
            "$SCRIPT_DIR/start-debug.sh"
            rc=$?
            if [ $rc -ne 0 ]; then
                echo "❌ start-debug.sh 返回非零状态: $rc"
                exit $rc
            fi
        else
            echo "❌ 未找到可执行的 start-debug.sh ($SCRIPT_DIR/start-debug.sh)"
            exit 1
        fi
        ;;
    run|*)
        if [ -x "$SCRIPT_DIR/start.sh" ]; then
            echo "▶️ 使用 start.sh 启动 (RUN 模式)"
            "$SCRIPT_DIR/start.sh"
            rc=$?
            if [ $rc -ne 0 ]; then
                echo "❌ start.sh 返回非零状态: $rc"
                exit $rc
            fi
        else
            echo "❌ 未找到可执行的 start.sh ($SCRIPT_DIR/start.sh)"
            exit 1
        fi
        ;;
esac

echo "================================"
echo "🎉 重启完成"