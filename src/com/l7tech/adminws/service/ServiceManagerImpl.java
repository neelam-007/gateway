package com.l7tech.adminws.service;

import com.l7tech.service.PublishedService;
import com.l7tech.objectmodel.*;
import com.l7tech.util.Locator;
import com.l7tech.logging.LogManager;
import com.l7tech.jini.export.RemoteService;
import com.sun.jini.start.LifeCycle;

import java.util.Collection;
import java.util.logging.Level;
import java.io.IOException;

import net.jini.config.ConfigurationException;

/**
 * The class is the admin service manager implementaiton.
 *
 */
public class ServiceManagerImpl extends RemoteService implements ServiceManager {


    public ServiceManagerImpl (String[] configOptions, LifeCycle lc)
            throws ConfigurationException, IOException {
        super(configOptions, lc);
    }

    public String resolveWsdlTarget(String url) throws java.rmi.RemoteException {
        try {
            java.net.URL urltarget = new java.net.URL(url);
            java.net.URLConnection connection = urltarget.openConnection();
            java.io.InputStream in = connection.getInputStream();
            byte[] buffer = new byte[2048];
            int read = in.read(buffer);
            StringBuffer out = new StringBuffer();
            while (read > 0) {
                out.append(new String(buffer, 0, read));
                read = in.read(buffer);
            }
            return out.toString();
        } catch (java.io.IOException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, e);
            throw new java.rmi.RemoteException("com.l7tech.adminws.service.Service cannot resolve WSDL " + e.getMessage(), e);
        }
    }

    public PublishedService findServiceByPrimaryKey(long oid) throws java.rmi.RemoteException {
        try {
            return getServiceManagerAndBeginTransaction().findByPrimaryKey(oid);
        } catch (FindException e) {
            throw new java.rmi.RemoteException(e.getMessage(), e);
        } finally {
            endTransaction();
        }
    }

    public EntityHeader[] findAllPublishedServices() throws java.rmi.RemoteException {
        try {
            Collection res = getServiceManagerAndBeginTransaction().findAllHeaders();
            return collectionToHeaderArray(res);
        } catch (FindException e) {
            throw new java.rmi.RemoteException(e.getMessage(), e);
        } finally {
            endTransaction();
        }
    }

    public EntityHeader[] findAllPublishedServicesByOffset(int offset, int windowSize) throws java.rmi.RemoteException {
        try {
            Collection res = getServiceManagerAndBeginTransaction().findAllHeaders(offset, windowSize);
            return collectionToHeaderArray(res);
        } catch (FindException e) {
            throw new java.rmi.RemoteException(e.getMessage(), e);
        } finally {
            endTransaction();
        }
    }

    /**
     * this save method handles both save and updates.
     * the distinction happens on the server side by inspecting the oid of the object
     * if the oid appears to be "virgin" a save is invoked, otherwise an update call is made.
     * @param service the object to be saved or upodated
     * @throws java.rmi.RemoteException
     */
    public long savePublishedService(PublishedService service) throws java.rmi.RemoteException {
        try {
            // does that object have a history?
            if (service.getOid() > 0) {
                // update patch to fix hibernate problem
                com.l7tech.service.ServiceManager manager = getServiceManagerAndBeginTransaction();
                PublishedService originalService = manager.findByPrimaryKey(service.getOid());
                originalService.copyFrom(service);
                manager.update(originalService);
                // end of patch
                // getServiceManagerAndBeginTransaction().update(service);
                LogManager.getInstance().getSystemLogger().log(Level.INFO, "Updated PublishedService: " + service.getOid());
                return service.getOid();
            }
            // ... or is it a new object
            else {
                LogManager.getInstance().getSystemLogger().log(Level.INFO, "Saving PublishedService: " + service.getOid());
                return getServiceManagerAndBeginTransaction().save(service);
            }
        } catch (Exception e) {
            throw new java.rmi.RemoteException(e.getMessage(), e);
        } finally {
            endTransaction();
        }
    }

    public void deletePublishedService(long oid) throws java.rmi.RemoteException {
        try {
            com.l7tech.service.ServiceManager theManagerDude = getServiceManagerAndBeginTransaction();
            PublishedService theExecutionee = theManagerDude.findByPrimaryKey(oid);
            theManagerDude.delete(theExecutionee);
            LogManager.getInstance().getSystemLogger().log(Level.INFO, "Deleted PublishedService: " + oid);
        } catch (FindException e) {
            throw new java.rmi.RemoteException(e.getMessage(), e);
        } catch (DeleteException e) {
            throw new java.rmi.RemoteException(e.getMessage(), e);
        } finally {
            endTransaction();
        }
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    private com.l7tech.service.ServiceManager getServiceManagerAndBeginTransaction() throws java.rmi.RemoteException {
        try {
            PersistenceContext.getCurrent().beginTransaction();
            if (serviceManagerInstance == null) {
                initialiseServiceManager();
            }
        } catch (Throwable e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "Exception in initialiseServiceManager", e);
            throw new java.rmi.RemoteException("Exception in ServiceManager.getServiceManagerAndBeginTransaction : " + e.getMessage(), e);
        }
        return serviceManagerInstance;
    }

    private void endTransaction() throws java.rmi.RemoteException {
        try {
            PersistenceContext context = PersistenceContext.getCurrent();
            context.commitTransaction();
            context.close();
        } catch (java.sql.SQLException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, e);
            throw new java.rmi.RemoteException("Exception commiting", e);
        } catch (TransactionException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, e);
            throw new java.rmi.RemoteException("Exception commiting", e);
        }
    }

    private EntityHeader[] collectionToHeaderArray(Collection input) throws java.rmi.RemoteException {
        if (input == null) return new EntityHeader[0];
        EntityHeader[] output = new EntityHeader[input.size()];
        int count = 0;
        java.util.Iterator i = input.iterator();
        while (i.hasNext()) {
            try {
                output[count] = (EntityHeader) i.next();
            } catch (ClassCastException e) {
                LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, e);
                throw new java.rmi.RemoteException("Collection contained something other than a com.l7tech.objectmodel.EntityHeader", e);
            }
            ++count;
        }
        return output;
    }

    private synchronized void initialiseServiceManager() throws java.rmi.RemoteException {
        try {
            serviceManagerInstance = (com.l7tech.service.ServiceManager)Locator.getDefault().lookup(com.l7tech.service.ServiceManager.class);
            if (serviceManagerInstance == null) throw new java.rmi.RemoteException("Cannot instantiate the ServiceManager");
        } catch (Throwable e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "Exception in Locator.getDefault().lookup(com.l7tech.service.ServiceManager.class)" + e.getMessage(), e);
            throw new java.rmi.RemoteException("Exception in Locator.getDefault().lookup: " + e.getMessage(), e);
        }
    }

    private com.l7tech.service.ServiceManager serviceManagerInstance = null;
}
