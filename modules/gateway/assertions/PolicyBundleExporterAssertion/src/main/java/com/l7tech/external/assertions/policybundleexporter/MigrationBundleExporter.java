package com.l7tech.external.assertions.policybundleexporter;

import com.l7tech.external.assertions.policybundleexporter.server.PolicyBundleExporterContext;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerAssertionRegistry;
import com.l7tech.server.policy.bundle.ssgman.GatewayManagementInvoker;
import com.l7tech.server.policy.bundle.ssgman.restman.RestmanInvoker;
import com.l7tech.server.policy.bundle.ssgman.restman.RestmanMessage;
import com.l7tech.server.policy.module.ModularAssertionModule;
import com.l7tech.util.Functions;
import com.l7tech.util.MissingRequiredElementException;
import com.l7tech.util.Pair;
import com.l7tech.util.TooManyChildElementsException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.AccessDeniedManagementResponse;
import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.UnexpectedManagementResponse;
import static com.l7tech.server.policy.bundle.ssgman.restman.RestmanInvoker.URL_1_0_BUNDLE;
import static com.l7tech.server.policy.bundle.ssgman.restman.RestmanInvoker.VAR_RESTMAN_ACTION;
import static com.l7tech.server.policy.bundle.ssgman.restman.RestmanInvoker.VAR_RESTMAN_URI;

/**
 * Export folder(s) as a migration bundle using the Gateway Management REST API.
 */
public class MigrationBundleExporter {
    private static final Logger logger = Logger.getLogger(MigrationBundleExporter.class.getName());

    @NotNull
    private final RestmanInvoker restmanInvoker;

    @NotNull
    private final PolicyBundleExporterContext exportContext;

    @NotNull
    private final ServerAssertionRegistry assertionRegistry;

    public MigrationBundleExporter(@NotNull final GatewayManagementInvoker gatewayManagementInvoker,
                                   @NotNull final PolicyBundleExporterContext exportContext,
                                   @NotNull final ServerAssertionRegistry assertionRegistry,
                                   @NotNull final Functions.Nullary<Boolean> cancelledCallback) {
        this.restmanInvoker = new RestmanInvoker(cancelledCallback, gatewayManagementInvoker);
        this.exportContext = exportContext;
        this.assertionRegistry = assertionRegistry;
    }

    public void export(final PolicyBundleExporterEvent exportEvent) throws IOException, InterruptedException, UnexpectedManagementResponse, AccessDeniedManagementResponse {
        logger.finest("Exporting bundle folder with goid " + exportContext.getBundleFolderGoid());

        final String requestXml = "";
        final PolicyEnforcementContext pec = restmanInvoker.getContext(requestXml);

        for (ComponentInfo componentInfo : exportEvent.getExportContext().getComponentInfoList()) {
            restmanInvoker.checkInterrupted();
            pec.setVariable(VAR_RESTMAN_URI, URL_1_0_BUNDLE + "?folder=" + componentInfo.getFolderHeader().getGoid());
            pec.setVariable(VAR_RESTMAN_ACTION, "GET");
            final Pair<AssertionStatus, RestmanMessage> exportResult = restmanInvoker.callManagementCheckInterrupted(pec, requestXml);
            final RestmanMessage exportMessage = exportResult.right;

            // check for errors
            if (exportMessage == null) {
                throw new RuntimeException("Export failed: response from restman is null.");
            } else if (exportMessage.hasMappingError()) {
                try {
                    throw new RuntimeException("Export failed: " + exportMessage.getMappingErrorsAsString());
                } catch (IOException e) {
                    throw new RuntimeException("Export failed:", e);
                }
            }

            this.resolveRequiredFolderMappings(exportMessage);

            exportEvent.setComponentRestmanBundleXmls(componentInfo.getFolderHeader().getGoid(), exportMessage.getBundleXml());

            // loop through policies to get assertion server module file names
            // TODO add support to read PolicyBundleExporterContext field to skip server module search (which is noticeably slower)
            for (String policyXml : exportMessage.getResourceSetPolicies()) {
                final Assertion assertions = WspReader.getDefault().parseStrictly(policyXml, WspReader.INCLUDE_DISABLED);
                if (assertions != null) {
                    final Iterator<Assertion> it = assertions.preorderIterator();
                    while (it.hasNext()) {
                        final Assertion assertion = it.next();
                        if (!AssertionRegistry.isCoreAssertion(assertion.getClass())) {
                            // TODO don't include modules that we know come pre-installed on Gateway (e.g. JdbcQueryAssertion-*.aar, ComparisonAssertion-*.aar, etc).
                            final ModularAssertionModule module = assertionRegistry.getModuleForClassLoader(assertion.getClass().getClassLoader());
                            exportEvent.getServerModuleFileNames().add(module.getName());

                            // TODO need top level license feature set?  (e.g. "set:Profile:Gateway")
                            // TODO how to get installer feature set? (e.g. "assertion:OAuthInstaller")
                            // currently show assertion level feature set (e.g. "assertion:JdbcQuery") for manual cross referencing to match one or more top level license feature(s)
                            exportEvent.getAssertionFeatureSetNames().add(assertion.getFeatureSetName());
                        }
                    }
                }
            }
        }
    }

    /**
     * Resolve Folder mappings with "FailOnNew" property with value of "true".
     * @param exportMessage export message
     * @throws java.io.IOException
     * @throws InterruptedException
     * @throws com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.UnexpectedManagementResponse
     * @throws com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.AccessDeniedManagementResponse
     */
    private void resolveRequiredFolderMappings(final RestmanMessage exportMessage) throws IOException, InterruptedException, UnexpectedManagementResponse, AccessDeniedManagementResponse {
        try {
            Set<String> folderSrcUriList = exportMessage.getMappingsFailOnNewFolders();
            for (String folderSrcUri : folderSrcUriList) {
                final String requestXml = "";
                final PolicyEnforcementContext pec = restmanInvoker.getContext(requestXml);

                pec.setVariable(VAR_RESTMAN_URI, folderSrcUri);
                pec.setVariable(VAR_RESTMAN_ACTION, "GET");
                final Pair<AssertionStatus, RestmanMessage> exportResult = restmanInvoker.callManagementCheckInterrupted(pec, requestXml);
                final RestmanMessage folderMessage = exportResult.right;

                // check for errors
                if (folderMessage == null) {
                    throw new RuntimeException("Export failed: response from restman is null.");
                }
                // todo (kpak) - check for errors in response.

                exportMessage.replaceFailOnNewFolderMapping(folderSrcUri, folderMessage);
            }
        } catch (TooManyChildElementsException | MissingRequiredElementException e) {
            e.printStackTrace();
        }
    }
}
