package com.l7tech.service.ws;

import com.l7tech.common.util.Locator;
import com.l7tech.objectmodel.*;
import com.l7tech.service.PublishedService;
import com.l7tech.service.ServiceAdmin;
import com.l7tech.service.ServiceManager;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.hibernate.HibernateException;

/**
 * Server side implementation of the ServiceAdmin admin api.
 * 
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * User: flascelles<br/>
 * Date: Jun 6, 2003
 */
public class ServiceAdminImpl implements ServiceAdmin {

    public static final String SERVICE_DEPENDENT_URL_PORTION = "/services/serviceAdmin";

    public ServiceAdminImpl() {
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
        clearContextSession();
        try {
            PublishedService service = getServiceManager().findByPrimaryKey(oid);
            if (service != null) {
                logger.finest("Returning service id " + oid + ", version " + service.getVersion());
            }
            return service;
        } finally {
            closeContext();
        }
    }

    public EntityHeader[] findAllPublishedServices() throws RemoteException, FindException {
        clearContextSession();
        try {
            Collection res = getServiceManager().findAllHeaders();
            return collectionToHeaderArray(res);
        } finally {
            closeContext();
        }
    }

    public EntityHeader[] findAllPublishedServicesByOffset(int offset, int windowSize)
                    throws RemoteException, FindException {
        clearContextSession();
        try {
            Collection res = getServiceManager().findAllHeaders(offset, windowSize);
            return collectionToHeaderArray(res);
        } finally {
            closeContext();
        }
    }

    /**
     * this save method handles both save and updates.
     * the distinction happens on the server side by inspecting the oid of the object
     * if the oid appears to be "virgin" a save is invoked, otherwise an update call is made.
     * @param service the object to be saved or upodated
     * @throws RemoteException
     *
     */
    public long savePublishedService(PublishedService service) throws RemoteException,
                                    UpdateException, SaveException, VersionException {
        clearContextSession();
        ServiceManager manager = getServiceManager();
        PersistenceContext pc = null;
        try {
            pc = PersistenceContext.getCurrent();
        } catch (SQLException e) {
            String msg = "could not get persistence context";
            logger.log(Level.WARNING, msg, e);
            throw new UpdateException(msg, e);
        }

        try {
            long oid = PublishedService.DEFAULT_OID;
            pc.beginTransaction();

            if (service.getOid() > 0) {
                // UPDATING EXISTING SERVICE
                oid = service.getOid();
                logger.fine("Updating PublishedService: " + oid);
                manager.update(service);
            } else {
                // SAVING NEW SERVICE
                logger.fine("Saving new PublishedService");
                oid = manager.save(service);
            }
            pc.commitTransaction();
            return oid;
        } catch (TransactionException e) {
            String msg = "Transaction exception (duplicate resolution parameters?). Rolling back.";
            logger.log(Level.WARNING, msg, e);
            try {
                pc.rollbackTransaction();
            } catch (TransactionException e1) {
                logger.log(Level.WARNING, "exception rolling back", e);
            }
            throw new UpdateException(msg, e);
        } finally {
            pc.close();
        }
    }

    public void deletePublishedService(long oid) throws RemoteException, DeleteException {
        clearContextSession();
        ServiceManager manager = null;
        PublishedService service = null;

        try {
            beginTransaction();
            manager = getServiceManager();
            service = manager.findByPrimaryKey(oid);
            manager.delete(service);
            logger.info("Deleted PublishedService: " + oid);
        } catch (FindException e) {
            throw new DeleteException("Could not find object to delete.", e);
        } finally {
            try {
                endTransaction();
            } catch ( TransactionException te ) {
                logger.log( Level.WARNING, te.getMessage(), te );
                throw new RemoteException( te.getMessage(), te );
            }
        }
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    private ServiceManager getServiceManager() {
        return (ServiceManager)Locator.getDefault().lookup(ServiceManager.class);
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

    private void clearContextSession() {
        try {
            HibernatePersistenceContext hpc = (HibernatePersistenceContext)PersistenceContext.getCurrent();
            hpc.getSession().clear();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "error clearing existing context", e);
        } catch (HibernateException e) {
            logger.log(Level.WARNING, "error clearing existing context", e);
        }

    }

    private final Logger logger = Logger.getLogger(getClass().getName());
}
