#!/bin/bash
# ============================================================
# deploy.sh - 企业级一键部署（安全替换/备份/回滚/健康检查）
# 顺序（安全模式）:
# 1. mvn 打包
# 2. 上传 同目录下脚本（restart.sh/start.sh/start-debug.sh/stop.sh）
# 3. 在远端执行 stop.sh
# 4. 备份远端旧 JAR
# 5. 上传新 JAR(临时名) -> 原子 mv 到目标名
# 6. 执行 restart.sh（传入 START_MODEL）
# 7. 健康检查（等待端口/服务），失败则回滚并重启旧 JAR
#
# 用法:
#   ./deploy.sh            # 默认 start_model=debug
#   ./deploy.sh debug      # 使用 debug 模式
#   START_MODEL=debug ./deploy.sh
#
# SSH 免密登录配置（首次需执行）:
#   ssh-keygen -t ed25519 -C "deploy-key"
#   ssh-copy-id -p 2222 bebopze@192.168.101.10
# ============================================================

set -o errexit
set -o pipefail
set -o nounset

# ---------------- 配置（请根据实际修改） ----------------
JAR_NAME="tdx-quant-1.0-SNAPSHOT.jar"
REMOTE_USER="bebopze"
REMOTE_HOST="192.168.101.10"
REMOTE_PORT="2222"
REMOTE_PATH="/home/bebopze/code/tdx-quant/prod"
SERVER_PORT=7001                # 应用端口（用于健康检查）
BACKUP_KEEP=3                   # 在远端保留最近 N 个备份
UPLOAD_TIMEOUT=300              # 上传/等待超时（秒）

# SSH 免密登录配置
SSH_KEY="${SSH_KEY:-$HOME/.ssh/id_ed25519}"   # 可通过环境变量指定密钥路径
SSH_OPTIONS="-o StrictHostKeyChecking=no -o BatchMode=yes -o ConnectTimeout=10 -p ${REMOTE_PORT}"
# 如果指定了密钥文件且存在，则添加到 SSH_OPTIONS
if [ -f "$SSH_KEY" ]; then
    SSH_OPTIONS="-i $SSH_KEY $SSH_OPTIONS"
fi
# --------------------------------------------------------

# 支持命令行参数或环境变量 START_MODEL
if [ "${1:-}" != "" ]; then
    START_MODEL="$1"
else
    START_MODEL="${START_MODEL:-debug}"
fi
START_MODEL=$(echo "$START_MODEL" | tr 'A-Z' 'a-z')

# 本地脚本目录 & 项目根
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
PROJECT_ROOT=$(cd "$SCRIPT_DIR/../.." && pwd)

echo "📁 项目根: $PROJECT_ROOT"
echo "🔧 启动模式: $START_MODEL"
cd "$PROJECT_ROOT"

# 必要性检查
if [ ! -f "pom.xml" ]; then
    echo "❌ 未找到 pom.xml，退出"
    exit 1
fi

# SSH 免密登录检查
echo ""
echo "=== 0) 检查 SSH 免密登录 ==="
check_ssh_connection() {
    ssh $SSH_OPTIONS "$REMOTE_USER@$REMOTE_HOST" "echo 'SSH连接成功'" 2>/dev/null
    return $?
}

if ! check_ssh_connection; then
    echo "❌ SSH 免密登录失败，请先配置免密登录："
    echo ""
    echo "   方法一（推荐）: 使用 ssh-copy-id"
    echo "   ----------------------------------------"
    echo "   # 1. 生成密钥（如果没有）"
    echo "   ssh-keygen -t ed25519 -C 'deploy-key'"
    echo ""
    echo "   # 2. 复制公钥到远程服务器"
    echo "   ssh-copy-id -p $REMOTE_PORT $REMOTE_USER@$REMOTE_HOST"
    echo ""
    echo "   方法二: 手动复制"
    echo "   ----------------------------------------"
    echo "   cat ~/.ssh/id_ed25519.pub | ssh -p $REMOTE_PORT $REMOTE_USER@$REMOTE_HOST 'mkdir -p ~/.ssh && cat >> ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys'"
    echo ""
    echo "   如果使用其他密钥文件，请设置环境变量："
    echo "   SSH_KEY=/path/to/your/key ./deploy.sh"
    echo ""
    exit 1
fi
echo "✅ SSH 免密登录正常"

# 远程执行 helper（返回状态）
remote_exec() {
    local cmd="$1"
    ssh $SSH_OPTIONS "$REMOTE_USER@$REMOTE_HOST" "bash -lc \"$cmd\""
}

# 远程复制 helper
remote_copy() {
    local src="$1"
    local dest="$2"
    scp -P "$REMOTE_PORT" -o StrictHostKeyChecking=no -o BatchMode=yes ${SSH_KEY:+-i "$SSH_KEY"} "$src" "$REMOTE_USER@$REMOTE_HOST:$dest"
}

# 1) 本地构建
echo ""
echo "=== 1) 本地 mvn 打包 ==="
mvn clean install -Dmaven.test.skip=true
echo "✅ 本地打包完成"

# 2) 上传部署脚本（排除 deploy.sh 本身）
echo ""
echo "=== 2) 上传部署脚本 (scripts in $SCRIPT_DIR) ==="
uploaded_any=false
for f in "$SCRIPT_DIR"/*.sh; do
    [ -e "$f" ] || continue
    base=$(basename "$f")
    if [ "$base" = "$(basename "$0")" ]; then
        # 跳过 deploy.sh 自身
        continue
    fi
    echo "-> 上传 $base ..."
    remote_copy "$f" "$REMOTE_PATH/"
    if [ $? -ne 0 ]; then
        echo "❌ 上传脚本 $base 失败"
        exit 1
    fi
    uploaded_any=true
done

if [ "$uploaded_any" = true ]; then
    echo "-> 设置远端脚本可执行权限"
    # 仅对 deploy 目录下的脚本设置权限
    # 简化：直接给远端脚本目录下的所有 .sh 加可执行位（更稳健）
    remote_exec "cd '$REMOTE_PATH' && chmod +x -- *.sh || true"
    echo "✅ 脚本上传并 chmod 完成"
else
    echo "⚠️ 未上传任何脚本"
fi

# 3) 远端 stop（必须）
echo ""
echo "=== 3) 在远端执行 stop.sh (如果存在) ==="
# 确保远端目录存在
remote_exec "mkdir -p '$REMOTE_PATH'"

# 设置 LANG 以防编码问题，并运行 stop.sh（若 stop.sh 不存在则回报错）
STOP_CMD="cd '$REMOTE_PATH' && export LANG=zh_CN.UTF-8 LC_ALL=zh_CN.UTF-8 && if [ -x ./stop.sh ]; then ./stop.sh; else echo 'WARN: stop.sh not found or not executable' >&2; exit 2; fi"
echo "-> 运行远端 stop.sh ..."
set +e
remote_exec "$STOP_CMD"
rc=$?
set -e
if [ $rc -ne 0 ]; then
    if [ $rc -eq 2 ]; then
        echo "❌ 远端未找到可执行 stop.sh，出于安全考虑中断部署"
    else
        echo "❌ stop.sh 返回非 0 状态 ($rc)，请先确认远端服务是否已停止"
    fi
    exit 1
fi
echo "✅ 远端 stop.sh 执行完成"

# 4) 备份并上传 JAR（先备份远端现有 JAR，再上传临时文件，再原子 mv）
echo ""
echo "=== 4) 备份旧 JAR 并上传新 JAR（原子替换） ==="
TIMESTAMP=$(date +%Y%m%d%H%M%S)
REMOTE_JAR="$REMOTE_PATH/$JAR_NAME"
REMOTE_BACKUP_DIR="$REMOTE_PATH/backups"
REMOTE_BACKUP_NAME="${JAR_NAME}.${TIMESTAMP}.bak"
REMOTE_TMP_NAME="${JAR_NAME}.uploading.${TIMESTAMP}"
echo "-> 远端备份目录: $REMOTE_BACKUP_DIR"
echo "-> 备份旧 JAR 为: $REMOTE_BACKUP_NAME (若存在)"

# 在远端创建备份目录并移动旧 jar（如果存在）
remote_exec "mkdir -p '$REMOTE_BACKUP_DIR' '$REMOTE_PATH' && if [ -f '$REMOTE_JAR' ]; then mv '$REMOTE_JAR' '$REMOTE_BACKUP_DIR/$REMOTE_BACKUP_NAME'; fi"

# 5) 上传新 JAR(临时名) -> 原子 mv 到目标名
echo ""
echo "=== 5) 上传新 JAR(临时名) -> 原子 mv 到目标名 ==="
echo "-> 上传新 JAR 到远端临时文件: $REMOTE_TMP_NAME"
remote_copy "target/$JAR_NAME" "$REMOTE_PATH/$REMOTE_TMP_NAME"
if [ $? -ne 0 ]; then
    echo "❌ JAR 上传失败，尝试恢复旧 JAR（如果有）并退出"
    # 尝试恢复旧 jar
    remote_exec "if [ -f '$REMOTE_BACKUP_DIR/$REMOTE_BACKUP_NAME' ]; then mv '$REMOTE_BACKUP_DIR/$REMOTE_BACKUP_NAME' '$REMOTE_JAR'; fi"
    exit 1
fi
echo "-> 上传成功，开始原子替换（mv 临时 -> 正式）"
remote_exec "cd '$REMOTE_PATH' && mv '$REMOTE_TMP_NAME' '$JAR_NAME' && chmod 644 '$JAR_NAME' || { echo 'ERROR: mv failed' >&2; exit 1; }"
echo "✅ JAR 已替换为新版本"

# 清理旧备份（保留最近 BACKUP_KEEP 个）
echo "-> 清理旧备份（保留最近 $BACKUP_KEEP 个）"
remote_exec "cd '$REMOTE_BACKUP_DIR' && ls -1tr ${JAR_NAME}.*.bak 2>/dev/null | head -n -$BACKUP_KEEP | xargs -r rm -f || true"
echo "✅ 备份清理完成"

# 6) 远端启动（restart.sh）
echo ""
echo "=== 6) 在远端执行 restart.sh (START_MODEL=$START_MODEL) ==="
RESTART_CMD="cd '$REMOTE_PATH' && export LANG=zh_CN.UTF-8 LC_ALL=zh_CN.UTF-8 && START_MODEL=$START_MODEL bash restart.sh"
remote_exec "$RESTART_CMD"
if [ $? -ne 0 ]; then
    echo "❌ 远端 restart.sh 返回非零，开始回滚（尝试恢复旧 JAR）"
    # 回滚逻辑：如果有备份，恢复并重启
    remote_exec "if [ -f '$REMOTE_BACKUP_DIR/$REMOTE_BACKUP_NAME' ]; then mv '$REMOTE_BACKUP_DIR/$REMOTE_BACKUP_NAME' '$REMOTE_JAR'; fi"
    echo "-> 恢复旧 JAR 完成，尝试启动旧版本"
    remote_exec "cd '$REMOTE_PATH' && export LANG=zh_CN.UTF-8 LC_ALL=zh_CN.UTF-8 && START_MODEL=debug bash restart.sh || true"
    echo "❌ 部署失败并回滚完毕"
    exit 1
fi
echo "✅ restart.sh 执行完成"


# 7) 健康检查（优先 HTTP，回退 nc/lsof；适配 WSL/容器）
echo ""
echo "=== 7.1) 健康检查（等待远端端口 $SERVER_PORT / HTTP 健康） ==="
HEALTH_WAIT=60
HEALTH_STEP=2
elapsed=0
check_ok=1

while [ $elapsed -lt $HEALTH_WAIT ]; do
  out=$(ssh $SSH_OPTIONS "$REMOTE_USER@$REMOTE_HOST" \
    "bash -lc '
      set -o pipefail
      # 1) 优先：actuator health（若暴露）
      if command -v curl >/dev/null 2>&1; then
        http_code=\$(curl -sS -o /dev/null -w \"%{http_code}\" http://localhost:$SERVER_PORT/actuator/health 2>/dev/null || true)
        if [ \"\$http_code\" = \"200\" ]; then echo OK; exit 0; fi
        # 2) 根路径检测（接受 2xx/3xx）
        http_code=\$(curl -sS -o /tmp/deploy_check_resp -w \"%{http_code}\" http://localhost:$SERVER_PORT/ 2>/dev/null || true)
        if [ -n \"\$http_code\" ] 2>/dev/null && [ \"\$http_code\" -ge 200 ] 2>/dev/null && [ \"\$http_code\" -lt 400 ] 2>/dev/null; then
          echo OK; exit 0
        fi
      fi
      # 3) 回退：nc (netcat)
      if command -v nc >/dev/null 2>&1; then
        if nc -z localhost $SERVER_PORT 2>/dev/null; then echo OK; exit 0; fi
      fi
      # 4) 回退：lsof
      if command -v lsof >/dev/null 2>&1; then
        if lsof -iTCP:$SERVER_PORT -sTCP:LISTEN -Pn 2>/dev/null | grep -q LISTEN; then echo OK; exit 0; fi
      fi
      # 5) 不能判断
      exit 1
    '")

  if [ "$out" = "OK" ]; then
    check_ok=0
    echo ""
    echo "✅ 远端应用已就绪（等待 ${elapsed}s）"
    break
  fi

  sleep $HEALTH_STEP
  elapsed=$((elapsed+HEALTH_STEP))
  echo -n "."
done

# 7.2) 健康检查（等待端口/服务），失败则回滚并重启旧 JAR
if [ $check_ok -ne 0 ]; then
  echo ""
      echo "=== 7.2) ⚠️ 健康检查超时（${HEALTH_WAIT}s），启动可能失败，开始回滚到旧版本（如存在）"
      # 回滚：把备份恢复并重启
      remote_exec "if [ -f '$REMOTE_BACKUP_DIR/$REMOTE_BACKUP_NAME' ]; then mv '$REMOTE_BACKUP_DIR/$REMOTE_BACKUP_NAME' '$REMOTE_JAR'; fi"
      remote_exec "cd '$REMOTE_PATH' && export LANG=zh_CN.UTF-8 LC_ALL=zh_CN.UTF-8 && START_MODEL=debug bash restart.sh || true"
      echo "❌ 部署失败，已回滚到旧版本（如存在备份）"
      exit 1
fi



echo ""
echo "🎉 部署成功！新版本已上线: http://$REMOTE_HOST:$SERVER_PORT"
echo "   远端路径: $REMOTE_JAR"
echo "   备份存放: $REMOTE_BACKUP_DIR/$REMOTE_BACKUP_NAME (如果存在)"
exit 0