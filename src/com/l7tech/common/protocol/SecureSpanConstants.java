/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.protocol;

/**
 * Holds the constants needed for communication between Agent, Console and Gateway, and provides some
 * documentation of the protocols used.
 *
 * User: mike
 * Date: Sep 2, 2003
 * Time: 4:15:04 PM
 */
public class SecureSpanConstants {

    /** The HTTP user agent sent by the current version of the Agent. */
    public static final String USER_AGENT = "L7 Agent; Protocol v2.0";

    /** The MIME type of the X.509 certificate sent by the certificate discovery server. */
    public static final String CERTIFICATE_MIME_TYPE = "application/x-x509-ca-cert";

    /**
     * The filename portion of the URL of the message processing service on the Gateway.
     */
    public static final String SSG_FILE = "/ssg/soap";

    /**
     * The  filename portion of the URL of the policy servlet and the certificate discovery server on the Gateway.
     * <p>
     * The policy servlet is the service that client can use to look up the policy that must be followed in order
     * to access a particular server.
     * <p>
     * The certificate discovery servlet is the service that clients (Agent or Console) can use to download
     * the Gateway's CA cert over unencrypted HTTP, while remaining confident that it arrived unmodified and was
     * transmitted by someone able to prove that they know the client's password.
     * TODO: This is a total mess; cert discovery should be a different servlet, not piggyback on policy servlet
     */
    public static final String CERT_PATH = "/ssg/policy/disco.modulator";

    /**
     * The filename portion of the URL of the certificate signing service on the Gateway.
     * This is the service that clients (currently just the Agent) can use to obtain a new client certificate.
     * The client must post an encoded PKCS10CertificationRequest to this url, over SSL, and providing
     * a valid username and password using HTTP Basic authentication.  After verifying that the credentials
     * are correct, that the user is permitted to obtain a certificate, and that the CSR is in the correct format
     * (with the proper username), this service will sign the CSR and return the new client certificate in
     * the response.
     */
    public static final String CERT_REQUEST_FILE = "/ssg/csr";

    /**
     * The filename portion of the URL of the Gateway's WSDL proxy service.
     * This is the service that clients (Agent or third-party) can connect to in order to get
     * a WSIL document listing public services; or (if authenticated) a WSIL document listing
     * services available to that authenticated user; or, if a serviceoid parameter is provided,
     * to download the WSDL for that service.
     *
     * URLs in download WSIL and WSDL documents will have been altered to point at the Gateway,
     * and will have any back-end URL information blinded.
     *
     * Example:  http://gateway.example.com:8080/ssg/wsdl?serviceoid=4653058
     */
    public static final String WSDL_PROXY_FILE = "/ssg/wsdl";

    /**
     * The filename portion of the URL of the Gateways' session manager service.
     * This is the service that the Agent connects to in order to establish a new session.
     * This is <i>not</i> the same as the SSL-level session, if any.
     * See HttpHeaders.XML_SESSID_HEADER_NAME for additional information about the XML security session.
     */
    public static final String SESSION_SERVICE_FILE = "/ssg/xmlencsession";

    /**
     * The filename portion of the URL for the Gateway's password changing service.
     * The agent calls this servlet to change the password of an internal account.
     */
    public static final String PASSWD_SERVICE_FILE = "/ssg/passwd";

    /**
     * The console compares this value with the value returned by IdentityAdmin.echoVersion()
     * this ensures that the console can talk to the server.
     */
    public static final String ADMIN_PROTOCOL_VERSION = "20040603";

    public static final String INVALID = "invalid";
    public static final String VALID = "valid";

    /**
     * Special HTTP query parameters used by the protocol used between the SecureSpanAgent and the SecureSpanGateway.
     */
    public static class HttpQueryParameters {
        /**
         * Contains the OID of the published service whose policy the Agent is attempting to obey, possibly
         * in the form of a policy version string (which looks like "oid|version" where version is the generation
         * count, updated by the database whenever the policy changes).
         *
         * <h3>Usages:<ul>
         * <li>Sent by the Agent to the policy servlet as part of the policy URL.  See POLICYURL_HEADER in {@link HttpHeaders}.
         * <li>Sent by the Agent to the Gateway's WSDL proxy servlet when a specified WSDL is requested.
         * <li>Sent by the Agent to the XML security session servlet to provide context for XML security sessions.
         *     Note that, after this initial session setup, the session can be reused for requests involving completely
         *     different services as long as they are from the same Agent instance and sent to the same Gateway.
         * </ul>
         */
        public static final String PARAM_SERVICEOID = "serviceoid";

        /**
         * Contains a nonzero value if the Agent desires to perform certificate discovery.
         * TODO: This is a total mess; cert discovery should be a different servlet, not piggyback on policy servlet
         *
         * <h3>Usages:<ul>
         * <li>Sent by the Agent to the policy servlet to indicate that it actually wants to talk to the certificate discovery servlet.
         *     Note that this is not the same as the CSR (certificate signing) servlet: the certificate discovery servlet is used
         *     by the Agent to discover the Gateway's server certificate, while the CSR servlet is used by the Agent to apply
         *     for a client certificate.
         * </ul>
         */
        public static final String PARAM_GETCERT = "getcert";

        /**
         * Holds the username making a certificate discovery request.
         *
         * <h3>Usages:<ul>
         * <li>Sent by the Agent to the certificate discovery servlet (inside the policy servlet).  The Gateway
         *     then provides hashes of the returned certificate hashed with the passwords of each user with a
         *     matching username from each known ID provider, and the Agent can thereby verify that the certificate
         *     was provided unmodified and by a Gateway that knows his password.  See CERT_CHECK_PREFIX in {@link HttpHeaders}.
         * </ul>
         */
        public static final String PARAM_USERNAME = "username";

        /**
         * Holds a random nonce value used to make it harder to cheat during certificate discovery.
         *
         * <h3>Usages:<ul>
         * <li>Sent by the Agent to the certificate discovery servlet (inside the policy servlet).  The gateway includes
         *     this nonce in its protective hashes of the returned certificate with the users password(s).  This makes
         *     it infeasible for a hostile Gateway-impersonator to precompute hashes of its certificate with likely passwords.
         *     See CERT_CHECK_PREFIX in {@link HttpHeaders}.
         * </ul>
         */
        public static final String PARAM_NONCE = "nonce";
    }

    /**
     * Special HTTP headers used by the protocol used between the SecureSpanAgent and the SecureSpanGateway.
     */
    public static class HttpHeaders {
        /**
         * Contains the version number of the policy used and/or expected by the entity providing the header.
         * This number is used to identify a particular generation of a particular service policy -- ie,
         * it's incremented whenever an administrator edits a policy and then saves his changes --
         * it doesn't identify which policy assertions are used or expected to be available or any
         * other protocol versioning.
         * <p>
         * The Agent does not attempt to interpret this value -- it is treated as an opaque string.  The Gateway
         * currently uses the form "oid|version" where OID is the published service object ID and version is the
         * generation count.
         *
         * <h3>Usages:<ul>
         * <li>Returned by the policy servlet to the Agent along with every policy download.
         * <li>Sent by the Agent to the Gateway along with every request that was processed by a policy.
         * </ul>
         */
        public static final String POLICY_VERSION = "L7-policy-version";

        /**
         * This header is returned by the ssg with value "invalid" when a client cert used for authentication was
         * no longer valid.
         *
         * <h3>Usages:<ul>
         * <li>Returned by the Gateway to the Agent in reply to any request whose body was signed by an invalid
         * client certificate, or that was made over SSL and presented an invalid client certificate during
         * the SSL handshake.
         * </ul>
         */
        public static final String CERT_STATUS = "L7-cert-status";

        /**
         * Contains the ID of the session established to hold security context information needed
         * by the two parties conversing.  This information includes the server write and server read
         * symmetric keys, the message sequence number, the creation timestamp, and a count of the number
         * of messages sent within the session.  The client creates a session by sending a request
         * to the session server; both client and server then keep a copy of the session fields, and
         * the session exists as long as both sides agree to recognize it.
         *
         * <p>This session is used for XML message security and is <i>not</i> the same as the SSL-level
         * session, if any.
         *
         * <h3>Usages:<ul>
         * <li>Sent by the Agent to the Gateway along with every request that is encrypted or that
         * is expecting a signed response.
         * <li>Returned by the session servlet to the Agent in response to every successful session establishment request.
         * </ul>
         */
        public static final String XML_SESSID_HEADER_NAME = "L7-Session-Id";

        /**
         * Contains a random long integer generated by the entity making the request.  This can be
         * added to digitally signed replies to protect against replay attacks.
         *
         * <h3>Usages:<ul>
         * <li>Sent by the Agent to the Gateway along with every request that is expecting a signed response.
         * </ul>
         */
        public static final String XML_NONCE_HEADER_NAME = "L7-Nonce";

        /**
         * Contains an indication of the status of the session, either "valid" or "invalid".  The SSG sets
         * this header if it rejects a request because it contains a reference to a session ID that is
         * either unknown or expired.
         *
         * <h3>Usages:<ul>
         * <li>Returned by the Gateway to the Agent in every response to a request that contained a Session-ID
         * header referencing a missing or expired session.
         * </ul>                             f
         */
        public static final String SESSION_STATUS_HTTP_HEADER = "L7-session-status";

        /**
         * Contains the URL of the policy that should be applied to a given request.  When included in
         * a response from the message processing servlet, this header is an explicit request for the client
         * to download a new policy.  May also be returned by the policy servlet if it wants an anonymous
         * HTTP client to retry a policy download using HTTPS + Basic auth.
         *
         * <h3>Usages:<ul>
         * <li>Returned by the Gateway to the Agent in every reponse to a request that either included
         * an incorrect policy version header, or whose behaviour failed to conform to the policy.
         *
         * <li>Returned by the policy servlet to the Agent if it wants the Agent to retry a policy download
         * using HTTP Basic auth over SSL.
         * </ul>
         */
        public static final String POLICYURL_HEADER = "L7-Policy-URL";

        /**
         * Contains a reconstruction of the original URL the end user client was connecting to when it
         * made contact with the Client Proxy.  This is passed on to the SSG in case the SSG wishes to
         * use the information for service routing.
         *
         * <h3>Usages:<ul>
         * <li>Sent by the Agent to the Gateway along with every request.
         * </ul>
         */
        public static final String ORIGINAL_URL = "L7-Original-URL";

        /**
         * Contains a hash value used for checking certificate validity.  These headers are provided in the
         * response from the SSG's certificate discovery server (PolicyServlet) so that the client
         * (ie, the Agent or Manager) can decide whether it trusts the certificate being downloaded.
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
         * Agent along with every download of the Gateway cluster's CA certificate.
         * </ul>
         */
        public static final String CERT_CHECK_PREFIX = "L7-Cert-Check-";

        /**
         * Returned by the session servlet, and contains the key to be used for future signed or encrypted
         * client requests in the associated session.
         * See HttpHeaders.XML_SESSID_HEADER_NAME for additional information about the XML security session.
         *
         * This key consists of 32 bytes of random data generated by the session servlet; it is base64 encoded
         * in the header.
         *
         * <h3>Usages:<ul>
         * <li>Returned by the session servlet to the Agent in response to every successful session establishment request.
         * </ul>
         */
        public static final String HEADER_KEYREQ = "keyreq";

        /**
         * Returned by the session servlet, and contains the key to be used for future signed or encrypted
         * server replies in the associated session.
         * See HttpHeaders.XML_SESSID_HEADER_NAME for additional information about the XML security session.
         *
         * This key consists of 32 bytes of random data generated by the session servlet; it is base64 encoded
         * in the header.
         *
         * <h3>Usages:<ul>
         * <li>Returned by the session servlet to the Agent in response to every successful session establishment request.
         * </ul>
         */
        public static final String HEADER_KEYRES = "keyres";

        /**
         * Sent by the client to the password service. This contains the Base-64 encoded value of the new password.
         * Passwords can only be changed for internal accounts. A successful password change will result in
         * the service returning a 200 code. Otherwise, the calling agent should not assume the password
         * change was successful.
         *
         * When requesting a password change, the agent must connect through https and must present his client
         * cert as part of the ssl handshake if he does happen to possess such a cert.
         *
         * As part of the password change, the existing client cert (if it exists) is revoked.
         */
        public static final String HEADER_NEWPASSWD = "L7-new-passwd";
    }

    /**
     * This is the value included as the Hash in a L7-Cert-Check-NNN: header when the real hash cannot be
     * computed because the user's password is unavailable to the Gateway, perhaps because only a one-way
     * hash of the password is stored in the database.
     */
    public static final String NOPASS = "NOPASS";
}
