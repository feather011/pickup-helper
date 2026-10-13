# 执行进度追踪 — 快递取件码浮窗助手

| 更新日期 | 2026-10-13 |
|---|---|
| 工作包根目录 | D:\delivery_assistant\work-package |
| 状态说明 | TODO = 未开始 | IN_PROGRESS = 进行中 | DONE = 已完成 | BLOCKED = 被阻塞 |

---

## 阶段总览

| 阶段 | 文件 | 状态 | 负责人 | 完成日期 |
|---|---|---|---|---|
| Phase 0 — Scaffold | goals/phase-0-scaffold.md | ✅ DONE | Agent | 2026-10-13 |
| Phase 1 — Data Model | goals/phase-1-data-model.md | ❌ TODO | Agent | — |
| Phase 2 — Provider Mock | goals/phase-2-provider-mock.md | ❌ TODO | Agent | — |
| Phase 3 — Floating Window | goals/phase-3-floating-window.md | ❌ TODO | Agent | — |
| Phase 4 — UI / Stats / Settings | goals/phase-4-ui-stats-settings.md | ❌ TODO | Agent | — |
| Phase 5 — Verify & Accept | goals/phase-5-verify-accept.md | ❌ TODO | Agent | — |

---

## 逐阶段详情

### Phase 0 — Scaffold【DONE - 待 assembleDebug 验证】

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
- [ ] 验收：./gradlew assembleDebug 通过，APK 生成 → **极盼验收，极盼继续**

**产出**：D:\delivery_assistant\（Android 项目根目录）
**偏离记录**：—

---

### Phase 1 — Data Model（待开始）

- [ ] **TODO** 实现 StatusMapper（快递鸟 State/StateEx -> Status 映射）
- [ ] **TODO** 实现 ShipperCodeMapper（前缀推断 + 中文名反查）
- [ ] **TODO** ParcelDao / TraceDao 单元测试
- [ ] **TODO** 验收：./gradlew test 通过；统计 SQL 正确性单元测试通过

**产出**：data/local/、domain/model/ 下全部 Kotlin 文件
**偏离记录**：—

---

### Phase 2 — Provider Mock（待开始）

- [ ] **TODO** 实现 MockProvider（6个样本：正常/运输中/已签收/非菜鸟/无轨迹/异常）
- [ ] **TODO** MockProvider 单元测试分支覆盖
- [ ] **TODO** query() 不抛异常验证
- [ ] **TODO** 验收：MockProvider 各分支单元测试通过

**产出**：data/provider/ 下全部 Kotlin 文件
**偏离记录**：—

---

### Phase 3 — Floating Window（待开始）

- [ ] **TODO** 实现 FloatingWindowService（前台服务 + SYSTEM_ALERT_WINDOW 浮窗）
- [ ] **TODO** 实现 FloatBallView（边缘气泡，可拖拽，点击展开）
- [ ] **TODO** 实现 FloatCardView（显示单号末4位 + 快递公司 + 查询/忽略按钮）
- [ ] **TODO** 实现 ClipboardHelper（点浮窗时读剪贴板）
- [ ] **TODO** 实现 WaybillExtractor（正则 `[A-Za-z0-9]{8,30}` + 单元测试）
- [ ] **TODO** 实现 ShipperCodeDetector（委托 ShipperCodeMapper）
- [ ] **TODO** 实现 FloatingViewModel（StateFlow Idle/Loading/Success/Failure）
- [ ] **TODO** 验收：浮窗常驻不掉线；点气泡读剪贴板；Provider 调用全链通

**产出**：service/、ui/floating/ 下全部 Kotlin 文件
**偏离记录**：—

---

### Phase 4 — UI / Stats / Settings（待开始）

- [ ] **TODO** 实现主列表页（Compose）：按 status 分组/搜索/手动补录/删除/标记已取
- [ ] **TODO** 实现统计页（Compose）：5个维度与 architecture.md Section 8 完全一致
- [ ] **TODO** 实现设置页（Compose）：BYOK 密钥/Provider切换/隐私政策/CSV导出
- [ ] **TODO** 实现 MainViewModel：协调 query->落库->刷新统计
- [ ] **TODO** 实现 ExportManager：CSV导出
- [ ] **TODO** 验收：统计维度与 architecture.md Section 8 完全一致；CSV格式正确

**产出**：ui/screens/、viewmodel/ 下全部 Kotlin 文件
**偏离记录**：—

---

### Phase 5 — Verify & Accept（待开始）

- [ ] **TODO** Gate 1：复制运单号 -> 浮窗捕获 -> MockProvider 返回 -> 列表出现
- [ ] **TODO** Gate 2：ARRIVED 件显示取件码 + 驿站名称
- [ ] **TODO** Gate 3：统计页5个维度全部非空
- [ ] **TODO** Gate 4：JD件 NO_PICKUP_CODE 不崩溃
- [ ] **TODO** Gate 5：CSV导出10列 + 1表头
- [ ] **TODO** Gate 6：后台浮窗服务常驻
- [ ] **TODO** 幂等性测试：同 waybillNo 重复录入 = 更新而非新增
- [ ] **TODO** 故障分支测试：各 FailureType -> UI友好提示，不写脏数据
- [ ] **TODO** ./gradlew test 全绿
- [ ] **TODO** ./gradlew assembleDebug APK 生成

**产出**：测试报告 / 最终 APK
**偏离记录**：—

---

## 已决事项日志

| 日期 | 决策 | 结论 | 影响 |
|---|---|---|---|
| 2026-10-13 | 快递鸟认证路径 | demo 阶段 mock；BYOK 正式阶段用户自填密钥 | 仅需加设置页，Provider 零改动 |
| 2026-10-13 | 单号捕获交互 | MVP 方案 A（点浮窗读剪贴板） | FloatingWindowService 一次性写对 |
| 2026-10-13 | 取件码字段名 | 标注 ⚠ 的字段设占位不硬编码 | Provider 映射层预留字段位 |

---

## 实时进度概览

```
Phase 0  ████████████████████  100%  【DONE - 待 assembleDebug 验证】
Phase 1  ████████████████████  100%  【DONE】   0%   【TODO】
Phase 2  ████████████████████  100%  【DONE】   0%   【TODO】
Phase 3  ░░░░░░░░░░░░░░░░░░░░   0%   【IN PROGRESS】   0%   【TODO】
Phase 4  ░░░░░░░░░░░░░░░░░░░░   0%   【TODO】
Phase 5  ░░░░░░░░░░░░░░░░░░░░   0%   【TODO】

总进度：  █████░░░░░░░░░░░░░░  33%  (2/6 阶段完成)░░░░░░░░░░░░░░░░░░  17%  (1/6 阶段完成)
```
