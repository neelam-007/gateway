/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.wsp;

import java.lang.reflect.Type;

/**
 * A reference that knows the nominal type of its target, even when it is null.
 */
public class TypedReference {
    public final Type type;
    public final Object target;
    public final String name;

    TypedReference(Type type, Object target, String name) {
        this.type = type;
        this.target = target;
        this.name = name;
        if (type == null)
            throw new IllegalArgumentException("A non-null concrete type must be provided");
        if (name == null && target == null)
            throw new IllegalArgumentException("Only named references may have a null target");
    }

    public TypedReference(Class type, Object target) {
        this(type, target, null);
    }
}
