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
public class VariableMap implements Map<String, Object> {
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private Map<String, Object> map = null;

    // 100% simple delegation beyond this point

    public int size() {
        if (map == null) return 0;
        return map().size();
    }

    public boolean isEmpty() {
        return map == null || map().isEmpty();
    }

    public boolean containsKey(Object key) {
        return map != null && map().containsKey(key);
    }

    public boolean containsValue(Object value) {
        return map != null && map().containsValue(value);
    }

    public Object get(Object key) {
        if (map == null) return null;
        return map().get(key);
    }

    public Object put(String key, Object value) {
        return map().put(key, value);
    }

    public Object remove(Object key) {
        if (map == null) return null;
        return map().remove(key);
    }

    public void putAll(Map<? extends String, ? extends Object> t) {
        map().putAll(t);
    }

    public void clear() {
        if (map == null) return;
        map().clear();
    }

    public Set<String> keySet() {
        if (map == null) return Collections.emptySet();
        return map().keySet();
    }

    public Collection<Object> values() {
        if (map == null) return Collections.emptyList();
        return map().values();
    }

    public Set<Map.Entry<String, Object>> entrySet() {
        if (map == null) return Collections.emptySet();
        return map().entrySet();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VariableMap that = (VariableMap)o;

        return !(map != null ? !map.equals(that.map) : that.map != null);
    }

    public int hashCode() {
        return (map != null ? map.hashCode() : 0);
    }

    private Map<String, Object> map() {
        if (map == null) map = new HashMap<String, Object>();
        return map;
    }
}
