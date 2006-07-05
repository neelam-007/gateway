/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common;

/**
 * Tag interface that represents a feature enabled by the LicenseManager.
 */
public interface Feature {
    /** @return the symbolic name of this feature, ie "assertion:xmlsec.SecureConversation".   Never null or empty. */
    String getName();

    /**
     * Feature for ADMIN catchall service lives here for now, since putting it into GatewayFeatureSets brings
     * the entire Gateway closure into the CustomAssertionsRegistrarImpl.
     */
    public static Feature ADMIN = new Feature() {
        public String getName() {
            return "service:Admin";
        }
    };
}
