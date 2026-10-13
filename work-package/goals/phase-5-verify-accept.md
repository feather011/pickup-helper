# Phase 5 — 验证 & 接收

**目标：针对 MockProvider 后端运行完整端到端验证套件，确认所有验收门通过，并生成可签名的 Debug APK 就继。

---

## 要求

1. **端到端测试**（androidTest/）：
   - 启动应用 → 启动 FloatingWindowService → 仿真复制已知模拟运单号到剪贴板 → 点击【查询取件码】 → 验证包裹出现在列表屏幕
   - 至少覆盖：YT001（普通鸟章）、SF002（运输中）、JD004（非鸟章/无取件码）、EMS006（异常）

2. **希等性测试**（androidTest/）：
   - 通过浮窗两次查询同一运单号
   - 验证 parcels 表中只有 1 条记录（SELECT COUNT(*) FROM parcels WHERE waybill_no = ?）

3. **失败分支测试**（androidTest/）：
   - 针对每个 MockProvider 失败用例，验证：
     - parcels 表中未写入任何行
     - 显示用户可视的错误消息（而非崩溃对话框）
     - FailureType 与预期类型匹配

4. **BYOK 流程测试**：
   - 在设置页输入占位符 EBusinessID + APP_KEY → 保存
   - 重启应用 → 验证值仍然存在（EncryptedSharedPreferences / Keystore 读回）
   - 切换提供者到 kdniao → 验证 ProviderConfig.ACTIVE == kdniao`

5. **回归测试**：
   - ./gradlew test 通过（所有单元测试，无跳过）
   - ./gradlew assembleDebug 产生有效 APK

6. **CSV 导出测试**：
   - 插入 3 条已知包裹 → 触发 CSV 导出 → 验证输出文件包含 3 条数据行 + 1 条表头行，列顺序正确

---

## 约束

- C1：所有端到端测试必须针对 MockProvider（ACTIVE=mock）运行 — 无需真实网络
- C2：希等性测试必须使用真实 Room 数据库（单元测试可用内存数据库，但 E2E 必须用 InstrumentationRegistry）
- C3：失败分支测试必须断言 SELECT COUNT(*) FROM parcels 返回 0（失败结果后）
- C4：本阶段不得修改任何生产代码 — 除非测试漏出真实帏洋，此时才修复并添加回归测试

---

## 标达验证 (Acceptance)

从条延件不错的某一节点 12必须通过的 6 个验收门通过：

- [ ] **门 1**：复制模拟普通运单号（YT001） → 点击浮窗 → MockProvider 返回 ARRIVED 并带取件码 → 列表屏幕显示该包裹，包含取件码和站点名称
- [ ] **门 2**：查询一个 ARRIVED 包裹（YT001） → 验证列表项目显示 pickupCode=A-03-1234、station=菁鸟骨站(望京SOHO店)、status=ARRIVED
- [ ] **门 3**：当 DB 中有 10+ 个包裹时，统计仪表盘显示：总数、本月数、待取数、已签收数、平台分布（条形）、状态分布（圆环）、快递员分布（条形）、30天趋势（柱状）、站点前10（排行列表） — 有效数据处显示非零值
- [ ] **门 4**：查询非鸟章运单号（JD004） → 返回 NO_PICKUP_CODE 失败 → 无崩溃 → 显示用户可见提示 该件无菁鸟取件码
- [ ] **门 5**：触发 CSV 导出 → 打开文件 → 验证列：waybill_no, shipper_code, shipper_name, platform, status, pickup_code, station_name, created_at, arrived_at, signed_at — 全部 10+ 列数据如有存在
- [ ] **门 6**：应用处于后台（没有活动可见）且 FloatingWindowService 正在运行时，点击浮窗气泡 → 卡片出现 → 查询正常进行（前台服务协议已验证）

最终交付物：
- [ ] Debug APK 位于 app/build/outputs/apk/debug/app-debug.apk
- [ ] ./gradlew test 所有测试通过
- [ ] 所有交互路径无崩溃报告
