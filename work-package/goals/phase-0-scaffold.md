# Phase 0 — 项目脚手架空 (Scaffold)

**目标**: 建立一个可运行的 Android 项目空壳，使用 Kotlin + Jetpack Compose + Room，所有包骨架屇就位，并生成一个 assembleDebug 构建成功的 APK。

---

## 要求

1. **项目结构**（位于 Android 项目根目录）：
   - app/src/main/java/com/pickuphelper/ 作为根包
   - 子包：data/local/,、data/provider/,、domain/model/,、ui/theme/,、ui/screens/,、service/,、viewmodel/
2. **Gradle 配置** (app/build.gradle.kts)：
   - Android Gradle Plugin (AGP) 8.x
   - Kotlin 1.9.x
   - Jetpack Compose BOM（最新稳定版）
   - Room 2.6.x 配合 KSP
   - Kotlin Coroutines
   - minSdk = 26（Android 8.0），targetSdk = 34
3. **AndroidManifest.xml**：
   - SYSTEM_ALERT_WINDOW 权限（浮窗）
   - FOREGROUND_SERVICE 权限（前台服务保活）
   - FOREGROUND_SERVICE_TYPE = shortService
   - INTERNET 权限
   - 一个 FloatingWindowService <service> 声明（exported=false）
4. **ProviderConfig**：ACTIVE = mock 确为默认值，运行时覆盖通过 DataStore（在 Phase 4 实现）
5. **空壳类**（所有骨架类）：
   - Parcel、Trace、PickupResult、Failure、Status（domain/model）
   - ParcelEntity、TraceEntity、ParcelDao、AppDatabase（data/local）
   - PickupProvider、MockProvider、ProviderManager（data/provider）
   - Compose Material3 主题（ColorScheme、Typography、Theme）（ui/theme/）
6. **隐私声明**：简单的字符串资源，声明运单号将被发送至快递鸟 API 进行查询，仅本地存储

---

## 约束

- C1: 本阶段不存在真实 API 调用 — 所有内容均为 stub/mock
- C2: 不得请求以上权限列表以外的权限
- C3: FloatingWindowService stub 必须在 manifest 中注册，但类体可为空操作
- C4: 所有 Room 实体使用 @PrimaryKey(autoGenerate = true) 实作 id，使用 @Index(unique=true) 实作 waybillNo
- C5: Compose 主题必须使用 Material3，不是 Material2

---

## 标达验证 (Acceptance)

- [ ] ./gradlew assembleDebug 完成无错，并在 app/build/outputs/apk/debug/ 生成调试 APK
- [ ] 所有子包存在，每个包至少有一个 .kt 文件
- [ ] AndroidManifest.xml 声明所有权限和 FloatingWindowService
- [ ] ProviderConfig.ACTIVE 默认值为 mock
- [ ] 新安装冷启后无运行时崩溃
