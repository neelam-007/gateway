package com.l7tech.identity;

import java.io.Serializable;

public class InternalUserBean extends UserBean implements Serializable {

    private Boolean enabled;
    private Long expiration;

    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Long getExpiration() {
        return expiration;
    }

    public void setExpiration(Long expiration) {
        this.expiration = expiration;
    }
}
