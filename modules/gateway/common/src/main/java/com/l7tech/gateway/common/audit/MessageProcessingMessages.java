/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.audit;

import java.util.logging.Level;

/**
 * Message catalog for messages audited by the message processor.
 * The ID range 3000-3499 inclusive is reserved for these messages.
 */
public class MessageProcessingMessages extends Messages {
    public static final M REQUEST_INVALID_XML_FORMAT  = m(3000, Level.WARNING, "Request XML is not well formed");
    public static final M MESSAGE_NOT_SOAP            = m(3001, Level.FINE, "Message is not soap");
    public static final M MESSAGE_NOT_SOAP_NO_WSS     = m(3002, Level.FINE, "Message is not SOAP and will not produce any WSS results");
    public static final M ERROR_WSS_PROCESSING        = m(3003, Level.INFO, "Error in WSS processing of request");
    public static final M ERROR_RETRIEVE_XML          = m(3004, Level.INFO, "Error getting XML document from request");
    public static final M SERVICE_NOT_FOUND           = m(3005, Level.INFO, "Service not found");
    public static final M SERVICE_DISABLED            = m(3006, Level.INFO, "Service disabled");
    public static final M RESOLVED_SERVICE            = m(3007, Level.FINE, "Resolved service {0} #{1}");
    public static final M POLICY_VERSION_INVALID      = m(3008, Level.FINEST, "Policy version passed is invalid: {0} instead of {1}|{2}");
    public static final M POLICY_VERSION_WRONG_FORMAT = m(3009, Level.FINE, "Wrong format for policy version");
    public static final M POLICY_ID_NOT_PROVIDED      = m(3010, Level.FINE, "Requestor did not provide policy ID");
    public static final M CANNOT_GET_POLICY           = m(3011, Level.WARNING, "Cannot get policy");
    public static final M RUNNING_POLICY              = m(3012, Level.FINEST, "Run the server policy");
    public static final M CANNOT_GET_STATS_OBJECT     = m(3013, Level.WARNING, "Cannot get statistics for published service");
    public static final M COMPLETION_STATUS           = m(3014, Level.FINE, "Request was completed with status {0} ({1})");
    public static final M SERVER_ERROR                = m(3015, Level.WARNING, "Policy status was NONE but routing was attempted anyway!");
    public static final M ROUTING_FAILED              = m(3016, Level.WARNING, "Request routing failed with status {0} ({1})");
    public static final M POLICY_EVALUATION_RESULT    = m(3017, Level.INFO, "Policy evaluation for service {0} resulted in status {1} ({2})");
    public static final M EVENT_MANAGER_EXCEPTION     = m(3018, Level.WARNING, "EventManager threw exception when logging message processing result");
    public static final M WSS_PROCESSING_COMPLETE     = m(3019, Level.FINEST, "WSS processing of request complete");
    public static final M LICENSE_NOT_ENABLED         = m(3020, Level.WARNING, "Message processor not enabled by license: {0}");
    public static final M METHOD_NOT_ALLOWED          = m(3021, Level.INFO, "HTTP method {0} not allowed for service {1}");
    public static final M REQUEST_INVALID_XML_FORMAT_WITH_DETAIL  = m(3022, Level.INFO, "Request XML is not well-formed [{0}]");
    public static final M MULTIPART_NOT_ALLOWED       = m(3023, Level.INFO, "Service does not accept multipart data");
    public static final M METHOD_NOT_ALLOWED_FAULT    = m(3024, Level.INFO, "HTTP method {0} not allowed");
    public static final M ERROR_WSS_SIGNATURE         = m(3025, Level.INFO, "Error in WSS signature processing: {0}");
    public static final M MESSAGE_NO_WSS              = m(3026, Level.INFO, "Message did not contain any WSS level security");
    public static final M MESSAGE_VAR_NOT_SOAP        = m(3027, Level.INFO, "{0} is not SOAP");
    public static final M MESSAGE_VAR_NO_WSS          = m(3028, Level.INFO, "{0} did not contain any WSS level security");
    public static final M MESSAGE_VAR_BAD_WSS         = m(3029, Level.INFO, "{0} WSS processing failed: {1}");    
    public static final M RESPONSE_IO_ERROR           = m(3030, Level.WARNING, "Response sent by the backend service generated an IOException: {0}.");
    public static final M CERT_KEY_USAGE              = m(3031, Level.WARNING, "Certificate key usage or extended key usage disallowed by key usage enforcement policy for activity: {0}");
    public static final M ERROR_WSS_PROCESSING_INFO   = m(3032, Level.INFO, "Error in WSS processing of request: {0}");
    public static final M MESSAGE_NO_SIG_CONFIRMATION = m(3033, Level.WARNING, "No suitable decoration was found for adding signature confirmations.");
    public static final M ERROR_WSS_RESPONSE          = m(3034, Level.WARNING, "Error in WSS processing of response: {0}");

    // Service resolver messages
    public static final M SR_HTTPURI_CACHEDFAIL      = m(3100, Level.FINE, "Using cached failure @\"{0}\"");
    public static final M SR_HTTPURI_PERFECT         = m(3101, Level.FINE, "Found a non-wildcard match for \"{0}\"");
    public static final M SR_HTTPURI_WILD_NONE       = m(3102, Level.FINE, "No match possible with URI \"{0}\"");
    public static final M SR_HTTPURI_WILD_ONE        = m(3103, Level.FINE, "One wildcard matched with URI \"{0}\"");
    public static final M SR_HTTPURI_WILD_MULTI      = m(3104, Level.FINE, "Multiple wildcard matches; using \"{0}\"");
    public static final M SR_HTTPURI_REAL_URI        = m(3105, Level.FINE, "Returning real URI: \"{0}\"");
    public static final M SR_HTTPURI_URI_FROM_HEADER = m(3106, Level.FINE, "Returning URI from header: \"{0}\"");
    public static final M SR_HTTPURI_BAD_HEADER      = m(3107, Level.WARNING, "Invalid L7-Original-URL value: \"{0}\"");

    public static final M SR_ORIGURL_NOHEADER       = m(3110, Level.FINEST, "The header \"{0}\" is not present");
    public static final M SR_ORIGURL_HEADER_MATCH   = m(3111, Level.FINEST, "Matched against the header \"{0}\" URL: \"{1}\"");
    public static final M SR_ORIGURL_HEADER_NOMATCH = m(3112, Level.FINEST, "Not Matched against the header \"{0}\" URL: \"{1}\"");
    public static final M SR_ORIGURL_URI_MATCH      = m(3113, Level.FINEST, "Matched against the Request URI: \"{0}\"");
    public static final M SR_ORIGURL_URI_NOMATCH    = m(3114, Level.FINEST, "Not Matched against the request URI: \"{0}\"");

    public static final M SR_SOAPACTION_NONE             = m(3120, Level.FINE, "SOAPAction not present");
    public static final M SR_SOAPACTION_NOT_HTTP_OR_SOAP = m(3121, Level.FINE, "Request is not SOAP or was not received via HTTP; no SOAPAction expected");

    public static final M SR_SOAPOPERATION_NOT_SOAP         = m(3130, Level.FINE, "Service is not SOAP");
    public static final M SR_SOAPOPERATION_NO_WSDL          = m(3131, Level.FINE, "Service is SOAP but has no WSDL");
    public static final M SR_SOAPOPERATION_WSDL_NO_BINDINGS = m(3132, Level.FINE, "WSDL has no bindings");
    // The following are deprecated because they were "moved" to {@link CommonMessages}, but they must remain here so that they can be looked up by ID for compatibility with old audit data
    /** @deprecated */ @Deprecated public static final M __UNUSED__SR_SOAPOPERATION_WSDL_NO_STYLE    = m(3133, CommonMessages.WSDL_OPERATION_NO_STYLE.getLevel(), CommonMessages.WSDL_OPERATION_NO_STYLE.getMessage());
    /** @deprecated */ @Deprecated public static final M __UNUSED__SR_SOAPOPERATION_WSDL_PART_TYPE   = m(3134, CommonMessages.WSDL_OPERATION_PART_TYPE.getLevel(), CommonMessages.WSDL_OPERATION_PART_TYPE.getMessage());
    /** @deprecated */ @Deprecated public static final M __UNUSED__SR_SOAPOPERATION_WSDL_DOCLIT_TYPE = m(3135, Level.INFO, "Input message {0} in document-style operation has a type, not an element");
    /** @deprecated */ @Deprecated public static final M __UNUSED__SR_SOAPOPERATION_BAD_STYLE        = m(3136, CommonMessages.WSDL_OPERATION_BAD_STYLE.getLevel(), CommonMessages.WSDL_OPERATION_BAD_STYLE.getMessage());
    /** @deprecated */ @Deprecated public static final M __UNUSED__SR_SOAPOPERATION_NO_QNAMES_FOR_OP = m(3137, CommonMessages.WSDL_OPERATION_NO_QNAMES_FOR_OP.getLevel(), CommonMessages.WSDL_OPERATION_NO_QNAMES_FOR_OP.getMessage());
    public static final M SR_SOAPOPERATION_NO_QNAMES_AT_ALL = m(3138, Level.INFO, "Unable to find any payload element QNames for service {0} (#{1})");
    public static final M SR_SOAPOPERATION_BAD_WSDL         = m(3139, Level.INFO, "Unable to parse WSDL for {0} service (#{1})");
    public static final M SR_SOAPOPERATION_FOUND_QNAME      = m(3140, Level.FINE, "Found payload QName \"{0}\"");

    /** @deprecated */ @Deprecated public static final M __UNUSED__SERVICE_CACHE_MODULE_UNLOAD      = m(3200, Level.INFO, "Recompiling all published services due to module unload");
    /** @deprecated */ @Deprecated public static final M __UNUSED__SERVICE_CACHE_RESETTING_SERVICES = m(3201, Level.INFO, "License changed/module loaded -- resetting {0} affected services");
    public static final M SERVICE_CACHE_DISABLING_SERVICE  = m(3202, Level.WARNING, "Unable to reenable service after license changed/module loaded: {0}: {1}");
    public static final M SERVICE_CACHE_NO_SERVICES        = m(3203, Level.FINEST, "Resolution failed; no Published Services");
    public static final M SERVICE_CACHE_RESOLVED_EARLY     = m(3204, Level.FINEST, "Service resolved early by {0}");
    public static final M SERVICE_CACHE_FAILED_EARLY       = m(3205, Level.FINE, "{0} eliminated all possible services");
    public static final M SERVICE_CACHE_NO_MATCH           = m(3206, Level.FINE, "Resolvers find no match for request");
    public static final M SERVICE_CACHE_RESOLVED           = m(3207, Level.FINEST, "Resolved request for \"{0}\" service (#{1})");
    public static final M SERVICE_CACHE_MULTI              = m(3208, Level.INFO, "Resolution failed; multiple services match the current request");
    public static final M SERVICE_CACHE_BAD_POLICY_FORMAT  = m(3209, Level.WARNING, "\"{0}\" service (#{1}) will be disabled; it has an unsupported policy format");
    public static final M SERVICE_CACHE_BAD_POLICY         = m(3210, Level.WARNING, "\"{0}\" service (#{1}) cannot be read properly and will be discarded from the service cache");
    public static final M SERVICE_CACHE_NOT_SOAP           = m(3211, Level.INFO, "Non-SOAP request resolved to SOAP service");
    public static final M SERVICE_CACHE_OPERATION_MISMATCH = m(3212, Level.INFO, "Resolved \"{0}\" service (#{1}) but request does not match any operation in the service''s WSDL");
    public static final M SERVICE_CACHE_ENABLING_SERVICE   = m(3213, Level.INFO, "Renable service after license or policy changed/module loaded: {0}: {1}");

    public static final M POLICY_CACHE_MODULE_UNLOAD      = m(3250, Level.INFO, "Recompiling all policies due to module unload");
    public static final M POLICY_CACHE_RESETTING_POLICIES = m(3251, Level.INFO, "License changed/module loaded -- resetting {0} affected policies");
    public static final M POLICY_CACHE_STORAGE_ERROR      = m(3252, Level.WARNING, "Error accessing policy (#{1})");
    public static final M POLICY_CACHE_BUILD              = m(3253, Level.INFO, "(Re)building policy cache");
    public static final M POLICY_CACHE_INVALID            = m(3254, Level.INFO, "Policy {0} (#{1}) is invalid: {2}");
    public static final M POLICY_CACHE_UNLICENSED         = m(3255, Level.WARNING,  "Policy {0} (#{1}) contains an unlicensed assertion: {2}");

     // MAX -                                                  m(3499
}
