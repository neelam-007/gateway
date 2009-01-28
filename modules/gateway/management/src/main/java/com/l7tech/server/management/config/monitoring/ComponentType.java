/**
 * Copyright (C) 2008-2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.monitoring;

/**
 * Identifies the type of component that is the subject of a monitoring {@link Trigger}.
 *
 * @author alex
 */
public enum ComponentType {
    /** An instance of the SSG software installed on some host; may be a member of a cluster. */
    NODE,

    /** A computer that hosts the SSG software (an appliance or not) */
    HOST,

    /** A cluster of SSG nodes */
    CLUSTER,

    /** An ESM instance */
    ENTERPRISE_MANAGER
}
