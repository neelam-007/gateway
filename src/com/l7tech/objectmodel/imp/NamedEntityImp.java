package com.l7tech.objectmodel.imp;

import com.l7tech.objectmodel.NamedEntity;

public abstract class NamedEntityImp extends EntityImp implements NamedEntity {
    public String getName() { return _name; }
    public void setName( String name ) { _name = name; }
    protected String _name;


    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        final NamedEntityImp that = (NamedEntityImp)o;

        if (_name != null ? !_name.equals(that._name) : that._name != null) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (_name != null ? _name.hashCode() : 0);
        return result;
    }
}
