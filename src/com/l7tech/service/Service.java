/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service;

import java.util.*;

/**
 * @author alex
 */
public abstract class Service {
    public Service() {
    }

    public Service( String name, Set operations ) {
        _name = name;
        _operations = operations;
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        _name = name;
    }

    public String getDescription() {
        return _description;
    }

    public void setDescription(String description) {
        _description = description;
    }

    public Iterator operations() {
        return Collections.unmodifiableSet(_operations).iterator();
    }

    public Set getOperations() {
        return _operations;
    }

    public void setOperations(Set operations) {
        _operations = operations;
    }

    protected String _name;
    protected String _description;
    protected Set _operations = Collections.EMPTY_SET;
}
