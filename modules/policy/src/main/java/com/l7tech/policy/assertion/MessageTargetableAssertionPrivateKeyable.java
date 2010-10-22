package com.l7tech.policy.assertion;

/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 *
 * Simple class which adds PrivateKeyable support
 * @author darmstrong
 */
public abstract class MessageTargetableAssertionPrivateKeyable
        extends MessageTargetableAssertion implements PrivateKeyable{

    @Override
    public boolean isUsesDefaultKeyStore() {
        return privateKeyableSupport.isUsesDefaultKeyStore();
    }

    @Override
    public void setUsesDefaultKeyStore(boolean usesDefault) {
        privateKeyableSupport.setUsesDefaultKeyStore(usesDefault);
    }

    @Override
    public long getNonDefaultKeystoreId() {
        return privateKeyableSupport.getNonDefaultKeystoreId();
    }

    @Override
    public void setNonDefaultKeystoreId(long nonDefaultId) {
        privateKeyableSupport.setNonDefaultKeystoreId(nonDefaultId);
    }

    @Override
    public String getKeyAlias() {
        return privateKeyableSupport.getKeyAlias();
    }

    @Override
    public void setKeyAlias(String keyid) {
        privateKeyableSupport.setKeyAlias(keyid);
    }

    // - PRIVATE
    private final PrivateKeyableSupport privateKeyableSupport = new PrivateKeyableSupport();
}
