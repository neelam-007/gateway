package com.l7tech.ssg.objectmodel.imp;

public abstract class NamedEntityImp extends StandardEntityImp {
    public String getName() { return _name; }
    public void setName( String name ) { _name = name; }

    protected String _name;
}
