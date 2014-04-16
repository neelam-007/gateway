/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.protocol;

import com.l7tech.util.ConfigFactory;

/**
 * Holds the constants needed for communication between Bridge, Console and Gateway, and provides some
 * documentation of the protocols used.
 *
 * User: mike
 * Date: Sep 2, 2003
 * Time: 4:15:04 PM
 */
public class SecureSpanConstants {

    /** The HTTP user agent sent by the current version of the Bridge. */
    public static final String USER_AGENT = "L7 Bridge; Protocol v2.0";

    /** The MIME type of the X.509 certificate sent by the certificate discovery server. */
    public static final String CERTIFICATE_MIME_TYPE = "application/x-x509-ca-cert";

    public static final String SSG_RESERVEDURI_PREFIX = "/ssg";

    /**
     * the regular expression that extracts the service oid or goid at the end of the original url ir request URI
     * Each regex must match the service OID or GOID as match group #1 or else fail
     */
    public static final String[] RESOLUTION_BY_ID_REGEXES = {
        "^/service/(\\d{1,20})$",
        "^/service/([0-9a-fA-F]{32})$",
    };
    public static final String[] RESOLUTION_BY_ID_REGEXES_PRE_8_1 = {
            "/service/(\\d{1,20})$",
            "/service/([0-9a-fA-F]{32})$",
    };

    public static boolean USE_RESOLUTION_BY_ID_PRE_8_1 = ConfigFactory.getBooleanProperty("com.l7tech.common.serviceResolutionByIdPre81", false);

    public static String[] getResolutionByIdRegexes(){
        return USE_RESOLUTION_BY_ID_PRE_8_1 ? RESOLUTION_BY_ID_REGEXES_PRE_8_1 : RESOLUTION_BY_ID_REGEXES;
    }

    /**
     * The filename portion of the URL of the message processing service on the Gateway.
     */
    public static final String SSG_FILE = SSG_RESERVEDURI_PREFIX + "/soap";

    /**
     * The prefix of the filename portion of the URL of the message processing service that requires a service OID in the URL.
     * The full URL will end in something like "/ssg/service/283761".
     */
    public static final String SERVICE_FILE = "/service/";

    /**
     * The filename portion of the URL of the policy servlet on the Gateway.
     * <p>
     * The policy servlet is the service that client can use to look up the policy that must be followed in order
     * to access a particular server.
     */
    public static final String POLICY_SERVICE_FILE = SSG_RESERVEDURI_PREFIX + "/policy/disco";

    /**
     * The filename portion fo the URL of the policy servlet for pre-3.2 Gateways and Bridges,
     * here for backward and forward compatibility.
     */
    public static final String PRE32_POLICY_SERVICE_FILE = SSG_RESERVEDURI_PREFIX + "/policy/disco.modulator";

    /**
     * The  filename portion of the URL of the certificate discovery server on the Gateway.
     * <p>
     * The certificate discovery servlet is the service that clients (Bridge or Console) can use to download
     * the Gateway's CA cert over unencrypted HTTP, while remaining confident that it arrived unmodified and was
     * transmitted by someone able to prove that they know the client's password.
     */
    public static final String CERT_PATH = SSG_RESERVEDURI_PREFIX + "/policy/disco.modulator";

    /**
     * The filename portion of the URL of the certificate signing service on the Gateway.
     * This is the service that clients (currently just the Bridge) can use to obtain a new client certificate.
     * The client must post an encoded PKCS10CertificationRequest to this url, over SSL, and providing
     * a valid username and password using HTTP Basic authentication.  After verifying that the credentials
     * are correct, that the user is permitted to obtain a certificate, and that the CSR is in the correct format
     * (with the proper username), this service will sign the CSR and return the new client certificate in
     * the response.
     */
    public static final String CERT_REQUEST_FILE = SSG_RESERVEDURI_PREFIX + "/csr";

    /**
     * The filename portion of the URL of the Gateway's WSDL proxy service.
     * This is the service that clients (Bridge or third-party) can connect to in order to get
     * a WSIL document listing public services; or (if authenticated) a WSIL document listing
     * services available to that authenticated user; or, if a serviceoid parameter is provided,
     * to download the WSDL for that service.
     *
     * URLs in download WSIL and WSDL documents will have been altered to point at the Gateway,
     * and will have any back-end URL information blinded.
     *
     * Example:  http://gateway.example.com:8080/ssg/wsdl?serviceoid=4653058
     */
    public static final String WSDL_PROXY_FILE = SSG_RESERVEDURI_PREFIX + "/wsdl";

    /**
     * The filename portion of the URL for the Gateway's password changing service.
     * The Bridge calls this servlet to change the password of an internal account.
     */
    public static final String PASSWD_SERVICE_FILE = SSG_RESERVEDURI_PREFIX + "/passwd";

    /**
     * The filename portion of the URL for the Gateway's WS-Trust RequestSecurityToken service.
     * Used for WS-SecureConversation sessions and for obtaining SAML tokens.
     * The Bridge calls this servlet with a signed soap request for a new WS-SC SecurityContext,
     * or for a new SAML token.
     */
    public static final String TOKEN_SERVICE_FILE = SSG_RESERVEDURI_PREFIX + "/token";

    /**
     * The console compares this value with the value returned by IdentityAdmin.echoVersion()
     * this ensures that the console can talk to the server.
     */
    public static final String ADMIN_PROTOCOL_VERSION = "20060228";

    public static final String CERT_INVALID = "invalid";
    public static final String CERT_VALID = "valid";

    /**
     * @see com.l7tech.server.identity.AuthenticationResult#isCertSignedByStaleCA()
     */
    public static final String CERT_STALE = "stale";

    /** SOAP faultcode for a stale or invalid WS-SecureConversation Security Context Token. */
    public static final String FAULTCODE_BADCONTEXTTOKEN = "wsc:BadContextToken";

    /** SOAP faultcode: "An error was discovered processing the Security header". */
    public static final String FAULTCODE_INVALIDSECURITY = "wsse:InvalidSecurity";

    /** SOAP faultcode: "The issuer of an assertion is not acceptable to the receiver". */
    public static final String FAULTCODE_INVALIDSECURITYTOKEN = "wsse:InvalidSecurityToken";

    /**
     * SOAP faultcode: "A referenced SAML assertion could not be retrieved." -saml token profile.
     *  "Referenced security token could not be retrieved." -wss 1.1
     */
    public static final String FAULTCODE_SECURITYTOKENUNAVAILABLE = "wsse:SecurityTokenUnavailable";

    /**
     * SOAP faultcode:
     *   "An assertion contains a saml:Condition element that the receiver does not understand."  or,
     *   "The receiver does not understand the extension schema used in an assertion."
     */
    public static final String FAULTCODE_UNSUPPORTEDSECURITYTOKEN = "wsse:UnsupportedSecurityToken";

    /** SOAP faultcode: "A signature within an assertion or referencing an assertion is invalid." */
    public static final String FAULTCODE_FAILEDCHECK = "wsse:FailedCheck";

    /** SOAP faultcode: "The security token could not be authenticated or authorized." */
    public static final String FAULTCODE_FAILEDAUTH = "wsse:FailedAuthentication";

    /** Namespace for reporting extra info in Layer 7's SAML SOAP faults. */
    public static final String FAULTDETAIL_SAML_NS = "http://layer7tech.com/saml";

    /** Element for reporting extra info in Layer 7's SAML SOAP faults. */
    public static final String FAULTDETAIL_SAML = "SamlFaultInfo";

    /** Element within {@link #FAULTDETAIL_SAML} for holding the problematic assertion ID in L7 SAML SOAP faults. */
    public static final String FAULTDETAIL_SAML_ASSERTIONID = "AssertionID";

    /** Elemetn within {@link #FAULTDETAIL_SAML} for holding the reason for the failure in L7 SAML SOAP faults. */
    public static final String FAULTDETAIL_SAML_REASON = "Reason";

    /** SAML failure reason: a condition did not hold true (ie, timestamp expired or not yet valid) */
    public static final String FAULTDETAIL_SAML_REASON_CONDITION = "Condition";

    /** SAML failure reason: a signature was not valid */
    public static final String FAULTDETAIL_SAML_REASON_SIGNATURE = "Signature";

    /** SAML failure reason: the issuer was not acceptable to the recipient. */
    public static final String FAULTDETAIL_SAML_REASON_ISSUER = "Issuer";

    /** SAML failure reason: the SAML assertion contained an unrecognized extension. */
    public static final String FAULTDETAIL_SAML_REASON_FORMAT = "Format";

    /**
     * Special HTTP query parameters used by the protocol used between the SecureSpanBridge and the SecureSpanGateway.
     */
    public static class HttpQueryParameters {
        /**
         * Contains the OID of the published service whose policy the Bridge is attempting to obey, possibly
         * in the form of a policy version string (which looks like "oid|version" where version is the generation
         * count, updated by the database whenever the policy changes).
         *
         * <h3>Usages:<ul>
         * <li>Sent by the Bridge to the policy servlet as part of the policy URL.  See POLICYURL_HEADER in {@link HttpHeaders}.
         * <li>Sent by the Bridge to the Gateway's WSDL proxy servlet when a specified WSDL is requested.
         * </ul>
         */
        public static final String PARAM_SERVICEOID = "serviceoid";

        /**
         * WSDL proxy servlet extra parameter prefix.
         *
         * <p>The name suggests this is just for service document related
         * parameters, but this is not necessarily the case.</p>
         */
        public static final String PARAM_WSDL_PREFIX = "servdoc";

        /**
         * Contains the OID of the service document WSDL dependency.
         *
         * <p>Sent by the Bridge to the Gateway's WSDL proxy servlet when a
         * specified WSDL is requested.</p>
         */
        public static final String PARAM_SERVICEDOCOID = "servdocoid";        

        /**
         * When downloading WSDL, this parameter can be used as an alternative to
         * SecureSpanConstants.HttpQueryParameters.PARAM_SERVICEOID and represents the
         * routing uri property of a published service whose WSDL is desired.
         * @see com.l7tech.server.WsdlProxyServlet
         */
        public static final String PARAM_URI = "uri";

        /**
         * When downloading WSDL, this parameter can be used as an alternative to
         * SecureSpanConstants.HttpQueryParameters.PARAM_SERVICEOID and represents a
         * soapaction used by the published service whose WSDL is desired.
         * @see com.l7tech.server.WsdlProxyServlet
         */
        public static final String PARAM_SACTION = "soapaction";

        /**
         * When downloading WSDL, this parameter can be used as an alternative to
         * SecureSpanConstants.HttpQueryParameters.PARAM_SERVICEOID and represents a
         * namespace used by the published service whose WSDL is desired.
         * @see com.l7tech.server.WsdlProxyServlet
         */
        public static final String PARAM_NS = "ns";

        /**
         * When getting WSIL document, this indicates whether the WSDL URL produced should
         * use queries with PARAM_SERVICEOID or a combination or PARAM_URI, PARAM_SACTION and
         * PARAM_NS.
         * Omiting this param means use serviceoid. A value of 'res' means use later option.
         *
         * @see com.l7tech.server.WsdlProxyServlet
         */
        public static final String PARAM_MODE = "mode";

        /**
         * Contains a nonzero value if the Bridge desires to perform certificate discovery.
         * TODO: This is a total mess; cert discovery should be a different servlet, not piggyback on policy servlet
         *
         * <h3>Usages:<ul>
         * <li>Sent by the Bridge to the policy servlet to indicate that it actually wants to talk to the certificate discovery servlet.
         *     Note that this is not the same as the CSR (certificate signing) servlet: the certificate discovery servlet is used
         *     by the Bridge to discover the Gateway's server certificate, while the CSR servlet is used by the Bridge to apply
         *     for a client certificate.
         * </ul>
         */
        public static final String PARAM_GETCERT = "getcert";

        /**
         * Holds the username making a certificate discovery request.
         *
         * <h3>Usages:<ul>
         * <li>Sent by the Bridge to the certificate discovery servlet (inside the policy servlet).  The Gateway
         *     then provides hashes of the returned certificate hashed with the passwords of each user with a
         *     matching username from each known ID provider, and the Bridge can thereby verify that the certificate
         *     was provided unmodified and by a Gateway that knows his password.  See CERT_CHECK_PREFIX in {@link HttpHeaders}.
         * </ul>
         */
        public static final String PARAM_USERNAME = "username";

        /**
         * Holds a random nonce value used to make it harder to cheat during certificate discovery.
         *
         * <h3>Usages:<ul>
         * <li>Sent by the Bridge to the certificate discovery servlet (inside the policy servlet).  The gateway includes
         *     this nonce in its protective hashes of the returned certificate with the users password(s).  This makes
         *     it infeasible for a hostile Gateway-impersonator to precompute hashes of its certificate with likely passwords.
         *     See CERT_CHECK_PREFIX in {@link HttpHeaders}.
         * </ul>
         */
        public static final String PARAM_NONCE = "nonce";

        /**
         * Parameter alongside a policy download URL that is meant to indicate that the requestor wants to get back
         * the full (non-filtered) policy document. This will be used when publishing service description and associated
         * policy document to a Systinet (or other UDDI implementation) registry. Usage is &fulldoc=yes
         */
        public static final String PARAM_FULLDOC = "fulldoc";

        /**
         * Parameter alongside a policy download URL that is meant to indicate that the requestor wants to get back
         * the policy document as inlined or not inlined.  This will be used when publishing service description
         * and associated policy document to a Systinet (or other UDDI implementation) registry.
         *
         * Usages: &inline
         * Possible values: yes or no
         * Eg. &inline=no
         */
        public static final String PARAM_INLINE = "inline";

        /**
         * Parameter to request that comments are included. Must have a value of yes (any case)
         * By default comments are not included.
         * Only applies when a full doc download is applicable.
         */
        public static final String PARAM_INCLUDE_COMMENTS = "comments";
    }

    /**
     * Special HTTP headers used by the protocol used between the SecureSpanBridge and the SecureSpanGateway.
     */
    public static class HttpHeaders {
        /**
         * Contains the version number of the policy used and/or expected by the entity providing the header.
         * This number is used to identify a particular generation of a particular service policy -- ie,
         * it's incremented whenever an administrator edits a policy and then saves his changes --
         * it doesn't identify which policy assertions are used or expected to be available or any
         * other protocol versioning.
         * <p>
         * The Bridge does not attempt to interpret this value -- it is treated as an opaque string.  The Gateway
         * currently uses the form "oid|version" where OID is the published service object ID and version is the
         * generation count.
         * <p/>
         * If a request contains more than one copy of this header, the recipient shall either signal an use the first copy
         * and ignore any subsequent copies.
         *
         * <h3>Usages:<ul>
         * <li>Returned by the policy servlet to the Bridge along with every policy download.
         * <li>Sent by the Bridge to the Gateway along with every request that was processed by a policy.
         * </ul>
         */
        public static final String POLICY_VERSION = "L7-policy-version";

        /**
         * This header is returned by the ssg with value "invalid" when a client cert used for authentication was
         * no longer valid.
         *
         * <h3>Usages:<ul>
         * <li>Returned by the Gateway to the Bridge in reply to any request whose body was signed by an invalid
         * client certificate, or that was made over SSL and presented an invalid client certificate during
         * the SSL handshake.
         * </ul>
         */
        public static final String CERT_STATUS = "L7-cert-status";

        /**
         * Contains the URL of the policy that should be applied to a given request.  When included in
         * a response from the message processing servlet, this header is an explicit request for the client
         * to download a new policy.  May also be returned by the policy servlet if it wants an anonymous
         * HTTP client to retry a policy download using HTTPS + Basic auth.
         *
         * <h3>Usages:<ul>
         * <li>Returned by the Gateway to the Bridge in every reponse to a request that either included
         * an incorrect policy version header, or whose behaviour failed to conform to the policy.
         *
         * <li>Returned by the policy servlet to the Bridge if it wants the Bridge to retry a policy download
         * using HTTP Basic auth over SSL.
         * </ul>
         */
        public static final String POLICYURL_HEADER = "L7-Policy-URL";

        /**
         * Contains a reconstruction of the original URL the end user client was connecting to when it
         * made contact with the Client Proxy.  This is passed on to the SSG in case the SSG wishes to
         * use the information for service routing.
         * <p/>
         * If a request contains more than one copy of this header, the recipient shall use the first copy
         * and ignore any subsequent copies.
         *
         * <h3>Usages:<ul>
         * <li>Sent by the Bridge to the Gateway along with every request.
         * </ul>
         */
        public static final String ORIGINAL_URL = "L7-Original-URL";

        /**
         * <b>Note: Obsolete as of SecureSpan 6.0</b>
         *
         * Contains a hash value used for checking certificate validity.  These headers are provided in the
         * response from the SSG's certificate discovery server (PolicyServlet) so that the client
         * (ie, the XVC) can decide whether it trusts the certificate being downloaded.
         *
         * <p>This is only the prefix of the full header name that will appear in the respone -- the full header
         * name for each cert check will be this prefix followed by the numeric ID of an authentication provider that
         * contained a matching username.  The value of the header will be the hex encoded MD5 digest
         * of the raw bytes of the cert (exactly as returned by the body of the response associated with
         * this header) + H(A1), where H(A1) is the MD5 of "username:realm:password".  The hex digest
         * will be followed by a semicolon, a space, and the realm name.
         *
         * <p>Here is a complete example:  <pre>L7-Cert-Check--1: de615f787075c54bd19ba64da4128553; myrealm</pre>
         *
         * <p>This means it's the cert check header for auth provider -1 (the builtin provider).  The large hex
         * number is the MD5 value of (H(A1) . nonce . objectId . certificate_bytes . H(A1)), where
         * H(A1) is the MD5 of "alice:myrealm:secret".
         *
         * <p>The username was
         * provided to the certificate server with the request and is thus already known to the client.
         * The realm is returned in the plaintext since the client does not necessarily know it yet.
         * "secret" is the password that the client is supposed to know already; if both client and server agree
         * about the value of the password, the client can trust that the certificate bytes were provided by someone
         * that knows the H(A1) of his password and that they haven't been tampered with in transit.
         *
         * <p>If the password for this user with this identitity provider is not available to the Gateway, it will
         * return the string "NOPASS" instead of a valid hash.
         *
         * <p><b>Bugs</b>: a hostile SSG could trick the client into accepting a bogus certificate simply by including
         * one L7-Cert-Check header in the response for every possible password value (or, more realistically,
         * just the top NNN most likely passwords, where NNN is some number chosen so that the client won't time
         * out or run out of memory before it finishes downloading the reponse).  I'm not sure how to fix this.
         *
         * <h3>Usages:<ul>
         * <li>Zero or more headers with this prefix are returned by the certificate discovery servlet to the
         * XVC along with every download of the Gateway cluster's SSL certificate.
         * </ul>
         */
        public static final String LEGACY_CERT_CHECK_PREFIX = "L7-Cert-Check-";

        /**
         * New for SecureSpan 6.0.
         *
         * Contains a hash value used for checking certificate validity.  These headers are provided in the response
         * from the SSG's certificate discovery server (PolicyServlet) so that the client
         * (ie, the XVC) can decide whether it trusts the certificate being downloaded.
         *
         * <p>The full header name for each check2 header is this prefix followed by the decimal numeric ID of an identity
         * provider that contined a matching username.  This numeric value may be negative, in which case the header
         * name will contain a doubled dash, eg "L7-Cert-Check2--1:".
         *
         * <p>The header value will be in the form "SERVERNONCEHEX; CHECKHEX; SALTHEX" where SERVERNONCEHEX is a hex
         * encoded random byte array chosen by the server, CHECKHEX is the hex encoded SHA-512 hash
         * value computed as described below, and SALTHEX is the hex encoded bytes of the matching user's encoded password
         * hash salt (ie, hexdump("$6$hUk9FPX2boJCzryM")).
         *
         * <p>
         * The CHECKHEX is computed as SHA512(PRIV, SHA512(PRIV, CS, SS, CERT)) where:
         *   PRIV is the complete SHA512Crypt hashed password string for this user (including salt and "$6$" prefix, converted to bytes using UTF-8);
         *   CS is a client-chosen random nonce (delivered from the client to the server in the "nonce" URL parameter);
         *   SS is a server-chosen random nonce (delivered from the server to the client in the SERVERNONCEHEX value); and
         *   CERT is the encoded certificate bytes.
         *
         * <p>If a check2 header cannot be generated for this user on this Gateway (perhaps because the Gateway does not have
         * the user's hashed password available in SHA512Crypt format, or because server certificate discovery is disabled) the
         * Gateway will return a check2 header with the string "NOPASS" in place of CHECKHEX.
         *
         * <h3>Usages:<ul>
         * <li>Zero or more headers with this prefix are returned by the certificate discovery servlet to the
         * XVC along with every download of the Gateway cluster's SSL certificate.
         * </ul>
         */
        public static final String CERT_CHECK2_PREFIX = "L7-Cert-Check2-";

        /**
         * Sent by the client to the password service. This contains the Base-64 encoded value of the new password.
         * Passwords can only be changed for internal accounts. A successful password change will result in
         * the service returning a 200 code. Otherwise, the calling Bridge should not assume the password
         * change was successful.
         *
         * When requesting a password change, the Bridge must connect through https and must present his client
         * cert as part of the ssl handshake if he does happen to possess such a cert.
         *
         * As part of the password change, the existing client cert (if it exists) is revoked.
         */
        public static final String HEADER_NEWPASSWD = "L7-new-passwd";
//
//        /**
//         * Sent by the client to the password service.  This contains the Base-64 encoded value of the old password.
//         * The old password is needed because it needs to verify certain criteria from the old password that will affect
//         * on the new password.
//         */
//        public static final String HEADER_OLDPASSWD = "L7-old-passwd";
//
        /**
         * <p>Sent by the client to the message processor to indicate that Remote Domain Identity Injection
         * was attempted for this request.</p>
         * <p>It is illegal for more than one L7-Domain-ID-Status header to appear in the top-level headers of
         * a single message.</p>
         * <p>The value of this header is in the following format:
         * <pre>{status}; {identifier1}="{headername1}", {identifier2}="{headername2}", ...</pre></p>
         * <p>Where {status} is a status code as defined by {@link DomainIdStatusCode} (case-insensitive)
         * and {identifier1} through {identifierN} are the names of included peer identifier values, and {headername1}
         * through {headernameN} are the quoted (per MIME, as with parameters in a Content-Type header)
         * names of HTTP headers that contain the corresponding values.</p>
         * <p>The values of the headers themselves should be encoded per RFC 2047 if they need to contain characters
         * that are illegal in header values, such as tabs or non-ASCII characters.</p>
         * <p>It is illegal for the same identifier name to appear more than once in a single L7-Domain-ID-Status header.</p>
         * <p><b>Value escaping</b>: values that contain whitespace, colons, or non-ASCII characters must be encoded
         * per RFC 2047.  This can be done easily by running them through {@link javax.mail.internet.MimeUtility#encodeText},
         * which also has the advantage of leaving the value unchanged if it doesn't need to be encoded.</p>
         * <p>For example:
         * <pre>
         * L7-Domain-ID-Status: INCLUDED; username="X-Injected-User-Name", namespace="X-Injected-Domain-Name", program="X-Injected-Program-Name"
         * X-Injected-User-Name: joeblow
         * X-Injected-Domain-Name: SALES
         * X-Injected-Program-Name: acmewarehouseclient.exe
         * </pre></p>
         * <p>A more complex example, where MIME encoding has been used to send non-ASCII values:
         * <pre>
         * L7-Domain-ID-Status: INCLUDED; username="X-Injected-User-Name", namespace="X-Injected-Domain-Name", program="X-Injected-Program-Name"
         * X-Injected-User-Name: =?utf-8?q?jos=C3=A9hern=C3=A1ndez?=
         * X-Injected-Domain-Name: =?utf-8?q?ingenier=C3=ADa?=
         * X-Injected-Program-Name: =?utf-8?q?International_=D0=B6=E2=99=A5=C5=92.exe?=
         * </pre></p>
         * <p>The semicolon after {status} may be omitted if no identifier names are included.<p/>
         * <p>See {@link DomainIdStatusHeader} for a utility class to parse and create this header's value.</p>
         */
        public static final String HEADER_DOMAINIDSTATUS = "L7-Domain-ID-Status";
    }

    /**
     * HTTP request header for indicating the originating client when a request
     * is routed between cluster nodes. It should contain the value returned by
     * {@link javax.servlet.http.HttpServletRequest#getRemoteHost()} when
     * applied to the original request.
     *
     * <h3>Usages:</h3><ul>
     * <li>Used when {@link com.l7tech.server.BackupServlet} routes a request
     * to the target node.
     * <li>Used when {@link com.l7tech.server.PingServlet} routes a system info
     * request to the target node.
     * </ul>
     *
     * @since SecureSpan 4.3
     */
    public static final String HEADER_ORIGINAL_HOST = "L7-Original-Host";

    /**
     * HTTP request header for indicating the originating client when a request
     * is routed between cluster nodes. It should contain the value returned by
     * {@link javax.servlet.http.HttpServletRequest#getRemoteAddr()} when
     * applied to the original request.
     *
     * <h3>Usages:</h3><ul>
     * <li>Used when {@link com.l7tech.server.BackupServlet} routes a request
     * to the target node.
     * <li>Used when {@link com.l7tech.server.PingServlet} routes a system info
     * request to the target node.
     * </ul>
     *
     * @since SecureSpan 4.3
     */
    public static final String HEADER_ORIGINAL_ADDR = "L7-Original-Addr";

    /**
     * This is the value included as the Hash in a L7-Cert-Check-NNN: header when the real hash cannot be
     * computed because the user's password is unavailable to the Gateway, perhaps because only a one-way
     * hash of the password is stored in the database.
     */
    public static final String NOPASS = "NOPASS";

    /**
     * Default character encoding used by passwords on the SSG
     */
    public static final String PASSWORD_ENCODING = "UTF-8";
}
