/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.agent.tools.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class ToolHelper {
    private static final Gson gson = new Gson();

    /**
     * Load prompt from the resource file of the invoking class
     * @param source class which calls this function
     * @param fileName the resource file name of prompt
     * @return the LLM request prompt template.
     */
    public static Map<String, String> loadDefaultPromptDictFromFile(Class<?> source, String fileName) {
        try (InputStream searchResponseIns = source.getResourceAsStream(fileName)) {
            if (searchResponseIns != null) {
                String defaultPromptContent = new String(searchResponseIns.readAllBytes(), StandardCharsets.UTF_8);
                return gson.fromJson(defaultPromptContent, Map.class);
            }
        } catch (IOException e) {
            log.error("Failed to load default prompt dict from file: {}", fileName, e);
        }
        return new HashMap<>();
    }

    /**
     * Flatten all the fields in the mappings, insert the field to fieldType mapping to a map
     * @param mappingSource the mappings of an index
     * @param fieldsToType the result containing the field to fieldType mapping
     * @param prefix the parent field path
     */
    public static void extractFieldNamesTypes(Map<String, Object> mappingSource, Map<String, String> fieldsToType, String prefix) {
        if (prefix.length() > 0) {
            prefix += ".";
        }

        for (Map.Entry<String, Object> entry : mappingSource.entrySet()) {
            String n = entry.getKey();
            Object v = entry.getValue();

            if (v instanceof Map) {
                Map<String, Object> vMap = (Map<String, Object>) v;
                if (vMap.containsKey("type")) {
                    if (!((vMap.getOrDefault("type", "")).equals("alias"))) {
                        fieldsToType.put(prefix + n, (String) vMap.get("type"));
                    }
                }
                if (vMap.containsKey("properties")) {
                    extractFieldNamesTypes((Map<String, Object>) vMap.get("properties"), fieldsToType, prefix + n);
                }
                if (vMap.containsKey("fields")) {
                    extractFieldNamesTypes((Map<String, Object>) vMap.get("fields"), fieldsToType, prefix + n);
                }
            }
        }
    }
}
