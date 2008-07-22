/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.wsp.pre32;

/**
 * A reference that knows the nominal type of its target, even when it is null.
 */
class Pre32TypedReference {
    public final Class type;
    public final Object target;
    public final String name;

    Pre32TypedReference(Class type, Object target, String name) {
        this.type = type;
        this.target = target;
        this.name = name;
        if (type == null)
            throw new IllegalArgumentException("A non-null concrete type must be provided");
        if (name == null && target == null)
            throw new IllegalArgumentException("Only named references may have a null target");
    }

    Pre32TypedReference(Class type, Object target) {
        this(type, target, null);
    }
}
