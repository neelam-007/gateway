package com.l7tech.server.tomcat;

import com.l7tech.common.io.SingleCertX509KeyManager;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.transport.http.HttpTransportModule;
import com.l7tech.util.ExceptionUtils;
import org.apache.tomcat.util.net.jsse.JSSESocketFactory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Extends Tomcat's JSSESocketFactory with the ability to get the private key from an SsgKeyStoreManager instance.
 */
public class SsgJSSESocketFactory extends JSSESocketFactory {
    protected static final Logger logger = Logger.getLogger(SsgJSSESocketFactory.class.getName());

    public static final String ATTR_CIPHERNAMES = "ciphers"; // comma separated list of enabled ciphers, ie TLS_RSA_WITH_AES_128_CBC_SHA
    public static final String ATTR_KEYSTOREOID = "keystoreOid"; // identifies a keystore available from SsgKeyStoreManager instead of one from disk
    public static final String ATTR_KEYALIAS = "keyAlias"; // alias of private key within the keystore

    private long transportModuleId = -1;
    private long connectorOid = -1;

    public SsgJSSESocketFactory() {
    }

    private Long getKeystoreOid() {
        Object value = attributes.get(ATTR_KEYSTOREOID);
        if (value instanceof String) {
            String s = (String)value;
            return Long.parseLong(s);
        } else if (value instanceof Long) {
            return (Long)value;
        } else {
            return null;
        }
    }

    private long getTransportModuleId() {
        synchronized (this) {
            if (transportModuleId != -1)
                return transportModuleId;
            Object instanceId = attributes.get(HttpTransportModule.CONNECTOR_ATTR_TRANSPORT_MODULE_ID);
            if (instanceId == null)
                return -1;
            return transportModuleId = Long.parseLong(instanceId.toString());
        }
    }

    private long getConnectorOid() {
        if (connectorOid != -1)
            return connectorOid;
        synchronized (this) {
            if (connectorOid != -1)
                return connectorOid;
            Object oid = attributes.get(HttpTransportModule.CONNECTOR_ATTR_CONNECTOR_OID);
            if (oid == null)
                return -1;
            return connectorOid = Long.parseLong(oid.toString());
        }
    }

    private HttpTransportModule getHttpTransportModule() {
        Object instanceId = attributes.get(HttpTransportModule.CONNECTOR_ATTR_TRANSPORT_MODULE_ID);
        if (instanceId == null) return null;
        return HttpTransportModule.getInstance(Long.parseLong(instanceId.toString()));
    }

    private SsgKeyStoreManager getSsgKeyStoreManager() {
        HttpTransportModule htm = getHttpTransportModule();
        return htm == null ? null : htm.getSsgKeyStoreManager();
    }

    protected KeyManager[] getKeyManagers(String keystoreType, String keystoreProvider, String algorithm, String keyAlias) throws Exception {
        Long keystoreOid = getKeystoreOid();
        if (keystoreOid == null)
            throw new IllegalStateException("No keystoreOid configured on SsgJSSESocketFactory");

        SsgKeyStoreManager ksm = getSsgKeyStoreManager();
        if (ksm == null)
            throw new IOException("Unable to create SSL socket -- a keystoreOid was specified, but no SsgKeyStoreManager instance was provided");
        SsgKeyEntry keyEntry = ksm.lookupKeyByKeyAlias(keyAlias, keystoreOid);
        X509Certificate[] certChain = keyEntry.getCertificateChain();
        PrivateKey privateKey = keyEntry.getPrivateKey();
        return new KeyManager[]{new SingleCertX509KeyManager(certChain, privateKey)};
    }

    @Override
    protected TrustManager[] getTrustManagers(String keystoreType, String keystoreProvider, String algorithm) throws Exception {
        HttpTransportModule htm = getHttpTransportModule();        
        ServerConfig serverConfig = htm == null ? ServerConfig.getInstance() : htm.getServerConfig();
        return new TrustManager[] {new ClientTrustingTrustManager(serverConfig) };
    }

    public Socket acceptSocket(ServerSocket socket) throws IOException {
        final long oid = getConnectorOid();
        try {
            return SsgServerSocketFactory.wrapSocket(getTransportModuleId(), oid, super.acceptSocket(socket));
        } catch (SocketException e) {
            // Check for connectors that are going to fail every time an attempt is made to start them (Bug #7553)
            if (e.getCause() == null && e.getMessage() != null &&
                    e.getMessage().contains("No available certificate or key corresponds to the SSL cipher suites which are enabled."))
            {
                synchronized (this) {
                    logger.log(Level.WARNING, "Marking connector " +  oid + " as disabled due to its cipher suite configuration: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                    HttpTransportModule.reportMisconfiguredConnector(getTransportModuleId(), oid);
                }
            }

            throw e;
        }
    }
}
