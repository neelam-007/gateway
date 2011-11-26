package com.l7tech.external.assertions.ssh;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.util.Functions;

import java.util.logging.Logger;

import static com.l7tech.policy.assertion.AssertionMetadata.ASSERTION_FACTORY;

/**
 * Assertion that requires SSH credentials are present.
 */
public class SshCredentialAssertion extends Assertion {
    protected static final Logger logger = Logger.getLogger(SshCredentialAssertion.class.getName());

    public static final String LISTEN_PROP_ENABLE_SCP = "l7.ssh.enableScp";
    public static final String LISTEN_PROP_ENABLE_SFTP = "l7.ssh.enableSftp";
    public static final String LISTEN_PROP_HOST_PRIVATE_KEY = "l7.ssh.hostPrivateKey";
    public static final String LISTEN_PROP_IDLE_TIMEOUT_MINUTES = "l7.ssh.idleTimeoutMinutes";
    public static final String LISTEN_PROP_MAX_CONCURRENT_SESSIONS_PER_USER = "l7.ssh.maxConcurrentSessionsPerUser";
    public static final String LISTEN_PROP_MAX_SESSIONS = "l7.ssh.maxSessions";

    private boolean permitPasswordCredential;
    private boolean permitPublicKeyCredential;

    /**
     * The SSH Credential assertion is always a credential source
     *
     * @return true
     */
    @Override
    public boolean isCredentialSource() {
        return true;
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = SshCredentialAssertion.class.getName() + ".metadataInitialized";

    /**
     * Create a new SshCredentialAssertion with default properties.
     * @return a new instance with default properties.  Never null.
     */
    public static SshCredentialAssertion newInstance() {
        SshCredentialAssertion sshCredentialAssertion = new SshCredentialAssertion();
        sshCredentialAssertion.setPermitPasswordCredential(true);
        sshCredentialAssertion.setPermitPublicKeyCredential(true);
        return sshCredentialAssertion;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();

        if (!Boolean.TRUE.equals(meta.get(META_INITIALIZED))) {

            // ensure inbound SSH transport gets wired up
            meta.put(AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.ssh.server.SshServerModuleLoadListener");

            // Set description for GUI
            meta.put(AssertionMetadata.SHORT_NAME, "Require SSH Credentials");
            meta.put(AssertionMetadata.DESCRIPTION, "The requester must provide credentials using SSH authentication");

            // Add to palette folder(s)
            //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing,
            //   misc, audit, policyLogic, threatProtection
            meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "accessControl" });
            meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/authentication.gif");

            // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
            meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "none");

            meta.put(ASSERTION_FACTORY, new Functions.Unary<SshCredentialAssertion, SshCredentialAssertion>(){
                @Override
                public SshCredentialAssertion call(final SshCredentialAssertion sshCredentialAssertion) {
                    return newInstance();
                }
            });
            meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/authentication.gif");
            meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.ssh.console.SshCredentialAssertionPropertiesDialog");
            meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
            meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

            meta.put(META_INITIALIZED, Boolean.TRUE);
        }

        return meta;
    }

    public boolean isPermitPasswordCredential() {
        return permitPasswordCredential;
    }

    public void setPermitPasswordCredential(boolean permitPasswordCredential) {
        this.permitPasswordCredential = permitPasswordCredential;
    }

    public boolean isPermitPublicKeyCredential() {
        return permitPublicKeyCredential;
    }

    public void setPermitPublicKeyCredential(boolean permitPublicKeyCredential) {
        this.permitPublicKeyCredential = permitPublicKeyCredential;
    }


}
