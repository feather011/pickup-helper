# 快递取件码浮窗助手 - Codex 执行代理协议
> 版本：v1.0 | 生效日期：2026-10-13 | 单一事实来源（SSOT）：architecture.md
> 本文件是 Codex Agent 的执行契约。执行前必须通读 architecture.md，然后按 goals/phase-N.md 顺序逐阶段展开。

---

## 1. 角色定义

**执行 Agent**：你是此项目的唯一执行者，负责：
- 理解并遵守本文档所有约束
- 按阶段顺序执行 goals/phase-0.md -> goals/phase-5.md
- 每次行动前引用 architecture.md 作为唯一事实来源
- 遇到模糊点先查 architecture.md，再查本文档，绝不凭记忆或假设行动
- 失败时优雅降级（不崩溃），记录在 progress.md 偏离栏

---

## 2. 执行顺序

Step 0: 读取 architecture.md（全文，精读）
Step 1: 执行 goals/phase-0.md（scaffold）
Step 2: 执行 goals/phase-1.md（data-model）
Step 3: 执行 goals/phase-2.md（provider-mock）
Step 4: 执行 goals/phase-3.md（floating-window）
Step 5: 执行 goals/phase-4.md（ui-stats-settings）
Step 6: 执行 goals/phase-5.md（verify-accept）

每个阶段完成后更新 progress.md 对应行。

---

## 3. 硬约束清单（不可绕过）

| # | 约束 | 说明 |
|---|---|---|
| H1 | 原生 Android / Kotlin + Jetpack Compose + Room | 不使用跨平台框架（Flutter/React Native 等） |
| H2 | Mock-First，ACTIVE = mock | 初始阶段强制 MockProvider，真实 API 接入须经 phase-5 验收 |
| H3 | 契约优先 + Provider 抽象 | App 内部只认规范模型（Parcel/Trace/PickupResult/Failure/Status），真实 API 在 Provider 内部映射 |
| H4 | 无云服务 | 不做多设备同步、不建中心化后端；数据仅存本地 |
| H5 | BYOK 密钥运行时从设置读取 | EBusinessID / APP_KEY 存于 DataStore + Android Keystore，不写死在代码里 |
| H6 | Android 10+ 必须点浮窗才读剪贴板 | 禁止被动后台监听剪贴板；用户点浮窗气泡才读 ClipboardManager |
| H7 | 取件码字段名未定，只留映射位 | 快递鸟取件码字段标注 的，Provider 内部映射时不硬编码字段名，设占位待上线前对齐 |
| H8 | 失败不崩溃 | 任何 Provider 调用失败（网络/超时/无取件码权限等）均返回 Failure，UI 友好提示，不写脏数据 |
| H9 | 仅申请最小权限 | SYSTEM_ALERT_WINDOW（浮窗）、FOREGROUND_SERVICE（前台服务保活）、INTERNET；不申请短信/通讯录/定位等无关权限 |
| H10 | 去重幂等 | 同一 waybillNo 重复录入 = 更新（UPSERT），不重复创建记录 |

---

## 4. 规范状态码枚举（Status）

枚举定义在 architecture.md Section 3。代码中使用枚举名，不使用原始整数：

`kotlin
enum class Status {
    NO_TRACE,    // 0 暂无轨迹
    COLLECTED,   // 1 已揽收
    IN_TRANSIT,  // 2 在途中
    ARRIVED,     // 3 已入库/入柜 -> 触发取件码展示
    SIGNED,      // 4 已签收
    EXCEPTION    // 5 问题件/异常
}
`

状态码含义：
- status == ARRIVED && pickupCode != null：UI 高亮待取件
- status == ARRIVED && pickupCode == null：非菜鸟驿站件，提示该件无菜鸟取件码

---

## 5. 逐阶段 Unfold 执行协议

### Phase 0 - Scaffold
- 创建 Android 项目空壳（Kotlin + Compose + Room）
- 配置 build.gradle.kts（AGP / Kotlin / Compose / Room 版本）
- 配置 AndroidManifest.xml（浮窗权限、前台服务权限、隐私政策声明）
- 创建 ProviderConfig.ACTIVE = mock
- 创建 data/local/ 包骨架（含 Database / Entity / DAO）
- 创建 domain/model/ 包骨架（含 Parcel / Trace / PickupResult / Failure / Status）
- 创建 ui/theme/ 包骨架（Compose Material3 主题）
- 验收：项目 assembleDebug 通过，APK 生成

### Phase 1 - Data Model
- 实现 ParcelEntity / TraceEntity（Room Entity，waybillNo 唯一索引）
- 实现 Parcel / Trace / PickupResult / Failure 规范模型（data class）
- 实现 Status 枚举与 StatusMapper（快递鸟 State/StateEx -> Status）
- 实现 ParcelDao（含 CRUD + 统计聚合 SQL）
- 实现 AppDatabase（Room，单例）
- 实现 ShipperCodeMapper（前缀推断 + 反查中文名）
- 验收：./gradlew test 通过；统计 SQL 正确性单元测试通过

### Phase 2 - Provider Mock
- 定义 PickupProvider 接口（query(waybillNo: String, shipperCode: String?): PickupResult）
- 实现 MockProvider：返回正常样本 + 各故障样本（网络超时/无轨迹/无取件码/非菜鸟件）
- 实现 ProviderConfig：运行时从 DataStore 读取 ACTIVE（mock/kdniao）
- 实现 ProviderManager：根据 ACTIVE 返回对应 Provider 实例
- 验收：MockProvider 各分支单元测试通过；query() 无论成功/失败均不抛异常

### Phase 3 - Floating Window
- 实现 FloatingWindowService（前台服务 + SYSTEM_ALERT_WINDOW 浮窗）
- 实现 FloatBallView：边缘气泡，可拖拽，点击展开卡片
- 实现 FloatCardView：显示单号后4位 + 快递公司 + 一键查询/忽略按钮
- 实现 ClipboardHelper：点浮窗时读剪贴板（Android 10+ 合规）；正则提取运单号（8-30位字母数字）
- 实现 ShipperCodeDetector：运单号前缀 -> shipperCode 推断
- 实现 FloatingViewModel：协调捕获->查询->结果的业务流
- 验收：浮窗常驻不掉线；点气泡读剪贴板；提取到单号后调用 Provider；无崩溃分支

### Phase 4 - UI / Stats / Settings
- 实现主列表页（Compose）：按 status 分组（待取/运输中/已签收）；支持搜索、手动补录、删除、标记已取
- 实现统计页（Compose）：总览卡片 + 平台分布（饼图）+ 状态分布（环形）+ 近30天趋势（柱状）+ 驿站 Top
- 实现设置页（Compose）：BYOK 密钥输入（EBusinessID / APP_KEY）；ACTIVE 切换（mock/kdniao）；隐私政策；CSV 导出
- 实现 MainViewModel：协调 query->落库->刷新列表+重算统计
- 实现 ExportManager：CSV 导出（用户主动触发）
- 验收：统计维度与 architecture.md Section 8 完全一致；CSV 导出格式正确

### Phase 5 - Verify & Accept
- 端到端测试：复制运单号 -> 点浮窗 -> MockProvider 返回 -> 列表出现记录 -> 统计 +1
- 重复录入幂等测试：同 waybillNo 再次录入 = 更新而非新增
- 故障分支测试：MockProvider 返回各 Failure 类型 -> UI 友好提示，不写脏数据
- 回归测试：全部 ./gradlew test 通过
- 验收 gate（6条）：见 architecture.md Section 11（可行性计划）与 goals/phase-5.md Acceptance

---

## 6. Definition of Done（每阶段必须满足）

| 阶段 | DoD |
|---|---|
| Phase 0 | assembleDebug 通过，APK 生成，包骨架齐全 |
| Phase 1 | 所有 Entity/Model/DAO/Mapper 单元测试通过，统计 SQL 验证正确 |
| Phase 2 | PickupProvider 接口各实现可切换，MockProvider 分支全覆盖 |
| Phase 3 | 浮窗服务常驻、点气泡读剪贴板、正则提取、Provider 调用全链路通 |
| Phase 4 | 列表/统计/设置三页功能完整，统计维度与 spec 一致 |
| Phase 5 | 端到端 mock 全通；重复录入幂等；故障分支不崩；./gradlew test 全绿 |

---

## 7. 已决关键决策（存档）

| 决策 | 结论 | 依据 |
|---|---|---|
| 快递鸟认证路径 | demo 阶段用 mock；BYOK 正式阶段用户自填密钥 | 可行性计划 Section 6 决策 A |
| 单号捕获交互 | MVP 方案 A（点浮窗读剪贴板）；不被动监听 | 可行性计划 Section 2.5 |
| 取件码字段名 | 标注 的字段，Provider 内映射时设占位不硬编码 | 架构规格 Section 4 |
| 无云同步 | 仅本地 SQLite，数据不过期不绑死账号 | 产品 PRD Section 1 |
| 失败处理策略 | 返回 Failure 对象，UI 提示，不抛异常不写脏数据 | 架构规格 Section 6 |

---

本文档由 Codex Agent 执行时必须首先读取。与 architecture.md 冲突处，以 architecture.md 为准。