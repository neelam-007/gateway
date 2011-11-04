package com.l7tech.external.assertions.ssh;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;

import java.util.logging.Logger;

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

            meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/authentication.gif");
            meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");

            meta.put(META_INITIALIZED, Boolean.TRUE);
        }

        return meta;
    }

}
