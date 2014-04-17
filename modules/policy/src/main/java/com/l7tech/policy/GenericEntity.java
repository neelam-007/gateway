package com.l7tech.policy;

import com.l7tech.security.rbac.RbacAttribute;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import org.hibernate.annotations.Proxy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents an entity added by a modular assertion, whose concrete type might not be known at compile time.
 */
@XmlRootElement
@Entity
@Proxy(lazy=false)
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@Table(name="generic_entity")
public class GenericEntity extends NamedEntityImp {
    private String description;
    private String entityClassName;
    private String valueXml;
    private boolean enabled = true;
    // If you add base fields, be sure to update copyBaseFields, and to provide for DB upgrades

    public GenericEntity() {
        entityClassName = getClass().getName();
    }

    /**
     * Set the specified generic entity to have the same base field values as this generic entity, overwriting some if necessary.
     *
     * @param source entity to copy from.  Required.
     * @param dest entity to copy to.  Required.
     */
    public static void copyBaseFields(GenericEntity source, GenericEntity dest) {
        dest.setGoid(source.getGoid());
        dest.setVersion(source.getVersion());
        dest.setName(source.getName());
        dest.setDescription(source.getDescription());
        dest.setEnabled(source.isEnabled());
        dest.setEntityClassName(source.getEntityClassName());
        dest.setValueXml(source.getValueXml());
    }

    @RbacAttribute
    @Size(min = 1, max = 255)
    @Override
    @Transient
    public String getName() {
        return super.getName();
    }

    @RbacAttribute
    @Size(min=0,max=131072) // limit to 128k
    @Column(name="description", length=Integer.MAX_VALUE)
    @Lob
    public final String getDescription() {
        return description;
    }

    public final void setDescription(String description) {
        this.description = description;
    }

    @RbacAttribute
    @NotNull
    @Size(min=1,max=255)
    @Column(name="classname", length=255)
    public final String getEntityClassName() {
        return entityClassName;
    }

    public final void setEntityClassName(String entityClassName) {
        Class thisClass = getClass();
        if (GenericEntity.class != thisClass && !thisClass.getName().equals(entityClassName))
            throw new IllegalArgumentException("Entity classname may only be " + thisClass.getName() + " for this concrete subclass of GenericEntity");
        this.entityClassName = entityClassName;
    }

    @RbacAttribute
    @Column(name="enabled")
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @NotNull
    @Size(min=0,max=131072) // limit to 128k
    @Column(name="value_xml", nullable=false, length=Integer.MAX_VALUE)
    @Lob
    public final String getValueXml() {
        return valueXml;
    }

    public final void setValueXml(String valueXml) {
        this.valueXml = valueXml;
    }
}
