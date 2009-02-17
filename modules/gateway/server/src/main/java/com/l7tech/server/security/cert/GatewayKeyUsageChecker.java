package com.l7tech.server.security.cert;

import com.l7tech.security.cert.KeyUsageChecker;
import com.l7tech.security.cert.KeyUsagePolicy;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.util.ExceptionUtils;
import org.xml.sax.SAXException;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * KeyUsageChecker that knows how to initialize itself from ServerConfig/ClusterPropertyManager
 * and sets itself as the default KeyUsageChecker for non-Spring-aware components like Trogdor etc.
 */
public class GatewayKeyUsageChecker extends KeyUsageChecker {
    private static final Logger logger = Logger.getLogger(GatewayKeyUsageChecker.class.getName());

    @SuppressWarnings({"UnusedDeclaration"}) // We declare ClusterPropertyManager to ensure it's been hooked up to ServerConfig already
    public GatewayKeyUsageChecker(ServerConfig serverConfig, ClusterPropertyManager clusterPropertyManager) {
        super(getGatewayKeyUsagePolicy(serverConfig), serverConfig.getProperty(ServerConfig.PARAM_KEY_USAGE));
        KeyUsageChecker.setDefault(this);
    }

    private static KeyUsagePolicy getGatewayKeyUsagePolicy(ServerConfig serverConfig) {
        String policyXml = serverConfig.getProperty(ServerConfig.PARAM_KEY_USAGE_POLICY_XML);
        if (policyXml == null || policyXml.trim().length() < 1)
            return makeDefaultPolicy();

        try {
            return KeyUsagePolicy.fromXml(policyXml);
        } catch (SAXException e) {
            logger.log(Level.SEVERE, "Malformed Key Usage enforcement policy; will use default policy: " + ExceptionUtils.getMessage(e), e);
            return makeDefaultPolicy();
        }
    }
}
