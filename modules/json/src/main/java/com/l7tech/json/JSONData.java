/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 *
 * Interface for JSON Data
 *
 * @author darmstrong
 */
package com.l7tech.json;

public interface JSONData {

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
     * @throws InvalidJsonException If the JSON contained by this JSONData is invalid.
     */
    Object getJsonObject() throws InvalidJsonException;
}
