package com.l7tech.external.assertions.samlpassertion;

import java.util.EnumSet;

/**
 * Status codes for SAML 1.x and 2.x Protocols.
 */
public enum SamlStatus {

    //- PUBLIC

    // SAML 1.x status
    SAML_SUCCESS(1, "Success"),
    SAML_VERSION_MISMATCH(1, "VersionMismatch"),
    SAML_REQUESTER(1, "Requester"),
    SAML_RESPONDER(1, "Responder"),
    SAML_REQUEST_VERSION_TOO_HIGH(1, "RequestVersionTooHigh"),
    SAML_REQUEST_VERSION_TOO_LOW(1, "RequestVersionTooLow"),
    SAML_REQUEST_VERSION_DEPRECATED(1, "RequestVersionDeprecated"),
    SAML_TOO_MANY_RESPONSES(1, "TooManyResponses"),
    SAML_REQUEST_DENIED(1, "RequestDenied"),
    SAML_RESOURCE_NOT_RECOGNIZED(1, "ResourceNotRecognized"),

    // SAML 2.x status
    SAML2_SUCCESS(2, "urn:oasis:names:tc:SAML:2.0:status:Success"),
    SAML2_REQUESTER(2, "urn:oasis:names:tc:SAML:2.0:status:Requester"),
    SAML2_RESPONDER(2, "urn:oasis:names:tc:SAML:2.0:status:Responder"),
    SAML2_VERSION_MISMATCH(2, "urn:oasis:names:tc:SAML:2.0:status:VersionMismatch"),
    SAML2_AUTHN_FAILED(2, "urn:oasis:names:tc:SAML:2.0:status:AuthnFailed"),
    SAML2_INVALID_ATTR_NAME_OR_VALUE(2, "urn:oasis:names:tc:SAML:2.0:status:InvalidAttrNameOrValue"),
    SAML2_INVALID_NAMEID_POLICY(2, "urn:oasis:names:tc:SAML:2.0:status:InvalidNameIDPolicy"),
    SAML2_NO_AUTHN_CONTEXT(2, "urn:oasis:names:tc:SAML:2.0:status:NoAuthnContext"),
    SAML2_NO_AVAILABLE_IDP(2, "urn:oasis:names:tc:SAML:2.0:status:NoAvailableIDP"),
    SAML2_NO_PASSIVE(2, "urn:oasis:names:tc:SAML:2.0:status:NoPassive"),
    SAML2_NO_SUPPORTED_IDP(2, "urn:oasis:names:tc:SAML:2.0:status:NoSupportedIDP"),
    SAML2_PARTIAL_LOGOUT(2, "urn:oasis:names:tc:SAML:2.0:status:PartialLogout"),
    SAML2_PROXY_COUNT_EXCEEDED(2, "urn:oasis:names:tc:SAML:2.0:status:ProxyCountExceeded"),
    SAML2_REQUEST_DENIED(2, "urn:oasis:names:tc:SAML:2.0:status:RequestDenied"),
    SAML2_REQUEST_UNSUPPORTED(2, "urn:oasis:names:tc:SAML:2.0:status:RequestUnsupported"),
    SAML2_REQUEST_VERSION_DEPRECATED(2, "urn:oasis:names:tc:SAML:2.0:status:RequestVersionDeprecated"),
    SAML2_REQUEST_VERSION_TOO_HIGH(2, "urn:oasis:names:tc:SAML:2.0:status:RequestVersionTooHigh"),
    SAML2_REQUEST_VERSION_TOO_LOW(2, "urn:oasis:names:tc:SAML:2.0:status:RequestVersionTooLow"),
    SAML2_RESOURCE_NOT_RECOGNIZED(2, "urn:oasis:names:tc:SAML:2.0:status:ResourceNotRecognized"),
    SAML2_TOO_MANY_RESPONSES(2, "urn:oasis:names:tc:SAML:2.0:status:TooManyResponses"),
    SAML2_UNKNOWN_ATTR_PROFILE(2, "urn:oasis:names:tc:SAML:2.0:status:UnknownAttrProfile"),
    SAML2_UNKNOWN_PRINCIPAL(2, "urn:oasis:names:tc:SAML:2.0:status:UnknownPrincipal"),
    SAML2_UNSUPPORTED_BINDING(2, "urn:oasis:names:tc:SAML:2.0:status:UnsupportedBinding");

    public static EnumSet<SamlStatus> getSaml1xStatuses() {
        return EnumSet.of(
                SAML_SUCCESS,
                SAML_VERSION_MISMATCH,
                SAML_REQUESTER,
                SAML_RESPONDER,
                SAML_REQUEST_VERSION_TOO_HIGH,
                SAML_REQUEST_VERSION_TOO_LOW,
                SAML_REQUEST_VERSION_DEPRECATED,
                SAML_TOO_MANY_RESPONSES,
                SAML_REQUEST_DENIED,
                SAML_RESOURCE_NOT_RECOGNIZED
            );
    }

    public static EnumSet<SamlStatus> getSaml2xStatuses() {
        return EnumSet.of(
                SAML2_SUCCESS,
                SAML2_REQUESTER,
                SAML2_RESPONDER,
                SAML2_VERSION_MISMATCH,
                SAML2_AUTHN_FAILED,
                SAML2_INVALID_ATTR_NAME_OR_VALUE,
                SAML2_INVALID_NAMEID_POLICY,
                SAML2_NO_AUTHN_CONTEXT,
                SAML2_NO_AVAILABLE_IDP,
                SAML2_NO_PASSIVE,
                SAML2_NO_SUPPORTED_IDP,
                SAML2_PARTIAL_LOGOUT,
                SAML2_PROXY_COUNT_EXCEEDED,
                SAML2_REQUEST_DENIED,
                SAML2_REQUEST_UNSUPPORTED,
                SAML2_REQUEST_VERSION_DEPRECATED,
                SAML2_REQUEST_VERSION_TOO_HIGH,
                SAML2_REQUEST_VERSION_TOO_LOW,
                SAML2_RESOURCE_NOT_RECOGNIZED,
                SAML2_TOO_MANY_RESPONSES,
                SAML2_UNKNOWN_ATTR_PROFILE,
                SAML2_UNKNOWN_PRINCIPAL,
                SAML2_UNSUPPORTED_BINDING
            );
    }

    public int getSamlVersion() {
        return samlVersion;
    }

    public String getValue() {
        return value;
    }

    //- PRIVATE

    private final int samlVersion;
    private final String value;
    
    private SamlStatus( final int samlVersion, final String value ) {
        this.samlVersion = samlVersion;
        this.value = value;
    }
}
