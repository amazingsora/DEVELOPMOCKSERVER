package org.amazingsora.mock.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/mock")
@CrossOrigin(origins = "*") 
public class GenericMockController {

    private final String DATA_DIR = "data/";
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * GET /api/mock/{entity}
     * 自動判斷回傳 List 或 Map
     */
    @GetMapping("/{entity}")
    public Object getData(@PathVariable String entity) throws IOException {
        return readJsonFile(entity);
    }

    /**
     * POST /api/mock/{entity}/update
     * 接收 {} 並更新至檔案中
     */
    @PostMapping("/{entity}/update")
    public Map<String, Object> updateData(@PathVariable String entity, @RequestBody Map<String, Object> payload) throws IOException {
        Object existingData = readJsonFile(entity);
        
        if (existingData instanceof List) {
            List<Map<String, Object>> list = (List<Map<String, Object>>) existingData;
            updateList(list, payload);
            writeJsonFile(entity, list);
        } else {
            Map<String, Object> map = (Map<String, Object>) existingData;
            map.putAll(payload);
            writeJsonFile(entity, map);
        }

        return Map.of(
            "status", "SUCCESS",
            "timestamp", System.currentTimeMillis(),
            "updatedEntity", entity
        );
    }


    private void updateList(List<Map<String, Object>> list, Map<String, Object> payload) {
        String idKey = "id"; 
        Object idValue = payload.get(idKey);

        if (idValue != null) {
            boolean found = false;
            for (Map<String, Object> item : list) {
                if (String.valueOf(item.get(idKey)).equals(String.valueOf(idValue))) {
                    item.putAll(payload); 
                    found = true;
                    break;
                }
            }
            if (!found) list.add(payload); 
        } else {
            list.add(payload); 
        }
    }

    private Object readJsonFile(String entity) throws IOException {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) dir.mkdirs();

        File file = new File(dir, entity + ".json");
        if (!file.exists()) return new HashMap<String, Object>();

        String content = new String(java.nio.file.Files.readAllBytes(file.toPath())).trim();
        if (content.startsWith("[")) {
            return mapper.readValue(file, new TypeReference<List<Map<String, Object>>>() {});
        } else {
            return mapper.readValue(file, new TypeReference<Map<String, Object>>() {});
        }
    }

    private void writeJsonFile(String entity, Object data) throws IOException {
        File file = new File(DATA_DIR + entity + ".json");
        mapper.writerWithDefaultPrettyPrinter().writeValue(file, data);
    }
}