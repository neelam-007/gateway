/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.audit;

import java.util.logging.Level;

/**
 * Message catalog for system componenets such as {@link com.l7tech.server.GatewayLicenseManager}.
 * The ID range 2000-2999 (inclusive) is reserved for these messages.
 */
public class SystemMessages extends Messages {
    public static final M DATABASE_ERROR                = m(2000, Level.WARNING, "Database error");
    public static final M DATABASE_ERROR_WITH_MORE_INFO = m(2001, Level.WARNING, "{0}. Database error");

    public static final M LICENSE_DB_ERROR_RETRY        = m(2010, Level.WARNING, "Database error reading license file.  Will keep current license and retry.");
    public static final M LICENSE_DB_ERROR_GAVEUP       = m(2011, Level.WARNING, "Database error reading license file.  Current license was too stale to keep.  Will keep trying.");
    public static final M LICENSE_NO_LICENSE            = m(2012, Level.WARNING, "No valid license is installed.  Some product features may be disabled.");
    public static final M LICENSE_FOUND                 = m(2013, Level.INFO,  "Valid license found");
    public static final M LICENSE_INVALID               = m(2014, Level.WARNING, "License file is not valid");
    public static final M LICENSE_UPDATED               = m(2015, Level.INFO, "License updated");

    public static final M SERVICE_WSDL_ERROR            = m(2016, Level.WARNING, "Service ''{0}'' WSDL error ''{1}''");

    public static final M SOCKET_TIMEOUT                = m(2017, Level.WARNING, "A remote network connection timed out.");

    public static final M FTPSERVER_NOT_CONFIGURED      = m(2020, Level.INFO, "Not starting FTP server (no listeners enabled).");
    public static final M FTPSERVER_START               = m(2021, Level.INFO, "Starting FTP server with listeners [''{0}''].");
    public static final M FTPSERVER_STOP                = m(2022, Level.INFO, "Stopping FTP server.");
    public static final M FTPSERVER_ERROR               = m(2023, Level.WARNING, "FTP server error ''{0}''.");

    // Used by CrlCacheImpl
    public static final M CERTVAL_CANT_FIND_ISSUER      = m(2030, Level.WARNING, "Unable to locate Trusted Cert Entry for issuer with DN \"{0}\" of certificate with DN \"{1}\"");
    public static final M CERTVAL_CHECKED               = m(2031, Level.FINE, "Certificate validated and verified");
    public static final M CERTVAL_REV_NOT_REVOKED       = m(2032, Level.FINE, "Cert {0} is not listed as revoked");
    public static final M CERTVAL_REV_REVOKED           = m(2033, Level.WARNING, "Cert {0} is listed as revoked");
    public static final M CERTVAL_CANT_BUILD_PATH       = m(2034, Level.WARNING, "Unable to build path for Certificate {0}: {1}");
    public static final M CERTVAL_INVALID_SETTING       = m(2035, Level.WARNING, "Invalid setting for validation level {0}: {1}");

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
    public static final M CERTVAL_REV_CACHE_STALE             = m(2050, Level.INFO,    "{0} cache for {1} was last updated at {2}; refreshing");
    public static final M CERTVAL_REV_CACHE_FRESH             = m(2051, Level.FINE,    "{0} cache for {1} was last updated at {2}; using cache");
    public static final M CERTVAL_REV_SIGNER_IS_ISSUER        = m(2052, Level.FINE,    "Using issuer ''{1}'' as {0} signer.");
    public static final M CERTVAL_REV_SIGNER_IS_ISSUER_DELE   = m(2053, Level.FINE,    "Using issuer authorized certificate ''{1}'' as {0} signer.");
    public static final M CERTVAL_REV_SIGNER_IS_TRUSTED       = m(2054, Level.FINE,    "Using trusted certificate ''{1}'' as {0} signer.");

    // CRL only
    public static final M CERTVAL_CERT_EXPIRED                = m(2070, Level.WARNING, "Certificate {0} has expired");
    public static final M CERTVAL_CERT_NOT_YET_VALID          = m(2071, Level.WARNING, "Certificate {0} is not yet valid");
    public static final M CERTVAL_CRL_SCOPE                   = m(2072, Level.FINE,    "CRL scope does not cover certificate ''{0}'', CRL URL is ''{1}''");
    public static final M CERTVAL_REV_CRL_INVALID             = m(2073, Level.WARNING, "CRL at {0} is invalid: {1}");

    // OCSP Only
    public static final M CERTVAL_OCSP_ERROR                  = m(2085, Level.WARNING, "Error during OCSP check for responder ''{0}'': {1}");
    public static final M CERTVAL_OCSP_SIGNER_CERT_REVOKED    = m(2086, Level.WARNING, "OCSP responder ''{0}'' signing certificate ''{1}'' is revoked.");

    public static final M AUTH_USER_DISABLED = m(2100, Level.INFO, "User {0} is disabled.");
    public static final M AUTH_USER_LOCKED   = m(2101, Level.INFO, "User {0} is locked.");
    public static final M AUTH_USER_EXPIRED  = m(2102, Level.INFO, "User {0} has expired.");

    // MAX -                                               m(2999
}
