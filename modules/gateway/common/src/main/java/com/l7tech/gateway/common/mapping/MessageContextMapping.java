package com.l7tech.gateway.common.mapping;

import java.io.Serializable;

/**
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Aug 21, 2008
 */
public class MessageContextMapping implements Serializable, Comparable<MessageContextMapping> {
    public static final String MAPPING_TYPES[] = new String[] {
        "IP Address",
        "Authenticated User",
        "Custom Mapping"
    };
    public static final int CUSTOM_MAPPING_TYPE_IDX = 2;

    public static final String DEFAULT_MAPPING_TYPES[] = new String[] {
            MAPPING_TYPES[0],
            MAPPING_TYPES[1],
    };

    public static final String DEFAULT_KEYS[] = new String[] {
        "IP_ADDRESS",
        "AUTH_USER"
    };

    public static final String DEFAULT_VALUE = "(SYSTEM DEFINED)";

    private String mappingType;
    private String key;
    private String value;

    public MessageContextMapping() {

    }

    /**
     * Make a copy of the message context mapping
     * @return
     */
    public MessageContextMapping asCopy() {
        MessageContextMapping newCopy = new MessageContextMapping();
        newCopy.setMappingType(mappingType);
        newCopy.setKey(key);
        newCopy.setValue(value);
        return newCopy;
    }

    public MessageContextMapping(String mappingType, String key, String value) {
        this.mappingType = mappingType;
        this.key = key;
        this.value = value;
    }

    public String getMappingType() {
        return mappingType;
    }

    public void setMappingType(String mappingType) {
        this.mappingType = mappingType;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Check if two mappings have the same mapping type and key.  But, don't care about if the values are the same or not.
     * @param other: the second mapping to compare.
     * @return true if two mappings have the same mapping type and key.
     */
    public boolean hasEqualTypeAndKeyExcludingValue(MessageContextMapping other) {
        boolean continueChecking;

        // Check if the mapping types are the same.
        if (mappingType == null) {
           continueChecking = other.getMappingType() == null;
        } else {
            continueChecking = mappingType.equals(other.getMappingType());
        }
        if (! continueChecking) return false;

        // Check if the keys are the same.
        if (key == null) {
           return (other.getKey() == null);
        } else if (other.getKey() == null) {
           return (key == null);
        } else {
            return key.toLowerCase().equals(other.getKey().toLowerCase());
        }
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MessageContextMapping that = (MessageContextMapping) o;

        if (mappingType != null ? !mappingType.equals(that.mappingType) : that.mappingType != null) return false;
        if (key != null ? !key.equals(that.key) : that.key != null) return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (mappingType != null ? mappingType.hashCode() : 0);
        result = 31 * result + (key != null ? key.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    public int compareTo(MessageContextMapping mcm) {
        // Step 1: check type
        String type1 = getMappingType();
        String type2 = mcm.getMappingType();
        if (type1 == null && type2 == null) return 0;
        else if (type1 == null)             return -1;
        else if (type2 == null)             return 1;
        else if (! type1.equals(type2))     return type1.toLowerCase().compareTo(type2.toLowerCase());

        // Step 2: check key
        String key1 = getMappingType();
        String key2 = mcm.getMappingType();
        if (key1 == null && key2 == null) return 0;
        else if (key1 == null)             return -1;
        else if (key2 == null)             return 1;
        else if (! key1.equals(key2))      return key1.toLowerCase().compareTo(key2.toLowerCase());

        // Step 3: check value
        String value1 = getMappingType();
        String value2 = mcm.getMappingType();
        if (value1 == null && value2 == null) return 0;
        else if (value1 == null)              return -1;
        else if (value2 == null)              return 1;
        else if (! value1.equals(value2))     return value1.toLowerCase().compareTo(value2.toLowerCase());

        return 0;
    }

    public String toString() {
        return
            "Mapping Type :" + this.getMappingType() + "\n" +
            "Mapping Key  :" + this.getKey() + "\n" +
            "Mapping Value:" + this.getValue() + "\n";
    }
}