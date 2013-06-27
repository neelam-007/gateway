package com.l7tech.objectmodel.imp;

import com.l7tech.objectmodel.PersistentEntity;
import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;

/**
 * @author alex
 */
@Deprecated //deprecated by com.l7tech.objectmodel.imp.GoidEntityImp
@MappedSuperclass
public abstract class PersistentEntityImp implements PersistentEntity, Serializable {
    protected PersistentEntityImp() {
        _oid = DEFAULT_OID;
        _loadTime = System.currentTimeMillis();
        _locked = false;
    }

    protected PersistentEntityImp(final PersistentEntity entity) {
        this();
        setOid(entity.getOid());
        setVersion(entity.getVersion());
    }

    @Override
    @Id @XmlTransient
    @Column(name="objectid", nullable=false, updatable=false)
    @GenericGenerator( name="generator", strategy = "layer7-generator" )
    @GeneratedValue( generator = "generator")
    public long getOid() {
        return _oid;
    }

    @Transient
    public Long getOidAsLong() {
        return _oidObject;
    }

    @Override
    @Transient
    @XmlID @XmlAttribute
    public String getId() {
        return Long.toString(_oid);
    }

    @Deprecated // only for XML, likely to throw NFE
    public void setId(String id) {
        checkLocked();
        if (id == null || id.length() == 0) {
            setOid(DEFAULT_OID);
        } else {
            setOid(Long.parseLong(id));
        }
    }

    @Override
    public void setOid( long oid ) {
        checkLocked();
        _oid = oid;
        _oidObject = oid;
    }

    /**
     * Any subclasses which has a version column must override this method and map it manually as it is transient.
     */
    @Override
    @Transient
    @XmlAttribute
    public int getVersion() {
        return _version;
    }

    @Override
    public void setVersion(int version) {
        checkLocked();
        _version = version;
    }

    @SuppressWarnings( { "RedundantIfStatement" } )
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final PersistentEntityImp entityImp = (PersistentEntityImp)o;

        if (_oid != entityImp._oid) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return (int)(_oid ^ (_oid >>> 32));
    }

    protected void lock() {
        _locked = true;
    }

    @Transient
    protected boolean isLocked() {
        return _locked;
    }

    /**
     * Throws IllegalStateException if {@link #isLocked}.
     */
    protected void checkLocked() {
        if ( isLocked() ) throw new IllegalStateException("Cannot update locked entity");
    }

    protected int _version;
	protected long _oid;
    protected Long _oidObject;
    protected final transient long _loadTime;
    protected transient boolean _locked; // read-only when locked
}
