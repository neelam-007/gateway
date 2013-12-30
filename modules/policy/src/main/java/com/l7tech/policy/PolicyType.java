/**
 * Copyright (C) 2006-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy;

import java.util.*;

import static com.l7tech.policy.PolicyType.Supertype.SERVICE;
import static com.l7tech.policy.PolicyType.Supertype.*;

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
    PRIVATE_SERVICE(SERVICE, "Private Service Policy", false, true),

    /**
     * A policy that can be used by any PublishedService
     */
    SHARED_SERVICE(SERVICE, "Shared Service Policy", false),

    /**
     * A reusable fragment that can be included into another policy
     */
    INCLUDE_FRAGMENT(FRAGMENT, "Included Policy Fragment", true, true),

    /**
     * A global fragment runs before or after a service policy
     */
    GLOBAL_FRAGMENT(FRAGMENT, "Global Policy Fragment", true, getGlobalTags(), false ),

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

    /**
     * A policy that is for internal use (see {@link Policy#getInternalTag} for what kind of internal policy it is)
     */
    INTERNAL(FRAGMENT, "Internal Use Policy", true, getAuditMessageFilterTags(), false),

    /**
     * A policy that is invoked by a policy-backed identity provider in order to perform some operation (such as authentication or, perhaps someday, user lookup).
     * Currently not zoneable to help protect people from accidentally giving away the keys to the kingdom by inadvertently allowing ability to modify these policies.
     */
    IDENTITY_PROVIDER_POLICY(FRAGMENT, "Policy-Backed Identity Provider Policy Fragment", true, getIdentityProviderTags(), false),
    ;

    public static final String TAG_GLOBAL_MESSAGE_RECEIVED = "message-received";
    public static final String TAG_GLOBAL_PRE_SECURITY = "pre-security";
    public static final String TAG_GLOBAL_PRE_SERVICE = "pre-service";
    public static final String TAG_GLOBAL_POST_SECURITY = "post-security";
    public static final String TAG_GLOBAL_POST_SERVICE = "post-service";
    public static final String TAG_GLOBAL_MESSAGE_COMPLETED = "message-completed";

    public static final String TAG_AUDIT_MESSAGE_FILTER = "audit-message-filter";
    public static final String TAG_AUDIT_VIEWER = "audit-viewer";

    public static final String TAG_PIDP_PASSWORD_AUTH = "password-auth";

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
        this(supertype, name, includeInGui, null, false);
    }

    private PolicyType(Supertype supertype, String name, boolean includeInGui, boolean securityZoneable) {
        this(supertype, name, includeInGui, null, securityZoneable);
    }

    private PolicyType(Supertype supertype, String name, boolean includeInGui, Collection<String> guiTags, boolean securityZoneable) {
        this.supertype = supertype;
        this.name = name;
        this.shownInGui = includeInGui;
        this.guiTags = guiTags==null ? Collections.<String>emptySet() : Collections.unmodifiableSet( new TreeSet<String>(guiTags) );
        this.securityZoneable = securityZoneable;
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

    /**
     * Get the policy tags that should be shown in the GUI for this policy type.
     *
     * @return The set of tags (may be empty, never null)
     */
    public Set<String> getGuiTags() {
        return guiTags;
    }

    public static Collection<String> getAuditMessageFilterTags(){
        return Arrays.asList( TAG_AUDIT_MESSAGE_FILTER, TAG_AUDIT_VIEWER);
    }

    public static Collection<String> getIdentityProviderTags() {
        return Arrays.asList(TAG_PIDP_PASSWORD_AUTH);
    }

    /**
     * @return true if policies of this type should have their security zone (if any) honored by the zone predicate.
     */
    public boolean isSecurityZoneable() {
        return securityZoneable;
    }

    private static Collection<String> getGlobalTags() {
        return Arrays.asList(
            TAG_GLOBAL_MESSAGE_RECEIVED,
            TAG_GLOBAL_PRE_SECURITY,
            TAG_GLOBAL_PRE_SERVICE,
            TAG_GLOBAL_POST_SECURITY,
            TAG_GLOBAL_POST_SERVICE,
            TAG_GLOBAL_MESSAGE_COMPLETED );
    }

    private final String name;
    private final Supertype supertype;
    private final boolean shownInGui;
    private final Set<String> guiTags;
    private final boolean securityZoneable;
}
