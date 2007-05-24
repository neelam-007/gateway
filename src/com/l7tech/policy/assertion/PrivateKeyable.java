package com.l7tech.policy.assertion;

/**
 * Implemented by assertions having the possibility of using a non-default key pair.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: May 22, 2007<br/>
 */
public interface PrivateKeyable {
    boolean isUsesDefaultKeyStore();
    void setUsesDefaultKeyStore(boolean usesDefault);
    long getNonDefaultKeystoreId();
    void setNonDefaultKeystoreId(long nonDefaultId);
    String getKeyAlias();
    void setKeyAlias(String keyid);
}
