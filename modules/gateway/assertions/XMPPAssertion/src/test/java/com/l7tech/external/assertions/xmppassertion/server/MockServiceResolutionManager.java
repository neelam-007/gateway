package com.l7tech.external.assertions.xmppassertion.server;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.Message;
import com.l7tech.server.service.resolution.ServiceIdResolver;
import com.l7tech.server.service.resolution.ServiceResolutionException;
import com.l7tech.server.service.resolution.ServiceResolutionManager;
import com.l7tech.server.service.resolution.ServiceResolver;
import com.l7tech.server.transport.ResolutionConfigurationManager;
import com.l7tech.server.transport.ResolutionConfigurationManagerStub;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

/**
 * User: rseminoff
 * Date: 25/05/12
 */
public class MockServiceResolutionManager extends ServiceResolutionManager  {
    public MockServiceResolutionManager(final ResolutionConfigurationManager resolutionConfigurationManager, final String resolutionConfigurationName, final Collection<ServiceResolver> resolvers, final Collection<ServiceResolver> validatingResolvers) {
        super(resolutionConfigurationManager, resolutionConfigurationName, resolvers, validatingResolvers);
    }

    public MockServiceResolutionManager() {
        super(new ResolutionConfigurationManagerStub(),
                "test",
                new ArrayList<ServiceResolver>() {{
                    add(new ServiceIdResolver(new LoggingAudit.LoggingAuditFactory()));
                }},
                new ArrayList<ServiceResolver>() {{
                    add(new ServiceIdResolver(new LoggingAudit.LoggingAuditFactory()));
                }});
    }


    @Override
    public PublishedService resolve(Audit auditor, Message req, ServiceResolutionListener rl, Collection<PublishedService> serviceSet) throws ServiceResolutionException {
        System.out.println(" *** MockServiceResolutionManager :: resolve(Audit...)");
        return null;
    }

    @Override
    public Collection<PublishedService> resolve(@Nullable String path, @Nullable String soapAction, @Nullable String namespace, Collection<PublishedService> serviceSet) throws ServiceResolutionException {
        System.out.println(" *** MockServiceResolutionManager :: resolve(String...)");
        return null;
    }

    @Override
    public void notifyServiceCreated(Audit auditor, PublishedService service) {
        System.out.println(" *** MockServiceResolutionManager :: notifyServiceCreated()");
    }

    @Override
    public void notifyServiceUpdated(Audit auditor, PublishedService service) {
        System.out.println(" *** MockServiceResolutionManager :: notifyServiceUpdated()");
    }

    @Override
    public void notifyServiceDeleted(PublishedService service) {
        System.out.println(" *** MockServiceResolutionManager :: notifyServiceDeleted()");
    }

    @Override
    public void checkResolution(PublishedService service, Collection<PublishedService> serviceSet) throws ServiceResolutionException {
        System.out.println(" *** MockServiceResolutionManager :: checkResolution()");
    }

}
