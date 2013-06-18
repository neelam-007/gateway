package com.l7tech.policy.assertion.ext.message;

import com.l7tech.policy.variable.InvalidDataException;

/**
 * A simple json data interface
 */
public interface CustomJsonData {

    /**
     * Get the JSON Data
     * @return String JSON data. Never null.
     */
    public String getJsonData();

    /**
     * Get the JSON data converted to simple Java objects.
     *
     * @return Object map of JSON data, values will be either Map, List, String, Integer, Long, BigInteger, Double,
     * boolean or null. Returned object should never be null.
     * @throws InvalidDataException If the JSON contained by this JSONData is invalid.
     */
    Object getJsonObject() throws InvalidDataException;
}
