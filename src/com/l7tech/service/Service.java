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
    public Service( Set operations ) {
        _operations = operations;
    }

    protected Set _operations = Collections.EMPTY_SET;
}
