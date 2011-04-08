/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.server.identity;

/**
 * Type of authentication. Placed here as currently useful in internal and external ldap identity providers.
 */
public enum AuthenticationType {
    /**
     * Authenticate a user in a service policy
     */
    MESSAGE_TRAFFIC,
    /**
     * Authentication a user for administrative use
     */
    ADMINISTRATION_POLICY_MANAGER,

    /**
     * ESM only supports username + password
     */
    ADMINISTRATION_ESM
}
