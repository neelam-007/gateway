/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion;

import com.l7tech.util.EnumTranslator;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.*;

/**
 * @author alex
 */
public class AssertionResourceType implements Serializable {
    private static final Map valuesByName = new HashMap();

    private static int next = 0;
    public static final AssertionResourceType STATIC = new AssertionResourceType(next++, "STATIC", "Static document");
    public static final AssertionResourceType SINGLE_URL = new AssertionResourceType(next++, "SINGLE_URL", "Read document from single URL");
    public static final AssertionResourceType MESSAGE_URL = new AssertionResourceType(next++, "MESSAGE_URL", "Read document using URL from message");
    public static final AssertionResourceType GLOBAL_RESOURCE = new AssertionResourceType(next++, "GLOBAL_RES", "Read document from a global resource shared across policies");
    private static final AssertionResourceType[] VALUES = {STATIC, SINGLE_URL, MESSAGE_URL, GLOBAL_RESOURCE};

    private final int num;
    private final String name;
    private final String description;

    private AssertionResourceType(int num, String name, String desc) {
        this.num = num;
        this.name = name;
        this.description = desc;
        valuesByName.put(name, this);
    }

    public int getNum() {
        return num;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    protected Object readResolve() throws ObjectStreamException {
        return VALUES[num];
    }

    public static List values() {
        return Collections.unmodifiableList(Arrays.asList(VALUES));
    }

    public static Object fromString(String s) {
        return valuesByName.get(s);
    }

    public String asString() {
        return name;
    }

    public String toString() {
        if (name == null) return "Undefined AssertionResourceType";
        return name;
    }

    public static EnumTranslator getEnumTranslator() {
        return new EnumTranslator() {
            public Object stringToObject(String s) throws IllegalArgumentException {
                AssertionResourceType o = (AssertionResourceType) valuesByName.get(s);
                if (o == null) throw new IllegalArgumentException("Unsupported resource type: " + s);
                return o;
            }

            public String objectToString(Object o) throws ClassCastException {
                return ((AssertionResourceType)o).getName();
            }
        };
    }
}
