# Phase 4 — UI / 统计 / 设置

**目标：实现三个主页面（包裹列表、统计仪表盘、设置），以及统筹查询到持久化流程的 MainViewModel，添加 CSV 导出。

---

## 要求

1. **包裹列表页**（ui/screens/）：
   - 从 ParcelDao.getAll() 读取所有包裹，以 LazyColumn 展示
   - 分组为三个标签/区块：待取件（ARRIVED + 未签收）、运输中（IN_TRANSIT、COLLECTED、NO_TRACE）、已签收（SIGNED）
   - 每条记录显示：运单号（隐蔽：前4位+*丫*+后4位）、快递公司名称、取件码（有的话）、站点名称（有的话）、根据导入时间格式化
   - 页面操作：屏哨则删除（调用 ParcelDao.deleteByWaybillNo）；点击展开诠明细节（最新运输记录、全部运输记录）
   - 底部操作条：【手动录入】按钮 → 对话框调试贴运单号并可选择平台
   - 对于 status=ARRIVED 的项目，有【标记已取】按钮（调用 ParcelDao.markPicked）

2. **统计仪表盘页**（ui/screens/）：
   - **摘要卡片行**：总数 / 本月数 / 待取数（ARRIVED 未取） / 已签收数
   - **平台分布**：水平条形图 — 按平台分组（taobao/pdd/jd/sf/other/null）
   - **状态分布**：圆环/饼图 — 按 Status 枚举分组
   - **快递员分布**：水平条形图 — 按快递员名称分组
   - **30天趋势**：柱状图 — 最近30天的每日数据，X轴为日期
   - **站点前10**：排行列表 — 站点名称 + 计数
   - 所有图表通过 Canvas 或轻量级 Compose 图表库（例如 Vico）绘制；无外部图表服务调用
   - 所有数据来自 ParcelDao 统计查询 — 本屏广无网络调用
   - 空状态：显示插略图 + 暂无数据，开始录入第一个包裹吧

3. **设置页**（ui/screens/）：
   - **BYOK 密钥输入区**：
     - EBusinessID 文本输入框
     - APP_KEY 文本输入框（隐藏输入）
     - 【保存密钥】按钮 → 写入 DataStore + Android Keystore
   - **提供者切换**：单选组 模拟 (当前) / 快递鸟真实API — 切换 ProviderConfig.setActive()
   - **隐私公告**：可折叠区块显示隐私政策文本
   - **导出**：【导出CSV】按钮 → 调用 ExportManager.exportToCsv(context) → 分享 Intent

4. **MainViewModel**（viewmodel/）：
   - al parcels: StateFlow<List<ParcelEntity>> — 观涉 ParcelDao.getAll()
   - al stats: StateFlow<StatsSummary> — 从 DAO 查询计算
   - un refreshStats()：重新查询所有统计 DAO 并更新 stats StateFlow
   - un upsertParcel(parcel: Parcel)：调用 ParcelDao.upsert() 然后 
efreshStats()
   - un deleteParcel(waybillNo: String)：调用 ParcelDao.deleteByWaybillNo() 然后 
efreshStats()
   - un markPicked(waybillNo: String)：调用 ParcelDao.markPicked() 然后 
efreshStats()

5. **ExportManager**（data/local/）：
   - suspend fun exportToCsv(context: Context): Uri?
   - 导出所有包裹到 CSV 文件：waybill_no, shipper_code, shipper_name, platform, status, pickup_code, station_name, created_at, arrived_at, signed_at
   - 使用 Context.createFileDocumentIntent（存储访问框架）保持跨 Android 版本兼容性
   - 返回创建文件的 Uri，用于分享 Intent

6. **导航**：
   - 底部导航栏，3 个项：【包裹列表】、【统计】、【设置】
   - 使用 Compose Navigation 或简单 when + setState 模式

---

## 约束

- C1：所有 DAO 调用必须是 suspend 函数，从 iewModelScope 调用（ViewModel + iewModelScope.launch / launchIn 来自 Compose）
- C2：统计仪表盘必须不进行任何网络调用 — 所有数据来自本地 DB 聚合
- C3：BYOK 密钥必须使用 EncryptedSharedPreferences 或 Android Keystore 存储 — 不能用纯文本 SharedPreferences
- C4：CSV 导出必须使用存储访问框架（无需 WRITE_EXTERNAL_STORAGE 权限）
- C5：StatsSummary 数据类必须包含条延件不错的所有5个统计维度（条延件不错所有统计维度）

---

## 标达验证 (Acceptance)

- [ ] 包裹列表显示所有包裹，按状态正确分组，卡片内容正确
- [ ] 点击【手动录入】并输入合法运单号 → 插入新包裹（upsert）
- [ ] 屏哨则删除从 DB 和列表中移除项目
- [ ] 【标记已取】更新 signedAt，项目移动到 已签收 区块
- [ ] 统计仪表盘显示所有5个维度：摘要卡片、平台条形图、状态圆环图、快递员条形图、30天趋势柱状图、站点前10排行列表 — 有效数据处显示非零值
- [ ] 数据库为空时显示空状态插略图（无崩溃，无空白屏）
- [ ] 设置页正确保存和读取 EBusinessID 和 APP_KEY（应用重启后保持）
- [ ] 提供者切换在 Mock 和 KdNiao 之间切换（仅影响未来查询）
- [ ] 【导出CSV】创建合法 CSV 文件并打开分享表单
- [ ] 底部导航在 3 个屏幕之间切换无状态丢失
- [ ] ./gradlew test 通过
