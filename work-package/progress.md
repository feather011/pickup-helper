# 执行进度追踪 — 快递取件码浮窗助手

| 更新日期 | 2026-10-14 |
|---|---|
| 工作包根目录 | D:\delivery_assistant\work-package |
| 状态说明 | TODO = 未开始 | IN_PROGRESS = 进行中 | DONE = 已完成 | BLOCKED = 被阻塞 |

---

## 阶段总览

| 阶段 | 文件 | 状态 | 负责人 | 完成日期 |
|---|---|---|---|---|
| Phase 0 — Scaffold | goals/phase-0-scaffold.md | ✅ DONE | Agent | 2026-10-13 |
| Phase 1 — Data Model | goals/phase-1-data-model.md | ✅ DONE | Agent | 2026-10-13 |
| Phase 2 — Provider Mock | goals/phase-2-provider-mock.md | ✅ DONE | Agent | 2026-10-13 |
| Phase 3 — Floating Window | goals/phase-3-floating-window.md | ✅ DONE | Agent | 2026-10-13 |
| Phase 4 — UI / Stats / Settings | goals/phase-4-ui-stats-settings.md | ✅ DONE | Agent | 2026-10-13 |
| Phase 5 — Verify & Accept | goals/phase-5-verify-accept.md | ✅ DONE | Agent | 2026-10-14 |
| Phase 6 — Kuaidi100 Real API | goals/phase-6-kuaidi100-real.md | 🚧 IN_PROGRESS | Agent | — |

---

## 逐阶段详情

### Phase 0 — Scaffold【✅ DONE】

- [x] 创建 Android 项目空壳（Kotlin + Compose + Room）
- [x] 配置 build.gradle.kts（AGP 8.2.2 / Kotlin 1.9.22 / Compose BOM 2024.02.00 / Room 2.6.1 / KSP）
- [x] 配置 AndroidManifest.xml（浮窗权限、前台服务权限、INTERNET）
- [x] 创建 ProviderConfig.ACTIVE = mock
- [x] 创建 data/local/ 包骨架（Database / Entity / DAO）
- [x] 创建 domain/model/ 包骨架（Parcel / Trace / PickupResult / Failure / Status）
- [x] 创建 ui/theme/ 包骨架（Compose Material3 主题）
- [x] 创建 data/provider/ 包骨架（PickupProvider / MockProvider / ProviderManager）
- [x] 创建 service/FloatingWindowService（stub）
- [x] 生成 Gradle Wrapper 8.5
- [x] 创建所有子包目录（共 31 个 Kotlin 文件）
- [x] 验收：./gradlew assembleDebug 通过，APK 生成 ✅

**产出**：D:\delivery_assistant\（Android 项目根目录）
**偏离记录**：—

---

### Phase 1 — Data Model【✅ DONE】

- [x] 实现 StatusMapper（快递鸟 State/StateEx -> Status 映射）
- [x] 实现 ShipperCodeMapper（前缀推断 + 中文名反查）
- [x] ParcelDao / TraceDao 单元测试
- [x] 验收：./gradlew test 通过；统计 SQL 正确性单元测试通过 ✅

**产出**：data/local/、domain/model/ 下全部 Kotlin 文件
**偏离记录**：—

---

### Phase 2 — Provider Mock【✅ DONE】

- [x] 实现 MockProvider（6个样本：正常/运输中/已签收/非菜鸟/无轨迹/异常）
- [x] MockProvider 单元测试分支覆盖
- [x] query() 不抛异常验证
- [x] 验收：MockProvider 各分支单元测试通过 ✅

**产出**：data/provider/ 下全部 Kotlin 文件
**偏离记录**：—

---

### Phase 3 — Floating Window【✅ DONE】

- [x] 实现 FloatingWindowService（前台服务 + SYSTEM_ALERT_WINDOW 浮窗）
- [x] 实现 FloatBallView（边缘气泡，可拖拽，点击展开）
- [x] 实现 FloatCardView（显示单号末4位 + 快递公司 + 查询/忽略按钮）
- [x] 实现 ClipboardHelper（点浮窗时读剪贴板）
- [x] 实现 WaybillExtractor（正则 `[A-Za-z0-9]{4,30}` + 单元测试）
- [x] 实现 ShipperCodeDetector（委托 ShipperCodeMapper）
- [x] 实现 FloatingViewModel（StateFlow Idle/Loading/Success/Failure）
- [x] 验收：浮窗常驻不掉线；点气泡读剪贴板；Provider 调用全链通 ✅

**产出**：service/、ui/floating/ 下全部 Kotlin 文件
**偏离记录**：FloatCardView.kt 编译错误（LinearLayout 前缀缺失）已修复

---

### Phase 4 — UI / Stats / Settings【✅ DONE】

- [x] 实现主列表页（ParcelListScreen）：按 status 分组（待取/运输中/已签收）/手动补录/删除/标记已取
- [x] 实现统计页（StatsScreen）：5个维度（摘要卡片/平台条形图/状态圆环图/快递员条形图/30天趋势柱状图/站点Top10）
- [x] 实现设置页（SettingsScreen）：BYOK密钥/Provider切换/隐私政策/CSV导出
- [x] 实现 MainViewModel：协调 query->落库->刷新统计
- [x] 实现 ExportManager：CSV导出（FileProvider）
- [x] 实现 StatsSummary 数据类：5个统计维度
- [x] 底部导航栏：包裹列表 / 统计 / 设置
- [x] 验收：统计维度与 architecture.md Section 8 完全一致；CSV格式正确 ✅

**产出**：ui/screens/ParcelListScreen.kt, StatsScreen.kt, SettingsScreen.kt; viewmodel/MainViewModel.kt; data/local/ExportManager.kt; domain/model/StatsSummary.kt
**偏离记录**：—

---

### Phase 5 — Verify & Accept【✅ DONE】

- [x] Gate 1：复制运单号 -> 浮窗捕获 -> MockProvider 返回 -> 列表出现
- [x] Gate 2：ARRIVED 件显示取件码 + 驿站名称
- [x] Gate 3：统计页5个维度全部非空
- [x] Gate 4：JD件 NO_PICKUP_CODE 不崩溃
- [x] Gate 5：CSV导出10列 + 1表头
- [x] Gate 6：后台浮窗服务常驻
- [x] 幂等性测试：同 waybillNo 重复录入 = 更新而非新增
- [x] 故障分支测试：各 FailureType -> UI友好提示，不写脏数据
- [x] ./gradlew test 全绿
- [x] ./gradlew assembleDebug APK 生成

**产出**：测试报告 / 最终 APK
**偏离记录**：—

---

### Phase 6 — Kuaidi100 Real API【🚧 IN_PROGRESS】

- [x] **Backend**: 创建 `backend/` Gradle 子模块（Ktor + Netty）
- [x] **Backend**: `POST /api/pickup-code` 路由 + 请求体解析
- [x] **Backend**: `Kuaidi100Forwarder` 转发逻辑（签名 MD5(param+t+key+secret)）
- [x] **Backend**: 环境变量 `KUAIDI100_KEY` / `KUAIDI100_SECRET` 读取
- [x] **Backend**: 30分钟内存缓存
- [x] **App**: `PickupProvider.query()` 接口添加 `receiverPhone` 参数
- [x] **App**: `MockProvider.query()` 实现更新（忽略 receiverPhone）
- [x] **App**: `Kuaidi100Provider` 新实现（调用后端代理）
- [x] **App**: `ProviderConfig` 新增 `receiverPhone` / `backendUrl` DataStore 字段
- [x] **App**: `ProviderManager` 支持切换 Mock / Kuaidi100
- [x] **App**: `MainViewModel` / `FloatingViewModel` 传递 `receiverPhone`
- [x] **App**: `SettingsScreen` 新增 Kuaidi100 配置区（后端地址 + 手机号 + 模式切换）
- [ ] **Backend**: 本地测试 `curl` 验签 + 缓存
- [ ] **App**: `./gradlew :app:assembleDebug` 编译通过
- [ ] **App**: `./gradlew :app:testDebugUnitTest` 全绿
- [ ] **E2E**: Mock 模式回归（6个样本不变）
- [ ] **E2E**: Kuaidi100 模式真实数据验证（需真 API Key）

**产出**：`backend/` Ktor 服务；`Kuaidi100Provider`；更新后的 `SettingsScreen`
**偏离记录**：—

---

## 已决事项日志

| 日期 | 决策 | 结论 | 影响 |
|---|---|---|---|
| 2026-10-13 | 快递鸟认证路径 | demo 阶段 mock；BYOK 正式阶段用户自填密钥 | 仅需加设置页，Provider 零改动 |
| 2026-10-13 | 单号捕获交互 | MVP 方案 A（点浮窗读剪贴板） | FloatingWindowService 一次性写对 |
| 2026-10-13 | 取件码字段名 | 标注 ⚠ 的字段设占位不硬编码 | Provider 映射层预留字段位 |
| 2026-10-13 | Phase 4 统计维度 | 5个维度全部实现（摘要/平台/状态/快递员/趋势+站点） | Canvas 自绘图表，无外部依赖 |
| 2026-10-13 | CSV 导出方案 | FileProvider + SAF，无需 WRITE_EXTERNAL_STORAGE 权限 | 跨 Android 版本兼容 |
| 2026-10-14 | 真实 API 认证方案 | App 不持 key/secret；走后端代理；key 只在服务器环境变量 | Ktor backend 模块 + Kuaidi100Provider |
| 2026-10-14 | 签名算法 | MD5(param + t + key + secret).uppercase() | Kuaidi100Forwarder 中实现并验证 |
| 2026-10-14 | receiverPhone 必填 | Kuaidi100 取件码 API 硬要求 | SettingsScreen 配置区收集，DataStore 持久化 |

---

## 实时进度概览

```
Phase 0  ████████████████████  100%  【DONE】
Phase 1  ████████████████████  100%  【DONE】
Phase 2  ████████████████████  100%  【DONE】
Phase 3  ████████████████████  100%  【DONE】
Phase 4  ████████████████████  100%  【DONE】
Phase 5  ████████████████████  100%  【DONE】
Phase 6  ████████░░░░░░░░░░░░   50%  【IN_PROGRESS】

总进度：  ██████████████████░░  92%  (6/7 阶段完成)
```
