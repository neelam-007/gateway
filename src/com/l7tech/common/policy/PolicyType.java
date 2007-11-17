/**
 * Copyright (C) 2006-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.common.policy;

import static com.l7tech.common.policy.PolicyType.Supertype.SERVICE;
import static com.l7tech.common.policy.PolicyType.Supertype.*;

/**
 * Types of {@link Policy} objects
 *
 * Every type is a "subtype" of either {@link Supertype#FRAGMENT} or {@link Supertype#SERVICE}
 * 
 * @author alex
 */
public enum PolicyType {
    /**
     * A policy that belongs to a single PublishedService
     */
    PRIVATE_SERVICE(SERVICE, "Private Service Policy", true),

    /**
     * A policy that can be used by any PublishedService
     */
    SHARED_SERVICE(SERVICE, "Shared Service Policy", false),
    /**
     * A reusable fragment that can be included into another policy
     */
    INCLUDE_FRAGMENT(FRAGMENT, "Included Policy Fragment", true),

    /**
     * A fragment that gets run prior to the invocation of a {@link #SHARED_SERVICE} or {@link #PRIVATE_SERVICE} policy
     */
    PRE_SERVICE_FRAGMENT(FRAGMENT, "Pre-Service Policy Fragment", false),

    /**
     * A fragment that gets run after the invocation of a {@link #SHARED_SERVICE} or {@link #PRIVATE_SERVICE} policy
     */
    POST_SERVICE_FRAGMENT(FRAGMENT, "Post-Service Policy Fragment", false),

    /**
     * A fragment that gets run before any routing assertion is attempted
     */
    PRE_ROUTING_FRAGMENT(FRAGMENT, "Pre-Routing Policy Fragment", false),

    /**
     * A fragment that gets run after any successful routing assertion
     */
    SUCCESSFUL_ROUTING_FRAGMENT(FRAGMENT, "Successful Routing Policy Fragment", false),

    /**
     * A fragment that gets run after any failed routing assertion
     */
    FAILED_ROUTING_FRAGMENT(FRAGMENT, "Failed Routing Policy Fragment", false),
    /**
     * A fragment that gets run after a successful authentication
     */
    AUTHENTICATION_SUCCESS_FRAGMENT(FRAGMENT, "Successful Authentication Policy Fragment", false),

    /**
     * A fragment that gets run after an authentication failure
     */
    AUTHENTICATION_FAILURE_FRAGMENT(FRAGMENT, "Failed Authentication Policy Fragment", false),

    /**
     * A fragment that gets run after a user or group is successfully authorized
     */
    AUTHORIZATION_SUCCESS_FRAGMENT(FRAGMENT, "Successful Authorization Policy Fragment", false),

    /**
     * A fragment that gets run after an authorization failure
     */
    AUTHORIZATION_FAILURE_FRAGMENT(FRAGMENT, "Failed Authorization Policy Fragment", false),
    ;

    @Override
    public String toString() {
        return name;
    }

    public static enum Supertype {
        /** The "subtypes" tag policies as pertaining to whole services */
        SERVICE,

        /** The "subtypes" tag policies as being fragments */
        FRAGMENT
    }

    public boolean isServicePolicy() {
        return supertype == SERVICE;
    }

    public boolean isPolicyFragment() {
        return supertype == FRAGMENT;
    }

    private PolicyType(Supertype supertype, String name, boolean includeInGui) {
        this.supertype = supertype;
        this.name = name;
        this.shownInGui = includeInGui;
    }

    public String getName() {
        return name;
    }

    public Supertype getSupertype() {
        return supertype;
    }

    /**
     * @return <code>true</code> if this type should be presented as an option in the GUI, i.e. is currently supported
     */
    public boolean isShownInGui() {
        return shownInGui;
    }

    private final String name;
    private final Supertype supertype;
    private final boolean shownInGui;
}
