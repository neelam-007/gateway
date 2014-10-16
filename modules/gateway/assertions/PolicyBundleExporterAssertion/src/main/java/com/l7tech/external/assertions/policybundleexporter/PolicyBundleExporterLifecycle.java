package com.l7tech.external.assertions.policybundleexporter;

import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.server.event.bundle.GatewayManagementRequestEvent;
import com.l7tech.server.event.bundle.PolicyBundleEvent;
import com.l7tech.server.event.system.DetailedSystemEvent;
import com.l7tech.server.event.system.LicenseChangeEvent;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerAssertionRegistry;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.bundle.ssgman.GatewayManagementInvoker;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.AccessDeniedManagementResponse;
import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.UnexpectedManagementResponse;

public class PolicyBundleExporterLifecycle implements ApplicationListener {

    public PolicyBundleExporterLifecycle(final ApplicationContext spring) {
        ApplicationEventProxy applicationEventProxy = spring.getBean("applicationEventProxy", ApplicationEventProxy.class);
        applicationEventProxy.addApplicationListener(this);
        this.spring = spring;
        isLicensed.set(isLicensed());
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof LicenseChangeEvent) {
            configureBeans();
            return;
        }

        if (!(applicationEvent instanceof GatewayManagementRequestEvent)) {
            return;
        }

        GatewayManagementRequestEvent mgmtRequest = (GatewayManagementRequestEvent) applicationEvent;
        if (mgmtRequest.isProcessed()) {
            return;
        }

        if (applicationEvent instanceof PolicyBundleEvent) {

            if (!isLicensed.get()) {
                return;
            }

            // process event
            final PolicyBundleEvent bundleEvent = (PolicyBundleEvent) applicationEvent;

            if (!"http://ns.l7tech.com/2012/09/policy-bundle".equals(bundleEvent.getPolicyBundleVersionNs())) {
                // not applicable
                return;
            }

            if (serverRestMgmtAssertion.get() == null) {
                // if we are licensed and no assertion, then we need to configure.
                // this works around issue with Gateway app context creation, there is currently no way of knowing
                // when it's safe to initialize. Issue is with bean 'wspReader'.
                configureBeans();
                if (serverRestMgmtAssertion.get() == null) {
                    bundleEvent.setReasonNotProcessed("Bundle exporter is not initialized");
                    return;
                }
            }

            if (applicationEvent instanceof PolicyBundleExporterEvent) {
                processExportEvent((PolicyBundleExporterEvent) applicationEvent);
            }
        }
    }

    /**
     * Wired via PolicyBundleExporterAssertion meta data.
     *
     * @param context spring application context
     */
    public static synchronized void onModuleLoaded(final ApplicationContext context) {

        if (instance != null) {
            logger.log(Level.WARNING, "Bundle Exporter module is already initialized");
        } else {
            instance = new PolicyBundleExporterLifecycle(context);
        }
    }

    public static synchronized void onModuleUnloaded() {
        if (instance != null) {
            logger.log(Level.FINE, "Bundle Exporter module is shutting down");
            instance = null;
        }
    }

    // - PRIVATE
    private static PolicyBundleExporterLifecycle instance = null;
    private static final Logger logger = Logger.getLogger(PolicyBundleExporterLifecycle.class.getName());
    private final ApplicationContext spring;
    private final AtomicReference<ServerAssertion> serverRestMgmtAssertion = new AtomicReference<>();
    private final AtomicBoolean isLicensed = new AtomicBoolean(false);
    private final AtomicReference<ServiceManager> serviceManager = new AtomicReference<>();
    private final AtomicReference<ServerAssertionRegistry> assertionRegistry = new AtomicReference<>();

    private static final String REST_GATEWAY_MGMT_POLICY_XML = "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:RESTGatewayManagement>\n" +
            "            <L7p:OtherTargetMessageVariable stringValue=\"request\"/>\n" +
            "            <L7p:Target target=\"OTHER\"/>\n" +
            "        </L7p:RESTGatewayManagement>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>\n";

    private boolean isLicensed() {
        LicenseManager licMan = spring.getBean("licenseManager", LicenseManager.class);
        return licMan.isFeatureEnabled(new PolicyBundleExporterAssertion().getFeatureSetName());
    }


    /**
     * This cannot be called from the constructor as the beans required are not available until after all modules are
     * loaded. Calling this during construction will cause a spring invocation error when trying to get wspReader.
     */
    private synchronized void configureBeans() {
        if (!isLicensed()) {
            isLicensed.set(false);
            logger.warning("Bundle Exporter module is not licensed and will not be available.");
            serverRestMgmtAssertion.set(null);
            serviceManager.set(null);
        } else {
            if (isLicensed.compareAndSet(false, true)) {
                logger.info("Bundle Exporter module is now licensed.");
            }
            if (serverRestMgmtAssertion.get() == null || serviceManager.get() == null) {
                logger.info("Initializing Bundle Exporter.");

                if (serverRestMgmtAssertion.get() == null) {
                    final WspReader wspReader = spring.getBean("wspReader", WspReader.class);
                    final ServerPolicyFactory serverPolicyFactory = spring.getBean("policyFactory", ServerPolicyFactory.class);
                    try {
                        Assertion assertion = wspReader.parseStrictly(REST_GATEWAY_MGMT_POLICY_XML, WspReader.Visibility.omitDisabled);
                        serverRestMgmtAssertion.compareAndSet(null, serverPolicyFactory.compilePolicy(assertion, false));
                    } catch (ServerPolicyException e) {
                        handleInitException(e, "Could not create REST Gateway Management assertion");
                    } catch (LicenseException e) {
                        handleInitException(e, "REST Gateway Management assertion is not licensed");
                    } catch (IOException e) {
                        handleInitException(e, "REST Gateway Management assertion is not available");
                    }
                }

                if (serviceManager.get() == null) {
                    serviceManager.set(spring.getBean("serviceManager", ServiceManager.class));
                }

                if (assertionRegistry.get() == null) {
                    assertionRegistry.set(spring.getBean("assertionRegistry", ServerAssertionRegistry.class));
                }
            }
        }
    }

    static class BundleExporterLifecycleEvent extends DetailedSystemEvent {
        private final String action;
        public BundleExporterLifecycleEvent(final Object source, final String note, final Level level, final String action) {
            super(source, Component.GW_BUNDLE_EXPORTER, null, level, note);
            this.action = action;
        }

        @Override
        public String getAction() {
            return action;
        }
    }

    private void processExportEvent(final PolicyBundleExporterEvent exportEvent) {
        final MigrationBundleExporter exporter = new MigrationBundleExporter(getGatewayMgmtInvoker(serverRestMgmtAssertion.get()), exportEvent.getExportContext(), assertionRegistry.get(), new Functions.Nullary<Boolean>() {
            @Override
            public Boolean call() {
                return exportEvent.isCancelled();
            }
        });

        try {
            exporter.export(exportEvent);
        } catch (IOException | InterruptedException | UnexpectedManagementResponse | AccessDeniedManagementResponse e) {
            exportEvent.setProcessingException(e);
        } finally {
            exportEvent.setProcessed(true);
        }
    }

    private GatewayManagementInvoker getGatewayMgmtInvoker(@NotNull final ServerAssertion serverAssertion) {
        return new GatewayManagementInvoker() {
            @Override
            public AssertionStatus checkRequest(PolicyEnforcementContext context) throws PolicyAssertionException, IOException {
                    return serverAssertion.checkRequest(context);
            }
        };
    }

    private void handleInitException(Exception e, @NotNull String logicalCause) {
        serverRestMgmtAssertion.set(null);
        serverRestMgmtAssertion.set(null);
        final BundleExporterLifecycleEvent problemEvent = new BundleExporterLifecycleEvent(this, "Could not initialize bundle exporter", Level.WARNING, "Initialization");
        final String details =  logicalCause + ". " + ExceptionUtils.getMessage(e);
        problemEvent.setAuditDetails(Arrays.asList(
                new AuditDetail(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                        new String[]{details}, ExceptionUtils.getDebugException(e))));

        spring.publishEvent(problemEvent);
    }
}
