package com.l7tech.service;

import com.l7tech.objectmodel.*;
import com.l7tech.policy.PolicyValidator;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.server.service.ServiceManager;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.support.ApplicationObjectSupport;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Collection;

/**
 * Class ServiceAdminStub.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 */
public class ServiceAdminStub extends ApplicationObjectSupport implements ServiceAdmin, InitializingBean {
    private PolicyValidator policyValidator;
    private ServiceManager serviceManager;

    /**
     * Retreive the actual PublishedService object from it's oid.
     *
     * @param oid
     * @return
     * @throws RemoteException
     */
    public PublishedService findServiceByID(String oid) throws RemoteException, FindException {
        return serviceManager.findByPrimaryKey(toLong(oid));
    }


    /**
     * Used by the console to retreive the actual wsdl located at a target
     * as seen by the ssg.
     *
     * @param url
     * @return a string containing the xml document
     */
    public String resolveWsdlTarget(String url) throws RemoteException {
        return serviceManager.resolveWsdlTarget(url);
    }

    /**
     * saves a published service along with it's policy assertions
     *
     * @param service
     * @return
     * @throws RemoteException
     */
    public long savePublishedService(PublishedService service)
      throws RemoteException, UpdateException, SaveException,
      VersionException, ResolutionParameterTooLongException {
        return serviceManager.save(service);
    }

    /**
     * deletes the service
     *
     * @param id service id
     * @throws RemoteException
     */
    public void deletePublishedService(String id) throws RemoteException, DeleteException {
        PublishedService service = null;
        try {
            long oid = toLong(id);
            service = serviceManager.findByPrimaryKey(oid);
            serviceManager.delete(service);
            logger.info("Deleted PublishedService: " + oid);
        } catch (FindException e) {
            throw new DeleteException("Could not find object to delete.", e);
        }


    }

    public PolicyValidatorResult validatePolicy(String policyXml, long serviceId) throws RemoteException {
        try {
            PublishedService service = serviceManager.findByPrimaryKey(serviceId);
            Assertion assertion = WspReader.parse(policyXml);
            return policyValidator.validate(assertion, service);
        } catch (FindException e) {
            throw new RemoteException("cannot get existing service: " + serviceId, e);
        } catch (IOException e) {
            throw new RemoteException("cannot parse passed policy xml", e);
        }
    }


    /**
     * Returns an unmodifiable collection of <code>EntityHeader</code>
     * objects for all instances of the entity class corresponding to
     * this Manager.
     *
     * @return A <code>Collection</code> of EntityHeader objects.
     */
    public EntityHeader[] findAllPublishedServices() throws RemoteException, FindException {
        Collection res = serviceManager.findAllHeaders();
        return collectionToHeaderArray(res);
    }

    /**
     * Returns an unmodifiable collection of <code>EntityHeader</code>
     * objects for instances of this entity class from a list sorted
     * by <code>oid</code>, selecting only a specific subset of the list.
     *
     * @return A <code>Collection</code> of EntityHeader objects.
     */
    public EntityHeader[] findAllPublishedServicesByOffset(int offset, int windowSize)
      throws RemoteException {
        throw new RuntimeException("Not Implemented");
    }

    private EntityHeader fromService(PublishedService s) {
        return new EntityHeader(Long.toString(s.getOid()), EntityType.SERVICE, s.getName(), null);
    }

    public void setPolicyValidator(PolicyValidator policyValidator) {
        this.policyValidator = policyValidator;
    }

    public void setServiceManager(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    public void afterPropertiesSet() throws Exception {
        if (policyValidator == null) {
            throw new IllegalArgumentException("Policy Validator is required");
        }
    }


    /**
     * Parse the String service ID to long (database format). Throws runtime exc
     *
     * @param serviceID the service ID, must not be null, and .
     * @return the service ID representing <code>long</code>
     * @throws IllegalArgumentException if service ID is null
     * @throws NumberFormatException    on parse error
     */
    private long toLong(String serviceID)
      throws IllegalArgumentException, NumberFormatException {
        if (serviceID == null) {
            throw new IllegalArgumentException();
        }
        return Long.parseLong(serviceID);
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
                throw new RemoteException("Collection contained something other than a EntityHeader", e);
            }
            ++count;
        }
        return output;
    }

}
