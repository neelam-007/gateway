package com.l7tech.common.audit;

import java.util.logging.Level;

/**
 * Message catalog for messages audited by policy assertions.
 * The ID range 4000-7999 inclusive is reserved for these messages.
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 */
public class AssertionMessages extends Messages {

    // ServerHttpRoutingAssertion messages
    public static final M HTTPROUTE_SSL_INIT_FAILED         = m(4000, Level.WARNING, "Could not initialize SSL Context");
    public static final M HTTPROUTE_BEGIN                   = m(4001, Level.INFO, "Processing HTTP(S) Routing assertion");
    public static final M HTTPROUTE_NON_SOAP_WRONG_FORMAT   = m(4002, Level.WARNING, true, false, "SOAP message expected but not found; requested option not supported by non-SOAP messages");
    public static final M HTTPROUTE_NON_SOAP_WRONG_POLICY   = m(4003, Level.WARNING, "Option not supported by non-SOAP messages; check policy for errors");
    public static final M HTTPROUTE_PROMOTING_ACTOR         = m(4004, Level.FINE, "Promoting actor {0}");
    public static final M HTTPROUTE_NO_SECURITY_HEADER      = m(4005, Level.INFO, "Routing assertion requested promotion of security header with actor {0}, but no such header found in message");
    public static final M HTTPROUTE_ERROR_READING_RESPONSE  = m(4006, Level.WARNING, true, false, "Error reading response");
    public static final M HTTPROUTE_CANT_RESOLVE_IP         = m(4007, Level.WARNING, "Could not resolve client IP address");
    public static final M HTTPROUTE_TAI_NOT_AUTHENTICATED   = m(4008, Level.FINE, "TAI credential chaining requested, but request was not authenticated");
    public static final M HTTPROUTE_TAI_CHAIN_USERNAME      = m(4009, Level.FINE, "TAI credential chaining requested; will chain username {0}");
    public static final M HTTPROUTE_TAI_NO_USER_ID          = m(4010, Level.WARNING, "TAI credential chaining requested, but requesting user does not have a unique identifier: ID is {0}");
    public static final M HTTPROUTE_TAI_CHAIN_LOGIN         = m(4011, Level.FINE, "TAI credential chaining requested, but there is no user; will chain pc.login {0}");
    public static final M HTTPROUTE_TAI_NO_USER             = m(4012, Level.WARNING, "TAI credential chaining requested, and request was authenticated, but had no user or pc.login");
    public static final M HTTPROUTE_ADD_OUTGOING_COOKIE     = m(4013, Level.FINE, "Adding outgoing cookie: name = {0}");
    public static final M HTTPROUTE_LOGIN_INFO              = m(4014, Level.FINE, "Using login ''{0}''");
    public static final M HTTPROUTE_OK                      = m(4015, Level.FINE, "Request routed successfully");
    public static final M HTTPROUTE_RESPONSE_STATUS         = m(4016, Level.WARNING, true, true, "Protected service ({0}) responded with status {1}");
    public static final M HTTPROUTE_ADDCOOKIE_VERSION       = m(4017, Level.FINE, "Adding outgoing cookie: name = {0}, version = {1}");
    public static final M HTTPROUTE_UPDATECOOKIE            = m(4018, Level.FINE,  "Updating cookie: name = {0}");
    public static final M BRIDGEROUTE_NO_ATTACHMENTS        = m(4019, Level.WARNING, "SecureSpan Bridge Routing assertion does not currently support SOAP with attachments; ignoring additional MIME parts");
    public static final M BRIDGEROUTE_BAD_CONFIG            = m(4020, Level.WARNING, "SecureSpan Bridge Routing assertion is configured with invalid protected service URL or policy XML");
    public static final M HTTPROUTE_BAD_ORIGINAL_URL        = m(4021, Level.WARNING, "Invalid original request URI -- using default");
    public static final M HTTPROUTE_ACCESS_DENIED           = m(4022, Level.WARNING, "Protected service has denied access based on credentials from the SecureSpan Bridge Routing assertion");
    public static final M HTTPROUTE_TOO_MANY_ATTEMPTS       = m(4023, Level.WARNING, "Unable to route to the service after multiple failed attempts");
    public static final M HTTPROUTE_SAML_SV_NOT_AUTH        = m(4024, Level.WARNING, "SAML Sender-Vouches forwarding requested, but request was not authenticated");
    public static final M HTTPROUTE_RESPONSE_STATUS_HANDLED = m(4025, Level.INFO, "Protected service ({0}) responded with status {1}; retrying");
    public static final M HTTPROUTE_BAD_STRATEGY_NAME       = m(4026, Level.WARNING, "Invalid routing failover strategy name: {0}; using default strategy");
    public static final M HTTPROUTE_FAILOVER_FROM_TO        = m(4027, Level.WARNING, "Routing failed to host = {0}, retrying to host = {1}");
    public static final M HTTP_UNKNOWN_HOST                 = m(4028, Level.WARNING, "Routing failed, unable to resolve IP for host = {0}");
    public static final M HTTP_SOCKET_EXCEPTION             = m(4029, Level.WARNING, "Routing failed, connection error: {0}");
    public static final M HTTPROUTE_PASSTHROUGH_REQUEST     = m(4030, Level.INFO, "Passthrough selected; adding request credentials to routed request");
    public static final M HTTPROUTE_PASSTHROUGH_REQUEST_NC  = m(4031, Level.FINE, "Passthrough selected but no credentials in SSG request to pass along");
    public static final M HTTPROUTE_PASSTHROUGH_RESPONSE    = m(4032, Level.INFO, "Passthrough selected; adding challenge to SSG response");
    public static final M HTTPROUTE_PASSTHROUGH_RESPONSE_NC = m(4033, Level.FINE, "Passthrough selected but no challenge in routed response");
    public static final M HTTPROUTE_RESPONSE_NOCONTENTTYPE  = m(4034, Level.WARNING, "Downstream service returned status ({0}) but is missing a content type header.");
    public static final M HTTPROUTE_RESPONSE_NOXML          = m(4035, Level.WARNING, "Downstream service returned status ({0}) with non-XML payload.");
    public static final M HTTPROUTE_INVALIDCOOKIE           = m(4036, Level.INFO, "Ignoring invalid cookie header ''{0}''");
    public static final M HTTPROUTE_RESPONSE_CHALLENGE      = m(4037, Level.INFO, "Protected service requires authentication.");
    public static final M HTTPROUTE_RESPONSE_BADSTATUS      = m(4038, Level.INFO, "Downstream service returned status ({0}). This is considered a failure case.");
    public static final M HTTPROUTE_CTYPEWOUTPAYLOAD        = m(4039, Level.INFO, "Downstream service returned an empty response but still included a content-type of ({0}).");
    public static final M BRIDGEROUTE_REQUEST_NOT_SOAP      = m(4040, Level.WARNING, "Bridge routing failed because request is not SOAP.  Bridge Routing Assertion does not currently support non-SOAP requests.");
    public static final M HTTPROUTE_SOCKET_TIMEOUT          = m(4041, Level.WARNING, "Remote network connection timed out.");
    public static final M GENERIC_ROUTING_PROBLEM           = m(4042, Level.WARNING, "Problem routing to {0}. Error msg: {1}");

    // ServerCredentialSourceAssertion messages
    public static final M AUTH_REQUIRED = m(4100, Level.INFO, "Authentication required");

    // ServerIdentityAssertion
    public static final M AUTHENTICATED_BUT_CREDENTIALS_NOT_FOUND = m(4200, Level.WARNING, "Request is authenticated but request has no login credentials!");
    public static final M CREDENTIALS_NOT_FOUND                   = m(4201, Level.WARNING, "No credentials found!");
    public static final M ALREADY_AUTHENTICATED                   = m(4202, Level.FINEST, "Request already authenticated");
    public static final M ID_PROVIDER_ID_NOT_SET                  = m(4203, Level.WARNING, "Cannot call checkRequest() when no valid identity provider OID has been set!");
    public static final M ID_PROVIDER_NOT_FOUND                   = m(4204, Level.WARNING, "Could not find identity provider!");
    public static final M ID_PROVIDER_NOT_EXIST                   = m(4205, Level.WARNING, "Identity assertion refers to a non-existent identity provider");
    public static final M AUTHENTICATED                           = m(4206, Level.FINE, "Authentication success {0}");
    public static final M INVALID_CERT                            = m(4207, Level.INFO, "Invalid client certificate for {0}");
    public static final M AUTHENTICATION_FAILED                   = m(4208, Level.INFO, "Authentication failed for {0}");
    public static final M IDASS_NOLOGIN_NOOID                     = m(4209, Level.WARNING, "Assertion not configure properly: both login and UID are null");
    public static final M IDPROV_MISMATCH                         = m(4210, Level.FINE, "Authentication failed because ID of provider did not match ({0} instead of {1})");
    public static final M USERID_MISMATCH                         = m(4211, Level.FINE, "Authentication failed because the user ID did not match");
    public static final M LOGIN_MISMATCH                          = m(4212, Level.FINE, "Authentication failed because the login did not match");
    public static final M GROUP_NOTEXIST                          = m(4213, Level.WARNING, "Assertions refer to a nonexistent group; policy may be corrupted");
    public static final M USER_NOT_IN_GROUP                       = m(4214, Level.FINE, "User not member of group");
    public static final M CACHED_GROUP_MEMBERSHIP_FAILURE         = m(4215, Level.FINE, "Reusing cached group membership failure");

    // ServerRequestWssOperation messages
    public static final M REQUESTWSS_NOT_FOR_US  = m(4300, Level.FINE, "Intended for another recipient; nothing to validate");
    public static final M REQUESTWSS_NONSOAP     = m(4301, Level.INFO, "Request not SOAP; cannot verify WS-Security contents");
    public static final M REQUESTWSS_NO_SECURITY = m(4302, Level.INFO, "Request did not contain any WSS level security");

    // ServerRequestSwAAssertion messages
    public static final M SWA_NOT_SOAP                    = m(4400, Level.WARNING, "Request not SOAP; cannot validate attachments");
    public static final M SWA_NOT_MULTIPART               = m(4401, Level.INFO, "The request does not contain attachment or is not a multipart message");
    public static final M SWA_OPERATION_NOT_FOUND         = m(4402, Level.FINEST, "Operation not found in the request: XPath expression is: {0}");
    public static final M SWA_REPEATED_OPERATION          = m(4403, Level.INFO, "Same operation appears more than once in the request: XPath expression is: {0}");
    public static final M SWA_OPERATION_NOT_ELEMENT_NODE  = m(4404, Level.INFO, "XPath pattern {0} found non-element node ''{1}''");
    public static final M SWA_PARAMETER_NOT_ELEMENT_NODE  = m(4405, Level.INFO, "XPath pattern {0}/{1} found non-element node ''{2}''");
    public static final M SWA_OPERATION_FOUND             = m(4406, Level.FINEST, "The operation {0} is found in the request");
    public static final M SWA_PART_NOT_FOUND              = m(4407, Level.FINE, "MIME part not found in the request: XPath expression is: {0}/{1})");
    public static final M SWA_REPEATED_MIME_PART          = m(4408, Level.FINE, "Same MIME part appears more than once in the request: XPath expression is: {0}/{1}");
    public static final M SWA_PARAMETER_FOUND             = m(4409, Level.FINEST, "Parameter {0} is found in the request");
    public static final M SWA_REFERENCE_NOT_FOUND         = m(4410, Level.INFO, "The reference (href) of the {0} is found in the request");
    public static final M SWA_REFERENCE_FOUND             = m(4411, Level.FINEST, "The href of the parameter {0} is found in the request, value={1}");
    public static final M SWA_INVALID_CONTENT_ID_URL      = m(4412, Level.INFO, "Invalid Content-ID URL {0}");
    public static final M SWA_NOT_IN_CONTENT_TYPES        = m(4413, Level.INFO, "The content type of the attachment {0} must be one of the types: {1}");
    public static final M SWA_BAD_CONTENT_TYPE            = m(4414, Level.INFO, "The content type of the attachment {0} must be: {1}");
    public static final M SWA_TOTAL_LENGTH_LIMIT_EXCEEDED = m(4415, Level.INFO, "The parameter [{0}] has {1} attachments: The total length exceeds the limit: {2} K bytes");
    public static final M SWA_PART_LENGTH_LIMIT_EXCEEDED  = m(4416, Level.INFO, "The length of the attachment {0} exceeds the limit: {1} K bytes");
    public static final M SWA_NO_ATTACHMENT               = m(4417, Level.INFO, "The required attachment {0} is not found in the request");
    public static final M SWA_UNEXPECTED_ATTACHMENT       = m(4418, Level.INFO, "Unexpected attachment {0} found in the request");
    public static final M SWA_INVALID_OPERATION           = m(4419, Level.INFO, "The operation specified in the request is invalid");
    public static final M SWA_INVALID_XML                 = m(4420, Level.WARNING, "Error parsing request, detail is ''{0}''.");
    public static final M SWA_EXTRA_ATTACHMENT            = m(4421, Level.INFO, "Passing extra attachment {0}.");
    public static final M SWA_EXTRA_LENGTH_EXCEEDED       = m(4422, Level.INFO, "Maximum length of extra attachments exceeds the limit {0} K bytes.");
    public static final M SWA_EXTRA_ATTACHMENT_DROPPED    = m(4423, Level.INFO, "Dropping extra attachment {0}.");
    public static final M SWA_NOT_SIGNED                  = m(4424, Level.WARNING, "Missing required signature for part ''{0}'', for attachment with Content-ID URL ''{1}''");

    // ServerRemoteIpRange messages
    public static final M IP_NOT_TCP             = m(4500, Level.INFO,    "Request was not received via TCP; cannot validate remote IP address");
    public static final M IP_ADDRESS_INVALID     = m(4501, Level.INFO,    "The remote address {0} is null or not in expected format");
    public static final M IP_ACCEPTED            = m(4502, Level.FINEST,  "Requestor address {0} is accepted");
    public static final M IP_REJECTED            = m(4503, Level.INFO,    "Requestor address {0} is not allowed");
    public static final M IP_ADDRESS_UNAVAILABLE = m(4504, Level.WARNING, "Could not resolve a remote IP address from the context variable {0}.");

    // ServerSecureConversation messages
    public static final M SC_REQUEST_NOT_SOAP                     = m(4600, Level.INFO, "Request not SOAP; unable to check for WS-SecureConversation token");
    public static final M SC_NO_WSS_LEVEL_SECURITY                = m(4601, Level.INFO, "This request did not contain any WSS level security");
    public static final M SC_NO_PROOF_OF_POSSESSION               = m(4602, Level.FINE, "Ignoring SecurityContextToken with no proof-of-possession");
    public static final M SC_TOKEN_INVALID                        = m(4603, Level.WARNING, "Request referred to a SecureConversation token unrecognized on this server; possible expired session - returning AUTH_FAILED");
    public static final M SC_SESSION_FOR_USER                     = m(4604, Level.FINE, "Secure Conversation session recognized for user {0}");
    public static final M SC_REQUEST_NOT_REFER_TO_SC_TOKEN        = m(4605, Level.INFO, "This request did not seem to refer to a Secure Conversation token");
    public static final M SC_UNABLE_TO_ATTACH_SC_TOKEN            = m(4606, Level.WARNING, false, true, "Response not SOAP; unable to attach WS-SecureConversation token");

    // ServerRequestXpathAssertion & ServerResponseXpathAssertion messages
    public static final M XPATH_REQUEST_NOT_XML                   = m(4700, Level.WARNING, true, false, "Request not XML; cannot evaluate XPath expression");
    public static final M XPATH_RESPONSE_NOT_XML                  = m(4701, Level.WARNING, false, true, "Response not XML; cannot evaluate XPath expression");
    public static final M XPATH_PATTERN_INVALID                   = m(4702, Level.WARNING, "Assertion has failed because the XPath pattern is null or empty");
    public static final M XPATH_PATTERN_NOT_MATCHED_REQUEST       = m(4703, Level.INFO, "Assertion has failed because the XPath pattern did not match request" );
    public static final M XPATH_PATTERN_NOT_MATCHED_RESPONSE      = m(4704, Level.INFO, "Assertion has failed because the XPath pattern did not match response" );
    public static final M XPATH_RESULT_TRUE                       = m(4705, Level.FINE, "XPath pattern returned true");
    public static final M XPATH_RESULT_FALSE                      = m(4706, Level.INFO, "XPath pattern returned false");
    public static final M XPATH_TEXT_NODE_FOUND                   = m(4707, Level.FINE, "XPath pattern found a text node");
    public static final M XPATH_ELEMENT_FOUND                     = m(4708, Level.FINE, "XPath pattern found an element");
    public static final M XPATH_OTHER_NODE_FOUND                  = m(4709, Level.FINE, "XPath pattern found some other node");
    public static final M XPATH_SUCCEED_REQUEST                   = m(4710, Level.FINE, "XPath pattern matched request; assertion succeeds");
    public static final M XPATH_SUCCEED_RESPONSE                  = m(4711, Level.FINE, "XPath pattern matched response; assertion succeeds");
    public static final M XPATH_MULTIPLE_RESULTS                  = m(4712, Level.FINE, "XPath pattern found {0} results; .result variable will contain first value");
    public static final M XPATH_RESULTS                           = m(4713, Level.FINE, "XPath result #{0}: \"{1}\"");
    public static final M XPATH_PATTERN_INVALID_MORE_INFO         = m(4714, Level.WARNING, "Cannot evaluate XPath expression: XPath pattern is invalid ''{0}''.");
    public static final M XPATH_PATTERN_NOT_MATCHED_REQUEST_MI    = m(4715, Level.INFO, "XPath pattern didn''t match request; assertion therefore fails; XPath is ''{0}''." );
    public static final M XPATH_PATTERN_NOT_MATCHED_RESPONSE_MI   = m(4716, Level.INFO, "XPath pattern didn''t match response; assertion therefore fails; XPath is ''{0}''." );

    // ServerRequestAcceleratedXpathAssertion & ServerResponseAcceleratedXpathAssertion messages
    public static final M ACCEL_XPATH_NO_HARDWARE                 = m(4750, Level.INFO, "Hardware acceleration not available; falling back to software XPath processing");
    public static final M ACCEL_XPATH_UNSUPPORTED_PATTERN         = m(4751, Level.INFO, "Hardware acceleration not available for this XPath expression; falling back to software XPath processing");
    public static final M ACCEL_XPATH_NO_CONTEXT                  = m(4752, Level.WARNING, "Message has no hardware acceleration context; falling back to software XPath processing");

    // ServerRequestWssX509Cert messages
    public static final M REQUEST_WSS_X509_FOR_ANOTHER_USER       = m(4800, Level.FINE, "This is intended for another recipient; there is nothing to validate");
    public static final M REQUEST_WSS_X509_NON_SOAP               = m(4801, Level.INFO, "Request not SOAP; unable to check for WS-Security signature");
    public static final M REQUEST_WSS_X509_NO_WSS_LEVEL_SECURITY  = m(4802, Level.INFO, "Request did not contain any WSS level security");
    public static final M REQUEST_WSS_X509_NO_TOKEN               = m(4803, Level.INFO, "No tokens were processed from this request; returning AUTH_REQUIRED");
    public static final M REQUEST_WSS_X509_TOO_MANY_VALID_SIG     = m(4804, Level.WARNING, true, false, "Request presented more than one valid signature from more than one client certificate");
    public static final M REQUEST_WSS_X509_CERT_LOADED            = m(4805, Level.FINE, "Certificate loaded as principal credential for CN:{0}");
    public static final M REQUEST_WSS_X509_NO_PROVEN_CERT         = m(4806, Level.INFO, "This assertion did not find a proven X.509 certificate to use as credentials - returning AUTH_REQUIRED");

    // ServerRequestWssReplayProtection messages
    public static final M REQUEST_WSS_REPLAY_NON_SOAP                       = m(4900, Level.INFO, "Request not SOAP; cannot check for replayed signed WS-Security message");
    public static final M REQUEST_WSS_REPLAY_NO_WSS_LEVEL_SECURITY          = m(4901, Level.INFO, "This request did not contain any WSS level security");
    public static final M REQUEST_WSS_REPLAY_NO_TIMESTAMP                   = m(4902, Level.INFO, "Assertion has failed because no timestamp present in request");
    public static final M REQUEST_WSS_REPLAY_TIMESTAMP_NOT_SIGNED           = m(4903, Level.INFO, "Assertion has failed because no signed timestamp present in request");
    public static final M REQUEST_WSS_REPLAY_TIMESTAMP_NO_CREATED_ELEMENT   = m(4904, Level.INFO, "Timestamp in request has no Created element");
    public static final M REQUEST_WSS_REPLAY_TIMESTAMP_NO_EXPIRES_ELEMENT   = m(4905, Level.INFO, "Timestamp in request has no Expires element; assuming expiry {0}ms after creation");
    public static final M REQUEST_WSS_REPLAY_CLOCK_SKEW                     = m(4906, Level.FINE, "Clock skew: message creation time is in the future: {0}; continuing anyway");
    public static final M REQUEST_WSS_REPLAY_TIMESTAMP_SIGNED_WITH_CERT     = m(4907, Level.FINER, "Timestamp was signed with an X.509 certificate");
    public static final M REQUEST_WSS_REPLAY_TIMESTAMP_SIGNED_WITH_SAML_HOK = m(4908, Level.FINER, "Timestamp was signed with a SAML holder-of-key assertion");
    public static final M REQUEST_WSS_REPLAY_TIMESTAMP_SIGNED_WITH_SC_KEY   = m(4909, Level.FINER, "Timestamp was signed with a WS-SecureConversation derived key");
    public static final M REQUEST_WSS_REPLAY_PROTECTION_SUCCEEDED           = m(4910, Level.FINEST, "Message ID {0} has not been seen before unique; assertion does not fail");
    public static final M REQUEST_WSS_REPLAY_TIMESTAMP_SIGNED_WITH_ENC_KEY  = m(4911, Level.FINER, "Timestamp was signed with an EncryptedKey");
    public static final M REQUEST_WSS_REPLAY_REPLAY                         = m(4912, Level.WARNING, "Message ID {0} is a replay");
    public static final M REQUEST_WSS_REPLAY_STALE_TIMESTAMP                = m(4913, Level.WARNING, "Request timestamp contained stale Expires date");
    public static final M REQUEST_WSS_REPLAY_CREATED_TOO_OLD                = m(4914, Level.WARNING, "Request timestamp contained Created older than the maximum message age hard cap");
    public static final M REQUEST_WSS_REPLAY_NO_SKI                         = m(4915, Level.WARNING, "Unable to generate replay-protection ID; a SKI cannot be derived from signing cert ''{0}''");
    public static final M REQUEST_WSS_REPLAY_UNSUPPORTED_TOKEN_TYPE         = m(4916, Level.WARNING, "Unable to generate replay-protection ID for timestamp -- it was signed, but with the unsupported token type {0}");

    // ServerCustomAssertionHolder
    public static final M CA_CREDENTIAL_INFO                                = m(5000, Level.FINE, "Service:{0}, custom assertion: {1}, principal:{2}");
    public static final M CA_INVALID_CA_DESCRIPTOR                          = m(5001, Level.WARNING, "Invalid custom assertion descriptor detected for {0}; policy element is misconfigured and will cause the policy to fail");

    // ServerHttpCredentialSource
    public static final M HTTP_CS_CANNOT_EXTRACT_CREDENTIALS                = m(5100, Level.INFO, "Request not HTTP; unable to extract HTTP credentials");

    // ServerWssBasic
    public static final M WSS_BASIC_FOR_ANOTHER_RECIPIENT                   = m(5200, Level.FINE, "This is intended for another recipient: nothing to validate");
    public static final M WSS_BASIC_NOT_SOAP                                = m(5201, Level.INFO, "Request not SOAP; cannot check for WS-Security UsernameToken");
    public static final M WSS_BASIC_NO_CREDENTIALS                          = m(5202, Level.INFO, "Request did not include WSS Basic credentials");
    public static final M WSS_BASIC_CANNOT_FIND_CREDENTIALS                 = m(5203, Level.INFO, "Cannot find credentials");
    public static final M WSS_BASIC_CANNOT_FIND_ENC_CREDENTIALS             = m(5204, Level.INFO,  "Request did not include an encrypted UsernameToken");
    public static final M WSS_BASIC_UNABLE_TO_ATTACH_TOKEN                  = m(5205, Level.WARNING, false, true, "Response not SOAP; unable to use WS-Security EncryptedUsernameToken");

    // ServerSslAssertion
    public static final M SSL_REQUIRED_PRESENT                              = m(5300, Level.FINE, "SSL required and present");
    public static final M SSL_REQUIRED_ABSENT                               = m(5301, Level.INFO, "SSL required but not present");
    public static final M SSL_FORBIDDEN_PRESENT                             = m(5302, Level.INFO, "SSL forbidden but present");
    public static final M SSL_FORBIDDEN_ABSENT                              = m(5303, Level.FINE, "SSL forbidden and not present");
    public static final M SSL_OPTIONAL_PRESENT                              = m(5304, Level.FINE, "SSL optional and present");
    public static final M SSL_OPTIONAL_ABSENT                               = m(5305, Level.FINE, "SSL optional and not present");

    // ServerResponseWssConfidentiality
    public static final M RESPONSE_WSS_CONF_REQUEST_NOT_SOAP                = m(5400, Level.INFO, "Request not SOAP; unable to check for WS-Security encrypted elements");
    public static final M RESPONSE_WSS_CONF_NO_WSS_SECURITY                 = m(5401, Level.INFO, "Request did not contain any WSS level security");
    public static final M RESPONSE_WSS_CONF_MORE_THAN_ONE_TOKEN             = m(5402, Level.WARNING, true, false, "Request included more than one X509 security token whose key ownership was proven");
    public static final M RESPONSE_WSS_CONF_NO_CERT_OR_SC_TOKEN             = m(5403, Level.WARNING, "Unable to encrypt response; request did not include X509 token or SecureConversation");
    public static final M RESPONSE_WSS_CONF_RESPONSE_NOT_SOAP               = m(5404, Level.WARNING, false, true, "Response not SOAP; unable to encrypt response elements");
    public static final M RESPONSE_WSS_CONF_RESPONSE_NOT_ENCRYPTED          = m(5405, Level.INFO, "No matching elements to encrypt in response: Assertion therefore fails");
    public static final M RESPONSE_WSS_CONF_RESPONSE_ENCRYPTED              = m(5406, Level.FINEST, "Designated {0} response elements for encryption");

    // ServerResponseWssIntegrity
    public static final M RESPONSE_WSS_INT_REQUEST_NOT_SOAP                 = m(5500, Level.INFO, "Request not SOAP; cannot sign response");
    public static final M RESPONSE_WSS_INT_RESPONSE_NOT_SOAP                = m(5501, Level.WARNING, false, true, "Response not SOAP; cannot apply WS-Security signature");
    public static final M RESPONSE_WSS_INT_RESPONSE_NO_MATCHING_EL          = m(5502, Level.INFO, "No matching elements to sign in response: Assertion therefore fails");
    public static final M RESPONSE_WSS_INT_RESPONSE_SIGNED                  = m(5503, Level.FINE, "Designated {0} response elements for signing");

    // ServerRequestWssIntegrity
    public static final M REQUEST_WSS_INT_RESPONSE_NOT_SOAP                 = m(5550, Level.FINE, "Response not SOAP; cannot return SignatureConfirmation");
    public static final M REQUEST_WSS_INT_REQUEST_MULTI_SIGNED              = m(5551, Level.WARNING, true, false, "Request has multiple signers; failing");

    // ServerSchemaValidation
    public static final M SCHEMA_VALIDATION_VALIDATE_REQUEST                = m(5600, Level.FINEST, "Validating request document");
    public static final M SCHEMA_VALIDATION_RESPONSE_NOT_XML                = m(5601, Level.WARNING, true, true, "Response not well-formed XML; cannot validate schema");
    public static final M SCHEMA_VALIDATION_VALIDATE_RESPONSE               = m(5602, Level.FINEST, "Validating response document");
    public static final M SCHEMA_VALIDATION_REQUEST_NOT_XML                 = m(5603, Level.WARNING, true, false, "Request not well-formed XML; cannot validate schema");
    public static final M SCHEMA_VALIDATION_FAILED                          = m(5604, Level.WARNING, true, true, "Schema validation failure: {0}");
    public static final M SCHEMA_VALIDATION_SUCCEEDED                       = m(5605, Level.FINEST, "Schema validation success");
    public static final M SCHEMA_VALIDATION_EMPTY_BODY                      = m(5606, Level.FINE, "Nothing to validate because the body is empty");
    public static final M SCHEMA_VALIDATION_NO_ACCEL                        = m(5607, Level.INFO, "Schema cannot be hardware accelerated");
    public static final M SCHEMA_VALIDATION_FALLBACK                        = m(5608, Level.INFO, "Hardware-accelerated schema validation failed; falling back to software");
    public static final M SCHEMA_VALIDATION_VALID_BUT_WRONG_NS              = m(5609, Level.INFO, "Message was valid but payload was in an unexpected namespace");
    public static final M SCHEMA_VALIDATION_GLOBALREF_BROKEN                = m(5610, Level.WARNING, "Cannot validate schema because the global schema named {0} cannot be retrieved");

    // ServerTimeRange
    public static final M TIME_RANGE_NOTHING_TO_CHECK                       = m(5700, Level.FINEST, "Nothing to check");
    public static final M TIME_RANGE_DOW_OUTSIDE_RANGE                      = m(5701, Level.INFO, "Failed because day of week outside allowed range");
    public static final M TIME_RANGE_TOD_OUTSIDE_RANGE                      = m(5702, Level.INFO, "Failed because time of day outside allowed range");
    public static final M TIME_RAGNE_WITHIN_RANGE                           = m(5703, Level.FINEST, "Request is within time range");

    // ServerUnknownAssertion
    public static final M UNKNOWN_ASSERTION                                 = m(5800, Level.WARNING, "Unknown assertion invoked; details: {0}");

    // ServerXslTransformation
    public static final M XSLT_REQ_NOT_XML   = m(5900, Level.INFO, "Message not XML; cannot perform XSL transformation");
    public static final M XSLT_REQUEST       = m(5901, Level.FINEST, "Transforming request");
    public static final M XSLT_RESP_NOT_XML  = m(5902, Level.INFO, "Response not XML; cannot perform XSL transformation");
    public static final M XSLT_RESPONSE      = m(5903, Level.FINEST, "Transforming response");
    public static final M XSLT_CONFIG_ISSUE  = m(5904, Level.WARNING, "Assertion does not specify whether transformation applies to request or response; returning failure");
    public static final M XSLT_NO_SUCH_PART  = m(5905, Level.WARNING, "Assertion refers to nonexistent MIME part {0}");
    public static final M XSLT_MULTIPLE_PIS  = m(5906, Level.WARNING, "Document contained multiple <?xml-stylesheet?> processing instructions; not currently supported");
    public static final M XSLT_CANT_READ_XSL = m(5907, Level.WARNING, "Could not retrieve linked XSL stylesheet at {0}: {1}");
    public static final M XSLT_BAD_EXT_XSL   = m(5908, Level.WARNING, "Unable to parse external XSL at {0}: {1}");
    public static final M XSLT_BAD_XSL       = m(5909, Level.WARNING, "Unable to parse XSL: {0}");
    public static final M XSLT_NO_PI         = m(5910, Level.WARNING, "No <?xml-stylesheet?> processing instruction was found in the message; assertion fails");
    public static final M XSLT_BAD_URL       = m(5911, Level.WARNING, "Stylesheet URL {0} did not match any configured regular expression");
    // note                                      5912 used below
    public static final M XSLT_TRANS_WARN    = m(5913, Level.INFO,    "XSL-T Warning ''{0}''");
    public static final M XSLT_TRANS_ERR     = m(5914, Level.INFO,    "XSL-T Error ''{0}''");
    public static final M XSLT_NO_PI_OK      = m(5915, Level.INFO, "No <?xml-stylesheet?> processing instruction was found in the message; assertion succeeds");

    // TODO move this message, now that it is shared among multiple assertion (XSLT + schema)
    public static final M RR_CANT_READ_REMOTE_RESOURCE = m(5912, Level.WARNING, "Could not retrieve remote resource at {0}: {1}; continuing using previous version");

    // ServerJmsRoutingAssertion
    public static final M JMS_ROUTING_CONNECT_FAILED                  = m(6000, Level.INFO, "Failed to establish JMS connection on try #{0}: Will retry after {1}ms");
    public static final M JMS_ROUTING_INBOUD_REQUEST_QUEUE_NOT_EMPTY  = m(6001, Level.FINE,  "Inbound request queue is not temporary; using selector to filter responses to our message");
    public static final M JMS_ROUTING_TOPIC_NOT_SUPPORTED             = m(6002, Level.WARNING, "Topics not supported!");
    public static final M JMS_ROUTING_REQUEST_ROUTED                  = m(6003, Level.FINER, "Routing request to protected service");
    public static final M JMS_ROUTING_GETTING_RESPONSE                = m(6004, Level.FINEST, "Getting response from protected service");
    public static final M JMS_ROUTING_NO_RESPONSE                     = m(6005, Level.WARNING, "Did not receive a routing reply within the timeout period of {0} ms; empty response being returned");
    public static final M JMS_ROUTING_GOT_RESPONSE                    = m(6006, Level.FINER, "Received routing reply");
    public static final M JMS_ROUTING_UNSUPPORTED_RESPONSE_MSG_TYPE   = m(6007, Level.WARNING, "Received JMS reply with unsupported message type {0}");
    public static final M JMS_ROUTING_NO_RESPONSE_EXPECTED            = m(6008, Level.INFO, "No response expected from protected service");
    public static final M JMS_ROUTING_DELETE_TEMPORARY_QUEUE          = m(6009, Level.FINER, "Deleting temporary queue" );
    public static final M JMS_ROUTING_RETURN_NO_REPLY                 = m(6010, Level.FINER, "Returning NO_REPLY (null) for {0}");
    public static final M JMS_ROUTING_RETURN_AUTOMATIC                = m(6011, Level.FINER, "Returning AUTOMATIC {0} for {1}");
    public static final M JMS_ROUTING_RETURN_REPLY_TO_OTHER           = m(6012, Level.FINER, "Returning REPLY_TO_OTHER {0} for {1}");
    public static final M JMS_ROUTING_UNKNOW_JMS_REPLY_TYPE           = m(6013, Level.WARNING, "Unknown JmsReplyType {0}");
    public static final M JMS_ROUTING_ENDPOINTS_ON_SAME_CONNECTION    = m(6014, Level.WARNING, "Request and reply endpoints must belong to the same connection");
    public static final M JMS_ROUTING_CREATE_REQUEST_AS_TEXT_MESSAGE  = m(6015, Level.FINER, "Creating request as TextMessage");
    public static final M JMS_ROUTING_CREATE_REQUEST_AS_BYTES_MESSAGE = m(6016, Level.FINER, "Creating request as BytesMessage");
    public static final M JMS_ROUTING_ROUTE_REQUEST_WITH_NO_REPLY     = m(6017, Level.FINE, "As routed request endpoint specified NO_REPLY, JMSReplyTo and JMSCorrelationID will not be set");
    public static final M JMS_ROUTING_SET_REPLYTO_CORRELCTIONID       = m(6018, Level.FINE, "Setting JMSReplyTo and JMSCorrelationID");
    public static final M JMS_ROUTING_NON_EXISTENT_ENDPOINT           = m(6019, Level.WARNING, "JMS Routing Assertion contains a reference to nonexistent JmsEndpoint #{0}");
    public static final M JMS_ROUTING_NO_SAML_SIGNER                  = m(6020, Level.WARNING, "JMS Routing Assertion cannot access SAML signing information");
    public static final M JMS_ROUTING_CANT_CONNECT_RETRYING           = m(6021, Level.WARNING, "Failed to establish JMS connection on try #{0}.  Will retry after {1}ms.");
    public static final M JMS_ROUTING_CANT_CONNECT_NOMORETRIES        = m(6022, Level.WARNING, "Tried {0} times to establish JMS connection and failed.");

    // ServerFtpRoutingAssertion
    public static final M FTP_ROUTING_FAILED_UPLOAD = m(6050, Level.WARNING, "Failed to upload request to {0}: {1}");
    public static final M FTP_ROUTING_SSL_NO_CERT = m(6051, Level.WARNING, "FTP server ({0}) did not identify itself properly: {1}");
    public static final M FTP_ROUTING_SSL_NOT_X509 = m(6052, Level.WARNING, "Cannot handle non-X.509 certificates from FTP server ({0})");
    public static final M FTP_ROUTING_SSL_UNTRUSTED = m(6053, Level.WARNING, "Cannot establish trust of SSL certificate from FTP server ({0}): {1}");
    public static final M FTP_ROUTING_PASSTHRU_NO_USERNAME = m(6054, Level.WARNING, "No user name found for passing through to FTP server");

    // ServerRequestWssSaml
    public static final M SAML_AUTHN_STMT_REQUEST_NOT_SOAP                     = m(6100, Level.FINEST, "Request not SOAP; cannot validate SAML statement");
    public static final M SAML_AUTHN_STMT_NO_TOKENS_PROCESSED                  = m(6101, Level.INFO, "No tokens were processed from this request: Returning AUTH_REQUIRED");
    public static final M SAML_AUTHN_STMT_MULTIPLE_SAML_ASSERTIONS_UNSUPPORTED = m(6102, Level.WARNING, true, false, "Request contained more than one SAML assertion");
    public static final M SAML_AUTHN_STMT_NO_ACCEPTABLE_SAML_ASSERTION         = m(6103, Level.INFO, "Assertion did not find an acceptable SAML assertion to use as credentials");
    public static final M SAML_STMT_VALIDATE_FAILED                            = m(6104, Level.WARNING, "SAML assertion validation errors: {0}");

    // ServerWsTrustCredentialExchange
    public static final M WSTRUST_NO_SUITABLE_CREDENTIALS = m(6200, Level.INFO, "The current request did not contain credentials of any supported type");
    public static final M WSTRUST_RSTR_BAD_TYPE           = m(6201, Level.WARNING, "WS-Trust response did not contain a security token of a supported type");
    public static final M WSTRUST_RSTR_STATUS_NON_200     = m(6202, Level.WARNING, "WS-Trust response had non-200 status");
    public static final M WSTRUST_NON_XML_MESSAGE         = m(6203, Level.INFO, "Cannot replace security token in a non-XML message");
    public static final M WSTRUST_DECORATION_FAILED       = m(6204, Level.WARNING, "Unable to replace security token");
    public static final M WSTRUST_ORIGINAL_TOKEN_NOT_XML  = m(6205, Level.INFO, "Original security token was not XML; cannot remove from request");
    public static final M WSTRUST_MULTI_TOKENS            = m(6206, Level.WARNING, "Multiple exchangeable Security Tokens found in request");
    public static final M WSTRUST_SERVER_HTTP_FAILED      = m(6207, Level.WARNING, "HTTP failure talking to WS-Trust server");

    //ServerRegex
    public static final M REGEX_PATTERN_INVALID   = m(6300, Level.WARNING, "Assertion has failed because of regex pattern ''{0}'' compile error: {1}");
    public static final M REGEX_TOO_BIG           = m(6301, Level.WARNING, "Regular expression cannot be evaluated; content is too large (>= " + Integer.MAX_VALUE + " bytes)");
    public static final M REGEX_NO_REPLACEMENT    = m(6302, Level.WARNING, "A replace was requested, but no replacement string was specified (null)");
    public static final M REGEX_NO_SUCH_PART      = m(6303, Level.WARNING, "Cannot search or replace in nonexistent part #{0}");
    public static final M REGEX_NO_ENCODING       = m(6304, Level.INFO,    "Character encoding not specified; will use default {0}");
    public static final M REGEX_ENCODING_OVERRIDE = m(6305, Level.FINE,    "Using overridden character encoding {0}");
    public static final M REGEX_NO_MATCH_FAILURE  = m(6306, Level.INFO,    "Failing because expression was not matched {0}");
    public static final M REGEX_MATCH_FAILURE     = m(6307, Level.INFO,    "Failing because expression was matched {0}");

    // SAML Browser General
    public static final M SAMLBROWSER_LOGINFORM_NON_200               = m(6400, Level.WARNING, "HTTP GET for login form resulted in non-200 status");
    public static final M SAMLBROWSER_LOGINFORM_NOT_HTML              = m(6401, Level.WARNING, "HTTP GET for login form resulted in non-HTML response");
    public static final M SAMLBROWSER_LOGINFORM_IOEXCEPTION           = m(6402, Level.WARNING, "Could not read login form HTML");
    public static final M SAMLBROWSER_LOGINFORM_PARSEEXCEPTION        = m(6403, Level.WARNING, "Unable to parse login form HTML");
    public static final M SAMLBROWSER_LOGINFORM_CANT_FIND_FIELDS      = m(6404, Level.WARNING, "Unable to find login and/or password field(s) in login form HTML");
    public static final M SAMLBROWSER_LOGINFORM_MULTIPLE_FIELDS       = m(6405, Level.WARNING, "Login form contained multiple username or password fields");
    public static final M SAMLBROWSER_LOGINFORM_MULTIPLE_FORMS        = m(6406, Level.WARNING, "Multiple login forms found");
    public static final M SAMLBROWSER_LOGINFORM_NO_FORM               = m(6407, Level.WARNING, "No matching login form found");
    public static final M SAMLBROWSER_LOGINFORM_BAD_METHOD            = m(6408, Level.WARNING, "Login form method was not POST");
    public static final M SAMLBROWSER_LOGINFORM_INVALID               = m(6409, Level.WARNING, "Login form is not valid");
    public static final M SAMLBROWSER_LOGINFORM_REDIRECT_INVALID      = m(6410, Level.WARNING, "Invalid redirect after FORM login");
    public static final M SAMLBROWSER_CREDENTIALS_NOCREDS             = m(6420, Level.WARNING, "Request does not contain any credentials");
    public static final M SAMLBROWSER_CREDENTIALS_CREDS_NOT_PASSWORD  = m(6421, Level.WARNING, "Request credentials do not include a password");

    // SAML Browser/Artifact
    public static final M SAMLBROWSERARTIFACT_RESPONSE_NON_302        = m(6500, Level.WARNING, "HTTP GET for login resulted in non-302 status");
    public static final M SAMLBROWSERARTIFACT_REDIRECT_NO_QUERY       = m(6501, Level.WARNING, "Redirect from login contained no query string");
    public static final M SAMLBROWSERARTIFACT_REDIRECT_BAD_QUERY      = m(6502, Level.WARNING, "Redirect query string could not be parsed");
    public static final M SAMLBROWSERARTIFACT_REDIRECT_NO_ARTIFACT    = m(6503, Level.WARNING, "Could not find SAML artifact in redirect query string");
    public static final M SAMLBROWSERARTIFACT_IOEXCEPTION             = m(6504, Level.WARNING, "Could not login");

    // XPath Credential Source
    public static final M XPATHCREDENTIAL_REQUEST_NOT_XML          = m(6600, Level.WARNING, "Request not XML");
    public static final M XPATHCREDENTIAL_LOGIN_XPATH_FAILED       = m(6601, Level.WARNING, "Login XPath evaluation failed");
    public static final M XPATHCREDENTIAL_LOGIN_XPATH_NOT_FOUND    = m(6602, Level.WARNING, "Login XPath evaluation failed to find any result");
    public static final M XPATHCREDENTIAL_LOGIN_FOUND_MULTI        = m(6603, Level.WARNING, "Login XPath evaluation found multiple results");
    public static final M XPATHCREDENTIAL_LOGIN_XPATH_WRONG_RESULT = m(6604, Level.WARNING, "Login XPath evaluation found content of an unsupported type");
    public static final M XPATHCREDENTIAL_LOGIN_PARENT_NOT_ELEMENT = m(6605, Level.WARNING, "Cannot remove login element; parent is not an Element");

    public static final M XPATHCREDENTIAL_PASS_XPATH_FAILED       = m(6611, Level.WARNING, "Password XPath evaluation failed");
    public static final M XPATHCREDENTIAL_PASS_XPATH_NOT_FOUND    = m(6612, Level.WARNING, "Password XPath evaluation failed to find any result");
    public static final M XPATHCREDENTIAL_PASS_FOUND_MULTI        = m(6613, Level.WARNING, "Login XPath evaluation found multiple results");
    public static final M XPATHCREDENTIAL_PASS_XPATH_WRONG_RESULT = m(6614, Level.WARNING, "Password XPath evaluation found content of an unsupported type");
    public static final M XPATHCREDENTIAL_PASS_PARENT_NOT_ELEMENT = m(6615, Level.WARNING, "Cannot remove password element; parent is not an Element");

    // Email and SNMP alerts
    public static final M EMAILALERT_MESSAGE_SENT = m(6700, Level.INFO, "Email message sent");
    public static final M EMAILALERT_BAD_TO_ADDR  = m(6701, Level.WARNING, "Bad destination email address(es)");
    public static final M EMAILALERT_BAD_FROM_ADDR= m(6702, Level.WARNING, "Bad source email address");
    public static final M SNMP_BAD_TRAP_OID       = m(6703, Level.WARNING, "The OID ending with zero is reserved for the message field: Using .1 for the trap OID instead");

    // HTTP Form POST
    public static final M HTTPFORM_WRONG_TYPE    = m(6800, Level.WARNING, true, false, "Request does not appear to be an HTTP form submission ({0})");
    public static final M HTTPFORM_NON_HTTP      = m(6801, Level.WARNING, "Request was not received via HTTP");
    public static final M HTTPFORM_MULTIVALUE    = m(6802, Level.WARNING, true, false, "Field {0} had multiple values; skipping");
    public static final M HTTPFORM_NO_SUCH_FIELD = m(6803, Level.WARNING, true, false, "Field {0} could not be found");
    public static final M HTTPFORM_NO_PARTS      = m(6804, Level.WARNING, "No MIME parts were found");
    public static final M HTTPFORM_BAD_MIME      = m(6805, Level.WARNING, "Unable to write new MIME message");
    public static final M HTTPFORM_TOO_BIG       = m(6806, Level.WARNING, "Field {0} is too large (>= " + 512 * 1024 + " bytes)");

    // HtmlFormDataAssertion
    public static final M HTMLFORMDATA_NOT_HTTP = m(6850, Level.INFO, "Request is not HTTP");
    public static final M HTTP_POST_NOT_FORM_DATA = m(6851, Level.INFO, "HTTP POST does not contain HTML Form data. (content type= {0})");
    public static final M HTMLFORMDATA_METHOD_NOT_ALLOWED = m(6852, Level.WARNING, "HTTP request method not allowed: {0}");
    public static final M HTMLFORMDATA_FIELD_NOT_FOUND = m(6582, Level.WARNING, "A required Form field is missing in the request. (name={0})");
    public static final M HTMLFORMDATA_UNKNOWN_FIELD_NOT_ALLOWED = m(6853, Level.WARNING, "Unspecified Form field encountered and not allowed. (name={0})");
    public static final M HTMLFORMDATA_UNKNOWN_FIELD_ALLOWED = m(6854, Level.FINE, "Unspecified Form field encountered but allowed through. (name={0})");
    public static final M HTMLFORMDATA_FAIL_DATATYPE = m(6855, Level.WARNING, "Form field value has wrong data type. (name={0}, value={1}, data type allowed={2})");
    public static final M HTMLFORMDATA_FAIL_MINOCCURS = m(6856, Level.WARNING, "Form field occurrences < min allowed. (name={0}, occurs={1}, min occurs allowed={2})");
    public static final M HTMLFORMDATA_FAIL_MAXOCCURS = m(6857, Level.WARNING, "Form field occurrences > max allowed. (name={0}, occurs={1}, max occurs allowed={2})");
    public static final M HTMLFORMDATA_LOCATION_NOT_ALLOWED = m(6858, Level.WARNING, "Form field is found in location not allowed. (name={0}, location not allowed={1})");

    // ServerThroughputQuota
    public static final M THROUGHPUT_QUOTA_EXCEEDED =    m(6900, Level.INFO, "Quota exceeded on counter {0}. Assertion limit is {1} current counter value is {2}");
    public static final M THROUGHPUT_QUOTA_ALREADY_MET = m(6901, Level.INFO, "Quota already exceeded on counter {0}");

    // ServerRateLimitAssertion
    public static final M RATELIMIT_RATE_EXCEEDED        = m(6950, Level.INFO, "Rate limit exceeded on rate limiter {0}");
    public static final M RATELIMIT_SLEPT_TOO_LONG       = m(6951, Level.INFO, "Unable to further delay request for rate limiter {0}, because maximum delay has been reached");
    public static final M RATELIMIT_NODE_CONCURRENCY     = m(6952, Level.INFO, "Unable to delay request for rate limiter {0}, because queued thread limit has been reached");
    public static final M RATELIMIT_CONCURRENCY_EXCEEDED = m(6953, Level.INFO, "Concurrency exceeded on rate limiter {0}.");

    // HTTP Form POST
    public static final M INVERSE_HTTPFORM_NO_SUCH_PART = m(7001, Level.WARNING, "Message has no part #{0}");
    public static final M INVERSE_HTTPFORM_TOO_BIG = m(7002, Level.WARNING, "Part #{0} is too large (>= " + 512 * 1024 + " bytes)");

    // Echo Routing assertion
    public static final M _UNUSED_CANNOT_ECHO_NON_XML = m(7050, Level.INFO, "Request cannot be echoed because it is not XML (Content-Type {0})");
    public static final M CANNOT_ECHO_NO_CTYPE        = m(7051, Level.INFO, "Requests cannot be echoed because it has no Content-Type");

    // ComparisonAssertion (formerly known as EqualityAssertion)
    public static final M COMPARISON_OK             = m(7100, Level.FINE, "Comparison matched");
    public static final M COMPARISON_NOT            = m(7101, Level.INFO, "Comparison did not match: {0}");
    public static final M COMPARISON_BAD_OPERATOR   = m(7102, Level.WARNING, "Unsupported operator: {0}");
    public static final M COMPARISON_NULL           = m(7103, Level.INFO, "At least one comparison value was null");
    public static final M COMPARISON_CONVERTING     = m(7104, Level.FINE, "Converting {0} value into {1}");
    public static final M COMPARISON_CANT_CONVERT   = m(7105, Level.INFO, "Value of type {0} cannot be converted to {1}");
    public static final M COMPARISON_NOT_COMPARABLE = m(7106, Level.INFO, "{0} Value for binary predicate ''{1}'' is not Comparable; using value.toString() instead");

    // CodeInjectionProtectionAssertion
    public static final M CODEINJECTIONPROTECTION_NOT_HTTP = m(7150, Level.FINE, "Request is not HTTP.");
    public static final M CODEINJECTIONPROTECTION_SKIP_RESPONSE_NOT_ROUTED = m(7151, Level.FINE, "No response body to checked because request has not been routed yet.");
    public static final M CODEINJECTIONPROTECTION_CANNOT_PARSE = m(7152, Level.WARNING, "Cannot parse {0} as {1}.");
    public static final M CODEINJECTIONPROTECTION_DETECTED_PARAM = m(7153, Level.WARNING, "Code injection detected in {0} parameter \"{1}\": {2}");
    public static final M CODEINJECTIONPROTECTION_DETECTED = m(7154, Level.WARNING, "Code injection detected in {0}: {1}");

    // SqlAttackAssertion
    public static final M SQLATTACK_UNRECOGNIZED_PROTECTION = m(7200, Level.WARNING, "Unrecognized protection name: {0}.  Assertion will always fail.");
    public static final M SQLATTACK_REQUEST_REJECTED        = m(7201, Level.WARNING, true, false, "Request was flagged by SQL attack protection assertion");
    public static final M SQLATTACK_ALREADY_ROUTED          = m(7203, Level.WARNING, "Unable to protect against SQL attacks - the request has already been routed");

    // RequestSizeLimit
    public static final M REQUEST_BODY_TOO_LARGE            = m(7220, Level.WARNING,  "Request body size exceeds configured limit");
    public static final M REQUEST_FIRST_PART_TOO_LARGE      = m(7221, Level.WARNING,  "Request first part size exceeds configured limit");

    // OversizedTextAssertion
    public static final M OVERSIZEDTEXT_ALREADY_ROUTED      = m(7230, Level.WARNING, "Unable to protect against document structure threats -- the request has already been routed");
    public static final M OVERSIZEDTEXT_OVERSIZED_TEXT      = m(7231, Level.WARNING, "Request includes an oversized text node or attribute value");
    public static final M XML_NESTING_DEPTH_EXCEEDED        = m(7232, Level.WARNING, "Request XML nesting depth exceeds the policy limit");
    public static final M OVERSIZEDTEXT_EXTRA_PAYLOAD       = m(7233, Level.WARNING, "Request message SOAP Body has too many children");
    public static final M REQUEST_NOT_SOAP                  = m(7234, Level.WARNING, "Request message does not have a valid SOAP Envelope"); 

    // ServerWsTrustCredentialExchange
    public static final M WSFEDPASS_NO_SUITABLE_CREDENTIALS = m(7300, Level.INFO, "The current request did not contain credentials of any supported type");
    public static final M WSFEDPASS_RSTR_BAD_TYPE           = m(7301, Level.WARNING, "WS-Federation response did not contain a security token of a supported type");
    public static final M WSFEDPASS_RSTR_STATUS_NON_200     = m(7302, Level.WARNING, "WS-Federation response had non-200 status");
    public static final M WSFEDPASS_NON_XML_MESSAGE         = m(7303, Level.INFO, "Cannot replace security token in non-XML message");
    public static final M WSFEDPASS_DECORATION_FAILED       = m(7304, Level.WARNING, "Unable to replace security token");
    public static final M WSFEDPASS_ORIGINAL_TOKEN_NOT_XML  = m(7305, Level.INFO, "Original security token was not XML; cannot remove from request");
    public static final M WSFEDPASS_MULTI_TOKENS            = m(7306, Level.WARNING, true, false, "Multiple security tokens found in request");
    public static final M WSFEDPASS_SERVER_HTTP_FAILED      = m(7307, Level.WARNING, "HTTP failure while communicating with WS-Federation server");
    public static final M WSFEDPASS_SERVER_HTTP_ENCODING    = m(7308, Level.WARNING, "Unknown encoding from WS-Federation server");
    public static final M WSFEDPASS_SERVER_HTML_INVALID     = m(7309, Level.WARNING, "Cannot parse HTML from WS-Federation server");
    public static final M WSFEDPASS_CONFIG_INVALID          = m(7310, Level.WARNING, "Invalid IP/STS URL in policy configuration");
    public static final M WSFEDPASS_AUTH_FAILED             = m(7311, Level.WARNING, "Authentication with service failed");
    public static final M WSFEDPASS_UNAUTHORIZED            = m(7312, Level.WARNING, "Not authorized to access this service");

    // ServerRequestWssKerberos messages
    public static final M REQUEST_WSS_KERBEROS_NON_SOAP               = m(7401, Level.INFO, "Request not SOAP; unable to check for WS-Security Binary Security Token");
    public static final M REQUEST_WSS_KERBEROS_NO_WSS_LEVEL_SECURITY  = m(7402, Level.INFO, "Request did not contain any WSS-level security");
    public static final M REQUEST_WSS_KERBEROS_NO_TOKEN               = m(7403, Level.INFO, "No tokens were processed from this request: Returning AUTH_REQUIRED");
    public static final M REQUEST_WSS_KERBEROS_NO_TICKET              = m(7404, Level.INFO, "This assertion did not find a Kerberos Binary Security Token to use as credentials. Returning AUTH_REQUIRED.");
    public static final M REQUEST_WSS_KERBEROS_GOT_TICKET             = m(7405, Level.FINE, "Kerberos ticket processed, principal is:{0}");
    public static final M REQUEST_WSS_KERBEROS_GOT_SESSION            = m(7406, Level.FINE, "Kerberos session processed, principal is:{0}");
    public static final M REQUEST_WSS_KERBEROS_INVALID_CONFIG         = m(7407, Level.WARNING, "Either the Kerberos server configuration is invalid or the KDC is unreachable");
    public static final M REQUEST_WSS_KERBEROS_INVALID_TICKET         = m(7408, Level.WARNING, "Could not process Kerberos ticket (not for this service?)");

    // ServerMappingAssertion messages
    public static final M MAPPING_NO_IDMAP    = m(7500, Level.WARNING, "No identity mMapping for provider #{0} found in attribute #{1}");
    public static final M MAPPING_NO_TOKMAP   = m(7501, Level.WARNING, "No security token mapping for provider #{0} found in attribute #{1}");
    public static final M MAPPING_NO_TOKVALUE = m(7502, Level.WARNING, "No suitable value could be found in any security token");
    public static final M MAPPING_NO_IDENTS   = m(7503, Level.WARNING, "No matching identities could be found");
    public static final M MAPPING_NO_IDVALUE  = m(7504, Level.WARNING, "No value could be found from any matching identity");

    public static final M USERDETAIL_FINEST  = m(-1, Level.FINEST,  "{0}");
    public static final M USERDETAIL_FINER   = m(-2, Level.FINER,   "{0}");
    public static final M USERDETAIL_FINE    = m(-3, Level.FINE,    "{0}");
    public static final M USERDETAIL_INFO    = m(-4, Level.INFO,    "{0}");
    public static final M USERDETAIL_WARNING = m(-5, Level.WARNING, "{0}");
    public static final M NO_SUCH_VARIABLE   = m(-6, Level.FINE, "No such variable: {0}");

    public static final M WSI_BSP_REQUEST_NON_SOAP       = m(7600, Level.INFO, "Request not SOAP; unable to check for WS-I Basic Security Profile compliance");
    public static final M WSI_BSP_RESPONSE_NON_SOAP      = m(7601, Level.INFO, false, true, "Response not SOAP; unable to check for WS-I Basic Security Profile compliance");
    public static final M WSI_BSP_REQUEST_NON_COMPLIANT  = m(7602, Level.WARNING, true, false, "WS-I BSP rule broken in request ({0}): {1}");
    public static final M WSI_BSP_RESPONSE_NON_COMPLIANT = m(7603, Level.WARNING, false, true, "WS-I BSP rule broken in response ({0}): {1}");
    public static final M WSI_BSP_REQUEST_FAIL           = m(7604, Level.INFO, "Failing non WS-I BSP compliant request");
    public static final M WSI_BSP_RESPONSE_FAIL          = m(7605, Level.INFO, "Failing non WS-I BSP compliant response");
    public static final M WSI_BSP_XPATH_ERROR            = m(7606, Level.WARNING, "Server WS-I BSP rules are incorrect");

    public static final M WSI_SAML_REQUEST_NON_SOAP       = m(7700, Level.INFO, "Request not SOAP; unable to check for WS-I SAML Token Profile compliance");
    public static final M WSI_SAML_RESPONSE_NON_SOAP      = m(7701, Level.INFO, false, true, "Response not SOAP; unable to check for WS-I  SAML Token Profile compliance");
    public static final M WSI_SAML_REQUEST_NON_COMPLIANT  = m(7702, Level.WARNING, true, false, "WS-I SAML Token Profile rule broken in request ({0}): {1}");
    public static final M WSI_SAML_RESPONSE_NON_COMPLIANT = m(7703, Level.WARNING, false, true, "WS-I SAML Token Profile rule broken in response ({0}): {1}");
    public static final M WSI_SAML_REQUEST_FAIL           = m(7704, Level.INFO, "Failing non WS-I SAML Token Profile compliant request");
    public static final M WSI_SAML_RESPONSE_FAIL          = m(7705, Level.INFO, "Failing non WS-I SAML Token Profile compliant response");
    public static final M WSI_SAML_XPATH_ERROR            = m(7706, Level.WARNING, "Server WS-I SAML Token Profile rules are incorrect");

    public static final M REQUEST_WSS_TIMESTAMP_NOTAPPLICABLE   = m(7800, Level.INFO, true, false, "The assertion is not applicable because the request is not XML or SOAP");
    public static final M REQUEST_WSS_TIMESTAMP_NOTIMESTAMP     = m(7801, Level.INFO, "No Timestamp found in the request");
    public static final M REQUEST_WSS_TIMESTAMP_NOT_SIGNED      = m(7802, Level.WARNING, "Timestamp found in the request, but was not signed");
    public static final M REQUEST_WSS_TIMESTAMP_CREATED_FUTURE  = m(7803, Level.WARNING, "Timestamp found in the request, but Created time was too far in the future");
    public static final M REQUEST_WSS_TIMESTAMP_EXPIRED         = m(7804, Level.WARNING, "Timestamp found in the request, but expired too long ago");
    public static final M REQUEST_WSS_TIMESTAMP_NO_EXPIRES      = m(7805, Level.WARNING, "Timestamp found in the request, but has no Expires time");
    public static final M REQUEST_WSS_TIMESTAMP_EXPIRES_TOOLATE = m(7806, Level.FINE, "Timestamp found in the request exceeds maximimum allowed lifetime, constraining to maximum");
    public static final M REQUEST_WSS_TIMESTAMP_NO_CREATED      = m(7807, Level.WARNING, "Timestamp found in the request, but has no Created time");
    public static final M REQUEST_WSS_TIMESTAMP_EXPIRED_TRUNC   = m(7809, Level.WARNING, "Timestamp found in the request, but is expired when constrained to maximum allowed lifetime");

    public static final M RESPONSE_WSS_TOKEN_UNSUPPORTED_TYPE = m(7900, Level.WARNING, "Unsupported security token type: {0}");
    public static final M RESPONSE_WSS_TOKEN_NO_CREDS         = m(7901, Level.WARNING, true, false, "No credentials were available from the request");
    public static final M RESPONSE_WSS_TOKEN_NO_USERNAME      = m(7902, Level.WARNING, true, false, "Credentials were available, but no username could be found");
    public static final M RESPONSE_WSS_TOKEN_NO_PASSWORD      = m(7903, Level.WARNING, true, false, "Password inclusion was requested, but no password could be found");

    public static final M CUSTOM_ASSERTION_INFO               = m(8000, Level.INFO, "Assertion ''{0}''; {1}");
    public static final M CUSTOM_ASSERTION_WARN               = m(8001, Level.WARNING, "Assertion ''{0}''; {1}");

    public static final M WSDLOPERATION_NOMATCH               = m(8100, Level.INFO, "Could not match WSDL operation ({0} instead of {1})");
    public static final M WSDLOPERATION_CANNOTIDENTIFY        = m(8101, Level.INFO, "Cannot identify any WSDL operation from request");

    public static final M HTTPNEGOTIATE_WARNING               = m(8200, Level.WARNING, "Could not process Kerberos token (Negotiate); error is ''{0}''");

    public static final M FTP_CREDENTIAL_NOT_FTP              = m(8300, Level.INFO, "Request not FTP; unable to extract FTP credentials.");
    public static final M FTP_CREDENTIAL_NO_AUTH              = m(8301, Level.FINE, "Not authenticated.");
    public static final M FTP_CREDENTIAL_AUTH_USER            = m(8302, Level.FINE, "Found credentials for user ''{0}''.");

    public static final M SAML_ISSUER_ISSUED_AUTHN  = m(8400, Level.FINE, "Issued SAML Authentication statement");
 	public static final M SAML_ISSUER_ISSUED_ATTR   = m(8401, Level.FINE, "Issued SAML Attribute statement");
 	public static final M SAML_ISSUER_ISSUED_AUTHZ  = m(8402, Level.FINE, "Issued SAML Authorization Decision statement");
 	public static final M SAML_ISSUER_ADDING_ATTR   = m(8403, Level.FINE, "Adding attribute {0} = {1}");
    public static final M SAML_ISSUER_AUTH_REQUIRED = m(8404, Level.WARNING, "NameIdentifier configured as \"From Authenticated User\", but no user has been authenticated");
    public static final M SAML_ISSUER_NOT_XML       = m(8405, Level.WARNING, "Message is not XML");
    public static final M SAML_ISSUER_NOT_SOAP      = m(8406, Level.WARNING, "Message is not SOAP");
    public static final M SAML_ISSUER_BAD_XML       = m(8407, Level.WARNING, "Message appeared to be SOAP but is not valid");
    public static final M SAML_ISSUER_CANT_DECORATE = m(8408, Level.WARNING, "WS-Security decoration failed");
    public static final M SAML_ISSUER_MISSING_NIVAL = m(8409, Level.WARNING, "Specified NameIdentifier chosen, but no value specified; using default");

    public static final M IDENTITY_ATTRIBUTE_NO_USER     = m(8450, Level.INFO, "No user from the expected identity provider has yet been authenticated");
    public static final M IDENTITY_ATTRIBUTE_MULTI_USERS = m(8451, Level.INFO, "Multiple users from the expected identity provider have been authenticated; choosing the first");

    public static final M INCLUDE_POLICY_EXCEPTION       = m(8500, Level.WARNING, "Included policy was updated, and is now invalid: {0}");
    public static final M INCLUDE_POLICY_NOT_FOUND       = m(8501, Level.WARNING, "Included policy #{0} ({1}) could not be located");

    public static final M WS_ADDRESSING_NO_HEADERS            = m(8550, Level.WARNING, "Required WS-Addressing headers not present");
    public static final M WS_ADDRESSING_NO_SIGNED_HEADERS     = m(8551, Level.WARNING, "Required signed WS-Addressing headers not present");
    public static final M WS_ADDRESSING_HEADERS_OK            = m(8552, Level.FINE, "WS-Addressing headers present");

    public static final M TEMPLATE_RESPONSE_EARLY    = m(8600, Level.FINE, "Sending response early");
    public static final M TEMPLATE_RESPONSE_NOT_HTTP = m(8601, Level.WARNING, "Unable to send early response for non HTTP message.");
}
