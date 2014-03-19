/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.gateway.common.audit;

import java.util.logging.Level;

/**
 * Message catalog for system componenets.
 *
 * The ID range 2000-2999 (inclusive) is reserved for these messages.
 */
public class SystemMessages extends Messages {
    public static final M DATABASE_ERROR                = m(2000, Level.WARNING, "Database error");
    public static final M DATABASE_ERROR_WITH_MORE_INFO = m(2001, Level.WARNING, "{0}. Database error");

    public static final M FIPS_MODE_ENABLED             = m(2005, Level.INFO,    "FIPS mode enabled");
    public static final M FIPS_MODE_DISABLED            = m(2006, Level.INFO,    "FIPS mode disabled");

    public static final M LICENSE_DB_ERROR_RETRY        = m(2010, Level.WARNING, "Database error reading license file.  Will keep current license and retry.");
    public static final M LICENSE_DB_ERROR_GAVEUP       = m(2011, Level.WARNING, "Database error reading license file.  Current license was too stale to keep.  Will keep trying.");
    public static final M LICENSE_NO_LICENSE            = m(2012, Level.WARNING, "No valid license is installed.  Some product features may be disabled.");
    public static final M LICENSE_FOUND                 = m(2013, Level.FINE,  "Valid license(s) found");
    public static final M LICENSE_INVALID               = m(2014, Level.WARNING, "License file is not valid");
    public static final M LICENSE_UPDATED               = m(2015, Level.INFO, "License updated");

    public static final M SERVICE_WSDL_ERROR            = m(2016, Level.WARNING, "Service ''{0}'' WSDL error ''{1}''");

    public static final M SOCKET_TIMEOUT                = m(2017, Level.WARNING, "A remote network connection timed out");

    /** @deprecated */ @Deprecated public static final M __UNUSED__FTPSERVER_NOT_CONFIGURED = m(2020, Level.INFO, "Not starting FTP server (no listeners enabled).");
    /** @deprecated */ @Deprecated public static final M __UNUSED__FTPSERVER_START          = m(2021, Level.INFO, "Starting FTP server: ''{0}''");
    /** @deprecated */ @Deprecated public static final M __UNUSED__FTPSERVER_STOP           = m(2022, Level.INFO, "Stopping FTP server: ''{0}''");
    /** @deprecated */ @Deprecated public static final M __UNUSED__FTPSERVER_ERROR          = m(2023, Level.WARNING, "FTP server error ''{0}''");

    public static final M HTTPSERVER_START              = m(2026, Level.INFO, "Starting HTTP server: ''{0}''");
    public static final M HTTPSERVER_STOP               = m(2027, Level.INFO, "Stopping HTTP server: ''{0}''");
    public static final M HTTPSERVER_ERROR              = m(2028, Level.WARNING, "HTTP server error ''{0}''");
    
    // Used by certificate validation
    public static final M CERTVAL_CANT_FIND_ISSUER      = m(2030, Level.WARNING, "Unable to locate Trusted Cert Entry for issuer with DN \"{0}\" of certificate with DN \"{1}\"");
    public static final M CERTVAL_CHECKED               = m(2031, Level.FINE, "Certificate validated and verified");
    public static final M CERTVAL_REV_NOT_REVOKED       = m(2032, Level.FINE, "Certificate {0} is not revoked");
    public static final M CERTVAL_REV_REVOKED           = m(2033, Level.WARNING, "Certificate {0} is revoked");
    public static final M CERTVAL_CANT_BUILD_PATH       = m(2034, Level.WARNING, "Unable to build path for Certificate {0}: {1}");
    public static final M CERTVAL_INVALID_SETTING       = m(2035, Level.WARNING, "Invalid setting for validation level {0}: {1}");
    public static final M CERTVAL_CERT_EXPIRED          = m(2036, Level.WARNING, "Certificate {0} has expired");
    public static final M CERTVAL_CERT_NOT_YET_VALID    = m(2037, Level.WARNING, "Certificate {0} is not yet valid");

    // In the following, {0} is either "CRL" or "OCSP".
    public static final M CERTVAL_REV_MULTIPLE_URLS           = m(2040, Level.WARNING, "Certificate {1} contained multiple {0} URLs");
    public static final M CERTVAL_REV_NO_URL                  = m(2041, Level.WARNING, "Certificate {1} contained no {0} URL");
    public static final M CERTVAL_REV_USING_STATIC_URL        = m(2042, Level.FINE,    "Using static {0} URL: {1}");
    public static final M CERTVAL_REV_URL_INVALID             = m(2043, Level.WARNING, "Invalid {0} URL: {1}");
    public static final M CERTVAL_REV_RETRIEVAL_FAILED        = m(2044, Level.WARNING, "Couldn''t get {0} response from URL {1}: {2}");
    public static final M CERTVAL_REV_CANT_GET_URLS_FROM_CERT = m(2045, Level.WARNING, "{0} URL(s) could not be parsed from certificate {1}");
    public static final M CERTVAL_REV_URL_MISMATCH            = m(2046, Level.WARNING, "No {0} URLs from Certificate {1} matched regex");
    public static final M CERTVAL_REV_URL_MATCH               = m(2047, Level.FINE,    "{0} URL {1} from Certificate {2} matches regex");
    public static final M CERTVAL_REV_ISSUER_NOT_FOUND        = m(2048, Level.WARNING, "Unable to locate {0} issuer certificate {1}");
    public static final M CERTVAL_REV_CACHE_MISS              = m(2049, Level.INFO,    "No {0} cache for {1}; refreshing");
    public static final M CERTVAL_REV_CACHE_HIT               = m(2050, Level.FINE,    "Using {0} cache for {1}");
    public static final M CERTVAL_REV_CACHE_STALE             = m(2051, Level.INFO,    "{0} cache for {1} refresh due at {2}; refreshing");
    public static final M CERTVAL_REV_CACHE_FRESH             = m(2052, Level.FINE,    "{0} cache for {1} refresh due at {2}; using cache");
    public static final M CERTVAL_REV_SIGNER_IS_ISSUER        = m(2053, Level.FINE,    "Using issuer ''{1}'' as {0} signer.");
    public static final M CERTVAL_REV_SIGNER_IS_ISSUER_DELE   = m(2054, Level.FINE,    "Using issuer authorized certificate ''{1}'' as {0} signer");
    public static final M CERTVAL_REV_SIGNER_IS_TRUSTED       = m(2055, Level.FINE,    "Using trusted certificate ''{1}'' as {0} signer");
    public static final M CERTVAL_REV_USE_CACHE               = m(2056, Level.WARNING, "Server unavailable for {0} update for {1}, using {2} cached version.");

    // CRL only
    public static final M CERTVAL_CRL_SCOPE                   = m(2070, Level.FINE,    "CRL scope does not cover certificate ''{0}'', CRL URL is ''{1}''");
    public static final M CERTVAL_REV_CRL_INVALID             = m(2071, Level.WARNING, "CRL at {0} is invalid: {1}");

    // OCSP Only
    public static final M CERTVAL_OCSP_ERROR                  = m(2085, Level.WARNING, "Error during OCSP check for responder ''{0}'': {1}");
    public static final M CERTVAL_OCSP_SIGNER_CERT_REVOKED    = m(2086, Level.WARNING, "OCSP responder ''{0}'' signing certificate ''{1}'' is revoked");
    public static final M CERTVAL_OCSP_BAD_RESPONSE_STATUS    = m(2087, Level.WARNING, "Bad status in OCSP check for responder ''{0}'': {1}");
    public static final M CERTVAL_OCSP_RECURSION              = m(2088, Level.WARNING, "Circular OCSP check for responder ''{0}''");

    // Policy versioning
    public static final M POLICY_VERSION_ACTIVATION           = m(2090, Level.INFO, "Activating version {0} of policy {1}");

    public static final M AUTH_USER_DISABLED = m(2100, Level.INFO, "User {0} is disabled");
    public static final M AUTH_USER_LOCKED   = m(2101, Level.INFO, "User {0} is locked");
    public static final M AUTH_USER_EXPIRED  = m(2102, Level.INFO, "User {0} has expired");
    public static final M AUTH_USER_EXCEED_ATTEMPT = m(2103, Level.INFO, "User ''{0}'' has exceeded max. number of failed logon attempts.  User has attempted {1} out of {2} allowable failure attempts.");
    public static final M AUTH_USER_EXCEED_INACTIVITY = m(2104, Level.INFO, "User ''{0}'' has exceeded inactivity period.  User''s last activity {1} days ago exceeded {2} days.");


    public static final M CERT_EXPIRY_CANT_FIND  = m(2150, Level.WARNING, "Unable to find trusted certificates to check for upcoming expirations; skipping this check");
    public static final M CERT_EXPIRY_BAD_CERT   = m(2151, Level.WARNING, "Unable to parse trusted certificate #{0} ({1}) in order to check for expiration; skipping");
    public static final M CERT_EXPIRING_FINE     = m(2152, Level.FINE, "Trusted certificate #{0} ({1}) will expire in {2}");
    public static final M CERT_EXPIRING_INFO     = m(2153, Level.INFO, "Trusted certificate #{0} ({1}) will expire in {2}");
    public static final M CERT_EXPIRING_WARNING  = m(2154, Level.WARNING, "Trusted certificate #{0} ({1}) will expire in {2}");
    public static final M CERT_EXPIRED           = m(2155, Level.WARNING, "Trusted certificate #{0} ({1}) expired {2} ago");
    public static final M CERT_EXPIRY_BAD_PERIOD = m(2156, Level.WARNING, "New expiry period value {0} is not a valid time unit; using {1} instead");

    // Audit Archiver
    public static final M AUDIT_ARCHIVER_MESSAGE_PROCESSING_SUSPENDED  = m(2200, Level.SEVERE, "Message processing suspended by the Audit Archiver: {0}");
    public static final M AUDIT_ARCHIVER_MESSAGE_PROCESSING_RESTARTED  = m(2201, Level.WARNING, "Message processing restarted by the Audit Archiver.");
    public static final M AUDIT_ARCHIVER_JOB_STARTED  = m(2202, Level.WARNING, "Started Audit Archiver job");
    public static final M AUDIT_ARCHIVER_JOB_ARCHIVED  = m(2203, Level.WARNING, "Archived and deleted audit records with time in [{0}:{1}]");
    public static final M AUDIT_ARCHIVER_JOB_COMPLETE  = m(2204, Level.WARNING, "Completed Audit Archiver job.");
    public static final M AUDIT_ARCHIVER_ERROR = m(2205, Level.WARNING, "Audit Archiver error: {0}");
    public static final M AUDIT_ARCHIVER_IMMEDIATE_TRIGGER  = m(2206, Level.WARNING, "Immediate Audit Archive trigger requested.");
    public static final M AUDIT_ARCHIVER_SOFT_LIMIT_REACHED  = m(2207, Level.WARNING, "Audit Archive current database size {0}% has reached and/or exceeded the soft limit of {1}%.");

    // FTP Client Utils
    public static final M FTP_SSL_NO_CERT = m(2250, Level.WARNING, "FTP server ({0}) did not identify itself properly: {1}");
    public static final M FTP_SSL_NOT_X509 = m(2251, Level.WARNING, "Cannot handle non-X.509 certificates from FTP server ({0})");
    public static final M FTP_SSL_UNTRUSTED = m(2252, Level.WARNING, "Cannot establish trust of SSL certificate from FTP server ({0}): {1}");

    // UDDI interaction
    public static final M UDDI_SUBSCRIPTION_NOTIFICATION_FAILED = m(2270, Level.WARNING, "Error processing subscription notification ''{0}''.");
    public static final M UDDI_SUBSCRIPTION_NOTIFICATION_BADKEY = m(2271, Level.WARNING, "Subscription key not recognized for notification ''{0}''.");
    public static final M UDDI_SUBSCRIPTION_POLL_FAILED         = m(2272, Level.WARNING, "Error polling subscription ''{0}''.");
    public static final M UDDI_SUBSCRIPTION_SUBSCRIBE_FAILED    = m(2273, Level.WARNING, "Error adding/renewing subscription ''{0}''.");
    public static final M UDDI_SUBSCRIPTION_UNSUBSCRIBE_FAILED  = m(2274, Level.WARNING, "Error removing subscription ''{0}''.");
    public static final M UDDI_METRICS_PUBLISH_FAILED           = m(2275, Level.WARNING, "Error publishing metrics ''{0}''.");
    public static final M UDDI_METRICS_PUBLISH_TMODEL_ERROR     = m(2276, Level.WARNING, "Error publishing metrics for service ''{0}'' ''{1}''.");
    public static final M UDDI_METRICS_CLEANUP_FAILED           = m(2277, Level.WARNING, "Error removing metrics ''{0}''.");
    public static final M UDDI_PUBLISH_UNEXPECTED_ERROR         = m(2278, Level.WARNING, "Unexpected error while publishing ''{0}''.");
    public static final M UDDI_PUBLISH_ENDPOINT_ROLLBACK_FAILED = m(2279, Level.WARNING, "Error rolling back publishing endpoint ''{0}'' ''{1}''.");
    public static final M UDDI_PUBLISH_REMOVE_ENDPOINT_BINDING  = m(2280, Level.WARNING, "Could not delete binding templates from Service with serviceKey ''{0}''. {1}");
    public static final M UDDI_PUBLISH_REMOVE_ENDPOINT_FAILED   = m(2281, Level.WARNING, "Error removing endpoint ''{0}''.");
    public static final M UDDI_PUBLISH_SERVICE_FAILED           = m(2282, Level.WARNING, "Error publishing service ''{0}''.");
    public static final M UDDI_PUBLISH_SERVICE_ROLLBACK_FAILED  = m(2283, Level.WARNING, "Error rolling back publishing service ''{0}''.");
    public static final M UDDI_PUBLISH_ENDPOINT_FAILED          = m(2284, Level.WARNING, "Error publishing endpoint ''{0}''.");
    public static final M UDDI_REMOVE_SERVICE_FAILED            = m(2285, Level.WARNING, "Could not delete proxied BusinessService ''{0}''.");
    public static final M UDDI_WSPOLICY_PUBLISH_FAILED          = m(2286, Level.WARNING, "Error publishing ws-policy attachment ''{0}''.");
    public static final M UDDI_NOTIFICATION_PROCESSING_FAILED   = m(2287, Level.WARNING, "Error processing UDDI notification: ''{0}''.");
    public static final M UDDI_NOTIFICATION_SERVICE_DISABLED    = m(2288, Level.WARNING, "UDDI Notification has caused Published Service to be disabled. Published Service ID ''{0}''.");
    public static final M UDDI_NOTIFICATION_SERVICE_DELETED     = m(2289, Level.WARNING, "UDDI Notification that monitored BusinessService has been deleted. Deleting Gateway records for serviceKey ''{0}''.");
    public static final M UDDI_MAINTENANCE_SERVICE_DELETED      = m(2290, Level.WARNING, "Error deleting record of proxied business service which has been deleted from UDDI Registry #({0}) with serviceKey ''{1}''");
    public static final M UDDI_NOTIFICATION_SERVICE_WSDL_UPDATE = m(2291, Level.INFO,    "Service WSDL updated from UDDI ''{0}''.");
    public static final M UDDI_NOTIFICATION_SERVICE_WSDL_ERROR  = m(2292, Level.WARNING, "Error updating service WSDL from UDDI ''{0}''.");
    public static final M UDDI_NOTIFICATION_ENDPOINT_NOT_FOUND  = m(2293, Level.WARNING, "Error finding endpoint for business service ''{0}'', wsdl:port ''{1}'', wsdl:binding ''{2}'' {3} for UDDI registry ''{4}''.");
    /*Audit message 2294 has been replaced by audit message 2298*/
    /** @deprecated */ @Deprecated public static final M UDDI_NOTIFICATION_ENDPOINT_UPDATED_OLD= m(2294, Level.INFO,    "Updated endpoint from UDDI ''{0}'' for business service ''{1}'', wsdl:port ''{2}'' for UDDI registry ''{3}''.");
    public static final M UDDI_NOTIFICATION_TRIGGERING_FAILED   = m(2295, Level.WARNING, "Error firing monitoring update events for UDDI Registry with id#({0}).");
    /*Audit message 2296 has been replaced by audit message 2299*/
    /** @deprecated */ @Deprecated public static final M UDDI_ORIGINAL_SERVICE_INVALIDATED_OLD = m(2296, Level.WARNING, "Original Business Service in UDDI can no longer be monitored. serviceKey: {0} UDDI Registry with id#({1}).");
    public static final M UDDI_GIF_SCHEME_NOT_AVAILABLE         = m(2297, Level.WARNING, "Service is configured to publish a GIF ''{0}'' endpoint which is no longer available on the Gateway. UDDI is now out of date. To fix either add / enable the listener or republish the GIF endpoint.");
    public static final M UDDI_NOTIFICATION_ENDPOINT_UPDATED    = m(2298, Level.INFO,    "Updated context variable service.defaultRoutingURL for published service #({0}) with updated endpoint from UDDI ''{1}'' for business service ''{2}'', wsdl:port ''{3}'' for UDDI registry ''{4}''.");
    public static final M UDDI_ORIGINAL_SERVICE_INVALIDATED     = m(2299, Level.WARNING, "Original Business Service ''{0}'' in UDDI Registry ''{1}'' is no longer eligible to be monitored. Published Service #({2}) can no longer be under UDDI Control.");

    // Caches
    public static final M URL_OBJECT_CACHE_REUSE = m( 2320, Level.WARNING, "Reusing previously-cached copy of remote {0}: URL {1}: {2}" );

    //audit-message-filter and audit-viewer policies
    public static final M AUDIT_MESSAGE_FILTER_POLICY_FAILED = m( 2360, Level.WARNING, "{0}. {1} message was not audited." );

    // Monitoring
    public static final M MONITOR_DB_REPLICATION_ERROR       = m( 2380, Level.WARNING, "Error accessing host/database {0}: {1}" );
    public static final M MONITOR_DB_REPLICATION_FAILED      = m( 2381, Level.WARNING, "Replication failing for host/database {0}: {1}" );
    public static final M MONITOR_DB_REPLICATION_RECOVERED   = m( 2382, Level.WARNING, "Replication recovered for host/database {0}" );

    // Connectors ( NOTE: see related list in AuditAwareConnectorTransportEvent )
    public static final M CONNECTOR_START = m(2400, Level.INFO, "Starting {0} listener: {1}");
    public static final M CONNECTOR_STOP  = m(2401, Level.INFO, "Stopping {0} listener: {1}");
    public static final M CONNECTOR_ERROR = m(2402, Level.WARNING, "{0} listener error: {1}");

    public static final M INVALID_CONTENT_TYPE = m(2420, Level.WARNING, "Cannot parse content-type value ''{0}'' from cluster property. Reason: {1}");
    public static final M INVALID_CUSTOM_DATE_FORMAT = m(2421, Level.WARNING, "Cannot parse custom date format ''{0}'' from cluster property. Reason: {1}");
    public static final M INVALID_AUTO_DATE_FORMAT = m(2422, Level.WARNING, "Cannot parse auto date format / pattern ''{0}'' from cluster property.");

    // License Management
    public static final M LICENSE_INSTALLED         = m(2500, Level.INFO, "License {0} installed.");
    public static final M LICENSE_REMOVED           = m(2501, Level.INFO, "License removed.");
    public static final M LICENSE_EXPIRED           = m(2502, Level.WARNING, "License {0} has expired.");
    public static final M LICENSE_NOT_YET_VALID     = m(2503, Level.WARNING, "License {0} is not yet valid.");
    public static final M LICENSE_INVALID_PRODUCT   = m(2504, Level.WARNING, "License {0} is not valid for this product.");
    public static final M LICENSE_INVALID_ISSUER    = m(2505, Level.WARNING, "License {0} is not signed by a trusted issuer.");

    // Service Debugger
    public static final M SERVICE_DEBUGGER_START    = m(2510, Level.FINE, "Starting Service Debugger for {0} ''{1}''.");
    public static final M SERVICE_DEBUGGER_STOP     = m(2511, Level.FINE, "Stopping Service Debugger for {0} ''{1}''.");

    // MAX -                                      m(2999
}
