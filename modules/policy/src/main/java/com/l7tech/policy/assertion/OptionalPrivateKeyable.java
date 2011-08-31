package com.l7tech.policy.assertion;

/**
 * An expanded PrivateKeyable interface that provides the ability to specify that no key at all is a valid
 * configuration.
 */
public interface OptionalPrivateKeyable extends PrivateKeyable {
    /**
     * Check whether this OptionalPrivateKeyable may be configured to use no key at all.
     *
     * @return true if setUsesNoKey(true) will result in a valid configuration.
     */
    boolean isUsesNoKeyAllowed();

    /**
     * Check whether no private key at all is currently configured.
     * <p/>
     * If this is true, best practice is for {@link #isUsesDefaultKeyStore()} to return true as well.
     *         If this returns true, then the return values from {@link #isUsesDefaultKeyStore()},
     *         {@link #getNonDefaultKeystoreId()}, and {@link #getKeyAlias()} should be ignored.
     * @return true if this private keyable is currently configured to use no key at all.
     */
    boolean isUsesNoKey();

    /**
     * Configure this private keyable to use no key at all.
     * <p/>
     * This is only guaranteed to result in a valid configuration if {@link #isUsesNoKeyAllowed()} is true.
     *
     * @param usesNoKey true if this private keyable should be configured to use no key at all.
     */
    void setUsesNoKey(boolean usesNoKey);
}
