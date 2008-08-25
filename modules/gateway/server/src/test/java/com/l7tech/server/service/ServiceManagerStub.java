package com.l7tech.server.service;

import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.*;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.StubDataStore;
import com.l7tech.server.EntityManagerStub;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.policy.Policy;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContext;

import javax.wsdl.WSDLException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.Collection;

/**
 * Class ServiceManagerStub.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 */
public class ServiceManagerStub extends EntityManagerStub<PublishedService, ServiceHeader> implements ServiceManager, ApplicationContextAware {
    private final PolicyManager policyManager;
    private ApplicationContext applicationContext;

    public ServiceManagerStub(PolicyManager policyManager) {
        super(toArray( StubDataStore.defaultStore().getPublishedServices().values()));
        this.policyManager = policyManager;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * Used by the console to retreive the actual wsdl located at a target
     * as seen by the ssg.
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
     */
    @Override
    public long save(PublishedService service) throws SaveException {
        Policy policy = service.getPolicy();
        if (policy == null) throw new IllegalArgumentException("Service saved without a policy");
        if (policy.getOid() == Policy.DEFAULT_OID) policyManager.save(policy);
        long oid = super.save(service);
        try {
            ServiceCache serviceCache = getServiceCache();
            serviceCache.cache(service);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return oid;
    }

    private ServiceCache getServiceCache() {
        return (ServiceCache) applicationContext.getBean("serviceCache", ServiceCache.class);
    }

    public void addManageServiceRole(PublishedService service) throws SaveException {
        // No-op for stub mode
    }

    /**
     * Returns the result from super.findAllHeaders. Specifying includealiases as either true or false has no affect
     * on the Collection returned
     * @param includeAliases
     * @return
     * @throws FindException
     */
    public Collection<ServiceHeader> findAllHeaders(boolean includeAliases) throws FindException {
        return super.findAllHeaders();
    }

    /**
     * updates a policy service. call this instead of save if the service
     * has an history. on the console side implementation, you can call save
     * either way and the oid will dictate whether the object should be saved
     * or updated.
     */
    @Override
    public void update(PublishedService service) throws UpdateException {
        super.update(service);
        try {
            ServiceCache serviceCache = getServiceCache();
            serviceCache.removeFromCache(service);
            serviceCache.cache(service);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void delete(PublishedService service) throws DeleteException {
        super.delete(service);
        ServiceCache serviceCache = getServiceCache();
        serviceCache.removeFromCache(service);
    }

    @Override
    public Class<PublishedService> getImpClass() {
        return PublishedService.class;
    }

    @Override
    public Class<PublishedService> getInterfaceClass() {
        return PublishedService.class;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.SERVICE;
    }

    @Override
    public String getTableName() {
        return "published_service";
    }

    @Override
    protected ServiceHeader header(PublishedService entity) {
        return new ServiceHeader( entity );
    }

    private static PublishedService[] toArray( Collection<PublishedService> publishedServices ) {
        return publishedServices.toArray( new PublishedService[publishedServices.size()] );
    }    
}
