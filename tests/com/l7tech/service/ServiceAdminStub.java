package com.l7tech.service;

import com.l7tech.common.xml.Wsdl;
import com.l7tech.identity.StubDataStore;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.policy.PolicyValidator;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.WspReader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.support.ApplicationObjectSupport;

import javax.wsdl.WSDLException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * Class ServiceAdminStub.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 */
public class ServiceAdminStub extends ApplicationObjectSupport implements ServiceAdmin, InitializingBean {
    private Map services;
    private PolicyValidator policyValidator;

    public ServiceAdminStub() {
        services = StubDataStore.defaultStore().getPublishedServices();
    }

    /**
     * Retreive the actual PublishedService object from it's oid.
     *
     * @param oid
     * @return
     * @throws RemoteException
     */
    public PublishedService findServiceByID(String oid) throws RemoteException {
        return
          (PublishedService)services.get(new Long(oid));
    }


    /**
     * Used by the console to retreive the actual wsdl located at a target
     * as seen by the ssg.
     *
     * @param url
     * @return a string containing the xml document
     */
    public String resolveWsdlTarget(String url) throws RemoteException {
        try {
            Wsdl wsdl =
              Wsdl.newInstance(null, new InputStreamReader(new URL(url).openStream()));
            StringWriter sw = new StringWriter();
            wsdl.toWriter(sw);
            return sw.toString();
        } catch (WSDLException e) {
            throw new RemoteException("resolveWsdlTarget()", e);
        } catch (java.io.IOException e) {
            throw new RemoteException("resolveWsdlTarget()", e);
        }
    }

    /**
     * saves a published service along with it's policy assertions
     *
     * @param service
     * @return
     * @throws RemoteException
     */
    public long savePublishedService(PublishedService service) throws RemoteException {
        long oid = service.getOid();
        if (oid == 0 || oid == Entity.DEFAULT_OID) {
            oid = StubDataStore.defaultStore().nextObjectId();
        }
        service.setOid(oid);
        Long key = new Long(oid);
        services.put(key, service);
        return oid;
    }

    /**
     * deletes the service
     *
     * @param id service id
     * @throws RemoteException
     */
    public void deletePublishedService(String id) throws RemoteException {
        if (services.remove(new Long(id)) == null) {
            throw new RemoteException("Could not find service oid= " + id);
        }

    }

    public PolicyValidatorResult validatePolicy(String policyXml, long serviceId) throws RemoteException {
        // todo
        try {
            PublishedService service = findServiceByID(Long.toString(serviceId));
            Assertion assertion = WspReader.parse(policyXml);
            return policyValidator.validate(assertion, service);
        } catch (IOException e) {
            logger.warn("cannot parse passed policy xml: " + policyXml, e);
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
    public EntityHeader[] findAllPublishedServices() throws RemoteException {
        Collection list = new ArrayList();
        for (Iterator i =
          services.keySet().iterator(); i.hasNext();) {
            Long key = (Long)i.next();
            list.add(fromService((PublishedService)services.get(key)));
        }
        return (EntityHeader[])list.toArray(new EntityHeader[]{});
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
        Collection list = new ArrayList();
        int index = 0;
        int count = 0;
        for (Iterator i =
          services.keySet().iterator(); i.hasNext(); index++) {
            Long key = (Long)i.next();

            if (index >= offset && count <= windowSize) {
                list.add(fromService((PublishedService)services.get(key)));
                count++;
            }
        }
        return (EntityHeader[])list.toArray(new EntityHeader[]{});
    }

    private EntityHeader fromService(PublishedService s) {
        return new EntityHeader(Long.toString(s.getOid()), EntityType.SERVICE, s.getName(), null);
    }

    public void setPolicyValidator(PolicyValidator policyValidator) {
        this.policyValidator = policyValidator;
    }

    public void afterPropertiesSet() throws Exception {
        if (policyValidator == null) {
            throw new IllegalArgumentException("Policy Validator is required");
        }
    }
}
