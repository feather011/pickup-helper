'use strict';

const http = require("http");
const https = require("https");
const crypto = require("crypto");
const url = require("url");

const KUAIDI100_KEY = process.env.KUAIDI100_KEY;
const KUAIDI100_CUSTOMER = process.env.KUAIDI100_CUSTOMER;
if (!KUAIDI100_KEY || !KUAIDI100_CUSTOMER) {
  console.error("Missing env vars: KUAIDI100_KEY and KUAIDI100_CUSTOMER must be set");
  process.exit(1);
}

const SHIPPER_MAP = {
  YTO:"yuantong", SF:"shunfeng", ZTO:"zhongtong", EMS:"ems", JD:"jd",
  STO:"shentong", Yunda:"yunda", ZJS:"zhaijisong", TT:"tiantian", JT:"jtexpress"
};

const CANONICAL = Object.keys(SHIPPER_MAP);
function normalizeAppCode(raw) {
  const s = String(raw || "").trim();
  if (!s) return null;
  return CANONICAL.find(c => c.toLowerCase() === s.toLowerCase()) || s;
}

const WAYBILL_PREFIX_MAP = [
  ["SF","SF"],["YT","YTO"],["YD","Yunda"],["EMS","EMS"],
  ["ZT","ZTO"],["ST","STO"],["JD","JD"],["JT","JT"],
  ["78","EMS"],["QQ","Yunda"]
];
function inferAppCode(waybillNo) {
  if (!waybillNo) return null;
  const u = waybillNo.toUpperCase();
  for (const [prefix, code] of WAYBILL_PREFIX_MAP) {
    if (u.startsWith(prefix)) return code;
  }
  return null;
}

const cache = new Map();
const CACHE_TTL_MS = 30 * 60 * 1000;

function cacheGet(k) {
  const e = cache.get(k);
  if (!e) return null;
  if (Date.now() - e.ts > CACHE_TTL_MS) { cache.delete(k); return null; }
  return e.data;
}
function cacheSet(k, d) { cache.set(k, { data: d, ts: Date.now() }); }
function cacheKey(wn, sc, ph) { return wn + "|" + (sc || "") + "|" + (ph || ""); }

function makeSign(p, k, c) {
  return crypto.createHash("md5").update(p + k + c).digest("hex").toUpperCase();
}

function callKuaidi100(wn, com, phone) {
  return new Promise((resolve, reject) => {
    const paramJson = JSON.stringify({ com, num: wn, phone: phone || "", from: "", to: "", resultv2: "1" });
    const sign = makeSign(paramJson, KUAIDI100_KEY, KUAIDI100_CUSTOMER);
    const params = new url.URLSearchParams();
    params.append("customer", KUAIDI100_CUSTOMER);
    params.append("param", paramJson);
    params.append("sign", sign);
    const body = params.toString();
    const options = {
      hostname: "poll.kuaidi100.com", port: 443, path: "/poll/query.do", method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded", "Content-Length": Buffer.byteLength(body), "User-Agent": "PickupHelper/1.0" }
    };
    const req = https.request(options, (res) => {
      let data = "";
      res.on("data", c => { data += c; });
      res.on("end", () => {
        if (res.statusCode !== 200) return reject(new Error("HTTP " + res.statusCode + ": " + data.slice(0, 200)));
        try { resolve(JSON.parse(data)); }
        catch { reject(new Error("JSON parse fail: " + data.slice(0, 200))); }
      });
    });
    req.on("error", reject);
    req.setTimeout(10000, () => { req.destroy(); reject(new Error("timeout")); });
    req.write(body);
    req.end();
  });
}

function mapState(kdState, traces) {
  if (kdState === 3) return { State: "3", StateEx: "301" };
  if (kdState === 14) return { State: "0", StateEx: "401" };
  if (kdState === 2) return { State: "0", StateEx: "402" };
  if (kdState === 5) return { State: "2", StateEx: "211" };
  if (kdState === 0 || kdState === 1) {
    if (traces && traces.length > 0) {
      const last = traces[traces.length - 1].context || "";
      if (last.includes("到站") || last.includes("入柜") || last.includes("驿站") || last.includes("自提点"))
        return { State: "2", StateEx: "211" };
    }
    return { State: kdState === 1 ? "1" : "2", StateEx: "0" };
  }
  return { State: "0", StateEx: "0" };
}

function buildAppResponse(kd, wn, sc) {
  const kdState = parseInt(kd.state, 10) || 0;
  const traces = (kd.data || []).map(t => ({ AcceptTime: t.time, AcceptStation: t.context, Remark: t.location || "" }));
  const { State, StateEx } = mapState(kdState, kd.data || []);
  return {
    result: true, returnCode: "200",
    data: [{ LogisticCode: wn, ShipperCode: sc, State, StateEx, PickupCode: null, StationName: null, StationAddress: null, Traces: traces }]
  };
}

const PORT = process.env.PORT || 8080;
http.createServer(async (req, res) => {
  const pathname = url.parse(req.url).pathname;
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type");
  if (req.method === "OPTIONS") { res.writeHead(204); res.end(); return; }
  if (pathname === "/health") { res.writeHead(200, { "Content-Type": "application/json" }); res.end(JSON.stringify({ ok: true, ts: new Date().toISOString() })); return; }
  if (req.method === "POST" && pathname === "/api/pickup-code") {
    let body = "";
    req.on("data", c => { body += c; });
    req.on("end", async () => {
      let reqBody;
      try { reqBody = JSON.parse(body); }
      catch { res.writeHead(400, { "Content-Type": "application/json" }); res.end(JSON.stringify({ result: false, message: "body must be JSON" })); return; }
      const { waybillNo, shipperCode, receiverPhone } = reqBody;
      if (!waybillNo || !waybillNo.trim()) { res.writeHead(400, { "Content-Type": "application/json" }); res.end(JSON.stringify({ result: false, message: "waybillNo required" })); return; }
      const ckey = cacheKey(waybillNo.trim(), shipperCode, receiverPhone);
      const cached = cacheGet(ckey);
      if (cached) { console.log("[CACHE HIT] " + waybillNo); res.writeHead(200, { "Content-Type": "application/json" }); res.end(JSON.stringify(cached)); return; }
      const appCode = normalizeAppCode(shipperCode);
      let inferred = null;
      if (!appCode) {
        inferred = inferAppCode(waybillNo.trim());
        if (!inferred) {
          res.writeHead(400, { "Content-Type": "application/json" });
          res.end(JSON.stringify({ result: false, message: "cannot detect shipper, please select manually in app" }));
          return;
        }
        console.log("[INFER] from prefix: " + inferred);
      }
      const com = appCode ? SHIPPER_MAP[appCode] : SHIPPER_MAP[inferred];
      const resolvedCode = appCode || inferred;
      try {
        console.log("[QUERY] " + waybillNo + " via " + com);
        const kdResp = await callKuaidi100(waybillNo.trim(), com, receiverPhone);
        if (kdResp.status !== "200" && !Array.isArray(kdResp.data)) { console.log("[KD100 ERR] " + kdResp.status + " " + kdResp.message); res.writeHead(200, { "Content-Type": "application/json" }); res.end(JSON.stringify({ result: false, message: kdResp.message || "kuaidi100 error" })); return; }
        const appResp = buildAppResponse(kdResp, waybillNo.trim(), resolvedCode);
        cacheSet(ckey, appResp);
        console.log("[OK] " + waybillNo + " state=" + kdResp.state + " traces=" + (kdResp.data || []).length);
        res.writeHead(200, { "Content-Type": "application/json" }); res.end(JSON.stringify(appResp));
      } catch (err) { console.error("[ERROR] " + waybillNo + ":", err.message); res.writeHead(200, { "Content-Type": "application/json" }); res.end(JSON.stringify({ result: false, message: "Query failed: " + err.message })); }
    });
    return;
  }
  res.writeHead(404, { "Content-Type": "application/json" }); res.end(JSON.stringify({ result: false, message: "Not found: " + pathname }));
}).listen(PORT, () => { console.log("Server running on http://localhost:" + PORT); console.log("Kuaidi100 backend ready (customer not logged)"); });
