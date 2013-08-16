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
import com.l7tech.server.event.system.DetailedSystemEvent;
import com.l7tech.server.event.system.LicenseChangeEvent;
import com.l7tech.server.event.wsman.DryRunInstallPolicyBundleEvent;
import com.l7tech.server.event.wsman.InstallPolicyBundleEvent;
import com.l7tech.server.event.wsman.PolicyBundleEvent;
import com.l7tech.server.event.wsman.WSManagementRequestEvent;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.bundle.BundleResolver;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerContext;
import com.l7tech.server.policy.bundle.PreBundleSavePolicyCallback;
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

import static com.l7tech.util.Functions.Nullary;

public class PolicyBundleInstallerLifecycle implements ApplicationListener{

    public PolicyBundleInstallerLifecycle(final ApplicationContext spring) {
        this.applicationEventProxy = spring.getBean("applicationEventProxy", ApplicationEventProxy.class);
        this.applicationEventProxy.addApplicationListener(this);
        this.spring = spring;
        isLicensed.set(isLicensed());
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof LicenseChangeEvent) {
            configureBeans();
            return;
        }

        if (!(applicationEvent instanceof WSManagementRequestEvent)) {
            return;
        }

        WSManagementRequestEvent mgmtRequest = (WSManagementRequestEvent) applicationEvent;
        if (mgmtRequest.isProcessed()) {
            return;
        }

        if (!"http://ns.l7tech.com/2010/04/gateway-management".equals(mgmtRequest.getBundleVersionNs())) {
            // not applicable
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

            if (serverMgmtAssertion.get() == null) {
                // if we are licensed and no assertion, then we need to configure.
                // this works around issue with Gateway app context creation, there is currently no way of knowing
                // when it's safe to initialize. Issue is with bean 'wspReader'.
                configureBeans();
                if (serverMgmtAssertion.get() == null) {
                    bundleEvent.setReasonNotProcessed("Bundle installer is not initialized");
                    return;
                }
            }

            if (applicationEvent instanceof InstallPolicyBundleEvent) {
                final InstallPolicyBundleEvent installEvent = (InstallPolicyBundleEvent) applicationEvent;
                processInstallEvent(installEvent);
            } else if (applicationEvent instanceof DryRunInstallPolicyBundleEvent) {
                final DryRunInstallPolicyBundleEvent dryRunEvent = (DryRunInstallPolicyBundleEvent) applicationEvent;

                final PolicyBundleInstallerContext context = dryRunEvent.getContext();

                final PolicyBundleInstaller installer = new PolicyBundleInstaller(getGatewayMgmtInvoker(), context, new Functions.Nullary<Boolean>() {
                                    @Override
                                    public Boolean call() {
                                        return dryRunEvent.isCancelled();
                                    }
                                });

                try {
                    installer.dryRunInstallBundle(dryRunEvent);
                } catch (BundleResolver.BundleResolverException e) {
                    dryRunEvent.setProcessingException(e);
                } catch (BundleResolver.UnknownBundleException e) {
                    dryRunEvent.setProcessingException(e);
                } catch (BundleResolver.InvalidBundleException e) {
                    dryRunEvent.setProcessingException(e);
                } catch (InterruptedException e) {
                    dryRunEvent.setProcessingException(e);
                } catch (AccessDeniedManagementResponse e) {
                    dryRunEvent.setProcessingException(e);
                } finally {
                    dryRunEvent.setProcessed(true);
                }
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
    private final ApplicationEventProxy applicationEventProxy;
    private final ApplicationContext spring;
    private final AtomicReference<ServerAssertion> serverMgmtAssertion = new AtomicReference<ServerAssertion>();
    private final AtomicBoolean isLicensed = new AtomicBoolean(false);

    private static final String GATEWAY_MGMT_POLICY_XML = "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:GatewayManagement/>\n" +
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
        } else {
            if (isLicensed.compareAndSet(false, true)) {
                logger.info("Bundle Installer module is now licensed.");
            }
            if (serverMgmtAssertion.get() == null) {
                logger.info("Initializing Bundle Installer.");

                final WspReader wspReader = spring.getBean("wspReader", WspReader.class);
                final ServerPolicyFactory serverPolicyFactory = spring.getBean("policyFactory", ServerPolicyFactory.class);

                try {
                    final Assertion assertion = wspReader.parseStrictly(GATEWAY_MGMT_POLICY_XML, WspReader.Visibility.omitDisabled);
                    serverMgmtAssertion.compareAndSet(null, serverPolicyFactory.compilePolicy(assertion, false));
                } catch (ServerPolicyException e) {
                    handleInitException(e, "Could not create Gateway Management assertion");
                } catch (LicenseException e) {
                    handleInitException(e, "Gateway Management assertion is not licensed");
                } catch (IOException e) {
                    handleInitException(e, "Gateway Management assertion is not available");
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
        final PreBundleSavePolicyCallback savePolicyCallback = installEvent.getPreBundleSavePolicyCallback();
        final PolicyBundleInstallerContext context = installEvent.getContext();
        final PolicyBundleInstaller installer = new PolicyBundleInstaller(getGatewayMgmtInvoker(), context, new Nullary<Boolean>() {
            @Override
            public Boolean call() {
                return installEvent.isCancelled();
            }
        });
        installer.setSavePolicyCallback(savePolicyCallback);

        try {
            installer.installBundle();
        } catch (PolicyBundleInstaller.InstallationException e) {
            installEvent.setProcessingException(e);
        } catch (BundleResolver.UnknownBundleException e) {
            installEvent.setProcessingException(e);
        } catch (BundleResolver.BundleResolverException e) {
            installEvent.setProcessingException(e);
        } catch (InterruptedException e) {
            installEvent.setProcessingException(e);
        } catch (BundleResolver.InvalidBundleException e) {
            installEvent.setProcessingException(e);
        } catch (UnexpectedManagementResponse e) {
            installEvent.setProcessingException(e);
        } catch (AccessDeniedManagementResponse e) {
            installEvent.setProcessingException(e);
        } catch (IOException e) {
            installEvent.setProcessingException(e);
        } catch (RuntimeException e) {
            // catch all for any runtime exceptions
            installEvent.setProcessingException(e);
        }

        installEvent.setProcessed(true);
    }

    private GatewayManagementInvoker getGatewayMgmtInvoker() {
        return new GatewayManagementInvoker() {
            final ServerAssertion serverAssertion = serverMgmtAssertion.get();
            @Override
            public AssertionStatus checkRequest(PolicyEnforcementContext context) throws PolicyAssertionException, IOException {
                if (serverAssertion != null) {
                    return serverAssertion.checkRequest(context);
                }

                throw new IOException("No gateway management assertion is available");
            }
        };
    }

    private void handleInitException(Exception e, @NotNull String logicalCause) {
        serverMgmtAssertion.set(null);
        final BundleInstallerLifecycleEvent problemEvent = new BundleInstallerLifecycleEvent(this, "Could not initialize bundle installer", Level.WARNING, "Initialization");
        final String details =  logicalCause + ". " + ExceptionUtils.getMessage(e);
        problemEvent.setAuditDetails(Arrays.asList(
                new AuditDetail(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                        new String[]{details}, ExceptionUtils.getDebugException(e))));

        spring.publishEvent(problemEvent);
    }
}
