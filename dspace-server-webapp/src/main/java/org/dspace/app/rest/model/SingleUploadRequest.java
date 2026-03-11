package org.dspace.app.rest.model;

import java.util.Map;

/**
 * SingleUploadRequest
 *
 * This class represents the JSON body that the client
 * sends to the Single Upload API.
 *
 * Spring automatically converts incoming JSON into this object.
 *
 * Example JSON:
 * {
 *   "bundle": "CaseType1",
 *   "mapping": "A|B",
 *   "file": "base64string",
 *   "metadata": {
 *       "dc.title": "Example Title"
 *   }
 * }
 */
public class SingleUploadRequest {

    // This represents the rule group (example: CaseType1)
    // It corresponds to the "bundle" column in bundle_map table.
    private String bundle;

    // This contains Parent|Child (example: A|B)
    // We will split this later in the controller.
    private String mapping;

    // This is the base64 encoded file.
    // Later in Phase 2 we will decode and create a bitstream.
    private String file;

    // Metadata is stored as key-value pairs.
    // For now we are using only "dc.title".
    private Map<String, String> metadata;

    // ===== Getter and Setter Methods =====

    public String getBundle() {
        return bundle;
    }

    public void setBundle(String bundle) {
        this.bundle = bundle;
    }

    public String getMapping() {
        return mapping;
    }

    public void setMapping(String mapping) {
        this.mapping = mapping;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
}