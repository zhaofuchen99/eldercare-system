#!/usr/bin/env bash
# =====================================================================
# 冒烟测试：AI 智能养老社区管理系统后端
# ---------------------------------------------------------------------
# 覆盖：核心流程（登录/健康数据/AI咨询/体检预约/活动报名）
#       安全（401 未登录、403 越权）
#       边界（积分不足、名额已满、错误验证码）
#       并发（同一时段多用户预约不超卖，需 DB 取验证码）
# 依赖：curl、jq、python3（并发/注册环节需 python3+pymysql 从库取验证码）
# 用法：
#   BASE_URL=http://localhost:8080 ./scripts/smoke-test.sh   # Windows 上跑（Git Bash / PowerShell）
#   ./scripts/smoke-test.sh                                  # WSL 跑：自动探测网关 IP 指向 Windows 后端
# 测试账号：13800000000/Admin@123456（管理员）、13800138000/Test@123456（会员）
# =====================================================================
set -u
PASS=0; FAIL=0; WARN=0; SKIP=0

# ---- 颜色 ----
if [ -t 1 ]; then C_G=$'\033[32m'; C_R=$'\033[31m'; C_Y=$'\033[33m'; C_B=$'\033[34m'; C_0=$'\033[0m'
else C_G=''; C_R=''; C_Y=''; C_B=''; C_0=''; fi

# ---- 后端地址探测 ----
detect_base() {
  [ -n "${BASE_URL:-}" ] && { echo "${BASE_URL%/}"; return; }
  curl -s -m 2 http://localhost:8080 -o /dev/null && { echo "http://localhost:8080"; return; }
  local gw; gw=$(ip route 2>/dev/null | awk '/default/{print $3}')
  if [ -n "$gw" ] && curl -s -m 2 "http://$gw:8080" -o /dev/null; then echo "http://$gw:8080"; return; fi
  echo "http://localhost:8080"
}
BASE="$(detect_base)"
echo "== 后端地址：$BASE"

# ---- DB 配置（并发/注册环节从 sms_code 表取验证码）----
DB_HOST="${DB_HOST:-$(ip route 2>/dev/null | awk '/default/{print $3}')}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-smoke}"
DB_PASS="${DB_PASS:-smoke_pass}"
DB_NAME="${DB_NAME:-eldercare}"

TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
TOMORROW=$(date -d tomorrow +%F 2>/dev/null || date -v+1d +%F)

# ---- 工具函数 ----
http() { # http METHOD URL [auth] [data]
  local method=$1 url=$2 auth=${3:-} data=${4:-}
  local args=(-s -m 60 -X "$method" -w '\n%{http_code}' -H 'Content-Type: application/json')
  [ -n "$auth" ] && args+=(-H "Authorization: Bearer $auth")
  [ -n "$data" ] && args+=(-d "$data")
  curl "${args[@]}" "$BASE$url"
}
code_of() { echo "$1" | tail -1; }
body_of() { echo "$1" | sed '$d'; }
jq_of() { echo "$1" | jq -r "$2" 2>/dev/null; }

# check_req 名称 方法 URL auth data 期望业务码
# 说明：业务码取响应体 Result<T>.code（业务错误如 400/404/409 走 HTTP 200 + body.code，
#      安全拦截 401/403 是 HTTP 状态且 body.code 同步），两者取 body.code 优先、HTTP 兜底。
check_req() {
  local name=$1 method=$2 url=$3 auth=$4 data=$5 want=$6
  local out code b biz got
  out=$(http "$method" "$url" "$auth" "$data")
  code=$(code_of "$out"); b=$(body_of "$out")
  biz=$(jq_of "$b" '.code // empty')
  got="$code"; [ -n "$biz" ] && got="$biz"
  if [ "$got" != "$want" ]; then
    FAIL=$((FAIL+1)); printf "${C_R}[FAIL]${C_0} %s：期望 %s，实际 %s（HTTP %s）\n" "$name" "$want" "$got" "$code" >&2
    echo "$b" | head -3 >&2
    return 1
  fi
  PASS=$((PASS+1)); printf "${C_G}[PASS]${C_0} %s（%s）\n" "$name" "$got" >&2
  echo "$b"
}
warn() { WARN=$((WARN+1)); printf "${C_Y}[WARN]${C_0} %s\n" "$1"; }
skip() { SKIP=$((SKIP+1)); printf "${C_B}[SKIP]${C_0} %s\n" "$1"; }

gen_phone() { echo "19$(( 100000000 + RANDOM % 900000000 ))"; }

get_sms_code() { # get_sms_code phone  → 从 MySQL 读最新未用验证码
  local phone=$1
  python3 - "$phone" <<PY 2>/dev/null
import sys, os
try:
    import pymysql
    conn = pymysql.connect(host=os.getenv('DB_HOST','$DB_HOST'), port=int(os.getenv('DB_PORT','$DB_PORT')),
                           user=os.getenv('DB_USER','$DB_USER'), password=os.getenv('DB_PASS','$DB_PASS'),
                           database=os.getenv('DB_NAME','$DB_NAME'), connect_timeout=5)
    cur = conn.cursor()
    cur.execute("SELECT code FROM sms_code WHERE phone=%s AND used=0 ORDER BY id DESC LIMIT 1", ('$phone',))
    row = cur.fetchone(); print(row[0] if row else '')
    conn.close()
except Exception:
    print('')
PY
}

register_user() { # register_user → 输出 "phone token"（失败输出空）
  local phone code out i
  for i in 1 2 3; do
    phone=$(gen_phone)
    http POST /api/sms/code "" "{\"phone\":\"$phone\"}" >/dev/null
    sleep 0.3
    code=$(get_sms_code "$phone")
    [ -n "$code" ] || { return 1; }
    out=$(http POST /api/auth/register "" "{\"phone\":\"$phone\",\"code\":\"$code\",\"password\":\"Test@123456\"}")
    if [ "$(code_of "$out")" = "200" ]; then
      echo "$phone $(jq_of "$(body_of "$out")" '.data.accessToken')"
      return 0
    fi
  done
  return 1
}

admin_member_points() { # admin_member_points admin_token member_id → points
  local b; b=$(body_of "$(http GET "/api/admin/members/$2" "$1")")
  jq_of "$b" '.data.points'
}

# =====================================================================
echo; echo "==================== 1. 安全：未登录拦截 ===================="
check_req "未登录访问会员接口（应 401）" GET /api/member/health "" "" 401 >/dev/null
check_req "未登录访问管理端接口（应 401）" GET /api/admin/dashboard "" "" 401 >/dev/null

echo; echo "==================== 2. 登录 ===================="
ADMIN=$(check_req "管理员登录" POST /api/auth/login "" '{"phone":"13800000000","password":"Admin@123456"}' 200)
ADMIN_TOKEN=$(jq_of "$ADMIN" '.data.accessToken')
MEMBER=$(check_req "会员登录" POST /api/auth/login "" '{"phone":"13800138000","password":"Test@123456"}' 200)
MEMBER_TOKEN=$(jq_of "$MEMBER" '.data.accessToken')
MEMBER_ID=$(jq_of "$MEMBER" '.data.userInfo.id')
MEMBER_POINTS=$(jq_of "$MEMBER" '.data.userInfo.points')
[ -n "$ADMIN_TOKEN" ] || { echo "管理员登录失败，终止"; exit 1; }
[ -n "$MEMBER_TOKEN" ] || { echo "会员登录失败，终止"; exit 1; }
echo "  会员 ID=$MEMBER_ID，当前积分=$MEMBER_POINTS"

echo; echo "==================== 3. 安全：越权访问 ===================="
check_req "会员访问管理端接口（应 403）" GET /api/admin/dashboard "$MEMBER_TOKEN" "" 403 >/dev/null

echo; echo "==================== 4. 核心流程：健康数据录入 ===================="
check_req "录入健康数据" POST /api/member/health "$MEMBER_TOKEN" \
  '{"systolic":130,"diastolic":85,"bloodSugar":5.2,"heartRate":72,"weight":62.5,"memo":"冒烟测试"}' 200 >/dev/null

echo; echo "==================== 5. 核心流程：AI 咨询 ===================="
SID=$(jq_of "$(body_of "$(http POST /api/member/chat/session "$MEMBER_TOKEN")")" '.data')
if [ -n "$SID" ]; then
  PASS=$((PASS+1)); printf "${C_G}[PASS]${C_0} AI 对话-创建会话（%s）\n" "$SID"
  ai=$(http POST /api/member/chat/message "$MEMBER_TOKEN" "{\"sessionId\":$SID,\"content\":\"你好，请用一句话介绍你自己\"}")
  if [ "$(code_of "$ai")" = "200" ] && [ -n "$(jq_of "$(body_of "$ai")" '.data // empty')" ]; then
    PASS=$((PASS+1)); printf "${C_G}[PASS]${C_0} AI 对话-发送消息（DeepSeek 回复正常）\n"
  else
    warn "AI 对话回复异常（未配 DEEPSEEK_API_KEY 或接口失败会报 500，属环境依赖，不判失败）"
  fi
else
  FAIL=$((FAIL+1)); printf "${C_R}[FAIL]${C_0} AI 对话-创建会话失败\n"
fi

echo; echo "==================== 6. 核心流程：体检预约 ===================="
# 确保会员积分 >= 100（预约测试要扣 50）
if [ "${MEMBER_POINTS:-0}" -lt 100 ]; then
  chk=$(body_of "$(http PUT "/api/admin/members/$MEMBER_ID/points" "$ADMIN_TOKEN" "{\"delta\":$((100-MEMBER_POINTS))}")")
  MEMBER_POINTS=100; printf "  会员积分不足，已补足至 100（余额 %s）\n" "$(jq_of "$chk" '.data')"
fi
PKID=$(jq_of "$(body_of "$(http POST /api/admin/appointment/package "$ADMIN_TOKEN" "{\"name\":\"冒烟预约套餐\",\"description\":\"冒烟\",\"price\":50,\"suitablePeople\":\"测试\",\"items\":[\"血压\"],\"status\":\"ENABLED\"}")")" '.data')
http POST /api/admin/appointment/slot/batch "$ADMIN_TOKEN" "{\"packageId\":$PKID,\"dates\":[\"$TOMORROW\"],\"timeRanges\":[\"10:00-11:00\"],\"maxCount\":2}" >/dev/null
SLOT=$(jq_of "$(body_of "$(http GET "/api/admin/appointment/slot?packageId=$PKID" "$ADMIN_TOKEN")")" '.data[0].id')
APPID=$(jq_of "$(check_req "会员提交预约" POST /api/member/appointment "$MEMBER_TOKEN" "{\"slotId\":$SLOT}" 200)" '.data')
[ -n "$APPID" ] && printf "  预约成功，预约ID=%s\n" "$APPID"
check_req "我的预约列表" GET "/api/member/appointment?page=1&size=5" "$MEMBER_TOKEN" "" 200 >/dev/null

echo; echo "==================== 7. 核心流程：活动报名 ===================="
NOW=$(date +%Y-%m-%dT%H:%M:%S)
RSTART=$(date -d '-1 hour' +%Y-%m-%dT%H:%M:%S 2>/dev/null || date -v-1H +%Y-%m-%dT%H:%M:%S)
REND=$(date -d '+1 day' +%Y-%m-%dT%H:%M:%S 2>/dev/null || date -v+1d +%Y-%m-%dT%H:%M:%S)
ASTART=$(date -d tomorrow +%Y-%m-%dT09:00:00 2>/dev/null || date -v+1d +%Y-%m-%dT09:00:00)
AEND=$(date -d tomorrow +%Y-%m-%dT11:00:00 2>/dev/null || date -v+1d +%Y-%m-%dT11:00:00)
ACTID=$(jq_of "$(body_of "$(http POST /api/admin/activity "$ADMIN_TOKEN" "{\"title\":\"冒烟活动\",\"location\":\"社区中心\",\"registrationStartTime\":\"$RSTART\",\"registrationEndTime\":\"$REND\",\"activityStartTime\":\"$ASTART\",\"activityEndTime\":\"$AEND\",\"maxParticipants\":50,\"status\":\"REGISTRATING\"}")")" '.data')
check_req "会员报名活动" POST "/api/member/activity/$ACTID/register" "$MEMBER_TOKEN" "" 200 >/dev/null

echo; echo "==================== 8. 边界：名额已满（同一时段重复预约）===================="
PKID2=$(jq_of "$(body_of "$(http POST /api/admin/appointment/package "$ADMIN_TOKEN" "{\"name\":\"冒烟满员套餐\",\"description\":\"冒烟\",\"price\":0,\"status\":\"ENABLED\"}")")" '.data')
http POST /api/admin/appointment/slot/batch "$ADMIN_TOKEN" "{\"packageId\":$PKID2,\"dates\":[\"$TOMORROW\"],\"timeRanges\":[\"11:00-12:00\"],\"maxCount\":1}" >/dev/null
SLOT2=$(jq_of "$(body_of "$(http GET "/api/admin/appointment/slot?packageId=$PKID2" "$ADMIN_TOKEN")")" '.data[0].id')
check_req "首次预约（应成功）" POST /api/member/appointment "$MEMBER_TOKEN" "{\"slotId\":$SLOT2}" 200 >/dev/null
check_req "再次预约同时段（应 409 名额已满）" POST /api/member/appointment "$MEMBER_TOKEN" "{\"slotId\":$SLOT2}" 409 >/dev/null

echo; echo "==================== 9. 边界：积分不足无法预约 ===================="
CUR=$(admin_member_points "$ADMIN_TOKEN" "$MEMBER_ID")
if [ "${CUR:-0}" -gt 0 ]; then
  http PUT "/api/admin/members/$MEMBER_ID/points" "$ADMIN_TOKEN" "{\"delta\":$(( -CUR ))}" >/dev/null
  printf "  会员积分已调为 0\n"
fi
PKID3=$(jq_of "$(body_of "$(http POST /api/admin/appointment/package "$ADMIN_TOKEN" "{\"name\":\"冒烟欠费套餐\",\"description\":\"冒烟\",\"price\":50,\"status\":\"ENABLED\"}")")" '.data')
http POST /api/admin/appointment/slot/batch "$ADMIN_TOKEN" "{\"packageId\":$PKID3,\"dates\":[\"$TOMORROW\"],\"timeRanges\":[\"14:00-15:00\"],\"maxCount\":2}" >/dev/null
SLOT3=$(jq_of "$(body_of "$(http GET "/api/admin/appointment/slot?packageId=$PKID3" "$ADMIN_TOKEN")")" '.data[0].id')
check_req "积分不足预约（应 409）" POST /api/member/appointment "$MEMBER_TOKEN" "{\"slotId\":$SLOT3}" 409 >/dev/null
chk=$(body_of "$(http PUT "/api/admin/members/$MEMBER_ID/points" "$ADMIN_TOKEN" '{"delta":100}')")
printf "  已恢复会员积分 +100，当前余额 %s\n" "$(jq_of "$chk" '.data')"

echo; echo "==================== 10. 边界：错误验证码 ===================="
P10=$(gen_phone)
http POST /api/sms/code "" "{\"phone\":\"$P10\"}" >/dev/null
check_req "错误验证码注册（应 400）" POST /api/auth/register "" "{\"phone\":\"$P10\",\"code\":\"000000\",\"password\":\"Test@123456\"}" 400 >/dev/null

echo; echo "==================== 11. 并发：同一时段不超卖（需 DB 取码）===================="
conc_test() {
  local pkid slot_id n_ok code i line phone tok reg
  pkid=$(jq_of "$(body_of "$(http POST /api/admin/appointment/package "$ADMIN_TOKEN" "{\"name\":\"冒烟并发套餐\",\"description\":\"冒烟\",\"price\":50,\"status\":\"ENABLED\"}")")" '.data')
  http POST /api/admin/appointment/slot/batch "$ADMIN_TOKEN" "{\"packageId\":$pkid,\"dates\":[\"$TOMORROW\"],\"timeRanges\":[\"16:00-17:00\"],\"maxCount\":3}" >/dev/null
  slot_id=$(jq_of "$(body_of "$(http GET "/api/admin/appointment/slot?packageId=$pkid" "$ADMIN_TOKEN")")" '.data[0].id')
  # 注册 4 个新会员
  local toks=() phones=()
  for i in 1 2 3 4; do
    reg=$(register_user) || { skip "并发测试跳过：无法从数据库读取验证码（未授权 DB 访问）"; return; }
    line=( $reg ); phones+=("${line[0]}"); toks+=("${line[1]}")
  done
  printf "  已注册 4 个新会员用于并发：%s\n" "${phones[*]}"
  # 4 个并发预约同一时段
  local j=0
  for tok in "${toks[@]}"; do
    j=$((j+1))
    ( curl -s -m 60 -X POST -H "Authorization: Bearer $tok" -H 'Content-Type: application/json' \
        -d "{\"slotId\":$slot_id}" -o "$TMP/conc_$j" "$BASE/api/member/appointment" ) &
  done
  wait
  n_ok=0
  for f in "$TMP"/conc_*; do
    if [ "$(jq -r '.code // empty' "$f" 2>/dev/null)" = "200" ]; then n_ok=$((n_ok+1)); fi
  done
  if [ "$n_ok" = "3" ]; then
    PASS=$((PASS+1)); printf "${C_G}[PASS]${C_0} 并发预约：3 个成功、1 个失败，无超卖\n"
  else
    FAIL=$((FAIL+1)); printf "${C_R}[FAIL]${C_0} 并发预约：期望 3 个成功，实际 %s 个（结果见 /tmp 输出）\n" "$n_ok"
  fi
  # 校验时段 current_count == 3
  local cc; cc=$(jq_of "$(body_of "$(http GET "/api/admin/appointment/slot?packageId=$pkid" "$ADMIN_TOKEN")")" '.data[0].currentCount')
  if [ "$cc" = "3" ]; then
    PASS=$((PASS+1)); printf "${C_G}[PASS]${C_0} 并发后时段 current_count=%s（=max_count，无超卖）\n" "$cc"
  else
    FAIL=$((FAIL+1)); printf "${C_R}[FAIL]${C_0} 并发后时段 current_count=%s（应为 3）\n" "$cc"
  fi
}
conc_test

# =====================================================================
echo; echo "==================== 汇总 ===================="
printf "${C_G}PASS %s${C_0}  ${C_R}FAIL %s${C_0}  ${C_Y}WARN %s${C_0}  ${C_B}SKIP %s${C_0}\n" "$PASS" "$FAIL" "$WARN" "$SKIP"
echo "（SKIP 一般因 DB 未授权无法取验证码，并发/注册项跳过；授权方式见 docs/验收自测清单.md 的说明）"
[ "$FAIL" = "0" ] && echo "结论：全部通过（含 SKIP/WARN 需人工确认的项请参考 docs/验收自测清单.md）" || echo "结论：存在失败项，请参考上方响应信息排查"
