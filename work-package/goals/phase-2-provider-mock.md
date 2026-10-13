# Phase 2 — Provider Mock 实现 (Provider Mock Implementation)

**目标**: 实现 `PickupProvider` 接口、`MockProvider` （6种样本场景）、`ProviderConfig` 运行时切换、以及 `ProviderManager` 路由到活跃 Provider 的道路，所有资源通过单元测试。

---

## 要求

1. **PickupProvider 接口** (data/provider/)：
   - `suspend fun query(waybillNo: String, shipperCode: String?): PickupResult`
   - 必须为非s随（suspend 函数）
   - 不得抬出任何异常 — 所有失败情况返回 `PickupResult(failure=Failure(...))`

2. **MockProvider** (data/provider/)：
   - 实现 `PickupProvider`
   - 包含 6 个确定的样本场景，按 waybillNo 后缀模式匹配：
     - `YT001` -> ARRIVED，pickupCode=`A-03-1234`，station=`菜鸟骨站(望京SOHO店)` （正常菜鸟包裹）
     - `SF002` -> IN_TRANSIT，pickupCode=null，station=null（运输途中，暂无取件码）
     - `ZTO003` -> SIGNED，pickupCode=`B-01-5678`，station=`菜鸟骨站(中关村店)` （已签收）
     - `JD004` -> ARRIVED，pickupCode=null，station=null（非菜鸟骨站，无取件码）
     - `YD005` -> NO_TRACE，pickupCode=null，station=null（暂无观念记录）
     - `EMS006` -> EXCEPTION，pickupCode=null，station=null（异常包裹）
   - 未知 waybillNo 模式回退到 NO_TRACE
   - 返回前使用 `delay()` （100-300ms 模拟网络延迟）
   - 成功时返回 `PickupResult(parcel=Parcel(...), traces=[...])`
   - 失败时返回 `PickupResult(failure=Failure(...))`

3. **FailureType 枚举** (domain/model/)：
   - NETWORK_ERROR, TIMEOUT, NO_TRACE, NO_PICKUP_CODE, PERMISSION_DENIED, INVALID_WAYBILL, UNKNOWN

4. **ProviderConfig** (data/provider/)：
   - 从 DataStore (`Preferences DataStore`) 读取 `active_provider`
   - 如未设置，默认返回 `mock`
   - 提供 `setActive(provider: String)` 方法（给 Phase 4 设置页调用）

5. **ProviderManager** (data/provider/)：
   - `fun get(): PickupProvider` — ACTIVE=mock 时返回 `MockProvider()`，ACTIVE=kdniao 时返回 `KuaidiNiaoProvider()`
   - 未来新增 Provider 只需在此添加一个 `when` 分支

---

## 约束

- C1: `MockProvider.query()` 不得抬出任何异常 — 即使输入无效也返回 `Failure`
- C2: `ProviderManager.get()` 不得返回 null
- C3: 本阶段不实现 `KuaidiNiaoProvider` — 只使用 Phase 0 的空壳
- C4: `MockProvider` 必须使用 `kotlinx.coroutines.delay` （不是 `Thread.sleep`）

---

## 标达验证 (Acceptance)

- [ ] `MockProvider().query(`YT001`, `YTO`)` 返回 `PickupResult`，`parcel.pickupCode == `A-03-1234`` ，并且 `parcel.status == Status.ARRIVED`
- [ ] `MockProvider().query(`SF002`, `SF`)` 返回 `PickupResult`，`parcel.pickupCode == null`，并且 `parcel.status == Status.IN_TRANSIT`
- [ ] `MockProvider().query(`JD004`, `JD`)` 返回 `PickupResult`，`failure?.type == FailureType.NO_PICKUP_CODE`
- [ ] `MockProvider().query(`EMS006`, `EMS`)` 返回 `PickupResult`，`failure?.type == FailureType.UNKNOWN`
- [ ] `MockProvider().query(`UNKNOWN_WAYBILL`, null)` 返回 `PickupResult`，`failure?.type == FailureType.NO_TRACE` （回退情况）
- [ ] 6 个 MockProvider 分支全部被单元测试覆盖（无跳过的 100% 分支覆盖率）
- [ ] `ProviderManager.get()` 当 ACTIVE=mock 时返回 `MockProvider` 实例
- [ ] `ProviderManager.get()` 当 ACTIVE=kdniao 时返回 `KuaidiNiaoProvider` （空壳即可）
- [ ] ./gradlew test 通过，无跳过测试
