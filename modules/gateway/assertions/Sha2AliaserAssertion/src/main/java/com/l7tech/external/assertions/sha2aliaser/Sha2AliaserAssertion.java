package com.l7tech.external.assertions.sha2aliaser;

import com.l7tech.external.assertions.sha2aliaser.server.Sha2AliaserModuleLoadListener;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;

import java.security.Provider;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Pseudo-assertion whose module load listener runs code to install aliases for SHA-2 in the SUN provider.
 */
public class Sha2AliaserAssertion extends Assertion {
    private static final String META_INITIALIZED = Sha2AliaserAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME, Sha2AliaserModuleLoadListener.class.getName() );

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
