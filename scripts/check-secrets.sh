#!/usr/bin/env bash
# check-secrets.sh —— 本地 / CI 机密扫描入口（对应 R4 安全不变量）
#
# 行为：
#   - 调用 gitleaks 扫描当前仓库（工作树 + 已跟踪文件），读取仓库根目录下的 .gitleaks.toml。
#   - 扫描结果中任一命中都会以非零退出码退出；脚本原样透传 gitleaks 的退出码。
#   - gitleaks 未安装时以清晰提示退出（退出码 127），附 Windows / macOS / Linux 安装方式。
#
# 用法：
#   bash scripts/check-secrets.sh
#
# 该脚本是幂等的（扫描只读），可以被 Git pre-commit hook、CI Pipeline 直接调用。
set -euo pipefail

# 解析仓库根目录（脚本位于 scripts/ 下）
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
CONFIG_FILE="${REPO_ROOT}/.gitleaks.toml"

if ! command -v gitleaks >/dev/null 2>&1; then
    cat >&2 <<'EOF'
[check-secrets] gitleaks 未安装。请先安装：
  - macOS:        brew install gitleaks
  - Linux:        https://github.com/gitleaks/gitleaks/releases
  - Windows:      scoop install gitleaks   # 或 choco install gitleaks
  - Go 用户:      go install github.com/gitleaks/gitleaks/v8@latest

安装完成后再次运行：bash scripts/check-secrets.sh
EOF
    exit 127
fi

if [ ! -f "${CONFIG_FILE}" ]; then
    echo "[check-secrets] 找不到配置文件：${CONFIG_FILE}" >&2
    exit 2
fi

echo "[check-secrets] 使用配置 ${CONFIG_FILE} 扫描仓库 ${REPO_ROOT} ..."

# --no-banner : 关闭 gitleaks ASCII logo，便于 CI 日志
# --redact    : 命中结果中的机密值打码，避免在日志中再次泄露
# --source .  : 扫描当前工作目录（包含未提交变更）
set +e
gitleaks detect \
    --config "${CONFIG_FILE}" \
    --no-banner \
    --redact \
    --source "${REPO_ROOT}"
status=$?
set -e

if [ "${status}" -eq 0 ]; then
    echo "[check-secrets] 未发现机密泄露。"
else
    echo "[check-secrets] 发现疑似机密泄露，退出码 ${status}。" >&2
fi

exit "${status}"
