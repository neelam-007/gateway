package com.l7tech.adminws.service;

import com.l7tech.service.PublishedService;
import com.l7tech.service.ServiceManager;
import com.l7tech.objectmodel.*;
import com.l7tech.util.Locator;
import com.l7tech.logging.LogManager;

import java.util.Collection;
import java.util.logging.Level;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 6, 2003
 *
 */
public class Service {

    public static final String SERVICE_DEPENDENT_URL_PORTION = "/services/serviceAdmin";

    public Service() {
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

    public com.l7tech.objectmodel.EntityHeader[] findAllPublishedServices() throws java.rmi.RemoteException {
        try {
            Collection res = getServiceManagerAndBeginTransaction().findAllHeaders();
            return collectionToHeaderArray(res);
        } catch (FindException e) {
            throw new java.rmi.RemoteException(e.getMessage(), e);
        } finally {
            endTransaction();
        }
    }

    public com.l7tech.objectmodel.EntityHeader[] findAllPublishedServicesByOffset(int offset, int windowSize) throws java.rmi.RemoteException {
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
                ServiceManager manager = getServiceManagerAndBeginTransaction();
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

    private ServiceManager getServiceManagerAndBeginTransaction() throws java.rmi.RemoteException {
        try {
            PersistenceContext.getCurrent().beginTransaction();
            if (serviceManagerInstance == null) {
                initialiseServiceManager();
            }
        } catch (Throwable e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "Exception in initialiseServiceManager", e);
            throw new java.rmi.RemoteException("Exception in ServiceManager.getServiceManagerAndBeginTransaction : "+ e.getMessage(), e);
        }
        return serviceManagerInstance;
    }

    private void endTransaction() throws java.rmi.RemoteException {
        try {
            PersistenceContext.getCurrent().commitTransaction();
        } catch (java.sql.SQLException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, e);
            throw new java.rmi.RemoteException("Exception commiting", e);
        } catch (TransactionException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, e);
            throw new java.rmi.RemoteException("Exception commiting", e);
        }
    }

    private com.l7tech.objectmodel.EntityHeader[] collectionToHeaderArray(java.util.Collection input) throws java.rmi.RemoteException {
        if (input == null) return new com.l7tech.objectmodel.EntityHeader[0];
        com.l7tech.objectmodel.EntityHeader[] output = new com.l7tech.objectmodel.EntityHeader[input.size()];
        int count = 0;
        java.util.Iterator i = input.iterator();
        while (i.hasNext()) {
            try {
                output[count] = (com.l7tech.objectmodel.EntityHeader)i.next();
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
            throw new java.rmi.RemoteException("Exception in Locator.getDefault().lookup: "+ e.getMessage(), e);
        }
    }

    private com.l7tech.service.ServiceManager serviceManagerInstance = null;
}
