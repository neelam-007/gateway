/*
 * Created on 7-May-2003
 */
package com.l7tech.identity.imp;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.objectmodel.imp.NamedEntityImp;

/**
 * @author alex
 */
public class IdentityProviderConfigImp extends NamedEntityImp implements IdentityProviderConfig {
    public String getDescription() {
        return _description;
    }

    public void setDescription(String description) {
        _description = description;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IdentityProviderConfigImp)) return false;

        final IdentityProviderConfigImp identityProviderConfigImp = (IdentityProviderConfigImp) o;

        if (_description != null ? !_description.equals(identityProviderConfigImp._description) : identityProviderConfigImp._description != null) return false;
        //if (_type != null ? !_type.equals(identityProviderConfigImp._type) : identityProviderConfigImp._type != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (_description != null ? _description.hashCode() : 0);
        //result = 29 * result + (_type != null ? _type.hashCode() : 0);
        return result;
    }

    private String _description;
}
