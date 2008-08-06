/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config;

import com.l7tech.objectmodel.Entity;

import java.io.Serializable;

/**
 * A bean that generically implements the "knob pattern" for attaching additional runtime configuration to entities so
 * that the same entity can be used in different runtime contexts without either creating additional subclasses or
 * polluting the parent bean API.
 *
 * @param <TT> the type of "type tag" that is used to categorize features within a given parent, usually an {@link Enum}.
 * @param <PT> the type of parent entity to which the feature is attached
 *  
 * @author alex
 */
public abstract class Feature<PT extends Entity, TT extends Serializable & Comparable> {
    /** The parent entity that owns this feature */
    private final PT parent;

    /** The tag value that distinguishes features within a given parent entity */
    private final TT tag;

    /** True if this type of feature can only be added once to each parent entity instance. */
    private final boolean unique;

    protected Feature(PT parent, TT tag, boolean unique) {
        this.parent = parent;
        this.tag = tag;
        this.unique = unique;
    }

    public PT getParent() {
        return parent;
    }

    public TT getTag() {
        return tag;
    }
                               
    public boolean isUnique() {
        return unique;
    }
}
