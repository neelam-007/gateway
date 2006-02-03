/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.variable;

import java.util.*;

/**
 * A {@link Map} of variable names and values, plus a {@link List} of names that were not found
 * <p>
 * Not thread-safe. 
 */
public class VariableMap implements Map {
    private final Map map = new HashMap();
    private final List badNames = new ArrayList();

    public void addBadName(String name) {
        badNames.add(name);
    }

    public String[] getBadNames() {
        return (String[])badNames.toArray(new String[0]);
    }

    // 100% simple delegation beyond this point

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    public Object get(Object key) {
        return map.get(key);
    }

    public Object put(Object key, Object value) {
        return map.put(key, value);
    }

    public Object remove(Object key) {
        return map.remove(key);
    }

    public void putAll(Map t) {
        map.putAll(t);
    }

    public void clear() {
        map.clear();
    }

    public Set keySet() {
        return map.keySet();
    }

    public Collection values() {
        return map.values();
    }

    public Set entrySet() {
        return map.entrySet();
    }

    public boolean equals(Object o) {
        return map.equals(o);
    }

    public int hashCode() {
        return map.hashCode();
    }
}
