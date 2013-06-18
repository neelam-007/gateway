package com.l7tech.gateway.common.custom;

import com.l7tech.json.InvalidJsonException;
import com.l7tech.json.JSONData;
import com.l7tech.policy.assertion.ext.message.CustomJsonData;
import com.l7tech.policy.variable.InvalidDataException;

import org.jetbrains.annotations.NotNull;

/**
 * Converts JSONData into CustomJASONData.
 *
 * @author tveninov
 */
public final class CustomToJsonData implements CustomJsonData {

    private final JSONData jasonData;

    /**
     * Construct from {@link JSONData} object.
     * @param jasonData object containing the Jason data. Cannot be null.
     */
    public CustomToJsonData(@NotNull final JSONData jasonData) {
        this.jasonData = jasonData;
    }
    
    @Override
    public String getJsonData() {
        return jasonData.getJsonData();
    }

    @Override
    public Object getJsonObject() throws InvalidDataException {
        try {
            return jasonData.getJsonObject();
        } catch (InvalidJsonException e) {
            throw new InvalidDataException(e.getCause());
        }
    }
}
