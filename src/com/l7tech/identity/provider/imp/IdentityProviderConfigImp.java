/*
 * Created on 7-May-2003
 */
package com.l7tech.identity.provider.imp;

import com.l7tech.identity.provider.IdentityProviderConfig;
import com.l7tech.identity.provider.IdentityProviderType;
import com.l7tech.objectmodel.imp.NamedEntityImp;

/**
 * @author alex
 */
public class IdentityProviderConfigImp extends NamedEntityImp implements IdentityProviderConfig {
    public String getDescription() {
        return _description;
    }

    public IdentityProviderType getType() {
        return _type;
    }

    public void setDescription(String description) {
        _description = description;
    }

    public void setType(IdentityProviderType type) {
        _type = type;
    }

    private String _description;
    private IdentityProviderType _type;
}
