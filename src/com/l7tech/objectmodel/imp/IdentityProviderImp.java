/*
 * Created on 7-May-2003
 */
package com.l7tech.objectmodel.imp;

import com.l7tech.objectmodel.IdentityProvider;
import com.l7tech.objectmodel.IdentityProviderType;

/**
 * @author alex
 */
public class IdentityProviderImp extends NamedEntityImp implements IdentityProvider {
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
