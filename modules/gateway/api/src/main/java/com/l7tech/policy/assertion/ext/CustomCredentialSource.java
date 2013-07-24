package com.l7tech.policy.assertion.ext;

/**
 * Implement <code>CustomCredentialSource</code> interface along with {@link #isCredentialSource()} method,
 * by returning <code>true</code>, to indicate that the CustomAssertion is credential source,
 * instead of placing the assertion into {@link Category#ACCESS_CONTROL ACCESS_CONTROL} category.
 * <p>
 * Note that for backwards compatibility, placing the assertion into <tt>AccessControl</tt> category
 * will still indicate that the assertion is credential source, regardless whether the assertion is
 * either, <i>not</i> implementing the <code>CustomCredentialSource</code> interface, or returning
 * <i><code>false</code></i> for {@link #isCredentialSource()} method.
 */
public interface CustomCredentialSource {

    /**
     * Implement this method to indicate, to the Gateway, whether the assertion is a credential source.
     *
     * @return true if credential source, false otherwise
     */
    boolean isCredentialSource();
}
