package com.l7tech.server;

import com.l7tech.common.audit.Messages;

import java.util.logging.Level;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class MessageProcessingMessages extends Messages {
    public static final M REQUEST_INVALID_XML_FORMAT  = m(3000, Level.SEVERE, "Request XML is not well-formed");
    public static final M MESSAGE_NOT_SOAP            = m(3001, Level.INFO, "Message is not soap");
    public static final M MESSAGE_NOT_SOAP_NO_WSS     = m(3002, Level.FINE, "Message is not SOAP; will not have any WSS results.");
    public static final M ERROR_WSS_PROCESSING        = m(3003, Level.SEVERE, "Error in WSS processing of request");
    public static final M ERROR_RETRIEVE_XML          = m(3004, Level.SEVERE, "Error getting xml document from request");
    public static final M SERVICE_NOT_FOUND           = m(3005, Level.WARNING, "Service not found");
    public static final M SERVICE_DISABLED            = m(3006, Level.WARNING, "Service disabled");
    public static final M RESOLVED_SERVICE            = m(3007, Level.INFO, "Resolved service {0} #{1}");
    public static final M POLICY_VERSION_INVALID      = m(3008, Level.FINEST, "policy version passed is invalid {0} instead of {1}|{2}");
    public static final M POLICY_VERSION_WRONG_FORMAT = m(3009, Level.FINE, "wrong format for policy version");
    public static final M POLICY_ID_NOT_PROVIDED      = m(3010, Level.FINE, "Requestor did not provide policy id.");
    public static final M CANNOT_GET_POLICY           = m(3011, Level.WARNING, "cannot get policy");
    public static final M RUNNING_POLICY              = m(3012, Level.FINEST, "Run the server policy");
    public static final M CANNOT_GET_STATS_OBJECT     = m(3013, Level.WARNING, "cannot get a stats object");
    public static final M COMPLETION_STATUS           = m(3014, Level.FINE, "Request was completed with status {0} ({1})");
    public static final M SERVER_ERROR                = m(3015, Level.SEVERE, "Policy status was NONE but routing was attempted anyway!");
    public static final M ROUTING_FAILED              = m(3016, Level.WARNING, "Request routing failed with status {0} ({1})");
    public static final M POLICY_EVALUATION_RESULT    = m(3017, Level.WARNING, "Policy evaluation resulted in status {0} ({1})");
    public static final M EVENT_MANAGER_EXCEPTION     = m(3018, Level.WARNING, "EventManager threw exception logging message processing result");
    public static final M WSS_PROCESSING_COMPLETE     = m(3019, Level.FINEST, "WSS processing of request complete.");
}
