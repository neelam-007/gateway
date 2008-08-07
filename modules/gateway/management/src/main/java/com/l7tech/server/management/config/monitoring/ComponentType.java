/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.monitoring;

/**
 * Identifies the type of component that is the subject of a monitoring {@link Trigger}.
 *
 * @author alex
 */
public enum ComponentType {
    NODE,
    HOST,
    CLUSTER,
    ENTERPRISE_MANAGER
}
