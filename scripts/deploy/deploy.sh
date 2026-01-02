#!/bin/bash
# ============================================================
# deploy.sh - 一键部署脚本（自动输入密码版）
# 顺序：
# 1. 上传 同目录下所有其他脚本（restart.sh、start.sh、start-debug.sh、stop.sh）
# 2. mvn 打包
# 3. 上传 JAR 包
# 4. 执行 restart.sh（将传入 START_MODEL 环境变量）
# 用法:
#   ./deploy.sh            # 默认 start_model=run
#   ./deploy.sh debug      # 使用 debug 模式（远端会执行 START_MODEL=debug bash restart.sh）
#   START_MODEL=debug ./deploy.sh
# ============================================================

set -o errexit
set -o pipefail
set -o nounset

# ==================== 配置 ====================
JAR_NAME="tdx-quant-1.0-SNAPSHOT.jar"
REMOTE_USER="bebopze"
REMOTE_PWD="123456"
REMOTE_HOST="192.168.101.10"
REMOTE_PORT="2222"
REMOTE_PATH="/home/bebopze/code/tdx-quant/prod"
# ==============================================

# 支持从命令行第一个参数或环境变量 START_MODEL 获取启动模式，默认 run
if [ "${1:-}" != "" ]; then
    START_MODEL="$1"
else
    START_MODEL="${START_MODEL:-run}"
fi
# 规范化
START_MODEL=$(echo "$START_MODEL" | tr 'A-Z' 'a-z')

# 获取脚本所在目录，然后切换到项目根目录
SCRIPT_DIR=$(cd "$(dirname "$0")"; pwd)
PROJECT_ROOT=$(cd "$SCRIPT_DIR/../.."; pwd)

echo "📁 项目根目录: $PROJECT_ROOT"
echo "🔧 将使用启动模式: $START_MODEL"
cd "$PROJECT_ROOT"

# 检查 pom.xml 是否存在
if [ ! -f "pom.xml" ]; then
    echo "❌ 未找到 pom.xml，请确认项目结构"
    exit 1
fi

# 检查 sshpass 是否安装
if ! command -v sshpass &> /dev/null; then
    echo "⚠️  sshpass 未安装，正在安装..."
    if [[ "$OSTYPE" == "darwin"* ]]; then
        brew install hudochenkov/sshpass/sshpass
    else
        sudo apt-get update
        sudo apt-get install -y sshpass
    fi
fi

# 便捷函数：远程执行命令（不自动退出脚本）
remote_exec() {
    local cmd="$1"
    sshpass -p "$REMOTE_PWD" ssh -p "$REMOTE_PORT" -o StrictHostKeyChecking=no \
        "$REMOTE_USER@$REMOTE_HOST" "$cmd"
}

# 确保远程目标目录存在
echo "=== 确保远程目录存在: $REMOTE_PATH ==="
sshpass -p "$REMOTE_PWD" ssh -p "$REMOTE_PORT" -o StrictHostKeyChecking=no \
    "$REMOTE_USER@$REMOTE_HOST" "mkdir -p '$REMOTE_PATH'"
if [ $? -ne 0 ]; then
    echo "❌ 无法在远程创建目录 $REMOTE_PATH"
    exit 1
fi

echo ""
echo "=== 1. 上传部署脚本（不包含 deploy.sh 自身） ==="
# 查找与当前 deploy.sh 同目录下的脚本并上传，排除 deploy.sh 本身
uploaded_any=false
for f in "$SCRIPT_DIR"/*.sh; do
    [ -e "$f" ] || continue
    base=$(basename "$f")
    if [ "$base" = "$(basename "$0")" ]; then
        continue
    fi
    echo "-> 上传 $base ..."
    sshpass -p "$REMOTE_PWD" scp -P "$REMOTE_PORT" -o StrictHostKeyChecking=no \
        "$f" "$REMOTE_USER@$REMOTE_HOST:$REMOTE_PATH/"
    if [ $? -ne 0 ]; then
        echo "❌ 上传脚本 $base 失败！"
        exit 1
    fi
    uploaded_any=true
done

if [ "$uploaded_any" = true ]; then
    echo "✅ 脚本上传完成，设置远程脚本为可执行..."
    # 给刚上传的脚本设置可执行权限
    for f in "$SCRIPT_DIR"/*.sh; do
        [ -e "$f" ] || continue
        base=$(basename "$f")
        if [ "$base" = "$(basename "$0")" ]; then
            continue
        fi
        remote_exec "chmod +x '$REMOTE_PATH/$base' || true"
    done
    echo "✅ 权限设置完成"
else
    echo "⚠️ 未找到要上传的脚本（除 deploy.sh 外）。"
fi

echo ""
echo "=== 2. 本地打包 (mvn) ==="
mvn clean install -Dmaven.test.skip=true
if [ $? -ne 0 ]; then
    echo "❌ 打包失败！"
    exit 1
fi
echo "✅ 打包成功"

echo ""
echo "=== 3. 上传 JAR ==="
if [ ! -f "target/$JAR_NAME" ]; then
    echo "❌ 未找到 target/$JAR_NAME，请确认打包输出"
    exit 1
fi

sshpass -p "$REMOTE_PWD" scp -P "$REMOTE_PORT" -o StrictHostKeyChecking=no \
    "target/$JAR_NAME" "$REMOTE_USER@$REMOTE_HOST:$REMOTE_PATH/"
if [ $? -ne 0 ]; then
    echo "❌ JAR 上传失败！"
    exit 1
fi
echo "✅ JAR 上传成功"

echo ""
echo "=== 4. 远程重启服务 (执行 restart.sh) ==="
# 通过 bash 执行 restart.sh，并传入 START_MODEL 环境变量（同时设置 LANG 以防乱码）
# 例如: START_MODEL=debug bash restart.sh
sshpass -p "$REMOTE_PWD" ssh -p "$REMOTE_PORT" -o StrictHostKeyChecking=no \
    "$REMOTE_USER@$REMOTE_HOST" "cd '$REMOTE_PATH' && export LANG=zh_CN.UTF-8 LC_ALL=zh_CN.UTF-8 && START_MODEL=$START_MODEL bash restart.sh"
if [ $? -ne 0 ]; then
    echo "❌ 远程执行 restart.sh 返回非零状态，请检查远程日志"
    exit 1
fi
echo ""
echo "🎉 === 部署完成 ==="