package com.l7tech.adminws.service;

import com.l7tech.service.PublishedService;
import com.l7tech.objectmodel.*;
import com.l7tech.util.Locator;

import java.util.Collection;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 6, 2003
 *
 */
public class Service {

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
            e.printStackTrace(System.err);
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
                getServiceManagerAndBeginTransaction().update(service);
                return service.getOid();
            }
            // ... or is it a new object
            else {
                return getServiceManagerAndBeginTransaction().save(service);
            }
        } catch (UpdateException e) {
            throw new java.rmi.RemoteException(e.getMessage(), e);
        } catch (SaveException e) {
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
        if (serviceManagerInstance == null){
            initialiseServiceManager();
        }
        try {
            PersistenceContext.getCurrent().beginTransaction();
        }
        catch (java.sql.SQLException e) {
            e.printStackTrace(System.err);
            throw new java.rmi.RemoteException("SQLException in ServiceManager.getServiceManagerAndBeginTransaction : "+ e.getMessage(), e);
        } catch (TransactionException e) {
            e.printStackTrace(System.err);
            throw new java.rmi.RemoteException("TransactionException in ServiceManager.getServiceManagerAndBeginTransaction : "+ e.getMessage(), e);
        }
        return serviceManagerInstance;
    }

    private void endTransaction() throws java.rmi.RemoteException {
        try {
            PersistenceContext.getCurrent().commitTransaction();
        } catch (java.sql.SQLException e) {
            e.printStackTrace(System.err);
            throw new java.rmi.RemoteException("Exception commiting", e);
        } catch (TransactionException e) {
            e.printStackTrace(System.err);
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
                e.printStackTrace(System.err);
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
        } catch (ClassCastException e) {
            e.printStackTrace(System.err);
            throw new java.rmi.RemoteException("ClassCastException in ServiceManager.initialiseServiceManager from Locator.getDefault().lookup", e);
        } catch (RuntimeException e) {
            e.printStackTrace(System.err);
            throw new java.rmi.RemoteException("RuntimeException in ServiceManager.initialiseServiceManager from Locator.getDefault().lookup: "+ e.getMessage(), e);
        }
    }

    /*
    public static void main (String[] args) throws Exception {
        Service me = new Service();
        System.out.println(me.resolveWsdlTarget("http://192.168.0.2:8080/simplewsdl.xml"));
    }

    private PublishedService createTestPubService() {
        PublishedService out = new PublishedService();
        out.setName("service name");
        out.setOid(132);
        out.setPolicyXml("<this is not really xml>");
        out.setWsdlXml("<this is not really xml>");
        return out;
    }

    private com.l7tech.objectmodel.EntityHeader[] createTestHeaders() {
        com.l7tech.objectmodel.EntityHeader[] out = new com.l7tech.objectmodel.EntityHeader[3];
        out[0] = new com.l7tech.objectmodel.EntityHeader(1, EntityType.SERVICE, "name", "description");
        out[1] = new com.l7tech.objectmodel.EntityHeader(2, EntityType.SERVICE, "name", "description");
        out[2] = new com.l7tech.objectmodel.EntityHeader(3, EntityType.SERVICE, "name", "description");
        return out;
    }
    */

    private com.l7tech.service.ServiceManager serviceManagerInstance = null;
}
