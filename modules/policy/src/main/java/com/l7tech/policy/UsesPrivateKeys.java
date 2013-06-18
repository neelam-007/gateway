package com.l7tech.policy;

import com.l7tech.objectmodel.SsgKeyHeader;

/**
 * This interface is to be used by assertions on entities that use PrivateKeys. It is used by the DependencyAnalyzer in
 * order to find private key dependencies on an object.
 * <p/>
 * For assertions if the assertion already implements PrivateKeyable then implementing this interface is not necessary.
 * For assertions it is recommended that PrivateKeyable is implemented in order to also get the "Select Private Key"
 * context menu.
 *
 * @author Victor Kazakov
 */
public interface UsesPrivateKeys {

    /**
     * This returns the private keys used by this object.
     *
     * @return The list of SsgKeyHeaders used. If this is null or the empty list then no private keys are used.
     */
    public SsgKeyHeader[] getPrivateKeysUsed();
}