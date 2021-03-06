/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config;

import java.util.Set;

/** @author alex */
public interface HasFeatures<FT extends Feature> {
    Set<FT> getFeatures();
}
