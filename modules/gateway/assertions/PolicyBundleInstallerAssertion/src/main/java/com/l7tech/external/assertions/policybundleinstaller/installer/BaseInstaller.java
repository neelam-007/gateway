package com.l7tech.external.assertions.policybundleinstaller.installer;

import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerContext;
import com.l7tech.server.policy.bundle.ssgman.BaseGatewayManagementInvoker;
import com.l7tech.server.policy.bundle.ssgman.restman.VersionModifier;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    public static String getSuffixedFolderName(@Nullable String versionModifier, @NotNull String folderName) {
        return VersionModifier.getSuffixedFolderName(versionModifier, folderName);
    }

    public static String getPrefixedEncapsulatedAssertionName(@Nullable String versionModifier, @NotNull String encapsulatedAssertionName) {
        return VersionModifier.getPrefixedEncapsulatedAssertionName(versionModifier, encapsulatedAssertionName);
    }

    protected String getPrefixedPolicyName(@NotNull String policyName) {
        return VersionModifier.getPrefixedPolicyName(context.getInstallationPrefix(), policyName);
    }

    protected String getPrefixedUrl(final String existingUrlPattern) {
        return VersionModifier.getPrefixedUrl(context.getInstallationPrefix(), existingUrlPattern);
    }

    protected static boolean isValidVersionModifier(@Nullable String versionModifier) {
        return VersionModifier.isValidVersionModifier(versionModifier);
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