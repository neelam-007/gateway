package com.l7tech.objectmodel.imp;

import com.l7tech.objectmodel.imp.EntityImp;

public abstract class NamedEntityImp extends EntityImp {
    public String getName() { return _name; }
    public void setName( String name ) { _name = name; }

    protected String _name;
}
