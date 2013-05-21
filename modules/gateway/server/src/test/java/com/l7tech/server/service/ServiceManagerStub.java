package com.l7tech.server.service;

import com.l7tech.gateway.common.StubDataStore;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceDocumentWsdlStrategy;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.Policy;
import com.l7tech.server.EntityManagerStub;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.xml.soap.SoapVersion;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.wsdl.WSDLException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
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
        this(policyManager, toArray( StubDataStore.defaultStore().getPublishedServices().values()));
    }

    public ServiceManagerStub(PolicyManager policyManager, PublishedService... services) {
        super(services);
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
            Wsdl wsdl = Wsdl.newInstance(null, new InputStreamReader(new URL(url).openStream()));
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
        Field soapVersionField = null;
        try {
            soapVersionField = service.getClass().getDeclaredField("_soapVersion");
            soapVersionField.setAccessible(true);
            final Object currentSoapVersion = soapVersionField.get(service);
            if (currentSoapVersion == null) {
                service.setSoapVersion(SoapVersion.UNKNOWN);
            }

            // allow copy constructor to be successful - call to getSoapVersion will not trigger parsing the WSDL
            final PublishedService newService = new PublishedService(service, true){
                private boolean setSoapVersionOnce = false;

                @Override
                public SoapVersion getSoapVersion() {
                    if (setSoapVersionOnce) {
                        return super.getSoapVersion();
                    }
                    _locked = false;
                    // set to null so that the version will be read from the wsdl
                    setSoapVersion(null);
                    final SoapVersion soapVersion = super.getSoapVersion();
                    _locked = true;
                    setSoapVersionOnce = true;
                    return soapVersion;
                }
            };
            if (currentSoapVersion == null) {
                //restore back to null so its back to it's original state
                service.setSoapVersion(null);
            }

            //TODO Test environment should not have access to UI resources
            //explicitly set the wsdl strategy to avoid possibly incorrectly loaded strategy in test environment
            newService.parseWsdlStrategy(new ServiceDocumentWsdlStrategy(null));
            // read actual soap version now that it's safe to do so
            newService.getSoapVersion();

            ServiceCache serviceCache = getServiceCache();
            serviceCache.cache(newService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (soapVersionField != null) {
                soapVersionField.setAccessible(false);
            }
        }
        return oid;
    }

    private ServiceCache getServiceCache() {
        return applicationContext.getBean("serviceCache", ServiceCache.class);
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

    @Override
    public Collection<PublishedService> findByRoutingUri(String routingUri) throws FindException {
        Collection<PublishedService> ret = new ArrayList<PublishedService>();
        for (PublishedService et : entities.values()) {
            if (routingUri.equals(et.getRoutingUri()))
                ret.add(et);

        }
        return ret;
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
            serviceCache.cache(new PublishedService(service, true));
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
    protected ServiceHeader header(PublishedService entity) {
        return new ServiceHeader( entity );
    }

    private static PublishedService[] toArray( Collection<PublishedService> publishedServices ) {
        return publishedServices.toArray( new PublishedService[publishedServices.size()] );
    }
}
