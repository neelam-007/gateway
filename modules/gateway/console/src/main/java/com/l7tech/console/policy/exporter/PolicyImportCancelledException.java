package com.l7tech.console.policy.exporter;

/**
 * This exception is supposed to be thrown when a policy import is cancelled. The caller
 * can catch this to deal with the specific case where the import is cancelled, rather
 * than the more general case where something went wrong.
 */
public class PolicyImportCancelledException extends Exception {
}
