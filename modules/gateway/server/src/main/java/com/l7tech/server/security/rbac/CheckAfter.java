/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.rbac;

/**
 * @author alex
*/
enum CheckAfter {
    /** Nothing to check after invocation */
    NONE,

    /** Return value must be Entity; check permission to execute operation against it */
    ENTITY,

    /** Return value must be EntityHeader; check permission to execute operation against its referent Entity */
    HEADER,

    /**
     * Return value is a Collection&lt;Entity&gt;; check permission to execute operation
     * against all entities in collection
     */
    COLLECTION
}
