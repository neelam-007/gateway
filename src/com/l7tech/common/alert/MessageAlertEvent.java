/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.common.alert;

import java.io.Serializable;

/**
 * Configuration for a {@link AlertEvent} that fires when interesting message processing events occur.
 */
public class MessageAlertEvent extends AlertEvent implements Serializable {
    /** OID of the {@link com.l7tech.service.PublishedService} this Trigger is interested in, or null for all services */
    private Long publishedServiceOid;

    /** OID of the {@link com.l7tech.identity.IdentityProviderConfig} this Trigger is interested in, or null for all providers. */
    private Long identityProviderOid;

    /** If not null and true, this Trigger will report on authentication failures against the IdentityProvider identified by {@link #identityProviderOid}. */
    private Boolean watchingAuthenticationFailures;

    /** OID or DN of the {@link com.l7tech.identity.User} this Trigger is interested in, or null for all users. Only meaningful if {@link #identityProviderOid} is set. */
    private String userId;

    /** OID or DN of the {@link com.l7tech.identity.Group} this Trigger is interested in, or null for all groups. Only meaningful if {@link #identityProviderOid} is set. */
    private String groupId;

    /** Classname of the {@link com.l7tech.policy.assertion.Assertion} this Trigger is interested in, or null for entire policies. */
    private String assertionClassname;

    /**
     * {@link com.l7tech.policy.assertion.AssertionStatus} this Trigger is interested in, or null for all results.
     * If {@link #assertionClassname} is null, this Trigger is interested in this status resulting from the entire policy.
     */
    private Integer assertionStatus;

    public Long getPublishedServiceOid() {
        return publishedServiceOid;
    }

    public void setPublishedServiceOid(Long publishedServiceOid) {
        this.publishedServiceOid = publishedServiceOid;
    }

    public Long getIdentityProviderOid() {
        return identityProviderOid;
    }

    public void setIdentityProviderOid(Long identityProviderOid) {
        this.identityProviderOid = identityProviderOid;
    }

    public Boolean isWatchingAuthenticationFailures() {
        return watchingAuthenticationFailures;
    }

    public void setWatchingAuthenticationFailures(Boolean watchingAuthenticationFailures) {
        this.watchingAuthenticationFailures = watchingAuthenticationFailures;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getAssertionClassname() {
        return assertionClassname;
    }

    public void setAssertionClassname(String assertionClassname) {
        this.assertionClassname = assertionClassname;
    }

    public Integer getAssertionStatus() {
        return assertionStatus;
    }

    public void setAssertionStatus(Integer assertionStatus) {
        this.assertionStatus = assertionStatus;
    }
}
