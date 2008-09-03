package com.l7tech.server.transport;

import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.transport.TransportAdmin;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.tomcat.ConnectionIdValve;
import com.l7tech.util.ExceptionUtils;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

/**
 * Server-side implementation of the TransportAdmin API.
 */
public class TransportAdminImpl implements TransportAdmin {
    private final SsgConnectorManager connectorManager;
    private final DefaultKey defaultKeystore;

    public TransportAdminImpl(SsgConnectorManager connectorManager, DefaultKey defaultKeystore) {
        this.connectorManager = connectorManager;
        this.defaultKeystore = defaultKeystore;
    }

    public Collection<SsgConnector> findAllSsgConnectors() throws FindException {
        return connectorManager.findAll();
    }

    public SsgConnector findSsgConnectorByPrimaryKey(long oid) throws FindException {
        return connectorManager.findByPrimaryKey(oid);
    }

    /**
     * Check if the specified connector represents the current admin connection.
     *
     * @param oid  the oid of the connector to examine.
     * @return true if this apears to match the current thread's active admin connection
     */
    private boolean isCurrentAdminConnection(long oid) {
        long currentConnection = ConnectionIdValve.getConnectorOid();
        return oid == currentConnection;
    }

    public long saveSsgConnector(SsgConnector connector) throws SaveException, UpdateException, CurrentAdminConnectionException {
        if (isCurrentAdminConnection(connector.getOid()))
            throw new CurrentAdminConnectionException("Unable to modify connector for current admin connection");
        if (connector.getOid() == SsgConnector.DEFAULT_OID) {
            return connectorManager.save(connector);
        } else {
            connectorManager.update(connector);
            return connector.getOid();
        }
    }

    public void deleteSsgConnector(long oid) throws DeleteException, FindException, CurrentAdminConnectionException {
        if (isCurrentAdminConnection(oid))
            throw new CurrentAdminConnectionException("Unable to delete connector for current admin connection");
        connectorManager.delete(oid);
    }

    private SSLContext getSslContext(){
        SSLContext context;
        try {
            final KeyManager[] keyManagers;
            keyManagers = defaultKeystore.getSslKeyManagers();
            context = SSLContext.getInstance("SSL");
            context.init(keyManagers, null, null);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(ExceptionUtils.getMessage(e));
        } catch (KeyManagementException e) {
            throw new RuntimeException(ExceptionUtils.getMessage(e));
        }
        return context;
    }

    public String[] getAllCipherSuiteNames() {
        return getSslContext().getSupportedSSLParameters().getCipherSuites();
    }

    public String[] getDefaultCipherSuiteNames() {
        return getSslContext().getDefaultSSLParameters().getCipherSuites();
    }

    public InetAddress[] getAvailableBindAddresses() {
        try {
            List<InetAddress> ret = new ArrayList<InetAddress>();
            Enumeration<NetworkInterface> faces = NetworkInterface.getNetworkInterfaces();
            while (faces.hasMoreElements()) {
                NetworkInterface face = faces.nextElement();
                Enumeration<InetAddress> addrs = face.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    ret.add(addrs.nextElement());
                }
            }
            return ret.toArray(new InetAddress[ret.size()]);
        } catch (SocketException e) {
            throw new RuntimeException("Unable to get network interfaces: " + ExceptionUtils.getMessage(e), e);
        }
    }

}
