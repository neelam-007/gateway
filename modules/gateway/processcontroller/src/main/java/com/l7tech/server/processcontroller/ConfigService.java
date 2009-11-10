/**
 * Copyright (C) 2008-2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller;

import com.l7tech.objectmodel.DeleteException;
import com.l7tech.server.management.config.host.HostConfig;
import com.l7tech.server.management.config.monitoring.MonitoringConfiguration;
import com.l7tech.server.management.config.node.NodeConfig;
import com.l7tech.util.Pair;

import java.io.File;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Set;

/** @author alex */
public interface ConfigService {
    String HOSTPROPERTIES_SSL_PORT = "host.controller.sslPort";
    String HOSTPROPERTIES_SSL_IPADDRESS = "host.controller.sslIpAddress";
    String HOSTPROPERTIES_SSL_KEYSTOREFILE = "host.controller.keystore.file";
    String HOSTPROPERTIES_SSL_KEYSTOREPASSWORD = "host.controller.keystore.password";
    String HOSTPROPERTIES_SSL_KEYSTORETYPE = "host.controller.keystore.type";
    String HOSTPROPERTIES_SSL_KEYSTOREALIAS = "host.controller.keystore.alias";
    String HOSTPROPERTIES_JRE = "host.jre";
    String HOSTPROPERTIES_NODEMANAGEMENT_ENABLED = "host.controller.remoteNodeManagement.enabled";
    String HOSTPROPERTIES_NODEMANAGEMENT_TRUSTSTORE_FILE = "host.controller.remoteNodeManagement.truststore.file";
    String HOSTPROPERTIES_NODEMANAGEMENT_TRUSTSTORE_TYPE = "host.controller.remoteNodeManagement.truststore.type";
    String HOSTPROPERTIES_NODEMANAGEMENT_TRUSTSTORE_PASSWORD = "host.controller.remoteNodeManagement.truststore.password";
    String HOSTPROPERTIES_PATCH_TRUSTSTORE_FILE = "host.controller.patch.truststore.file";
    String HOSTPROPERTIES_PATCH_TRUSTSTORE_TYPE = "host.controller.patch.truststore.type";
    String HOSTPROPERTIES_PATCH_TRUSTSTORE_PASSWORD = "host.controller.patch.truststore.password";
    String HOSTPROPERTIES_PATCHES_DIR = "host.patches.dir";
    String HOSTPROPERTIES_PATCHES_LOG = "host.patches.logfile";
    String HOSTPROPERTIES_TYPE = "host.type";
    String HOSTPROPERTIES_ID = "host.id";
    String HOSTPROPERTIES_SAMPLER_TIMEOUT_SLOW_CONNECT= "host.sampler.timeout.slow.connect";
    String HOSTPROPERTIES_SAMPLER_TIMEOUT_SLOW_READ = "host.sampler.timeout.slow.read";
    String HOSTPROPERTIES_SAMPLER_TIMEOUT_FAST_CONNECT= "host.sampler.timeout.fast.connect";
    String HOSTPROPERTIES_SAMPLER_TIMEOUT_FAST_READ = "host.sampler.timeout.fast.read";

    String DEFAULT_PATCHES_SUBDIR = "var/patches";
    String DEFAULT_PATCHES_LOGFILE = "var/logs/patches.log";
    String DEFAULT_PATCHES_CERT_FILENAME = "patchesCert.pem";
    String DEFAULT_PATCHES_CERT_ALIAS = "l7patchesCert";
    String DEFAULT_PATCH_TRUSTSTORE_FILENAME = "patches.jks";
    String DEFAULT_REMOTE_MANAGEMENT_TRUSTSTORE_FILENAME = "remoteNodeManagementTruststore.p12";

    public static final String PC_HOMEDIR_PROPERTY = "com.l7tech.server.processcontroller.homeDirectory";

    HostConfig getHost();

    void addServiceNode(NodeConfig node) throws IOException;

    void updateServiceNode(NodeConfig node) throws IOException;

    /** The parent directory of the node directories. */
    File getNodeBaseDirectory();

    /** Patch packages repository dir */
    File getPatchesDirectory();

    /** Patch log file */
    String getPatchesLog();

    /** The base context-path for services servlets. */
    String getServicesContextBasePath();

    /** The URL path for the API endpoint configured with the specified property name in processcontroller.properties. */
    String getApiEndpoint(ApiWebEndpoint endpoint);

    /** @return the directory containing scripts that get executed by appliance programs including process controller. */
    File getApplianceLibexecDirectory();

    /** The certificate chain and private key for the PC's SSL listener */
    Pair<X509Certificate[], PrivateKey> getSslKeypair();

    /** The PC's SSL port. */
    int getSslPort();

    /** The PC's SSL ip address. */
    String getSslIPAddress();

    /** The client certificates that will be accepted by the remote {@link com.l7tech.server.management.api.node.NodeManagementApi}. */
    Set<X509Certificate> getTrustedRemoteNodeManagementCerts();

    /** The certificates that are trusted to sign patches. */
    Set<X509Certificate> getTrustedPatchCerts();

    /** The $JAVA_HOME/bin/java launcher */
    File getJavaBinary();

    void deleteNode(String nodeName) throws DeleteException, IOException;

    /**
     * @return the monitoring configuration that is currently in effect, or null if there isn't one.
     */
    MonitoringConfiguration getCurrentMonitoringConfiguration();

    /**
     * Is this PC responsible for monitoring cluster-wide properties?
     * 
     * @return true if this PC node is responsible for monitoring cluster-wide properties; false if not.
     */
    boolean isResponsibleForClusterMonitoring();

    /**
     * Accept a new MonitoringConfiguration and notify the monitoring system about it
     * @param config the new configuration
     */
    void pushMonitoringConfiguration(MonitoringConfiguration config);

    /**
     * Check if the SCA is enabled on the default node.
     * TODO this should use the ScaFeature
     *
     * @return true if the SCA should be enabled on the default node.
     */
    boolean isUseSca();

    /**
     * Get an int value from host.properties.
     * 
     * @param propertyName name of the property to get.  Required.
     * @param defaultValue default value to use if property is not configured.  Required.
     * @return the property value (or default).
     */
    int getIntProperty(String propertyName, int defaultValue);

    /**
     * Get a boolean value from host.properties.
     *
     * @param propertyName name of the property to get. Required.
     * @param defaultValue default value to use if property is not configured or is invalid. Required.
     * @return The property value, or the default
     */
    boolean getBooleanProperty(String propertyName, boolean defaultValue);
}
