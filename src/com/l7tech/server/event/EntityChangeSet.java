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
    public static final EntityChangeSet NONE = new EntityChangeSet(new Object[0], new Object[0], new Object[0]);

    public EntityChangeSet(Object[] propertyNames, Object[] oldValues, Object[] newValues) {
        map = new HashMap();
        for ( int i = 0; i < propertyNames.length; i++ ) {
            String propertyName = (String)propertyNames[i];
            map.put(propertyName, new PropertyChange(oldValues[i], newValues[i]));
        }
    }

    public Iterator getProperties() {
        return Collections.unmodifiableSet(map.keySet()).iterator();
    }

    public Object getOldValue(String property) {
        PropertyChange pc = (PropertyChange)map.get(property);
        if (pc == null) return null;
        return pc.ovalue;
    }

    public Object getNewValue(String property) {
        PropertyChange pc = (PropertyChange)map.get(property);
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

    private final Map map;
}
