package org.amazingsora.mock.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MockConfigLoader {

    private static final String CONFIG_DIR = "config/";
    private final ObjectMapper mapper = new ObjectMapper();
    // cache: configName -> config map
    private final ConcurrentHashMap<String, Map<String, Object>> cache = new ConcurrentHashMap<>();

    public Map<String, Object> loadConfig(String configName) throws IOException {
        File file = new File(CONFIG_DIR + configName + ".json");
        if (!file.exists()) return null;
        // Always reload from disk so editing JSON takes effect immediately (no restart needed)
        Map<String, Object> config = mapper.readValue(file, new TypeReference<>() {});
        cache.put(configName, config);
        return config;
    }

    public ObjectMapper getMapper() {
        return mapper;
    }
}
