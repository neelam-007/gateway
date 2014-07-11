package com.l7tech.external.assertions.xmppassertion.server;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.decorator.WssDecorator;
import com.l7tech.security.xml.processor.SecurityContextFinder;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.audit.AuditContextFactory;
import com.l7tech.server.audit.MessageSummaryAuditFactory;
import com.l7tech.server.log.TrafficLogger;
import com.l7tech.server.policy.PolicyCache;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.server.service.ServiceMetricsServices;
import com.l7tech.util.Config;
import org.springframework.context.ApplicationEventPublisher;

/**
 * User: rseminoff
 * Date: 24/05/12
 */
public class MockMessageProcessor extends MessageProcessor {
    /**
     * Create the new <code>MessageProcessor</code> instance with the service
     * manager, Wss Decorator instance and the server private key.
     * All arguments are required
     *
     * @param sc                            the service cache
     * @param pc                            the policy cache
     * @param wssd                          the Wss Decorator
     * @param securityTokenResolver         the security token resolver to use
     * @param licenseManager                the SSG's Licence Manager
     * @param metricsServices               the SSG's ServiceMetricsManager
     * @param auditContextFactory           audit context factory for message processing
     * @param config                        config provider
     * @param trafficLogger                 traffic logger
     * @param messageProcessingEventChannel channel on which to publish the message processed event (for auditing)
     * @throws IllegalArgumentException if any of the arguments is null
     */
    public MockMessageProcessor(final ServiceCache sc, final PolicyCache pc, final WssDecorator wssd, final SecurityTokenResolver securityTokenResolver, final SecurityContextFinder securityContextFinder, final LicenseManager licenseManager, final ServiceMetricsServices metricsServices, final AuditContextFactory auditContextFactory, final MessageSummaryAuditFactory messageSummaryAuditFactory, final Config config, final TrafficLogger trafficLogger, ApplicationEventPublisher messageProcessingEventChannel) throws IllegalArgumentException {
        super(sc, pc, wssd, securityTokenResolver, securityContextFinder, licenseManager, metricsServices, auditContextFactory, messageSummaryAuditFactory, config, trafficLogger, messageProcessingEventChannel);
    }

}
