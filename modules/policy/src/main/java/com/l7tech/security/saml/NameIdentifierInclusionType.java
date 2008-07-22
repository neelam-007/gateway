/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.security.saml;

/**
 * @author alex
*/
public enum NameIdentifierInclusionType {
    /**
     * Don't include a NameIdentifier at all (e.g. for confidentiality reasons)
     * <p>
     * Note that in the SAML schema NameIdentifier is optional.
     */
    NONE,

    /**
     * NameIdentifer should be set to the result of the {@link com.l7tech.policy.assertion.SamlIssuerAssertion#nameIdentifierValue} expression
     */
    SPECIFIED,

    /**
     * NameIdentifier should be taken from the credentials used to authenticate the user
     */
    FROM_CREDS,

    /**
     * NameIdentifier should be taken from the user who was authenticated
     */
    FROM_USER
}
