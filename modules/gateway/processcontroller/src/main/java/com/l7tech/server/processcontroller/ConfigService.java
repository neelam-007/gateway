/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller;

import com.l7tech.server.management.config.host.HostConfig;
import com.l7tech.server.management.config.node.NodeConfig;
import com.l7tech.util.Pair;

import java.io.File;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.Set;

/** @author alex */
public interface ConfigService {
    HostConfig getHost();

    void addServiceNode(NodeConfig node);

    void updateServiceNode(NodeConfig node);

    /** The parent directory of the node directories. */
    File getNodeBaseDirectory();

    /** The certificate chain and private key for the PC's SSL listener */
    Pair<X509Certificate[], RSAPrivateKey> getSslKeypair();

    /** The PC's SSL port. */
    int getSslPort();

    /** The client certificates that will be accepted by the remote {@link com.l7tech.server.management.api.node.NodeManagementApi}. */
    Set<X509Certificate> getTrustedRemoteNodeManagementCerts();

    /** The $JAVA_HOME/bin/java launcher */
    File getJavaBinary();
}
