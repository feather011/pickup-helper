# Phase 6: Kuaidi100 Real API Integration

## Overview

Connect the app to real Kuaidi100 API via a self-hosted Ktor proxy backend, replacing Mock mode with production data.

---

## Requirements

### Backend Proxy (Ktor)

- **`POST /api/pickup-code`**: Accepts JSON body `{waybillNo, shipperCode?, receiverPhone?}`, forwards to Kuaidi100 API, returns raw response.
- **Authentication**: `key` and `secret` read from environment variables (`KUAIDI100_KEY`, `KUAIDI100_SECRET`). **Never hardcoded or logged.**
- **Signature**: `MD5(param + t + key + secret).uppercase()` — verified correct order.
- **Caching**: In-memory cache (30 min TTL) to reduce API calls (free tier: 100 req/day).
- **Platform**: Ktor + Netty, runs on port 8080. `gradlew :backend:run` to start.
- **Error handling**: Returns `{"success": false, "message": "..."}` on any failure; never crash.

### App Integration

- **`PickupProvider.query(waybillNo, shipperCode, receiverPhone)`**: Interface updated with `receiverPhone` param (required by Kuaidi100 pickup-code API).
- **`Kuaidi100Provider`**: New Provider implementation that calls the backend proxy (App does NOT hold key/secret).
- **`ProviderConfig`**: Added `receiverPhone`, `backendUrl` fields stored in DataStore.
- **`ProviderManager`**: Switches between `MockProvider` and `Kuaidi100Provider` based on `ACTIVE` config.
- **`SettingsScreen`**: New UI section for:
  - Backend URL input (`http://<server>:8080`)
  - Receiver phone input (required for pickup-code API)
  - Data source mode toggle: Mock / Kuaidi100
  - Legacy EBusinessID/APP_KEY fields (kept for backward compat)
- **BYOK**: Backend URL and phone stored at runtime in DataStore (not hardcoded).
- **Downgrade**: If `backendUrl` is not configured, `Kuaidi100Provider` returns `CONFIG_ERROR` failure with friendly message.

---

## Constraints

- **No key in App**: App never holds `KUAIDI100_KEY` / `KUAIDI100_SECRET`; they live in backend environment variables only.
- **No direct Kuaidi100 access from App**: All real API calls go through the backend proxy.
- **Mock-first unchanged**: `ProviderConfig.ACTIVE = "mock"` remains the default; real API only activates when user explicitly switches.
- **receiverPhone required**: Kuaidi100 pickup-code API requires receiver phone number; App prompts user to fill it in settings.
- **No cloud account / multi-device sync** (MVP scope).
- **Failure does not crash**: Network errors / API errors return `PickupResult.failure`; UI shows friendly message; no force close.

---

## Acceptance (Goal)

### Backend

- [ ] `backend/` module builds: `gradlew :backend:build`
- [ ] `gradlew :backend:run` starts server on port 8080
- [ ] `curl -X POST http://localhost:8080/api/pickup-code -H "Content-Type: application/json" -d '{"waybillNo":"YT001","shipperCode":"YTO","receiverPhone":"13800138000"}'` returns JSON (or error if `KUAIDI100_KEY`/`KUAIDI100_SECRET` env vars not set)
- [ ] Signature calculation: `MD5(param + t + key + secret).uppercase()` matches Kuaidi100 spec
- [ ] Cache works: Same waybill queried twice within 30 min hits cache (log shows `[Cache HIT]`)

### App Integration

- [ ] `PickupProvider.query()` interface has `receiverPhone: String? = null` parameter
- [ ] `MockProvider.query()` accepts `receiverPhone` but ignores it
- [ ] `Kuaidi100Provider` calls `POST <backendUrl>/api/pickup-code` with JSON body
- [ ] `ProviderConfig` stores `receiverPhone` and `backendUrl` in DataStore
- [ ] Settings screen shows "Kuaidi100 真实 API 配置" section when Kuaidi100 mode selected
- [ ] Settings screen saves backend URL and receiver phone to DataStore
- [ ] `ProviderManager.get()` returns `Kuaidi100Provider` when `ACTIVE = "kdniao"`, `MockProvider` when `ACTIVE = "mock"`
- [ ] Unknown waybill in mock mode still returns `NOT_FOUND` failure (unchanged behavior)

### End-to-End

- [ ] With `ACTIVE = "mock"`: All 6 mock samples work as before (YT001→A-03-1234, etc.)
- [ ] With `ACTIVE = "kdniao"` + valid backend + valid phone: Real data returned
- [ ] With `ACTIVE = "kdniao"` + no backend URL: Shows config error message, no crash
- [ ] `gradlew :app:assembleDebug` succeeds
- [ ] `gradlew :app:testDebugUnitTest` passes

---

## Key Decisions (Resolved)

1. **Backend proxy chosen over direct App→Kuaidi100**: Key/secret stay on server; App only holds backend URL.
2. **Signature order confirmed**: `MD5(param + t + key + secret).uppercase()` — verified against Kuaidi100 docs.
3. **receiverPhone is mandatory**: Kuaidi100 pickup-code API requires it; prompted in Settings; stored in DataStore.
4. **30-min cache**: Free tier is 100 req/day; caching significantly reduces consumption.
5. **DataStore over SharedPreferences**: Consistent with existing ProviderConfig approach; easy to migrate to EncryptedDataStore later.
