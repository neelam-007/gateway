/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.rbac;

/**
 * @author alex
*/
enum CheckBefore {
    /** Nothing to check before invocation */
    NONE,

    /** Check permission to execute operation against entity with specified OID */
    OID,

    /** Check permission to execute operation against specified Entity */
    ENTITY,

    /** Check permission to execute operation against all entities of type */
    ALL
}
