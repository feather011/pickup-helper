# 快递取件码浮窗助手（PickupHelper）

> Android 原生应用：桌面浮窗 + 剪贴板自动识别快递取件码，支持 Mock 与真实快递100 API 自由切换。

## 技术栈

| 层 | 技术 |
|---|---|
| App | Kotlin · Jetpack Compose · MVVM · ViewModel · Room · DataStore |
| 浮窗 | SYSTEM_ALERT_WINDOW · 剪贴板监听 |
| 后端 | Node.js 标准库**零依赖单文件**（替代原 Ktor：因 Gradle 缓存损坏改道，反而更轻、更稳） |
| 数据源 | 快递100 实时查询（免费额度验证）/ Mock |

## 真实性声明（面试如实陈述用）

本仓库对「真」与「演示」做了明确区分，方便如实讲解：

- ✅ **物流轨迹**：已用快递100 实时查询免费额度跑通**真实链路**（极兔单号，状态「已签收」，12 条轨迹），并有单测覆盖映射与兜底逻辑。
- 🟡 **取件码**：目前为 **Mock**。取件码是快递100 独立预付费产品，账号未开通；架构已留 Kuaidi100RealtimeProvider 占位，开通即接。
- 🟡 **顺丰**：真实 API 强制收件手机号，示例未含真实单号，故**未实测**（不在简历谎称已测）。
- 🔒 **密钥安全**：仅在后端环境变量，App 端不持；曾于对话贴出的 key 建议生产前在快递100 后台轮换。

## 安全设计

- 密钥收敛到后端，链路为 App → 后端 → 快递100，App 永不见 key
- Android 9+ 明文 HTTP 经 
etworkSecurityConfig **仅放行 10.0.2.2**（模拟器回环），不全局放开
- 后端响应做 JsonNull 安全解析，避免缺字段导致客户端崩溃

## 快速开始

`ash
# 1. 启动后端（需先设置环境变量）
cd backend-node
 = "你的KEY"
 = "你的CUSTOMER"
node server.js

# 2. App 设置页 → 数据源切「真实API」+ 填后端地址 http://10.0.2.2:8080 + 手机号
# 3. 模拟器中打开 App → 授权悬浮窗 → 浮窗读取剪贴板 / 手动录入单号
`

## 验证证据

- **单元测试**：:app:testDebugUnitTest → **26/26 通过**（含 romChineseName 中文名→编码映射、用户手选 > 前缀推断的兜底优先级）
- **真实链路截图（已签收）**：work-package/test-screens/12_delivered_tab.png
- **失败分支按设计工作**（手选「圆通」查极兔单号 → 无结果 + 不落库）：work-package/test-screens/test1_user_priority_failure.png

## 已知限制 / 路线图

- 取件码接入快递100 真实 API（需开通独立预付费产品）
- 顺丰真实链路实测（需真实单号 + 收件手机号）
- 多账号 / 多包裹批量查询
- 桌面 / 锁屏小组件

## License

MIT（示例项目，可自由借鉴与二次开发）。
