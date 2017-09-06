package com.l7tech.json;

import org.jetbrains.annotations.NotNull;

/**
 * Enum of Json Validation Version
 */
public enum JsonSchemaVersion {

    DRAFT_V2("http://json-schema.org/draft-02/schema#", "JSON Schema Draft V2"),
    DRAFT_V4("http://json-schema.org/draft-04/schema#", "JSON Schema Draft V4");

    @NotNull
    private final String uri;
    @NotNull
    private final String displayName;

    JsonSchemaVersion(@NotNull final String uri, @NotNull final String displayName) {
        this.uri = uri;
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * <p>Tells you whether this version's URI matches {@code schemaUri}.</p>
     * @param schemaUri the URI you want to check
     * @return true if your URI matches this version's
     */
    public boolean matchesSchemaUri(String schemaUri) {
        return uri.equals(schemaUri);
    }

    public static boolean isKnown(String uri) {
        for (JsonSchemaVersion version :values()) {
            if (version.matchesSchemaUri(uri)) {
                return true;
            }
        }
        return false;
    }

    public static JsonSchemaVersion fromUri(String uri) {
        for (JsonSchemaVersion version :values()) {
            if (version.matchesSchemaUri(uri)) {
                return version;
            }
        }
        return null;
    }

    public static JsonSchemaVersion fromDisplayName(String displayName) {
        for (JsonSchemaVersion version :values()) {
            if (version.getDisplayName().equals(displayName)) {
                return version;
            }
        }
        return null;
    }

}
