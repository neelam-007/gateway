package com.l7tech.server.service;

import com.l7tech.common.util.Locator;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.PolicyValidator;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.remote.jini.export.RemoteService;
import com.l7tech.server.policy.validator.ServerPolicyValidator;
import com.l7tech.service.PublishedService;
import com.l7tech.service.ResolutionParameterTooLongException;
import com.l7tech.service.ServiceAdmin;
import com.sun.jini.start.LifeCycle;
import net.jini.config.ConfigurationException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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

    public String resolveWsdlTarget(String url) throws IOException, MalformedURLException {
        try {
            URL urltarget = new URL(url);
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(url);
            // support for passing username and password in the url from the ssm
            String userinfo = urltarget.getUserInfo();
            if (userinfo != null && userinfo.indexOf(':') > -1) {
                String login = userinfo.substring(0, userinfo.indexOf(':'));
                String passwd = userinfo.substring(userinfo.indexOf(':')+1, userinfo.length());
                HttpState state = client.getState();
                get.setDoAuthentication(true);
                state.setAuthenticationPreemptive(true);
                state.setCredentials(null, null, new UsernamePasswordCredentials(login, passwd));
            }
            int ret = client.executeMethod(get);
            byte[] body = null;
            if (ret == 200) {
                body = get.getResponseBody();
                return new String(body, get.getResponseCharSet());
            } else {
                String msg = "The URL " + url + " is returning code " + ret;
                logger.info(msg);
                throw new RemoteException(msg);
            }
        } catch (MalformedURLException e) {
            String msg = "Bad url: " + url;
            logger.log(Level.WARNING, msg, e);
            throw e;
        } catch (HttpException e) {
            String msg = "Http error getting " + url;
            logger.log(Level.WARNING, msg, e);
            IOException ioe =new  IOException(msg);
            ioe.initCause(e);
            throw ioe;
        }
    }

    public PublishedService findServiceByID(String serviceID) throws RemoteException, FindException {
        try {
            long oid = toLong(serviceID);
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

    public PolicyValidatorResult validatePolicy(String policyXml, long serviceid) throws RemoteException {
        try {
            PublishedService service = getServiceManager().findByPrimaryKey(serviceid);
            Assertion assertion = WspReader.parse(policyXml);
            PolicyValidator validator = new ServerPolicyValidator();
            return validator.validate(assertion, service);
        } catch (FindException e) {
            logger.log(Level.WARNING, "cannot get existing service: " + serviceid, e);
            throw new RemoteException("cannot get existing service: " + serviceid, e);
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
                                    UpdateException, SaveException, VersionException, ResolutionParameterTooLongException {
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

    public void deletePublishedService(String serviceID) throws RemoteException, DeleteException {
        ServiceManager manager = null;
        PublishedService service = null;
        try {
            long oid = toLong(serviceID);
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

    /**
     * Parse the String service ID to long (database format). Throws runtime exc
     * @param serviceID the service ID, must not be null, and .
     * @return the service ID representing <code>long</code>
     *
     * @throws IllegalArgumentException if service ID is null
     * @throws NumberFormatException on parse error
     */
    private long toLong(String serviceID)
      throws IllegalArgumentException, NumberFormatException {
        if (serviceID == null) {
                throw new IllegalArgumentException();
            }
        return Long.parseLong(serviceID);
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
}
