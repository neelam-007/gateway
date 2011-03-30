package com.l7tech.gateway.common.security.password;

import com.l7tech.objectmodel.FindException;

/**
 * Interface provided to beans that will need to be able to expand (strict) "${secpass.foo.plaintext}" references.
 */
public interface SecurePasswordReferenceExpander {
    /**
     * Utility method to recognize a password that may actually be a single ${secpass.*.plaintext} reference.
     *
     * @param passwordOrSecpassRef a string to examine.  If null or empty or does not strictly match the format of a single ${secpass.*.plaintext} reference then this method will return the original argument unchanged.
     * @return the corresponding secure password plaintext, if the input was a secpass reference; otherwise, the input unchanged.  May be null only if the input is null.
     * @throws FindException if there is an error looking up a secure password instance.
     */
    char[] expandPasswordReference(String passwordOrSecpassRef) throws FindException;
}
