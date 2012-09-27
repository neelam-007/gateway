package com.l7tech.server;

/**
 * Parameter name constants for use with ServerConfig.
 */
public interface ServerConfigParams {
    String PARAM_LDAP_TEMPLATES = "ldapTemplatesPath";
    String PARAM_UDDI_TEMPLATES = "uddiTemplatesPath";
    String PARAM_HOSTNAME = "hostname";
    String PARAM_HTTPPORT = "httpPort";
    String PARAM_HTTPSPORT = "httpsPort";
    String PARAM_HTTP_SESSION_NAME = "httpSessionName";
    String PARAM_SYSTEMPROPS = "systemPropertiesPath";
    String PARAM_MULTICAST_ADDRESS = "multicastAddress";
    String PARAM_MULTICAST_ENABLED = "cluster.replayProtection.multicast.enabled";
    String PARAM_SSG_HOME_DIRECTORY = "ssgHome";
    String PARAM_SSG_APPLIANCE_DIRECTORY = "ssgAppliance";
    String PARAM_CONFIG_DIRECTORY = "configDirectory";
    String PARAM_VAR_DIRECTORY = "varDirectory";
    String PARAM_WEB_DIRECTORY = "webDirectory";
    String PARAM_SSG_LOG_DIRECTORY = "logDirectory";
    String PARAM_SSG_LOG_FILE_PATTERN_TEMPLATE = "logFileTemplate";
    String PARAM_ATTACHMENT_DIRECTORY = "attachmentDirectory";
    String PARAM_ATTACHMENT_DISK_THRESHOLD = "attachmentDiskThreshold";
    String PARAM_MESSAGECACHE_DIRECTORY = "messageCacheDirectory";
    String PARAM_MESSAGECACHE_RESETGENERATION = "messageCacheResetGeneration";
    String PARAM_MESSAGECACHE_DISK_THRESHOLD = "messageCacheDiskThreshold";
    String PARAM_MODULAR_ASSERTIONS_DIRECTORY = "modularAssertionsDirectory";
    String PARAM_MODULAR_ASSERTIONS_RESCAN_MILLIS = "modularAssertionsRescanMillis";
    String PARAM_MODULAR_ASSERTIONS_FILE_EXTENSIONS = "modularAssertionsFileExtensions";
    String PARAM_TRACE_POLICY_GUID = "trace.policy.guid"; // serverconfig name must be same as cluster property name for this property
    String PARAM_AUDIT_SINK_POLICY_GUID = "audit.sink.policy.guid";
    String PARAM_AUDIT_LOOKUP_POLICY_GUID = "audit.lookup.policy.guid";
    String PARAM_AUDIT_SINK_FALLBACK_ON_FAIL = "audit.sink.fallbackToInternal";
    String PARAM_AUDIT_SINK_ALWAYS_FALLBACK = "audit.sink.alwaysSaveInternal";
    String PARAM_AUDIT_MESSAGE_THRESHOLD = "auditMessageThreshold";
    String PARAM_AUDIT_ADMIN_THRESHOLD = "auditAdminThreshold";
    String PARAM_AUDIT_SYSTEM_CLIENT_THRESHOLD = "auditClientSystemLogsThreshold";
    String PARAM_AUDIT_PURGE_MINIMUM_AGE = "auditPurgeMinimumAge";
    String PARAM_AUDIT_REFRESH_PERIOD_SECS = "auditViewerRefreshSeconds";
    String PARAM_AUDIT_HINTING_ENABLED = "auditHintingEnabled";
    String PARAM_AUDIT_ASSERTION_STATUS_ENABLED = "auditAssertionStatusEnabled";
    String PARAM_AUDIT_ASSOCIATED_LOGS_THRESHOLD = "auditAssociatedLogsThreshold";
    String PARAM_AUDIT_USE_ASSOCIATED_LOGS_THRESHOLD = "auditAssociatedLogsThresholdRespected";
    String PARAM_AUDIT_LOG_FORMAT_SERVICE_HEADER = "auditLogFormatServiceHeader";
    String PARAM_AUDIT_LOG_FORMAT_SERVICE_FOOTER = "auditLogFormatServiceFooter";
    String PARAM_AUDIT_LOG_FORMAT_SERVICE_DETAIL = "auditLogFormatServiceDetail";
    String PARAM_AUDIT_LOG_FORMAT_OTHER = "auditLogFormatOther";
    String PARAM_AUDIT_LOG_FORMAT_OTHER_DETAIL = "auditLogFormatOtherDetail";
    String PARAM_AUDIT_ARCHIVER_TIMER_PERIOD = "auditArchiverTimerPeriod";
    String PARAM_AUDIT_ARCHIVER_SHUTDOWN_THRESHOLD = "auditArchiverShutdownThreshold";
    String PARAM_AUDIT_ARCHIVER_WARNING_THRESHOLD = "auditArchiverWarningThreshold";
    String PARAM_AUDIT_ARCHIVER_START_THRESHOLD = "auditArchiverStartThreshold";
    String PARAM_AUDIT_ARCHIVER_STOP_THRESHOLD = "auditArchiverStopThreshold";
    String PARAM_AUDIT_ARCHIVER_STALE_TIMEOUT = "auditArchiverStaleTimeout";
    String PARAM_AUDIT_ARCHIVER_BATCH_SIZE = "auditArchiverBatchSize";
    String PARAM_AUDIT_ARCHIVER_IN_PROGRESS = "audit.archiverInProgress";
    String PARAM_AUDIT_ARCHIVER_FTP_DESTINATION = "audit.archiver.ftp.config";
    String PARAM_AUDIT_ARCHIVER_FTP_FILEPREFIX = "auditArchiverFtpFileprefix";
    String PARAM_AUDIT_ARCHIVER_FTP_MAX_UPLOAD_FILE_SIZE = "auditArchiverFtpMaxUploadFileSize";
    String PARAM_CONFIG_AUDIT_SIGN_CLUSTER = "audit.signing";
    String PARAM_AUDIT_SIGN_MAX_VALIDATE = "audit.validateSignature.maxrecords";
    String PARAM_AUDIT_SEARCH_MAX_MESSAGE_SIZE = "audit.search.maxMessageSize";
    String PARAM_METRICS_FINE_INTERVAL = "metricsFineInterval";
    String PARAM_IO_FRONT_BLOCKED_READ_TIMEOUT = "ioInReadTimeout";
    String PARAM_IO_FRONT_SLOW_READ_THRESHOLD = "ioInSlowReadThreshold";
    String PARAM_IO_FRONT_SLOW_READ_RATE = "ioInSlowReadRate";
    String PARAM_IO_BACK_CONNECTION_TIMEOUT = "ioOutConnectionTimeout";
    String PARAM_IO_BACK_READ_TIMEOUT = "ioOutReadTimeout";
    String PARAM_IO_BACK_HTTPS_HOST_CHECK = "ioHttpsHostVerify";
    String PARAM_IO_HOST_ALLOW_WILDCARD = "ioHttpsHostAllowWildcard";
    String PARAM_IO_STALE_CHECK_PER_INTERVAL = "ioStaleCheckCount";
    String PARAM_IO_STALE_MAX_HOSTS = "ioStaleCheckHosts";
    String PARAM_IO_FIRST_PART_MAX_BYTES = "ioXmlPartMaxBytes";
    String PARAM_SIGNED_PART_MAX_BYTES = "ioAttachmentSignedMaxBytes";
    String PARAM_XSLT_CACHE_MAX_ENTRIES = "xsltMaxCacheEntries";
    String PARAM_XSLT_CACHE_MAX_AGE = "xsltMaxCacheAge";
    String PARAM_XSLT_CACHE_MAX_STALE_AGE = "xsltMaxStaleCacheAge";
    String PARAM_SCHEMA_CACHE_MAX_ENTRIES = "schemaMaxCacheEntries";
    String PARAM_SCHEMA_CACHE_MAX_AGE = "schemaMaxCacheAge";
    String PARAM_SCHEMA_CACHE_MAX_STALE_AGE = "schemaMaxStaleCacheAge";
    String PARAM_SCHEMA_CACHE_HARDWARE_RECOMPILE_LATENCY = "schemaRecompileLatency";
    String PARAM_SCHEMA_CACHE_HARDWARE_RECOMPILE_MIN_AGE = "schemaRecompileMinAge";
    String PARAM_SCHEMA_CACHE_HARDWARE_RECOMPILE_MAX_AGE = "schemaRecompileMaxAge";
    String PARAM_SCHEMA_CACHE_MAX_SCHEMA_SIZE = "schemaCacheMaxSchemaSize";
    String PARAM_SCHEMA_REMOTE_URL_REGEX = "schema.remoteResourceRegex";
    String PARAM_EPHEMERAL_KEY_CACHE_MAX_ENTRIES = "ephemeralKeyMaxCacheEntries";
    String PARAM_RSA_SIGNATURE_CACHE_MAX_ENTRIES = "rsaSignatureCacheMaxEntries";
    String PARAM_SCHEMA_SOFTWARE_FALLBACK = "schemaSoftwareFallback";
    String PARAM_SCHEMA_ALLOW_DOCTYPE = "schema.allowDoctype";
    String PARAM_KEYSTORE_SEARCH_FOR_ALIAS = "keyStoreSearchForAlias";
    String PARAM_KEYSTORE_DEFAULT_SSL_KEY = "keyStoreDefaultSslKey";
    String PARAM_KEYSTORE_DEFAULT_CA_KEY = "keyStoreDefaultCaKey";
    String PARAM_KEYSTORE_AUDIT_VIEWER_KEY = "keyStoreAuditViewerKey";
    String PARAM_KEYSTORE_AUDIT_SIGNING_KEY = "keyStoreAuditSigningKey";
    String PARAM_CERT_EXPIRY_CHECK_PERIOD = "trustedCert.expiryCheckPeriod";
    String PARAM_CERT_EXPIRY_FINE_AGE = "trustedCert.expiryFineAge";
    String PARAM_CERT_EXPIRY_INFO_AGE = "trustedCert.expiryInfoAge";
    String PARAM_CERT_EXPIRY_WARNING_AGE = "trustedCert.expiryWarningAge";
    String PARAM_CLUSTER_PORT = "clusterNodePort";
    String PARAM_CLUSTER_ADMIN_APPLET_PORT = "clusterAdminAppletPort";
    String PARAM_IO_HTTP_POOL_MAX_CONCURRENCY = "ioHttpPoolMaxConcurrency";
    String PARAM_IO_HTTP_POOL_MAX_IDLE_TIME = "ioHttpPoolMaxIdleTime";
    String PARAM_IO_HTTP_POOL_MIN_SPARE_THREADS = "ioHttpPoolMinSpareThreads";
    String PARAM_POLICY_VALIDATION_MAX_CONCURRENCY = "serverPolicyValidation.maxConcurrency";
    String PARAM_POLICY_VERSIONING_MAX_REVISIONS = "policyVersioningMaxRevisions";
    String PARAM_TEMPLATE_STRICTMODE = "template.strictMode";
    String PARAM_TEMPLATE_MULTIVALUE_DELIMITER = "template.defaultMultivalueDelimiter";
    String PARAM_SOAP_REJECT_MUST_UNDERSTAND = "soapRejectMustUnderstand";
    String PARAM_AUTH_CACHE_SUCCESS_CACHE_SIZE = "authCacheSuccessCacheSize";
    String PARAM_AUTH_CACHE_FAILURE_CACHE_SIZE = "authCacheFailureCacheSize";
    String PARAM_AUTH_CACHE_MAX_SUCCESS_TIME = "authCacheMaxSuccessTime";
    String PARAM_AUTH_CACHE_MAX_FAILURE_TIME = "authCacheMaxFailureTime";
    String PARAM_AUTH_CACHE_GROUP_MEMB_CACHE_SIZE = "authCacheGroupMembershipCacheSize";
    String PARAM_PRINCIPAL_SESSION_CACHE_SIZE = "principalSessionCacheSize";
    String PARAM_PRINCIPAL_SESSION_CACHE_MAX_TIME = "principalSessionCacheMaxTime";
    String PARAM_PRINCIPAL_SESSION_CACHE_MAX_PRINCIPAL_GROUPS = "principalSessionCacheMaxPrincipalGroups";
    String PARAM_JMS_RESPONSE_TIMEOUT = "ioJmsResponseTimeout";
    String PARAM_JMS_MESSAGE_MAX_BYTES = "ioJmsMessageMaxBytes";
    String PARAM_PROCESS_CONTROLLER_PRESENT = "processControllerPresent";
    String PARAM_PROCESS_CONTROLLER_PORT = "processControllerPort";
    String PARAM_PROCESS_CONTROLLER_EXTERNAL_PORT = "processControllerExternalPort";
    String PARAM_ADD_MAPPINGS_INTO_AUDIT = "customerMapping.addToGatewayAuditEvents";
    String PARAM_ADD_MAPPINGS_INTO_SERVICE_METRICS = "customerMapping.addToServiceMetrics";
    String PARAM_CERTIFICATE_DISCOVERY_ENABLED = "services.certificateDiscoveryEnabled";
    String PARAM_SNMP_QUERY_SERVICE_ENABLED = "builtinService.snmpQuery.enabled";
    String PARAM_EMAIL_LISTENER_CONNECTION_TIMEOUT = "ioMailInConnectTimeout";
    String PARAM_EMAIL_LISTENER_TIMEOUT = "ioMailInTimeout";
    String PARAM_EMAIL_MESSAGE_MAX_BYTES= "ioEmailMessageMaxBytes";
    String PARAM_LOG_DIRECTORY = "logDirectory";
    String PARAM_MAX_LDAP_SEARCH_RESULT_SIZE = "maxLdapSearchResultSize";
    String PARAM_MAX_LDAP_GROUP_SEARCH_RESULT_SIZE = "ldap.group.searchMaxResults";
    String PARAM_MAX_LOGIN_ATTEMPTS_ALLOW = "logon.maxAllowableAttempts";
    String PARAM_MAX_LOCKOUT_TIME = "logon.lockoutTime";
    String PARAM_SESSION_EXPIRY = "logon.sessionExpiry";
    String PARAM_INACTIVITY_PERIOD = "logon.inactivityPeriod";
    String PARAM_KERBEROS_CONFIG_REALM = "krb5Realm";
    String PARAM_KERBEROS_CONFIG_KDC = "krb5KDC";
    String PARAM_LDAPCERTINDEX_REBUILD_INTERVAL="ldapCertIndexInterval";
    String PARAM_LDAPCERT_CACHE_LIFETIME="ldapCertCacheLifetime";
    String PARAM_LDAP_COMPARISON_CASE_INSENSITIVE="ldapCaseInsensitiveComparison";
    String PARAM_LDAP_CONNECTION_TIMEOUT = "ldapConnectionTimeout";
    String PARAM_LDAP_READ_TIMEOUT = "ldapReadTimeout";
    String PARAM_LDAP_REFERRAL = "ldapReferral";
    String PARAM_LDAP_IGNORE_PARTIAL_RESULTS = "ldapIgnorePartialResultException";
    String PARAM_TIMESTAMP_CREATED_FUTURE_GRACE = "timestampCreatedFutureGrace";
    String PARAM_TIMESTAMP_EXPIRES_PAST_GRACE = "timestampExpiresPastGrace";
    String PARAM_KEY_USAGE = "pkix.keyUsage";
    String PARAM_KEY_USAGE_POLICY_XML = "pkix.keyUsagePolicy";
    String PARAM_FIPS = "security.fips.enabled";
    String PARAM_PCIDSS_ENABLED= "security.pcidss.enabled";
    String PARAM_WSS_ALLOW_MULTIPLE_TIMESTAMP_SIGNATURES = "wss.processor.allowMultipleTimestampSignatures";
    String PARAM_WSS_ALLOW_UNKNOWN_BINARY_SECURITY_TOKENS = "wss.processor.allowUnknownBinarySecurityTokens";
    String PARAM_WSS_DECORATOR_MUSTUNDERSTAND = "wss.decorator.mustUnderstand";
    String PARAM_WSS_PROCESSOR_STRICT_SIG_CONFIRMATION = "wss.processor.strictSignatureConfirmationValidation";
    String PARAM_WSS_PROCESSOR_LAZY_REQUEST = "wss.processor.enableDeferredRequestProcessing";
    String PARAM_LOGON_WARNING_BANNER = "logon.warningBanner";
    String PARAM_JDBC_CONNECTION_DEFAULT_DRIVERCLASS_LIST = "jdbcConnection.driverClass.defaultList";
    String PARAM_JDBC_QUERY_MAXRECORDS_DEFAULT = "jdbcQuery.maxRecords.defaultValue";
    String PARAM_JDBC_CONNECTION_POOLING_DEFAULT_MINPOOLSIZE = "jdbcConnection.pooling.minPoolSize.defaultValue";
    String PARAM_JDBC_CONNECTION_POOLING_DEFAULT_MAXPOOLSIZE = "jdbcConnection.pooling.maxPoolSize.defaultValue";
    String PARAM_SAML_VALIDATE_BEFORE_OFFSET_MINUTES = "samlValidateBeforeOffsetMinutes";
    String PARAM_SAML_VALIDATE_AFTER_OFFSET_MINUTES = "samlValidateAfterOffsetMinutes";
    String PARAM_DOCUMENT_DOWNLOAD_MAXSIZE = "documentDownload.maxSize";
    String PARAM_WSDL_MAX_DOWNLOAD_SIZE = "wsdlDocMaxDownloadSize";
    String PARAM_XSL_MAX_DOWNLOAD_SIZE = "xslDocMaxDownloadSize";
    String PARAM_OTHER_TEXTUAL_CONTENT_TYPES = "otherTextualContentTypes";
    String PARAM_AUDIT_EXPORT_GROUP_CONCAT_MAX_LEN = "audit.export.group_concat_max_len";
    String PARAM_AUDIT_MESSAGE_LIMIT_SIZE = "audit.messageSizeLimit";
    String PARAM_AUDIT_LOOKUP_CACHE_MESSAGE_LIMIT_SIZE = "audit.lookup.cache.messageSizeLimit";
    String PARAM_MAX_FOLDER_DEPTH = "policyorganization.maxFolderDepth";
    String PARAM_BIND_ONLY_LDAP_USERNAME_PATTERN = "ldapSimpleUsernamePattern";
    String PARAM_DEBUG_SSL = "ioDebugSsl";
    String PARAM_DEBUG_SSL_VALUE = "ioDebugSslValue";
    String PARAM_LOG_STDOUT_LEVEL = "logStdOutLevel";
    String PARAM_LOG_STDERR_LEVEL = "logStdErrLevel";
    String PARAM_IO_HTTP_RESPONSE_STREAM_UNLIMITED = "ioHttpResponseStreamUnlimited";
    String PARAM_IO_HTTP_RESPONSE_STREAMING = "ioHttpResponseStreaming";
    String PARAM_IO_JMS_MESSAGE_MAX_BYTES = "ioJmsMessageMaxBytes";
    String PARAM_IO_MQ_MESSAGE_MAX_BYTES = "ioMqMessageMaxBytes";
    String PARAM_DATE_TIME_CUSTOM_FORMATS = "datetime.customFormats";
    String PARAM_DATE_TIME_AUTO_FORMATS = "datetime.autoFormats";
}