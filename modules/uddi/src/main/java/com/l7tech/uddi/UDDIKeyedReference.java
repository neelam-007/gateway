/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.uddi;

import java.io.Serializable;

/**
 * Represents a UDDI KeyedReference.
 * Properties of class cannot be changed without writing an upgrade task as values are persisted in the properties
 * map of UDDIProxiedServiceInfo.
 *
 * Setters are only provided so that this object can be persisted.
 */
public class UDDIKeyedReference implements Serializable {
    public static final String GENERAL_KEYWORDS = "uddi:uddi.org:categorization:general_keywords";

    /**
     * Create a new keyed reference with the given values.
     *
     * @param tModelKey The key (required and must not be empty)
     * @param keyName The name (optional)
     * @param keyValue The value (required)
     */
    public UDDIKeyedReference( final String tModelKey,
                               final String keyName,
                               final String keyValue) {
        if(keyValue == null || keyValue.trim().isEmpty()) throw new IllegalArgumentException("tModelKey value cannot be null or empty");
        if(tModelKey.equals(GENERAL_KEYWORDS) && (keyName == null || keyName.trim().isEmpty())){
            //See http://www.uddi.org/pubs/uddi_v3.htm#_Ref8978058
            throw new IllegalArgumentException("If the keyedReference is a general keywords classification, then name cannot be null or empty");
        }

        this.tModelKey = tModelKey;
        this.keyValue = keyValue;
        this.keyName = keyName;
    }

    /**
     * No arg constructor required for saving to db. Don't use this constructor.
     */
    public UDDIKeyedReference() {
    }

    public String getTModelKey() {
        return this.tModelKey;
    }

    public void setTModelKey(String tModelKey) {
        this.tModelKey = tModelKey;
    }

    public String getKeyName() {
        return this.keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public String getKeyValue() {
        return this.keyValue;
    }

    public void setKeyValue(String keyValue) {
        this.keyValue = keyValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UDDIKeyedReference that = (UDDIKeyedReference) o;

        if (!keyValue.equals(that.keyValue)) return false;
        //keys are language and case insensitive in UDDI.
        if (!tModelKey.equalsIgnoreCase(that.tModelKey)) return false;

        if(tModelKey.equals(GENERAL_KEYWORDS)){
            if (keyName != null ? !keyName.equals(that.keyName) : that.keyName != null) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = tModelKey.hashCode();
        result = 31 * result + keyValue.hashCode();
        if(tModelKey.equals(GENERAL_KEYWORDS)){
            result = 31 * result + (keyName != null ? keyName.hashCode() : 0);
        }

        return result;
    }

    private String tModelKey;
    private String keyValue;
    private String keyName;
}
