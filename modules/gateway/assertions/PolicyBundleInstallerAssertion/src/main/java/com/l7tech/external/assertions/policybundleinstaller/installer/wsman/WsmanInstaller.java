package com.l7tech.external.assertions.policybundleinstaller.installer.wsman;

import com.l7tech.external.assertions.policybundleinstaller.installer.BaseInstaller;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerContext;
import com.l7tech.server.policy.bundle.ssgman.BaseGatewayManagementInvoker;
import com.l7tech.server.policy.bundle.ssgman.GatewayManagementInvoker;
import com.l7tech.server.policy.bundle.ssgman.wsman.WsmanInvoker;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import com.l7tech.xml.xpath.XpathUtil;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;

import java.util.UUID;

import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.getNamespaceMap;

/**
 * Common code useful for all wsman policy bundle installers.
 */
public abstract class WsmanInstaller extends BaseInstaller {
    protected final WsmanInvoker wsmanInvoker;

    public WsmanInstaller(@NotNull final PolicyBundleInstallerContext context,
                          @NotNull final Functions.Nullary<Boolean> cancelledCallback,
                          @NotNull final GatewayManagementInvoker gatewayManagementInvoker) {
        super(context, cancelledCallback);
        this.wsmanInvoker = new WsmanInvoker(cancelledCallback, gatewayManagementInvoker);
    }

    protected static String getUuid() {
        return "uuid:" + UUID.randomUUID();
    }

    /**
     * Check if a soap response has fault with InvalidSelector sub code.
     *
     * @param response this may be the soap response from a get selector request.
     * @return true if found
     * @throws com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.UnexpectedManagementResponse parsing or xpath
     */
    protected static boolean hasFaultSubCodeInvalidSelectors(final Document response) throws GatewayManagementDocumentUtilities.UnexpectedManagementResponse {
        return XpathUtil.findElements(response.getDocumentElement(), "//env:Fault/env:Code/env:Subcode/env:Value[text()=\"wsman:InvalidSelectors\"]", getNamespaceMap()).size() > 0;
    }

    protected Pair<AssertionStatus, Document> callManagementCheckInterrupted(String requestXml) throws InterruptedException,
            GatewayManagementDocumentUtilities.AccessDeniedManagementResponse, GatewayManagementDocumentUtilities.UnexpectedManagementResponse {
        return wsmanInvoker.callManagementCheckInterrupted(requestXml);
    }

    @NotNull
    @Override
    public BaseGatewayManagementInvoker getManagementClient() {
        return wsmanInvoker;
    }
}
