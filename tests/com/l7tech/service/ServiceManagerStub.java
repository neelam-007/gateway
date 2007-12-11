package com.l7tech.service;

import com.l7tech.common.policy.Policy;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.identity.StubDataStore;
import com.l7tech.objectmodel.*;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.server.service.ServiceManager;
import org.springframework.beans.factory.InitializingBean;

import javax.wsdl.WSDLException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class ServiceManagerStub.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 */
public class ServiceManagerStub extends EntityManagerStub<PublishedService,ServiceHeader> implements ServiceManager, InitializingBean {
    private static Logger logger = Logger.getLogger(ServiceManagerStub.class.getName());
    private ServiceCache serviceCache;
    private final PolicyManager policyManager;

    public ServiceManagerStub(PolicyManager policyManager) {
        super(StubDataStore.defaultStore().getPublishedServices().values().toArray(new PublishedService[0]));
        this.policyManager = policyManager;
    }

    /**
     * Used by the console to retreive the actual wsdl located at a target
     * as seen by the ssg.
     * 
     * @param url
     * @return a string containing the xml document
     */
    public String resolveWsdlTarget(String url) {
        try {
            Wsdl wsdl =
              Wsdl.newInstance(null, new InputStreamReader(new URL(url).openStream()));
            StringWriter sw = new StringWriter();
            wsdl.toWriter(sw);
            return sw.toString();
        } catch (WSDLException e) {
            throw new RuntimeException("resolveWsdlTarget()", e);
        } catch (java.io.IOException e) {
            throw new RuntimeException("resolveWsdlTarget()", e);
        }
    }

    /**
     * saves a published service along with it's policy assertions
     * 
     * @param service
     * @throws SaveException
     */
    public long save(PublishedService service) throws SaveException {
        Policy policy = service.getPolicy();
        if (policy == null) throw new IllegalArgumentException("Service saved without a policy");
        if (policy.getOid() == Policy.DEFAULT_OID) policyManager.save(policy);
        long oid = super.save(service);
        try {
            serviceCache.cache(service);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return oid;
    }

    public void addManageServiceRole(PublishedService service) throws SaveException {
        // No-op for stub mode
    }

    /**
     * updates a policy service. call this instead of save if the service
     * has an history. on the console side implementation, you can call save
     * either way and the oid will dictate whether the object should be saved
     * or updated.
     * 
     * @param service
     * @throws UpdateException
     */
    public void update(PublishedService service) throws UpdateException {
        super.update(service);
        try {
            serviceCache.removeFromCache(service);
            serviceCache.cache(service);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * deletes the service
     * 
     * @param service
     * @throws DeleteException
     */
    public void delete(PublishedService service) throws DeleteException {
        super.delete(service);
        try {
            serviceCache.removeFromCache(service);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public Class getImpClass() {
        return PublishedService.class;
    }

    public Class getInterfaceClass() {
        return PublishedService.class;
    }

    public EntityType getEntityType() {
        return EntityType.SERVICE;
    }

    public String getTableName() {
        return "published_service";
    }


    public void setServiceCache(ServiceCache serviceCache) {
        this.serviceCache = serviceCache;
    }

    public void afterPropertiesSet() throws Exception {
        initializeServiceCache();
    }

    private void initializeServiceCache() throws ObjectModelException {
        // build the cache if necessary
        try {
            if (serviceCache.size() > 0) {
                logger.finest("cache already built (?)");
            } else {
                logger.finest("building service cache");
                Collection<PublishedService> services = findAll();
                for (PublishedService service : services) {
                    Policy policy = service.getPolicy();
                    if (policy.getOid() == Policy.DEFAULT_OID) policyManager.save(policy);

                    try {
                        serviceCache.cache(service);
                    } catch (ServerPolicyException e) {
                        logger.log(Level.WARNING, "Service disabled: " + ExceptionUtils.getMessage(e), e);
                    }
                }
            }
            // make sure the integrity check is running
            serviceCache.initiateIntegrityCheckProcess();
        } catch (InterruptedException e) {
            throw new ObjectModelException("Exception building cache", e);
        }
    }

    protected ServiceHeader header(PublishedService entity) {
        return new ServiceHeader( entity );
    }
}
