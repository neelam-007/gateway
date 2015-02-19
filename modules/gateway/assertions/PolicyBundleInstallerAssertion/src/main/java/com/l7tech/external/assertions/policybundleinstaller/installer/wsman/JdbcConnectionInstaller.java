package com.l7tech.external.assertions.policybundleinstaller.installer.wsman;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.policy.bundle.BundleMapping;
import com.l7tech.server.event.bundle.DryRunInstallPolicyBundleEvent;
import com.l7tech.server.policy.bundle.BundleResolver;
import com.l7tech.server.policy.bundle.BundleUtils;
import com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerContext;
import com.l7tech.server.policy.bundle.ssgman.GatewayManagementInvoker;
import com.l7tech.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.l7tech.policy.bundle.BundleMapping.Type.JDBC_CONNECTION_NAME;
import static com.l7tech.server.policy.bundle.BundleResolver.InvalidBundleException;
import static com.l7tech.server.policy.bundle.PolicyUtils.findJdbcReferences;
import static com.l7tech.server.policy.bundle.ssgman.wsman.WsmanInvoker.GATEWAY_MGMT_ENUMERATE_FILTER;

/**
 * JDBC connection logic.
 */
public class JdbcConnectionInstaller extends WsmanInstaller {
    public static final String JDBC_MGMT_NS = "http://ns.l7tech.com/2010/04/gateway-management/jdbcConnections";

    public JdbcConnectionInstaller(@NotNull final PolicyBundleInstallerContext context,
                                   @NotNull final Functions.Nullary<Boolean> cancelledCallback,
                                   @NotNull final GatewayManagementInvoker gatewayManagementInvoker) {
        super(context, cancelledCallback, gatewayManagementInvoker);
    }

    public static void setJdbcReferencesInPolicy(@NotNull final Document policyDocumentFromResource, @Nullable final Map<String, String> mappedJdbcReferences) throws InvalidBundleException {
        if (mappedJdbcReferences != null && !mappedJdbcReferences.isEmpty()) {
            final List<Element> jdbcReferencesElms = findJdbcReferences(policyDocumentFromResource.getDocumentElement());
            for (Element jdbcRefElm : jdbcReferencesElms) {
                try {
                    final Element connNameElm = XmlUtil.findExactlyOneChildElementByName(jdbcRefElm, BundleUtils.L7_NS_POLICY, "ConnectionName");
                    final String policyConnName = connNameElm.getAttribute("stringValue").trim();
                    if (mappedJdbcReferences.containsKey(policyConnName)) {
                        connNameElm.setAttribute("stringValue", mappedJdbcReferences.get(policyConnName));
                    }

                } catch (TooManyChildElementsException | MissingRequiredElementException e) {
                    throw new InvalidBundleException("Could not find jdbc reference to update: " + ExceptionUtils.getMessage(e));
                }
            }
        }
    }

    public void dryRunInstall(@NotNull final DryRunInstallPolicyBundleEvent dryRunEvent)
            throws InterruptedException, BundleResolver.UnknownBundleException, BundleResolver.BundleResolverException, InvalidBundleException, GatewayManagementDocumentUtilities.AccessDeniedManagementResponse {
        checkInterrupted();
        final BundleInfo bundleInfo = context.getBundleInfo();
        final Set<String> jdbcConnRefs = bundleInfo.getJdbcConnectionReferences();
        logger.finest("Dry run checking " + jdbcConnRefs.size() + " JDBC reference(s).");
        final BundleMapping bundleMapping = context.getBundleMapping();
        if (!jdbcConnRefs.isEmpty()) {
            final Map<String, String> jdbcMappings = bundleMapping.getMappings(JDBC_CONNECTION_NAME);

            // validate each, consider any mapping that may be present.
            for (String jdbcConnRef : jdbcConnRefs) {
                checkInterrupted();
                final String jdbcConnToVerify = (jdbcMappings.containsKey(jdbcConnRef)) ? jdbcMappings.get(jdbcConnRef) : jdbcConnRef;
                try {
                    final List<Goid> foundConns = findMatchingJdbcConnection(jdbcConnToVerify);
                    if (foundConns.isEmpty()) {
                        dryRunEvent.addMissingJdbcConnection(jdbcConnToVerify);
                    }
                } catch (GatewayManagementDocumentUtilities.UnexpectedManagementResponse e) {
                    throw new InvalidBundleException("Could not verify JDBC Connection '" + jdbcConnToVerify + "'", e);
                }
            }
        }
    }

    @NotNull
    private List<Goid> findMatchingJdbcConnection(String jdbcConnection) throws GatewayManagementDocumentUtilities.AccessDeniedManagementResponse, InterruptedException, GatewayManagementDocumentUtilities.UnexpectedManagementResponse {
        logger.finest("Finding JDBC connection '" + jdbcConnection + "'.");
        final String serviceFilter = MessageFormat.format(GATEWAY_MGMT_ENUMERATE_FILTER, getUuid(),
                JDBC_MGMT_NS, 10, "/l7:JDBCConnection/l7:Name[text()='" + jdbcConnection + "']");

        final Pair<AssertionStatus, Document> documentPair = callManagementCheckInterrupted(serviceFilter);
        return GatewayManagementDocumentUtilities.getSelectorId(documentPair.right, true);
    }
}
