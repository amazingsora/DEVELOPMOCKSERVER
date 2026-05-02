# Mock Server — 操作說明

## 啟動
```bash
./mvnw spring-boot:run
```
或 Docker：
```bash
docker-compose up
```

---

## 目錄結構
```
config/      ← API 設定檔（新增 API 在這裡）
data/        ← 實際資料（查詢/新增/修改會讀寫這裡）
```

---

## 新增一支 API（5分鐘內完成）

### 步驟 1：在 config/ 建立設定檔

建立 `config/{entity}.json`，例如 `config/trade.json`：

```json
{
  "url": "/trade",
  "method": "POST",
  "description": "查詢 trade 資料",
  "idField": "trade-id",
  "queryFields": [
    "external-root-reference",
    "trade-status"
  ],
  "adminUI": {
    "label": "Trade 查詢",
    "group": "Trade"
  }
}
```

**欄位說明：**
| 欄位 | 說明 | 範例 |
|------|------|------|
| `idField` | 新增/修改時用來比對的唯一鍵 | `"limit-id"` |
| `queryFields` | 查詢時可以用來過濾的欄位（req 帶哪些 key） | `["external-root-reference"]` |
| `adminUI.label` | 未來 Admin UI 顯示的名稱 | `"Trade 查詢"` |
| `adminUI.group` | 未來 Admin UI 的分組 | `"Trade"` |

### 步驟 2：在 data/ 建立資料檔

建立 `data/{entity}.json`，貼上後端提供的 response 範本，多筆資料用不同的 queryFields 值：

```json
{
  "trade": [
    {
      "trade-id": 1,
      "external-root-reference": 1001,
      "trade-status": 1,
      "amount": 500000
    },
    {
      "trade-id": 2,
      "external-root-reference": 1002,
      "trade-status": 2,
      "amount": 300000
    }
  ],
  "messages": {
    "max-severity-code": 0,
    "max-severity-desc": "SUCCESS",
    "message-list": []
  },
  "total-no-of-records": 2
}
```

### 步驟 3：呼叫 API（不需要重啟 server）

```
POST http://localhost:8080/trade
Content-Type: application/json

{
  "external-root-reference": 1001
}
```

---

## API 端點總覽

| 用途 | 方法 | URL |
|------|------|-----|
| 查詢（支援過濾） | POST | `/{entity}` |
| 新增一筆 | POST | `/{entity}/create` |
| 修改一筆 | POST | `/{entity}/update` |
| 列出所有 config（Admin 用） | GET | `/admin/configs` |
| 取得單一 config（Admin 用） | GET | `/admin/configs/{entity}` |

---

## 查詢過濾行為

- req 帶的欄位：作為過濾條件（完全比對）
- req 不帶的欄位：不過濾，回傳所有
- req 為空 `{}`：回傳全部資料

### 範例

`data/limit.json` 有 2 筆，`external-root-reference` 分別是 1001 和 1002

```json
// 只帶 external-root-reference → 只回傳 1001 那筆
POST /limit
{ "external-root-reference": 1001 }

// 空 body → 回傳全部 2 筆
POST /limit
{}
```

---

## 新增/修改行為

**新增** `POST /{entity}/create`：直接 append 一筆進 data JSON

**修改** `POST /{entity}/update`：
- req 帶 idField（例如 `limit-id`）→ 找到那筆並更新
- req 不帶 idField → 當作新增處理

---

## 未來 Admin UI 規格（預留）

以下 endpoint 已開通，等前端串接：

```
GET /admin/configs          → 列出所有已設定的 entity
GET /admin/configs/{entity} → 取得單一 entity 的完整 config
```

未來 UI 功能規劃：
- [ ] 瀏覽所有 API config
- [ ] 線上編輯 queryFields / idField
- [ ] 貼上 response 範本自動產生 data JSON
- [ ] 一鍵重置資料（還原成原始範本）
- [ ] Exception 情境設定（指定條件 → 回傳特定錯誤 response）
