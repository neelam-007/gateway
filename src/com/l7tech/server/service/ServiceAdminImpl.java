package com.l7tech.server.service;

import com.l7tech.common.util.Locator;
import com.l7tech.objectmodel.*;
import com.l7tech.remote.jini.export.RemoteService;
import com.l7tech.service.PublishedService;
import com.l7tech.service.ServiceAdmin;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.PolicyValidator;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.server.policy.validator.ServerPolicyValidator;
import com.sun.jini.start.LifeCycle;
import net.jini.config.ConfigurationException;

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
 * Server side implementation of the ServiceAdmin admin api.
 * 
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * User: flascelles<br/>
 * Date: Jun 6, 2003
 */
public class ServiceAdminImpl extends RemoteService implements ServiceAdmin {
    public ServiceAdminImpl( String[] options, LifeCycle lifeCycle ) throws ConfigurationException, IOException {
        super( options, lifeCycle );
    }

    public static final String SERVICE_DEPENDENT_URL_PORTION = "/services/serviceAdmin";

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

    public PolicyValidatorResult validatePolicy(String policyXml, boolean isSoap) throws RemoteException {
        try {
            Assertion assertion = WspReader.parse(policyXml);
            PolicyValidator validator = new ServerPolicyValidator();
            return validator.validate(assertion, isSoap);
        } catch (IOException e) {
            logger.log(Level.WARNING, "cannot parse passed policy xml: " + policyXml, e);
            throw new RemoteException("cannot parse passed policy xml", e);
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
            enforceAdminRole();
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
        ServiceManager manager = null;
        PublishedService service = null;

        try {
            enforceAdminRole();
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

    private final Logger logger = Logger.getLogger(getClass().getName());
}
