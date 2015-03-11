/*
 * Copyright (C) 2004-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.audit;

import java.util.logging.Level;

/**
 * Message catalog for messages audited by policy assertions.
 * The ID range 4000-99999 inclusive is reserved for these messages.
 *
 * N.B. Don't change semantics or number of parameters! See MessagesTest.testMessageParameters()
 */
@SuppressWarnings({"UnusedDeclaration"})
public class AssertionMessages extends Messages {

    // Generic Assertion Messages (4330- 4399 reserved for use here)
    public static final Messages.M REQUESTWSS_NO_SECURITY = m(4302, Level.INFO, "Request did not contain any WSS level security");
    public static final Messages.M ASSERTION_MISCONFIGURED = m(4304, Level.WARNING, false, false, "Assertion configuration error: {0}"); // Provided as less-drastic alternative to throwing PolicyAssertionException
    public static final Messages.M MESSAGE_NOT_SOAP = m(4305, Level.WARNING, "{0} message not soap. {1}");
    public static final Messages.M NO_SUCH_PART = m(4306, Level.WARNING, "{0} message has no part {1}");
    public static final Messages.M MESSAGE_NOT_INITIALIZED = m(4307, Level.WARNING, "{0} message is not initialized");

    public static final M MESSAGE_TARGET_ERROR = m(4330, Level.WARNING, "Invalid target message, variable \"{0}\": {1}");
    public static final M MESSAGE_NOT_XML = m(4331, Level.WARNING, "{0} message not XML. {1}");

    // ServerHttpRoutingAssertion messages
    public static final M HTTPROUTE_SSL_INIT_FAILED = m(4000, Level.WARNING, "Could not initialize SSL Context");
    public static final M HTTPROUTE_BEGIN = m(4001, Level.INFO, "Route via HTTP(S) Assertion");
    public static final M HTTPROUTE_NON_SOAP_WRONG_FORMAT = m(4002, Level.INFO, "Requested option not supported by non-SOAP messages");
    public static final M HTTPROUTE_NON_SOAP_WRONG_POLICY = m(4003, Level.WARNING, "Option not supported by non-SOAP messages; check policy for errors");
    public static final M HTTPROUTE_PROMOTING_ACTOR = m(4004, Level.FINE, "Promoting actor {0}");
    public static final M HTTPROUTE_NO_SECURITY_HEADER = m(4005, Level.INFO, "Routing assertion requested promotion of security header with actor {0}, but no such header found in message");
    public static final M HTTPROUTE_ERROR_READING_RESPONSE = m(4006, Level.WARNING, true, false, "Error reading response");
    public static final M HTTPROUTE_CANT_RESOLVE_IP = m(4007, Level.WARNING, "Could not resolve client IP address");
    public static final M HTTPROUTE_TAI_NOT_AUTHENTICATED = m(4008, Level.FINE, "TAI credential chaining requested, but request was not authenticated");
    public static final M HTTPROUTE_TAI_CHAIN_USERNAME = m(4009, Level.FINE, "TAI credential chaining requested; will chain username {0}");
    public static final M HTTPROUTE_TAI_NO_USER_ID = m(4010, Level.WARNING, "TAI credential chaining requested, but requesting user does not have a unique identifier: ID is {0}");
    public static final M HTTPROUTE_TAI_CHAIN_LOGIN = m(4011, Level.FINE, "TAI credential chaining requested, but there is no user; will chain pc.login {0}");
    public static final M HTTPROUTE_TAI_NO_USER = m(4012, Level.WARNING, "TAI credential chaining requested, and request was authenticated, but had no user or pc.login");
    public static final M HTTPROUTE_ADD_OUTGOING_COOKIE = m(4013, Level.FINE, "Adding outgoing cookie: name = {0}");
    public static final M HTTPROUTE_LOGIN_INFO = m(4014, Level.FINE, "Using login ''{0}''");
    public static final M HTTPROUTE_OK = m(4015, Level.FINE, "Request routed successfully");
    public static final M HTTPROUTE_RESPONSE_STATUS = m(4016, Level.WARNING, true, true, "Protected service ({0}) responded with status {1}");
    public static final M HTTPROUTE_ADDCOOKIE_VERSION = m(4017, Level.FINE, "Adding outgoing cookie: name = {0}, version = {1}");
    public static final M HTTPROUTE_UPDATECOOKIE = m(4018, Level.FINE, "Updating cookie: name = {0}");
    /**
     * @deprecated
     */
    @Deprecated
    public static final M BRIDGEROUTE_NO_ATTACHMENTS = m(4019, Level.WARNING, "Route via SecureSpan Bridge assertion does not currently support SOAP with attachments; ignoring additional MIME parts");
    /**
     * @deprecated
     */
    @Deprecated
    public static final M BRIDGEROUTE_BAD_CONFIG = m(4020, Level.WARNING, "Route via SecureSpan Bridge assertion is configured with invalid protected service URL or policy XML");
    public static final M HTTPROUTE_BAD_ORIGINAL_URL = m(4021, Level.WARNING, "Invalid original request URI -- using default");
    /**
     * @deprecated
     */
    @Deprecated
    public static final M HTTPROUTE_ACCESS_DENIED = m(4022, Level.WARNING, "Protected service has denied access based on credentials from the Route via SecureSpan Bridge assertion");
    public static final M HTTPROUTE_TOO_MANY_ATTEMPTS = m(4023, Level.WARNING, "Unable to route to the service after multiple failed attempts");
    public static final M HTTPROUTE_SAML_SV_NOT_AUTH = m(4024, Level.WARNING, "SAML Sender-Vouches forwarding requested, but request was not authenticated");
    public static final M HTTPROUTE_RESPONSE_STATUS_HANDLED = m(4025, Level.INFO, "Protected service ({0}) responded with status {1}; retrying");
    public static final M HTTPROUTE_BAD_STRATEGY_NAME = m(4026, Level.WARNING, "Invalid routing failover strategy name: {0}; using default strategy");
    public static final M HTTPROUTE_FAILOVER_FROM_TO = m(4027, Level.WARNING, "Routing failed to host = {0}, retrying to host = {1}");
    public static final M HTTPROUTE_UNKNOWN_HOST = m(4028, Level.WARNING, "Routing failed, unable to resolve IP for host = {0}");
    public static final M HTTPROUTE_SOCKET_EXCEPTION = m(4029, Level.WARNING, "Routing failed, connection error: {0}");
    public static final M HTTPROUTE_PASSTHROUGH_REQUEST = m(4030, Level.INFO, "Passthrough selected; adding request credentials to routed request");
    public static final M HTTPROUTE_PASSTHROUGH_REQUEST_NC = m(4031, Level.FINE, "Passthrough selected but no credentials in Gateway request to pass along");
    public static final M HTTPROUTE_PASSTHROUGH_RESPONSE = m(4032, Level.INFO, "Passthrough selected; adding challenge to Gateway response");
    public static final M HTTPROUTE_PASSTHROUGH_RESPONSE_NC = m(4033, Level.FINE, "Passthrough selected but no challenge in routed response");
    public static final M HTTPROUTE_RESPONSE_NOCONTENTTYPE = m(4034, Level.WARNING, "Downstream service returned status ({0}) but is missing a content type header.");
    public static final M HTTPROUTE_RESPONSE_NOXML = m(4035, Level.WARNING, "Downstream service returned status ({0}) with non-XML payload.");
    public static final M HTTPROUTE_INVALIDCOOKIE = m(4036, Level.INFO, "Ignoring invalid cookie header ''{0}''");
    public static final M HTTPROUTE_RESPONSE_CHALLENGE = m(4037, Level.INFO, "Protected service requires authentication.");
    public static final M HTTPROUTE_RESPONSE_BADSTATUS = m(4038, Level.INFO, "Downstream service returned status ({0}). This is considered a failure case.");
    /**
     * @deprecated
     */
    @Deprecated
    public static final M _UNUSED_HTTPROUTE_CTYPEWOUTPAYLOAD = m(4039, Level.INFO, "Downstream service returned an empty response but still included a content-type of ({0}).");
    /**
     * @deprecated
     */
    @Deprecated
    public static final M BRIDGEROUTE_REQUEST_NOT_SOAP = m(4040, Level.WARNING, "Route via SecureSpan Bridge Assertion failed because request is not SOAP; this assertion currently does not support non-SOAP requests.");
    public static final M HTTPROUTE_SOCKET_TIMEOUT = m(4041, Level.WARNING, "Remote network connection timed out.");
    public static final M HTTPROUTE_GENERIC_PROBLEM = m(4042, Level.WARNING, "Problem routing to {0}. Error msg: {1}");
    public static final M HTTPROUTE_USING_KERBEROS_ERROR = m(4043, Level.WARNING, "Routing with Kerberos ticket failed with: {0}");
    public static final M HTTPROUTE_BAD_GZIP_STREAM = m(4044, Level.WARNING, "Bad GZip input stream.  A compressed request resulted in an uncompressed response.");
    public static final M HTTPROUTE_UNEXPECTED_METHOD = m(4045, Level.WARNING, "Unexpected request HTTP method {0}; using POST");
    public static final M HTTPROUTE_DEFAULT_METHOD_NON_HTTP = m(4046, Level.INFO, "Request was not HTTP; using POST");
    public static final M HTTPROUTE_DEFAULT_METHOD_VAR = m(4047, Level.INFO, "Request is a context variable; using POST");
    /**
     * @deprecated
     */
    @Deprecated
    public static final M BRIDGEROUTE_WSS_PROCESSING_RESP = m(4048, Level.WARNING, "Error in WSS processing of response ''{0}''");
    public static final M HTTPROUTE_RESPONSE_DEFCONTENTTYPE = m(4049, Level.INFO, "Downstream service response did not include a content type header, using default.");
    public static final M HTTPROUTE_CONFIGURATION_ERROR = m(4050, Level.WARNING, "Invalid HTTP configuration ''{0}''.");

    // ServerCredentialSourceAssertion messages
    public static final M HTTPCREDS_AUTH_REQUIRED = m(4100, Level.INFO, "Authentication required");
    public static final M HTTPCREDS_NO_AUTHN_HEADER = m(4101, Level.INFO, "No Authorization header");
    public static final M HTTPCREDS_BAD_AUTHN_HEADER = m(4102, Level.WARNING, "Bad Authorization header: {0}");
    public static final M HTTPCREDS_NA_AUTHN_HEADER = m(4103, Level.FINE, "Authorization header not applicable for this assertion");
    public static final M HTTPCREDS_FOUND_USER = m(4104, Level.INFO, "Found user: {0}");
    public static final M HTTPCREDS_CHALLENGING = m(4105, Level.FINE, "Sending WWW-Authenticate: {0}");
    public static final M HTTPDIGEST_NONCE_VALID = m(4106, Level.FINE, "Nonce {0} for user {1} still valid");
    public static final M HTTPDIGEST_NONCE_EXPIRED = m(4107, Level.INFO, "Nonce {0} for user {1} expired");
    public static final M HTTPDIGEST_NONCE_GENERATED = m(4108, Level.FINE, "Generated new nonce {0}");
    public static final M HTTPNEGOTIATE_USING_CONN_CREDS = m(4109, Level.FINE, "Using connection credentials");
    public static final M HTTPCOOKIE_FOUND = m(4110, Level.FINE, "Found cookie with the name: {0}");
    public static final M HTTPCOOKIE_NOT_FOUND = m(4111, Level.FINE, "No cookie found with the name: {0}");
    public static final M HTTPCLIENTCERT_NOT_HTTP = m(4112, Level.INFO, "Request not received over HTTP; cannot check for client certificate");
    public static final M HTTPCLIENTCERT_NO_CERT = m(4113, Level.INFO, "No Client Certificate was present in the request.");
    public static final M HTTPCLIENTCERT_FOUND = m(4114, Level.INFO, "Found client certificate for {0}");
    public static final M HTTPCOOKIE_FOUND_EMPTY = m(4115, Level.FINE, "Ignoring empty cookie with the name: {0}");
    public static final M HTTPNEGOTIATE_NTLM_AUTH = m(4116, Level.FINE, "NTLM Authorization found. Negotiate challenge will not be sent.");
    //ServerNtlmAuthentication
    public static final M NTLM_AUTHENTICATION_FAILED = m(4120, Level.WARNING, "Authentication failed: {0}");
    public static final M NTLM_AUTHENTICATION_MISSING_ACCOUNT_INFO = m(4121, Level.WARNING, "Account Info is missing");
    public static final M NTLM_AUTHENTICATION_CONNECTION_EXPIRED = m(4122, Level.FINE, "Connection {0} expired. Authentication is required") ;
    public static final M NTLM_AUTHENTICATION_IDLE_TIMEOUT_EXPIRED = m(4123, Level.FINE, "Idle timeout for connection {0} expired. Authentication is required") ;
    public static final M NTLM_AUTHENTICATION_MISSING_AUTHORIZATION_ATTRIBUTE = m(4124, Level.WARNING, "Attribute {0} from Account Info is missing");
    public static final M NTLM_AUTHENTICATION_USER_AUTHENTICATED = m(4125, Level.INFO, "Authenticated NTLM user: {0}");
    public static final M NTLM_AUTHENTICATION_IDENTITY_PROVIDER_CONFIG_FAILURE = m(4126, Level.WARNING, "Misconfigured Identity Provider: {0}");

    // ServerResolveServiceAssertion, service resolution
    public static final Messages.M RESOLVE_SERVICE_ALREADY_RESOLVED = m(4150, Level.INFO, "Service resolution has already been performed for this request");
    public static final Messages.M RESOLVE_SERVICE_ALREADY_HARDWIRED = m(4151, Level.INFO, "Request has already been assigned to a service");
    public static final Messages.M RESOLVE_SERVICE_NOT_FOUND = m(4152, Level.INFO, "No service matched the specified parameters");
    public static final Messages.M RESOLVE_SERVICE_FOUND_MULTI = m(4153, Level.INFO, "More than one service matched the specified parameters");
    public static final Messages.M RESOLVE_SERVICE_FAILED = m(4154, Level.WARNING, "Service resolution failed: {0}");
    public static final Messages.M RESOLVE_SERVICE_SUCCEEDED = m(4155, Level.INFO, "Resolved to service ID: {0}");
    public static final Messages.M RESOLVE_SERVICE_NO_PREFIX= m(4156, Level.WARNING, "No prefix specified.");

    // ServerIdentityAssertion
    /**
     * @deprecated
     */
    @Deprecated
    public static final M _UNUSED_IDENTITY_AUTHENTICATED_NO_CREDS = m(4200, Level.WARNING, "Request is authenticated but request has no login credentials!");
    public static final M IDENTITY_NO_CREDS = m(4201, Level.WARNING, "No credentials found!");
    /**
     * @deprecated
     */
    @Deprecated
    public static final M _UNUSED_IDENTITY_ALREADY_AUTHENTICATED = m(4202, Level.FINEST, "Request already authenticated");
    public static final M IDENTITY_PROVIDER_NOT_SET = m(4203, Level.WARNING, "Cannot call checkRequest() when no valid identity provider ID has been set!");
    public static final M IDENTITY_PROVIDER_NOT_FOUND = m(4204, Level.WARNING, "Could not find identity provider! ");
    public static final M IDENTITY_PROVIDER_NOT_EXIST = m(4205, Level.WARNING, "Identity assertion refers to a non-existent identity provider");
    public static final M IDENTITY_AUTHENTICATED = m(4206, Level.FINE, "Authentication success {0}");
    public static final M IDENTITY_INVALID_CERT = m(4207, Level.INFO, "Invalid client certificate for {0}");
    public static final M IDENTITY_AUTHENTICATION_FAILED = m(4208, Level.INFO, "Authentication failed for {0}");
    public static final M SPECIFICUSER_NOLOGIN_NOOID = m(4209, Level.WARNING, "Assertion not configure properly: both login and UID are null");
    public static final M SPECIFICUSER_PROVIDER_MISMATCH = m(4210, Level.FINE, "Authentication failed because ID of provider did not match ({0} instead of {1})");
    public static final M SPECIFICUSER_USERID_MISMATCH = m(4211, Level.FINE, "Authentication failed because the user ID did not match");
    public static final M SPECIFICUSER_LOGIN_MISMATCH = m(4212, Level.FINE, "Authentication failed because the login did not match");
    public static final M MEMBEROFGROUP_GROUP_NOT_EXIST = m(4213, Level.WARNING, "Assertions refer to a nonexistent group; policy may be corrupted");
    public static final M MEMBEROFGROUP_USER_NOT_MEMBER = m(4214, Level.FINE, "User not member of group");
    public static final M MEMBEROFGROUP_USING_CACHED_FAIL = m(4215, Level.FINE, "Reusing cached group membership failure");
    public static final M IDENTITY_AUTHENTICATED_NO_CREDS = m(4216, Level.WARNING, "{0} message is authenticated but has no login credentials!");
    public static final M IDENTITY_CREDENTIAL_FAILED = m(4217, Level.FINE, "Credentials failed for {0} due to ''{1}''");
    public static final M MEMBEROFGROUP_GROUP_DISALBED = m(4218, Level.WARNING, "Authentication failed because the group {0} is disabled");

    // ServerRequestWssOperation messages
    public static final M REQUESTWSS_NOT_FOR_US = m(4300, Level.FINE, "Intended for another recipient; nothing to validate");
    /**
     * @deprecated
     */
    @Deprecated
    public static final M _UNUSED_REQUIREWSS_NONSOAP = m(4301, Level.INFO, "Request not SOAP; cannot verify WS-Security contents");
    // Move the below message to the top "Generic Assertion Messages"
    //public static final M REQUESTWSS_NO_SECURITY = m(4302, Level.INFO, "Request did not contain any WSS level security");
    public static final M REQUIREWSS_NONSOAP = m(4303, Level.INFO, "{0} message not SOAP; cannot verify WS-Security contents");
    // 4304
    // 4305 Used for "Generic Assertion Messages" above

    // 4330- 4399 reserved for new "Generic Assertion Messages" above

    // ServerRequestSwAAssertion messages
    public static final M SWA_NOT_SOAP = m(4400, Level.WARNING, "Request not SOAP; cannot validate attachments");
    public static final M SWA_NOT_MULTIPART = m(4401, Level.INFO, "The request does not contain attachment or is not a multipart message");
    public static final M SWA_OPERATION_NOT_FOUND = m(4402, Level.FINEST, "Operation not found in the request: XPath expression is: {0}");
    public static final M SWA_REPEATED_OPERATION = m(4403, Level.INFO, "Same operation appears more than once in the request: XPath expression is: {0}");
    public static final M SWA_OPERATION_NOT_ELEMENT_NODE = m(4404, Level.INFO, "XPath pattern {0} found non-element node ''{1}''");
    public static final M SWA_PARAMETER_NOT_ELEMENT_NODE = m(4405, Level.INFO, "XPath pattern {0}/{1} found non-element node ''{2}''");
    public static final M SWA_OPERATION_FOUND = m(4406, Level.FINEST, "The operation {0} is found in the request");
    public static final M SWA_PART_NOT_FOUND = m(4407, Level.FINE, "MIME part not found in the request: XPath expression is: {0}/{1})");
    public static final M SWA_REPEATED_MIME_PART = m(4408, Level.FINE, "Same MIME part appears more than once in the request: XPath expression is: {0}/{1}");
    public static final M SWA_PARAMETER_FOUND = m(4409, Level.FINEST, "Parameter {0} is found in the request");
    public static final M SWA_REFERENCE_NOT_FOUND = m(4410, Level.INFO, "The reference (href) of the {0} is found in the request");
    public static final M SWA_REFERENCE_FOUND = m(4411, Level.FINEST, "The href of the parameter {0} is found in the request, value={1}");
    public static final M SWA_INVALID_CONTENT_ID_URL = m(4412, Level.INFO, "Invalid Content-ID URL {0}");
    public static final M SWA_NOT_IN_CONTENT_TYPES = m(4413, Level.INFO, "The content type of the attachment {0} must be one of the types: {1}");
    public static final M SWA_BAD_CONTENT_TYPE = m(4414, Level.INFO, "The content type of the attachment {0} must be: {1}");
    public static final M SWA_TOTAL_LENGTH_LIMIT_EXCEEDED = m(4415, Level.INFO, "The parameter [{0}] has {1} attachments: The total length exceeds the limit: {2} K bytes");
    public static final M SWA_PART_LENGTH_LIMIT_EXCEEDED = m(4416, Level.INFO, "The length of the attachment {0} exceeds the limit: {1} K bytes");
    public static final M SWA_NO_ATTACHMENT = m(4417, Level.INFO, "The required attachment {0} is not found in the request");
    public static final M SWA_UNEXPECTED_ATTACHMENT = m(4418, Level.INFO, "Unexpected attachment {0} found in the request");
    public static final M SWA_INVALID_OPERATION = m(4419, Level.INFO, "The operation specified in the request is invalid");
    public static final M SWA_INVALID_XML = m(4420, Level.WARNING, "Error parsing request, detail is ''{0}''.");
    public static final M SWA_EXTRA_ATTACHMENT = m(4421, Level.INFO, "Passing extra attachment {0}.");
    public static final M SWA_EXTRA_LENGTH_EXCEEDED = m(4422, Level.INFO, "Maximum length of extra attachments exceeds the limit {0} K bytes.");
    public static final M SWA_EXTRA_ATTACHMENT_DROPPED = m(4423, Level.INFO, "Dropping extra attachment {0}.");
    public static final M SWA_NOT_SIGNED = m(4424, Level.WARNING, "Missing required signature for part ''{0}'', for attachment with Content-ID URL ''{1}''");

    // ServerRemoteIpRange messages
    public static final M IP_NOT_TCP = m(4500, Level.INFO, "Request was not received via TCP; cannot validate remote IP address");
    public static final M IP_ADDRESS_INVALID = m(4501, Level.INFO, "The remote address {0} is null or not in expected format");
    public static final M IP_ACCEPTED = m(4502, Level.FINEST, "Requestor address {0} is accepted");
    public static final M IP_REJECTED = m(4503, Level.INFO, "Requestor address {0} is not allowed");
    public static final M IP_ADDRESS_UNAVAILABLE = m(4504, Level.WARNING, "Could not resolve a remote IP address from the context variable {0}.");
    public static final M IP_INVALID_RANGE = m(4505, Level.WARNING, "Invalid IP range configured: {0}.");

    // ServerSecureConversation messages
    public static final M SC_REQUEST_NOT_SOAP = m(4600, Level.INFO, "Request not SOAP; unable to check for WS-SecureConversation token");
    /**
     * @deprecated
     */
    @Deprecated
    public static final M _UNUSED_SC_NO_WSS_LEVEL_SECURITY = m(4601, Level.INFO, "This request did not contain any WSS level security");
    public static final M SC_NO_PROOF_OF_POSSESSION = m(4602, Level.FINE, "Ignoring SecurityContextToken with no proof-of-possession");
    public static final M SC_TOKEN_INVALID = m(4603, Level.WARNING, "Request referred to a SecureConversation token unrecognized on this server; possible expired session - returning AUTH_FAILED");
    public static final M SC_SESSION_FOR_USER = m(4604, Level.FINE, "Secure Conversation session recognized for user {0}");
    public static final M SC_REQUEST_NOT_REFER_TO_SC_TOKEN = m(4605, Level.INFO, "This request did not seem to refer to a Secure Conversation token");
    public static final M SC_UNABLE_TO_ATTACH_SC_TOKEN = m(4606, Level.WARNING, false, true, "Response not SOAP; unable to attach WS-SecureConversation token");

    // ServerRequestXpathAssertion & ServerResponseXpathAssertion messages
    public static final M XPATH_REQUEST_NOT_XML = m(4700, Level.WARNING, true, false, "Request not XML; cannot evaluate XPath expression");
    public static final M XPATH_RESPONSE_NOT_XML = m(4701, Level.WARNING, false, true, "Response not XML; cannot evaluate XPath expression");
    public static final M XPATH_PATTERN_INVALID = m(4702, Level.WARNING, "Assertion has failed because the XPath pattern is null or empty");
    public static final M XPATH_PATTERN_NOT_MATCHED_REQUEST = m(4703, Level.INFO, "Assertion has failed because the XPath pattern did not match request");
    public static final M XPATH_PATTERN_NOT_MATCHED_RESPONSE = m(4704, Level.INFO, "Assertion has failed because the XPath pattern did not match response or target message");
    public static final M XPATH_RESULT_TRUE = m(4705, Level.FINE, "XPath pattern returned true");
    public static final M XPATH_RESULT_FALSE = m(4706, Level.INFO, "XPath pattern returned false");
    public static final M XPATH_TEXT_NODE_FOUND = m(4707, Level.FINE, "XPath pattern found a text node");
    public static final M XPATH_ELEMENT_FOUND = m(4708, Level.FINE, "XPath pattern found an element");
    public static final M XPATH_OTHER_NODE_FOUND = m(4709, Level.FINE, "XPath pattern found some other node");
    public static final M XPATH_SUCCEED_REQUEST = m(4710, Level.FINE, "XPath pattern matched request; assertion succeeds");
    public static final M XPATH_SUCCEED_RESPONSE = m(4711, Level.FINE, "XPath pattern matched response; assertion succeeds");
    public static final M XPATH_MULTIPLE_RESULTS = m(4712, Level.FINE, "XPath pattern found {0} results; .result variable will contain first value");
    public static final M XPATH_RESULTS = m(4713, Level.FINE, "XPath result #{0}: \"{1}\"");
    public static final M XPATH_PATTERN_INVALID_MORE_INFO = m(4714, Level.WARNING, "Cannot evaluate XPath expression: XPath pattern is invalid ''{0}''.");
    public static final M XPATH_PATTERN_NOT_MATCHED_REQUEST_MI = m(4715, Level.INFO, "XPath pattern didn''t match request; assertion therefore fails; XPath is ''{0}''.");
    public static final M XPATH_PATTERN_NOT_MATCHED_RESPONSE_MI = m(4716, Level.INFO, "XPath pattern didn''t match response or target message; assertion therefore fails; XPath is ''{0}''.");
    public static final M XPATH_NOT_ACCELERATED = m(4717, Level.FINE, "Multiple result elements expected, using non-accelerated XPath.");
    public static final M XPATH_PATTERN_IS = m(4718, Level.FINE, "XPath is ''{0}''");
    public static final M XPATH_MESSAGE_NOT_XML = m(4719, Level.WARNING, "{0} not XML; cannot evaluate XPath expression");
    public static final M XPATH_UNRESOLVABLE_PREFIX = m(4720, Level.WARNING, "Cannot resolve namespace prefix {0}");
    public static final M XPATH_DYNAMIC_PATTERN_INVALID = m(4721, Level.WARNING, "Assertion has failed because the fully-dynamic XPath pattern variable is missing or invalid");

    // ServerRequestAcceleratedXpathAssertion & ServerResponseAcceleratedXpathAssertion messages
    public static final M ACCEL_XPATH_NO_HARDWARE = m(4750, Level.INFO, "Hardware acceleration not available; falling back to software XPath processing");
    public static final M ACCEL_XPATH_UNSUPPORTED_PATTERN = m(4751, Level.INFO, "Hardware acceleration not available for this XPath expression; falling back to software XPath processing");
    public static final M ACCEL_XPATH_NO_CONTEXT = m(4752, Level.FINE, "Message has no hardware acceleration context; falling back to software XPath processing");

    // ServerRequestWssX509Cert messages
    public static final M WSS_X509_FOR_ANOTHER_USER = m(4800, Level.FINE, "This is intended for another recipient; there is nothing to validate");
    /**
     * @deprecated
     */
    @Deprecated
    public static final M _UNUSED_REQUEST_WSS_X509_NON_SOAP = m(4801, Level.INFO, "Request not SOAP; unable to check for WS-Security signature");
    /**
     * @deprecated
     */
    @Deprecated
    public static final M _UNUSED_REQUEST_WSS_X509_NO_WSS_LEVEL_SECURITY = m(4802, Level.INFO, "Request did not contain any WSS level security");
    /**
     * @deprecated
     */
    @Deprecated
    public static final M _UNUSED_REQUEST_WSS_X509_NO_TOKEN = m(4803, Level.INFO, "No tokens were processed from this request; returning AUTH_REQUIRED");
    /**
     * @deprecated
     */
    @Deprecated
    public static final M _UNUSED_REQUEST_WSS_X509_TOO_MANY_VALID_SIG = m(4804, Level.WARNING, true, false, "Request presented more than one valid signature from more than one client certificate");
    public static final M WSS_X509_CERT_LOADED = m(4805, Level.FINE, "Certificate loaded as principal credential for CN:{0}");
    /**
     * @deprecated
     */
    @Deprecated
    public static final M _UNUSED_REQUEST_WSS_X509_NO_PROVEN_CERT = m(4806, Level.INFO, "This assertion did not find a proven X.509 certificate to use as credentials - returning AUTH_REQUIRED");
    public static final M WSS_X509_NON_SOAP = m(4807, Level.INFO, "{0} not SOAP; unable to check for WS-Security signature");
    public static final M WSS_X509_NO_WSS_LEVEL_SECURITY = m(4808, Level.INFO, "{0} did not contain any WSS level security");
    public static final M WSS_X509_NO_TOKEN = m(4809, Level.INFO, "No tokens were processed from {0}; returning {1}");
    public static final M WSS_X509_TOO_MANY_VALID_SIG = m(4810, Level.WARNING, true, false, "{0} presented more than one valid signature.");
    public static final M WSS_X509_TOO_MANY_VALID_SIG_IDENTITY = m(4811, Level.WARNING, true, false, "{0} presented more than one valid signature for {1}.");
    public static final M WSS_X509_NO_PROVEN_CERT = m(4812, Level.INFO, "No proven {0} X.509 certificate to use as credentials - returning {1}");

    // Saml2AttributeQuery modular assertion messages
    /**
     * @deprecated Message only used by tactical SAML Attribute Query implementation which is deprecated.
     */
    public static final M SAML2_AQ_REQUEST_DIGSIG_NO_SIG = m(4850, Level.WARNING, "No signature found for element.");
    /**
     * @deprecated Message only used by tactical SAML Attribute Query implementation which is deprecated.
     */
    public static final M SAML2_AQ_REQUEST_DIGSIG_VAR_UNUSABLE = m(4851, Level.WARNING, "The input variable was not set properly.");
    /**
     * @deprecated Message only used by tactical SAML Attribute Query implementation which is deprecated.
     */
    public static final M SAML2_AQ_REQUEST_SAML_ATTR_FORBIDDEN = m(4852, Level.WARNING, "Requester tried to access a forbidden attribute");
    /**
     * @deprecated Message only used by tactical SAML Attribute Query implementation which is deprecated.
     */
    public static final M SAML2_AQ_REQUEST_SAML_ATTR_UNKNOWN = m(4853, Level.WARNING, "Requester tried to access an unknown attribute");
    /**
     * @deprecated Message only used by tactical SAML Attribute Query implementation which is deprecated.
     */
    public static final M SAML2_AQ_RESPONSE_ENCRYPT_SAML_ASSERTION_VAR_UNUSABLE = m(4854, Level.WARNING, "The variable saml2.encrypt.cert.subjectDN was not set properly.");
    /**
     * @deprecated Message only used by tactical SAML Attribute Query implementation which is deprecated.
     */
    public static final M SAML2_AQ_RESPONSE_ENCRYPT_SAML_ASSERTION_CERT_NOT_FOUND = m(4855, Level.WARNING, "The certificate \"{0}\" was not found.");
    /**
     * @deprecated Message only used by tactical SAML Attribute Query implementation which is deprecated.
     */
    public static final M SAML2_AQ_RESPONSE_ENCRYPT_SAML_ASSERTION_PK_NOT_FOUND = m(4856, Level.WARNING, "The private key \"{0}\" was not found.");

    // / Sophos  modular assertion messages
    public static final M SOPHOS_RESPONSE_FINEST = m(4870, Level.FINEST, "Sophos AV detected a virus name ( {0} ), type ( {1} ), location ( {2} ), disinfectable ( {3} ). ");
    public static final M SOPHOS_RESPONSE_FINER = m(4871, Level.FINER, "Sophos AV detected a virus name ( {0} ), type ( {1} ), location ( {2} ), disinfectable ( {3} ). ");
    public static final M SOPHOS_RESPONSE_FINE = m(4872, Level.FINE, "Sophos AV detected a virus name ( {0} ), type ( {1} ), location ( {2} ), disinfectable ( {3} ). ");
    public static final M SOPHOS_RESPONSE_INFO = m(4873, Level.INFO, "Sophos AV detected a virus name ( {0} ), type ( {1} ), location ( {2} ), disinfectable ( {3} ). ");
    public static final M SOPHOS_RESPONSE_WARNING = m(4874, Level.WARNING, "Sophos AV detected a virus name ( {0} ), type ( {1} ), location ( {2} ), disinfectable ( {3} ). ");

    // ServerRequestWssReplayProtection messages
    public static final M REQUEST_WSS_REPLAY_NON_SOAP = m(4900, Level.INFO, "{0} not SOAP; cannot check for replayed signed WS-Security message");
    public static final M REQUEST_WSS_REPLAY_NO_WSS_LEVEL_SECURITY = m(4901, Level.INFO, "{0} did not contain any WSS level security");
    public static final M REQUEST_WSS_REPLAY_NO_TIMESTAMP = m(4902, Level.INFO, "No timestamp present in {0}");
    public static final M REQUEST_WSS_REPLAY_TIMESTAMP_NOT_SIGNED = m(4903, Level.INFO, "No signed timestamp present in {0}");
    public static final M REQUEST_WSS_REPLAY_TIMESTAMP_NO_CREATED_ELEMENT = m(4904, Level.INFO, "Timestamp in {0} has no Created element");
    public static final M REQUEST_WSS_REPLAY_TIMESTAMP_NO_EXPIRES_ELEMENT = m(4905, Level.INFO, "Timestamp in {0} has no Expires element; assuming expiry {1}ms after creation");
    public static final M REQUEST_WSS_REPLAY_CLOCK_SKEW = m(4906, Level.FINE, "Clock skew: {0} message creation time is in the future: {1}; continuing anyway");
    public static final M REQUEST_WSS_REPLAY_TIMESTAMP_SIGNED_WITH_CERT = m(4907, Level.FINER, "Timestamp in {0} was signed with an X.509 certificate");
    public static final M REQUEST_WSS_REPLAY_TIMESTAMP_SIGNED_WITH_SAML_HOK = m(4908, Level.FINER, "Timestamp in {0} was signed with a SAML holder-of-key assertion");
    public static final M REQUEST_WSS_REPLAY_TIMESTAMP_SIGNED_WITH_SC_KEY = m(4909, Level.FINER, "Timestamp in {0} was signed with a WS-SecureConversation derived key");
    public static final M REQUEST_WSS_REPLAY_PROTECTION_SUCCEEDED = m(4910, Level.FINEST, "Message ID {0} in {1} has not been seen before");
    public static final M REQUEST_WSS_REPLAY_TIMESTAMP_SIGNED_WITH_ENC_KEY = m(4911, Level.FINER, "Timestamp in {0} was signed with an EncryptedKey");
    public static final M REQUEST_WSS_REPLAY_REPLAY = m(4912, Level.WARNING, "Message ID {0} in {1} is a replay");
    public static final M REQUEST_WSS_REPLAY_STALE_TIMESTAMP = m(4913, Level.WARNING, "{0} timestamp contained stale Expires date");
    public static final M REQUEST_WSS_REPLAY_CREATED_TOO_OLD = m(4914, Level.WARNING, "{0} timestamp contained Created older than the maximum message age hard cap");
    public static final M REQUEST_WSS_REPLAY_NO_SKI = m(4915, Level.WARNING, "Unable to generate replay-protection ID for {0}; a SKI cannot be derived from signing cert ''{1}''");
    public static final M REQUEST_WSS_REPLAY_UNSUPPORTED_TOKEN_TYPE = m(4916, Level.WARNING, "Unable to generate replay-protection ID for {0} timestamp -- it was signed, but with the unsupported token type {1}");
    public static final M REQUEST_WSS_REPLAY_MULTIPLE_SENDER_IDS = m(4917, Level.WARNING, "Found multiple eligible sender identity tokens in {0}; unable to proceed");
    public static final M REQUEST_WSS_REPLAY_MULTIPLE_MESSAGE_IDS = m(4918, Level.WARNING, "Found multiple signed wsa:MessageID values in {0}; unable to proceed");
    public static final M REQUEST_WSS_REPLAY_GOT_SIGNED_MESSAGE_ID = m(4919, Level.FINE, "Found signed wsa:MessageID in {0}: ''{1}''");
    public static final M REQUEST_WSS_REPLAY_NO_SIGNED_MESSAGE_ID = m(4920, Level.FINE, "No signed wsa:MessageID was present in {0}; using Timestamp instead");
    public static final M REQUEST_WSS_REPLAY_MESSAGE_ID_TOO_LARGE = m(4921, Level.WARNING, "wsa:MessageID too large ''{0}''; unable to proceed");
    public static final M REQUEST_WSS_REPLAY_TIMESTAMP_SIGNED_WITH_KERBEROS = m(4922, Level.FINER, "Timestamp in {0} was signed with a Kerberos token");
    public static final M REQUEST_WSS_REPLAY_USING_SCOPE_AND_ID = m(4923, Level.FINE, "{0} replay protection using scope ''{1}'' and identifier ''{2}''");
    public static final M REQUEST_WSS_REPLAY_CUSTOM_VAR_ERROR = m(4924, Level.WARNING, "Error processing variables for {0} ''{1}''; unable to proceed");
    public static final M REQUEST_WSS_REPLAY_CUSTOM_VAR_EMPTY = m(4925, Level.WARNING, "{0} replay message identifier is empty; unable to proceed");

    // ServerCustomAssertionHolder
    public static final M CA_CREDENTIAL_INFO = m(5000, Level.FINE, "Service:{0}, custom assertion: {1}, principal:{2}");
    public static final M CA_INVALID_CA_DESCRIPTOR = m(5001, Level.WARNING, "Invalid custom assertion descriptor detected for {0}; policy element is misconfigured and will cause the policy to fail");

    // ServerHttpCredentialSource
    public static final M HTTP_CS_CANNOT_EXTRACT_CREDENTIALS = m(5100, Level.INFO, "Request not HTTP; unable to extract HTTP credentials");

    // ServerWssBasic
    public static final M WSS_BASIC_FOR_ANOTHER_RECIPIENT = m(5200, Level.FINE, "This is intended for another recipient: nothing to validate");
    /**
     * @deprecated
     */
    @Deprecated
    public static final M _UNUSED_WSS_BASIC_NOT_SOAP = m(5201, Level.INFO, "Request not SOAP; cannot check for WS-Security UsernameToken");
    /**
     * @deprecated
     */
    @Deprecated
    public static final M _UNUSED_WSS_BASIC_NO_CREDENTIALS = m(5202, Level.INFO, "Request did not include WSS Basic credentials");
    public static final M WSS_BASIC_CANNOT_FIND_CREDENTIALS = m(5203, Level.INFO, "Cannot find credentials");
    /**
     * @deprecated
     */
    @Deprecated
    public static final M _UNUSED_WSS_BASIC_CANNOT_FIND_ENC_CREDENTIALS = m(5204, Level.INFO, "Request did not include an encrypted UsernameToken");
    public static final M WSS_BASIC_UNABLE_TO_ATTACH_TOKEN = m(5205, Level.WARNING, false, true, "Response not SOAP; unable to use WS-Security EncryptedUsernameToken");
    public static final M WSS_BASIC_NOT_SOAP = m(5206, Level.INFO, "{0} message is not SOAP; cannot check for WS-Security UsernameToken");
    public static final M WSS_BASIC_NO_CREDENTIALS = m(5207, Level.INFO, "{0} message did not include WSS Basic credentials");
    public static final M WSS_BASIC_CANNOT_FIND_ENC_CREDENTIALS = m(5208, Level.INFO, "{0} message did not include an encrypted UsernameToken");

    // ServerSslAssertion
    public static final M SSL_REQUIRED_PRESENT = m(5300, Level.FINE, "SSL required and present");
    public static final M SSL_REQUIRED_ABSENT = m(5301, Level.INFO, "SSL required but not present");
    public static final M SSL_FORBIDDEN_PRESENT = m(5302, Level.INFO, "SSL forbidden but present");
    public static final M SSL_FORBIDDEN_ABSENT = m(5303, Level.FINE, "SSL forbidden and not present");
    public static final M SSL_OPTIONAL_PRESENT = m(5304, Level.FINE, "SSL optional and present");
    public static final M SSL_OPTIONAL_ABSENT = m(5305, Level.FINE, "SSL optional and not present");

    // ServerResponseWssConfidentiality
    /**
     * @deprecated
     */
    @Deprecated
    public static final M _UNUSED_RESPONSE_WSS_CONF_REQUEST_NOT_SOAP = m(5400, Level.INFO, "Request not SOAP; unable to check for WS-Security encrypted elements");
    /**
     * @deprecated
     */
    @Deprecated
    public static final M _UNUSED_RESPONSE_WSS_CONF_NO_WSS_SECURITY = m(5401, Level.INFO, "Request did not contain any WSS level security");
    public static final M WSS_ENCRYPT_MORE_THAN_ONE_TOKEN = m(5402, Level.WARNING, true, false, "Request included more than one X509 security token whose key ownership was proven");
    /**
     * @deprecated
     */
    @Deprecated
    public static final M _UNUSED_WSS_ENCRYPT_NO_CERT_OR_SC_TOKEN = m(5403, Level.WARNING, "Unable to encrypt response; request did not include X509 token or SecureConversation");
    /**
     * @deprecated
     */
    @Deprecated
    public static final M _UNUSED_RESPONSE_WSS_CONF_RESPONSE_NOT_SOAP = m(5404, Level.WARNING, false, true, "Response not SOAP; unable to encrypt response elements");
    /**
     * @deprecated
     */
    @Deprecated
    public static final M _UNUSED_RESPONSE_WSS_CONF_RESPONSE_NOT_ENCRYPTED = m(5405, Level.INFO, "No matching elements to encrypt in response: Assertion therefore fails");
    /**
     * @deprecated
     */
    @Deprecated
    public static final M _UNUSED_RESPONSE_WSS_CONF_RESPONSE_ENCRYPTED = m(5406, Level.FINEST, "Designated {0} response elements for encryption");
    public static final M WSS_ENCRYPT_MESSAGE_NOT_SOAP = m(5407, Level.WARNING, false, true, "{0} message not SOAP; unable to encrypt message elements");
    public static final M WSS_ENCRYPT_MESSAGE_NOT_ENCRYPTED = m(5408, Level.INFO, "No matching elements to encrypt in {0} message: Assertion therefore fails");
    public static final M WSS_ENCRYPT_MESSAGE_ENCRYPTED = m(5409, Level.FINEST, "Designated {1} {0} message elements for encryption");
    public static final M WSS_ENCRYPT_NO_CERT_OR_SC_TOKEN = m(5410, Level.INFO, "Request did not include a token suitable for response encryption.");

    // ServerResponseWssIntegrity
    /**
     * @deprecated
     */
    @Deprecated
    public static final M ADD_WSS_SIGNATURE_REQUEST_NOT_SOAP = m(5500, Level.INFO, "Request not SOAP; cannot sign response");
    /**
     * @deprecated
     */
    @Deprecated
    public static final M _UNUSED_RESPONSE_WSS_INT_RESPONSE_NOT_SOAP = m(5501, Level.WARNING, false, true, "Response not SOAP; cannot apply WS-Security signature");
    /**
     * @deprecated
     */
    @Deprecated
    public static final M _UNUSED_RESPONSE_WSS_INT_RESPONSE_NO_MATCHING_EL = m(5502, Level.INFO, "No matching elements to sign in response: Assertion therefore fails");
    /**
     * @deprecated
     */
    @Deprecated
    public static final M _UNUSED_RESPONSE_WSS_INT_RESPONSE_SIGNED = m(5503, Level.FINE, "Designated {0} response elements for signing");
    public static final M ADD_WSS_SIGNATURE_MESSAGE_NOT_SOAP = m(5504, Level.WARNING, false, true, "{0} message not SOAP; cannot apply WS-Security signature");
    public static final M ADD_WSS_SIGNATURE_MESSAGE_NO_MATCHING_EL = m(5505, Level.INFO, "No matching elements to sign in {0} message: Assertion therefore fails");
    public static final M ADD_WSS_SIGNATURE_MESSAGE_SIGNED = m(5506, Level.FINE, "Designated {1} {0} message elements for signing");

    // ServerRequestWssIntegrity
    public static final M REQUIRE_WSS_SIGNATURE_RESPONSE_NOT_SOAP = m(5550, Level.FINE, "Response not SOAP; cannot return SignatureConfirmation");
    public static final M REQUIRE_WSS_SIGNATURE_REQUEST_MULTI_SIGNED = m(5551, Level.WARNING, true, false, "Request has multiple signers; failing");
    public static final M REQUIRE_WSS_SIGNATURE_CONFIRMATION_FAILED = m(5552, Level.WARNING, true, false, "Signature confirmation failed: {0}");

    // ServerSchemaValidation
    /**
     * @deprecated
     */
    @Deprecated
    public static final M _UNUSED_SCHEMA_VALIDATION_VALIDATE_REQUEST = m(5600, Level.FINEST, "Validating request document");
    /**
     * @deprecated
     */
    @Deprecated
    public static final M _UNUSED_SCHEMA_VALIDATION_RESPONSE_NOT_XML = m(5601, Level.INFO, true, true, "Response not well-formed XML; cannot validate schema");
    /**
     * @deprecated
     */
    @Deprecated
    public static final M _UNUSED_SCHEMA_VALIDATION_VALIDATE_RESPONSE = m(5602, Level.FINEST, "Validating response document");
    /**
     * @deprecated
     */
    @Deprecated
    public static final M _UNUSED_SCHEMA_VALIDATION_REQUEST_NOT_XML = m(5603, Level.INFO, true, false, "Request not well-formed XML; cannot validate schema");
    public static final M SCHEMA_VALIDATION_FAILED = m(5604, Level.INFO, true, true, "Schema validation failure: {0}");
    public static final M SCHEMA_VALIDATION_SUCCEEDED = m(5605, Level.FINEST, "Schema validation success");
    /**
     * @deprecated
     */
    @Deprecated
    public static final M _UNUSED_SCHEMA_VALIDATION_EMPTY_BODY = m(5606, Level.FINE, "Nothing to validate because the body is empty");
    public static final M SCHEMA_VALIDATION_NO_ACCEL = m(5607, Level.INFO, "Schema cannot be hardware accelerated");
    public static final M SCHEMA_VALIDATION_FALLBACK = m(5608, Level.INFO, "Hardware-accelerated schema validation failed; falling back to software");
    public static final M SCHEMA_VALIDATION_VALID_BUT_WRONG_NS = m(5609, Level.INFO, "Message was valid but payload was in an unexpected namespace");
    public static final M SCHEMA_VALIDATION_GLOBALREF_BROKEN = m(5610, Level.WARNING, "Cannot validate schema because the global schema named {0} cannot be retrieved");
    public static final M SCHEMA_VALIDATION_NOT_XML = m(5611, Level.INFO, "{0} is not well-formed XML; cannot validate");
    public static final M SCHEMA_VALIDATION_VALIDATING = m(5612, Level.FINEST, "Validating {0}");
    public static final M SCHEMA_VALIDATION_IO_ERROR = m(5613, Level.WARNING, "Cannot validate schema because schema information cannot be retrieved: {0}");

    // ServerTimeRange
    public static final M TIME_RANGE_NOTHING_TO_CHECK = m(5700, Level.FINEST, "Nothing to check");
    public static final M TIME_RANGE_DOW_OUTSIDE_RANGE = m(5701, Level.INFO, "Failed because day of week outside allowed range");
    public static final M TIME_RANGE_TOD_OUTSIDE_RANGE = m(5702, Level.INFO, "Failed because time of day outside allowed range");
    public static final M TIME_RAGNE_WITHIN_RANGE = m(5703, Level.FINEST, "Request is within time range");

    // ServerUnknownAssertion
    public static final M UNKNOWN_ASSERTION = m(5800, Level.WARNING, "Unknown assertion invoked; details: {0}");

    // ServerXslTransformation
    public static final M XSLT_MSG_NOT_XML = m(5900, Level.INFO, "Message not XML; cannot perform XSL transformation");
    public static final M XSLT_REQUEST = m(5901, Level.FINEST, "Transforming request");
    /**
     * @deprecated {@link #XSLT_MSG_NOT_XML} is always used now
     */
    @Deprecated
    public static final M __UNUSED_XSLT_RESP_NOT_XML = m(5902, Level.INFO, "Response not XML; cannot perform XSL transformation");
    public static final M XSLT_RESPONSE = m(5903, Level.FINEST, "Transforming response");
    public static final M XSLT_CONFIG_ISSUE = m(5904, Level.WARNING, "Assertion does not specify whether transformation applies to request or response; returning failure");
    public static final M XSLT_NO_SUCH_PART = m(5905, Level.WARNING, "Assertion refers to nonexistent MIME part {0}");
    public static final M XSLT_MULTIPLE_PIS = m(5906, Level.WARNING, "Document contained multiple <?xml-stylesheet?> processing instructions; not currently supported");
    public static final M XSLT_CANT_READ_XSL = m(5907, Level.WARNING, "Could not retrieve linked XSL stylesheet at {0}: {1}");
    public static final M XSLT_BAD_EXT_XSL = m(5908, Level.WARNING, "Unable to parse external XSL at {0}: {1}");
    public static final M XSLT_BAD_XSL = m(5909, Level.WARNING, "Unable to parse XSL: {0}");
    public static final M XSLT_NO_PI = m(5910, Level.WARNING, "No <?xml-stylesheet?> processing instruction was found in the message; assertion fails");
    public static final M XSLT_BAD_URL = m(5911, Level.WARNING, "Stylesheet URL {0} did not match any configured regular expression");
    // note                                      5912 used below
    public static final M XSLT_TRANS_WARN = m(5913, Level.INFO, "XSL-T Warning ''{0}''");
    public static final M XSLT_TRANS_ERR = m(5914, Level.INFO, "XSL-T Error ''{0}''");
    public static final M XSLT_NO_PI_OK = m(5915, Level.INFO, "No <?xml-stylesheet?> processing instruction was found in the message; assertion succeeds");
    public static final M XSLT_OTHER = m(5916, Level.FINEST, "Transforming message ''{0}''");

    // TODO move this message, now that it is shared among multiple assertion (XSLT + schema)
    public static final M RR_CANT_READ_REMOTE_RESOURCE = m(5912, Level.WARNING, "Could not retrieve remote resource at {0}: {1}; continuing using previous version");

    // ServerJmsRoutingAssertion
    public static final M JMS_ROUTING_CONNECT_FAILED = m(6000, Level.INFO, "Failed to establish JMS connection on try #{0}: Will retry after {1}ms");
    public static final M JMS_ROUTING_INBOUD_REQUEST_QUEUE_NOT_EMPTY = m(6001, Level.FINE, "Inbound request queue is not temporary; using selector to filter responses to our message");
    public static final M JMS_ROUTING_NO_TOPIC_WITH_REPLY = m(6002, Level.WARNING, "Topics not supported when reply type is not NO_REPLY");
    public static final M JMS_ROUTING_REQUEST_ROUTED = m(6003, Level.FINER, "Routing request to protected service");
    public static final M JMS_ROUTING_GETTING_RESPONSE = m(6004, Level.FINEST, "Getting response from protected service");
    public static final M JMS_ROUTING_NO_RESPONSE = m(6005, Level.WARNING, "Did not receive a routing reply within the timeout period of {0} ms; empty response being returned");
    public static final M JMS_ROUTING_GOT_RESPONSE = m(6006, Level.FINER, "Received routing reply");
    public static final M JMS_ROUTING_UNSUPPORTED_RESPONSE_MSG_TYPE = m(6007, Level.WARNING, "Received JMS reply with unsupported message type {0}");
    public static final M JMS_ROUTING_NO_RESPONSE_EXPECTED = m(6008, Level.INFO, "No response expected from protected service");
    public static final M JMS_ROUTING_DELETE_TEMPORARY_QUEUE = m(6009, Level.FINER, "Deleting temporary queue");
    @Deprecated
    public static final M __UNUSED_JMS_ROUTING_RETURN_NO_REPLY = m(6010, Level.FINER, "Returning NO_REPLY (null) for {0}");
    @Deprecated
    public static final M __UNUSED_JMS_ROUTING_RETURN_AUTOMATIC = m(6011, Level.FINER, "Returning AUTOMATIC {0} for {1}");
    @Deprecated
    public static final M __UNUSED_JMS_ROUTING_RETURN_REPLY_TO_OTHER = m(6012, Level.FINER, "Returning REPLY_TO_OTHER {0} for {1}");
    public static final M JMS_ROUTING_UNKNOW_JMS_REPLY_TYPE = m(6013, Level.WARNING, "Unknown JmsReplyType {0}");
    @Deprecated
    public static final M __UNUSED_JMS_ROUTING_ENDPOINTS_ON_SAME_CONNECTION = m(6014, Level.WARNING, "Request and reply endpoints must belong to the same connection");
    public static final M JMS_ROUTING_CREATE_REQUEST_AS_TEXT_MESSAGE = m(6015, Level.FINER, "Creating request as TextMessage");
    public static final M JMS_ROUTING_CREATE_REQUEST_AS_BYTES_MESSAGE = m(6016, Level.FINER, "Creating request as BytesMessage");
    public static final M JMS_ROUTING_REQUEST_WITH_NO_REPLY = m(6017, Level.FINE, "Outbound request endpoint {0} specifies NO_REPLY");
    public static final M JMS_ROUTING_REQUEST_WITH_REPLY_TO_OTHER = m(6018, Level.FINE, "Outbound request endpoint {0} specifies REPLY_TO_OTHER, setting JMSReplyTo to {1}");
    public static final M JMS_ROUTING_NON_EXISTENT_ENDPOINT = m(6019, Level.WARNING, "Route via JMS Assertion contains a reference to nonexistent JmsEndpoint #{0}");
    public static final M JMS_ROUTING_NO_SAML_SIGNER = m(6020, Level.WARNING, "Route via JMS Assertion cannot access SAML signing information");
    public static final M JMS_ROUTING_CANT_CONNECT_RETRYING = m(6021, Level.WARNING, "Failed to establish JMS connection on try #{0}.  Will retry after {1}ms.");
    public static final M JMS_ROUTING_CANT_CONNECT_NOMORETRIES = m(6022, Level.WARNING, "Tried {0} times to establish JMS connection and failed.");
    public static final M JMS_ROUTING_REQUEST_WITH_AUTOMATIC = m(6023, Level.FINE, "Outbound request endpoint {0} specifies AUTOMATIC, using temporary queue");
    public static final M JMS_ROUTING_REQUEST_TOO_LARGE = m(6024, Level.WARNING, "Request message too large.");
    public static final M JMS_ROUTING_TEMPLATE_ERROR = m(6025, Level.WARNING, "Error processing JMS outbound template ''{0}''.");
    @Deprecated
    public static final M __UNUSED_JMS_ROUTING_DESTINATION_SESSION_MISMATCH = m(6026, Level.WARNING, "JMS Destination/Session type mismatch.");
    public static final M JMS_ROUTING_MISSING_MESSAGE_ID = m(6027, Level.WARNING, "Sent message had no message ID. Unable to correlate.");
    public static final M JMS_ROUTING_CONFIGURATION_ERROR = m(6028, Level.WARNING, "Invalid JMS configuration ''{0}''.");
    public static final M JMS_ROUTING_RESPONSE_TOO_LARGE = m(6029, Level.WARNING, "Response message too large.");
    public static final M JMS_ROUTING_NON_SETTABLE_JMS_PROPERTY =  m(6030,Level.WARNING, "Cannot set JMS Property ''{0}'' to value ''{1}'' on IBM MQ JMS Provider. {2}");
    public static final M JMS_ROUTING_MESSAGE_FORMAT_ERROR = m(6031,Level.WARNING,"JMS message format error while constructing JMS message to route: {0}");
    public static final M JMS_ROUTING_INCOMPATIBLE_JMS_HEADER_TYPE = m(6032, Level.WARNING, "Transport property type is not compatible with JMS Header ''{0}''");
    public static final M JMS_ROUTING_ERROR_SENDING_MESSAGE = m(6033, Level.WARNING, "Unable to send JMS message to destination ''{0}'' : {1}.");
    public static final M JMS_ROUTING_NOT_SUPPORTED_JMS_HEADER =  m(6034,Level.WARNING, "JMS Header ''{0}'' is not supported.");
    public static final M JMS_ROUTING_NOT_SETTABLE_JMS_HEADER =  m(6035,Level.WARNING, "JMS Header ''{0}'' is not settable.");

    // ServerFtpRoutingAssertion
    @Deprecated
    public static final M FTP_ROUTING_FAILED_UPLOAD = m(6050, Level.WARNING, "Failed to upload request to {0}: {1}");
    public static final M FTP_ROUTING_PASSTHRU_NO_USERNAME = m(6054, Level.WARNING, "No user name found for passing through to FTP server");
    public static final M FTP_ROUTING_NO_COMMAND = m(6055, Level.WARNING, "No FTP command specified");
    public static final M FTP_ROUTING_UNSUPPORTED_COMMAND = m(6056, Level.WARNING, "FTP command ''{0}'' is not supported");
    public static final M FTP_ROUTING_SUCCEEDED_TRANSIENT_FAILURE = m(6057, Level.INFO, "FTP routing succeeded; transient negative completion reply code returned for command ''{0}'': {1}");
    public static final M FTP_ROUTING_SUCCEEDED_PERMANENT_FAILURE = m(6058, Level.INFO, "FTP routing succeeded; permanent negative completion reply code returned for command ''{0}'': {1}");
    public static final M FTP_ROUTING_FAILED_TRANSIENT_FAILURE = m(6059, Level.WARNING, "FTP routing failed; transient negative completion reply code returned for command ''{0}'': {1}");
    public static final M FTP_ROUTING_FAILED_PERMANENT_FAILURE = m(6060, Level.WARNING, "FTP routing failed; permanent negative completion reply code returned for command ''{0}'': {1}");
    public static final M FTP_ROUTING_FAILED_NO_REPLY = m(6061, Level.WARNING, "FTP routing failed; no reply returned for command ''{0}'': {1}");
    public static final M FTP_ROUTING_FAILED_INVALID_REPLY = m(6062, Level.WARNING, "FTP routing failed; invalid or unsupported reply code returned for command ''{0}'': {1}");
    public static final M FTP_ROUTING_SUCCEEDED = m(6063, Level.FINE, "FTP routing succeeded");
    public static final M FTP_ROUTING_ERROR = m(6064, Level.WARNING, "FTP routing error: {0}");
    public static final M FTP_ROUTING_CONNECTION_ERROR = m(6065, Level.WARNING, "FTP routing failed; connection error: {0}");
    public static final M FTP_ROUTING_UNABLE_TO_FIND_STORED_PASSWORD = m(6066, Level.WARNING, "Unable to find stored gateway account password: {0}");

    // ServerRequestWssSaml
    @Deprecated
    public static final M __UNUSED_SAML_AUTHN_STMT_REQUEST_NOT_SOAP = m(6100, Level.FINEST, "Request not SOAP; cannot validate SAML statement");
    @Deprecated
    public static final M __UNUSED_SAML_AUTHN_STMT_NO_TOKENS_PROCESSED = m(6101, Level.INFO, "No tokens were processed from this request: Returning AUTH_REQUIRED");
    @Deprecated
    public static final M __UNUSED_SAML_AUTHN_STMT_MULTIPLE_SAML_ASSERTIONS_UNSUPPORTED = m(6102, Level.WARNING, true, false, "Request contained more than one SAML token");
    public static final M SAML_AUTHN_STMT_NO_ACCEPTABLE_SAML_ASSERTION = m(6103, Level.INFO, "Assertion did not find an acceptable SAML token to use as credentials");
    public static final M SAML_STMT_VALIDATE_FAILED = m(6104, Level.WARNING, "SAML token validation errors: {0}");
    public static final M SAML_AUTHN_STMT_REQUEST_NOT_SOAP = m(6105, Level.FINEST, "{0} message not SOAP; cannot validate SAML statement");
    public static final M SAML_AUTHN_STMT_NO_TOKENS_PROCESSED = m(6106, Level.INFO, "No tokens were processed from {0} message: Returning AUTH_REQUIRED");
    public static final M SAML_AUTHN_STMT_MULTIPLE_SAML_ASSERTIONS_UNSUPPORTED = m(6107, Level.WARNING, true, false, "{0} message contained more than one SAML token");
    public static final M SAML_TOKEN_EXPIRATION_WARNING = m(6108, Level.WARNING, true, false, "SAML token is expired when constrained to maximum allowed lifetime");
    public static final M SAML_NAME_IDENTIFIER_INVALID_DN = m(6109, Level.INFO, true, false, "SAML token name identifier contained an invalid DN value for X509SubjectName format");

    // ServerWsTrustCredentialExchange
    public static final M WSTRUST_NO_SUITABLE_CREDENTIALS = m(6200, Level.INFO, "The current request did not contain credentials of any supported type");
    public static final M WSTRUST_RSTR_BAD_TYPE = m(6201, Level.WARNING, "WS-Trust response did not contain a security token of a supported type");
    public static final M WSTRUST_RSTR_STATUS_NON_200 = m(6202, Level.WARNING, "WS-Trust response had non-200 status");
    public static final M WSTRUST_NON_XML_MESSAGE = m(6203, Level.INFO, "Cannot replace security token in a non-XML message");
    public static final M WSTRUST_DECORATION_FAILED = m(6204, Level.WARNING, "Unable to replace security token");
    public static final M WSTRUST_ORIGINAL_TOKEN_NOT_XML = m(6205, Level.INFO, "Original security token was not XML; cannot remove from request");
    public static final M WSTRUST_MULTI_TOKENS = m(6206, Level.WARNING, "Multiple exchangeable Security Tokens found in request");
    public static final M WSTRUST_SERVER_HTTP_FAILED = m(6207, Level.WARNING, "HTTP failure talking to WS-Trust server");
    public static final M WSTRUST_NOT_SUPPORTED = m(6208, Level.WARNING, "Unsupported WS-Trust namespace: {0}");

    //ServerRegex
    public static final M REGEX_PATTERN_INVALID = m(6300, Level.WARNING, "Assertion has failed because of regex pattern ''{0}'' compile error: {1}");
    public static final M REGEX_TOO_BIG = m(6301, Level.WARNING, "Regular expression cannot be evaluated; content is too large (>= " + Integer.MAX_VALUE + " bytes)");
    public static final M REGEX_NO_REPLACEMENT = m(6302, Level.WARNING, "A replace was requested, but no replacement string was specified (null)");
    public static final M REGEX_NO_SUCH_PART = m(6303, Level.WARNING, "Cannot search or replace in nonexistent part #{0}");
    public static final M REGEX_NO_ENCODING = m(6304, Level.INFO, "Character encoding not specified; will use default {0}");
    public static final M REGEX_ENCODING_OVERRIDE = m(6305, Level.FINE, "Using overridden character encoding {0}");
    public static final M REGEX_NO_MATCH_FAILURE = m(6306, Level.INFO, "Failing because expression was not matched {0}");
    public static final M REGEX_MATCH_FAILURE = m(6307, Level.INFO, "Failing because expression was matched {0}");
    public static final M REGEX_REPLACEMENT_INVALID = m(6308, Level.WARNING, "Failing because replacement expression was not valid: {0}");

    // SAML Browser General
    public static final M SAMLBROWSER_LOGINFORM_NON_200 = m(6400, Level.WARNING, "HTTP GET for login form resulted in non-200 status");
    public static final M SAMLBROWSER_LOGINFORM_NOT_HTML = m(6401, Level.WARNING, "HTTP GET for login form resulted in non-HTML response");
    public static final M SAMLBROWSER_LOGINFORM_IOEXCEPTION = m(6402, Level.WARNING, "Could not read login form HTML");
    public static final M SAMLBROWSER_LOGINFORM_PARSEEXCEPTION = m(6403, Level.WARNING, "Unable to parse login form HTML");
    public static final M SAMLBROWSER_LOGINFORM_CANT_FIND_FIELDS = m(6404, Level.WARNING, "Unable to find login and/or password field(s) in login form HTML");
    public static final M SAMLBROWSER_LOGINFORM_MULTIPLE_FIELDS = m(6405, Level.WARNING, "Login form contained multiple username or password fields");
    public static final M SAMLBROWSER_LOGINFORM_MULTIPLE_FORMS = m(6406, Level.WARNING, "Multiple login forms found");
    public static final M SAMLBROWSER_LOGINFORM_NO_FORM = m(6407, Level.WARNING, "No matching login form found");
    public static final M SAMLBROWSER_LOGINFORM_BAD_METHOD = m(6408, Level.WARNING, "Login form method was not POST");
    public static final M SAMLBROWSER_LOGINFORM_INVALID = m(6409, Level.WARNING, "Login form is not valid");
    public static final M SAMLBROWSER_LOGINFORM_REDIRECT_INVALID = m(6410, Level.WARNING, "Invalid redirect after FORM login");
    public static final M SAMLBROWSER_CREDENTIALS_NOCREDS = m(6420, Level.WARNING, "Request does not contain any credentials");
    public static final M SAMLBROWSER_CREDENTIALS_CREDS_NOT_PASSWORD = m(6421, Level.WARNING, "Request credentials do not include a password");

    // SAML Browser/Artifact
    public static final M SAMLBROWSERARTIFACT_RESPONSE_NON_302 = m(6500, Level.WARNING, "HTTP GET for login resulted in non-302 status");
    public static final M SAMLBROWSERARTIFACT_REDIRECT_NO_QUERY = m(6501, Level.WARNING, "Redirect from login contained no query string");
    public static final M SAMLBROWSERARTIFACT_REDIRECT_BAD_QUERY = m(6502, Level.WARNING, "Redirect query string could not be parsed");
    public static final M SAMLBROWSERARTIFACT_REDIRECT_NO_ARTIFACT = m(6503, Level.WARNING, "Could not find SAML artifact in redirect query string");
    public static final M SAMLBROWSERARTIFACT_IOEXCEPTION = m(6504, Level.WARNING, "Could not login");

    // XPath Credential Source
    public static final M XPATHCREDENTIAL_REQUEST_NOT_XML = m(6600, Level.WARNING, "Request not XML");
    public static final M XPATHCREDENTIAL_LOGIN_XPATH_FAILED = m(6601, Level.INFO, "Login XPath evaluation failed");
    public static final M XPATHCREDENTIAL_LOGIN_XPATH_NOT_FOUND = m(6602, Level.INFO, "Login XPath evaluation failed to find any result");
    public static final M XPATHCREDENTIAL_LOGIN_FOUND_MULTI = m(6603, Level.WARNING, "Login XPath evaluation found multiple results");
    public static final M XPATHCREDENTIAL_LOGIN_XPATH_WRONG_RESULT = m(6604, Level.WARNING, "Login XPath evaluation found content of an unsupported type");
    public static final M XPATHCREDENTIAL_LOGIN_PARENT_NOT_ELEMENT = m(6605, Level.WARNING, "Cannot remove login element; parent is not an Element");

    public static final M XPATHCREDENTIAL_PASS_XPATH_FAILED = m(6611, Level.WARNING, "Password XPath evaluation failed");
    public static final M XPATHCREDENTIAL_PASS_XPATH_NOT_FOUND = m(6612, Level.WARNING, "Password XPath evaluation failed to find any result");
    public static final M XPATHCREDENTIAL_PASS_FOUND_MULTI = m(6613, Level.WARNING, "Login XPath evaluation found multiple results");
    public static final M XPATHCREDENTIAL_PASS_XPATH_WRONG_RESULT = m(6614, Level.WARNING, "Password XPath evaluation found content of an unsupported type");
    public static final M XPATHCREDENTIAL_PASS_PARENT_NOT_ELEMENT = m(6615, Level.WARNING, "Cannot remove password element; parent is not an Element");

    // Email and SNMP alerts
    public static final M EMAILALERT_MESSAGE_SENT = m(6700, Level.INFO, "Email message sent");
    public static final M EMAILALERT_BAD_TO_ADDR = m(6701, Level.WARNING, "Bad destination email address(es)");
    public static final M EMAILALERT_BAD_FROM_ADDR = m(6702, Level.WARNING, "Bad source email address");
    public static final M SNMP_BAD_TRAP_OID = m(6703, Level.WARNING, "The OID ending with zero is reserved for the message field: Using .1 for the trap OID instead");
    public static final M EMAILALERT_AUTH_FAIL = m(6704, Level.WARNING, "Authentication failure, message not sent");
    public static final M EMAILALERT_SSL_FAIL = m(6705, Level.WARNING, "SSL connection failure, message not sent");
    public static final M EMAILALERT_CONNECT_FAIL = m(6706, Level.WARNING, "Connection failure, message not sent");
    public static final M EMAILALERT_BAD_PORT = m(6707, Level.WARNING, "Bad smtp port set, message not sent");
    public static final M EMAILALERT_BAD_HOST = m(6708, Level.WARNING, "Bad smtp host set or not set at all, message not sent");
    public static final M EMAILALERT_BAD_USER = m(6709, Level.WARNING, "Bad smtp user name set or not set at all, message not sent");
    public static final M EMAILALERT_BAD_PWD = m(6710, Level.WARNING, "Bad smtp password set or not set at all, message not sent");
    public static final M SNMP_INVALID_TRAP_OID = m(6711, Level.WARNING, "Invalid OID (value={0}). Using .1 for the trap OID instead");
    public static final M SNMP_BAD_HOST = m(6712, Level.WARNING, "Bad smtp host set or not set at all (value={0})");


    // HTTP Form POST
    public static final M HTTPFORM_WRONG_TYPE = m(6800, Level.WARNING, true, false, "Request does not appear to be an HTTP form submission ({0})");
    public static final M HTTPFORM_NON_HTTP = m(6801, Level.WARNING, "Request was not received via HTTP");
    public static final M HTTPFORM_MULTIVALUE = m(6802, Level.WARNING, true, false, "Field {0} had multiple values; skipping");
    public static final M HTTPFORM_NO_SUCH_FIELD = m(6803, Level.WARNING, true, false, "Field {0} could not be found");
    public static final M HTTPFORM_NO_PARTS = m(6804, Level.WARNING, "No MIME parts were found");
    public static final M HTTPFORM_BAD_MIME = m(6805, Level.WARNING, "Unable to write new MIME message");
    public static final M HTTPFORM_TOO_BIG = m(6806, Level.WARNING, "Field {0} is too large (>= " + 512 * 1024 + " bytes)");

    // HtmlFormDataAssertion
    public static final M HTMLFORMDATA_NOT_HTTP = m(6850, Level.INFO, "Request is not HTTP");
    public static final M HTTP_POST_NOT_FORM_DATA = m(6851, Level.INFO, "HTTP POST does not contain HTML Form data. (content type= {0})");
    public static final M HTMLFORMDATA_METHOD_NOT_ALLOWED = m(6852, Level.INFO, "HTTP request method not allowed: {0}");
    public static final M HTMLFORMDATA_FIELD_NOT_FOUND = m(6582, Level.INFO, "A required Form field is missing in the request. (name={0})");
    public static final M HTMLFORMDATA_UNKNOWN_FIELD_NOT_ALLOWED = m(6853, Level.INFO, "Unspecified Form field encountered and not allowed. (name={0})");
    public static final M HTMLFORMDATA_UNKNOWN_FIELD_ALLOWED = m(6854, Level.FINE, "Unspecified Form field encountered but allowed through. (name={0})");
    public static final M HTMLFORMDATA_FAIL_DATATYPE = m(6855, Level.INFO, "Form field value has wrong data type. (name={0}, value={1}, data type allowed={2})");
    public static final M HTMLFORMDATA_FAIL_MINOCCURS = m(6856, Level.INFO, "Form field occurrences < min allowed. (name={0}, occurs={1}, min occurs allowed={2})");
    public static final M HTMLFORMDATA_FAIL_MAXOCCURS = m(6857, Level.INFO, "Form field occurrences > max allowed. (name={0}, occurs={1}, max occurs allowed={2})");
    public static final M HTMLFORMDATA_LOCATION_NOT_ALLOWED = m(6858, Level.INFO, "Form field is found in location not allowed. (name={0}, location not allowed={1})");
    public static final M HTMLFORMDATA_EMPTY_NOT_ALLOWED = m(6589, Level.INFO, "A required Form field is empty. (name={0})");

    // ServerThroughputQuota
    public static final M THROUGHPUT_QUOTA_EXCEEDED = m(6900, Level.INFO, "Quota exceeded on counter {0}. Assertion limit is {1} current counter value is {2}");
    public static final M THROUGHPUT_QUOTA_ALREADY_MET = m(6901, Level.INFO, "Quota already exceeded on counter {0}");
    public static final M THROUGHPUT_QUOTA_INVALID_COUNTER_ID = m(6902, Level.WARNING, "Invalid Quota Counter ID: {0}");
    public static final M THROUGHPUT_QUOTA_INVALID_MAX_QUOTA = m(6903, Level.WARNING, "Configured max quota value {0} is too large. The max value allowed is {1}");
    public static final M THROUGHPUT_QUOTA_INVALID_NEGATIVE_VALUE = m(6904, Level.WARNING, "Variable ''{0}'' is required to be a postive value");

    // ServerRateLimitAssertion
    public static final M RATELIMIT_RATE_EXCEEDED = m(6950, Level.INFO, "Rate limit exceeded on rate limiter {0}");
    public static final M RATELIMIT_SLEPT_TOO_LONG = m(6951, Level.INFO, "Unable to further delay request for rate limiter {0}, because maximum delay has been reached");
    public static final M RATELIMIT_NODE_CONCURRENCY = m(6952, Level.INFO, "Unable to delay request for rate limiter {0}, because queued thread limit has been reached");
    public static final M RATELIMIT_CONCURRENCY_EXCEEDED = m(6953, Level.INFO, "Concurrency exceeded on rate limiter {0}.");
    @Deprecated
    public static final M __UNUSED_RATELIMIT_MAX_RPS_TO_LARGE = m(6954, Level.INFO, "Rate limit of {0} exceeds maximum rate limit of {1}. Setting maximum limit to {2}.");
    public static final M RATELIMIT_BLACKED_OUT = m(6955, Level.INFO, "Failing request because counter is blacked out due to previous failure");

    // HTTP Form POST
    public static final M INVERSE_HTTPFORM_NO_SUCH_PART = m(7001, Level.WARNING, "Message has no part #{0}");
    public static final M INVERSE_HTTPFORM_TOO_BIG = m(7002, Level.WARNING, "Part #{0} is too large (>= " + 512 * 1024 + " bytes)");

    // Map Value
    public static final Messages.M MAP_VALUE_PATTERN_NOT_MATCHED = m(7025, Level.FINE, "Pattern not matched: {0}");
    public static final Messages.M MAP_VALUE_NO_PATTERNS_MATCHED = m(7026, Level.INFO, "No patterns were matched");
    public static final Messages.M MAP_VALUE_PATTERN_MATCHED = m(7027, Level.INFO, "Pattern matched: {0}");

    // Echo Routing assertion
    public static final M _UNUSED_CANNOT_ECHO_NON_XML = m(7050, Level.INFO, "Request cannot be echoed because it is not XML (Content-Type {0})");
    public static final M CANNOT_ECHO_NO_CTYPE = m(7051, Level.INFO, "Requests cannot be echoed because it has no Content-Type");

    // ComparisonAssertion (formerly known as EqualityAssertion)
    public static final M COMPARISON_OK = m(7100, Level.FINE, "Comparison matched");
    public static final M COMPARISON_NOT = m(7101, Level.INFO, "Comparison did not match: {0}");
    public static final M COMPARISON_BAD_OPERATOR = m(7102, Level.WARNING, "Unsupported operator: {0}");
    public static final M COMPARISON_NULL = m(7103, Level.INFO, "At least one comparison value was null");
    public static final M COMPARISON_CONVERTING = m(7104, Level.FINE, "Converting {0} value into {1}");
    public static final M COMPARISON_CANT_CONVERT = m(7105, Level.INFO, "Value of type {0} cannot be converted to {1}");
    public static final M COMPARISON_NOT_COMPARABLE = m(7106, Level.INFO, "{0} Value for binary predicate ''{1}'' is not Comparable; using value.toString() instead");
    public static final M COMPARISON_RIGHT_IS_NULL = m(7107, Level.INFO, "Right value of null is not supported by comparison ''{0}''");

    // CodeInjectionProtectionAssertion
    public static final M CODEINJECTIONPROTECTION_NOT_HTTP = m(7150, Level.FINE, "Target message is not HTTP.");
    public static final M CODEINJECTIONPROTECTION_SKIP_RESPONSE_NOT_ROUTED = m(7151, Level.FINE, "No response body to check because request has not been routed yet.");
    public static final M CODEINJECTIONPROTECTION_CANNOT_PARSE = m(7152, Level.WARNING, "Cannot parse {0} as {1}.");
    public static final M CODEINJECTIONPROTECTION_DETECTED_PARAM = m(7153, Level.WARNING, "{3} detected in {0} parameter \"{1}\": {2}");
    public static final M CODEINJECTIONPROTECTION_DETECTED = m(7154, Level.WARNING, "{2} detected in {0}: {1}");
    public static final M CODEINJECTIONPROTECTION_CANNOT_PARSE_CONTENT_TYPE = m(7155, Level.WARNING, "Message is not HTTP, cannot parse content type ''{0}''");
    public static final M CODEINJECTIONPROTECTION_SCANNING_URL_QUERY_STRING = m(7156, Level.FINE, "Scanning request URL query string.");
    public static final M CODEINJECTIONPROTECTION_SCANNING_BODY_URLENCODED = m(7157, Level.FINE, "Scanning request message body as application/x-www-form-urlencoded.");
    public static final M CODEINJECTIONPROTECTION_SCANNING_BODY_FORMDATA = m(7158, Level.FINE, "Scanning {0} message body as multipart/form-data.");
    public static final M CODEINJECTIONPROTECTION_SCANNING_BODY_XML = m(7159, Level.FINE, "Scanning {0} message body as text/xml.");
    public static final M CODEINJECTIONPROTECTION_SCANNING_BODY_TEXT = m(7160, Level.FINE, "Scanning {0} message body as text.");
    public static final M CODEINJECTIONPROTECTION_SCANNING_ATTACHMENT_XML = m(7161, Level.FINE, "Scanning {0} as text/xml.");
    public static final M CODEINJECTIONPROTECTION_SCANNING_ATTACHMENT_TEXT = m(7162, Level.FINE, "Scanning {0} as text/xml.");
    public static final M CODEINJECTIONPROTECTION_ALREADY_ROUTED = m(7163, Level.WARNING, "Unable to protect against code injection attacks - the request has already been routed");
    public static final M CODEINJECTIONPROTECTION_SCANNING_BODY_JSON = m(7164, Level.FINE, "Scanning {0} message body as application/json.");
    public static final M CODEINJECTIONPROTECTION_SCANNING_URL_PATH = m(7165, Level.FINE, "Scanning request URL path.");
    public static final M CODEINJECTIONPROTECTION_DETECTED_PATH = m(7166, Level.WARNING, "{3} detected in {0} path \"{1}\": {2}");

    // SqlAttackAssertion
    public static final M SQLATTACK_UNRECOGNIZED_PROTECTION = m(7200, Level.WARNING, "Unrecognized protection name: {0}.  Assertion will always fail.");
    public static final M SQLATTACK_ALREADY_ROUTED = m(7203, Level.WARNING, "Unable to protect against SQL attacks - the request has already been routed");
    public static final M SQLATTACK_REJECTED = m(7204, Level.WARNING, true, false, "{0} was flagged by Protect Against SQL Attacks Assertion");
    public static final M SQLATTACK_SKIP_RESPONSE_NOT_ROUTED = m(7205, Level.FINE, "No response body to check because request has not been routed yet.");
    public static final M SQLATTACK_NOT_HTTP = m(7210, Level.FINE, "Target message is not HTTP.");
    public static final M SQLATTACK_SCANNING_URL_QUERY_STRING = m(7211, Level.FINE, "Scanning request URL query string.");
    public static final M SQLATTACK_SCANNING_BODY_TEXT = m(7212, Level.FINE, "Scanning {0} message body as text.");
    public static final M SQLATTACK_CANNOT_PARSE = m(7213, Level.WARNING, "Cannot parse {0} as {1}.");
    public static final M SQLATTACK_DETECTED_PARAM = m(7214, Level.WARNING, "{3} detected in {0} parameter \"{1}\": {2}");
    public static final M SQLATTACK_DETECTED = m(7215, Level.WARNING, "{2} detected in {0}: {1}");
    public static final M SQLATTACK_SCANNING_URL_PATH = m(7216, Level.FINE, "Scanning request URL path.");
    public static final M SQLATTACK_DETECTED_PATH = m(7217, Level.WARNING, "{3} detected in {0} path \"{1}\": {2}");

    /**
     * @deprecated
     */
    @Deprecated
    public static final M _UNUSED_SQLATTACK_REQUEST_REJECTED = m(7201, Level.WARNING, true, false, "Request was flagged by SQL attack protection assertion");

    // RequestSizeLimit
    /**
     * @deprecated
     */
    @Deprecated
    public static final M REQUEST_BODY_TOO_LARGE = m(7220, Level.WARNING, "Request body size exceeds configured limit");
    /**
     * @deprecated
     */
    @Deprecated
    public static final M REQUEST_FIRST_PART_TOO_LARGE = m(7221, Level.WARNING, "Request first part size exceeds configured limit");
    public static final M MESSAGE_BODY_TOO_LARGE = m(7222, Level.WARNING, "{0} body size exceeds configured limit");
    public static final M MESSAGE_FIRST_PART_TOO_LARGE = m(7223, Level.WARNING, "{0} first part size exceeds configured limit");
    public static final M MESSAGE_BAD_CONTENT_TYPE = m(7224, Level.WARNING, "{0} content type is syntactically invalid: {1}");

    // MessageBufferingAssertion
    public static final Messages.M MESSAGE_ALREADY_BUFFERED = m(7225, Level.WARNING, "{0} message has already been buffered");

    // OversizedTextAssertion
    public static final M OVERSIZEDTEXT_ALREADY_ROUTED = m(7230, Level.WARNING, "Unable to protect against document structure threats -- the request has already been routed");
    /**
     * @deprecated
     */
    @Deprecated
    public static final M _UNUSED_OVERSIZEDTEXT_OVERSIZED_TEXT = m(7231, Level.WARNING, "Request includes an oversized text node or attribute value");
    /**
     * @deprecated
     */
    @Deprecated
    public static final M _UNUSED_XML_NESTING_DEPTH_EXCEEDED = m(7232, Level.WARNING, "Request XML nesting depth exceeds the policy limit");
    /**
     * @deprecated
     */
    @Deprecated
    public static final M _UNUSED_OVERSIZEDTEXT_EXTRA_PAYLOAD = m(7233, Level.WARNING, "Request message SOAP Body has too many children");
    public static final Messages.M REQUEST_NOT_SOAP = m(7234, Level.WARNING, "Request message does not have a valid SOAP Envelope");
    public static final M REQUEST_BAD_XML = m(7235, Level.WARNING, "Request message is not well-formed XML");
    public static final M RESPONSE_BAD_XML = m(7236, Level.WARNING, "Response message is not well-formed XML");
    public static final M MESSAGE_BAD_XML = m(7237, Level.WARNING, "Message is not well-formed XML");
    public static final M OVERSIZEDTEXT_NODE_OR_ATTRIBUTE = m(7238, Level.WARNING, "{0} includes an oversized text node or attribute value");
    public static final M OVERSIZEDTEXT_XML_NESTING_DEPTH_EXCEEDED = m(7239, Level.WARNING, "{0} XML nesting depth exceeds the policy limit");
    public static final M OVERSIZEDTEXT_EXTRA_PAYLOAD_ELEMENTS = m(7240, Level.WARNING, "{0} message SOAP Body has too many children");
    public static final M OVERSIZEDTEXT_NOT_SOAP = m(7241, Level.WARNING, "{0} message does not have a valid SOAP Envelope");
    public static final M OVERSIZEDTEXT_NOT_XML = m(7242, Level.WARNING, "{0} is not XML.");
    public static final M OVERSIZEDTEXT_SKIP_RESPONSE_NOT_ROUTED = m(7243, Level.FINE, "No response body to check because request has not been routed yet.");
    public static final M MESSAGE_VARIABLE_BAD_XML = m(7244, Level.WARNING, "Message variable {0} does not contain well-formed XML");
    public static final M MESSAGE_VARIABLE_NOT_XML = m(7245, Level.INFO, "Message variable {0} does not contain XML");
    public static final M OVERSIZEDTEXT_NS_DECLARATION_EXCEEDED = m(7246, Level.WARNING, "{0} exceeds namespace declaration limit");
    public static final M OVERSIZEDTEXT_NS_PREFIX_DECLARATION_EXCEEDED = m(7247, Level.WARNING, "{0} exceeds namespace prefix declaration limit");
    public static final M MESSAGE_VARIABLE_NOT_XML_WARNING = m(7248, Level.WARNING, "Message variable {0} does not contain XML");

    // ServerWsTrustCredentialExchange
    public static final M WSFEDPASS_NO_SUITABLE_CREDENTIALS = m(7300, Level.INFO, "The current request did not contain credentials of any supported type");
    public static final M WSFEDPASS_RSTR_BAD_TYPE = m(7301, Level.WARNING, "WS-Federation response did not contain a security token of a supported type");
    public static final M WSFEDPASS_RSTR_STATUS_NON_200 = m(7302, Level.WARNING, "WS-Federation response had non-200 status");
    public static final M WSFEDPASS_NON_XML_MESSAGE = m(7303, Level.INFO, "Cannot replace security token in non-XML message");
    public static final M WSFEDPASS_DECORATION_FAILED = m(7304, Level.WARNING, "Unable to replace security token");
    public static final M WSFEDPASS_ORIGINAL_TOKEN_NOT_XML = m(7305, Level.INFO, "Original security token was not XML; cannot remove from request");
    public static final M WSFEDPASS_MULTI_TOKENS = m(7306, Level.WARNING, true, false, "Multiple security tokens found in request");
    public static final M WSFEDPASS_SERVER_HTTP_FAILED = m(7307, Level.WARNING, "HTTP failure while communicating with WS-Federation server");
    public static final M WSFEDPASS_SERVER_HTTP_ENCODING = m(7308, Level.WARNING, "Unknown encoding from WS-Federation server");
    public static final M WSFEDPASS_SERVER_HTML_INVALID = m(7309, Level.WARNING, "Cannot parse HTML from WS-Federation server");
    public static final M WSFEDPASS_CONFIG_INVALID = m(7310, Level.WARNING, "Invalid IP/STS URL in policy configuration");
    public static final M WSFEDPASS_AUTH_FAILED = m(7311, Level.WARNING, "Authentication with service failed");
    public static final M WSFEDPASS_UNAUTHORIZED = m(7312, Level.WARNING, "Not authorized to access this service");

    // ServerRequestWssKerberos messages
    public static final M REQUEST_WSS_KERBEROS_NON_SOAP = m(7401, Level.INFO, "Request not SOAP; unable to check for WS-Security Binary Security Token");
    /**
     * @deprecated
     */
    @Deprecated
    public static final M _UNUSED_REQUEST_WSS_KERBEROS_NO_WSS_LEVEL_SECURITY = m(7402, Level.INFO, "Request did not contain any WSS-level security");
    public static final M REQUEST_WSS_KERBEROS_NO_TOKEN = m(7403, Level.INFO, "No tokens were processed from this request: Returning AUTH_REQUIRED");
    public static final M REQUEST_WSS_KERBEROS_NO_TICKET = m(7404, Level.INFO, "This assertion did not find a Kerberos Binary Security Token to use as credentials. Returning AUTH_REQUIRED.");
    public static final M REQUEST_WSS_KERBEROS_GOT_TICKET = m(7405, Level.FINE, "Kerberos ticket processed, principal is:{0}");
    public static final M REQUEST_WSS_KERBEROS_GOT_SESSION = m(7406, Level.FINE, "Kerberos session processed, principal is:{0}");
    public static final M REQUEST_WSS_KERBEROS_INVALID_CONFIG = m(7407, Level.WARNING, "Either the Kerberos server configuration is invalid or the KDC is unreachable");
    public static final M REQUEST_WSS_KERBEROS_INVALID_TICKET = m(7408, Level.WARNING, "Could not process Kerberos ticket (not for this service?)");

    // ServerMappingAssertion messages
    public static final M MAPPING_NO_IDMAP = m(7500, Level.WARNING, "No identity mMapping for provider #{0} found in attribute #{1}");
    public static final M MAPPING_NO_TOKMAP = m(7501, Level.WARNING, "No security token mapping for provider #{0} found in attribute #{1}");
    public static final M MAPPING_NO_TOKVALUE = m(7502, Level.WARNING, "No suitable value could be found in any security token");
    public static final M MAPPING_NO_IDENTS = m(7503, Level.WARNING, "No matching identities could be found");
    public static final M MAPPING_NO_IDVALUE = m(7504, Level.WARNING, "No value could be found from any matching identity");

    public static final Messages.M USERDETAIL_FINEST = m(-1, Level.FINEST, "{0}");
    public static final Messages.M USERDETAIL_FINER = m(-2, Level.FINER, "{0}");
    public static final Messages.M USERDETAIL_FINE = m(-3, Level.FINE, "{0}");
    public static final Messages.M USERDETAIL_INFO = m(-4, Level.INFO, "{0}");
    public static final Messages.M USERDETAIL_WARNING = m(-5, Level.WARNING, "{0}");
    public static final Messages.M NO_SUCH_VARIABLE = m(-6, Level.FINE, "No such variable: {0}");
    public static final Messages.M VARIABLE_IS_NULL = m(-7, Level.FINE, "Variable exists but has no value: {0}");
    public static final Messages.M VARIABLE_NOTSET = m(-8, Level.WARNING, "Variable cannot be set: {0}");
    public static final Messages.M VARIABLE_INVALID_VALUE = m(-9, Level.FINE, "Variable ''{0}'' should be of type ''{1}''");
    public static final Messages.M NO_SUCH_VARIABLE_WARNING = m(-10, Level.WARNING, "No such variable: {0}");
    // Do not add values here, these values are outside the permitted range for assertion messages
    // (add to the "Generic Assertion Messages" section at the top)

    public static final M WSI_BSP_REQUEST_NON_SOAP = m(7600, Level.INFO, "Request not SOAP; unable to check for WS-I Basic Security Profile compliance");
    public static final M WSI_BSP_RESPONSE_NON_SOAP = m(7601, Level.INFO, false, true, "Response not SOAP; unable to check for WS-I Basic Security Profile compliance");
    public static final M WSI_BSP_REQUEST_NON_COMPLIANT = m(7602, Level.WARNING, true, false, "WS-I BSP rule broken in request ({0}): {1}");
    public static final M WSI_BSP_RESPONSE_NON_COMPLIANT = m(7603, Level.WARNING, false, true, "WS-I BSP rule broken in response ({0}): {1}");
    public static final M WSI_BSP_REQUEST_FAIL = m(7604, Level.INFO, "Failing non WS-I BSP compliant request");
    public static final M WSI_BSP_RESPONSE_FAIL = m(7605, Level.INFO, "Failing non WS-I BSP compliant response");
    public static final M WSI_BSP_XPATH_ERROR = m(7606, Level.WARNING, "Server WS-I BSP rules are incorrect");

    public static final M WSI_SAML_REQUEST_NON_SOAP = m(7700, Level.INFO, "Request not SOAP; unable to check for WS-I SAML Token Profile compliance");
    public static final M WSI_SAML_RESPONSE_NON_SOAP = m(7701, Level.INFO, false, true, "Response not SOAP; unable to check for WS-I  SAML Token Profile compliance");
    public static final M WSI_SAML_REQUEST_NON_COMPLIANT = m(7702, Level.WARNING, true, false, "WS-I SAML Token Profile rule broken in request ({0}): {1}");
    public static final M WSI_SAML_RESPONSE_NON_COMPLIANT = m(7703, Level.WARNING, false, true, "WS-I SAML Token Profile rule broken in response ({0}): {1}");
    public static final M WSI_SAML_REQUEST_FAIL = m(7704, Level.INFO, "Failing non WS-I SAML Token Profile compliant request");
    public static final M WSI_SAML_RESPONSE_FAIL = m(7705, Level.INFO, "Failing non WS-I SAML Token Profile compliant response");
    public static final M WSI_SAML_XPATH_ERROR = m(7706, Level.WARNING, "Server WS-I SAML Token Profile rules are incorrect");

    public static final M REQUIRE_WSS_TIMESTAMP_NOTAPPLICABLE = m(7800, Level.INFO, true, false, "The assertion is not applicable because {0} is not XML or SOAP");
    public static final M REQUIRE_WSS_TIMESTAMP_NOTIMESTAMP = m(7801, Level.INFO, "No Timestamp found in {0}");
    public static final M REQUIRE_WSS_TIMESTAMP_NOT_SIGNED = m(7802, Level.WARNING, "Timestamp found in {0}, but was not signed");
    public static final M REQUIRE_WSS_TIMESTAMP_CREATED_FUTURE = m(7803, Level.WARNING, "Timestamp found in {0}, but Created time was too far in the future");
    public static final M REQUIRE_WSS_TIMESTAMP_EXPIRED = m(7804, Level.WARNING, "Timestamp found in {0}, but expired too long ago");
    public static final M REQUIRE_WSS_TIMESTAMP_NO_EXPIRES = m(7805, Level.WARNING, "Timestamp found in {0}, but has no Expires time");
    public static final M REQUIRE_WSS_TIMESTAMP_EXPIRES_TOOLATE = m(7806, Level.FINE, "Timestamp found in {0} exceeds maximimum allowed lifetime, constraining to maximum");
    public static final M REQUIRE_WSS_TIMESTAMP_NO_CREATED = m(7807, Level.WARNING, "Timestamp found in {0}, but has no Created time");
    public static final M REQUIRE_WSS_TIMESTAMP_EXPIRED_TRUNC = m(7809, Level.WARNING, "Timestamp found in {0}, but is expired when constrained to maximum allowed lifetime");

    public static final M ADD_WSS_TOKEN_UNSUPPORTED_TYPE = m(7900, Level.WARNING, "Unsupported security token type: {0}");
    public static final M ADD_WSS_TOKEN_NO_CREDS = m(7901, Level.WARNING, true, false, "No credentials were available from the request");
    public static final M ADD_WSS_TOKEN_NO_USERNAME = m(7902, Level.WARNING, true, false, "Credentials were available, but no username could be found");
    public static final M ADD_WSS_TOKEN_NO_PASSWORD = m(7903, Level.WARNING, true, false, "Password inclusion was requested, but no password could be found");
    public static final M ADD_WSS_TOKEN_NOT_SESSION = m(7904, Level.WARNING, false, false, "Specified context variable exists but does not contain a WS-SecureConversation session");
    public static final M ADD_WSS_TOKEN_NOT_SAML = m(7905, Level.WARNING, false, false, "SAML assertion variable did not contain a valid SAML assertion: {0}");
    public static final M ADD_WSS_TOKEN_MULTIPLE_REQ_TOKENS = m(7906, Level.INFO, false, false, "Unable to identify the encryption recipient because we are decorating a response to a request with multiple eligible tokens.  Encryption recipient must be specified explicitly.");
    public static final M ADD_WSS_TOKEN_SAML_SECRET_KEY_UNAVAILABLE = m(7907, Level.WARNING, false, false, "The SAML assertion uses a secret key for subject confirmation, but the Gateway does not already possess this key, and is unable to unwrap it from the EncryptedKey");
    public static final M ADD_WSS_TOKEN_SAML_SECRET_KEY_UNAVAILABLE_WITH_MORE_INFO = m(7908, Level.WARNING, false, false, "The SAML assertion uses a secret key for subject confirmation, but the Gateway does not already possess this key, and is unable to unwrap it from the EncryptedKey: {0}");

    public static final M CUSTOM_ASSERTION_INFO = m(8000, Level.INFO, "Assertion ''{0}''; {1}");
    public static final M CUSTOM_ASSERTION_WARN = m(8001, Level.WARNING, "Assertion ''{0}''; {1}");

    public static final M WSDLOPERATION_NOMATCH = m(8100, Level.INFO, "Could not match WSDL operation ({0} instead of {1})");
    public static final M WSDLOPERATION_CANNOTIDENTIFY = m(8101, Level.INFO, "Cannot identify any WSDL operation from request");

    public static final M HTTPNEGOTIATE_WARNING = m(8200, Level.WARNING, "Could not process Kerberos token (Negotiate); error is ''{0}''");

    public static final M FTP_CREDENTIAL_NOT_FTP = m(8300, Level.INFO, "Request not FTP; unable to extract FTP credentials.");
    public static final M FTP_CREDENTIAL_NO_AUTH = m(8301, Level.FINE, "Not authenticated.");
    public static final M FTP_CREDENTIAL_AUTH_USER = m(8302, Level.FINE, "Found credentials for user ''{0}''.");

    public static final M SAML_ISSUER_ISSUED_AUTHN = m(8400, Level.FINE, "Issued SAML Authentication statement");
    public static final M SAML_ISSUER_ISSUED_ATTR = m(8401, Level.FINE, "Issued SAML Attribute statement");
    public static final M SAML_ISSUER_ISSUED_AUTHZ = m(8402, Level.FINE, "Issued SAML Authorization Decision statement");
    public static final M SAML_ISSUER_ADDING_ATTR = m(8403, Level.FINE, "Adding attribute {0} = {1}");
    public static final M SAML_ISSUER_AUTH_REQUIRED = m(8404, Level.WARNING, "NameIdentifier configured as \"From Authenticated User\", but no user has been authenticated");
    public static final M SAML_ISSUER_NOT_XML = m(8405, Level.WARNING, "Message is not XML");
    public static final M SAML_ISSUER_NOT_SOAP = m(8406, Level.WARNING, "Message is not SOAP");
    public static final M SAML_ISSUER_BAD_XML = m(8407, Level.WARNING, "Message appeared to be SOAP but is not valid");
    public static final M SAML_ISSUER_CANT_DECORATE = m(8408, Level.WARNING, "WS-Security decoration failed");
    public static final M SAML_ISSUER_MISSING_NIVAL = m(8409, Level.WARNING, "Specified NameIdentifier chosen, but no value specified; using default");
    public static final M SAML_ISSUER_ATTR_STMT_MISSING_ATTRIBUTE = m(8410, Level.FINE, "One or more configured Attributes are missing: {0}");
    public static final M SAML_ISSUER_ATTR_STMT_INVALID_FILTER_ATTRIBUTE = m(8411, Level.WARNING, "Ignoring invalid filter Attribute / AttributeDesignator value: {0}");
    public static final M SAML_ISSUER_ATTR_STMT_FAIL_UNKNOWN_FILTER_ATTRIBUTE = m(8412, Level.FINE, "Attribute filter contained one or more unknown Attribute / AttributeDesignator elements: {0}");
    public static final M SAML_ISSUER_ATTR_STMT_DUPLICATE_FILTER_ATTRIBUTE = m(8413, Level.WARNING, "Attribute filter values contained duplicate Attribute / AttributeDesignator elements: {0}");
    public static final M SAML_ISSUER_ATTR_STMT_FILTER_REMOVED_ALL_ATTRIBUTES = m(8414, Level.FINE, "No Attributes were available after SAML Attribute filter was applied.");
    public static final M SAML_ISSUER_ATTR_STMT_VALUE_EXCLUDED_ATTRIBUTES = m(8415, Level.FINE, "Attribute filter AttributeValue excluded some Attributes: {0}");
    public static final M SAML_ISSUER_ATTR_STMT_FILTERED_ATTRIBUTES = m(8416, Level.FINE, "Attribute filter filtered some Attributes: {0}");
    public static final M SAML_ISSUER_CANNOT_PARSE_XML = m(8417, Level.WARNING, "Error parsing the {0} for expected SOAP Message: {1}");
    public static final M SAML_ISSUER_BAD_XML_WITH_ERROR = m(8418, Level.WARNING, "Message appeared to be SOAP but is not valid: {0}");
    public static final M SAML_ISSUER_ATTR_STMT_VALUE_EXCLUDED_ATTRIBUTE_DETAILS = m(8419, Level.FINE, "Resolved value for Attribute ''{0}'' was filtered as its value ''{1}'' was not included in the corresponding filter Attribute''s AttributeValue.");
    public static final M SAML_ISSUER_ATTR_STMT_PROCESSING_WARNING = m(8420, Level.WARNING, "Problem processing Attribute Statement: {0}");
    public static final M SAML_ISSUER_ATTR_STMT_FILTER_EXPRESSION_NO_VALUES = m(8421, Level.FINE, "Filter expression ''{0}'' yielded no values.");

    public static final M IDENTITY_ATTRIBUTE_NO_USER = m(8450, Level.INFO, "No user from the expected identity provider has yet been authenticated");
    public static final M IDENTITY_ATTRIBUTE_MULTI_USERS = m(8451, Level.INFO, "Multiple users from the expected identity provider have been authenticated; choosing the first");

    public static final M INCLUDE_POLICY_INVALID = m(8500, Level.WARNING, "Included policy was updated, and is now invalid: {0}");
    public static final M INCLUDE_POLICY_NOT_FOUND = m(8501, Level.WARNING, "Included policy #{0} ({1}) could not be located");
    public static final M INCLUDE_POLICY_FAILURE = m(8502, Level.WARNING, "Included policy failure: {0}");

    public static final M WS_ADDRESSING_NO_HEADERS = m(8550, Level.WARNING, "Required WS-Addressing headers not present");
    public static final M WS_ADDRESSING_NO_SIGNED_HEADERS = m(8551, Level.WARNING, "Required signed WS-Addressing headers not present");
    public static final M WS_ADDRESSING_HEADERS_OK = m(8552, Level.FINE, "WS-Addressing headers present");
    public static final M WS_ADDRESSING_HEADERS_NONE = m(8553, Level.FINE, "No WS-Addressing headers found");
    public static final M WS_ADDRESSING_HEADERS_SIGNED_NONE = m(8554, Level.FINE, "No signed WS-Addressing headers found");
    public static final M WS_ADDRESSING_FOUND_HEADERS = m(8555, Level.FINE, "Found WS-Addressing headers for namespace {0}");
    public static final M WS_ADDRESSING_FOUND_SIGNED_HEADERS = m(8556, Level.FINE, "Found signed WS-Addressing headers for namespace {0}");

    public static final M TEMPLATE_RESPONSE_EARLY = m(8600, Level.FINE, "Sending response early");
    public static final M TEMPLATE_RESPONSE_NOT_HTTP = m(8601, Level.WARNING, "Unable to send early response for non HTTP message.");
    public static final M TEMPLATE_RESPONSE_INVALID_STATUS = m(8602, Level.WARNING, "Invalid response status: {0}");

    public static final M NCESDECO_NOT_SOAP = m(8650, Level.WARNING, "{0} is not SOAP");
    public static final M NCESDECO_BAD_XML = m(8651, Level.WARNING, "{0} parse failure: {1}");
    public static final M NCESDECO_NO_CREDS = m(8652, Level.WARNING, "Credentials are required for internal SAML generation, but no credentials have been collected");
    public static final M NCESDECO_IDFE = m(8653, Level.WARNING, "Invalid {0} message Format: {1}");
    public static final M NCESDECO_WARN_MISC = m(8654, Level.WARNING, "Unable to decorate {0}: {1}");

    public static final M NCESVALID_NO_MSG = m(8700, Level.WARNING, "{0} variable has not been set; unable to proceed");
    public static final M NCESVALID_BAD_XML = m(8701, Level.WARNING, "{0} parse failure: {1}");
    public static final M NCESVALID_NOT_SOAP = m(8702, Level.WARNING, "{0} is not soap");
    public static final M NCESVALID_NO_SAML = m(8703, Level.WARNING, "{0} did not contain a signed SAML token");
    public static final M NCESVALID_NO_TIMESTAMP = m(8704, Level.WARNING, "{0} did not contain a signed wsu:Timestamp");
    public static final M NCESVALID_NO_MESSAGEID = m(8705, Level.WARNING, "{0} did not contain a signed wsa:MessageID");
    public static final M NCESVALID_BODY_NOT_SIGNED = m(8706, Level.WARNING, "{0} SOAP Body was not signed");
    public static final M NCESVALID_DIFF_SIGNATURES = m(8707, Level.WARNING, "{0} contained the expected elements, but they were covered by different Signatures");
    public static final M NCESVALID_NO_CERTIFICATE = m(8708, Level.WARNING, "{0} does not use an X.509 certificate for signing");
    public static final M NCESVALID_CERT_NOT_USED = m(8709, Level.WARNING, "{0} signing X.509 certificate does not cover expected elements");
    public static final M NCESVALID_CERT_VAL_ERROR = m(8710, Level.WARNING, "{0} signing X.509 certificate validation error");
    public static final M NCESVALID_CERT_UNTRUSTED = m(8711, Level.WARNING, "{0} signing X.509 certificate is not trusted");

    public static final M DOMAINID_REQUEST_NOT_HTTP = m(8780, Level.INFO, "Request is not HTTP; could not get domain ID injection header");
    public static final M DOMAINID_NOT_ATTEMPTED = m(8781, Level.INFO, "Requestor did not attempt to include domain ID information");
    public static final M DOMAINID_BAD_REQUEST = m(8782, Level.WARNING, "Invalid format for {0}: {1}");
    public static final M DOMAINID_FAILED = m(8783, Level.WARNING, "Requestor attempted to gather domain ID information but encountered an error");
    public static final M DOMAINID_DECLINED = m(8784, Level.INFO, "Requestor explicitly declines to provide domain ID information");
    public static final M DOMAINID_INCOMPLETE = m(8785, Level.WARNING, "Requestor provided incomplete domain ID information");
    public static final M DOMAINID_IDENTIFIER_MISSING = m(8786, Level.WARNING, "Requestor did not include required identifier: {0}");

    public static final M REMOVE_ELEMENT_NOT_XML = m(8800, Level.WARNING, "Message is not XML.");
    public static final M INSERT_ELEMENT_EXISTING_NOT_FOUND = m(8801, Level.WARNING, "Unable to insert element because no existing element was found");
    public static final M INSERT_ELEMENT_EXISTING_TOO_MANY = m(8802, Level.WARNING, "Unable to insert element because more than one existing element was found");
    public static final M INSERT_ELEMENT_BAD_FRAGMENT = m(8803, Level.WARNING, "Unable to insert element because the new element was not a well-formed XML fragment");

    public static final M WSSECURITY_NON_SOAP = m(8850, Level.WARNING, "Message is not SOAP.");
    public static final M WSSECURITY_ERROR = m(8851, Level.WARNING, "Unable to decorate {0}: {1}");
    public static final M WSSECURITY_RECIP_NO_CERT = m(8852, Level.WARNING, "Could not find trusted certificate {0}");
    public static final M WSSECURITY_RECIP_CERT_ERROR = m(8853, Level.WARNING, "Error when finding trusted certificate {0}: {1}");
    public static final M WSSECURITY_RECIP_CERT_EXP = m(8854, Level.INFO, "Error checking certificate expiry for {0}");

    public static final M ADD_WSS_USERNAME_NOT_SOAP = m(8880, Level.WARNING, "{0} message is not SOAP, cannot add WSS UsernameToken");
    public static final M ADD_WSS_USERNAME_MORE_THAN_ONE_TOKEN = m(8881, Level.WARNING, "Request included more than one X509 security token whose key ownership was proven");

    public static final M XACML_REQUEST_ERROR = m(8900, Level.WARNING, "Error generating request: {0}");
    public static final M XACML_NOT_FOUND_OPTION_OFF = m(8901, Level.INFO, "A value for {0} was not found. Cannot add <Attribute> element to the {1} element");
    public static final M XACML_NOT_FOUND_OPTION_ON = m(8902, Level.WARNING, "Assertion failed: a value for {0} was not found");
    public static final M XACML_INVALID_XML_ATTRIBUTE = m(8903, Level.WARNING, "XML attribute name {0} with value {1} are not valid for an XML attribute");
    public static final M XACML_INCORRECT_NUM_RESULTS_FOR_FIELD = m(8904, Level.INFO, "Found {0} results for field {1}. Only the first value will be used");
    public static final M XACML_INCORRECT_TYPE_FOR_FIELD = m(8905, Level.INFO, "Incorrect xpath result type {0} found for field {1}. Cannot add <Attribute> element to the {2} element");
    public static final M XACML_BASE_EXPRESSION_NO_RESULTS = m(8906, Level.INFO, "Xpath base expression {0} found no results. Affects all dependent attribute fields");
    public static final M XACML_INVALID_ISSUE_INSTANT = m(8907, Level.INFO, "Invalid value for issue instant: {0} IssueInstant, if supplied, must be a valid datetime with a format \"yyyy-MM-dd'T'HH:mm:ss[Z]\"");
    public static final M XACML_NOT_ALL_VALUES_USED = m(8908, Level.INFO, "Not all values from {0} were used as {1} also part of iteration and had less values");
    public static final M XACML_NOT_ALL_CTX_VARS_USED = m(8909, Level.INFO, "Only {0} values from all referenced context variables will be used. The largest referenced variable has {1} values");
    public static final M XACML_INCORRECT_NAMESPACE_URI = m(8910, Level.INFO, "Namespace prefix {0} with incorrect namespace URI may cause XPath base pattern to match no results");
    public static final M XACML_UNSUPPORTED_MIXED_CONTENT = m(8911, Level.INFO, "Unsupported type of mixed content found for AttributeValue: {0}");
    public static final M XACML_CANNOT_IMPORT_XML_ELEMENT_INTO_REQUEST = m(8912, Level.WARNING, "Cannot import XML element into XACML request document: {0}");

    public static final M XACML_PDP_INVALID_REQUEST = m(8930, Level.WARNING, "Error processing XACML request: {0}");
    public static final M XACML_PDP_REQUEST_NOT_ENCAPSULATED = m(8931, Level.WARNING, "XACML request is not SOAP encapsulated");
    public static final M XACML_PDP_REQUEST_NAMESPACE_UNKNOWN = m(8932, Level.WARNING, "XACML request namespace is not recognized: {0}");

    public static final M MTOM_ENCODE_ERROR = m(8960, Level.WARNING, "Error encoding MTOM message: {0}");
    public static final M MTOM_ENCODE_NONE = m(8961, Level.INFO, "Not encoding MTOM message, no elements found to encode.");
    public static final M MTOM_ENCODE_INVALID_XPATH = m(8962, Level.WARNING, "Invalid XPath expression for MTOM encoding ''{0}''");
    public static final M MTOM_ENCODE_XPATH_FAILED = m(8963, Level.WARNING, "XPath expression did not match.");
    public static final M MTOM_DECODE_ERROR = m(8964, Level.WARNING, "Error decoding MTOM message: {0}");
    public static final M MTOM_VALIDATE_ERROR = m(8965, Level.WARNING, "Error validating MTOM message: {0}");

    public static final M MCM_VARIABLE_NOT_FOUND = m(9001, Level.WARNING, "Message context mapping variable not found {0}.");
    public static final M MCM_MAPPING_OVERRIDDEN = m(9002, Level.INFO, "Message context mapping overridden {0}.");
    public static final M MCM_TOO_MANY_MAPPINGS = m(9003, Level.WARNING, "Message context mapping dropped {0}.");
    public static final M MCM_TOO_LONG_VALUE = m(9004, Level.WARNING, "Message context mapping value truncated {0}.");

    public static final M LDAP_QUERY_SEARCH_FILTER = m(9025, Level.FINE, "LDAP Query using search filter: {0}");
    public static final M LDAP_QUERY_ERROR = m(9026, Level.WARNING, "LDAP Query error: {0}");
    public static final M LDAP_QUERY_NO_RESULTS = m(9027, Level.INFO, "The search filter ''{0}'' did not return any ldap entry");
    public static final M LDAP_QUERY_TOO_MANY_RESULTS = m(9028, Level.INFO, "The search filter ''{0}'' returned too many results (maximum {1})");
    public static final M LDAP_QUERY_MULTIVALUED_ATTR = m(9029, Level.WARNING, "Attribute ''{0}'' has multiple values");

    public static final M GATEWAYMANAGEMENT_ERROR = m(9050, Level.WARNING, "Error processing management request: {0}");

    public static final M JDBC_CANNOT_START_POOLING = m(9100, Level.WARNING, "JDBC Connection Pooling cannot start due to: {0}");
    public static final M JDBC_CANNOT_CONFIG_CONNECTION_POOL = m(9101, Level.WARNING, "Cannot configure a pool associated with a JDBC connection {0} due to: {1}");
    public static final M JDBC_CANNOT_DELETE_CONNECTION_POOL = m(9102, Level.WARNING, "Cannot delete a pool associated with a JDBC connection {0} due to: {1}");
    public static final M JDBC_CONNECTION_DISABLED = m(9103, Level.INFO, "The Gateway would not configure a disabled JDBC connection {0}");
    public static final M JDBC_QUERYING_FAILURE_ASSERTION_FAILED = m(9104, Level.WARNING, "\"Perform JDBC Query\" assertion failed due to: {0}");
    public static final M JDBC_NO_QUERY_RESULT_ASSERTION_FAILED = m(9105, Level.INFO, "\"Perform JDBC Query\" assertion failed due to no query results via a connection {0}");
    public static final M JDBC_DELETED_CONNECTION_POOL = m(9106, Level.FINE, "Removed connection pool associated with a JDBC connection {0}");
    public static final M JDBC_CONNECTION_POOL_NON_WHITE_LISTED_DRIVER = m(9107, Level.WARNING, "Using non white listed JDBC Driver Class ''{0}'' for JDBC Connection ''{1}''");

    public static final M JSON_SCHEMA_VALIDATION_FAILED = m(9130, Level.INFO, "JSON Schema validation failure. {0}");
    public static final M JSON_INVALID_JSON = m(9131, Level.INFO, "{0} is not well-formed JSON.");
    public static final M JSON_SCHEMA_VALIDATION_VALIDATING = m(9132, Level.FINEST, "Validating {0} against JSON Schema.");
    public static final M JSON_VALIDATION_SUCCEEDED = m(9133, Level.FINEST, "JSON Schema validation success.");
    public static final M JSON_SCHEMA_VALIDATION_IO_ERROR = m(9134, Level.WARNING, "Cannot validate JSON schema because JSON schema information cannot be retrieved: {0}");

    public static final M ENCODE_DECODE_ERROR = m(9160, Level.WARNING, "Error encoding or decoding: {0}.");
    public static final M ENCODE_DECODE_STRICT = m(9161, Level.WARNING, "Strict processing failed.");
    public static final M ENCODE_DECODE_OUT_TYPE = m(9162, Level.WARNING, "Variable of type ''{0}'' could not be created from decoded data: {1}.");
    public static final M ENCODE_DECODE_IN_TYPE = m(9163, Level.WARNING, "Variable of type ''{0}'' cannot be accessed as ''{1}'' for encoding or decoding.");
    public static final M ENCODE_DECODE_IN_ACCESS = m(9164, Level.WARNING, "Error accessing variable of type ''{0}'': {1}.");

    public static final M SAMLP_RESPONSE_BUILDER_GENERIC = m(9190, Level.WARNING, "Cannot {0} SAML Protocol Response: ''{1}''.");

    public static final M SAMLP_PROCREQ_PROFILE_VIOLATION = m(9230, Level.WARNING, "SAML 2.0 Web SSO profile rule violation: {0}.");
    public static final M SAMLP_PROCREQ_BINDING_ERROR = m(9231, Level.WARNING, "Cannot access AuthnRequest for binding ''{0}'': ''{1}''.");
    public static final M SAMLP_PROCREQ_INVALID_REQUEST = m(9232, Level.WARNING, "Invalid AuthnRequest: ''{0}''.");
    public static final M SAMLP_PROCREQ_SIGNING_ERROR = m(9233, Level.WARNING, "Signature validation failure: ''{0}''.");
    public static final M SAMLP_1_1_PROCREQ_PROFILE_VIOLATION = m(9234, Level.WARNING, "SAML 1.1 Web SSO profile rule violation: {0}.");

    public static final M ADD_WS_ADDRESSING_NO_SOAP_ACTION = m(9260, Level.WARNING, "Target message has no associated SOAPAction. Cannot automatically add the Action element.");
    public static final M ADD_WS_ADDRESSING_NO_ACTION_SUPPLIED = m(9261, Level.WARNING, "Action is a required WS-Addressing messaging property. No value found at runtime.");
    public static final M ADD_WS_ADDRESSING_NO_WSDL_ACTION_FOUND = m(9262, Level.WARNING, "Action extension element not found in the WSDL.");
    public static final M ADD_WS_ADDRESSING_INVALID_URI_VALUE_INFO = m(9263, Level.INFO, "Invalid URI value ''{0}'' for WS-Addressing ''{1}'' property.");
    public static final M ADD_WS_ADDRESSING_INVALID_URI_VALUE_WARN = m(9264, Level.WARNING, "Invalid URI value ''{0}'' for required WS-Addressing ''{1}'' property.");
    public static final M ADD_WS_ADDRESSING_INVALID_NAMESPACE = m(9265, Level.WARNING, "Invalid namespace: ''{0}''. {1}.");

    public static final M STS_INVALID_RST_REQUEST = m(9290, Level.WARNING, "Invalid RST SOAP Request: {0}");
    public static final M STS_INVALID_SECURITY_TOKEN = m(9291, Level.WARNING, "Invalid Security Token: {0}");
    public static final M STS_EXPIRED_SC_SESSION = m(9292, Level.WARNING, "Expired Secure Conversation Session: {0}");
    public static final M STS_AUTHENTICATION_FAILURE = m(9293, Level.WARNING, "Authentication Failure: {0}");
    public static final M STS_AUTHORIZATION_FAILURE = m(9294, Level.WARNING, "Authorization Failure: {0}");
    public static final M STS_TOKEN_ISSUE_ERROR = m(9295, Level.WARNING, "Unable to issue token: {0}");

    public static final M RST_BUILDER_ERROR = m(9330, Level.WARNING, "Error building RST: {0}");
    public static final M RST_BUILDER_OUTPUT = m(9331, Level.WARNING, "Unable to create RST message variable ''{0}''");

    public static final M RSTR_PROCESSOR_NOT_SOAP = m(9350, Level.WARNING, "{0} is not SOAP");
    public static final M RSTR_PROCESSOR_BAD_XML = m(9351, Level.WARNING, "{0} parse failure: {1}");
    public static final M RSTR_PROCESSOR_INVALID = m(9352, Level.WARNING, "Invalid response: {0}");
    public static final M RSTR_PROCESSOR_ENCRYPTION_ERROR = m(9353, Level.WARNING, "Error processing encrypted key: {0}");
    public static final M RSTR_PROCESSOR_UNEXPECTED_TOKEN = m(9354, Level.WARNING, "Expected token of type ''{0}'', but found: {1}");

    public static final M OUTBOUND_SECURE_CONVERSATION_LOOKUP_FAILURE = m(9380, Level.WARNING, "Session Lookup Failure: {0}");
    public static final M OUTBOUND_SECURE_CONVERSATION_ESTABLISHMENT_FAILURE = m(9381, Level.WARNING, "Outbound Secure Conversation Establishment Failure: {0}");

    public static final M CSRF_PROTECTION_REQUEST_NOT_HTTP = m(9400, Level.WARNING, "The request was not an HTTP request, failing assertion.");
    public static final M CSRF_PROTECTION_MULTIPLE_COOKIE_VALUES = m(9401, Level.WARNING, "Multiple cookie values were detected, failing assertion.");
    public static final M CSRF_PROTECTION_NO_COOKIE_VALUE = m(9402, Level.WARNING, "The expected cookie value was not found, failing assertion.");
    public static final M CSRF_PROTECTION_WRONG_REQUEST_TYPE = m(9403, Level.WARNING, "Looking for a {0} parameter, but the request was not a {0} request, failing assertion.");
    public static final M CSRF_PROTECTION_NO_PARAMETER = m(9404, Level.WARNING, "The expected parameter was not found, failing assertion.");
    public static final M CSRF_PROTECTION_MULTIPLE_PARAMETER_VALUES = m(9405, Level.WARNING, "The parameter had more than one value, failing assertion.");
    public static final M CSRF_PROTECTION_COOKIE_PARAMETER_MISMATCH = m(9406, Level.WARNING, "The parameter did not match the cookie value, failing assertion.");
    public static final M CSRF_PROTECTION_MISSING_REFERER = m(9407, Level.WARNING, "The HTTP-Referer header was not provided but it is required, failing assertion.");
    public static final M CSRF_PROTECTION_MULTIPLE_REFERERS = m(9408, Level.WARNING, "The HTTP-Referer header was provided multiple times, failing assertion.");
    public static final M CSRF_PROTECTION_INVALID_REFERER = m(9409, Level.WARNING, "The HTTP-Referer header value ''{0}'' was not valid, failing assertion.");

    public static final M API_AUTHORIZE_FAILED = m(9410, Level.WARNING, "Authorized failed: {0}");
    public static final M API_AUTHORIZE_FAILED_WITH_USAGE = m(9411, Level.WARNING, "Failed to authorize: usage will exceed for {0}");
    public static final M API_AUTHORIZE_FAILED_WITH_INVALID_USAGE = m(9412, Level.WARNING, "Failed to authorize: usage metric not found: {0}");
    public static final M API_REPORT_FAILED = m(9413, Level.WARNING, "Failed to report: {0}");

    public static final M JSON_TRANSFORMATION_FAILED = m(9420, Level.WARNING, "Failed to perform transformation.");

    public static final M SSH_CREDENTIAL_NOT_SSH = m(9430, Level.INFO, "Request not SSH; unable to extract SSH credentials.");
    public static final M SSH_CREDENTIAL_NO_AUTH = m(9431, Level.FINE, "Not authenticated.");
    public static final M SSH_CREDENTIAL_AUTH_USER = m(9432, Level.FINE, "Found credentials for user ''{0}''.");
    public static final M SSH_ROUTING_PASSTHRU_NO_USERNAME = m(9433, Level.WARNING, "No user name found for passing through to SSH server");
    public static final M SSH_ROUTING_ERROR = m(9434, Level.WARNING, "SSH routing error: {0}");
    public static final M SSH_ROUTING_INFO= m(9435, Level.FINE, "SSH routing: {0}");

    // ICAP modular assertion messages
    public static final M ICAP_INVALID_TIMEOUT = m(9445, Level.WARNING, "Invalid timeout value from timeout ({0}).  Timeout value must be a valid integer with range 1 to 3600 inclusive.");
    public static final M ICAP_INVALID_PORT = m(9446, Level.WARNING, "Invalid port specified, port must be between 1 and 63353: {0}.");
    public static final M ICAP_INVALID_URI = m(9447, Level.WARNING, "Invalid ICAP URI: {0}.");
    public static final M ICAP_CONNECTION_FAILED = m(9448, Level.WARNING, "Unable to connect to the specified server: {0}.");
    public static final M ICAP_IO_ERROR = m(9449, Level.WARNING, "I/O error occurred while scanning message {0}.");
    public static final M ICAP_MIME_ERROR = m(9450, Level.WARNING, "Error reading MIME content from {0} : {1}.");
    public static final M ICAP_SCAN_ERROR = m(9451, Level.WARNING, "Error occurred while scanning content {0} : {1}.");
    public static final M ICAP_SERVICE_UNAVAILABLE = m(9452, Level.WARNING, "Service not available {0}.");
    public static final M ICAP_NO_VALID_SERVER = m(9453, Level.WARNING, "No valid ICAP server entries found.");
    public static final M ICAP_VIRUS_DETECTED = m(9454, Level.WARNING, "Virus detected in {0} ({1}).");
    public static final M ICAP_VIRUS_RESPONSE_STATUS = m(9455, Level.WARNING, "ICAP Status: ({0}: {1}).");
    public static final M ICAP_VIRUS_RESPONSE_HEADERS = m(9456, Level.WARNING, "{0}");
    public static final M ICAP_NO_RESPONSE = m(9457, Level.WARNING, "No ICAP response received.");
    public static final M ICAP_UNSUPPORTED_ENCODING = m(9458, Level.WARNING, "Unsupported encoding: {0}.");

    public static final M SAMLP_ATTRIBUTE_QUERY_INVALID = m(9500, Level.INFO, "Invalid AttributeQuery: ''{0}''.");
    public static final M SAMLP_ATTRIBUTE_QUERY_NOT_SOAP_ENCAPSULATED = m(9501, Level.INFO, "AttributeQuery request is not SOAP encapsulated");
    public static final M SAMLP_ATTRIBUTE_QUERY_NOT_SUPPORTED_VALUE = m(9502, Level.INFO, "Unsupported value found for {0} in AttributeQuery. Found {1} expected one of {2}");
    public static final M SAMLP_ATTRIBUTE_QUERY_UNEXPECTED_DECRYPT_RESULTS = m(9503, Level.INFO, "Unexpected results after decrypting encrypted name identifier: {0}");
    public static final M SAMLP_ATTRIBUTE_QUERY_NO_DECRYPTION = m(9504, Level.FINE, "EncryptedID element found but not decrypted. Context variables related to Subject will have no values" );

    @Deprecated
    public static final Messages.M _UNUSED_CERT_LOOKUP_NAME = m(9550, Level.FINE, "Looking up certificate for name ''{0}''.");
    @Deprecated
    public static final Messages.M _UNUSED_CERT_LOOKUP_NOTFOUND = m(9551, Level.WARNING, "Certificate not found for name ''{0}''." );
    @Deprecated
    public static final Messages.M _UNUSED_CERT_LOOKUP_MULTIPLE = m(9552, Level.WARNING, "Multiple certificates found for name ''{0}''." );
    @Deprecated
    public static final Messages.M _UNUSED_CERT_LOOKUP_ERROR = m(9553, Level.WARNING, "Error looking up certificate ''{0}''." );
    public static final Messages.M CERT_ANY_LOOKUP_NAME = m(9554, Level.FINE, "Looking up certificate for {0} ''{1}''.");
    public static final Messages.M CERT_ANY_LOOKUP_NOTFOUND = m(9555, Level.WARNING, "Certificate not found for {0} ''{1}''." );
    public static final Messages.M CERT_ANY_LOOKUP_MULTIPLE = m(9556, Level.WARNING, "Multiple certificates found for {0} ''{1}''." );
    public static final Messages.M CERT_ANY_LOOKUP_ERROR = m(9557, Level.WARNING, "Error looking up certificate for {0} ''{1}'': {2}." );

    public static final M SAMLP_REQUEST_BUILDER_INVALID_URI = m(9580, Level.WARNING, "Invalid URI value found for {0}. Resolved value: ''{1}''. Reason: ''{2}''." );
    public static final M SAMLP_REQUEST_BUILDER_FAILED_TO_BUILD = m(9581, Level.WARNING, "Failed to build SAML Protocol Request: {0}" );

    public static final Messages.M MQ_ROUTING_REQUEST_ROUTED                = m(9610, Level.FINER, "Routing request to protected service");
    public static final Messages.M MQ_ROUTING_NO_RESPONSE                   = m(9611, Level.WARNING, "Did not receive a routing reply within the timeout period of {0}ms; empty response being returned");
    public static final Messages.M MQ_ROUTING_GOT_RESPONSE                  = m(9612, Level.FINER, "Received routing reply");
    public static final Messages.M MQ_ROUTING_NO_RESPONSE_EXPECTED          = m(9613, Level.INFO, "No response expected from protected service");
    public static final Messages.M MQ_ROUTING_REQUEST_WITH_NO_REPLY         = m(9614, Level.FINE, "Outbound request endpoint {0} specifies NO_REPLY");
    public static final Messages.M MQ_ROUTING_REQUEST_WITH_REPLY_TO_OTHER   = m(9615, Level.FINE, "Outbound request endpoint {0} specifies REPLY_TO_OTHER, setting replyToQueueName to {1}");
    public static final Messages.M MQ_ROUTING_CANT_CONNECT_RETRYING         = m(9616, Level.WARNING, "Failed to establish MQ connection on try #{0}.  Will retry after {1}ms.");
    public static final Messages.M MQ_ROUTING_CANT_CONNECT_NOMORETRIES      = m(9617, Level.WARNING, "Tried {0} times to establish MQ connection and failed.");
    public static final Messages.M MQ_ROUTING_REQUEST_WITH_AUTOMATIC        = m(9618, Level.FINE, "Outbound request endpoint {0} specifies AUTOMATIC, using temporary queue");
    public static final Messages.M MQ_ROUTING_REQUEST_TOO_LARGE             = m(9619, Level.WARNING, "Request message too large.");
    public static final Messages.M MQ_ROUTING_RESPONSE_TOO_LARGE            = m(9620, Level.WARNING, "Response message too large.");
    public static final Messages.M MQ_ROUTING_CONFIGURATION_ERROR           = m(9621, Level.WARNING, "Invalid MQ configuration ''{0}''.");
    public static final Messages.M MQ_ROUTING_TEMPLATE_ERROR                = m(9622, Level.WARNING, "Error processing MQ outbound template ''{0}''.");
    public static final Messages.M MQ_ROUTING_RESPONSE_TIMEOUT_ERROR        = m(9623, Level.WARNING, "Ignoring invalid response timeout: {0}");
    public static final Messages.M MQ_ROUTING_RESPONSE_TIMEOUT              = m(9624, Level.FINER, "Using response timeout {0}ms.");
    public static final Messages.M MQ_ROUTING_RESPONSE_SIZE_LIMIT_ERROR     = m(9625, Level.WARNING, "Ignoring invalid response size limit: {0}");
    public static final Messages.M MQ_ROUTING_RESPONSE_SIZE_LIMIT           = m(9626, Level.FINER, "Using response size limit {0} (bytes)");
    public static final Messages.M MQ_ROUTING_CANT_SET_MSG_DESCRIPTOR       = m(9627, Level.WARNING, "Ignoring invalid message descriptor field: {0}");
    public static final Messages.M MQ_ROUTING_CANT_SET_MSG_PROPERTY         = m(9628, Level.WARNING, "Ignoring invalid message property field: {0}");
    public static final Messages.M MQ_ROUTING_CANT_SET_MSG_HEADER         = m(9629, Level.WARNING, "Ignoring invalid message header field: {0}");
    public static final Messages.M MQ_ROUTING_WARNING_STATUS         = m(9630, Level.WARNING, "Routing completed with warning status. Reason code: {0}");

    public static final Messages.M GENERATE_HASH_VARIABLE_NOT_SET     = m(9635, Level.WARNING, "''{0}'' is not set.");
    public static final Messages.M GENERATE_HASH_UNSUPPORTED_ALGORITHM     = m(9636, Level.WARNING, "Unsupported Algorithm: ''{0}''");
    public static final Messages.M GENERATE_HASH_INVALID_KEY     = m(9637, Level.WARNING, "Invalid key: ''{0}''");
    public static final Messages.M GENERATE_HASH_ERROR     = m(9638, Level.WARNING, "Error generating hash signature.");

    public static final Messages.M EVALUATE_JSON_PATH_INVALID_JSON     = m(9645, Level.WARNING, "Source is not a valid JSON.");
    public static final Messages.M EVALUATE_JSON_PATH_INVALID_EXPRESSION     = m(9646, Level.WARNING, "Invalid JSON Path expression: ''{0}''");
    public static final Messages.M EVALUATE_JSON_PATH_INVALID_EVALUATOR     = m(9647, Level.WARNING, "Invalid evaluator: ''{0}''");
    public static final Messages.M EVALUATE_JSON_PATH_ERROR     = m(9648, Level.WARNING, "Error occurred evaluating JSON Path: ''{0}''");
    public static final Messages.M EVALUATE_JSON_PATH_NOT_FOUND     = m(9649, Level.WARNING, "Could not find any matching result; assertion therefore fails; Expression is ''{0}''.");

    public static final Messages.M LOOKUP_DYNAMIC_VARIABLE_NOT_FOUND     = m(9655, Level.INFO, "Context variable ''{0}'' is not found.");
    public static final Messages.M LOOKUP_DYNAMIC_VARIABLE_MISSING_SOURCE     = m(9656, Level.WARNING, "Source variable is not set.");
    public static final Messages.M LOOKUP_DYNAMIC_VARIABLE_MISSING_TARGET     = m(9657, Level.WARNING, "Target output variable is not set.");
    public static final Messages.M LOOKUP_DYNAMIC_VARIABLE_INVALID_SYNTAX     = m(9658, Level.WARNING, "Invalid variable syntax: {0}");
    public static final Messages.M LOOKUP_DYNAMIC_VARIABLE_UNSUPPORTED_TYPE     = m(9659, Level.WARNING, "''{0}'' is not a supported data type.");
    public static final Messages.M LOOKUP_DYNAMIC_VARIABLE_TYPE_MISMATCH     = m(9660, Level.WARNING, "Target data type is ''{0}'' but found ''{1}''.");

    // Set variable assertion
    public static final M SET_VARIABLE_UNABLE_TO_PARSE_DATE = m(9680, Level.INFO, "Unable to parse date string: {0}");
    public static final M SET_VARIABLE_UNRECOGNISED_DATE_FORMAT = m(9681, Level.INFO, "Date string format is not recognized: {0}");
    public static final M SET_VARIABLE_INVALID_DATE_OFFSET = m(9682, Level.INFO, "Invalid integer date offset: {0}");
    public static final M SET_VARIABLE_INVALID_DATE_PATTERN = m(9683, Level.WARNING, "Invalid date format pattern: {0}");
    public static final M SET_VARIABLE_UNABLE_TO_PARSE_INTEGER = m(9684, Level.INFO, "Unable to parse integer string: {0}");


    // Generate OAuth signature base string assertion
    public static final M OAUTH_DUPLICATE_PARAMETER = m(9670, Level.WARNING, "Found duplicate oauth parameter {0} with values {1}");
    public static final M OAUTH_INVALID_REQUEST_URL = m(9671, Level.WARNING, "Invalid request url: {0}");
    public static final M OAUTH_MISSING_HTTP_METHOD = m(9672, Level.WARNING, "Http method is empty");
    public static final M OAUTH_PARAMETERS = m(9673, Level.INFO, "OAuth parameters found: {0}");
    public static final M OAUTH_MISSING_PARAMETER = m(9674, Level.WARNING, "Required oauth parameter is missing or empty: {0}");
    public static final M OAUTH_INVALID_PARAMETER =  m(9675, Level.WARNING, "Value for {0} is invalid: {1}");
    public static final M OAUTH_INVALID_QUERY_PARAMETER =  m(9676, Level.WARNING, "Query parameter {0} is not allowed");
    public static final M OAUTH_INVALID_PARAMETERS =  m(9677, Level.WARNING, "Invalid oauth parameters: {0}");

    //Kerberos Authentication assertion
    public static final M KA_OPTION_NOT_SUPPORTED = m(9701, Level.WARNING, "{0} option does not support {1}");
    public static final M KA_KERBEROS_EXCEPTION = m(9702, Level.WARNING, "Unable to obtain Kerberos Service Ticket: {0}");
    public static final M KA_UNABLE_TO_FIND_PASSWORD = m(9703, Level.WARNING, "Unable to find stored gateway account password: {0}");
    public static final M KA_FAILED_TO_OBTAIN_KERBEROS_TICKET = m(9704, Level.WARNING, "Unable to obtain kerberos service ticket for service principal: ''{0}''");
    public static final M KA_LOGIN_CREDENTIALS_NOT_FOUND = m(9705, Level.WARNING, "Unable to find login credentials");
    public static final M KA_ADDED_KERBEROS_CREDENTIALS = m(9706, Level.FINE, "Added Kerberos Credentials to Authentication Context. Service Principal: ''{0}'', Client Principal: ''{1}''");
    public static final M KA_LOGIN_REALM_NOT_SUPPORT = m(9707, Level.WARNING, "Unable to handle Realm: {0}");
    public static final M KA_KERBEROS_KDC_EXCEPTION = m(9708, Level.WARNING, "Error message returned from KDC: {0}");

    //Adaptive Load Balancing Assertion
    public static final M ADAPTIVE_LOAD_BALANCING_FAIL = m(9721, Level.WARNING, "Service route failed, feedback: {0}");
    public static final M ADAPTIVE_LOAD_BALANCING_NO_ROUTE = m(9722, Level.WARNING, "Strategy {0} returned no route");
    public static final M ADAPTIVE_LOAD_BALANCING_VAR_NOT_FOUND = m(9723, Level.WARNING, "{0} variable not found in policy enforcement context");
    public static final M ADAPTIVE_LOAD_BALANCING_WRONG_VAR_TYPE = m(9724, Level.WARNING, "{0} variable is not the right type!");
    public static final M ADAPTIVE_LOAD_BALANCING_CRS_NO_ROUTES = m(9725, Level.WARNING, "Create Routing Strategy Assertion has no routes!");

    public static final M CACHE_LOOKUP_INVALID_MAX_AGE = m(9800, Level.WARNING, "Resolved maximum acceptable cache age value is invalid ''{0}''. Value must be in seconds between ''{1}'' and ''{2}'' inclusive");
    public static final M CACHE_LOOKUP_RETRIEVED = m(9801, Level.FINE, "Retrieved from cache: ''{0}''");
    public static final M CACHE_LOOKUP_MISS = m(9802, Level.FINE, "Cache miss with key: ''{0}''");

    public static final M CACHE_STORAGE_INVALID_VALUE = m(9900, Level.WARNING, "Invalid configuration value: {0}");
    public static final M CACHE_STORAGE_STORED = m(9901, Level.FINE, "Stored to cache: ''{0}''");

    public static final M OTK_INSTALLER_ERROR = m(9920, Level.WARNING, "OTK Installation Problem: {0}");
    public static final M OTK_DRY_RUN_CONFLICT = m(9921, Level.INFO, "Component {0} conflicts for {1}: {2}");
    public static final M POLICY_BUNDLE_INSTALLER_ERROR = m(9922, Level.WARNING, "{0} Installation Problem: {1}"); // {0}: installer type;  {1}: problem detail
    public static final M POLICY_BUNDLE_INSTALLER_DRY_RUN_CONFLICT = m(9923, Level.INFO, "Component {0} conflicts for {1}: {2}");

    // ValidateCertificate assertion
    public static final M CERT_NOT_FOUND = m(10000, Level.WARNING, "No certificate found for variable: {0}");
    public static final M CERT_VALIDATION_STATUS_FAILURE = m(10001, Level.WARNING, "Certificate {0} validation ({1}) failed with status: {2}");
    public static final M CERT_VALIDATION_FAILURE = m(10002, Level.WARNING, "Certificate {0} validation ({1}) failed: {2}");

    // Audit Detail Assertion
    public static final M CUSTOM_LOGGER_NAME_FALLBACK_DUE_TO_VARIABLES_NOT_EXIST = m(10020, Level.WARNING, "The custom logger name uses non-existing context variables: {0}.  The custom logger name now falls back to the default package name, {1}.");
    public static final M CUSTOM_LOGGER_NAME_FALLBACK_DUE_TO_INVALID_PACKAGE_NAME = m(10021, Level.WARNING, "The custom logger name contains invalid package name derived from context variable(s): {0}. The custom logger name now falls back to the default package name, {1}.");

    // Encapsulated Assertion
    public static final M ENCASS_INVALID_BACKING_POLICY = m(10050, Level.WARNING, "Underlying policy fragment is invalid for Encapsulated Assertion: {0}");

    //SiteMinder Assertion
    public static final M SINGLE_SIGN_ON_ERROR = m(10100, Level.WARNING, "{0} assertion: CA Single Sign-On Policy Server Error: {1}");
    public static final M SINGLE_SIGN_ON_FINE = m(10101, Level.FINE, "CA Single Sign-On {0} assertion: {1}");
    public static final M SINGLE_SIGN_ON_WARNING = m(10102, Level.WARNING, "CA Single Sign-On {0} assertion: {1}");

    //Radius Assertion
    public static final M RADIUS_AUTH_ERROR = m(10200, Level.WARNING, "Radius Server Error: {0}");
    public static final M RADIUS_AUTH_NO_CREDENTIAL = m(10201, Level.INFO, "No credentials found!");
    public static final M RADIUS_AUTH_AUTHENTICATION_FAILED = m(10202, Level.FINE, "Authentication Against Radius Server failed for credentials: {0}");

    // Manage Transport Properties/Headers Assertion
    public static final M HEADER_ADDED = m(10350, Level.FINE, "Added header/property with name {0} and value {1}");
    public static final M HEADER_REMOVED_BY_NAME = m(10351, Level.FINE, "Removed header/property with name {0}");
    public static final M HEADER_REMOVED_BY_NAME_AND_VALUE = m(10352, Level.FINE, "Removed header/property with name {0} and value {1}");
    public static final M EMPTY_HEADER_NAME = m(10353, Level.WARNING, "Header/property name is empty");

    // Add or Remove Cookie Assertion
    public static final M INVALID_COOKIE_MAX_AGE = m(10400, Level.WARNING, "Cookie max age is invalid: {0}");
    public static final M EMPTY_COOKIE_NAME = m(10401, Level.WARNING, "Cookie name is null or empty");
    public static final M COOKIES_NOT_MATCHED = m(10402, Level.FINE, "No cookies matched {0}");
    public static final M COOKIE_ALREADY_EXISTS = m(10403, Level.WARNING, "A cookie with name {0}, domain {1} and path {2} already exists");
    public static final M COOKIE_ADDED = m(10404, Level.FINE, "Added cookie with name {0} and value {1}");
    public static final M COOKIE_REMOVED = m(10405, Level.FINE, "Removed cookie with name {0} and value {1}");
    public static final M INVALID_COOKIE_VERSION = m(10406, Level.WARNING, "Cookie version is invalid: {0}");
    public static final M EMPTY_COOKIE_ATTRIBUTE = m(10407, Level.WARNING, "Cookie {0} is null or empty");

    // Replace Tag Content Assertion
    public static final M EMPTY_SEARCH_TEXT = m(10450, Level.WARNING, "Text to search is empty");
    public static final M EMPTY_TAGS_TEXT = m(10451, Level.WARNING, "Tags to search is empty");
    public static final M EMPTY_TAG_TEXT = m(10452, Level.WARNING, "Ignoring empty tag");
    public static final M TAG_NOT_FOUND = m(10453, Level.FINE, "Tag not found: {0}");
    public static final M NO_REPLACEMENTS = m(10454, Level.FINE, "No replacements performed");

    // Protect Against JSON Document Structure Threats Assertion
    public static final M JSON_THREAT_PROTECTION_TARGET_NOT_JSON = m(10500, Level.WARNING, "{0} is not JSON.");
    public static final M JSON_THREAT_PROTECTION_TARGET_INVALID_JSON = m(10501, Level.WARNING, "{0} is not well-formed JSON.");
    public static final M JSON_THREAT_PROTECTION_CONTAINER_DEPTH_EXCEEDED = m(10502, Level.WARNING, "Container depth constraint violated at line {0}.");
    public static final M JSON_THREAT_PROTECTION_OBJECT_ENTRY_COUNT_EXCEEDED = m(10503, Level.WARNING, "Object entry count constraint violated at line {0}.");
    public static final M JSON_THREAT_PROTECTION_ARRAY_ENTRY_COUNT_EXCEEDED = m(10504, Level.WARNING, "Array entry count constraint violated at line {0}.");
    public static final M JSON_THREAT_PROTECTION_ENTRY_NAME_LENGTH_EXCEEDED = m(10505, Level.WARNING, "Entry name length constraint violated at line {0}.");
    public static final M JSON_THREAT_PROTECTION_STRING_VALUE_LENGTH_EXCEEDED = m(10506, Level.WARNING, "String value length constraint violated at line {0}.");

    // OData Validation Assertion
    public static final M ODATA_VALIDATION_INVALID_SMD = m(10600, Level.WARNING, "The specified Service Metadata Document is invalid: {0}");
    public static final M ODATA_VALIDATION_INVALID_URI = m(10601, Level.WARNING, "Could not parse OData resource path: {0}");
    public static final M ODATA_VALIDATION_TARGET_INVALID_PAYLOAD = m(10602, Level.WARNING, "{0} payload could not be parsed: {1}");
    public static final M ODATA_VALIDATION_REQUEST_MADE_FOR_SMD = m(10603, Level.WARNING, "Request for Service Metadata Document attempted.");
    public static final M ODATA_VALIDATION_REQUEST_MADE_FOR_RAW_VALUE = m(10604, Level.WARNING, "Request for raw value attempted: {0}.");
    public static final M ODATA_VALIDATION_FORBIDDEN_OPERATION_ATTEMPTED = m(10605, Level.WARNING, "OData request attempted using forbidden operation ''{0}''.");
    public static final M ODATA_VALIDATION_EXPRESSION_ERROR = m(10606, Level.WARNING, "Unable to parse {0} expression: {1}");
    public static final M ODATA_VALIDATION_MESSAGE_NOT_HTTP_REQUEST = m(10607, Level.WARNING, "{0} is not an HTTP request; cannot automatically determine HTTP method.");
    public static final M ODATA_VALIDATION_INVALID_HTTP_METHOD = m(10608, Level.WARNING, "Invalid OData HTTP method: ''{0}''");
    public static final M ODATA_VALIDATION_EMPTY_HTTP_METHOD = m(10609, Level.WARNING, "HTTP method is null or empty.");

    // Retrieve Service WSDL Assertion
    public static final M RETRIEVE_WSDL_INVALID_SERVICE_ID = m(10700, Level.WARNING, "Invalid Service ID: {0}");
    public static final M RETRIEVE_WSDL_SERVICE_NOT_FOUND = m(10701, Level.WARNING, "Could not find service with ID: {0}");
    public static final M RETRIEVE_WSDL_SERVICE_NOT_SOAP = m(10702, Level.WARNING, "Service is not SOAP.");
    public static final M RETRIEVE_WSDL_ERROR_PARSING_WSDL = m(10703, Level.WARNING, "Could not parse WSDL XML: {0}");
    public static final M RETRIEVE_WSDL_NO_PROTOCOL = m(10704, Level.WARNING, "No protocol specified.");
    public static final M RETRIEVE_WSDL_INVALID_PORT = m(10705, Level.WARNING, "Invalid port: {0}");
    public static final M RETRIEVE_WSDL_NO_PORT = m(10706, Level.WARNING, "No port specified.");
    public static final M RETRIEVE_WSDL_INVALID_ENDPOINT_URL = m(10707, Level.WARNING, "Invalid endpoint URL: {0}");
    public static final M RETRIEVE_WSDL_PROXY_URL_CREATION_FAILURE = m(10708, Level.WARNING, "Failed to create proxy URL for reference: {0}");
    public static final M RETRIEVE_WSDL_NO_HOSTNAME = m(10709, Level.WARNING, "No host name specified.");
    public static final M RETRIEVE_WSDL_NO_SERVICE_DOCUMENT_ID = m(10710, Level.WARNING, "No Service Document ID specified.");
    public static final M RETRIEVE_WSDL_PROXYING_DISABLED_FOR_DEPENDENCY = m(10711, Level.WARNING, "Dependency reference proxying must be enabled in order to retrieve Service Documents.");
    public static final M RETRIEVE_WSDL_INVALID_SERVICE_DOCUMENT_ID = m(10712, Level.WARNING, "Invalid Service Document ID: {0}");
    public static final M RETRIEVE_WSDL_SERVICE_DOCUMENT_NOT_FOUND = m(10713, Level.WARNING, "Could not find Service Document with ID: {0}");
    public static final M RETRIEVE_WSDL_ERROR_PARSING_SERVICE_DOCUMENT = m(10714, Level.WARNING, "Could not parse Service Document XML: {0}");
    public static final M RETRIEVE_WSDL_SERVICE_ID_BLANK = m(10715, Level.WARNING, "Service ID is empty.");
    public static final M RETRIEVE_WSDL_SERVICE_DOCUMENT_ID_BLANK = m(10716, Level.WARNING, "Service Document ID is empty.");

    public static final M JWT_MISSING_SOURCE_PAYLOAD = m(10800, Level.WARNING, "Source payload is required.");
    public static final M JWT_MISSING_HEADERS = m(10801, Level.WARNING, "Assertion was instructed to {0} headers, but no headers were found.");
    public static final M JWT_PRIVATE_KEY_NOT_FOUND = m(10802, Level.WARNING, "Could not find the specified private key.");
    public static final M JWT_KEYSTORE_ERROR = m(10803, Level.WARNING, "Could not access keystore to find the specified key.");
    public static final M JWT_KEY_RECOVERY_ERROR = m(10804, Level.WARNING, "Could not recover private key.");
    public static final M JWT_JOSE_ERROR = m(10805, Level.WARNING, "JOSE Error: {0}");
    public static final M JWT_SOURCE_HEADERS_ERROR = m(10806, Level.WARNING, "Invalid header found: {0}");
    public static final M JWT_INVALID_KEY = m(10807, Level.WARNING, "Invalid Key: The given key {0} is not valid for use with {1}.");

    public static final M JWT_INVALID_KEY_USAGE = m(10808, Level.WARNING, "{0}");

    public static final M JWT_MISSING_JWS_HMAC_SECRET = m(10810, Level.WARNING, "Key is required when using creating MAC.");
    public static final M JWT_MISSING_JWS_PRIVATE_KEY = m(10811, Level.WARNING, "Private Key is required when using creating a digitial signature.");
    public static final M JWT_MISSING_JWS_KID = m(10812, Level.WARNING, "Key ID is required for retrieval of key from a JSON Web Key Set.");
    public static final M JWT_JWS_KEY_ERROR = m(10813, Level.WARNING, "Could not find signing key for JWS operation.");

    public static final M JWT_JWE_KEY_ERROR = m(10820, Level.WARNING, "Could not find encryption key for JWE operation.");
    public static final M JWT_JWE_PUBLIC_KEY_ERROR = m(10821, Level.WARNING, "Could not get the public key as specified.");

    public static final M JWT_DECODE_ERROR = m(10830, Level.WARNING, "Source payload is neither JWS or JWE.");
    public static final M JWT_DECODE_INVALID_TYPE = m(10831, Level.WARNING, "Can not decode JWE using a secret, please reconfigure the assertion to use a Recipient key or JWK/JWKS.");
    public static final M JWT_INVALID_ALGORITHM = m(10832, Level.WARNING, "Invalid algorithm: {0}");
    public static final M JWT_DECODE_MISSING_SECRET = m(10833, Level.WARNING, "Missing secret to verify JWS signature.");
    public static final M JWT_GENERAL_DECODE_ERROR = m(10834, Level.WARNING, "Error decoding: {0}");

    public static final M JWT_JWK_ERROR = m(10840, Level.WARNING, "Error creating JWKS: {0}");
    public static final M JWT_JWK_NOT_FOUND = m(10841, Level.WARNING, "Could not find key with the alias of {0}");

    // CassandraQuery Assertion
    public static final M CASSANDRA_CONNECTION_CANNOT_CONNECT = m(10900, Level.WARNING, "Connection to Cassandra cluster associated with connection {0} not available due to {1}");
    public static final M CASSANDRA_CONNECTION_MANAGER_FINE_MESSAGE = m(10901, Level.FINE, "Cassandra Connection Manager: {0}");
    public static final M CASSANDRA_CONNECTION_DISABLED = m(10902, Level.INFO, "The Gateway would not connect via disabled Cassandra connection {0}");
    public static final M CASSANDRA_QUERYING_FAILURE_ASSERTION_FAILED = m(10903, Level.WARNING, "\"Perform Cassandra Query\" assertion failed due to: {0}");
    public static final M CASSANDRA_NO_QUERY_RESULT_ASSERTION_FAILED = m(10904, Level.WARNING, "\"Perform Cassandra Query\" assertion failed due to no query results via a connection {0}");
    public static final M CASSANDRA_CANNOT_REMOVE_CONNECTION = m(10905, Level.WARNING, "Unable to remove Cassandra connection due to: {0}");

    public static final M HANDLE_ERRORS_MSG = m(11000, Level.WARNING, "Policy processing caught an exception: {0}");

    // InvokePolicyAsync Assertion
    public static final M WORK_QUEUE_EXECUTOR_NOT_AVAIL = m(11100, Level.WARNING, "Executor associated with work queue {0} not available due to {1}");
    public static final M WORK_QUEUE_EXECUTOR_FINE = m(11101, Level.FINE, "Work Queue Executor Manager: {0}");

    // Highest ID reserved for AssertionMessages = 99999
}

