package com.l7tech.service.ws;

import com.l7tech.service.PublishedService;
import com.l7tech.service.ServiceStatistics;
import com.l7tech.service.ServiceAdmin;
import com.l7tech.service.ServiceManager;
import com.l7tech.objectmodel.*;
import com.l7tech.common.util.Locator;
import com.l7tech.logging.LogManager;
import com.l7tech.server.MessageProcessor;

import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.rmi.RemoteException;
import java.net.URL;
import java.net.URLConnection;
import java.io.InputStream;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 6, 2003
 *
 */
public class ServiceAdminImpl implements ServiceAdmin {

    public static final String SERVICE_DEPENDENT_URL_PORTION = "/services/serviceAdmin";

    public ServiceAdminImpl() {
        logger = LogManager.getInstance().getSystemLogger();
    }

    public String resolveWsdlTarget(String url) throws RemoteException {
        try {
            URL urltarget = new URL(url);
            URLConnection connection = urltarget.openConnection();
            InputStream in = connection.getInputStream();
            byte[] buffer = new byte[2048];
            int read = in.read(buffer);
            StringBuffer out = new StringBuffer();
            while (read > 0) {
                out.append(new String(buffer, 0, read));
                read = in.read(buffer);
            }
            return out.toString();
        } catch (IOException e) {
            String msg = "Cannot resolve WSDL target " + url;
            logger.log(Level.WARNING, msg, e);
            throw new RemoteException(msg + e.getMessage(), e);
        }
    }

    public PublishedService findServiceByPrimaryKey(long oid) throws RemoteException, FindException {
        try {
            return getServiceManager().findByPrimaryKey(oid);
        } finally {
            closeContext();
        }
    }

    public EntityHeader[] findAllPublishedServices() throws RemoteException, FindException {
        try {
            Collection res = getServiceManager().findAllHeaders();
            return collectionToHeaderArray(res);
        } finally {
            closeContext();
        }
    }

    public EntityHeader[] findAllPublishedServicesByOffset(int offset, int windowSize)
                    throws RemoteException, FindException {
        try {
            Collection res = getServiceManager().findAllHeaders(offset, windowSize);
            return collectionToHeaderArray(res);
        } finally {
            closeContext();
        }
    }

    public ServiceStatistics getStatistics( long serviceOid ) throws RemoteException {
        MessageProcessor proc = MessageProcessor.getInstance();
        return proc.getServiceStatistics( serviceOid );
    }

    /**
     * this save method handles both save and updates.
     * the distinction happens on the server side by inspecting the oid of the object
     * if the oid appears to be "virgin" a save is invoked, otherwise an update call is made.
     * @param service the object to be saved or upodated
     * @throws RemoteException
     */
    public long savePublishedService(PublishedService service) throws RemoteException,
                                    UpdateException, SaveException, VersionException {
        beginTransaction();
        try {
            // does that object have a history?
            if (service.getOid() > 0) {
                com.l7tech.service.ServiceManager manager = getServiceManager();
                manager.update(service);
                return service.getOid();
            } else { // ... or is it a new object
                logger.info("Saving PublishedService: " + service.getOid());
                return getServiceManager().save(service);
            }
        } finally {
            endTransaction();
        }
    }

    public void deletePublishedService(long oid) throws RemoteException, DeleteException {
        beginTransaction();
        try {
            com.l7tech.service.ServiceManager theManagerDude = getServiceManager();
            PublishedService theExecutionee = theManagerDude.findByPrimaryKey(oid);
            theManagerDude.delete(theExecutionee);
            logger.info("Deleted PublishedService: " + oid);
        } catch (FindException e) {
            throw new DeleteException("Could not find object to delete.", e);
        } finally {
            endTransaction();
        }
    }

    public Map serviceMap() throws RemoteException {
        return serviceManagerInstance.serviceMap();
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    private ServiceManager getServiceManager() throws RemoteException {
        if (serviceManagerInstance == null) {
            initialiseServiceManager();
        }
        return serviceManagerInstance;
    }

    private void beginTransaction() throws RemoteException {
        try {
            PersistenceContext.getCurrent().beginTransaction();
        } catch (TransactionException e) {
            String msg = "cannot begin transaction.";
            throw new RemoteException(msg);
        } catch (SQLException e) {
            String msg = "cannot begin transaction.";
            throw new RemoteException(msg);
        }
    }

    private void closeContext() {
        try {
            PersistenceContext.getCurrent().close();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "error closing context", e);
        }
    }

    private void endTransaction() {
        try {
            PersistenceContext.getCurrent().flush();
            PersistenceContext.getCurrent().close();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Exception commiting", e);
        } catch (TransactionException e) {
            logger.log(Level.WARNING, "Exception commiting", e);
        } catch (ObjectModelException e) {
            logger.log(Level.WARNING, "Exception commiting", e);
        }
    }

    private EntityHeader[] collectionToHeaderArray(Collection input) throws RemoteException {
        if (input == null) return new EntityHeader[0];
        EntityHeader[] output = new EntityHeader[input.size()];
        int count = 0;
        java.util.Iterator i = input.iterator();
        while (i.hasNext()) {
            try {
                output[count] = (EntityHeader)i.next();
            } catch (ClassCastException e) {
                logger.log(Level.SEVERE, null, e);
                throw new RemoteException("Collection contained something other than a EntityHeader", e);
            }
            ++count;
        }
        return output;
    }

    private synchronized void initialiseServiceManager() throws RemoteException {
        serviceManagerInstance = (com.l7tech.service.ServiceManager)Locator.getDefault().
                                    lookup(com.l7tech.service.ServiceManager.class);
        if (serviceManagerInstance == null) {
            throw new RemoteException("Cannot instantiate the ServiceManager");
        }
    }

    private ServiceManager serviceManagerInstance = null;
    private Logger logger = null;
}
