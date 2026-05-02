package org.amazingsora.mock.server.controller;

import org.amazingsora.mock.server.service.MockConfigLoader;
import org.amazingsora.mock.server.service.MockDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000"})
public class GenericMockController {

    private final MockConfigLoader configLoader;
    private final MockDataService dataService;

    public GenericMockController(MockConfigLoader configLoader, MockDataService dataService) {
        this.configLoader = configLoader;
        this.dataService = dataService;
    }

    /**
     * 查詢 — POST /{entity}
     * 讀 config/{entity}.json 取得 queryFields，然後過濾 data/{entity}.json
     */
    @PostMapping("/{entity}")
    public ResponseEntity<Object> query(
            @PathVariable String entity,
            @RequestBody(required = false) Map<String, Object> req) {

        if (!isValidEntity(entity)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid entity name"));
        }

        try {
            Map<String, Object> config = configLoader.loadConfig(entity);
            if (config == null) {
                return ResponseEntity.notFound().build();
            }

            @SuppressWarnings("unchecked")
            List<String> queryFields = (List<String>) config.getOrDefault("queryFields", List.of());

            Object result = dataService.query(entity, req, queryFields);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 新增 — POST /{entity}/create
     */
    @PostMapping("/{entity}/create")
    public ResponseEntity<Object> create(
            @PathVariable String entity,
            @RequestBody Map<String, Object> payload) {

        if (!isValidEntity(entity)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid entity name"));
        }

        try {
            Map<String, Object> result = dataService.create(entity, payload);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 修改 — POST /{entity}/update
     * config 裡的 idField 定義用哪個欄位來比對（預設用 "id"）
     */
    @PostMapping("/{entity}/update")
    public ResponseEntity<Object> update(
            @PathVariable String entity,
            @RequestBody Map<String, Object> payload) {

        if (!isValidEntity(entity)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid entity name"));
        }

        try {
            Map<String, Object> config = configLoader.loadConfig(entity);
            String idField = config != null
                    ? (String) config.getOrDefault("idField", "id")
                    : "id";

            Map<String, Object> result = dataService.update(entity, payload, idField);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ─── 預留 Admin UI 用的 endpoint（之後串前端）────────────────────────────

    /**
     * 取得所有 config 列表 — 給未來 /admin UI 用
     * GET /admin/configs
     */
    @GetMapping("/admin/configs")
    public ResponseEntity<Object> listConfigs() {
        java.io.File configDir = new java.io.File("config/");
        if (!configDir.exists()) return ResponseEntity.ok(List.of());
        String[] files = configDir.list((d, name) -> name.endsWith(".json"));
        if (files == null) return ResponseEntity.ok(List.of());
        List<String> names = java.util.Arrays.stream(files)
                .map(f -> f.replace(".json", ""))
                .toList();
        return ResponseEntity.ok(names);
    }

    /**
     * 取得單一 config — 給未來 /admin UI 用
     * GET /admin/configs/{entity}
     */
    @GetMapping("/admin/configs/{entity}")
    public ResponseEntity<Object> getConfig(@PathVariable String entity) {
        if (!isValidEntity(entity)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid entity name"));
        }
        try {
            Map<String, Object> config = configLoader.loadConfig(entity);
            if (config == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ─── helper ──────────────────────────────────────────────────────────────

    private boolean isValidEntity(String entity) {
        return entity != null && entity.matches("^[a-zA-Z0-9_-]+$");
    }
}
