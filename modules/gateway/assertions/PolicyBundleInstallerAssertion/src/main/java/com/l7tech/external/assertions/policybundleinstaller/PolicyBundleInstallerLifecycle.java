package com.l7tech.external.assertions.policybundleinstaller;

import com.l7tech.gateway.common.LicenseException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.server.event.wsman.InstallPolicyBundleEvent;
import com.l7tech.server.event.system.Initializing;
import com.l7tech.server.event.wsman.WSManagementRequestEvent;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.bundle.BundleResolver;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerContext;
import com.l7tech.server.policy.bundle.PreBundleSavePolicyCallback;
import com.l7tech.server.util.ApplicationEventProxy;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PolicyBundleInstallerLifecycle implements ApplicationListener{

    public PolicyBundleInstallerLifecycle(final ApplicationContext spring) {
        this.applicationEventProxy = spring.getBean("applicationEventProxy", ApplicationEventProxy.class);
        this.applicationEventProxy.addApplicationListener(this);
        this.spring = spring;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {

        if (applicationEvent instanceof Initializing) {
            configureBeans();
            return;
        } else if (!(applicationEvent instanceof WSManagementRequestEvent)) {
            return;
        }

        WSManagementRequestEvent mgmtRequest = (WSManagementRequestEvent) applicationEvent;
        if (mgmtRequest.isProcessed()) {
            return;
        }

        if(!"http://ns.l7tech.com/2010/04/gateway-management".equals(mgmtRequest.getBundleVersionNs())){
            // not applicable
            return;
        }

        if (applicationEvent instanceof InstallPolicyBundleEvent) {

            // process event
            final InstallPolicyBundleEvent bundleEvent = (InstallPolicyBundleEvent) applicationEvent;

            if(!"http://ns.l7tech.com/2012/09/policy-bundle".equals(bundleEvent.getPolicyBundleVersionNs())){
                // not applicable
                return;
            }

            final BundleResolver bundleResolver = bundleEvent.getBundleResolver();
            final PreBundleSavePolicyCallback savePolicyCallback = bundleEvent.getPreBundleSavePolicyCallback();
            final PolicyBundleInstaller installer = new PolicyBundleInstaller(bundleResolver, savePolicyCallback, new GatewayManagementInvoker() {
                @Override
                public AssertionStatus checkRequest(PolicyEnforcementContext context) throws PolicyAssertionException, IOException {
                    return serverMgmtAssertion.checkRequest(context);
                }
            });

            final PolicyBundleInstallerContext context = bundleEvent.getContext();
            try {
                installer.install(context);

            } catch (PolicyBundleInstaller.InstallationException e) {
                bundleEvent.setProcessingException(e);
            } catch (BundleResolver.UnknownBundleException e) {
                bundleEvent.setProcessingException(e);
            } catch (BundleResolver.BundleResolverException e) {
                bundleEvent.setProcessingException(e);
            } catch (InterruptedException e) {
                bundleEvent.setProcessingException(e);
            } catch (BundleResolver.InvalidBundleException e) {
                bundleEvent.setProcessingException(e);
            } catch (Exception e) {
                // catch all for any runtime exceptions
                bundleEvent.setProcessingException(e);
            }
            bundleEvent.setProcessed(true);
        }
    }

    /**
     * Wired via PolicyBundleInstallerAssertion meta data.
     *
     * @param context spring application context
     */
    public static synchronized void onModuleLoaded(final ApplicationContext context) {

        if (instance != null) {
            logger.log(Level.WARNING, "PolicyBundleInstaller module is already initialized");
        } else {
            instance = new PolicyBundleInstallerLifecycle(context);
        }
    }

    // - PRIVATE
    private static PolicyBundleInstallerLifecycle instance = null;
    private static final Logger logger = Logger.getLogger(PolicyBundleInstallerLifecycle.class.getName());
    private final ApplicationEventProxy applicationEventProxy;
    private final ApplicationContext spring;
    private ServerAssertion serverMgmtAssertion; // multi threaded

    private final String GATEWAY_MGMT_POLICY_XML = "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:GatewayManagement/>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>\n";


    private void configureBeans() {
        //todo reduce level
        logger.info("Initializing OAuth Installer.");

        final WspReader wspReader = spring.getBean("wspReader", WspReader.class);
        final ServerPolicyFactory serverPolicyFactory = spring.getBean("policyFactory", ServerPolicyFactory.class);

        try {
            final Assertion assertion = wspReader.parseStrictly(GATEWAY_MGMT_POLICY_XML, WspReader.Visibility.omitDisabled);
            serverMgmtAssertion = serverPolicyFactory.compilePolicy(assertion, false);
        } catch (ServerPolicyException e) {
            // todo log and audit with stack trace
            throw new RuntimeException(e);
        } catch (LicenseException e) {
            // todo log and audit with stack trace
            throw new RuntimeException(e);
        } catch (IOException e) {
            // todo log and audit with stack trace
            throw new RuntimeException(e);
        }
    }
}
