/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.common.security.saml;

import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;

import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.HashMap;

/**
 * The <code>SubjectStatement</code> class describes the subject that the SAML statement
 * is about. This is passed to <code>SamlAssertionGenerator</code> methods that generate
 * assertions.
 *
 * @author emil
 * @see com.l7tech.common.security.saml.SamlAssertionGenerator
 */
public abstract class SubjectStatement {
    protected SubjectStatement() {
    }

    /**
     * holder of key confirmation type
     */
    public static final Confirmation HOLDER_OF_KEY = new Confirmation(SamlConstants.CONFIRMATION_HOLDER_OF_KEY);
    /**
     * sender vouches confirmation type
     */
    public static final Confirmation SENDER_VOUCHES = new Confirmation(SamlConstants.CONFIRMATION_SENDER_VOUCHES);
    /**
     * bearer confirmation type
     */
    public static final Confirmation BEARER = new Confirmation(SamlConstants.CONFIRMATION_BEARER);

    private String confirmationMethod = null;
    private String nameQualifier = null;
    private String nameFormat = null;
    private String name = null;
    private String subjectConfirmationData = null;
    private Object keyInfo = null;
    private KeyInfoInclusionType subjectConfirmationKeyInfoType = null;
    private NameIdentifierInclusionType nameIdentifierType = null;

    /**
     * Creates the authentication statement
     *
     * @param credentials  the credentials source for subject statement
     * @param confirmation the confirmation method type
     * @param keyInfoType
     * @param nameIdType
     * @param overrideNameValue a value to override the NameIdentifier value from the credentials
     * @param overrideNameFormat a value to override the NameIdentifier format from the credentials
     * @param nameQualifier
     * @return the authentication statement for the subject statement, confirmation and method
     */
    public static SubjectStatement createAuthenticationStatement(LoginCredentials credentials,
                                                                 Confirmation confirmation,
                                                                 KeyInfoInclusionType keyInfoType,
                                                                 NameIdentifierInclusionType nameIdType,
                                                                 String overrideNameValue,
                                                                 String overrideNameFormat,
                                                                 String nameQualifier)
    {
        return new AuthenticationStatement(credentials, confirmation, keyInfoType, nameIdType, overrideNameValue, overrideNameFormat, nameQualifier);
    }

    /**
     * Creates an attribute statement
     *
     * @param credentials  the credentials source for subject statement
     * @param confirmation the confirmation method type
     * @param keyInfoType
     * @param nameIdType
     * @param overrideNameValue a value to override the NameIdentifier value from the credentials
     * @param overrideNameFormat a value to override the NameIdentifier format from the credentials
     * @param nameQualifier
     * @return the authentication statement for the subject statement, confirmation and method
     */
    public static SubjectStatement createAttributeStatement(LoginCredentials credentials,
                                                            Confirmation confirmation,
                                                            String attributeName,
                                                            String attributeNamespaceOrFormat,
                                                            String attributeValue,
                                                            KeyInfoInclusionType keyInfoType,
                                                            NameIdentifierInclusionType nameIdType,
                                                            String overrideNameValue,
                                                            String overrideNameFormat,
                                                            String nameQualifier)
    {
        Attribute[] attributes = new Attribute[] {
            new Attribute(attributeName, attributeNamespaceOrFormat, attributeValue)
        };

        return new AttributeStatement(credentials, confirmation, attributes, keyInfoType, nameIdType, overrideNameValue, overrideNameFormat, nameQualifier);
    }

    /**
     * Creates an attribute statement with multiple attributes
     *
     * @param credentials  the credentials source for subject statement
     * @param confirmation the confirmation method type
     * @param keyInfoType if true, an X.509 subject will include just the thumbprintSHA1 of the subject cert.
     *                    Otherwise, it will contain the entire base64-encoded signing cert.
     * @param nameIdType
     * @param overrideNameValue a value to override the NameIdentifier value from the credentials
     * @param overrideNameFormat a value to override the NameIdentifier format from the credentials
     * @param nameQualifier
     * @return the authentication statement for the subject statement, confirmation and method
     */
    public static SubjectStatement createAttributeStatement(LoginCredentials credentials,
                                                            Confirmation confirmation,
                                                            Attribute[] attributes,
                                                            KeyInfoInclusionType keyInfoType,
                                                            NameIdentifierInclusionType nameIdType,
                                                            String overrideNameValue,
                                                            String overrideNameFormat,
                                                            String nameQualifier)
    {
        return new AttributeStatement(credentials, confirmation, attributes, keyInfoType, nameIdType, overrideNameValue, overrideNameFormat, nameQualifier);
    }


    /**
     * Creates the authorization statement
     *
     * @param credentials  the credentials source for subject statement
     * @param confirmation the confirmation method type
     * @param keyInfoType
     * @param nameIdType
     * @param overrideNameValue @return the sender vouches subject statement
     * @param overrideNameFormat
     * @param nameQualifier
     */
    public static SubjectStatement createAuthorizationStatement(LoginCredentials credentials,
                                                                Confirmation confirmation,
                                                                KeyInfoInclusionType keyInfoType,
                                                                String resource, String action, String actionNamespace,
                                                                NameIdentifierInclusionType nameIdType,
                                                                String overrideNameValue, String overrideNameFormat,
                                                                String nameQualifier)
    {
        return new AuthorizationStatement(credentials, confirmation, resource, action, actionNamespace, keyInfoType, nameIdType, overrideNameValue, overrideNameFormat, nameQualifier);
    }

    /**
     * Protected cxonstructor for subclassing that populates the subject statement properties
     *
     * @param credentials  the source of this subject statement
     * @param confirmation the cvo
     * @param keyInfoType
     * @param nameIdType
     * @param overrideNameValue
     * @param overrideNameFormat
     */
    protected SubjectStatement(LoginCredentials credentials,
                               Confirmation confirmation,
                               KeyInfoInclusionType keyInfoType,
                               NameIdentifierInclusionType nameIdType,
                               String overrideNameValue, 
                               String overrideNameFormat,
                               String nameQualifier)
    {
        if (credentials == null) throw new IllegalArgumentException();

        this.nameQualifier = nameQualifier;

        CredentialFormat format = credentials.getFormat();
        if (HOLDER_OF_KEY.equals(confirmation)) {
            final X509Certificate clientCert = credentials.getClientCert();
            if (!CredentialFormat.CLIENTCERT.equals(format) || clientCert == null) {
                throw new IllegalArgumentException("Credential format of Client cert and client certificate are required");
            }
        }

        setNameIdentifierType(nameIdType);
        setSubjectConfirmationKeyInfoType(keyInfoType);

        if (nameIdType != NameIdentifierInclusionType.NONE) {
            final X509Certificate clientCert = credentials.getClientCert();
            if (clientCert != null) {
                setKeyInfo(clientCert);
                setNameFormat(overrideNameFormat == null ? SamlConstants.NAMEIDENTIFIER_X509_SUBJECT : overrideNameFormat);
                if (overrideNameValue == null) {
                    setName(clientCert.getSubjectDN().getName());
                } else {
                    setName(overrideNameValue);
                }
            } else {
                setNameFormat(overrideNameFormat == null ? SamlConstants.NAMEIDENTIFIER_UNSPECIFIED : overrideNameFormat);
                final String login = credentials.getLogin();
                if (overrideNameValue == null) {
                    setName(login == null ? "" : login);
                } else {
                    setName(overrideNameValue);
                }
            }
        }

        setConfirmationMethod(confirmation == null ? null : confirmation.uri);
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
        return HOLDER_OF_KEY.uri.equals(confirmationMethod);
    }

    /**
     * @return whether this subject statement represents the sender vouches
     *         confirmation method
     */
    public boolean isConfirmationSenderVouches() {
        return SENDER_VOUCHES.uri.equals(confirmationMethod);
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

    public KeyInfoInclusionType getSubjectConfirmationKeyInfoType() {
        return subjectConfirmationKeyInfoType;
    }

    public void setSubjectConfirmationKeyInfoType(KeyInfoInclusionType subjectConfirmationKeyInfoType) {
        this.subjectConfirmationKeyInfoType = subjectConfirmationKeyInfoType;
    }

    public NameIdentifierInclusionType getNameIdentifierType() {
        return nameIdentifierType;
    }

    public void setNameIdentifierType(NameIdentifierInclusionType nameIdentifierType) {
        this.nameIdentifierType = nameIdentifierType;
    }

    /**
     * The confirmation method. Sender vouches, holder of key etc.
     */
    public static class Confirmation {
        private final String uri;

        private Confirmation(String uri) {
            this.uri = uri;
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
            return uri.hashCode();
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
            return this.uri.equals(((Confirmation)that).uri);
        }

        static final Map<String, Confirmation> confirmationMap = new HashMap<String, Confirmation>();
        static {
            confirmationMap.put(SamlConstants.CONFIRMATION_HOLDER_OF_KEY, HOLDER_OF_KEY);
            confirmationMap.put(SamlConstants.CONFIRMATION_SENDER_VOUCHES, SENDER_VOUCHES);
            confirmationMap.put(SamlConstants.CONFIRMATION_BEARER, BEARER);
            confirmationMap.put(SamlConstants.CONFIRMATION_SAML2_HOLDER_OF_KEY, HOLDER_OF_KEY);
            confirmationMap.put(SamlConstants.CONFIRMATION_SAML2_SENDER_VOUCHES, SENDER_VOUCHES);
            confirmationMap.put(SamlConstants.CONFIRMATION_SAML2_BEARER, BEARER);
        }

        public static Confirmation forUri(String uri) {
            return confirmationMap.get(uri);
        }

        public String getUri() {
            return uri;
        }
    }
}