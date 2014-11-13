package com.l7tech.external.assertions.policybundleinstaller.installer;

import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerContext;
import com.l7tech.server.policy.bundle.ssgman.BaseGatewayManagementInvoker;
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
        return isValidVersionModifier(versionModifier) ? folderName + " " + versionModifier : folderName;
    }

    public static String getPrefixedPolicyName(@Nullable String versionModifier, @NotNull String policyName) {
        return isValidVersionModifier(versionModifier) ? versionModifier + " " + policyName : policyName;
    }

    public static String getPrefixedUrl(@Nullable String versionModifier, @NotNull String urlPattern) {
        return isValidVersionModifier(versionModifier) ? "/" + versionModifier + urlPattern : urlPattern;
    }

    public static String getPrefixedEncapsulatedAssertionName(@Nullable String versionModifier, @NotNull String encapsulatedAssertionName) {
        return isValidVersionModifier(versionModifier) ? versionModifier + " " + encapsulatedAssertionName : encapsulatedAssertionName;
    }

    protected String getPrefixedPolicyName(@NotNull String policyName) {
        return getPrefixedPolicyName(context.getInstallationPrefix(), policyName);
    }

    protected String getPrefixedUrl(final String existingUrlPattern) {
        return getPrefixedUrl(context.getInstallationPrefix(), existingUrlPattern);
    }

    protected static boolean isValidVersionModifier(@Nullable String versionModifier) {
        return versionModifier != null && !versionModifier.trim().isEmpty();
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