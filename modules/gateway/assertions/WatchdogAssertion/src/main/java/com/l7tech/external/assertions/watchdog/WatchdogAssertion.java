package com.l7tech.external.assertions.watchdog;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;

/**
 * An assertion that audits a stack trace if the current request takes longer than the specified number of milliseconds
 * after the assertion executes.
 */
public class WatchdogAssertion extends Assertion {
    public long milliseconds = 5 * 60L * 1000L;

    public long getMilliseconds() {
        return milliseconds;
    }

    public void setMilliseconds(long milliseconds) {
        this.milliseconds = milliseconds;
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = WatchdogAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.DESCRIPTION, "Request Watchdog Timer.  Logs information about requests that run for too long.");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "misc" });
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
