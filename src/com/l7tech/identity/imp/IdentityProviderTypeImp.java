/*
 * Created on 7-May-2003
 */
package com.l7tech.identity.imp;

import com.l7tech.identity.IdentityProviderType;
import com.l7tech.objectmodel.imp.NamedEntityImp;

/**
 * @author alex
 */
public class IdentityProviderTypeImp extends NamedEntityImp implements IdentityProviderType {

    public IdentityProviderTypeImp() {
        super();
    }
    public String getDescription() {
        return _description;
    }

    public String getClassName() {
        return _className;
    }

    public void setDescription(String description) {
        _description = description;
    }

    public void setClassName(String className) {
        _className = className;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IdentityProviderTypeImp)) return false;

        final IdentityProviderTypeImp identityProviderTypeImp = (IdentityProviderTypeImp) o;

        if (_oid != DEFAULT_OID ? !(_oid == identityProviderTypeImp._oid) : identityProviderTypeImp._oid != DEFAULT_OID ) return false;
        if (_className != null ? !_className.equals(identityProviderTypeImp._className) : identityProviderTypeImp._className != null) return false;
        if (_description != null ? !_description.equals(identityProviderTypeImp._description) : identityProviderTypeImp._description != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (_description != null ? _description.hashCode() : 0);
        result = 29 * result + (_className != null ? _className.hashCode() : 0);
        result = 29 * result + (int)_oid;
        return result;
    }

    private String _description;
    private String _className;
}
