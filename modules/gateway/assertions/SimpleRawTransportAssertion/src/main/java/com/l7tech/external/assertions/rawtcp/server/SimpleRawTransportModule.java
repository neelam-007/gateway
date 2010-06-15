package com.l7tech.external.assertions.rawtcp.server;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.GatewayState;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.identity.cert.TrustedCertServices;
import com.l7tech.server.transport.ListenerException;
import com.l7tech.server.transport.SsgConnectorManager;
import com.l7tech.server.transport.TransportModule;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

/**
 *
 */
public class SimpleRawTransportModule extends TransportModule {
    private static final Logger logger = Logger.getLogger(SimpleRawTransportModule.class.getName());
    private static final Set<String> SUPPORTED_SCHEMES = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    static {
        SUPPORTED_SCHEMES.addAll(Arrays.asList("l7.raw.tcp"));
        SUPPORTED_SCHEMES.addAll(Arrays.asList("l7.raw.udp"));
    }

    private final GatewayState gatewayState;

    public SimpleRawTransportModule(LicenseManager licenseManager,
                                    SsgConnectorManager ssgConnectorManager,
                                    TrustedCertServices trustedCertServices,
                                    DefaultKey defaultKey,
                                    ServerConfig serverConfig,
                                    GatewayState gatewayState)
    {
        super("Simple raw transport module", logger, null, licenseManager, ssgConnectorManager, trustedCertServices, defaultKey, serverConfig);
        this.gatewayState = gatewayState;
    }

    private static <T> T getBean(BeanFactory beanFactory, String beanName, Class<T> beanClass) {
        @SuppressWarnings({"unchecked"}) T got = (T)beanFactory.getBean(beanName, beanClass);
        if (got != null && beanClass.isAssignableFrom(got.getClass()))
            return got;
        throw new IllegalStateException("uanble to get get: " + beanName);

    }

    static SimpleRawTransportModule createModule(ApplicationContext appContext) {
        LicenseManager licenseManager = getBean(appContext, "licenseManager", LicenseManager.class);
        SsgConnectorManager ssgConnectorManager = getBean(appContext, "ssgConnectorManager", SsgConnectorManager.class);
        TrustedCertServices trustedCertServices = getBean(appContext, "trustedCertServices", TrustedCertServices.class);
        DefaultKey defaultKey = getBean(appContext, "defaultKey", DefaultKey.class);
        ServerConfig serverConfig = getBean(appContext, "serverConfig", ServerConfig.class);
        GatewayState gatewayState = getBean(appContext, "gatewayState", GatewayState.class);
        return new SimpleRawTransportModule(licenseManager, ssgConnectorManager, trustedCertServices, defaultKey, serverConfig, gatewayState);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        super.onApplicationEvent(applicationEvent);

        if (applicationEvent instanceof ReadyForMessages) {
            startInitialConnectors();
        }
    }

    private void startInitialConnectors() {
        // TODO
    }

    @Override
    protected void doStart() throws LifecycleException {
        super.doStart();
        registerCustomProtocols();
        if (gatewayState.isReadyForMessages()) {
            startInitialConnectors();
        }
    }

    private void registerCustomProtocols() {
        for (String scheme : SUPPORTED_SCHEMES) {
            ssgConnectorManager.registerCustomProtocol(scheme, this);
        }
    }

    @Override
    protected void doClose() throws LifecycleException {
        super.doClose();
        unregisterCustomProtocols();
    }

    private void unregisterCustomProtocols() {
        for (String scheme : SUPPORTED_SCHEMES) {
            ssgConnectorManager.unregisterCustomProtocol(scheme, this);
        }        
    }

    @Override
    protected void addConnector(SsgConnector connector) throws ListenerException {
        // TODO
    }

    @Override
    protected void removeConnector(long oid) {
        // TODO
    }

    @Override
    protected Set<String> getSupportedSchemes() {
        return SUPPORTED_SCHEMES;
    }
}
