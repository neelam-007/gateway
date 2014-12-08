package com.l7tech.external.assertions.policybundleinstaller;

import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.server.event.bundle.DryRunInstallPolicyBundleEvent;
import com.l7tech.server.event.bundle.GatewayManagementRequestEvent;
import com.l7tech.server.event.bundle.InstallPolicyBundleEvent;
import com.l7tech.server.event.bundle.PolicyBundleInstallerEvent;
import com.l7tech.server.event.system.DetailedSystemEvent;
import com.l7tech.server.event.system.LicenseChangeEvent;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerAssertionRegistry;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.bundle.BundleResolver;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerCallback;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerContext;
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

import static com.l7tech.server.policy.bundle.BundleUtils.NS_BUNDLE;
import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.AccessDeniedManagementResponse;
import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.UnexpectedManagementResponse;
import static com.l7tech.util.Functions.Nullary;

public class PolicyBundleInstallerLifecycle implements ApplicationListener {

    public PolicyBundleInstallerLifecycle(final ApplicationContext spring) {
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

        if (applicationEvent instanceof PolicyBundleInstallerEvent) {

            if (!isLicensed.get()) {
                return;
            }

            // process event
            final PolicyBundleInstallerEvent bundleInstallerEvent = (PolicyBundleInstallerEvent) applicationEvent;

            if (!NS_BUNDLE.equals(bundleInstallerEvent.getPolicyBundleVersionNs())) {
                // not applicable
                return;
            }

            if (serverMgmtAssertion.get() == null) {
                // if we are licensed and no assertion, then we need to configure.
                // this works around issue with Gateway app context creation, there is currently no way of knowing
                // when it's safe to initialize. Issue is with bean 'wspReader'.
                configureBeans();
                if (serverMgmtAssertion.get() == null) {
                    bundleInstallerEvent.setReasonNotProcessed("Bundle installer is not initialized");
                    return;
                }
            }

            if (applicationEvent instanceof InstallPolicyBundleEvent) {
                processInstallEvent((InstallPolicyBundleEvent) applicationEvent);
            } else if (applicationEvent instanceof DryRunInstallPolicyBundleEvent) {
                processDryRunEvent((DryRunInstallPolicyBundleEvent) applicationEvent);
            }
        }
    }

    /**
     * Wired via PolicyBundleInstallerAssertion meta data.
     *
     * @param context spring application context
     */
    public static synchronized void onModuleLoaded(final ApplicationContext context) {

        if (instance != null) {
            logger.log(Level.WARNING, "Bundle Installer module is already initialized");
        } else {
            instance = new PolicyBundleInstallerLifecycle(context);
        }
    }

    public static synchronized void onModuleUnloaded() {
        if (instance != null) {
            logger.log(Level.FINE, "Bundle Installer module is shutting down");
            instance = null;
        }
    }

    // - PRIVATE
    private static PolicyBundleInstallerLifecycle instance = null;
    private static final Logger logger = Logger.getLogger(PolicyBundleInstallerLifecycle.class.getName());
    private final ApplicationContext spring;
    private final AtomicReference<ServerAssertion> serverMgmtAssertion = new AtomicReference<>();
    private final AtomicReference<ServerAssertion> serverRestMgmtAssertion = new AtomicReference<>();
    private final AtomicBoolean isLicensed = new AtomicBoolean(false);
    private final AtomicReference<ServiceManager> serviceManager = new AtomicReference<>();
    private final AtomicReference<ServerAssertionRegistry> assertionRegistry = new AtomicReference<>();

    private static final String GATEWAY_MGMT_POLICY_XML = "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:GatewayManagement/>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>\n";
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
        return licMan.isFeatureEnabled(new PolicyBundleInstallerAssertion().getFeatureSetName());
    }


    /**
     * This cannot be called from the constructor as the beans required are not available until after all modules are
     * loaded. Calling this during construction will cause a spring invocation error when trying to get wspReader.
     */
    private synchronized void configureBeans() {
        if (!isLicensed()) {
            isLicensed.set(false);
            logger.warning("Bundle Installer module is not licensed and will not be available.");
            serverMgmtAssertion.set(null);
            serviceManager.set(null);
        } else {
            if (isLicensed.compareAndSet(false, true)) {
                logger.info("Bundle Installer module is now licensed.");
            }
            if (serverMgmtAssertion.get() == null || serviceManager.get() == null) {
                logger.info("Initializing Bundle Installer.");

                if (serverMgmtAssertion.get() == null) {
                    final WspReader wspReader = spring.getBean("wspReader", WspReader.class);
                    final ServerPolicyFactory serverPolicyFactory = spring.getBean("policyFactory", ServerPolicyFactory.class);
                    try {
                        Assertion assertion = wspReader.parseStrictly(GATEWAY_MGMT_POLICY_XML, WspReader.Visibility.omitDisabled);
                        serverMgmtAssertion.compareAndSet(null, serverPolicyFactory.compilePolicy(assertion, false));
                        assertion = wspReader.parseStrictly(REST_GATEWAY_MGMT_POLICY_XML, WspReader.Visibility.omitDisabled);
                        serverRestMgmtAssertion.compareAndSet(null, serverPolicyFactory.compilePolicy(assertion, false));
                    } catch (ServerPolicyException e) {
                        handleInitException(e, "Could not create Gateway Management assertion");
                    } catch (LicenseException e) {
                        handleInitException(e, "Gateway Management assertion is not licensed");
                    } catch (IOException e) {
                        handleInitException(e, "Gateway Management assertion is not available");
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

    static class BundleInstallerLifecycleEvent extends DetailedSystemEvent {
        private final String action;
        public BundleInstallerLifecycleEvent(final Object source, final String note, final Level level, final String action) {
            super(source, Component.GW_BUNDLE_INSTALLER, null, level, note);
            this.action = action;
        }

        @Override
        public String getAction() {
            return action;
        }
    }

    private void processInstallEvent(final InstallPolicyBundleEvent installEvent) {
        final PolicyBundleInstallerContext context = installEvent.getContext();
        final PolicyBundleInstaller installer = new PolicyBundleInstaller(
                getGatewayMgmtInvoker(serverMgmtAssertion.get()), getGatewayMgmtInvoker(serverRestMgmtAssertion.get()), context, serviceManager.get(), new Nullary<Boolean>() {
            @Override
            public Boolean call() {
                return installEvent.isCancelled();
            }
        });
        installer.setPolicyBundleInstallerCallback(installEvent.getPolicyBundleInstallerCallback());
        installer.setAuthenticatedUser(installEvent.getAuthenticatedUser());

        try {
            installer.installBundle();
        } catch (PolicyBundleInstaller.InstallationException | BundleResolver.UnknownBundleException | BundleResolver.BundleResolverException
                | InterruptedException | BundleResolver.InvalidBundleException | UnexpectedManagementResponse | AccessDeniedManagementResponse | IOException | RuntimeException e) {
            installEvent.setProcessingException(e);
        }

        installEvent.setProcessed(true);
    }

    private void processDryRunEvent(final DryRunInstallPolicyBundleEvent dryRunEvent) {
        final PolicyBundleInstallerContext context = dryRunEvent.getContext();
        final PolicyBundleInstaller installer = new PolicyBundleInstaller(
                getGatewayMgmtInvoker(serverMgmtAssertion.get()), getGatewayMgmtInvoker(serverRestMgmtAssertion.get()), context, serviceManager.get(), new Functions.Nullary<Boolean>() {
            @Override
            public Boolean call() {
                return dryRunEvent.isCancelled();
            }
        });
        installer.setPolicyBundleInstallerCallback(dryRunEvent.getPolicyBundleInstallerCallback());
        installer.setAuthenticatedUser(dryRunEvent.getAuthenticatedUser());

        try {
            installer.dryRunInstallBundle(dryRunEvent);
        } catch (BundleResolver.BundleResolverException | BundleResolver.UnknownBundleException | BundleResolver.InvalidBundleException
                | InterruptedException | AccessDeniedManagementResponse | PolicyBundleInstallerCallback.CallbackException e) {
            dryRunEvent.setProcessingException(e);
        } finally {
            dryRunEvent.setProcessed(true);
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
        serverMgmtAssertion.set(null);
        serverRestMgmtAssertion.set(null);
        final BundleInstallerLifecycleEvent problemEvent = new BundleInstallerLifecycleEvent(this, "Could not initialize bundle installer", Level.WARNING, "Initialization");
        final String details =  logicalCause + ". " + ExceptionUtils.getMessage(e);
        problemEvent.setAuditDetails(Arrays.asList(
                new AuditDetail(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                        new String[]{details}, ExceptionUtils.getDebugException(e))));

        spring.publishEvent(problemEvent);
    }
}
