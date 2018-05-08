package com.l7tech.json;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static com.l7tech.json.JsonSchemaVersion.DRAFT_V2;

/**
 * Factory for creation of JSONData and JSONSchema.
 *
 * Different implementations e.g. javascript or other Java implementation can be managed here inside this module.
 *
 * Copyright (C) 2010, Layer 7 Technologies Inc.
 * @author darmstrong
 */
public enum JSONFactory {

    INSTANCE;

    private static final String UNSUPPORTED_SCHEMA_VERSION = "Unsupported schema version: ";

    /**
     * Returns an object-based representation of your JSON object string provided in argument {@code jsonData}.
     * The reason that a {@link JsonSchemaVersion version} argument is needed is that the validator we are using
     * for JSON Schema draft v2 relies on a very old version of Jackson, so the JSONode representation passed to it
     * needs to use this old version. This is a bit of a layering violation and should be changed when we either
     * get rid of JSON Schema v2 altogether or replace the old validator with one that accepts newer versions
     * of Jackson.
     *
     * @param jsonData your JSON data as a string
     * @param version the JSON Schema version indicating the validator that you plan to use this representation with
     * @return an object-based representation of the JSON in your {@code jsonData}
     */
    @NotNull
    public JSONData newJsonData(final String jsonData, final JsonSchemaVersion version) throws InvalidJsonException {
        switch (version) {
            case DRAFT_V2:
                return buildJsonDataV2(jsonData);

            case DRAFT_V4:
                return buildJsonDataV4(jsonData);

            default:
                throw new InvalidJsonException(UNSUPPORTED_SCHEMA_VERSION + version);
        }
    }

    @NotNull
    private JacksonJsonDataCodehaus buildJsonDataV2(final String jsonData) {
        final org.codehaus.jackson.map.ObjectMapper OBJECT_MAPPER_V2 = new org.codehaus.jackson.map.ObjectMapper();
        return new JacksonJsonDataCodehaus(OBJECT_MAPPER_V2, jsonData);
    }

    @NotNull
    private JacksonJsonDataFasterxml buildJsonDataV4(final String jsonData) {
        final com.fasterxml.jackson.databind.ObjectMapper OBJECT_MAPPER_V4 = new com.fasterxml.jackson.databind.ObjectMapper();
        return new JacksonJsonDataFasterxml(OBJECT_MAPPER_V4, jsonData);
    }

    /**
     * <p>This method is provided for backward-compatibility reasons only.
     * Version 2 was the only supported one, so this interface defaults to {@link JsonSchemaVersion#DRAFT_V2}
     * for old code's sake.</p>
     * <p>Do not use this method for new functionality.</p>
     * <p>This method delegates to {@link #newJsonData(String, JsonSchemaVersion)}.</p>
     *
     * @param jsonData a string containing your JSON text
     * @return a {@link JSONData} object containing an object-based representation of your {@code jsonData}
     */
    @NotNull
    public JSONData newJsonData(final String jsonData) throws InvalidJsonException {
        return newJsonData(jsonData, DRAFT_V2);
    }

    /**
     * Returns a JSON Schema validator wrapper.
     * Call {@link JSONSchema#validate(JSONData)} on the object returned by this method to validate JSON data
     * against the schema provided in your {@code jsonSchema} argument.
     *
     * @param jsonSchema a string containing the text representation of a JSON schema
     * @param version the draft version of your schema - version supported are ONLY {@link JsonSchemaVersion#DRAFT_V2}
     *                and {@link JsonSchemaVersion#DRAFT_V4}
     * @return a JSON Schema validator wrapper ready to validate JSON data against the {@code jsonSchema} provided
     * @throws InvalidJsonException if your JSON Schema is not valid JSON or if the validator library
     *          has any other problem creating a validator
     */
    @NotNull
    public JSONSchema newJsonSchema(final String jsonSchema, final JsonSchemaVersion version)
            throws IOException, InvalidJsonException {
        return newJsonSchema(newJsonData(jsonSchema, version));
    }

    /**
     * Returns a JSON Schema validator wrapper.
     * Call {@link JSONSchema#validate(JSONData)} on the object returned by this method to validate JSON data
     * against the schema provided in your {@code jsonSchema} argument.
     *
     * @param jsonSchema a string containing the text representation of a JSON schema
     * @return a JSON Schema validator wrapper ready to validate JSON data against the {@code jsonSchema} provided
     * @throws InvalidJsonException if your JSON Schema is not valid JSON or if the validator library
     *          has any other problem creating a validator
     */
    @NotNull
    public <D extends JSONData> JSONSchema newJsonSchema(final D jsonSchema) throws InvalidJsonException {
        if (jsonSchema instanceof JacksonJsonDataCodehaus) {
            return new JacksonJsonSchemaV2((JacksonJsonDataCodehaus) jsonSchema);
        }
        if (jsonSchema instanceof JacksonJsonDataFasterxml) {
            return new JacksonJsonSchemaV4((JacksonJsonDataFasterxml) jsonSchema);
        }
        throw new IllegalArgumentException("jsonSchema is of an unknown type");
    }

}
