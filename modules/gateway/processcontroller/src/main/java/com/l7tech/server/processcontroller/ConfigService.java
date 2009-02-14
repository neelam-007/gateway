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
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
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
    String HOSTPROPERTIES_NODEMANAGEMENTTRUSTSTORE_FILE = "host.controller.remoteNodeManagement.truststore.file";
    String HOSTPROPERTIES_NODEMANAGEMENTTRUSTSTORE_TYPE = "host.controller.remoteNodeManagement.truststore.type";
    String HOSTPROPERTIES_NODEMANAGEMENTTRUSTSTORE_PASSWORD = "host.controller.remoteNodeManagement.truststore.password";
    String HOSTPROPERTIES_TYPE = "host.type";
    String HOSTPROPERTIES_ID = "host.id";

    String DEFAULT_REMOTE_MANAGEMENT_TRUSTSTORE_FILENAME = "remoteNodeManagementTruststore.p12";

    HostConfig getHost();

    void addServiceNode(NodeConfig node);

    void updateServiceNode(NodeConfig node);

    /** The parent directory of the node directories. */
    File getNodeBaseDirectory();

    /** The certificate chain and private key for the PC's SSL listener */
    Pair<X509Certificate[], RSAPrivateKey> getSslKeypair();

    /** The PC's SSL port. */
    int getSslPort();

    /** The PC's SSL ip address. */
    String getSslIPAddress();

    /** The client certificates that will be accepted by the remote {@link com.l7tech.server.management.api.node.NodeManagementApi}. */
    Set<X509Certificate> getTrustedRemoteNodeManagementCerts();

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
}
