package com.l7tech.external.assertions.policybundleexporter.server;

import com.l7tech.external.assertions.policybundleexporter.AarFileGenerator;
import com.l7tech.external.assertions.policybundleexporter.PolicyBundleExporterAdmin;
import com.l7tech.external.assertions.policybundleexporter.PolicyBundleExporterProperties;
import com.l7tech.external.assertions.policybundleexporter.PolicyBundleExporterEvent;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.admin.AsyncAdminMethodsImpl;
import com.l7tech.server.event.AdminInfo;
import com.l7tech.server.event.admin.DetailedAdminEvent;
import com.l7tech.server.policy.module.ModularAssertionModulesConfig;
import com.l7tech.server.policy.module.ModulesConfig;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;

/**
 * Implementation for client side (Policy Manager) calls to the server (Gateway) to support installer export.
 */
public class PolicyBundleExporterAdminImpl extends AsyncAdminMethodsImpl implements PolicyBundleExporterAdmin {
    private final ApplicationEventPublisher appEventPublisher;
    private final ExecutorService executorService;
    private final ModulesConfig modulesConfig;

    public PolicyBundleExporterAdminImpl(@NotNull final ApplicationContext appContext) {
        appEventPublisher = appContext;
        executorService = Executors.newCachedThreadPool();
        modulesConfig = new ModularAssertionModulesConfig(appContext.getBean("serverConfig", ServerConfig.class), appContext.getBean("licenseManager", LicenseManager.class));
    }

    @NotNull
    @Override
    public JobId<PolicyBundleExporterAdmin.InstallerAarFile> generateInstallerAarFile(@NotNull final PolicyBundleExporterProperties exportProperties) throws IOException {
        final Future<InstallerAarFile> future = executorService.submit(AdminInfo.find(false).wrapCallable(new Callable<InstallerAarFile>() {
            @Override
            public InstallerAarFile call() throws Exception {
                try {
                    final PolicyBundleExporterEvent exportPolicyBundleEvent = new PolicyBundleExporterEvent(this, new PolicyBundleExporterContext(exportProperties.getBundleFolder().getGoid(), exportProperties.getComponentInfoList()));
                    appEventPublisher.publishEvent(exportPolicyBundleEvent);

                    // TODO need to validate
                    //        if (validateEventProcessed(exportPolicyBundleEvent)) {
                    //            break outer;
                    //        }

                    exportProperties.setComponentRestmanBundleXmls(exportPolicyBundleEvent.getComponentRestmanBundleXmls());

                    return new InstallerAarFile(
                        new AarFileGenerator().generateInstallerAarFile(exportProperties),
                        exportPolicyBundleEvent.getServerModuleFileNames(),    // TODO using reference should be safe ... or do we need to make a copy of ServerModuleFileNames?
                        exportPolicyBundleEvent.getAssertionFeatureSetNames());   // TODO using reference should be safe ... or do we need to make a copy of AssertionFeatureSetNames?
                } catch(IOException e) {
                    final String message = "Problem during export of folder " + exportProperties.getBundleFolder().getPath() + " as " + exportProperties.getBundleName();
                    final DetailedAdminEvent problemEvent = new DetailedAdminEvent(this, message, Level.WARNING);
                    //  TODO need to audit
                    // problemEvent.setAuditDetails(Arrays.asList(newAuditDetailExportError(e, message)));
                    appEventPublisher.publishEvent(problemEvent);
                    throw e;
                }

            }
        }));

        return registerJob(future, InstallerAarFile.class);
    }

    @NotNull
    @Override
    public JobId<ServerModuleFile> getServerModuleFile(@NotNull final String serverModuleFileName) throws IOException {
        final Future<ServerModuleFile> future = executorService.submit(AdminInfo.find(false).wrapCallable(new Callable<ServerModuleFile>() {
            @Override
            public ServerModuleFile call() throws Exception {
                // get the Modular Assertion module file
                final File serverModuleFile = new File(modulesConfig.getModuleDir().getAbsolutePath() + File.separator + serverModuleFileName);

                // TODO test for .jar suffix and get Custom Assertion using CustomAssertionModulesConfig

                return new ServerModuleFile(IOUtils.slurpFile(serverModuleFile));
            }
        }));

        return registerJob(future, ServerModuleFile.class);
    }

    @NotNull
    private AuditDetail newAuditDetailExportError(@NotNull final IOException e, @NotNull final String message) {
        // TODO replace POLICY_BUNDLE_INSTALLER_ERROR with something like POLICY_BUNDLE_EXPORTER_ERROR
        return new AuditDetail(AssertionMessages.POLICY_BUNDLE_INSTALLER_ERROR, new String[]{message, ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
    }
}
