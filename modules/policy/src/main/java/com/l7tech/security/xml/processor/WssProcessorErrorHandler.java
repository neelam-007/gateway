package com.l7tech.security.xml.processor;

/**
 * An error handler for notifying about non-fatal errors that occur while performing WSS processing.
 */
public abstract class WssProcessorErrorHandler {
    /**
     * Report that an error was encountered while attempting to decrypt some encrypted XML whose symmetric decryption
     * key was already located and available.  As this may be a sign of an adaptive known ciphertext attack,
     * users of WssProcessor may wish to be informed of this so they can audit the failed decryption (even
     * if the actual decryption operation pretends to succeed by returning a bogus result, per one of the
     * attack countermeasures).
     * <p/>
     * The default implementation of this method does nothing.
     *
     * @param t an exception that was encountered while attempting to decrypt some XML using a secret key
     *          known to the WSS processor.
     *          <p/>
     *          Note: it is crucial that implementors do not vary their behavior based on the content of this
     *          throwable in anyway way detectible by an attacker.  Note that this includes timing differences,
     *          possibly including those due to branching.
     */
    public void onDecryptionError(Throwable t) {
    }
}
