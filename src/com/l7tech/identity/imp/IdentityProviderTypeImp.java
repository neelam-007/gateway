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

    private String _description;
    private String _className;
}
