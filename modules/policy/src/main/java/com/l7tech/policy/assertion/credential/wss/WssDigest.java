package com.l7tech.policy.assertion.credential.wss;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.wsp.WspSensitive;
import com.l7tech.search.Dependency;

/**
 * WSS Digest authentication.
 */
public class WssDigest extends WssCredentialSourceAssertion {
    private boolean requireNonce = false;
    private boolean requireTimestamp = false;
    private String requiredUsername = null;
    private String requiredPassword = null;

    public boolean isRequireNonce() {
        return requireNonce;
    }

    public void setRequireNonce(boolean requireNonce) {
        this.requireNonce = requireNonce;
    }

    public boolean isRequireTimestamp() {
        return requireTimestamp;
    }

    public void setRequireTimestamp(boolean requireTimestamp) {
        this.requireTimestamp = requireTimestamp;
    }

    @WspSensitive
    @Dependency(methodReturnType = Dependency.MethodReturnType.VARIABLE, type = Dependency.DependencyType.SECURE_PASSWORD)
    public String getRequiredPassword() {
        return requiredPassword;
    }

    public void setRequiredPassword(String requiredPassword) {
        this.requiredPassword = requiredPassword;
    }

    public String getRequiredUsername() {
        return requiredUsername;
    }

    public void setRequiredUsername(String requiredUsername) {
        this.requiredUsername = requiredUsername;
    }

    @Override
    protected VariablesUsed doGetVariablesUsed() {
        return super.doGetVariablesUsed().withExpressions(requiredUsername, requiredPassword);
    }

    final static String baseName = "Require WS-Security Password Digest Credentials";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<WssCredentialSourceAssertion>(){
        @Override
        public String getAssertionName( final WssCredentialSourceAssertion assertion, final boolean decorate) {
            if(!decorate) return baseName;
            return AssertionUtils.decorateName(assertion, baseName);
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION, "The requestor must provide DIGEST credentials in a WSS Username Token");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "accessControl" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/authentication.gif");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.WssDigestAssertionPropertiesDialog");

        meta.put(AssertionMetadata.USED_BY_CLIENT, Boolean.FALSE); // assertion XML should not be sent to clients since it might contain the username and password

        return meta;
    }
}
