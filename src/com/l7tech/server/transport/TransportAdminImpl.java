package com.l7tech.server.transport;

import com.l7tech.common.LicenseException;
import com.l7tech.common.transport.SsgConnector;
import com.l7tech.common.transport.TransportAdmin;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.policy.AssertionLicense;
import com.l7tech.server.GatewayFeatureSets;
import com.l7tech.server.KeystoreUtils;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import java.rmi.RemoteException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Collection;

/**
 * Server-side implementation of the TransportAdmin API.
 */
public class TransportAdminImpl implements TransportAdmin {
    private final AssertionLicense licenseManager;
    private final SsgConnectorManager connectorManager;
    private final KeystoreUtils defaultKeystore;

    public TransportAdminImpl(AssertionLicense licenseManager, SsgConnectorManager connectorManager, KeystoreUtils defaultKeystore) {
        this.licenseManager = licenseManager;
        this.connectorManager = connectorManager;
        this.defaultKeystore = defaultKeystore;
    }

    private void checkLicense() throws RemoteException {
        try {
            licenseManager.requireFeature(GatewayFeatureSets.SERVICE_ADMIN);
        } catch (LicenseException e) {
            // New exception to conceal original stack trace from LicenseManager
            throw new RemoteException(ExceptionUtils.getMessage(e), new LicenseException(e.getMessage()));
        }
    }

    public Collection<SsgConnector> findAllSsgConnectors() throws RemoteException, FindException {
        checkLicense();
        return connectorManager.findAll();
    }

    public SsgConnector findSsgConnectorByPrimaryKey(long oid) throws RemoteException, FindException {
        checkLicense();
        return connectorManager.findByPrimaryKey(oid);
    }

    /**
     * Check if the specified connector represents the current admin connection.
     *
     * @param oid  the oid of the connector to examine.
     * @return true if this apears to match the current thread's active admin connection
     */
    private boolean isCurrentAdminConnection(long oid) {
        // TODO!!!   find a way to get the current connector OID shoved into the thread local context for each admin call,
        //           and then check it here
        return false;
    }

    public long saveSsgConnector(SsgConnector connector) throws RemoteException, SaveException, UpdateException {
        checkLicense();
        if (isCurrentAdminConnection(connector.getOid()))
            throw new UpdateException("Unable to modify connector for current admin connection");
        if (connector.getOid() == SsgConnector.DEFAULT_OID) {
            return connectorManager.save(connector);
        } else {
            connectorManager.update(connector);
            return connector.getOid();
        }
    }

    public void deleteSsgConnector(long oid) throws RemoteException, DeleteException, FindException {
        checkLicense();
        if (isCurrentAdminConnection(oid))
            throw new DeleteException("Unable to delete connector for current admin connection");
        connectorManager.delete(oid);
    }

    private SSLContext getSslContext() throws RemoteException {
        SSLContext context;
        try {
            final KeyManager[] keyManagers;
            keyManagers = defaultKeystore.getSSLKeyManagers();
            context = SSLContext.getInstance("SSL");
            context.init(keyManagers, null, null);
        } catch (NoSuchAlgorithmException e) {
            throw new RemoteException(ExceptionUtils.getMessage(e));
        } catch (KeyManagementException e) {
            throw new RemoteException(ExceptionUtils.getMessage(e));
        } catch (KeyStoreException e) {
            throw new RemoteException(ExceptionUtils.getMessage(e));
        } catch (UnrecoverableKeyException e) {
            throw new RemoteException(ExceptionUtils.getMessage(e));
        }
        return context;
    }

    public String[] getAllCipherSuiteNames() throws RemoteException {
        return getSslContext().getSupportedSSLParameters().getCipherSuites();
    }

    public String[] getDefaultCipherSuiteNames() throws RemoteException {
        return getSslContext().getDefaultSSLParameters().getCipherSuites();
    }
}
