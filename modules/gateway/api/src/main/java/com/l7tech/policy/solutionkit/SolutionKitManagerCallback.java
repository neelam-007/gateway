package com.l7tech.policy.solutionkit;

import org.w3c.dom.Document;

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
     * @param solutionKitDocuments Solution Kit meta data document and Layer7 Restman Migration Bundle Documents,
     *                             any changes made to this document will be persisted when the entities are imported via Restman.
     * @throws CallbackException if the implementation encounters an error and wants the installation to halt
     */
    public void preMigrationBundleImport(final Document solutionKitDocuments, final SolutionKitManagerContext context) throws CallbackException {
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
}
