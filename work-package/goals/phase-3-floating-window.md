# Phase 3 — 浮窗

**目标**：实现浮窗服务、可拖动气泡 UI、点击展开的卡片、符合 Android 10 规范的剪贴板读取器、运单号正则提取器以及快递公司代码检测器，所有组件通过 FloatingViewModel 串联。

---

## 要求

1. **FloatingWindowService**（service/）：
   - 继承 ndroid.app.Service
   - 以前台服务方式启动，带有持久通知（“取件码浮窗助手 运行中”）
   - 使用 WindowManager 添加/移除 FloatBallView 和 FloatCardView
   - 重写 onBind() 返回 null
   - 应用进程被杀时自动优雅停止

2. **FloatBallView**（ui/floating/）：
   - 圆形气泡（直径 48dp），半透明，可拖动
   - 默认位置：屏幕右下边缘，距边缘 16dp
   - 单击触发显示 FloatCardView
   - 点击卡片外部区域：收起卡片，恢复仅气泡状态

3. **FloatCardView**（ui/floating/）：
   -  Elevated 卡片（宽 280dp），展示内容：
     - 检测到的运单号末 4 位（如 ****1234）
     - 检测到的快递公司名称（或 未知快递）
     -【查询取件码】主按钮
     -【忽略】次按钮（收起卡片）
   - 未检测到运单号时：显示 未识别到运单号，请复制物流单号后重试
   - 卡片显示在气泡上方；点击【查询取件码】触发 FloatingViewModel

4. **ClipboardHelper**（service/）：
   - un readClipboardOnce(): String? — 仅在主动调用时读取剪贴板文本（无被动监听）
   - 仅从 FloatCardView 的【查询取件码】点击处理器调用，而非剪贴板变更时调用
   - 剪贴板为空或非文本时返回 null

5. **WaybillExtractor**（service/）：
   - un extract(text: String): String? — 对输入文本执行正则 [A-Za-z0-9]{8,30}，返回首个匹配结果，否则返回 null
   - 需有单元测试覆盖：合法、不合法及边界输入

6. **ShipperCodeDetector**（service/）：
   - 委托 Phase 1 的 ShipperCodeMapper
   - un detect(waybillNo: String): String? — 返回检测到的 shipperCode，未知时返回 null

7. **FloatingViewModel**（viewmodel/）：
   - un onQueryClicked()：读取剪贴板 → 提取运单号 → 检测快递公司代码 → 调用 ProviderManager.get().query() → 持久化结果到 DB → 发射 UI 状态
   - UI 状态：Idle、Loading、Success(parcel: Parcel)、Failure(failure: Failure)
   - 使用 StateFlow 实现响应式 UI 更新

8. **持久化集成**（viewmodel/）：
   - 收到 Success：通过 ParcelDao.upsert() 插入/更新 ParcelEntity，然后发射 Success 状态
   - 收到 Failure：不写入 DB，仅发射 Failure 状态

---

## 约束

- C1：ClipboardManager 必须仅在用户主动点击【查询取件码】时访问，禁止 ClipboardListener 或后台轮询
- C2：应用进入后台后，浮窗必须保持可见（前台服务协议）
- C3：WindowManager 布局参数必须使用 TYPE_APPLICATION_OVERLAY（Android 8+），而非 TYPE_SYSTEM_ALERT
- C4：气泡必须可拖动 — 实现 View.OnDragListener 或重写 onTouchEvent 追踪 MotionEvent 增量
- C5：Android 10+ 在后台读取剪贴板需要应用处于前台；确保卡片仅在应用获得焦点时显示

---

## 达标验证 (Acceptance)

- [ ] 前台服务启动时带有不可清除的通知
- [ ] 浮窗气泡出现在屏幕右下角
- [ ] 气泡可拖动，松手后保持在落下位置
- [ ] 点击气泡打开卡片
- [ ] 剪贴板含有合法运单号时点击【查询取件码】 → 调用 ProviderManager.get().query() → 显示加载 → 显示结果
- [ ] 点击【忽略】收起卡片，恢复仅气泡状态
- [ ] 剪贴板无匹配文本时，卡片显示 未识别到运单号 提示
- [ ] WaybillExtractor.extract() 通过：YT12345678 → YT12345678；SF1234567890 → SF1234567890；bc → null；SF1234abc → SF1234；空字符串 → null
- [ ] ShipperCodeDetector.detect(\YT12345678\) 返回 YTO；未知前缀返回 null
- [ ] 所有交互路径均无崩溃（成功、失败、空剪贴板、未知运单号）
- [ ] ./gradlew test 通过（含 WaybillExtractor 和 ShipperCodeDetector 的单元测试）
