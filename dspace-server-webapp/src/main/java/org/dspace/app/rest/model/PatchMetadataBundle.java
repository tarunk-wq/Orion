package org.dspace.app.rest.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PatchMetadataBundle {
	private Map<String, List<String>> metadataMap = new HashMap<>();

    public void addValue(String field, String value) {
        metadataMap.computeIfAbsent(field, k -> new ArrayList<>()).add(value);
    }

    public Map<String, List<String>> getMetadataMap() {
        return metadataMap;
    }
}
