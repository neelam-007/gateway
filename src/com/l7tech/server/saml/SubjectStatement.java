/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server.saml;

import com.l7tech.common.security.saml.SamlConstants;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;

import java.security.cert.X509Certificate;

/**
 * The <code>SubjectStatement</code> class describes the subject that the SAML statement
 * is about. This is passed to <code>SamlAssertionGenerator</code> methods that generate
 * assertions.
 *
 * @author emil
 * @version Jan 31, 2005
 * @see SamlAssertionGenerator
 */
public class SubjectStatement {
    /**
     * holder of key confirmation type
     */
    public static final Confirmation HOLDER_OF_KEY = new Confirmation(SamlConstants.CONFIRMATION_HOLDER_OF_KEY);
    /**
     * sender vouches confirmation type
     */
    public static final Confirmation SENDER_VOUCHES = new Confirmation(SamlConstants.CONFIRMATION_SENDER_VOUCHES);
    private String confirmationMethod = null;
    private String nameQualifier = null;
    private String nameFormat = null;
    private String name = null;
    private String subjectConfirmationData = null;
    private Object keyInfo = null;

    /**
     * Creates the authentication statement
     *
     * @param credentials  the credentials source for subject statement
     * @param confirmation the confirmation method type
     * @return the authentication statement for the subject statement, confirmation and method
     */
    public static SubjectStatement createAuthenticationStatement(LoginCredentials credentials,
                                                                 Confirmation confirmation) {
        return new AuthenticationStatement(credentials, confirmation);
    }

    /**
     * Creates the authorization statement
     *
     * @param credentials  the credentials source for subject statement
     * @param confirmation the confirmation method type
     * @return the sender vouches subject statement
     */
    public static SubjectStatement createAuthorizationStatement(LoginCredentials credentials,
                                                                Confirmation confirmation,
                                                                String resource, String action, String actionNamespace) {
        return new AuthorizationStatement(credentials, confirmation, resource, action, actionNamespace);
    }

    /**
     * Protected cxonstructor for subclassing that populates the subject statement properties
     * @param credentials  the source of this subject statement
     * @param confirmation  the cvo
     */
    protected SubjectStatement(LoginCredentials credentials, Confirmation confirmation) {
        if (credentials == null) {
            throw new IllegalArgumentException();
        }
        if (credentials.getLogin() == null) {
            throw new IllegalArgumentException("Login name is required");
        }
        setName(credentials.getLogin());
        setConfirmationMethod(confirmation.method);

        CredentialFormat format = credentials.getFormat();
        if (confirmation.equals(HOLDER_OF_KEY)) {
            final X509Certificate clientCert = credentials.getClientCert();
            if (!CredentialFormat.CLIENTCERT.equals(format) || clientCert == null) {
                throw new IllegalArgumentException("Credential format of Client cert and client certificate are required");
            }
        }

        if (CredentialFormat.CLIENTCERT.equals(format)) {
            setKeyInfo(credentials.getClientCert());
            setNameFormat(SamlConstants.NAMEIDENTIFIER_X509_SUBJECT);
        } else {
            setNameFormat(SamlConstants.NAMEIDENTIFIER_UNSPECIFIED);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getConfirmationMethod() {
        return confirmationMethod;
    }

    public void setConfirmationMethod(String confirmationMethod) {
        this.confirmationMethod = confirmationMethod;
    }

    /**
     * @return whether this subject statement represents holder of key
     *         confirmation method
     */
    public boolean isConfirmationHolderOfKey() {
        return HOLDER_OF_KEY.method.equals(confirmationMethod);
    }

    /**
     *
     * @return whether this subject statement represents the sender vouches
     *         confirmation method
     */
    public boolean isConfirmationSenderVouches() {
        return SENDER_VOUCHES.equals(confirmationMethod);
    }

    public Object getKeyInfo() {
        return keyInfo;
    }

    public void setKeyInfo(Object keyInfo) {
        this.keyInfo = keyInfo;
    }

    public String getNameFormat() {
        return nameFormat;
    }

    public void setNameFormat(String nameFormat) {
        this.nameFormat = nameFormat;
    }

    public String getNameQualifier() {
        return nameQualifier;
    }

    public void setNameQualifier(String nameQualifier) {
        this.nameQualifier = nameQualifier;
    }

    public String getSubjectConfirmationData() {
        return subjectConfirmationData;
    }

    public void setSubjectConfirmationData(String subjectConfirmationData) {
        this.subjectConfirmationData = subjectConfirmationData;
    }

    /**
     * The confirmation method. Sender vouches, holder of key etc.
     */
    public static class Confirmation {
        private final String method;

        private Confirmation(String method) {
            this.method = method;
        }

        /**
         * Returns a hash code value for the object.
         * The method is implemented to satisfy general contract of <code>hashCode</code>
         * and <code>equals</code>.
         *
         * @return a hash code value for this object.
         * @see Object#equals(Object)
         */
        public int hashCode() {
            return method.hashCode();
        }


        /**
         * Indicates whether some other object is "equal to" this one.
         *
         * @param that the reference object with which to compare.
         * @return <code>true</code> if this object is the same as the obj
         *         argument; <code>false</code> otherwise.
         * @see #hashCode()
         */
        public boolean equals(java.lang.Object that) {
            if (that == this) return true;
            if (!(that instanceof Confirmation)) return false;
            return this.method.equals(((Confirmation)that).method);
        }
    }


}