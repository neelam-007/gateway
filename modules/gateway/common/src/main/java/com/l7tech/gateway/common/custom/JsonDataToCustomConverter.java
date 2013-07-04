package com.l7tech.gateway.common.custom;

import com.l7tech.json.InvalidJsonException;
import com.l7tech.json.JSONData;
import com.l7tech.policy.assertion.ext.message.CustomJsonData;
import com.l7tech.policy.assertion.ext.message.InvalidDataException;

import org.jetbrains.annotations.NotNull;

/**
 * Converts {@link JSONData} into {@link CustomJsonData}.
 */
public final class JsonDataToCustomConverter implements CustomJsonData {

    private final JSONData jsonData;

    /**
     * Construct from {@link JSONData} object.
     * @param jsonData object containing the JSON data. Cannot be null.
     */
    public JsonDataToCustomConverter(@NotNull final JSONData jsonData) {
        this.jsonData = jsonData;
    }

    @Override
    public String getJsonData() {
        return jsonData.getJsonData();
    }

    @Override
    public Object getJsonObject() throws InvalidDataException {
        try {
            return jsonData.getJsonObject();
        } catch (InvalidJsonException e) {
            throw new InvalidDataException(e.getMessage(), e.getCause());
        }
    }
}
