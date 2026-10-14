# Kuaidi100 Backend Proxy

Node.js zero-dependency backend that proxies requests to Kuaidi100's real-time tracking API.

## Setup

1. Copy `.env.example` to `.env` and fill in your Kuaidi100 credentials:
   ```
   cp .env.example .env
   ```

2. Get your `KUAIDI100_KEY` and `KUAIDI100_CUSTOMER` from https://www.kuaidi100.com/openapi/

## Run

```bash
node server.js
```

Or with environment variables directly:

```bash
KUAIDI100_KEY=your_key KUAIDI100_CUSTOMER=your_customer node server.js
```

Server starts on `http://localhost:8080`.

## API

### GET /health
Health check.

### POST /api/pickup-code

Request:
```json
{
  "waybillNo": "JT5533257831458",
  "shipperCode": "JT",
  "receiverPhone": ""
}
```

Response:
```json
{
  "result": true,
  "returnCode": "200",
  "data": [{
    "LogisticCode": "JT5533257831458",
    "ShipperCode": "JT",
    "State": "3",
    "StateEx": "301",
    "PickupCode": null,
    "StationName": null,
    "Traces": [...]
  }]
}
```

## Shipper Code Mapping (App Code -> Kuaidi100 API Code)

| App Code | Kuaidi100 Code |
|----------|---------------|
| YTO      | yuantong      |
| SF       | shunfeng      |
| ZTO      | zhongtong     |
| EMS      | ems           |
| JD       | jd            |
| STO      | shentong      |
| Yunda    | yunda         |
| ZJS      | zhaijisong    |
| TT       | tiantian      |
| JT       | jtexpress     |

## Notes

- The App does NOT hold any API keys. All requests go through this backend.
- 30-minute in-memory cache per waybill to avoid redundant API calls and prevent rate-limit locks.
- If `shipperCode` is not provided, the server infers it from waybill number prefixes (SF/YT/YD/EMS/ZT/ST/JD/JT).
- The real-time tracking API does NOT return pickup codes. Use Mock mode for pickup code demos.
