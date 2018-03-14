package com.l7tech.server;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.decorator.WssDecorator;
import com.l7tech.security.xml.processor.SecurityContextFinder;
import com.l7tech.server.audit.AuditContextFactory;
import com.l7tech.server.audit.MessageSummaryAuditFactory;
import com.l7tech.server.event.metrics.ServiceFinished;
import com.l7tech.server.log.TrafficLogger;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.metrics.GatewayMetricsPublisher;
import com.l7tech.server.message.metrics.GatewayMetricsSupport;
import com.l7tech.server.message.metrics.MockGatewayMetricsPublisher;
import com.l7tech.server.messageprocessor.injection.MessageProcessorInjector;
import com.l7tech.server.policy.PolicyCache;
import com.l7tech.server.policy.PolicyMetadata;
import com.l7tech.server.policy.PolicyMetadataStub;
import com.l7tech.server.policy.ServerPolicyHandle;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.server.service.ServiceMetricsServices;
import com.l7tech.server.service.resolution.ServiceResolutionException;
import com.l7tech.server.stepdebug.DebugManager;
import com.l7tech.util.Config;
import com.l7tech.util.Functions;
import org.junit.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import static com.l7tech.server.ServerConfigParams.PARAM_RELAY_GATEWAY_METRICS_ENABLE;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class MessageProcessorTest {

    private final static String SERVICE_ID = "serviceId";
    private final static Goid POLICY_GOID = new Goid(1L, 2L);
    private final static Goid SERVICE_GOID = new Goid(3L, 4L);

    @Test
    public void shouldFireServiceFinishedWhenBlah() throws Exception {
        final ApplicationEventPublisher applicationEventPublisher = new ApplicationEventPublisherBuilder().build();
        final MessageProcessor messageProcessor = new MessageProcessor(
                new ServiceCacheBuilder()
                        .withService(new PublishedServiceBuilder()
                                .withId(SERVICE_ID).withGoid(SERVICE_GOID)
                                .withPolicy(new PolicyBuilder().withGoid(POLICY_GOID).build())
                                .build())
                        .build(),
                new PolicyCacheBuilder().withPath(POLICY_GOID, new ArrayList<>()).build(),
                new WssDecoratorBuilder().build(),
                new SecurityTokenResolverBuilder().build(),
                new SecurityContextFinderBuilder().build(),
                new LicenseManagerBuilder().build(),
                new ServiceMetricsServicesBuilder().build(),
                new AuditContextFactoryBuilder().build(),
                new MessageSummaryAuditFactoryBuilder().build(),
                new MessageProcessorInjectorBuilder().build(),
                new ConfigBuilder().withBooleanSetting(PARAM_RELAY_GATEWAY_METRICS_ENABLE, true).build(),
                new TrafficLoggerBuilder().build(),
                applicationEventPublisher);

        messageProcessor.debugManager = new DebugManagerBuilder().build();
        final MockGatewayMetricsPublisher gatewayMetricsEventsPublisher = new GatewayMetricsEventPublisherBuilder().build();
        messageProcessor.gatewayMetricsEventsPublisher = gatewayMetricsEventsPublisher;
        messageProcessor.afterPropertiesSet();
        messageProcessor.processMessage(new PolicyEnforcementContextBuilder()
                .withRequest(new Message())
                .withResponse(new Message())
                .build());

        verify(gatewayMetricsEventsPublisher, times(1)).publishEvent(any(ServiceFinished.class));
    }


    class ServiceCacheBuilder {
        private final ServiceCache serviceCache;

        ServiceCacheBuilder() {
            this.serviceCache = mock(ServiceCache.class);
        }

        ServiceCacheBuilder withService(PublishedService service) {
            try {
                when(serviceCache.resolve(any(Message.class), any(ServiceCache.ResolutionListener.class)))
                        .thenReturn(service);

                final ServerPolicyHandle serverPolicyHandle = new ServerPolicyHandleBuilder()
                        .withMetadata(new PolicyMetadataStub())
                        .build();
                when(serviceCache.getServerPolicy(any(Goid.class))).thenReturn(serverPolicyHandle);

                return this;

            } catch (ServiceResolutionException e) {
                throw new RuntimeException("Unexpected exception, test failed", e);
            }
        }

        ServiceCache build() {
            return serviceCache;
        }
    }

    class ServerPolicyHandleBuilder {
        private final ServerPolicyHandle serverPolicyHandle;

        ServerPolicyHandleBuilder() {
            serverPolicyHandle = mock(ServerPolicyHandle.class);
        }

        ServerPolicyHandleBuilder withMetadata(PolicyMetadata policyMetadata) {
            try {
                when(serverPolicyHandle.getPolicyMetadata()).thenReturn(policyMetadata);
                when(serverPolicyHandle.checkRequest(any(PolicyEnforcementContext.class)))
                        .thenReturn(AssertionStatus.NONE);

            } catch (Exception e) {
                throw new RuntimeException("Unexpected exception, test failed", e);
            }

            return this;
        }

        ServerPolicyHandle build() {
            return serverPolicyHandle;
        }
    }

    class PolicyCacheBuilder {
        private final PolicyCache policyCache;

        PolicyCacheBuilder() {
            this.policyCache = mock(PolicyCache.class);
        }

        PolicyCacheBuilder withPath(Goid goid, List<Folder> folderPath) {
            when(policyCache.getFolderPath(goid)).thenReturn(Collections.unmodifiableList(folderPath));
            return this;
        }

        PolicyCache build() {
            return policyCache;
        }
    }

    class WssDecoratorBuilder {
        private final WssDecorator wssDecorator;

        WssDecoratorBuilder() {
            this.wssDecorator = mock(WssDecorator.class);
        }

        WssDecorator build() {
            return wssDecorator;
        }
    }

    class SecurityTokenResolverBuilder {
        private final SecurityTokenResolver securityTokenResolver;

        SecurityTokenResolverBuilder() {
            this.securityTokenResolver = mock(SecurityTokenResolver.class);
        }

        SecurityTokenResolver build() {
            return securityTokenResolver;
        }
    }

    class SecurityContextFinderBuilder {
        private final SecurityContextFinder securityContextFinder;

        SecurityContextFinderBuilder() {
            this.securityContextFinder = mock(SecurityContextFinder.class);
        }

        SecurityContextFinder build() {
            return securityContextFinder;
        }
    }

    class LicenseManagerBuilder {
        private final LicenseManager licenseManager;

        LicenseManagerBuilder() {
            this.licenseManager = mock(LicenseManager.class);
        }

        LicenseManager build() {
            return licenseManager;
        }
    }

    class ServiceMetricsServicesBuilder {
        private final ServiceMetricsServices serviceMetricsServices;

        ServiceMetricsServicesBuilder() {
            this.serviceMetricsServices = mock(ServiceMetricsServices.class);
        }

        ServiceMetricsServices build() {
            return serviceMetricsServices;
        }
    }

    class AuditContextFactoryBuilder {
        private final AuditContextFactory auditContextFactory;

        AuditContextFactoryBuilder() {
            this.auditContextFactory = mock(AuditContextFactory.class);
        }

        AuditContextFactory build() {
            try {
                when(auditContextFactory.doWithNewAuditContext(any(Callable.class), any(Functions.Nullary.class)))
                        .thenAnswer(invocationOnMock -> ((Callable) invocationOnMock.getArguments()[0]).call());

            } catch (Exception e) {
                throw new RuntimeException("Unexpected exception, test failed", e);
            }

            return auditContextFactory;
        }
    }

    class MessageSummaryAuditFactoryBuilder {
        private final MessageSummaryAuditFactory messageSummaryAuditFactory;

        MessageSummaryAuditFactoryBuilder() {
            this.messageSummaryAuditFactory = mock(MessageSummaryAuditFactory.class);
        }

        MessageSummaryAuditFactory build() {
            return messageSummaryAuditFactory;
        }
    }

    class ConfigBuilder {
        private final Config config;

        ConfigBuilder() {
            this.config = mock(Config.class);
        }

        ConfigBuilder withBooleanSetting(String property, boolean value) {
            when(config.getBooleanProperty(eq(property), anyBoolean())).thenReturn(value);
            return this;
        }

        Config build() {
            return config;
        }
    }

    class TrafficLoggerBuilder {
        private final TrafficLogger trafficLogger;

        TrafficLoggerBuilder() {
            this.trafficLogger = mock(TrafficLogger.class);
        }

        TrafficLogger build() {
            return trafficLogger;
        }
    }

    class ApplicationEventPublisherBuilder {
        private final ApplicationEventPublisher applicationEventPublisher;

        ApplicationEventPublisherBuilder() {
            this.applicationEventPublisher = mock(ApplicationEventPublisher.class);
        }

        ApplicationEventPublisher build() {
            return applicationEventPublisher;
        }
    }

    class PolicyEnforcementContextBuilder {
        private final PolicyEnforcementContext policyEnforcementContext;
        private PublishedService service;
        private GatewayMetricsPublisher publisher;

        PolicyEnforcementContextBuilder() {
            this.policyEnforcementContext = mock(PolicyEnforcementContext.class,
                    withSettings().extraInterfaces(GatewayMetricsSupport.class));
        }

        PolicyEnforcementContextBuilder withRequest(Message request) {
            when(policyEnforcementContext.getRequest()).thenReturn(request);
            return this;
        }

        PolicyEnforcementContextBuilder withResponse(Message response) {
            when(policyEnforcementContext.getResponse()).thenReturn(response);
            return this;
        }

        PolicyEnforcementContext build() {
            doAnswer(invocationOnMock -> service = (PublishedService) invocationOnMock.getArguments()[0])
                    .when(policyEnforcementContext).setService(any(PublishedService.class));
            when(policyEnforcementContext.getService()).thenAnswer(invocationOnMock -> service);

            doAnswer(
                    invocationOnMock -> publisher = (GatewayMetricsPublisher) invocationOnMock.getArguments()[0])
                    .when((GatewayMetricsSupport) policyEnforcementContext).setGatewayMetricsEventsPublisher(
                            any(GatewayMetricsPublisher.class));
            when(((GatewayMetricsSupport) policyEnforcementContext).getGatewayMetricsEventsPublisher())
                    .thenAnswer(invocationOnMock -> publisher);

            return policyEnforcementContext;
        }
    }

    class PublishedServiceBuilder {
        private final PublishedService publishedService;

        PublishedServiceBuilder() {
            this.publishedService = mock(PublishedService.class);
        }

        PublishedServiceBuilder withId(String id) {
            when(publishedService.getId()).thenReturn(id);
            return this;
        }

        PublishedServiceBuilder withGoid(Goid goid) {
            when(publishedService.getGoid()).thenReturn(goid);
            return this;
        }

        PublishedServiceBuilder withPolicy(Policy policy) {
            when(publishedService.getPolicy()).thenReturn(policy);
            return this;
        }

        PublishedService build() {
            when(publishedService.isDisabled()).thenReturn(false);
            return publishedService;
        }
    }

    class DebugManagerBuilder {
        private final DebugManager debugManager;

        DebugManagerBuilder() {
            this.debugManager = mock(DebugManager.class);
        }

        DebugManager build() {
            return debugManager;
        }
    }

    class GatewayMetricsEventPublisherBuilder {
        private final MockGatewayMetricsPublisher gatewayMetricsPublisher;

        GatewayMetricsEventPublisherBuilder() {
            this.gatewayMetricsPublisher = mock(MockGatewayMetricsPublisher.class);
        }

        MockGatewayMetricsPublisher build() {
            when(gatewayMetricsPublisher.hasSubscribers()).thenReturn(true);
            return gatewayMetricsPublisher;
        }
    }

    class PolicyBuilder {
        private final Policy policy;

        PolicyBuilder() {
            this.policy = mock(Policy.class);
        }

        PolicyBuilder withGoid(Goid goid) {
            when(policy.getGoid()).thenReturn(goid);
            return this;
        }

        Policy build() {
            return policy;
        }
    }

    class MessageProcessorInjectorBuilder {
        private final MessageProcessorInjector messageProcessorInjector;

        MessageProcessorInjectorBuilder() {
            messageProcessorInjector = mock(MessageProcessorInjector.class);
        }

        MessageProcessorInjector build() {
            return messageProcessorInjector;
        }
    }

}