package com.l7tech.external.assertions.jwt;

import com.l7tech.objectmodel.Goid;

public class JwkKeyInfo {

    private Goid sourceKeyGoid;
    private String sourceKeyAlias;

    private String keyId;

    private String publicKeyUse;

    public Goid getSourceKeyGoid() {
        return sourceKeyGoid;
    }

    public void setSourceKeyGoid(Goid sourceKeyGoid) {
        this.sourceKeyGoid = sourceKeyGoid;
    }

    public String getSourceKeyAlias() {
        return sourceKeyAlias;
    }

    public void setSourceKeyAlias(String sourceKeyAlias) {
        this.sourceKeyAlias = sourceKeyAlias;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getPublicKeyUse() {
        return publicKeyUse;
    }

    public void setPublicKeyUse(String publicKeyUse) {
        this.publicKeyUse = publicKeyUse;
    }
}
