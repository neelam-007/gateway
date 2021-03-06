package com.l7tech.objectmodel.imp;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.util.GoidUpgradeMapper;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;

/**
 * This is similar persistent entity but used a Goid instead of an oid.
 *
 * @author Victor Kazakov
 */
@MappedSuperclass
public abstract class PersistentEntityImp implements PersistentEntity, Serializable {
    private Goid goid;
    private int version;
    protected final transient long loadTime;
    protected transient boolean locked; // read-only when locked
    protected transient boolean preserveId;

    private static final long serialVersionUID = 5416621649996405984L;

    protected PersistentEntityImp() {
        goid = DEFAULT_GOID;
        loadTime = System.currentTimeMillis();
        locked = false;
        preserveId = false;
    }

    protected PersistentEntityImp(final PersistentEntity entity) {
        this();
        setGoid(entity.getGoid());
        setVersion(entity.getVersion());
    }

    @Override
    @Id
    @XmlTransient
    @Column(name="goid", nullable=false, updatable=false)
    @GenericGenerator( name="generator", strategy = "layer7-goid-generator" )
    @GeneratedValue( generator = "generator")
    @Type(type = "com.l7tech.server.util.GoidType")
    public Goid getGoid() {
        return goid;
    }

    @Override
    public void setGoid(Goid goid) {
        this.goid = goid.clone();
    }

    @Override
    @Transient
    @XmlTransient
    public boolean isUnsaved() {
        return DEFAULT_GOID.equals(goid);
    }

    @Transient
    @XmlTransient
    boolean isPreserveId() {
        return preserveId;
    }

    void preserveId() {
         preserveId = true;
    }

    /**
     * Any subclasses which has a version column must override this method and map it manually as it is transient.
     */
    @Override
    @Transient
    @XmlAttribute
    public int getVersion() {
        return version;
    }

    @Override
    public void setVersion(int version) {
        checkLocked();
        this.version = version;
    }

    @Override
    @Transient
    @XmlID @XmlAttribute
    public String getId() {
        return goid.toString();
    }

    @Deprecated // only for XML, likely to throw NFE
    public void setId(String id) {
        checkLocked();
        if (id == null || id.length() == 0) {
            setGoid(DEFAULT_GOID);
        } else {
            try {
                setGoid(GoidUpgradeMapper.mapId(EntityType.findTypeByEntity(this.getClass()), id));
            } catch (IllegalArgumentException e) {
                setGoid(DEFAULT_GOID);
            }
        }
    }

    protected void lock() {
        locked = true;
    }

    @Transient
    protected boolean isLocked() {
        return locked;
    }

    /**
     * Throws IllegalStateException if {@link #isLocked}.
     */
    protected void checkLocked() {
        if ( isLocked() ) throw new IllegalStateException("Cannot update locked entity");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PersistentEntityImp that = (PersistentEntityImp) o;

        //noinspection RedundantIfStatement
        if (goid != null ? !goid.equals(that.goid) : that.goid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return goid != null ? goid.hashCode() : 0;
    }
}
