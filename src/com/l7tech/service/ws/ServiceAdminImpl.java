package com.l7tech.service.ws;

import com.l7tech.common.util.Locator;
import com.l7tech.logging.LogManager;
import com.l7tech.objectmodel.*;
import com.l7tech.service.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        try {
            return ServiceCache.getInstance().getServiceStatistics(serviceOid);
        } catch (InterruptedException e) {
            throw new RemoteException("cache exception", e);
        }
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
        com.l7tech.service.ServiceManagerImp manager = null;

        beginTransaction();
        long oid = PublishedService.DEFAULT_OID;
        try {
            manager = getServiceManager();

            // does that object have a history?
            if (service.getOid() > 0) {
                manager.update(service);
                oid = service.getOid();
            } else { // ... or is it a new object
                logger.info("Saving PublishedService: " + service.getOid());
                oid = manager.save(service);
            }
            return oid;
        } finally {
            PersistenceContext pc = null;
            try {
                pc = PersistenceContext.getCurrent();
                pc.commitTransaction();

                if ( manager != null && oid != PublishedService.DEFAULT_OID ) {
                    PublishedService newService = manager.findByPrimaryKey( service.getOid() );
                    ServiceCache.getInstance().cache(newService);
                }
            } catch ( TransactionException te ) {
                logger.log( Level.WARNING, te.getMessage(), te );
                throw new RemoteException( te.getMessage(), te );
            } catch ( SQLException se ) {
                logger.log( Level.SEVERE, se.getMessage(), se );
                throw new RemoteException( se.getMessage(), se );
            } catch ( FindException fe ) {
                logger.log( Level.SEVERE, fe.getMessage(), fe );
                throw new RemoteException( fe.getMessage(), fe );
            } catch (InterruptedException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
                throw new RemoteException(e.getMessage(), e);
            } catch (IOException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
                throw new RemoteException(e.getMessage(), e);
            } finally {
                if ( pc != null ) pc.close();
            }
        }
    }

    public void deletePublishedService(long oid) throws RemoteException, DeleteException {
        com.l7tech.service.ServiceManagerImp manager = null;
        PublishedService service = null;

        beginTransaction();
        try {
            manager = getServiceManager();
            service = manager.findByPrimaryKey(oid);
            manager.delete(service);
            logger.info("Deleted PublishedService: " + oid);
        } catch (FindException e) {
            throw new DeleteException("Could not find object to delete.", e);
        } finally {
            try {
                endTransaction();

                ServiceCache.getInstance().removeFromCache(service);
            } catch ( TransactionException te ) {
                logger.log( Level.WARNING, te.getMessage(), te );
                throw new RemoteException( te.getMessage(), te );
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, e.getMessage(), e);
                throw new RemoteException(e.getMessage(), e);
            }
        }
    }

    /*public Map serviceMap() throws RemoteException {
        return serviceManagerInstance.serviceMap();
    }*/

    // ************************************************
    // PRIVATES
    // ************************************************

    private ServiceManagerImp getServiceManager() throws RemoteException {
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

    private void endTransaction() throws TransactionException {
        try {
            PersistenceContext.getCurrent().commitTransaction();
            PersistenceContext.getCurrent().close();
        } catch (SQLException e) {
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
        serviceManagerInstance = (com.l7tech.service.ServiceManagerImp)Locator.getDefault().
                                    lookup(com.l7tech.service.ServiceManager.class);
        if (serviceManagerInstance == null) {
            throw new RemoteException("Cannot instantiate the ServiceManager");
        }
    }

    private ServiceManagerImp serviceManagerInstance = null;
    private Logger logger = null;
}
