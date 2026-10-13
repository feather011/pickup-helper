# Phase 1 — 数据模型 (Data Model)

**目标**: 完成规范域模型、Room 实体、DAOs（包含全部统计聚合查询）以及快递公司代码前缀推断器，所有资源通过单元测试。

---

## 要求

1. **领域模型** (domain/model/)：
   - `Parcel`：数据类，完与 architecture.md 第 2.1 节完全一致（包含所有字段）
   - `Trace`：数据类，完与 architecture.md 第 2.2 节
   - `PickupResult`：数据类，完与 architecture.md 第 2.3 节（parcel+traces 或 failure 两者之一有值，绝不同时有值）
   - `Failure`：数据类，完与 architecture.md 第 2.4 节
   - `Status`：枚举，值为 NO_TRACE=0、COLLECTED=1、IN_TRANSIT=2、ARRIVED=3、SIGNED=4、EXCEPTION=5

2. **Room 实体** (data/local/)：
   - `ParcelEntity`：Room @Entity，tableName=parcels，id=autoGenerate，waybillNo=唯一紧引
   - `TraceEntity`：Room @Entity，tableName=traces，id=autoGenerate，waybillNo=FK 指向 parcels
   - 两者均必须有一个映射函数，用于领域模型互相转换

3. **DAOs** (data/local/)：
   - `ParcelDao`。必需的方法：
     - `upsert(parcel: ParcelEntity)` — INSERT OR REPLACE（専等去重）
     - `getByWaybillNo(waybillNo: String): ParcelEntity?`
     - `getAll(): Flow<List<ParcelEntity>>`
     - `getByStatus(status: Int): Flow<List<ParcelEntity>>`
     - `updateStatus(waybillNo: String, status: Int, updatedAt: Long)`
     - `markPicked(waybillNo: String, pickedAt: Long)`
     - `getCountTotal(): Int`
     - `getCountThisMonth(since: Long): Int`
     - `getCountArrivedNotPicked(): Int`
     - `getCountSigned(): Int`
     - `getGroupByShipper(): List<Pair<String, Int>>`
     - `getGroupByStatus(): List<Pair<Int, Int>>`
     - `getGroupByPlatform(): List<Pair<String?, Int>>`
     - `getDailyTrendSince(since: Long): List<Pair<String, Int>>`（日期字符 -> 计数）
     - `getTopStations(limit: Int): List<Pair<String, Int>>`
   - `TraceDao`。方法：
     - `insertAll(traces: List<TraceEntity>)`
     - `getByWaybillNo(waybillNo: String): List<TraceEntity>`
     - `deleteByWaybillNo(waybillNo: String)`

4. **AppDatabase**：Room `@Database`注解，entities=[ParcelEntity::class, TraceEntity::class]，version=1，exportSchema=false。提供 `getInstance(context)` 单例方法。

5. **StatusMapper**：将快递鸟 `State` 和 `StateEx`整数映射为 `Status`枚举值，按 architecture.md 第 3 节。

6. **ShipperCodeMapper**：基于前缀的推断（例如 YT -> YTO、SF -> SF）和中文名反查，按 architecture.md 第 7.2 节。

---

## 约束

- C1: `upsert` 必须使用 INSERT OR REPLACE，以保证同一 waybillNo 重复查询更新而非重复插入（H10）
- C2: 所有统计查询必须使用原生 SQL 或 Room `@Query` — 不允许接收后内存过滤
- C3: `PickupResult`构造函数必须强制执行：parcel 和 failure 之间有且仅有一个非 null
- C4: 所有返回 Flow 的 DAO 方法不得在主线程调用（使用 `@Dao` + suspend 或 `withContext(Dispatchers.IO)`）

---

## 标达验证 (Acceptance)

- [ ] 所有领域模型 `Parcel`,、`Trace`,、`PickupResult`,、`Failure`,、`Status`编译通过且通过结构相等测试
- [ ] `ParcelEntity` 和 `TraceEntity`双向映射至/从领域模型正确
- [ ] `AppDatabase.getInstance(context)`调用多次返回同一实例（单例）
- [ ] `upsert(parcel)`对同一 waybillNo 调用两次后，DB 中仅有 1 行记录（封等性）
- [ ] 每个统计 DAO 查询在已知内存数据库上返回正确结果（单元测试）：
  - 总数计数与预期值匹配
  - 本月计数按时间戳过滤
  - `getGroupByShipper` 按 shipperName 分组
  - `getDailyTrendSince` 返回日期按升序
  - `getTopStations(10)` 返回至多 10 行，按计数降序
- [ ] `StatusMapper`将 KdNiao State=211 映射为 Status.ARRIVED
- [ ] `ShipperCodeMapper.detect(`YT12345678`)` 返回 `YTO`；`toChineseName(`SF`)` 返回 `顺丰`
- [ ] 所有测试通过：./gradlew test（无跳过测试）
