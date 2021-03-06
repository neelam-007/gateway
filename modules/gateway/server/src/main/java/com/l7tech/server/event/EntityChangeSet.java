/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.event;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A map of persistent object properties that have been changed.  Propagated in an {@link com.l7tech.server.event.admin.Updated} event.
 * <p>
 * Immutable.
 * @author alex
 * @version $Revision$
 */
public class EntityChangeSet {
    public static final EntityChangeSet NONE = new EntityChangeSet(new String[0], new Object[0], new Object[0]);

    public EntityChangeSet(String[] propertyNames, Object[] oldValues, Object[] newValues) {
        map = new HashMap<String, PropertyChange>();
        for ( int i = 0; i < propertyNames.length; i++ ) {
            String propertyName = propertyNames[i];
            map.put(propertyName, new PropertyChange(oldValues == null ? null : oldValues[i], newValues[i]));
        }
    }

    public Iterator<String> getProperties() {
        return Collections.unmodifiableSet(map.keySet()).iterator();
    }

    public int getNumProperties() {
        return map.size();
    }

    public Object getOldValue(String property) {
        PropertyChange pc = map.get(property);
        if (pc == null) return null;
        return pc.ovalue;
    }

    public Object getNewValue(String property) {
        PropertyChange pc = map.get(property);
        if (pc == null) return null;
        return pc.nvalue;
    }

    private static class PropertyChange {
        private PropertyChange(Object ovalue, Object nvalue) {
            this.ovalue = ovalue;
            this.nvalue = nvalue;
        }
        private final Object ovalue;
        private final Object nvalue;
    }

    private final Map<String, PropertyChange> map;
}
