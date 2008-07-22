/*
 * Copyright (C) 2007 Layer 7 Technologies, Inc.
 */

package com.l7tech.gateway.common.transport.jms;

/**
 * Constant values from TIBCO EMS Java API. Duplicated here so that the TIBCO
 * EMS Jar files do not have to be distributed with the Manager.
 *
 * See TIBCO EMS Java API and User Guide for meaning of each constant.
 *
 * @author rmak
 * @since SecureSpan 3.7
 */
public class TibcoEmsConstants {
    public static final String SSL = "ssl";

    /** Constants with same name as those in <code>com.tibco.tibjms.naming.TibjmsContext</code>. */
    public class TibjmsContext {
        public static final String SECURITY_PROTOCOL = "com.tibco.tibjms.naming.security_protocol";
        public static final String SSL_AUTH_ONLY = "com.tibco.tibjms.naming.ssl_auth_only";
        public static final String SSL_DEBUG_TRACE = "com.tibco.tibjms.naming.ssl_debug_trace";
        public static final String SSL_ENABLE_VERIFY_HOST = "com.tibco.tibjms.naming.ssl_enable_verify_host";
        public static final String SSL_ENABLE_VERIFY_HOST_NAME = "com.tibco.tibjms.naming.ssl_enable_verify_hostname";
        public static final String SSL_EXPECTED_HOST_NAME = "com.tibco.tibjms.naming.ssl_expected_hostname";
        public static final String SSL_IDENTITY = "com.tibco.tibjms.naming.ssl_identity";
        public static final String SSL_PASSWORD = "com.tibco.tibjms.naming.ssl_password";
        public static final String SSL_TRACE = "com.tibco.tibjms.naming.ssl_trace";
        public static final String SSL_TRUSTED_CERTIFICATES = "com.tibco.tibjms.naming.ssl_trusted_certs";
    }

    /** Constants with same name as those in <code>com.tibco.tibjms.naming.TibjmsSSL</code>. */
    public class TibjmsSSL {
        public static final String AUTH_ONLY = "com.tibco.tibjms.ssl.auth_only";
        public static final String DEBUG_TRACE = "com.tibco.tibjms.ssl.debug_trace";
        public static final String ENABLE_VERIFY_HOST = "com.tibco.tibjms.ssl.enable_verify_host";
        public static final String ENABLE_VERIFY_HOST_NAME = "com.tibco.tibjms.ssl.enable_verify_hostname";
        public static final String EXPECTED_HOST_NAME = "com.tibco.tibjms.ssl.expected_hostname";
        public static final String IDENTITY = "com.tibco.tibjms.ssl.identity";
        public static final String ISSUER_CERTIFICATES = "com.tibco.tibjms.ssl.issuer_certs";
        public static final String PASSWORD = "com.tibco.tibjms.ssl.password";
        public static final String PRIVATE_KEY = "com.tibco.tibjms.ssl.private_key";
        public static final String TRACE = "com.tibco.tibjms.ssl.trace";
        public static final String TRUSTED_CERTIFICATES = "com.tibco.tibjms.ssl.trusted_certs";
        public static final String VENDOR = "com.tibco.tibjms.ssl.vendor";
    }
}
