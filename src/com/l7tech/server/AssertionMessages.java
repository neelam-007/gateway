package com.l7tech.server;

import com.l7tech.common.audit.Messages;

import java.util.logging.Level;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
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
    public static final M RESPONSE_STATUS                      = m(4016, Level.FINE, "Protected service ({0}) responded with status {1}");
    public static final M ADD_OUTGOING_COOKIE_WITH_VERSION     = m(4017, Level.FINE, "Adding outgoing cookie: name = {0}, version = {1}");
    public static final M UPDATE_COOKIE                        = m(4018, Level.FINE,  "Updating cookie: name = {0}");
    public static final M BRIDGE_NO_ATTACHMENTS                = m(4019, Level.WARNING, "Bridge Routing Assertion does not currently support SOAP-with-attachments.  Ignoring additional MIME parts");


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
    public static final M AUTHENTICATION_FAILED                   = m(4208, Level.SEVERE, "Authentication failed for {0}");

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

    // ServerRequestXpathAssertion messages
    public static final M REQUEST_XPATH_ONLY                      = m(4700, Level.WARNING, "RequestXPathAssertion only works on XML requests; assertion therefore fails.");
    public static final M REQUEST_XPATH_PATTERN_INVALID           = m(4701, Level.WARNING, "XPath pattern is null or empty; assertion therefore fails.");
    public static final M REQUEST_XPATH_PATTERN_NOT_MATCHED       = m(4702, Level.INFO, "XPath pattern {0} didn't match request; assertion therefore fails." );
    public static final M REQUEST_XPATH_RESULT_TRUE               = m(4703, Level.FINE, "XPath pattern {0} returned true");
    public static final M REQUEST_XPATH_RESULT_FALSE              = m(4704, Level.INFO, "XPath pattern {0} returned false");
    public static final M REQUEST_XPATH_TEXT_NODE_FOUND           = m(4705, Level.FINE, "XPath pattern {0} found a text node {1}");
    public static final M REQUEST_XPATH_ELEMENT_FOUND             = m(4706, Level.FINE, "XPath pattern {0} found an element {1}");
    public static final M REQUEST_XPATH_OTHER_NODE_FOUND          = m(4707, Level.FINE,  "XPath pattern {0} found some other node {1}");
    public static final M REQUEST_XPATH_SUCCEED                   = m(4708, Level.FINE, "XPath pattern {0} matched request; assertion therefore succeeds.");

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
    public static final M REQUEST_WSS_REPLAY_PROTECTION_SUCCEEDED           = m(4910, Level.FINEST, "Message ID {0} has not been seen before unique; assertion succeeds");

    // ServerCustomAssertionHolder
    public static final M CA_CREDENTIAL_INFO                                = m(5000, Level.FINE, "Service:{0}, custom assertion: {1}, principal:{2}");
    public static final M CA_INVALID_CA_DESCRIPTOR                          = m(5001, Level.WARNING, "Invalid custom assertion descriptor detected for {0}, This policy element is misconfigured and will cause the policy to fail.");

}
