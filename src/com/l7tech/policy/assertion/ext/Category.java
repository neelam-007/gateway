package com.l7tech.policy.assertion.ext;

import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * The enum style class that describes custom assertions categories.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class Category implements Serializable {
    private static int index = 0;
    public static final Category ALL = new Category(index++, "All");
    public static final Category IDENTITY = new Category(index++, "Identity");
    public static final Category TRANSFORM = new Category(index++, "Transform");
    public static final Category ACCESS_CONTROL = new Category(index++, "AccessControl");
    public static final Category UNFILLED = new Category(index++, "Unfilled");

    private final int myKey;
    private final String myName;

    private Category(int key, String name) {
        myKey = key;
        myName = name;
    }

    /**
     * @return all known categories
     */
    public static Category[] getCategories() {
        return VALUES;
    }

    /**
     * Resolve category by name
     *
     * @param name the category name
     * @return the corresponding category or <b>null</b>
     */
    public static Category asCategory(String name) {
        if (name == null) return null;
        for (int i = 0; i < VALUES.length; i++) {
            Category value = VALUES[i];
            if (value.myName.equals(name)) {
                return value;
            }
        }
        return null;
    }

    public String toString() {
        return myName;
    }

    /**
     * Resolves instances being deserialized to the predefined constants
     *
     * @return the object reference of the newly created object after it is
     *         deserialized.
     * @throws ObjectStreamException
     */
    private Object readResolve() throws ObjectStreamException {
        return VALUES[myKey];
    }

    private static final Category[] VALUES = {ALL, IDENTITY, TRANSFORM, ACCESS_CONTROL, UNFILLED};
}
