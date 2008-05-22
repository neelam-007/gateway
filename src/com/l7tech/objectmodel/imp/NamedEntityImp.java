package com.l7tech.objectmodel.imp;

import com.l7tech.objectmodel.NamedEntity;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class NamedEntityImp extends PersistentEntityImp implements NamedEntity {

    public NamedEntityImp() {
        super();
    }
    
    protected NamedEntityImp(final NamedEntityImp entity) {
        super(entity);
        setName(entity.getName());
    }

    @Column(name="name", nullable=false)
    public String getName() {
        return _name;
    }

    public void setName( String name ) {
        if ( isLocked() ) throw new IllegalStateException("Cannot update locked entity");
        _name = name;
    }

    @SuppressWarnings( { "RedundantIfStatement" } )
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        final NamedEntityImp that = (NamedEntityImp)o;

        if (_name != null ? !_name.equals(that._name) : that._name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (_name != null ? _name.hashCode() : 0);
        return result;
    }

    protected String _name;
}
