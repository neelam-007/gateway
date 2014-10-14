package com.l7tech.external.assertions.policybundleinstaller.installer;

import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerContext;
import com.l7tech.server.policy.bundle.ssgman.BaseGatewayManagementInvoker;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.logging.Logger;

/**
 * Common code useful for all policy bundle install.
 */
public abstract class BaseInstaller {
    protected static final Logger logger = Logger.getLogger(BaseInstaller.class.getName());
    protected static final String POLICY_REVISION_COMMENT_FORMAT = "{0} (v{1})";

    @NotNull
    protected final PolicyBundleInstallerContext context;
    @NotNull
    protected final Functions.Nullary<Boolean> cancelledCallback;

    public BaseInstaller(@NotNull final PolicyBundleInstallerContext context,
                         @NotNull final Functions.Nullary<Boolean> cancelledCallback) {
        this.context = context;
        this.cancelledCallback = cancelledCallback;
    }

    protected static boolean isPrefixValid(String installationPrefix) {
        return installationPrefix != null && !installationPrefix.trim().isEmpty();
    }

    protected String getPrefixedPolicyName(@NotNull String policyName) {
        if(isPrefixValid(context.getInstallationPrefix())) {
            return context.getInstallationPrefix() + " " + policyName;
        } else {
            return policyName;
        }
    }

    protected String getPolicyRevisionComment(final BundleInfo bundleInfo) {
        return MessageFormat.format(POLICY_REVISION_COMMENT_FORMAT, bundleInfo.getName(), bundleInfo.getVersion());
    }

    @NotNull
    abstract protected BaseGatewayManagementInvoker getManagementClient();

    protected void checkInterrupted() throws InterruptedException {
        getManagementClient().checkInterrupted();
    }
}