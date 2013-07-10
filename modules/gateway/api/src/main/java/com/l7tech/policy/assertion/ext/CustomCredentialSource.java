package com.l7tech.policy.assertion.ext;

/**
 * Implement the <code>CustomCredentialSource</code> interface to indicate that the CustomAssertion is credential source,
 * instead of placing the assertion into {@link Category#ACCESS_CONTROL ACCESS_CONTROL} category.
 * <p>
 * Note that this interface does <i>not</i> contain any methods.
 * Implementing the interface will indicate to the SSG that the CustomAssertion is credential source,
 * not implementing the interface will indicate to the SSG that the CustomAssertion is <i>not</i> credential source.
 * <p>
 * Additional note: for backwards compatibility, placing the assertion into <tt>AccessControl</tt> category
 * will still indicate that the CustomAssertion is credential source, even if the CustomAssertion is <i>not</i>
 * implementing the <code>CustomCredentialSource</code> interface.
 */
public interface CustomCredentialSource {
}
