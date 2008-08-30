package com.l7tech.gateway.common.mapping;

import java.io.Serializable;

/**
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Aug 21, 2008
 */
public class MessageContextMapping implements Serializable {
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

    public boolean hasEqualTypeAndKeyDifferentValue(MessageContextMapping other) {
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
           continueChecking = other.getKey() == null;
        } else {
            continueChecking = key.equals(other.getKey());
        }
        if (! continueChecking) return false;

        // Check if the values are the same.
        if (value == null) {
           return other.getValue() != null;
        } else {
            return !value.equals(other.getValue());
        }
    }

    public boolean equals(Object obj) {
        if (! (obj instanceof MessageContextMapping)) return false;

        MessageContextMapping other = (MessageContextMapping)obj;
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
           continueChecking = other.getKey() == null;
        } else {
            continueChecking = key.equals(other.getKey());
        }
        if (! continueChecking) return false;

        // Check if the values are the same.
        if (value == null) {
           return other.getValue() == null;
        } else {
            return value.equals(other.getValue());
        }
    }

    public String toString() {
        return
            "Mapping Type :" + this.getMappingType() + "\n" +
            "Mapping Key  :" + this.getKey() + "\n" +
            "Mapping Value:" + this.getValue() + "\n";
    }
}
