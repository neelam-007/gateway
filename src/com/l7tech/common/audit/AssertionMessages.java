package com.l7tech.common.audit;

import java.util.logging.Level;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 */
public class AssertionMessages extends Messages {

    // ServerHttpRoutingAssertion messages
    public static final M SSL_CONTEXT_INIT_FAILED              = m(4000, Level.SEVERE, "Couldn't initialize SSL Context");
    public static final M HTTP_ROUTING_ASSERTION               = m(4001, Level.INFO, "Processing HTTP routing assertion");
    public static final M NON_SOAP_NOT_SUPPORTED_WRONG_FORMAT  = m(4002, Level.WARNING, "This option is not supported for non-soap messages. This message is supposed to be soap but does not appear to be.");
    public static final M NON_SOAP_NOT_SUPPORTED_WRONG_POLICY  = m(4003, Level.WARNING, "This option is not supported for non-soap messages. Something is wrong with this policy.");
    public static final M PROMOMTING_ACTOR                     = m(4004, Level.FINE, "promoting actor {0}");
    public static final M NO_SECURITY_HEADER                   = m(4005, Level.INFO, "Routing assertion asked for security header with actor {0} be promoted but there was no such security header present in the message.");
    public static final M ERROR_READING_RESPONSE               = m(4006, Level.SEVERE, "Error reading response");
    public static final M CANNOT_RESOLVE_IP_ADDRESS            = m(4007, Level.WARNING, "Couldn't resolve client IP address");
    public static final M TAI_REQUEST_NOT_AUTHENTICATED        = m(4008, Level.FINE, "TAI credential chaining requested, but request was not authenticated.");
    public static final M TAI_REQUEST_CHAIN_USERNAME           = m(4009, Level.FINE, "TAI credential chaining requested; will chain username {0}");
    public static final M TAI_REQUEST_USER_ID_NOT_UNIQUE       = m(4010, Level.WARNING, "TAI credential chaining requested, but request User did not have a unique identifier: id is {0}");
    public static final M TAI_REQUEST_CHAIN_LOGIN              = m(4011, Level.FINE, "TAI credential chaining requested, but there is no User; will chain pc.login {0}");
    public static final M TAI_REQUEST_NO_USER_OR_LOGIN         = m(4012, Level.WARNING, "TAI credential chaining requested, and request was authenticated, but had no User or pc.login");
    public static final M ADD_OUTGOING_COOKIE                  = m(4013, Level.FINE, "Adding outgoing cookie: name = {0}");
    public static final M LOGIN_INFO                           = m(4014, Level.FINE, "Using login '{0}'");
    public static final M ROUTED_OK                            = m(4015, Level.FINE, "Request routed successfully");
    public static final M RESPONSE_STATUS                      = m(4016, Level.WARNING, "Protected service ({0}) responded with status {1}");
    public static final M ADD_OUTGOING_COOKIE_WITH_VERSION     = m(4017, Level.FINE, "Adding outgoing cookie: name = {0}, version = {1}");
    public static final M UPDATE_COOKIE                        = m(4018, Level.FINE,  "Updating cookie: name = {0}");
    public static final M BRIDGE_NO_ATTACHMENTS                = m(4019, Level.WARNING, "Bridge Routing Assertion does not currently support SOAP-with-attachments.  Ignoring additional MIME parts");
    public static final M BRIDGE_BAD_CONFIG                    = m(4020, Level.SEVERE, "Bridge Routing Assertion is configured with invalid protected service URL or policy XML");
    public static final M BAD_ORIGINAL_REQUEST_URL             = m(4021, Level.WARNING, "Invalid original request URI -- using default");
    public static final M ACCESS_DENIED                        = m(4022, Level.WARNING, "Protected service denies access with current BridgeRoutingAssertion credentials");
    public static final M TOO_MANY_ROUTING_ATTEMPTS            = m(4023, Level.WARNING, "Too many failed attempts to route to this service: giving up");
    public static final M SAML_SV_REQUEST_NOT_AUTHENTICATED    = m(4024, Level.WARNING, "SAML Sender-Vouches forwarding requested, but request was not authenticated.");

    // ServerCredentialSourceAssertion messages
    public static final M AUTH_REQUIRED                        = m(4100, Level.INFO, "Authentication Required");

    // ServerIdentityAssertion
    public static final M AUTHENTICATED_BUT_CREDENTIALS_NOT_FOUND = m(4200, Level.WARNING, "Request is authenticated but request has no LoginCredentials!");
    public static final M CREDENTIALS_NOT_FOUND                   = m(4201, Level.INFO, "No credentials found!");
    public static final M ALREADY_AUTHENTICATED                   = m(4202, Level.FINEST, "Request already authenticated");
    public static final M ID_PROVIDER_ID_NOT_SET                  = m(4203, Level.SEVERE, "Can't call checkRequest() when no valid identityProviderOid has been set!");
    public static final M ID_PROVIDER_NOT_FOUND                   = m(4204, Level.SEVERE, "Couldn't find identity provider!");
    public static final M ID_PROVIDER_NOT_EXIST                   = m(4205, Level.WARNING, "id assertion refers to an id provider which does not exist anymore");
    public static final M AUTHENTICATED                           = m(4206, Level.FINEST, "Authenticated {0}");
    public static final M INVALID_CERT                            = m(4207, Level.INFO, "Invalid client cert for {0}");
    public static final M AUTHENTICATION_FAILED                   = m(4208, Level.INFO, "Authentication failed for {0}");

    // ServerRequestWssOperation messages
    public static final M NOTHING_TO_VALIDATE                     = m(4300, Level.FINE, "This is intended for another recipient, there is nothing to validate here.");
    public static final M CANNOT_VERIFY_WS_SECURITY               = m(4301, Level.INFO, "Request not SOAP; cannot verify WS-Security contents");
    public static final M NO_WSS_LEVEL_SECURITY                   = m(4302, Level.INFO, "This request did not contain any WSS level security.");

    // ServerRequestSwAAssertion messages
    public static final M REQUEST_NOT_SOAP                        = m(4400, Level.INFO, "Request not SOAP; cannot validate attachments");
    public static final M NOT_MULTIPART_MESSAGE                   = m(4401, Level.INFO, "The request does not contain attachment or is not a mulitipart message");
    public static final M OPERATION_NOT_FOUND                     = m(4402, Level.FINEST, "Operation not found in the request. Xpath expression is: {0}");
    public static final M SAME_OPERATION_APPEARS_MORE_THAN_ONCE   = m(4403, Level.INFO, "Same operation appears more than once in the request. Xpath expression is: {0}");
    public static final M OPERATION_IS_NON_ELEMENT_NODE           = m(4404, Level.INFO, "XPath pattern {0} found non-element node '{1}'");
    public static final M PARAMETER_IS_NON_ELEMENT_NODE           = m(4405, Level.INFO, "XPath pattern {0}/{1} found non-element node '{2}'");
    public static final M OPERATION_FOUND                         = m(4406, Level.FINEST, "The operation {0} is found in the request");
    public static final M MIME_PART_NOT_FOUND                     = m(4407, Level.FINE, "MIME Part not found in the request. Xpath expression is: {0}/{1})");
    public static final M SAME_MIME_PART_APPEARS_MORE_THAN_ONCE   = m(4408, Level.FINE, "Same MIME Part appears more than once in the request. Xpath expression is: {0}/{1}");
    public static final M PARAMETER_FOUND                         = m(4409, Level.FINEST, "Parameter {0} is found in the request");
    public static final M REFERENCE_NOT_FOUND                     = m(4410, Level.INFO, "The reference (href) of the {0} is found in the request");
    public static final M REFERENCE_FOUND                         = m(4411, Level.FINEST, "The href of the parameter {0} is found in the request, value={1}");
    public static final M INVALID_CONTENT_ID_URL                  = m(4412, Level.INFO, "Invalid Content-ID URL {0}");
    public static final M MUST_BE_ONE_OF_CONTENT_TYPES            = m(4413, Level.INFO, "The content type of the attachment {0} must be one of the types: {1}");
    public static final M INCORRECT_CONTENT_TYPE                  = m(4414, Level.INFO, "The content type of the attachment {0} must be: {1}");
    public static final M TOTAL_LENGTH_LIMIT_EXCEEDED             = m(4415, Level.INFO, "The parameter [{0}] has {1} attachments. The total length exceeds the limit: {2} K bytes");
    public static final M INDIVIDUAL_LENGTH_LIMIT_EXCEEDED        = m(4416, Level.INFO, "The length of the attachment {0} exceeds the limit: {1} K bytes");
    public static final M ATTACHMENT_NOT_FOUND                    = m(4417, Level.INFO, "The required attachment {0} is not found in the request");
    public static final M UNEXPECTED_ATTACHMENT_FOUND             = m(4418, Level.INFO, "Unexpected attachment {0} found in the request.");
    public static final M INVALID_OPERATION                       = m(4419, Level.INFO, "The operation specified in the request is invalid.");

    // ServerRemoteIpRange messages
    public static final M CANNOT_VALIDATE_IP_ADDRESS              = m(4500, Level.INFO, "Request was not received via TCP; cannot validate remote IP address");
    public static final M REMOTE_ADDRESS_INVALID                  = m(4501, Level.INFO, "The remote address {0} is null or not in expected format.");
    public static final M REQUESTOR_ADDRESS_ACCEPTED              = m(4502, Level.FINEST, "Requestor address {0} is accepted.");
    public static final M REQUESTOR_ADDRESS_REJECTED              = m(4503, Level.INFO,  "Requestor address {0} is not allowed");

    // ServerSecureConversation messages
    public static final M SC_REQUEST_NOT_SOAP                     = m(4600, Level.INFO, "Request not SOAP; unable to check for WS-SecureConversation token");
    public static final M SC_NO_WSS_LEVEL_SECURITY                = m(4601, Level.INFO, "This request did not contain any WSS level security.");
    public static final M SC_NO_PROOF_OF_POSSESSION               = m(4602, Level.FINE, "Ignoring SecurityContextToken with no proof-of-possession");
    public static final M SC_TOKEN_INVALID                        = m(4603, Level.WARNING, "The request referred to a SecureConversation token that is not recognized on this server. Perhaps the session has expired. Returning AUTH_FAILED.");
    public static final M SC_SESSION_FOR_USER                     = m(4604, Level.FINE, "Secure Conversation session recognized for user {0}");
    public static final M SC_REQUEST_NOT_REFER_TO_SC_TOKEN        = m(4605, Level.INFO, "This request did not seem to refer to a Secure Conversation token.");
    public static final M SC_UNABLE_TO_ATTACH_SC_TOKEN            = m(4606, Level.WARNING, "Response not SOAP; unable to attach WS-SecureConversation token");

    // ServerRequestXpathAssertion & ServerResponseXpathAssertion messages
    public static final M XPATH_REQUEST_NOT_XML                   = m(4700, Level.WARNING, "Request not XML; cannot evaluate XPath expression");
    public static final M XPATH_RESPONSE_NOT_XML                  = m(4701, Level.WARNING, "Response not XML; cannot evaluate XPath expression");
    public static final M XPATH_PATTERN_INVALID                   = m(4702, Level.WARNING, "XPath pattern is null or empty; assertion therefore fails.");
    public static final M XPATH_PATTERN_NOT_MATCHED_REQUEST       = m(4703, Level.INFO, "XPath pattern didn't match request; assertion therefore fails." );
    public static final M XPATH_PATTERN_NOT_MATCHED_RESPONSE      = m(4704, Level.INFO, "XPath pattern didn't match response; assertion therefore fails." );
    public static final M XPATH_RESULT_TRUE                       = m(4705, Level.FINE, "XPath pattern returned true");
    public static final M XPATH_RESULT_FALSE                      = m(4706, Level.INFO, "XPath pattern returned false");
    public static final M XPATH_TEXT_NODE_FOUND                   = m(4707, Level.FINE, "XPath pattern found a text node");
    public static final M XPATH_ELEMENT_FOUND                     = m(4708, Level.FINE, "XPath pattern found an element");
    public static final M XPATH_OTHER_NODE_FOUND                  = m(4709, Level.FINE,  "XPath pattern found some other node");
    public static final M XPATH_SUCCEED_REQUEST                   = m(4710, Level.FINE, "XPath pattern matched request; assertion therefore succeeds.");
    public static final M XPATH_SUCCEED_RESPONSE                  = m(4711, Level.FINE, "XPath pattern matched response; assertion therefore succeeds.");

    // ServerRequestAcceleratedXpathAssertion & ServerResponseAcceleratedXpathAssertion messages
    public static final M ACCEL_XPATH_NO_HARDWARE                 = m(4750, Level.INFO, "Hardware acceleration not available; falling back to software xpath processing.");
    public static final M ACCEL_XPATH_UNSUPPORTED_PATTERN         = m(4751, Level.INFO, "Hardware acceleration not available for this xpath expression; falling back to software xpath processing.");
    public static final M ACCEL_XPATH_NO_CONTEXT                  = m(4752, Level.WARNING, "This message has no hardware acceleration context; falling back to software xpath processing.");

    // ServerRequestWssX509Cert messages
    public static final M REQUEST_WSS_X509_FOR_ANOTHER_USER       = m(4800, Level.FINE, "This is intended for another recipient, there is nothing to validate here.");
    public static final M REQUEST_WSS_X509_NON_SOAP               = m(4801, Level.INFO, "Request not SOAP; unable to check for WS-Security signature");
    public static final M REQUEST_WSS_X509_NO_WSS_LEVEL_SECURITY  = m(4802, Level.INFO, "This request did not contain any WSS level security.");
    public static final M REQUEST_WSS_X509_NO_TOKEN               = m(4803, Level.INFO, "No tokens were processed from this request. Returning AUTH_REQUIRED.");
    public static final M REQUEST_WSS_X509_TOO_MANY_VALID_SIG     = m(4804, Level.SEVERE, "We got a request that presented more than one valid signature from {0} more than one client cert. This is not yet supported");
    public static final M REQUEST_WSS_X509_CERT_LOADED            = m(4805, Level.FINE, "Cert loaded as principal credential for CN:{0}");
    public static final M REQUEST_WSS_X509_NO_PROVEN_CERT         = m(4806, Level.INFO, "This assertion did not find a proven x509 cert to use as credentials. Returning AUTH_REQUIRED.");

    // ServerRequestWssReplayProtection messages
    public static final M REQUEST_WSS_REPLAY_NON_SOAP                       = m(4900, Level.INFO, "Request not SOAP; cannot check for replayed signed WS-Security message");
    public static final M REQUEST_WSS_REPLAY_NO_WSS_LEVEL_SECURITY          = m(4901, Level.INFO, "This request did not contain any WSS level security.");
    public static final M REQUEST_WSS_REPLAY_NO_TIMESTAMP                   = m(4902, Level.INFO, "No timestamp present in request; assertion therefore fails.");
    public static final M REQUEST_WSS_REPLAY_TIMESTAMP_NOT_SIGNED           = m(4903, Level.INFO, "Timestamp present in request was not signed; assertion therefore fails.");
    public static final M REQUEST_WSS_REPLAY_TIMESTAMP_NO_CREATED_ELEMENT   = m(4904, Level.INFO, "Timestamp in request has no Created element.");
    public static final M REQUEST_WSS_REPLAY_TIMESTAMP_NO_EXPIRES_ELEMENT   = m(4905, Level.INFO, "Timestamp in request has no Expires element; assuming expiry {0}ms after creation");
    public static final M REQUEST_WSS_REPLAY_CLOCK_SKEW                     = m(4906, Level.FINE, "Clock skew: message creation time is in the future: {0}; continuing anyway");
    public static final M REQUEST_WSS_REPLAY_TIMESTAMP_SIGNED_WITH_CERT     = m(4907, Level.FINER, "Timestamp was signed with an X509 BinarySecurityToken");
    public static final M REQUEST_WSS_REPLAY_TIMESTAMP_SIGNED_WITH_SAML_HOK = m(4908, Level.FINER, "Timestamp was signed with a SAML holder-of-key assertion");
    public static final M REQUEST_WSS_REPLAY_TIMESTAMP_SIGNED_WITH_SC_KEY   = m(4909, Level.FINER, "Timestamp was signed with a WS-SecureConversation derived key");
    public static final M REQUEST_WSS_REPLAY_PROTECTION_SUCCEEDED           = m(4910, Level.FINEST, "Message ID {0} has not been seen before unique; assertion does not fail");

    // ServerCustomAssertionHolder
    public static final M CA_CREDENTIAL_INFO                                = m(5000, Level.FINE, "Service:{0}, custom assertion: {1}, principal:{2}");
    public static final M CA_INVALID_CA_DESCRIPTOR                          = m(5001, Level.WARNING, "Invalid custom assertion descriptor detected for {0}, This policy element is misconfigured and will cause the policy to fail.");

    // ServerHttpCredentialSource
    public static final M HTTP_CS_CANNOT_EXTRACT_CREDENTIALS                = m(5100, Level.INFO, "Request not HTTP; unable to extract HTTP credentials");

    // ServerWssBasic
    public static final M WSS_BASIC_FOR_ANOTHER_RECIPIENT                   = m(5200, Level.FINE, "This is intended for another recipient, nothing to validate.");
    public static final M WSS_BASIC_NOT_SOAP                                = m(5201, Level.INFO, "Request not SOAP: Cannot check for WS-Security UsernameToken");
    public static final M WSS_BASIC_NO_CREDENTIALS                          = m(5202, Level.INFO, "Request did not include WSS Basic credentials.");
    public static final M WSS_BASIC_CANNOT_FIND_CREDENTIALS                 = m(5203, Level.INFO, "cannot find credentials");

    // ServerSslAssertion
    public static final M SSL_REQUIRED_PRESENT                              = m(5300, Level.FINE, "SSL required and present");
    public static final M SSL_REQUIRED_ABSENT                               = m(5301, Level.INFO, "SSL required but not present");
    public static final M SSL_FORBIDDEN_PRESENT                             = m(5302, Level.INFO, "SSL forbidden but present");
    public static final M SSL_FORBIDDEN_ABSENT                              = m(5303, Level.FINE, "SSL forbidden and not present");
    public static final M SSL_OPTIONAL_PRESENT                              = m(5304, Level.FINE, "SSL optional and present");
    public static final M SSL_OPTIONAL_ABSENT                               = m(5305, Level.FINE, "SSL optional and not present");

    // ServerResponseWssConfidentiality
    public static final M RESPONSE_WSS_CONF_REQUEST_NOT_SOAP                = m(5400, Level.INFO, "Request not SOAP; unable to check for WS-Security encrypted elements");
    public static final M RESPONSE_WSS_CONF_NO_WSS_SECURITY                 = m(5401, Level.INFO, "This request did not contain any WSS level security.");
    public static final M RESPONSE_WSS_CONF_MORE_THAN_ONE_TOKEN             = m(5402, Level.WARNING, "Request included more than one X509 security token whose key ownership was proven");
    public static final M RESPONSE_WSS_CONF_NO_CERT_OR_SC_TOKEN             = m(5403, Level.WARNING, "Unable to encrypt response. Request did not include x509 token or secure conversation.");
    public static final M RESPONSE_WSS_CONF_RESPONSE_NOT_SOAP               = m(5404, Level.WARNING, "Response not SOAP; unable to encrypt response elements");
    public static final M RESPONSE_WSS_CONF_RESPONSE_NOT_ENCRYPTED          = m(5405, Level.FINE, "No matching elements to encrypt in response.  Returning success.");
    public static final M RESPONSE_WSS_CONF_RESPONSE_ENCRYPTED              = m(5406, Level.FINEST, "Designated {0} response elements for encryption");

    // ServerResponseWssIntegrity
    public static final M RESPONSE_WSS_INT_REQUEST_NOT_SOAP                 = m(5500, Level.INFO, "Request not SOAP; cannot verify WS-Security signature");
    public static final M RESPONSE_WSS_INT_RESPONSE_NOT_SOAP                = m(5501, Level.WARNING, "Response not SOAP; cannot apply WS-Security signature");
    public static final M RESPONSE_WSS_INT_RESPONSE_NOT_SIGNED              = m(5502, Level.FINE, "No matching elements to sign in response.  Returning success.");
    public static final M RESPONSE_WSS_INT_RESPONSE_SIGNED                  = m(5503, Level.FINE, "Designated {0} response elements for signing");

    // ServerSchemaValidation                                            
    public static final M SCHEMA_VALIDATION_VALIDATE_REQUEST                = m(5600, Level.FINEST, "Validating response document");
    public static final M SCHEMA_VALIDATION_RESPONSE_NOT_XML                = m(5601, Level.INFO, "Response not XML; cannot validate schema");
    public static final M SCHEMA_VALIDATION_VALIDATE_RESPONSE               = m(5602, Level.FINEST, "Validating request document");
    public static final M SCHEMA_VALIDATION_REQUEST_NOT_XML                 = m(5603, Level.INFO, "Request not XML; cannot validate schema");
    public static final M SCHEMA_VALIDATION_FAILED                          = m(5604, Level.FINE, "Assertion failure: {0}");
    public static final M SCHEMA_VALIDATION_SUCCEEDED                       = m(5605, Level.FINEST, "Schema validation success");
    public static final M SCHEMA_VALIDATION_EMPTY_BODY                      = m(5606, Level.FINE, "Empty body. Nothing to validate");
    public static final M SCHEMA_VALIDATION_NO_ACCEL                        = m(5607, Level.WARNING, "Schema can not be hardware-accelerated");
    public static final M SCHEMA_VALIDATION_FALLBACK                        = m(5608, Level.WARNING, "Hardware-accelerated schema validation failed; falling back to software");

    // ServerTimeRange
    public static final M TIME_RANGE_NOTHING_TO_CHECK                       = m(5700, Level.FINEST, "Nothing to check.");
    public static final M TIME_RANGE_DOW_OUTSIDE_RANGE                      = m(5701, Level.INFO, "Day of week outside allowed range. Returning failure.");
    public static final M TIME_RANGE_TOD_OUTSIDE_RANGE                      = m(5702, Level.INFO, "Time of day outside allowed range. Returning failure.");
    public static final M TIME_RAGNE_WITHIN_RANGE                           = m(5703, Level.FINEST, "Request within TimeRange.");

    // ServerUnknownAssertion
    public static final M UNKNOWN_ASSERTION                                 = m(5800, Level.WARNING, "The unknown assertion invoked. Detail message is: {0}");

    // ServerXslTransformation
    public static final M XSL_TRAN_REQUEST_NOT_XML                        = m(5900, Level.INFO, "Request not XML; cannot perform XSL transformation");
    public static final M XSL_TRAN_REQUEST                                = m(5901, Level.FINEST, "Transforming request");
    public static final M XSL_TRAN_RESPONSE_NOT_XML                       = m(5902, Level.INFO, "Response not XML; cannot perform XSL transformation");
    public static final M XSL_TRAN_RESPONSE                               = m(5903, Level.FINEST, "Transforming response");
    public static final M XSL_TRAN_CONFIG_ISSUE                           = m(5904, Level.WARNING, "Assertion is not configured properly. should specify if transformation should apply to request or to response. returning failure.");

    // ServerJmsRoutingAssertion
    public static final M JMS_ROUTING_CONNECT_FAILED                      = m(6000, Level.INFO, "Failed to establish JMS connection on try #{0}. Will retry after {1}ms.");
    public static final M JMS_ROUTING_INBOUD_REQUEST_QUEUE_NOT_EMPTY      = m(6001, Level.FINE,  "Inbound request queue is not temporary; using selector to filter responses to our message");
    public static final M JMS_ROUTING_TOPIC_NOT_SUPPORTED                 = m(6002, Level.SEVERE, "Topics not supported!");
    public static final M JMS_ROUTING_REQUEST_ROUTED                      = m(6003, Level.FINER, "Routing request to protected service");
    public static final M JMS_ROUTING_GETTING_RESPONSE                    = m(6004, Level.FINEST, "Getting response from protected service");
    public static final M JMS_ROUTING_NO_RESPONSE                         = m(6005, Level.WARNING, "Did not receive a routing reply within timeout of {0} ms. Will return empty response");
    public static final M JMS_ROUTING_GOT_RESPONSE                        = m(6006, Level.FINER, "Received routing reply");
    public static final M JMS_ROUTING_UNSUPPORTED_RESPONSE_MSG_TYPE       = m(6007, Level.WARNING, "Received JMS reply with unsupported message type {0}");
    public static final M JMS_ROUTING_NO_RESPONSE_EXPECTED                = m(6008, Level.INFO, "No response expected from protected service");
    public static final M JMS_ROUTING_DELETE_TEMPORARY_QUEUE              = m(6009, Level.FINER, "Deleting temporary queue" );
    public static final M JMS_ROUTING_RETURN_NO_REPLY                     = m(6010, Level.FINER, "Returning NO_REPLY (null) for {0}");
    public static final M JMS_ROUTING_RETURN_AUTOMATIC                    = m(6011, Level.FINER, "Returning AUTOMATIC {0} for {1}");
    public static final M JMS_ROUTING_RETURN_REPLY_TO_OTHER               = m(6012, Level.FINER, "Returning REPLY_TO_OTHER {0} for {1}");
    public static final M JMS_ROUTING_UNKNOW_JMS_REPLY_TYPE               = m(6013, Level.SEVERE, "Unknown JmsReplyType {0}");
    public static final M JMS_ROUTING_ENDPOINTS_ON_SAME_CONNECTION        = m(6014, Level.SEVERE, "Request and reply endpoints must belong to the same connection");
    public static final M JMS_ROUTING_CREATE_REQUEST_AS_TEXT_MESSAGE      = m(6015, Level.FINER, "Creating request as TextMessage");
    public static final M JMS_ROUTING_CREATE_REQUEST_AS_BYTES_MESSAGE     = m(6016, Level.FINER, "Creating request as BytesMessage");
    public static final M JMS_ROUTING_ROUTE_REQUEST_WITH_NO_REPLY         = m(6017, Level.FINE, "Routed request endpoint specified NO_REPLY, won't set JMSReplyTo and JMSCorrelationID");
    public static final M JMS_ROUTING_SET_REPLYTO_CORRELCTIONID           = m(6018, Level.FINE, "Setting JMSReplyTo and JMSCorrelationID");
    public static final M JMS_ROUTING_NON_EXISTENT_ENDPOINT               = m(6019, Level.SEVERE, "JmsRoutingAssertion contains a reference to nonexistent JmsEndpoint #{0}");

    // ServerRequestWssSaml
    public static final M SAML_AUTHN_STMT_REQUEST_NOT_SOAP                     = m(6100, Level.FINEST, "Request not SOAP; cannot validate Saml Statement");
    public static final M SAML_AUTHN_STMT_NO_TOKENS_PROCESSED                  = m(6101, Level.INFO, "No tokens were processed from this request. Returning AUTH_REQUIRED.");
    public static final M SAML_AUTHN_STMT_MULTIPLE_SAML_ASSERTIONS_UNSUPPORTED = m(6102, Level.SEVERE, "We got a request that contained more than one SAML assertion. This is not currently supported.");
    public static final M SAML_AUTHN_STMT_NO_ACCEPTABLE_SAML_ASSERTION         = m(6103, Level.INFO, "This assertion did not find an acceptable SAML assertion to use as credentials.");
    public static final M SAML_STMT_VALIDATE_FAILED                            = m(6104, Level.SEVERE, "Saml Statement validation failed");

    // ServerWsTrustCredentialExchange
    public static final M WSTRUST_NO_SUITABLE_CREDENTIALS = m(6200, Level.INFO, "The current request did not contain credentials of any supported type");
    public static final M WSTRUST_RSTR_BAD_TYPE           = m(6201, Level.WARNING, "WS-Trust response did not contain a security token of a supported type");
    public static final M WSTRUST_RSTR_STATUS_NON_200     = m(6202, Level.WARNING, "WS-Trust response had non-200 status");
    public static final M WSTRUST_NON_XML_MESSAGE         = m(6203, Level.INFO, "Can't replace security token in non-XML message");
    public static final M WSTRUST_DECORATION_FAILED       = m(6204, Level.WARNING, "Unable to replace security token");
    public static final M WSTRUST_ORIGINAL_TOKEN_NOT_XML  = m(6205, Level.INFO, "Original security token was not XML; cannot remove from request");
    public static final M WSTRUST_MULTI_TOKENS            = m(6206, Level.WARNING, "Multiple Security Tokens found in request");
    public static final M WSTRUST_SERVER_HTTP_FAILED      = m(6207, Level.WARNING, "HTTP failure talking to WS-Trust server");


    //ServerRegex
    public static final M REGEX_PATTERN_INVALID   = m(6300, Level.WARNING, "Regex pattern '{0}' compile error: {1}; assertion therefore fails.");
    public static final M REGEX_TOO_BIG           = m(6301, Level.WARNING, "Regular expression cannot be evaluated; content is too large (>= " + 1024 * 512 + " bytes)");
    public static final M REGEX_NO_REPLACEMENT    = m(6302, Level.WARNING, "Replace requested, and no replace string specified (null).");
    public static final M REGEX_NO_SUCH_PART      = m(6303, Level.WARNING, "Cannot search or replace in nonexistent part #{0}");
    public static final M REGEX_NO_ENCODING       = m(6304, Level.INFO, "Character encoding not specified; will use default {0}");
    public static final M REGEX_ENCODING_OVERRIDE = m(6305, Level.FINE, "Using overridden character encoding {0}");

    // SAML Browser/POST
    public static final M SAMLBROWSERPOST_LOGINFORM_NON_200           = m(6400, Level.WARNING, "HTTP GET for login form resulted in non-200 status");
    public static final M SAMLBROWSERPOST_LOGINFORM_NOT_HTML          = m(6401, Level.WARNING, "HTTP GET for login form resulted in non-HTML response");
    public static final M SAMLBROWSERPOST_LOGINFORM_IOEXCEPTION       = m(6402, Level.WARNING, "Couldn't read login form HTML");
    public static final M SAMLBROWSERPOST_LOGINFORM_PARSEEXCEPTION    = m(6403, Level.WARNING, "Unable to parse login form HTML");
    public static final M SAMLBROWSERPOST_LOGINFORM_CANT_FIND_FIELDS  = m(6404, Level.WARNING, "Unable to find login and/or password field(s) in login form HTML");
    public static final M SAMLBROWSERPOST_LOGINFORM_MULTIPLE_FIELDS   = m(6405, Level.WARNING, "Login form contained multiple username or password fields");
    public static final M SAMLBROWSERPOST_LOGINFORM_MULTIPLE_FORMS    = m(6406, Level.WARNING, "Multiple login forms found");
    public static final M SAMLBROWSERPOST_LOGINFORM_NO_FORM           = m(6407, Level.WARNING, "No matching login form found");

    // SAML Browser/Artifact
    public static final M SAMLBROWSERARTIFACT_RESPONSE_NON_302        = m(6500, Level.WARNING, "HTTP GET for login resulted in non-302 status");
    public static final M SAMLBROWSERARTIFACT_REDIRECT_NO_QUERY       = m(6501, Level.WARNING, "Redirect from login contained no query string");
    public static final M SAMLBROWSERARTIFACT_REDIRECT_BAD_QUERY      = m(6502, Level.WARNING, "Redirect query string could not be parsed");
    public static final M SAMLBROWSERARTIFACT_REDIRECT_NO_ARTIFACT    = m(6503, Level.WARNING, "Couldn't find SAML artifact in redirect query string");
    public static final M SAMLBROWSERARTIFACT_IOEXCEPTION             = m(6504, Level.WARNING, "Couldn't login");

    // XPath Credential Source
    public static final M XPATHCREDENTIAL_REQUEST_NOT_XML          = m(6600, Level.WARNING, "Request not valid XML");
    public static final M XPATHCREDENTIAL_LOGIN_XPATH_FAILED       = m(6601, Level.WARNING, "Login XPath evaluation failed");
    public static final M XPATHCREDENTIAL_LOGIN_XPATH_NOT_FOUND    = m(6602, Level.WARNING, "Login XPath evaluation failed to find any result");
    public static final M XPATHCREDENTIAL_LOGIN_FOUND_MULTI        = m(6603, Level.WARNING, "Login XPath evaluation found multiple results");
    public static final M XPATHCREDENTIAL_LOGIN_XPATH_WRONG_RESULT = m(6604, Level.WARNING, "Login XPath evaluation found content of an unsupported type");
    public static final M XPATHCREDENTIAL_LOGIN_PARENT_NOT_ELEMENT = m(6605, Level.WARNING, "Can't remove login element; parent is not an Element");

    public static final M XPATHCREDENTIAL_PASS_XPATH_FAILED       = m(6611, Level.WARNING, "Password XPath evaluation failed");
    public static final M XPATHCREDENTIAL_PASS_XPATH_NOT_FOUND    = m(6612, Level.WARNING, "Password XPath evaluation failed to find any result");
    public static final M XPATHCREDENTIAL_PASS_FOUND_MULTI        = m(6613, Level.WARNING, "Login XPath evaluation found multiple results");
    public static final M XPATHCREDENTIAL_PASS_XPATH_WRONG_RESULT = m(6614, Level.WARNING, "Password XPath evaluation found content of an unsupported type");
    public static final M XPATHCREDENTIAL_PASS_PARENT_NOT_ELEMENT = m(6615, Level.WARNING, "Can't remove password element; parent is not an Element");

    // Email and SNMP alerts
    public static final M EMAILALERT_MESSAGE_SENT = m(6700, Level.INFO, "Email message sent");
    public static final M EMAILALERT_BAD_TO_ADDR  = m(6701, Level.WARNING, "Bad destination email address");
    public static final M EMAILALERT_BAD_FROM_ADDR= m(6702, Level.WARNING, "Bad source email address");
    public static final M SNMP_BAD_TRAP_OID       = m(6703, Level.WARNING, "The OID ending with zero is reserved for the message field.  Using .1 for the trap OID instead.");

    // HTTP Form POST
    public static final M HTTPFORM_WRONG_TYPE    = m(6800, Level.WARNING, "Request does not appear to be an HTTP form submission ({0})");
    public static final M HTTPFORM_NON_HTTP      = m(6801, Level.WARNING, "Request was not received via HTTP");
    public static final M HTTPFORM_MULTIVALUE    = m(6802, Level.WARNING, "Field {0} had multiple values; skipping");
    public static final M HTTPFORM_NO_SUCH_FIELD = m(6803, Level.WARNING, "Field {0} could not be found");
    public static final M HTTPFORM_NO_PARTS      = m(6804, Level.WARNING, "No MIME parts were found");
    public static final M HTTPFORM_BAD_MIME      = m(6805, Level.WARNING, "Unable to write new MIME message");
    public static final M HTTPFORM_TOO_BIG       = m(6806, Level.WARNING, "Field {0} is too large (>= " + 512 * 1024 + " bytes)");

    // ServerThroughputQuota
    public static final M THROUGHPUT_QUOTA_EXCEEDED =    m(6900, Level.INFO, "Quota exceeded on counter {0}. Assertion limit is {1} current counter value is {2}");
    public static final M THROUGHPUT_QUOTA_ALREADY_MET = m(6901, Level.INFO, "Quota already exceeded on counter {0}.");

    // HTTP Form POST
    public static final M INVERSE_HTTPFORM_NO_SUCH_PART = m(7001, Level.WARNING, "Message has no part #{0}");
    public static final M INVERSE_HTTPFORM_TOO_BIG = m(7002, Level.WARNING, "Part #{0} is too large (>= " + 512 * 1024 + " bytes)");
}
