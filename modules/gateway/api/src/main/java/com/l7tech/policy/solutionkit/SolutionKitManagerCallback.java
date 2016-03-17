package com.l7tech.policy.solutionkit;

import java.util.*;

/**
 * WARNING: this class is under development and is currently for CA internal use only.
 * This interface contract may change without notice.
 *
 * Provides an opportunity for the caller to configure the contents of the migration bundle during a supported
 * event of the migration bundle lifecycle on the Gateway.  The events supported may grow over time.
 */
public abstract class SolutionKitManagerCallback {

    /**
     * Provides a callback opportunity to configure the contents of the migration bundle document before it's sent to Restman.
     *
     * @param context The solution kit manager context.  Including: Solution Kit metadata, restman migration bundle, and Solution Kit custom data.
     * @throws CallbackException if the implementation encounters an error and wants the installation to halt
     */
    public void preMigrationBundleImport(final SolutionKitManagerContext context) throws CallbackException {
        // alternative method names preBundle(...), preBundleDo(...), preBundleExecute(...), doBeforeExecute(...), preExecute(...), preInstall(...), doBeforeInstall(...), doWork(...)

        // no custom callback, override to implement in sub class if required
    }

    public static class CallbackException extends Exception {
        public CallbackException(String message) {
            super(message);
        }
        public CallbackException(Throwable t) {
            super(t);
        }
    }

    /**
     * Keeps a map of all {@code SolutionKit}'s context objects (with key being the {@code SolutionKit} id/guid),
     * including the parent kit.
     */
    private final Map<String, SolutionKitManagerContext> contextMap = new LinkedHashMap<String, SolutionKitManagerContext>();

    /**
     * Get individual {@link SolutionKitManagerContext} for all {@code SolutionKit}'s in the skar,
     * mapped by the {@code SolutionKit} {@code GUID}.
     * <p/>
     * Use this method to retrieve info about other kits in the skar, like see their meta info or even access their individual
     * key-value pairs.
     *
     * @return a read-only {@code Map} of individual {@link SolutionKitManagerContext}.
     */
    public final Map<String, SolutionKitManagerContext> getContextMap() {
        return contextMap;
    }

    /**
     * Keeps a {@code Set} of {@code GUID}'s containing all {@code SolutionKit}'s selected by the user to be installed or upgraded.
     */
    private final Set<String> selectedSolutionKits = new HashSet<String>();

    /**
     * Get a {@code Set} of {@code GUID}'s containing all {@code SolutionKit}'s selected by the user to be installed or upgraded.
     * <p/>
     * Note that this method is available only from the custom callback, as the user can modify the selection after
     * the custom UI has been executed (thus making this list inaccurate), and there is no clean way to hook the selection
     * change even with the custom UI.
     *
     * @return {@code Set} of {@code SolutionKit} {@code GUID}'s the user selected for installation or upgrade.
     */
    public final Set<String> getSelectedSolutionKits() {
        return selectedSolutionKits;
    }
}
