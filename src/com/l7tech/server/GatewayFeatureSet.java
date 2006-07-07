/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server;

import java.util.Set;
import java.util.List;
import java.util.Collections;
import java.util.Arrays;

/**
 * @author mike
 */
public class GatewayFeatureSet {
    final String name;
    final String desc;
    final String note;
    final GatewayFeatureSet[] sets;

    public GatewayFeatureSet(String name, String desc, String note, GatewayFeatureSet[] sets) {
        if (name == null || name.trim().length() < 1) throw new IllegalArgumentException("Invalid set name: " + name);
        this.name = name;
        this.desc = desc;
        this.note = note;
        this.sets = sets != null ? sets : new GatewayFeatureSet[0];
    }

    public GatewayFeatureSet(String name, String desc) {
        this(name, desc, null, new GatewayFeatureSet[0]);
    }

    public String getNote() {
        return note == null ? "" : note;
    }

    /** @return true if the specified feature set is containing in this or any subset.  This does a deep search and may be slow.  */
    public boolean contains(String name) {
        if (name.equals(this.name)) return true;
        for (GatewayFeatureSet featureSet : sets)
            if (featureSet.contains(name)) return true;
        return false;
    }

    /** Collect the names of this feature set and any sub sets it enables into the specified set. */
    public void collectAllFeatureNames(Set<String> collector) {
        for (GatewayFeatureSet subset : sets)
            subset.collectAllFeatureNames(collector);
        collector.add(name);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return desc;
    }

    /** @return read-only view of child feature sets implied by this parent set.  May be empty but never null. */
    public List<GatewayFeatureSet> getChildren() {
        return Collections.unmodifiableList(Arrays.asList(sets));
    }
}
