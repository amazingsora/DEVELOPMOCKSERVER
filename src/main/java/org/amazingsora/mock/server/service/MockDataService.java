package org.amazingsora.mock.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class MockDataService {

    @Value("${mock.data.dir:data/}")
    private String dataDir;

    private final ObjectMapper mapper;
    private final ReentrantLock lock = new ReentrantLock();

    public MockDataService(MockConfigLoader configLoader) {
        this.mapper = configLoader.getMapper();
    }

    /**
     * 查詢：從 data/{entity}.json 讀取資料，依 req 參數過濾
     * config 裡的 queryFields 定義哪些欄位可以用來過濾
     */
    @SuppressWarnings("unchecked")
    public Object query(String entity, Map<String, Object> req, List<String> queryFields) throws IOException {
        Object raw = readJsonFile(entity);

        // 如果根層是 Map（像 limit 的結構：{ "limit": [...], "messages": {...}, "total-no-of-records": 0 }）
        if (raw instanceof Map) {
            Map<String, Object> responseMap = (Map<String, Object>) raw;

            // 找出哪個 key 是 List（主資料陣列）
            String listKey = findListKey(responseMap);
            if (listKey != null && req != null && !req.isEmpty()) {
                List<Map<String, Object>> allItems = (List<Map<String, Object>>) responseMap.get(listKey);
                List<Map<String, Object>> filtered = filterItems(allItems, req, queryFields);

                // 建立新的 response，只換掉主資料陣列，其他欄位（messages 等）保持不變
                Map<String, Object> result = new LinkedHashMap<>(responseMap);
                result.put(listKey, filtered);
                result.put("total-no-of-records", filtered.size());
                return result;
            }
            return responseMap;
        }

        // 根層是 List（簡單陣列格式）
        if (raw instanceof List && req != null && !req.isEmpty()) {
            List<Map<String, Object>> allItems = (List<Map<String, Object>>) raw;
            return filterItems(allItems, req, queryFields);
        }

        return raw;
    }

    /**
     * 新增：照 req 結構新增一筆進 data JSON
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> create(String entity, Map<String, Object> payload) throws IOException {
        lock.lock();
        try {
            Object raw = readJsonFile(entity);
            if (raw instanceof Map) {
                Map<String, Object> responseMap = (Map<String, Object>) raw;
                String listKey = findListKey(responseMap);
                if (listKey != null) {
                    List<Map<String, Object>> list = (List<Map<String, Object>>) responseMap.get(listKey);
                    list.add(payload);
                    responseMap.put("total-no-of-records", list.size());
                    writeJsonFile(entity, responseMap);
                }
            } else if (raw instanceof List) {
                List<Map<String, Object>> list = (List<Map<String, Object>>) raw;
                list.add(payload);
                writeJsonFile(entity, list);
            }
            return successResponse(entity, "CREATE");
        } finally {
            lock.unlock();
        }
    }

    /**
     * 修改：找到對應 id 的資料並更新
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> update(String entity, Map<String, Object> payload, String idField) throws IOException {
        lock.lock();
        try {
            Object raw = readJsonFile(entity);
            if (raw instanceof Map) {
                Map<String, Object> responseMap = (Map<String, Object>) raw;
                String listKey = findListKey(responseMap);
                if (listKey != null) {
                    List<Map<String, Object>> list = (List<Map<String, Object>>) responseMap.get(listKey);
                    updateList(list, payload, idField);
                    writeJsonFile(entity, responseMap);
                }
            } else if (raw instanceof List) {
                List<Map<String, Object>> list = (List<Map<String, Object>>) raw;
                updateList(list, payload, idField);
                writeJsonFile(entity, list);
            }
            return successResponse(entity, "UPDATE");
        } finally {
            lock.unlock();
        }
    }

    // ─── private helpers ──────────────────────────────────────────────────────

    private List<Map<String, Object>> filterItems(
            List<Map<String, Object>> items,
            Map<String, Object> req,
            List<String> queryFields) {

        if (items == null) return Collections.emptyList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> item : items) {
            if (matches(item, req, queryFields)) {
                result.add(item);
            }
        }
        return result;
    }

    private boolean matches(Map<String, Object> item, Map<String, Object> req, List<String> queryFields) {
        for (String field : queryFields) {
            Object reqVal = req.get(field);
            // req 沒帶這個欄位 → 不過濾這個欄位
            if (reqVal == null || reqVal.toString().isBlank()) continue;
            Object itemVal = item.get(field);
            if (itemVal == null) return false;
            if (!String.valueOf(itemVal).equals(String.valueOf(reqVal))) return false;
        }
        return true;
    }

    private void updateList(List<Map<String, Object>> list, Map<String, Object> payload, String idField) {
        Object idValue = payload.get(idField);
        if (idValue == null) {
            list.add(payload);
            return;
        }
        boolean found = false;
        for (Map<String, Object> item : list) {
            if (String.valueOf(item.get(idField)).equals(String.valueOf(idValue))) {
                item.putAll(payload);
                found = true;
                break;
            }
        }
        if (!found) list.add(payload);
    }

    private String findListKey(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() instanceof List) return entry.getKey();
        }
        return null;
    }

    private Object readJsonFile(String entity) throws IOException {
        File dir = new File(dataDir);
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, entity + ".json");
        if (!file.exists()) return new HashMap<String, Object>();
        String content = new String(java.nio.file.Files.readAllBytes(file.toPath())).trim();
        if (content.startsWith("[")) {
            return mapper.readValue(file, new TypeReference<List<Map<String, Object>>>() {});
        }
        return mapper.readValue(file, new TypeReference<Map<String, Object>>() {});
    }

    private void writeJsonFile(String entity, Object data) throws IOException {
        File file = new File(dataDir + entity + ".json");
        mapper.writerWithDefaultPrettyPrinter().writeValue(file, data);
    }

    private Map<String, Object> successResponse(String entity, String action) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("status", "SUCCESS");
        resp.put("action", action);
        resp.put("entity", entity);
        resp.put("timestamp", System.currentTimeMillis());
        return resp;
    }
}
