# 架构规格：Mock-First 取件码浮窗助手 — 单一事实来源（SSOT）
> 版本：v1.0 | 生效日期：2026-10-13
> 重要提醒：带 ⚠ 标注的字段为快递鸟取件码 API 字段名，尚未在官方文档中完全坐实，上线前须按官方最新文档补齐映射；mock 阶段不影响功能正确性。
> 执行 Agent 须将本文档作为唯一事实来源，任何实现决策均须回查此处。

---

## 1. 设计原则

**契约优先（Contract-First）+ Provider 抽象**

错误做法：UI -> 直接调快递鸟 -> 直接展示快递鸟 JSON
后果：mock 阶段和真实阶段数据结构两套管，后期接入等于重写；字段一变全崩。

正确做法：
[复制运单号] -> [浮窗捕获] -> [PickupProvider.query()] -> [规范模型 ParcelResult] -> [Room 落库] -> [UI + 统计]
                                         ↑
                               MockProvider  /  KuaidiNiaoProvider（二选一，配置切换）

App 的其余部分（浮窗、数据库、列表、统计）永远只跟 PickupResult / Parcel / Trace 打交道。
换数据源 = 换一个 Provider 实现，一行配置（ProviderConfig.ACTIVE）。

---

## 2. 规范内部模型（Canonical Model）

> 这是整个项目最重要的定义。真实 API 字段名若与下表不同，只在 Section 4 映射表里改，不动这里、不动 UI、不动 DB。

### 2.1 Parcel（一条包裹记录，落库实体）

| 字段 | 类型 | 说明 |
|---|---|---|
| waybillNo | String | 运单号（主键 / 唯一索引） |
| shipperCode | String | 快递公司编码（ZTO/YTO/SF/JD...），用于真实 API 必填 |
| shipperName | String | 快递公司中文名（展示用） |
| platform | String? | 来源平台（taobao/pdd/jd/sf/other/null），由用户选择或前缀推断，可空 |
| pickupCode | String? | 取件码（如 A-03-1234）；非菜鸟件为 null |
| stationName | String? | 驿站名称（如 菜鸟驿站(XX小区店)）；无则为 null |
| stationAddress | String? | 驿站地址；无则 null |
| status | Int | 规范状态码（见 Section 3 枚举，不要用快递鸟原值） |
| latestTrace | String? | 最新一条轨迹描述（列表页展示） |
| arrivedAt | Long? | 入库/到站时间戳（统计本月到站用） |
| signedAt | Long? | 签收时间戳（统计已取用） |
| createdAt | Long | 录入时间戳 |
| updatedAt | Long | 最近更新时间戳 |

### 2.2 Trace（轨迹节点，Parcel 一对多）

| 字段 | 类型 | 说明 |
|---|---|---|
| waybillNo | String | 关联运单号 |
| time | String | 快递鸟 AcceptTime（如 2026-10-13 10:00:00） |
| station | String | 快递鸟 AcceptStation / Remark 合并 |
| action | Int | 规范状态码（与 Parcel.status 同枚举） |

### 2.3 PickupResult（Provider.query 返回值，不落库，业务层消费）

| 字段 | 类型 | 说明 |
|---|---|---|
| parcel | Parcel | 成功时填充 |
| traces | List<Trace> | 成功时填充 |
| failure | Failure? | 失败时填充（见 Section 6） |

### 2.4 Failure（失败信息，不落库）

| 字段 | 类型 | 说明 |
|---|---|---|
| type | FailureType | 失败类型枚举（见 Section 6） |
| message | String | 友好提示消息 |
| rawResponse | String? | 原始响应（调试用，可空） |

---

## 3. 规范状态码枚举（Status）

不把快递鸟原始 State 存进库。统一映射：

| 值 | 枚举名 | 含义 | 触发条件 |
|---|---|---|---|
| 0 | NO_TRACE | 暂无轨迹 | 快递鸟 State=0 或无轨迹 |
| 1 | COLLECTED | 已揽收 | 快递鸟 State=1 |
| 2 | IN_TRANSIT | 在途中 | 快递鸟 State=2 |
| 3 | ARRIVED | 已入库/入柜 | 快递鸟 StateEx=211 或含驿站/快递柜关键词 -> 触发取件码展示 |
| 4 | SIGNED | 已签收 | 快递鸟 StateEx=3xx |
| 5 | EXCEPTION | 问题件/异常 | 快递鸟 StateEx=4xx |

**取件码展示判定**：当 status == ARRIVED 且 pickupCode != null 时，UI 高亮待取件。
**无取件码判定**：当 status == ARRIVED 且 pickupCode == null 时，显示该件无菜鸟取件码提示。

---

## 4. 快递鸟真实报文 -> 规范模型 映射表

> 来源：快递鸟即时查询 RequestType=1002（请求 URL 以官方最新文档为准）。
> 以下为已确认字段；带 ⚠ 的字段官方文档未在本次核验中完全坐实，上线前按官方取件码 API / 增值物流接口文档补齐映射即可，不影响 mock 阶段。

| 规范字段 | 快递鸟字段 | 备注 |
|---|---|---|
| waybillNo | LogisticCode | 请求回显 |
| shipperCode | ShipperCode | 请求必填；mock 阶段可忽略 |
| shipperName | （本地编码表反查） | 由 ShipperCode 映射中文名 |
| status | State / StateEx | 按 Section 3 枚举映射 |
| latestTrace | Traces[0].AcceptStation + Remark | 数组按时间倒序取首条 |
| traces[].time | Traces[].AcceptTime | |
| traces[].station | Traces[].AcceptStation + Remark | |
| traces[].action | Traces[].Action 或 StateEx | 映射 Section 3 枚举 |
| pickupCode | ⚠ PickupCode / Code | 取件码字段名以官方取件码 API 文档为准 |
| stationName | ⚠ StationName / BranchName | 驿站名 |
| stationAddress | ⚠ StationAddress | 驿站地址 |
| failure | Success=false + Reason | 失败时填充 |

**请求参数（真实 Provider 组装）**：
`
POST <baseUrl>
Content-Type: application/x-www-form-urlencoded
Body:
  RequestData = URLEncoder.encode(JSON, UTF-8)
  EBusinessID = <用户配置的EBusinessID>
  RequestType = 1002
  DataSign = Base64(MD5(EBusinessID + RequestData + AppKey))
  DataType = 2
`

---

## 5. Provider 接口与切换点

### 5.1 PickupProvider 接口

`kotlin
interface PickupProvider {
    suspend fun query(waybillNo: String, shipperCode: String?): PickupResult
}
`

### 5.2 ProviderConfig

`kotlin
object ProviderConfig {
    // 运行时从 DataStore 读取，不写死在代码里
    // mock | kdniao
    val ACTIVE: String get() = dataStore.getString( active_provider, mock)
}
`

### 5.3 ProviderManager（切换点）

`kotlin
object ProviderManager {
    fun get(): PickupProvider = when (ProviderConfig.ACTIVE) {
        kdniao -> KuaidiNiaoProvider()
        else -> MockProvider()
    }
}
`

### 5.4 MockProvider 样本数据

MockProvider 须内置以下样本，供测试覆盖：

| 样本 | waybillNo | pickupCode | stationName | status | 用途 |
|---|---|---|---|---|---|
| 正常菜鸟件 | YT1234567890001 | A-03-1234 | 菜鸟驿站(望京SOHO店) | ARRIVED | 核心成功路径 |
| 运输中件 | SF1234567890002 | null | null | IN_TRANSIT | 无取件码阶段 |
| 已签收件 | ZTO1234567890003 | B-01-5678 | 菜鸟驿站(中关村店) | SIGNED | 签收统计 |
| 非菜鸟件 | JD1234567890004 | null | null | ARRIVED | 提示无取件码权限 |
| 无轨迹件 | YD1234567890005 | null | null | NO_TRACE | 无轨迹提示 |
| 异常件 | EMS1234567890006 | null | null | EXCEPTION | 异常提示 |

---

## 6. Failure 类型枚举

`kotlin
enum class FailureType {
    NETWORK_ERROR,    // 网络异常/超时
    TIMEOUT,          // 请求超时（快递鸟默认 10s）
    NO_TRACE,         // 无轨迹（单号不存在或未揽收）
    NO_PICKUP_CODE,   // 查到轨迹但无取件码（非菜鸟驿站件）
    PERMISSION_DENIED,// 密钥无取件码权限（个人认证只有轨迹权限）
    INVALID_WAYBILL,  // 单号格式无效
    UNKNOWN           // 未知错误
}
`

---

## 7. 完整流程（含 ShipperCode 前缀推断）

### 7.1 浮窗捕获完整流程

`
1. 用户在任意 App 复制运单号
2. 系统浮窗常驻（FloatingWindowService，前台服务保活）
3. 用户点一下浮窗气泡 -> 浮窗获焦点
4. 浮窗读 ClipboardManager 文本（仅此时读取，Android 10+ 合规）
5. 正则提取运单号（格式校验：非空 + 长度 8-30 + 字母数字组合）
   - 提取失败 -> 提示未识别到运单号，请复制物流单号
6. 运单号前缀 -> 推断 shipperCode（编码表见下）
   - 推断不出 -> 默认传 null（真实 Provider 可走快递公司识别 API 或让用户选）
7. 调 PickupProvider.query(waybillNo, shipperCode)
8. 返回 PickupResult：
   - 成功 -> 写/更新 Room 的 Parcel + Traces -> 刷新列表 + 重算统计
   - 失败 -> 按 Failure 类型弹对应提示（不崩溃、不写脏数据）
9. 列表页按 status 分组（待取/运输中/已签收），统计模块实时聚合
`

### 7.2 ShipperCode 前缀推断表

`
YT   -> YTO    圆通
ZT   -> ZTO    中通（ZTO 也直接映射 ZTO）
SF   -> SF     顺丰
JD   -> JD     京东
ST   -> STO    申通
YD   -> YD     韵达
HT   -> HTKY   百世
EMS  -> EMS    邮政
DB   -> DBL    德邦
JT   -> JTSD   极兔
`

该表同时用于把 shipperCode 反查成中文名（shipperName）。

---

## 8. 存储与统计 SQL

### 8.1 Room 表定义

`sql
-- 包裹主表
CREATE TABLE parcels (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,
  waybill_no      TEXT UNIQUE NOT NULL,     -- 运单号（唯一，去重依据，UPSERT）
  shipper_code    TEXT,                     -- 快递公司代码
  shipper_name    TEXT,                     -- 快递公司中文名
  platform        TEXT,                     -- 来源平台（taobao/pdd/jd/sf/other/null）
  status          INTEGER DEFAULT 0,        -- 规范状态码 0-5
  pickup_code     TEXT,                     -- 取件码，可空
  station_name    TEXT,                     -- 驿站名，可空
  station_address TEXT,                     -- 驿站地址，可空
  latest_trace    TEXT,                     -- 最新轨迹描述，可空
  arrived_at      INTEGER,                  -- 入库时间戳，可空
  signed_at       INTEGER,                  -- 签收时间戳，可空
  created_at      INTEGER NOT NULL,         -- 录入时间戳(ms)
  updated_at      INTEGER NOT NULL          -- 更新时间戳(ms)
);

-- 轨迹表
CREATE TABLE traces (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  waybill_no  TEXT NOT NULL,                -- 关联运单号
  trace_time  TEXT NOT NULL,                -- 时间字符串
  trace_station TEXT NOT NULL,              -- 站点描述
  trace_action INTEGER NOT NULL,            -- 规范状态码
  FOREIGN KEY (waybill_no) REFERENCES parcels(waybill_no)
);
CREATE INDEX idx_traces_waybill ON traces(waybill_no);
`

### 8.2 统计 SQL（全部基于本地表聚合，离线可用）

`sql
-- 总览：累计数
SELECT COUNT(*) FROM parcels;

-- 总览：本月数
SELECT COUNT(*) FROM parcels WHERE created_at > ?;  -- 本月1日0点时间戳

-- 总览：待取件（已到站且无签收）
SELECT COUNT(*) FROM parcels WHERE status = 3 AND signed_at IS NULL;

-- 总览：已签收
SELECT COUNT(*) FROM parcels WHERE status = 4;

-- 分布：按快递公司
SELECT shipper_name, COUNT(*) c FROM parcels GROUP BY shipper_name ORDER BY c DESC;

-- 分布：按状态
SELECT status, COUNT(*) c FROM parcels GROUP BY status;

-- 分布：按平台
SELECT platform, COUNT(*) c FROM parcels GROUP BY platform;

-- 趋势：近30天每日收件量
SELECT date(created_at/1000, unixepoch) d, COUNT(*) c
FROM parcels
WHERE created_at > ?
GROUP BY d
ORDER BY d ASC;

-- 驿站 Top 10
SELECT station_name, COUNT(*) c FROM parcels
WHERE station_name IS NOT NULL
GROUP BY station_name
ORDER BY c DESC
LIMIT 10;
`

---

## 9. Mock -> 真实切换清单（预计半天工作量）

1. ProviderConfig.ACTIVE = kdniao，填 EBusinessID / APP_KEY（从设置页读取）
2. 实现 KuaidiNiaoProvider：组装请求 + 签名（先用官方调试页验证签名函数）+ 解析 + Section 4 映射
3. 用 1-2 个真实已到站运单号跑通 query()，比对 pickupCode / stationName 是否正确
4. 补齐 Section 4 ⚠ 字段的真实映射（取件码/驿站名字段名以上线前官方文档为准）
5. 回归测试：重复录入同单号 = 更新而非新增；NO_PICKUP 类件 UI 友好提示

---

## 10. BYOK 部署模型

- 目标形态：练手 + 自己/同学用，小范围，不建中心化后端
- 凭证来源：KuaidiNiaoProvider 运行时从用户设置读取 EBusinessID / APP_KEY
- 存储方式：DataStore + Android Keystore 加密，不落明文 SharedPreferences
- 设置页（新增）：用户粘贴自己的快递鸟密钥、选择 mock / kdniao；未填密钥时强制走 mock，避免空 key 崩溃
- 取件码降级（关键）：个人认证（仅手机号）大概率只拿物流轨迹（1002），pickupCode 可能为 null；此时 UI 提示该密钥暂无取件码权限（需企业认证），仍正常展示轨迹与统计
- 额度：个人免费档每日 500 次轨迹查询，自用小范围足够

---

## 11. 硬边界

- 不从拼多多/京东/顺丰自动拉单号（无消费者开放 API + 隐私号 + 爬取违反 ToS）
- 不逆向菜鸟私有 API；所有数据通过正规聚合 API（快递鸟）获取
- 不做云账号/多设备同步（MVP）
- 不处理京东自营/顺丰直送/多多驿站的取件码（无菜鸟码，只存轨迹）
- 不替代菜鸟取件/身份码，只展示取件码作辅助
- 禁止被动后台监听剪贴板（Android 10+ 必须点浮窗才读）
- 仅申请最小权限：SYSTEM_ALERT_WINDOW + FOREGROUND_SERVICE + INTERNET

---

## 12. MVP 验收标准（最终 Gate）

1. 在拼多多复制一个真实运单号 -> 浮窗捕获 -> 自动查出取件码 -> 列表出现该条
2. 复制一个已到站菜鸟件 -> 列表显示取件码 + 驿站名，status=已到站
3. 统计页能看到：累计数、平台分布、状态分布、近30天趋势、驿站 Top
4. 复制一个京东自营/顺丰直送件 -> 正常存为非菜鸟驿站件，不崩溃
5. 本地数据可导出 CSV
6. 退出 APP 后浮窗仍能在复制时捕获（前台服务常驻）